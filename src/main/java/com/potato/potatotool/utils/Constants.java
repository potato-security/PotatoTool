package com.potato.potatotool.utils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.*;

import java.util.Map;
import java.util.Properties;

import com.potato.potatotool.utils.jsonUtils.OrderedJSONObject;
import org.json.JSONObject;

/**
 * @author Potato
 * @date 2023/4/25 11:56
 */


public class Constants {

    // 初始化config.properties, 全局只会调用一次
    private static final Properties props;
    static {
         props = new Properties();
        try {
            InputStream in = Constants.class.getClassLoader().getResourceAsStream("config.properties");
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的值
     */
    public static String getConfigInfo(String propertyName) {
        return props.getProperty(propertyName);
    }


    // jar无法读取绝对路径、只能读取stream/释放临时文件
    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的resources临时路径
     */
    public static String getResourceFileTmpPath(String propertyName) {

        String resourceFilePath = props.getProperty(propertyName);

        if (resourceFilePath == null) {
            resourceFilePath = propertyName;
            propertyName = "img";
        }

        InputStream inputStream = Constants.class.getClassLoader().getResourceAsStream(resourceFilePath);
        if (inputStream == null) {
            throw new RuntimeException("资源文件不存在或为空资源，请检查config.properties, propertyName=" + propertyName);
        }
        try {
            File tempFile = File.createTempFile(propertyName, null);    //propertyName+时间戳随机数.tmp
            tempFile.deleteOnExit();

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                int read;
                byte[] buffer = new byte[1024];
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            inputStream.close();
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("临时资源创建错误 " + resourceFilePath, e);
        }
    }


    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的resourcesStream
     */
    public static InputStream getResourceStream(String propertyName) {

        String resourceFilePath = props.getProperty(propertyName);

        InputStream inputStream = Constants.class.getClassLoader().getResourceAsStream(resourceFilePath);
        if (inputStream == null) {
            throw new RuntimeException("资源文件不存在或为空资源，请检查config.properties, propertyName=" + propertyName);
        }

        return inputStream;
    }


    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的resourcesStreamByte
     */
    public static byte[] getResourceStreamByte(String propertyName) {

        String resourceFilePath = props.getProperty(propertyName);

        InputStream inputStream = Constants.class.getClassLoader().getResourceAsStream(resourceFilePath);
        if (inputStream == null) {
            throw new RuntimeException("资源文件不存在或为空资源，请检查config.properties, propertyName=" + propertyName);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        try{
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        }catch (Exception e){
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }


    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的resourcesString
     */
    public static String getResourceString(String propertyName) {
        String fileContent="";

        String resourceFilePath = props.getProperty(propertyName);

        try {
            InputStream inputStream = Constants.class.getClassLoader().getResourceAsStream(resourceFilePath);

            if (inputStream == null) {
                throw new RuntimeException("资源文件不存在或为空资源，请检查config.properties, propertyName=" + propertyName);
            }

            // 使用缓冲区读取文件数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator()); // 添加换行符
            }
            reader.close();

            // 获取文件内容字符串
            fileContent = stringBuilder.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return fileContent;
    }


    /**
     * @param propertyName  config.properties内设置好的key名称
     * @return              返回key对应的resources绝对路径    //打包jar不适用
     */
    public static String getResourceFilePath(String propertyName) {

        String resourceFilePath = props.getProperty(propertyName);

        URL resourceUrl = Constants.class.getClassLoader().getResource(resourceFilePath);
        if (resourceUrl != null) {
            try {
                String decodedResourceFilePath = URLDecoder.decode(resourceUrl.getPath(), "UTF-8");
                if (decodedResourceFilePath.contains(":")) {
                    // Windows 系统下需要去掉前面的斜杠
                    decodedResourceFilePath = decodedResourceFilePath.substring(1);
                }
                return decodedResourceFilePath;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("资源文件不存在，请检查config.properties, propertyName=" + propertyName);
        }
        return null;
    }


    /**
     * @param directoryPath 目录路径
     * @return              返回递归后的所有目录文件路径
     */
    public static ArrayList<String> listFiles(String directoryPath) {
        ArrayList<String> filePaths = new ArrayList<String>();

        File directory = new File(directoryPath);

        File[] files = directory.listFiles();
        Arrays.sort(files);

        for (File file : files) {
            if (file.isFile()) {
                filePaths.add(file.getAbsolutePath());
            } else if (file.isDirectory()) {
                ArrayList<String> subDirectoryFiles = listFiles(file.getAbsolutePath());
                filePaths.addAll(subDirectoryFiles);
            }
        }
        return filePaths;
    }

    /**
     * 一下为针对外部config配置文件
     */
    private static final String CONFIG_FOLDER = ".PotatoTool";
    private static final String CONFIG_FILE = "config.json";
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static void saveConfig(String key, Object value) {

        try {
            Path configFolder = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
            Files.createDirectories(configFolder);

            Path configFile = configFolder.resolve(CONFIG_FILE);

            JsonObject config = new JsonObject();
            if (Files.exists(configFile)) {
                // 读取已有配置
                String content = new String(Files.readAllBytes(configFile));
                config = gson.fromJson(content, JsonObject.class);
            }

            // 更新配置
            config.add(key, gson.toJsonTree(value));

            // 写入更新后的配置
            String json = gson.toJson(config);
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("配置已保存");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 兼容批量修改
     */
    public static void saveConfig(Map<String, Object> configMap) {

        try {
            Path configFolder = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
            Files.createDirectories(configFolder);

            Path configFile = configFolder.resolve(CONFIG_FILE);

            JsonObject config = new JsonObject();
            if (Files.exists(configFile)) {
                // 读取已有配置
                String content = new String(Files.readAllBytes(configFile));
                config = gson.fromJson(content, JsonObject.class);
            }

            // 更新配置
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                config.add(key, gson.toJsonTree(value));
            }

            // 写入更新后的配置
            String json = gson.toJson(config);
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("配置已保存");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object getOutsideConfig(String key) {
        try {
            Path configFile = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER, CONFIG_FILE);

            if (Files.exists(configFile)) {
                String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                JsonObject jsonObject = (new Gson()).fromJson(content, JsonObject.class);
                return jsonObject.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取配置时出错!");
        }
        return null;
    }

    public static JsonElement findKey(JsonElement element, String searchKey) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(searchKey)) {
                return obj.get(searchKey);
            }

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement value = findKey(entry.getValue(), searchKey);
                if (value != null) {
                    return value;
                }
            }

        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement elem : array) {
                JsonElement value = findKey(elem, searchKey);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

}
