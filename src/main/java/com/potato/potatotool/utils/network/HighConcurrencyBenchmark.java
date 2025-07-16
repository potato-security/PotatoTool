package com.potato.potatotool.utils.network;

import com.potato.potatotool.utils.okhttp.OkHttpCustomResponse;
import com.potato.potatotool.utils.okhttp.OkHttpRequestObj;
import com.potato.potatotool.utils.okhttp.OkHttpRequestUtils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.net.URL;
import java.net.HttpURLConnection;
import okhttp3.OkHttpClient;
import okhttp3.Dispatcher;
import okhttp3.ConnectionPool;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HTTP客户端高并发性能测试
 * 专门针对千万级请求场景进行性能对比
 * @author Benchmark
 */
public class HighConcurrencyBenchmark {

    private static final NumberFormat timeFormat = new DecimalFormat("#0.000");
    private static final NumberFormat rpsFormat = new DecimalFormat("#,##0.0");
    
    // 测试配置参数
    private static final int WARM_UP_REQUESTS = 100;
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 20;
    
    // 测试URL列表
    private static final String[] TEST_HOSTS = {
        "httpbin.org", 
        "postman-echo.com",
        "example.com",
        "www.baidu.com"
    };

    /**
     * 检测可用的测试主机
     */
    private static String findAvailableTestHost() {
        for (String host : TEST_HOSTS) {
            try {
                URL url = new URL("https://" + host);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode >= 200 && responseCode < 400) {
                    System.out.println("使用测试主机: " + host);
                    return host;
                }
            } catch (Exception e) {
                // 尝试下一个主机
                System.out.println("主机不可用: " + host + " (" + e.getMessage() + ")");
            }
        }
        
