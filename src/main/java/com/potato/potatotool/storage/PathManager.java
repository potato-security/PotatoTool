package com.potato.potatotool.storage;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 路径管理器 - 统一管理所有文件路径
 * 实现配置固定、资源可迁移的两级存储结构
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class PathManager {
    
    private static PathManager instance;
    
    private static final String CONFIG_FOLDER = ".PotatoTool";
    private static final String CONFIG_FILE = "config.json";
    private static final String RESOURCES_FOLDER = "resources";
    
    // 资源文件名常量
    public static final String BCPROV_JAR = "bcprov.jar";
    public static final String IP2REGION_FILE = "ip2region.xdb";
    public static final String MD5_DATABASE_FILE = "md5_database.db";
    public static final String MD5_DATABASE_GZIP = "md5_database.db.gzip";
    
    // 漏洞扫描相关
    public static final String VULNSCAN_FOLDER = "vulnscan";
    public static final String VULNSCAN_DB_FILE = "vulnscan.db";
    public static final String POC_DB_FILE = "poc.db";
    public static final String POC_DB_GZIP = "poc.db.gzip";
    
    // 资源文件期望大小常量（用于完整性检查）
    public static final long BCPROV_EXPECTED_SIZE = (long) (7.9 * 1024 * 1024);      // 7.9MB
    public static final long IP2REGION_EXPECTED_SIZE = (long) (10.5 * 1024 * 1024);  // 10.5MB
    public static final long WINKB_EXPECTED_SIZE = (long) (70.5 * 1024 * 1024);      // 70.5MB
    public static final long MD5_DB_MIN_SIZE = (long) (1.66 * 1024 * 1024 * 1024);   // 1.66GB（最小完整大小）
    
    private final Path configBasePath;   // 固定：user.home/.PotatoTool
    private Path resourcePath;           // 可配置：默认 configBasePath/resources
    
    private PathManager() {
        // 配置基础路径固定在用户目录
        this.configBasePath = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
        
        // 初始化资源路径
        initializeResourcePath();
    }
    
    /**
     * 获取PathManager单例
     */
    public static synchronized PathManager getInstance() {
        if (instance == null) {
            instance = new PathManager();
        }
        return instance;
    }
    
    /**
     * 初始化资源路径
     * 优先级：
     * 1. config.json顶层的ResourcePath配置
     * 2. 环境变量 POTATO_TOOL_DATA
     * 3. 默认路径 user.home/.PotatoTool/resources
     */
    private void initializeResourcePath() {
        try {
            // 1. 尝试从配置文件读取（顶层的ResourcePath）
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            if (config != null && config.has(ConfigConstants.UPDATE_RESOURCE_PATH)) {
                String customPath = config.get(ConfigConstants.UPDATE_RESOURCE_PATH).getAsString();
                if (customPath != null && !customPath.isEmpty()) {
                    Path path = Paths.get(customPath);
                    if (validatePath(path, false)) {
                        this.resourcePath = path;
                        if (debugMode) System.out.println("使用自定义资源路径: " + path);
                        return;
                    } else {
                        System.err.println("自定义资源路径无效，使用默认路径: " + customPath);
                    }
                }
            }
            
            // 2. 尝试从环境变量读取
            String envPath = System.getenv("POTATO_TOOL_DATA");
            if (envPath != null && !envPath.isEmpty()) {
                Path path = Paths.get(envPath);
                if (validatePath(path, false)) {
                    this.resourcePath = path;
                    if (debugMode) System.out.println("使用环境变量资源路径: " + path);
                    return;
                }
            }
            
            // 3. 使用默认路径
            this.resourcePath = configBasePath.resolve(RESOURCES_FOLDER);
            Files.createDirectories(this.resourcePath);
            // 使用默认路径不输出（减少日志）
            
        } catch (Exception e) {
            // 如果出错，使用默认路径
            this.resourcePath = configBasePath.resolve(RESOURCES_FOLDER);
            System.err.println("初始化资源路径失败，使用默认路径: " + e.getMessage());
        }
    }
    
    /**
     * 验证路径是否可用
     * @param path 要验证的路径
     * @param createIfNotExists 如果不存在是否创建
     * @return true if valid and accessible
     */
    public boolean validatePath(Path path, boolean createIfNotExists) {
        try {
            if (!Files.exists(path)) {
                if (createIfNotExists) {
                    Files.createDirectories(path);
                } else {
                    return false;
                }
            }
            
            // 检查读写权限
            if (!Files.isReadable(path) || !Files.isWritable(path)) {
                return false;
            }
            
            // 检查磁盘空间（至少需要3GB，因为md5数据库就接近2GB）
            File file = path.toFile();
            long usableSpace = file.getUsableSpace();
            long minRequiredSpace = 3L * 1024 * 1024 * 1024;  // 3GB
            if (usableSpace < minRequiredSpace) {
                System.err.println("磁盘空间不足: " + formatSize(usableSpace) + 
                                 "，至少需要: " + formatSize(minRequiredSpace));
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("验证路径失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取配置文件基础路径（固定）
     */
    public Path getConfigBasePath() {
        return configBasePath;
    }
    
    /**
     * 获取配置文件路径
     */
    public Path getConfigFilePath() {
        return configBasePath.resolve(CONFIG_FILE);
    }
    
    /**
     * 获取资源文件基础路径（可配置）
     */
    public Path getResourceBasePath() {
        return resourcePath;
    }
    
    /**
     * 获取特定资源文件路径
     * @param resourceName 资源名称（如 "md5_database.db"）
     */
    public Path getResourcePath(String resourceName) {
        return resourcePath.resolve(resourceName);
    }
    
    /**
     * 获取BC加密库路径（放在资源目录，支持迁移）
     */
    public Path getBcprovPath() {
        return resourcePath.resolve(BCPROV_JAR);
    }
    
    /**
     * 获取IP2Region数据库路径
     */
    public Path getIp2RegionPath() {
        return resourcePath.resolve(IP2REGION_FILE);
    }
    
    /**
     * 获取MD5数据库路径
     */
    public Path getMd5DatabasePath() {
        return resourcePath.resolve(MD5_DATABASE_FILE);
    }
    
    /**
     * 获取MD5数据库压缩文件路径
     */
    public Path getMd5DatabaseGzipPath() {
        return resourcePath.resolve(MD5_DATABASE_GZIP);
    }
    
    /**
     * 获取临时文件目录
     */
    public Path getTempPath() {
        try {
            Path temp = resourcePath.resolve("temp");
            Files.createDirectories(temp);
            return temp;
        } catch (IOException e) {
            // 降级到系统临时目录
            return Paths.get(System.getProperty("java.io.tmpdir"), "PotatoTool");
        }
    }
    
    /**
     * 更改资源路径（需要重启生效）
     * @param newPath 新的资源路径
     * @return 是否设置成功
     */
    public boolean changeResourcePath(String newPath) {
        try {
            Path path = Paths.get(newPath);
            
            // 验证新路径
            if (!validatePath(path, true)) {
                System.err.println("新路径验证失败: " + newPath);
                return false;
            }
            
            // 保存到配置文件顶层
            Constants.saveConfig(ConfigConstants.UPDATE_RESOURCE_PATH, newPath);
            this.resourcePath = path;
            
            // 成功保存，不输出日志（减少日志）
            return true;
            
        } catch (Exception e) {
            System.err.println("更改资源路径失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取路径占用空间
     */
    public long getDirectorySize(Path path) {
        try {
            if (!Files.exists(path)) {
                return 0;
            }
            
            return Files.walk(path)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 获取可用磁盘空间
     */
    public long getAvailableSpace(Path path) {
        try {
            return path.toFile().getUsableSpace();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 格式化大小
     */
    public static String formatSize(long size) {
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
     * 检查是否有足够的磁盘空间
     * @param requiredSpace 需要的空间（字节）
     * @return true if enough space
     */
    public boolean hasEnoughSpace(long requiredSpace) {
        long available = getAvailableSpace(resourcePath);
        return available >= requiredSpace;
    }
    
    /**
     * 迁移资源文件到新位置
     * @param newPath 新位置
     * @param callback 进度回调
     * @return 是否成功
     */
    public boolean migrateResources(Path newPath, MigrationCallback callback) {
        try {
            // 验证新路径
            if (!validatePath(newPath, true)) {
                callback.onError(I18nUtils.getString("update.exception.path.invalid"));
                return false;
            }
            
            // 检查空间
            long currentSize = getDirectorySize(resourcePath);
            long availableSpace = getAvailableSpace(newPath);
            if (availableSpace < currentSize) {
                callback.onError(I18nUtils.getString("update.exception.disk.space",
                               formatSize(currentSize), formatSize(availableSpace)));
                return false;
            }
            
            List<Path> fileList = new ArrayList<Path>();
            try (Stream<Path> stream = Files.walk(resourcePath)) {
                stream.filter(Files::isRegularFile).forEach(fileList::add);
            }

            if (fileList.isEmpty()) {
                changeResourcePath(newPath.toString());
                callback.onProgress(0, 0, I18nUtils.getString("update.exception.no.files"));
                callback.onComplete();
                return true;
            }

            int total = fileList.size();
            int current = 0;

            for (Path source : fileList) {
                Path relativePath = resourcePath.relativize(source);
                Path target = newPath.resolve(relativePath);

                callback.onProgress(current, total, relativePath.toString());

                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.copy(source, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);

                current++;
            }
            
            // 更新配置
            changeResourcePath(newPath.toString());
            
            callback.onComplete();
            return true;
            
        } catch (Exception e) {
            callback.onError(I18nUtils.getString("update.exception.migration.failed", e.getMessage()));
            return false;
        }
    }
    
    /**
     * 迁移进度回调接口
     */
    public interface MigrationCallback {
        void onProgress(int current, int total, String message);
        void onComplete();
        void onError(String error);
    }
}
