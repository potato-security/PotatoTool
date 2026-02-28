package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.core.FingerprintService.FingerprintResult;
import com.potato.potatotool.content.redTeam.vulnScanner.event.*;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.ScanExecutionException;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanTask;
import com.potato.potatotool.content.redTeam.vulnScanner.model.TaskState;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 核心扫描引擎（支持暂停和恢复）
 * 负责执行漏洞扫描任务
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanEngine {
    
    private final ScanConfig scanConfig;
    private final ScanEventDispatcher eventDispatcher;
    private final PocExecutor pocExecutor;
    private final VulnScanDatabase database;
    
    private ExecutorService executorService;
    private volatile boolean isScanning = false;
    private volatile boolean isPaused = false;
    private String currentScanId;
    
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger vulnerabilityCount = new AtomicInteger(0);
    private final List<ScanResult> scanResults = Collections.synchronizedList(new ArrayList<>());
    
    // 支持暂停/恢复
    private final Set<String> completedTaskIds = Collections.synchronizedSet(new HashSet<>());
    private List<String> currentTargets;
    private List<PocObj.Poc> currentPocs;
    private long scanStartTime;
    
    // 请求聚合
    private ClusteredPocExecutor clusteredExecutor;

    // 指纹结果映射（由 VulnScanService 设置，用于附加到 ScanResult）
    private volatile Map<String, FingerprintResult> fingerprintResultMap;

    // 数据库批处理队列
    private final BlockingQueue<TaskState> dbQueue = new LinkedBlockingQueue<>(5000);
    private volatile boolean stopDbThread = false;
    private Thread dbWriterThread;
    
    public ScanEngine(ScanConfig scanConfig) {
        this.scanConfig = scanConfig != null ? scanConfig : new ScanConfig();
        this.eventDispatcher = new ScanEventDispatcher(false);
        this.pocExecutor = new PocExecutor(this.scanConfig);
        this.database = VulnScanDatabase.getInstance();
        
        // 启动数据库写入线程
        startDbWriterThread();
    }
    
    private void startDbWriterThread() {
        stopDbThread = false;
        dbWriterThread = new Thread(() -> {
            List<TaskState> batch = new ArrayList<>();
            while (!stopDbThread || !dbQueue.isEmpty()) {
                try {
                    // 每次最多取100个或者等待1秒
                    TaskState task = dbQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        batch.add(task);
                        // 尝试获取更多，直到达到批次大小
                        dbQueue.drainTo(batch, 99);
                    }
                    
                    if (!batch.isEmpty()) {
                        try {
                            database.saveTaskStates(batch);
                            batch.clear();
                        } catch (Exception e) {
                            System.err.println("批量保存任务状态失败: " + e.getMessage());
                            // 失败重试或丢弃? 目前策略是丢弃，避免无限阻塞
                            batch.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("DB写入线程异常: " + e.getMessage());
                }
            }
        }, "scan-db-writer");
        dbWriterThread.setDaemon(true);
        dbWriterThread.start();
    }
    
    /**
     * 添加事件监听器
     */
    public void addEventListener(ScanEventListener listener) {
        eventDispatcher.addEventListener(listener);
    }
    
    /**
     * 移除事件监听器
     */
    public void removeEventListener(ScanEventListener listener) {
        eventDispatcher.removeEventListener(listener);
    }

    /**
     * 获取 PocExecutor（供 SmartPocSelector 使用）
     */
    public PocExecutor getPocExecutor() {
        return pocExecutor;
    }

    /**
     * 获取事件分发器（供 SmartPocSelector 使用）
     */
    public ScanEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * 设置指纹结果（单目标，向后兼容）
     */
    public void setFingerprintResult(FingerprintResult fingerprintResult) {
        if (fingerprintResult != null) {
            Map<String, FingerprintResult> map = new HashMap<>();
            map.put(null, fingerprintResult);
            this.fingerprintResultMap = map;
        } else {
            this.fingerprintResultMap = null;
        }
    }

    /**
     * 设置多目标指纹结果映射
     */
    public void setFingerprintResultMap(Map<String, FingerprintResult> fingerprintResultMap) {
        this.fingerprintResultMap = fingerprintResultMap;
    }

    /**
     * 获取指定目标的指纹结果
     */
    private FingerprintResult getFingerprintForTarget(String target) {
        if (fingerprintResultMap == null) return null;
        FingerprintResult result = fingerprintResultMap.get(target);
        if (result != null) return result;
        // 单目标兼容：尝试 null key
        return fingerprintResultMap.get(null);
    }
    
    /**
     * 开始扫描
     * 
     * @param targets 目标列表
     * @param pocs POC列表
     */
    public void startScan(List<String> targets, List<PocObj.Poc> pocs) {
        if (isScanning) {
            throw new IllegalStateException("扫描任务正在进行中");
        }
        
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("目标列表不能为空");
        }
        
        if (pocs == null || pocs.isEmpty()) {
            throw new IllegalArgumentException("POC列表不能为空");
        }
        
        // 生成扫描ID
        currentScanId = generateScanId();
        
        // 初始化
        isScanning = true;
        isPaused = false;
        scanResults.clear();
        completedTasks.set(0);
        vulnerabilityCount.set(0);
        completedTaskIds.clear();
        currentTargets = new ArrayList<>(targets);
        currentPocs = new ArrayList<>(pocs);
        scanStartTime = System.currentTimeMillis();
        
        // 初始化线程池
        initExecutorService();
        
        // 创建扫描任务
        List<ScanTask> tasks = createScanTasks(targets, pocs);
        totalTasks.set(tasks.size());
        
        // 保存扫描状态到数据库
        saveScanState();
        
        // 触发扫描开始事件
        eventDispatcher.dispatchScanStarted(
            new ScanStartedEvent(this, currentScanId, targets.size(), pocs.size(), tasks.size())
        );
        
        // 执行扫描
        executeScan(tasks);
    }
    
    /**
     * 恢复扫描
     */
    public void resumeScan(ScanState state, List<PocObj.Poc> pocs) {
        // 修复: 允许从暂停状态恢复,即使 isScanning 仍为 true
        if (isScanning && !isPaused) {
            throw new IllegalStateException("扫描任务正在进行中");
        }

        if (pocs == null || pocs.isEmpty()) {
            throw new IllegalArgumentException("POC列表不能为空");
        }
        
        // 恢复状态
        currentScanId = state.getScanId();
        isScanning = true;
        isPaused = false;
        scanStartTime = state.getStartTime();
        
        // 恢复配置
        scanConfig.setThreads(state.getThreads());
        scanConfig.setTimeout(state.getTimeout());
        if (state.getProxy() != null && !state.getProxy().isEmpty()) {
            scanConfig.setProxy(state.getProxy());
        }
        
        // 恢复目标和POC
        currentTargets = new ArrayList<>(state.getTargets());
        currentPocs = new ArrayList<>(pocs);
        
        // 恢复进度
        completedTasks.set(state.getCompletedTasks());
        totalTasks.set(state.getTotalTasks());

        // 加载已完成的任务ID
        try {
            completedTaskIds.clear();
            completedTaskIds.addAll(database.loadCompletedTaskIds(currentScanId));
        } catch (Exception e) {
            System.err.println("加载已完成任务失败: " + e.getMessage());
        }

        // 修复: 恢复之前发现的漏洞结果
        try {
            List<TaskState> vulnerableTasks = database.loadVulnerableTasks(currentScanId);
            ScanLogger.getInstance().info("SCAN", "从数据库加载了 " + vulnerableTasks.size() + " 个漏洞记录");

            // 根据 POC ID 构建 POC 映射表
            Map<String, PocObj.Poc> pocMap = new HashMap<>();
            for (PocObj.Poc poc : pocs) {
                pocMap.put(poc.getId(), poc);
            }

            // 重建 ScanResult 对象
            for (TaskState task : vulnerableTasks) {
                PocObj.Poc poc = pocMap.get(task.getPocId());
                if (poc != null) {
                    ScanResult result = new ScanResult();
                    result.setTarget(task.getTarget());
                    result.setPoc(poc);
                    result.setVulnerable(true);
                    result.setTimestamp(task.getEndTime());

                    scanResults.add(result);
                    vulnerabilityCount.incrementAndGet();

                    // 触发漏洞发现事件，让UI更新
                    eventDispatcher.dispatchVulnerabilityFound(
                        new VulnerabilityFoundEvent(this, currentScanId, result)
                    );
                } else {
                    ScanLogger.getInstance().warn("SCAN", "POC不存在: " + task.getPocId());
                }
            }
        } catch (Exception e) {
            System.err.println("恢复漏洞结果失败: " + e.getMessage());
        }
        
        // 初始化线程池
        initExecutorService();
        
        // 创建所有任务
        List<ScanTask> allTasks = createScanTasks(currentTargets, currentPocs);
        
        // 过滤出未完成的任务
        List<ScanTask> remainingTasks = allTasks.stream()
            .filter(task -> !isTaskCompleted(task))
            .collect(Collectors.toList());
        
        ScanLogger.getInstance().info("SCAN", "恢复扫描: " + currentScanId +
            ", 总任务: " + allTasks.size() +
            ", 已完成: " + completedTaskIds.size() +
            ", 剩余: " + remainingTasks.size());
        
        // 更新数据库状态为运行中
        try {
            database.resumeScanState(currentScanId);
        } catch (Exception e) {
            System.err.println("更新扫描状态失败: " + e.getMessage());
        }
        
        // 触发扫描开始事件
        eventDispatcher.dispatchScanStarted(
            new ScanStartedEvent(this, currentScanId, currentTargets.size(), currentPocs.size(), remainingTasks.size())
        );
        
        // 执行剩余任务
        executeScan(remainingTasks);
    }
    
    /**
     * 暂停扫描（强行暂停，立即中断所有任务）
     */
    public void pauseScan() {
        if (!isScanning || isPaused) {
            return;
        }

        isPaused = true;

        // 强行中断线程池，立即停止所有任务（不等待确认）
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        // 等待数据库批量写入队列完成（避免丢失已完成的任务记录）
        try {
            Thread.sleep(500); // 等待500毫秒让队列中的任务写入数据库
            // 手动刷新队列中剩余的任务
            List<TaskState> remaining = new ArrayList<>();
            dbQueue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                database.saveTaskStates(remaining);
                ScanLogger.getInstance().info("SCAN", "刷新了 " + remaining.size() + " 个待写入的任务记录");
            }
        } catch (Exception e) {
            ScanLogger.getInstance().warn("SCAN", "刷新任务队列失败: " + e.getMessage());
        }

        // 注意：不要调用 cleanupInterruptedTasks()
        // 因为暂停后恢复时应该继续之前的进度，而不是重新开始
        // 已完成的任务记录应该保留在数据库中

        // 更新数据库状态
        try {
            database.pauseScanState(currentScanId);
            saveScanState();
        } catch (Exception e) {
            System.err.println("保存暂停状态失败: " + e.getMessage());
        }

        // 保存到扫描历史记录（scan_records表）
        // 使用saveOrUpdateScanRecord确保同一次扫描只有一条历史记录
        try {
            // 构建配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("threads", scanConfig.getThreads());
            config.put("timeout", scanConfig.getTimeout());
            config.put("retries", scanConfig.getRetries());
            if (scanConfig.getProxy() != null) {
                config.put("proxy", scanConfig.getProxy());
            }
            if (currentTargets != null) {
                config.put("targetCount", currentTargets.size());
            }
            if (currentPocs != null) {
                config.put("pocCount", currentPocs.size());
            }

            // 计算扫描持续时间（秒）
            long duration = (System.currentTimeMillis() - scanStartTime) / 1000;

            // 保存或更新扫描记录到历史（使用scan_id关联，同一扫描任务只保留一条记录）
            database.saveOrUpdateScanRecord(
                currentScanId,
                new ArrayList<>(scanResults),
                config,
                duration,
                "paused"
            );
        } catch (Exception e) {
            System.err.println("保存暂停记录到历史失败: " + e.getMessage());
        }

        ScanLogger.getInstance().info("SCAN", "扫描已强行暂停: " + currentScanId +
            ", 已完成: " + completedTasks.get() + "/" + totalTasks.get());
    }

    /**
     * 生成扫描ID
     */
    private String generateScanId() {
        return "scan-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 初始化线程池
     * 从VulnScanConfig读取线程池配置，支持用户自定义
     */
    private void initExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // 从全局配置读取线程池参数
        VulnScanConfig globalConfig = VulnScanConfig.getInstance();
        int configCoreThreads = globalConfig.getCoreThreads();
        int configMaxThreads = globalConfig.getMaxThreads();
        int configQueueSize = globalConfig.getQueueSize();
        
        // 确定最终使用的值：优先使用scanConfig，然后是全局配置
        int baseCoreThreads = scanConfig.getThreads() > 0 ? scanConfig.getThreads() : configCoreThreads;
        int coreThreads = Math.max(2, baseCoreThreads);
        int maxThreads = Math.max(coreThreads, configMaxThreads > 0 ? configMaxThreads : 
            Runtime.getRuntime().availableProcessors() * 2);
        int queueSize = configQueueSize > 0 ? configQueueSize : 1000;
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreThreads,
            maxThreads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueSize),
            new NamedThreadFactory("scan-engine"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        
        this.executorService = executor;

        if (scanConfig.isDebug()) {
            ScanLogger.getInstance().debug("SYSTEM", "扫描线程池初始化: coreThreads=" + coreThreads +
                ", maxThreads=" + maxThreads + ", queueSize=" + queueSize);
        }
    }
    
    /**
     * 创建扫描任务
     */
    private List<ScanTask> createScanTasks(List<String> targets, List<PocObj.Poc> pocs) {
        List<ScanTask> tasks = new ArrayList<>();
        
        for (String target : targets) {
            for (PocObj.Poc poc : pocs) {
                tasks.add(new ScanTask(target, poc));
            }
        }
        
        return tasks;
    }
    
    /**
     * 检查任务是否已完成
     */
    private boolean isTaskCompleted(ScanTask task) {
        String taskId = TaskState.generateTaskId(task.getTarget(), task.getPoc().getId());
        return completedTaskIds.contains(taskId);
    }
    
    /**
     * 执行扫描
     */
    private void executeScan(List<ScanTask> tasks) {
        // 如果没有任务，直接返回
        if (tasks == null || tasks.isEmpty()) {
            ScanLogger.getInstance().warn("SCAN", "没有需要执行的任务");
            return;
        }

        // 如果启用聚合，按目标分组聚类执行
        if (scanConfig.isEnableClustering()) {
            if (isAllSameTarget(tasks)) {
                executeWithClustering(tasks);
                return;
            }

            // 多目标：每组独立聚类，用 CompletableFuture 并行
            Map<String, List<ScanTask>> tasksByTarget = new LinkedHashMap<>();
            for (ScanTask task : tasks) {
                tasksByTarget.computeIfAbsent(task.getTarget(), k -> new ArrayList<>()).add(task);
            }

            final String thisScanId = currentScanId;
            CompletableFuture<?>[] futures = tasksByTarget.values().stream()
                .map(group -> CompletableFuture.runAsync(() -> executeClusterGroup(group), executorService))
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures)
                .thenRun(() -> handleScanCompletion(thisScanId))
                .exceptionally(throwable -> {
                    handleScanException(thisScanId, throwable);
                    return null;
                });
            return;
        }

        // 保存当前扫描ID，用于回调中验证（防止恢复扫描后旧回调错误触发）
        final String thisScanId = currentScanId;

        // 提交所有任务
        CompletableFuture<?>[] futures = tasks.stream()
            .map(task -> CompletableFuture.runAsync(() -> executeTask(task), executorService))
            .toArray(CompletableFuture[]::new);

        // 等待所有任务完成
        CompletableFuture.allOf(futures)
            .thenRun(() -> handleScanCompletion(thisScanId))
            .exceptionally(throwable -> {
                handleScanException(thisScanId, throwable);
                return null;
            });
    }

    /**
     * 检查所有任务是否指向同一目标
     */
    private boolean isAllSameTarget(List<ScanTask> tasks) {
        if (tasks.size() <= 1) return true;
        String first = tasks.get(0).getTarget();
        for (int i = 1; i < tasks.size(); i++) {
            if (!first.equals(tasks.get(i).getTarget())) return false;
        }
        return true;
    }

    /**
     * 使用聚类执行器执行扫描
     */
    private void executeWithClustering(List<ScanTask> tasks) {
        final String thisScanId = currentScanId;

        // 延迟初始化 ClusteredPocExecutor
        if (clusteredExecutor == null) {
            clusteredExecutor = new ClusteredPocExecutor(scanConfig, pocExecutor);
        }

        String target = tasks.get(0).getTarget();
        List<PocObj.Poc> pocs = new ArrayList<>();
        for (ScanTask task : tasks) {
            pocs.add(task.getPoc());
        }

        eventDispatcher.dispatchScanInfo(new ScanInfoEvent(this, currentScanId,
            ScanInfoEvent.InfoType.CLUSTERING_STATS, "启用请求聚类优化，POC 数量: " + pocs.size()));

        CompletableFuture.runAsync(() -> {
            try {
                List<ScanResult> results = clusteredExecutor.executeClusteredPocs(target, pocs);

                // 处理聚类结果
                for (ScanResult result : results) {
                    if (isPaused || !isScanning) break;
                    if (!thisScanId.equals(currentScanId)) break;

                    // 附加指纹信息
                    FingerprintResult fp = getFingerprintForTarget(result.getTarget());
                    if (fp != null && fp.hasFingerprint()) {
                        result.setFingerprint(fp.toFingerprintInfo());
                    }

                    if (result.isVulnerable()) {
                        scanResults.add(result);
                        vulnerabilityCount.incrementAndGet();
                        eventDispatcher.dispatchVulnerabilityFound(
                            new VulnerabilityFoundEvent(this, currentScanId, result));
                    }

                    // 保存任务状态
                    String taskId = TaskState.generateTaskId(result.getTarget(),
                        result.getPoc() != null ? result.getPoc().getId() : "unknown");
                    TaskState taskState = new TaskState();
                    taskState.setTaskId(taskId);
                    taskState.setScanId(currentScanId);
                    taskState.setTarget(result.getTarget());
                    taskState.setPocId(result.getPoc() != null ? result.getPoc().getId() : "unknown");
                    taskState.setCompleted(true);
                    taskState.setVulnerable(result.isVulnerable());
                    taskState.setStartTime(result.getTimestamp());
                    taskState.setEndTime(System.currentTimeMillis());
                    if (!dbQueue.offer(taskState)) {
                        database.saveTaskState(taskState);
                    }
                    completedTaskIds.add(taskId);

                    int completed = completedTasks.incrementAndGet();
                    int total = totalTasks.get();
                    if (completed % 10 == 0 || completed == total) {
                        eventDispatcher.dispatchScanProgress(
                            new ScanProgressEvent(this, currentScanId, completed, total, getVulnerabilityCount()));
                    }
                }

                // 输出聚类统计
                ClusteredPocExecutor.ClusterStats stats = clusteredExecutor.getStats();
                eventDispatcher.dispatchScanInfo(new ScanInfoEvent(this, currentScanId,
                    ScanInfoEvent.InfoType.CLUSTERING_STATS, stats.toString()));

            } catch (Exception e) {
                ScanLogger.getInstance().error("SCAN", "聚类执行失败: " + e.getMessage());
            }
        }, executorService)
        .thenRun(() -> handleScanCompletion(thisScanId))
        .exceptionally(throwable -> {
            handleScanException(thisScanId, throwable);
            return null;
        });
    }

    /**
     * 执行单个目标组的聚类扫描（不含 completion handler）
     */
    private void executeClusterGroup(List<ScanTask> tasks) {
        if (clusteredExecutor == null) {
            clusteredExecutor = new ClusteredPocExecutor(scanConfig, pocExecutor);
        }

        String target = tasks.get(0).getTarget();
        List<PocObj.Poc> pocs = new ArrayList<>();
        for (ScanTask task : tasks) {
            pocs.add(task.getPoc());
        }

        try {
            List<ScanResult> results = clusteredExecutor.executeClusteredPocs(target, pocs);

            for (ScanResult result : results) {
                if (isPaused || !isScanning) break;

                FingerprintResult fp = getFingerprintForTarget(result.getTarget());
                if (fp != null && fp.hasFingerprint()) {
                    result.setFingerprint(fp.toFingerprintInfo());
                }

                if (result.isVulnerable()) {
                    scanResults.add(result);
                    vulnerabilityCount.incrementAndGet();
                    eventDispatcher.dispatchVulnerabilityFound(
                        new VulnerabilityFoundEvent(this, currentScanId, result));
                }

                String taskId = TaskState.generateTaskId(result.getTarget(),
                    result.getPoc() != null ? result.getPoc().getId() : "unknown");
                TaskState taskState = new TaskState();
                taskState.setTaskId(taskId);
                taskState.setScanId(currentScanId);
                taskState.setTarget(result.getTarget());
                taskState.setPocId(result.getPoc() != null ? result.getPoc().getId() : "unknown");
                taskState.setCompleted(true);
                taskState.setVulnerable(result.isVulnerable());
                taskState.setStartTime(result.getTimestamp());
                taskState.setEndTime(System.currentTimeMillis());
                if (!dbQueue.offer(taskState)) {
                    database.saveTaskState(taskState);
                }
                completedTaskIds.add(taskId);

                int completed = completedTasks.incrementAndGet();
                int total = totalTasks.get();
                if (completed % 10 == 0 || completed == total) {
                    eventDispatcher.dispatchScanProgress(
                        new ScanProgressEvent(this, currentScanId, completed, total, getVulnerabilityCount()));
                }
            }
        } catch (Exception e) {
            ScanLogger.getInstance().error("SCAN", "聚类执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理扫描正常完成
     */
    private void handleScanCompletion(String thisScanId) {
        if (!thisScanId.equals(currentScanId)) {
            ScanLogger.getInstance().debug("SCAN", "扫描ID不匹配，忽略完成事件: " + thisScanId);
            return;
        }
        if (isPaused) {
            isScanning = false;
            ScanLogger.getInstance().debug("SCAN", "扫描已暂停，忽略完成事件");
            return;
        }
        if (executorService.isShutdown()) {
            ScanLogger.getInstance().debug("SCAN", "线程池已关闭，忽略完成事件");
            return;
        }

        long duration = System.currentTimeMillis() - scanStartTime;
        isScanning = false;

        try {
            int[] counts = countBySeverity();
            database.completeScanTask(currentScanId, getVulnerabilityCount(),
                counts[0], counts[1], counts[2], counts[3], counts[4]);
        } catch (Exception e) {
            System.err.println("更新扫描状态失败: " + e.getMessage());
        }

        eventDispatcher.dispatchScanCompleted(
            new ScanCompletedEvent(this, currentScanId,
                new ArrayList<>(scanResults), duration, true));
    }

    /**
     * 处理扫描异常
     */
    private void handleScanException(String thisScanId, Throwable throwable) {
        if (!thisScanId.equals(currentScanId)) {
            ScanLogger.getInstance().debug("SCAN", "扫描ID不匹配，忽略异常事件: " + thisScanId);
            return;
        }
        if (isPaused) {
            ScanLogger.getInstance().debug("SCAN", "扫描已暂停，忽略异常事件");
            return;
        }
        if (executorService.isShutdown()) {
            ScanLogger.getInstance().debug("SCAN", "线程池已关闭，忽略异常事件");
            return;
        }

        long duration = System.currentTimeMillis() - scanStartTime;
        isScanning = false;
        isPaused = false;

        eventDispatcher.dispatchScanError(
            new ScanErrorEvent(this, currentScanId, "扫描执行失败", throwable));
        eventDispatcher.dispatchScanCompleted(
            new ScanCompletedEvent(this, currentScanId,
                new ArrayList<>(scanResults), duration, false));
    }
    
    /**
     * 执行单个扫描任务
     */
    private void executeTask(ScanTask task) {
        String taskId = TaskState.generateTaskId(task.getTarget(), task.getPoc().getId());

        // 标记任务是否被跳过（用于决定是否增加计数器）
        boolean taskSkipped = false;

        try {
            // 检查是否暂停或停止
            if (isPaused || !isScanning) {
                taskSkipped = true;
                return;
            }

            // 检查是否已完成（用于恢复扫描）
            if (completedTaskIds.contains(taskId)) {
                taskSkipped = true;
                return;
            }

            long taskStartTime = System.currentTimeMillis();

            // 执行POC前再次检查暂停状态
            if (isPaused || !isScanning) {
                taskSkipped = true;
                return;
            }

            // 执行POC
            ScanResult result = pocExecutor.execute(task.getTarget(), task.getPoc());

            // 执行POC后再次检查暂停状态（避免保存无效结果）
            if (isPaused || !isScanning) {
                taskSkipped = true;
                return;
            }

            if (result != null) {
                // 附加指纹信息
                FingerprintResult fp = getFingerprintForTarget(task.getTarget());
                if (fp != null && fp.hasFingerprint()) {
                    result.setFingerprint(fp.toFingerprintInfo());
                }

                // 内存优化：仅保存发现漏洞的结果，防止大规模扫描时OOM
                if (result.isVulnerable()) {
                    scanResults.add(result);
                    vulnerabilityCount.incrementAndGet();

                    // 如果发现漏洞，触发事件
                    eventDispatcher.dispatchVulnerabilityFound(
                        new VulnerabilityFoundEvent(this, currentScanId, result)
                    );
                }

                // 保存任务状态
                TaskState taskState = new TaskState();
                taskState.setTaskId(taskId);
                taskState.setScanId(currentScanId);
                taskState.setTarget(task.getTarget());
                taskState.setPocId(task.getPoc().getId());
                taskState.setCompleted(true);
                taskState.setVulnerable(result.isVulnerable());
                taskState.setStartTime(taskStartTime);
                taskState.setEndTime(System.currentTimeMillis());

                try {
                    // 使用队列异步批量保存，提高性能
                    if (!dbQueue.offer(taskState)) {
                        // 队列满了，降级为直接保存或丢弃（为了性能选择丢弃非重要状态? 不，应该阻塞或直接保存）
                        // 这里选择直接保存以保证数据完整性，虽然会慢一点
                        database.saveTaskState(taskState);
                    }
                    // 仅在内存中标记已完成，不等待DB确认
                    completedTaskIds.add(taskId);
                } catch (Exception e) {
                    System.err.println("保存任务状态失败: " + e.getMessage());
                }
            }

        } catch (NetworkException e) {
            // 网络异常，记录但不中断扫描
            if (scanConfig.isDebug()) {
                eventDispatcher.dispatchScanError(
                    new ScanErrorEvent(this, currentScanId, task.getTarget(),
                        task.getPoc().getId(), e.getMessage(), e)
                );
            }
        } catch (ScanExecutionException e) {
            // 扫描执行异常
            if (scanConfig.isDebug()) {
                eventDispatcher.dispatchScanError(
                    new ScanErrorEvent(this, currentScanId, task.getTarget(),
                        task.getPoc().getId(), e.getMessage(), e)
                );
            }
        } catch (Exception e) {
            // 检查是否是中断导致的异常
            if (e.getCause() instanceof InterruptedException ||
                e.getMessage() != null && e.getMessage().contains("被中断")) {
                Thread.currentThread().interrupt();
                taskSkipped = true;
                return;
            }
            // 其他异常
            eventDispatcher.dispatchScanError(
                new ScanErrorEvent(this, currentScanId, task.getTarget(),
                    task.getPoc().getId(), "未知错误: " + e.getMessage(), e)
            );
        } finally {
            // 如果任务被跳过（暂停/停止/已完成），不更新进度
            if (taskSkipped) {
                return;
            }

            // 更新进度计数器（无论如何都要增加，确保最终进度为100%）
            int completed = completedTasks.incrementAndGet();
            int total = totalTasks.get();

            // 如果已暂停或停止，不触发进度事件（但计数器已更新）
            if (isPaused || !isScanning) {
                return;
            }

            int vulnerabilities = getVulnerabilityCount();

            // 动态进度更新频率：任务少时每个都更新，任务多时按比例
            int progressInterval = Math.max(1, total / 100);
            if (completed <= 3 || completed == total || completed % progressInterval == 0) {
                // 触发进度事件
                eventDispatcher.dispatchScanProgress(
                    new ScanProgressEvent(this, currentScanId, completed, total, vulnerabilities)
                );
            }

            // 数据库更新频率较低（每50个或完成时）
            if (completed % 50 == 0 || completed == total) {
                try {
                    database.updateScanState(currentScanId,
                        isPaused ? ScanState.Status.PAUSED : ScanState.Status.RUNNING,
                        completed, vulnerabilities);
                } catch (Exception e) {
                    // 忽略更新错误
                }
            }
        }
    }
    
    /**
     * 保存扫描状态
     */
    private void saveScanState() {
        try {
            ScanState state = new ScanState();
            state.setScanId(currentScanId);
            state.setStatus(isPaused ? ScanState.Status.PAUSED : ScanState.Status.RUNNING);
            state.setStartTime(scanStartTime);
            state.setPauseTime(isPaused ? System.currentTimeMillis() : 0);
            state.setThreads(scanConfig.getThreads());
            state.setTimeout(scanConfig.getTimeout());
            state.setProxy(scanConfig.getProxy());
            state.setTargets(currentTargets);

            // 保存POC格式选择
            if (scanConfig.getEnabledPocFormats() != null) {
                state.setSelectedFormats(new ArrayList<>(scanConfig.getEnabledPocFormats()));
            }

            // 保存POC ID列表
            List<String> pocIds = currentPocs.stream()
                .map(PocObj.Poc::getId)
                .collect(Collectors.toList());
            state.setPocIds(pocIds);

            state.setTotalTasks(totalTasks.get());
            state.setCompletedTasks(completedTasks.get());
            state.setVulnerabilitiesFound(getVulnerabilityCount());

            // 保存严重度统计
            int[] counts = countBySeverity();
            state.setCriticalCount(counts[0]);
            state.setHighCount(counts[1]);
            state.setMediumCount(counts[2]);
            state.setLowCount(counts[3]);
            state.setInfoCount(counts[4]);

            database.saveScanState(state);
        } catch (Exception e) {
            System.err.println("保存扫描状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止扫描（强行停止，立即中断所有任务）
     */
    public void stopScan() {
        if (!isScanning) {
            return;
        }

        isScanning = false;
        isPaused = false;

        // 强行中断线程池，立即停止所有任务（不等待确认）
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        // 等待数据库批量写入队列完成（避免丢失已完成的任务记录）
        try {
            Thread.sleep(500); // 等待500毫秒让队列中的任务写入数据库
            // 手动刷新队列中剩余的任务
            List<TaskState> remaining = new ArrayList<>();
            dbQueue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                database.saveTaskStates(remaining);
                ScanLogger.getInstance().info("SCAN", "刷新了 " + remaining.size() + " 个待写入的任务记录");
            }
        } catch (Exception e) {
            ScanLogger.getInstance().warn("SCAN", "刷新任务队列失败: " + e.getMessage());
        }

        // 注意：停止操作保留已完成的任务记录
        // 不调用 cleanupInterruptedTasks()，以便用户可以查看已发现的漏洞

        // 更新数据库状态为停止（包含严重度统计）
        try {
            database.stopScanTask(currentScanId);
            int[] counts = countBySeverity();
            database.updateScanVulnCounts(currentScanId, getVulnerabilityCount(),
                counts[0], counts[1], counts[2], counts[3], counts[4]);
        } catch (Exception e) {
            System.err.println("更新扫描状态失败: " + e.getMessage());
        }

        ScanLogger.getInstance().info("SCAN", "扫描已强行停止: " + currentScanId +
            ", 已完成: " + completedTasks.get() + "/" + totalTasks.get());
    }
    
    /**
     * 检查是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * 检查是否已暂停
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * 获取扫描结果
     */
    public List<ScanResult> getScanResults() {
        return new ArrayList<>(scanResults);
    }
    
    /**
     * 获取漏洞数量（无锁，O(1)）
     */
    public int getVulnerabilityCount() {
        return vulnerabilityCount.get();
    }

    /**
     * 按严重度统计漏洞数量
     * @return [critical, high, medium, low, info]
     */
    private int[] countBySeverity() {
        int[] counts = new int[5];
        for (ScanResult result : scanResults) {
            if (result.isVulnerable() && result.getPoc() != null) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                if (severity != null) {
                    switch (severity) {
                        case CRITICAL: counts[0]++; break;
                        case HIGH: counts[1]++; break;
                        case MEDIUM: counts[2]++; break;
                        case LOW: counts[3]++; break;
                        case INFO: counts[4]++; break;
                    }
                }
            }
        }
        return counts;
    }

    /**
     * 获取当前扫描ID
     */
    public String getCurrentScanId() {
        return currentScanId;
    }
    
    /**
     * 获取当前进度
     */
    public int getCompletedTasks() {
        return completedTasks.get();
    }
    
    public int getTotalTasks() {
        return totalTasks.get();
    }
    
    /**
     * 关闭扫描引擎
     */
    public void shutdown() {
        stopScan();
        
        // 停止DB写入线程
        stopDbThread = true;
        if (dbWriterThread != null) {
            dbWriterThread.interrupt();
        }
        
        eventDispatcher.shutdown();
        pocExecutor.shutdown();
    }
    
    /**
     * 命名线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