        // 默认返回第一个主机
        System.out.println("警告: 所有测试主机均不可用，使用默认主机: " + TEST_HOSTS[0]);
        return TEST_HOSTS[0];
    }

    /**
     * 测试结果统计类
     */
    private static class BenchmarkResult {
        private final String clientName;
        private final int totalRequests;
        private final int threads;
        private final long totalTime;
        private final int successCount;
        private final int failCount;
        private final long bytesTransferred;
        private final List<Long> responseTimes = new ArrayList<>();
        
        public BenchmarkResult(String clientName, int totalRequests, int threads, long totalTime, 
                              int successCount, int failCount, long bytesTransferred) {
            this.clientName = clientName;
            this.totalRequests = totalRequests;
            this.threads = threads;
            this.totalTime = totalTime;
            this.successCount = successCount;
            this.failCount = failCount;
            this.bytesTransferred = bytesTransferred;
        }
        
        public void addResponseTime(long time) {
            responseTimes.add(time);
        }
        
        public double getRps() {
            return (double) successCount / (totalTime / 1000.0);
        }
        
        public double getAverageResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            return responseTimes.stream().mapToLong(v -> v).average().orElse(0);
        }
        
        public long getP95ResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            Collections.sort(responseTimes);
            int idx = (int) Math.ceil(responseTimes.size() * 0.95) - 1;
            return responseTimes.get(idx);
        }
        
        public long getP99ResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            Collections.sort(responseTimes);
            int idx = (int) Math.ceil(responseTimes.size() * 0.99) - 1;
            return responseTimes.get(idx);
        }
        
        public long getMinResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            return Collections.min(responseTimes);
        }
        
        public long getMaxResponseTime() {
            if (responseTimes.isEmpty()) return 0;
            return Collections.max(responseTimes);
        }
        
        public double getBytesPerSec() {
            return bytesTransferred / (totalTime / 1000.0);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("===== ").append(clientName).append(" 测试结果 =====\n");
            sb.append("总请求数: ").append(totalRequests).append("\n");
            sb.append("线程数: ").append(threads).append("\n");
            sb.append("成功请求: ").append(successCount).append(" (").append(String.format("%.2f", (double)successCount/totalRequests*100)).append("%)\n");
            sb.append("失败请求: ").append(failCount).append(" (").append(String.format("%.2f", (double)failCount/totalRequests*100)).append("%)\n");
            sb.append("总耗时: ").append(totalTime).append("ms (").append(timeFormat.format(totalTime/1000.0)).append("秒)\n");
            sb.append("吞吐量: ").append(rpsFormat.format(getRps())).append(" 请求/秒\n");
            sb.append("数据传输: ").append(String.format("%.2f", bytesTransferred/(1024.0*1024.0))).append("MB (")
                     .append(String.format("%.2f", getBytesPerSec()/(1024.0*1024.0))).append("MB/s)\n");
            
            if (!responseTimes.isEmpty()) {
                sb.append("响应时间统计:\n");
                sb.append("  最小: ").append(getMinResponseTime()).append("ms\n");
                sb.append("  最大: ").append(getMaxResponseTime()).append("ms\n");
                sb.append("  平均: ").append(String.format("%.2f", getAverageResponseTime())).append("ms\n");
                sb.append("  P95: ").append(getP95ResponseTime()).append("ms\n");
                sb.append("  P99: ").append(getP99ResponseTime()).append("ms\n");
            }
            
            return sb.toString();
        }
    }
    
    /**
     * 使用OkHttp进行高并发测试
     */
    public static BenchmarkResult benchmarkOkHttp(String url, int totalRequests, int concurrentThreads) {
        System.out.println("开始OkHttp高并发测试: " + totalRequests + "请求, " + concurrentThreads + "线程");
        
        // 创建OkHttpClient，优化连接池，用于整个测试过程
        OkHttpClient sharedClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(concurrentThreads, 5, TimeUnit.MINUTES))
            .dispatcher(new okhttp3.Dispatcher(Executors.newFixedThreadPool(concurrentThreads)))
            .build();
        
        // 预热
        warmupOkHttp(url, WARM_UP_REQUESTS);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    OkHttpRequestObj requestObj = new OkHttpRequestObj()
                            .setUrl(url)
                            .setMethod("GET")
                            .setTimeOut(CONNECT_TIMEOUT);
                    
                    OkHttpCustomResponse response = null;
                    try {
                        // 使用共享客户端
                        response = OkHttpRequestUtils.requests(requestObj, sharedClient);
                        long requestEnd = System.currentTimeMillis();
                        
                        if (response != null) {
                            byte[] data = response.getByteArray(); // 提前获取数据
                            totalBytes.addAndGet(data != null ? data.length : 0);
                            successCount.incrementAndGet();
                            responseTimes.add(requestEnd - requestStart);
                        } else {
                            failCount.incrementAndGet();
                        }
                    } finally {
                        // 确保响应资源被释放
                        if (response != null) {
                            response.disconnect();
                        }
                        latch.countDown();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        
        // 关闭共享客户端，释放资源
        sharedClient.dispatcher().executorService().shutdown();
        
        BenchmarkResult result = new BenchmarkResult(
            "OkHttp",
            totalRequests,
            concurrentThreads,
            endTime - startTime,
            successCount.get(),
            failCount.get(),
            totalBytes.get()
        );
        
        responseTimes.forEach(result::addResponseTime);
        
        return result;
    }
    
    /**
     * 使用HttpURLConnection进行高并发测试
     */
    public static BenchmarkResult benchmarkHttpURLConnection(String url, int totalRequests, int concurrentThreads) {
        System.out.println("开始HttpURLConnection高并发测试: " + totalRequests + "请求, " + concurrentThreads + "线程");
        
        // 预热
        warmupHttpURLConnection(url, WARM_UP_REQUESTS);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    RequestObj requestObj = new RequestObj()
                            .setUrl(url)
                            .setMethod("GET")
                            .setTimeOut(CONNECT_TIMEOUT);
                    
                    CustomHttpResponse response = null;
                    try {
                        response = RequestUtils.requests(requestObj);
                        long requestEnd = System.currentTimeMillis();
                        
                        if (response != null) {
                            int responseCode = response.getResponseCode();
                            if (responseCode == 200) {
                                byte[] data = response.getByteArray();
                                totalBytes.addAndGet(data != null ? data.length : 0);
                                successCount.incrementAndGet();
                                responseTimes.add(requestEnd - requestStart);
                            } else {
                                failCount.incrementAndGet();
                            }
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        // HttpURLConnection会由响应对象内部管理，通常会自动关闭
                        // 但为了安全起见，我们仍然调用disconnect
                        if (response != null) {
                            response.disconnect();
                        }
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        
        BenchmarkResult result = new BenchmarkResult(
            "HttpURLConnection",
            totalRequests,
            concurrentThreads,
            endTime - startTime,
            successCount.get(),
            failCount.get(),
            totalBytes.get()
        );
        
        responseTimes.forEach(result::addResponseTime);
        
        return result;
    }
    
    /**
     * 进行大规模扫描测试
     * @param urls 要测试的URL列表
     * @param concurrency 并发线程数
     */
    public static void runMassiveScanTest(List<String> urls, int concurrency) throws Exception {
        System.out.println("\n========== 大规模网络扫描性能测试 ==========");
        System.out.println("测试条件: " + urls.size() + "个目标, 并发数: " + concurrency);
        
        // 创建结果保存文件
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String resultFile = "benchmark_results_" + timestamp + ".txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(resultFile))) {
            writer.println("大规模网络扫描性能测试");
            writer.println("测试时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("目标数量: " + urls.size());
            writer.println("并发线程数: " + concurrency);
            writer.println();
            
            // 1. OkHttp测试
            BenchmarkResult okHttpResult = benchmarkOkHttp(urls, concurrency);
            System.out.println(okHttpResult);
            writer.println(okHttpResult);
            writer.println();
            
            // 2. HttpURLConnection测试
            BenchmarkResult urlConnResult = benchmarkHttpURLConnection(urls, concurrency);
            System.out.println(urlConnResult);
            writer.println(urlConnResult);
            writer.println();
            
            // 比较结果
            double rpsRatio = okHttpResult.getRps() / urlConnResult.getRps();
            String fasterClient = rpsRatio > 1 ? "OkHttp" : "HttpURLConnection";
            double speedupFactor = rpsRatio > 1 ? rpsRatio : 1.0/rpsRatio;
            
            String comparison = String.format(
                "结果比较: %s 的吞吐量是 %s 的 %.2f 倍",
                fasterClient,
                fasterClient.equals("OkHttp") ? "HttpURLConnection" : "OkHttp",
                speedupFactor
            );
            
            System.out.println(comparison);
            writer.println(comparison);
        }
        
        System.out.println("测试结果已保存到: " + resultFile);
    }
    
    /**
     * OkHttp批量URL测试 - 实时进度和内容检测版本
     * 适用于成千上百万条网络请求的漏洞扫描场景
     */
    private static BenchmarkResult benchmarkOkHttp(List<String> urls, int concurrentThreads) {
        System.out.println("开始OkHttp大规模扫描测试: " + urls.size() + "个目标, " + concurrentThreads + "线程");
        
        // 创建优化的OkHttpClient配置对象
        OkHttpRequestObj clientConfigObj = new OkHttpRequestObj()
                .setTimeOut(CONNECT_TIMEOUT)
                .setRetries(1)
                .setFollowRedirects(true)
                .setMethod("GET");  // 使用GET请求以便获取内容
                
        // 设置优化的连接池和调度器
        ConnectionPool connectionPool = new ConnectionPool(
                Math.min(concurrentThreads * 2, 200), // 最大空闲连接数
                5, TimeUnit.MINUTES); // 保持连接5分钟
        
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(Math.min(concurrentThreads * 4, 64)); // 最大并发请求数
        dispatcher.setMaxRequestsPerHost(Math.min(concurrentThreads / 2, 10)); // 每个主机最大并发请求数
        
        clientConfigObj.setConnectionPool(connectionPool)
                      .setDispatcher(dispatcher);
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger dockerCount = new AtomicInteger(0); // 计数包含"docker"的响应
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        // 创建进度监控
        AtomicInteger completedCount = new AtomicInteger(0);
        ScheduledExecutorService progressMonitor = Executors.newSingleThreadScheduledExecutor();
        progressMonitor.scheduleAtFixedRate(() -> {
            int completed = completedCount.get();
            System.out.println("OkHttp进度: 已完成 " + completed + "/" + urls.size() + 
                    " (" + (completed * 100 / Math.max(urls.size(), 1)) + "%), 剩余 " + 
                    (urls.size() - completed) + " 个请求");
        }, 1, 5, TimeUnit.SECONDS);
        
        try {
            // 准备请求对象列表
            List<OkHttpRequestObj> requestObjs = new ArrayList<>();
            for (String url : urls) {
                OkHttpRequestObj requestObj = new OkHttpRequestObj()
                        .setUrl(url)
                        .setMethod("GET")  // 使用GET请求以便获取内容
                        .setTimeOut(CONNECT_TIMEOUT);
                requestObjs.add(requestObj);
            }
            
            // 创建结果处理回调
            Consumer<OkHttpRequestUtils.AsyncRequestResult> resultCallback = result -> {
                try {
                    completedCount.incrementAndGet(); // 更新完成计数
                    
                    if (result.isSuccess()) {
                        OkHttpCustomResponse response = result.getResponse();
                        if (response != null) {
                            try {
                                // 获取响应数据
                                byte[] data = response.getByteArray();
                                totalBytes.addAndGet(data != null ? data.length : 0);
                                
                                // 检查响应内容是否包含"docker"
                                String content = response.getTextStr();
                                if (content != null && content.toLowerCase().contains("docker")) {
                                    System.out.println("123 - 发现docker关键字: " + result.getUrl());
                                    dockerCount.incrementAndGet();
                                }
                                
                                // 更新统计信息
                                successCount.incrementAndGet();
                                responseTimes.add(result.getDuration());
                            } finally {
                                // 确保释放响应资源
                                response.disconnect();
                            }
                        } else {
                            failCount.incrementAndGet();
                        }
                    } else {
                        failCount.incrementAndGet();
                        // 限制错误日志数量
                        if (result.getIndex() % 100 == 0) {
                            Exception e = result.getException();
                            String errorMsg = e != null ? e.getMessage() : "未知错误";
                            System.err.println("请求失败 [" + result.getIndex() + "]: " + result.getUrl() + " - " + errorMsg);
                        }
                    }
                } catch (Exception e) {
                    // 确保回调不会抛出异常
                    System.err.println("处理结果异常: " + e.getMessage());
                }
            };
            
            // 使用新的异步请求方法，支持实时处理结果
            CompletableFuture<Void> future = OkHttpRequestUtils.requestsAsyncWithCallback(
                    requestObjs,
                    executor,
                    clientConfigObj,
                    resultCallback,
                    concurrentThreads  // 批处理大小与线程数相同
            );
            
            // 等待所有请求完成或超时
            try {
                future.get(urls.size() * 30L / concurrentThreads, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.out.println("警告: 部分请求超时未完成，已处理 " + completedCount.get() + "/" + urls.size());
            }
            
        } catch (Exception e) {
            System.err.println("OkHttp批量请求异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            progressMonitor.shutdownNow();
            executor.shutdown();
            
            try {
                // 等待线程池关闭，最多等待10秒
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        // 打印包含docker的响应数量
        System.out.println("包含'docker'关键字的响应数量: " + dockerCount.get());
        
        BenchmarkResult result = new BenchmarkResult(
            "OkHttp",
            urls.size(),
            concurrentThreads,
            endTime - startTime,
            successCount.get(),
            failCount.get(),
            totalBytes.get()
        );
        
        responseTimes.forEach(result::addResponseTime);
        
        return result;
    }
    
    /**
     * 创建优化的Dispatcher
     */
    private static Dispatcher createOptimizedDispatcher(int concurrentThreads) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(Math.min(concurrentThreads * 4, 64)); // 最大并发请求数
        dispatcher.setMaxRequestsPerHost(Math.min(concurrentThreads / 2, 10)); // 每个主机最大并发请求数
        return dispatcher;
    }
    
    /**
     * HttpURLConnection批量URL测试
     */
    private static BenchmarkResult benchmarkHttpURLConnection(List<String> urls, int concurrentThreads) {
        System.out.println("开始HttpURLConnection大规模扫描测试: " + urls.size() + "个目标, " + concurrentThreads + "线程");
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(urls.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        // 添加进度监控线程
        ScheduledExecutorService progressMonitor = Executors.newSingleThreadScheduledExecutor();
        AtomicLong lastCount = new AtomicLong(urls.size());
        AtomicInteger stuckCounter = new AtomicInteger(0);
        
        progressMonitor.scheduleAtFixedRate(() -> {
            long currentCount = latch.getCount();
            long processed = urls.size() - currentCount;
            System.out.println("HttpURLConnection进度: 已完成 " + processed + "/" + urls.size() + 
                    " (" + (processed * 100 / urls.size()) + "%), 剩余 " + currentCount + " 个请求");
            
            // 检测是否卡住（连续多次计数不变）
            if (currentCount > 0 && currentCount == lastCount.get()) {
                if (stuckCounter.incrementAndGet() >= 5) { // 连续5次进度不变，可能卡住了
                    System.out.println("警告: 检测到HttpURLConnection可能卡住了，尝试继续...");
                    // 这里不会强制中断，但会记录问题
                }
            } else {
                stuckCounter.set(0);
                lastCount.set(currentCount);
            }
        }, 5, 5, TimeUnit.SECONDS); // 每5秒报告一次进度
        
        long startTime = System.currentTimeMillis();
        
        for (String url : urls) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    RequestObj requestObj = new RequestObj()
                            .setUrl(url)
                            .setMethod("GET")  // 使用HEAD请求减少数据传输
                            .setTimeOut(CONNECT_TIMEOUT); // 设置连接超时
                    
                    CustomHttpResponse response = null;
                    try {
                        response = RequestUtils.requests(requestObj);
                        long requestEnd = System.currentTimeMillis();
                        
                        if (response != null) {
                            // 提前获取数据到内存缓存
                            byte[] data = response.getByteArray();
                            totalBytes.addAndGet(data != null ? data.length : 0);
                            successCount.incrementAndGet();
                            responseTimes.add(requestEnd - requestStart);
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // 记录详细的错误信息，帮助诊断
                        failCount.incrementAndGet();
                    } finally {
                        // 确保响应资源被释放
                        if (response != null) {
                            try {
                                response.disconnect();
                            } catch (Exception e) {
                                // 忽略关闭时的错误
                            }
                        }
                        latch.countDown();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    latch.countDown();
                }
            });
        }
        
        try {
            // 添加超时机制，避免永久等待
            if (!latch.await(urls.size() * 30L / concurrentThreads, TimeUnit.SECONDS)) {
                System.out.println("警告: HttpURLConnection测试超时，强制结束");
            }
        } catch (InterruptedException e) {
            System.out.println("HttpURLConnection测试被中断: " + e.getMessage());
        } finally {
            progressMonitor.shutdownNow(); // 确保进度监控线程关闭
        }
        
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        
        // 尝试强制关闭执行器，避免线程泄漏
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        BenchmarkResult result = new BenchmarkResult(
            "HttpURLConnection",
            urls.size(),
            concurrentThreads,
            endTime - startTime,
            successCount.get(),
            failCount.get(),
            totalBytes.get()
        );
        
        responseTimes.forEach(result::addResponseTime);
        
        return result;
    }
    
    /**
     * 预热OkHttp
     */
    private static void warmupOkHttp(String url, int count) {
        System.out.println("预热OkHttp中...");
        for (int i = 0; i < count; i++) {
            try {
                OkHttpRequestObj requestObj = new OkHttpRequestObj()
                        .setUrl(url)
                        .setMethod("GET")
                        .setTimeOut(CONNECT_TIMEOUT);
                OkHttpRequestUtils.requests(requestObj);
            } catch (Exception e) {
                // 忽略预热期间的异常
            }
        }
    }
    
    /**
     * 预热HttpURLConnection
     */
    private static void warmupHttpURLConnection(String url, int count) {
        System.out.println("预热HttpURLConnection中...");
        for (int i = 0; i < count; i++) {
            try {
                RequestObj requestObj = new RequestObj()
                        .setUrl(url)
                        .setMethod("GET")
                        .setTimeOut(CONNECT_TIMEOUT);
                RequestUtils.requests(requestObj);
            } catch (Exception e) {
                // 忽略预热期间的异常
            }
        }
    }
    
    /**
     * 生成测试URL列表
     */
    public static List<String> generateTestUrls(int count) {
        List<String> urls = new ArrayList<>();
        
        // 找到可用的测试主机
        String availableHost = findAvailableTestHost();
        
        // 备用域名列表，如果主测试主机不可用
        String[] domains = {
            availableHost, "baidu.com", "qq.com", 
            "taobao.com", "jd.com", "github.com", "microsoft.com", "apple.com"
        };
        
        String[] paths = {
            "", "get", "status/200", "ip", "user-agent", "headers", "robots.txt", "favicon.ico"
        };
        
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            String domain = domains[random.nextInt(domains.length)];
            String path = paths[random.nextInt(paths.length)];
            String protocol = random.nextBoolean() ? "http" : "https";
            
            urls.add(String.format("%s://%s/%s", protocol, domain, path));
        }
        
        return urls;
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        try {
            // 测试参数配置
            int totalUrls = 1000; // 设置为10000000实际测试时
            int concurrency = 50;   // 并发线程数
            
            // 生成测试URL
            List<String> testUrls = generateTestUrls(totalUrls);
            
            // 运行测试
            runMassiveScanTest(testUrls, concurrency);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 