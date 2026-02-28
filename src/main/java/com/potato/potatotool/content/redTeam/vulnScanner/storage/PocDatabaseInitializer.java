package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.utils.core.Constants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * POC 数据库初始化器（新方案）
 * 
 * 首次运行：从 JAR 内置 POC 目录读取文件，解析后写入本地 SQLite 数据库
 * 版本更新：对比版本号，仅导入新增的 POC
 * 
 * 内置 POC 目录结构（打包在 JAR 中）：
 * com/potato/potatotool/content/redTeam/vulnScanner/poc/
 * ├── nucleiPoc/
 * ├── gobyPoc/
 * ├── xrayPoc/
 * └── pocsuitePoc/
 * 
 * @author Potato
 * @date 2025/12/19
 */
public class PocDatabaseInitializer {

    private static final String POC_VERSION_KEY = ConfigConstants.VULNSCAN_POC_DB_VERSION;

    private static PocDatabaseInitializer instance;
    private final PathManager pathManager;
    private final PocLoader pocLoader;

    // 初始化状态标志
    private volatile boolean initialized = false;
    private volatile boolean initializing = false;
    private final Object initLock = new Object();

    private PocDatabaseInitializer() {
        this.pathManager = PathManager.getInstance();
        this.pocLoader = new PocLoader();
        this.pocLoader.setSkipInvalidPocs(true);
    }
    
    public static synchronized PocDatabaseInitializer getInstance() {
        if (instance == null) {
            instance = new PocDatabaseInitializer();
        }
        return instance;
    }
    
