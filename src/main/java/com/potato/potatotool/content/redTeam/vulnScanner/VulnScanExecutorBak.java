package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocManager;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
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
public class VulnScanExecutorBak {
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
    public VulnScanExecutorBak() {
        this.scanConfig = new ScanConfig();
        initExecutor();
    }

    /**
     * 构造函数
     * @param scanConfig 扫描配置
     */
    public VulnScanExecutorBak(ScanConfig scanConfig) {
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
     * 执行POC步骤
     * @param target 目标URL
     * @param poc 当前POC对象
     * @return 是否匹配成功
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
                boolean matched = matchResponse(response, step.getMatchers(), step.getMatchersCondition());

                // 如果匹配失败且步骤是必要的，则返回失败
                if (!matched) {
                    return false;
                }

                // 提取变量
                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                   extractVariables(response, step.getExtractors(), extractedValues);
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

    /**
     * 从响应中提取变量
     * @param response HTTP响应
     * @param extractors 提取器列表
     * @param extractedValues 提取的变量映射
     */
    private void extractVariables(CustomHttpResponse response, List<PocObj.Matcher> extractors, Map<String, String> extractedValues) {
        if (response == null || extractors == null || extractors.isEmpty()) {
            return;
        }

        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null) {
                continue; // 跳过无效提取器
            }

            try {
                String part = extractor.getPart();
                if (part == null || part.isEmpty()) {
                    part = "body"; // 默认从响应体提取
                }

                String content = getResponsePart(response, part);
                if (content == null || content.isEmpty()) {
                    continue; // 跳过空内容
                }

                String extractedValue = null;

                // 根据提取器类型进行提取
                switch (extractor.getType()) {
                    case REGEX:
                        extractedValue = extractRegex(content, extractor.getValues(), extractor.getGroup());
                        break;
                    case JSON:
                        extractedValue = extractJson(content, extractor.getValues());
                        break;
                    case XPATH:
                        extractedValue = extractXpath(content, extractor.getValues(), extractor.getAttribute());
                        break;
                    case DSL:
                        extractedValue = extractDsl(response, extractor.getValues());
                        break;
                    case KVAL:
                        extractedValue = extractKval(content, extractor.getValues());
                        break;
                    default:
                        // 默认尝试使用正则表达式提取
                        extractedValue = extractRegex(content, extractor.getValues(), 1);
                }

                // 更新提取的变量
                if (extractedValue != null) {
                    extractedValues.put(extractor.getName(), extractedValue);
                    if (scanConfig.isDebug()) {
                        System.out.println("提取变量成功: " + extractor.getName() + " = " + extractedValue);
                    }
                }
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    System.err.println("提取变量失败: " + extractor.getName() + ", 错误: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 使用正则表达式提取内容
     * @param content 待提取的内容
     * @param patterns 正则表达式列表
     * @param group 匹配组号
     * @return 提取的内容
     */
    private String extractRegex(String content, List<String> patterns, int group) {
        if (content == null || patterns == null || patterns.isEmpty()) {
            return null;
        }

        for (String pattern : patterns) {
            try {
                Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
                Matcher matcher = regex.matcher(content);
                if (matcher.find()) {
                    // 确保组号有效
                    int effectiveGroup = group;
                    if (effectiveGroup < 0 || effectiveGroup > matcher.groupCount()) {
                        effectiveGroup = matcher.groupCount() > 0 ? 1 : 0;
                    }
                    return matcher.group(effectiveGroup);
                }
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    System.err.println("正则表达式提取失败: " + pattern + ", 错误: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 使用JSON路径提取内容
     * @param content JSON内容
     * @param paths JSON路径列表
     * @return 提取的内容
     */
    private String extractJson(String content, List<String> paths) {
        return JsonExtractor.extractJson(content, paths);
    }

    /**
     * 使用XPath提取内容
     * @param content XML内容
     * @param xpaths XPath列表
     * @param attribute 属性名
     * @return 提取的内容
     */
    private String extractXpath(String content, List<String> xpaths, String attribute) {
        if (content == null || xpaths == null || xpaths.isEmpty()) {
            return null;
        }

        try {
            // 创建DOM解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            
            // 创建XPath对象
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            
            for (String xpathExpr : xpaths) {
                try {
                    // 执行XPath查询
                    Object result;
                    if (attribute != null && !attribute.isEmpty()) {
                        // 查询属性
                        result = xpath.evaluate(xpathExpr + "/@" + attribute, doc, XPathConstants.STRING);
                    } else {
                        // 查询节点内容
                        result = xpath.evaluate(xpathExpr, doc, XPathConstants.STRING);
                    }
                    
                    if (result != null && !result.toString().isEmpty()) {
                        return result.toString();
                    }
                } catch (Exception e) {
                    if (scanConfig.isDebug()) {
                        System.err.println("XPath提取失败: " + xpathExpr + ", 错误: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("XML解析失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 使用DSL表达式提取内容
     * @param response HTTP响应
     * @param expressions DSL表达式列表
     * @return 提取的内容
     */
    private String extractDsl(CustomHttpResponse response, List<String> expressions) {
        if (response == null || expressions == null || expressions.isEmpty()) {
            return null;
        }

        // 创建上下文环境
        Map<String, Object> context = createDslContext(response);
        
        for (String expr : expressions) {
            try {
                if (expr.contains(" = ")) {
                    // 提取赋值表达式右侧的值
                    String[] parts = expr.split(" = ", 2);
                    if (parts.length == 2) {
                        String valueExpr = parts[1];
                        Object value = resolveValue(context, valueExpr);
                        if (value != null) {
                            return value.toString();
                        }
                    }
                } else {
                    // 直接解析表达式
                    Object value = resolveValue(context, expr);
                    if (value != null) {
                        return value.toString();
                    }
                }
            } catch (Exception e) {
                if (scanConfig.isDebug()) {
                    System.err.println("DSL表达式提取失败: " + expr + ", 错误: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 提取键值对
     * @param content 内容
     * @param kvExpressions 键值表达式列表
     * @return 提取的值
     */
    private String extractKval(String content, List<String> kvExpressions) {
        if (content == null || kvExpressions == null || kvExpressions.isEmpty()) {
            return null;
        }

        Map<String, String> kvMap = new HashMap<>();
        
        // 解析内容为键值对
        try {
            // 分割行
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 尝试以多种分隔符分割
                String[] kv = null;
                if (line.contains(":")) {
                    kv = line.split(":", 2);
                } else if (line.contains("=")) {
                    kv = line.split("=", 2);
                } else if (line.contains("\t")) {
                    kv = line.split("\t", 2);
                } else if (line.contains(" - ")) {
                    kv = line.split(" - ", 2);
                } else if (line.contains("; ")) {
                    // 处理HTTP Cookie格式: name=value; name2=value2
                    String[] parts = line.split("; ");
                    for (String part : parts) {
                        if (part.contains("=")) {
                            String[] cookiePair = part.split("=", 2);
                            if (cookiePair.length == 2) {
                                kvMap.put(cookiePair[0].trim(), cookiePair[1].trim());
                            }
                        }
                    }
                    continue; // 已处理此行，继续下一行
                }
                
                if (kv != null && kv.length == 2) {
                    kvMap.put(kv[0].trim(), kv[1].trim());
                }
                
                // 处理HTTP头格式的特殊情况（比如Set-Cookie可能有多行）
                if (kv != null && kv.length == 2 && "Set-Cookie".equalsIgnoreCase(kv[0].trim())) {
                    String cookieStr = kv[1].trim();
                    if (cookieStr.contains("=")) {
                        String[] cookieParts = cookieStr.split(";");
                        if (cookieParts.length > 0) {
                            String[] mainCookie = cookieParts[0].split("=", 2);
                            if (mainCookie.length == 2) {
                                // 保存Cookie名称和值
                                kvMap.put(mainCookie[0].trim(), mainCookie[1].trim());
                            }
                        }
                    }
                }
            }
            
            // 处理HTTP头格式（可能存在于整个content中）
            // 例如：从整个响应中提取特定的头信息
            for (String expr : kvExpressions) {
                // 尝试匹配HTTP头格式：Header: Value
                Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(expr) + "\\s*:\\s*([^\\r\\n]+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
            
            // 使用表达式提取值
            for (String expr : kvExpressions) {
                if (kvMap.containsKey(expr)) {
                    return kvMap.get(expr);
                }
            }
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("键值对提取失败: " + e.getMessage());
                e.printStackTrace(); // 打印更详细的错误信息
            }
        }
        return null;
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
                    // 新处理方式: 先解析成嵌套Matcher结构再处理
                    return matchDslWithNestedMatchers(response, matcher.getValues());
                case GROUP:
                    // 处理子匹配器组
                    if (matcher.getSubMatchers() != null && !matcher.getSubMatchers().isEmpty()) {
                        List<PocObj.Matcher> subMatchers = matcher.getSubMatchers();
                        String condition = matcher.getCondition();
                        PocObj.MatchersCondition matchersCondition = PocObj.MatchersCondition.AND;
                        if (condition != null && condition.equalsIgnoreCase("OR")) {
                            matchersCondition = PocObj.MatchersCondition.OR;
                        }
                        return matchResponse(response, subMatchers, matchersCondition);
                    }
                    return false;
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
     * 使用嵌套Matcher结构处理DSL表达式
     * @param response HTTP响应
     * @param dslExpressions DSL表达式列表
     * @return 是否匹配成功
     */
    private boolean matchDslWithNestedMatchers(CustomHttpResponse response, List<String> dslExpressions) {
        if (dslExpressions == null || dslExpressions.isEmpty()) {
            return false;
        }

        try {
            // 创建上下文环境
            Map<String, Object> context = createDslContext(response);

            // 处理每个DSL表达式
            for (String dslExpr : dslExpressions) {
                try {
                    // 解析DSL表达式为Matcher结构
                    List<PocObj.Matcher> dslMatchers = parseDslToMatchers(dslExpr, context);
                    if (dslMatchers != null && !dslMatchers.isEmpty()) {
                        // 如果只有一个匹配器，直接处理
                        if (dslMatchers.size() == 1) {
                            if (matchSingleDslMatcher(context, dslMatchers.get(0))) {
                                return true;
                            }
                        } else {
                            // 多个匹配器，默认使用AND条件
                            if (matchMultipleDslMatchers(context, dslMatchers, PocObj.MatchersCondition.AND)) {
                                return true;
                            }
                        }
                    } else {
                        // 降级：如果无法解析为Matcher结构，尝试使用原始方式
                        if (evaluateUnifiedDsl(context, dslExpr)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    if (scanConfig.isDebug()) {
                        System.err.println("DSL表达式解析失败: " + dslExpr + ", 错误: " + e.getMessage());
                    }
                    // 尝试原始方式作为退路
                    if (evaluateUnifiedDsl(context, dslExpr)) {
                        return true;
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
     * 将DSL表达式解析为匹配器结构
     * @param dslExpr DSL表达式
     * @param context 上下文
     * @return 匹配器列表
     */
    private List<PocObj.Matcher> parseDslToMatchers(String dslExpr, Map<String, Object> context) {
        List<PocObj.Matcher> matchers = new ArrayList<>();

        // 标准化表达式
        String normalizedExpr = normalizeExpression(dslExpr);

        // 处理逻辑运算符分割表达式
        if (normalizedExpr.contains(" && ")) {
            // AND 逻辑
            String[] parts = normalizedExpr.split(" && ");
            for (String part : parts) {
                PocObj.Matcher subMatcher = createDslMatcher(part.trim(), context);
                if (subMatcher != null) {
                    matchers.add(subMatcher);
                }
            }
            // 如果有多个匹配器，创建一个GROUP类型的主匹配器
            if (matchers.size() > 1) {
                PocObj.Matcher groupMatcher = new PocObj.Matcher();
                groupMatcher.setType(PocObj.MatcherType.GROUP);
                groupMatcher.setSubMatchers(matchers);
                groupMatcher.setCondition("AND");

                List<PocObj.Matcher> result = new ArrayList<>();
                result.add(groupMatcher);
                return result;
            }
        } else if (normalizedExpr.contains(" || ")) {
            // OR 逻辑
            String[] parts = normalizedExpr.split(" \\|\\| ");
            for (String part : parts) {
                PocObj.Matcher subMatcher = createDslMatcher(part.trim(), context);
                if (subMatcher != null) {
                    matchers.add(subMatcher);
                }
            }
            // 如果有多个匹配器，创建一个GROUP类型的主匹配器
            if (matchers.size() > 1) {
                PocObj.Matcher groupMatcher = new PocObj.Matcher();
                groupMatcher.setType(PocObj.MatcherType.GROUP);
                groupMatcher.setSubMatchers(matchers);
                groupMatcher.setCondition("OR");

                List<PocObj.Matcher> result = new ArrayList<>();
                result.add(groupMatcher);
                return result;
            }
        } else {
            // 单个表达式
            PocObj.Matcher matcher = createDslMatcher(normalizedExpr, context);
            if (matcher != null) {
                matchers.add(matcher);
            }
        }

        return matchers;
    }

    /**
     * 创建单个DSL匹配器
     * @param expr 表达式
     * @param context 上下文
     * @return 匹配器对象
     */
    private PocObj.Matcher createDslMatcher(String expr, Map<String, Object> context) {
        PocObj.Matcher matcher = new PocObj.Matcher();

        try {
            // 处理包含函数的表达式
            if (expr.contains("contains(")) {
                matcher.setType(PocObj.MatcherType.WORD);
                parseContainsFunction(expr, matcher, context);
            } else if (expr.contains("ignoreCase(")) {
                matcher.setType(PocObj.MatcherType.WORD);
                matcher.setCaseInsensitive(true);
                parseIgnoreCaseFunction(expr, matcher, context);
            } else if (expr.contains("matches(") || expr.contains("bmatches(")) {
                matcher.setType(PocObj.MatcherType.REGEX);
                parseRegexFunction(expr, matcher, context);
            } else if (expr.contains("len(")) {
                matcher.setType(PocObj.MatcherType.SIZE);
                parseLengthFunction(expr, matcher, context);
            } else if (expr.contains("status")) {
                matcher.setType(PocObj.MatcherType.STATUS);
                parseStatusExpression(expr, matcher, context);
            } else if (isComparisonExpression(expr)) {
                // 处理比较表达式
                parseComparisonExpression(expr, matcher, context);
            } else {
                // 无法识别的表达式，返回原始DSL类型
                matcher.setType(PocObj.MatcherType.DSL);
                List<String> values = new ArrayList<>();
                values.add(expr);
                matcher.setValues(values);
            }

            return matcher;
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("创建DSL匹配器失败: " + expr + ", 错误: " + e.getMessage());
            }

            // 创建一个降级的DSL匹配器
            matcher.setType(PocObj.MatcherType.DSL);
            List<String> values = new ArrayList<>();
            values.add(expr);
            matcher.setValues(values);
            return matcher;
        }
    }

    /**
     * 解析contains函数
     */
    private void parseContainsFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("contains(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argsStr = expr.substring(openBracket + 1, closeBracket);
        String[] args = splitFunctionArgs(argsStr);

        if (args.length >= 2) {
            // 设置匹配的部分（body, header等）
            String sourcePart = stripQuotes(args[0]);
            matcher.setPart(determinePart(sourcePart, context));

            // 设置匹配值
            List<String> values = new ArrayList<>();
            values.add(stripQuotes(args[1]));
            matcher.setValues(values);

            // 设置操作类型为包含
            matcher.setOperation(PocObj.OperationType.CONTAINS);
        }
    }

    /**
     * 解析ignoreCase函数
     */
    private void parseIgnoreCaseFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("ignoreCase(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String innerExpr = expr.substring(openBracket + 1, closeBracket);

        // 如果内部是contains函数，递归解析
        if (innerExpr.contains("contains(")) {
            parseContainsFunction(innerExpr, matcher, context);
            matcher.setCaseInsensitive(true);
        }
    }

    /**
     * 解析正则表达式函数
     */
    private void parseRegexFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("matches(");
        if (funcIndex < 0) {
            funcIndex = expr.indexOf("bmatches(");
        }
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argsStr = expr.substring(openBracket + 1, closeBracket);
        String[] args = splitFunctionArgs(argsStr);

        if (args.length >= 2) {
            // 设置匹配的部分
            String sourcePart = stripQuotes(args[0]);
            matcher.setPart(determinePart(sourcePart, context));

            // 设置匹配值（正则表达式）
            List<String> values = new ArrayList<>();
            values.add(stripQuotes(args[1]));
            matcher.setValues(values);
        }
    }

    /**
     * 解析长度函数
     */
    private void parseLengthFunction(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        int funcIndex = expr.indexOf("len(");
        if (funcIndex < 0) return;

        int openBracket = expr.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expr, openBracket);
        if (closeBracket < 0) return;

        String argStr = expr.substring(openBracket + 1, closeBracket);

        // 找出比较操作符
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;
        String valueStr = null;

        if (expr.contains("==")) {
            opType = PocObj.OperationType.EQUAL;
            valueStr = expr.substring(expr.indexOf("==") + 2).trim();
        } else if (expr.contains("!=")) {
            opType = PocObj.OperationType.NOT_EQUAL;
            valueStr = expr.substring(expr.indexOf("!=") + 2).trim();
        } else if (expr.contains(">=")) {
            opType = PocObj.OperationType.GREATER_EQUAL;
            valueStr = expr.substring(expr.indexOf(">=") + 2).trim();
        } else if (expr.contains("<=")) {
            opType = PocObj.OperationType.LESS_EQUAL;
            valueStr = expr.substring(expr.indexOf("<=") + 2).trim();
        } else if (expr.contains(">")) {
            opType = PocObj.OperationType.GREATER;
            valueStr = expr.substring(expr.indexOf(">") + 1).trim();
        } else if (expr.contains("<")) {
            opType = PocObj.OperationType.LESS;
            valueStr = expr.substring(expr.indexOf("<") + 1).trim();
        }

        // 设置匹配的部分
        matcher.setPart(determinePart(stripQuotes(argStr), context));

        // 设置操作类型
        matcher.setOperation(opType);

        // 设置匹配值
        if (valueStr != null) {
            List<String> values = new ArrayList<>();
            values.add(valueStr);
            matcher.setValues(values);
        }
    }

    /**
     * 解析状态码表达式
     */
    private void parseStatusExpression(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        matcher.setPart("status");

        // 找出比较操作符
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;
        String valueStr = null;

        if (expr.contains("==")) {
            opType = PocObj.OperationType.EQUAL;
            valueStr = expr.substring(expr.indexOf("==") + 2).trim();
        } else if (expr.contains("!=")) {
            opType = PocObj.OperationType.NOT_EQUAL;
            valueStr = expr.substring(expr.indexOf("!=") + 2).trim();
        } else if (expr.contains(">=")) {
            opType = PocObj.OperationType.GREATER_EQUAL;
            valueStr = expr.substring(expr.indexOf(">=") + 2).trim();
        } else if (expr.contains("<=")) {
            opType = PocObj.OperationType.LESS_EQUAL;
            valueStr = expr.substring(expr.indexOf("<=") + 2).trim();
        } else if (expr.contains(">")) {
            opType = PocObj.OperationType.GREATER;
            valueStr = expr.substring(expr.indexOf(">") + 1).trim();
        } else if (expr.contains("<")) {
            opType = PocObj.OperationType.LESS;
            valueStr = expr.substring(expr.indexOf("<") + 1).trim();
        }

        // 设置操作类型
        matcher.setOperation(opType);

        // 设置匹配值
        if (valueStr != null) {
            List<String> values = new ArrayList<>();
            values.add(valueStr);
            matcher.setValues(values);
        }
    }

    /**
     * 解析比较表达式
     */
    private void parseComparisonExpression(String expr, PocObj.Matcher matcher, Map<String, Object> context) {
        String leftPart = null;
        String rightPart = null;
        PocObj.OperationType opType = PocObj.OperationType.DEFAULT;

        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.EQUAL;
            }
        } else if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.NOT_EQUAL;
            }
        } else if (expr.contains(">=")) {
            String[] parts = expr.split(">=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.GREATER_EQUAL;
            }
        } else if (expr.contains("<=")) {
            String[] parts = expr.split("<=");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.LESS_EQUAL;
            }
        } else if (expr.contains(">")) {
            String[] parts = expr.split(">");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.GREATER;
            }
        } else if (expr.contains("<")) {
            String[] parts = expr.split("<");
            if (parts.length >= 2) {
                leftPart = parts[0].trim();
                rightPart = parts[1].trim();
                opType = PocObj.OperationType.LESS;
            }
        }

        if (leftPart != null && rightPart != null) {
            // 判断左侧是什么类型的表达式
            if (leftPart.contains("status") || leftPart.contains("response.status_code")) {
                matcher.setType(PocObj.MatcherType.STATUS);
                matcher.setPart("status");
            } else if (leftPart.contains("content_length") || leftPart.contains("response.content_length")) {
                matcher.setType(PocObj.MatcherType.SIZE);
                matcher.setPart("body");
            } else if (leftPart.contains("time") || leftPart.contains("response.time") || leftPart.contains("latency")) {
                matcher.setType(PocObj.MatcherType.TIME);
                matcher.setPart("time");
            } else {
                // 默认处理为WORD类型
                matcher.setType(PocObj.MatcherType.WORD);
                matcher.setPart(determinePart(leftPart, context));
            }

            // 设置操作类型
            matcher.setOperation(opType);

            // 设置匹配值
            List<String> values = new ArrayList<>();
            values.add(rightPart);
            matcher.setValues(values);
        }
    }

    /**
     * 确定表达式引用的部分（body, header等）
     */
    private String determinePart(String reference, Map<String, Object> context) {
        if (reference == null) return "body";

        reference = reference.trim();

        // 处理常见的引用方式
        if (reference.equals("body") || reference.equals("response.body") || reference.equals("response.body_string")) {
            return "body";
        } else if (reference.equals("headers") || reference.equals("response.headers") || reference.equals("all_headers") || reference.equals("response.all_headers")) {
            return "headers";
        } else if (reference.equals("status") || reference.equals("status_code") || reference.equals("response.status") || reference.equals("response.status_code")) {
            return "status";
        } else if (reference.startsWith("response.") && context.containsKey("response")) {
            // 判断是否引用具体的响应头
            Map<String, Object> responseObj = (Map<String, Object>) context.get("response");
            if (responseObj != null) {
                String headerName = reference.substring("response.".length());
                if (responseObj.containsKey(headerName)) {
                    return headerName;
                }
            }
        }

        // 默认返回body
        return "body";
    }

    /**
     * 判断是否为比较表达式
     */
    private boolean isComparisonExpression(String expr) {
        return expr.contains("==") || expr.contains("!=") ||
                expr.contains(">=") || expr.contains("<=") ||
                expr.contains(">") || expr.contains("<");
    }

    /**
     * 匹配单个DSL匹配器
     */
    private boolean matchSingleDslMatcher(Map<String, Object> context, PocObj.Matcher matcher) {
        // 根据匹配器类型判断如何处理
        switch (matcher.getType()) {
            case DSL:
                // 原始DSL表达式
                List<String> values = matcher.getValues();
                if (values != null && !values.isEmpty()) {
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
                }
                return false;

            case GROUP:
                // 处理组匹配器
                if (matcher.getSubMatchers() != null && !matcher.getSubMatchers().isEmpty()) {
                    String condition = matcher.getCondition();
                    PocObj.MatchersCondition matchersCondition = PocObj.MatchersCondition.AND;
                    if (condition != null && condition.equalsIgnoreCase("OR")) {
                        matchersCondition = PocObj.MatchersCondition.OR;
                    }
                    return matchMultipleDslMatchers(context, matcher.getSubMatchers(), matchersCondition);
                }
                return false;

            default:
                // 其他类型的匹配器，直接返回false
                if (scanConfig.isDebug()) {
                    System.err.println("不支持在DSL中使用的匹配器类型: " + matcher.getType());
                }
                return false;
        }
    }

    /**
     * 匹配多个DSL匹配器
     */
    private boolean matchMultipleDslMatchers(Map<String, Object> context, List<PocObj.Matcher> matchers, PocObj.MatchersCondition condition) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        if (condition == PocObj.MatchersCondition.OR) {
            // OR逻辑：任一匹配成功即返回true
            for (PocObj.Matcher matcher : matchers) {
                if (matchSingleDslMatcher(context, matcher)) {
                    return true;
                }
            }
            return false;
        } else {
            // AND逻辑：全部匹配成功才返回true
            for (PocObj.Matcher matcher : matchers) {
                if (!matchSingleDslMatcher(context, matcher)) {
                    return false;
                }
            }
            return true;
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
                                        headers.append(value).append("; ");
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
                            return String.join("; ", headerValues);
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
     * 匹配JSON
     * @param content JSON内容
     * @param values 匹配值列表
     * @return 是否匹配成功
     */
    private boolean matchJson(String content, List<String> values) {
        return JsonExtractor.matchJson(content, values);
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
            MessageDigest md = MessageDigest.getInstance("MD5");
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
            MessageDigest md = MessageDigest.getInstance("SHA-1");
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
        System.out.println("----------------------------------------------------");
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
                "body", "body_string", "status_code", "content_length", "content_type", "latency",
                "header", "html_element", "html_attribute", "raw", "request", "response",
                "htmlelement", "jsonpath", "contains_all", "contains_any", "compare_versions",
                "startswith", "endswith", "mmh3", "base64_py", "base64_decode", "hex_encode",
                "hex_decode", "replace", "tolower", "toupper", "interactsh_protocol", "tostring",
                "json_minify"
        };

        // 已知的对象/属性列表
        String[] knownObjects = {
                "response", "request", "status", "headers", "body", "content_type",
                "content_length", "raw", "time", "latency", "path", "host", "scheme", "port", "url",
                "header", "location", "body_1", "body_2", "body_3", "body_4", "body_5",
                "header_1", "header_2", "header_3", "header_4", "header_5", "status_code_1",
                "status_code_2", "location_1", "location_2", "content_type", "server", "set_cookie",
                "all_headers", "data", "BaseURL", "version", "internal_detected_version", "last_version"
        };

        // 查找可能的函数调用
        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(expression);

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
                if (scanConfig.isDebug()) {
                    System.err.println("未识别的函数: " + foundFunction);
                }
                return true; // 发现未知函数
            }
        }

        // 查找可能的对象/属性引用
        Pattern objPattern = Pattern.compile("\\b(\\w+)\\.(\\w+)");
        Matcher objMatcher = objPattern.matcher(expression);

        while (objMatcher.find()) {
            String object = objMatcher.group(1);
            // 检查对象是否在已知列表中
            boolean isKnownObj = false;
            for (String knownObj : knownObjects) {
                if (object.equals(knownObj)) {
                    isKnownObj = true;
                    break;
                }
            }

            if (!isKnownObj && !object.equals("__length_result") &&
                    !object.equals("__lower_case_result") && !object.equals("__upper_case_result") &&
                    !object.equals("__hash_result") && !object.equals("__base64_result") &&
                    !object.equals("__header_result") && !object.equals("__substr_result")) {
                if (scanConfig.isDebug()) {
                    System.err.println("未识别的对象/属性: " + object);
                }
                return true; // 发现未知对象
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
            File logFile = new File("errorPoc.txt");
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
            String timestamp = dateFormat.format(new Date());

            bufferedWriter.write("[" + timestamp + "] " + expression + "\n");
            bufferedWriter.close();

            if (scanConfig.isDebug()) {
                System.out.println("[!] 发现未识别的DSL表达式: " + expression);
            }
        } catch (IOException e) {
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
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        expression = expression.trim();

        // 查找"and"、"or"和"not"
        Pattern andPattern = Pattern.compile("\\band\\b", Pattern.CASE_INSENSITIVE);
        Pattern orPattern = Pattern.compile("\\bor\\b", Pattern.CASE_INSENSITIVE);
        Pattern notPattern = Pattern.compile("\\bnot\\b", Pattern.CASE_INSENSITIVE);

        Matcher andMatcher = andPattern.matcher(expression);
        Matcher orMatcher = orPattern.matcher(expression);
        Matcher notMatcher = notPattern.matcher(expression);

        if (andMatcher.find()) {
            String[] parts = expression.split("\\band\\b", 2);
            return evaluateDslExpression(context, parts[0]) && evaluateDslExpression(context, parts[1]);
        } else if (orMatcher.find()) {
            String[] parts = expression.split("\\bor\\b", 2);
            return evaluateDslExpression(context, parts[0]) || evaluateDslExpression(context, parts[1]);
        } else if (notMatcher.find()) {
            String remainingExpr = expression.substring(notMatcher.end()).trim();
            return !evaluateDslExpression(context, remainingExpr);
        }

        // 先检查是否有未被识别的函数或对象
        if (containsUnrecognizedFunctions(expression)) {
            if (scanConfig.isDebug()) {
                System.err.println("警告: 发现未识别的函数或对象在表达式: " + expression);
            }
        }

        // 评估特定函数
        if (expression.contains("contains_all(")) {
            return evaluateContainsAllFunction(context, expression);
        } else if (expression.contains("contains_any(")) {
            return evaluateContainsAnyFunction(context, expression);
        } else if (expression.contains("startswith(")) {
            return evaluateStartsWithFunction(context, expression);
        } else if (expression.contains("endswith(")) {
            return evaluateEndsWithFunction(context, expression);
        } else if (expression.contains("base64_decode(")) {
            return evaluateBase64DecodeFunction(context, expression);
        } else if (expression.contains("version_compare(")) {
            return evaluateVersionCompareFunction(context, expression);
        } else if (expression.contains("md5(") || expression.contains("sha1(") || expression.contains("sha256(")) {
            if (expression.contains("md5(")) {
                return evaluateHashFunction(context, expression, "md5");
            } else if (expression.contains("sha1(")) {
                return evaluateHashFunction(context, expression, "sha1");
            } else if (expression.contains("sha256(")) {
                return evaluateHashFunction(context, expression, "sha256");
            }
        } else if (expression.contains("base64(")) {
            return evaluateBase64Function(context, expression);
        } else if (expression.contains("header(")) {
            return evaluateHeaderFunction(context, expression);
        } else if (expression.contains("substr(")) {
            return evaluateSubstrFunction(context, expression);
        } else if (expression.contains("ignoreCase(")) {
            return evaluateIgnoreCaseFunction(context, expression);
        } else if (expression.contains("toLowerCase(") || expression.contains("to_lower(") || expression.contains("tolower(")) {
            return evaluateLowerCaseFunction(context, expression);
        } else if (expression.contains("toUpperCase(") || expression.contains("to_upper(") || expression.contains("toupper(")) {
            return evaluateUpperCaseFunction(context, expression);
        } else if (expression.contains("len(")) {
            return evaluateLengthFunction(context, expression);
        }

        // 评估比较表达式
        return evaluateComparisonExpression(context, expression);
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
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(fieldValue.toString());
            return matcher.find();
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("正则表达式错误: " + patternStr + ", " + e.getMessage());
            }
            return false;
        }
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

        // 添加响应相关的核心字段
        String bodyContent = response.getTextStr();
        responseMap.put("body", bodyContent);
        responseMap.put("body_string", bodyContent); // 别名，兼容性
        responseMap.put("status", status);
        responseMap.put("status_code", status); // 别名，兼容性

        // 设置Content-Type和Content-Length
        String contentType = getResponseHeader(response.getHeaderFields(), "Content-Type");
        responseMap.put("content_type", contentType);

        int contentLength = response.getContentLength();
        responseMap.put("content_length", contentLength);

        // 设置头部信息
        Map<String, List<String>> headers = response.getHeaderFields();
        responseMap.put("headers", headers);
        responseMap.put("all_headers", formatAllHeaders(headers)); // 格式化的所有头

        // 原始响应
        responseMap.put("raw", bodyContent);

        // 响应时间
        long responseTime = response.getResponseTime();
        responseMap.put("time", responseTime);
        responseMap.put("latency", responseTime); // 别名，兼容性

        // 添加常见头部作为响应对象的直接属性，方便访问
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey() != null) {
                String headerValue = String.join(", ", header.getValue());
                responseMap.put(header.getKey().toLowerCase().replace("-", "_"), headerValue);
            }
        }

        context.put("response", responseMap);

        // 添加请求相关的变量
        Map<String, Object> requestMap = new HashMap<>();
        try {
            URL url = response.getURL();
            String urlString = url.toString();
            requestMap.put("url", urlString);
            requestMap.put("path", url.getPath());
            requestMap.put("host", url.getHost());
            requestMap.put("scheme", url.getProtocol());
            int port = url.getPort();
            requestMap.put("port", port == -1 ? url.getDefaultPort() : port);

            // 添加查询参数
            String query = url.getQuery();
            if (query != null && !query.isEmpty()) {
                requestMap.put("query", query);

                // 解析查询参数
                Map<String, String> queryParams = new HashMap<>();
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        queryParams.put(
                                pair.substring(0, idx),
                                idx < pair.length() - 1 ? pair.substring(idx + 1) : ""
                        );
                    }
                }
                requestMap.put("query_params", queryParams);
            }
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("解析URL失败: " + e.getMessage());
            }
        }

        context.put("request", requestMap);

        return context;
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

    /**
     * 从响应头集合中获取指定头的值
     * @param headers 响应头集合
     * @param headerName 头名称
     * @return 头值或空字符串
     */
    private String getResponseHeader(Map<String, List<String>> headers, String headerName) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (headerName != null && entry.getKey() != null && headerName.equalsIgnoreCase(entry.getKey())) {
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
     * 格式化所有HTTP头为单个字符串
     * @param headers HTTP头集合
     * @return 格式化的HTTP头字符串
     */
    private String formatAllHeaders(Map<String, List<String>> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            if (headerName != null) { // 跳过null键（状态行）
                List<String> values = entry.getValue();
                for (String value : values) {
                    sb.append(headerName).append(": ").append(value).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 评估哈希函数 (md5/sha1/sha256)
     * @param context DSL上下文
     * @param expression 表达式
     * @param hashType 哈希类型
     * @return 评估结果
     */
    private boolean evaluateHashFunction(Map<String, Object> context, String expression, String hashType) {
        try {
            // 解析函数参数
            int startIndex = expression.indexOf(hashType + "(") + hashType.length() + 1;
            int endIndex = findClosingBracket(expression, startIndex);

            if (endIndex == -1) {
                System.err.println("无法解析哈希函数: " + expression);
                return false;
            }

            String functionArgs = expression.substring(startIndex, endIndex);
            String[] args = splitFunctionArgs(functionArgs);

            if (args.length < 1) {
                System.err.println("哈希函数参数不足: " + expression);
                return false;
            }

            // 解析第一个参数
            String input = resolveStringValue(context, args[0].trim());
            if (input == null) {
                return false;
            }

            // 计算哈希值
            String hashValue;
            MessageDigest digest;

            switch (hashType.toLowerCase()) {
                case "md5":
                    digest = MessageDigest.getInstance("MD5");
                    break;
                case "sha1":
                    digest = MessageDigest.getInstance("SHA-1");
                    break;
                case "sha256":
                    digest = MessageDigest.getInstance("SHA-256");
                    break;
                default:
                    System.err.println("不支持的哈希类型: " + hashType);
                    return false;
            }

            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            hashValue = sb.toString();

            // 如果有变量名作为第二个参数，将结果存储到上下文中
            if (args.length > 1) {
                String varName = args[1].trim();
                varName = stripQuotes(varName); // 移除可能的引号
                context.put(varName, hashValue);
            }

            return true;
        } catch (Exception e) {
            System.err.println("评估哈希函数时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估Base64编码函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateBase64Function(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("base64(");
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

        // 计算Base64编码
        String base64Value;
        try {
            base64Value = Base64.getEncoder().encodeToString(
                    innerValue.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("Base64编码失败: " + e.getMessage());
            }
            return false;
        }

        // 将计算结果存入上下文
        Map<String, Object> base64Context = new HashMap<>(context);
        base64Context.put("__base64_result", base64Value);

        // 处理编码后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(base64Context, "__base64_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(base64Context, "__base64_result " + remainingExpr);
        }

        return !base64Value.isEmpty();
    }

    /**
     * 评估header函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateHeaderFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("header(");
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

        // 移除引号
        innerExpr = stripQuotes(innerExpr);

        // 获取头部值 - 使用getResponseHeaderFromContext代替getResponseHeader
        String headerValue = getResponseHeaderFromContext(context, innerExpr);
        if (headerValue == null) {
            return false;
        }

        // 将头部值存入上下文
        Map<String, Object> headerContext = new HashMap<>(context);
        headerContext.put("__header_result", headerValue);

        // 处理头部值的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(headerContext, "__header_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(headerContext, "__header_result " + remainingExpr);
        }

        return !headerValue.isEmpty();
    }

    /**
     * 从上下文中获取响应头部值
     * @param context DSL上下文
     * @param headerName 头部名称
     * @return 头部值
     */
    private String getResponseHeaderFromContext(Map<String, Object> context, String headerName) {
        // 尝试从RESP_HEADERS中获取
        Object headers = context.get("RESP_HEADERS");
        if (headers instanceof Map) {
            Map<?, ?> headersMap = (Map<?, ?>) headers;
            // 不区分大小写查找头部
            for (Map.Entry<?, ?> entry : headersMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(headerName)) {
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        List<?> values = (List<?>) value;
                        if (!values.isEmpty()) {
                            return values.get(0).toString();
                        }
                    } else if (value != null) {
                        return value.toString();
                    }
                }
            }
        }

        // 如果找不到，返回null
        return null;
    }

    /**
     * 评估substr函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateSubstrFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("substr(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            if (scanConfig.isDebug()) {
                System.err.println("substr函数参数不足: " + argsStr);
            }
            return false;
        }

        // 解析参数
        Object strObj = resolveValue(context, args[0].trim());
        if (strObj == null) {
            return false;
        }
        String str = strObj.toString();

        try {
            int startPos = Integer.parseInt(stripQuotes(args[1].trim()));
            String result;

            if (args.length > 2) {
                // 如果有长度参数
                int length = Integer.parseInt(stripQuotes(args[2].trim()));
                if (startPos < 0) {
                    startPos = Math.max(0, str.length() + startPos);
                }
                if (startPos >= str.length()) {
                    result = "";
                } else {
                    int endPos = Math.min(startPos + length, str.length());
                    result = str.substring(startPos, endPos);
                }
            } else {
                // 如果只有起始位置
                if (startPos < 0) {
                    startPos = Math.max(0, str.length() + startPos);
                }
                if (startPos >= str.length()) {
                    result = "";
                } else {
                    result = str.substring(startPos);
                }
            }

            // 将结果存入上下文
            Map<String, Object> substrContext = new HashMap<>(context);
            substrContext.put("__substr_result", result);

            // 处理子字符串的比较
            String remainingExpr = expression.substring(closeBracket + 1).trim();
            if (remainingExpr.startsWith(".")) {
                return evaluateDslExpression(substrContext, "__substr_result" + remainingExpr);
            } else if (!remainingExpr.isEmpty()) {
                return evaluateComparisonExpression(substrContext, "__substr_result " + remainingExpr);
            }

            return !result.isEmpty();

        } catch (NumberFormatException e) {
            if (scanConfig.isDebug()) {
                System.err.println("substr函数解析数字参数失败: " + e.getMessage());
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

        // 解析表达式并忽略大小写
        Map<String, Object> ignoreCaseContext = new HashMap<>(context);
        return evaluateDslExpression(ignoreCaseContext, innerExpr);
    }

    /**
     * 评估toLowerCase/to_lower函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateLowerCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex;
        if (expression.contains("toLowerCase(")) {
            funcIndex = expression.indexOf("toLowerCase(");
        } else if (expression.contains("to_lower(")) {
            funcIndex = expression.indexOf("to_lower(");
        } else if (expression.contains("tolower(")) {
            funcIndex = expression.indexOf("tolower(");
        } else {
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

        // 转换为小写
        String lowerCaseValue = innerValue.toString().toLowerCase();

        // 将转换结果存入上下文
        Map<String, Object> lowerCaseContext = new HashMap<>(context);
        lowerCaseContext.put("__lower_case_result", lowerCaseValue);

        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(lowerCaseContext, "__lower_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(lowerCaseContext, "__lower_case_result " + remainingExpr);
        }

        return !lowerCaseValue.isEmpty();
    }

    /**
     * 评估toUpperCase/to_upper函数
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateUpperCaseFunction(Map<String, Object> context, String expression) {
        int funcIndex;
        if (expression.contains("toUpperCase(")) {
            funcIndex = expression.indexOf("toUpperCase(");
        } else if (expression.contains("to_upper(")) {
            funcIndex = expression.indexOf("to_upper(");
        } else if (expression.contains("toupper(")) {
            funcIndex = expression.indexOf("toupper(");
        } else {
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

        // 转换为大写
        String upperCaseValue = innerValue.toString().toUpperCase();

        // 将转换结果存入上下文
        Map<String, Object> upperCaseContext = new HashMap<>(context);
        upperCaseContext.put("__upper_case_result", upperCaseValue);

        // 处理转换后的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return evaluateDslExpression(upperCaseContext, "__upper_case_result" + remainingExpr);
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(upperCaseContext, "__upper_case_result " + remainingExpr);
        }

        return !upperCaseValue.isEmpty();
    }

    /**
     * 评估length函数
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

        // 计算长度
        int length = innerValue.toString().length();

        // 将长度结果存入上下文
        Map<String, Object> lengthContext = new HashMap<>(context);
        lengthContext.put("__length_result", length);

        // 处理长度的比较
        String remainingExpr = expression.substring(closeBracket + 1).trim();
        if (remainingExpr.startsWith(".")) {
            return false; // 长度是一个整数，不能再访问属性
        } else if (!remainingExpr.isEmpty()) {
            return evaluateComparisonExpression(lengthContext, "__length_result " + remainingExpr);
        }

        return length > 0;
    }

    /**
     * 去除字符串两端的引号
     * @param str 输入字符串
     * @return 去除引号后的字符串
     */
    private String stripQuotes(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }

        if ((str.startsWith("\"") && str.endsWith("\"")) ||
                (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1);
        }

        return str;
    }

    /**
     * 分割函数参数，考虑引号内的逗号
     * @param argsStr 参数字符串
     * @return 分割后的参数数组
     */
    private String[] splitFunctionArgs(String argsStr) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = '"';
        int nestedBrackets = 0;

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuote = false;
                }
                currentArg.append(c);
            } else if (c == '(' && !inQuote) {
                nestedBrackets++;
                currentArg.append(c);
            } else if (c == ')' && !inQuote) {
                nestedBrackets--;
                currentArg.append(c);
            } else if (c == ',' && !inQuote && nestedBrackets == 0) {
                args.add(currentArg.toString());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }

        return args.toArray(new String[0]);
    }

    /**
     * 评估contains_all函数，检查字段是否包含所有指定的值
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateContainsAllFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("contains_all(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            if (scanConfig.isDebug()) {
                System.err.println("contains_all函数需要至少两个参数: " + expression);
            }
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 检查是否包含所有值
        for (int i = 1; i < args.length; i++) {
            Object valueObj = resolveValue(context, args[i].trim());
            if (valueObj == null) {
                return false;
            }

            String value = valueObj.toString();

            // 如果有任何一个值不包含，则返回false
            if (!sourceStr.contains(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 评估contains_any函数，检查字段是否包含任意一个指定的值
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateContainsAnyFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("contains_any(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length < 2) {
            if (scanConfig.isDebug()) {
                System.err.println("contains_any函数需要至少两个参数: " + expression);
            }
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 检查是否包含任意一个值
        for (int i = 1; i < args.length; i++) {
            Object valueObj = resolveValue(context, args[i].trim());
            if (valueObj == null) {
                continue;
            }

            String value = valueObj.toString();

            // 如果包含任意一个值，则返回true
            if (sourceStr.contains(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 评估startswith函数，检查字段是否以指定字符串开头
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateStartsWithFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("startswith(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 2) {
            if (scanConfig.isDebug()) {
                System.err.println("startswith函数需要两个参数: " + expression);
            }
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 解析前缀
        Object prefixObj = resolveValue(context, args[1].trim());
        if (prefixObj == null) {
            return false;
        }
        String prefix = prefixObj.toString();

        return sourceStr.startsWith(prefix);
    }

    /**
     * 评估endswith函数，检查字段是否以指定字符串结尾
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateEndsWithFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("endswith(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 2) {
            if (scanConfig.isDebug()) {
                System.err.println("endswith函数需要两个参数: " + expression);
            }
            return false;
        }

        // 解析目标字符串
        Object source = resolveValue(context, args[0].trim());
        if (source == null) {
            return false;
        }
        String sourceStr = source.toString();

        // 解析后缀
        Object suffixObj = resolveValue(context, args[1].trim());
        if (suffixObj == null) {
            return false;
        }
        String suffix = suffixObj.toString();

        return sourceStr.endsWith(suffix);
    }

    /**
     * 评估base64_decode函数，将base64编码的字符串解码
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateBase64DecodeFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("base64_decode(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String innerExpr = expression.substring(openBracket + 1, closeBracket).trim();

        // 解析内部表达式的值
        Object innerValue = resolveValue(context, innerExpr);
        if (innerValue == null) {
            return false;
        }

        try {
            // 解码base64
            String base64Str = innerValue.toString();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            String decodedStr = new String(decodedBytes, StandardCharsets.UTF_8);

            // 创建一个新上下文，包含解码后的结果
            Map<String, Object> decodedContext = new HashMap<>(context);
            decodedContext.put("__base64_decoded_result", decodedStr);

            // 处理后续表达式
            String remainingExpr = expression.substring(closeBracket + 1).trim();
            if (remainingExpr.startsWith(".")) {
                return evaluateDslExpression(decodedContext, "__base64_decoded_result" + remainingExpr);
            } else if (!remainingExpr.isEmpty()) {
                return evaluateComparisonExpression(decodedContext, "__base64_decoded_result " + remainingExpr);
            }

            // 如果没有后续操作，返回解码结果（非空为真）
            return !decodedStr.isEmpty();
        } catch (Exception e) {
            if (scanConfig.isDebug()) {
                System.err.println("base64解码失败: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 评估version_compare函数，比较两个版本号
     * @param context DSL上下文
     * @param expression 表达式
     * @return 评估结果
     */
    private boolean evaluateVersionCompareFunction(Map<String, Object> context, String expression) {
        int funcIndex = expression.indexOf("version_compare(");
        if (funcIndex < 0) {
            return false;
        }

        // 提取括号中的内容
        int openBracket = expression.indexOf('(', funcIndex);
        int closeBracket = findClosingBracket(expression, openBracket);
        if (openBracket < 0 || closeBracket < 0) {
            return false;
        }

        // 获取参数
        String argsStr = expression.substring(openBracket + 1, closeBracket).trim();
        String[] args = splitFunctionArgs(argsStr);

        if (args.length != 3) {
            if (scanConfig.isDebug()) {
                System.err.println("version_compare函数需要三个参数: " + expression);
            }
            return false;
        }

        // 解析两个版本号和比较操作符
        Object version1Obj = resolveValue(context, args[0].trim());
        Object version2Obj = resolveValue(context, args[1].trim());
        String operator = stripQuotes(args[2].trim());

        if (version1Obj == null || version2Obj == null) {
            return false;
        }

        String version1 = version1Obj.toString();
        String version2 = version2Obj.toString();

        // 分割版本号为组件
        String[] v1Components = version1.split("\\.");
        String[] v2Components = version2.split("\\.");

        // 比较每个组件
        int maxLength = Math.max(v1Components.length, v2Components.length);
        for (int i = 0; i < maxLength; i++) {
            int v1Comp = (i < v1Components.length) ? parseInt(v1Components[i]) : 0;
            int v2Comp = (i < v2Components.length) ? parseInt(v2Components[i]) : 0;

            if (v1Comp < v2Comp) {
                return operator.equals("<") || operator.equals("<=") || operator.equals("!=");
            } else if (v1Comp > v2Comp) {
                return operator.equals(">") || operator.equals(">=") || operator.equals("!=");
            }
        }

        // 如果版本号完全相等
        return operator.equals("==") || operator.equals("<=") || operator.equals(">=");
    }

    /**
     * 辅助方法：尝试将字符串解析为整数，失败则返回0
     */
    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析字符串值
     * @param context DSL上下文
     * @param reference 引用或字面值
     * @return 解析后的字符串值
     */
    private String resolveStringValue(Map<String, Object> context, String reference) {
        Object value = resolveValue(context, reference);
        return value != null ? value.toString() : null;
    }
}