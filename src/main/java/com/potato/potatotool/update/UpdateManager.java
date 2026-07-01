package com.potato.potatotool.update;

import com.potato.potatotool.update.app.AppUpdater;
import com.potato.potatotool.update.downloader.DownloadProgressCallback;
import com.potato.potatotool.update.manifest.Manifest;
import com.potato.potatotool.update.resource.ResourceUpdater;
import com.potato.potatotool.utils.core.I18nUtils;

import java.util.List;

/**
 * 更新管理器 - 统一的更新管理入口
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class UpdateManager {
    
    private static UpdateManager instance;
    
    private final UpdateChecker updateChecker;
    private final AppUpdater appUpdater;
    private final ResourceUpdater resourceUpdater;
    
    private UpdateInfo lastCheckResult;
    
    private UpdateManager() {
        this.updateChecker = new UpdateChecker();
        this.appUpdater = new AppUpdater();
        this.resourceUpdater = new ResourceUpdater();
    }
    
    /**
     * 获取UpdateManager单例
     */
    public static synchronized UpdateManager getInstance() {
        if (instance == null) {
            instance = new UpdateManager();
        }
        return instance;
    }
    
    /**
     * 检查更新
     * @return 更新信息
     */
    public UpdateInfo checkForUpdates() {
        try {
            lastCheckResult = updateChecker.checkForUpdates();
            return lastCheckResult;
        } catch (Exception e) {
            System.err.println("检查更新失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("检查更新失败", e);
        }
    }
    
    /**
     * 获取上次检查结果
     */
    public UpdateInfo getLastCheckResult() {
        return lastCheckResult;
    }
    
    /**
     * 更新软件
     * @param callback 下载进度回调
     */
    public void updateApp(UpdateCallback callback) {
        if (lastCheckResult == null || !lastCheckResult.isAppNeedUpdate()) {
            callback.onError(I18nUtils.getString("update.error.download"));
            return;
        }
        
        new Thread(() -> {
            try {
                // 检查是否有多个下载源
                Manifest.AppVersion appVersion = lastCheckResult.getAppVersion();
                String platform = UpdateChecker.getPlatformIdentifier();
                Manifest.FileInfo fileInfo = appVersion.getFiles().get(platform);
                
                if (fileInfo != null && fileInfo.getUrl().getAllUrls().size() > 1) {
                    // 显示正在选择下载源
                    callback.onStart(I18nUtils.getString("update.message.selecting.source"));
                    Thread.sleep(100);  // 短暂延迟让UI更新
                }
                
                callback.onStart(I18nUtils.getString("update.message.start.app"));
                
                appUpdater.downloadAndPrepareUpdate(appVersion, 
                    new DownloadProgressCallback() {
                        @Override
                        public void onStart(String url, long totalSize) {
                            callback.onProgress(0, totalSize, 0, I18nUtils.getString("update.message.downloading"));
                        }
                        
                        @Override
                        public void onProgress(long downloaded, long total, int percentage, long speed) {
                            callback.onProgress(downloaded, total, percentage, 
                                              formatSpeed(speed));
                        }
                        
                        @Override
                        public void onComplete(java.nio.file.Path file) {
                            callback.onProgress(100, 100, 100, I18nUtils.getString("update.message.completed"));
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                        
                        @Override
                        public void onCancel() {
                            callback.onCancel();
                        }
                    });
                
                callback.onReadyToInstall();
                
            } catch (Exception e) {
                callback.onError(I18nUtils.getString("update.message.download.failed", e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 执行软件更新（重启应用）
     */
    public void executeAppUpdate() throws Exception {
        appUpdater.executeUpdate();
    }
    
    /**
     * 更新指定的资源
     * @param resourceUpdate 资源更新信息
     * @param callback 更新回调
     */
    public void updateResource(ResourceUpdate resourceUpdate, UpdateCallback callback) {
        new Thread(() -> {
            try {
                callback.onStart(I18nUtils.getString("update.message.start.resource", 
                                                     resourceUpdate.getDisplayName()));
                
                resourceUpdater.updateResource(resourceUpdate,
                    new DownloadProgressCallback() {
                        @Override
                        public void onStart(String url, long totalSize) {
                            callback.onProgress(0, totalSize, 0, I18nUtils.getString("update.message.downloading"));
                        }
                        
                        @Override
                        public void onProgress(long downloaded, long total, int percentage, long speed) {
                            callback.onProgress(downloaded, total, percentage, 
                                              formatSpeed(speed));
                        }
                        
                        @Override
                        public void onComplete(java.nio.file.Path file) {
                            callback.onProgress(100, 100, 100, I18nUtils.getString("update.message.completed"));
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                        
                        @Override
                        public void onCancel() {
                            callback.onCancel();
                        }
                    });
                
                callback.onComplete(I18nUtils.getString("update.message.resource.complete", 
                                                        resourceUpdate.getDisplayName()));
                
            } catch (Exception e) {
                callback.onError(I18nUtils.getString("update.message.resource.failed", 
                                                     e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 批量更新资源
     * @param resourceUpdates 资源更新列表
     * @param callback 批量更新回调
     */
    public void updateResources(List<ResourceUpdate> resourceUpdates, 
                               BatchUpdateCallback callback) {
        new Thread(() -> {
            try {
                callback.onStart(I18nUtils.getString("update.message.start.batch"));
                
                resourceUpdater.updateResources(resourceUpdates,
                    new ResourceUpdater.BatchUpdateCallback() {
                        @Override
                        public void onResourceStart(int current, int total, String resourceName) {
                            callback.onBatchProgress(current, total, resourceName, 0, 0, 0);
                        }
                        
                        @Override
                        public void onResourceProgress(int current, int total, String resourceName,
                                                     long downloaded, long totalSize) {
                            int percentage = totalSize > 0 ? 
                                           (int) ((downloaded * 100) / totalSize) : 0;
                            callback.onBatchProgress(current, total, resourceName,
                                                   downloaded, totalSize, percentage);
                        }
                        
                        @Override
                        public void onResourceComplete(int current, int total, String resourceName) {
                            callback.onBatchProgress(current, total, resourceName, -1, -1, 100);
                        }
                        
                        @Override
                        public void onResourceError(int current, int total, String resourceName, String error) {
                            callback.onBatchProgress(current, total, resourceName, -2, -2, -1);
                        }
                        
                        @Override
                        public void onAllComplete() {
                            callback.onComplete(I18nUtils.getString("update.message.all.complete"));
                        }
                        
                        @Override
                        public void onCancel() {
                            callback.onCancel();
                        }
                    });
                
            } catch (Exception e) {
                callback.onError(I18nUtils.getString("update.message.batch.failed", e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 格式化速度
     */
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.2f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 取消当前资源下载
     */
    public void cancelResourceUpdate() {
        resourceUpdater.cancel();
    }

    /**
     * 更新回调接口
     */
    public interface UpdateCallback {
        void onStart(String message);
        void onProgress(long current, long total, int percentage, String message);
        void onComplete(String message);
        void onError(String error);
        void onCancel();
        void onReadyToInstall();  // 软件更新准备就绪，可以安装
    }
    
    /**
     * 批量更新回调接口
     */
    public interface BatchUpdateCallback {
        void onStart(String message);
        void onBatchProgress(int current, int total, String resourceName, 
                           long downloaded, long totalSize, int percentage);
        void onComplete(String message);
        void onError(String error);
        void onCancel();
    }
}

