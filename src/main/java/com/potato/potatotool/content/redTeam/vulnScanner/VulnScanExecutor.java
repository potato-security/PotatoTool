package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocManager;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.requestUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 漏洞扫描执行器，用于执行POC扫描任务
 * @author Potato
 * @date 2025/3/21 15:00
 */
public class VulnScanExecutor {
    // 扫描线程池
    private ExecutorService scanExecutor;
    // 扫描结果
    private final List<ScanResult> scanResults = Collections.synchronizedList(new ArrayList<>());
    // POC列表
    private List<PocObj.Poc> pocList = new ArrayList<>();
    // 目标列表
    private final List<String> targetList = new ArrayList<>();
    // 扫描配置
    private ScanConfig scanConfig;
    // 是否正在扫描
    private boolean isScanning = false;

    /**
     * 默认构造函数
     */
    public VulnScanExecutor() {
        this.scanConfig = new ScanConfig();
        initExecutor();
    }

    /**
     * 构造函数
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
     * 加载POC
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
     * @param targets 目标字符串，可以是单个URL、多个URL（以逗号分隔）或包含URL的文件路径
     * @return 解析的目标数量
     */
    public int parseTargets(String targets) {
        // 清空之前的目标列表
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
     * 添加单个目标
     * @param target 目标URL
     */
    private void addTarget(String target) {
        if (target.isEmpty()) {
            return;
        }

        // 如果目标不包含协议，则添加http://前缀
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = "http://" + target;
        }

        // 验证URL格式
        try {
            new URL(target);
            targetList.add(target);
        } catch (MalformedURLException e) {
            System.err.println("无效的URL: " + target);
        }
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
        // 重新初始化线程池
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
        return this.scanConfig;
    }

    /**
     * 开始扫描
     * @param callback 扫描结果回调
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
                                ScanResult result = scanTarget(scanTask.target, scanTask.poc);
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
     * 扫描任务类
     */
    private static class ScanTask {
        private final String target;
        private final PocObj.Poc poc;

        public ScanTask(String target, PocObj.Poc poc) {
            this.target = target;
            this.poc = poc;
        }

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

        // 关闭线程池
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            try {
                // 先尝试优雅关闭
                scanExecutor.shutdown();

                // 等待任务完成，最多等待5秒
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
                // 如果当前线程被中断，则强制关闭线程池
                scanExecutor.shutdownNow();
                // 保持中断状态
                Thread.currentThread().interrupt();
            } finally {
                // 重新初始化线程池
                initExecutor();
            }
        }

        System.out.println("扫描任务已停止");
    }

    /**
     * 是否正在扫描
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
                boolean isVulnerable = executeSteps(target, poc.getVerifySteps(), poc.getGlobalConfig());

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
     * 执行POC步骤
     * @param target 目标URL
     * @param steps POC步骤列表
     * @param globalConfig 全局配置
     * @return 是否匹配成功
     */
    private boolean executeSteps(String target, List<PocObj.PocStep> steps, PocObj.GlobalConfig globalConfig) throws Exception {
        if (target == null || steps == null || steps.isEmpty()) {
            if (scanConfig.isDebug()) {
                System.err.println("无效的POC步骤或目标URL");
            }
            return false;
        }

        Map<String, String> extractedValues = new HashMap<>();

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
                    processRawRequest(requestObj, step.getRaw(), target, extractedValues);
                } else {
                    // 替换变量
                    String path = replaceVariables(step.getPath(), extractedValues);
                    String body = replaceVariables(step.getBody(), extractedValues);

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
                                headers.put(entry.getKey(), replaceVariables(entry.getValue(), extractedValues));
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
                }

                // 发送请求
                response = requestUtils.requests(requestObj);

                // 检查响应是否为空
                if (response == null) {
                    if (scanConfig.isDebug()) {
                        System.err.println("请求返回空响应");
                    }
                    continue; // 跳过当前步骤，继续下一步
                }

                // 匹配结果
                boolean matched = matchResponse(response, step.getMatchers(), step.getMatchersCondition());

                // 如果匹配失败且步骤是必要的，则返回失败
                if (!matched) {
                    return false;
                }

                // 提取变量 - 使用传统的提取器Map（向后兼容）
                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
