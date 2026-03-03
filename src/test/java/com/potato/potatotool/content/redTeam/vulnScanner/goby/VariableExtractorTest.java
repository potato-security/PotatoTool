package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VariableExtractor 变量提取器单元测试
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("VariableExtractor 变量提取测试")
public class VariableExtractorTest {
    
    private MockCustomHttpResponse mockResponse;
    private Map<String, String> extractedValues;
    
    @BeforeEach
    public void setUp() {
        mockResponse = new MockCustomHttpResponse();
        extractedValues = new HashMap<>();
    }
    
    /**
     * 测试用的 CustomHttpResponse stub 实现
     */
    private static class MockCustomHttpResponse extends CustomHttpResponse {
        private String textStr = "";
        private int responseCode = 200;
        private Map<String, List<String>> headerFields = new HashMap<>();
        private String headerFieldsText = "";
        private String allResponseText = "";
        
        public MockCustomHttpResponse() {
            super(null);
        }
        
        public void setTextStr(String textStr) {
            this.textStr = textStr;
        }
        
        @Override
        public String getTextStr() {
            return textStr;
        }
        
        public void setResponseCode(int code) {
            this.responseCode = code;
        }
        
        @Override
        public int getResponseCode() {
            return responseCode;
        }
        
        public void setHeaderFields(Map<String, List<String>> headers) {
            this.headerFields = headers;
        }
        
        @Override
        public Map<String, List<String>> getHeaderFields() {
            return headerFields;
        }
        
        public void setHeaderFieldsText(String text) {
            this.headerFieldsText = text;
        }
        
        @Override
        public String getHeaderFieldsText() {
            return headerFieldsText;
        }
        
        public void setAllResponseText(String text) {
            this.allResponseText = text;
        }
        
        @Override
        public String getAllResponseText() {
            return allResponseText;
        }
    }
    
    @Test
    @DisplayName("测试正则表达式提取 - 基本匹配")
    public void testRegexExtraction() {
        // 模拟响应
        mockResponse.setTextStr("token=abc123def456");
        
        // 创建提取器
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token=([a-zA-Z0-9]+)"));
        extractor.setGroup(1); // 提取第一个捕获组
        
        // 执行提取
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        // 验证结果
        assertEquals("abc123def456", extractedValues.get("token"));
    }
    
    @Test
    @DisplayName("测试正则表达式提取 - 命名分组")
    public void testRegexNamedGroup() {
        mockResponse.setTextStr("user: admin, id: 12345");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("user");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("user: (?<username>\\w+)"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("admin", extractedValues.get("user"));
    }
    
    @Test
    @DisplayName("测试正则表达式提取 - Python风格命名分组")
    public void testRegexPythonNamedGroup() {
        mockResponse.setTextStr("session_id=xyz789abc");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("session");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        // Python风格: (?P<name>pattern)
        extractor.setValues(Arrays.asList("session_id=(?P<sid>[a-z0-9]+)"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("xyz789abc", extractedValues.get("session"));
    }
    
    @Test
    @DisplayName("测试正则表达式提取 - 多个捕获组")
    public void testRegexMultipleGroups() {
        mockResponse.setTextStr("Version: 2.5.1 (Build 12345)");
        
        // 提取版本号（第一个捕获组）
        PocObj.Matcher extractor1 = new PocObj.Matcher();
        extractor1.setName("version");
        extractor1.setType(PocObj.MatcherType.REGEX);
        extractor1.setPart("body");
        extractor1.setValues(Arrays.asList("Version: ([\\d.]+) \\(Build (\\d+)\\)"));
        extractor1.setGroup(1);
        
        // 提取构建号（第二个捕获组）
        PocObj.Matcher extractor2 = new PocObj.Matcher();
        extractor2.setName("build");
        extractor2.setType(PocObj.MatcherType.REGEX);
        extractor2.setPart("body");
        extractor2.setValues(Arrays.asList("Version: ([\\d.]+) \\(Build (\\d+)\\)"));
        extractor2.setGroup(2);
        
        VariableExtractor.extractVariables(mockResponse, 
            Arrays.asList(extractor1, extractor2), extractedValues);
        
        assertEquals("2.5.1", extractedValues.get("version"));
        assertEquals("12345", extractedValues.get("build"));
    }
    
    @Test
    @DisplayName("测试正则表达式提取 - 完整匹配")
    public void testRegexFullMatch() {
        mockResponse.setTextStr("response: success");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("full");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("response: (\\w+)"));
        extractor.setGroup(0); // 0表示完整匹配
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("response: success", extractedValues.get("full"));
    }
    
    @Test
    @DisplayName("测试从响应头提取")
    public void testHeaderExtraction() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Arrays.asList("JSESSIONID=abc123; Path=/; HttpOnly"));
        mockResponse.setHeaderFields(headers);
        mockResponse.setHeaderFieldsText("Set-Cookie: JSESSIONID=abc123; Path=/; HttpOnly\n");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("session");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("header");
        extractor.setValues(Arrays.asList("JSESSIONID=([^;]+)"));
        extractor.setGroup(1);
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("abc123", extractedValues.get("session"));
    }
    
