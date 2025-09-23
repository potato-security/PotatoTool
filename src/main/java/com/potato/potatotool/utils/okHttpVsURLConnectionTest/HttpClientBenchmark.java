package com.potato.potatotool.utils.okHttpVsURLConnectionTest;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;
import com.potato.potatotool.utils.okhttp.OkHttpCustomResponse;
import com.potato.potatotool.utils.okhttp.OkHttpRequestObj;
import com.potato.potatotool.utils.okhttp.OkHttpRequestUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.ConnectionPool;
import java.util.concurrent.TimeUnit;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP客户端性能对比测试类
 * 对比OkHttp和HttpURLConnection封装实现的性能差异
 * @author Benchmark
 */
public class HttpClientBenchmark {

    // 格式化数字显示
    private static final NumberFormat timeFormat = new DecimalFormat("#0.000");
    private static final NumberFormat sizeFormat = new DecimalFormat("#0.00");
    private static final NumberFormat memoryFormat = new DecimalFormat("#,##0.00");
    private static final NumberFormat percentFormat = new DecimalFormat("#0.0");
    
    // 共享的OkHttpClient实例，用于提高连接复用效率
    private static OkHttpClient createSharedClient(int connectionPoolSize) {
        return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(connectionPoolSize, 5, TimeUnit.MINUTES))
            .build();
    }
    
    // 初始化一个默认的共享客户端，但在测试中会根据实际并发度创建新的客户端
    private static OkHttpClient sharedClient = createSharedClient(100);

    // 测试基础URL - 添加多个备选服务
    private static final String[] TEST_BASE_URLS = {
        "https://postman-echo.com",
        "https://httpbin.org",
        "https://www.example.com"
    };
    private static final String[] TEST_DOWNLOAD_URLS = {
        "https://speed.cloudflare.com/",
        "https://www.example.com/"
    };
    private static final String[] TEST_LARGE_FILE_URLS = {
        "https://filesamples.com/samples/document/pdf/sample3.pdf", // PDF文件样本
        "https://github.com/mozilla/pdf.js/raw/master/test/pdfs/tracemonkey.pdf", // Mozilla PDF测试文件
        "https://cdn.kernel.org/pub/linux/kernel/v6.x/linux-6.5.tar.sign" // 小文件
    };
    
    // 选择可用的测试URL
    private static String selectWorkingTestUrl(String[] urls, String path) {
        for (String baseUrl : urls) {
            String fullUrl = path.isEmpty() ? baseUrl : baseUrl + path;
            try {
                URL url = new URL(fullUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode >= 200 && responseCode < 400) {
                    System.out.println("使用测试URL: " + fullUrl);
                    return fullUrl;
                }
            } catch (Exception e) {
                // 尝试下一个URL
                System.out.println("URL不可用: " + fullUrl + " (" + e.getMessage() + ")");
            }
        }
        // 如果所有URL都不可用，返回第一个（可能仍然失败，但至少有值）
        String defaultUrl = path.isEmpty() ? urls[0] : urls[0] + path;
        System.out.println("警告: 所有测试URL均不可用，使用默认URL: " + defaultUrl);
        return defaultUrl;
    }

    /**
     * 资源使用统计类
     */
    private static class ResourceUsage {
        private final long memoryBefore;
        private final long memoryAfter;
        private final long bytesTransferred;
        private final int connections;
        
        public ResourceUsage(long memoryBefore, long memoryAfter, long bytesTransferred, int connections) {
            this.memoryBefore = memoryBefore;
            this.memoryAfter = memoryAfter;
            this.bytesTransferred = bytesTransferred;
            this.connections = connections;
        }
        
        public long getMemoryUsed() {
            return memoryAfter - memoryBefore;
        }
        
        public long getBytesTransferred() {
            return bytesTransferred;
        }
        
        public int getConnections() {
            return connections;
        }
    }

    /**
     * 获取当前内存使用情况
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
     * 记录资源使用的帮助类
     */
    private static class ResourceMonitor {
        private final Runtime runtime;
        private long startMemory;
        private long startTime;
        private double startCpu;
        
        public ResourceMonitor() {
            this.runtime = Runtime.getRuntime();
            reset();
        }
        
        public void reset() {
            // 进行多次垃圾回收以获得更稳定的内存基准
            System.gc();
            try {
                Thread.sleep(100); // 等待垃圾回收完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            System.gc();
            try {
                Thread.sleep(100); // 再次等待垃圾回收完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            this.startMemory = runtime.totalMemory() - runtime.freeMemory();
            this.startTime = System.currentTimeMillis();
            this.startCpu = getCpuUsage();
        }
        
        public long getMemoryUsed() {
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            // 确保不会返回负值
            return Math.max(0, currentMemory - startMemory);
        }
        
        public long getTimeElapsed() {
            return System.currentTimeMillis() - startTime;
        }
        
        public double getCpuUsagePercent() {
            double currentCpu = getCpuUsage();
            if (startCpu < 0 || currentCpu < 0) {
                return -1; // 无法计算
            }
            return currentCpu - startCpu;
        }
        
        public String getMemoryUsedMB() {
            return memoryFormat.format(getMemoryUsed() / (1024.0 * 1024.0));
        }
        
        public String getTimeElapsedSeconds() {
            return timeFormat.format(getTimeElapsed() / 1000.0);
        }
        
        public String getCpuUsageFormatted() {
            double cpuUsage = getCpuUsagePercent();
            if (cpuUsage < 0) {
                return "不可用";
            }
            return percentFormat.format(cpuUsage) + "%";
        }
    }

    /**
     * 单线程简单GET请求性能测试
     */
    public static void testSimpleGetRequests(int iterations) {
        System.out.println("\n========== 简单GET请求性能测试 ==========");
        System.out.println("测试条件: " + iterations + "次请求, 单线程");
        
        // 找到可用的测试URL
        String testUrl = selectWorkingTestUrl(TEST_BASE_URLS, "/get");
        
        try {
            // 进行独立的预热，确保两种客户端都经过相同的预热
            System.out.println("开始预热...");
            int warmupRequests = Math.min(iterations / 10, 50);
            System.out.println("预热请求数量: " + warmupRequests);
            
            // 创建单独的客户端实例用于预热，确保不影响测试
            OkHttpClient warmupClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
            
            // OkHttp预热
            try {
                for (int i = 0; i < warmupRequests; i++) {
                    OkHttpRequestObj requestObj = new OkHttpRequestObj()
                            .setUrl(testUrl + "?warmup=" + i)
                            .setMethod("GET")
                            .setTimeOut(10)
                            .setFollowRedirects(true);
                    
                    OkHttpCustomResponse response = null;
                    try {
                        // 使用预热专用客户端
                        response = OkHttpRequestUtils.requests(requestObj, warmupClient);
                        if (response != null) {
                            response.getByteArray(); // 预读数据
                            response.disconnect();
                        }
                    } catch (Exception e) {
                        // 忽略预热错误
                    }
                }
                System.out.println("OkHttp预热完成");
            } catch (Exception e) {
                System.out.println("OkHttp预热失败: " + e.getMessage());
            }
            
            // 关闭预热客户端以释放资源
            try {
                warmupClient.dispatcher().executorService().shutdown();
                warmupClient.connectionPool().evictAll();
            } catch (Exception e) {
                // 忽略关闭错误
            }
            
            // HttpURLConnection预热
            try {
                for (int i = 0; i < warmupRequests; i++) {
                    RequestObj requestObj = new RequestObj()
                            .setUrl(testUrl + "?warmup=" + i)
                            .setMethod("GET");
                    
                    CustomHttpResponse response = null;
                    try {
                        response = RequestUtils.requests(requestObj);
                        if (response != null) {
                            response.getByteArray(); // 预读数据
                            response.disconnect();
                        }
                    } catch (Exception e) {
                        // 忽略预热错误
                    }
                }
                System.out.println("HttpURLConnection预热完成");
            } catch (Exception e) {
                System.out.println("HttpURLConnection预热失败: " + e.getMessage());
            }
            
            // 清理共享OkHttpClient连接池，确保测试公平性
            sharedClient.connectionPool().evictAll();
            
            // 预热后等待一小段时间，让系统稳定下来
            try {
                System.out.println("等待系统稳定...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.out.println("执行垃圾回收...");
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.out.println("开始测试...");
            
            // 创建资源采样器，500毫秒采样一次，保证采样粒度足够细
            ResourceSampler okHttpResourceSampler = new ResourceSampler(500);
            okHttpResourceSampler.start();
            System.out.println("已启动OkHttp资源监控，每500毫秒采样一次");
            
            // OkHttp测试开始（与预热完全分离）
            double okHttpTime = 0;
            int okHttpSuccessCount = 0;
            long okHttpStartTime = System.currentTimeMillis();
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            AtomicLong okHttpBytesTransferred = new AtomicLong(0);
            
            for (int i = 0; i < iterations; i++) {
                try {
                    OkHttpCustomResponse response = sendOkHttpRequest(testUrl);
                    if (response != null) {
                        byte[] data = response.getByteArray();
                        okHttpBytesTransferred.addAndGet(data != null ? data.length : 0);
                    }
                    okHttpSuccessCount++;
                } catch (Exception e) {
                    System.out.println("OkHttp请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            okHttpResourceSampler.stop(); // 停止资源采样
            System.out.println("OkHttp测试完成，停止资源监控");
            
            okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            ResourceUsage okHttpResources = new ResourceUsage(
                okHttpMemoryBefore, 
                okHttpMemoryAfter, 
                okHttpBytesTransferred.get(),
                sharedClient.connectionPool().connectionCount()
            );
            
            // 强制垃圾回收，确保测试间隔的内存状态一致
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 创建资源采样器，500毫秒采样一次，保证采样粒度足够细
            ResourceSampler urlConnResourceSampler = new ResourceSampler(500);
            urlConnResourceSampler.start();
            System.out.println("已启动HttpURLConnection资源监控，每500毫秒采样一次");
            
            // HttpURLConnection测试
            double urlConnTime = 0;
            int urlConnSuccessCount = 0;
            long urlConnStartTime = System.currentTimeMillis();
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            AtomicLong urlConnBytesTransferred = new AtomicLong(0);
            
            for (int i = 0; i < iterations; i++) {
                try {
                    CustomHttpResponse response = sendHttpURLConnectionRequest(testUrl);
                    if (response != null) {
                        byte[] data = response.getByteArray();
                        urlConnBytesTransferred.addAndGet(data != null ? data.length : 0);
                    }
                    urlConnSuccessCount++;
                } catch (Exception e) {
                    System.out.println("HttpURLConnection请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            urlConnResourceSampler.stop(); // 停止资源采样
            System.out.println("HttpURLConnection测试完成，已停止资源监控");
            
            urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            ResourceUsage urlConnResources = new ResourceUsage(
                urlConnMemoryBefore,
                urlConnMemoryAfter,
                urlConnBytesTransferred.get(),
                0  // HttpURLConnection没有连接池计数方法
            );
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功率: " + 
                    okHttpSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount > 0 ? okHttpTime / okHttpSuccessCount : 0) + "秒/请求");
            System.out.println("OkHttp 资源使用: 内存增加: " + 
                    memoryFormat.format(okHttpResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(okHttpResources.getBytesTransferred() / (1024.0)) + "KB, 活跃连接: " + 
                    okHttpResources.getConnections());
            System.out.println(okHttpResourceSampler.toString()); // 输出采样结果
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功率: " + 
                    urlConnSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount > 0 ? urlConnTime / urlConnSuccessCount : 0) + "秒/请求");
            System.out.println("HttpURLConnection 资源使用: 内存增加: " + 
                    memoryFormat.format(urlConnResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(urlConnResources.getBytesTransferred() / (1024.0)) + "KB");
            System.out.println(urlConnResourceSampler.toString()); // 输出采样结果
            
            if (okHttpSuccessCount > 0 && urlConnSuccessCount > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
                
                double memoryRatio = (double)okHttpResources.getMemoryUsed() / urlConnResources.getMemoryUsed();
                System.out.println("内存使用比例: OkHttp 内存使用是 HttpURLConnection 的 " + 
                        timeFormat.format(memoryRatio) + "倍");
                
                double dataRatio = (double)okHttpResources.getBytesTransferred() / urlConnResources.getBytesTransferred();
                System.out.println("数据传输比例: OkHttp 数据传输是 HttpURLConnection 的 " + 
                        timeFormat.format(dataRatio) + "倍");
                
                // 添加实时资源监控对比结果
                System.out.println("\n========== 实时资源监控对比 ==========");
                System.out.println(String.format("平均内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageMemory(), 
                    urlConnResourceSampler.getAverageMemory(),
                    okHttpResourceSampler.getAverageMemory() / Math.max(0.001, urlConnResourceSampler.getAverageMemory())));
                    
                System.out.println(String.format("平均CPU使用率: OkHttp=%.1f%%, HttpURLConnection=%.1f%% (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageCpu(), 
                    urlConnResourceSampler.getAverageCpu(),
                    okHttpResourceSampler.getAverageCpu() > 0 ? 
                        okHttpResourceSampler.getAverageCpu() / Math.max(0.1, urlConnResourceSampler.getAverageCpu()) : 0));
                    
                System.out.println(String.format("平均线程数: OkHttp=%.1f, HttpURLConnection=%.1f (差异: %.1f)",
                    okHttpResourceSampler.getAverageThreadCount(), 
                    urlConnResourceSampler.getAverageThreadCount(),
                    okHttpResourceSampler.getAverageThreadCount() - urlConnResourceSampler.getAverageThreadCount()));
                    
                // 资源效率比较 
                double okHttpEfficiency = okHttpSuccessCount / (okHttpResourceSampler.getAverageMemory() * okHttpTime);
                double urlConnEfficiency = urlConnSuccessCount / (urlConnResourceSampler.getAverageMemory() * urlConnTime);
                System.out.println(String.format("资源效率(请求/MB·秒): OkHttp=%.2f, HttpURLConnection=%.2f (比例: %.2f倍)",
                    okHttpEfficiency, urlConnEfficiency, 
                    okHttpEfficiency / Math.max(0.001, urlConnEfficiency)));
            } else {
                System.out.println("无法比较性能：部分或全部测试失败");
            }
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 单线程复杂POST请求性能测试
     */
    public static void testComplexPostRequests(int iterations) {
        System.out.println("\n========== 复杂POST请求性能测试 ==========");
        System.out.println("测试条件: " + iterations + "次请求, 单线程, 包含JSON数据");
        
        try {
            // 找到可用的测试URL
            String testUrl = selectWorkingTestUrl(TEST_BASE_URLS, "/post");
            
            // 创建测试数据
            Map<String, Object> testData = new HashMap<>();
            testData.put("name", "test_user");
            testData.put("id", 12345);
            testData.put("metadata", createComplexJsonData(50));
            String postData = mapToJsonString(testData);
            
            // 进行独立的预热，确保两种客户端都经过相同的预热
            System.out.println("开始预热...");
            int warmupRequests = Math.min(iterations / 10, 30);
            System.out.println("预热请求数量: " + warmupRequests);
            
            // 创建单独的客户端实例用于预热，确保不影响测试
            OkHttpClient warmupClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
                
            // 预热专用的POST数据（与测试数据分开）
            Map<String, Object> warmupPostData = new HashMap<>();
            warmupPostData.put("name", "warmup_user");
            warmupPostData.put("id", 99999);
            warmupPostData.put("metadata", createComplexJsonData(20)); // 小一点的数据集
            String warmupJson = mapToJsonString(warmupPostData);
            
            // OkHttp预热
            try {
                for (int i = 0; i < warmupRequests; i++) {
                    OkHttpRequestObj requestObj = new OkHttpRequestObj()
                            .setUrl(testUrl + "?warmup=" + i)
                            .setMethod("POST")
                            .setPostMethod("Json")
                            .setPostData(warmupJson)
                            .setTimeOut(10)
                            .setFollowRedirects(true);
                    
                    OkHttpCustomResponse response = null;
                    try {
                        // 使用预热专用客户端
                        response = OkHttpRequestUtils.requests(requestObj, warmupClient);
                        if (response != null) {
                            response.getByteArray(); // 预读数据
                            response.disconnect();
                        }
            } catch (Exception e) {
                        // 忽略预热错误
                    }
                }
                System.out.println("OkHttp POST预热完成");
            } catch (Exception e) {
                System.out.println("OkHttp POST预热失败: " + e.getMessage());
            }
            
            // 关闭预热客户端以释放资源
            try {
                warmupClient.dispatcher().executorService().shutdown();
                warmupClient.connectionPool().evictAll();
            } catch (Exception e) {
                // 忽略关闭错误
            }
            
            // HttpURLConnection预热
            try {
                for (int i = 0; i < warmupRequests; i++) {
                    RequestObj requestObj = new RequestObj()
                            .setUrl(testUrl + "?warmup=" + i)
                            .setMethod("POST")
                            .setPostMethod("Json")
                            .setPostData(warmupJson);
                    
                    CustomHttpResponse response = null;
                    try {
                        response = RequestUtils.requests(requestObj);
                        if (response != null) {
                            response.getByteArray(); // 预读数据
                            response.disconnect();
                        }
                    } catch (Exception e) {
                        // 忽略预热错误
                    }
                }
                System.out.println("HttpURLConnection POST预热完成");
            } catch (Exception e) {
                System.out.println("HttpURLConnection POST预热失败: " + e.getMessage());
            }
            
            // 清理共享OkHttpClient连接池，确保测试公平性
            sharedClient.connectionPool().evictAll();
            
            // 预热后等待一小段时间，让系统稳定下来
            try {
                System.out.println("等待系统稳定...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.out.println("执行垃圾回收...");
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.out.println("开始测试...");
            
            // 创建资源采样器，500毫秒采样一次
            ResourceSampler okHttpResourceSampler = new ResourceSampler(500);
            okHttpResourceSampler.start();
            System.out.println("已启动OkHttp资源监控，每500毫秒采样一次");
            
            // OkHttp测试开始（与预热完全分离）
            double okHttpTime = 0;
            int okHttpSuccessCount = 0;
            long okHttpStartTime = System.currentTimeMillis();
            
            // 添加内存和资源监控
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            AtomicLong okHttpBytesTransferred = new AtomicLong(0);
            
            for (int i = 0; i < iterations; i++) {
                try {
                    OkHttpCustomResponse response = sendOkHttpPostRequest(testUrl, postData);
                    if (response != null) {
                        byte[] data = response.getByteArray();
                        okHttpBytesTransferred.addAndGet(data != null ? data.length : 0);
                    }
                    okHttpSuccessCount++;
                } catch (Exception e) {
                    System.out.println("OkHttp POST请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            okHttpResourceSampler.stop(); // 停止资源采样
            System.out.println("OkHttp测试完成，已停止资源监控");
            
            okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            ResourceUsage okHttpResources = new ResourceUsage(
                okHttpMemoryBefore,
                okHttpMemoryAfter,
                okHttpBytesTransferred.get(),
                sharedClient.connectionPool().connectionCount()
            );
            
            // 强制垃圾回收，确保测试间隔的内存状态一致
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 创建资源采样器，500毫秒采样一次
            ResourceSampler urlConnResourceSampler = new ResourceSampler(500);
            urlConnResourceSampler.start();
            System.out.println("已启动HttpURLConnection资源监控，每500毫秒采样一次");
            
            // HttpURLConnection测试
            double urlConnTime = 0;
            int urlConnSuccessCount = 0;
            long urlConnStartTime = System.currentTimeMillis();
            
            // 添加内存和资源监控
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            AtomicLong urlConnBytesTransferred = new AtomicLong(0);
            
            for (int i = 0; i < iterations; i++) {
                try {
                    CustomHttpResponse response = sendHttpURLConnectionPostRequest(testUrl, postData);
                    if (response != null) {
                        byte[] data = response.getByteArray();
                        urlConnBytesTransferred.addAndGet(data != null ? data.length : 0);
                    }
                    urlConnSuccessCount++;
                } catch (Exception e) {
                    System.out.println("HttpURLConnection POST请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            urlConnResourceSampler.stop(); // 停止资源采样
            System.out.println("HttpURLConnection测试完成，已停止资源监控");
            
            urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            ResourceUsage urlConnResources = new ResourceUsage(
                urlConnMemoryBefore,
                urlConnMemoryAfter,
                urlConnBytesTransferred.get(),
                0 // HttpURLConnection没有连接池计数方法
            );
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功率: " + 
                    okHttpSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount > 0 ? okHttpTime / okHttpSuccessCount : 0) + "秒/请求");
            System.out.println("OkHttp 资源使用: 内存增加: " + 
                    memoryFormat.format(okHttpResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(okHttpResources.getBytesTransferred() / (1024.0)) + "KB, 活跃连接: " + 
                    okHttpResources.getConnections());
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功率: " + 
                    urlConnSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount > 0 ? urlConnTime / urlConnSuccessCount : 0) + "秒/请求");
            System.out.println("HttpURLConnection 资源使用: 内存增加: " + 
                    memoryFormat.format(urlConnResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(urlConnResources.getBytesTransferred() / (1024.0)) + "KB");
            
            if (okHttpSuccessCount > 0 && urlConnSuccessCount > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
                
                // 添加内存和资源比较
                double memoryRatio = (double)okHttpResources.getMemoryUsed() / urlConnResources.getMemoryUsed();
                System.out.println("内存使用比例: OkHttp 内存使用是 HttpURLConnection 的 " + 
                        timeFormat.format(memoryRatio) + "倍");
                
                double dataRatio = (double)okHttpResources.getBytesTransferred() / urlConnResources.getBytesTransferred();
                System.out.println("数据传输比例: OkHttp 数据传输是 HttpURLConnection 的 " + 
                        timeFormat.format(dataRatio) + "倍");
                
                // 添加实时资源监控对比结果
                System.out.println("\n========== 复杂POST请求资源监控对比 ==========");
                System.out.println(String.format("平均内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageMemory(), 
                    urlConnResourceSampler.getAverageMemory(),
                    okHttpResourceSampler.getAverageMemory() / Math.max(0.001, urlConnResourceSampler.getAverageMemory())));
                    
                System.out.println(String.format("平均CPU使用率: OkHttp=%.1f%%, HttpURLConnection=%.1f%% (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageCpu(), 
                    urlConnResourceSampler.getAverageCpu(),
                    okHttpResourceSampler.getAverageCpu() > 0 ? 
                        okHttpResourceSampler.getAverageCpu() / Math.max(0.1, urlConnResourceSampler.getAverageCpu()) : 0));
                    
                System.out.println(String.format("平均线程数: OkHttp=%.1f, HttpURLConnection=%.1f (差异: %.1f)",
                    okHttpResourceSampler.getAverageThreadCount(), 
                    urlConnResourceSampler.getAverageThreadCount(),
                    okHttpResourceSampler.getAverageThreadCount() - urlConnResourceSampler.getAverageThreadCount()));
                
                // 复杂POST请求特有指标：每字节数据传输的资源消耗
                double okHttpByteEfficiency = okHttpResourceSampler.getAverageMemory() * 1024 * 1024 / Math.max(1, okHttpResources.getBytesTransferred());
                double urlConnByteEfficiency = urlConnResourceSampler.getAverageMemory() * 1024 * 1024 / Math.max(1, urlConnResources.getBytesTransferred());
                System.out.println(String.format("每传输1KB数据的内存使用(KB): OkHttp=%.2f, HttpURLConnection=%.2f (比例: %.2f倍)",
                    okHttpByteEfficiency, urlConnByteEfficiency, 
                    okHttpByteEfficiency / Math.max(0.001, urlConnByteEfficiency)));
            } else {
                System.out.println("无法比较性能：部分或全部测试失败");
            }
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 文件下载性能测试
     */
    public static void testFileDownload() {
        System.out.println("\n========== 文件下载性能测试 ==========");
        System.out.println("测试条件: 下载文件");
        
        try {
            // 找到可用的测试URL
            String testUrl = selectWorkingTestUrl(TEST_LARGE_FILE_URLS, "");
            
            // 创建临时目录
            File tempDir = new File("temp_download");
            if (!tempDir.exists()) {
                tempDir.mkdir();
            }
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.out.println("执行垃圾回收...");
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 创建资源采样器，200毫秒采样一次，因为文件下载可能会有突发的资源使用
            ResourceSampler okHttpResourceSampler = new ResourceSampler(200);
            okHttpResourceSampler.start();
            System.out.println("已启动OkHttp下载资源监控，每200毫秒采样一次");
            
            // OkHttp下载测试
            File okHttpFile = new File(tempDir, "okhttp_download.bin");
            long okHttpStartTime = 0;
            long okHttpEndTime = 0;
            long okHttpFileSize = 0;
            boolean okHttpSuccess = false;
            
            // 记录内存使用前状态
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            
            // 获取开始时的线程状态
            Thread[] okHttpThreadsBefore = getThreadSnapshot();
            
            try {
                okHttpStartTime = System.currentTimeMillis();
                okHttpFileSize = downloadFileWithOkHttp(testUrl, okHttpFile);
                okHttpEndTime = System.currentTimeMillis();
                okHttpSuccess = true;
            } catch (Exception e) {
                System.out.println("OkHttp下载失败: " + e.getMessage());
                okHttpEndTime = System.currentTimeMillis();
            }
            
            // 记录内存使用后状态
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            
            // 获取结束时的线程状态
            Thread[] okHttpThreadsAfter = getThreadSnapshot();
            
            // 停止资源采样
            okHttpResourceSampler.stop();
            System.out.println("OkHttp下载测试完成，停止资源监控");
            
            double okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            double okHttpSpeed = okHttpSuccess ? okHttpFileSize / (1024.0 * 1024.0) / okHttpTime : 0; // MB/s
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 创建资源采样器，200毫秒采样一次
            ResourceSampler urlConnResourceSampler = new ResourceSampler(200);
            urlConnResourceSampler.start();
            System.out.println("已启动HttpURLConnection下载资源监控，每200毫秒采样一次");
            
            // HttpURLConnection下载测试
            File urlConnFile = new File(tempDir, "urlconn_download.bin");
            long urlConnStartTime = 0;
            long urlConnEndTime = 0;
            long urlConnFileSize = 0;
            boolean urlConnSuccess = false;
            
            // 记录内存使用前状态
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            
            // 获取开始时的线程状态
            Thread[] urlConnThreadsBefore = getThreadSnapshot();
            
            try {
                urlConnStartTime = System.currentTimeMillis();
                urlConnFileSize = downloadFileWithHttpURLConnection(testUrl, urlConnFile);
                urlConnEndTime = System.currentTimeMillis();
                urlConnSuccess = true;
            } catch (Exception e) {
                System.out.println("HttpURLConnection下载失败: " + e.getMessage());
                urlConnEndTime = System.currentTimeMillis();
            }
            
            // 记录内存使用后状态
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            
            // 获取结束时的线程状态
            Thread[] urlConnThreadsAfter = getThreadSnapshot();
            
            // 停止资源采样
            urlConnResourceSampler.stop();
            System.out.println("HttpURLConnection下载测试完成，停止资源监控");
            
            double urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            double urlConnSpeed = urlConnSuccess ? urlConnFileSize / (1024.0 * 1024.0) / urlConnTime : 0; // MB/s
            
            // 结果输出
            if (okHttpSuccess) {
                System.out.println("OkHttp 下载耗时: " + timeFormat.format(okHttpTime) + "秒, 大小: " + 
                        sizeFormat.format(okHttpFileSize / (1024.0 * 1024.0)) + "MB, 速度: " + 
                        sizeFormat.format(okHttpSpeed) + "MB/s");
                System.out.println("OkHttp 内存使用: " + 
                        memoryFormat.format((okHttpMemoryAfter - okHttpMemoryBefore) / (1024.0 * 1024.0)) + "MB");
                System.out.println(okHttpResourceSampler.toString());
                System.out.println("OkHttp 线程变化: " + (okHttpThreadsAfter.length - okHttpThreadsBefore.length) +
                        " (下载前: " + okHttpThreadsBefore.length + ", 下载后: " + okHttpThreadsAfter.length + ")");
            } else {
                System.out.println("OkHttp 下载失败");
            }
            
            if (urlConnSuccess) {
                System.out.println("HttpURLConnection 下载耗时: " + timeFormat.format(urlConnTime) + "秒, 大小: " + 
                        sizeFormat.format(urlConnFileSize / (1024.0 * 1024.0)) + "MB, 速度: " + 
                        sizeFormat.format(urlConnSpeed) + "MB/s");
                System.out.println("HttpURLConnection 内存使用: " + 
                        memoryFormat.format((urlConnMemoryAfter - urlConnMemoryBefore) / (1024.0 * 1024.0)) + "MB");
                System.out.println(urlConnResourceSampler.toString());
                System.out.println("HttpURLConnection 线程变化: " + (urlConnThreadsAfter.length - urlConnThreadsBefore.length) +
                        " (下载前: " + urlConnThreadsBefore.length + ", 下载后: " + urlConnThreadsAfter.length + ")");
            } else {
                System.out.println("HttpURLConnection 下载失败");
            }
            
            if (okHttpSuccess && urlConnSuccess) {
                double speedRatio = okHttpSpeed / urlConnSpeed;
                System.out.println("性能比例: OkHttp 下载速度是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
                
                // 添加内存使用比例
                double memoryRatio = (double)(okHttpMemoryAfter - okHttpMemoryBefore) / 
                                    (urlConnMemoryAfter - urlConnMemoryBefore);
                System.out.println("内存使用比例: OkHttp 内存使用是 HttpURLConnection 的 " + 
                        timeFormat.format(memoryRatio) + "倍");
                
                // 添加线程级别资源对比
                System.out.println("\n========== 文件下载线程级别资源监控对比 ==========");
                System.out.println(String.format("平均内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB",
                    okHttpResourceSampler.getAverageMemory(), urlConnResourceSampler.getAverageMemory()));
                
                System.out.println(String.format("峰值内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB", 
                    okHttpResourceSampler.getMaxMemory(), urlConnResourceSampler.getMaxMemory()));
                
                System.out.println(String.format("平均CPU使用率: OkHttp=%.1f%%, HttpURLConnection=%.1f%%",
                    okHttpResourceSampler.getAverageCpu(), urlConnResourceSampler.getAverageCpu()));
                
                System.out.println(String.format("平均线程数: OkHttp=%.1f, HttpURLConnection=%.1f",
                    okHttpResourceSampler.getAverageThreadCount(), urlConnResourceSampler.getAverageThreadCount()));
                
                // 特别针对文件下载的效率计算
                double okHttpMBPerMemoryMB = okHttpFileSize / (1024.0 * 1024.0) / okHttpResourceSampler.getAverageMemory();
                double urlConnMBPerMemoryMB = urlConnFileSize / (1024.0 * 1024.0) / urlConnResourceSampler.getAverageMemory();
                
                System.out.println(String.format("下载效率(下载MB/内存MB): OkHttp=%.2f, HttpURLConnection=%.2f (比例: %.2f倍)",
                    okHttpMBPerMemoryMB, urlConnMBPerMemoryMB, 
                    okHttpMBPerMemoryMB / Math.max(0.001, urlConnMBPerMemoryMB)));
                
                // 内存效率（每MB文件的内存消耗）
                double okHttpMemoryEfficiency = (okHttpMemoryAfter - okHttpMemoryBefore) / 
                                              (okHttpFileSize / (1024.0 * 1024.0));
                double urlConnMemoryEfficiency = (urlConnMemoryAfter - urlConnMemoryBefore) / 
                                               (urlConnFileSize / (1024.0 * 1024.0));
                System.out.println("内存效率: OkHttp每下载1MB数据使用 " + 
                        memoryFormat.format(okHttpMemoryEfficiency / (1024.0 * 1024.0)) + "MB内存, HttpURLConnection使用 " + 
                        memoryFormat.format(urlConnMemoryEfficiency / (1024.0 * 1024.0)) + "MB内存");
            } else {
                System.out.println("无法比较下载速度：部分或全部测试失败");
            }
            
            // 清理临时文件
            try {
                if (okHttpFile.exists()) okHttpFile.delete();
                if (urlConnFile.exists()) urlConnFile.delete();
            } catch (Exception e) {
                System.out.println("清理临时文件失败: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 大规模并发请求测试
     */
    public static void testMassiveParallelRequests(int totalRequests, int concurrency) {
        System.out.println("\n========== 大规模并发请求性能测试 ==========");
        System.out.println("测试条件: 共" + totalRequests + "次请求, 并发数: " + concurrency);
        
        try {
            // 找到可用的测试URL
            String testUrl = selectWorkingTestUrl(TEST_BASE_URLS, "/get");
            
            // 创建线程池，用于预热和测试
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            
            // 创建适合此测试的OkHttpClient，确保配置与并发度匹配
            OkHttpClient testClient = createSharedClient(concurrency);
            
            // 进行独立的预热，确保两种客户端都经过相同的并发预热
            System.out.println("开始并发预热...");
            int warmupRequests = Math.min(totalRequests / 10, 100);
            System.out.println("预热请求数量: " + warmupRequests + ", 并发数: " + concurrency);
            
            // 创建单独的OkHttp客户端实例用于预热
            OkHttpClient warmupClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(concurrency, 5, TimeUnit.MINUTES))
                .build();
            
            // OkHttp并发预热
            try {
                CountDownLatch warmupLatch = new CountDownLatch(warmupRequests);
                AtomicInteger warmupSuccess = new AtomicInteger(0);
                
                for (int i = 0; i < warmupRequests; i++) {
                    final int requestId = i;
                    executor.execute(() -> {
                        try {
                            OkHttpRequestObj requestObj = new OkHttpRequestObj()
                                    .setUrl(testUrl + "?warmup=" + requestId)
                                    .setMethod("GET")
                                    .setTimeOut(10)
                                    .setFollowRedirects(true);
                            
                            OkHttpCustomResponse response = null;
                            try {
                                response = OkHttpRequestUtils.requests(requestObj, warmupClient);
                                if (response != null) {
                                    response.getByteArray(); // 预读数据
                                    response.disconnect();
                                    warmupSuccess.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // 忽略预热错误
                            }
                        } catch (Exception e) {
                            // 忽略预热错误
                        } finally {
                            warmupLatch.countDown();
                        }
                    });
                }
                
                warmupLatch.await(30, TimeUnit.SECONDS); // 等待预热完成，最多30秒
                System.out.println("OkHttp并发预热完成，成功率: " + 
                        warmupSuccess.get() + "/" + warmupRequests + " (" + 
                        (warmupSuccess.get() * 100 / warmupRequests) + "%)");
            } catch (Exception e) {
                System.out.println("OkHttp并发预热异常: " + e.getMessage());
            }
            
            // 关闭预热客户端以释放资源
            try {
                warmupClient.dispatcher().executorService().shutdown();
                warmupClient.connectionPool().evictAll();
            } catch (Exception e) {
                // 忽略关闭错误
            }
            
            // HttpURLConnection并发预热
            try {
                CountDownLatch warmupLatch = new CountDownLatch(warmupRequests);
                AtomicInteger warmupSuccess = new AtomicInteger(0);
                
                for (int i = 0; i < warmupRequests; i++) {
                    final int requestId = i;
                    executor.execute(() -> {
                        try {
                            RequestObj requestObj = new RequestObj()
                                    .setUrl(testUrl + "?warmup=" + requestId)
                                    .setMethod("GET")
                                    .setTimeOut(10);
                            
                            CustomHttpResponse response = null;
                            try {
                                response = RequestUtils.requests(requestObj);
                                if (response != null) {
                                    response.getByteArray(); // 预读数据
                                    response.disconnect();
                                    warmupSuccess.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // 忽略预热错误
                            }
                        } catch (Exception e) {
                            // 忽略预热错误
                        } finally {
                            warmupLatch.countDown();
                        }
                    });
                }
                
                warmupLatch.await(30, TimeUnit.SECONDS); // 等待预热完成，最多30秒
                System.out.println("HttpURLConnection并发预热完成，成功率: " + 
                        warmupSuccess.get() + "/" + warmupRequests + " (" + 
                        (warmupSuccess.get() * 100 / warmupRequests) + "%)");
            } catch (Exception e) {
                System.out.println("HttpURLConnection并发预热异常: " + e.getMessage());
            }
            
            // 清理共享OkHttpClient连接池，确保测试公平性
            sharedClient.connectionPool().evictAll();
            
            // 预热后等待一小段时间，让系统稳定下来
            try {
                System.out.println("等待系统稳定...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.out.println("执行垃圾回收...");
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.out.println("开始测试...");
            
            // 创建资源采样器，用于OkHttp测试
            ResourceSampler okHttpResourceSampler = new ResourceSampler(500);
            okHttpResourceSampler.start();
            System.out.println("已启动OkHttp并发请求资源监控，每500毫秒采样一次");
            
            // OkHttp测试开始（与预热完全分离）
            AtomicInteger okHttpSuccessCount = new AtomicInteger(0);
            AtomicInteger okHttpFailCount = new AtomicInteger(0);
            AtomicLong okHttpBytesTransferred = new AtomicLong(0);
            
            // 记录测试前内存使用
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            Thread[] okHttpThreadsBefore = getThreadSnapshot();
            
            long okHttpStartTime = System.currentTimeMillis();
            CountDownLatch okHttpLatch = new CountDownLatch(totalRequests);
            
            for (int i = 0; i < totalRequests; i++) {
                final int requestId = i;
                executor.execute(() -> {
                    try {
                        OkHttpCustomResponse response = sendOkHttpRequest(testUrl + "?id=" + UUID.randomUUID().toString());
                        if (response != null) {
                            byte[] data = response.getByteArray();
                            okHttpBytesTransferred.addAndGet(data != null ? data.length : 0);
                        }
                        okHttpSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        if (requestId % 50 == 0) { // 限制错误日志数量
                            System.out.println("OkHttp并发请求 #" + requestId + " 失败: " + e.getMessage());
                        }
                        okHttpFailCount.incrementAndGet();
                    } finally {
                        okHttpLatch.countDown();
                    }
                });
            }
            
            try {
                okHttpLatch.await(); // 等待所有请求完成
            } catch (InterruptedException e) {
                System.out.println("OkHttp测试被中断: " + e.getMessage());
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            double okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            double okHttpRps = okHttpSuccessCount.get() / okHttpTime; // 每秒请求数
            
            // 记录测试后内存使用和连接池情况
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            Thread[] okHttpThreadsAfter = getThreadSnapshot();
            int okHttpConnections = testClient.connectionPool().connectionCount();
            
            // 停止资源采样器
            okHttpResourceSampler.stop();
            System.out.println("OkHttp并发测试完成，停止资源监控");
            
            ResourceUsage okHttpResources = new ResourceUsage(
                okHttpMemoryBefore,
                okHttpMemoryAfter,
                okHttpBytesTransferred.get(),
                okHttpConnections
            );
            
            // 强制垃圾回收，确保测试间隔的内存状态一致
            System.gc();
            try {
                Thread.sleep(2000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 创建资源采样器，用于HttpURLConnection测试
            ResourceSampler urlConnResourceSampler = new ResourceSampler(500);
            urlConnResourceSampler.start();
            System.out.println("已启动HttpURLConnection并发请求资源监控，每500毫秒采样一次");
            
            // HttpURLConnection测试
            AtomicInteger urlConnSuccessCount = new AtomicInteger(0);
            AtomicInteger urlConnFailCount = new AtomicInteger(0);
            AtomicLong urlConnBytesTransferred = new AtomicLong(0);
            
            // 记录测试前内存使用
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            Thread[] urlConnThreadsBefore = getThreadSnapshot();
            
            long urlConnStartTime = System.currentTimeMillis();
            CountDownLatch urlConnLatch = new CountDownLatch(totalRequests);
            
            for (int i = 0; i < totalRequests; i++) {
                final int requestId = i;
                executor.execute(() -> {
                    try {
                        CustomHttpResponse response = sendHttpURLConnectionRequest(testUrl + "?id=" + UUID.randomUUID().toString());
                        if (response != null) {
                            byte[] data = response.getByteArray();
                            urlConnBytesTransferred.addAndGet(data != null ? data.length : 0);
                        }
                        urlConnSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        if (requestId % 50 == 0) { // 限制错误日志数量
                            System.out.println("HttpURLConnection并发请求 #" + requestId + " 失败: " + e.getMessage());
                        }
                        urlConnFailCount.incrementAndGet();
                    } finally {
                        urlConnLatch.countDown();
                    }
                });
            }
            
            try {
                urlConnLatch.await(); // 等待所有请求完成
            } catch (InterruptedException e) {
                System.out.println("HttpURLConnection测试被中断: " + e.getMessage());
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            double urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            double urlConnRps = urlConnSuccessCount.get() / urlConnTime; // 每秒请求数
            
            // 记录测试后内存使用
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            Thread[] urlConnThreadsAfter = getThreadSnapshot();
            
            // 停止资源采样器
            urlConnResourceSampler.stop();
            System.out.println("HttpURLConnection并发测试完成，停止资源监控");
            
            ResourceUsage urlConnResources = new ResourceUsage(
                urlConnMemoryBefore,
                urlConnMemoryAfter,
                urlConnBytesTransferred.get(),
                0 // HttpURLConnection没有连接池计数方法
            );
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功: " + okHttpSuccessCount.get() + 
                    ", 失败: " + okHttpFailCount.get() + ", QPS: " + sizeFormat.format(okHttpRps));
            System.out.println("OkHttp 资源使用: 内存增加: " + 
                    memoryFormat.format(okHttpResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(okHttpResources.getBytesTransferred() / (1024.0 * 1024.0)) + "MB, 活跃连接: " + 
                    okHttpResources.getConnections());
            System.out.println("OkHttp 线程变化: " + (okHttpThreadsAfter.length - okHttpThreadsBefore.length) + 
                    " (测试前: " + okHttpThreadsBefore.length + ", 测试后: " + okHttpThreadsAfter.length + ")");
            System.out.println(okHttpResourceSampler.toString());
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功: " + urlConnSuccessCount.get() + 
                    ", 失败: " + urlConnFailCount.get() + ", QPS: " + sizeFormat.format(urlConnRps));
            System.out.println("HttpURLConnection 资源使用: 内存增加: " + 
                    memoryFormat.format(urlConnResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(urlConnResources.getBytesTransferred() / (1024.0 * 1024.0)) + "MB");
            System.out.println("HttpURLConnection 线程变化: " + (urlConnThreadsAfter.length - urlConnThreadsBefore.length) + 
                    " (测试前: " + urlConnThreadsBefore.length + ", 测试后: " + urlConnThreadsAfter.length + ")");
            System.out.println(urlConnResourceSampler.toString());
            
            if (okHttpSuccessCount.get() > 0 && urlConnSuccessCount.get() > 0) {
                double speedRatio = okHttpRps / urlConnRps;
                System.out.println("性能比例: OkHttp QPS是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍" : timeFormat.format(1/speedRatio) + "倍"));
                
                double memoryRatio = (double)okHttpResources.getMemoryUsed() / urlConnResources.getMemoryUsed();
                System.out.println("内存使用比例: OkHttp 内存使用是 HttpURLConnection 的 " + 
                        timeFormat.format(memoryRatio) + "倍");
                
                double dataRatio = (double)okHttpResources.getBytesTransferred() / urlConnResources.getBytesTransferred();
                System.out.println("数据传输比例: OkHttp 数据传输量是 HttpURLConnection 的 " + 
                        timeFormat.format(dataRatio) + "倍");
                
                // 添加线程级别资源监控对比结果
                System.out.println("\n========== 高并发请求线程级别资源监控对比 ==========");
                System.out.println(String.format("平均内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageMemory(), 
                    urlConnResourceSampler.getAverageMemory(),
                    okHttpResourceSampler.getAverageMemory() / Math.max(0.001, urlConnResourceSampler.getAverageMemory())));
                    
                System.out.println(String.format("峰值内存使用: OkHttp=%.2fMB, HttpURLConnection=%.2fMB (比例: %.2f倍)",
                    okHttpResourceSampler.getMaxMemory(), 
                    urlConnResourceSampler.getMaxMemory(),
                    okHttpResourceSampler.getMaxMemory() / Math.max(0.001, urlConnResourceSampler.getMaxMemory())));
                    
                System.out.println(String.format("平均CPU使用率: OkHttp=%.1f%%, HttpURLConnection=%.1f%% (比例: %.2f倍)",
                    okHttpResourceSampler.getAverageCpu(), 
                    urlConnResourceSampler.getAverageCpu(),
                    okHttpResourceSampler.getAverageCpu() > 0 ? 
                        okHttpResourceSampler.getAverageCpu() / Math.max(0.1, urlConnResourceSampler.getAverageCpu()) : 0));
                    
                System.out.println(String.format("平均线程数: OkHttp=%.1f, HttpURLConnection=%.1f (差异: %.1f)",
                    okHttpResourceSampler.getAverageThreadCount(), 
                    urlConnResourceSampler.getAverageThreadCount(),
                    okHttpResourceSampler.getAverageThreadCount() - urlConnResourceSampler.getAverageThreadCount()));
                    
                // 特别针对并发测试的效率计算
                double okHttpRpsPerMemoryMB = okHttpRps / okHttpResourceSampler.getAverageMemory();
                double urlConnRpsPerMemoryMB = urlConnRps / urlConnResourceSampler.getAverageMemory();
                
                System.out.println(String.format("内存效率(QPS/MB): OkHttp=%.2f, HttpURLConnection=%.2f (比例: %.2f倍)",
                    okHttpRpsPerMemoryMB, urlConnRpsPerMemoryMB, 
                    okHttpRpsPerMemoryMB / Math.max(0.001, urlConnRpsPerMemoryMB)));
                
                // 每线程效率
                double okHttpRpsPerThread = okHttpRps / okHttpResourceSampler.getAverageThreadCount();
                double urlConnRpsPerThread = urlConnRps / urlConnResourceSampler.getAverageThreadCount();
                
                System.out.println(String.format("线程效率(QPS/线程): OkHttp=%.2f, HttpURLConnection=%.2f (比例: %.2f倍)",
                    okHttpRpsPerThread, urlConnRpsPerThread,
                    okHttpRpsPerThread / Math.max(0.001, urlConnRpsPerThread)));
                
                // 添加总结报告
                System.out.println("\n========== 大规模并发测试资源对比总结 ==========");
                System.out.println("OkHttp与HttpURLConnection在大规模并发测试中的资源使用差异:");
                
                // QPS性能对比
                if (speedRatio > 1.1) {
                    System.out.println("- QPS性能: OkHttp 明显优于 HttpURLConnection (" + timeFormat.format(speedRatio) + "倍)");
                } else if (speedRatio < 0.9) {
                    System.out.println("- QPS性能: HttpURLConnection 略优于 OkHttp (" + timeFormat.format(1/speedRatio) + "倍)");
                } else {
                    System.out.println("- QPS性能: 两者基本相当");
                }
                
                // 内存使用对比
                if (memoryRatio > 1.2) {
                    System.out.println("- 内存使用: OkHttp 比 HttpURLConnection 高 " + timeFormat.format(memoryRatio) + "倍");
                    System.out.println("  (注: OkHttp维护连接池和复杂组件导致内存开销更大)");
                } else if (memoryRatio < 0.8) {
                    System.out.println("- 内存使用: OkHttp 比 HttpURLConnection 低 " + timeFormat.format(1/memoryRatio) + "倍");
                } else {
                    System.out.println("- 内存使用: 两者相差不大");
                }
                
                // 数据传输对比
                if (Math.abs(dataRatio - 1.0) > 0.05) {
                    System.out.println("- 数据传输: OkHttp 的数据传输量是 HttpURLConnection 的 " + timeFormat.format(dataRatio) + "倍");
                    System.out.println("  (差异可能来自请求头处理、压缩算法或连接复用等方面)");
                } else {
                    System.out.println("- 数据传输: 两者传输量基本一致");
                }
                
                // 线程效率对比
                double threadEfficiencyRatio = okHttpRpsPerThread / urlConnRpsPerThread;
                if (threadEfficiencyRatio > 1.1) {
                    System.out.println("- 线程效率: OkHttp 每线程处理能力是 HttpURLConnection 的 " + 
                            timeFormat.format(threadEfficiencyRatio) + "倍");
                } else if (threadEfficiencyRatio < 0.9) {
                    System.out.println("- 线程效率: HttpURLConnection 每线程处理能力是 OkHttp 的 " + 
                            timeFormat.format(1/threadEfficiencyRatio) + "倍");
                } else {
                    System.out.println("- 线程效率: 两者每线程处理能力相当");
                }
                
                // 连接池分析
                System.out.println("- 连接池效率: OkHttp 在测试结束时有 " + okHttpResources.getConnections() + " 个活跃连接");
                System.out.println("  (相比之下，HttpURLConnection 每个请求通常会创建新连接)");
            } else {
                System.out.println("无法比较QPS：部分或全部测试失败");
            }
                    
            executor.shutdown();
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 连接池效率测试
     */
    public static void testConnectionPooling(int iterations, int concurrency) {
        System.out.println("\n========== 连接池效率测试 ==========");
        System.out.println("测试条件: " + iterations + "次请求到同一主机, 并发数: " + concurrency);
        
        try {
            // 找到可用的测试URL
            String testUrl = selectWorkingTestUrl(TEST_BASE_URLS, "/get");
            
            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            
            // 为此测试专门创建一个OkHttpClient，确保连接池大小与并发度一致
            // 这样与HttpURLConnection的公平性更高（后者默认每个并发线程一个连接）
            OkHttpClient testClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(concurrency, 5, TimeUnit.MINUTES))
                .build();
            
            // 进行独立的预热，特别重要：连接池测试中预热可以让连接池初始化
            System.out.println("开始连接池预热...");
            int warmupRequests = Math.min(iterations / 10, 50);
            System.out.println("预热请求数量: " + warmupRequests + " (目标主机: " + new URL(testUrl).getHost() + ")");
            
            // 创建单独的OkHttp客户端实例用于预热
            OkHttpClient warmupClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(concurrency, 5, TimeUnit.MINUTES))
                .build();
            
            // OkHttp连接池预热
            try {
                CountDownLatch warmupLatch = new CountDownLatch(warmupRequests);
                AtomicInteger warmupSuccess = new AtomicInteger(0);
                
                for (int i = 0; i < warmupRequests; i++) {
                    final int requestId = i;
                    executor.execute(() -> {
                        try {
                            OkHttpRequestObj requestObj = new OkHttpRequestObj()
                                    .setUrl(testUrl + "?poolwarmup=" + requestId)
                                    .setMethod("GET")
                                    .setTimeOut(10)
                                    .setFollowRedirects(true);
                            
                            OkHttpCustomResponse response = null;
                            try {
                                response = OkHttpRequestUtils.requests(requestObj, warmupClient);
                                if (response != null) {
                                    response.getByteArray(); // 预读数据
                                    response.disconnect();
                                    warmupSuccess.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // 忽略预热错误
                            }
                        } catch (Exception e) {
                            // 忽略预热错误
                        } finally {
                            warmupLatch.countDown();
                        }
                    });
                }
                
                warmupLatch.await(30, TimeUnit.SECONDS); // 等待预热完成，最多30秒
                System.out.println("OkHttp连接池预热完成，成功率: " + 
                        warmupSuccess.get() + "/" + warmupRequests + " (" + 
                        (warmupSuccess.get() * 100 / warmupRequests) + "%)");
            } catch (Exception e) {
                System.out.println("OkHttp连接池预热异常: " + e.getMessage());
            }
            
            // 关闭预热客户端以释放资源
            try {
                warmupClient.dispatcher().executorService().shutdown();
                warmupClient.connectionPool().evictAll();
            } catch (Exception e) {
                // 忽略关闭错误
            }
            
            // HttpURLConnection预热 (虽然它没有真正的连接池，但为了公平对比也进行预热)
            try {
                CountDownLatch warmupLatch = new CountDownLatch(warmupRequests);
                AtomicInteger warmupSuccess = new AtomicInteger(0);
                
                for (int i = 0; i < warmupRequests; i++) {
                    final int requestId = i;
                    executor.execute(() -> {
                        try {
                            RequestObj requestObj = new RequestObj()
                                    .setUrl(testUrl + "?poolwarmup=" + requestId)
                                    .setMethod("GET")
                                    .setTimeOut(10);
                            
                            CustomHttpResponse response = null;
                            try {
                                response = RequestUtils.requests(requestObj);
                                if (response != null) {
                                    response.getByteArray(); // 预读数据
                                    response.disconnect();
                                    warmupSuccess.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // 忽略预热错误
                            }
                        } catch (Exception e) {
                            // 忽略预热错误
                        } finally {
                            warmupLatch.countDown();
                        }
                    });
                }
                
                warmupLatch.await(30, TimeUnit.SECONDS); // 等待预热完成，最多30秒
                System.out.println("HttpURLConnection预热完成，成功率: " + 
                        warmupSuccess.get() + "/" + warmupRequests + " (" + 
                        (warmupSuccess.get() * 100 / warmupRequests) + "%)");
            } catch (Exception e) {
                System.out.println("HttpURLConnection预热异常: " + e.getMessage());
            }
            
            // 清理共享OkHttpClient连接池，确保测试公平性
            sharedClient.connectionPool().evictAll();
            
            // 预热后等待，让连接池稳定
            try {
                System.out.println("等待系统稳定...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            // 强制垃圾回收，确保测试前内存状态一致
            System.out.println("执行垃圾回收...");
            System.gc();
            try {
                Thread.sleep(1000); // 等待垃圾收集完成
            } catch (InterruptedException e) {
                // 忽略中断
            }
            
            System.out.println("开始测试...");
            
            // OkHttp测试开始（与预热完全分离）
            AtomicInteger okHttpSuccessCount = new AtomicInteger(0);
            AtomicInteger okHttpFailCount = new AtomicInteger(0);
            AtomicLong okHttpBytesTransferred = new AtomicLong(0);
            
            // 记录测试前内存使用和连接数
            long okHttpMemoryBefore = getCurrentMemoryUsage();
            int okHttpConnectionsBefore = sharedClient.connectionPool().connectionCount();
            
            long okHttpStartTime = System.currentTimeMillis();
            CountDownLatch okHttpLatch = new CountDownLatch(iterations);
            
            for (int i = 0; i < iterations; i++) {
                final int id = i;
                executor.execute(() -> {
                    try {
                        OkHttpRequestObj requestObj = new OkHttpRequestObj()
                                .setUrl(testUrl + "?id=" + id)
                                .setMethod("GET")
                                .setTimeOut(10)
                                .setFollowRedirects(true);
                                
                        OkHttpCustomResponse response = null;
                        try {
                            // 使用专用于此测试的客户端，而不是共享客户端
                            response = OkHttpRequestUtils.requests(requestObj, testClient);
                            if (response != null) {
                                byte[] data = response.getByteArray();
                                okHttpBytesTransferred.addAndGet(data != null ? data.length : 0);
                                okHttpSuccessCount.incrementAndGet();
                            } else {
                                okHttpFailCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            okHttpFailCount.incrementAndGet();
                        } finally {
                            // 确保响应资源被释放
                            if (response != null) {
                                try {
                                    response.disconnect();
                                } catch (Exception e) {
                                    // 忽略关闭时的错误
                                }
                            }
                            okHttpLatch.countDown();
                        }
                    } catch (Exception e) {
                        okHttpFailCount.incrementAndGet();
                        okHttpLatch.countDown();
                    }
                });
            }
            
            try {
                okHttpLatch.await(); // 等待所有请求完成
            } catch (InterruptedException e) {
                System.out.println("OkHttp测试被中断: " + e.getMessage());
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            double okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            
            // 记录测试后内存使用和连接池数据
            long okHttpMemoryAfter = getCurrentMemoryUsage();
            int okHttpConnectionsAfter = testClient.connectionPool().connectionCount();
            ResourceUsage okHttpResources = new ResourceUsage(
                okHttpMemoryBefore,
                okHttpMemoryAfter,
                okHttpBytesTransferred.get(),
                okHttpConnectionsAfter
            );
            
            // HttpURLConnection测试
            AtomicInteger urlConnSuccessCount = new AtomicInteger(0);
            AtomicInteger urlConnFailCount = new AtomicInteger(0);
            AtomicLong urlConnBytesTransferred = new AtomicLong(0);
            
            // 记录测试前内存使用
            long urlConnMemoryBefore = getCurrentMemoryUsage();
            
            long urlConnStartTime = System.currentTimeMillis();
            CountDownLatch urlConnLatch = new CountDownLatch(iterations);
            
            for (int i = 0; i < iterations; i++) {
                final int id = i;
                executor.execute(() -> {
                    try {
                        CustomHttpResponse response = sendHttpURLConnectionRequest(testUrl + "?id=" + id);
                        if (response != null) {
                            byte[] data = response.getByteArray();
                            urlConnBytesTransferred.addAndGet(data != null ? data.length : 0);
                        }
                        urlConnSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        urlConnFailCount.incrementAndGet();
                    } finally {
                        urlConnLatch.countDown();
                    }
                });
            }
            
            try {
                urlConnLatch.await(); // 等待所有请求完成
            } catch (InterruptedException e) {
                System.out.println("HttpURLConnection测试被中断: " + e.getMessage());
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            double urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            
            // 记录测试后内存使用
            long urlConnMemoryAfter = getCurrentMemoryUsage();
            ResourceUsage urlConnResources = new ResourceUsage(
                urlConnMemoryBefore,
                urlConnMemoryAfter,
                urlConnBytesTransferred.get(),
                0 // HttpURLConnection没有连接池计数方法
            );
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功: " + okHttpSuccessCount.get() + 
                    ", 失败: " + okHttpFailCount.get() + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount.get() > 0 ? okHttpTime / okHttpSuccessCount.get() : 0) + "秒/请求");
            System.out.println("OkHttp 资源使用: 内存增加: " + 
                    memoryFormat.format(okHttpResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(okHttpResources.getBytesTransferred() / (1024.0 * 1024.0)) + "MB, 活跃连接: " + 
                    okHttpResources.getConnections() + " (增加: " + (okHttpConnectionsAfter - okHttpConnectionsBefore) + ")");
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功: " + 
                    urlConnSuccessCount.get() + ", 失败: " + urlConnFailCount.get() + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount.get() > 0 ? urlConnTime / urlConnSuccessCount.get() : 0) + "秒/请求");
            System.out.println("HttpURLConnection 资源使用: 内存增加: " + 
                    memoryFormat.format(urlConnResources.getMemoryUsed() / (1024.0 * 1024.0)) + "MB, 数据传输: " + 
                    sizeFormat.format(urlConnResources.getBytesTransferred() / (1024.0 * 1024.0)) + "MB");
            
            if (okHttpSuccessCount.get() > 0 && urlConnSuccessCount.get() > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
                
                double memoryRatio = (double)okHttpResources.getMemoryUsed() / urlConnResources.getMemoryUsed();
                System.out.println("内存使用比例: OkHttp 内存使用是 HttpURLConnection 的 " + 
                        timeFormat.format(memoryRatio) + "倍");
                
                double dataRatio = (double)okHttpResources.getBytesTransferred() / urlConnResources.getBytesTransferred();
                System.out.println("数据传输比例: OkHttp 数据传输量是 HttpURLConnection 的 " + 
                        timeFormat.format(dataRatio) + "倍");
                
                // 连接池重用率估算
                int uniqueRequestCount = iterations;
                int expectedConnectionsWithoutPooling = Math.min(uniqueRequestCount, concurrency);
                double connectionReuseRatio = expectedConnectionsWithoutPooling / (double)Math.max(1, okHttpResources.getConnections());
                System.out.println("OkHttp连接重用率: 约" + timeFormat.format(connectionReuseRatio) + "倍 (理论上每个连接处理了" + 
                        timeFormat.format(iterations / (double)Math.max(1, okHttpResources.getConnections())) + "个请求)");
                
                // 添加连接池测试资源使用总结
                System.out.println("\n========== 连接池效率测试资源对比总结 ==========");
                System.out.println("OkHttp连接池效率分析:");
                System.out.println("- 连接重用: OkHttp使用了 " + okHttpResources.getConnections() + 
                        " 个连接处理了 " + iterations + " 个请求");
                System.out.println("  理论重用率: " + timeFormat.format(connectionReuseRatio) + "倍");
                System.out.println("  每个连接平均处理: " + 
                        timeFormat.format(iterations / (double)Math.max(1, okHttpResources.getConnections())) + " 个请求");
                
                // 性能分析
                if (speedRatio > 1.2) {
                    System.out.println("- 性能提升: OkHttp 比 HttpURLConnection 快 " + timeFormat.format(speedRatio) + "倍");
                    System.out.println("  (连接复用显著降低了连接建立的开销)");
                } else if (speedRatio < 0.8) {
                    System.out.println("- 性能: HttpURLConnection 比 OkHttp 快 " + 
                            timeFormat.format(1/speedRatio) + "倍 (不符合预期，可能有其他因素影响)");
                } else {
                    System.out.println("- 性能: 两者差异不明显，连接池优势可能不明显");
                }
                
                // 资源使用分析
                System.out.println("\n资源使用差异分析:");
                
                // 内存差异
                if (memoryRatio > 1.2) {
                    System.out.println("- 内存占用: OkHttp 比 HttpURLConnection 高 " + timeFormat.format(memoryRatio) + "倍");
                    System.out.println("  (连接池维护需要额外的内存开销，但在高频请求场景下是值得的)");
                } else if (memoryRatio < 0.8) {
                    System.out.println("- 内存占用: OkHttp 比 HttpURLConnection 低 " + 
                            timeFormat.format(1/memoryRatio) + "倍 (连接复用带来了内存节约)");
                } else {
                    System.out.println("- 内存占用: 两者差异不明显");
                }
                
                // 网络效率
                System.out.println("- 网络效率: OkHttp 通过连接复用减少了TCP握手次数，降低延迟");
                System.out.println("  实际连接数: " + okHttpResources.getConnections() + 
                        "，理论最大连接数(不复用): " + expectedConnectionsWithoutPooling);
                
                // 最佳应用场景分析
                System.out.println("\n应用场景建议:");
                System.out.println("- OkHttp更适合: 频繁请求相同域名、需要保持连接的长时间运行应用");
                System.out.println("- HttpURLConnection更适合: 简单的单次请求、内存受限环境、不需要连接复用的场景");
            } else {
                System.out.println("无法比较性能：部分或全部测试失败");
            }
                    
            executor.shutdown();
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 运行所有性能测试
     */
    public static void runAllBenchmarks() {
        System.out.println("======================================");
        System.out.println("    HTTP客户端性能对比测试开始");
        System.out.println("======================================");
        
        try {
            // 创建资源监控器
            ResourceMonitor globalMonitor = new ResourceMonitor();
            
            // 创建结果保存文件
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String resultFile = "benchmark_results_" + timestamp + ".txt";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(resultFile))) {
                writer.println("======================================");
                writer.println("    HTTP客户端性能对比测试结果");
                writer.println("======================================");
                writer.println("测试时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println("Java版本: " + System.getProperty("java.version"));
                writer.println("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                writer.println("处理器核心数: " + Runtime.getRuntime().availableProcessors());
                writer.println("最大可用内存: " + memoryFormat.format(Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0)) + " MB");
                writer.println();
                
                // 运行各项测试并保存结果 - 所有请求数量增加10倍
                writer.println("\n========== 简单GET请求性能测试 ==========");
                captureTestResults(writer, () -> testSimpleGetRequests(100)); // 原来是50
                
                writer.println("\n========== 复杂POST请求性能测试 ==========");
                captureTestResults(writer, () -> testComplexPostRequests(100)); // 原来是30
                
                writer.println("\n========== 文件下载性能测试 ==========");
                captureTestResults(writer, () -> testFileDownload());
                
                writer.println("\n========== 连接池效率测试 ==========");
                captureTestResults(writer, () -> testConnectionPooling(200, 20)); // 原来是100, 10
                
                writer.println("\n========== 大规模并发请求性能测试 ==========");
                captureTestResults(writer, () -> testMassiveParallelRequests(1000, 100)); // 原来是500, 50
                
                // 输出全局资源使用情况
                writer.println("\n========== 全局资源使用统计 ==========");
                writer.println("总测试时间: " + globalMonitor.getTimeElapsedSeconds() + " 秒");
                writer.println("总内存增长: " + globalMonitor.getMemoryUsedMB() + " MB");
                writer.println("CPU使用率: " + globalMonitor.getCpuUsageFormatted());
                
                writer.println("\n======================================");
                writer.println("    HTTP客户端性能对比测试结束");
                writer.println("======================================");
                
                // 添加资源使用总结
                writer.println("\n========== 资源使用总结比较 ==========");
                writer.println("OkHttp 优势:");
                writer.println("- 连接池复用: 可减少连接建立次数，降低延迟和资源开销");
                writer.println("- 请求分发器: 提供更好的并发控制和请求管理");
                writer.println("- 连接保活: 可降低重复请求的延迟");
                writer.println("- 更高的并发性能: 在高并发场景下通常表现更好");
                writer.println("- 更现代的API设计: 提供更灵活的请求配置和拦截器机制");
                
                writer.println("\nHttpURLConnection 优势:");
                writer.println("- 内存占用: 通常比OkHttp更低");
                writer.println("- 无外部依赖: 是Java标准库的一部分，不需要额外依赖");
                writer.println("- 简单场景: 在简单的低并发场景下可能更轻量");
                writer.println("- 低开销: 在只需要少量请求的场景下启动更快");
                
                writer.println("\n应用场景推荐:");
                writer.println("- 推荐使用OkHttp的场景:");
                writer.println("  * 大规模并发请求");
                writer.println("  * 频繁访问相同服务器");
                writer.println("  * 长时间运行的应用程序");
                writer.println("  * 需要请求重试、拦截器等高级功能");
                
                writer.println("- 推荐使用HttpURLConnection的场景:");
                writer.println("  * 简单的单次请求");
                writer.println("  * 内存受限的环境");
                writer.println("  * 不需要连接复用的临时任务");
                writer.println("  * 追求最小依赖的项目");
                
                writer.println("\n备注:");
                writer.println("- 内存使用差异仅供参考，实际值会受到JVM垃圾回收周期的影响");
                writer.println("- 连接重用能力是OkHttp最大的优势，在高并发和频繁请求相同服务器的场景下尤为明显");
                writer.println("- 数据传输量的差异主要来自请求头和编码方式的不同");
                writer.println("- CPU使用率通常与并发量和请求处理方式有关");
                
                System.out.println("\n测试结果已保存到: " + resultFile);
            }
        } catch (Exception e) {
            System.out.println("\n======================================");
            System.out.println("    测试过程中发生未处理的异常");
            System.out.println("======================================");
            e.printStackTrace();
        }
    }
    
    /**
     * 捕获测试执行结果并同时输出到控制台和文件
     */
    private static void captureTestResults(PrintWriter writer, Runnable test) {
        // 保存原始输出流
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            // 创建一个自定义输出流，同时输出到控制台和文件
            System.setOut(new PrintStream(System.out) {
                @Override
                public void println(String x) {
                    originalOut.println(x);
                    writer.println(x);
                }
                
                @Override
                public void print(String x) {
                    originalOut.print(x);
                    writer.print(x);
                }
            });
            
            // 同样处理错误输出流，确保错误信息也被记录
            System.setErr(new PrintStream(System.err) {
                @Override
                public void println(String x) {
                    originalErr.println(x);
                    writer.println("错误: " + x);
                }
                
                @Override
                public void print(String x) {
                    originalErr.print(x);
                    writer.print("错误: " + x);
                }
            });
            
            // 执行测试
            test.run();
            
        } finally {
            // 恢复原始输出流
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        try {
            runAllBenchmarks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 工具方法 ====================
    
    /**
     * 发送OkHttp GET请求
     */
    private static OkHttpCustomResponse sendOkHttpRequest(String url) throws Exception {
        OkHttpRequestObj requestObj = new OkHttpRequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setTimeOut(10)
                .setFollowRedirects(true);
        
        OkHttpCustomResponse response = null;
        try {
            // 使用共享客户端可以提高连接复用效率
            response = OkHttpRequestUtils.requests(requestObj, sharedClient);
            // 预读数据到内存以防止连接泄漏
            if (response != null) {
                response.getByteArray();
            }
            return response;
        } catch (Exception e) {
            if (response != null) {
                response.disconnect(); // 确保响应被关闭
            }
            throw e;
        }
    }
    
    /**
     * 发送HttpURLConnection GET请求
     */
    private static CustomHttpResponse sendHttpURLConnectionRequest(String url) throws Exception {
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("GET");
        CustomHttpResponse response = null;
        try {
            response = RequestUtils.requests(requestObj);
            // 预读数据到内存以防止连接泄漏
            if (response != null) {
                response.getByteArray();
            }
            return response;
        } catch (Exception e) {
            if (response != null) {
                response.disconnect();
            }
            throw e;
        }
    }
    
    /**
     * 发送OkHttp POST请求
     */
    private static OkHttpCustomResponse sendOkHttpPostRequest(String url, String postData) throws Exception {
        OkHttpRequestObj requestObj = new OkHttpRequestObj()
                .setUrl(url)
                .setMethod("POST")
                .setPostMethod("Json")
                .setPostData(postData)
                .setTimeOut(10)
                .setFollowRedirects(true);
        
        OkHttpCustomResponse response = null;
        try {
            // 使用共享客户端
            response = OkHttpRequestUtils.requests(requestObj, sharedClient);
            // 预读数据到内存以防止连接泄漏
            if (response != null) {
                response.getByteArray();
            }
            return response;
        } catch (Exception e) {
            if (response != null) {
                response.disconnect(); // 确保响应被关闭
            }
            throw e;
        }
    }
    
    /**
     * 发送HttpURLConnection POST请求
     */
    private static CustomHttpResponse sendHttpURLConnectionPostRequest(String url, String postData) throws Exception {
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("POST")
                .setPostMethod("Json")
                .setPostData(postData);
        CustomHttpResponse response = null;
        try {
            response = RequestUtils.requests(requestObj);
            // 预读数据到内存以防止连接泄漏
            if (response != null) {
                response.getByteArray();
            }
            return response;
        } catch (Exception e) {
            if (response != null) {
                response.disconnect();
            }
            throw e;
        }
    }
    
    /**
     * 使用OkHttp下载文件
     */
    private static long downloadFileWithOkHttp(String url, File outputFile) throws Exception {
        OkHttpRequestObj requestObj = new OkHttpRequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setTimeOut(30); // 增加下载超时时间
        
        OkHttpCustomResponse response = null;
        try {
            response = OkHttpRequestUtils.requests(requestObj);
            
            // 使用封装好的saveToFile方法直接保存文件
            String savedPath = response.saveToFile(outputFile.getAbsolutePath(), true);
            System.out.println("savedPath: " + savedPath);
            
            if (savedPath != null) {
                return new File(savedPath).length();
            } else {
                throw new IOException("文件下载失败");
            }
        } finally {
            if (response != null) {
                response.disconnect(); // 确保响应被关闭
            }
        }
    }
    
    /**
     * 使用HttpURLConnection下载文件
     */
    private static long downloadFileWithHttpURLConnection(String url, File outputFile) throws Exception {
        RequestObj requestObj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setTimeOut(30); // 增加下载超时时间
        
        CustomHttpResponse response = null;
        try {
            response = RequestUtils.requests(requestObj);
            
            /// 使用封装好的saveToFile方法直接保存文件
            String savedPath = response.saveToFile(outputFile.getAbsolutePath(), true);
            
            if (savedPath != null) {
                return new File(savedPath).length();
            } else {
                throw new IOException("文件下载失败");
            }
        } finally {
            if (response != null) {
                response.disconnect(); // 确保响应被关闭
            }
        }
    }
    
    /**
     * 创建复杂的JSON测试数据
     */
    private static Map<String, Object> createComplexJsonData(int depth) {
        Map<String, Object> data = new HashMap<>();
        
        // 添加基础字段
        data.put("id", UUID.randomUUID().toString());
        data.put("timestamp", System.currentTimeMillis());
        data.put("active", true);
        data.put("score", Math.random() * 100);
        
        // 添加数组
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tags.add("tag-" + i);
        }
        data.put("tags", tags);
        
        // 添加嵌套对象
        if (depth > 0) {
            data.put("nested", createComplexJsonData(depth - 1));
        }
        
        return data;
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private static String mapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append(listToJsonString((List<?>) value));
            } else if (value instanceof Map) {
                sb.append(mapToJsonString((Map<String, Object>) value));
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 将List转换为JSON字符串
     */
    private static String listToJsonString(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(item).append("\"");
            } else if (item instanceof Number || item instanceof Boolean) {
                sb.append(item);
            } else if (item instanceof List) {
                sb.append(listToJsonString((List<?>) item));
            } else if (item instanceof Map) {
                sb.append(mapToJsonString((Map<String, Object>) item));
            }
        }
        
        sb.append("]");
        return sb.toString();
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
     * 获取当前内存使用情况 (轻量版，不进行垃圾回收，用于测试过程中)
     */
    private static long getLightMemoryUsage() {
        // 不进行垃圾回收，直接返回当前内存使用情况
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 获取当前所有线程的快照
     */
    private static Thread[] getThreadSnapshot() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        
        // 预估线程数量，通常会多估计一些
        int estimatedCount = rootGroup.activeCount() * 2;
        Thread[] threads = new Thread[estimatedCount];
        int actualCount = rootGroup.enumerate(threads);
        
        // 创建一个正好大小的数组返回
        Thread[] result = new Thread[actualCount];
        System.arraycopy(threads, 0, result, 0, actualCount);
        return result;
    }
} 