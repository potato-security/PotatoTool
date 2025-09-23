package com.potato.potatotool.utils.okHttpVsURLConnectionTest;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * HTTP客户端高并发性能测试
 * 专门针对千万级请求场景进行性能对比
 * @author Benchmark
 */
public class HighConcurrencyBenchmark {

    private static final NumberFormat timeFormat = new DecimalFormat("#0.000");
    private static final NumberFormat rpsFormat = new DecimalFormat("#,##0.0");
    
    // 测试配置参数
    private static final int WARM_UP_REQUESTS = 50;
    private static final int CONNECT_TIMEOUT = 20;
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
        private long memoryUsed = 0;    // 内存使用量（字节）
        private double cpuUsage = 0.0;  // CPU使用率变化（百分比）
        private ResourceSampler resourceSampler; // 资源采样器
        
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
        
        public void setSampledResourceInfo(ResourceSampler sampler) {
            this.resourceSampler = sampler;
        }
        
        public ResourceSampler getSampledResourceInfo() {
            return this.resourceSampler;
        }
        
        public void setMemoryUsed(long memoryUsed) {
            this.memoryUsed = memoryUsed;
        }
        
        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        
        public long getMemoryUsed() {
            return memoryUsed;
        }
        
        public double getCpuUsage() {
            return cpuUsage;
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
        
        public double getMemoryPerRequest() {
            return successCount > 0 ? (double)memoryUsed / successCount : 0;
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
            
            // 添加资源使用信息
            if (memoryUsed > 0) {
                sb.append("内存使用: ").append(String.format("%.2f", memoryUsed/(1024.0*1024.0))).append("MB (")
                  .append(String.format("%.2f", getMemoryPerRequest()/1024.0)).append("KB/请求)\n");
            }
            
            if (cpuUsage > 0) {
                sb.append("CPU使用率: ").append(String.format("%.1f", cpuUsage)).append("%\n");
            }
            
            // 添加采样资源信息
            if (resourceSampler != null) {
                sb.append("\n资源监控采样结果:\n");
                sb.append("  采样次数: ").append(resourceSampler.getSampleCount())
                  .append(" (每").append(resourceSampler.getSamplingIntervalMs()).append("ms一次, 总计")
                  .append(String.format("%.1f", resourceSampler.getDuration() / 1000.0)).append("秒)\n");
                sb.append("  平均内存: ").append(String.format("%.2f", resourceSampler.getAverageMemory())).append("MB")
                  .append(" (峰值: ").append(String.format("%.2f", resourceSampler.getMaxMemory())).append("MB)\n");
                sb.append("  平均CPU: ").append(String.format("%.1f", resourceSampler.getAverageCpu())).append("%")
                  .append(" (峰值: ").append(String.format("%.1f", resourceSampler.getMaxCpu())).append("%)\n");
                sb.append("  平均线程数: ").append(String.format("%.1f", resourceSampler.getAverageThreadCount())).append("\n");
            }
            
            if (!responseTimes.isEmpty()) {
                sb.append("\n响应时间统计:\n");
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
     * 资源监控采样类，用于定期采集JVM和线程资源使用情况
     */
    private static class ResourceSampler {
        private final long samplingIntervalMs; // 采样间隔(毫秒)
        private final List<Long> memorySamples = new ArrayList<>();
        private final List<Double> cpuSamples = new ArrayList<>();
        private final List<Long> threadCountSamples = new ArrayList<>();
        private final List<Long> threadCpuTimeSamples = new ArrayList<>();
        
        private volatile boolean running = false;
        private Thread samplerThread;
        private long startTime;
        private long endTime;
        
        public ResourceSampler(long samplingIntervalMs) {
            this.samplingIntervalMs = samplingIntervalMs;
        }
        
        public long getSamplingIntervalMs() {
            return samplingIntervalMs;
        }
        
        public void start() {
            if (running) return;
            
            running = true;
            startTime = System.currentTimeMillis();
            memorySamples.clear();
            cpuSamples.clear();
            threadCountSamples.clear();
            threadCpuTimeSamples.clear();
            
            samplerThread = new Thread(() -> {
                try {
                    com.sun.management.OperatingSystemMXBean osBean = 
                        (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                    java.lang.management.ThreadMXBean threadBean = 
                        java.lang.management.ManagementFactory.getThreadMXBean();
                    
                    while (running) {
                        // 采集JVM内存使用
                        Runtime runtime = Runtime.getRuntime();
                        memorySamples.add(runtime.totalMemory() - runtime.freeMemory());
                        
                        // 采集CPU使用率
                        cpuSamples.add(osBean.getProcessCpuLoad() * 100.0);
                        
                        // 采集线程数
                        threadCountSamples.add((long) threadBean.getThreadCount());
                        
                        // 采集所有线程的CPU时间总和
                        long totalThreadCpuTime = 0;
                        if (threadBean.isThreadCpuTimeSupported()) {
                            long[] threadIds = threadBean.getAllThreadIds();
                            for (long id : threadIds) {
                                long cpuTime = threadBean.getThreadCpuTime(id);
                                if (cpuTime != -1) {
                                    totalThreadCpuTime += cpuTime;
                                }
                            }
                            threadCpuTimeSamples.add(totalThreadCpuTime / 1_000_000); // 转换为毫秒
                        }
                        
                        Thread.sleep(samplingIntervalMs);
                    }
                } catch (InterruptedException e) {
                    // 线程被中断，停止采样
                } catch (Exception e) {
                    System.err.println("资源采样异常: " + e.getMessage());
                }
            });
            
            samplerThread.setName("Resource-Sampler");
            samplerThread.setDaemon(true); // 使用守护线程，不阻止JVM退出
            samplerThread.start();
        }
        
        public void stop() {
            if (!running) return;
            
            running = false;
            endTime = System.currentTimeMillis();
            
            if (samplerThread != null) {
                samplerThread.interrupt();
                try {
                    samplerThread.join(1000); // 等待采样线程结束
                } catch (InterruptedException e) {
                    // 忽略
                }
            }
        }
        
        public double getAverageMemory() {
            if (memorySamples.isEmpty()) return 0;
            return memorySamples.stream().mapToLong(v -> v).average().orElse(0) / (1024.0 * 1024.0); // 转换为MB
        }
        
        public double getMaxMemory() {
            if (memorySamples.isEmpty()) return 0;
            return memorySamples.stream().mapToLong(v -> v).max().orElse(0) / (1024.0 * 1024.0); // 转换为MB
        }
        
        public double getAverageCpu() {
            if (cpuSamples.isEmpty()) return 0;
            return cpuSamples.stream().mapToDouble(v -> v).average().orElse(0);
        }
        
        public double getMaxCpu() {
            if (cpuSamples.isEmpty()) return 0;
            return cpuSamples.stream().mapToDouble(v -> v).max().orElse(0);
        }
        
        public double getAverageThreadCount() {
            if (threadCountSamples.isEmpty()) return 0;
            return threadCountSamples.stream().mapToLong(v -> v).average().orElse(0);
        }
        
        public double getAverageThreadCpuTime() {
            if (threadCpuTimeSamples.isEmpty()) return 0;
            return threadCpuTimeSamples.stream().mapToLong(v -> v).average().orElse(0);
        }
        
        public long getSampleCount() {
            return memorySamples.size();
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("资源采样统计:\n");
            sb.append(String.format("- 采样次数: %d (间隔: %d毫秒, 总时间: %.2f秒)\n", 
                    getSampleCount(), samplingIntervalMs, getDuration() / 1000.0));
            sb.append(String.format("- 平均内存使用: %.2f MB (峰值: %.2f MB)\n", 
                    getAverageMemory(), getMaxMemory()));
            sb.append(String.format("- 平均CPU使用率: %.1f%% (峰值: %.1f%%)\n", 
                    getAverageCpu(), getMaxCpu()));
            sb.append(String.format("- 平均线程数: %.1f\n", getAverageThreadCount()));
            if (!threadCpuTimeSamples.isEmpty()) {
                sb.append(String.format("- 平均线程CPU时间: %.2f毫秒\n", getAverageThreadCpuTime()));
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
     * 使用CSV文件中读取的所有不重复URL进行测试
     * @param urls 要测试的URL列表（从CSV文件中读取的不重复URL）
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
            
            // 在测试前进行单独的资源状态重置和测量
            System.out.println("准备OkHttp测试环境...");
            
            // 强制进行垃圾回收，但在正式测试开始前，所以不影响测试结果
            System.out.println("执行垃圾回收...");
            
            // 休眠一小段时间确保系统稳定
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.gc();
            System.gc();
            
            try {
                Thread.sleep(2000); // 等待垃圾回收完成并稳定
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 获取起始CPU状态 - 在测试前
            double startCpu = getCpuUsage();
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            
            System.out.println("开始OkHttp测试...");
            
            // 1. OkHttp测试 - 此时内存和CPU监控都在测试外进行，不会影响测试结果
            BenchmarkResult okHttpResult = benchmarkOkHttp(urls, concurrency);
            
            // 测试后立即测量资源使用，但确保这些测量不会影响测试时间
            System.out.println("OkHttp测试完成，测量资源使用情况...");
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            double okHttpCpuUsage = getCpuUsage() - startCpu;
            
            // 设置资源使用情况
            okHttpResult.setMemoryUsed(okHttpMemoryAfter - okHttpMemoryBefore);
            okHttpResult.setCpuUsage(Math.max(0, okHttpCpuUsage));
            
            System.out.println(okHttpResult);
            writer.println(okHttpResult);
            
            // 添加资源使用情况
            String okHttpResourceSummary = String.format(
                "OkHttp 资源使用: 内存增加: %.2f MB, CPU使用率: %.1f%%",
                (okHttpMemoryAfter - okHttpMemoryBefore) / (1024.0 * 1024.0),
                okHttpCpuUsage > 0 ? okHttpCpuUsage : 0
            );
            System.out.println(okHttpResourceSummary);
            writer.println(okHttpResourceSummary);
            writer.println();
            
            // 在两次测试间确保系统状态重置，并且不计入测试时间
            System.out.println("准备HttpURLConnection测试环境...");
            System.out.println("执行垃圾回收...");
            
            try {
                Thread.sleep(2000); // 确保两次测试之间有足够的冷却时间
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.gc();
            System.gc();
            
            try {
                Thread.sleep(2000); // 等待垃圾回收完成并稳定
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 重置CPU监控 - 在测试前
            startCpu = getCpuUsage();
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            
            System.out.println("开始HttpURLConnection测试...");
            
            // 2. HttpURLConnection测试
            BenchmarkResult urlConnResult = benchmarkHttpURLConnection(urls, concurrency);
            
            // 测试后立即测量资源使用，但确保这些测量不会影响测试时间
            System.out.println("HttpURLConnection测试完成，测量资源使用情况...");
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            double urlConnCpuUsage = getCpuUsage() - startCpu;
            
            // 设置资源使用情况
            urlConnResult.setMemoryUsed(urlConnMemoryAfter - urlConnMemoryBefore);
            urlConnResult.setCpuUsage(Math.max(0, urlConnCpuUsage));
            
            System.out.println(urlConnResult);
            writer.println(urlConnResult);
            
            // 添加资源使用情况
            String urlConnResourceSummary = String.format(
                "HttpURLConnection 资源使用: 内存增加: %.2f MB, CPU使用率: %.1f%%",
                (urlConnMemoryAfter - urlConnMemoryBefore) / (1024.0 * 1024.0),
                urlConnCpuUsage > 0 ? urlConnCpuUsage : 0
            );
            System.out.println(urlConnResourceSummary);
            writer.println(urlConnResourceSummary);
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
            
            // 添加资源使用比较
            double memoryRatio = (okHttpMemoryAfter - okHttpMemoryBefore) / 
                               (double)(urlConnMemoryAfter - urlConnMemoryBefore);
            
            String memoryComparison = String.format(
                "内存使用比较: OkHttp 内存消耗是 HttpURLConnection 的 %.2f 倍",
                memoryRatio
            );
            
            System.out.println(memoryComparison);
            writer.println(memoryComparison);
            
            // 添加每请求资源效率对比
            double okHttpMemoryPerRequest = (okHttpMemoryAfter - okHttpMemoryBefore) / 
                                         (double)Math.max(1, okHttpResult.successCount);
            double urlConnMemoryPerRequest = (urlConnMemoryAfter - urlConnMemoryBefore) / 
                                         (double)Math.max(1, urlConnResult.successCount);
            
            String efficiencyComparison = String.format(
                "每请求内存效率: OkHttp: %.2f KB/请求, HttpURLConnection: %.2f KB/请求",
                okHttpMemoryPerRequest / 1024.0,
                urlConnMemoryPerRequest / 1024.0
            );
            
            System.out.println(efficiencyComparison);
            writer.println(efficiencyComparison);
            
            // 添加详细的资源采样比较
            if (okHttpResult.getSampledResourceInfo() != null && urlConnResult.getSampledResourceInfo() != null) {
                ResourceSampler okHttpSampler = okHttpResult.getSampledResourceInfo();
                ResourceSampler urlConnSampler = urlConnResult.getSampledResourceInfo();
                
                writer.println("\n========== 资源采样详细对比 ==========");
                
                // 内存使用对比
                writer.println(String.format("内存使用 (MB):\n  OkHttp     - 平均: %.2f, 峰值: %.2f", 
                    okHttpSampler.getAverageMemory(), okHttpSampler.getMaxMemory()));
                writer.println(String.format("  HttpURLConn - 平均: %.2f, 峰值: %.2f", 
                    urlConnSampler.getAverageMemory(), urlConnSampler.getMaxMemory()));
                
                // CPU使用对比
                writer.println(String.format("CPU使用率 (百分比):\n  OkHttp     - 平均: %.1f, 峰值: %.1f", 
                    okHttpSampler.getAverageCpu(), okHttpSampler.getMaxCpu()));
                writer.println(String.format("  HttpURLConn - 平均: %.1f, 峰值: %.1f", 
                    urlConnSampler.getAverageCpu(), urlConnSampler.getMaxCpu()));
                
                // 线程使用对比
                writer.println(String.format("线程数:\n  OkHttp     - 平均: %.1f", 
                    okHttpSampler.getAverageThreadCount()));
                writer.println(String.format("  HttpURLConn - 平均: %.1f", 
                    urlConnSampler.getAverageThreadCount()));
                
                // 每请求资源效率对比（考虑成功请求）
                writer.println(String.format("每请求内存效率 (KB/请求):\n  OkHttp     - %.2f", 
                    okHttpSampler.getAverageMemory() * 1024 / okHttpResult.successCount));
                writer.println(String.format("  HttpURLConn - %.2f", 
                    urlConnSampler.getAverageMemory() * 1024 / urlConnResult.successCount));
                
                // 采样完整度对比
                writer.println(String.format("监控采样次数:\n  OkHttp     - %d (监控时长: %.1f秒)", 
                    okHttpSampler.getSampleCount(), okHttpSampler.getDuration() / 1000.0));
                writer.println(String.format("  HttpURLConn - %d (监控时长: %.1f秒)", 
                    urlConnSampler.getSampleCount(), urlConnSampler.getDuration() / 1000.0));
                
                // 吞吐量与资源关系
                double okHttpMemoryEfficiency = okHttpResult.getRps() / okHttpSampler.getAverageMemory();
                double urlConnMemoryEfficiency = urlConnResult.getRps() / urlConnSampler.getAverageMemory();
                
                writer.println(String.format("吞吐量/内存比 (RPS/MB):\n  OkHttp     - %.2f", okHttpMemoryEfficiency));
                writer.println(String.format("  HttpURLConn - %.2f", urlConnMemoryEfficiency));
                
                writer.println(String.format("结论: %s 在相同内存条件下每MB内存的吞吐量是 %s 的 %.2f 倍",
                    okHttpMemoryEfficiency > urlConnMemoryEfficiency ? "OkHttp" : "HttpURLConnection",
                    okHttpMemoryEfficiency > urlConnMemoryEfficiency ? "HttpURLConnection" : "OkHttp",
                    Math.max(okHttpMemoryEfficiency, urlConnMemoryEfficiency) / 
                    Math.min(okHttpMemoryEfficiency, urlConnMemoryEfficiency)));
            }
            
            // 添加有关公平比较的说明
            String fairnessNote = "注意: 为确保公平比较，OkHttp的配置已调整为与HttpURLConnection类似的并发模型:\n" +
                    "- 连接池大小设置为与并发线程数相同 (" + concurrency + ")\n" +
                    "- 每个主机的最大请求数也设置为与并发线程数相同\n" +
                    "- 这种配置确保两个客户端在相同条件下比较性能";
            
            System.out.println("\n" + fairnessNote);
            writer.println("\n" + fairnessNote);
        }
        
        System.out.println("测试结果已保存到: " + resultFile);
    }
    
    /**
     * OkHttp批量URL测试 - 实时进度和内容检测版本
     * 适用于成千上百万条网络请求的漏洞扫描场景
     */
    private static BenchmarkResult benchmarkOkHttp(List<String> urls, int concurrentThreads) {
        System.out.println("开始OkHttp大规模扫描测试: " + urls.size() + "个目标, " + concurrentThreads + "线程");
        
        // 进行资源监控
        long startMemory = getCurrentMemoryUsage();
        double startCpu = getCpuUsage();
        
        // 创建资源采样器，设置1秒采样一次
        ResourceSampler resourceSampler = new ResourceSampler(1000);
        resourceSampler.start();
        System.out.println("已启动实时资源监控，每秒采样一次");
        
        // 创建优化的OkHttpClient配置对象
        OkHttpRequestObj clientConfigObj = new OkHttpRequestObj()
                .setTimeOut(CONNECT_TIMEOUT)
                .setRetries(1)
                .setFollowRedirects(true)
                .setMethod("GET");  // 使用GET请求以便获取内容
                
        // 设置连接池和调度器 - 与HttpURLConnection保持一致的并发级别
        // HttpURLConnection默认为每个目标主机创建单独的连接，最大连接数等于线程数
        // 因此将OkHttp配置为最大连接数等于线程数，确保公平比较
        ConnectionPool connectionPool = new ConnectionPool(
                concurrentThreads, // 最大连接数与线程数相同，确保公平比较
                5, TimeUnit.MINUTES); // 保持连接5分钟
        
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(concurrentThreads); // 最大并发请求数与线程数相同
        dispatcher.setMaxRequestsPerHost(concurrentThreads); // 每个主机最大并发与线程数相同
        
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
            
            // 添加轻量级资源监控，降低监控频率，仅在大量完成时监控
            // 每完成10%的请求或每1000个请求进行一次监控
            if (completed > 0 && (completed % 1000 == 0 || completed * 10 / urls.size() > (completed-1) * 10 / urls.size())) {
                long currentMemory = getLightMemoryUsage(); // 使用轻量版内存监控
                System.out.println("OkHttp资源监控 - 当前内存使用: " + 
                    String.format("%.2f", currentMemory / (1024.0 * 1024.0)) + "MB");
            }
        }, 5, 10, TimeUnit.SECONDS); // 增加监控间隔到10秒，降低干扰
        
        try {
            // 准备请求对象列表
            List<OkHttpRequestObj> requestObjs = new ArrayList<>();
            for (String url : urls) {
                OkHttpRequestObj requestObj = new OkHttpRequestObj()
                        .setUrl(url)
                        .setMethod("GET")  // 使用GET请求以便获取内容
                        .setTimeOut(CONNECT_TIMEOUT)
                        .setRetries(1)
                        .setFollowRedirects(true);
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
            
            // 停止资源采样
            resourceSampler.stop();
        }
        
        long endTime = System.currentTimeMillis();
        
        // 获取最终资源使用情况
        long endMemory = getCurrentMemoryUsage();
        double endCpu = getCpuUsage();
        
        // 打印资源使用情况
        System.out.println("OkHttp资源使用 - 内存: " + 
                String.format("%.2f MB", (endMemory - startMemory) / (1024.0 * 1024.0)) + 
                ", CPU使用率变化: " + String.format("%.1f%%", Math.max(0, endCpu - startCpu)));
        
        // 打印资源采样结果
        System.out.println("OkHttp" + resourceSampler.toString());
        
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
        
        // 设置资源使用情况 - 使用采样的平均值而不是简单的差值
        result.setMemoryUsed((long)(resourceSampler.getAverageMemory() * 1024 * 1024)); // 转回字节
        result.setCpuUsage(resourceSampler.getAverageCpu());
        
        // 在结果中添加资源采样数据
        result.setSampledResourceInfo(resourceSampler);
        
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
        
        // 进行资源监控
        long startMemory = getCurrentMemoryUsage();
        double startCpu = getCpuUsage();
        
        // 创建资源采样器，设置1秒采样一次
        ResourceSampler resourceSampler = new ResourceSampler(1000);
        resourceSampler.start();
        System.out.println("已启动实时资源监控，每秒采样一次");
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch latch = new CountDownLatch(urls.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger dockerCount = new AtomicInteger(0); // 计数包含"docker"的响应
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
            
            // 添加轻量级资源监控，降低监控频率
            // 每完成10%的请求或每1000个请求进行一次监控
            if (processed > 0 && (processed % 1000 == 0 || processed * 10 / urls.size() > (processed-1) * 10 / urls.size())) {
                long currentMemory = getLightMemoryUsage(); // 使用轻量版内存监控
                System.out.println("HttpURLConnection资源监控 - 当前内存使用: " + 
                    String.format("%.2f", currentMemory / (1024.0 * 1024.0)) + "MB");
            }
            
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
        }, 5, 10, TimeUnit.SECONDS); // 每10秒报告一次进度，降低干扰
        
        long startTime = System.currentTimeMillis();
        
        for (String url : urls) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    RequestObj requestObj = new RequestObj()
                            .setUrl(url)
                            .setMethod("GET")  // 使用GET请求以获取完整内容
                            .setTimeOut(CONNECT_TIMEOUT); // 设置连接超时
                    
                    CustomHttpResponse response = null;
                    try {
                        response = RequestUtils.requests(requestObj);
                        long requestEnd = System.currentTimeMillis();
                        
                        if (response != null) {
                            int responseCode = response.getResponseCode();
                            // 修改：检查响应码范围与OkHttp保持一致
                            if (responseCode >= 200 && responseCode < 400) {
                                // 提前获取数据到内存缓存
                                byte[] data = response.getByteArray();
                                totalBytes.addAndGet(data != null ? data.length : 0);
                                
                                // 添加：检查响应内容是否包含"docker"，与OkHttp保持一致
                                String content = response.getTextStr();
                                if (content != null && content.toLowerCase().contains("docker")) {
                                    System.out.println("发现docker关键字: " + url);
                                    dockerCount.incrementAndGet();
                                }
                                
                                successCount.incrementAndGet();
                                responseTimes.add(requestEnd - requestStart);
                            }
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // 记录详细的错误信息，帮助诊断
                        failCount.incrementAndGet();
                        // 限制错误日志数量
                        if (failCount.get() % 100 == 0) {
                            System.err.println("请求异常: " + url + " - " + e.getMessage());
                        }
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
        
        // 停止资源采样
        resourceSampler.stop();
        
        // 获取最终资源使用情况
        long endMemory = getCurrentMemoryUsage();
        double endCpu = getCpuUsage();
        
        // 打印资源使用情况
        System.out.println("HttpURLConnection资源使用 - 内存: " + 
                String.format("%.2f MB", (endMemory - startMemory) / (1024.0 * 1024.0)) + 
                ", CPU使用率变化: " + String.format("%.1f%%", Math.max(0, endCpu - startCpu)));
        
        // 打印资源采样结果
        System.out.println("HttpURLConnection" + resourceSampler.toString());
        
        // 打印包含docker的响应数量，与OkHttp保持一致
        System.out.println("包含'docker'关键字的响应数量: " + dockerCount.get());
        
        BenchmarkResult result = new BenchmarkResult(
            "HttpURLConnection",
            urls.size(),
            concurrentThreads,
            endTime - startTime,
            successCount.get(),
            failCount.get(),
            totalBytes.get()
        );
        
        // 设置资源使用情况 - 使用采样的平均值而不是简单的差值
        result.setMemoryUsed((long)(resourceSampler.getAverageMemory() * 1024 * 1024)); // 转回字节
        result.setCpuUsage(resourceSampler.getAverageCpu());
        
        // 在结果中添加资源采样数据
        result.setSampledResourceInfo(resourceSampler);
        
        responseTimes.forEach(result::addResponseTime);
        
        return result;
    }
    
    /**
     * 预热OkHttp - 使用单独的客户端实例，避免影响测试
     */
    private static void warmupOkHttp(String url, int count) {
        System.out.println("预热OkHttp中 (" + count + "请求)...");
        
        // 为预热创建一个独立的OkHttp客户端实例，确保预热与测试分离
        OkHttpClient warmupClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES))
            .build();
            
        for (int i = 0; i < count; i++) {
            try {
                OkHttpRequestObj requestObj = new OkHttpRequestObj()
                        .setUrl(url + "?warmup=" + i) // 添加参数避免缓存
                        .setMethod("GET")
                        .setTimeOut(CONNECT_TIMEOUT);
                
                OkHttpCustomResponse response = null;
                try {
                    // 使用专用的预热客户端
                    response = OkHttpRequestUtils.requests(requestObj, warmupClient);
                    
                    // 确保读取内容并关闭连接
                    if (response != null) {
                        response.getByteArray();
                        response.disconnect();
                    }
                } catch (Exception e) {
                    // 忽略预热期间的异常
                }
            } catch (Exception e) {
                // 忽略预热异常
            }
        }
        
        // 关闭并清理预热客户端资源
        try {
            warmupClient.dispatcher().executorService().shutdown();
            warmupClient.connectionPool().evictAll();
        } catch (Exception e) {
            // 忽略关闭异常
        }
        
        // 强制垃圾回收，确保预热资源不会影响后续测试
        System.gc();
        
        System.out.println("OkHttp预热完成");
    }
    
    /**
     * 预热HttpURLConnection - 确保预热不会影响测试
     */
    private static void warmupHttpURLConnection(String url, int count) {
        System.out.println("预热HttpURLConnection中 (" + count + "请求)...");
        for (int i = 0; i < count; i++) {
            try {
                RequestObj requestObj = new RequestObj()
                        .setUrl(url + "?warmup=" + i) // 添加参数避免缓存
                        .setMethod("GET")
                        .setTimeOut(CONNECT_TIMEOUT);
                
                CustomHttpResponse response = null;
                try {
                    response = RequestUtils.requests(requestObj);
                    
                    // 确保读取内容并关闭连接
                    if (response != null) {
                        response.getByteArray();
                        response.disconnect();
                    }
                } catch (Exception e) {
                    // 忽略预热期间的异常
                }
            } catch (Exception e) {
                // 忽略预热异常
            }
        }
        
        // 强制垃圾回收，确保预热资源不会影响后续测试
        System.gc();
        
        System.out.println("HttpURLConnection预热完成");
    }
    
    /**
     * 生成测试URL列表
     * 不再生成随机URL，完全依赖CSV文件中的URL，如果CSV文件不可用则返回空列表
     * @param count 已废弃参数，保留是为了向后兼容
     * @return URL列表，如果CSV不可用则返回空列表
     */
    public static List<String> generateTestUrls(int count) {
        // 指定CSV文件路径
        String csvFilePath = "/Users/a/Desktop/项目开发/PotatoTool/testData.csv";
        
        // 从CSV文件读取URL
        List<String> urlsFromCsv = readUrlsFromCsv(csvFilePath, -1);
        
        // 如果从CSV读取到URL，则直接返回
        if (!urlsFromCsv.isEmpty()) {
            System.out.println("使用CSV文件中的URL进行测试，共计 " + urlsFromCsv.size() + " 个URL");
            return urlsFromCsv;
        }
        
        // 如果CSV文件读取失败，返回空列表
        System.err.println("警告: CSV文件读取失败或没有有效URL，测试将终止");
        return new ArrayList<>();
    }
    
    /**
     * 从CSV文件读取URL列表
     * @param csvFilePath CSV文件路径
     * @param count 已废弃参数，保留是为了向后兼容
     * @return URL列表
     */
    public static List<String> readUrlsFromCsv(String csvFilePath, int count) {
        List<String> urls = new ArrayList<>();
        Path path = Paths.get(csvFilePath);
        
        System.out.println("尝试从CSV文件读取URL: " + path.toAbsolutePath());
        
        // 检查文件是否存在
        if (!path.toFile().exists()) {
            System.err.println("错误: CSV文件不存在: " + path.toAbsolutePath());
            System.err.println("请确保文件路径正确并且文件存在。");
            return urls; // 返回空列表
        }
        
        // 检查是否是CSV文件
        if (!path.toString().toLowerCase().endsWith(".csv")) {
            System.err.println("警告: 指定的文件可能不是CSV文件: " + path.toAbsolutePath());
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            // 读取第一行标题
            String header = reader.readLine();
            if (header == null) {
                System.err.println("错误: CSV文件为空或格式不正确");
                return urls;
            }
            
            // 检查列数是否足够
            String[] headerColumns = header.split(",");
            if (headerColumns.length < 9) {
                System.err.println("错误: CSV文件列数不足9列，当前只有 " + headerColumns.length + " 列");
                System.err.println("需要从第9列提取URL，请确保CSV文件格式正确");
                return urls;
            }
            
            // 显示第9列的标题，方便确认
            System.out.println("第9列标题(索引8): " + headerColumns[8]);
            
            String line;
            int lineNumber = 1; // 标题行是第1行
            Set<String> uniqueUrls = new HashSet<>(); // 使用Set去重
            int validUrlCount = 0;
            int invalidUrlCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");
                // 提取第9列作为URL (索引为8)
                if (parts.length > 8) {
                    String url = parts[8].trim();
                    // 确保URL格式有效
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        if (uniqueUrls.add(url)) { // 添加到Set并检查是否是新URL
                            validUrlCount++;
                        }
                    } else if (!url.isEmpty()) {
                        invalidUrlCount++;
                        if (invalidUrlCount <= 5) { // 只显示前5个无效URL
                            System.err.println("第 " + lineNumber + " 行无效URL: " + url);
                        }
                    }
                } else {
                    // 行的列数不足
                    System.err.println("警告: 第 " + lineNumber + " 行列数不足 (只有 " + parts.length + " 列)");
                }
            }
            
            if (invalidUrlCount > 5) {
                System.err.println("... 以及其他 " + (invalidUrlCount - 5) + " 个无效URL");
            }
            
            if (uniqueUrls.isEmpty()) {
                System.err.println("错误: 未从CSV中找到有效的URL。请检查文件格式和URL列。");
                return urls;
            }
            
            // 直接使用去重后的URL列表
            System.out.println("从CSV读取了 " + uniqueUrls.size() + " 个有效的唯一URL");
            
            // 转换Set为List
            urls.addAll(uniqueUrls);
            
            // 显示部分URL样例
            if (urls.size() > 0) {
                System.out.println("URL样例:");
                int sampleSize = Math.min(5, urls.size());
                for (int i = 0; i < sampleSize; i++) {
                    System.out.println("  " + (i+1) + ": " + urls.get(i));
                }
            }
            
            System.out.println("最终测试URL数量: " + urls.size());
        } catch (Exception e) {
            System.err.println("从CSV文件读取URL失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return urls;
    }
    
    /**
     * 获取当前内存使用情况 (完整版，包含垃圾回收，仅用于测试前后)
     */
    private static long getCurrentMemoryUsage() {
        // 先进行两次垃圾回收以获得更准确的内存使用情况
        System.gc();
        try {
            Thread.sleep(100); // 等待第一次垃圾回收完成
        } catch (InterruptedException e) {
            // 忽略中断
        }
        System.gc();
        try {
            Thread.sleep(100); // 等待第二次垃圾回收完成
        } catch (InterruptedException e) {
            // 忽略中断
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 获取当前内存使用情况 (轻量版，不进行垃圾回收，用于测试过程中)
     */
    private static long getLightMemoryUsage() {
        // 不进行垃圾回收，直接返回当前内存使用情况
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 获取CPU使用率
     */
    private static double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad() * 100.0; // 返回百分比
        } catch (Exception e) {
            return -1; // 无法获取CPU使用率
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        try {
            System.out.println("======================================");
            System.out.println("    高并发HTTP客户端性能测试");
            System.out.println("======================================");
            System.out.println("将从CSV文件读取URL进行测试: /Users/a/Desktop/项目开发/PotatoTool/testData.csv");
            
            // 测试参数配置
            int concurrency = 50; // 默认并发线程数
            
            // 检查命令行参数
            if (args.length >= 1) {
                try {
                    concurrency = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("无效的并发数参数，使用默认值: " + concurrency);
                }
            }
            
            System.out.println("测试配置: 并发线程数=" + concurrency);
            
            // 读取CSV中的所有有效URL
            List<String> testUrls = generateTestUrls(-1);
            
            if (testUrls.isEmpty()) {
                System.err.println("错误: 无法获取有效的测试URL。请确保CSV文件存在且包含有效URL。");
                System.err.println("测试终止。");
                return;
            }
            
            System.out.println("测试URL数量: " + testUrls.size());
            
            // 运行测试
            runMassiveScanTest(testUrls, concurrency);
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 