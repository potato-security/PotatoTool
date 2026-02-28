package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GobyJsonObj 数据模型单元测试
 * 测试 Goby JSON POC 的数据结构解析
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("GobyJsonObj 数据模型测试")
public class GobyJsonObjTest {
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Test
    @DisplayName("测试基本POC结构解析")
    public void testBasicPocStructure() {
        String json = "{\n" +
            "  \"Name\": \"Test POC\",\n" +
            "  \"Level\": \"3\",\n" +
            "  \"Tags\": [\"rce\", \"injection\"],\n" +
            "  \"Description\": \"Test Description\",\n" +
            "  \"Author\": \"Potato\",\n" +
            "  \"ScanSteps\": [],\n" +
            "  \"ExploitSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc = gson.fromJson(json, GobyJsonObj.PocJson.class);
        
        assertNotNull(poc, "POC解析结果不应为null");
        assertEquals("Test POC", poc.getName(), "Name字段不匹配");
        assertEquals("3", poc.getLevel(), "Level字段不匹配");
        assertEquals(2, poc.getTags().size(), "Tags数量不匹配");
        assertTrue(poc.getTags().contains("rce"), "Tags应包含rce");
        assertEquals("Test Description", poc.getDescription(), "Description不匹配");
        assertEquals("Potato", poc.getAuthor(), "Author不匹配");
    }
    
    @Test
    @DisplayName("测试Request结构解析 - 标准字段")
    public void testRequestStructure() {
        String json = "{\n" +
            "  \"method\": \"POST\",\n" +
            "  \"uri\": \"/api/test\",\n" +
            "  \"follow_redirect\": false,\n" +
            "  \"header\": {\n" +
            "    \"Content-Type\": \"application/json\",\n" +
            "    \"User-Agent\": \"Test\"\n" +
            "  },\n" +
            "  \"data_type\": \"text\",\n" +
            "  \"data\": \"test=123\"\n" +
            "}";
        
        GobyJsonObj.Request request = gson.fromJson(json, GobyJsonObj.Request.class);
        
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/test", request.getUri());
        assertFalse(request.isFollow_redirect());
        assertNotNull(request.getHeader());
        assertEquals("application/json", request.getHeader().get("Content-Type"));
        assertEquals("text", request.getData_type());
        assertEquals("test=123", request.getData());
    }
    
    @Test
    @DisplayName("测试Request结构解析 - raw报文")
    public void testRequestWithRawField() {
        String json = "{\n" +
            "  \"raw\": \"GET /test HTTP/1.1\\r\\nHost: example.com\\r\\n\\r\\n\"\n" +
            "}";
        
        GobyJsonObj.Request request = gson.fromJson(json, GobyJsonObj.Request.class);
        
        assertNotNull(request);
        assertNotNull(request.getRaw());
        assertTrue(request.getRaw().contains("GET /test"));
    }
    
    @Test
    @DisplayName("测试Request结构解析 - 多URI路径")
    public void testRequestWithMultipleUris() {
        String json = "{\n" +
            "  \"method\": \"GET\",\n" +
            "  \"uris\": [\"/path1\", \"/path2\", \"/path3\"]\n" +
            "}";
        
        GobyJsonObj.Request request = gson.fromJson(json, GobyJsonObj.Request.class);
        
        assertNotNull(request);
        assertNotNull(request.getUris());
        assertEquals(3, request.getUris().size());
        
        List<String> allUris = request.getAllUris();
        assertEquals(3, allUris.size());
        assertEquals("/path1", allUris.get(0));
        assertEquals("/path2", allUris.get(1));
        assertEquals("/path3", allUris.get(2));
    }
    
