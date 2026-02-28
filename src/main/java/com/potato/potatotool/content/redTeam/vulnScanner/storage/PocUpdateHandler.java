package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * POC 热更新处理器（新方案）
 * 
 * 基于 poc.json 清单进行增量更新：
 * 1. 拉取云端 poc.json 清单
 * 2. 与本地清单对比差异
 * 3. 只下载变化的 POC 文件
 * 4. 应用更新（新增/删除/修改）
 * 
 * 云端结构：
 * https://potato.gold/poc/
 * ├── poc.json              # 清单文件
 * ├── nuclei/
 * │   ├── cves/2024/
 * │   │   ├── CVE-2024-0001.yaml
 * │   │   └── CVE-2024-0002.yaml
 * │   └── ...
 * ├── goby/
 * │   └── ...
 * └── ...
 * 
 * @author Potato
 * @date 2025/12/19
 */
public class PocUpdateHandler {
    
    private static final String POC_VERSION_KEY = ConfigConstants.VULNSCAN_POC_DB_VERSION;
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    
    private final PocDatabaseInitializer initializer;
    private final OkHttpClient httpClient;
    
    // 默认更新源
    private String primaryBaseUrl = "https://potato.gold/poc/";
    private String mirrorBaseUrl = "https://raw.githubusercontent.com/user/poc-repo/main/";
    
