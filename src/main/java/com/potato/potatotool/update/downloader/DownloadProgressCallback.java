package com.potato.potatotool.update.downloader;

import java.nio.file.Path;

/**
 * 下载进度回调接口
 * 
 * @author Potato
 * @date 2025/10/11
 */
public interface DownloadProgressCallback {
    
    /**
     * 下载开始
     * @param url 下载URL
     * @param totalSize 文件总大小（字节），-1表示未知
     */
    void onStart(String url, long totalSize);
    
    /**
     * 下载进度更新
     * @param downloaded 已下载字节数
     * @param total 总字节数
     * @param percentage 百分比（0-100）
     * @param speed 下载速度（字节/秒）
     */
    void onProgress(long downloaded, long total, int percentage, long speed);
    
    /**
     * 下载完成
     * @param file 下载的文件路径
     */
    void onComplete(Path file);
    
    /**
     * 下载失败
     * @param error 错误信息
     */
    void onError(String error);
    
    /**
     * 下载取消
     */
    void onCancel();
}

