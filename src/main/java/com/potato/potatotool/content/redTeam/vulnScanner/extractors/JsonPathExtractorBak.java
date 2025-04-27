package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON路径提取工具类
 * 支持各种复杂的JSON路径表达式
 * @author Potato
 * @date 2025/3/21 15:00
 */
public class JsonPathExtractorBak {

    private JsonPathExtractorBak() {
        // 私有构造函数，防止实例化
    }

    /**
     * 从JSON内容中提取值
     * @param content JSON字符串内容
     * @param paths JSON路径列表
     * @return 提取的内容
     */
    public static String extractJson(String content, List<String> paths) {
        if (content == null || paths == null || paths.isEmpty()) {
            return null;
        }

        try {
            // 尝试解析JSON内容
            String jsonContent = content.trim();
            if (!jsonContent.startsWith("{") && !jsonContent.startsWith("[")) {
                return null;
            }
            
            // 使用GSON解析JSON
            JsonElement jsonElement;
            try {
                jsonElement = JsonParser.parseString(jsonContent);
            } catch (Exception e) {
                return null;
            }
            
            // 尝试每个路径
            for (String path : paths) {
                try {
                    // 处理特殊的JSON路径表达式
                    if (path.contains("[") || path.contains("|") || path.contains("select") || 
                        path.contains("..") || path.contains("empty") || path.contains("//")) {
                        
                        String result = processComplexJsonPath(jsonElement, path);
                        if (result != null) {
                            return result;
                        }
                    } else {
                        // 简单路径处理
                        String normalizedPath = path;
                        if (normalizedPath.startsWith("$")) {
                            normalizedPath = normalizedPath.substring(1);
                        }
                        if (normalizedPath.startsWith(".")) {
                            normalizedPath = normalizedPath.substring(1);
                        }
                        
                        String[] segments = normalizedPath.split("\\.");
                        Object result = traverseJsonElement(jsonElement, segments, 0);
                        if (result != null) {
                            return result.toString();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    /**
     * 处理复杂JSON路径表达式
     * @param jsonElement JSON元素
     * @param path 复杂路径
     * @return 提取的值
     */
    public static String processComplexJsonPath(JsonElement jsonElement, String path) {
        try {
            // 处理数组索引访问 - 如 .data[0].field
            if (path.contains("[") && path.contains("]")) {
                return processArrayIndexPath(jsonElement, path);
            }
            
            // 处理管道操作符 - 如 .data | .[].name
            if (path.contains("|")) {
                return processPipePath(jsonElement, path);
            }
            
            // 处理递归下降操作符 - 如 ..|objects|.nodeId
            if (path.contains("..")) {
                return processRecursivePath(jsonElement, path);
            }
            
            // 处理过滤表达式 - 如 .entities[] | select(.roles[] | contains("registrant"))
            if (path.contains("select")) {
                return processSelectPath(jsonElement, path);
            }
            
            // 处理空值处理 - 如 .nodeId//empty[0]
            if (path.contains("//empty")) {
                return processEmptyFallbackPath(jsonElement, path);
            }
            
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 处理带数组索引的JSON路径
     * @param jsonElement JSON元素
     * @param path 路径表达式
     * @return 提取的值
     */
    public static String processArrayIndexPath(JsonElement jsonElement, String path) {
        try {
            // 移除$前缀
            String normalizedPath = path.startsWith("$") ? path.substring(1) : path;
            if (normalizedPath.startsWith(".")) {
                normalizedPath = normalizedPath.substring(1);
            }
            
            // 分析路径
            Pattern pattern = Pattern.compile("([^\\[\\]]+)\\[(\\d+)\\]");
            Matcher matcher = pattern.matcher(normalizedPath);
            
            JsonElement currentElement = jsonElement;
            String segments = normalizedPath;
            
            while (segments != null && !segments.isEmpty()) {
                matcher = pattern.matcher(segments);
                if (matcher.find()) {
                    // 处理数组部分前的路径
                    String beforeArray = matcher.group(1);
                    if (!beforeArray.isEmpty()) {
                        if (beforeArray.startsWith(".")) {
                            beforeArray = beforeArray.substring(1);
                        }
                        if (!beforeArray.isEmpty()) {
                            String[] parts = beforeArray.split("\\.");
                            for (String part : parts) {
                                if (!part.isEmpty()) {
                                    if (currentElement.isJsonObject()) {
                                        currentElement = currentElement.getAsJsonObject().get(part);
                                        if (currentElement == null) {
                                            return null;
                                        }
                                    } else {
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                    
                    // 处理数组索引
                    int index = Integer.parseInt(matcher.group(2));
                    if (currentElement.isJsonArray()) {
                        JsonArray array = currentElement.getAsJsonArray();
                        if (index < array.size()) {
                            currentElement = array.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                    
                    // 继续处理剩余部分
                    segments = segments.substring(matcher.end());
                    if (segments.startsWith(".")) {
                        segments = segments.substring(1);
                    }
                } else {
                    // 处理剩余的非数组部分
                    if (!segments.isEmpty()) {
                        String[] parts = segments.split("\\.");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                if (currentElement.isJsonObject()) {
                                    currentElement = currentElement.getAsJsonObject().get(part);
                                    if (currentElement == null) {
                                        return null;
                                    }
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                    break;
                }
            }
            
            // 返回最终结果
            if (currentElement != null) {
                if (currentElement.isJsonPrimitive()) {
                    return currentElement.getAsString();
                } else {
                    return currentElement.toString();
                }
            }
            
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 处理管道操作符的JSON路径
     * @param jsonElement JSON元素
     * @param path 路径表达式
     * @return 提取的值
     */
    public static String processPipePath(JsonElement jsonElement, String path) {
        try {
            // 移除$前缀
            String normalizedPath = path.startsWith("$") ? path.substring(1) : path;
            
            // 分割管道表达式
            String[] pipeParts = normalizedPath.split("\\|");
            JsonElement currentElement = jsonElement;
            
            for (String part : pipeParts) {
                part = part.trim();
                if (part.isEmpty()) continue;
                
                // 处理数组展开表达式 - 如 .[]
                if (part.equals(".[]") || part.endsWith("[]")) {
                    if (currentElement.isJsonArray()) {
                        JsonArray array = currentElement.getAsJsonArray();
                        if (!array.isEmpty()) {
                            // 简单起见，我们取第一个元素
                            currentElement = array.get(0);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
                // 处理普通路径
                else {
                    if (part.startsWith(".")) {
                        part = part.substring(1);
                    }
                    if (part.contains("[") && part.contains("]")) {
                        // 包含数组索引的路径
                        String tempResult = processArrayIndexPath(currentElement, "." + part);
                        if (tempResult != null) {
                            try {
                                // 尝试将结果解析为JSON
                                currentElement = JsonParser.parseString(tempResult);
                            } catch (Exception e) {
                                // 如果不是有效的JSON，则创建一个包含字符串的JsonPrimitive
                                return tempResult;
                            }
                        } else {
                            return null;
                        }
                    } else {
                        // 普通的点分路径
                        String[] segments = part.split("\\.");
                        for (String segment : segments) {
                            if (!segment.isEmpty()) {
                                if (currentElement.isJsonObject()) {
                                    currentElement = currentElement.getAsJsonObject().get(segment);
                                    if (currentElement == null) {
                                        return null;
                                    }
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
            
            // 返回最终结果
            if (currentElement.isJsonPrimitive()) {
                return currentElement.getAsString();
            } else {
                return currentElement.toString();
            }
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 处理递归表达式的JSON路径
     * @param jsonElement JSON元素
     * @param path 路径表达式
     * @return 提取的值
     */
    public static String processRecursivePath(JsonElement jsonElement, String path) {
        try {
            // 移除$前缀
            String normalizedPath = path.startsWith("$") ? path.substring(1) : path;
            
            // 分解递归表达式
            String[] parts = normalizedPath.split("\\.\\.");
            if (parts.length < 2) {
                return null;
            }
            
            // 处理第一部分
            JsonElement currentElement = jsonElement;
            if (parts[0].length() > 0 && !parts[0].equals("$")) {
                String[] segments = parts[0].substring(parts[0].startsWith(".") ? 1 : 0).split("\\.");
                for (String segment : segments) {
                    if (!segment.isEmpty()) {
                        if (currentElement.isJsonObject()) {
                            currentElement = currentElement.getAsJsonObject().get(segment);
                            if (currentElement == null) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                }
            }
            
            // 递归搜索第二部分指定的属性
            String searchProperty = parts[1];
            if (searchProperty.startsWith("|")) {
                searchProperty = searchProperty.substring(1).trim();
            }
            if (searchProperty.startsWith(".")) {
                searchProperty = searchProperty.substring(1);
            }
            
            List<String> results = new ArrayList<>();
            findPropertyRecursively(currentElement, searchProperty, results);
            
            if (!results.isEmpty()) {
                return results.get(0);
            }
            
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 递归查找属性
     * @param jsonElement JSON元素
     * @param propertyPath 属性路径
     * @param results 结果列表
     */
    public static void findPropertyRecursively(JsonElement jsonElement, String propertyPath, List<String> results) {
        if (jsonElement == null) return;
        
        if (jsonElement.isJsonObject()) {
            JsonObject obj = jsonElement.getAsJsonObject();
            
            // 检查当前对象是否有目标属性
            String[] segments = propertyPath.split("\\.");
            JsonElement targetElement = obj;
            boolean found = true;
            
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                
                if (targetElement.isJsonObject() && targetElement.getAsJsonObject().has(segment)) {
                    targetElement = targetElement.getAsJsonObject().get(segment);
                } else {
                    found = false;
                    break;
                }
            }
            
            if (found && targetElement != null) {
                if (targetElement.isJsonPrimitive()) {
                    results.add(targetElement.getAsString());
                } else {
                    results.add(targetElement.toString());
                }
            }
            
            // 递归处理子对象
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                findPropertyRecursively(entry.getValue(), propertyPath, results);
            }
        } else if (jsonElement.isJsonArray()) {
            // 递归处理数组中的每个元素
            JsonArray array = jsonElement.getAsJsonArray();
            for (JsonElement element : array) {
                findPropertyRecursively(element, propertyPath, results);
            }
        }
    }
    
    /**
     * 处理select过滤表达式的JSON路径
     * @param jsonElement JSON元素
     * @param path 路径表达式
     * @return 提取的值
     */
    public static String processSelectPath(JsonElement jsonElement, String path) {
        try {
            // 由于select表达式非常复杂，这里实现一个简化版本
            // 主要处理 .entities[] | select(.roles[] | contains("registrant")) | .vcardArray[1].[] | select(.[0] == "fn") | .[-1] 类型的表达式
            
            if (path.contains("select") && path.contains("contains")) {
                // 提取要查找的值
                Pattern containsPattern = Pattern.compile("contains\\(\\s*\"([^\"]+)\"\\s*\\)");
                Matcher containsMatcher = containsPattern.matcher(path);
                
                if (containsMatcher.find()) {
                    String searchValue = containsMatcher.group(1);
                    
                    // 提取要在哪个数组中查找
                    Pattern arrayPattern = Pattern.compile("\\.(\\w+)\\[\\]");
                    Matcher arrayMatcher = arrayPattern.matcher(path);
                    
                    if (arrayMatcher.find()) {
                        String arrayProperty = arrayMatcher.group(1);
                        
                        // 查找包含指定值的对象
                        if (jsonElement.isJsonObject()) {
                            JsonElement targetArray = jsonElement.getAsJsonObject().get(arrayProperty);
                            if (targetArray != null && targetArray.isJsonArray()) {
                                for (JsonElement item : targetArray.getAsJsonArray()) {
                                    // 检查此项是否包含目标值
                                    if (item.toString().contains(searchValue)) {
                                        // 检查路径后半部分是否有其他提取操作
                                        int selectPos = path.indexOf("select");
                                        int pipePos = path.indexOf("|", selectPos);
                                        
                                        if (pipePos > 0 && pipePos < path.length() - 1) {
                                            // 对匹配的项应用剩余路径
                                            String remainingPath = path.substring(pipePos + 1).trim();
                                            // 简化处理：如果包含第二个select，直接返回项
                                            if (remainingPath.contains("select")) {
                                                return item.toString();
                                            } else if (remainingPath.startsWith(".")) {
                                                // 提取指定属性
                                                String propPath = remainingPath.substring(1);
                                                if (propPath.contains("[") && propPath.contains("]")) {
                                                    return processArrayIndexPath(item, remainingPath);
                                                } else {
                                                    String[] segments = propPath.split("\\.");
                                                    JsonElement result = item;
                                                    for (String segment : segments) {
                                                        if (!segment.isEmpty() && result.isJsonObject()) {
                                                            result = result.getAsJsonObject().get(segment);
                                                            if (result == null) break;
                                                        }
                                                    }
                                                    
                                                    if (result != null) {
                                                        if (result.isJsonPrimitive()) {
                                                            return result.getAsString();
                                                        } else {
                                                            return result.toString();
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // 没有后续处理，返回匹配项
                                            return item.toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 处理空值回退的JSON路径
     * @param jsonElement JSON元素
     * @param path 路径表达式
     * @return 提取的值
     */
    public static String processEmptyFallbackPath(JsonElement jsonElement, String path) {
        try {
            // 处理 .nodeId//empty[0] 类型的表达式
            String[] parts = path.split("//empty");
            if (parts.length < 1) {
                return null;
            }
            
            // 尝试主路径
            String mainPath = parts[0];
            if (mainPath.startsWith("$")) {
                mainPath = mainPath.substring(1);
            }
            if (mainPath.startsWith(".")) {
                mainPath = mainPath.substring(1);
            }
            
            String[] segments = mainPath.split("\\.");
            JsonElement currentElement = jsonElement;
            
            for (String segment : segments) {
                if (!segment.isEmpty()) {
                    if (currentElement.isJsonObject() && currentElement.getAsJsonObject().has(segment)) {
                        currentElement = currentElement.getAsJsonObject().get(segment);
                    } else {
                        // 主路径不存在，尝试回退值
                        String fallbackPart = parts[1];
                        if (fallbackPart.startsWith("[") && fallbackPart.contains("]")) {
                            // 简化处理：如果是[0]，返回null或空字符串
                            return "";
                        }
                        return null;
                    }
                }
            }
            
            // 主路径存在
            if (currentElement != null) {
                if (currentElement.isJsonPrimitive()) {
                    return currentElement.getAsString();
                } else {
                    return currentElement.toString();
                }
            }
            
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * 遍历JSON元素
     * @param jsonElement JSON元素
     * @param segments 路径段数组
     * @param index 当前段索引
     * @return 提取的值
     */
    public static Object traverseJsonElement(JsonElement jsonElement, String[] segments, int index) {
        if (jsonElement == null || index >= segments.length) {
            return jsonElement;
        }
        
        String segment = segments[index];
        if (segment.isEmpty()) {
            return traverseJsonElement(jsonElement, segments, index + 1);
        }
        
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has(segment)) {
                JsonElement nextElement = jsonObject.get(segment);
                if (index == segments.length - 1) {
                    // 最后一个段
                    if (nextElement.isJsonPrimitive()) {
                        return nextElement.getAsString();
                    } else {
                        return nextElement.toString();
                    }
                } else {
                    // 继续遍历
                    return traverseJsonElement(nextElement, segments, index + 1);
                }
            }
        } else if (jsonElement.isJsonArray() && segment.matches("\\d+")) {
            // 如果是数组且段是数字，尝试作为索引访问
            try {
                int arrayIndex = Integer.parseInt(segment);
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                if (arrayIndex >= 0 && arrayIndex < jsonArray.size()) {
                    JsonElement nextElement = jsonArray.get(arrayIndex);
                    if (index == segments.length - 1) {
                        // 最后一个段
                        if (nextElement.isJsonPrimitive()) {
                            return nextElement.getAsString();
                        } else {
                            return nextElement.toString();
                        }
                    } else {
                        // 继续遍历
                        return traverseJsonElement(nextElement, segments, index + 1);
                    }
                }
            } catch (NumberFormatException e) {
                // 不是有效的数组索引
            }
        }
        
        return null;
    }

    /**
     * 匹配JSON内容
     * @param content JSON字符串内容
     * @param values 匹配值列表
     * @return 是否匹配成功
     */
    public static boolean matchJson(String content, List<String> values) {
        // 简单实现，仅支持JSON字符串匹配
        try {
            for (String value : values) {
                if (content.contains(value)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }
} 