    public PocUpdateHandler() {
        this.initializer = PocDatabaseInitializer.getInstance();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 设置更新源
     */
    public void setUpdateSources(String primary, String mirror) {
        if (primary != null) this.primaryBaseUrl = primary;
        if (mirror != null) this.mirrorBaseUrl = mirror;
    }
    
    /**
     * 检查更新
     * 
     * 注意：不再依赖本地清单文件，而是直接根据远程清单和数据库中的 content_hash 判断
     * - toAdd: 远程有，本地 DB 无（根据 md5 判断）
     * - toUpdate: 远程 md5 与本地不同（根据 action=update 判断）
     * - toDelete: 只根据云端 deleted 字段
     * 
     * @return 差异结果，如果无法获取返回 null
     */
    public CheckResult checkForUpdates() {
        try {
            // 1. 下载远程清单
            PocManifest remoteManifest = fetchRemoteManifest();
            if (remoteManifest == null) {
                return new CheckResult(false, "无法获取远程清单", null, null);
            }
            
            // 2. 获取本地数据库中已有的 content_hash 集合
            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
            java.util.Set<String> existingHashes;
            try {
                existingHashes = new java.util.HashSet<>(dbManager.getAllContentHashes().values());
                existingHashes.remove(null);
            } catch (Exception e) {
                existingHashes = new java.util.HashSet<>();
            }
            
            // 3. 构建差异结果
            PocManifest.DiffResult diff = new PocManifest.DiffResult();
            
            for (PocManifest.FileEntryWithPath entry : remoteManifest.getAllFiles()) {
                String remoteMd5 = entry.getFile().getMd5();
                PocManifest.Action action = entry.getFile().getAction();
                
                if (action == PocManifest.Action.UPDATE) {
                    // 更新类型：直接添加到更新列表
                    diff.getToUpdate().add(entry);
                } else if (remoteMd5 != null && !existingHashes.contains(remoteMd5)) {
                    // 本地不存在该 md5，需要新增
                    diff.getToAdd().add(entry);
                }
            }
            
            // 4. 删除只根据云端 deleted 字段
            if (remoteManifest.getDeleted() != null) {
                diff.getDeletedEntries().addAll(remoteManifest.getDeleted());
            }
            
            return new CheckResult(true, "检查完成", diff, remoteManifest);
            
        } catch (Exception e) {
            System.err.println("检查更新失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new CheckResult(false, "检查失败: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * 执行更新
     * @param callback 进度回调
     * @return 更新结果
     */
    public UpdateResult update(UpdateCallback callback) {
        try {
            // 1. 检查更新
            if (callback != null) callback.onProgress("正在检查更新...", 0);
            CheckResult checkResult = checkForUpdates();
            
            if (!checkResult.isSuccess()) {
                return new UpdateResult(false, checkResult.getMessage(), 0, 0, 0);
            }
            
            PocManifest.DiffResult diff = checkResult.getDiff();
            PocManifest remoteManifest = checkResult.getRemoteManifest();
            
            if (!diff.hasChanges()) {
                return new UpdateResult(true, "已是最新版本", 0, 0, 0);
            }
            
            if (callback != null) {
                callback.onProgress("发现 " + diff.getTotalChanges() + " 个变更", 10);
            }
            
            // 2. 下载需要的 POC 文件
            Map<String, String> downloadedPocs = new HashMap<>();
            
            // 下载新增的
            int totalToDownload = diff.getToAdd().size() + diff.getToUpdate().size();
            int downloaded = 0;
            
            for (PocManifest.FileEntryWithPath entry : diff.getToAdd()) {
                String content = downloadPocFile(entry, remoteManifest.getBaseUrl());
                if (content != null) {
                    downloadedPocs.put(entry.getFullPath(), content);
                }
                downloaded++;
                if (callback != null) {
                    int progress = 10 + (int) (downloaded * 70.0 / totalToDownload);
                    callback.onProgress("下载中 " + downloaded + "/" + totalToDownload, progress);
                }
            }
            
            // 下载更新的
            for (PocManifest.FileEntryWithPath entry : diff.getToUpdate()) {
                String content = downloadPocFile(entry, remoteManifest.getBaseUrl());
                if (content != null) {
                    downloadedPocs.put(entry.getFullPath(), content);
                }
                downloaded++;
                if (callback != null) {
                    int progress = 10 + (int) (downloaded * 70.0 / totalToDownload);
                    callback.onProgress("下载中 " + downloaded + "/" + totalToDownload, progress);
                }
            }
            
            // 3. 应用更新
            if (callback != null) callback.onProgress("正在应用更新...", 80);
            
            PocDatabaseInitializer.UpdateResult initResult = initializer.applyManifestUpdate(
                remoteManifest,
                downloadedPocs,
                message -> {
                    if (callback != null) callback.onProgress(message, 90);
                }
            );
            
            if (callback != null) callback.onProgress("更新完成", 100);
            
            return new UpdateResult(
                initResult.isSuccess(),
                initResult.getMessage(),
                initResult.getAddedCount(),
                initResult.getUpdatedCount(),
                initResult.getDeletedCount()
            );
            
        } catch (Exception e) {
            System.err.println("更新失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new UpdateResult(false, "更新失败: " + e.getMessage(), 0, 0, 0);
        }
    }
    
    /**
     * 获取远程清单
     */
    private PocManifest fetchRemoteManifest() {
        // 尝试主源
        String manifestUrl = primaryBaseUrl + "poc.json";
        String content = fetchUrl(manifestUrl);
        
        if (content == null && mirrorBaseUrl != null) {
            // 尝试镜像源
            manifestUrl = mirrorBaseUrl + "poc.json";
            content = fetchUrl(manifestUrl);
        }
        
        if (content != null) {
            try {
                return PocManifest.fromJson(content);
            } catch (Exception e) {
                if (debugMode) System.err.println("解析清单失败: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 下载 POC 文件
     */
    private String downloadPocFile(PocManifest.FileEntryWithPath entry, String baseUrl) {
        String url = entry.getDownloadUrl(baseUrl != null ? baseUrl : primaryBaseUrl);
        String content = fetchUrl(url);
        
        if (content == null && mirrorBaseUrl != null) {
            // 尝试镜像源
            url = entry.getDownloadUrl(mirrorBaseUrl);
            content = fetchUrl(url);
        }
        
        return content;
    }
    
    /**
     * HTTP GET 请求
     */
    private String fetchUrl(String url) {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "PotatoTool-PocUpdater/1.0")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            if (debugMode) System.err.println("请求失败: " + url + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取本地版本
     */
    public String getLocalVersion() {
        try {
            com.google.gson.JsonObject config = 
                (com.google.gson.JsonObject) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (config != null && config.has(POC_VERSION_KEY)) {
                return config.get(POC_VERSION_KEY).getAsString();
            }
        } catch (Exception e) {
            if (debugMode) System.err.println("获取本地版本失败: " + e.getMessage());
        }
        return null;
    }
    
    // ==================== 结果类 ====================
    
    /**
     * 检查结果
     */
    public static class CheckResult {
        private final boolean success;
        private final String message;
        private final PocManifest.DiffResult diff;
        private final PocManifest remoteManifest;
        
        public CheckResult(boolean success, String message, 
                          PocManifest.DiffResult diff, PocManifest remoteManifest) {
            this.success = success;
            this.message = message;
            this.diff = diff;
            this.remoteManifest = remoteManifest;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public PocManifest.DiffResult getDiff() { return diff; }
        public PocManifest remoteManifest() { return remoteManifest; }
        public PocManifest getRemoteManifest() { return remoteManifest; }
        
        public boolean hasUpdates() {
            return diff != null && diff.hasChanges();
        }
    }
    
    /**
     * 更新结果
     */
    public static class UpdateResult {
        private final boolean success;
        private final String message;
        private final int addedCount;
        private final int updatedCount;
        private final int deletedCount;
        
        public UpdateResult(boolean success, String message, int added, int updated, int deleted) {
            this.success = success;
            this.message = message;
            this.addedCount = added;
            this.updatedCount = updated;
            this.deletedCount = deleted;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getAddedCount() { return addedCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getDeletedCount() { return deletedCount; }
        
        public int getTotalChanges() {
            return addedCount + updatedCount + deletedCount;
        }
        
        @Override
        public String toString() {
            return String.format("UpdateResult{success=%s, add=%d, update=%d, delete=%d}",
                    success, addedCount, updatedCount, deletedCount);
        }
    }
    
    /**
     * 更新回调
     */
    public interface UpdateCallback {
        void onProgress(String message, int percent);
    }
}
