package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.UUID;

/**
 * POC数据库管理器
 * 负责POC的CRUD操作
 * 
 * 去重逻辑：统一使用 content_hash (MD5) 作为去重依据
 * 
 * @author Potato
 * @date 2025/11/25
 */
public class PocDatabaseManager {
    
    private static PocDatabaseManager instance;
    private final VulnScanDatabase database;
    private final Gson gson = new Gson();
    
    /**
     * 导入结果
     */
    public static class ImportResult {
        private final boolean success;
        private final String message;
        private final String pocId;
        private final DuplicateType duplicateType;
        
        public ImportResult(boolean success, String message, String pocId, DuplicateType duplicateType) {
            this.success = success;
            this.message = message;
            this.pocId = pocId;
            this.duplicateType = duplicateType;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getPocId() { return pocId; }
        public DuplicateType getDuplicateType() { return duplicateType; }
        public boolean isDuplicate() { return duplicateType != null; }
    }
    
    /**
     * 重复类型枚举
     */
    public enum DuplicateType {
        CONTENT_HASH("内容重复");
        
        private final String description;
        DuplicateType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
    
    /**
     * POC实体类（数据库记录）
     */
    public static class PocEntity {
        private String id;
        private String name;
        private String originalFormat;
        private String originalFilename;
        private String severity;
        private String protocol;
        private List<String> protocols;
        private List<String> tags;
        private String category;
        private String author;
        private String description;
        private List<String> reference;
        private String cveId;
        private String originalContent;
        private PocObj.Poc parsedContent;
        private boolean enabled;
        private int executionCount;
        private int successCount;
        private int errorCount;
        private String lastUsedAt;
        private String version;
        private String importedFrom;
        private String createdAt;
        private String updatedAt;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOriginalFormat() { return originalFormat; }
        public void setOriginalFormat(String originalFormat) { this.originalFormat = originalFormat; }
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public List<String> getProtocols() { return protocols; }
        public void setProtocols(List<String> protocols) { this.protocols = protocols; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getReference() { return reference; }
        public void setReference(List<String> reference) { this.reference = reference; }
        public String getCveId() { return cveId; }
        public void setCveId(String cveId) { this.cveId = cveId; }
        public String getOriginalContent() { return originalContent; }
        public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
        public PocObj.Poc getParsedContent() { return parsedContent; }
        public void setParsedContent(PocObj.Poc parsedContent) { this.parsedContent = parsedContent; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getExecutionCount() { return executionCount; }
        public void setExecutionCount(int executionCount) { this.executionCount = executionCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public String getLastUsedAt() { return lastUsedAt; }
        public void setLastUsedAt(String lastUsedAt) { this.lastUsedAt = lastUsedAt; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getImportedFrom() { return importedFrom; }
        public void setImportedFrom(String importedFrom) { this.importedFrom = importedFrom; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
    
    private PocDatabaseManager() {
        this.database = VulnScanDatabase.getInstance();
    }
    
    public static synchronized PocDatabaseManager getInstance() {
        if (instance == null) {
            instance = new PocDatabaseManager();
        }
        return instance;
    }
    
    private Connection getConnection() {
        return database.getConnection();
    }
    
    // ==================== 去重检查 ====================

    /**
     * 检查内容哈希是否已存在
     */
    public boolean existsByContentHash(String contentHash) throws SQLException {
        if (contentHash == null || contentHash.isEmpty()) {
            return false;
        }
        String sql = "SELECT 1 FROM poc_repository WHERE content_hash = ? LIMIT 1";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, contentHash);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
    
    public boolean existsById(String id) throws SQLException {
        String sql = "SELECT 1 FROM poc_repository WHERE id = ? LIMIT 1";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
    
    // ==================== CRUD 操作 ====================
    
    /**
     * 插入POC（基于 content_hash 去重）
     */
    public ImportResult insertPoc(PocObj.Poc poc, String originalContent, String filename, String importedFrom) 
            throws SQLException {
        // 计算内容哈希并检查重复
        String contentHash = calculateContentHash(originalContent);
        if (existsByContentHash(contentHash)) {
            return new ImportResult(false, DuplicateType.CONTENT_HASH.getDescription(), 
                poc.getId(), DuplicateType.CONTENT_HASH);
        }
        
        String cveId = extractCveId(poc);
        String pocId = poc.getId();
        if (pocId == null || pocId.trim().isEmpty()) {
            pocId = "poc-" + (contentHash != null ? contentHash.substring(0, 12) : UUID.randomUUID().toString().substring(0, 12));
        }

        String sql = "INSERT INTO poc_repository (" +
            "id, name, original_format, original_filename, severity, protocol, " +
            "protocols, tags, category, author, description, reference, cve_id, " +
            "original_content, parsed_content, content_hash, enabled, imported_from" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, pocId);
            pstmt.setString(2, poc.getName());
            pstmt.setString(3, poc.getOriginalFormat());
            pstmt.setString(4, filename);
            pstmt.setString(5, poc.getSeverity() != null ? poc.getSeverity().name() : null);
            pstmt.setString(6, poc.getProtocol());
            pstmt.setString(7, gson.toJson(Collections.singletonList(poc.getProtocol())));
            pstmt.setString(8, gson.toJson(poc.getTags()));
            pstmt.setString(9, poc.getCategory() != null ? poc.getCategory().name() : null);
            pstmt.setString(10, poc.getAuthor());
            pstmt.setString(11, poc.getDescription());
            pstmt.setString(12, gson.toJson(poc.getReferences()));
            pstmt.setString(13, cveId);
            pstmt.setString(14, originalContent);
            pstmt.setString(15, gson.toJson(poc));
            pstmt.setString(16, contentHash);
            pstmt.setInt(17, 1);
            pstmt.setString(18, importedFrom);
            
            pstmt.executeUpdate();
            return new ImportResult(true, "导入成功", pocId, null);
        }
    }
    
    /**
     * 从POC中提取CVE ID
     */
    private String extractCveId(PocObj.Poc poc) {
        if (poc.getId() != null && poc.getId().toUpperCase().startsWith("CVE-")) {
            return poc.getId();
        }
        if (poc.getTags() != null) {
            for (String tag : poc.getTags()) {
                if (tag != null && tag.toUpperCase().startsWith("CVE-")) {
                    return tag.toUpperCase();
                }
            }
        }
        return null;
    }

    /**
     * 计算内容的 MD5 哈希值
     */
    public static String calculateContentHash(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 POC 的内容哈希值
     */
    public String getContentHash(String pocId) throws SQLException {
        String sql = "SELECT content_hash FROM poc_repository WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, pocId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("content_hash");
            }
        }
        return null;
    }

    /**
     * 获取所有 POC 的 ID 和内容哈希映射
     */
    public Map<String, String> getAllContentHashes() throws SQLException {
        Map<String, String> hashes = new HashMap<>();
        String sql = "SELECT id, content_hash FROM poc_repository";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String hash = rs.getString("content_hash");
                if (id != null) {
                    hashes.put(id, hash);
                }
            }
        }
        return hashes;
    }
    
    /**
     * 根据ID获取POC
     */
    public PocEntity getPocById(String id) throws SQLException {
        String sql = "SELECT * FROM poc_repository WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return resultSetToEntity(rs);
            }
        }
        return null;
    }
    
