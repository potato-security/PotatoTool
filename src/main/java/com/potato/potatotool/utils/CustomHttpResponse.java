package com.potato.potatotool.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @author Potato
 * @date 2023/4/13 14:32
 */


/**
 * README：
 *          con.getTextStr() \ getDocument() \ con.getJson() \ con.saveToFile(savePath)
 * 【为保证最高运行效率】，以上四种结果输出不能同时出现，否则会报错java.io.IOException: stream is closed
 *
 * Tips:
 *              若强行支持，请读取响应结果存储 private byte[] textBuffer复用;
 *
 */
public class CustomHttpResponse{

    private HttpURLConnection con;

    public CustomHttpResponse(HttpURLConnection con) {
        this.con = con;
    }

    public String getTextStr() { // 存在getText方法

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder responseString = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseString.append(inputLine);
            }
            return responseString.toString();
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
    interface ResponseCallback {
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

    public byte[] getByteArray() throws Exception {
        try (InputStream inputStream = con.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[16 * 1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
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

}