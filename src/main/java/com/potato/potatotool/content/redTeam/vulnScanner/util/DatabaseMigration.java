package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.utils.data.StrUtils;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库迁移工具
 * 负责将旧版本的数据库从JAR目录迁移到ResourcePath/vulnscan/
 * 
 * 旧路径: {JAR目录}/.PotatoTool/vulnscan.db
 * 新路径: {ResourcePath}/vulnscan/vulnscan.db
 * 
 * @author Potato
 * @date 2025/11/25
 */
public class DatabaseMigration {
    
    private static final String DB_FOLDER = "vulnscan";
    private static final String DB_FILE = "vulnscan.db";
    private static final String OLD_CONFIG_FOLDER = ".PotatoTool";
    
    /**
     * 迁移回调接口
     */
    public interface MigrationCallback {
        void onStart(String message);
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String message, Exception e);
    }
    
    /**
     * 迁移结果
     */
    public static class MigrationResult {
        private final boolean success;
        private final String message;
        private final Path oldPath;
        private final Path newPath;
        private final boolean migrated;  // 是否实际执行了迁移
        
        public MigrationResult(boolean success, String message, Path oldPath, Path newPath, boolean migrated) {
            this.success = success;
            this.message = message;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.migrated = migrated;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Path getOldPath() { return oldPath; }
        public Path getNewPath() { return newPath; }
        public boolean isMigrated() { return migrated; }
    }
    
    /**
     * 获取旧数据库路径（JAR目录）
     */
    public static Path getOldDatabasePath() {
        String jarDir = StrUtils.getCurrentJarDir();
        return Paths.get(jarDir, OLD_CONFIG_FOLDER, DB_FILE);
    }
    
    /**
     * 获取新数据库目录（ResourcePath/vulnscan/）
     */
    public static Path getNewDatabaseDirectory() {
        return PathManager.getInstance().getResourcePath(DB_FOLDER);
    }
    
    /**
     * 获取新数据库路径
     */
    public static Path getNewDatabasePath() {
        return getNewDatabaseDirectory().resolve(DB_FILE);
    }
    
    /**
     * 检查是否需要迁移
     * @return true 如果旧数据库存在且新数据库不存在
     */
    public static boolean needsMigration() {
        Path oldPath = getOldDatabasePath();
        Path newPath = getNewDatabasePath();
        
        return Files.exists(oldPath) && !Files.exists(newPath);
    }
    
    /**
     * 检查旧数据库是否存在
     */
    public static boolean hasOldDatabase() {
        return Files.exists(getOldDatabasePath());
    }
    
    /**
     * 检查新数据库是否存在
     */
    public static boolean hasNewDatabase() {
        return Files.exists(getNewDatabasePath());
    }
    
    /**
     * 执行数据库迁移
     * @return 迁移结果
     */
    public static MigrationResult migrate() {
        return migrate(null);
    }
    
    /**
     * 执行数据库迁移（带回调）
     * @param callback 迁移进度回调
     * @return 迁移结果
     */
    public static MigrationResult migrate(MigrationCallback callback) {
        Path oldPath = getOldDatabasePath();
        Path newPath = getNewDatabasePath();
        Path newDir = getNewDatabaseDirectory();
        
        if (callback != null) {
            callback.onStart("开始检查数据库迁移...");
        }
        
        // 检查新数据库是否已存在
        if (Files.exists(newPath)) {
            String msg = "新数据库已存在，无需迁移: " + newPath;
            if (callback != null) {
                callback.onSuccess(msg);
            }
            return new MigrationResult(true, msg, oldPath, newPath, false);
        }
        
        // 检查旧数据库是否存在
        if (!Files.exists(oldPath)) {
            String msg = "旧数据库不存在，将创建新数据库: " + newPath;
            if (callback != null) {
                callback.onProgress(msg);
            }
            
            // 创建新数据库目录
            try {
                Files.createDirectories(newDir);
                return new MigrationResult(true, msg, oldPath, newPath, false);
            } catch (IOException e) {
                String errMsg = "创建数据库目录失败: " + e.getMessage();
                if (callback != null) {
                    callback.onError(errMsg, e);
                }
                return new MigrationResult(false, errMsg, oldPath, newPath, false);
            }
        }
        
        // 执行迁移
        if (callback != null) {
            callback.onProgress("正在迁移数据库: " + oldPath + " -> " + newPath);
        }
        
        try {
            // 创建目标目录
            Files.createDirectories(newDir);
            
            // 复制数据库文件
            Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 验证迁移后的数据库
            if (verifyDatabase(newPath)) {
                String msg = "数据库迁移成功: " + newPath;
                if (callback != null) {
                    callback.onSuccess(msg);
                }
                
                // 可选：删除旧数据库（保留备份）
                // Files.delete(oldPath);
                
                return new MigrationResult(true, msg, oldPath, newPath, true);
            } else {
                // 验证失败，删除已复制的文件
                Files.deleteIfExists(newPath);
                String errMsg = "数据库迁移验证失败";
                if (callback != null) {
                    callback.onError(errMsg, null);
                }
                return new MigrationResult(false, errMsg, oldPath, newPath, false);
            }
            
        } catch (IOException e) {
            String errMsg = "数据库迁移失败: " + e.getMessage();
            if (callback != null) {
                callback.onError(errMsg, e);
            }
            return new MigrationResult(false, errMsg, oldPath, newPath, false);
        }
    }
    
    /**
     * 验证数据库文件是否有效
     * @param dbPath 数据库路径
     * @return 是否有效
     */
    public static boolean verifyDatabase(Path dbPath) {
        if (!Files.exists(dbPath)) {
            return false;
        }
        
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dbPath.toString();
            
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                // 尝试执行简单查询验证数据库完整性
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("数据库验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前使用的数据库路径
     * 优先使用新路径，如果不存在则使用旧路径，都不存在则返回新路径（待创建）
     */
    public static Path getCurrentDatabasePath() {
        Path newPath = getNewDatabasePath();
        Path oldPath = getOldDatabasePath();
        
        if (Files.exists(newPath)) {
            return newPath;
        } else if (Files.exists(oldPath)) {
            return oldPath;
        } else {
            return newPath;  // 返回新路径用于创建
        }
    }
    
    /**
     * 确保数据库目录存在
     * @return 数据库目录路径
     */
    public static Path ensureDatabaseDirectory() throws IOException {
        Path dir = getNewDatabaseDirectory();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }
    
    /**
     * 获取数据库状态信息
     */
    public static String getDatabaseStatus() {
        StringBuilder sb = new StringBuilder();
        
        Path oldPath = getOldDatabasePath();
        Path newPath = getNewDatabasePath();
        
        sb.append("数据库状态信息:\n");
        sb.append("  旧路径: ").append(oldPath).append("\n");
        sb.append("  旧库存在: ").append(Files.exists(oldPath)).append("\n");
        sb.append("  新路径: ").append(newPath).append("\n");
        sb.append("  新库存在: ").append(Files.exists(newPath)).append("\n");
        sb.append("  需要迁移: ").append(needsMigration()).append("\n");
        sb.append("  当前使用: ").append(getCurrentDatabasePath()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * 备份数据库
     * @param backupSuffix 备份文件后缀（如 ".backup" 或时间戳）
     * @return 备份文件路径，失败返回null
     */
    public static Path backupDatabase(String backupSuffix) {
        Path currentPath = getCurrentDatabasePath();
        
        if (!Files.exists(currentPath)) {
            return null;
        }
        
        String backupName = DB_FILE + (backupSuffix != null ? backupSuffix : ".backup");
        Path backupPath = currentPath.getParent().resolve(backupName);
        
        try {
            Files.copy(currentPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("数据库已备份: " + backupPath);
            return backupPath;
        } catch (IOException e) {
            System.err.println("数据库备份失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从备份恢复数据库
     * @param backupPath 备份文件路径
     * @return 是否恢复成功
     */
    public static boolean restoreFromBackup(Path backupPath) {
        if (!Files.exists(backupPath)) {
            System.err.println("备份文件不存在: " + backupPath);
            return false;
        }
        
        Path targetPath = getNewDatabasePath();
        
        try {
            Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("数据库已恢复: " + targetPath);
            return true;
        } catch (IOException e) {
            System.err.println("数据库恢复失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除旧数据库（迁移成功后调用）
     * @return 是否删除成功
     */
    public static boolean deleteOldDatabase() {
        Path oldPath = getOldDatabasePath();
        
        if (!Files.exists(oldPath)) {
            return true;  // 已经不存在
        }
        
        // 确保新数据库存在且有效
        Path newPath = getNewDatabasePath();
        if (!Files.exists(newPath) || !verifyDatabase(newPath)) {
            System.err.println("新数据库无效，拒绝删除旧数据库");
            return false;
        }
        
        try {
            Files.delete(oldPath);
            System.out.println("旧数据库已删除: " + oldPath);
            return true;
        } catch (IOException e) {
            System.err.println("删除旧数据库失败: " + e.getMessage());
            return false;
        }
    }
}