    /**
     * 获取所有启用的POC
     */
    public List<PocObj.Poc> getAllEnabledPocs() throws SQLException {
        List<PocObj.Poc> pocs = new ArrayList<>();
        // 同时查询数据库主键 id 和 parsed_content
        String sql = "SELECT id, parsed_content FROM poc_repository WHERE enabled = 1";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dbId = rs.getString("id");  // 数据库主键
                String json = rs.getString("parsed_content");
                PocObj.Poc poc = gson.fromJson(json, PocObj.Poc.class);
                if (poc != null) {
                    // 确保 POC 对象的 ID 与数据库主键一致
                    poc.setId(dbId);
                    pocs.add(poc);
                }
            }
        }
        return pocs;
    }
    
    /**
     * 获取所有POC实体
     */
    public List<PocEntity> getAllPocEntities() throws SQLException {
        List<PocEntity> entities = new ArrayList<>();
        String sql = "SELECT * FROM poc_repository ORDER BY created_at DESC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entities.add(resultSetToEntity(rs));
            }
        }
        return entities;
    }
    
    /**
     * 获取所有 POC ID 集合
     */
    public Set<String> getAllPocIds() throws SQLException {
        Set<String> ids = new HashSet<>();
        String sql = "SELECT id FROM poc_repository";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        }
        return ids;
    }
    
    private PocEntity resultSetToEntity(ResultSet rs) throws SQLException {
        PocEntity entity = new PocEntity();
        entity.setId(rs.getString("id"));
        entity.setName(rs.getString("name"));
        entity.setOriginalFormat(rs.getString("original_format"));
        entity.setOriginalFilename(rs.getString("original_filename"));
        entity.setSeverity(rs.getString("severity"));
        entity.setProtocol(rs.getString("protocol"));
        
        Type listType = new TypeToken<List<String>>(){}.getType();
        entity.setProtocols(gson.fromJson(rs.getString("protocols"), listType));
        entity.setTags(gson.fromJson(rs.getString("tags"), listType));
        entity.setReference(gson.fromJson(rs.getString("reference"), listType));
        
        entity.setCategory(rs.getString("category"));
        entity.setAuthor(rs.getString("author"));
        entity.setDescription(rs.getString("description"));
        entity.setCveId(rs.getString("cve_id"));
        entity.setOriginalContent(rs.getString("original_content"));
        entity.setParsedContent(gson.fromJson(rs.getString("parsed_content"), PocObj.Poc.class));
        entity.setEnabled(rs.getInt("enabled") == 1);
        entity.setExecutionCount(rs.getInt("execution_count"));
        entity.setSuccessCount(rs.getInt("success_count"));
        entity.setErrorCount(rs.getInt("error_count"));
        entity.setLastUsedAt(rs.getString("last_used_at"));
        entity.setVersion(rs.getString("version"));
        entity.setImportedFrom(rs.getString("imported_from"));
        entity.setCreatedAt(rs.getString("created_at"));
        entity.setUpdatedAt(rs.getString("updated_at"));
        
        return entity;
    }
    
    /**
     * 更新POC启用状态
     */
    public boolean setEnabled(String id, boolean enabled) throws SQLException {
        String sql = "UPDATE poc_repository SET enabled = ?, updated_at = datetime('now') WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, enabled ? 1 : 0);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量更新POC启用状态
     */
    public int batchSetEnabled(List<String> ids, boolean enabled) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE poc_repository SET enabled = ?, updated_at = datetime('now') WHERE id = ?";
        int count = 0;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            for (String id : ids) {
                pstmt.setInt(1, enabled ? 1 : 0);
                pstmt.setString(2, id);
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            for (int r : results) {
                if (r > 0) count++;
            }
        }
        return count;
    }

    /**
     * 批量删除POC
     */
    public int batchDelete(List<String> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String sql = "DELETE FROM poc_repository WHERE id = ?";
        int count = 0;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            for (String id : ids) {
                pstmt.setString(1, id);
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            for (int r : results) {
                if (r > 0) count++;
            }
        }
        return count;
    }

    /**
     * 删除POC
     */
    public boolean deletePoc(String id) throws SQLException {
        String sql = "DELETE FROM poc_repository WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 更新执行统计
     */
    public void updateExecutionStats(String id, boolean success) throws SQLException {
        String sql = success ? 
            "UPDATE poc_repository SET execution_count = execution_count + 1, " +
            "success_count = success_count + 1, last_used_at = datetime('now'), " +
            "updated_at = datetime('now') WHERE id = ?" :
            "UPDATE poc_repository SET execution_count = execution_count + 1, " +
            "error_count = error_count + 1, last_used_at = datetime('now'), " +
            "updated_at = datetime('now') WHERE id = ?";
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 获取POC统计信息
     */
    public Map<String, Object> getStatistics() throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        try (Statement stmt = getConnection().createStatement()) {
            // 总数
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM poc_repository");
            stats.put("total", rs.next() ? rs.getInt(1) : 0);
            
            // 启用数
            rs = stmt.executeQuery("SELECT COUNT(*) FROM poc_repository WHERE enabled = 1");
            stats.put("enabled", rs.next() ? rs.getInt(1) : 0);
            
            // 按格式统计
            rs = stmt.executeQuery("SELECT original_format, COUNT(*) as cnt FROM poc_repository GROUP BY original_format");
            Map<String, Integer> byFormat = new LinkedHashMap<>();
            while (rs.next()) {
                byFormat.put(rs.getString("original_format"), rs.getInt("cnt"));
            }
            stats.put("byFormat", byFormat);
            
            // 按严重度统计
            rs = stmt.executeQuery("SELECT severity, COUNT(*) as cnt FROM poc_repository GROUP BY severity");
            Map<String, Integer> bySeverity = new LinkedHashMap<>();
            while (rs.next()) {
                bySeverity.put(rs.getString("severity"), rs.getInt("cnt"));
            }
            stats.put("bySeverity", bySeverity);
        }
        
        return stats;
    }
    
    /**
     * 获取POC总数
     */
    public int getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM poc_repository";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    /**
     * 清空所有POC
     */
    public void clearAll() throws SQLException {
        String sql = "DELETE FROM poc_repository";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量插入结果
     */
    public static class BatchInsertResult {
        private final int successCount;
        private final int skipCount;  // 重复跳过的数量
        private final List<FailedEntry> failedEntries;
        private final List<String> skippedFiles;  // 跳过的文件名列表
        private final Map<String, String> skippedHashMap;  // 跳过文件的hash映射

        public BatchInsertResult(int successCount, int skipCount, List<FailedEntry> failedEntries) {
            this(successCount, skipCount, failedEntries, new ArrayList<>(), new HashMap<>());
        }

        public BatchInsertResult(int successCount, int skipCount, List<FailedEntry> failedEntries, List<String> skippedFiles) {
            this(successCount, skipCount, failedEntries, skippedFiles, new HashMap<>());
        }

        public BatchInsertResult(int successCount, int skipCount, List<FailedEntry> failedEntries, 
                                 List<String> skippedFiles, Map<String, String> skippedHashMap) {
            this.successCount = successCount;
            this.skipCount = skipCount;
            this.failedEntries = failedEntries != null ? failedEntries : new ArrayList<>();
            this.skippedFiles = skippedFiles != null ? skippedFiles : new ArrayList<>();
            this.skippedHashMap = skippedHashMap != null ? skippedHashMap : new HashMap<>();
        }

        public int getSuccessCount() { return successCount; }
        public int getSkipCount() { return skipCount; }
        public List<FailedEntry> getFailedEntries() { return failedEntries; }
        public int getFailedCount() { return failedEntries.size(); }
        public boolean hasFailures() { return !failedEntries.isEmpty(); }
        public List<String> getSkippedFiles() { return skippedFiles; }
        public Map<String, String> getSkippedHashMap() { return skippedHashMap; }
    }

    /**
     * 失败条目
     */
    public static class FailedEntry {
        private final String filename;
        private final String reason;
        private final String detail;

        public FailedEntry(String filename, String reason, String detail) {
            this.filename = filename;
            this.reason = reason;
            this.detail = detail;
        }

        public String getFilename() { return filename; }
        public String getReason() { return reason; }
        public String getDetail() { return detail; }

        @Override
        public String toString() {
            return filename + ": " + reason + (detail != null ? " (" + detail + ")" : "");
        }
    }

    /**
     * 批量插入POC（性能优化：使用事务批量处理）
     * 使用 PocEntry 列表，避免 ID 作为唯一标识的问题
     *
     * @param entries POC条目列表（包含POC对象、原始内容和文件名）
     * @param source 导入来源
     * @return 批量插入结果（包含成功数量和失败详情）
     */
    public BatchInsertResult batchInsertPocsWithDetails(List<PocDatabaseInitializer.PocEntry> entries, String source)
            throws SQLException {
        List<FailedEntry> failedEntries = new ArrayList<>();
        List<String> skippedFiles = new ArrayList<>();

        if (entries == null || entries.isEmpty()) {
            return new BatchInsertResult(0, 0, failedEntries, skippedFiles);
        }

        Connection conn = getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        // 使用 INSERT OR IGNORE 避免唯一约束冲突（基于 content_hash）
        String sql = "INSERT OR IGNORE INTO poc_repository (" +
            "id, name, original_format, original_filename, severity, protocol, " +
            "protocols, tags, category, author, description, reference, cve_id, " +
            "original_content, parsed_content, content_hash, enabled, imported_from" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int successCount = 0;
        int skipCount = 0;
        int batchCount = 0;
        final int BATCH_SIZE = 100;
        // 用于检测当前导入过程中的重复（避免同一批次内因未提交而无法命中 DB 唯一索引）
        Set<String> seenContentHashes = new HashSet<>();
        List<String> currentBatchFiles = new ArrayList<>();
        List<String> currentBatchHashes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (PocDatabaseInitializer.PocEntry entry : entries) {
                PocObj.Poc poc = entry.getPoc();
                String content = entry.getContent();
                String filename = entry.getFilename();

                // POC 对象为空
                if (poc == null) {
                    failedEntries.add(new FailedEntry(filename, "POC解析失败", "返回空对象"));
                    continue;
                }

                // 检查协议是否有效
                if (poc.getProtocol() == null || poc.getProtocol().isEmpty()) {
                    failedEntries.add(new FailedEntry(filename, "未检测到协议",
                        "ID: " + (poc.getId() != null ? poc.getId() : "无")));
                    continue;
                }

                try {
                    // 计算内容哈希
                    String contentHash = calculateContentHash(content);
                    
                    // 先检查当前导入过程内是否已有相同哈希（防止同批次重复）
                    if (contentHash != null && seenContentHashes.contains(contentHash)) {
                        skippedFiles.add(filename);
                        skipCount++;
                        continue;
                    }

                    // 再检查数据库是否已有相同哈希（历史数据重复）
                    if (contentHash != null && existsByContentHash(contentHash)) {
                        skippedFiles.add(filename);
                        skipCount++;
                        continue;
                    }
                    
                    // 生成唯一 ID：基于内容哈希，避免 POC 自带 ID 冲突
                    // 原因：很多 POC 文件使用通用的 name 字段（如 "poc-yaml-test"），导致 ID 冲突
                    String pocId = contentHash != null ? 
                        "poc-" + contentHash.substring(0, 16) : 
                        "poc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

                    String cveId = extractCveId(poc);

                    pstmt.setString(1, pocId);
                    pstmt.setString(2, poc.getName() != null ? poc.getName() : filename);
                    pstmt.setString(3, poc.getOriginalFormat());
                    pstmt.setString(4, filename);
                    pstmt.setString(5, poc.getSeverity() != null ? poc.getSeverity().name() : null);
                    pstmt.setString(6, poc.getProtocol());
                    pstmt.setString(7, gson.toJson(Collections.singletonList(poc.getProtocol())));
                    pstmt.setString(8, gson.toJson(poc.getTags()));
                    pstmt.setString(9, poc.getCategory() != null ? poc.getCategory().name() : null);
                    pstmt.setString(10, poc.getAuthor());
                    pstmt.setString(11, poc.getDescription());
                    pstmt.setString(12, gson.toJson(poc.getReferences()));
                    pstmt.setString(13, cveId);
                    pstmt.setString(14, content);
                    pstmt.setString(15, gson.toJson(poc));
                    pstmt.setString(16, contentHash);
                    pstmt.setInt(17, 1);
                    pstmt.setString(18, source);

                    pstmt.addBatch();
                    currentBatchFiles.add(filename);
                    currentBatchHashes.add(contentHash);
                    batchCount++;
                    seenContentHashes.add(contentHash);

                    if (batchCount >= BATCH_SIZE) {
                        int[] results = pstmt.executeBatch();
                        for (int i = 0; i < results.length; i++) {
                            if (results[i] == 0) {
                                skipCount++;
                                if (i < currentBatchFiles.size()) {
                                    skippedFiles.add(currentBatchFiles.get(i));
                                }
                            } else {
                                successCount++;
                            }
                        }
                        conn.commit();
                        batchCount = 0;
                        currentBatchFiles.clear();
                        currentBatchHashes.clear();
                    }
                } catch (Exception e) {
                    failedEntries.add(new FailedEntry(filename, "数据库插入失败", e.getMessage()));
                }
            }

            // 处理剩余的批次
            if (batchCount > 0) {
                int[] results = pstmt.executeBatch();
                for (int i = 0; i < results.length; i++) {
                    if (results[i] == 0) {
                        skipCount++;
                        if (i < currentBatchFiles.size()) {
                            skippedFiles.add(currentBatchFiles.get(i));
                        }
                    } else {
                        successCount++;
                    }
                }
                conn.commit();
            }

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }

        return new BatchInsertResult(successCount, skipCount, failedEntries, skippedFiles);
    }

    /**
     * 批量插入POC（简化版，只返回成功数量）
     */
    public int batchInsertPocs(List<PocDatabaseInitializer.PocEntry> entries, String source)
            throws SQLException {
        BatchInsertResult result = batchInsertPocsWithDetails(entries, source);
        return result.getSuccessCount();
    }
}
