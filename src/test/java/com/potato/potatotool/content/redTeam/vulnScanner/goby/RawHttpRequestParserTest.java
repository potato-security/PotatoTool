package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.potato.potatotool.content.redTeam.vulnScanner.http.RawHttpRequestParser;
import com.potato.potatotool.content.redTeam.vulnScanner.http.RawHttpRequestParser.ParsedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RawHttpRequestParser 原始HTTP报文解析器单元测试
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("RawHttpRequestParser 原始HTTP报文解析测试")
public class RawHttpRequestParserTest {
    
    @Test
    @DisplayName("测试基本GET请求解析")
    public void testBasicGetRequest() {
        String raw = "GET /api/users HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "User-Agent: Mozilla/5.0\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("/api/users", parsed.getPath());
        assertEquals("HTTP/1.1", parsed.getProtocol());
        assertEquals("example.com", parsed.getHeaders().get("Host"));
        assertEquals("Mozilla/5.0", parsed.getHeaders().get("User-Agent"));
        assertNull(parsed.getBody());
    }
    
    @Test
    @DisplayName("测试基本POST请求解析")
    public void testBasicPostRequest() {
        String raw = "POST /api/login HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Content-Type: application/x-www-form-urlencoded\r\n" +
                    "Content-Length: 27\r\n" +
                    "\r\n" +
                    "username=admin&password=123";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("POST", parsed.getMethod());
        assertEquals("/api/login", parsed.getPath());
        assertEquals("application/x-www-form-urlencoded", 
                    parsed.getHeaders().get("Content-Type"));
        assertEquals("username=admin&password=123", parsed.getBody());
    }
    
    @Test
    @DisplayName("测试JSON请求体解析")
    public void testJsonBodyRequest() {
        String raw = "POST /api/data HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Content-Type: application/json\r\n" +
                    "\r\n" +
                    "{\"key\":\"value\",\"number\":123}";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("POST", parsed.getMethod());
        assertEquals("/api/data", parsed.getPath());
        assertTrue(parsed.getBody().contains("\"key\":\"value\""));
        assertTrue(parsed.getBody().contains("\"number\":123"));
    }
    
    @Test
    @DisplayName("测试多行请求体解析")
    public void testMultilineBody() {
        String raw = "POST /api/upload HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "Line 1\n" +
                    "Line 2\n" +
                    "Line 3";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("POST", parsed.getMethod());
        assertTrue(parsed.getBody().contains("Line 1"));
        assertTrue(parsed.getBody().contains("Line 2"));
        assertTrue(parsed.getBody().contains("Line 3"));
    }
    
    @Test
    @DisplayName("测试请求路径带查询参数")
    public void testPathWithQueryString() {
        String raw = "GET /api/search?q=test&page=1 HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("/api/search?q=test&page=1", parsed.getPath());
    }
    
    @Test
    @DisplayName("测试请求头大小写处理")
    public void testHeaderCaseHandling() {
        String raw = "GET /test HTTP/1.1\r\n" +
                    "HOST: example.com\r\n" +
                    "content-type: text/html\r\n" +
                    "User-Agent: Test\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        // 保持原始大小写
        assertTrue(parsed.getHeaders().containsKey("HOST") ||
                  parsed.getHeaders().containsKey("Host"));
        assertTrue(parsed.getHeaders().containsKey("content-type") ||
                  parsed.getHeaders().containsKey("Content-Type"));
    }
    
    @Test
    @DisplayName("测试空请求头处理")
    public void testEmptyHeaders() {
        String raw = "GET /test HTTP/1.1\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("/test", parsed.getPath());
        assertTrue(parsed.getHeaders().isEmpty());
    }
    