    @Test
    @DisplayName("测试Request结构解析 - uri和uris兼容性")
    public void testRequestUriCompatibility() {
        // 只有uri字段
        String json1 = "{\"uri\": \"/single-path\"}";
        GobyJsonObj.Request request1 = gson.fromJson(json1, GobyJsonObj.Request.class);
        List<String> uris1 = request1.getAllUris();
        assertEquals(1, uris1.size());
        assertEquals("/single-path", uris1.get(0));
        
        // 只有uris字段
        String json2 = "{\"uris\": [\"/path1\", \"/path2\"]}";
        GobyJsonObj.Request request2 = gson.fromJson(json2, GobyJsonObj.Request.class);
        List<String> uris2 = request2.getAllUris();
        assertEquals(2, uris2.size());
        
        // 同时存在uri和uris，优先使用uris
        String json3 = "{\"uri\": \"/ignored\", \"uris\": [\"/path1\", \"/path2\"]}";
        GobyJsonObj.Request request3 = gson.fromJson(json3, GobyJsonObj.Request.class);
        List<String> uris3 = request3.getAllUris();
        assertEquals(2, uris3.size());
        assertEquals("/path1", uris3.get(0));
    }
    
    @Test
    @DisplayName("测试ResponseTest结构解析")
    public void testResponseTestStructure() {
        String json = "{\n" +
            "  \"type\": \"group\",\n" +
            "  \"operation\": \"AND\",\n" +
            "  \"checks\": [\n" +
            "    {\n" +
            "      \"type\": \"item\",\n" +
            "      \"variable\": \"$code\",\n" +
            "      \"operation\": \"==\",\n" +
            "      \"value\": \"200\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        GobyJsonObj.ResponseTest responseTest = gson.fromJson(json, GobyJsonObj.ResponseTest.class);
        
        assertNotNull(responseTest);
        assertEquals("group", responseTest.getType());
        assertEquals("AND", responseTest.getOperation());
        assertNotNull(responseTest.getChecks());
        assertEquals(1, responseTest.getChecks().size());
        
        GobyJsonObj.Check check = responseTest.getChecks().get(0);
        assertEquals("item", check.getType());
        assertEquals("$code", check.getVariable());
        assertEquals("==", check.getOperation());
        assertEquals("200", check.getValue());
    }
    
    @Test
    @DisplayName("测试嵌套Check结构解析")
    public void testNestedCheckStructure() {
        String json = "{\n" +
            "  \"type\": \"group\",\n" +
            "  \"operation\": \"AND\",\n" +
            "  \"checks\": [\n" +
            "    {\n" +
            "      \"type\": \"item\",\n" +
            "      \"variable\": \"$code\",\n" +
            "      \"operation\": \"==\",\n" +
            "      \"value\": \"200\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"group\",\n" +
            "      \"operation\": \"OR\",\n" +
            "      \"checks\": [\n" +
            "        {\n" +
            "          \"type\": \"item\",\n" +
            "          \"variable\": \"$body\",\n" +
            "          \"operation\": \"contains\",\n" +
            "          \"value\": \"success\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"item\",\n" +
            "          \"variable\": \"$body\",\n" +
            "          \"operation\": \"contains\",\n" +
            "          \"value\": \"ok\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        GobyJsonObj.Check check = gson.fromJson(json, GobyJsonObj.Check.class);
        
        assertNotNull(check);
        assertEquals("group", check.getType());
        assertEquals("AND", check.getOperation());
        assertEquals(2, check.getChecks().size());
        
        // 第二个check是嵌套的group
        GobyJsonObj.Check nestedGroup = check.getChecks().get(1);
        assertEquals("group", nestedGroup.getType());
        assertEquals("OR", nestedGroup.getOperation());
        assertEquals(2, nestedGroup.getChecks().size());
    }
    
