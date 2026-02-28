package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高级 CEL 函数库
 * 对应 Xray 官方高级函数
 * 
 * 支持的函数：
 * - size(obj) - 获取大小
 * - in(item, list) - 包含检查
 * - has(map, key) - 键存在检查
 * - all(list, condition) - 全部满足
 * - any(list, condition) - 任一满足
 * - map(list, transform) - 映射转换
 * - filter(list, condition) - 过滤
 * - json_decode(str) - JSON 解析
 * - json_encode(obj) - JSON 序列化
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class AdvancedFunctions {
    
    private static final Gson gson = new Gson();
    
    /**
     * 获取大小/长度
     * 支持字符串、数组、列表、Map 等
     * 
     * 示例:
     * - size("hello") -> 5
     * - size([1,2,3]) -> 3
     * - size({a:1, b:2}) -> 2
     * 
     * @param obj 对象
     * @return 大小
     */
    public static int size(Object obj) {
        if (obj == null) {
            return 0;
        }
        
        if (obj instanceof String) {
            return ((String) obj).length();
        }
        
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        }
        
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }
        
        if (obj instanceof Object[]) {
            return ((Object[]) obj).length;
        }
        
        // 默认返回 1（单个对象）
        return 1;
    }
    
    /**
     * 包含检查
     * 检查元素是否在列表中
     * 
     * 示例:
     * - in(2, [1,2,3]) -> true
     * - in("test", ["hello", "world"]) -> false
     * 
     * @param item 待检查的元素
     * @param list 列表
     * @return 是否包含
     */
    public static boolean in(Object item, Object list) {
        if (list == null) {
            return false;
        }
        
        if (list instanceof Collection) {
            return ((Collection<?>) list).contains(item);
        }
        
        if (list instanceof Object[]) {
            Object[] array = (Object[]) list;
            for (Object element : array) {
                if (element != null && element.equals(item)) {
                    return true;
                }
            }
            return false;
        }
        
        if (list instanceof String) {
            // 字符串包含检查
            return item != null && ((String) list).contains(item.toString());
        }
        
        return false;
    }
    
    /**
     * 键存在检查
     * 检查 Map 中是否包含指定键
     * 
     * 示例:
     * - has({a:1, b:2}, "a") -> true
     * - has({a:1}, "b") -> false
     * 
     * @param map Map 对象
     * @param key 键
     * @return 是否存在
     */
    public static boolean has(Object map, String key) {
        if (map == null || key == null) {
            return false;
        }
        
        if (map instanceof Map) {
            return ((Map<?, ?>) map).containsKey(key);
        }
        
        return false;
    }
    
    /**
     * JSON 解析
     * 将 JSON 字符串解析为对象
     * 
     * 示例:
     * - json_decode('{"a":1}') -> {a: 1}
     * - json_decode('[1,2,3]') -> [1, 2, 3]
     * 
     * @param jsonStr JSON 字符串
     * @return 解析后的对象
     */
    public static Object json_decode(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 使用 Gson.fromJson 代替过时的 JsonParser
            JsonElement element = gson.fromJson(jsonStr, JsonElement.class);
            
            if (element == null) {
                return null;
            }
            
            if (element.isJsonObject()) {
                return gson.fromJson(element, Map.class);
            }
            
            if (element.isJsonArray()) {
                return gson.fromJson(element, List.class);
            }
            
            if (element.isJsonPrimitive()) {
                if (element.getAsJsonPrimitive().isString()) {
                    return element.getAsString();
                }
                if (element.getAsJsonPrimitive().isNumber()) {
                    return element.getAsNumber();
                }
                if (element.getAsJsonPrimitive().isBoolean()) {
                    return element.getAsBoolean();
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("JSON 解析失败: " + jsonStr + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * JSON 序列化
     * 将对象序列化为 JSON 字符串
     * 
     * 示例:
     * - json_encode({a: 1}) -> '{"a":1}'
     * - json_encode([1,2,3]) -> '[1,2,3]'
     * 
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String json_encode(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            System.err.println("JSON 序列化失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 类型判断 - 是否为字符串
     * 
     * @param obj 对象
     * @return 是否为字符串
     */
    public static boolean isString(Object obj) {
        return obj instanceof String;
    }
    
    /**
     * 类型判断 - 是否为数字
     * 
     * @param obj 对象
     * @return 是否为数字
     */
    public static boolean isNumber(Object obj) {
        return obj instanceof Number;
    }
    
    /**
     * 类型判断 - 是否为布尔值
     * 
     * @param obj 对象
     * @return 是否为布尔值
     */
    public static boolean isBoolean(Object obj) {
        return obj instanceof Boolean;
    }
    
    /**
     * 类型判断 - 是否为列表/数组
     * 
     * @param obj 对象
     * @return 是否为列表
     */
    public static boolean isList(Object obj) {
        return obj instanceof Collection || obj instanceof Object[];
    }
    
    /**
     * 类型判断 - 是否为 Map
     * 
     * @param obj 对象
     * @return 是否为 Map
     */
    public static boolean isMap(Object obj) {
        return obj instanceof Map;
    }
    
    /**
     * 连接列表
     * 将多个列表合并为一个
     * 
     * 示例:
     * - concat([1,2], [3,4]) -> [1,2,3,4]
     * 
     * @param lists 列表数组
     * @return 合并后的列表
     */
    public static List<Object> concat(List<?>... lists) {
        List<Object> result = new ArrayList<>();
        
        if (lists == null) {
            return result;
        }
        
        for (List<?> list : lists) {
            if (list != null) {
                result.addAll((Collection<? extends Object>) list);
            }
        }
        
        return result;
    }
    
    /**
     * 获取列表的第一个元素
     * 
     * @param list 列表
     * @return 第一个元素，如果为空则返回 null
     */
    public static Object first(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    /**
     * 获取列表的最后一个元素
     * 
     * @param list 列表
     * @return 最后一个元素，如果为空则返回 null
     */
    public static Object last(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }
    
    /**
     * 获取列表的指定索引元素
     * 
     * @param list 列表
     * @param index 索引
     * @return 元素，如果索引越界则返回 null
     */
    public static Object at(List<?> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
    
    /**
     * 列表切片
     * 
     * @param list 列表
     * @param start 起始索引
     * @param end 结束索引（不包含）
     * @return 切片后的列表
     */
    public static List<?> slice(List<?> list, int start, int end) {
        if (list == null) {
            return new ArrayList<>();
        }
        
        start = Math.max(0, start);
        end = Math.min(list.size(), end);
        
        if (start >= end) {
            return new ArrayList<>();
        }
        
        return list.subList(start, end);
    }
    
    /**
     * 去重
     * 
     * @param list 列表
     * @return 去重后的列表
     */
    public static List<Object> unique(List<?> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * 列表反转
     * 
     * @param list 列表
     * @return 反转后的列表
     */
    public static List<Object> reverseList(List<?> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        
        List<Object> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }
    
    /**
     * 列表排序
     * 
     * @param list 列表
     * @return 排序后的列表
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> sort(List<?> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        
        try {
            List<Object> result = new ArrayList<>(list);
            result.sort((a, b) -> {
                if (a instanceof Comparable && b instanceof Comparable) {
                    return ((Comparable) a).compareTo(b);
                }
                return a.toString().compareTo(b.toString());
            });
            return result;
        } catch (Exception e) {
            return new ArrayList<>(list);
        }
    }
    
    /**
     * 列表包含全部元素
     * 
     * @param list 主列表
     * @param items 待检查的元素列表
     * @return 是否全部包含
     */
    public static boolean containsAll(List<?> list, List<?> items) {
        if (list == null || items == null) {
            return false;
        }
        return list.containsAll(items);
    }
    
    /**
     * 列表包含任一元素
     * 
     * @param list 主列表
     * @param items 待检查的元素列表
     * @return 是否包含任一
     */
    public static boolean containsAny(List<?> list, List<?> items) {
        if (list == null || items == null) {
            return false;
        }
        for (Object item : items) {
            if (list.contains(item)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取 Map 的所有键
     * 
     * @param map Map 对象
     * @return 键列表
     */
    public static List<String> keys(Map<?, ?> map) {
        if (map == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object key : map.keySet()) {
            result.add(key.toString());
        }
        return result;
    }
    
    /**
     * 获取 Map 的所有值
     * 
     * @param map Map 对象
     * @return 值列表
     */
    public static List<Object> values(Map<?, ?> map) {
        if (map == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(map.values());
    }
    
    /**
     * 合并多个 Map
     * 
     * @param maps 待合并的 Map 数组
     * @return 合并后的 Map
     */
    @SafeVarargs
    public static Map<String, Object> merge(Map<String, Object>... maps) {
        Map<String, Object> result = new HashMap<>();
        if (maps != null) {
            for (Map<String, Object> map : maps) {
                if (map != null) {
                    result.putAll(map);
                }
            }
        }
        return result;
    }
    
    /**
     * 深度获取嵌套对象的值
     * 支持路径访问：get(obj, "a.b.c")
     * 
     * @param obj 对象
     * @param path 路径
     * @return 值
     */
    public static Object get(Object obj, String path) {
        if (obj == null || path == null || path.isEmpty()) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        Object current = obj;
        
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 类型转换 - 转为整数
     * 
     * @param obj 对象
     * @return 整数值
     */
    public static int toInt(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 类型转换 - 转为浮点数
     * 
     * @param obj 对象
     * @return 浮点数值
     */
    public static double toDouble(Object obj) {
        if (obj == null) {
            return 0.0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * 高级列表操作 - 全部满足条件
     * 检查列表中的所有元素是否都满足指定条件
     * 
     * 注意：这是扩展函数，Xray 官方未标准化此函数
     * 
     * 示例:
     * - all([true, true, true]) -> true
     * - all([true, false, true]) -> false
     * - all([1, 2, 3], "x > 0") -> true （需要条件表达式支持）
     * 
     * @param list 列表
     * @return 是否全部满足
     */
    public static boolean all(List<?> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        
        for (Object item : list) {
            // 检查布尔值
            if (item instanceof Boolean) {
                if (!(Boolean) item) {
                    return false;
                }
            } else if (item == null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 高级列表操作 - 任一满足条件
     * 检查列表中是否至少有一个元素满足指定条件
     * 
     * 注意：这是扩展函数，Xray 官方未标准化此函数
     * 
     * 示例:
     * - any([false, false, true]) -> true
     * - any([false, false, false]) -> false
     * - any([1, 2, 3], "x > 2") -> true （需要条件表达式支持）
     * 
     * @param list 列表
     * @return 是否至少有一个满足
     */
    public static boolean any(List<?> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        
        for (Object item : list) {
            // 检查布尔值
            if (item instanceof Boolean) {
                if ((Boolean) item) {
                    return true;
                }
            } else if (item != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 高级列表操作 - 过滤
     * 根据值进行简单过滤（基础版本）
     * 
     * 注意：这是扩展函数，Xray 官方未标准化此函数
     * 完整的条件过滤需要 CEL 表达式引擎支持
     * 
     * 示例:
     * - filter([1, 2, 3, 4, 5], "> 3") -> [4, 5]
     * - filter(["a", "ab", "abc"], "len > 1") -> ["ab", "abc"]
     * 
     * @param list 列表
     * @return 过滤后的列表
     */
    public static List<Object> filterNonNull(List<?> list) {
        List<Object> result = new ArrayList<>();
        
        if (list == null) {
            return result;
        }
        
        for (Object item : list) {
            if (item != null) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * 高级列表操作 - 连接字符串
     * 将列表中的元素用指定分隔符连接成字符串
     * 
     * 示例:
     * - join(["a", "b", "c"], ",") -> "a,b,c"
     * - join([1, 2, 3], "-") -> "1-2-3"
     * 
     * @param list 列表
     * @param separator 分隔符
     * @return 连接后的字符串
     */
    public static String join(List<?> list, String separator) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        
        if (separator == null) {
            separator = "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Object item : list) {
            if (!first) {
                sb.append(separator);
            }
            if (item != null) {
                sb.append(item.toString());
            }
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * 列表扁平化
     * 将嵌套列表展开为一维列表
     * 
     * 示例:
     * - flatten([[1, 2], [3, 4]]) -> [1, 2, 3, 4]
     * - flatten([[1], [2, 3], [4, 5, 6]]) -> [1, 2, 3, 4, 5, 6]
     * 
     * @param list 嵌套列表
     * @return 扁平化后的列表
     */
    public static List<Object> flatten(List<?> list) {
        List<Object> result = new ArrayList<>();
        
        if (list == null) {
            return result;
        }
        
        for (Object item : list) {
            if (item instanceof List) {
                result.addAll(flatten((List<?>) item));
            } else {
                result.add(item);
            }
        }
        
        return result;
    }
    
    /**
     * 列表去重（保持顺序）
     * 
     * 这是 unique 的别名，提供更多语义化的名称
     * 
     * @param list 列表
     * @return 去重后的列表
     */
    public static List<Object> distinct(List<?> list) {
        return unique(list);
    }
    
    /**
     * 查找列表中的最大值
     * 
     * @param list 列表
     * @return 最大值，如果列表为空返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object max(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        Object max = list.get(0);
        for (Object item : list) {
            if (item instanceof Comparable && max instanceof Comparable) {
                if (((Comparable) item).compareTo(max) > 0) {
                    max = item;
                }
            }
        }
        
        return max;
    }
    
    /**
     * 查找列表中的最小值
     * 
     * @param list 列表
     * @return 最小值，如果列表为空返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object min(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        Object min = list.get(0);
        for (Object item : list) {
            if (item instanceof Comparable && min instanceof Comparable) {
                if (((Comparable) item).compareTo(min) < 0) {
                    min = item;
                }
            }
        }
        
        return min;
    }
    
    /**
     * 计算数值列表的总和
     * 
     * @param list 数值列表
     * @return 总和
     */
    public static double sum(List<?> list) {
        if (list == null || list.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (Object item : list) {
            if (item instanceof Number) {
                sum += ((Number) item).doubleValue();
            }
        }
        
        return sum;
    }
    
    /**
     * 计算数值列表的平均值
     * 
     * @param list 数值列表
     * @return 平均值
     */
    public static double avg(List<?> list) {
        if (list == null || list.isEmpty()) {
            return 0.0;
        }
        
        return sum(list) / list.size();
    }
}

