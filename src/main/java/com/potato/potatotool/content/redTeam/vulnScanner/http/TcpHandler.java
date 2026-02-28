package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP协议处理器
 * 用于Nuclei TCP协议段的实现
 * 支持原始TCP连接、十六进制数据发送和接收
 *
 * @author PotatoTool
 */
public class TcpHandler {

    private static final int DEFAULT_TIMEOUT = 10000; // 10秒默认超时

    /**
     * TCP响应结果
     */
    public static class TcpResponse {
        private String address;           // 目标地址
        private boolean success;          // 是否成功
        private byte[] data;              // 接收到的数据
        private String dataString;        // 数据的字符串表示
        private long duration;            // 耗时（毫秒）
        private String error;             // 错误信息
        private int bytesReceived;        // 接收的字节数
        private int bytesSent;            // 发送的字节数

        public TcpResponse(String address) {
            this.address = address;
            this.success = false;
        }

        // Getters
        public String getAddress() { return address; }
        public boolean isSuccess() { return success; }
        public byte[] getData() { return data; }
        public String getDataString() { return dataString; }
        public long getDuration() { return duration; }
        public String getError() { return error; }
        public int getBytesReceived() { return bytesReceived; }
        public int getBytesSent() { return bytesSent; }

        // Setters
        public void setSuccess(boolean success) { this.success = success; }
        public void setData(byte[] data) {
            this.data = data;
            if (data != null) {
                this.dataString = new String(data, StandardCharsets.UTF_8);
                this.bytesReceived = data.length;
            }
        }
        public void setDuration(long duration) { this.duration = duration; }
        public void setError(String error) { this.error = error; }
        public void setBytesSent(int bytesSent) { this.bytesSent = bytesSent; }

        @Override
        public String toString() {
            if (success) {
                return String.format("TCP %s: 成功 (发送%d字节, 接收%d字节, %dms)",
                                   address, bytesSent, bytesReceived, duration);
            } else {
                return String.format("TCP %s: 失败 (%s)", address, error);
            }
        }
    }

    /**
     * 发送TCP请求
     *
     * @param address 目标地址 (host:port)
     * @param data 要发送的数据（字节数组）
     * @param timeout 超时时间（毫秒）
     * @return TCP响应对象
     */
    public static TcpResponse send(String address, byte[] data, int timeout) {
        TcpResponse response = new TcpResponse(address);
        long startTime = System.currentTimeMillis();

        Socket socket = null;
        try {
            // 解析地址
            String[] parts = address.split(":");
            if (parts.length < 2) {
                response.setError("地址格式错误，应为 host:port");
                return response;
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // 创建Socket连接
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            // 发送数据
            if (data != null && data.length > 0) {
                OutputStream out = socket.getOutputStream();
                out.write(data);
                out.flush();
                response.setBytesSent(data.length);
            }

            // 接收响应
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;

            // 读取数据，直到超时或连接关闭
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);

                // 如果没有更多数据可读，退出
                if (in.available() == 0) {
                    break;
                }
            }

            response.setData(baos.toByteArray());
            response.setSuccess(true);
            response.setDuration(System.currentTimeMillis() - startTime);

        } catch (SocketTimeoutException e) {
            response.setError("连接超时");
            response.setDuration(System.currentTimeMillis() - startTime);
        } catch (ConnectException e) {
            response.setError("连接被拒绝");
            response.setDuration(System.currentTimeMillis() - startTime);
        } catch (UnknownHostException e) {
            response.setError("未知主机: " + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            response.setError("IO错误: " + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            response.setError("未知错误: " + e.getMessage());
            response.setDuration(System.currentTimeMillis() - startTime);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        return response;
    }

    /**
     * 发送TCP请求（使用默认超时）
     */
    public static TcpResponse send(String address, byte[] data) {
        return send(address, data, DEFAULT_TIMEOUT);
    }

    /**
     * 发送TCP请求（字符串数据）
     */
    public static TcpResponse send(String address, String data, int timeout) {
        byte[] bytes = data != null ? data.getBytes(StandardCharsets.UTF_8) : null;
        return send(address, bytes, timeout);
    }

    /**
     * 发送TCP请求（十六进制字符串数据）
     *
     * @param address 目标地址
     * @param hexData 十六进制字符串，如 "48656C6C6F" 表示 "Hello"
     * @param timeout 超时时间
     * @return TCP响应
     */
    public static TcpResponse sendHex(String address, String hexData, int timeout) {
        try {
            byte[] bytes = hexStringToByteArray(hexData);
            return send(address, bytes, timeout);
        } catch (Exception e) {
            TcpResponse response = new TcpResponse(address);
            response.setError("十六进制数据解析失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String hexString) {
        // 移除空格和换行
        hexString = hexString.replaceAll("\\s+", "");

        int len = hexString.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 批量TCP探测（检查端口是否开放）
     *
     * @param host 目标主机
     * @param ports 端口列表
     * @param timeout 超时时间
     * @return 开放的端口列表
     */
    public static List<Integer> probePortsBatch(String host, int[] ports, int timeout) {
        List<Integer> openPorts = new ArrayList<>();

        for (int port : ports) {
            if (isPortOpen(host, port, timeout)) {
                openPorts.add(port);
            }
        }

        return openPorts;
    }

    /**
     * 检查端口是否开放
     */
    public static boolean isPortOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 读取TCP Banner（服务识别）
     *
     * @param address 目标地址
     * @param timeout 超时时间
     * @return Banner字符串
     */
    public static String readBanner(String address, int timeout) {
        TcpResponse response = send(address, (byte[]) null, timeout);

        if (response.isSuccess() && response.getData() != null) {
            return response.getDataString();
        }

        return "";
    }

    /**
     * TCP交互式会话（发送多个请求）
     */
    public static class TcpSession {
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private String address;

        public TcpSession(String address, int timeout) throws IOException {
            this.address = address;

            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            in = socket.getInputStream();
            out = socket.getOutputStream();
        }

        public void send(byte[] data) throws IOException {
            out.write(data);
            out.flush();
        }

        public byte[] receive() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);

                if (in.available() == 0) {
                    break;
                }
            }

            return baos.toByteArray();
        }

        public void close() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java TcpHandler <host:port> [hex_data]");
            System.out.println("示例: java TcpHandler www.google.com:80");
            System.out.println("示例: java TcpHandler www.google.com:80 474554202F");
            return;
        }

        String address = args[0];
        String hexData = args.length > 1 ? args[1] : null;

        System.out.println("TCP连接测试: " + address);

        TcpResponse response;
        if (hexData != null) {
            System.out.println("发送十六进制数据: " + hexData);
            response = sendHex(address, hexData, DEFAULT_TIMEOUT);
        } else {
            System.out.println("仅连接测试（不发送数据）");
            response = send(address, (byte[]) null, DEFAULT_TIMEOUT);
        }

        System.out.println("结果: " + response);

        if (response.isSuccess() && response.getData() != null) {
            System.out.println("接收数据 (UTF-8):");
            System.out.println(response.getDataString());
            System.out.println("\n接收数据 (HEX):");
            System.out.println(byteArrayToHexString(response.getData()));
        }
    }
}
