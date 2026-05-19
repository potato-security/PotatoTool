package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
import com.potato.potatotool.content.redTeam.vulnScanner.model.TaskState;
import com.potato.potatotool.content.redTeam.vulnScanner.util.DatabaseMigration;
import com.potato.potatotool.storage.PathManager;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * 漏洞扫描数据库管理
 * 使用SQLite存储扫描历史记录和POC仓库
 * 
 * 数据库位置优先级：
 * 1. 配置文件中的 VulnScan.dbPath（如果配置）
 * 2. ResourcePath/vulnscan/vulnscan.db（新默认位置）
 * 3. {JAR目录}/.PotatoTool/vulnscan.db（旧位置，仅用于迁移）
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class VulnScanDatabase {
    
    private static VulnScanDatabase instance;
    // SQLite 单连接在并发 prepare/step 下容易互相阻塞，这里统一串行访问。
    private Connection connection;
    private final Gson gson = new Gson();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final String DB_FOLDER = "vulnscan";
    private static final String DB_FILE = "vulnscan.db";
    
    private String dbPath;  // 实际使用的数据库路径
    
    private VulnScanDatabase() {
        // 检查并执行数据库迁移
        checkAndMigrate();
        initDatabase();
    }
    
    /**
     * 检查并执行数据库迁移
     */
    private void checkAndMigrate() {
        if (DatabaseMigration.needsMigration()) {
            System.out.println("检测到旧版数据库，正在迁移...");
            DatabaseMigration.MigrationResult result = DatabaseMigration.migrate(
                new DatabaseMigration.MigrationCallback() {
                    @Override
                    public void onStart(String message) {
                        System.out.println("✓ " + message);
                    }
                    
                    @Override
                    public void onProgress(String message) {
                        System.out.println("  → " + message);
                    }
                    
                    @Override
                    public void onSuccess(String message) {
                        System.out.println("✓ " + message);
                    }
                    
                    @Override
                    public void onError(String message, Exception e) {
                        System.err.println("✗ " + message);
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }
                }
            );
            
            if (result.isSuccess() && result.isMigrated()) {
                System.out.println("数据库迁移完成");
            }
        }
    }
    
    /**
     * 获取数据库路径
     * 优先级：配置文件 > ResourcePath/vulnscan > 旧路径
     */
    private String getDatabasePath() {
        // 1. 先检查配置文件中是否有自定义路径
        String configPath = VulnScanConfig.getInstance().getDatabasePath();
        if (configPath != null && !configPath.trim().isEmpty()) {
            return configPath;
        }
        
        // 2. 使用新默认路径 ResourcePath/vulnscan/
        Path newPath = PathManager.getInstance().getResourcePath(DB_FOLDER).resolve(DB_FILE);
        return newPath.toString();
    }
    
    public static synchronized VulnScanDatabase getInstance() {
        if (instance == null) {
            instance = new VulnScanDatabase();
        }
        return instance;
    }
    
    private void initDatabase() {
        try {
            // 获取数据库路径
            dbPath = getDatabasePath();
            
            // 创建数据库目录
            Path dbDir = Paths.get(dbPath).getParent();
            if (dbDir != null && !Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
            }
            
            // 连接数据库
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA journal_mode=WAL");
                pragma.execute("PRAGMA synchronous=NORMAL");
                pragma.execute("PRAGMA busy_timeout=3000");
            }
            
            // 创建表
            createTables();
            
            System.out.println("✓ 漏洞扫描数据库初始化成功: " + dbPath);
        } catch (Exception e) {
            System.err.println("✗ 数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private synchronized void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // 扫描记录表（scan_id关联scan_states表，实现同一扫描任务只有一条历史记录）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS scan_records (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  scan_id TEXT," +                                     // 关联scan_states的scan_id
            "  scan_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  target_count INTEGER NOT NULL," +
            "  poc_count INTEGER NOT NULL," +
            "  vuln_count INTEGER NOT NULL," +
            "  critical_count INTEGER DEFAULT 0," +
            "  high_count INTEGER DEFAULT 0," +
            "  medium_count INTEGER DEFAULT 0," +
            "  low_count INTEGER DEFAULT 0," +
            "  info_count INTEGER DEFAULT 0," +
            "  duration INTEGER," +
            "  config TEXT," +
            "  status TEXT" +
            ")"
        );

        // 添加scan_id列（如果不存在）
        try {
            stmt.execute("ALTER TABLE scan_records ADD COLUMN scan_id TEXT");
        } catch (SQLException e) {
            // 列已存在，忽略错误
        }

        // 创建scan_id索引
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_scan_records_scan_id ON scan_records(scan_id)");
        
        // 漏洞详情表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS vuln_details (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  scan_id INTEGER NOT NULL," +
            "  target TEXT NOT NULL," +
            "  poc_id TEXT NOT NULL," +
            "  poc_name TEXT," +
            "  poc_format TEXT," +
            "  severity TEXT," +
            "  vuln_type TEXT," +
            "  protocol TEXT," +
            "  description TEXT," +
            "  found_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (scan_id) REFERENCES scan_records(id) ON DELETE CASCADE" +
            ")"
        );
        
        // 扫描状态表（支持暂停/恢复，统一任务管理）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS scan_states (" +
            "  scan_id TEXT PRIMARY KEY," +
            "  status TEXT NOT NULL," +                              // RUNNING, PAUSED, STOPPED, COMPLETED
            "  start_time INTEGER NOT NULL," +
            "  pause_time INTEGER," +
            "  resume_time INTEGER," +
            "  end_time INTEGER," +                                  // 新增：结束时间
            "  threads INTEGER," +
            "  timeout INTEGER," +
            "  proxy TEXT," +
            "  selected_formats TEXT," +
            "  targets TEXT NOT NULL," +
            "  poc_ids TEXT NOT NULL," +
            "  total_tasks INTEGER," +
            "  completed_tasks INTEGER," +
            "  vulnerabilities_found INTEGER," +
            "  critical_count INTEGER DEFAULT 0," +                  // 新增：严重度统计
            "  high_count INTEGER DEFAULT 0," +
            "  medium_count INTEGER DEFAULT 0," +
            "  low_count INTEGER DEFAULT 0," +
            "  info_count INTEGER DEFAULT 0," +
            "  additional_config TEXT," +
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // 添加新列（如果不存在）
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN end_time INTEGER"); } catch (SQLException e) {}
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN critical_count INTEGER DEFAULT 0"); } catch (SQLException e) {}
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN high_count INTEGER DEFAULT 0"); } catch (SQLException e) {}
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN medium_count INTEGER DEFAULT 0"); } catch (SQLException e) {}
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN low_count INTEGER DEFAULT 0"); } catch (SQLException e) {}
        try { stmt.execute("ALTER TABLE scan_states ADD COLUMN info_count INTEGER DEFAULT 0"); } catch (SQLException e) {}
        
        // 任务状态表（记录每个任务的完成情况）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS task_states (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  task_id TEXT NOT NULL," +
            "  scan_id TEXT NOT NULL," +
            "  target TEXT NOT NULL," +
            "  poc_id TEXT NOT NULL," +
            "  completed INTEGER DEFAULT 0," +
            "  vulnerable INTEGER DEFAULT 0," +
            "  start_time INTEGER," +
            "  end_time INTEGER," +
            "  FOREIGN KEY (scan_id) REFERENCES scan_states(scan_id) ON DELETE CASCADE" +
            ")"
        );
        
        // POC仓库表（存储导入的POC）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS poc_repository (" +
            "  id TEXT PRIMARY KEY," +                          // POC唯一标识
            "  name TEXT NOT NULL," +                           // POC名称
            "  original_format TEXT NOT NULL," +                // 原始格式(nuclei/xray/goby/pocsuite)
            "  original_filename TEXT NOT NULL," +              // 原始文件名
            "  severity TEXT," +                                // 严重程度
            "  protocol TEXT," +                                // 主协议
            "  protocols TEXT," +                               // 支持的协议列表(JSON)
            "  tags TEXT," +                                    // 标签列表(JSON)
            "  category TEXT," +                                // 分类
            "  author TEXT," +                                  // 作者
            "  description TEXT," +                             // 描述
            "  reference TEXT," +                               // 参考链接(JSON)
            "  cve_id TEXT," +                                  // CVE编号
            "  original_content TEXT NOT NULL," +               // 原始文件内容
            "  parsed_content TEXT NOT NULL," +                 // 解析后的PocObj JSON
            "  content_hash TEXT," +                            // 内容MD5哈希(去重依据)
            "  enabled INTEGER DEFAULT 1," +                    // 是否启用
            "  execution_count INTEGER DEFAULT 0," +            // 执行次数
            "  success_count INTEGER DEFAULT 0," +              // 成功次数
            "  error_count INTEGER DEFAULT 0," +                // 错误次数
            "  last_used_at DATETIME," +                        // 最后使用时间
            "  version TEXT DEFAULT '1.0'," +                   // POC版本
            "  imported_from TEXT," +                           // 导入来源
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," + // 创建时间
            "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" + // 更新时间
            ")"
        );
        
        // 创建索引 - 扫描相关
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_scan_time ON scan_records(scan_time)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_scan_id ON vuln_details(scan_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_severity ON vuln_details(severity)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_scan_state_status ON scan_states(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_task_scan_id ON task_states(scan_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_task_completed ON task_states(completed)");

        // 创建索引 - POC仓库相关
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_severity ON poc_repository(severity)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_protocol ON poc_repository(protocol)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_enabled ON poc_repository(enabled)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_format ON poc_repository(original_format)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_cve ON poc_repository(cve_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_poc_filename ON poc_repository(original_filename)");

        // 创建唯一索引 - 基于内容哈希去重（删除旧的基于文件名的唯一索引）
        stmt.execute("DROP INDEX IF EXISTS idx_unique_filename");

        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_content_hash ON poc_repository(content_hash)");

        stmt.close();
        System.out.println("✓ 数据库表创建成功");
    }
    
    /**
     * 保存扫描记录
     * @return 扫描ID
     */
    public synchronized long saveScanRecord(List<ScanResult> results, Map<String, Object> config,
                               long durationSeconds, String status) throws SQLException {
        // 允许暂停状态的扫描保存空结果
        if (results == null || (results.isEmpty() && !"paused".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("扫描结果不能为空");
        }

        // 统计数据
        int targetCount = 0;
        int pocCount = 0;
        int vulnCount = 0;

        Map<PocObj.Severity, Integer> severityCount = new HashMap<>();
        severityCount.put(PocObj.Severity.CRITICAL, 0);
        severityCount.put(PocObj.Severity.HIGH, 0);
        severityCount.put(PocObj.Severity.MEDIUM, 0);
        severityCount.put(PocObj.Severity.LOW, 0);
        severityCount.put(PocObj.Severity.INFO, 0);

        // 如果有结果，进行统计
        if (results != null && !results.isEmpty()) {
            targetCount = (int) results.stream()
                .map(ScanResult::getTarget)
                .distinct()
                .count();
            pocCount = (int) results.stream()
                .map(r -> r.getPoc().getId())
                .distinct()
                .count();
            vulnCount = results.size();

            for (ScanResult result : results) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                severityCount.put(severity, severityCount.get(severity) + 1);
            }
        }

        // 插入扫描记录
        String sql = "INSERT INTO scan_records (target_count, poc_count, vuln_count, " +
                    "critical_count, high_count, medium_count, low_count, info_count, " +
                    "duration, config, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, targetCount);
        pstmt.setInt(2, pocCount);
        pstmt.setInt(3, vulnCount);
        pstmt.setInt(4, severityCount.get(PocObj.Severity.CRITICAL));
        pstmt.setInt(5, severityCount.get(PocObj.Severity.HIGH));
        pstmt.setInt(6, severityCount.get(PocObj.Severity.MEDIUM));
        pstmt.setInt(7, severityCount.get(PocObj.Severity.LOW));
        pstmt.setInt(8, severityCount.get(PocObj.Severity.INFO));
        pstmt.setLong(9, durationSeconds);
        pstmt.setString(10, gson.toJson(config));
        pstmt.setString(11, status);
        pstmt.executeUpdate();

        // 获取生成的ID
        ResultSet rs = pstmt.getGeneratedKeys();
        long scanId = 0;
        if (rs.next()) {
            scanId = rs.getLong(1);
        }
        rs.close();
        pstmt.close();

        // 插入漏洞详情（如果有结果）
        if (results != null && !results.isEmpty()) {
            saveVulnDetails(scanId, results);
        }

        System.out.println("✓ 扫描记录已保存，ID: " + scanId);
        return scanId;
    }
    
    private synchronized void saveVulnDetails(long scanId, List<ScanResult> results) throws SQLException {
        String sql = "INSERT INTO vuln_details (scan_id, target, poc_id, poc_name, " +
                    "poc_format, severity, vuln_type, protocol, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(sql);

        for (ScanResult result : results) {
            PocObj.Poc poc = result.getPoc();
            pstmt.setLong(1, scanId);
            pstmt.setString(2, result.getTarget());
            pstmt.setString(3, poc.getId());
            pstmt.setString(4, poc.getName());
            pstmt.setString(5, poc.getOriginalFormat());
            pstmt.setString(6, poc.getSeverity().name());
            pstmt.setString(7, poc.getVulType());
            pstmt.setString(8, poc.getProtocol());
            pstmt.setString(9, poc.getDescription());
            pstmt.addBatch();
        }

        pstmt.executeBatch();
        pstmt.close();
    }

    /**
     * 保存或更新扫描记录（同一次扫描任务只保留一条历史记录）
     *
     * 当扫描任务暂停、恢复、再次暂停或完成时，都使用此方法：
     * - 如果该scan_id对应的记录已存在，则更新
     * - 如果不存在，则插入新记录
     *
     * @param scanId 扫描任务唯一标识（来自scan_states表）
     * @param results 扫描结果列表
     * @param config 扫描配置
     * @param durationSeconds 扫描耗时（秒）
     * @param status 扫描状态（paused/completed/stopped）
     * @return 历史记录ID
     */
    public synchronized long saveOrUpdateScanRecord(String scanId, List<ScanResult> results,
                                       Map<String, Object> config, long durationSeconds,
                                       String status) throws SQLException {
        // 允许暂停状态保存空结果
        if (results == null || (results.isEmpty() && !"paused".equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("扫描结果不能为空");
        }

        // 统计数据
        int targetCount = 0;
        int pocCount = 0;
        int vulnCount = 0;

        Map<PocObj.Severity, Integer> severityCount = new HashMap<>();
        severityCount.put(PocObj.Severity.CRITICAL, 0);
        severityCount.put(PocObj.Severity.HIGH, 0);
        severityCount.put(PocObj.Severity.MEDIUM, 0);
        severityCount.put(PocObj.Severity.LOW, 0);
        severityCount.put(PocObj.Severity.INFO, 0);

        if (results != null && !results.isEmpty()) {
            targetCount = (int) results.stream()
                .map(ScanResult::getTarget)
                .distinct()
                .count();
            pocCount = (int) results.stream()
                .map(r -> r.getPoc().getId())
                .distinct()
                .count();
            vulnCount = results.size();

            for (ScanResult result : results) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                severityCount.put(severity, severityCount.get(severity) + 1);
            }
        }

        // 查找是否已存在该scan_id的记录
        Long existingRecordId = findScanRecordByScanId(scanId);

        if (existingRecordId != null) {
            // 更新现有记录
            String updateSql = "UPDATE scan_records SET " +
                    "scan_time = datetime('now'), " +
                    "target_count = ?, poc_count = ?, vuln_count = ?, " +
                    "critical_count = ?, high_count = ?, medium_count = ?, " +
                    "low_count = ?, info_count = ?, duration = ?, config = ?, status = ? " +
                    "WHERE id = ?";

            PreparedStatement pstmt = connection.prepareStatement(updateSql);
            pstmt.setInt(1, targetCount);
            pstmt.setInt(2, pocCount);
            pstmt.setInt(3, vulnCount);
            pstmt.setInt(4, severityCount.get(PocObj.Severity.CRITICAL));
            pstmt.setInt(5, severityCount.get(PocObj.Severity.HIGH));
            pstmt.setInt(6, severityCount.get(PocObj.Severity.MEDIUM));
            pstmt.setInt(7, severityCount.get(PocObj.Severity.LOW));
            pstmt.setInt(8, severityCount.get(PocObj.Severity.INFO));
            pstmt.setLong(9, durationSeconds);
            pstmt.setString(10, gson.toJson(config));
            pstmt.setString(11, status);
            pstmt.setLong(12, existingRecordId);
            pstmt.executeUpdate();
            pstmt.close();

            // 删除旧的漏洞详情
            deleteVulnDetails(existingRecordId);

            // 插入新的漏洞详情
            if (results != null && !results.isEmpty()) {
                saveVulnDetails(existingRecordId, results);
            }

            System.out.println("✓ 扫描记录已更新，ID: " + existingRecordId + ", scan_id: " + scanId);
            return existingRecordId;
        } else {
            // 插入新记录（带scan_id）
            String insertSql = "INSERT INTO scan_records (scan_id, target_count, poc_count, vuln_count, " +
                    "critical_count, high_count, medium_count, low_count, info_count, " +
                    "duration, config, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, scanId);
            pstmt.setInt(2, targetCount);
            pstmt.setInt(3, pocCount);
            pstmt.setInt(4, vulnCount);
            pstmt.setInt(5, severityCount.get(PocObj.Severity.CRITICAL));
            pstmt.setInt(6, severityCount.get(PocObj.Severity.HIGH));
            pstmt.setInt(7, severityCount.get(PocObj.Severity.MEDIUM));
            pstmt.setInt(8, severityCount.get(PocObj.Severity.LOW));
            pstmt.setInt(9, severityCount.get(PocObj.Severity.INFO));
            pstmt.setLong(10, durationSeconds);
            pstmt.setString(11, gson.toJson(config));
            pstmt.setString(12, status);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            long recordId = 0;
            if (rs.next()) {
                recordId = rs.getLong(1);
            }
            rs.close();
            pstmt.close();

            // 插入漏洞详情
            if (results != null && !results.isEmpty()) {
                saveVulnDetails(recordId, results);
            }

            System.out.println("✓ 扫描记录已保存，ID: " + recordId + ", scan_id: " + scanId);
            return recordId;
        }
    }

    /**
     * 根据scan_id查找历史记录ID
     */
    private synchronized Long findScanRecordByScanId(String scanId) throws SQLException {
        if (scanId == null || scanId.isEmpty()) {
            return null;
        }

        String sql = "SELECT id FROM scan_records WHERE scan_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);

        ResultSet rs = pstmt.executeQuery();
        Long recordId = null;
        if (rs.next()) {
            recordId = rs.getLong("id");
        }
        rs.close();
        pstmt.close();
        return recordId;
    }

    /**
     * 删除指定扫描记录的漏洞详情
     */
    private synchronized void deleteVulnDetails(long scanId) throws SQLException {
        String sql = "DELETE FROM vuln_details WHERE scan_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setLong(1, scanId);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 查询扫描历史
     */
    public synchronized List<ScanHistory> queryHistory(Date startDate, Date endDate, int limit) throws SQLException {
        String sql = "SELECT * FROM scan_records WHERE scan_time BETWEEN ? AND ? " +
                    "ORDER BY scan_time DESC LIMIT ?";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, sdf.format(startDate));
        pstmt.setString(2, sdf.format(endDate));
        pstmt.setInt(3, limit);
        
        ResultSet rs = pstmt.executeQuery();
        List<ScanHistory> histories = new ArrayList<>();
        
        while (rs.next()) {
            ScanHistory history = new ScanHistory();
            history.setId(rs.getLong("id"));
            history.setScanTime(rs.getString("scan_time"));
            history.setTargetCount(rs.getInt("target_count"));
            history.setPocCount(rs.getInt("poc_count"));
            history.setVulnCount(rs.getInt("vuln_count"));
            history.setCriticalCount(rs.getInt("critical_count"));
            history.setHighCount(rs.getInt("high_count"));
            history.setMediumCount(rs.getInt("medium_count"));
            history.setLowCount(rs.getInt("low_count"));
            history.setInfoCount(rs.getInt("info_count"));
            history.setDuration(rs.getLong("duration"));
            history.setStatus(rs.getString("status"));
            
            // 解析配置
            String configJson = rs.getString("config");
            if (configJson != null) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                history.setConfig(gson.fromJson(configJson, type));
            }
            
            histories.add(history);
        }
        
        rs.close();
        pstmt.close();
        return histories;
    }
    
    /**
     * 查询所有历史记录
     */
    public synchronized List<ScanHistory> queryAllHistory() throws SQLException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -10); // 10年前
        Date startDate = cal.getTime();
        Date endDate = new Date();
        return queryHistory(startDate, endDate, 1000);
    }
    
    /**
     * 加载历史记录详情
     */
    public synchronized List<VulnDetail> loadHistoryDetails(long scanId) throws SQLException {
        String sql = "SELECT * FROM vuln_details WHERE scan_id = ? ORDER BY id";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setLong(1, scanId);
        
        ResultSet rs = pstmt.executeQuery();
        List<VulnDetail> details = new ArrayList<>();
        
        while (rs.next()) {
            VulnDetail detail = new VulnDetail();
            detail.setId(rs.getLong("id"));
            detail.setScanId(rs.getLong("scan_id"));
            detail.setTarget(rs.getString("target"));
            detail.setPocId(rs.getString("poc_id"));
            detail.setPocName(rs.getString("poc_name"));
            detail.setPocFormat(rs.getString("poc_format"));
            detail.setSeverity(rs.getString("severity"));
            detail.setVulnType(rs.getString("vuln_type"));
            detail.setProtocol(rs.getString("protocol"));
            detail.setDescription(rs.getString("description"));
            detail.setFoundTime(rs.getString("found_time"));
            details.add(detail);
        }
        
        rs.close();
        pstmt.close();
        return details;
    }
    
    /**
     * 删除历史记录
     */
    public synchronized void deleteHistory(long scanId) throws SQLException {
        String sql = "DELETE FROM scan_records WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setLong(1, scanId);
        int deleted = pstmt.executeUpdate();
        pstmt.close();
        
        if (deleted > 0) {
            System.out.println("✓ 已删除扫描记录: " + scanId);
        }
    }
    
    /**
     * 清空所有历史记录
     */
    public synchronized void clearAllHistory() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.execute("DELETE FROM scan_records");
        stmt.execute("DELETE FROM vuln_details");
        stmt.execute("DELETE FROM scan_states");
        stmt.execute("DELETE FROM task_states");
        stmt.close();

        System.out.println("✓ 已清空所有历史记录");
    }
    
    /**
     * 获取统计信息
     */
    public synchronized DatabaseStatistics getStatistics() throws SQLException {
        DatabaseStatistics stats = new DatabaseStatistics();
        
        Statement stmt = connection.createStatement();
        
        // 总扫描次数
        ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as count FROM scan_records");
        if (rs1.next()) {
            stats.setTotalScans(rs1.getInt("count"));
        }
        rs1.close();
        
        // 总漏洞数
        ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) as count FROM vuln_details");
        if (rs2.next()) {
            stats.setTotalVulns(rs2.getInt("count"));
        }
        rs2.close();
        
        // 按严重度统计
        ResultSet rs3 = stmt.executeQuery(
            "SELECT severity, COUNT(*) as count FROM vuln_details GROUP BY severity"
        );
        Map<String, Integer> severityStats = new HashMap<>();
        while (rs3.next()) {
            severityStats.put(rs3.getString("severity"), rs3.getInt("count"));
        }
        stats.setSeverityStats(severityStats);
        rs3.close();
        
        stmt.close();
        return stats;
    }
    
    // ==================== 扫描状态管理（支持暂停/恢复）====================
    
    /**
     * 保存扫描状态
     */
    public synchronized void saveScanState(ScanState state) throws SQLException {
        String sql = "INSERT OR REPLACE INTO scan_states (" +
                    "scan_id, status, start_time, pause_time, resume_time, " +
                    "threads, timeout, proxy, selected_formats, targets, poc_ids, " +
                    "total_tasks, completed_tasks, vulnerabilities_found, additional_config, updated_at" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, state.getScanId());
        pstmt.setString(2, state.getStatus().name());
        pstmt.setLong(3, state.getStartTime());
        pstmt.setLong(4, state.getPauseTime());
        pstmt.setLong(5, state.getResumeTime());
        pstmt.setInt(6, state.getThreads());
        pstmt.setInt(7, state.getTimeout());
        pstmt.setString(8, state.getProxy());
        pstmt.setString(9, gson.toJson(state.getSelectedFormats()));
        pstmt.setString(10, gson.toJson(state.getTargets()));
        pstmt.setString(11, gson.toJson(state.getPocIds()));
        pstmt.setInt(12, state.getTotalTasks());
        pstmt.setInt(13, state.getCompletedTasks());
        pstmt.setInt(14, state.getVulnerabilitiesFound());
        pstmt.setString(15, gson.toJson(state.getAdditionalConfig()));
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 更新扫描状态
     */
    public synchronized void updateScanState(String scanId, ScanState.Status status, 
                                int completedTasks, int vulnerabilitiesFound) throws SQLException {
        String sql = "UPDATE scan_states SET status = ?, completed_tasks = ?, " +
                    "vulnerabilities_found = ?, updated_at = datetime('now') WHERE scan_id = ?";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, status.name());
        pstmt.setInt(2, completedTasks);
        pstmt.setInt(3, vulnerabilitiesFound);
        pstmt.setString(4, scanId);
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 标记扫描为暂停状态
     */
    public synchronized void pauseScanState(String scanId) throws SQLException {
        String sql = "UPDATE scan_states SET status = ?, pause_time = ?, " +
                    "updated_at = datetime('now') WHERE scan_id = ?";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, ScanState.Status.PAUSED.name());
        pstmt.setLong(2, System.currentTimeMillis());
        pstmt.setString(3, scanId);
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 标记扫描为恢复状态
     */
    public synchronized void resumeScanState(String scanId) throws SQLException {
        String sql = "UPDATE scan_states SET status = ?, resume_time = ?, " +
                    "updated_at = datetime('now') WHERE scan_id = ?";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, ScanState.Status.RUNNING.name());
        pstmt.setLong(2, System.currentTimeMillis());
        pstmt.setString(3, scanId);
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 查询扫描状态
     */
    public synchronized ScanState loadScanState(String scanId) throws SQLException {
        String sql = "SELECT * FROM scan_states WHERE scan_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);
        
        ResultSet rs = pstmt.executeQuery();
        ScanState state = null;
        
        if (rs.next()) {
            state = new ScanState();
            state.setScanId(rs.getString("scan_id"));
            state.setStatus(ScanState.Status.valueOf(rs.getString("status")));
            state.setStartTime(rs.getLong("start_time"));
            state.setPauseTime(rs.getLong("pause_time"));
            state.setResumeTime(rs.getLong("resume_time"));
            state.setThreads(rs.getInt("threads"));
            state.setTimeout(rs.getInt("timeout"));
            state.setProxy(rs.getString("proxy"));
            
            // 解析JSON字段
            Type listType = new TypeToken<List<String>>(){}.getType();
            state.setSelectedFormats(gson.fromJson(rs.getString("selected_formats"), listType));
            state.setTargets(gson.fromJson(rs.getString("targets"), listType));
            state.setPocIds(gson.fromJson(rs.getString("poc_ids"), listType));
            
            state.setTotalTasks(rs.getInt("total_tasks"));
            state.setCompletedTasks(rs.getInt("completed_tasks"));
            state.setVulnerabilitiesFound(rs.getInt("vulnerabilities_found"));
            
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            String configJson = rs.getString("additional_config");
            if (configJson != null && !configJson.isEmpty()) {
                state.setAdditionalConfig(gson.fromJson(configJson, mapType));
            }
        }
        
        rs.close();
        pstmt.close();
        return state;
    }
    
    /**
     * 查询所有未完成的扫描（过滤掉进度100%的扫描）
     */
    public synchronized List<ScanState> loadPausedScans() throws SQLException {
        String sql = "SELECT * FROM scan_states WHERE (status = ? OR status = ?) " +
                    "AND completed_tasks < total_tasks " +
                    "ORDER BY updated_at DESC";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, ScanState.Status.PAUSED.name());
        pstmt.setString(2, ScanState.Status.RUNNING.name());

        ResultSet rs = pstmt.executeQuery();
        List<ScanState> states = new ArrayList<>();

        while (rs.next()) {
            ScanState state = new ScanState();
            state.setScanId(rs.getString("scan_id"));
            state.setStatus(ScanState.Status.valueOf(rs.getString("status")));
            state.setStartTime(rs.getLong("start_time"));
            state.setPauseTime(rs.getLong("pause_time"));
            state.setResumeTime(rs.getLong("resume_time"));
            state.setThreads(rs.getInt("threads"));
            state.setTimeout(rs.getInt("timeout"));
            state.setTotalTasks(rs.getInt("total_tasks"));
            state.setCompletedTasks(rs.getInt("completed_tasks"));
            state.setVulnerabilitiesFound(rs.getInt("vulnerabilities_found"));

            Type listType = new TypeToken<List<String>>(){}.getType();
            state.setTargets(gson.fromJson(rs.getString("targets"), listType));
            state.setPocIds(gson.fromJson(rs.getString("poc_ids"), listType));

            states.add(state);
        }

        rs.close();
        pstmt.close();
        return states;
    }
    
    /**
     * 删除扫描状态
     */
    public synchronized void deleteScanState(String scanId) throws SQLException {
        String sql = "DELETE FROM scan_states WHERE scan_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 保存任务状态
     */
    public synchronized void saveTaskState(TaskState task) throws SQLException {
        String sql = "INSERT INTO task_states (task_id, scan_id, target, poc_id, " +
                    "completed, vulnerable, start_time, end_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, task.getTaskId());
        pstmt.setString(2, task.getScanId());
        pstmt.setString(3, task.getTarget());
        pstmt.setString(4, task.getPocId());
        pstmt.setInt(5, task.isCompleted() ? 1 : 0);
        pstmt.setInt(6, task.isVulnerable() ? 1 : 0);
        pstmt.setLong(7, task.getStartTime());
        pstmt.setLong(8, task.getEndTime());
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * 批量保存任务状态
     */
    public synchronized void saveTaskStates(List<TaskState> tasks) throws SQLException {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        String sql = "INSERT INTO task_states (task_id, scan_id, target, poc_id, " +
                    "completed, vulnerable, start_time, end_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        
        for (TaskState task : tasks) {
            pstmt.setString(1, task.getTaskId());
            pstmt.setString(2, task.getScanId());
            pstmt.setString(3, task.getTarget());
            pstmt.setString(4, task.getPocId());
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setInt(6, task.isVulnerable() ? 1 : 0);
            pstmt.setLong(7, task.getStartTime());
            pstmt.setLong(8, task.getEndTime());
            pstmt.addBatch();
        }
        
        pstmt.executeBatch();
        pstmt.close();
    }
    
    /**
     * 查询已完成的任务ID列表
     */
    public synchronized List<String> loadCompletedTaskIds(String scanId) throws SQLException {
        String sql = "SELECT task_id FROM task_states WHERE scan_id = ? AND completed = 1";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);
        
        ResultSet rs = pstmt.executeQuery();
        List<String> taskIds = new ArrayList<>();
        
        while (rs.next()) {
            taskIds.add(rs.getString("task_id"));
        }
        
        rs.close();
        pstmt.close();
        return taskIds;
    }
    
    /**
     * 清理扫描的任务状态
     */
    public synchronized void clearTaskStates(String scanId) throws SQLException {
        String sql = "DELETE FROM task_states WHERE scan_id = ?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);
        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * 加载扫描中发现的漏洞任务(用于恢复扫描时重建结果)
     */
    public synchronized List<TaskState> loadVulnerableTasks(String scanId) throws SQLException {
        String sql = "SELECT * FROM task_states WHERE scan_id = ? AND vulnerable = 1";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, scanId);

        ResultSet rs = pstmt.executeQuery();
        List<TaskState> tasks = new ArrayList<>();

        while (rs.next()) {
            TaskState task = new TaskState();
            task.setTaskId(rs.getString("task_id"));
            task.setScanId(rs.getString("scan_id"));
            task.setTarget(rs.getString("target"));
            task.setPocId(rs.getString("poc_id"));
            task.setCompleted(rs.getInt("completed") == 1);
            task.setVulnerable(rs.getInt("vulnerable") == 1);
            task.setStartTime(rs.getLong("start_time"));
            task.setEndTime(rs.getLong("end_time"));
            tasks.add(task);
        }

        rs.close();
        pstmt.close();
        return tasks;
    }

    // ==================== 统一任务管理（新增）====================

    /**
     * 加载所有扫描任务（用于历史记录展示）
     * 按更新时间倒序排列
     */
    public synchronized List<ScanState> loadAllScanTasks() throws SQLException {
        String sql = "SELECT * FROM scan_states ORDER BY updated_at DESC";
        PreparedStatement pstmt = connection.prepareStatement(sql);

        ResultSet rs = pstmt.executeQuery();
        List<ScanState> states = new ArrayList<>();

        while (rs.next()) {
            ScanState state = parseScanStateFromResultSet(rs);
            states.add(state);
        }

        rs.close();
        pstmt.close();
        return states;
    }

    /**
     * 加载所有扫描任务（带日期过滤）
     */
    public synchronized List<ScanState> loadScanTasksByDateRange(Date startDate, Date endDate, int limit) throws SQLException {
        String sql = "SELECT * FROM scan_states WHERE created_at BETWEEN ? AND ? ORDER BY updated_at DESC LIMIT ?";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, sdf.format(startDate));
        pstmt.setString(2, sdf.format(endDate));
        pstmt.setInt(3, limit);

        ResultSet rs = pstmt.executeQuery();
        List<ScanState> states = new ArrayList<>();

        while (rs.next()) {
            ScanState state = parseScanStateFromResultSet(rs);
            states.add(state);
        }

        rs.close();
        pstmt.close();
        return states;
    }

    /**
     * 从 ResultSet 解析 ScanState 对象
     */
    private ScanState parseScanStateFromResultSet(ResultSet rs) throws SQLException {
        ScanState state = new ScanState();
        state.setScanId(rs.getString("scan_id"));
        state.setStatus(ScanState.Status.valueOf(rs.getString("status")));
        state.setStartTime(rs.getLong("start_time"));
        state.setPauseTime(rs.getLong("pause_time"));
        state.setResumeTime(rs.getLong("resume_time"));
        state.setEndTime(rs.getLong("end_time"));
        state.setThreads(rs.getInt("threads"));
        state.setTimeout(rs.getInt("timeout"));
        state.setProxy(rs.getString("proxy"));
        state.setTotalTasks(rs.getInt("total_tasks"));
        state.setCompletedTasks(rs.getInt("completed_tasks"));
        state.setVulnerabilitiesFound(rs.getInt("vulnerabilities_found"));

        // 严重度统计
        state.setCriticalCount(rs.getInt("critical_count"));
        state.setHighCount(rs.getInt("high_count"));
        state.setMediumCount(rs.getInt("medium_count"));
        state.setLowCount(rs.getInt("low_count"));
        state.setInfoCount(rs.getInt("info_count"));

        // 解析 JSON 字段
        Type listType = new TypeToken<List<String>>(){}.getType();
        String formatsJson = rs.getString("selected_formats");
        if (formatsJson != null && !formatsJson.isEmpty()) {
            state.setSelectedFormats(gson.fromJson(formatsJson, listType));
        }
        state.setTargets(gson.fromJson(rs.getString("targets"), listType));
        state.setPocIds(gson.fromJson(rs.getString("poc_ids"), listType));

        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        String configJson = rs.getString("additional_config");
        if (configJson != null && !configJson.isEmpty()) {
            state.setAdditionalConfig(gson.fromJson(configJson, mapType));
        }

        return state;
    }

    /**
     * 标记扫描为完成状态
     */
    public synchronized void completeScanTask(String scanId, int vulnCount,
                                  int criticalCount, int highCount, int mediumCount,
                                  int lowCount, int infoCount) throws SQLException {
        String sql = "UPDATE scan_states SET status = ?, end_time = ?, completed_tasks = total_tasks, " +
                    "vulnerabilities_found = ?, critical_count = ?, high_count = ?, " +
                    "medium_count = ?, low_count = ?, info_count = ?, " +
                    "updated_at = datetime('now') WHERE scan_id = ?";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, ScanState.Status.COMPLETED.name());
        pstmt.setLong(2, System.currentTimeMillis());
        pstmt.setInt(3, vulnCount);
        pstmt.setInt(4, criticalCount);
        pstmt.setInt(5, highCount);
        pstmt.setInt(6, mediumCount);
        pstmt.setInt(7, lowCount);
        pstmt.setInt(8, infoCount);
        pstmt.setString(9, scanId);

        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * 标记扫描为停止状态
     */
    public synchronized void stopScanTask(String scanId) throws SQLException {
        String sql = "UPDATE scan_states SET status = ?, end_time = ?, " +
                    "updated_at = datetime('now') WHERE scan_id = ?";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, ScanState.Status.STOPPED.name());
        pstmt.setLong(2, System.currentTimeMillis());
        pstmt.setString(3, scanId);

        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * 更新扫描任务的漏洞统计
     */
    public synchronized void updateScanVulnCounts(String scanId, int vulnCount,
                                      int criticalCount, int highCount, int mediumCount,
                                      int lowCount, int infoCount) throws SQLException {
        String sql = "UPDATE scan_states SET vulnerabilities_found = ?, " +
                    "critical_count = ?, high_count = ?, medium_count = ?, " +
                    "low_count = ?, info_count = ?, updated_at = datetime('now') WHERE scan_id = ?";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, vulnCount);
        pstmt.setInt(2, criticalCount);
        pstmt.setInt(3, highCount);
        pstmt.setInt(4, mediumCount);
        pstmt.setInt(5, lowCount);
        pstmt.setInt(6, infoCount);
        pstmt.setString(7, scanId);

        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
     * 计算扫描耗时（毫秒）
     */
    public long calculateScanDuration(ScanState state) {
        if (state == null) return 0;

        long endTime = state.getEndTime() > 0 ? state.getEndTime() : System.currentTimeMillis();
        long startTime = state.getStartTime();

        // 如果有暂停时间，需要减去暂停期间的时间
        // 简化处理：直接返回 endTime - startTime
        return endTime - startTime;
    }

    /**
     * 获取当前数据库路径
     */
    public String getDbPath() {
        return dbPath;
    }
    
    /**
     * 获取数据库连接（供PocDatabaseManager使用）
     */
    public synchronized Connection getConnection() {
        return connection;
    }
    
    /**
     * 检查连接是否有效
     */
    public synchronized boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 关闭数据库连接
     */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✓ 数据库连接已关闭");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