    @Test
    @DisplayName("测试ExpParam参数解析 - 大小写兼容")
    public void testExpParamCaseCompatibility() {
        // 大写字段
        String json1 = "{\n" +
            "  \"Name\": \"cmd\",\n" +
            "  \"Type\": \"input\",\n" +
            "  \"Value\": \"whoami\"\n" +
            "}";
        
        GobyJsonObj.ExpParam param1 = gson.fromJson(json1, GobyJsonObj.ExpParam.class);
        assertEquals("cmd", param1.getName());
        assertEquals("input", param1.getType());
        assertEquals("whoami", param1.getValue());
        
        // 小写字段
        String json2 = "{\n" +
            "  \"name\": \"cmd\",\n" +
            "  \"type\": \"input\",\n" +
            "  \"value\": \"whoami\"\n" +
            "}";
        
        GobyJsonObj.ExpParam param2 = gson.fromJson(json2, GobyJsonObj.ExpParam.class);
        assertEquals("cmd", param2.getName());
        assertEquals("input", param2.getType());
        assertEquals("whoami", param2.getValue());
        
        // 混合大小写
        String json3 = "{\n" +
            "  \"name\": \"lower\",\n" +
            "  \"Name\": \"upper\"\n" +
            "}";
        
        GobyJsonObj.ExpParam param3 = gson.fromJson(json3, GobyJsonObj.ExpParam.class);
        // 小写优先
        assertEquals("lower", param3.getName());
    }
    
    @Test
    @DisplayName("测试GlobalVariables全局变量解析")
    public void testGlobalVariables() {
        String json = "{\n" +
            "  \"Name\": \"Test POC\",\n" +
            "  \"GlobalVariables\": {\n" +
            "    \"var1\": \"value1\",\n" +
            "    \"var2\": \"value2\"\n" +
            "  },\n" +
            "  \"ScanSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc = gson.fromJson(json, GobyJsonObj.PocJson.class);
        
        assertNotNull(poc.getGlobalVariables());
        assertEquals(2, poc.getGlobalVariables().size());
        assertEquals("value1", poc.getGlobalVariables().get("var1"));
        assertEquals("value2", poc.getGlobalVariables().get("var2"));
    }
    
    @Test
    @DisplayName("测试Authentication认证配置解析")
    public void testAuthentication() {
        String json = "{\n" +
            "  \"Name\": \"Test POC\",\n" +
            "  \"Authentication\": {\n" +
            "    \"type\": \"basic\",\n" +
            "    \"username\": \"admin\",\n" +
            "    \"password\": \"123456\"\n" +
            "  },\n" +
            "  \"ScanSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc = gson.fromJson(json, GobyJsonObj.PocJson.class);
        
        assertNotNull(poc.getAuthentication());
        assertEquals("basic", poc.getAuthentication().getType());
        assertEquals("admin", poc.getAuthentication().getUsername());
        assertEquals("123456", poc.getAuthentication().getPassword());
    }
    
    @Test
    @DisplayName("测试Authentication - Bearer Token")
    public void testAuthenticationBearer() {
        String json = "{\n" +
            "  \"type\": \"bearer\",\n" +
            "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\"\n" +
            "}";
        
        GobyJsonObj.Authentication auth = gson.fromJson(json, GobyJsonObj.Authentication.class);
        
        assertNotNull(auth);
        assertEquals("bearer", auth.getType());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", auth.getToken());
    }
    
    @Test
    @DisplayName("测试Authentication - Cookie")
    public void testAuthenticationCookie() {
        String json = "{\n" +
            "  \"type\": \"cookie\",\n" +
            "  \"cookies\": {\n" +
            "    \"PHPSESSID\": \"abc123\",\n" +
            "    \"token\": \"xyz789\"\n" +
            "  }\n" +
            "}";
        
        GobyJsonObj.Authentication auth = gson.fromJson(json, GobyJsonObj.Authentication.class);
        
        assertNotNull(auth);
        assertEquals("cookie", auth.getType());
        assertNotNull(auth.getCookies());
        assertEquals(2, auth.getCookies().size());
        assertEquals("abc123", auth.getCookies().get("PHPSESSID"));
        assertEquals("xyz789", auth.getCookies().get("token"));
    }
    
