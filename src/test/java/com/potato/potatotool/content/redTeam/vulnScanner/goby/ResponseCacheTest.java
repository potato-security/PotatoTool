package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache;
import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache.CachedResponse;
import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache.DiffResult;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResponseCache 响应缓存单元测试
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("ResponseCache 响应缓存测试")
public class ResponseCacheTest {
    
    private ResponseCache cache;
    private MockCustomHttpResponse mockResponse1;
    private MockCustomHttpResponse mockResponse2;
    
    @BeforeEach
    public void setUp() {
        cache = new ResponseCache();
        mockResponse1 = new MockCustomHttpResponse();
        mockResponse2 = new MockCustomHttpResponse();
    }
    
    /**
     * 测试用的 CustomHttpResponse stub 实现
     */
    private static class MockCustomHttpResponse extends CustomHttpResponse {
        private int responseCode = 200;
        private String textStr = "";
        private String headerFieldsText = "";
        private byte[] byteArray = new byte[0];
        
        public MockCustomHttpResponse() {
            super(null);
        }
        
        public void setResponseCode(int code) {
            this.responseCode = code;
        }
        
        @Override
        public int getResponseCode() {
            return responseCode;
        }
        
        public void setTextStr(String text) {
            this.textStr = text;
        }
        
        @Override
        public String getTextStr() {
            return textStr;
        }
        
        public void setHeaderFieldsText(String text) {
            this.headerFieldsText = text;
        }
        
        @Override
        public String getHeaderFieldsText() {
            return headerFieldsText;
        }
        
        public void setByteArray(byte[] bytes) {
            this.byteArray = bytes;
        }
        
        @Override
        public byte[] getByteArray() {
            return byteArray;
        }
    }
    
    @Test
    @DisplayName("测试基本缓存存取")
    public void testBasicCachePutAndGet() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("response body");
        mockResponse1.setHeaderFieldsText("Content-Type: text/html");
        mockResponse1.setByteArray("response body".getBytes());
        
        long requestTime = System.currentTimeMillis();
        long responseTime = requestTime + 100;
        
        cache.put("step1", mockResponse1, requestTime, responseTime);
        
