package com.potato.potatotool.utils;

import java.io.*;
import java.lang.annotation.ElementType;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.*;

import static com.potato.potatotool.ToStart.debugMode;


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
                byte[] buffer = new byte[16 * 1024];
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


    public static ArrayList<String> getResourceList(String propertyName) {
        ArrayList<String> list = new ArrayList();

        String resourceFilePath = props.getProperty(propertyName);

        try {
            InputStream inputStream = Constants.class.getClassLoader().getResourceAsStream(resourceFilePath);

            if (inputStream == null) {
                throw new RuntimeException("资源文件不存在或为空资源，请检查config.properties, propertyName=" + propertyName);
            }

            // 使用缓冲区读取文件数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line.trim());
            }
            reader.close();

        }catch (IOException e) {
            e.printStackTrace();
        }

        return list;
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
                String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                config = gson.fromJson(content, JsonObject.class);
            }

            // 更新配置
            config.add(key, gson.toJsonTree(value));

            // 写入更新后的配置
            String json = gson.toJson(config);
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("配置已保存");
            cachedConfig = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 兼容批量修改
     */
    public static boolean saveConfig(Map<String, Object> configMap) {
        try {
            Path configFolder = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
            Files.createDirectories(configFolder);

            Path configFile = configFolder.resolve(CONFIG_FILE);

            JsonObject config = new JsonObject();
            if (Files.exists(configFile)) {
                // 读取已有配置
                String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
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
            cachedConfig = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void saveConfig(JsonObject configJsonObj) {
        try {
            Path configFolder = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
            Files.createDirectories(configFolder);
            Path configFile = configFolder.resolve(CONFIG_FILE);

            // 写入更新后的配置
            String json = gson.toJson(configJsonObj);
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("配置已保存");
            cachedConfig = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void saveConfig(JsonElement configJsonElement) {
        saveConfig(configJsonElement.getAsJsonObject());
    }
    public static void saveConfig(Map<String, Object> configMap, String topKey) {

        try {
            Path configFolder = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER);
            Files.createDirectories(configFolder);

            Path configFile = configFolder.resolve(CONFIG_FILE);

            JsonObject config = new JsonObject();
            if (Files.exists(configFile)) {
                // 读取已有配置
                String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                config = gson.fromJson(content, JsonObject.class);
            }

            // 获取或创建第一层对象
            JsonObject topLevelObject = config.has(topKey)
                    ? config.getAsJsonObject(topKey)
                    : new JsonObject();
            // 更新第二层key的内容
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                topLevelObject.add(key, gson.toJsonTree(value));
            }

            // 将更新后的对象放回到第一层
            config.add(topKey, topLevelObject);

            // 写入更新后的配置
            String json = gson.toJson(config);
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));

            System.out.println("配置已保存");
            cachedConfig = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取JsonElement的类型
     */
    private static ElementType getJsonElementType(JsonElement element) {
        if (element.isJsonObject()) return ElementType.OBJECT;
        if (element.isJsonArray()) return ElementType.ARRAY;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) return ElementType.STRING;
            if (primitive.isNumber()) return ElementType.NUMBER;
            if (primitive.isBoolean()) return ElementType.BOOLEAN;
        }
        return ElementType.NULL;
    }
    private enum ElementType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    /**
     * 递归合并两个JsonElement  本地文件Json和资源文件Json
     * 规则：
     * 1. 保留本地配置中的现有值
     * 2. 添加新配置中新增的字段
     * 3. 对于对象，递归合并
     * @return
     */
    public static JsonElement mergeJsonElements(JsonElement localElement, JsonElement newElement){
        // 如果本地配置为 null 或 不存在 或 不同类型，使用资源中的新配置
        if (localElement == null || localElement.isJsonNull() || getJsonElementType(localElement) != getJsonElementType(newElement)) {
            return newElement;
        }

        // 根据类型分别处理
        if (localElement.isJsonObject()) {
            return mergeJsonObjects(localElement.getAsJsonObject(), newElement.getAsJsonObject());
        } else if (localElement.isJsonArray()) {
            return mergeJsonArrays(localElement.getAsJsonArray(), newElement.getAsJsonArray());
        } else {
            // 基本类型（字符串、数字、布尔值），保留本地值
            return localElement;
        }
    }

    /**
     * 递归合并两个JsonObject
     */
    public static JsonObject mergeJsonObjects(JsonObject localObj, JsonObject newObj) {
        JsonObject merged = new JsonObject();

        // 首先复制所有本地配置
        for (Map.Entry<String, JsonElement> entry : localObj.entrySet()) {
            merged.add(entry.getKey(), entry.getValue());
        }

        // 遍历新配置中的所有字段
        for (Map.Entry<String, JsonElement> entry : newObj.entrySet()) {
            String key = entry.getKey();
            JsonElement newValue = entry.getValue();

            // 如果本地配置没有这个字段，直接添加
            if (!localObj.has(key)) {
                merged.add(key, newValue);
                continue;
            }

            // 递归合并现有字段
            JsonElement localValue = localObj.get(key);
            merged.add(key, mergeJsonElements(localValue, newValue));
        }

        return merged;
    }

    /**
     * 合并两个JsonArray
     * 根据数组元素的类型采用不同的合并策略
     */
    private static JsonArray mergeJsonArrays(JsonArray localArr, JsonArray newArr) {
        // 如果本地数组为空，使用新数组
        if (localArr.size() == 0) {
            return newArr;
        }

        // 如果数组内容是对象，尝试智能合并
        if (isArrayOfObjects(localArr) && isArrayOfObjects(newArr)) {
            return mergeArraysOfObjects(localArr, newArr);
        }

        // 如果是简单类型的数组，保留本地值，但添加新的不重复项
        return mergeSimpleArrays(localArr, newArr);
    }

    /**
     * 检查是否是对象数组
     */
    private static boolean isArrayOfObjects(JsonArray arr) {
        return arr.size() > 0 && arr.get(0).isJsonObject();
    }

    /**
     * 合并对象数组
     * 如果对象有唯一标识符（如id、title等），根据标识符合并
     */
    private static JsonArray mergeArraysOfObjects(JsonArray localArr, JsonArray newArr) {
        JsonArray merged = new JsonArray();
        Map<String, JsonObject> objectMap = new HashMap<>();

        // 尝试找到可能的标识符字段
        String idField = findIdentifierField(localArr.get(0).getAsJsonObject());

        // 如果没有标识符字段，返回本地数组
        if (idField == null) {
            return localArr;
        }

        // 将本地数组中的对象放入Map
        for (JsonElement element : localArr) {
            JsonObject obj = element.getAsJsonObject();
            String id = obj.get(idField).getAsString();
            objectMap.put(id, obj);
        }

        // 合并新数组中的对象
        for (JsonElement element : newArr) {
            JsonObject newObj = element.getAsJsonObject();
            String id = newObj.get(idField).getAsString();

            if (objectMap.containsKey(id)) {
                // 如果对象已存在，递归合并
                JsonObject localObj = objectMap.get(id);
                objectMap.put(id, mergeJsonObjects(localObj, newObj));
            } else {
                // 如果是新对象，直接添加
                objectMap.put(id, newObj);
            }
        }

        // 构建结果数组
        for (JsonObject obj : objectMap.values()) {
            merged.add(obj);
        }

        return merged;
    }

    /**
     * 查找可能的标识符字段
     */
    private static String findIdentifierField(JsonObject obj) {
        String[] possibleIds = {"id", "title", "name", "key"};
        for (String field : possibleIds) {
            if (obj.has(field)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 合并简单类型数组（字符串、数字等）
     * 保留本地值，添加新的不重复项
     */
    private static JsonArray mergeSimpleArrays(JsonArray localArr, JsonArray newArr) {
        JsonArray merged = new JsonArray();
        Set<String> values = new HashSet<>();

        // 添加本地数组的所有元素
        for (JsonElement element : localArr) {
            merged.add(element);
            values.add(element.toString());
        }

        // 添加新数组中不重复的元素
        for (JsonElement element : newArr) {
            if (!values.contains(element.toString())) {
                merged.add(element);
                values.add(element.toString());
            }
        }

        return merged;
    }

    public static JsonObject cachedConfig = null;
    public static Object getOutsideConfig(String key) {
        try {
            Path configFile = Paths.get(System.getProperty("user.home"), CONFIG_FOLDER, CONFIG_FILE);

            if (Files.exists(configFile)) {
                if(cachedConfig==null) {
                    String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                    cachedConfig = (new Gson()).fromJson(content, JsonObject.class);
                }
                if(key==null) return cachedConfig;
                return cachedConfig.get(key);
            }
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
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

    public static void copyResourceToFile(String resourceName, Path targetPath, long expectedSize) throws IOException {
        if (!Files.exists(targetPath) || Files.size(targetPath) < expectedSize) {
            try (InputStream inputStream = getResourceStream(resourceName);
                 FileOutputStream outputStream = new FileOutputStream(targetPath.toString())) {
                byte[] buffer = new byte[16 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // 判断configFolder路径下是否存在文件以prefix开头
    public static boolean hasFileWithPrefix(Path configFolder, String prefix) {
        try {
            return Files.list(configFolder)
                    .filter(path -> Files.isRegularFile(path))
                    .anyMatch(path -> path.getFileName().toString().startsWith(prefix));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
