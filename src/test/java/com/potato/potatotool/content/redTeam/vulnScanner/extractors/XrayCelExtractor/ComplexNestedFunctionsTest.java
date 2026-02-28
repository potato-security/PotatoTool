package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 复杂嵌套函数测试
 * 测试多层嵌套、复杂组合的 CEL 表达式
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("复杂嵌套函数测试")
public class ComplexNestedFunctionsTest {
    
    private Map<String, Object> context;
    
    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
    }
    
    @Test
    @DisplayName("测试多层函数嵌套 - 3层")
    public void testThreeLevelNesting() {
        context.put("text", "Hello World");
        
        // toUpper(reverse(substr(text, 0, 5)))
        // 步骤: substr -> "Hello" -> reverse -> "olleH" -> toUpper -> "OLLEH"
        Object result = XrayCelParser.evaluateForValue("toUpper(reverse(substr(text, 0, 5)))", context);
        assertEquals("OLLEH", result);
        
        // base64(md5(toLower(text)))
        context.put("password", "Admin123");
        result = XrayCelParser.evaluateForValue("base64(md5(toLower(password)))", context);
        assertNotNull(result);
        assertTrue(result.toString().length() > 0);
    }
    
    @Test
    @DisplayName("测试多层函数嵌套 - 4层")
    public void testFourLevelNesting() {
        context.put("data", "test");
        
        // len(base64(hexEncode(md5(data))))
        Object result = XrayCelParser.evaluateForValue("len(base64(hexEncode(md5(data))))", context);
        assertNotNull(result);
        assertTrue((int) result > 0);
        
        // toUpper(substr(reverse(toLower(data)), 0, 2))
        result = XrayCelParser.evaluateForValue("toUpper(substr(reverse(toLower(data)), 0, 2))", context);
        assertEquals("TS", result);
    }
    
    @Test
    @DisplayName("测试编码链 - 多重编码")
    public void testEncodingChain() {
        context.put("payload", "admin");
        
        // Base64(URL(Hex(payload)))
        Object hex = XrayCelParser.evaluateForValue("hexEncode(payload)", context);
        Object url = XrayCelParser.evaluateForValue("urlencode(hexEncode(payload))", context);
        Object base64 = XrayCelParser.evaluateForValue("base64(urlencode(hexEncode(payload)))", context);
        
        assertNotNull(hex);
        assertNotNull(url);
        assertNotNull(base64);
        
        // 验证往返
        Object decoded1 = XrayCelParser.evaluateForValue("base64Decode(base64(payload))", context);
        assertEquals("admin", decoded1);
    }
    
    @Test
    @DisplayName("测试哈希链 - 多重哈希")
    public void testHashChain() {
        context.put("secret", "password123");
        
        // MD5(SHA256(secret))
        Object result = XrayCelParser.evaluateForValue("md5(sha256(secret))", context);
        assertNotNull(result);
        assertEquals(32, result.toString().length());
        
        // SHA256(MD5(SHA1(secret)))
        result = XrayCelParser.evaluateForValue("sha256(md5(sha1(secret)))", context);
        assertNotNull(result);
        assertEquals(64, result.toString().length());
    }
    
    @Test
    @DisplayName("测试条件嵌套")
    public void testNestedConditionals() {
        context.put("score", 85);
        context.put("age", 20);
        
        // 嵌套三元运算符
        Object result = XrayCelParser.evaluateForValue(
            "score > 90 ? \"A\" : (score > 80 ? \"B\" : (score > 70 ? \"C\" : \"D\"))", 
            context
        );
        assertEquals("B", result);
        
        // 复合条件
        boolean boolResult = XrayCelParser.evaluateLogical(
            "score > 60 && age >= 18 && (score > 80 || age > 25)", 
            context
        );
        assertTrue(boolResult);
    }
    
    @Test
    @DisplayName("测试列表操作链")
    public void testListOperationChain() {
        List<Integer> numbers = Arrays.asList(5, 2, 8, 2, 1, 8, 5);
        context.put("numbers", numbers);
        
        // unique -> sort -> first
        Object first = XrayCelParser.evaluateForValue("first(sort(unique(numbers)))", context);
        
        assertEquals(1, first);
        
        // max(sort(unique(numbers)))
        Object max = XrayCelParser.evaluateForValue("max(sort(unique(numbers)))", context);
        assertEquals(8, max);
        
        // sum(unique(numbers))
        // unique([5, 2, 8, 2, 1, 8, 5]) = [5, 2, 8, 1]
        // sum = 5 + 2 + 8 + 1 = 16
        Object sum = XrayCelParser.evaluateForValue("sum(unique(numbers))", context);
        assertEquals(16.0, ((Number) sum).doubleValue(), 0.001);
    }
    
    @Test
    @DisplayName("测试字符串处理链")
    public void testStringProcessingChain() {
        context.put("input", "  Hello World  ");
        
        // trim -> toLower -> replaceAll -> toUpper
        Object result = XrayCelParser.evaluateForValue("toUpper(replaceAll(toLower(trim(input)), \" \", \"_\"))", context);
        assertEquals("HELLO_WORLD", result);
        
        // len(base64(trim(input)))
        result = XrayCelParser.evaluateForValue("len(base64(trim(input)))", context);
        assertTrue((int) result > 0);
    }
    
    @Test
    @DisplayName("测试JSON处理嵌套")
    public void testJsonNesting() {
        String jsonData = "{\"user\":\"admin\",\"roles\":[\"admin\",\"user\"]}";
        context.put("jsonStr", jsonData);
        
        // json_decode -> get keys
        Object decoded = XrayCelParser.evaluateForValue("json_decode(jsonStr)", context);
        assertNotNull(decoded);
        assertTrue(decoded instanceof Map);
        
        // 嵌套: json_encode(json_decode(jsonStr))
        Object reEncoded = XrayCelParser.evaluateForValue("json_encode(json_decode(jsonStr))", context);
        assertNotNull(reEncoded);
        
        // 复杂嵌套: base64(json_encode(json_decode(jsonStr)))
        Object complex = XrayCelParser.evaluateForValue("base64(json_encode(json_decode(jsonStr)))", context);
        assertNotNull(complex);
    }
    
    @Test
    @DisplayName("测试数学表达式嵌套")
    public void testMathNesting() {
        context.put("a", 10);
        context.put("b", 5);
        context.put("c", 2);
        context.put("d", 3);
        
        // ((a + b) * c) - d
        Object result = XrayCelParser.evaluateForValue("((a + b) * c) - d", context);
        assertEquals(27.0, ((Number) result).doubleValue(), 0.001);
        
        // a + (b * (c + d))
        result = XrayCelParser.evaluateForValue("a + (b * (c + d))", context);
        assertEquals(35.0, ((Number) result).doubleValue(), 0.001);
        
        // 与函数结合: toInt((a + b) / c)
        result = XrayCelParser.evaluateForValue("toInt((a + b) / c)", context);
        assertEquals(7, result);
    }
    
    @Test
    @DisplayName("测试复杂属性访问链")
    public void testComplexPropertyChain() {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "admin");
        user.put("id", 1);
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("email", "admin@test.com");
        profile.put("level", 5);
        user.put("profile", profile);
        
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        context.put("data", data);
        
        // 深度属性访问
        Object result = XrayCelParser.evaluateForValue("get(data, \"user.profile.email\")", context);
        assertEquals("admin@test.com", result);
        
        result = XrayCelParser.evaluateForValue("get(data, \"user.profile.level\")", context);
        assertEquals(5, result);
    }
    
    @Test
    @DisplayName("测试切片与函数组合")
    public void testSliceWithFunctions() {
        List<String> items = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        context.put("items", items);
        
        // 切片后获取第一个
        Object result = XrayCelParser.evaluateForValue("first(items[1:4])", context);
        assertEquals("banana", result);
        
        // 切片后获取长度
        result = XrayCelParser.evaluateForValue("size(items[0:3])", context);
        assertEquals(3, result);
        
        // 切片后转大写并连接
        result = XrayCelParser.evaluateForValue("join(items[0:3], \",\")", context);
        assertEquals("apple,banana,cherry", result);
    }
    
    @Test
    @DisplayName("测试正则表达式嵌套")
    public void testRegexNesting() {
        context.put("data", "token=ABC123DEF456");
        
        // submatch -> first -> toLower
        Object matches = XrayCelParser.evaluateForValue("submatch(data, \"token=(\\\\w+)\")", context);
        assertNotNull(matches);
        assertTrue(matches instanceof List);
        
        @SuppressWarnings("unchecked")
        List<String> matchList = (List<String>) matches;
        context.put("token", matchList.get(0));
        
        Object lower = XrayCelParser.evaluateForValue("toLower(token)", context);
        assertEquals("abc123def456", lower);
    }
    
    @Test
    @DisplayName("测试随机函数组合")
    public void testRandomCombinations() {
        // randomInt 作为参数
        Object result = XrayCelParser.evaluateForValue("randomLowercase(randomInt(5, 10))", context);
        assertNotNull(result);
        int length = result.toString().length();
        assertTrue(length >= 5 && length <= 10);
        
        // 随机字符串 + Base64
        result = XrayCelParser.evaluateForValue("base64(rand_text_alphanumeric(10))", context);
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("测试极限嵌套 - 5层以上")
    public void testExtremNesting() {
        context.put("data", "test");
        
        // 5层: toUpper(reverse(substr(base64(md5(data)), 0, 10)))
        Object result = XrayCelParser.evaluateForValue("toUpper(reverse(substr(base64(md5(data)), 0, 10)))", context);
        assertNotNull(result);
        assertEquals(10, result.toString().length());
        
        // 6层: len(base64(hexEncode(toUpper(reverse(substr(data, 0, 3))))))
        result = XrayCelParser.evaluateForValue("len(base64(hexEncode(toUpper(reverse(substr(data, 0, 3))))))", context);
        assertNotNull(result);
        assertTrue((int) result > 0);
    }
    
    @Test
    @DisplayName("测试混合类型嵌套")
    public void testMixedTypeNesting() {
        context.put("num", 42);
        context.put("str", "value");
        
        // 数字转字符串再处理
        Object result = XrayCelParser.evaluateForValue("base64(string(num))", context);
        assertNotNull(result);
        
        // 字符串转数字再计算
        context.put("numStr", "123");
        result = XrayCelParser.evaluateForValue("toInt(numStr) + num", context);
        assertEquals(165.0, ((Number) result).doubleValue(), 0.001);
    }
    
    @Test
    @DisplayName("测试复杂布尔表达式嵌套")
    public void testComplexBooleanNesting() {
        context.put("a", 10);
        context.put("b", 5);
        context.put("c", 15);
        context.put("active", true);
        
        // 多层括号和逻辑运算
        boolean result = XrayCelParser.evaluateLogical(
            "((a > b && b < c) || !active) && (a + b) == c",
            context
        );
        assertTrue(result);
        
        // 嵌套条件与函数
        context.put("text", "admin");
        result = XrayCelParser.evaluateLogical(
            "(len(text) > 3 && contains(text, \"admin\")) || (a > 20 && active)",
            context
        );
        assertTrue(result);
    }
    
    @Test
    @DisplayName("测试性能 - 复杂嵌套表达式")
    public void testComplexNestingPerformance() {
        context.put("data", "performance_test_data");
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            // 复杂5层嵌套
            XrayCelParser.evaluateForValue("toUpper(reverse(substr(base64(md5(data)), 0, 10)))", context);
        }
        
        long time = System.currentTimeMillis() - start;
        assertTrue(time < 5000, "复杂嵌套表达式性能不佳: " + time + "ms");
    }
    
    @Test
    @DisplayName("测试边界 - 空参数嵌套")
    public void testNestingWithEmpty() {
        context.put("empty", "");
        context.put("nullValue", null);
        
        // 空字符串嵌套
        Object result = XrayCelParser.evaluateForValue("len(toUpper(trim(empty)))", context);
        assertEquals(0, result);
        
        // null 值处理
        result = XrayCelParser.evaluateForValue("len(string(nullValue))", context);
        assertNotNull(result);
    }
}

