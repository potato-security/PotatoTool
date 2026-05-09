package com.potato.potatotool.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.manifest.Manifest;
import com.potato.potatotool.update.manifest.ManifestFetcher;
import com.potato.potatotool.update.manifest.VersionComparator;
import com.potato.potatotool.utils.core.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 更新检查器 - 检查软件和资源更新
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class UpdateChecker {
    
    private final ManifestFetcher manifestFetcher;
    
    public UpdateChecker() {
        this.manifestFetcher = new ManifestFetcher();
    }
    
    /**
     * 检查更新
     * @return 更新信息
     * @throws Exception 检查失败
     */
    public UpdateInfo checkForUpdates() throws Exception {
        // 1. 获取远程清单
        Manifest manifest = manifestFetcher.fetchManifest();
        
        // 2. 同步本地已存在的资源文件到config.json
        syncLocalResources(manifest);
        
        // 3. 获取本地版本信息（同步后重新获取）
        String localAppVersion = getLocalAppVersion();
        Map<String, String> localResourceVersions = getLocalResourceVersions();
        
        // 4. 对比软件版本
        boolean appNeedUpdate = false;
        if (manifest.getApp() != null && manifest.getApp().getVersion() != null) {
            String remoteVersion = manifest.getApp().getVersion();
            try {
                appNeedUpdate = VersionComparator.isNewer(remoteVersion, localAppVersion);
                if (appNeedUpdate) {
                    if (debugMode) System.out.println("发现软件更新: " + localAppVersion + " -> " + remoteVersion);
                }
            } catch (Exception e) {
                System.err.println("版本比对失败: " + e.getMessage());
            }
        }
        
        // 4. 对比资源版本
        List<ResourceUpdate> resourceUpdates = new ArrayList<>();
        if (manifest.getResources() != null) {
            for (Manifest.ResourceItem resource : manifest.getResources()) {
                String resourceName = resource.getName();
                String remoteVersion = resource.getVersion();
                String localVersion = localResourceVersions.get(resourceName);
                
                boolean needUpdate = false;
                if (localVersion == null || localVersion.isEmpty()) {
                    // 本地没有此资源
                    needUpdate = true;
                } else if (!localVersion.equals(remoteVersion)) {
                    // 版本不一致
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    // 检查该资源是否被跳过（仅用于日志输出）
                    boolean isSkipped = isResourceSkipped(resourceName, remoteVersion);
                    if (isSkipped) {
                        if (debugMode) System.out.println("跳过资源更新提示: " + resource.getDisplayName() + 
                                         " v" + remoteVersion + " (用户已选择跳过)");
                    }
                    
                    // 即使被跳过，也要添加到更新列表中，让UI层决定是否默认勾选
                    resourceUpdates.add(new ResourceUpdate(
                        resource,
                        localVersion,
                        remoteVersion
                    ));
                    
                    if (!isSkipped) {
                        if (debugMode) System.out.println("发现资源更新: " + resource.getDisplayName() + 
                                         " (" + (localVersion == null ? "未安装" : localVersion) + 
                                         " -> " + remoteVersion + ")");
                    }
                }
            }
        }
        
        // 5. 更新最后检查时间
        updateLastCheckMetadata(manifest);
        
        return new UpdateInfo(
            appNeedUpdate,
            manifest.getApp(),
            localAppVersion,
            resourceUpdates,
            manifest
        );
    }
    
    /**
     * 获取本地软件版本
     * 直接从 JAR 内置的 config.json 读取版本号
     */
    private String getLocalAppVersion() {
        try {
            // 从 JAR 内置的 resources/conf/config.json 读取
            String configContent = Constants.getResourceString("config");
            if (configContent != null && !configContent.isEmpty()) {
                JsonObject config = com.google.gson.JsonParser.parseString(configContent).getAsJsonObject();
                if (config.has(ConfigConstants.UPDATE_APP_VERSION)) {
                    String version = config.get(ConfigConstants.UPDATE_APP_VERSION).getAsString();
                    if (debugMode) System.out.println("读取内置版本: " + version);
                    return version;
                }
            }
            
            // 如果读取失败，返回默认版本
            System.err.println("无法从内置 config.json 读取版本，使用默认值");
            return "2.5";
            
        } catch (Exception e) {
            System.err.println("获取本地版本失败: " + e.getMessage());
            return "2.5";
        }
    }
    
    /**
     * 获取本地资源版本映射
     */
    private Map<String, String> getLocalResourceVersions() {
        Map<String, String> versions = new HashMap<>();
        
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(ConfigConstants.UPDATE);
            if (config != null && config.has(ConfigConstants.UPDATE_RESOURCES)) {
                JsonObject resources = config.getAsJsonObject(ConfigConstants.UPDATE_RESOURCES);
                
                for (String key : resources.keySet()) {
                    JsonObject resourceInfo = resources.getAsJsonObject(key);
                    if (resourceInfo.has(ConfigConstants.UPDATE_VERSION)) {
                        String version = resourceInfo.get(ConfigConstants.UPDATE_VERSION).getAsString();
                        versions.put(key, version);
                    }
                }
            }
            
            
        } catch (Exception e) {
            System.err.println("获取本地资源版本失败: " + e.getMessage());
        }
        
        return versions;
    }
    
    /**
     * 更新最后检查时间
     */
    private void updateLastCheckMetadata(Manifest manifest) {
        try {
            String currentTime = java.time.Instant.now().toString();
            
            // 直接保存到UpDate节点下
            Map<String, Object> updateMap = new LinkedHashMap<>();
            updateMap.put(ConfigConstants.UPDATE_LAST_CHECK_TIME, currentTime);
            if (manifest != null) {
                if (manifest.getSourceUrl() != null && !manifest.getSourceUrl().isEmpty()) {
                    updateMap.put(ConfigConstants.UPDATE_LAST_MANIFEST_SOURCE, manifest.getSourceUrl());
                }
                if (manifest.getVersion() != null && !manifest.getVersion().isEmpty()) {
                    updateMap.put(ConfigConstants.UPDATE_LAST_MANIFEST_VERSION, manifest.getVersion());
                }
            }
            
            Constants.saveConfig(updateMap, ConfigConstants.UPDATE);
            
        } catch (Exception e) {
            System.err.println("更新检查时间失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步本地已存在的资源文件到config.json（快速模式）
     * 只检查文件存在性，不验证哈希值，以加快启动速度
     * 
     * @param manifest 云端清单
     */
    private void syncLocalResources(Manifest manifest) {
        if (manifest == null || manifest.getResources() == null) {
            return;
        }
        
        try {
            PathManager pathManager = PathManager.getInstance();
            JsonObject config = (JsonObject) Constants.getOutsideConfig(ConfigConstants.UPDATE);
            JsonObject resources = null;
            
            if (config != null && config.has(ConfigConstants.UPDATE_RESOURCES)) {
                resources = config.getAsJsonObject(ConfigConstants.UPDATE_RESOURCES);
            }
            
            boolean needSave = false;
            // 使用LinkedHashMap保证字段顺序
            Map<String, Object> updateMap = new LinkedHashMap<>();
            
            // 如果resources对象不存在，创建一个新的
            if (resources == null) {
                resources = new JsonObject();
                needSave = true;
            }
            
            // 遍历manifest中的所有资源
            for (Manifest.ResourceItem resource : manifest.getResources()) {
                String resourceName = resource.getName();
                String remoteVersion = resource.getVersion();
                
                // 检查本地文件是否存在
                Path localFile = getResourceLocalPath(pathManager, resource);
                if (localFile == null || !Files.exists(localFile)) {
                    // 文件不存在，跳过
                    continue;
                }
                
                // 验证文件大小（确保文件完整性）
                // 对于固定文件名的资源（如md5_database.db），大小验证很重要
                if (resource.getFiles() != null) {
                    try {
                        long actualSize = Files.size(localFile);
                        long expectedSize;
                        
                        // 关键：如果是压缩资源，本地文件是解压后的，应该用uncompressedSize验证
                        if (resource.getFiles().isCompressed() && resource.getFiles().getUncompressedSize() > 0) {
                            expectedSize = resource.getFiles().getUncompressedSize();
                        } else if (resource.getFiles().getSize() > 0) {
                            expectedSize = resource.getFiles().getSize();
                        } else {
                            // 没有提供size信息，跳过验证
                            continue;
                        }
                        
                        // 允许1MB的误差范围
                        long tolerance = 1024 * 1024;
                        if (Math.abs(actualSize - expectedSize) > tolerance) {
                            if (debugMode) System.out.println("跳过资源同步: " + resource.getDisplayName() + 
                                             " (文件大小不匹配: 实际=" + formatSize(actualSize) + 
                                             ", 预期=" + formatSize(expectedSize) + ")");
                            continue; // 大小不匹配，跳过此资源
                        }
                    } catch (IOException e) {
                        System.err.println("无法读取文件大小: " + localFile + ", " + e.getMessage());
                        continue;
                    }
                }
                
                // 快速模式：只检查文件存在性和大小，不验证哈希值（提升启动速度）
                // 检查config.json中的记录是否需要更新
                boolean needUpdate = false;
                String configVersion = null;
                
                if (resources.has(resourceName) && 
                    resources.getAsJsonObject(resourceName).has(ConfigConstants.UPDATE_VERSION)) {
                    configVersion = resources.getAsJsonObject(resourceName)
                                           .get(ConfigConstants.UPDATE_VERSION).getAsString();
                    // 如果config.json中已有版本记录，信任用户配置，不自动覆盖
                    // 只有当版本比manifest更旧时才提示（但不强制同步）
                    if (!remoteVersion.equals(configVersion)) {
                        if (debugMode) System.out.println("本地资源版本与云端不同: " + resource.getDisplayName() + 
                                         " (本地: " + configVersion + ", 云端: " + remoteVersion + ")，保持本地配置");
                    }
                    // config.json已有记录，不需要更新（信任本地配置）
                    needUpdate = false;
                } else {
                    // config.json中没有记录，需要添加
                    needUpdate = true;
                    if (debugMode) System.out.println("检测到本地资源文件: " + resource.getDisplayName() + 
                                     " (版本: " + remoteVersion + ")，添加配置记录");
                }
                
                if (needUpdate) {
                    // 提取文件名（不包含路径）
                    String fileName = localFile.getFileName().toString();
                    
                    // 准备资源信息，使用LinkedHashMap保证字段顺序
                    Map<String, Object> resourceInfo = new LinkedHashMap<>();
                    resourceInfo.put(ConfigConstants.UPDATE_VERSION, remoteVersion);
                    resourceInfo.put(ConfigConstants.UPDATE_FILE_NAME, fileName);
                    
                    // 保存checksum信息（从manifest获取），使用LinkedHashMap保证字段顺序
                    if (resource.getFiles() != null && resource.getFiles().getChecksum() != null) {
                        Manifest.Checksum checksum = resource.getFiles().getChecksum();
                        Map<String, String> checksumMap = new LinkedHashMap<>();
                        if (checksum.getSha256() != null) {
                            checksumMap.put(ConfigConstants.UPDATE_CHECKSUM_SHA256, checksum.getSha256());
                        }
                        if (checksum.getMd5() != null) {
                            checksumMap.put(ConfigConstants.UPDATE_CHECKSUM_MD5, checksum.getMd5());
                        }
                        if (!checksumMap.isEmpty()) {
                            resourceInfo.put(ConfigConstants.UPDATE_CHECKSUM, checksumMap);
                        }
                    }
                    
                    // 同时更新内存中的JsonObject（用于后续判断）
                    JsonObject resourceObj = new JsonObject();
                    resourceObj.addProperty(ConfigConstants.UPDATE_VERSION, remoteVersion);
                    resourceObj.addProperty(ConfigConstants.UPDATE_FILE_NAME, fileName);
                    resources.add(resourceName, resourceObj);
                    
                    // 添加到待保存的map，使用LinkedHashMap保证字段顺序
                    if (!updateMap.containsKey(ConfigConstants.UPDATE_RESOURCES)) {
                        updateMap.put(ConfigConstants.UPDATE_RESOURCES, new LinkedHashMap<String, Object>());
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resourcesMap = (Map<String, Object>) updateMap.get(ConfigConstants.UPDATE_RESOURCES);
                    resourcesMap.put(resourceName, resourceInfo);
                    
                    needSave = true;
                }
            }
            
            // 保存配置（合并模式，不覆盖已存在的其他资源）
            if (needSave && !updateMap.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resourcesMap = (Map<String, Object>) updateMap.get(ConfigConstants.UPDATE_RESOURCES);
                if (resourcesMap != null && !resourcesMap.isEmpty()) {
                    Constants.saveResourceConfigMerge(resourcesMap);
                }
                if (debugMode) System.out.println("本地资源配置已同步");
                // 清除缓存，强制重新读取
                Constants.cachedConfig = null;
            }
            
        } catch (Exception e) {
            System.err.println("同步本地资源失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 根据资源项获取本地文件路径
     * 
     * @param pathManager 路径管理器
     * @param resource 资源项
     * @return 本地文件路径，如果无法确定则返回null
     */
    private Path getResourceLocalPath(PathManager pathManager, Manifest.ResourceItem resource) {
        // 如果manifest中指定了localPath，优先使用它
        if (resource.getFiles() != null && resource.getFiles().getLocalPath() != null) {
            String localPath = resource.getFiles().getLocalPath();
            return pathManager.getResourcePath(localPath);
        }
        
        // 否则根据资源名称推断
        String resourceName = resource.getName();
        switch (resourceName) {
            case "md5":
            case "MD5Database":
                return pathManager.getMd5DatabasePath();
            case "ip2region":
                return pathManager.getIp2RegionPath();
            case "bcprov":
                return pathManager.getBcprovPath();
            case "winKbInfo":
                // Windows补丁信息库，根据版本推断文件名
                if (resource.getVersion() != null) {
                    return pathManager.getResourcePath("winKbInfo" + resource.getVersion() + ".csv");
                }
                return null;
            default:
                // 尝试从displayName或name推断文件名
                if (resource.getDisplayName() != null) {
                    return pathManager.getResourcePath(resource.getDisplayName());
                }
                return pathManager.getResourcePath(resourceName);
        }
    }
    
    /**
     * 检查资源是否被用户跳过
     */
    private boolean isResourceSkipped(String resourceName, String version) {
        try {
            JsonObject updateConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.UPDATE);
            if (updateConfig != null && updateConfig.has(ConfigConstants.UPDATE_RESOURCES)) {
                JsonObject resources = updateConfig.getAsJsonObject(ConfigConstants.UPDATE_RESOURCES);
                if (resources.has(resourceName)) {
                    JsonObject resourceConfig = resources.getAsJsonObject(resourceName);
                    // 检查skippedVersions数组
                    if (resourceConfig.has(ConfigConstants.UPDATE_SKIPPED_VERSIONS)) {
                        JsonElement skippedElem = resourceConfig.get(ConfigConstants.UPDATE_SKIPPED_VERSIONS);
                        if (skippedElem.isJsonArray()) {
                            for (JsonElement elem : skippedElem.getAsJsonArray()) {
                                if (elem.getAsString().equals(version)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 格式化文件大小显示
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
     * 获取平台标识
     * 注意：PotatoTool的JAR是跨平台的，只需区分JDK版本
     * @return 平台标识，如 "jdk8", "jdk11"
     */
    public static String getPlatformIdentifier() {
        String javaVersion = System.getProperty("java.version");
        
        // 只区分JDK版本，不区分操作系统（因为JAR是跨平台的）
        String jdkVersion;
        if (javaVersion.startsWith("1.8")) {
            jdkVersion = "jdk8";
        } else {
            // JDK 11, 17, 21等都使用jdk11的JAR
            jdkVersion = "jdk11";
        }
        
        return jdkVersion;
    }
}
