package com.potato.potatotool.update.downloader;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 多源下载器 - 智能选择最快下载源，支持断点续传
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class MultiSourceDownloader {
    
    private static final int CONNECT_TIMEOUT = 10000;  // 10秒连接超时
    private static final int READ_TIMEOUT = 30000;     // 30秒读取超时
    private static final int SPEED_TEST_SIZE = 102400; // 100KB测速
    private static final int BUFFER_SIZE = 8192;       // 8KB缓冲区
    
    private volatile boolean cancelled = false;  // 取消标志
    
    /**
     * 下载文件（多源策略）
     * @param urls URL列表
     * @param targetFile 目标文件
     * @param callback 进度回调
     * @throws Exception 下载失败
     */
    public void downloadWithFallback(List<String> urls, Path targetFile, 
                                    DownloadProgressCallback callback) throws Exception {
        if (urls == null || urls.isEmpty()) {
            throw new Exception("未提供下载URL");
        }
        
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            try {
                if (urls.size() > 1) {
                    if (debugMode) System.out.println("尝试下载源 " + (i + 1) + "/" + urls.size());
                }
                
                download(url, targetFile, callback);
                
                return;
                
            } catch (Exception e) {
                errors.add(e);
                
                // 如果不是最后一个源，继续尝试
                if (i < urls.size() - 1) {
                    if (debugMode) System.out.println("下载失败，尝试备用源...");
                    // 清理部分下载的文件
                    try {
                        if (Files.exists(targetFile)) {
                            Files.delete(targetFile);
                        }
                    } catch (Exception cleanupEx) {
                        // 忽略清理错误
                    }
                } else {
                    // 最后一个源也失败了
                    System.err.println("下载失败: " + e.getMessage());
                }
            }
        }
        
        // 所有源都失败
        Exception lastError = errors.get(errors.size() - 1);
        throw new Exception("所有下载源均失败", lastError);
    }
    
    /**
     * 从单个源下载文件（支持断点续传）
     */
    public void download(String urlString, Path targetFile, 
                        DownloadProgressCallback callback) throws Exception {
        CustomHttpResponse response = null;
        
        try {
            // 检查是否已存在部分下载
            long existingSize = Files.exists(targetFile) ? Files.size(targetFile) : 0;
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(urlString)
                    .setMethod("GET")
                    .setTimeOut(CONNECT_TIMEOUT / 1000)
                    .setReadTimeout(READ_TIMEOUT / 1000);
            
            // 支持断点续传
            Map<String, String> headers = new HashMap<>();
            if (existingSize > 0) {
                headers.put("Range", "bytes=" + existingSize + "-");
                if (debugMode) System.out.println("检测到部分下载，启用断点续传: " + formatSize(existingSize));
            }
            if (!headers.isEmpty()) {
                requestObj.setHeaders(headers);
            }
            
            response = RequestUtils.requests(requestObj, null);
            
            int responseCode = response.getResponseCode();
            
            // 206 表示部分内容（断点续传成功）
            // 200 表示完整内容
            if (responseCode != 200 && responseCode != 206) {
                throw new Exception("HTTP错误: " + responseCode);
            }
            
            // 获取文件总大小和剩余需要下载的大小
            long contentLength = response.getContentLength();
            long totalSize;
            long remainingSize;
            boolean appendMode;
            
            if (responseCode == 206) {
                // 断点续传成功，206 响应
                totalSize = contentLength + existingSize;
                remainingSize = contentLength;
                appendMode = true;
                if (debugMode) System.out.println("断点续传成功，剩余: " + formatSize(remainingSize));
            } else {
                // 200 响应，服务器返回完整文件
                totalSize = contentLength;
                
                // 服务器不支持断点续传或Range请求无效，需要重新下载
                if (existingSize > 0) {
                    if (debugMode) {
                        System.out.println("服务器不支持断点续传，删除旧文件重新下载");
                        System.out.println("本地文件大小: " + formatSize(existingSize) + ", 服务器文件大小: " + formatSize(contentLength));
                    }
                    
                    response.disconnect();
                    response = null;
                    Files.delete(targetFile);
                    existingSize = 0;
                    
                    // 重新连接
                    requestObj = new RequestObj()
                            .setUrl(urlString)
                            .setMethod("GET")
                            .setTimeOut(CONNECT_TIMEOUT / 1000)
                            .setReadTimeout(READ_TIMEOUT / 1000);
                    
                    response = RequestUtils.requests(requestObj, null);
                    responseCode = response.getResponseCode();
                    contentLength = response.getContentLength();
                    totalSize = contentLength;
                }
                
                remainingSize = contentLength;
                appendMode = false;
            }
            
            callback.onStart(urlString, totalSize);
            
            // 下载文件
            try (InputStream in = response.getInputStream();
                 FileOutputStream out = new FileOutputStream(targetFile.toFile(), appendMode)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                long downloaded = existingSize;
                long lastUpdateTime = System.currentTimeMillis();
                long lastUpdateBytes = downloaded;
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    // 检查是否取消
                    if (cancelled) {
                        callback.onCancel();
                        throw new Exception("下载已取消");
                    }
                    
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    // 更新进度（限制频率：每100ms更新一次）
                    long now = System.currentTimeMillis();
                    if (now - lastUpdateTime >= 100) {
                        long elapsed = now - lastUpdateTime;
                        long downloadedSinceLastUpdate = downloaded - lastUpdateBytes;
                        long speed = (downloadedSinceLastUpdate * 1000) / elapsed;  // 字节/秒
                        
                        int percentage = totalSize > 0 ? 
                                        (int) ((downloaded * 100) / totalSize) : 0;
                        
                        callback.onProgress(downloaded, totalSize, percentage, speed);
                        
                        lastUpdateTime = now;
                        lastUpdateBytes = downloaded;
                    }
                }
                
                // 最后更新一次进度
                callback.onProgress(downloaded, totalSize, 100, 0);
                callback.onComplete(targetFile);
            }
            
        } finally {
            if (response != null) {
                response.disconnect();
            }
        }
    }
    
    /**
     * 智能选择最快的下载源
     * @param urls URL列表
     * @return 最快的URL，如果都不可达则返回第一个
     */
    public String selectBestSource(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        
        if (urls.size() == 1) {
            return urls.get(0);
        }
        
        // 测速选择最快源
        String bestUrl = urls.get(0);
        long bestSpeed = -1;
        
        for (String url : urls) {
            try {
                long speed = testDownloadSpeed(url);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestUrl = url;
                }
            } catch (Exception e) {
                // 源不可达，跳过
            }
        }
        
        return bestUrl;
    }
    
    /**
     * 测试下载速度
     * @return 速度（字节/秒），-1表示不可达
     */
    private long testDownloadSpeed(String urlString) throws Exception {
        CustomHttpResponse response = null;
        
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Range", "bytes=0-" + (SPEED_TEST_SIZE - 1));
            
            RequestObj requestObj = new RequestObj()
                    .setUrl(urlString)
                    .setMethod("GET")
                    .setTimeOut(3)
                    .setReadTimeout(5)
                    .setHeaders(headers);
            
            response = RequestUtils.requests(requestObj, null);
            
            try (InputStream in = response.getInputStream()) {
                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                int bytesRead;
                
                while (totalBytes < SPEED_TEST_SIZE && (bytesRead = in.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed == 0) elapsed = 1;  // 避免除零
                
                return (totalBytes * 1000) / elapsed;  // 字节/秒
            }
            
        } finally {
            if (response != null) {
                response.disconnect();
            }
        }
    }
    
    /**
     * 取消下载
     */
    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * 重置取消标志
     */
    public void reset() {
        this.cancelled = false;
    }
    
    /**
     * 格式化大小
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化速度
     */
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 0) {
            return "N/A";
        }
        return formatSize(bytesPerSecond) + "/s";
    }
}

