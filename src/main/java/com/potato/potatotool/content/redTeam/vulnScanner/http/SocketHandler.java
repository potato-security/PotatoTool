package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Socket 处理器，用于执行 TCP 通信
 */
public class SocketHandler {

    /**
     * 执行 TCP 通信
     *
     * @param host    主机地址
     * @param port    端口
     * @param inputs  输入列表
     * @param timeout 超时时间（秒）
     * @return Socket 响应
     */
    public static SocketResponse execute(String host, int port, List<PocObj.Input> inputs, int timeout) {
        SocketResponse response = new SocketResponse();
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), timeout * 1000);
            socket.setSoTimeout(timeout * 1000);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream receivedData = new ByteArrayOutputStream();

            for (PocObj.Input input : inputs) {
                String data = input.getData();
                String type = input.getType(); // hex 或 text
                String read = input.getRead(); // 读取字节数

                // 发送数据
                if (StringUtils.isNotEmpty(data)) {
                    byte[] bytesToSend;
                    if ("hex".equalsIgnoreCase(type)) {
                        bytesToSend = hexStringToByteArray(data);
                    } else {
                        bytesToSend = data.getBytes(StandardCharsets.UTF_8);
                    }
                    outputStream.write(bytesToSend);
                    outputStream.flush();
                }

                // 读取数据
                if (StringUtils.isNotEmpty(read)) {
                    int readSize = 1024; // 默认读取大小
                    try {
                        readSize = Integer.parseInt(read);
                    } catch (NumberFormatException ignored) {
                    }

                    if (readSize > 0) {
                        byte[] buffer = new byte[readSize];
                        int totalRead = 0;
                        while (totalRead < readSize) {
                            int bytesRead = inputStream.read(buffer, totalRead, readSize - totalRead);
                            if (bytesRead == -1) break; // 流已关闭
                            totalRead += bytesRead;
                        }
                        receivedData.write(buffer, 0, totalRead);
                    } else {
                        // 读取直到没有数据或超时
                        try {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                receivedData.write(buffer, 0, bytesRead);
                                if (inputStream.available() == 0) {
                                    // 短暂等待以防网络延迟
                                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                                    if (inputStream.available() == 0) break;
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            // 读取超时，视为读取完成（对于非阻塞协议这是正常的）
                        }
                    }
                }
            }
            
            // 如果没有显式的 read 指令，尝试读取剩余数据
            if (inputs.stream().noneMatch(i -> StringUtils.isNotEmpty(i.getRead())) && inputStream.available() > 0) {
                 byte[] buffer = new byte[1024];
                 int bytesRead;
                 while ((bytesRead = inputStream.read(buffer)) != -1) {
                     receivedData.write(buffer, 0, bytesRead);
                     if (inputStream.available() == 0) break;
                 }
            }

            response.setRaw(receivedData.toByteArray());
            response.setSuccess(true);

        } catch (IOException e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }

        return response;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Data
    public static class SocketResponse {
        private boolean success;
        private byte[] raw;
        private String error;

        public String getRawString() {
            return raw != null ? new String(raw, StandardCharsets.UTF_8) : "";
        }
    }
}
