package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 字符串函数测试
 * 测试所有 Xray 字符串操作函数
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray 字符串函数测试")
public class StringFunctionsTest {
    
    @Test
    @DisplayName("测试 len() - 字符串长度")
    public void testLen() {
        assertEquals(5, StringFunctions.len("hello"));
        assertEquals(0, StringFunctions.len(""));
        assertEquals(0, StringFunctions.len(null));
        assertEquals(7, StringFunctions.len("测试中文123")); // 4个中文字符 + 3个数字 = 7
        
        // 测试集合长度
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertEquals(3, StringFunctions.len(list));
        
        // 测试 Map 长度
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, StringFunctions.len(map));
    }
    
    @Test
    @DisplayName("测试 substr() - 子字符串提取")
    public void testSubstr() {
        assertEquals("hel", StringFunctions.substr("hello", 0, 3));
        assertEquals("orl", StringFunctions.substr("world", 1, 3));
        assertEquals("st", StringFunctions.substr("test", 2, 10));
        assertEquals("", StringFunctions.substr("test", 10, 5));
        assertEquals("", StringFunctions.substr(null, 0, 5));
        
        // 边界测试
        assertEquals("hello", StringFunctions.substr("hello", 0, 100));
        assertEquals("", StringFunctions.substr("test", -1, 3));
    }
    
    @Test
    @DisplayName("测试 replaceAll() - 字符串替换")
    public void testReplaceAll() {
        assertEquals("hell0 w0rld", StringFunctions.replaceAll("hello world", "o", "0"));
        assertEquals("TEST TEST", StringFunctions.replaceAll("test test", "test", "TEST"));
        assertEquals("abc", StringFunctions.replaceAll("abc", "x", "y"));
        assertEquals("", StringFunctions.replaceAll("", "a", "b"));
        
        // 空值测试
        assertEquals("", StringFunctions.replaceAll(null, "a", "b"));
        assertEquals("test", StringFunctions.replaceAll("test", null, "x"));
        assertEquals("test", StringFunctions.replaceAll("test", "", "x"));
    }
    
    @Test
    @DisplayName("测试 toUpper() 和 toLower() - 大小写转换")
    public void testCaseConversion() {
        assertEquals("HELLO", StringFunctions.toUpper("hello"));
        assertEquals("TEST123", StringFunctions.toUpper("Test123"));
        assertEquals("", StringFunctions.toUpper(null));
        
        assertEquals("hello", StringFunctions.toLower("HELLO"));
        assertEquals("test123", StringFunctions.toLower("Test123"));
        assertEquals("", StringFunctions.toLower(null));
    }
    
    @Test
    @DisplayName("测试 repeat() - 字符串重复")
    public void testRepeat() {
        assertEquals("ababab", StringFunctions.repeat("ab", 3));
        assertEquals("xxxxx", StringFunctions.repeat("x", 5));
        assertEquals("", StringFunctions.repeat("test", 0));
        assertEquals("", StringFunctions.repeat(null, 5));
        assertEquals("", StringFunctions.repeat("test", -1));
    }
    
    @Test
    @DisplayName("测试 reverse() - 字符串反转")
    public void testReverse() {
        assertEquals("olleh", StringFunctions.reverse("hello"));
        assertEquals("54321", StringFunctions.reverse("12345"));
        assertEquals("", StringFunctions.reverse(""));
        assertEquals("", StringFunctions.reverse(null));
        assertEquals("文中试测", StringFunctions.reverse("测试中文"));
    }
    
    @Test
    @DisplayName("测试 printable() - 可打印字符转换")
    public void testPrintable() {
        assertEquals("hello", StringFunctions.printable("hello"));
        assertEquals("test\\x0a\\x0d", StringFunctions.printable("test\n\r"));
        assertEquals("\\x01\\x02\\x03", StringFunctions.printable("\u0001\u0002\u0003"));
        
        // 测试 ASCII 范围
        String result = StringFunctions.printable("A\u0000B");
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.contains("\\x00"));
    }
    
    @Test
    @DisplayName("测试 contains() - 包含检查")
    public void testContains() {
        assertTrue(StringFunctions.contains("hello world", "world"));
        assertFalse(StringFunctions.contains("test", "abc"));
        assertFalse(StringFunctions.contains(null, "test"));
        assertFalse(StringFunctions.contains("test", null));
        assertTrue(StringFunctions.contains("测试中文", "中文"));
    }
    
    @Test
    @DisplayName("测试 startsWith() 和 endsWith() - 前后缀检查")
    public void testPrefixSuffix() {
        assertTrue(StringFunctions.startsWith("hello world", "hello"));
        assertFalse(StringFunctions.startsWith("test", "abc"));
        assertFalse(StringFunctions.startsWith(null, "test"));
        
        assertTrue(StringFunctions.endsWith("hello world", "world"));
        assertFalse(StringFunctions.endsWith("test", "abc"));
        assertFalse(StringFunctions.endsWith(null, "test"));
    }
    
    @Test
    @DisplayName("测试 indexOf() - 位置查找")
    public void testIndexOf() {
        assertEquals(6, StringFunctions.indexOf("hello world", "world"));
        assertEquals(-1, StringFunctions.indexOf("test", "abc"));
        assertEquals(-1, StringFunctions.indexOf(null, "test"));
        assertEquals(0, StringFunctions.indexOf("test", "test"));
    }
    
    @Test
    @DisplayName("测试 submatch() - 正则提取")
    public void testSubmatch() {
        // 基本提取
        List<String> result = StringFunctions.submatch("token=abc123", "token=(\\w+)");
        assertEquals(1, result.size());
        assertEquals("abc123", result.get(0));
        
        // 多个捕获组
        result = StringFunctions.submatch("user:admin pass:123", "user:(\\w+) pass:(\\w+)");
        assertEquals(2, result.size());
        assertEquals("admin", result.get(0));
        assertEquals("123", result.get(1));
        
        // 无匹配
        result = StringFunctions.submatch("no match", "\\d+");
        assertTrue(result.isEmpty());
        
        // 空值测试
        result = StringFunctions.submatch(null, "test");
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("测试 matches() - 正则匹配")
    public void testMatches() {
        assertTrue(StringFunctions.matches("test123", "\\w+"));
        assertFalse(StringFunctions.matches("abc", "\\d+"));
        assertTrue(StringFunctions.matches("12345", "\\d+"));
        assertFalse(StringFunctions.matches(null, "test"));
        
        // 复杂正则
        assertTrue(StringFunctions.matches("admin@example.com", "\\w+@\\w+\\.\\w+"));
    }
    
    @Test
    @DisplayName("测试 trim() - 去除空白")
    public void testTrim() {
        assertEquals("hello", StringFunctions.trim("  hello  "));
        assertEquals("test", StringFunctions.trim("\n\ttest\r\n"));
        assertEquals("", StringFunctions.trim("   "));
        assertEquals("", StringFunctions.trim(null));
    }
    
    @Test
    @DisplayName("测试 split() - 字符串分割")
    public void testSplit() {
        List<String> result = StringFunctions.split("a,b,c", ",");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        
        result = StringFunctions.split("hello world", " ");
        assertEquals(2, result.size());
        assertEquals("hello", result.get(0));
        assertEquals("world", result.get(1));
        
        // 特殊分隔符
        result = StringFunctions.split("a|b|c", "|");
        assertEquals(3, result.size());
        
        // 空值测试
        result = StringFunctions.split(null, ",");
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("复杂场景 - 链式操作")
    public void testComplexScenarios() {
        // 场景1: 提取并转换
        String input = "TOKEN=ABC123DEF";
        List<String> matches = StringFunctions.submatch(input, "TOKEN=(\\w+)");
        String token = matches.get(0);
        String lower = StringFunctions.toLower(token);
        assertEquals("abc123def", lower);
        
        // 场景2: 重复并替换
        String repeated = StringFunctions.repeat("ab", 3);
        String replaced = StringFunctions.replaceAll(repeated, "b", "X");
        assertEquals("aXaXaX", replaced);
        
        // 场景3: 提取子串并反转
        String sub = StringFunctions.substr("hello world", 0, 5);
        String reversed = StringFunctions.reverse(sub);
        assertEquals("olleh", reversed);
    }
}




