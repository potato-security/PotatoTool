package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * JARM指纹识别核心实现
 *
 * JARM是一种主动TLS指纹识别技术，通过发送10个特制的TLS Client Hello并分析响应来生成62字符指纹
 *
 * 指纹格式: 30字符(10个探测的密码套件+版本) + 32字符(扩展和ALPN的SHA256哈希)
 *
 * @author PotatoTool
 */
public class JarmFingerprinter {

    private static final int DEFAULT_TIMEOUT = 10000; // 10秒超时
    private static final int CONNECT_TIMEOUT = 5000;  // 5秒连接超时

    // 缓存JARM结果，避免重复计算
    private static final Map<String, String> jarmCache = new ConcurrentHashMap<>();

    /**
     * 计算JARM指纹 (带缓存)
     *
     * @param host 目标主机
     * @param port 目标端口
     * @return 62字符JARM指纹，失败返回空字符串
     */
    public static String computeJarm(String host, int port) {
        String cacheKey = host + ":" + port;

        // 检查缓存
        if (jarmCache.containsKey(cacheKey)) {
            return jarmCache.get(cacheKey);
        }

        try {
            String fingerprint = computeJarmUncached(host, port);
            jarmCache.put(cacheKey, fingerprint);
            return fingerprint;
        } catch (Exception e) {
            System.err.println("JARM指纹计算失败 " + host + ":" + port + " - " + e.getMessage());
            return "";
        }
    }

    /**
     * 计算JARM指纹 (不使用缓存)
     */
    private static String computeJarmUncached(String host, int port) throws Exception {
        JarmProbe[] probes = JarmProbe.createStandardProbes();

        // 存储10个探测的结果
        ProbeResult[] results = new ProbeResult[10];

        // 并行执行10个探测 (提高速度)
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<ProbeResult>> futures = new ArrayList<>();

        for (int i = 0; i < probes.length; i++) {
            final int probeIndex = i;
            final JarmProbe probe = probes[i];

            Future<ProbeResult> future = executor.submit(() -> {
                return sendProbe(host, port, probe);
            });

            futures.add(future);
        }

        // 收集结果
        for (int i = 0; i < futures.size(); i++) {
            try {
                results[i] = futures.get(i).get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                results[i] = new ProbeResult(); // 超时返回空结果
            } catch (Exception e) {
                results[i] = new ProbeResult(); // 错误返回空结果
            }
        }

        executor.shutdown();

        // 计算指纹
        return calculateFingerprint(results);
    }

