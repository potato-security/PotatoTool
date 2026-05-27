package com.potato.potatotool.utils.core;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.google.gson.*;
import com.potato.potatotool.content.classObj.ConfigConstants;

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


    public static String getResourceUrl(String path){
        return Constants.class.getResource(path).toExternalForm();
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
    private static final Object CONFIG_LOCK = new Object();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static boolean saveConfig(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        synchronized (CONFIG_LOCK) {
            try {
                JsonObject config = loadConfigForWrite();
                config.add(key, toJsonElement(value));
                return writeConfig(config);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 兼容批量修改
     */
    public static boolean saveConfig(Map<String, Object> configMap) {
        synchronized (CONFIG_LOCK) {
            try {
                JsonObject config = loadConfigForWrite();
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    config.add(entry.getKey(), toJsonElement(entry.getValue()));
                }
                return writeConfig(config);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static boolean saveConfig(JsonObject configJsonObj) {
        synchronized (CONFIG_LOCK) {
            try {
                return writeConfig(configJsonObj == null ? new JsonObject() : configJsonObj.deepCopy());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static boolean saveConfig(JsonElement configJsonElement) {
        if (configJsonElement == null || !configJsonElement.isJsonObject()) {
            return false;
        }
        return saveConfig(configJsonElement.getAsJsonObject());
    }

    public static boolean saveConfig(Map<String, Object> configMap, String topKey) {
        if (topKey == null || topKey.trim().isEmpty()) {
            return false;
        }

        synchronized (CONFIG_LOCK) {
            try {
                JsonObject config = loadConfigForWrite();
                JsonObject topLevelObject = config.has(topKey) && config.get(topKey).isJsonObject()
                        ? config.getAsJsonObject(topKey).deepCopy()
                        : new JsonObject();
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    topLevelObject.add(entry.getKey(), toJsonElement(entry.getValue()));
                }
                config.add(topKey, topLevelObject);
                return writeConfig(config);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    
    /**
     * 合并保存资源配置到 UpDate.resources 节点（不覆盖其他资源）
     * 
     * 使用场景：更新单个或多个资源配置时，自动合并现有资源，避免覆盖
     * 
     * @param resourcesMap 要更新的资源配置Map，key为资源名称（如"winKbInfo"），value为资源配置信息
     * @throws IllegalArgumentException 如果 resourcesMap 为空
     * 
     * @example
     * <pre>
     * // 更新单个资源
     * Map<String, Object> resourceInfo = new LinkedHashMap<>();
     * resourceInfo.put("version", "20250101");
     * resourceInfo.put("fileName", "winKbInfo20250101.csv");
     * 
     * Map<String, Object> resourcesMap = new LinkedHashMap<>();
     * resourcesMap.put("winKbInfo", resourceInfo);
     * 
     * Constants.saveResourceConfigMerge(resourcesMap);
     * </pre>
     */
    public static boolean saveResourceConfigMerge(Map<String, Object> resourcesMap) {
        if (resourcesMap == null || resourcesMap.isEmpty()) {
            throw new IllegalArgumentException("resourcesMap 不能为空");
        }
        
        try {
            // 1. 读取现有配置
            JsonObject config = (JsonObject) getOutsideConfig(ConfigConstants.UPDATE);
            JsonObject existingResources = null;
            
            if (config != null && config.has(ConfigConstants.UPDATE_RESOURCES)) {
                existingResources = config.getAsJsonObject(ConfigConstants.UPDATE_RESOURCES);
            } else {
                existingResources = new JsonObject();
            }
            
            // 2. 使用 JsonObject 直接构建合并结果，避免类型混合问题
            JsonObject mergedResources = new JsonObject();
            
            // 先添加现有的所有资源（保留未更新的资源）
            for (String key : existingResources.keySet()) {
                JsonElement element = existingResources.get(key);
                mergedResources.add(key, element == null ? JsonNull.INSTANCE : element.deepCopy());
            }
            
            // 再添加或更新新资源（覆盖同名资源）
            // 统一转换为 JsonElement，确保类型一致性
            for (Map.Entry<String, Object> entry : resourcesMap.entrySet()) {
                mergedResources.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
            }
            
            // 3. 保存合并后的配置
            Map<String, Object> saveMap = new LinkedHashMap<>();
            saveMap.put(ConfigConstants.UPDATE_RESOURCES, mergedResources);
            return saveConfig(saveMap, ConfigConstants.UPDATE);
            
        } catch (Exception e) {
            System.err.println("合并保存资源配置失败: " + e.getMessage());
            e.printStackTrace();
            return false;
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
     * 规则：以 newObj（JAR内资源文件）的字段顺序为准，但优先使用 localObj（本地配置）的值
     */
    public static JsonObject mergeJsonObjects(JsonObject localObj, JsonObject newObj) {
        JsonObject merged = new JsonObject();

        // 按照新配置（JAR内）的字段顺序遍历
        for (Map.Entry<String, JsonElement> entry : newObj.entrySet()) {
            String key = entry.getKey();
            JsonElement newValue = entry.getValue();

            // 如果本地配置有这个字段，递归合并（保留本地值）
            if (localObj.has(key)) {
                JsonElement localValue = localObj.get(key);
                merged.add(key, mergeJsonElements(localValue, newValue));
            } else {
                // 本地配置没有这个字段，使用新配置的值（新增字段）
                merged.add(key, newValue);
            }
        }
        
        // 保留本地配置中存在但新配置中不存在的字段（向后兼容旧配置）
        for (Map.Entry<String, JsonElement> entry : localObj.entrySet()) {
            String key = entry.getKey();
            if (!newObj.has(key)) {
                merged.add(key, entry.getValue());
            }
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

    public static volatile JsonObject cachedConfig = null;

    public static Object getOutsideConfig(String key) {
        synchronized (CONFIG_LOCK) {
            try {
                Path configFile = getOutsideConfigFile();
                if (!Files.exists(configFile)) {
                    return null;
                }
                if (cachedConfig == null) {
                    cachedConfig = readConfig(configFile);
                }
                JsonElement value = key == null ? cachedConfig : cachedConfig.get(key);
                return value == null ? null : value.deepCopy();
            } catch (Exception e) {
                if(debugMode) e.printStackTrace();
                if (debugMode) {
                    System.out.println("读取配置时出错!");
                }
                return null;
            }
        }
    }

    private static Path getOutsideConfigFile() {
        return Paths.get(System.getProperty("user.home"), CONFIG_FOLDER, CONFIG_FILE);
    }

    private static JsonObject loadConfigForWrite() throws IOException {
        Path configFile = getOutsideConfigFile();
        if (Files.exists(configFile)) {
            return readConfig(configFile);
        }
        return new JsonObject();
    }

    private static JsonObject readConfig(Path configFile) throws IOException {
        String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return new JsonObject();
        }

        JsonElement element = JsonParser.parseString(content);
        if (!element.isJsonObject()) {
            throw new JsonParseException("配置文件根节点不是对象: " + configFile);
        }
        return element.getAsJsonObject();
    }

    private static JsonElement toJsonElement(Object value) {
        if (value instanceof JsonElement) {
            return ((JsonElement) value).deepCopy();
        }
        return gson.toJsonTree(value);
    }

    private static boolean writeConfig(JsonObject config) throws IOException {
        Path configFile = getOutsideConfigFile();
        Path configFolder = configFile.getParent();
        Files.createDirectories(configFolder);

        Path tempFile = Files.createTempFile(configFolder, "config", ".tmp");
        try {
            byte[] json = gson.toJson(config).getBytes(StandardCharsets.UTF_8);
            Files.write(tempFile, json);
            try {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
            cachedConfig = config.deepCopy();
            return true;
        } finally {
            Files.deleteIfExists(tempFile);
        }
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

    /**
     * 从JAR资源复制文件到目标路径
     * 会检查文件是否存在以及完整性（通过大小判断）
     * 
     * @param resourceName 资源名称（config.properties中的key）
     * @param targetPath 目标路径
     * @param expectedSize 期望的文件大小（字节），用于判断文件是否完整
     * @throws IOException 复制失败
     */
    public static void copyResourceToFile(String resourceName, Path targetPath, long expectedSize) throws IOException {
        // 检查文件是否存在且完整
        if (!Files.exists(targetPath)) {
            System.out.println("文件不存在，准备释放: " + targetPath.getFileName());
        } else {
            long currentSize = Files.size(targetPath);
            if (currentSize < expectedSize) {
                System.out.println("文件不完整 (当前: " + currentSize + " 字节, 期望: " + expectedSize + " 字节)，重新释放: " + targetPath.getFileName());
            } else {
                // 文件完整，跳过（不输出，减少日志）
                return;
            }
        }
        
        // 确保目标目录存在
        Files.createDirectories(targetPath.getParent());
        
        // 从JAR资源复制文件
        try (InputStream inputStream = getResourceStream(resourceName);
             FileOutputStream outputStream = new FileOutputStream(targetPath.toString())) {
            
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            long totalWritten = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;
            }
            
            System.out.println("文件释放成功: " + targetPath.getFileName() + " (" + totalWritten + " 字节)");
            
            // 验证文件大小
            long actualSize = Files.size(targetPath);
            if (actualSize < expectedSize * 0.95) {  // 允许5%误差
                System.err.println("警告: 释放的文件大小异常 (实际: " + actualSize + ", 期望: " + expectedSize + ")");
            }
            
        } catch (Exception e) {
            System.err.println("释放文件失败: " + targetPath.getFileName());
            e.printStackTrace();
            throw new IOException("释放资源文件失败: " + resourceName, e);
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