//                    extractVariables(response, step.getExtractors(), extractedValues);
                }
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    System.err.println("执行POC步骤时发生异常: " + e.getMessage());
                    e.printStackTrace();
                }
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

    /**
     * 处理raw格式的HTTP请求
     * @param requestObj 请求对象
     * @param rawLines raw请求行列表
     * @param target 目标URL
     * @param variables 变量映射
     */
    private void processRawRequest(RequestObj requestObj, List<String> rawLines, String target, Map<String, String> variables) {
        if (rawLines == null || rawLines.isEmpty()) {
            return;
        }

        // 处理特殊变量，如@timeout等
        int startIndex = 0;
        // 检查所有以@开头的行，可能有多行特殊变量
        while (startIndex < rawLines.size() && rawLines.get(startIndex).trim().startsWith("@")) {
            String specialLine = rawLines.get(startIndex).trim();
            processSpecialVariable(requestObj, specialLine);
            startIndex++; // 跳过这一行
        }

        // 将所有行合并为一个字符串，并替换变量
        StringBuilder rawBuilder = new StringBuilder();
        for (int i = startIndex; i < rawLines.size(); i++) {
            rawBuilder.append(replaceVariables(rawLines.get(i), variables)).append("\n");
        }
        String rawRequest = rawBuilder.toString();

        // 分割请求行、请求头和请求体
        String[] parts = rawRequest.split("\n\n", 2);
        String headerPart = parts[0];
        String bodyPart = parts.length > 1 ? parts[1] : "";

        // 解析请求行和请求头
        String[] headerLines = headerPart.split("\n");
        if (headerLines.length > 0) {
            // 解析请求行（第一行）：METHOD /path HTTP/1.1
            String requestLine = headerLines[0].trim();
            String[] requestLineParts = requestLine.split(" ", 3);
            if (requestLineParts.length >= 2) {
                // 设置请求方法
                String method = requestLineParts[0].toUpperCase();
                requestObj.setMethod(method);

                // 解析路径
                String path = requestLineParts[1];

                // 构建完整URL
                String url = target;
                if (path != null && !path.isEmpty()) {
                    // 如果路径是完整URL，则直接使用
                    if (path.startsWith("http://") || path.startsWith("https://")) {
                        url = path;
                    } else {
                        // 否则拼接目标URL和路径
                        if (!path.startsWith("/")) {
                            path = "/" + path;
                        }
                        if (target.endsWith("/")) {
                            target = target.substring(0, target.length() - 1);
                        }
                        url = target + path;
                    }
                }
                requestObj.setUrl(url);
            }

            // 解析请求头（从第二行开始）
            Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < headerLines.length; i++) {
                String headerLine = headerLines[i].trim();
                if (headerLine.isEmpty()) {
                    continue;
                }

                int colonIndex = headerLine.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = headerLine.substring(0, colonIndex).trim();
                    String headerValue = headerLine.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }

            // 设置请求头
            if (!headers.isEmpty()) {
                requestObj.setHeaders(headers);
            }
        }

        // 设置请求体
        if (!bodyPart.isEmpty()) {
            requestObj.setPostData(bodyPart);
        }

        // 根据Content-Type设置postMethod
        Map<String, String> headers = requestObj.getHeaders();
        if (headers != null) {
            String contentType = headers.get("Content-Type");
            if (contentType != null) {
                if (contentType.contains("application/json")) {
                    requestObj.setPostMethod("JSON");
                } else if (contentType.contains("multipart/form-data")) {
                    requestObj.setPostMethod("FORM");
                } else {
                    requestObj.setPostMethod("RAW");
                }
            }
        }
    }

    /**
     * 替换变量
     * @param input 输入字符串
     * @param variables 变量映射
     * @return 替换后的字符串
     */
    private String replaceVariables(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty() || variables == null || variables.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return result;
    }

    /**
     * 匹配响应
     * @param response HTTP响应
     * @param matchers 匹配器列表
     * @param condition 匹配条件
     * @return 是否匹配成功
     */
    private boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers, PocObj.MatchersCondition condition) {
        // 如果响应为空，则匹配失败
        if (response == null) {
            if (scanConfig.isDebug()) {
                System.err.println("匹配响应失败: 响应对象为空");
            }
            return false;
        }

        // 如果没有匹配器，则认为匹配成功（因为没有任何条件需要满足）
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }

        try {
            boolean result = (condition == PocObj.MatchersCondition.AND);

            for (PocObj.Matcher matcher : matchers) {
                // 检查匹配器是否为空
                if (matcher == null) {
                    if (scanConfig.isDebug()) {
                        System.err.println("跳过空匹配器");
                    }
                    continue;
                }

                boolean matchResult = matchSingleMatcher(response, matcher);
                System.out.println("匹配结果: " + matchResult); // 打印匹配结果

                // 如果是反向匹配，则取反结果
                if (matcher.isNegative()) {
                    matchResult = !matchResult;
                }

                if (condition == PocObj.MatchersCondition.AND) {
                    result = result && matchResult;
                    if (!result) {
                        break; // 短路AND
                    }
                } else { // OR
                    result = result || matchResult;
                    if (result) {
                        break; // 短路OR
                    }
                }
            }

            return result;
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("匹配响应失败: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 匹配单个匹配器
     * @param response HTTP响应
     * @param matcher 匹配器
     * @return 是否匹配成功
     */
    private boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher) {
        if (response == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }

        String part = matcher.getPart();
        if (part == null || part.isEmpty()) {
            part = "body"; // 默认匹配响应体
        }

        String content = getResponsePart(response, part);
        if (content == null) {
            return false;
        }

        try {
            // 根据匹配器类型进行匹配
            switch (matcher.getType()) {
                case STATUS:
                    try {
                        int responseCode = response.getResponseCode();
                        System.out.println("响应状态码: " + responseCode);
                        System.out.println("matcher.getValues(): " + matcher.getValues());
                        return matchStatus(responseCode, matcher.getValues());
                    } catch (Exception e) {
                        if (scanConfig.isDebug()) {
                            System.err.println("获取响应状态码失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                        // 尝试从异常消息中提取状态码
                        String message = e.getMessage();
                        if (message != null && message.contains("response code: ")) {
                            try {
                                String codeStr = message.substring(message.indexOf("response code: ") + 15).trim();
                                int code = Integer.parseInt(codeStr.split(" ")[0]);
                                return matchStatus(code, matcher.getValues());
                            } catch (Exception ex) {
                                // 忽略解析错误
                            }
                        }
                        return false;
                    }
                case SIZE:
                    System.out.println("内容长度: " + content.length());
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchSize(content.length(), matcher.getValues(), matcher.getOperation());
                case WORD:
                    System.out.println("内容: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchWord(content, matcher.getValues(), matcher.isCaseInsensitive());
                case REGEX:
                    System.out.println("正则: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchRegex(content, matcher.getValues(), matcher.isCaseInsensitive());
                case BINARY:
                    System.out.println("数据流: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchBinary(content, matcher.getValues());
                case HASH:
                    System.out.println("HASH: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchHash(content, matcher.getValues(), matcher.getOperation());
                case JSON:
                    System.out.println("JSON: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchJson(content, matcher.getValues());
                case TIME:
                    System.out.println("TIME: " + response.getResponseTime());
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchTime(response.getResponseTime(), matcher.getValues(), matcher.getOperation());
                case DSL:
                    System.out.println("DSL: " + content);
                    System.out.println("matcher.getValues(): " + matcher.getValues());
                    return matchDsl(response, matcher.getValues());
                default:
                    return false;
            }
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("匹配器执行失败: " + matcher.getType() + ", 错误: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 获取响应的指定部分
     * @param response HTTP响应
     * @param part 部分名称（body, header, status等）
     * @return 响应内容
     */
    private String getResponsePart(CustomHttpResponse response, String part) {
        if (response == null || part == null || part.isEmpty()) {
            return null;
        }

        try {
            switch (part.toLowerCase()) {
                case "body":
                    String bodyText = response.getTextStr();
                    return bodyText != null ? bodyText : "";
                case "header":
                case "headers":
                    StringBuilder headers = new StringBuilder();
                    Map<String, List<String>> headerFields = response.getHeaderFields();
                    if (headerFields != null) {
                        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                            if (entry.getKey() != null) {
                                headers.append(entry.getKey()).append(": ");
                            }
                            if (entry.getValue() != null) {
                                for (String value : entry.getValue()) {
                                    if (value != null) {
                                        headers.append(value).append(", ");
                                    }
                                }
                            }
                            headers.append("\n");
                        }
                    }
                    return headers.toString();
                case "status":
                    try {
                        // 确保连接对象不为空
                        if (response == null) {
                            return "0";
                        }
                        return String.valueOf(response.getResponseCode());
                    } catch (Exception e) {
                        if (scanConfig.isDebug()) {
                            System.err.println("获取响应状态码失败: " + e.getMessage());
                        }
                        return "0"; // 返回默认值
                    }
                case "all":
                    String textStr = response.getTextStr();
                    String headerStr = getResponsePart(response, "headers");
                    return (textStr != null ? textStr : "") + "\n" + (headerStr != null ? headerStr : "");
                default:
                    // 尝试获取特定的响应头
                    Map<String, List<String>> fields = response.getHeaderFields();
                    if (fields != null) {
                        List<String> headerValues = fields.get(part);
                        if (headerValues != null && !headerValues.isEmpty()) {
                            return String.join(", ", headerValues);
                        }
                    }
                    return null;
            }
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("获取响应部分失败: " + part + ", 错误: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 匹配状态码
     * @param statusCode 响应状态码
     * @param values 匹配值列表
     * @return 是否匹配成功
     */
    private boolean matchStatus(int statusCode, List<String> values) {
        for (String value : values) {
            try {
                if (Integer.parseInt(value) == statusCode) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 匹配大小
     * @param size 内容大小
     * @param values 匹配值列表
     * @param operation 操作类型
     * @return 是否匹配成功
     */
    private boolean matchSize(int size, List<String> values, PocObj.OperationType operation) {
        for (String value : values) {
            try {
                int targetSize = Integer.parseInt(value);

                if (operation == null || operation == PocObj.OperationType.DEFAULT || operation == PocObj.OperationType.EQUAL) {
                    if (size == targetSize) {
                        return true;
                    }
                } else {
                    switch (operation) {
                        case NOT_EQUAL:
                            if (size != targetSize) {
                                return true;
                            }
                            break;
                        case GREATER:
                            if (size > targetSize) {
                                return true;
                            }
                            break;
                        case LESS:
                            if (size < targetSize) {
                                return true;
                            }
                            break;
                        case GREATER_EQUAL:
                            if (size >= targetSize) {
                                return true;
                            }
                            break;
                        case LESS_EQUAL:
                            if (size <= targetSize) {
                                return true;
                            }
                            break;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 匹配关键词
     * @param content 内容
     * @param values 匹配值列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private boolean matchWord(String content, List<String> values, boolean caseInsensitive) {
        if (caseInsensitive) {
            content = content.toLowerCase();
        }

        for (String value : values) {
            String matchValue = caseInsensitive ? value.toLowerCase() : value;
            if (content.contains(matchValue)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配正则表达式
     * @param content 内容
     * @param values 匹配值列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private boolean matchRegex(String content, List<String> values, boolean caseInsensitive) {
        for (String regex : values) {
            try {
                Pattern pattern = caseInsensitive ?
                        Pattern.compile(regex, Pattern.CASE_INSENSITIVE) :
                        Pattern.compile(regex);

                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * 匹配二进制内容
     * @param content 内容
     * @param values 匹配值列表
     * @return 是否匹配成功
     */
    private boolean matchBinary(String content, List<String> values) {
        // 简单实现，仅支持十六进制字符串匹配
        for (String value : values) {
            try {
                byte[] bytes = hexStringToByteArray(value);
                String hexContent = bytesToHexString(content.getBytes());

                if (hexContent.contains(value.toLowerCase())) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * 匹配哈希值
     * @param content 内容
     * @param values 匹配值列表
     * @param operation 操作类型
     * @return 是否匹配成功
     */
    private boolean matchHash(String content, List<String> values, PocObj.OperationType operation) {
        // 简单实现，仅支持MD5和SHA1
        try {
            String md5Hash = calculateMD5(content);
            String sha1Hash = calculateSHA1(content);

            for (String value : values) {
                if (value.equalsIgnoreCase(md5Hash) || value.equalsIgnoreCase(sha1Hash)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * 匹配JSON内容
     * @param content 内容
     * @param values 匹配值列表
     * @return 是否匹配成功
     */
    private boolean matchJson(String content, List<String> values) {
        // 简单实现，仅支持JSON字符串匹配
        try {
            for (String value : values) {
                if (content.contains(value)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * 匹配响应时间
     * @param responseTime 响应时间（毫秒）
     * @param values 匹配值列表
     * @param operation 操作类型
     * @return 是否匹配成功
     */
    private boolean matchTime(long responseTime, List<String> values, PocObj.OperationType operation) {
        for (String value : values) {
            try {
                long targetTime = Long.parseLong(value);

                if (operation == null || operation == PocObj.OperationType.DEFAULT || operation == PocObj.OperationType.EQUAL) {
                    if (responseTime == targetTime) {
                        return true;
                    }
                } else {
                    switch (operation) {
                        case NOT_EQUAL:
                            if (responseTime != targetTime) {
                                return true;
                            }
                            break;
                        case GREATER:
                            if (responseTime > targetTime) {
                                return true;
                            }
                            break;
                        case LESS:
                            if (responseTime < targetTime) {
                                return true;
                            }
                            break;
                        case GREATER_EQUAL:
                            if (responseTime >= targetTime) {
                                return true;
                            }
                            break;
                        case LESS_EQUAL:
                            if (responseTime <= targetTime) {
                                return true;
                            }
                            break;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 将十六进制字符串转换为字节数组
     * @param hexString 十六进制字符串
     * @return 字节数组
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 计算MD5哈希值
     * @param content 内容
     * @return MD5哈希值
     */
    private String calculateMD5(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(content.getBytes());
            return bytesToHexString(array);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 计算SHA1哈希值
     * @param content 内容
     * @return SHA1哈希值
     */
    private String calculateSHA1(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] array = md.digest(content.getBytes());
            return bytesToHexString(array);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 匹配DSL表达式
     * @param response HTTP响应
     * @param values DSL表达式列表
     * @return 是否匹配成功
     */
    private boolean matchDsl(CustomHttpResponse response, List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        
        try {
            // 创建统一的DSL上下文环境
            Map<String, Object> context = createDslContext(response);
            
            // 遍历所有DSL表达式，任一匹配则返回true
            for (String expr : values) {
                try {
                    if (evaluateUnifiedDsl(context, expr)) {
                        return true;
                    }
                } catch (Exception e) {
                    if (scanConfig.isDebug()) {
                        System.err.println("DSL表达式解析失败: " + expr + ", 错误: " + e.getMessage());
                    }
                }
            }
            return false;
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("DSL匹配失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 评估统一的DSL表达式
     * @param context DSL上下文
     * @param expression DSL表达式
     * @return 表达式评估结果
     */
    private boolean evaluateUnifiedDsl(Map<String, Object> context, String expression) {
        // 预处理表达式，标准化语法差异
        String normalizedExpr = normalizeExpression(expression);
        
        // 解析并评估表达式
        return evaluateDslExpression(context, normalizedExpr);
    }
    
    /**
     * 标准化DSL表达式
     * @param expression 原始表达式
     * @return 标准化后的表达式
     */
    private String normalizeExpression(String expression) {
        String originalExpression = expression.trim();
        String normalized = originalExpression;
        
        // 替换Xray特有的语法为统一格式
        // 替换bcontains为contains
        normalized = normalized.replaceAll("bcontains\\s*\\(", "contains(");
        
        // 替换b"字符串"或b'字符串'为普通字符串
        normalized = normalized.replaceAll("b([\"'])", "$1");
        
        // 替换icontains为ignoreCase(contains( 形式
        normalized = normalized.replaceAll("icontains\\s*\\(", "ignoreCase(contains(");
        
        // 替换ibcontains为ignoreCase(contains( 形式 - Xray特有
        normalized = normalized.replaceAll("ibcontains\\s*\\(", "ignoreCase(contains(");
        
        // 替换bmatches为matches - Xray特有
        normalized = normalized.replaceAll("bmatches\\s*\\(", "matches(");
        
        // 替换Nuclei特有的语法
        // to_lower()替换为toLowerCase()
        normalized = normalized.replaceAll("to_lower\\s*\\(", "toLowerCase(");
        
        // to_upper()替换为toUpperCase()
        normalized = normalized.replaceAll("to_upper\\s*\\(", "toUpperCase(");
        
        // 检查处理前后表达式是否有变化，如果没有变化但包含特殊函数，可能是无法识别的表达式
        if (originalExpression.equals(normalized)) {
            // 检查是否包含可能未被处理的特殊函数
            if (containsUnrecognizedFunctions(originalExpression)) {
                logUnrecognizedExpression(originalExpression);
            }
        }
        
        return normalized;
    }
    
    /**
     * 检查表达式中是否包含未被识别的特殊函数
     * @param expression DSL表达式
     * @return 是否包含未识别的函数
     */
    private boolean containsUnrecognizedFunctions(String expression) {
        // 已知可以处理的函数列表
        String[] knownFunctions = {
            "contains", "bcontains", "icontains", "ibcontains", "matches", "bmatches",
            "to_lower", "toLowerCase", "to_upper", "toUpperCase", "ignoreCase", 
            "base64", "md5", "sha1", "sha256", "substr", "len", "substr", "regex", "rand",
            "string", "bytes", "reverse", "wait", "sleep", "submatch", "all_headers",
            "body_string", "status_code", "content_length", "content_type", "latency"
        };
        
        // 查找可能的函数调用
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\w+)\\s*\\(");
        java.util.regex.Matcher matcher = pattern.matcher(expression);
        
        while (matcher.find()) {
            String foundFunction = matcher.group(1);
            boolean isKnown = false;
            
            for (String knownFunction : knownFunctions) {
                if (foundFunction.equals(knownFunction)) {
                    isKnown = true;
                    break;
                }
            }
            
            if (!isKnown) {
                return true; // 发现未知函数
            }
        }
        
        return false;
    }
    
    /**
     * 记录未能识别的DSL表达式到文件
     * @param expression 未识别的DSL表达式
     */
    private void logUnrecognizedExpression(String expression) {
        try {
            java.io.File logFile = new java.io.File("1.txt");
            boolean fileExists = logFile.exists();
            
            java.io.FileWriter writer = new java.io.FileWriter(logFile, true); // 追加模式
            java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(writer);
            
            // 如果是新文件，添加标题
            if (!fileExists) {
                bufferedWriter.write("未识别的DSL表达式记录:\n");
                bufferedWriter.write("===================\n\n");
            }
            
            // 记录时间戳和表达式
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new java.util.Date());
            
            bufferedWriter.write("[" + timestamp + "] " + expression + "\n");
            bufferedWriter.close();
            
            if (scanConfig.isDebug()) {
                System.out.println("[!] 发现未识别的DSL表达式: " + expression);
            }
        } catch (java.io.IOException e) {
            if (scanConfig.isDebug()) {
                System.err.println("[×] 记录未识别的DSL表达式时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 评估标准化后的DSL表达式
     * @param context DSL上下文
     * @param expression DSL表达式
     * @return 表达式评估结果
     */
    private boolean evaluateDslExpression(Map<String, Object> context, String expression) {
        // 处理复合表达式
        if (expression.contains(" && ")) {
            return evaluateLogicalAnd(context, expression);
        } else if (expression.contains(" || ")) {
            return evaluateLogicalOr(context, expression);
        }
        
        // 处理各种函数和操作符
        if (expression.contains("contains(")) {
            return evaluateContainsFunction(context, expression);
        } else if (expression.contains("matches(") || expression.contains("regex(")) {
            return evaluateRegexFunction(context, expression);
        } else if (expression.contains("ignoreCase(")) {
            return evaluateIgnoreCaseFunction(context, expression);
        } else if (expression.contains("toLowerCase(")) {
            return evaluateLowerCaseFunction(context, expression);
        } else if (expression.contains("toUpperCase(")) {
            return evaluateUpperCaseFunction(context, expression);
        } else if (expression.contains("len(")) {
            return evaluateLengthFunction(context, expression);
        } else if (evaluateComparisonExpression(context, expression)) {
            // 比较表达式单独处理，涵盖各种比较操作符
            return true;
        }
        
        // 无法识别的表达式
        if (scanConfig.isDebug()) {
            System.err.println("无法识别的DSL表达式: " + expression);
        }
        return false;
    }
    
    /**
     * 评估逻辑与表达式
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateLogicalAnd(Map<String, Object> context, String expression) {
        String[] parts = expression.split(" && ");
        for (String part : parts) {
            if (!evaluateDslExpression(context, part.trim())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 评估逻辑或表达式
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateLogicalOr(Map<String, Object> context, String expression) {
        String[] parts = expression.split(" \\|\\| ");
        for (String part : parts) {
            if (evaluateDslExpression(context, part.trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 评估contains函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateContainsFunction(Map<String, Object> context, String expression) {
        // 处理形如 field.contains("value") 的表达式
        int containsIndex = expression.indexOf("contains(");
        if (containsIndex <= 0) {
            return false;
        }
        
        String fieldPath = expression.substring(0, containsIndex).trim();
        if (fieldPath.endsWith(".")) {
            fieldPath = fieldPath.substring(0, fieldPath.length() - 1);
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', containsIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String valueStr = expression.substring(openBracket + 1, closeBracket).trim();
        // 解析值，可能是字符串字面量或其他表达式
        Object searchValue = resolveValue(context, valueStr);
        if (searchValue == null) {
            return false;
        }
        
        // 获取字段值并检查是否包含指定值
        Object fieldValue = resolveValue(context, fieldPath);
        if (fieldValue == null) {
            return false;
        }
        
        return fieldValue.toString().contains(searchValue.toString());
    }
    
    /**
     * 评估正则表达式匹配函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateRegexFunction(Map<String, Object> context, String expression) {
        // 获取函数名（matches或regex）
        String funcName = expression.contains("matches(") ? "matches" : "regex";
        int funcIndex = expression.indexOf(funcName + "(");
        if (funcIndex <= 0) {
            return false;
        }
        
        String fieldPath = expression.substring(0, funcIndex).trim();
        if (fieldPath.endsWith(".")) {
            fieldPath = fieldPath.substring(0, fieldPath.length() - 1);
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String patternStr = expression.substring(openBracket + 1, closeBracket).trim();
        // 移除引号
        patternStr = stripQuotes(patternStr);
        
        // 获取字段值并使用正则匹配
        Object fieldValue = resolveValue(context, fieldPath);
        if (fieldValue == null) {
            return false;
        }
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
            java.util.regex.Matcher matcher = pattern.matcher(fieldValue.toString());
            return matcher.find();
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("正则表达式错误: " + patternStr + ", " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 评估忽略大小写函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateIgnoreCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("ignoreCase(");
        if (funcIndex < 0) {
            return false;
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();
        
        // 创建一个带有大小写不敏感标志的上下文副本
        Map<String, Object> caseInsensitiveContext = new HashMap<>(context);
        caseInsensitiveContext.put("__case_insensitive", true);
        
        // 评估内部表达式
        return evaluateDslExpression(caseInsensitiveContext, innerExpr);
    }
    
    /**
     * 评估转小写函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateLowerCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("toLowerCase(");
        if (funcIndex < 0) {
            return false;
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();
        
        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }
        
        // 转换为小写并存入新上下文
        Map<String, Object> lowerCaseContext = new HashMap<>(context);
        lowerCaseContext.put("__lower_case_result", innerValue.toString().toLowerCase());
        
        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(lowerCaseContext, "__lower_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            // 如果有剩余表达式，那么它应该是一个比较操作
            return evaluateComparisonExpression(lowerCaseContext, "__lower_case_result " + remainingExpr);
        }
        
        // 如果没有后续操作，返回转换后的值
        return !lowerCaseContext.get("__lower_case_result").toString().isEmpty();
    }
    
    /**
     * 评估转大写函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateUpperCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("toUpperCase(");
        if (funcIndex < 0) {
            return false;
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();
        
        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }
        
        // 转换为大写并存入新上下文
        Map<String, Object> upperCaseContext = new HashMap<>(context);
        upperCaseContext.put("__upper_case_result", innerValue.toString().toUpperCase());
        
        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(upperCaseContext, "__upper_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            // 如果有剩余表达式，那么它应该是一个比较操作
            return evaluateComparisonExpression(upperCaseContext, "__upper_case_result " + remainingExpr);
        }
        
        // 如果没有后续操作，返回转换后的值
        return !upperCaseContext.get("__upper_case_result").toString().isEmpty();
    }
    
    /**
     * 评估长度函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateLengthFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("len(");
        if (funcIndex < 0) {
            return false;
        }
        
        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }
        
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();
        
        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }
        
        // 计算长度并存入新上下文
        Map<String, Object> lengthContext = new HashMap<>(context);
        lengthContext.put("__length_result", innerValue.toString().length());
        
        // 处理长度后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (!remainingExpr.isEmpty()) {
            // 如果有剩余表达式，那么它应该是一个比较操作
            return evaluateComparisonExpression(lengthContext, "__length_result " + remainingExpr);
        }
        
        // 如果没有后续操作，返回长度值（非零为真）
        return Integer.parseInt(lengthContext.get("__length_result").toString()) > 0;
    }
    
    /**
     * 评估比较表达式
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateComparisonExpression(Map<String, Object> context, String expression) {
        // 检查各种比较操作符
        if (expression.contains("==")) {
            return processComparisonOperator(context, expression, "==");
        } else if (expression.contains("!=")) {
            return processComparisonOperator(context, expression, "!=");
        } else if (expression.contains(">=")) {
            return processComparisonOperator(context, expression, ">=");
        } else if (expression.contains("<=")) {
            return processComparisonOperator(context, expression, "<=");
        } else if (expression.contains(">")) {
            return processComparisonOperator(context, expression, ">");
        } else if (expression.contains("<")) {
            return processComparisonOperator(context, expression, "<");
        }
        
        return false;
    }
    
    /**
     * 处理比较操作符
     * @param context DSL上下文
     * @param expression 表达式
     * @param operator 操作符
     * @return 比较结果
     */
    private boolean processComparisonOperator(Map<String, Object> context, String expression, String operator) {
        String[] parts = expression.split(operator, 2);
        if (parts.length != 2) {
            return false;
        }
        
        String leftExpr = parts[0].trim();
        String rightExpr = parts[1].trim();
        
        Object leftValue = resolveValue(context, leftExpr);
        Object rightValue = resolveValue(context, rightExpr);
        
        if (leftValue == null || rightValue == null) {
            return false;
        }
        
        // 检查是否忽略大小写
        boolean ignoreCase = context.containsKey("__case_insensitive") && 
                            (Boolean)context.get("__case_insensitive");
        
        // 尝试数值比较
        try {
            double leftNum = Double.parseDouble(leftValue.toString());
            double rightNum = Double.parseDouble(rightValue.toString());
            
            switch (operator) {
                case "==": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                default: return false;
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，按字符串比较
            String leftStr = leftValue.toString();
            String rightStr = rightValue.toString();
            
            if (ignoreCase) {
                leftStr = leftStr.toLowerCase();
                rightStr = rightStr.toLowerCase();
            }
            
            int comparison = leftStr.compareTo(rightStr);
            
            switch (operator) {
                case "==": return comparison == 0;
                case "!=": return comparison != 0;
                case ">": return comparison > 0;
                case "<": return comparison < 0;
                case ">=": return comparison >= 0;
                case "<=": return comparison <= 0;
                default: return false;
            }
        }
    }
    
    /**
     * 查找对应的右括号位置
     * @param expression 表达式
     * @param openBracketPos 左括号位置
     * @return 右括号位置，未找到返回-1
     */
    private int findClosingBracket(String expression, int openBracketPos) {
        if (openBracketPos < 0 || openBracketPos >= expression.length()) {
            return -1;
        }
        
        int count = 1;
        for (int i = openBracketPos + 1; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * 去除字符串两端的引号
     * @param str 原始字符串
     * @return 去除引号后的字符串
     */
    private String stripQuotes(String str) {
        if (str == null) {
            return "";
        }
        
        str = str.trim();
        if ((str.startsWith("\"") && str.endsWith("\"")) || 
            (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1);
        }
        
        return str;
    }
    
    /**
     * 解析上下文中的值
     * @param context 上下文
     * @param reference 引用路径
     * @return 解析后的值
     */
    private Object resolveValue(Map<String, Object> context, String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        
        // 检查是否是字面量
        if (reference.startsWith("\"") && reference.endsWith("\"")) {
            return reference.substring(1, reference.length() - 1);
        }
        if (reference.startsWith("'") && reference.endsWith("'")) {
            return reference.substring(1, reference.length() - 1);
        }
        
        // 尝试解析数字
        try {
            return Double.parseDouble(reference);
        } catch (NumberFormatException ignored) {
            // 不是数字，继续
        }
        
        // 解析字段引用
        return getFieldValue(context, reference);
    }

    /**
     * 创建DSL表达式的上下文
     * @param response HTTP响应
     * @return 上下文映射
     */
    private Map<String, Object> createDslContext(CustomHttpResponse response) {
        // 创建顶级上下文对象
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> responseMap = new HashMap<>();

        int status = 0;
        try {
            status = response.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseMap.put("body", response.getTextStr());
        responseMap.put("status", status);
        responseMap.put("content_type", getResponseHeader(response.getHeaderFields(), "Content-Type"));
        responseMap.put("headers", response.getHeaderFields());
        responseMap.put("raw", response.getTextStr()); // 原始响应内容
        responseMap.put("time", response.getResponseTime());
        responseMap.putAll(response.getHeaderFields());
        
        context.put("response", responseMap);
        
        // 添加请求相关的变量
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("url", response.getURL().toString());
        
        // 提取URL的各个部分
        try {
            URL url = response.getURL();
            requestMap.put("path", url.getPath());
            requestMap.put("host", url.getHost());
            requestMap.put("scheme", url.getProtocol());
            requestMap.put("port", url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("解析URL失败: " + e.getMessage());
            }
        }
        
        context.put("request", requestMap);
        
        return context;
    }
    
    /**
     * 从响应头集合中获取指定头的值
     * @param headers 响应头集合
     * @param headerName 头名称
     * @return 头值或空字符串
     */
    private String getResponseHeader(Map<String, List<String>> headers, String headerName) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (headerName.equalsIgnoreCase(entry.getKey())) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return String.join(", ", values);
                    }
                    break;
                }
            }
        }
        return "";
    }

    /**
     * 扫描配置类
     */
    public static class ScanConfig {
        private int threads = 10; // 默认线程数
        private String protocol; // 协议类型过滤
        private PocObj.Severity severity; // 严重程度过滤
        private boolean debug = false; // 是否开启调试模式
        private String proxy; // 全局代理
        private int timeOut = 30; // 默认超时时间（秒）
        
        public int getThreads() {
            return threads;
        }
        
        public void setThreads(int threads) {
            this.threads = threads;
        }
        
        public String getProtocol() {
            return protocol;
        }
        
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
        
        public PocObj.Severity getSeverity() {
            return severity;
        }
        
        public void setSeverity(PocObj.Severity severity) {
            this.severity = severity;
        }
        
        public boolean isDebug() {
            return debug;
        }
        
        public void setDebug(boolean debug) {
            this.debug = debug;
        }
        
        public String getProxy() {
            return proxy;
        }
        
        public void setProxy(String proxy) {
            this.proxy = proxy;
        }
        
        public int getTimeOut() {
            return timeOut;
        }
        
        public void setTimeOut(int timeOut) {
            this.timeOut = timeOut;
        }
    }
    
    /**
     * 扫描结果类
     */
    public static class ScanResult {
        private String target; // 目标URL
        private PocObj.Poc poc; // 匹配的POC
        private boolean vulnerable; // 是否存在漏洞
        private long timestamp; // 扫描时间戳
        private Map<String, Object> details = new HashMap<>(); // 详细信息
        
        public String getTarget() {
            return target;
        }
        
        public void setTarget(String target) {
            this.target = target;
        }
        
        public PocObj.Poc getPoc() {
            return poc;
        }
        
        public void setPoc(PocObj.Poc poc) {
            this.poc = poc;
        }
        
        public boolean isVulnerable() {
            return vulnerable;
        }
        
        public void setVulnerable(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public Map<String, Object> getDetails() {
            return details;
        }
        
        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
        
        public void addDetail(String key, Object value) {
            this.details.put(key, value);
        }
        
        @Override
        public String toString() {
            return "目标: " + target + ", POC: " + poc.getName() + ", 漏洞: " + (vulnerable ? "存在" : "不存在");
        }
    }
    
    /**
     * 扫描回调接口
     */
    public interface ScanCallback {
        /**
         * 扫描结果回调
         * @param result 扫描结果
         */
        void onResult(ScanResult result);
        
        /**
         * 扫描完成回调
         * @param results 所有扫描结果
         */
        void onComplete(List<ScanResult> results);
    }


    /**
     * 处理特殊变量
     * @param requestObj 请求对象
     * @param specialLine 包含特殊变量的行
     */
    private void processSpecialVariable(RequestObj requestObj, String specialLine) {
        if (specialLine == null || !specialLine.startsWith("@")) {
            return;
        }
        
        if (scanConfig.isDebug()) {
            System.out.println("处理特殊变量: " + specialLine);
        }
        
        // 分割特殊变量行，格式如：@timeout=10 @proxy=127.0.0.1:8080 @followRedirect=true
        String[] variables = specialLine.split("\\s+");
        
        for (String variable : variables) {
            if (!variable.startsWith("@")) {
                continue;
            }
            
            // 去掉@前缀
            String varWithoutPrefix = variable.substring(1);
            
            // 分割变量名和值
            String[] parts = varWithoutPrefix.split("=", 2);
            if (parts.length != 2) {
                if (scanConfig.isDebug()) {
                    System.err.println("特殊变量格式错误: " + variable + "，应为 @name=value 格式");
                }
                continue;
            }
            
            String name = parts[0].trim().toLowerCase();
            String value = parts[1].trim();
            
            // 根据变量名设置请求对象的属性
            switch (name) {
                case "timeout":
                    try {
                        int timeout = Integer.parseInt(value);
                        if (timeout > 0) {
                            requestObj.setTimeOut(timeout);
                            // 同时更新ScanConfig中的超时时间，确保一致性
                            if (scanConfig != null) {
                                scanConfig.setTimeOut(timeout);
                            }
                        } else {
                            System.err.println("超时值必须大于0: " + value);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("无效的超时值: " + value);
                    }
                    break;
                    
                case "proxy":
                    requestObj.setProxies(value);
                    break;
                    
                case "followredirect":
                case "followredirects":
                    requestObj.setFollowRedirects(Boolean.parseBoolean(value));
                    break;
                    
                case "retries":
                    try {
                        int retries = Integer.parseInt(value);
                        requestObj.setRetries(retries);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试次数: " + value);
                    }
                    break;
                    
                case "retrywaittime":
                    try {
                        int retryWaitTime = Integer.parseInt(value);
                        requestObj.setRetryWaitTime(retryWaitTime);
                    } catch (NumberFormatException e) {
                        System.err.println("无效的重试等待时间: " + value);
                    }
                    break;
                    
                case "randomuseragent":
                    requestObj.setRandomUserAgent(Boolean.parseBoolean(value));
                    break;
                    
                case "nouseragent":
                    requestObj.setNoUserAgent(Boolean.parseBoolean(value));
                    break;
                    
                case "strictsslvalidation":
                    requestObj.setStrictSslValidation(Boolean.parseBoolean(value));
                    break;
                    
                case "proxiestype":
                    try {
                        requestObj.setProxiesType(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println(e.getMessage());
                    }
                    break;
                    
                default:
                    if (scanConfig.isDebug()) {
                        System.out.println("未知的特殊变量: " + name + "=" + value);
                    }
                    break;
            }
        }
    }


    /**
     * 根据字段路径获取字段值
     * @param context 上下文映射
     * @param fieldPath 字段路径
     * @return 字段值或null
     */
    private Object getFieldValue(Map<String, Object> context, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }
        
        String[] parts = fieldPath.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
}