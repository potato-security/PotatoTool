package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.ResponseMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanTask;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocManager;

import com.potato.potatotool.content.redTeam.vulnScanner.util.PerformanceMonitor;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConnectionPoolManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConfigOptimizer;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 漏洞扫描执行器
 * 负责加载POC、解析目标、执行扫描任务
 */
public class VulnScanExecutor {

    private ExecutorService scanExecutor;

    private final List<ScanResult> scanResults = Collections.synchronizedList(new ArrayList<>());
    private List<PocObj.Poc> pocList = new ArrayList<>();
    private final List<String> targetList = new ArrayList<>();
    private ScanConfig scanConfig;
    private boolean isScanning = false;

    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger totalTasks = new AtomicInteger(0);

    /**
     * 创建扫描执行器
     */
    public VulnScanExecutor() {
        this.scanConfig = new ScanConfig();
        initExecutor();
    }

    /**
     * 创建扫描执行器
     * @param scanConfig 扫描配置
     */
    public VulnScanExecutor(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
        initExecutor();
    }

    /**
     * 初始化线程池
     */
    private void initExecutor() {
        // 关闭现有线程池
        shutdownExecutors();

        // 获取CPU核心数和配置的线程数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int configThreads = scanConfig != null ? scanConfig.getThreads() : Math.max(4, cpuCores);
        
        // 计算线程池参数
        int coreThreads = Math.max(2, configThreads);
        int maxThreads = Math.max(coreThreads, cpuCores * 2);
        int queueSize = Math.max(1000, configThreads * 50);
        
        // 创建统一的扫描线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreThreads, // 核心线程数
                maxThreads, // 最大线程数
                60L, TimeUnit.SECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(queueSize), // 有界队列
                new NamedThreadFactory("vuln-scan"),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用者所在线程执行
        );
        executor.allowCoreThreadTimeOut(true); // 允许核心线程超时，节省资源
        
        this.scanExecutor = executor;
        