        assertTrue(cache.contains("step1"));
        CachedResponse cached = cache.get("step1");
        assertNotNull(cached);
        assertEquals(200, cached.getStatusCode());
        assertEquals("response body", cached.getBody());
        assertEquals(100, cached.getResponseTimeMs());
    }
    
    @Test
    @DisplayName("测试缓存不存在")
    public void testCacheNotExists() {
        assertFalse(cache.contains("nonexistent"));
        assertNull(cache.get("nonexistent"));
    }
    
    @Test
    @DisplayName("测试缓存覆盖")
    public void testCacheOverwrite() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("first");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("first".getBytes());
        
        mockResponse2.setResponseCode(404);
        mockResponse2.setTextStr("second");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("second".getBytes());
        
        long time = System.currentTimeMillis();
        
        cache.put("key", mockResponse1, time, time + 100);
        assertEquals("first", cache.get("key").getBody());
        
        cache.put("key", mockResponse2, time, time + 200);
        assertEquals("second", cache.get("key").getBody());
        assertEquals(404, cache.get("key").getStatusCode());
    }
    
    @Test
    @DisplayName("测试缓存清空")
    public void testCacheClear() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("test");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("test".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("key1", mockResponse1, time, time + 100);
        cache.put("key2", mockResponse1, time, time + 100);
        
        assertEquals(2, cache.size());
        
        cache.clear();
        
        assertEquals(0, cache.size());
        assertFalse(cache.contains("key1"));
        assertFalse(cache.contains("key2"));
    }
    
    @Test
    @DisplayName("测试diff - body差异")
    public void testDiffBody() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("response A");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("response A".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("response B");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("response B".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "body");
        
        assertNotNull(result);
        assertTrue(result.isDifferent());
        assertTrue(result.getDiffValue() > 0);
    }
    
    @Test
    @DisplayName("测试diff - body相同")
    public void testDiffBodySame() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("same response");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("same response".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("same response");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("same response".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "body");
        
        assertNotNull(result);
        assertFalse(result.isDifferent());
        assertEquals(0.0, result.getDiffValue());
    }
    
    @Test
    @DisplayName("测试diff - status差异")
    public void testDiffStatus() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("ok");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("ok".getBytes());
        
        mockResponse2.setResponseCode(404);
        mockResponse2.setTextStr("not found");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("not found".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "status");
        
        assertNotNull(result);
        assertTrue(result.isDifferent());
        assertEquals(204, result.getDiffValue()); // 404 - 200 = 204
    }
    
    @Test
    @DisplayName("测试diff - length差异")
    public void testDiffLength() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("short");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("short".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("much longer response");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("much longer response".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "length");
        
        assertNotNull(result);
        assertTrue(result.isDifferent());
        assertTrue(result.getDiffValue() > 0);
    }
    
    @Test
    @DisplayName("测试diff - time差异")
    public void testDiffTime() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("fast");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("fast".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("slow");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("slow".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 50);  // 50ms
        cache.put("resp2", mockResponse2, time, time + 5000); // 5000ms
        
        DiffResult result = cache.diff("resp1", "resp2", "time");
        
        assertNotNull(result);
        assertTrue(result.isDifferent()); // 差异大于100ms
        assertTrue(result.getDiffValue() > 100);
    }
    
    @Test
    @DisplayName("测试diff - header差异")
    public void testDiffHeader() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("test");
        mockResponse1.setHeaderFieldsText("Content-Type: text/html\nServer: Apache");
        mockResponse1.setByteArray("test".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("test");
        mockResponse2.setHeaderFieldsText("Content-Type: application/json\nServer: Nginx");
        mockResponse2.setByteArray("test".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "header");
        
        assertNotNull(result);
        assertTrue(result.isDifferent());
    }
    
    @Test
    @DisplayName("测试diff - 缓存不存在")
    public void testDiffCacheNotExists() {
        DiffResult result = cache.diff("nonexistent1", "nonexistent2", "body");
        
        assertNotNull(result);
        assertFalse(result.isDifferent());
        assertTrue(result.getDescription().contains("不存在"));
    }
    
    @Test
    @DisplayName("测试diff - 默认类型")
    public void testDiffDefaultType() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("response A");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray("response A".getBytes());
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("response B");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray("response B".getBytes());
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        // 不指定类型，应该默认为body
        DiffResult result1 = cache.diff("resp1", "resp2", null);
        assertNotNull(result1);
        
        DiffResult result2 = cache.diff("resp1", "resp2", "");
        assertNotNull(result2);
        
        DiffResult result3 = cache.diff("resp1", "resp2", "unknown_type");
        assertNotNull(result3);
    }
    
    @Test
    @DisplayName("测试null参数处理")
    public void testNullParameters() {
        // put with null key - 不应该添加
        cache.put(null, mockResponse1, 0, 100);
        assertFalse(cache.contains(null));
        
        // put with null response - 不应该添加
        cache.put("key", null, 0, 100);
        assertFalse(cache.contains("key"));
    }
    
    @Test
    @DisplayName("测试CachedResponse对象")
    public void testCachedResponseObject() {
        long requestTime = System.currentTimeMillis();
        long responseTime = requestTime + 500;
        
        CachedResponse cached = new CachedResponse(
            200,
            "test body",
            "test header",
            "test body".getBytes(),
            500,
            requestTime,
            responseTime
        );
        
        assertEquals(200, cached.getStatusCode());
        assertEquals("test body", cached.getBody());
        assertEquals("test header", cached.getHeader());
        assertEquals(500, cached.getResponseTimeMs());
        assertEquals(requestTime, cached.getRequestTimestamp());
        assertEquals(responseTime, cached.getResponseTimestamp());
        assertArrayEquals("test body".getBytes(), cached.getRawBytes());
    }
    
    @Test
    @DisplayName("测试diff - 空body处理")
    public void testDiffEmptyBody() {
        mockResponse1.setResponseCode(200);
        mockResponse1.setTextStr("");
        mockResponse1.setHeaderFieldsText("");
        mockResponse1.setByteArray(new byte[0]);
        
        mockResponse2.setResponseCode(200);
        mockResponse2.setTextStr("");
        mockResponse2.setHeaderFieldsText("");
        mockResponse2.setByteArray(new byte[0]);
        
        long time = System.currentTimeMillis();
        cache.put("resp1", mockResponse1, time, time + 100);
        cache.put("resp2", mockResponse2, time, time + 100);
        
        DiffResult result = cache.diff("resp1", "resp2", "body");
        
        assertNotNull(result);
        assertFalse(result.isDifferent());
        assertTrue(result.getDescription().contains("空"));
    }
    
    @Test
    @DisplayName("测试DiffResult toString")
    public void testDiffResultToString() {
        DiffResult result = new DiffResult(true, 100.5, "Test description");
        
        String str = result.toString();
        assertTrue(str.contains("true"));
        assertTrue(str.contains("100.5"));
        assertTrue(str.contains("Test description"));
    }
}

