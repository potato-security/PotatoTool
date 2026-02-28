package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * JARM指纹识别高层封装
 * 提供批量扫描、异步计算、结果管理等功能
 *
 * @author PotatoTool
 */
public class JarmHandler {

    /**
     * JARM扫描结果
     */
    public static class JarmResult {
        private String host;
        private int port;
        private String fingerprint;
        private boolean success;
        private long elapsedMs;
        private String error;

        public JarmResult(String host, int port) {
            this.host = host;
            this.port = port;
            this.success = false;
        }

        // Getters and setters
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getFingerprint() { return fingerprint; }
        public boolean isSuccess() { return success; }
        public long getElapsedMs() { return elapsedMs; }
        public String getError() { return error; }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            this.success = fingerprint != null && !fingerprint.isEmpty();
        }

        public void setElapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        public void setError(String error) {
            this.error = error;
            this.success = false;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("%s:%d => %s (%dms)", host, port, fingerprint, elapsedMs);
            } else {
                return String.format("%s:%d => 失败 (%s)", host, port, error != null ? error : "未知错误");
            }
        }
    }

    /**
     * 计算JARM指纹（同步）
     *
     * @param host 目标主机
     * @param port 目标端口
     * @return JARM结果对象
     */
    public static JarmResult compute(String host, int port) {
        JarmResult result = new JarmResult(host, port);
        long startTime = System.currentTimeMillis();

        try {
            String fingerprint = JarmFingerprinter.computeJarm(host, port);
            result.setFingerprint(fingerprint);
            result.setElapsedMs(System.currentTimeMillis() - startTime);

            if (fingerprint.isEmpty()) {
                result.setError("指纹计算返回空");
            }
        } catch (Exception e) {
            result.setError(e.getMessage());
            result.setElapsedMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * 计算JARM指纹（异步）
     *
     * @param host 目标主机
     * @param port 目标端口
     * @return CompletableFuture包装的结果
     */
    public static CompletableFuture<JarmResult> computeAsync(String host, int port) {
        return CompletableFuture.supplyAsync(() -> compute(host, port));
    }

    /**
     * 批量计算JARM指纹
     *
     * @param targets 目标列表，格式: ["host:port", "host2:port2", ...]
     * @return 结果映射表: host:port => JarmResult
     */
    public static Map<String, JarmResult> computeBatch(String[] targets) {
        Map<String, JarmResult> results = new HashMap<>();

        // 异步并行计算
        CompletableFuture<?>[] futures = new CompletableFuture[targets.length];

        for (int i = 0; i < targets.length; i++) {
            String target = targets[i];
            String[] parts = target.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;

            futures[i] = computeAsync(host, port).thenAccept(result -> {
                synchronized (results) {
                    results.put(target, result);
                }
            });
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();

        return results;
    }

    /**
     * 解析主机名和端口
     *
     * @param hostPort 格式: "host:port" 或 "host"
     * @return [host, port]
     */
    public static String[] parseHostPort(String hostPort) {
        String[] parts = hostPort.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;
        return new String[]{host, String.valueOf(port)};
    }

    /**
     * 快速检查主机是否支持TLS
     *
     * @param host 目标主机
     * @param port 目标端口
     * @return true表示支持
     */
    public static boolean isTlsSupported(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清空JARM缓存
     */
    public static void clearCache() {
        JarmFingerprinter.clearCache();
    }

    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        int cacheSize = JarmFingerprinter.getCacheSize();
        return String.format("JARM缓存: %d个指纹", cacheSize);
    }

    /**
     * 比较两个JARM指纹是否匹配
     *
     * @param fingerprint1 指纹1
     * @param fingerprint2 指纹2
     * @return true表示匹配
     */
    public static boolean compareFingerprints(String fingerprint1, String fingerprint2) {
        if (fingerprint1 == null || fingerprint2 == null) {
            return false;
        }

        // 完全匹配
        if (fingerprint1.equals(fingerprint2)) {
            return true;
        }

        // 忽略大小写匹配
        return fingerprint1.equalsIgnoreCase(fingerprint2);
    }

    /**
     * 验证JARM指纹格式是否正确
     *
     * @param fingerprint JARM指纹
     * @return true表示格式正确
     */
    public static boolean isValidFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            return false;
        }

        // JARM指纹应为62个十六进制字符
        if (fingerprint.length() != 62) {
            return false;
        }

        // 检查是否全为十六进制字符
        return fingerprint.matches("[0-9a-fA-F]{62}");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: java JarmHandler <host:port> [host2:port2 ...]");
            System.out.println("示例: java JarmHandler www.google.com:443 www.baidu.com:443");
            return;
        }

        System.out.println("=== JARM指纹批量扫描 ===\n");

        Map<String, JarmResult> results = computeBatch(args);

        System.out.println("扫描结果:");
        for (Map.Entry<String, JarmResult> entry : results.entrySet()) {
            System.out.println("  " + entry.getValue());
        }

        System.out.println("\n" + getCacheStats());
    }
}
