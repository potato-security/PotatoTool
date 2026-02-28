package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 编码函数测试
 * 测试所有 Xray 编码/解码函数
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray 编码函数测试")
public class EncodingFunctionsTest {
    
    @Test
    @DisplayName("测试 Base64 编码和解码")
    public void testBase64() {
        // 基本测试
        String encoded = EncodingFunctions.base64("test");
        assertEquals("dGVzdA==", encoded);
        
        String decoded = EncodingFunctions.base64Decode("dGVzdA==");
        assertEquals("test", decoded);
        
        // 复杂字符串
        encoded = EncodingFunctions.base64("admin:password");
        assertEquals("YWRtaW46cGFzc3dvcmQ=", encoded);
        
        decoded = EncodingFunctions.base64Decode("YWRtaW46cGFzc3dvcmQ=");
        assertEquals("admin:password", decoded);
        
        // 中文测试
        encoded = EncodingFunctions.base64("测试中文");
        decoded = EncodingFunctions.base64Decode(encoded);
        assertEquals("测试中文", decoded);
        
        // 空值测试
        assertEquals("", EncodingFunctions.base64(null));
        assertEquals("", EncodingFunctions.base64Decode(null));
        assertEquals("", EncodingFunctions.base64Decode(""));
    }
    
    @Test
    @DisplayName("测试 Base64 编码解码往返")
    public void testBase64RoundTrip() {
        String[] testCases = {
            "hello world",
            "admin@123",
            "特殊字符!@#$%^&*()",
            "长文本测试Long text test with 中文 characters",
            "a",
            "12345",
            ""
        };
        
        for (String original : testCases) {
            String encoded = EncodingFunctions.base64(original);
            String decoded = EncodingFunctions.base64Decode(encoded);
            assertEquals(original, decoded, "Base64 往返失败: " + original);
        }
    }
    
    @Test
    @DisplayName("测试 URL 编码和解码")
    public void testUrlEncode() {
        // 基本测试
        String encoded = EncodingFunctions.urlencode("hello world");
        assertTrue(encoded.contains("hello") && encoded.contains("world"));
        
        String decoded = EncodingFunctions.urldecode(encoded);
        assertEquals("hello world", decoded);
        
        // 特殊字符
        encoded = EncodingFunctions.urlencode("test=1&key=2");
        assertTrue(encoded.contains("%3D") || encoded.contains("%26"));
        
        decoded = EncodingFunctions.urldecode(encoded);
        assertEquals("test=1&key=2", decoded);
        
        // 中文测试
        encoded = EncodingFunctions.urlencode("测试");
        decoded = EncodingFunctions.urldecode(encoded);
        assertEquals("测试", decoded);
        
        // 空值测试
        assertEquals("", EncodingFunctions.urlencode(null));
        assertEquals("", EncodingFunctions.urldecode(null));
    }
    
    @Test
    @DisplayName("测试 URL 编码解码往返")
    public void testUrlEncodeRoundTrip() {
        String[] testCases = {
            "hello world",
            "test=1&key=2",
            "admin@example.com",
            "path/to/file",
            "query?param=value",
            "测试中文",
            "special!@#$%"
        };
        
        for (String original : testCases) {
            String encoded = EncodingFunctions.urlencode(original);
            String decoded = EncodingFunctions.urldecode(encoded);
            assertEquals(original, decoded, "URL 编码往返失败: " + original);
        }
    }
    
    @Test
    @DisplayName("测试十六进制编码和解码")
    public void testHexEncode() {
        // 基本测试
        String encoded = EncodingFunctions.hexEncode("test");
        assertEquals("74657374", encoded);
        
        String decoded = EncodingFunctions.hexDecode("74657374");
        assertEquals("test", decoded);
        
        // 更多测试
        encoded = EncodingFunctions.hexEncode("admin");
        assertEquals("61646d696e", encoded);
        
        decoded = EncodingFunctions.hexDecode("61646d696e");
        assertEquals("admin", decoded);
        
        // 空值测试
        assertEquals("", EncodingFunctions.hexEncode(null));
        assertEquals("", EncodingFunctions.hexDecode(null));
    }
    
