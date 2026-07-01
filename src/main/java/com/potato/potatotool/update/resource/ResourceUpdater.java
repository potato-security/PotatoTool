package com.potato.potatotool.update.resource;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.ResourceUpdate;
import com.potato.potatotool.update.downloader.DownloadProgressCallback;
import com.potato.potatotool.update.downloader.MultiSourceDownloader;
import com.potato.potatotool.update.manifest.Manifest;
import com.potato.potatotool.update.verifier.FileVerifier;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.GzipUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 资源更新器 - 更新资源文件
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class ResourceUpdater {
    
    private final MultiSourceDownloader downloader;
    private final PathManager pathManager;
    
    public ResourceUpdater() {
        this.downloader = new MultiSourceDownloader();
        this.pathManager = PathManager.getInstance();
    }
    
    /**
     * 更新单个资源
     * @param resourceUpdate 资源更新信息
     * @param callback 下载进度回调
     * @throws Exception 更新失败
     * 
     * 1. 检查磁盘空间 
     * 2. 获取并选择最佳下载源 
     * 3. 确定文件路径
     *  - downloadFile: 下载的文件（可能是压缩包）
     *  - finalFile: 最终文件（解压后或直接下载）
     * 4. 尝试使用缓存 → tryUseCachedFile()
     *  - 成功 → return 
     *  - 失败 → 继续
     * 5. 下载文件（支持断点续传）
     * 6. 解压文件（如果需要）
     * 7. 校验文件完整性 
     * 8. 安装资源 → finishUpdate() 
     */
    public void updateResource(ResourceUpdate resourceUpdate, 
                              DownloadProgressCallback callback) throws Exception {
        Manifest.ResourceItem resource = resourceUpdate.getResource();
        Manifest.FileDetails fileDetails = resource.getFiles();
        
        // 1. 检查磁盘空间
        long requiredSpace = fileDetails.getMinDiskSpace();
        if (!pathManager.hasEnoughSpace(requiredSpace)) {
            long available = pathManager.getAvailableSpace(pathManager.getResourceBasePath());
            throw new Exception(I18nUtils.getString("update.exception.disk.space",
                              PathManager.formatSize(requiredSpace), 
                              PathManager.formatSize(available)));
        }
        
        // 2. 获取下载URL并选择最佳源
        List<String> urls = fileDetails.getUrl().getAllUrls();
        if (urls.isEmpty()) {
            throw new Exception(I18nUtils.getString("update.exception.no.url"));
        }
        
        // 选择最佳下载源（静默进行，状态由批量更新回调处理）
        if (urls.size() > 1) {
            if (debugMode) System.out.println("检测到多个下载源，正在选择最佳源...");
            String bestUrl = downloader.selectBestSource(urls);
            if (bestUrl != null) {
                if (debugMode) System.out.println("已选择最佳下载源");
                urls.remove(bestUrl);
                urls.add(0, bestUrl);
            }
        }
        
        // 3. 确定文件路径
        Path tempDir = pathManager.getTempPath();
        Files.createDirectories(tempDir);
        
        String fileName = extractFileName(urls.get(0));
        Path downloadFile = tempDir.resolve(fileName);
        Path finalFile = getExpectedFinalFile(downloadFile, fileDetails);
        
        // 4. 尝试使用缓存文件
        if (tryUseCachedFile(finalFile, downloadFile, fileDetails, resource, callback, urls.get(0))) {
            return;  // 使用缓存成功，直接返回
        }
        
        // 5. 下载文件（支持断点续传）
        downloader.downloadWithFallback(urls, downloadFile, callback);
        
        // 6. 解压文件（如果需要）
        if (fileDetails.isCompressed()) {
            if (debugMode) System.out.println("正在解压文件...");
            if (callback != null) {
                callback.onProgress(0, 0, 100, -4);  // 解压中
            }
        }
        finalFile = processResourceFile(downloadFile, fileDetails);
        
        // 7. 校验文件完整性
        if (debugMode) System.out.println("正在校验文件完整性...");
        if (callback != null) {
            callback.onProgress(0, 0, 100, -3);  // 校验中
        }
        
        if (!FileVerifier.verifyChecksum(finalFile, fileDetails.getChecksum())) {
            if (!downloadFile.equals(finalFile)) {
                Files.deleteIfExists(downloadFile);
            }
            throw new Exception(I18nUtils.getString("update.exception.checksum.failed"));
        }
        
        // 8. 安装资源
        finishUpdate(finalFile, resource, fileDetails);
        if (debugMode) System.out.println("资源更新完成: " + resource.getDisplayName());
    }
    
    /**
     * 获取预期的最终文件路径（解压后的文件）
     */
    private Path getExpectedFinalFile(Path downloadFile, Manifest.FileDetails fileDetails) {
        if (!fileDetails.isCompressed()) {
            return downloadFile;
        }
        
        String originalName = downloadFile.getFileName().toString();
        if (originalName.endsWith(".gzip") || originalName.endsWith(".gz")) {
            originalName = originalName.replaceAll("\\.(gzip|gz)$", "");
        }
        return downloadFile.getParent().resolve(originalName);
    }
    
    /**
     * 尝试使用缓存文件
     * @return true 如果成功使用缓存，false 需要重新下载
     */
    private boolean tryUseCachedFile(Path finalFile, Path downloadFile, 
                                     Manifest.FileDetails fileDetails,
                                     Manifest.ResourceItem resource,
                                     DownloadProgressCallback callback,
                                     String url) {
        // 检查最终文件（解压后的文件或直接下载的文件）
        if (Files.exists(finalFile)) {
            String fileDesc = fileDetails.isCompressed() ? "解压后的文件" : "本地文件";
            // 对于压缩文件：如果解压后的文件损坏，需要同时删除压缩包
            // 对于非压缩文件：finalFile == downloadFile，fileToDelete 传 null 避免重复删除
            Path fileToDelete = fileDetails.isCompressed() ? downloadFile : null;
            return checkAndUseCachedFile(finalFile, fileToDelete, fileDetails, resource, callback, url, fileDesc);
        }
        
        // 压缩文件不需要校验压缩包本身，下载器会自动处理断点续传
        // 非压缩文件的 finalFile == downloadFile，上面已经检查过了
        return false;  // 没有可用的缓存
    }
    
    /**
     * 检查并使用缓存文件
     */
    private boolean checkAndUseCachedFile(Path fileToCheck, Path fileToDelete,
                                         Manifest.FileDetails fileDetails,
                                         Manifest.ResourceItem resource,
                                         DownloadProgressCallback callback,
                                         String url, String fileDesc) {
        try {
            long fileSize = Files.size(fileToCheck);
            if (debugMode) {
                System.out.println("检测到" + fileDesc + ": " + fileToCheck + 
                                 " (" + PathManager.formatSize(fileSize) + ")");
                System.out.println("校验文件...");
            }
            
            if (FileVerifier.verifyChecksum(fileToCheck, fileDetails.getChecksum())) {
                if (debugMode) System.out.println("文件校验通过，跳过下载" + 
                                 (fileDetails.isCompressed() ? "和解压" : ""));
                
                // 通知回调
                if (callback != null) {
                    callback.onStart(url, fileSize);
                    callback.onProgress(fileSize, fileSize, 100, 0);
                    callback.onComplete(fileToCheck);
                }
                
                // 完成安装
                finishUpdate(fileToCheck, resource, fileDetails);
                if (debugMode) System.out.println("资源更新完成（使用缓存）: " + resource.getDisplayName());
                return true;
            } else {
                if (debugMode) System.out.println(fileDesc + "校验失败，删除并重新下载");
                Files.deleteIfExists(fileToCheck);
                if (fileToDelete != null) {
                    Files.deleteIfExists(fileToDelete);
                }
            }
        } catch (Exception e) {
            System.err.println("检查" + fileDesc + "时出错: " + e.getMessage());
            try {
                Files.deleteIfExists(fileToCheck);
                if (fileToDelete != null) {
                    Files.deleteIfExists(fileToDelete);
                }
            } catch (Exception ex) {
                // 忽略
            }
        }
        
        return false;
    }
    
    /**
     * 完成更新：安装资源并更新配置
     */
    private void finishUpdate(Path sourceFile, Manifest.ResourceItem resource,
                             Manifest.FileDetails fileDetails) throws IOException {
        Path targetFile = pathManager.getResourcePath(fileDetails.getLocalPath());
        installResource(sourceFile, targetFile);
        updateResourceConfig(resource.getName(), resource.getVersion(),
                           targetFile.toString(), fileDetails.getChecksum());
    }
    
    /**
     * 批量更新资源
     * @param resourceUpdates 资源更新列表
     * @param callback 总体进度回调
     * @throws Exception 更新失败
     */
    public void updateResources(List<ResourceUpdate> resourceUpdates, 
                               BatchUpdateCallback callback) throws Exception {
        int total = resourceUpdates.size();
        
        for (int i = 0; i < resourceUpdates.size(); i++) {
            final int current = i + 1;  // 声明为final
            ResourceUpdate resourceUpdate = resourceUpdates.get(i);
            String resourceName = resourceUpdate.getDisplayName();
            
            callback.onResourceStart(current, total, resourceName);
            
            try {
                // 检查是否有多个下载源，如果有则通知UI正在选择
                Manifest.FileDetails fileDetails = resourceUpdate.getResource().getFiles();
                List<String> urls = fileDetails.getUrl().getAllUrls();
                if (urls.size() > 1) {
                    callback.onResourceProgress(current, total, resourceName, -5L, -5L);
                }
                
                updateResource(resourceUpdate, new DownloadProgressCallback() {
                    @Override
                    public void onStart(String url, long totalSize) {
                        callback.onResourceProgress(current, total, resourceName, 0, totalSize);
                    }
                    
                    @Override
                    public void onProgress(long downloaded, long totalSize, int percentage, long speed) {
                        callback.onResourceProgress(current, total, resourceName, downloaded, totalSize);
                    }
                    
                    @Override
                    public void onComplete(Path file) {
                        // 由外层处理
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onResourceError(current, total, resourceName, error);
                    }
                    
                    @Override
                    public void onCancel() {
                        callback.onCancel();
                    }
                });
                
                callback.onResourceComplete(current, total, resourceName);
                
            } catch (Exception e) {
                callback.onResourceError(current, total, resourceName, e.getMessage());
                // 所有资源都是可选的，下载失败时仅记录错误，不抛出异常
            }
        }
        
        callback.onAllComplete();
    }
    
    /**
     * 处理资源文件（解压等）
     */
    private Path processResourceFile(Path downloadFile, Manifest.FileDetails fileDetails) 
            throws IOException {
        if (!fileDetails.isCompressed()) {
            return downloadFile;
        }
        
        String compressionType = fileDetails.getCompressionType();
        if ("gzip".equalsIgnoreCase(compressionType)) {
            // 解压gzip
            String originalName = downloadFile.getFileName().toString();
            if (originalName.endsWith(".gzip") || originalName.endsWith(".gz")) {
                originalName = originalName.replaceAll("\\.(gzip|gz)$", "");
            }
            
            Path uncompressedFile = downloadFile.getParent().resolve(originalName);
            
            if (debugMode) System.out.println("解压gzip文件...");
            GzipUtils.unGzipFile(downloadFile.toString(), uncompressedFile.toString(), true);
            
            return uncompressedFile;
        }
        
        // 不支持的压缩格式，直接返回
        if (compressionType != null && !compressionType.isEmpty()) {
            System.err.println("警告: 不支持的压缩格式 " + compressionType);
        }
        return downloadFile;
    }
    
    /**
     * 安装资源文件
     */
    private void installResource(Path sourceFile, Path targetFile) throws IOException {
        Path backup = null;
        
        // 备份旧文件（如果存在）
        if (Files.exists(targetFile)) {
            backup = targetFile.resolveSibling(targetFile.getFileName() + ".backup");
            try {
                Files.move(targetFile, backup, 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (debugMode) System.out.println("已备份旧文件: " + backup);
            } catch (IOException e) {
                System.err.println("备份旧文件失败: " + e.getMessage());
                backup = null;  // 备份失败，不要在后续删除
            }
        }
        
        // 移动新文件到目标位置
        try {
            Files.createDirectories(targetFile.getParent());
            Files.move(sourceFile, targetFile, 
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // 安装成功，删除备份文件
            if (backup != null && Files.exists(backup)) {
                Files.deleteIfExists(backup);
                if (debugMode) System.out.println("已删除备份文件: " + backup);
            }
            
        } catch (IOException e) {
            // 安装失败，尝试恢复备份
            if (backup != null && Files.exists(backup)) {
                try {
                    Files.move(backup, targetFile, 
                              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    if (debugMode) System.out.println("已恢复备份文件: " + targetFile);
                } catch (IOException ex) {
                    System.err.println("恢复备份失败: " + ex.getMessage());
                }
            }
            throw e;  // 重新抛出异常
        }
    }
    
    /**
     * 更新资源配置（使用合并模式，不会覆盖其他资源）
     */
    private void updateResourceConfig(String resourceName, String version, 
                                     String path, Manifest.Checksum checksum) {
        try {
            // 提取文件名（不包含路径）
            String fileName = Paths.get(path).getFileName().toString();
            
            // 使用LinkedHashMap保证字段顺序
            Map<String, Object> resourceInfo = new LinkedHashMap<>();
            resourceInfo.put(ConfigConstants.UPDATE_VERSION, version);
            resourceInfo.put(ConfigConstants.UPDATE_FILE_NAME, fileName);
            
            // 保存checksum信息
            if (checksum != null) {
                Map<String, String> checksumMap = new LinkedHashMap<>();
                if (checksum.getSha256() != null && !checksum.getSha256().isEmpty()) {
                    checksumMap.put(ConfigConstants.UPDATE_CHECKSUM_SHA256, checksum.getSha256());
                }
                if (checksum.getMd5() != null && !checksum.getMd5().isEmpty()) {
                    checksumMap.put(ConfigConstants.UPDATE_CHECKSUM_MD5, checksum.getMd5());
                }
                if (!checksumMap.isEmpty()) {
                    resourceInfo.put(ConfigConstants.UPDATE_CHECKSUM, checksumMap);
                }
            }
            
            // 使用合并保存方法，避免覆盖其他资源（如 md5Database 等）
            Map<String, Object> resourcesMap = new LinkedHashMap<>();
            resourcesMap.put(resourceName, resourceInfo);
            
            Constants.saveResourceConfigMerge(resourcesMap);
            
        } catch (Exception e) {
            System.err.println("更新配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新本地资源配置（使用合并模式，不会覆盖其他资源）
     * 
     * @param resourceName 资源名称（如"winKbInfo"）
     * @param filePath 文件路径
     */
    public static void updateLocalResourceConfig(String resourceName, String filePath) {
        try {
            Path file = Paths.get(filePath);
            
            // 从文件名提取版本号（日期）
            String version = extractDateFromFileName(filePath);
            
            // 提取文件名（不包含路径）
            String fileName = file.getFileName().toString();
            
            // 更新配置到 UpDate.resources.resourceName，使用LinkedHashMap保证字段顺序
            Map<String, Object> resourceInfo = new LinkedHashMap<>();
            resourceInfo.put(ConfigConstants.UPDATE_VERSION, version);
            resourceInfo.put(ConfigConstants.UPDATE_FILE_NAME, fileName);
            
            // 内置资源没有checksum（从JAR释放），留空或不添加
            // 后续checkForUpdates时会从manifest获取并更新
            
            // 使用合并保存方法，避免覆盖其他资源（如 md5Database 等）
            Map<String, Object> resourcesMap = new LinkedHashMap<>();
            resourcesMap.put(resourceName, resourceInfo);
            
            Constants.saveResourceConfigMerge(resourcesMap);
                        
        } catch (Exception e) {
            System.err.println("更新资源配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文件名中提取日期（数字）
     */
    private static String extractDateFromFileName(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        Pattern pattern = Pattern.compile("(\\d{4,})");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    /**
     * 从URL提取文件名
     */
    private String extractFileName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0) {
            return url.substring(lastSlash + 1);
        }
        return "resource_" + System.currentTimeMillis();
    }
    
    /**
     * 取消当前下载
     */
    public void cancel() {
        downloader.cancel();
    }

    /**
     * 批量更新回调接口
     */
    public interface BatchUpdateCallback {
        void onResourceStart(int current, int total, String resourceName);
        void onResourceProgress(int current, int total, String resourceName, 
                               long downloaded, long totalSize);
        void onResourceComplete(int current, int total, String resourceName);
        void onResourceError(int current, int total, String resourceName, String error);
        void onAllComplete();
        void onCancel();
    }
}

