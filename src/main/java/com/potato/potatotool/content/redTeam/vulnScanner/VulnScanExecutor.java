package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.ResponseMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanTask;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocManager;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

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
        // 如果已存在线程池且未关闭，则先关闭
        if (this.scanExecutor != null && !this.scanExecutor.isShutdown()) {
            try {
                this.scanExecutor.shutdown();
                // 等待线程池终止，最多等待3秒
                if (!this.scanExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    this.scanExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.scanExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 获取线程数，确保至少有1个线程
        int threadCount = Math.max(1, scanConfig != null ? scanConfig.getThreads() : 10);

        // 创建新的线程池，使用有界队列防止内存溢出
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadCount, // 核心线程数
                threadCount, // 最大线程数
                60L, TimeUnit.SECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(10000), // 有界队列
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "scan-thread-" + threadNumber.getAndIncrement());
                        if (t.isDaemon()) {
                            t.setDaemon(false); // 确保非守护线程
                        }
                        if (t.getPriority() != Thread.NORM_PRIORITY) {
                            t.setPriority(Thread.NORM_PRIORITY);
                        }
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用者所在线程执行
        );

        // 允许核心线程超时，提高资源利用率
        executor.allowCoreThreadTimeOut(true);

        this.scanExecutor = executor;
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
            System.err.println("加载POC失败: " + e.getMessage());
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
                System.err.println("读取目标文件失败: " + e.getMessage());
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
    public void startScan(ScanCallback callback) {
        if (isScanning) {
            System.err.println("扫描任务正在进行中，请等待当前扫描完成");
            return;
        }

        if (pocList.isEmpty()) {
            System.err.println("未加载任何POC，请先加载POC");
            return;
        }

        if (targetList.isEmpty()) {
            System.err.println("未设置任何目标，请先设置目标");
            return;
        }

        // 确保线程池已初始化
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            initExecutor();
        }

        // 标记为正在扫描
        isScanning = true;

        // 清空之前的扫描结果
        scanResults.clear();

        // 过滤POC列表
        List<PocObj.Poc> filteredPocList = new ArrayList<>();
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

            filteredPocList.add(poc);
        }

        // 计算总任务数
        final int totalTasks = targetList.size() * filteredPocList.size();
        final int batchSize = Math.min(1000, totalTasks); // 每批最多1000个任务
        final AtomicInteger completedTasks = new AtomicInteger(0);

        // 创建一个监控任务，管理批量执行和完成通知
        scanExecutor.submit(() -> {
            try {
                // 分批创建和执行任务
                List<List<ScanTask>> batches = createBatches(targetList, filteredPocList, batchSize);

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

                        Future<?> task = scanExecutor.submit(() -> {
                            try {
                                ScanResult result = scanTarget(scanTask.getTarget(), scanTask.getPoc());
                                if (result != null) {
                                    scanResults.add(result);
                                    if (callback != null) {
                                        try {
                                            callback.onResult(result);
                                        } catch (Exception e) {
                                            System.err.println("回调执行异常: " + e.getMessage());
                                            if (scanConfig.isDebug()) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("扫描任务执行异常: " + e.getMessage());
                                if (scanConfig.isDebug()) {
                                    e.printStackTrace();
                                }
                            } finally {
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
                            int timeoutSeconds = scanConfig.getTimeOut() > 0 ? scanConfig.getTimeOut() * 2 : 60;
                            task.get(timeoutSeconds, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            // 任务超时，取消任务
                            task.cancel(true);
                            System.err.println("扫描任务超时已取消");
                        } catch (InterruptedException e) {
                            // 线程被中断
                            Thread.currentThread().interrupt();
                            System.err.println("扫描任务被中断");
                            break;
                        } catch (ExecutionException e) {
                            System.err.println("等待扫描任务完成时发生异常: " + e.getMessage());
                            if (scanConfig.isDebug()) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 在批次之间暂停一小段时间，避免资源过度消耗
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 标记为扫描完成
                isScanning = false;

                // 通知扫描完成
                if (callback != null) {
                    callback.onComplete(scanResults);
                }

                System.out.println("扫描完成，共完成 " + completedTasks.get() + "/" + totalTasks + " 个任务");

            } catch (Exception e) {
                System.err.println("监控任务执行异常: " + e.getMessage());
                if (scanConfig.isDebug()) {
                    e.printStackTrace();
                }
                isScanning = false;
            }
        });
    }

    /**
     * 创建任务批次
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

        // 分批
        List<List<ScanTask>> batches = new ArrayList<>();
        for (int i = 0; i < allTasks.size(); i += batchSize) {
            batches.add(allTasks.subList(i, Math.min(i + batchSize, allTasks.size())));
        }

        return batches;
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
        System.out.println("正在停止扫描任务...");
        
        // 尝试优雅关闭线程池
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            List<Runnable> pendingTasks = scanExecutor.shutdownNow();
            
            if (scanConfig.isDebug()) {
                System.out.println("停止扫描，取消 " + pendingTasks.size() + " 个待执行任务");
            }
            
            try {
                // 等待所有任务完成
                if (!scanExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    // 如果超时，则强制关闭
                    List<Runnable> droppedTasks = scanExecutor.shutdownNow();
                    System.out.println("强制停止了 " + droppedTasks.size() + " 个未完成的任务");

                    // 再次等待，确保所有任务都被终止
                    if (!scanExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.err.println("线程池无法完全关闭");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 重新初始化线程池
                initExecutor();
            }
        }
        System.out.println("扫描任务已停止");
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
        if (scanConfig.isDebug()) {
            System.out.println("正在扫描目标: " + target + " 使用POC: " + poc.getName());
        }

        try {
            // 执行验证步骤
            if (poc.getVerifySteps() != null && !poc.getVerifySteps().isEmpty()) {
                boolean isVulnerable = executeSteps(target, poc);

                if (isVulnerable) {
                    // 创建扫描结果
                    ScanResult result = new ScanResult();
                    result.setTarget(target);
                    result.setPoc(poc);
                    result.setVulnerable(true);
                    result.setTimestamp(System.currentTimeMillis());

                    // 添加详细信息
                    result.addDetail("protocol", poc.getProtocol());
                    result.addDetail("severity", poc.getSeverity());
                    result.addDetail("description", poc.getDescription());
                    result.addDetail("references", poc.getReferences());

                    System.out.println("[发现漏洞] 目标: " + target + " POC: " + poc.getName());
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("扫描目标时发生异常: " + target + " POC: " + poc.getName() + " 错误: " + e.getMessage());
            if (scanConfig.isDebug()) {
                e.printStackTrace();
            }
        }

        return null;
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
                System.err.println("无效的POC或目标URL");
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

            CustomHttpResponse response = null;
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
                } else if (scanConfig != null && scanConfig.getTimeOut() > 0) {
                    // 使用全局扫描配置的超时时间
                    requestObj.setTimeOut(scanConfig.getTimeOut());
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
                response = RequestUtils.requests(requestObj);

                // 检查响应是否为空
                if (response == null) {
                    if (scanConfig.isDebug()) {
                        System.err.println("请求返回空响应");
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
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    System.err.println("执行POC步骤时发生异常: " + e.getMessage());
                    e.printStackTrace();
                }
                throw e; // 重新抛出异常，让调用者处理
            } finally {
                // 释放资源
                if (response != null) {
                    try {
                        response.disconnect();
                    } catch (Exception e) {
                        // 忽略关闭连接时的异常
                        if (scanConfig.isDebug()) {
                            System.err.println("关闭连接时发生异常: " + e.getMessage());
                        }
                    }
                }
            }
        }

        // 所有步骤执行完毕，返回成功
        return true;
    }
}