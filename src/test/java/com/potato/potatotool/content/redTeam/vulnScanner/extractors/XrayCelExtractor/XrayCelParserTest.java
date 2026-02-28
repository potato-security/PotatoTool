package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * XrayCel 解析器测试
 * 测试 CEL 表达式解析和评估
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray CEL 解析器测试")
public class XrayCelParserTest {
    
    private Map<String, Object> context;
    
    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
        
        // 构建 response 对象
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "admin login success");
        response.put("latency", 150);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Server", "Apache/2.4.41");
        headers.put("Content-Type", "text/html");
        response.put("headers", headers);
        
        context.put("response", response);
        
        // 添加一些变量
        context.put("username", "admin");
        context.put("count", 5);
        context.put("active", true);
    }
    
    @Test
    @DisplayName("测试比较运算符")
    public void testComparisonOperators() {
        // 数值比较
        context.put("a", 10);
        context.put("b", 5);
        
        assertTrue(XrayCelParser.evaluateSingle("a == 10", context));
        assertTrue(XrayCelParser.evaluateSingle("a != 5", context));
        assertTrue(XrayCelParser.evaluateSingle("a > b", context));
        assertTrue(XrayCelParser.evaluateSingle("b < a", context));
        assertTrue(XrayCelParser.evaluateSingle("a >= 10", context));
        assertTrue(XrayCelParser.evaluateSingle("b <= 5", context));
        
        // 字符串比较
        context.put("str", "test");
        assertTrue(XrayCelParser.evaluateSingle("str == \"test\"", context));
        assertTrue(XrayCelParser.evaluateSingle("str != \"hello\"", context));
    }
    
    @Test
    @DisplayName("测试逻辑运算符")
    public void testLogicalOperators() {
        context.put("a", true);
        context.put("b", false);
        context.put("x", 10);
        context.put("y", 5);
        
        // AND 运算
        assertTrue(XrayCelParser.evaluateLogical("a && a", context));
        assertFalse(XrayCelParser.evaluateLogical("a && b", context));
        assertTrue(XrayCelParser.evaluateLogical("x > 5 && y < 10", context));
        
        // OR 运算
        assertTrue(XrayCelParser.evaluateLogical("a || b", context));
        assertTrue(XrayCelParser.evaluateLogical("b || a", context));
        assertFalse(XrayCelParser.evaluateLogical("b || b", context));
        
        // NOT 运算
        assertFalse(XrayCelParser.evaluateLogical("!a", context));
        assertTrue(XrayCelParser.evaluateLogical("!b", context));
        
        // 复合逻辑
        assertTrue(XrayCelParser.evaluateLogical("(a && x > 5) || b", context));
        assertTrue(XrayCelParser.evaluateLogical("a && (x > 5 || y > 10)", context));
    }
    
    @Test
    @DisplayName("测试函数调用")
    public void testFunctionCalls() {
        context.put("text", "hello world");
        context.put("number", "123");
        
        // 字符串函数
        Object result = XrayCelParser.evaluateForValue("len(text)", context);
        assertEquals(11, result);
        
        result = XrayCelParser.evaluateForValue("toUpper(text)", context);
        assertEquals("HELLO WORLD", result);
        
        result = XrayCelParser.evaluateForValue("substr(text, 0, 5)", context);
        assertEquals("hello", result);
        
        // 编码函数
        result = XrayCelParser.evaluateForValue("base64(text)", context);
        assertNotNull(result);
        
        // 哈希函数
        result = XrayCelParser.evaluateForValue("md5(text)", context);
        assertNotNull(result);
        assertEquals(32, result.toString().length());
        
        // 类型转换
        result = XrayCelParser.evaluateForValue("toInt(number)", context);
        assertEquals(123, result);
    }
    
    @Test
    @DisplayName("测试嵌套函数调用")
    public void testNestedFunctionCalls() {
        context.put("text", "hello");
        
        // base64(md5(text))
        Object result = XrayCelParser.evaluateForValue("base64(md5(text))", context);
        assertNotNull(result);
        
        // toUpper(substr(text, 0, 3))
        result = XrayCelParser.evaluateForValue("toUpper(substr(text, 0, 3))", context);
        assertEquals("HEL", result);
        
        // len(base64(text))
        result = XrayCelParser.evaluateForValue("len(base64(text))", context);
        assertTrue((int) result > 0);
        
        // 三层嵌套: toUpper(reverse(substr(text, 0, 3)))
        result = XrayCelParser.evaluateForValue("toUpper(reverse(substr(text, 0, 3)))", context);
        assertEquals("LEH", result);
    }
    
    @Test
    @DisplayName("测试属性访问")
    public void testPropertyAccess() {
        // 简单属性
        Object result = XrayCelParser.evaluateForValue("response.status", context);
        assertEquals(200, result);
        
        result = XrayCelParser.evaluateForValue("response.body", context);
        assertEquals("admin login success", result);
        
        // 嵌套属性
        result = XrayCelParser.evaluateForValue("response.headers", context);
        assertNotNull(result);
        assertTrue(result instanceof Map);
    }
    
    @Test
    @DisplayName("测试数组索引访问")
    public void testArrayAccess() {
        List<String> items = Arrays.asList("item1", "item2", "item3");
        context.put("items", items);
        
        Object result = XrayCelParser.evaluateForValue("items[0]", context);
        assertEquals("item1", result);
        
        result = XrayCelParser.evaluateForValue("items[2]", context);
        assertEquals("item3", result);
        
        // Map 键访问
        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        context.put("data", data);
        
        result = XrayCelParser.evaluateForValue("data[\"key1\"]", context);
        assertEquals("value1", result);
    }
    
    @Test
    @DisplayName("测试切片语法")
    public void testSliceSyntax() {
        List<Integer> numbers = Arrays.asList(0, 1, 2, 3, 4, 5);
        context.put("numbers", numbers);
        
        // [1:4] -> [1, 2, 3]
        Object result = XrayCelParser.evaluateForValue("numbers[1:4]", context);
        assertNotNull(result);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Integer> slice = (List<Integer>) result;
        assertEquals(3, slice.size());
        assertEquals(1, slice.get(0));
        assertEquals(3, slice.get(2));
        
        // [:3] -> [0, 1, 2]
        result = XrayCelParser.evaluateForValue("numbers[:3]", context);
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Integer> slice2 = (List<Integer>) result;
        assertEquals(3, slice2.size());
        
        // [2:] -> [2, 3, 4, 5]
        result = XrayCelParser.evaluateForValue("numbers[2:]", context);
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<Integer> slice3 = (List<Integer>) result;
        assertEquals(4, slice3.size());
    }
    
    @Test
    @DisplayName("测试三元运算符")
    public void testTernaryOperator() {
        context.put("score", 85);
        
        // 简单三元
        Object result = XrayCelParser.evaluateForValue("score > 80 ? \"good\" : \"bad\"", context);
        assertEquals("good", result);
        
        context.put("score", 60);
        result = XrayCelParser.evaluateForValue("score > 80 ? \"good\" : \"bad\"", context);
        assertEquals("bad", result);
        
        // 嵌套三元
        context.put("score", 95);
        result = XrayCelParser.evaluateForValue("score > 90 ? \"excellent\" : (score > 70 ? \"good\" : \"bad\")", context);
        assertEquals("excellent", result);
        
        context.put("score", 75);
        result = XrayCelParser.evaluateForValue("score > 90 ? \"excellent\" : (score > 70 ? \"good\" : \"bad\")", context);
        assertEquals("good", result);
    }
    
    @Test
    @DisplayName("测试数学表达式")
    public void testMathExpressions() {
        context.put("a", 10);
        context.put("b", 5);
        context.put("c", 2);
        
        // 基本运算
        Object result = XrayCelParser.evaluateForValue("a + b", context);
        assertEquals(15.0, ((Number) result).doubleValue(), 0.001);
        
        result = XrayCelParser.evaluateForValue("a - b", context);
        assertEquals(5.0, ((Number) result).doubleValue(), 0.001);
        
        result = XrayCelParser.evaluateForValue("a * c", context);
        assertEquals(20.0, ((Number) result).doubleValue(), 0.001);
        
        result = XrayCelParser.evaluateForValue("a / b", context);
        assertEquals(2.0, ((Number) result).doubleValue(), 0.001);
        
        // 复合运算
        result = XrayCelParser.evaluateForValue("a + b * c", context);
        assertEquals(20.0, ((Number) result).doubleValue(), 0.001);
        
        result = XrayCelParser.evaluateForValue("(a + b) * c", context);
        assertEquals(30.0, ((Number) result).doubleValue(), 0.001);
    }
    
    @Test
    @DisplayName("测试复杂 CEL 表达式")
    public void testComplexCelExpressions() {
        // 复合条件
        assertTrue(XrayCelParser.evaluateSingle("response.status == 200", context));
        assertTrue(XrayCelParser.evaluateLogical("response.status == 200 && len(response.body) > 10", context));
        
        // 函数与条件结合
        context.put("text", "admin");
        assertTrue(XrayCelParser.evaluateLogical("contains(response.body, text)", context));
        assertTrue(XrayCelParser.evaluateLogical("response.status == 200 && contains(response.body, \"admin\")", context));
        
        // 多层逻辑
        assertTrue(XrayCelParser.evaluateLogical(
            "(response.status == 200 || response.status == 201) && contains(response.body, \"success\")", 
            context
        ));
    }
    
    @Test
    @DisplayName("测试字符串操作函数")
    public void testStringOperations() {
        context.put("str", "Hello World");
        
        // contains
        assertTrue((Boolean) XrayCelParser.evaluateForValue("contains(str, \"World\")", context));
        assertFalse((Boolean) XrayCelParser.evaluateForValue("contains(str, \"test\")", context));
        
        // startsWith
        assertTrue((Boolean) XrayCelParser.evaluateForValue("startsWith(str, \"Hello\")", context));
        
        // endsWith
        assertTrue((Boolean) XrayCelParser.evaluateForValue("endsWith(str, \"World\")", context));
        
        // replaceAll
        Object result = XrayCelParser.evaluateForValue("replaceAll(str, \"World\", \"Java\")", context);
        assertEquals("Hello Java", result);
        
        // repeat
        result = XrayCelParser.evaluateForValue("repeat(\"ab\", 3)", context);
        assertEquals("ababab", result);
    }
    
    @Test
    @DisplayName("测试正则表达式")
    public void testRegex() {
        context.put("text", "token=abc123def");
        
        // matches
        assertTrue((Boolean) XrayCelParser.evaluateForValue("matches(text, \".*token=.*\")", context));
        
        // submatch
        Object result = XrayCelParser.evaluateForValue("submatch(text, \"token=(\\\\w+)\")", context);
        assertNotNull(result);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<String> matches = (List<String>) result;
        assertEquals(1, matches.size());
        assertEquals("abc123def", matches.get(0));
    }
    
    @Test
    @DisplayName("测试随机函数")
    public void testRandomFunctions() {
        // randomInt
        Object result = XrayCelParser.evaluateForValue("randomInt(1, 10)", context);
        assertNotNull(result);
        assertTrue(result instanceof Number);
        int random = ((Number) result).intValue();
        assertTrue(random >= 1 && random <= 10);
        
        // randomLowercase
        result = XrayCelParser.evaluateForValue("randomLowercase(10)", context);
        assertNotNull(result);
        assertEquals(10, result.toString().length());
        
        // randomUUID
        result = XrayCelParser.evaluateForValue("randomUUID()", context);
        assertNotNull(result);
        assertTrue(result.toString().matches("[0-9a-f-]+"));
    }
    
    @Test
    @DisplayName("测试高级列表操作")
    public void testAdvancedListOperations() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        context.put("numbers", numbers);
        
        // size
        Object result = XrayCelParser.evaluateForValue("size(numbers)", context);
        assertEquals(5, result);
        
        // first
        result = XrayCelParser.evaluateForValue("first(numbers)", context);
        assertEquals(1, result);
        
        // last
        result = XrayCelParser.evaluateForValue("last(numbers)", context);
        assertEquals(5, result);
        
        // sum
        result = XrayCelParser.evaluateForValue("sum(numbers)", context);
        assertEquals(15.0, ((Number) result).doubleValue(), 0.001);
        
        // max
        result = XrayCelParser.evaluateForValue("max(numbers)", context);
        assertEquals(5, result);
        
        // min
        result = XrayCelParser.evaluateForValue("min(numbers)", context);
        assertEquals(1, result);
    }
    
    @Test
    @DisplayName("测试规则调用表达式")
    public void testRuleExpressions() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("r0", true);
        rules.put("r1", true);
        rules.put("r2", false);
        
        Map<String, Object> ruleContext = new HashMap<>();
        ruleContext.putAll(rules);
        
        // 简单规则调用
        assertTrue(XrayCelParser.evaluateSingle("r0()", ruleContext));
        assertFalse(XrayCelParser.evaluateSingle("r2()", ruleContext));
        
        // 规则组合
        assertTrue(XrayCelParser.evaluateLogical("r0() && r1()", ruleContext));
        assertFalse(XrayCelParser.evaluateLogical("r0() && r2()", ruleContext));
        assertTrue(XrayCelParser.evaluateLogical("r0() || r2()", ruleContext));
    }
    
    @Test
    @DisplayName("测试语法验证")
    public void testSyntaxValidation() {
        // 正确的语法
        assertDoesNotThrow(() -> XrayCelParser.validateCelSyntax("response.status == 200"));
        assertDoesNotThrow(() -> XrayCelParser.validateCelSyntax("len(response.body) > 10"));
        assertDoesNotThrow(() -> XrayCelParser.validateCelSyntax("a && b || c"));
        
        // 错误的语法
        assertThrows(IllegalArgumentException.class, () -> 
            XrayCelParser.validateCelSyntax("(a + b"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            XrayCelParser.validateCelSyntax("a + b)"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            XrayCelParser.validateCelSyntax(""));
    }
    
    @Test
    @DisplayName("测试边界条件")
    public void testBoundaryConditions() {
        // 空上下文
        Map<String, Object> emptyContext = new HashMap<>();
        assertFalse(XrayCelParser.evaluateSingle("unknown == 1", emptyContext));
        
        // null 值
        context.put("nullValue", null);
        assertFalse(XrayCelParser.evaluateSingle("nullValue == 1", context));
        
        // 空字符串
        assertFalse(XrayCelParser.evaluateSingle("", context));
        assertNull(XrayCelParser.evaluateForValue("", context));
    }
    
    @Test
    @DisplayName("复杂嵌套场景 - 真实 POC 模拟")
    public void testRealPocScenarios() {
        // 模拟 SQL 注入时间盲注检测
        context.put("r0latency", 100);
        context.put("sleepSecond1", 7);
        
        Map<String, Object> response1 = new HashMap<>();
        response1.put("latency", 7200);
        response1.put("body", "statistics offworkstate");
        context.put("response", response1);
        
        // 检查延时是否符合预期
        boolean result = XrayCelParser.evaluateLogical(
            "response.latency - r0latency >= sleepSecond1 * 1000 - 1000 && contains(response.body, \"statistics\")", 
            context
        );
        assertTrue(result);
        
        // 模拟弱密码检测
        Map<String, Object> loginResponse = new HashMap<>();
        loginResponse.put("status", 200);
        loginResponse.put("body", "window.location main.html");
        context.put("response", loginResponse);
        
        result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"window.location\") && contains(response.body, \"main.html\")", 
            context
        );
        assertTrue(result);
    }
}



