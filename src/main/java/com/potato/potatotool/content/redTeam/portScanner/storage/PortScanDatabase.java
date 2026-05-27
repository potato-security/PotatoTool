package com.potato.potatotool.content.redTeam.portScanner.storage;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import com.potato.potatotool.storage.PathManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PortScanDatabase {
    private static PortScanDatabase instance;
    private final Path dbPath;

    private PortScanDatabase() {
        this(PathManager.getInstance().getConfigBasePath().resolve("portscan_history.db"));
    }

    PortScanDatabase(Path dbPath) {
        this.dbPath = dbPath;
        initialize();
    }

    public static synchronized PortScanDatabase getInstance() {
        if (instance == null) {
            instance = new PortScanDatabase();
        }
        return instance;
    }

    public void saveScan(PortScanResult result) throws Exception {
        if (result == null) {
            return;
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement scanStmt = connection.prepareStatement("insert or replace into scan_history(scan_id,start_time,end_time,total_tasks,completed_tasks,open_count) values(?,?,?,?,?,?)")) {
                scanStmt.setString(1, result.getScanId());
                scanStmt.setLong(2, result.getStartTime());
                scanStmt.setLong(3, result.getEndTime());
                scanStmt.setLong(4, result.getTotalTasks());
                scanStmt.setLong(5, result.getCompletedTasks());
                scanStmt.setInt(6, result.getOpenResults().size());
                scanStmt.executeUpdate();
            }
            try (PreparedStatement deleteStmt = connection.prepareStatement("delete from port_result where scan_id=?")) {
                deleteStmt.setString(1, result.getScanId());
                deleteStmt.executeUpdate();
            }
            try (PreparedStatement portStmt = connection.prepareStatement("insert into port_result(scan_id,host,port,state,service,banner,tls,rtt_ms,timestamp) values(?,?,?,?,?,?,?,?,?)")) {
                for (PortResult port : result.getResults()) {
                    portStmt.setString(1, result.getScanId());
                    portStmt.setString(2, port.getHost());
                    portStmt.setInt(3, port.getPort());
                    portStmt.setString(4, String.valueOf(port.getState()));
                    portStmt.setString(5, port.getService());
                    portStmt.setString(6, port.getBanner());
                    portStmt.setInt(7, port.isTls() ? 1 : 0);
                    portStmt.setLong(8, port.getRttMs());
                    portStmt.setLong(9, port.getTimestamp());
                    portStmt.addBatch();
                }
                portStmt.executeBatch();
            }
            connection.commit();
        }
    }

    public List<HistoryItem> loadHistory() throws Exception {
        List<HistoryItem> items = new ArrayList<>();
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("select scan_id,start_time,end_time,total_tasks,completed_tasks,open_count from scan_history order by start_time desc limit 100")) {
            while (rs.next()) {
                items.add(new HistoryItem(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6)));
            }
        }
        return items;
    }

    public PortScanResult loadScan(String scanId) throws Exception {
        if (scanId == null || scanId.trim().isEmpty()) {
            return null;
        }
        PortScanResult result = null;
        try (Connection connection = getConnection()) {
            try (PreparedStatement scanStmt = connection.prepareStatement("select scan_id,start_time,end_time,total_tasks,completed_tasks from scan_history where scan_id=?")) {
                scanStmt.setString(1, scanId);
                try (ResultSet rs = scanStmt.executeQuery()) {
                    if (rs.next()) {
                        result = new PortScanResult();
                        result.setScanId(rs.getString(1));
                        result.setStartTime(rs.getLong(2));
                        result.setEndTime(rs.getLong(3));
                        result.setTotalTasks(rs.getLong(4));
                        result.setCompletedTasks(rs.getLong(5));
                    }
                }
            }
            if (result == null) {
                return null;
            }
            try (PreparedStatement portStmt = connection.prepareStatement("select host,port,state,service,banner,tls,rtt_ms,timestamp from port_result where scan_id=? order by host,port")) {
                portStmt.setString(1, scanId);
                try (ResultSet rs = portStmt.executeQuery()) {
                    while (rs.next()) {
                        PortResult port = new PortResult(rs.getString(1), rs.getInt(2), PortState.valueOf(rs.getString(3)));
                        port.setService(rs.getString(4));
                        port.setBanner(rs.getString(5));
                        port.setTls(rs.getInt(6) == 1);
                        port.setRttMs(rs.getLong(7));
                        port.setTimestamp(rs.getLong(8));
                        result.addOrUpdate(port);
                    }
                }
            }
        }
        return result;
    }

    public void deleteScan(String scanId) throws Exception {
        if (scanId == null || scanId.trim().isEmpty()) {
            return;
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement portStmt = connection.prepareStatement("delete from port_result where scan_id=?")) {
                portStmt.setString(1, scanId);
                portStmt.executeUpdate();
            }
            try (PreparedStatement scanStmt = connection.prepareStatement("delete from scan_history where scan_id=?")) {
                scanStmt.setString(1, scanId);
                scanStmt.executeUpdate();
            }
            connection.commit();
        }
    }

    public void clearHistory() throws Exception {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("delete from port_result");
                stmt.executeUpdate("delete from scan_history");
            }
            connection.commit();
        }
    }

    private void initialize() {
        try {
            Files.createDirectories(dbPath.getParent());
            try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
                stmt.execute("create table if not exists scan_history(scan_id text primary key,start_time integer,end_time integer,total_tasks integer,completed_tasks integer,open_count integer)");
                stmt.execute("create table if not exists port_result(id integer primary key autoincrement,scan_id text,host text,port integer,state text,service text,banner text,tls integer,rtt_ms integer,timestamp integer)");
                stmt.execute("create index if not exists idx_portscan_result_scan on port_result(scan_id)");
            }
        } catch (Exception e) {
            System.err.println("初始化端口扫描数据库失败: " + e.getMessage());
        }
    }

    private Connection getConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    public static class HistoryItem {
        private final String scanId;
        private final long startTime;
        private final long endTime;
        private final long totalTasks;
        private final long completedTasks;
        private final int openCount;

        public HistoryItem(String scanId, long startTime, long endTime, long totalTasks, long completedTasks, int openCount) {
            this.scanId = scanId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.openCount = openCount;
        }

        public String getScanId() {
            return scanId;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public long getTotalTasks() {
            return totalTasks;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }

        public int getOpenCount() {
            return openCount;
        }

        @Override
        public String toString() {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime));
            long duration = Math.max(0L, endTime - startTime) / 1000L;
            return time + " | open=" + openCount + " | " + completedTasks + "/" + totalTasks + " | " + duration + "s | " + scanId;
        }
    }
}
