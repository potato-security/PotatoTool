package com.potato.potatotool.content.redTeam.vulnScanner.loader;

import com.potato.potatotool.storage.PathManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * POC远程更新器
 * 支持从GitHub等远程仓库下载和更新POC
 * 
 * @author Potato
 * @date 2025/11/26
 */
public class PocUpdater {
    
    // 预定义的POC仓库
    public static final Map<String, PocSource> POC_SOURCES = new LinkedHashMap<>();
    
    static {
        // Nuclei模板
        POC_SOURCES.put("nuclei-templates", new PocSource(
            "Nuclei Templates",
            "https://github.com/projectdiscovery/nuclei-templates/archive/refs/heads/main.zip",
            "nuclei",
            "官方Nuclei POC模板库"
        ));
        
        // 其他可选仓库
        POC_SOURCES.put("vulhub", new PocSource(
            "Vulhub POCs",
            "https://github.com/vulhub/vulhub/archive/refs/heads/master.zip",
            "nuclei",
            "Vulhub漏洞环境POC"
        ));
    }
    
    private final Path pocDirectory;
    private Consumer<String> progressCallback;
    private Consumer<Integer> percentCallback;
    
    public PocUpdater() {
        this.pocDirectory = PathManager.getInstance().getResourcePath("vulnscan").resolve("pocs");
        try {
            Files.createDirectories(pocDirectory);
        } catch (IOException e) {
            System.err.println("创建POC目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置进度回调
     */
    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }
    
    /**
     * 设置百分比回调
     */
    public void setPercentCallback(Consumer<Integer> callback) {
        this.percentCallback = callback;
    }
    
    /**
     * 获取可用的POC源列表
     */
    public List<PocSource> getAvailableSources() {
        return new ArrayList<>(POC_SOURCES.values());
    }
    
    /**
     * 从指定源更新POC
     */
    public UpdateResult updateFromSource(String sourceKey) {
        PocSource source = POC_SOURCES.get(sourceKey);
        if (source == null) {
            return new UpdateResult(false, "未知的POC源: " + sourceKey, 0, 0);
        }
        
        return updateFromUrl(source.getUrl(), source.getFormat(), source.getName());
    }
    
    /**
     * 从URL下载并更新POC
     */
    public UpdateResult updateFromUrl(String urlStr, String format, String sourceName) {
        Path tempFile = null;
        int newCount = 0;
        int updatedCount = 0;
        
        try {
            reportProgress("正在连接: " + sourceName);
            
            // 下载ZIP文件
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "PotatoTool/1.0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return new UpdateResult(false, "下载失败: HTTP " + responseCode, 0, 0);
            }
            
            int contentLength = conn.getContentLength();
            reportProgress("正在下载... (大小: " + formatSize(contentLength) + ")");
            
            // 保存到临时文件
            tempFile = Files.createTempFile("poc_update_", ".zip");
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    
                    if (contentLength > 0) {
                        int percent = (int) (totalRead * 100 / contentLength);
                        reportPercent(percent);
                    }
                }
            }
            
            reportProgress("下载完成，正在解压...");
            
            // 解压并导入
            Path targetDir = pocDirectory.resolve(format);
            Files.createDirectories(targetDir);
            
            int[] counts = extractAndImport(tempFile, targetDir, format);
            newCount = counts[0];
            updatedCount = counts[1];
            
            reportProgress("更新完成: 新增 " + newCount + " 个, 更新 " + updatedCount + " 个");
            
            return new UpdateResult(true, "更新成功", newCount, updatedCount);
            
        } catch (Exception e) {
            return new UpdateResult(false, "更新失败: " + e.getMessage(), newCount, updatedCount);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }
    
    /**
     * 解压并导入POC文件
     */
    private int[] extractAndImport(Path zipFile, Path targetDir, String format) throws IOException {
        int newCount = 0;
        int updatedCount = 0;
        
        try (ZipInputStream zis = new ZipInputStream(
                Files.newInputStream(zipFile), StandardCharsets.UTF_8)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                String name = entry.getName();
                
                // 只处理POC文件
                if (!isPocFile(name, format)) {
                    continue;
                }
                
                // 提取文件名（去掉仓库前缀目录）
                String fileName = extractFileName(name);
                if (fileName == null) {
                    continue;
                }
                
                Path targetFile = targetDir.resolve(fileName);
                
                // 确保父目录存在
                Files.createDirectories(targetFile.getParent());
                
                boolean exists = Files.exists(targetFile);
                
                // 写入文件
                try (OutputStream out = Files.newOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
                
                if (exists) {
                    updatedCount++;
                } else {
                    newCount++;
                }
                
                zis.closeEntry();
            }
        }
        
        return new int[]{newCount, updatedCount};
    }
    
    /**
     * 检查是否是POC文件
     */
    private boolean isPocFile(String name, String format) {
        name = name.toLowerCase();
        
        switch (format) {
            case "nuclei":
                return name.endsWith(".yaml") || name.endsWith(".yml");
            case "xray":
                return name.endsWith(".yml") || name.endsWith(".yaml");
            case "goby":
                return name.endsWith(".json");
            case "pocsuite":
                return name.endsWith(".py");
            default:
                return name.endsWith(".yaml") || name.endsWith(".yml") || 
                       name.endsWith(".json") || name.endsWith(".py");
        }
    }
    
    /**
     * 提取文件名（去掉仓库根目录前缀）
     */
    private String extractFileName(String fullPath) {
        // 格式: repo-name-branch/path/to/file.yaml
        int firstSlash = fullPath.indexOf('/');
        if (firstSlash > 0 && firstSlash < fullPath.length() - 1) {
            return fullPath.substring(firstSlash + 1);
        }
        return fullPath;
    }
    
    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 0) return "未知";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
    
    private void reportProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
        System.out.println("[POC更新] " + message);
    }
    
    private void reportPercent(int percent) {
        if (percentCallback != null) {
            percentCallback.accept(percent);
        }
    }
    
    /**
     * POC源信息
     */
    public static class PocSource {
        private final String name;
        private final String url;
        private final String format;
        private final String description;
        
        public PocSource(String name, String url, String format, String description) {
            this.name = name;
            this.url = url;
            this.format = format;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getFormat() { return format; }
        public String getDescription() { return description; }
    }
    
    /**
     * 更新结果
     */
    public static class UpdateResult {
        private final boolean success;
        private final String message;
        private final int newCount;
        private final int updatedCount;
        
        public UpdateResult(boolean success, String message, int newCount, int updatedCount) {
            this.success = success;
            this.message = message;
            this.newCount = newCount;
            this.updatedCount = updatedCount;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getNewCount() { return newCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getTotalCount() { return newCount + updatedCount; }
    }
}