    /**
     * 发送单个TLS探测
     */
    private static ProbeResult sendProbe(String host, int port, JarmProbe probe) {
        Socket socket = null;
        try {
            // 创建Socket连接
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            socket.setSoTimeout(DEFAULT_TIMEOUT);

            // 构造Client Hello
            byte[] clientHello = TlsPacketBuilder.buildClientHello(host, probe);

            // 发送Client Hello
            OutputStream out = socket.getOutputStream();
            out.write(clientHello);
            out.flush();

            // 接收Server Hello
            InputStream in = socket.getInputStream();
            byte[] serverResponse = readTlsRecord(in);

            // 解析Server Hello
            TlsPacketBuilder.ServerHelloResult serverHello =
                TlsPacketBuilder.parseServerHello(serverResponse);

            ProbeResult result = new ProbeResult();
            result.success = serverHello.success;
            result.serverVersion = serverHello.serverVersion;
            result.cipherSuite = serverHello.cipherSuite;
            result.extensions = serverHello.extensions;
            result.alpnProtocol = serverHello.alpnProtocol;

            return result;

        } catch (SocketTimeoutException e) {
            // 超时
            return new ProbeResult();
        } catch (IOException e) {
            // 连接失败或读取失败
            return new ProbeResult();
        } catch (Exception e) {
            // 其他错误
            System.err.println("探测" + probe.getProbeNumber() + "失败: " + e.getMessage());
            return new ProbeResult();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 读取一个完整的TLS Record
     */
    private static byte[] readTlsRecord(InputStream in) throws IOException {
        // TLS Record Header: 5 bytes
        byte[] header = new byte[5];
        int bytesRead = in.read(header);

        if (bytesRead < 5) {
            return new byte[0];
        }

        // 解析记录长度
        int recordLength = ((header[3] & 0xFF) << 8) | (header[4] & 0xFF);

        // 读取记录内容
        byte[] content = new byte[recordLength];
        int totalRead = 0;

        while (totalRead < recordLength) {
            int read = in.read(content, totalRead, recordLength - totalRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }

        // 合并header和content
        byte[] fullRecord = new byte[5 + totalRead];
        System.arraycopy(header, 0, fullRecord, 0, 5);
        System.arraycopy(content, 0, fullRecord, 5, totalRead);

        return fullRecord;
    }

    /**
     * 计算62字符JARM指纹
     *
     * 格式: 30字符(密码+版本) + 32字符(SHA256哈希)
     */
    private static String calculateFingerprint(ProbeResult[] results) {
        StringBuilder fingerprint = new StringBuilder();

        // Part 1: 前30个字符 - 每个探测的密码套件+TLS版本 (3字符/探测)
        for (ProbeResult result : results) {
            if (result.success) {
                // 获取密码套件索引 (简化版 - 使用密码套件的hex值前两位)
                int cipherIndex = findCipherIndex(result.cipherSuite);
                String cipherHex = String.format("%02x", cipherIndex);

                // 获取TLS版本字符
                String version = getTlsVersionChar(result.serverVersion);

                fingerprint.append(cipherHex).append(version);
            } else {
                // 失败的探测返回 "000"
                fingerprint.append("000");
            }
        }

        // Part 2: 后32个字符 - 扩展和ALPN的SHA256哈希
        String hashPart = calculateExtensionHash(results);
        fingerprint.append(hashPart);

        return fingerprint.toString();
    }

    /**
     * 查找密码套件在标准列表中的索引
     */
    private static int findCipherIndex(int cipherSuite) {
        for (int i = 0; i < JarmProbe.ALL_CIPHER_SUITES.length; i++) {
            if (JarmProbe.ALL_CIPHER_SUITES[i] == cipherSuite) {
                return i;
            }
        }
        // 如果不在列表中，返回0
        return 0;
    }

    /**
     * 获取TLS版本对应的字符
     */
    private static String getTlsVersionChar(int version) {
        switch (version) {
            case 0x0300: return "a"; // SSL 3.0
            case 0x0301: return "b"; // TLS 1.0
            case 0x0302: return "c"; // TLS 1.1
            case 0x0303: return "d"; // TLS 1.2
            case 0x0304: return "e"; // TLS 1.3
            default: return "0";     // 未知或失败
        }
    }

    /**
     * 计算扩展和ALPN的SHA256哈希 (32字符)
     */
    private static String calculateExtensionHash(ProbeResult[] results) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 收集所有扩展和ALPN信息
            StringBuilder data = new StringBuilder();

            for (ProbeResult result : results) {
                // 添加扩展列表
                if (result.extensions != null && !result.extensions.isEmpty()) {
                    for (int ext : result.extensions) {
                        data.append(String.format("%04x", ext));
                    }
                } else {
                    data.append("000");
                }

                // 添加ALPN协议
                if (result.alpnProtocol != null && !result.alpnProtocol.isEmpty()) {
                    data.append(result.alpnProtocol);
                } else {
                    data.append("00");
                }

                data.append("|"); // 分隔符
            }

            // 计算SHA256
            byte[] hash = digest.digest(data.toString().getBytes("UTF-8"));

            // 转换为hex字符串，取前32字符
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(16, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 如果不足32字符，补0
            while (hexString.length() < 32) {
                hexString.append('0');
            }

            return hexString.substring(0, 32);

        } catch (Exception e) {
            // 如果SHA256失败，返回32个0
            return "00000000000000000000000000000000";
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        jarmCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return jarmCache.size();
    }

    /**
     * 探测结果
     */
    private static class ProbeResult {
        boolean success = false;
        int serverVersion = 0;
        int cipherSuite = 0;
        List<Integer> extensions = new ArrayList<>();
        String alpnProtocol = null;
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java JarmFingerprinter <host> [port]");
            System.out.println("示例: java JarmFingerprinter www.google.com 443");
            return;
        }

        String host = args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 443;

        System.out.println("正在计算 " + host + ":" + port + " 的JARM指纹...");
        long startTime = System.currentTimeMillis();

        String fingerprint = computeJarm(host, port);

        long elapsed = System.currentTimeMillis() - startTime;

        if (fingerprint.isEmpty()) {
            System.out.println("✗ JARM指纹计算失败");
        } else {
            System.out.println("✓ JARM指纹: " + fingerprint);
            System.out.println("  耗时: " + elapsed + "ms");
        }
    }
}
