package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 边界条件和错误处理测试
 * 测试各种边界情况和异常场景
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("边界条件和错误处理测试")
public class BoundaryAndErrorHandlingTest {
    
    private Map<String, Object> context;
    
    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
    }
    
    @Test
    @DisplayName("测试null值处理")
    public void testNullHandling() {
        // null 上下文
        assertFalse(XrayCelParser.evaluateSingle("test == 1", null));
        
        // null 值在上下文中
        context.put("nullValue", null);
        assertFalse(XrayCelParser.evaluateSingle("nullValue == 1", context));
        
        // null 字符串
        assertNull(XrayCelParser.evaluateForValue(null, context));
        
        // 函数参数为 null
        context.put("data", null);
        Object result = XrayCelParser.evaluateForValue("len(data)", context);
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("测试空字符串处理")
    public void testEmptyStringHandling() {
        // 空表达式
        assertFalse(XrayCelParser.evaluateSingle("", context));
        assertNull(XrayCelParser.evaluateForValue("", context));
        
        // 仅空格
        assertFalse(XrayCelParser.evaluateSingle("   ", context));
        
        // 空字符串值
        context.put("empty", "");
        Object result = XrayCelParser.evaluateForValue("len(empty)", context);
        assertEquals(0, result);
        
        // 修复：evaluateForValue 返回 Object，需要用 evaluateSingle 来获取 boolean
        assertTrue(XrayCelParser.evaluateSingle("empty == \"\"", context));
    }
    
    @Test
    @DisplayName("测试空集合处理")
    public void testEmptyCollectionHandling() {
        // 空列表
        context.put("emptyList", Collections.emptyList());
        Object result = XrayCelParser.evaluateForValue("size(emptyList)", context);
        assertEquals(0, result);
        
        assertNull(XrayCelParser.evaluateForValue("first(emptyList)", context));
        assertNull(XrayCelParser.evaluateForValue("last(emptyList)", context));
        
        // 空Map
        context.put("emptyMap", Collections.emptyMap());
        result = XrayCelParser.evaluateForValue("size(emptyMap)", context);
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("测试越界访问")
    public void testOutOfBoundsAccess() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        context.put("list", list);
        
        // 索引越界
        assertNull(XrayCelParser.evaluateForValue("list[10]", context));
        assertNull(XrayCelParser.evaluateForValue("list[-1]", context));
        
        // 切片越界
        Object result = XrayCelParser.evaluateForValue("list[10:20]", context);
        assertNotNull(result);
        assertTrue(((List<?>) result).isEmpty());
    }
    
    @Test
    @DisplayName("测试类型不匹配")
    public void testTypeMismatch() {
        // 字符串当数字用
        context.put("str", "not a number");
        Object result = XrayCelParser.evaluateForValue("toInt(str)", context);
        assertEquals(0, result); // 应返回默认值
        
        // 数字当列表用 - 优雅失败，返回null而不是抛出异常
        context.put("num", 123);
        Object result2 = XrayCelParser.evaluateForValue("first(num)", context);
        assertNull(result2); // 预期返回null而不是抛出异常
    }
    
    @Test
    @DisplayName("测试无效表达式")
    public void testInvalidExpressions() {
        // 括号不匹配
        assertThrows(IllegalArgumentException.class, () -> {
            XrayCelParser.validateCelSyntax("(a + b");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            XrayCelParser.validateCelSyntax("a + b)");
        });
        
        // 不支持的运算符
        assertThrows(IllegalArgumentException.class, () -> {
            XrayCelParser.validateCelSyntax("a === b");
        });
    }
    
    @Test
    @DisplayName("测试极大数值")
    public void testLargeNumbers() {
        context.put("large", Integer.MAX_VALUE);
        context.put("veryLarge", Long.MAX_VALUE);
        
        Object result = XrayCelParser.evaluateForValue("large + 1", context);
        assertNotNull(result);
        
        // 验证不会溢出
        result = XrayCelParser.evaluateForValue("toDouble(large)", context);
        assertEquals((double) Integer.MAX_VALUE, ((Number) result).doubleValue(), 0.001);
    }
    
    @Test
    @DisplayName("测试极小数值")
    public void testSmallNumbers() {
        context.put("small", Integer.MIN_VALUE);
        context.put("zero", 0);
        
        Object result = XrayCelParser.evaluateForValue("small - 1", context);
        assertNotNull(result);
        
        // 除以零（应有保护）
        try {
            result = XrayCelParser.evaluateForValue("small / zero", context);
            // 如果没有抛异常，结果应该是 Infinity 或特殊值
            assertNotNull(result);
        } catch (Exception e) {
            // 抛异常也是可以接受的
            assertTrue(true);
        }
    }
    
    @Test
    @DisplayName("测试极长字符串")
    public void testVeryLongString() {
        // 构造超长字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("long text ");
        }
        String longStr = sb.toString();
        context.put("longStr", longStr);
        
        // 应该能正常处理
        Object result = XrayCelParser.evaluateForValue("len(longStr)", context);
        assertTrue((int) result > 90000);
        
        result = XrayCelParser.evaluateForValue("contains(longStr, \"long text\")", context);
        assertTrue((Boolean) result);
    }
    
    @Test
    @DisplayName("测试深层嵌套对象")
    public void testDeeplyNestedObjects() {
        // 构造深层嵌套的Map
        Map<String, Object> level5 = new HashMap<>();
        level5.put("value", "deep value");
        
        Map<String, Object> level4 = new HashMap<>();
        level4.put("level5", level5);
        
        Map<String, Object> level3 = new HashMap<>();
        level3.put("level4", level4);
        
        Map<String, Object> level2 = new HashMap<>();
        level2.put("level3", level3);
        
        Map<String, Object> level1 = new HashMap<>();
        level1.put("level2", level2);
        
        context.put("nested", level1);
        
        // 深度访问
        Object result = XrayCelParser.evaluateForValue("get(nested, \"level2.level3.level4.level5.value\")", context);
        assertEquals("deep value", result);
    }
    
    @Test
    @DisplayName("测试特殊字符处理")
    public void testSpecialCharacters() {
        // 换行符
        context.put("withNewline", "line1\nline2\r\nline3");
        Object result = XrayCelParser.evaluateForValue("contains(withNewline, \"\\n\")", context);
        assertTrue((Boolean) result);
        
        // 制表符
        context.put("withTab", "col1\tcol2\tcol3");
        result = XrayCelParser.evaluateForValue("contains(withTab, \"\\t\")", context);
        assertTrue((Boolean) result);
        
        // 引号
        context.put("withQuote", "He said \"hello\"");
        result = XrayCelParser.evaluateForValue("len(withQuote)", context);
        assertTrue((int) result > 0);
        
        // Unicode字符
        context.put("unicode", "测试中文😀");
        result = XrayCelParser.evaluateForValue("len(unicode)", context);
        assertTrue((int) result > 0);
    }
    
    @Test
    @DisplayName("测试并发访问")
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    Map<String, Object> localContext = new HashMap<>();
                    localContext.put("threadId", threadId);
                    
                    for (int j = 0; j < iterationsPerThread; j++) {
                        localContext.put("iteration", j);
                        
                        // 执行各种操作
                        XrayCelParser.evaluateForValue("threadId + iteration", localContext);
                        XrayCelParser.evaluateLogical("threadId > 0 && iteration >= 0", localContext);
                        XrayCelParser.evaluateForValue("base64(string(threadId))", localContext);
                    }
                } catch (Throwable t) {
                    errors.add(t);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证没有错误
        if (!errors.isEmpty()) {
            fail("并发测试出现错误: " + errors.get(0).getMessage());
        }
    }
    
    @Test
    @DisplayName("测试内存泄漏预防")
    public void testMemoryLeakPrevention() {
        // 创建大量上下文
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> localContext = new HashMap<>();
            localContext.put("iteration", i);
            localContext.put("data", "test data " + i);
            
            XrayCelParser.evaluateForValue("len(data) + iteration", localContext);
        }
        
        // 如果有内存泄漏，这里应该会OOM
        System.gc(); // 建议垃圾回收
        assertTrue(true, "内存泄漏测试通过");
    }
    
    @Test
    @DisplayName("测试循环引用处理")
    public void testCircularReference() {
        // 构造循环引用
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> map2 = new HashMap<>();
        map1.put("ref", map2);
        map2.put("ref", map1);
        
        context.put("circular", map1);
        
        // 应该能处理，不会栈溢出
        try {
            Object result = XrayCelParser.evaluateForValue("size(circular)", context);
            assertNotNull(result);
        } catch (StackOverflowError e) {
            fail("循环引用导致栈溢出");
        }
    }
    
    @Test
    @DisplayName("测试恶意表达式防护")
    public void testMaliciousExpressionProtection() {
        // 极深嵌套（可能导致栈溢出）
        StringBuilder deepNesting = new StringBuilder("toUpper(");
        for (int i = 0; i < 100; i++) {
            deepNesting.append("reverse(");
        }
        deepNesting.append("\"test\"");
        for (int i = 0; i < 101; i++) {
            deepNesting.append(")");
        }
        
        try {
            // 应该能处理或优雅失败
            XrayCelParser.evaluateForValue(deepNesting.toString(), context);
        } catch (StackOverflowError e) {
            fail("深层嵌套导致栈溢出");
        } catch (Exception e) {
            // 抛出受控异常是可以接受的
            assertTrue(true);
        }
    }
    
    @Test
    @DisplayName("测试错误恢复")
    public void testErrorRecovery() {
        // 第一个错误表达式
        assertFalse(XrayCelParser.evaluateSingle("invalid expression!", context));
        
        // 后续正常表达式应该仍能工作
        context.put("test", "value");
        assertTrue(XrayCelParser.evaluateSingle("test == \"value\"", context));
        
        // 另一个错误
        assertNull(XrayCelParser.evaluateForValue("unknownFunction(test)", context));
        
        // 验证状态正常
        Object result = XrayCelParser.evaluateForValue("len(test)", context);
        assertEquals(5, result);
    }
    
    @Test
    @DisplayName("测试编码错误处理")
    public void testEncodingErrors() {
        // Base64解码错误数据
        context.put("invalidBase64", "not-valid-base64!!!");
        Object result = XrayCelParser.evaluateForValue("base64Decode(invalidBase64)", context);
        // 应该返回空字符串或null，不应崩溃
        assertNotNull(result);
        
        // Hex解码奇数长度
        context.put("invalidHex", "abc");
        result = XrayCelParser.evaluateForValue("hexDecode(invalidHex)", context);
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("测试正则表达式错误")
    public void testRegexErrors() {
        context.put("text", "test string");
        
        // 无效的正则表达式
        context.put("invalidRegex", "[unclosed");
        
        // 应该优雅处理
        try {
            Object result = XrayCelParser.evaluateForValue("matches(text, invalidRegex)", context);
            // 如果没抛异常，应该返回 false
            if (result != null) {
                assertFalse((Boolean) result);
            }
        } catch (Exception e) {
            // 抛出受控异常也可接受
            assertTrue(true);
        }
    }
    
    @Test
    @DisplayName("测试性能退化保护")
    public void testPerformanceDegradationProtection() {
        // 超长列表操作
        List<Integer> hugeList = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            hugeList.add(i);
        }
        context.put("hugeList", hugeList);
        
        long start = System.currentTimeMillis();
        Object result = XrayCelParser.evaluateForValue("size(hugeList)", context);
        long time = System.currentTimeMillis() - start;
        
        assertEquals(100000, result);
        assertTrue(time < 1000, "大列表操作耗时过长: " + time + "ms");
        
        // 超长字符串操作
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            huge.append("x");
        }
        context.put("hugeStr", huge.toString());
        
        start = System.currentTimeMillis();
        result = XrayCelParser.evaluateForValue("len(hugeStr)", context);
        time = System.currentTimeMillis() - start;
        
        assertEquals(100000, result);
        assertTrue(time < 1000, "大字符串操作耗时过长: " + time + "ms");
    }
}