        // 输出线程池配置信息
        logSafely(String.format("线程池配置 - 核心线程数=%d, 最大线程数=%d, 队列容量=%d", 
                coreThreads, maxThreads, queueSize));
    }
    
    /**
     * 关闭线程池
     */
    private void shutdownExecutors() {
        shutdownExecutor(this.scanExecutor, "vuln-scan");
    }
    
    /**
     * 关闭指定线程池
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            try {
                logSafely("正在关闭线程池: " + name);
                executor.shutdown();
                // 增加等待时间，确保所有任务完成
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logSafely("强制关闭线程池: " + name);
                    executor.shutdownNow();
                    // 再次等待强制关闭完成
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logErrorSafely("线程池 " + name + " 无法完全关闭");
                    }
                }
                logSafely("线程池 " + name + " 已关闭");
            } catch (InterruptedException e) {
                logSafely("线程池关闭被中断，强制关闭: " + name);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
            // 设置为守护线程，允许程序正常退出
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * 加载POC文件
     * @param pocDirPath POC目录路径
     * @return 加载的POC数量
     */
    public int loadPocs(String pocDirPath) {
        try {
            this.pocList = PocManager.parseAllPocs(pocDirPath);
            return this.pocList.size();
        } catch (Exception e) {
            logErrorSafely("加载POC失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 设置POC列表
     * @param pocList POC列表
     */
    public void setPocList(List<PocObj.Poc> pocList) {
        this.pocList = pocList;
    }

    /**
     * 获取POC列表
     * @return POC列表
     */
    public List<PocObj.Poc> getPocList() {
        return this.pocList;
    }

    /**
     * 解析目标
     * @param targets 目标字符串
     * @return 解析的目标数量
     */
    public int parseTargets(String targets) {
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        targetList.clear();

        // 检查是否是文件路径
        File file = new File(targets);
        if (file.exists() && file.isFile()) {
            try {
                // 从文件中读取目标
                List<String> lines = Files.readAllLines(Paths.get(targets));
                for (String line : lines) {
                    addTarget(line.trim());
                }
            } catch (IOException e) {
                logErrorSafely("读取目标文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // 按逗号分隔目标
            String[] targetArray = targets.split(",");
            for (String target : targetArray) {
                addTarget(target.trim());
            }
        }

        return targetList.size();
    }

    /**
     * 添加目标
     * @param target 目标URL
     */
    private void addTarget(String target) {
        if (target == null || target.isEmpty()) {
            return;
        }

        // 规范化URL
        String normalizedTarget = target;
        
        // 如果没有协议前缀，添加默认的http前缀
        if (!normalizedTarget.startsWith("http://") && !normalizedTarget.startsWith("https://")) {
            normalizedTarget = "http://" + normalizedTarget;
        }
        
        // 如果有协议过滤，检查URL是否符合过滤条件
        if (scanConfig.getProtocol() != null && !scanConfig.getProtocol().isEmpty()) {
            String protocol = scanConfig.getProtocol().toLowerCase();
            if (normalizedTarget.startsWith("http://") && !"http".equals(protocol)) {
                return;
            } else if (normalizedTarget.startsWith("https://") && !"https".equals(protocol)) {
                return;
            }
        }
        
        // 添加到目标列表
        targetList.add(normalizedTarget);
    }

    /**
     * 获取目标列表
     * @return 目标列表
     */
    public List<String> getTargetList() {
        return this.targetList;
    }

    /**
     * 设置扫描配置
     * @param scanConfig 扫描配置
     */
    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
        
        // 根据配置更新线程池
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            scanExecutor.shutdown();
        }
        
        initExecutor();
    }

    /**
     * 获取扫描配置
     * @return 扫描配置
     */
    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    /**
     * 开始扫描
     * @param callback 扫描回调
     */
    public void startScan(VulnScanCallback callback) {
        if (isScanning) {
            logErrorSafely("扫描任务正在进行中，请等待当前扫描完成");
            return;
        }

        if (pocList.isEmpty()) {
            logErrorSafely("未加载任何POC，请先加载POC");
            return;
        }

        if (targetList.isEmpty()) {
            logErrorSafely("未设置任何目标，请先设置目标");
            return;
        }

        // 确保线程池已初始化
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            initExecutor();
        }

        // 标记为正在扫描
        isScanning = true;

        // 清空之前的扫描结果和缓存
        scanResults.clear();

        completedTasks.set(0);
        
        // 启动性能监控
        PerformanceMonitor.getInstance().startMonitoring();

        // 基础过滤POC列表
        List<PocObj.Poc> basicFilteredPocs = new ArrayList<>();
        for (PocObj.Poc poc : pocList) {
            // 根据协议类型过滤POC
            if (scanConfig.getProtocol() != null && !scanConfig.getProtocol().isEmpty()
                    && !scanConfig.getProtocol().equalsIgnoreCase(poc.getProtocol())) {
                continue;
            }

            // 根据严重程度过滤POC
            if (scanConfig.getSeverity() != null && poc.getSeverity() != null
                    && scanConfig.getSeverity().ordinal() < poc.getSeverity().ordinal()) {
                continue;
            }

            basicFilteredPocs.add(poc);
        }

        // 为每个目标智能选择POC
        Map<String, List<PocObj.Poc>> targetPocMap = new HashMap<>();
        for (String target : targetList) {
            targetPocMap.put(target, basicFilteredPocs);
        }

        // 计算总任务数
        int calculatedTotalTasks = targetPocMap.values().stream()
            .mapToInt(List::size)
            .sum();
        totalTasks.set(calculatedTotalTasks);
        
        // 使用配置优化器预估最优配置
        ConfigOptimizer optimizer = ConfigOptimizer.getInstance();
        ScanConfig optimizedConfig = optimizer.estimateOptimalConfig(targetList.size(), basicFilteredPocs.size(), scanConfig);
        
        if (scanConfig.isDebug()) {
            logSafely("原始配置: 线程数=" + scanConfig.getThreads() + ", 超时=" + scanConfig.getTimeout() + "秒");
        logSafely("优化配置: 线程数=" + optimizedConfig.getThreads() + ", 超时=" + optimizedConfig.getTimeout() + "秒");
            
            // 如果优化配置与原配置差异较大，给出建议
            if (Math.abs(optimizedConfig.getThreads() - scanConfig.getThreads()) > 2) {
                logSafely("建议调整线程数为: " + optimizedConfig.getThreads());
            }
        }
        
        final int batchSize = Math.min(1000, calculatedTotalTasks); // 每批最多1000个任务

        // 创建一个监控任务，管理批量执行和完成通知
        scanExecutor.submit(() -> {
            try {
                // 创建优化的任务列表
                List<ScanTask> allTasks = createOptimizedTasks(targetPocMap);
                List<List<ScanTask>> batches = createBatches(allTasks, batchSize);

                for (List<ScanTask> batch : batches) {
                    if (!isScanning) {
                        // 如果扫描已停止，则退出
                        break;
                    }

                    // 为当前批次创建任务列表
                    List<Future<?>> batchTasks = new ArrayList<>();

                    // 提交当前批次的任务
                    for (ScanTask scanTask : batch) {
                        if (!isScanning) {
                            break;
                        }

                        // 使用统一的线程池
                        ExecutorService executor = scanExecutor;

                        Future<?> task = executor.submit(() -> {
                            long taskStartTime = System.currentTimeMillis();
                            try {
                                ScanResult result = scanTarget(scanTask.getTarget(), scanTask.getPoc());
                                if (result != null) {
                                    scanResults.add(result);
                                    if (callback != null) {
                                        try {
                                            callback.onResult(result);
                                        } catch (Exception e) {
                                            logErrorSafely("回调执行异常: " + e.getMessage());
                                            if (scanConfig.isDebug()) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logErrorSafely("扫描任务执行异常: " + e.getMessage());
                                if (scanConfig.isDebug()) {
                                    e.printStackTrace();
                                }
                            } finally {
                                // 记录线程池性能
                                long taskDuration = System.currentTimeMillis() - taskStartTime;
                                PerformanceMonitor.getInstance().recordThreadPoolPerformance("vuln-scan", taskDuration);
                                
                                // 更新完成任务计数
                                completedTasks.incrementAndGet();
                            }
                        });
                        batchTasks.add(task);
                    }

                    // 等待当前批次的任务完成，设置超时时间
                    for (Future<?> task : batchTasks) {
                        try {
                            // 设置任务超时时间，防止任务卡住
                            int timeoutSeconds = scanConfig.getTimeout() > 0 ? scanConfig.getTimeout() * 2 : 60;
                            task.get(timeoutSeconds, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            // 任务超时，取消任务
                            task.cancel(true);
                            logErrorSafely("扫描任务超时已取消");
                        } catch (InterruptedException e) {
                            // 线程被中断
                            Thread.currentThread().interrupt();
                            logErrorSafely("扫描任务被中断");
                            break;
                        } catch (ExecutionException e) {
                            logErrorSafely("等待扫描任务完成时发生异常: " + e.getMessage());
                            if (scanConfig.isDebug()) {
                                e.printStackTrace();
                            }
                        }
                    }

                }

                // 标记为扫描完成
                isScanning = false;

                // 停止性能监控并输出报告
                PerformanceMonitor monitor = PerformanceMonitor.getInstance();
                monitor.stopMonitoring();
                
                logSafely("扫描完成，共完成 " + completedTasks.get() + "/" + totalTasks.get() + " 个任务");
                
                // 输出性能报告和优化建议
                if (scanConfig.isDebug()) {
                   logSafely(monitor.getPerformanceReport());

                   // 生成配置优化建议
                   // 使用已存在的optimizer实例
                   PerformanceMonitor.PerformanceMetrics metrics = monitor.getCurrentMetrics();
                   ConfigOptimizer.OptimizationSuggestion suggestion = optimizer.optimizeConfig(metrics, scanConfig);

                   logSafely("\n=== 配置优化建议 ===");
            logSafely(suggestion.toString());

                   // 输出连接池状态
                   ConnectionPoolManager.ConnectionPoolStatus poolStatus = ConnectionPoolManager.getInstance().getStatus();
                   logSafely("\n=== 连接池状态 ===");
            logSafely(poolStatus.toString());
                }

                // 通知扫描完成
                if (callback != null) {
                    callback.onComplete(scanResults);
                }

            } catch (Exception e) {
                logErrorSafely("监控任务执行异常: " + e.getMessage());
                if (scanConfig.isDebug()) {
                    e.printStackTrace();
                }
                isScanning = false;
            }
        });
    }

    /**
     * 创建任务列表
     * @param targetPocMap 目标和POC的映射
     * @return 任务列表
     */
    private List<ScanTask> createOptimizedTasks(Map<String, List<PocObj.Poc>> targetPocMap) {
        List<ScanTask> allTasks = new ArrayList<>();

        // 创建所有扫描任务
        for (Map.Entry<String, List<PocObj.Poc>> entry : targetPocMap.entrySet()) {
            String target = entry.getKey();
            for (PocObj.Poc poc : entry.getValue()) {
                ScanTask task = new ScanTask(target, poc);
                allTasks.add(task);
            }
        }
        
        return allTasks;
    }

    /**
     * 创建任务批次（重载方法）
     * @param allTasks 所有任务列表
     * @param batchSize 批次大小
     * @return 任务批次列表
     */
    private List<List<ScanTask>> createBatches(List<ScanTask> allTasks, int batchSize) {
        List<List<ScanTask>> batches = new ArrayList<>();
        for (int i = 0; i < allTasks.size(); i += batchSize) {
            batches.add(allTasks.subList(i, Math.min(i + batchSize, allTasks.size())));
        }
        return batches;
    }

    /**
     * 创建任务批次（原方法保持兼容性）
     * @param targets 目标列表
     * @param pocs POC列表
     * @param batchSize 批次大小
     * @return 任务批次列表
     */
    private List<List<ScanTask>> createBatches(List<String> targets, List<PocObj.Poc> pocs, int batchSize) {
        List<ScanTask> allTasks = new ArrayList<>();

        // 创建所有任务
        for (String target : targets) {
            for (PocObj.Poc poc : pocs) {
                allTasks.add(new ScanTask(target, poc));
            }
        }

        return createBatches(allTasks, batchSize);
    }

    /**
     * 停止扫描
     */
    public void stopScan() {
        if (!isScanning) {
            return;
        }

        // 先标记为非扫描状态，防止新任务提交
        isScanning = false;
        logSafely("正在停止扫描任务...");
        
        // 关闭所有线程池
        shutdownExecutors();
        
        
        // 关闭连接池管理器
        ConnectionPoolManager.getInstance().shutdown();
        
        // 重置计数器
        completedTasks.set(0);
        totalTasks.set(0);
        
        // 停止性能监控
        PerformanceMonitor.getInstance().stopMonitoring();
        
        // 重新初始化线程池
        initExecutor();
        
        logSafely("扫描任务已停止");
    }

    /**
     * 检查是否正在扫描
     * @return 是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * 获取扫描结果
     * @return 扫描结果列表
     */
    public List<ScanResult> getScanResults() {
        return scanResults;
    }

    /**
     * 扫描单个目标
     * @param target 目标URL
     * @param poc POC对象
     * @return 扫描结果
     */
    private ScanResult scanTarget(String target, PocObj.Poc poc) {
        long startTime = System.currentTimeMillis();
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        ConnectionPoolManager connectionPool = ConnectionPoolManager.getInstance();

        
        // 尝试获取连接
        if (!connectionPool.acquireConnection(target)) {
            if (scanConfig.isDebug()) {
                logErrorSafely("无法获取到 " + target + " 的连接，跳过扫描");
            }
            return createErrorResult(target, poc, new Exception("连接池已满，无法获取连接"), startTime);
        }
        
        if (scanConfig.isDebug()) {
            logSafely("正在扫描目标: " + target + " 使用POC: " + poc.getName());
        }

        ScanResult result = null;
        boolean successful = false;
        try {
            // 执行验证步骤
            if (poc.getVerifySteps() != null && !poc.getVerifySteps().isEmpty()) {
                boolean isVulnerable = executeSteps(target, poc);
                successful = true;

                if (isVulnerable) {
                    // 创建扫描结果
                    result = new ScanResult();
                    result.setTarget(target);
                    result.setPoc(poc);
                    result.setVulnerable(true);
                    result.setTimestamp(System.currentTimeMillis());

                    // 添加详细信息
                    result.addDetail("protocol", poc.getProtocol());
                    result.addDetail("severity", poc.getSeverity());
                    result.addDetail("scan_duration", System.currentTimeMillis() - startTime);
                    result.addDetail("description", poc.getDescription());
                    result.addDetail("references", poc.getReferences());

                    logSafely("[发现漏洞] 目标: " + target + " POC: " + poc.getName());
                    
                    return result;
                } else {
                    // 创建未发现漏洞的结果
                    result = createNegativeResult(target, poc, startTime);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (scanConfig.isDebug()) {
                logErrorSafely("扫描被中断: " + target + " - " + poc.getName());
            }
            result = createErrorResult(target, poc, e, startTime);
        } catch (Exception e) {
            // 增强异常处理，包含目标和POC信息
            String errorMsg = "扫描异常 [目标: " + target + ", POC: " + poc.getName() + "]: " + e.getMessage();
            
            // 判断是否为网络相关异常
            boolean isNetworkError = e.getMessage() != null && 
                (e.getMessage().contains("请求失败，超出重试次数") ||
                 e.getMessage().contains("Connection timed out") ||
                 e.getMessage().contains("Read timed out") ||
                 e.getMessage().contains("SocketTimeoutException") ||
                 e.getMessage().contains("ConnectException") ||
                 e.getMessage().contains("UnknownHostException"));
            
            if (isNetworkError) {
                // 网络错误，简化日志输出
                if (scanConfig.isDebug()) {
                    logErrorSafely("网络请求失败: " + target + " - " + poc.getName() + ": " + e.getMessage());
                }
            } else {
                // 其他异常，详细记录
                if (scanConfig.isDebug()) {
                    logErrorSafely(errorMsg);
                    synchronized (System.err) {
                        e.printStackTrace();
                    }
                } else {
                    logErrorSafely(errorMsg);
                }
            }
            // 创建错误结果
            result = createErrorResult(target, poc, e, startTime);
        } finally {
            // 释放连接
            connectionPool.releaseConnection(target);
            
            // 记录性能指标
            long duration = System.currentTimeMillis() - startTime;
            monitor.recordScan(duration, successful);
            monitor.recordPocUsage(poc.getId(), duration);
        }
        

        
        return result != null ? result : createErrorResult(target, poc, 
            new Exception("未知错误：扫描结果为null"), startTime);
    }
    

    
    /**
     * 创建负面结果（未发现漏洞）
     */
    private ScanResult createNegativeResult(String target, PocObj.Poc poc, long startTime) {
        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setPoc(poc);
        result.setVulnerable(false);
        result.setTimestamp(System.currentTimeMillis());
        result.addDetail("scan_duration", System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 创建错误结果
     */
    private ScanResult createErrorResult(String target, PocObj.Poc poc, Exception e, long startTime) {
        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setPoc(poc);
        result.setVulnerable(false);
        result.setTimestamp(System.currentTimeMillis());
        result.addDetail("scan_duration", System.currentTimeMillis() - startTime);
        result.addDetail("error", e.getMessage());
        result.addDetail("error_type", e.getClass().getSimpleName());
        return result;
    }

    /**
     * 执行POC检测步骤
     * @param target 目标URL
     * @param poc POC对象
     * @return 是否存在漏洞
     * @throws Exception 执行异常
     */
    private boolean executeSteps(String target, PocObj.Poc poc) throws Exception {
        if (target == null || poc == null || poc.getVerifySteps() == null || poc.getVerifySteps().isEmpty()) {
            if (scanConfig.isDebug()) {
                logErrorSafely("无效的POC或目标URL");
            }
            return false;
        }

        List<PocObj.PocStep> steps = poc.getVerifySteps();
        PocObj.GlobalConfig globalConfig = poc.getGlobalConfig();

        // 从POC对象中获取变量，而不是创建新的HashMap
        Map<String, List<String>> pocVariables = poc.getVariables();
        Map<String, String> extractedValues = new HashMap<>();

        // 预处理POC变量 - 将多值变量列表转换为单个字符串
        if (pocVariables != null && !pocVariables.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    // 默认使用第一个值
                    extractedValues.put(entry.getKey(), entry.getValue().get(0));
                }
            }
        }

        for (PocObj.PocStep step : steps) {
            if (step == null) {
                continue; // 跳过空步骤
            }

            try {
                // 创建请求对象
                RequestObj requestObj = new RequestObj();

                // 检查是否使用raw请求格式
                if (step.getRaw() != null && !step.getRaw().isEmpty()) {
                    // 处理raw格式的HTTP请求
                    HttpHandler.processRawRequest(requestObj, step.getRaw(), target, extractedValues);
                } else {
                    // 替换变量
                    String path = HttpHandler.replaceVariables(step.getPath(), extractedValues);
                    String body = HttpHandler.replaceVariables(step.getBody(), extractedValues);

                    // 构建完整URL
                    String url = target;
                    if (path != null && !path.isEmpty()) {
                        if (!path.startsWith("/")) {
                            path = "/" + path;
                        }
                        if (target.endsWith("/")) {
                            target = target.substring(0, target.length() - 1);
                        }
                        url = target + path;
                    }

                    requestObj.setUrl(url);

                    // 设置请求方法
                    if (step.getMethod() != null && !step.getMethod().isEmpty()) {
                        requestObj.setMethod(step.getMethod());
                    }

                    // 设置请求头
                    if (step.getHeaders() != null && !step.getHeaders().isEmpty()) {
                        Map<String, String> headers = new HashMap<>();
                        for (Map.Entry<String, String> entry : step.getHeaders().entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                headers.put(entry.getKey(), HttpHandler.replaceVariables(entry.getValue(), extractedValues));
                            }
                        }
                        requestObj.setHeaders(headers);
                    }

                    // 设置请求体
                    if (body != null && !body.isEmpty()) {
                        requestObj.setPostData(body);
                    }
                }

                // 设置全局请求头
                if (globalConfig != null && globalConfig.getGlobalHeaders() != null && !globalConfig.getGlobalHeaders().isEmpty()) {
                    Map<String, String> headers = new HashMap<>();
                    for (Map.Entry<String, String> entry : globalConfig.getGlobalHeaders().entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            headers.put(entry.getKey(), entry.getValue());
                        }
                    }

                    if (requestObj.getHeaders() != null) {
                        headers.putAll(requestObj.getHeaders());
                    }
                    requestObj.setHeaders(headers);
                }

                // 设置超时时间
                if (step.getTimeout() > 0) {
                    requestObj.setTimeOut(step.getTimeout());
                } else if (globalConfig != null && globalConfig.getRetryInterval() > 0) {
                    requestObj.setTimeOut(globalConfig.getRetryInterval() / 1000);
                } else if (scanConfig != null && scanConfig.getTimeout() > 0) {
            // 使用全局扫描配置的超时时间
            requestObj.setTimeOut(scanConfig.getTimeout());
                }

                // 设置重试次数
                if (step.getRetries() > 0) {
                    requestObj.setRetries(step.getRetries());
                } else if (globalConfig != null && globalConfig.getMaxRetries() > 0) {
                    requestObj.setRetries(globalConfig.getMaxRetries());
                }

                // 设置是否跟随重定向
                requestObj.setFollowRedirects(step.isFollowRedirect());

                // 设置代理
                if (step.getProxy() != null && !step.getProxy().isEmpty()) {
                    requestObj.setProxies(step.getProxy());
                } else if (globalConfig != null && globalConfig.getProxy() != null && !globalConfig.getProxy().isEmpty()) {
                    requestObj.setProxies(globalConfig.getProxy());
                } else if (scanConfig != null && scanConfig.getProxy() != null && !scanConfig.getProxy().isEmpty()) {
                    // 使用全局扫描配置的代理
                    requestObj.setProxies(scanConfig.getProxy());
                }

                // 发送请求
                int maxResponseSize = (scanConfig != null && scanConfig.getMaxResponseSize() > 0) 
                    ? scanConfig.getMaxResponseSize() : Integer.MAX_VALUE; // 默认无大小限制
                requestObj.setMaxResponseSize(maxResponseSize);

                try (CustomHttpResponse response = requests(requestObj)){
                    // 检查响应是否为空
                    if (response == null) {
                        if (scanConfig.isDebug()) {
                            logErrorSafely("请求返回空响应");
                        }
                        continue; // 跳过当前步骤，继续下一步
                    }

                    // 匹配结果
                    boolean matched = ResponseMatcher.matchResponse(response, step.getMatchers(), step.getMatchersCondition());

                    // 如果匹配失败且步骤是必要的，则返回失败
                    if (!matched) {
                        return false;
                    }

                    // 提取变量
                    if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                        VariableExtractor.extractVariables(response, step.getExtractors(), extractedValues);
                    }
                }
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    logErrorSafely("执行POC步骤时发生异常: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // 区分不同类型的异常进行处理
                if (e.getMessage() != null && 
                    (e.getMessage().contains("资源未找到") ||
                     e.getMessage().contains("请求失败，超出重试次数") ||
                     e.getMessage().contains("Connection timed out") ||
                     e.getMessage().contains("Read timed out") ||
                     e.getMessage().contains("SocketTimeoutException") ||
                     e.getMessage().contains("ConnectException") ||
                     e.getMessage().contains("UnknownHostException"))) {
                    // 对于网络相关的预期错误，记录日志但继续执行
                    if (scanConfig.isDebug()) {
                        logErrorSafely("网络请求失败，跳过当前步骤继续执行: " + e.getMessage());
                    }
                    return false; // 返回false表示匹配失败，但不中断扫描
                } else {
                    // 对于其他严重异常（如配置错误、代码错误等），重新抛出
                    if (scanConfig.isDebug()) {
                        logErrorSafely("严重异常，停止当前POC执行: " + e.getMessage());
                        e.printStackTrace();
                    }
                    throw e;
                }
            }
        }

        // 所有步骤执行完毕，返回成功
        return true;
    }

    /**
     * 线程安全的日志输出方法
     * 防止多线程并发输出时出现字符串混乱
     */
    private synchronized void logSafely(String message) {
        System.out.println(message);
    }

    /**
     * 线程安全的错误日志输出方法
     * 防止多线程并发输出时出现字符串混乱
     */
    private synchronized void logErrorSafely(String message) {
        System.err.println(message);
    }
}