package com.potato.potatotool.utils.network;

import com.potato.potatotool.utils.okhttp.OkHttpCustomResponse;
import com.potato.potatotool.utils.okhttp.OkHttpRequestObj;
import com.potato.potatotool.utils.okhttp.OkHttpRequestUtils;

import java.io.File;
import java.io.FileOutputStream;
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

/**
 * HTTP客户端性能对比测试类
 * 对比OkHttp和HttpURLConnection封装实现的性能差异
 * @author Benchmark
 */
public class HttpClientBenchmark {

    // 格式化数字显示
    private static final NumberFormat timeFormat = new DecimalFormat("#0.000");
    private static final NumberFormat sizeFormat = new DecimalFormat("#0.00");
    
    // 共享的OkHttpClient实例，用于提高连接复用效率
    private static final OkHttpClient sharedClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
        .build();

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
     * 单线程简单GET请求性能测试
     */
    public static void testSimpleGetRequests(int iterations) {
        System.out.println("\n========== 简单GET请求性能测试 ==========");
        System.out.println("测试条件: " + iterations + "次请求, 单线程");
        
        // 找到可用的测试URL
        String testUrl = selectWorkingTestUrl(TEST_BASE_URLS, "/get");
        
        try {
            // 预热
            try {
                sendOkHttpRequest(testUrl);
            } catch (Exception e) {
                System.out.println("OkHttp预热失败: " + e.getMessage());
            }
            
            try {
                sendHttpURLConnectionRequest(testUrl);
            } catch (Exception e) {
                System.out.println("HttpURLConnection预热失败: " + e.getMessage());
            }
            
            // OkHttp测试
            double okHttpTime = 0;
            int okHttpSuccessCount = 0;
            long okHttpStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                try {
                    sendOkHttpRequest(testUrl);
                    okHttpSuccessCount++;
                } catch (Exception e) {
                    System.out.println("OkHttp请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            
            // HttpURLConnection测试
            double urlConnTime = 0;
            int urlConnSuccessCount = 0;
            long urlConnStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                try {
                    sendHttpURLConnectionRequest(testUrl);
                    urlConnSuccessCount++;
                } catch (Exception e) {
                    System.out.println("HttpURLConnection请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功率: " + 
                    okHttpSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount > 0 ? okHttpTime / okHttpSuccessCount : 0) + "秒/请求");
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功率: " + 
                    urlConnSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount > 0 ? urlConnTime / urlConnSuccessCount : 0) + "秒/请求");
            
            if (okHttpSuccessCount > 0 && urlConnSuccessCount > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
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
            
            // 预热
            try {
                sendOkHttpPostRequest(testUrl, postData);
            } catch (Exception e) {
                System.out.println("OkHttp预热失败: " + e.getMessage());
            }
            
            try {
                sendHttpURLConnectionPostRequest(testUrl, postData);
            } catch (Exception e) {
                System.out.println("HttpURLConnection预热失败: " + e.getMessage());
            }
            
            // OkHttp测试
            double okHttpTime = 0;
            int okHttpSuccessCount = 0;
            long okHttpStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                try {
                    sendOkHttpPostRequest(testUrl, postData);
                    okHttpSuccessCount++;
                } catch (Exception e) {
                    System.out.println("OkHttp POST请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long okHttpEndTime = System.currentTimeMillis();
            okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            
            // HttpURLConnection测试
            double urlConnTime = 0;
            int urlConnSuccessCount = 0;
            long urlConnStartTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                try {
                    sendHttpURLConnectionPostRequest(testUrl, postData);
                    urlConnSuccessCount++;
                } catch (Exception e) {
                    System.out.println("HttpURLConnection POST请求失败 #" + i + ": " + e.getMessage());
                }
            }
            
            long urlConnEndTime = System.currentTimeMillis();
            urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功率: " + 
                    okHttpSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount > 0 ? okHttpTime / okHttpSuccessCount : 0) + "秒/请求");
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功率: " + 
                    urlConnSuccessCount + "/" + iterations + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount > 0 ? urlConnTime / urlConnSuccessCount : 0) + "秒/请求");
            