    @Test
    @DisplayName("测试完整ScanStep结构解析")
    public void testCompleteScanStep() {
        String json = "{\n" +
            "  \"Request\": {\n" +
            "    \"method\": \"POST\",\n" +
            "    \"uri\": \"/api/login\",\n" +
            "    \"header\": {\n" +
            "      \"Content-Type\": \"application/json\"\n" +
            "    },\n" +
            "    \"data\": \"{\\\"user\\\":\\\"admin\\\"}\",\n" +
            "    \"set_variable\": [\"token|body|regex|token=([^&]+)\"]\n" +
            "  },\n" +
            "  \"ResponseTest\": {\n" +
            "    \"type\": \"group\",\n" +
            "    \"operation\": \"AND\",\n" +
            "    \"checks\": [\n" +
            "      {\n" +
            "        \"type\": \"item\",\n" +
            "        \"variable\": \"$code\",\n" +
            "        \"operation\": \"==\",\n" +
            "        \"value\": \"200\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"SetVariable\": [\"result|body|regex|success\"]\n" +
            "}";
        
        GobyJsonObj.ScanStep step = gson.fromJson(json, GobyJsonObj.ScanStep.class);
        
        assertNotNull(step);
        assertNotNull(step.getRequest());
        assertEquals("POST", step.getRequest().getMethod());
        assertEquals("/api/login", step.getRequest().getUri());
        assertNotNull(step.getRequest().getSet_variable());
        assertEquals(1, step.getRequest().getSet_variable().size());
        
        assertNotNull(step.getResponseTest());
        assertEquals("group", step.getResponseTest().getType());
        assertEquals(1, step.getResponseTest().getChecks().size());
        
        assertNotNull(step.getSetVariable());
        assertEquals(1, step.getSetVariable().size());
    }
    
    @Test
    @DisplayName("测试Recommendation字段的两种拼写")
    public void testRecommendationSpelling() {
        // 正确拼写
        String json1 = "{\n" +
            "  \"Name\": \"Test\",\n" +
            "  \"Recommendation\": \"Please update\",\n" +
            "  \"ScanSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc1 = gson.fromJson(json1, GobyJsonObj.PocJson.class);
        assertEquals("Please update", poc1.getRecommendation());
        
        // 法语拼写（常见错误）
        String json2 = "{\n" +
            "  \"Name\": \"Test\",\n" +
            "  \"Recommandation\": \"Please update\",\n" +
            "  \"ScanSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc2 = gson.fromJson(json2, GobyJsonObj.PocJson.class);
        assertEquals("Please update", poc2.getRecommandation());
    }
    
    @Test
    @DisplayName("测试空POC处理")
    public void testEmptyPoc() {
        String json = "{\n" +
            "  \"ScanSteps\": [],\n" +
            "  \"ExploitSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc = gson.fromJson(json, GobyJsonObj.PocJson.class);
        
        assertNotNull(poc);
        assertNotNull(poc.getScanSteps());
        assertNotNull(poc.getExploitSteps());
        assertEquals(0, poc.getScanSteps().size());
        assertEquals(0, poc.getExploitSteps().size());
    }
    
    @Test
    @DisplayName("测试特殊字符和转义")
    public void testSpecialCharacters() {
        String json = "{\n" +
            "  \"Name\": \"Test \\\"POC\\\"\",\n" +
            "  \"Description\": \"Line1\\nLine2\\tTabbed\",\n" +
            "  \"ScanSteps\": []\n" +
            "}";
        
        GobyJsonObj.PocJson poc = gson.fromJson(json, GobyJsonObj.PocJson.class);
        
        assertNotNull(poc);
        assertEquals("Test \"POC\"", poc.getName());
        assertTrue(poc.getDescription().contains("\n"));
        assertTrue(poc.getDescription().contains("\t"));
    }
}

