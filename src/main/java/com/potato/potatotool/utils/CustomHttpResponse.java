package com.potato.potatotool.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2023/4/13 14:32
 */


/**
 * README：
 *          con.getTextStr() \ con.getJson() \ con.saveToFile(savePath)
 * 【为保证最高运行效率】，以上三种结果输出不能同时出现，否则会报错java.io.IOException: stream is closed
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

        try {

            StringBuilder responseString = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseString.append(inputLine);
            }
            in.close();

            return responseString.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public void getSSEStreamingJson(ResponseCallback callback) { // 获取服务器发送事件(SSE)流式响应json

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {

                callback.onResponse(line);
                // 回调处理每次响应
            }
            reader.close();
            con.disconnect();

        } catch (Exception e) {
            if(e.toString().contains("Premature EOF")) {
                callback.onResponse("[[Premature EOF]]");
            }else if(e.toString().contains("Server returned HTTP response code: 502")){
                callback.onResponse("[[Response code 502]]");
            }else if(e.toString().contains("Read timed out")){
                callback.onResponse("[[Read timed out]]");
            }
            e.printStackTrace();
        }

    }
    interface ResponseCallback {
        void onResponse(String line);
    }


    public JsonObject getJson() {

        JsonObject result = null;
        try{
            String testStr = getTextStr();
            if(testStr != null && testStr != ""){
                result = (new Gson()).fromJson(testStr, JsonObject.class);;
            }else {
                result = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String saveToFile(String filePath,Boolean showSpeed){

        try{

            InputStream inputStream = con.getInputStream();

            // 文件保存位置
            File saveFile = new File(filePath);
            if (saveFile.isDirectory()) { // 传入文件夹
                if (!saveFile.exists()) {
                    saveFile.mkdir();
                    saveFile = new File(saveFile + File.separator + getFileName(con));
                }
            }
            FileOutputStream fos = new FileOutputStream(saveFile);

            int fileSize = getContentLength();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            int len;

            long startTime = System.currentTimeMillis();
            long lastBytesRead = 0;

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
                        long remainingTime = (fileSize - currentBytesRead) / bytesPerSecond;

                        int progress = (int) ((bytesRead / (float) fileSize) * 100);
                        System.out.printf("下载进度: %d%%, 速度: %d KB/s, 预估时间: %d s\n",
                                progress, bytesPerSecond / 1024, remainingTime);

                        lastBytesRead = currentBytesRead;
                        startTime = currentTime;
                    }
                }

            }else {

                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }

            }

            if (fos != null) {
                fos.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }

            return saveFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    // 获取文件名
    private String getFileName(HttpURLConnection connection) {
        String fileName = "";
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
            // 从Content-Disposition中获取文件名
            int index = disposition.indexOf("filename=");
            if (index > 0) {
                fileName = disposition.substring(index + 10, disposition.length() - 1);
            }
        } else {
            // 从URL中获取文件名
            fileName = connection.getURL().toString();
            fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.length());
        }
        return fileName;
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