    @Test
    @DisplayName("测试十六进制编码解码往返")
    public void testHexEncodeRoundTrip() {
        String[] testCases = {
            "test",
            "admin",
            "password123",
            "hello world",
            "测试",
            "!@#$%^&*()",
            "a",
            "ABC"
        };
        
        for (String original : testCases) {
            String encoded = EncodingFunctions.hexEncode(original);
            String decoded = EncodingFunctions.hexDecode(encoded);
            assertEquals(original, decoded, "十六进制往返失败: " + original);
        }
    }
    
    @Test
    @DisplayName("测试十六进制解码异常处理")
    public void testHexDecodeError() {
        // 奇数长度
        String result = EncodingFunctions.hexDecode("123");
        assertEquals("", result);
        
        // 非法字符
        result = EncodingFunctions.hexDecode("GHIJ");
        assertEquals("", result);
    }
    
    @Test
    @DisplayName("测试 HTML 实体编码和解码")
    public void testHtmlEscape() {
        // 基本测试
        String encoded = EncodingFunctions.htmlEscape("<script>");
        assertEquals("&lt;script&gt;", encoded);
        
        String decoded = EncodingFunctions.htmlUnescape("&lt;script&gt;");
        assertEquals("<script>", decoded);
        
        // 多个特殊字符
        encoded = EncodingFunctions.htmlEscape("a & b");
        assertEquals("a &amp; b", encoded);
        
        decoded = EncodingFunctions.htmlUnescape("a &amp; b");
        assertEquals("a & b", decoded);
        
        // 所有特殊字符
        encoded = EncodingFunctions.htmlEscape("<>&\"'/");
        assertTrue(encoded.contains("&lt;"));
        assertTrue(encoded.contains("&gt;"));
        assertTrue(encoded.contains("&amp;"));
        
        // 空值测试
        assertEquals("", EncodingFunctions.htmlEscape(null));
        assertEquals("", EncodingFunctions.htmlUnescape(null));
    }
    
    @Test
    @DisplayName("测试 HTML 编码解码往返")
    public void testHtmlEscapeRoundTrip() {
        String[] testCases = {
            "<script>alert('xss')</script>",
            "a & b",
            "<div>content</div>",
            "\"quoted\"",
            "'single'",
            "path/to/file"
        };
        
        for (String original : testCases) {
            String encoded = EncodingFunctions.htmlEscape(original);
            String decoded = EncodingFunctions.htmlUnescape(encoded);
            assertEquals(original, decoded, "HTML 编码往返失败: " + original);
        }
    }
    
    @Test
    @DisplayName("复杂场景 - 多重编码")
    public void testComplexEncoding() {
        // 场景1: Base64(Hex(text))
        String original = "admin";
        String hex = EncodingFunctions.hexEncode(original);
        String base64 = EncodingFunctions.base64(hex);
        String decoded1 = EncodingFunctions.base64Decode(base64);
        String decoded2 = EncodingFunctions.hexDecode(decoded1);
        assertEquals(original, decoded2);
        
        // 场景2: URL(Base64(text))
        original = "test data";
        String b64 = EncodingFunctions.base64(original);
        String url = EncodingFunctions.urlencode(b64);
        String urlDec = EncodingFunctions.urldecode(url);
        String b64Dec = EncodingFunctions.base64Decode(urlDec);
        assertEquals(original, b64Dec);
    }
    
    @Test
    @DisplayName("性能测试 - 大数据编码")
    public void testPerformance() {
        // 生成大字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("test data ").append(i).append(" ");
        }
        String largeText = sb.toString();
        
        // Base64 编码解码
        long start = System.currentTimeMillis();
        String encoded = EncodingFunctions.base64(largeText);
        String decoded = EncodingFunctions.base64Decode(encoded);
        long time = System.currentTimeMillis() - start;
        
        assertEquals(largeText, decoded);
        assertTrue(time < 1000, "编码解码耗时过长: " + time + "ms");
    }
}




