package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.ReverseObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuntimeExpressionEvaluator 单元测试
 */
@DisplayName("运行时表达式评估器测试")
public class RuntimeExpressionEvaluatorTest {

    @Test
    @DisplayName("测试运行时表达式检测")
    public void testIsRuntimeExpression() {
        assertTrue(RuntimeExpressionEvaluator.isRuntimeExpression("@@expression:newReverse()"));
        assertTrue(RuntimeExpressionEvaluator.isRuntimeExpression("@@expression:reverse.url"));
        assertFalse(RuntimeExpressionEvaluator.isRuntimeExpression("simple_value"));
        assertFalse(RuntimeExpressionEvaluator.isRuntimeExpression(null));
        assertFalse(RuntimeExpressionEvaluator.isRuntimeExpression(""));
    }

    @Test
    @DisplayName("测试获取表达式内容")
    public void testGetExpressionContent() {
        assertEquals("newReverse()", 
            RuntimeExpressionEvaluator.getExpressionContent("@@expression:newReverse()"));
        assertEquals("reverse.url", 
            RuntimeExpressionEvaluator.getExpressionContent("@@expression:reverse.url"));
        assertEquals("simple_value", 
            RuntimeExpressionEvaluator.getExpressionContent("simple_value"));
    }

    @Test
    @DisplayName("测试变量保留判断")
    public void testShouldPreserveVariable() {
        assertTrue(RuntimeExpressionEvaluator.shouldPreserveVariable("@@expression:newReverse()"));
        assertTrue(RuntimeExpressionEvaluator.shouldPreserveVariable("@@expression:reverse.url"));
        assertTrue(RuntimeExpressionEvaluator.shouldPreserveVariable("@@expression:newHTTPRequest()"));
        assertFalse(RuntimeExpressionEvaluator.shouldPreserveVariable("simple_value"));
        assertFalse(RuntimeExpressionEvaluator.shouldPreserveVariable(null));
    }

    @Test
    @DisplayName("测试对象创建表达式检测")
    public void testIsObjectCreationExpression() {
        assertTrue(RuntimeExpressionEvaluator.isObjectCreationExpression("newReverse()"));
        assertTrue(RuntimeExpressionEvaluator.isObjectCreationExpression("newHTTPRequest()"));
        assertFalse(RuntimeExpressionEvaluator.isObjectCreationExpression("reverse.url"));
        assertFalse(RuntimeExpressionEvaluator.isObjectCreationExpression(null));
    }

    @Test
    @DisplayName("测试属性访问表达式检测")
    public void testIsPropertyAccessExpression() {
        assertTrue(RuntimeExpressionEvaluator.isPropertyAccessExpression("reverse.url"));
        assertTrue(RuntimeExpressionEvaluator.isPropertyAccessExpression("response.body"));
        assertFalse(RuntimeExpressionEvaluator.isPropertyAccessExpression("newReverse()"));
        assertFalse(RuntimeExpressionEvaluator.isPropertyAccessExpression(null));
    }

    @Test
    @DisplayName("测试简单变量评估")
    public void testEvaluateSimpleVariables() {
        Map<String, List<String>> pocVariables = new HashMap<>();
        pocVariables.put("var1", Collections.singletonList("value1"));
        pocVariables.put("var2", Collections.singletonList("value2"));

        Map<String, Object> result = RuntimeExpressionEvaluator.evaluateVariables(pocVariables);

        assertEquals("value1", result.get("var1"));
        assertEquals("value2", result.get("var2"));
    }

    @Test
    @DisplayName("测试空变量处理")
    public void testEvaluateNullVariables() {
        Map<String, Object> result = RuntimeExpressionEvaluator.evaluateVariables(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = RuntimeExpressionEvaluator.evaluateVariables(new HashMap<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试 ReverseObject 属性获取")
    public void testGetObjectPropertyForReverseObject() {
        // 创建模拟的 ReverseObject
        ReverseObject reverse = new ReverseObject();
        
        // 测试获取 url 属性
        Object url = RuntimeExpressionEvaluator.getObjectProperty(reverse, "url");
        assertNotNull(url);
        assertTrue(url.toString().contains("http"));
        
        // 测试获取 domain 属性
        Object domain = RuntimeExpressionEvaluator.getObjectProperty(reverse, "domain");
        assertNotNull(domain);
    }

    @Test
    @DisplayName("测试空对象属性获取")
    public void testGetObjectPropertyNull() {
        assertNull(RuntimeExpressionEvaluator.getObjectProperty(null, "url"));
        assertNull(RuntimeExpressionEvaluator.getObjectProperty(new Object(), null));
    }

    @Test
    @DisplayName("测试通用反射属性获取")
    public void testGetObjectPropertyReflection() {
        // 使用一个简单的测试对象
        TestBean bean = new TestBean("test", 123);
        
        assertEquals("test", RuntimeExpressionEvaluator.getObjectProperty(bean, "name"));
        assertEquals(123, RuntimeExpressionEvaluator.getObjectProperty(bean, "value"));
    }

    /**
     * 测试用的简单 Bean
     */
    public static class TestBean {
        private String name;
        private int value;

        public TestBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
    }
}
