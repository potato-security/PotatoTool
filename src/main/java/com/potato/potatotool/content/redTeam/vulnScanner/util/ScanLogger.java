package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.storage.PathManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 扫描日志管理器
 * 提供扫描日志的记录、查询和持久化功能
 *
 * @author Potato
 * @date 2025/11/26
 */
public class ScanLogger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MAX_MEMORY_LOGS = 1000;

    private static ScanLogger instance;

    private final ConcurrentLinkedQueue<LogEntry> memoryLogs = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final Path logDirectory;
    private String currentScanId;
    private PrintWriter currentLogWriter;

    /**
     * 日志监听器接口
     */
    public interface LogListener {
        /**
         * 当有新日志记录时被调用
         * @param entry 新的日志条目
         */
        void onNewLog(LogEntry entry);
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR, SUCCESS
    }
    
    public static class LogEntry {
        private final long timestamp;
        private final LogLevel level;
        private final String category;
        private final String message;
        private final String scanId;
        
        public LogEntry(LogLevel level, String category, String message, String scanId) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.category = category;
            this.message = message;
            this.scanId = scanId;
        }
        
        public long getTimestamp() { return timestamp; }
        public LogLevel getLevel() { return level; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public String getScanId() { return scanId; }
        
        public String getFormattedTime() {
            return DATE_FORMAT.format(new Date(timestamp));
        }
        
        @Override
        public String toString() {
            return String.format("[%s] [%s] [%s] %s", 
                getFormattedTime(), level, category, message);
        }
    }
    
    private ScanLogger() {
        this.logDirectory = PathManager.getInstance().getResourcePath("vulnscan").resolve("logs");
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException e) {
            System.err.println("无法创建日志目录: " + e.getMessage());
        }
    }
    
    public static synchronized ScanLogger getInstance() {
        if (instance == null) {
            instance = new ScanLogger();
        }
        return instance;
    }
    
    /**
     * 开始新的扫描会话
     */
    public void startScanSession(String scanId) {
        this.currentScanId = scanId;
        
        // 创建日志文件
        String fileName = String.format("scan_%s_%s.log", 
            FILE_DATE_FORMAT.format(new Date()), scanId);
        Path logFile = logDirectory.resolve(fileName);
        
        try {
            currentLogWriter = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile.toFile(), true), StandardCharsets.UTF_8)));
            info("SYSTEM", "扫描会话开始: " + scanId);
        } catch (IOException e) {
            System.err.println("无法创建日志文件: " + e.getMessage());
        }
    }
    
    /**
     * 结束扫描会话
     */
    public void endScanSession() {
        if (currentLogWriter != null) {
            info("SYSTEM", "扫描会话结束: " + currentScanId);
            currentLogWriter.close();
            currentLogWriter = null;
        }
        currentScanId = null;
    }
    
    /**
     * 记录调试日志
     */
    public void debug(String category, String message) {
        log(LogLevel.DEBUG, category, message);
    }
    
    /**
     * 记录信息日志
     */
    public void info(String category, String message) {
        log(LogLevel.INFO, category, message);
    }
    
    /**
     * 记录警告日志
     */
    public void warn(String category, String message) {
        log(LogLevel.WARN, category, message);
    }
    
    /**
     * 记录错误日志
     */
    public void error(String category, String message) {
        log(LogLevel.ERROR, category, message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public void error(String category, String message, Throwable throwable) {
        log(LogLevel.ERROR, category, message + " - " + throwable.getMessage());
    }
    
    /**
     * 记录成功日志
     */
    public void success(String category, String message) {
        log(LogLevel.SUCCESS, category, message);
    }
    
    /**
     * 记录日志
     */
    private void log(LogLevel level, String category, String message) {
        LogEntry entry = new LogEntry(level, category, message, currentScanId);

        // 添加到内存队列
        memoryLogs.offer(entry);
        while (memoryLogs.size() > MAX_MEMORY_LOGS) {
            memoryLogs.poll();
        }

        // 写入文件
        if (currentLogWriter != null) {
            currentLogWriter.println(entry.toString());
            currentLogWriter.flush();
        }

        // 通知所有监听器
        notifyListeners(entry);
    }

    /**
     * 添加日志监听器
     */
    public void addListener(LogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除日志监听器
     */
    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners(LogEntry entry) {
        for (LogListener listener : listeners) {
            try {
                listener.onNewLog(entry);
            } catch (Exception e) {
                // 忽略监听器异常，避免影响日志记录
                System.err.println("日志监听器异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取内存中的日志
     */
    public List<LogEntry> getMemoryLogs() {
        return new ArrayList<>(memoryLogs);
    }
    
    /**
     * 获取指定级别的日志
     */
    public List<LogEntry> getLogsByLevel(LogLevel level) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : memoryLogs) {
            if (entry.getLevel() == level) {
                result.add(entry);
            }
        }
        return result;
    }
    
    /**
     * 获取指定类别的日志
     */
    public List<LogEntry> getLogsByCategory(String category) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : memoryLogs) {
            if (category.equals(entry.getCategory())) {
                result.add(entry);
            }
        }
        return result;
    }
    
    /**
     * 清空内存日志
     */
    public void clearMemoryLogs() {
        memoryLogs.clear();
    }
    
    /**
     * 获取所有日志文件列表
     */
    public List<Path> getLogFiles() {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDirectory, "*.log")) {
            for (Path path : stream) {
                files.add(path);
            }
        } catch (IOException e) {
            System.err.println("无法读取日志目录: " + e.getMessage());
        }
        // 按时间倒序排列
        files.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (IOException e) {
                return 0;
            }
        });
        return files;
    }
    
    /**
     * 读取日志文件内容
     */
    public List<String> readLogFile(Path logFile) {
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("无法读取日志文件: " + e.getMessage());
        }
        return lines;
    }
    
    /**
     * 删除日志文件
     */
    public boolean deleteLogFile(Path logFile) {
        try {
            return Files.deleteIfExists(logFile);
        } catch (IOException e) {
            System.err.println("无法删除日志文件: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清理旧日志（保留最近N天）
     */
    public int cleanOldLogs(int keepDays) {
        int deleted = 0;
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);
        
        for (Path logFile : getLogFiles()) {
            try {
                if (Files.getLastModifiedTime(logFile).toMillis() < cutoffTime) {
                    Files.delete(logFile);
                    deleted++;
                }
            } catch (IOException e) {
                System.err.println("无法删除旧日志: " + e.getMessage());
            }
        }
        return deleted;
    }
    
    /**
     * 记录扫描事件
     */
    public void logScanEvent(String target, String pocId, String result, long duration) {
        String message = String.format("Target: %s | POC: %s | Result: %s | Duration: %dms",
            target, pocId, result, duration);
        if ("VULNERABLE".equals(result)) {
            success("SCAN", message);
        } else if ("ERROR".equals(result)) {
            error("SCAN", message);
        } else {
            info("SCAN", message);
        }
    }
    
    /**
     * 记录网络请求
     */
    public void logRequest(String method, String url, int statusCode, long duration) {
        String message = String.format("%s %s -> %d (%dms)", method, url, statusCode, duration);
        if (statusCode >= 400) {
            warn("HTTP", message);
        } else {
            debug("HTTP", message);
        }
    }
}