            if (okHttpSuccessCount > 0 && urlConnSuccessCount > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
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
            
            // OkHttp下载测试
            File okHttpFile = new File(tempDir, "okhttp_download.bin");
            long okHttpStartTime = 0;
            long okHttpEndTime = 0;
            long okHttpFileSize = 0;
            boolean okHttpSuccess = false;
            
            try {
                okHttpStartTime = System.currentTimeMillis();
                okHttpFileSize = downloadFileWithOkHttp(testUrl, okHttpFile);
                okHttpEndTime = System.currentTimeMillis();
                okHttpSuccess = true;
            } catch (Exception e) {
                System.out.println("OkHttp下载失败: " + e.getMessage());
                okHttpEndTime = System.currentTimeMillis();
            }
            
            double okHttpTime = (okHttpEndTime - okHttpStartTime) / 1000.0;
            double okHttpSpeed = okHttpSuccess ? okHttpFileSize / (1024.0 * 1024.0) / okHttpTime : 0; // MB/s
            
            // HttpURLConnection下载测试
            File urlConnFile = new File(tempDir, "urlconn_download.bin");
            long urlConnStartTime = 0;
            long urlConnEndTime = 0;
            long urlConnFileSize = 0;
            boolean urlConnSuccess = false;
            
            try {
                urlConnStartTime = System.currentTimeMillis();
                urlConnFileSize = downloadFileWithHttpURLConnection(testUrl, urlConnFile);
                urlConnEndTime = System.currentTimeMillis();
                urlConnSuccess = true;
            } catch (Exception e) {
                System.out.println("HttpURLConnection下载失败: " + e.getMessage());
                urlConnEndTime = System.currentTimeMillis();
            }
            
            double urlConnTime = (urlConnEndTime - urlConnStartTime) / 1000.0;
            double urlConnSpeed = urlConnSuccess ? urlConnFileSize / (1024.0 * 1024.0) / urlConnTime : 0; // MB/s
            
            // 结果输出
            if (okHttpSuccess) {
                System.out.println("OkHttp 下载耗时: " + timeFormat.format(okHttpTime) + "秒, 大小: " + 
                        sizeFormat.format(okHttpFileSize / (1024.0 * 1024.0)) + "MB, 速度: " + 
                        sizeFormat.format(okHttpSpeed) + "MB/s");
            } else {
                System.out.println("OkHttp 下载失败");
            }
            
            if (urlConnSuccess) {
                System.out.println("HttpURLConnection 下载耗时: " + timeFormat.format(urlConnTime) + "秒, 大小: " + 
                        sizeFormat.format(urlConnFileSize / (1024.0 * 1024.0)) + "MB, 速度: " + 
                        sizeFormat.format(urlConnSpeed) + "MB/s");
            } else {
                System.out.println("HttpURLConnection 下载失败");
            }
            
            if (okHttpSuccess && urlConnSuccess) {
                double speedRatio = okHttpSpeed / urlConnSpeed;
                System.out.println("性能比例: OkHttp 下载速度是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
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
            
            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            
            // OkHttp测试
            AtomicInteger okHttpSuccessCount = new AtomicInteger(0);
            AtomicInteger okHttpFailCount = new AtomicInteger(0);
            
            long okHttpStartTime = System.currentTimeMillis();
            CountDownLatch okHttpLatch = new CountDownLatch(totalRequests);
            
            for (int i = 0; i < totalRequests; i++) {
                final int requestId = i;
                executor.execute(() -> {
                    try {
                        sendOkHttpRequest(testUrl + "?id=" + UUID.randomUUID().toString());
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
            
            // HttpURLConnection测试
            AtomicInteger urlConnSuccessCount = new AtomicInteger(0);
            AtomicInteger urlConnFailCount = new AtomicInteger(0);
            
            long urlConnStartTime = System.currentTimeMillis();
            CountDownLatch urlConnLatch = new CountDownLatch(totalRequests);
            
            for (int i = 0; i < totalRequests; i++) {
                final int requestId = i;
                executor.execute(() -> {
                    try {
                        sendHttpURLConnectionRequest(testUrl + "?id=" + UUID.randomUUID().toString());
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
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功: " + okHttpSuccessCount.get() + 
                    ", 失败: " + okHttpFailCount.get() + ", QPS: " + sizeFormat.format(okHttpRps));
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功: " + urlConnSuccessCount.get() + 
                    ", 失败: " + urlConnFailCount.get() + ", QPS: " + sizeFormat.format(urlConnRps));
            
            if (okHttpSuccessCount.get() > 0 && urlConnSuccessCount.get() > 0) {
                double speedRatio = okHttpRps / urlConnRps;
                System.out.println("性能比例: OkHttp QPS是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍" : timeFormat.format(1/speedRatio) + "倍"));
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
            
            // OkHttp测试
            AtomicInteger okHttpSuccessCount = new AtomicInteger(0);
            AtomicInteger okHttpFailCount = new AtomicInteger(0);
            
            long okHttpStartTime = System.currentTimeMillis();
            CountDownLatch okHttpLatch = new CountDownLatch(iterations);
            
            for (int i = 0; i < iterations; i++) {
                final int id = i;
                executor.execute(() -> {
                    try {
                        sendOkHttpRequest(testUrl + "?id=" + id);
                        okHttpSuccessCount.incrementAndGet();
                    } catch (Exception e) {
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
            
            // HttpURLConnection测试
            AtomicInteger urlConnSuccessCount = new AtomicInteger(0);
            AtomicInteger urlConnFailCount = new AtomicInteger(0);
            
            long urlConnStartTime = System.currentTimeMillis();
            CountDownLatch urlConnLatch = new CountDownLatch(iterations);
            
            for (int i = 0; i < iterations; i++) {
                final int id = i;
                executor.execute(() -> {
                    try {
                        sendHttpURLConnectionRequest(testUrl + "?id=" + id);
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
            
            // 结果输出
            System.out.println("OkHttp 总耗时: " + timeFormat.format(okHttpTime) + "秒, 成功: " + okHttpSuccessCount.get() + 
                    ", 失败: " + okHttpFailCount.get() + ", 平均: " + 
                    timeFormat.format(okHttpSuccessCount.get() > 0 ? okHttpTime / okHttpSuccessCount.get() : 0) + "秒/请求");
            
            System.out.println("HttpURLConnection 总耗时: " + timeFormat.format(urlConnTime) + "秒, 成功: " + 
                    urlConnSuccessCount.get() + ", 失败: " + urlConnFailCount.get() + ", 平均: " + 
                    timeFormat.format(urlConnSuccessCount.get() > 0 ? urlConnTime / urlConnSuccessCount.get() : 0) + "秒/请求");
            
            if (okHttpSuccessCount.get() > 0 && urlConnSuccessCount.get() > 0) {
                double speedRatio = urlConnTime / okHttpTime;
                System.out.println("性能比例: OkHttp 是 HttpURLConnection 的 " + 
                        (speedRatio > 1 ? timeFormat.format(speedRatio) + "倍快" : timeFormat.format(1/speedRatio) + "倍慢"));
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
            // 运行各项测试
            testSimpleGetRequests(50);
            testComplexPostRequests(30);
            testFileDownload();
            testConnectionPooling(100, 10);
            testMassiveParallelRequests(500, 50);
            
            System.out.println("\n======================================");
            System.out.println("    HTTP客户端性能对比测试结束");
            System.out.println("======================================");
        } catch (Exception e) {
            System.out.println("\n======================================");
            System.out.println("    测试过程中发生未处理的异常");
            System.out.println("======================================");
            e.printStackTrace();
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
                .setMethod("GET");
        
        CustomHttpResponse response = null;
        try {
            response = RequestUtils.requests(requestObj);
            
            // 将响应写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(response.getByteArray());
            }
            
            return outputFile.length();
        } finally {
            if (response != null) {
                response.disconnect();
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
} 