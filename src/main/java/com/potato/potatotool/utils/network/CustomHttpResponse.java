package com.potato.potatotool.utils.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.potato.potatotool.utils.data.GzipUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @author Potato
 * @date 2023/4/13 14:32
 */

public class CustomHttpResponse{

    private HttpURLConnection con;
    private long responseTime;
    private byte[] dataBuffer; // 添加数据缓冲区
    private int maxResponseSize; // 最大响应大小限制

    public CustomHttpResponse(HttpURLConnection con) {
        this.con = con;
        this.maxResponseSize = Integer.MAX_VALUE; // 默认无大小限制
    }
    
    public CustomHttpResponse(HttpURLConnection con, int maxResponseSize) {
        this.con = con;
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
                InputStream inputStream = null;
                try {
                    // 根据响应状态码选择合适的输入流
                    int responseCode = con.getResponseCode();
                    if (responseCode >= 400) {
                        // 对于4xx和5xx错误，使用错误流
                        inputStream = con.getErrorStream();
                        if (inputStream == null) {
                            // 如果错误流为空，尝试使用普通输入流
                            inputStream = con.getInputStream();
                        }
                    } else {
                        // 对于2xx和3xx响应，使用普通输入流
                        inputStream = con.getInputStream();
                    }
                    
                    if (inputStream != null) {
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
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
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (Exception e) {
                // 对于网络异常等情况，返回空数组而不是抛出异常
                if (con != null) {
                    try {
                        System.err.println("读取响应数据失败，状态码: " + con.getResponseCode() + ", 错误: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception ex) {
                        System.err.println("读取响应数据失败: " + e.getMessage());
                    }
                }
                dataBuffer = new byte[0];
            }
        }
        return dataBuffer;
    }

    public void clearBuffer() {
        dataBuffer = null;
    }

    public String getTextStr() {
        List<String> charsetList = getHeaderField("Content-Type");
        String charsetStr = "UTF-8";
        if(charsetList!=null) {
            for (String data : charsetList) {
                data = data.trim();
                if (data.toLowerCase().contains("charset=")) {
                    charsetStr = data.substring(data.toLowerCase().indexOf("charset=") + 8);
                }
            }
        }

        Charset charset = null;
        if(!charsetStr.isEmpty()) charset = Charset.forName(charsetStr);

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

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    callback.onResponse(line);
                }
                // 回调处理每次响应
            }
            con.disconnect();
        } catch (Exception e) {
            handleException(e, callback);
        } finally {
            con.disconnect();
        }

    }

    public void disconnect() {
        if (con!= null) {
            clearBuffer();
            con.disconnect();
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

    public String saveToFile(String filePath,Boolean showSpeed){

        try (InputStream inputStream = con.getInputStream();
             FileOutputStream fos = new FileOutputStream(getSaveFile(filePath))) {

            File saveFile = getSaveFile(filePath);

            byte[] buffer = new byte[16 * 1024];
            int bytesRead = 0, len;
            long startTime = System.currentTimeMillis(), lastBytesRead = 0;
            int fileSize = con.getContentLength();

            // 该判断不要写while内，影响运行速率
            if (showSpeed) {

                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    bytesRead += len;

                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime;
                    if (elapsedTime >= 1000){
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

            }else {
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            return saveFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }


    public String saveToFileByGzip(String filePath, ProgressBar progressBar, Label progressLabel){

        try (InputStream inputStream = con.getInputStream();
             FileOutputStream fos = new FileOutputStream(getSaveGzipFile(filePath))) {

            File saveFile = getSaveGzipFile(filePath);

            byte[] buffer = new byte[16 * 1024];
            int bytesRead = 0, len;
            long startTime = System.currentTimeMillis(), lastBytesRead = 0;
            int fileSize = con.getContentLength();

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
            String absolutePath = gzipAbsolutePath.replace(".gzip","");

            GzipUtils.unGzipFile(gzipAbsolutePath, absolutePath, true);

            return absolutePath;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }


    public String saveToFileByGzip(String filePath,Boolean showSpeed){

        try (InputStream inputStream = con.getInputStream();
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
             FileOutputStream fos = new FileOutputStream(getSaveFile(filePath))) {

            File saveFile = getSaveFile(filePath);

            byte[] buffer = new byte[16 * 1024];
            int bytesRead = 0, len;
            long startTime = System.currentTimeMillis(), lastBytesRead = 0;
            int fileSize = con.getContentLength();

            // 该判断不要写while内，影响运行速率
            if (showSpeed) {

                while ((len = gzipInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    bytesRead += len;

                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime;
                    if (elapsedTime >= 1000){
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

            }else {
                while ((len = gzipInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            return saveFile.getAbsolutePath();

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
            saveFile = new File(saveFile, getFileName(con));
        }
        return saveFile;
    }

    private File getSaveGzipFile(String filePath) throws IOException {
        File saveFile = new File(filePath);
        if (saveFile.isDirectory()) {
            if (!saveFile.exists()) {
                saveFile.mkdirs();
            }
            String fileName = getFileName(con);
            if (fileName!="" && !fileName.endsWith(".gzip")) fileName = fileName + ".gzip";
            saveFile = new File(saveFile, fileName);
        }
        return saveFile;
    }

    // 获取文件名
    private String getFileName(HttpURLConnection connection) {
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
            // 从Content-Disposition中获取文件名
            Pattern pattern = Pattern.compile("filename\\s*=\\s*\"?([^\";]+)\"?");
            Matcher matcher = pattern.matcher(disposition);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } else {
            // 从URL中获取文件名
            String fileName = connection.getURL().toString();
            return fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.length());
        }
        return "";
    }


    public URL getURL() {
        return con.getURL();
    }

    public int getResponseCode() throws Exception {
        return con.getResponseCode();
    }

    public int getContentLength() {
        return con.getContentLength();
    }

    public long getExpiration() {
        return con.getExpiration();
    }

    public long getLastModified() {
        return con.getLastModified();
    }

    public String getContentType() {
        return con.getContentType();
    }

    public InputStream getInputStream() throws Exception {
        return con.getInputStream();
    }

    public byte[] getByteArray() {
        return getDataBuffer();
    }

    public String getContentEncoding() {
        return con.getContentEncoding();
    }

    public Map<String, List<String>> getHeaderFields() {
        return con.getHeaderFields();
    }
    public String getHeaderField(int n) {
        return con.getHeaderField(n);
    }
    public List<String> getHeaderField(String str) {
        return con.getHeaderFields().get(str);
    }
    public String getHeaderFields(int n) {
        return con.getHeaderFieldKey(n);
    }

    public String getResponseMessage() {
        try {
            return con.getResponseMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHeaderFieldsText() {
        StringBuilder sb = new StringBuilder();
        // 添加响应头
        Map<String, List<String>> headers = con.getHeaderFields();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                List<String> headerValues = entry.getValue();
                if (headerValues != null && !headerValues.isEmpty()) {
                    for (String value : headerValues) {
                        sb.append(headerName).append(": ").append(value).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    public String getAllResponseText() {
        StringBuilder sb = new StringBuilder();
        int statusCode = 0;
        String responseMessage = "";
        try {
            statusCode = con.getResponseCode();
            responseMessage = con.getResponseMessage();
        }catch (Exception e){};

        // 添加状态行
        sb.append("HTTP/1.1 ").append(statusCode).append(" ")
                .append(responseMessage).append("\n");

        // 添加响应头
        Map<String, List<String>> headers = con.getHeaderFields();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                List<String> headerValues = entry.getValue();
                if (headerValues != null && !headerValues.isEmpty()) {
                    for (String value : headerValues) {
                        sb.append(headerName).append(": ").append(value).append("\n");
                    }
                }
            }
        }

        // 添加空行和响应体
        sb.append("\n").append(getTextStr());

        return sb.toString();
    }
}