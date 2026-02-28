package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 高级函数测试
 * 测试所有 Xray 高级操作函数
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray 高级函数测试")
public class AdvancedFunctionsTest {
    
    @Test
    @DisplayName("测试 size() - 获取大小")
    public void testSize() {
        assertEquals(5, AdvancedFunctions.size("hello"));
        assertEquals(3, AdvancedFunctions.size(Arrays.asList(1, 2, 3)));
        
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, AdvancedFunctions.size(map));
        
        assertEquals(0, AdvancedFunctions.size(null));
        assertEquals(0, AdvancedFunctions.size(""));
        
        Object[] array = {1, 2, 3, 4};
        assertEquals(4, AdvancedFunctions.size(array));
    }
    
    @Test
    @DisplayName("测试 in() - 包含检查")
    public void testIn() {
        assertTrue(AdvancedFunctions.in(2, Arrays.asList(1, 2, 3)));
        assertFalse(AdvancedFunctions.in("test", Arrays.asList("hello", "world")));
        assertTrue(AdvancedFunctions.in("hello", Arrays.asList("hello", "world")));
        assertFalse(AdvancedFunctions.in(5, Arrays.asList(1, 2, 3)));
        
        // 数组测试
        Object[] array = {"a", "b", "c"};
        assertTrue(AdvancedFunctions.in("b", array));
        assertFalse(AdvancedFunctions.in("d", array));
        
        // 字符串包含
        assertTrue(AdvancedFunctions.in("world", "hello world"));
        assertFalse(AdvancedFunctions.in("test", "hello world"));
        
        // null 测试
        assertFalse(AdvancedFunctions.in(1, null));
        assertFalse(AdvancedFunctions.in(null, Arrays.asList(1, 2, 3)));
    }
    
    @Test
    @DisplayName("测试 has() - 键存在检查")
    public void testHas() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        assertTrue(AdvancedFunctions.has(map, "a"));
        assertTrue(AdvancedFunctions.has(map, "b"));
        assertFalse(AdvancedFunctions.has(map, "c"));
        assertFalse(AdvancedFunctions.has(null, "a"));
        assertFalse(AdvancedFunctions.has(map, null));
    }
    
    @Test
    @DisplayName("测试 json_decode() - JSON 解析")
    public void testJsonDecode() {
        // 对象解析
        Object result = AdvancedFunctions.json_decode("{\"a\":1,\"b\":2}");
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals(1.0, ((Number) map.get("a")).doubleValue(), 0.001);
        
        // 数组解析
        result = AdvancedFunctions.json_decode("[1,2,3]");
        assertNotNull(result);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        assertEquals(3, list.size());
        
        // 字符串解析
        result = AdvancedFunctions.json_decode("\"hello\"");
        assertEquals("hello", result);
        
        // 数字解析
        result = AdvancedFunctions.json_decode("123");
        assertTrue(result instanceof Number);
        
        // 布尔值解析
        result = AdvancedFunctions.json_decode("true");
        assertEquals(true, result);
        
        // null 解析
        assertNull(AdvancedFunctions.json_decode(null));
        assertNull(AdvancedFunctions.json_decode(""));
    }
    
    @Test
    @DisplayName("测试 json_encode() - JSON 序列化")
    public void testJsonEncode() {
        // 对象序列化
        Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", "test");
        String json = AdvancedFunctions.json_encode(map);
        assertTrue(json.contains("\"a\""));
        assertTrue(json.contains("\"b\""));
        
        // 数组序列化
        List<Integer> list = Arrays.asList(1, 2, 3);
        json = AdvancedFunctions.json_encode(list);
        assertEquals("[1,2,3]", json);
        
        // 字符串序列化
        json = AdvancedFunctions.json_encode("hello");
        assertEquals("\"hello\"", json);
        
        // null 序列化
        json = AdvancedFunctions.json_encode(null);
        assertEquals("null", json);
    }
    
    @Test
    @DisplayName("测试 JSON 往返")
    public void testJsonRoundTrip() {
        // 对象往返
        Map<String, Object> original = new HashMap<>();
        original.put("name", "admin");
        original.put("age", 25);
        original.put("active", true);
        
        String json = AdvancedFunctions.json_encode(original);
        Object decoded = AdvancedFunctions.json_decode(json);
        assertNotNull(decoded);
        assertTrue(decoded instanceof Map);
        
        // 数组往返
        List<Object> originalList = Arrays.asList(1, "test", true);
        json = AdvancedFunctions.json_encode(originalList);
        decoded = AdvancedFunctions.json_decode(json);
        assertNotNull(decoded);
        assertTrue(decoded instanceof List);
    }
    
    @Test
    @DisplayName("测试类型判断函数")
    public void testTypeChecking() {
        assertTrue(AdvancedFunctions.isString("test"));
        assertFalse(AdvancedFunctions.isString(123));
        
        assertTrue(AdvancedFunctions.isNumber(123));
        assertTrue(AdvancedFunctions.isNumber(3.14));
        assertFalse(AdvancedFunctions.isNumber("123"));
        
        assertTrue(AdvancedFunctions.isBoolean(true));
        assertTrue(AdvancedFunctions.isBoolean(false));
        assertFalse(AdvancedFunctions.isBoolean(1));
        
        assertTrue(AdvancedFunctions.isList(Arrays.asList(1, 2, 3)));
        assertTrue(AdvancedFunctions.isList(new Object[]{1, 2, 3}));
        assertFalse(AdvancedFunctions.isList("test"));
        
        assertTrue(AdvancedFunctions.isMap(new HashMap<>()));
        assertFalse(AdvancedFunctions.isMap(Arrays.asList(1, 2)));
    }
    
    @Test
    @DisplayName("测试列表操作 - first, last, at")
    public void testListAccess() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertEquals(1, AdvancedFunctions.first(list));
        assertEquals(5, AdvancedFunctions.last(list));
        assertEquals(3, AdvancedFunctions.at(list, 2));
        
        assertNull(AdvancedFunctions.first(null));
        assertNull(AdvancedFunctions.first(Collections.emptyList()));
        assertNull(AdvancedFunctions.at(list, 10));
        assertNull(AdvancedFunctions.at(list, -1));
    }
    
    @Test
    @DisplayName("测试列表切片")
    public void testSlice() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        List<?> slice = AdvancedFunctions.slice(list, 0, 3);
        assertEquals(3, slice.size());
        assertEquals(1, slice.get(0));
        assertEquals(3, slice.get(2));
        
        slice = AdvancedFunctions.slice(list, 2, 5);
        assertEquals(3, slice.size());
        
        // 边界测试
        slice = AdvancedFunctions.slice(list, 0, 100);
        assertEquals(5, slice.size());
        
        slice = AdvancedFunctions.slice(list, 10, 20);
        assertTrue(slice.isEmpty());
    }
    
    @Test
    @DisplayName("测试列表去重")
    public void testUnique() {
        List<Integer> list = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        List<Object> unique = AdvancedFunctions.unique(list);
        
        assertEquals(4, unique.size());
        assertTrue(unique.contains(1));
        assertTrue(unique.contains(2));
        assertTrue(unique.contains(3));
        assertTrue(unique.contains(4));
        
        List<Object> empty = AdvancedFunctions.unique(null);
        assertTrue(empty.isEmpty());
    }
    
    @Test
    @DisplayName("测试列表反转")
    public void testReverseList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Object> reversed = AdvancedFunctions.reverseList(list);
        
        assertEquals(5, reversed.size());
        assertEquals(5, reversed.get(0));
        assertEquals(1, reversed.get(4));
    }
    
    @Test
    @DisplayName("测试列表排序")
    public void testSort() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        List<Object> sorted = AdvancedFunctions.sort(list);
        
        assertEquals(8, sorted.size());
        assertEquals(1, sorted.get(0));
        assertEquals(9, sorted.get(7));
        
        // 字符串排序
        List<String> strList = Arrays.asList("c", "a", "b");
        sorted = AdvancedFunctions.sort(strList);
        assertEquals("a", sorted.get(0));
        assertEquals("c", sorted.get(2));
    }
    
    @Test
    @DisplayName("测试 containsAll 和 containsAny")
    public void testContains() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertTrue(AdvancedFunctions.containsAll(list, Arrays.asList(1, 2, 3)));
        assertFalse(AdvancedFunctions.containsAll(list, Arrays.asList(1, 6)));
        
        assertTrue(AdvancedFunctions.containsAny(list, Arrays.asList(5, 6, 7)));
        assertFalse(AdvancedFunctions.containsAny(list, Arrays.asList(6, 7, 8)));
    }
    
    @Test
    @DisplayName("测试 Map 操作 - keys 和 values")
    public void testMapOperations() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        
        List<String> keys = AdvancedFunctions.keys(map);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
        
        List<Object> values = AdvancedFunctions.values(map);
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }
    
    @Test
    @DisplayName("测试 Map 合并")
    public void testMerge() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        
        Map<String, Object> map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);
        
        Map<String, Object> merged = AdvancedFunctions.merge(map1, map2);
        assertEquals(4, merged.size());
        assertEquals(1, merged.get("a"));
        assertEquals(4, merged.get("d"));
        
        // 覆盖测试
        map2.put("a", 10);
        merged = AdvancedFunctions.merge(map1, map2);
        assertEquals(10, merged.get("a"));
    }
    
    @Test
    @DisplayName("测试深度获取 - get()")
    public void testGet() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("c", "value");
        
        Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", inner);
        
        assertEquals(1, AdvancedFunctions.get(map, "a"));
        assertEquals(inner, AdvancedFunctions.get(map, "b"));
        assertEquals("value", AdvancedFunctions.get(map, "b.c"));
        assertNull(AdvancedFunctions.get(map, "b.d"));
        assertNull(AdvancedFunctions.get(map, "x.y.z"));
    }
    
    @Test
    @DisplayName("测试类型转换 - toInt 和 toDouble")
    public void testTypeConversion() {
        assertEquals(123, AdvancedFunctions.toInt(123));
        assertEquals(123, AdvancedFunctions.toInt("123"));
        assertEquals(123, AdvancedFunctions.toInt(123.45));
        assertEquals(0, AdvancedFunctions.toInt("abc"));
        assertEquals(0, AdvancedFunctions.toInt(null));
        
        assertEquals(3.14, AdvancedFunctions.toDouble(3.14), 0.001);
        assertEquals(123.0, AdvancedFunctions.toDouble(123), 0.001);
        assertEquals(123.45, AdvancedFunctions.toDouble("123.45"), 0.001);
        assertEquals(0.0, AdvancedFunctions.toDouble("abc"), 0.001);
    }
    
    @Test
    @DisplayName("测试 all() 和 any()")
    public void testAllAny() {
        List<Object> allTrue = Arrays.asList(true, true, true);
        assertTrue(AdvancedFunctions.all(allTrue));
        
        List<Object> someFalse = Arrays.asList(true, false, true);
        assertFalse(AdvancedFunctions.all(someFalse));
        
        List<Object> allFalse = Arrays.asList(false, false, false);
        assertFalse(AdvancedFunctions.any(allFalse));
        
        List<Object> someTrue = Arrays.asList(false, false, true);
        assertTrue(AdvancedFunctions.any(someTrue));
    }
    
    @Test
    @DisplayName("测试 join()")
    public void testJoin() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a,b,c", AdvancedFunctions.join(list, ","));
        assertEquals("a-b-c", AdvancedFunctions.join(list, "-"));
        assertEquals("abc", AdvancedFunctions.join(list, ""));
        
        List<Integer> intList = Arrays.asList(1, 2, 3);
        assertEquals("1,2,3", AdvancedFunctions.join(intList, ","));
    }
    
    @Test
    @DisplayName("测试 flatten() - 列表扁平化")
    public void testFlatten() {
        List<Object> nested = Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList(3, 4),
            Arrays.asList(5, 6)
        );
        
        List<Object> flat = AdvancedFunctions.flatten(nested);
        assertEquals(6, flat.size());
        assertEquals(1, flat.get(0));
        assertEquals(6, flat.get(5));
        
        // 多层嵌套
        List<Object> deepNested = Arrays.asList(
            Arrays.asList(1, Arrays.asList(2, 3)),
            Arrays.asList(4, 5)
        );
        flat = AdvancedFunctions.flatten(deepNested);
        assertEquals(5, flat.size());
    }
    
    @Test
    @DisplayName("测试 max() 和 min()")
    public void testMaxMin() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        
        assertEquals(9, AdvancedFunctions.max(list));
        assertEquals(1, AdvancedFunctions.min(list));
        
        assertNull(AdvancedFunctions.max(null));
        assertNull(AdvancedFunctions.max(Collections.emptyList()));
    }
    
    @Test
    @DisplayName("测试 sum() 和 avg()")
    public void testSumAvg() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertEquals(15.0, AdvancedFunctions.sum(list), 0.001);
        assertEquals(3.0, AdvancedFunctions.avg(list), 0.001);
        
        List<Double> doubleList = Arrays.asList(1.5, 2.5, 3.5);
        assertEquals(7.5, AdvancedFunctions.sum(doubleList), 0.001);
        assertEquals(2.5, AdvancedFunctions.avg(doubleList), 0.001);
    }
    
    @Test
    @DisplayName("复杂场景 - 链式操作")
    public void testComplexChaining() {
        // 场景1: 列表处理链
        List<Integer> list = Arrays.asList(5, 2, 8, 1, 9, 2, 5);
        List<Object> unique = AdvancedFunctions.unique(list);
        List<Object> sorted = AdvancedFunctions.sort(unique);
        Object max = AdvancedFunctions.max(sorted);
        assertEquals(9, max);
        
        // 场景2: JSON 处理
        Map<String, Object> data = new HashMap<>();
        data.put("users", Arrays.asList("alice", "bob", "charlie"));
        data.put("count", 3);
        
        String json = AdvancedFunctions.json_encode(data);
        Object decoded = AdvancedFunctions.json_decode(json);
        assertTrue(decoded instanceof Map);
        
        // 场景3: 复杂数据提取
        Map<String, Object> nested = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("value", 42);
        nested.put("data", inner);
        
        Object value = AdvancedFunctions.get(nested, "data.value");
        assertEquals(42, value);
    }
}