    @Test
    @DisplayName("测试从状态码提取")
    public void testStatusExtraction() {
        mockResponse.setResponseCode(200);
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("status");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("status");
        extractor.setValues(Arrays.asList("(\\d+)"));
        extractor.setGroup(1);
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("200", extractedValues.get("status"));
    }
    
    @Test
    @DisplayName("测试JSON提取")
    public void testJsonExtraction() {
        mockResponse.setTextStr(
            "{\"status\":\"success\",\"data\":{\"token\":\"xyz123\",\"user\":\"admin\"}}"
        );
        
        // 提取token
        PocObj.Matcher extractor1 = new PocObj.Matcher();
        extractor1.setName("token");
        extractor1.setType(PocObj.MatcherType.JSON);
        extractor1.setPart("body");
        extractor1.setValues(Arrays.asList("$.data.token"));
        
        // 提取user
        PocObj.Matcher extractor2 = new PocObj.Matcher();
        extractor2.setName("user");
        extractor2.setType(PocObj.MatcherType.JSON);
        extractor2.setPart("body");
        extractor2.setValues(Arrays.asList("$.data.user"));
        
        VariableExtractor.extractVariables(mockResponse, 
            Arrays.asList(extractor1, extractor2), extractedValues);
        
        assertEquals("xyz123", extractedValues.get("token"));
        assertEquals("admin", extractedValues.get("user"));
    }
    
