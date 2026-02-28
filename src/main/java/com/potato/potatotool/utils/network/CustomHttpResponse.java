package com.potato.potatotool.utils.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.potato.potatotool.utils.data.GzipUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.HashMap;

/**
 * 基于OkHttp3的自定义HTTP响应类
 * 提供与原CustomHttpResponse兼容的API
 * @author Potato
 * @date 2024/12/19
 */
public class CustomHttpResponse implements AutoCloseable {

    private Response response;
    private long responseTime;
    private byte[] dataBuffer; // 添加数据缓冲区
    private int maxResponseSize; // 最大响应大小限制

    public CustomHttpResponse(Response response) {
        this.response = response;
        this.maxResponseSize = Integer.MAX_VALUE; // 默认无大小限制
    }
    
    public CustomHttpResponse(Response response, int maxResponseSize) {
        this.response = response;
        this.maxResponseSize = maxResponseSize;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    // 获取或初始化数据缓冲区
    private byte[] getDataBuffer() {
        return getDataBuffer(this.maxResponseSize);
    }
    
    // 获取或初始化数据缓冲区（带大小限制）
    private byte[] getDataBuffer(int maxSize) {
        if (dataBuffer == null) {
            try {
                ResponseBody body = response.body();
                int statusCode = response.code();

                if (body != null) {
                    try (InputStream inputStream = body.byteStream();
                         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[16 * 1024];
                        int bytesRead;
                        int totalBytesRead = 0;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            // 检查大小限制
                            if (totalBytesRead + bytesRead > maxSize) {
                                System.err.println("响应数据过大，已达到限制: " + maxSize + " 字节，截断读取");
                                int remainingBytes = maxSize - totalBytesRead;
                                if (remainingBytes > 0) {
                                    outputStream.write(buffer, 0, remainingBytes);
                                }
                                break;
                            }

                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        dataBuffer = outputStream.toByteArray();
                    }
                } else {
                    dataBuffer = new byte[0];
                }
            } catch (java.io.EOFException e) {
                // 针对 EOF 异常的特殊处理（常见于重定向响应）
                int statusCode = response.code();
                if (statusCode >= 300 && statusCode < 400) {
                    // 3xx 重定向：EOF 是正常的，不输出完整堆栈
                    System.err.println("读取重定向响应体时遇到 EOF（状态码: " + statusCode + "），这通常是正常的");
                } else {
                    // 非重定向状态码：输出详细错误
                    System.err.println("读取响应数据失败，状态码: " + statusCode + ", 错误: EOFException");
                    e.printStackTrace();
                }
                dataBuffer = new byte[0];
            } catch (Exception e) {
                // 其他异常：完整输出
                System.err.println("读取响应数据失败，状态码: " + response.code() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                dataBuffer = new byte[0];
            }
        }
        return dataBuffer;
    }

    public void clearBuffer() {
        dataBuffer = null;
    }

    public String getTextStr() {
        String charsetStr = "UTF-8";
        String contentType = response.header("Content-Type");
        
        if (contentType != null && contentType.toLowerCase().contains("charset=")) {
            charsetStr = contentType.substring(contentType.toLowerCase().indexOf("charset=") + 8);
            // 移除可能的分号和空格
            if (charsetStr.contains(";")) {
                charsetStr = charsetStr.substring(0, charsetStr.indexOf(";"));
            }
            charsetStr = charsetStr.trim();
        }

        Charset charset = null;
        if (!charsetStr.isEmpty()) {
            try {
                charset = Charset.forName(charsetStr);
            } catch (Exception e) {
                charset = StandardCharsets.UTF_8;
            }
        } else {
            charset = StandardCharsets.UTF_8;
        }

        try {
            byte[] buffer = getDataBuffer();
            return new String(buffer, charset);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Document getDocument() { // 转换Jsoup 的 Document 对象，用于解析标签，获取标签内容
        String textStr = getTextStr();
        if (textStr != null && !textStr.isEmpty()) {
            return Jsoup.parse(textStr);
        }
        return null;
    }

    public void getSSEStreamingJson(ResponseCallback callback) { // 获取服务器发送事件(SSE)流式响应json
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    callback.onResponse(line);
                }
                // 回调处理每次响应
            }
        } catch (Exception e) {
            handleException(e, callback);
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        if (response != null) {
            clearBuffer();
            response.close();
            response = null;
        }
    }

    public interface ResponseCallback {
        void onResponse(String line);
    }
    
    private void handleException(Exception e, ResponseCallback callback) {
        String message = e.getMessage();
        if (message.contains("Premature EOF")) {
            callback.onResponse("[[Premature EOF]]");
        } else if (message.contains("Server returned HTTP response code: 502")) {
            callback.onResponse("[[Response code 502]]");
        } else if (message.contains("Read timed out")) {
            callback.onResponse("[[Read timed out]]");
        }
        e.printStackTrace();
    }

    public JsonElement getJson() {
        try {
            String textStr = getTextStr();
            return textStr != null && !textStr.isEmpty() ? new Gson().fromJson(textStr, JsonElement.class) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String saveToFile(String filePath, Boolean showSpeed) {
        try {
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            
            File saveFile = getSaveFile(filePath);
            
            try (InputStream inputStream = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(saveFile)) {

                byte[] buffer = new byte[16 * 1024];
                int bytesRead = 0, len;
                long startTime = System.currentTimeMillis(), lastBytesRead = 0;
                long fileSize = body.contentLength();

                // 该判断不要写while内，影响运行速率
                if (showSpeed && fileSize > 0) {
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        bytesRead += len;

                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTime;
                        if (elapsedTime >= 1000) {
                            long currentBytesRead = bytesRead;
                            long bytesPerSecond = (currentBytesRead - lastBytesRead) * 1000 / elapsedTime;
                            long remainingTimeInSeconds = (fileSize - currentBytesRead) / bytesPerSecond;

                            String timeUnit;
                            long remainingTime;
                            if (remainingTimeInSeconds >= 3600) {
                                timeUnit = "小时";
                                remainingTime = remainingTimeInSeconds / 3600;
                            } else if (remainingTimeInSeconds >= 60) {
                                timeUnit = "分钟";
                                remainingTime = remainingTimeInSeconds / 60;
                            } else {
                                timeUnit = "秒";
                                remainingTime = remainingTimeInSeconds;
                            }

                            String speedUnit;
                            double speed;
                            if (bytesPerSecond >= 1024 * 1024) {
                                speedUnit = "MB/s";
                                speed = bytesPerSecond / (1024.0 * 1024.0);
                            } else {
                                speedUnit = "KB/s";
                                speed = bytesPerSecond / 1024.0;
                            }

                            double progress = (double) bytesRead / fileSize; // 基于压缩文件大小计算进度

                            System.out.printf("下载进度: %.0f%%, 速度: %.2f %s, 预估时间: %d %s\n",
                                    progress * 100, speed, speedUnit, remainingTime, timeUnit);

                            lastBytesRead = currentBytesRead;
                            startTime = currentTime;
                        }
                    }
                } else {
                    while ((len = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }

                return saveFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String saveToFileByGzip(String filePath, ProgressBar progressBar, Label progressLabel) {
        try {
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            
            File saveFile = getSaveGzipFile(filePath);
            
            try (InputStream inputStream = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(saveFile)) {

                byte[] buffer = new byte[16 * 1024];
                int bytesRead = 0, len;
                long startTime = System.currentTimeMillis(), lastBytesRead = 0;
                long fileSize = body.contentLength();

                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    bytesRead += len;

                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime;
                    if (elapsedTime >= 1000 && fileSize > 0) {
                        long currentBytesRead = bytesRead;
                        long bytesPerSecond = (currentBytesRead - lastBytesRead) * 1000 / elapsedTime;
                        long remainingTimeInSeconds = (fileSize - currentBytesRead) / bytesPerSecond;

                        String timeUnit;
                        long remainingTime;
                        if (remainingTimeInSeconds >= 3600) {
                            timeUnit = "小时";
                            remainingTime = remainingTimeInSeconds / 3600;
                        } else if (remainingTimeInSeconds >= 60) {
                            timeUnit = "分钟";
                            remainingTime = remainingTimeInSeconds / 60;
                        } else {
                            timeUnit = "秒";
                            remainingTime = remainingTimeInSeconds;
                        }

                        String speedUnit;
                        double speed;
                        if (bytesPerSecond >= 1024 * 1024) {
                            speedUnit = "MB/s";
                            speed = bytesPerSecond / (1024.0 * 1024.0);
                        } else {
                            speedUnit = "KB/s";
                            speed = bytesPerSecond / 1024.0;
                        }

                        double progress = (double) bytesRead / fileSize; // 基于压缩文件大小计算进度

                         Platform.runLater(() -> {
                             progressBar.setProgress(progress); // 更新进度条
                             progressLabel.setText(String.format("下载进度: %.0f%%, 速度: %.2f %s, 预估时间: %d %s",
                                     progress * 100, speed, speedUnit, remainingTime, timeUnit));
                         });

                        lastBytesRead = currentBytesRead;
                        startTime = currentTime;
                    }
                }
                
                 Platform.runLater(() -> {
                     progressBar.setProgress(1);
                     progressLabel.setText("解压中，请稍等……");
                 });

                String gzipAbsolutePath = saveFile.getAbsolutePath();
                String absolutePath = gzipAbsolutePath.replace(".gzip", "");

                GzipUtils.unGzipFile(gzipAbsolutePath, absolutePath, true);

                return absolutePath;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String saveToFileByGzip(String filePath, Boolean showSpeed) {
        try {
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            
            File saveFile = getSaveFile(filePath);
            
            try (InputStream inputStream = body.byteStream();
                 GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                 FileOutputStream fos = new FileOutputStream(saveFile)) {

                byte[] buffer = new byte[16 * 1024];
                int bytesRead = 0, len;
                long startTime = System.currentTimeMillis(), lastBytesRead = 0;
                long fileSize = body.contentLength();

                // 该判断不要写while内，影响运行速率
                if (showSpeed && fileSize > 0) {
                    while ((len = gzipInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        bytesRead += len;

                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTime;
                        if (elapsedTime >= 1000) {
                            long currentBytesRead = bytesRead;
                            long bytesPerSecond = (currentBytesRead - lastBytesRead) * 1000 / elapsedTime;
                            long remainingTimeInSeconds = (fileSize - currentBytesRead) / bytesPerSecond;

                            String timeUnit;
                            long remainingTime;
                            if (remainingTimeInSeconds >= 3600) {
                                timeUnit = "小时";
                                remainingTime = remainingTimeInSeconds / 3600;
                            } else if (remainingTimeInSeconds >= 60) {
                                timeUnit = "分钟";
                                remainingTime = remainingTimeInSeconds / 60;
                            } else {
                                timeUnit = "秒";
                                remainingTime = remainingTimeInSeconds;
                            }

                            String speedUnit;
                            double speed;
                            if (bytesPerSecond >= 1024 * 1024) {
                                speedUnit = "MB/s";
                                speed = bytesPerSecond / (1024.0 * 1024.0);
                            } else {
                                speedUnit = "KB/s";
                                speed = bytesPerSecond / 1024.0;
                            }

                            double progress = (double) bytesRead / fileSize;

                            System.out.printf("下载进度: %.0f%%, 速度: %.2f %s, 预估时间: %d %s\n",
                                    progress * 100, speed, speedUnit, remainingTime, timeUnit);

                            lastBytesRead = currentBytesRead;
                            startTime = currentTime;
                        }
                    }
                } else {
                    while ((len = gzipInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }

                return saveFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File getSaveFile(String filePath) throws IOException {
        File saveFile = new File(filePath);
        if (saveFile.isDirectory()) {
            if (!saveFile.exists()) {
                saveFile.mkdirs();
            }
            saveFile = new File(saveFile, getFileName());
        }
        return saveFile;
    }

    private File getSaveGzipFile(String filePath) throws IOException {
        File saveFile = new File(filePath);
        if (saveFile.isDirectory()) {
            if (!saveFile.exists()) {
                saveFile.mkdirs();
            }
            String fileName = getFileName();
            if (!fileName.isEmpty() && !fileName.endsWith(".gzip")) {
                fileName = fileName + ".gzip";
            }
            saveFile = new File(saveFile, fileName);
        }
        return saveFile;
    }

    // 获取文件名
    private String getFileName() {
        String disposition = response.header("Content-Disposition");
        if (disposition != null) {
            // 从Content-Disposition中获取文件名
            Pattern pattern = Pattern.compile("filename\\s*=\\s*\"?([^\";]+)\"?");
            Matcher matcher = pattern.matcher(disposition);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } else {
            // 从URL中获取文件名
            String fileName = response.request().url().toString();
            return fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        }
        return "";
    }

    public URL getURL() {
        try {
            return response.request().url().url();
        } catch (Exception e) {
            return null;
        }
    }

    public int getResponseCode() {
        return response.code();
    }

    public int getContentLength() {
        ResponseBody body = response.body();
        if (body != null) {
            long length = body.contentLength();
            return length > Integer.MAX_VALUE ? -1 : (int) length;
        }
        return -1;
    }

    public long getLastModified() {
        String lastModified = response.header("Last-Modified");
        if (lastModified != null) {
            try {
                return java.text.DateFormat.getDateInstance().parse(lastModified).getTime();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public Response getRequest() {
        return response;
    }

    public String getContentType() {
        return response.header("Content-Type");
    }

    public InputStream getInputStream() throws Exception {
        ResponseBody body = response.body();
        return body != null ? body.byteStream() : null;
    }

    public byte[] getByteArray() {
        return getDataBuffer();
    }

    public String getContentEncoding() {
        return response.header("Content-Encoding");
    }

    public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> headerMap = new HashMap<>();
        for (String name : response.headers().names()) {
            headerMap.put(name, response.headers().values(name));
        }
        return headerMap;
    }
    
    public String getHeaderField(int n) {
        if (n < response.headers().size()) {
            return response.headers().value(n);
        }
        return null;
    }
    
    public List<String> getHeaderField(String str) {
        return response.headers().values(str);
    }
    
    public String getHeaderFields(int n) {
        if (n < response.headers().size()) {
            return response.headers().name(n);
        }
        return null;
    }

    public String getResponseMessage() {
        return response.message();
    }

    public String getHeaderFieldsText() {
        StringBuilder sb = new StringBuilder();
        // 添加响应头
        for (String name : response.headers().names()) {
            for (String value : response.headers().values(name)) {
                sb.append(name).append(": ").append(value).append("\n");
            }
        }
        return sb.toString();
    }

    public String getAllResponseText() {
        StringBuilder sb = new StringBuilder();
        
        // 添加状态行 - 使用实际的协议版本
        sb.append(response.protocol().toString().toUpperCase()).append(" ")
                .append(response.code()).append(" ")
                .append(response.message()).append("\n");

        // 添加响应头
        for (String name : response.headers().names()) {
            for (String value : response.headers().values(name)) {
                sb.append(name).append(": ").append(value).append("\n");
            }
        }

        // 添加空行和响应体
        sb.append("\n").append(getTextStr());

        return sb.toString();
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}