    @Test
    @DisplayName("测试请求头值包含冒号")
    public void testHeaderValueWithColon() {
        String raw = "GET /test HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Authorization: Bearer token:with:colons\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("Bearer token:with:colons", 
                    parsed.getHeaders().get("Authorization"));
    }
    
    @Test
    @DisplayName("测试使用\\n分隔的请求")
    public void testLinuxLineEndings() {
        String raw = "GET /test HTTP/1.1\n" +
                    "Host: example.com\n" +
                    "User-Agent: Test\n" +
                    "\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("/test", parsed.getPath());
        assertEquals("example.com", parsed.getHeaders().get("Host"));
    }
    
    @Test
    @DisplayName("测试协议版本处理")
    public void testProtocolVersion() {
        // HTTP/1.0
        String raw1 = "GET /test HTTP/1.0\r\n\r\n";
        ParsedRequest parsed1 = RawHttpRequestParser.parse(raw1);
        assertEquals("HTTP/1.0", parsed1.getProtocol());
        
        // HTTP/1.1
        String raw2 = "GET /test HTTP/1.1\r\n\r\n";
        ParsedRequest parsed2 = RawHttpRequestParser.parse(raw2);
        assertEquals("HTTP/1.1", parsed2.getProtocol());
        
        // HTTP/2.0
        String raw3 = "GET /test HTTP/2.0\r\n\r\n";
        ParsedRequest parsed3 = RawHttpRequestParser.parse(raw3);
        assertEquals("HTTP/2.0", parsed3.getProtocol());
    }
    
    @Test
    @DisplayName("测试缺少协议版本的请求")
    public void testMissingProtocolVersion() {
        String raw = "GET /test\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("/test", parsed.getPath());
        assertEquals("HTTP/1.1", parsed.getProtocol()); // 默认值
    }

    @Test
    @DisplayName("测试缺省路径的请求行")
    public void testBareRequestLineWithoutPath() {
        String raw = "GET HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n";

        ParsedRequest parsed = RawHttpRequestParser.parse(raw);

        assertNotNull(parsed);
        assertEquals("GET", parsed.getMethod());
        assertEquals("", parsed.getPath());
        assertEquals("HTTP/1.1", parsed.getProtocol());
    }
    
    @Test
    @DisplayName("测试各种HTTP方法")
    public void testVariousHttpMethods() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"};
        
        for (String method : methods) {
            String raw = method + " /test HTTP/1.1\r\n\r\n";
            ParsedRequest parsed = RawHttpRequestParser.parse(raw);
            assertEquals(method, parsed.getMethod(), "方法 " + method + " 解析失败");
        }
    }
    
    @Test
    @DisplayName("测试方法名大小写转换")
    public void testMethodCaseConversion() {
        String raw = "get /test HTTP/1.1\r\n\r\n";
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        // 方法名应转换为大写
        assertEquals("GET", parsed.getMethod());
    }
    
    @Test
    @DisplayName("测试重新构建HTTP请求")
    public void testBuildRequest() {
        String original = "POST /api/test HTTP/1.1\r\n" +
                         "Host: example.com\r\n" +
                         "Content-Type: text/plain\r\n" +
                         "\r\n" +
                         "test body";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(original);
        String rebuilt = RawHttpRequestParser.build(parsed);
        
        assertNotNull(rebuilt);
        assertTrue(rebuilt.contains("POST /api/test HTTP/1.1"));
        assertTrue(rebuilt.contains("Host: example.com"));
        assertTrue(rebuilt.contains("Content-Type: text/plain"));
        assertTrue(rebuilt.contains("test body"));
    }
    
    @Test
    @DisplayName("测试解析和重建往返一致性")
    public void testParseAndBuildRoundTrip() {
        String original = "GET /test HTTP/1.1\r\n" +
                         "Host: example.com\r\n" +
                         "User-Agent: Test\r\n" +
                         "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(original);
        String rebuilt = RawHttpRequestParser.build(parsed);
        ParsedRequest reparsed = RawHttpRequestParser.parse(rebuilt);
        
        assertEquals(parsed.getMethod(), reparsed.getMethod());
        assertEquals(parsed.getPath(), reparsed.getPath());
        assertEquals(parsed.getProtocol(), reparsed.getProtocol());
        assertEquals(parsed.getHeaders().size(), reparsed.getHeaders().size());
    }
    
    @Test
    @DisplayName("测试无效请求 - null")
    public void testInvalidRequestNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            RawHttpRequestParser.parse(null);
        });
    }
    
    @Test
    @DisplayName("测试无效请求 - 空字符串")
    public void testInvalidRequestEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            RawHttpRequestParser.parse("");
        });
    }
    
    @Test
    @DisplayName("测试无效请求 - 只有空白")
    public void testInvalidRequestWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> {
            RawHttpRequestParser.parse("   \n  \r\n  ");
        });
    }
    
    @Test
    @DisplayName("测试无效请求 - 缺少请求行")
    public void testInvalidRequestNoRequestLine() {
        String raw = "Host: example.com\r\n\r\n";
        
        assertThrows(IllegalArgumentException.class, () -> {
            RawHttpRequestParser.parse(raw);
        });
    }
    
    @Test
    @DisplayName("测试请求格式验证")
    public void testRequestValidation() {
        // 有效请求
        String valid = "GET /test HTTP/1.1\r\n\r\n";
        assertTrue(RawHttpRequestParser.isValidRawRequest(valid));
        
        // 无效请求
        assertFalse(RawHttpRequestParser.isValidRawRequest(null));
        assertFalse(RawHttpRequestParser.isValidRawRequest(""));
        assertFalse(RawHttpRequestParser.isValidRawRequest("invalid"));
    }
    
    @Test
    @DisplayName("测试Cookie请求头解析")
    public void testCookieHeader() {
        String raw = "GET /test HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Cookie: session=abc123; token=xyz789; user=admin\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        String cookie = parsed.getHeaders().get("Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("session=abc123"));
        assertTrue(cookie.contains("token=xyz789"));
        assertTrue(cookie.contains("user=admin"));
    }
    
    @Test
    @DisplayName("测试特殊字符编码的请求体")
    public void testEncodedBody() {
        String raw = "POST /api HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n" +
                    "data=%E4%B8%AD%E6%96%87&test=123";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals("data=%E4%B8%AD%E6%96%87&test=123", parsed.getBody());
    }
    
    @Test
    @DisplayName("测试XML请求体解析")
    public void testXmlBody() {
        String raw = "POST /api HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "Content-Type: application/xml\r\n" +
                    "\r\n" +
                    "<?xml version=\"1.0\"?>\n" +
                    "<root>\n" +
                    "  <item>value</item>\n" +
                    "</root>";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertTrue(parsed.getBody().contains("<?xml"));
        assertTrue(parsed.getBody().contains("<root>"));
        assertTrue(parsed.getBody().contains("<item>value</item>"));
    }
    
    @Test
    @DisplayName("测试超长请求头处理")
    public void testVeryLongHeaders() {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longValue.append("x");
        }
        
        String raw = "GET /test HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "X-Long-Header: " + longValue.toString() + "\r\n" +
                    "\r\n";
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals(longValue.toString(), parsed.getHeaders().get("X-Long-Header"));
    }
    
    @Test
    @DisplayName("测试超长请求体处理")
    public void testVeryLongBody() {
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longBody.append("data");
        }
        
        String raw = "POST /test HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n" +
                    longBody.toString();
        
        ParsedRequest parsed = RawHttpRequestParser.parse(raw);
        
        assertNotNull(parsed);
        assertEquals(longBody.toString(), parsed.getBody());
    }
}