    @Test
    @DisplayName("测试JSON数组提取")
    public void testJsonArrayExtraction() {
        mockResponse.setTextStr(
            "{\"items\":[{\"id\":1,\"name\":\"first\"},{\"id\":2,\"name\":\"second\"}]}"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("first_item");
        extractor.setType(PocObj.MatcherType.JSON);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("$.items[0].name"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("first", extractedValues.get("first_item"));
    }
    
    @Test
    @DisplayName("测试XPath提取 - 基本元素")
    public void testXPathExtraction() {
        mockResponse.setTextStr(
            "<?xml version=\"1.0\"?><root><user>admin</user><id>123</id></root>"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("user");
        extractor.setType(PocObj.MatcherType.XPATH);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("//user"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("admin", extractedValues.get("user"));
    }
    
    @Test
    @DisplayName("测试XPath提取 - 属性")
    public void testXPathAttributeExtraction() {
        mockResponse.setTextStr(
            "<?xml version=\"1.0\"?><root><item id=\"abc123\" name=\"test\"/></root>"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("item_id");
        extractor.setType(PocObj.MatcherType.XPATH);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("//item"));
        extractor.setAttribute("id");
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("abc123", extractedValues.get("item_id"));
    }
    
    @Test
    @DisplayName("测试KVAL键值对提取 - 冒号分隔")
    public void testKvalExtractionColon() {
        mockResponse.setTextStr(
            "session_id: abc123\nuser_id: 456\nstatus: active"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("session");
        extractor.setType(PocObj.MatcherType.KVAL);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("session_id"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("abc123", extractedValues.get("session"));
    }
    
    @Test
    @DisplayName("测试KVAL键值对提取 - 等号分隔")
    public void testKvalExtractionEquals() {
        mockResponse.setTextStr(
            "token=xyz789\nexpires=3600\ntype=bearer"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.KVAL);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("xyz789", extractedValues.get("token"));
    }
    
    @Test
    @DisplayName("测试KVAL提取 - Cookie格式")
    public void testKvalExtractionCookie() {
        mockResponse.setTextStr(
            "session=abc123; token=xyz789; user=admin"
        );
        
        PocObj.Matcher extractor1 = new PocObj.Matcher();
        extractor1.setName("session");
        extractor1.setType(PocObj.MatcherType.KVAL);
        extractor1.setPart("body");
        extractor1.setValues(Arrays.asList("session"));
        
        PocObj.Matcher extractor2 = new PocObj.Matcher();
        extractor2.setName("token");
        extractor2.setType(PocObj.MatcherType.KVAL);
        extractor2.setPart("body");
        extractor2.setValues(Arrays.asList("token"));
        
        VariableExtractor.extractVariables(mockResponse, 
            Arrays.asList(extractor1, extractor2), extractedValues);
        
        assertEquals("abc123", extractedValues.get("session"));
        assertEquals("xyz789", extractedValues.get("token"));
    }
    
    @Test
    @DisplayName("测试KVAL提取 - Set-Cookie头")
    public void testKvalExtractionSetCookie() {
        mockResponse.setTextStr(
            "Set-Cookie: PHPSESSID=abc123; Path=/; HttpOnly; Secure"
        );
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("session");
        extractor.setType(PocObj.MatcherType.KVAL);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("PHPSESSID"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertEquals("abc123", extractedValues.get("session"));
    }
    
    @Test
    @DisplayName("测试多个提取器顺序执行")
    public void testMultipleExtractorsInOrder() {
        mockResponse.setTextStr(
            "token=abc123&session=xyz789&user=admin"
        );
        
        List<PocObj.Matcher> extractors = new ArrayList<>();
        
        PocObj.Matcher ext1 = new PocObj.Matcher();
        ext1.setName("token");
        ext1.setType(PocObj.MatcherType.REGEX);
        ext1.setPart("body");
        ext1.setValues(Arrays.asList("token=([^&]+)"));
        ext1.setGroup(1);
        extractors.add(ext1);
        
        PocObj.Matcher ext2 = new PocObj.Matcher();
        ext2.setName("session");
        ext2.setType(PocObj.MatcherType.REGEX);
        ext2.setPart("body");
        ext2.setValues(Arrays.asList("session=([^&]+)"));
        ext2.setGroup(1);
        extractors.add(ext2);
        
        PocObj.Matcher ext3 = new PocObj.Matcher();
        ext3.setName("user");
        ext3.setType(PocObj.MatcherType.REGEX);
        ext3.setPart("body");
        ext3.setValues(Arrays.asList("user=([^&]+)"));
        ext3.setGroup(1);
        extractors.add(ext3);
        
        VariableExtractor.extractVariables(mockResponse, extractors, extractedValues);
        
        assertEquals(3, extractedValues.size());
        assertEquals("abc123", extractedValues.get("token"));
        assertEquals("xyz789", extractedValues.get("session"));
        assertEquals("admin", extractedValues.get("user"));
    }
    
    @Test
    @DisplayName("测试提取失败不影响其他提取器")
    public void testExtractionFailureIsolation() {
        mockResponse.setTextStr("valid_token=abc123");
        
        List<PocObj.Matcher> extractors = new ArrayList<>();
        
        // 这个会成功
        PocObj.Matcher ext1 = new PocObj.Matcher();
        ext1.setName("valid");
        ext1.setType(PocObj.MatcherType.REGEX);
        ext1.setPart("body");
        ext1.setValues(Arrays.asList("valid_token=([^&]+)"));
        ext1.setGroup(1);
        extractors.add(ext1);
        
        // 这个会失败（匹配不到）
        PocObj.Matcher ext2 = new PocObj.Matcher();
        ext2.setName("invalid");
        ext2.setType(PocObj.MatcherType.REGEX);
        ext2.setPart("body");
        ext2.setValues(Arrays.asList("nonexistent_pattern"));
        ext2.setGroup(1);
        extractors.add(ext2);
        
        // 这个也会成功
        PocObj.Matcher ext3 = new PocObj.Matcher();
        ext3.setName("another");
        ext3.setType(PocObj.MatcherType.REGEX);
        ext3.setPart("body");
        ext3.setValues(Arrays.asList("([a-z_]+)="));
        ext3.setGroup(1);
        extractors.add(ext3);
        
        VariableExtractor.extractVariables(mockResponse, extractors, extractedValues);
        
        // 成功的提取器应该提取到值
        assertEquals("abc123", extractedValues.get("valid"));
        assertEquals("valid_token", extractedValues.get("another"));
        // 失败的提取器不应该添加到结果中
        assertFalse(extractedValues.containsKey("invalid"));
    }
    
    @Test
    @DisplayName("测试空响应处理")
    public void testEmptyResponse() {
        mockResponse.setTextStr("");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token=([^&]+)"));
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        // 空响应不应该提取到任何值
        assertFalse(extractedValues.containsKey("token"));
    }
    
    @Test
    @DisplayName("测试null参数处理")
    public void testNullParameters() {
        // null响应
        VariableExtractor.extractVariables(null, Arrays.asList(new PocObj.Matcher()), extractedValues);
        assertTrue(extractedValues.isEmpty());
        
        // null提取器列表
        VariableExtractor.extractVariables(mockResponse, null, extractedValues);
        assertTrue(extractedValues.isEmpty());
        
        // null变量映射
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(new PocObj.Matcher()), null);
        // 不应抛出异常
    }
    
    @Test
    @DisplayName("测试无效正则表达式处理")
    public void testInvalidRegex() {
        mockResponse.setTextStr("test data");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("test");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        // 无效的正则表达式
        extractor.setValues(Arrays.asList("[invalid(regex"));
        
        // 不应抛出异常，只是提取失败
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertFalse(extractedValues.containsKey("test"));
    }
    
    @Test
    @DisplayName("测试无效JSON处理")
    public void testInvalidJson() {
        mockResponse.setTextStr("not a json string");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("test");
        extractor.setType(PocObj.MatcherType.JSON);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("$.token"));
        
        // 不应抛出异常
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertFalse(extractedValues.containsKey("test"));
    }
    
    @Test
    @DisplayName("测试无效XML处理")
    public void testInvalidXml() {
        mockResponse.setTextStr("not xml content");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("test");
        extractor.setType(PocObj.MatcherType.XPATH);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("//user"));
        
        // 不应抛出异常
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        assertFalse(extractedValues.containsKey("test"));
    }
    
    @Test
    @DisplayName("测试正则表达式 - 自动组号选择")
    public void testRegexAutoGroupSelection() {
        mockResponse.setTextStr("result: success");
        
        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("result");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("result: (\\w+)"));
        extractor.setGroup(-1); // -1表示自动选择
        
        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);
        
        // 应该自动选择第一个捕获组
        assertEquals("success", extractedValues.get("result"));
    }
    
    @Test
    @DisplayName("测试internal提取器不会写入结果")
    public void testInternalExtractorNotExported() {
        mockResponse.setTextStr("token=abc123");

        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token=([a-z0-9]+)"));
        extractor.setGroup(1);
        extractor.setInternal("true");

        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);

        assertFalse(extractedValues.containsKey("token"));
    }

    @Test
    @DisplayName("测试KVAL默认part为header")
    public void testKvalDefaultPartHeader() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Arrays.asList("SID=abc123; Path=/; HttpOnly"));
        mockResponse.setHeaderFields(headers);
        mockResponse.setHeaderFieldsText("Set-Cookie: SID=abc123; Path=/; HttpOnly\n");

        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("sid");
        extractor.setType(PocObj.MatcherType.KVAL);
        extractor.setPart(null);
        extractor.setValues(Arrays.asList("SID"));

        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);

        assertEquals("abc123", extractedValues.get("sid"));
    }

    @Test
    @DisplayName("测试JSON默认part为body")
    public void testJsonDefaultPartBody() {
        mockResponse.setTextStr("{\"data\":{\"token\":\"xyz789\"}}");

        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.JSON);
        extractor.setPart(null);
        extractor.setValues(Arrays.asList("$.data.token"));

        VariableExtractor.extractVariables(mockResponse, Arrays.asList(extractor), extractedValues);

        assertEquals("xyz789", extractedValues.get("token"));
    }

    @Test
    @DisplayName("测试纯文本提取器（非HTTP协议）")
    public void testTextExtractorForNonHttpProtocols() {
        Map<String, Object> extractedObj = new HashMap<>();
        String raw = "result=ok token=abc123";

        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token=([a-z0-9]+)"));
        extractor.setGroup(1);

        VariableExtractor.extractVariablesFromTextObj(raw, Arrays.asList(extractor), extractedObj);

        assertEquals("abc123", extractedObj.get("token"));
    }

    @Test
    @DisplayName("测试纯文本internal提取器不会写入结果")
    public void testTextInternalExtractorNotExported() {
        Map<String, Object> extractedObj = new HashMap<>();
        String raw = "token=abc123";

        PocObj.Matcher extractor = new PocObj.Matcher();
        extractor.setName("token");
        extractor.setType(PocObj.MatcherType.REGEX);
        extractor.setPart("body");
        extractor.setValues(Arrays.asList("token=([a-z0-9]+)"));
        extractor.setGroup(1);
        extractor.setInternal("true");

        VariableExtractor.extractVariablesFromTextObj(raw, Arrays.asList(extractor), extractedObj);

        assertFalse(extractedObj.containsKey("token"));
    }
}