    /**
     * 初始化 POC 数据库
     * @param callback 进度回调
     * @return 初始化结果
     */
    public InitResult initialize(InitCallback callback) {
        synchronized (initLock) {
            if (initialized) {
                return new InitResult(true, "数据库已初始化", 0);
            }
            if (initializing) {
                // 等待其他线程完成初始化
                try {
                    while (initializing && !initialized) {
                        initLock.wait(1000);
                    }
                    return new InitResult(true, "数据库已由其他线程初始化", 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new InitResult(false, "等待初始化被中断", 0);
                }
            }
            initializing = true;
        }

        try {
            Path dbPath = getDbPath();
            boolean dbExists = Files.exists(dbPath);
            
            if (!dbExists) {
                // 首次运行，从内置 POC 初始化
                if (callback != null) callback.onProgress("首次初始化，正在加载内置 POC...");
                return initializeFromBuiltinPocs(callback);
            }
            
            // 数据库已存在，检查版本
            String localVersion = getLocalVersion();
            String builtinVersion = getBuiltinVersion();
            
            if (builtinVersion != null && !builtinVersion.equals(localVersion)) {
                // 版本不同，进行增量更新
                if (callback != null) callback.onProgress("检测到新版本，正在更新...");
                return differentialUpdateFromBuiltin(callback);
            }
            
            if (callback != null) callback.onProgress("数据库已是最新");
            return new InitResult(true, "数据库已是最新", 0);
            
        } catch (Exception e) {
            System.err.println("POC 数据库初始化失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new InitResult(false, "初始化失败: " + e.getMessage(), 0);
        } finally {
            synchronized (initLock) {
                initialized = true;
                initializing = false;
                initLock.notifyAll();
            }
        }
    }

    /**
     * 等待初始化完成
     * @param timeoutMs 超时时间（毫秒），0 表示无限等待
     * @return 是否初始化完成
     */
    public boolean waitForInitialization(long timeoutMs) {
        synchronized (initLock) {
            if (initialized) {
                return true;
            }

            long startTime = System.currentTimeMillis();
            while (!initialized) {
                try {
                    long waitTime = timeoutMs > 0 ?
                        Math.max(1, timeoutMs - (System.currentTimeMillis() - startTime)) : 0;
                    if (timeoutMs > 0 && waitTime <= 0) {
                        return initialized;
                    }
                    initLock.wait(waitTime > 0 ? waitTime : 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return initialized;
                }
            }
            return initialized;
        }
    }

    /**
     * 检查是否已初始化完成
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 从内置 POC 目录初始化数据库（流式处理，减少内存占用）
     */
    private InitResult initializeFromBuiltinPocs(InitCallback callback) {
        try {
            // 1. 读取内置 POC 文件路径列表
            List<String> pocPaths = getBuiltinPocPaths();
            if (pocPaths.isEmpty()) {
                return new InitResult(false, "未找到内置 POC 文件", 0);
            }

            if (callback != null) callback.onProgress("找到 " + pocPaths.size() + " 个 POC 文件");

            // 2. 流式处理：边加载边插入，减少内存占用
            final int BATCH_SIZE = 100;
            List<PocEntry> batch = new ArrayList<>(BATCH_SIZE);
            List<PocObj.Poc> allPocs = new ArrayList<>(); // 用于生成清单
            List<PocDatabaseManager.FailedEntry> allFailures = new ArrayList<>(); // 收集所有失败
            List<String> allSkippedFiles = new ArrayList<>(); // 收集所有跳过的文件
            int totalInserted = 0;
            int totalSkipped = 0;
            int processed = 0;

            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();

            for (int i = 0; i < pocPaths.size(); i++) {
                String path = pocPaths.get(i);
                String filename = getFileName(path);
                try {
                    String content = readBuiltinPocContent(path);
                    if (content == null || content.trim().isEmpty()) {
                        allFailures.add(new PocDatabaseManager.FailedEntry(
                            filename, "文件内容为空", null));
                        continue;
                    }

                    PocObj.Poc poc = pocLoader.loadFromContent(content, filename, path);
                    if (poc == null) {
                        allFailures.add(new PocDatabaseManager.FailedEntry(
                            filename, "POC解析失败", "返回空对象"));
                        continue;
                    }

                    batch.add(new PocEntry(poc, content, filename));
                    allPocs.add(poc);
                    processed++;

                    // 达到批量大小，执行插入并清空缓存
                    if (batch.size() >= BATCH_SIZE) {
                        PocDatabaseManager.BatchInsertResult result =
                            dbManager.batchInsertPocsWithDetails(batch, "builtin");
                        totalInserted += result.getSuccessCount();
                        totalSkipped += result.getSkipCount();
                        allFailures.addAll(result.getFailedEntries());
                        allSkippedFiles.addAll(result.getSkippedFiles());
                        batch.clear();

                        if (callback != null) {
                            callback.onProgress("已处理 " + processed + "/" + pocPaths.size() +
                                               " (成功 " + totalInserted + ", 跳过 " + totalSkipped +
                                               ", 失败 " + allFailures.size() + ")");
                        }
                    }
                } catch (Exception e) {
                    allFailures.add(new PocDatabaseManager.FailedEntry(
                        filename, "加载异常", e.getMessage()));
                    if (debugMode) System.err.println("加载 POC 失败: " + path + " - " + e.getMessage());
                }
            }

            // 3. 处理剩余的 POC
            if (!batch.isEmpty()) {
                PocDatabaseManager.BatchInsertResult result =
                    dbManager.batchInsertPocsWithDetails(batch, "builtin");
                totalInserted += result.getSuccessCount();
                totalSkipped += result.getSkipCount();
                allFailures.addAll(result.getFailedEntries());
                allSkippedFiles.addAll(result.getSkippedFiles());
                batch.clear();
            }

            // 4. 更新版本号
            String version = getBuiltinVersion();
            if (version != null) {
                updateLocalVersion(version);
            }

            // 5. 通知失败信息
            if (!allFailures.isEmpty() && callback != null) {
                callback.onFailures(allFailures);
            }

            String message = String.format("初始化完成 (成功: %d, 跳过: %d, 失败: %d)",
                                          totalInserted, totalSkipped, allFailures.size());
            if (callback != null) callback.onProgress(message);
            
            // 6. 输出跳过的文件列表（调试模式）
            if (totalSkipped > 0 && debugMode && !allSkippedFiles.isEmpty()) {
                System.out.println("\n跳过的 " + totalSkipped + " 个 POC 文件（content_hash 重复）：");
                for (String file : allSkippedFiles) {
                    System.out.println("  - " + file);
                }
            }

            return new InitResult(true, message, totalInserted, totalSkipped, allFailures);

        } catch (Exception e) {
            System.err.println("从内置 POC 初始化失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new InitResult(false, "初始化失败: " + e.getMessage(), 0);
        }
    }

    /**
     * 从内置 POC 进行增量更新（支持内容变化检测）
     * 使用内容 hash 作为去重依据，而非 POC ID
     */
    private InitResult differentialUpdateFromBuiltin(InitCallback callback) {
        try {
            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();

            // 1. 获取数据库中已有的内容 hash 集合
            Set<String> existingHashes;
            try {
                existingHashes = new HashSet<>(dbManager.getAllContentHashes().values());
                existingHashes.remove(null); // 移除 null 值
            } catch (Exception e) {
                // 数据库可能还未初始化,直接进行完整初始化
                if (debugMode) System.err.println("无法获取已有POC信息,执行完整初始化: " + e.getMessage());
                return initializeFromBuiltinPocs(callback);
            }

            // 2. 读取内置 POC，找出新增或更新的
            List<String> pocPaths = getBuiltinPocPaths();
            List<PocEntry> newEntries = new ArrayList<>();

            for (String path : pocPaths) {
                try {
                    String content = readBuiltinPocContent(path);
                    if (content != null && !content.trim().isEmpty()) {
                        String contentHash = PocDatabaseManager.calculateContentHash(content);

                        // 只有内容不存在时才添加（基于 hash 去重）
                        if (contentHash != null && !existingHashes.contains(contentHash)) {
                            String filename = getFileName(path);
                            PocObj.Poc poc = pocLoader.loadFromContent(content, filename, path);
                            if (poc != null) {
                                newEntries.add(new PocEntry(poc, content, filename));
                            }
                        }
                    }
                } catch (Exception e) {
                    if (debugMode) System.err.println("加载 POC 失败: " + path);
                }
            }

            if (newEntries.isEmpty()) {
                String version = getBuiltinVersion();
                if (version != null) updateLocalVersion(version);
                return new InitResult(true, "无新增或更新的 POC", 0);
            }

            if (callback != null) {
                callback.onProgress("发现 " + newEntries.size() + " 个新增/更新的 POC");
            }

            // 3. 写入新增的 POC
            int inserted = insertPocsToDatabase(newEntries, "builtin-update");

            // 4. 更新版本号
            String version = getBuiltinVersion();
            if (version != null) updateLocalVersion(version);

            return new InitResult(true, "增量更新完成 (新增: " + inserted + ")", inserted);

        } catch (Exception e) {
            System.err.println("增量更新失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new InitResult(false, "更新失败: " + e.getMessage(), 0);
        }
    }
    
    /**
     * 应用清单更新（热更新使用）
     */
    public UpdateResult applyManifestUpdate(PocManifest remoteManifest, 
                                            Map<String, String> downloadedPocs,
                                            InitCallback callback) {
        int added = 0, updated = 0, deleted = 0;
        
        try {
            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
            
            // 1. 处理删除的 POC
            if (remoteManifest.getDeleted() != null && !remoteManifest.getDeleted().isEmpty()) {
                for (PocManifest.DeletedEntry entry : remoteManifest.getDeleted()) {
                    String pocId = entry.getEffectiveId();
                    if (pocId == null) continue;
                    try {
                        if (dbManager.deletePoc(pocId)) {
                            deleted++;
                        }
                    } catch (SQLException e) {
                        if (debugMode) System.err.println("删除 POC 失败: " + pocId);
                    }
                }
                if (callback != null) callback.onProgress("已删除 " + deleted + " 个 POC");
            }
            
            // 2. 处理需要更新的 POC（根据 md5 删除旧的）
            for (PocManifest.FileEntryWithPath entry : remoteManifest.getAllFiles()) {
                if (entry.getFile().getAction() == PocManifest.Action.UPDATE) {
                    String fullPath = entry.getFullPath();
                    if (downloadedPocs.containsKey(fullPath)) {
                        try {
                            String content = downloadedPocs.get(fullPath);
                            PocObj.Poc poc = pocLoader.loadFromContent(content, entry.getFile().getName(), fullPath);
                            if (poc != null && poc.getId() != null) {
                                dbManager.deletePoc(poc.getId());
                                updated++;
                            }
                        } catch (Exception e) {
                            if (debugMode) System.err.println("处理更新 POC 失败: " + fullPath);
                        }
                    }
                }
            }

            // 3. 插入新增和更新的 POC（使用 PocEntry 列表，避免 ID 作为唯一标识的问题）
            List<PocEntry> entriesToInsert = new ArrayList<>();

            for (Map.Entry<String, String> entry : downloadedPocs.entrySet()) {
                try {
                    String fullPath = entry.getKey();
                    String content = entry.getValue();
                    String filename = getFileName(fullPath);
                    PocObj.Poc poc = pocLoader.loadFromContent(content, filename, fullPath);
                    // POC 对象不为空即可（ID 可能为空，后面会处理）
                    if (poc != null) {
                        entriesToInsert.add(new PocEntry(poc, content, filename));
                    }
                } catch (Exception e) {
                    if (debugMode) System.err.println("解析 POC 失败: " + entry.getKey());
                }
            }

            if (!entriesToInsert.isEmpty()) {
                int insertedCount = insertPocsToDatabase(entriesToInsert, "hot-update");
                added = insertedCount - updated; // 扣除更新的数量
                if (added < 0) added = 0;
                if (callback != null) callback.onProgress("已导入 " + insertedCount + " 个 POC");
            }
            
            // 4. 更新版本号
            if (remoteManifest.getVersion() != null) {
                updateLocalVersion(remoteManifest.getVersion());
            }
            
            return new UpdateResult(true, "更新成功", added, updated, deleted);
            
        } catch (Exception e) {
            System.err.println("应用清单更新失败: " + e.getMessage());
            if (debugMode) e.printStackTrace();
            return new UpdateResult(false, "更新失败: " + e.getMessage(), added, updated, deleted);
        }
    }
    
    /**
     * 获取内置 POC 文件路径列表
     */
    private List<String> getBuiltinPocPaths() {
        // 使用 Constants 封装的方法获取 POC 目录路径和文件列表
        List<String> paths = Constants.listFiles(Constants.getResourceFilePath("poc"));
        if (paths == null) {
            paths = new ArrayList<>();
        }
        return paths.stream()
            .filter(p -> p.endsWith(".yaml") || p.endsWith(".yml") || p.endsWith(".json"))
            .filter(p -> !p.contains(".DS_Store"))
            .collect(Collectors.toList());
    }

    /**
     * 读取内置 POC 文件内容（使用绝对路径直接读取）
     */
    private String readBuiltinPocContent(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (debugMode) System.err.println("读取文件失败: " + filePath);
            return null;
        }
    }
    
    /**
     * 插入 POC 到数据库（使用批量事务优化）
     */
    private int insertPocsToDatabase(List<PocEntry> entries, String source) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        PocDatabaseManager dbManager = PocDatabaseManager.getInstance();

        try {
            // 使用批量插入方法，性能提升约10倍
            return dbManager.batchInsertPocs(entries, source);
        } catch (SQLException e) {
            if (debugMode) {
                System.err.println("批量插入 POC 失败: " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }
    }
    
    
    // ==================== 工具方法 ====================
    
    private Path getDbPath() {
        return pathManager.getResourcePath("vulnscan").resolve("vulnscan.db");
    }
    
    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
    
    public String getLocalVersion() {
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (config != null && config.has(POC_VERSION_KEY)) {
                return config.get(POC_VERSION_KEY).getAsString();
            }
        } catch (Exception e) {
            if (debugMode) System.err.println("获取本地版本失败: " + e.getMessage());
        }
        return null;
    }
    
    public String getBuiltinVersion() {
        // 1. 优先从 JAR 内置版本文件读取 (poc/version.txt)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("poc/version.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String version = reader.readLine();
                    if (version != null && !version.trim().isEmpty()) {
                        return version.trim();
                    }
                }
            }
        } catch (Exception e) {
            if (debugMode) System.err.println("读取内置版本文件失败: " + e.getMessage());
        }

        // 2. 回退到默认版本
        return "1.0.0";
    }
    
    private void updateLocalVersion(String version) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put(POC_VERSION_KEY, version);
            Constants.saveConfig(config, ConfigConstants.VULNSCAN);
        } catch (Exception e) {
            if (debugMode) System.err.println("更新版本号失败: " + e.getMessage());
        }
    }
    
    // ==================== 数据类 ====================

    /**
     * POC 条目（包含 POC 对象、原始内容和文件名）
     */
    public static class PocEntry {
        private final PocObj.Poc poc;
        private final String content;
        private final String filename;

        public PocEntry(PocObj.Poc poc, String content, String filename) {
            this.poc = poc;
            this.content = content;
            this.filename = filename;
        }

        public PocObj.Poc getPoc() { return poc; }
        public String getContent() { return content; }
        public String getFilename() { return filename; }
    }

    // ==================== 结果类 ====================

    public static class InitResult {
        private final boolean success;
        private final String message;
        private final int pocCount;
        private final int skipCount;  // 跳过（重复）的数量
        private final List<PocDatabaseManager.FailedEntry> failedEntries;

        public InitResult(boolean success, String message, int pocCount) {
            this(success, message, pocCount, 0, null);
        }

        public InitResult(boolean success, String message, int pocCount, int skipCount,
                          List<PocDatabaseManager.FailedEntry> failedEntries) {
            this.success = success;
            this.message = message;
            this.pocCount = pocCount;
            this.skipCount = skipCount;
            this.failedEntries = failedEntries != null ? failedEntries : new ArrayList<>();
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getPocCount() { return pocCount; }
        public int getSkipCount() { return skipCount; }
        public List<PocDatabaseManager.FailedEntry> getFailedEntries() { return failedEntries; }
        public int getFailedCount() { return failedEntries.size(); }
        public boolean hasFailures() { return !failedEntries.isEmpty(); }
    }
    
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
    }

    /**
     * 初始化回调接口
     */
    public interface InitCallback {
        /**
         * 进度更新
         */
        void onProgress(String message);

        /**
         * 发现失败的 POC（可选实现）
         * @param failedEntries 失败条目列表
         */
        default void onFailures(List<PocDatabaseManager.FailedEntry> failedEntries) {
            // 默认空实现，子类可覆盖
        }
    }
}
