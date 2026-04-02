package com.potato.potatotool.content.redTeam.vulnScanner;

import com.google.gson.Gson;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.PocsuitePocConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Pocsuite JsonPoc 全面测试套件
 * 测试各种复杂场景和边界情况，确保与官方解析一致
 * 
 * @author Potato
 * @date 2025-01-XX
 */
public class PocsuiteComprehensiveTest {
    
    private Gson gson;
    private PocsuitePocConverter converter;
    
    @BeforeEach
    public void setUp() {
        gson = new Gson();
        converter = new PocsuitePocConverter();
    }
    
    /**
     * 读取文件内容（Java 8 兼容）
     */
    private String readFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    // ==================== 基本字段解析测试 ====================
    
    @Test
    public void testBasicPocInfoFields() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"test-001\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"vulDate\": \"2024-01-01\",\n" +
                "    \"createDate\": \"2024-01-01\",\n" +
                "    \"updateDate\": \"2024-01-02\",\n" +
                "    \"name\": \"Test POC\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"SQL Injection\",\n" +
                "    \"author\": \"TestAuthor\",\n" +
                "    \"references\": [\"https://example.com\"],\n" +
                "    \"appName\": \"TestApp\",\n" +
                "    \"appVersion\": \"1.0.0\",\n" +
                "    \"appPowerLink\": \"https://example.com\",\n" +
                "    \"desc\": \"Test description\",\n" +
                "    \"samples\": [\"https://example.com/vuln\"]\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\"step\": \"0\", \"method\": \"GET\", \"vulPath\": \"/test\"}]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertEquals("vulID", "test-001", converted.getId());
        assertEquals("name", "Test POC", converted.getName());
        assertEquals("version", "1.0", converted.getVersion());
        assertEquals("protocol", "http", converted.getProtocol());
        assertEquals("vulType", "SQL Injection", converted.getVulType());
        assertEquals("author", "TestAuthor", converted.getAuthor());
        // description 会包含 vulDate（如果有的话），所以只检查是否包含原始描述
        assertTrue("description应包含Test description", converted.getDescription().contains("Test description"));
        assertEquals("createTime", "2024-01-01", converted.getCreateTime());
        assertEquals("updateTime", "2024-01-02", converted.getUpdateTime());
        assertNotNull("references不应为null", converted.getReferences());
        assertFalse("references不应为空", converted.getReferences().isEmpty());
        assertEquals("appName", "TestApp", converted.getProduct());
        assertEquals("appPowerLink", "https://example.com", converted.getAppPowerLink());
    }
    
    @Test
    public void testEmptyPocInfo() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\"step\": \"0\", \"method\": \"GET\", \"vulPath\": \"/test\"}]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertEquals("空pocInfo应使用默认protocol", "http", converted.getProtocol());
    }
    
    // ==================== Step字段解析测试 ====================
    
    @Test
    public void testStepFields() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"POST\",\n" +
                "      \"vulPath\": \"/api/test\",\n" +
                "      \"params\": \"a=1&b=2\",\n" +
                "      \"necessary\": \"需要先登录\",\n" +
                "      \"headers\": {\n" +
                "        \"Content-Type\": \"application/json\",\n" +
                "        \"Authorization\": \"Bearer token\"\n" +
                "      },\n" +
                "      \"status\": \"200\"\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull(converted);
        assertNotNull(converted.getVerifySteps());
        assertFalse(converted.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertEquals("step", "0", step.getStepId());
        assertEquals("method", "POST", step.getMethod());
        assertEquals("path", "/api/test", step.getPath());
        assertEquals("body", "a=1&b=2", step.getBody());
        assertNotNull("headers不应为null", step.getHeaders());
        assertEquals("headers数量", 2, step.getHeaders().size());
        assertEquals("Content-Type", "application/json", step.getHeaders().get("Content-Type"));
        assertEquals("Authorization", "Bearer token", step.getHeaders().get("Authorization"));
    }
    
    @Test
    public void testStepWithoutMethod() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"vulPath\": \"/test\"\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertEquals("默认method应为GET", "GET", step.getMethod());
    }
    
    @Test
    public void testStepWithoutPath() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\"\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertEquals("默认path应为/", "/", step.getPath());
    }
    
    // ==================== Match字段解析测试 ====================
    
    @Test
    public void testRegexMatch() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"match\": {\n" +
                "        \"regex\": [\"success\", \"ok\"]\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull("matchers不应为null", step.getMatchers());
        
        PocObj.Matcher regexMatcher = step.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.REGEX)
            .findFirst()
            .orElse(null);
        
        assertNotNull("应存在REGEX匹配器", regexMatcher);
        assertEquals("regex数量", 2, regexMatcher.getValues().size());
        assertTrue("应包含success", regexMatcher.getValues().contains("success"));
        assertTrue("应包含ok", regexMatcher.getValues().contains("ok"));
        assertEquals("condition", "OR", regexMatcher.getCondition());
        assertEquals("part", "body", regexMatcher.getPart());
    }
    
    @Test
    public void testTimeMatch() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"match\": {\n" +
                "        \"time\": \"5\"\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        PocObj.Matcher timeMatcher = step.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.TIME)
            .findFirst()
            .orElse(null);
        
        assertNotNull("应存在TIME匹配器", timeMatcher);
        assertEquals("时间值", "5", timeMatcher.getValues().get(0));
        assertEquals("操作类型", PocObj.OperationType.GREATER_EQUAL, timeMatcher.getOperation());
        assertEquals("part", "response_time", timeMatcher.getPart());
        assertEquals("时间单位", "s", timeMatcher.getTimeUnit());
    }
    
    @Test
    public void testStatusMatch() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"status\": \"200\"\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        PocObj.Matcher statusMatcher = step.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.STATUS)
            .findFirst()
            .orElse(null);
        
        assertNotNull("应存在STATUS匹配器", statusMatcher);
        assertEquals("状态码", "200", statusMatcher.getValues().get(0));
        assertEquals("操作类型", PocObj.OperationType.EQUAL, statusMatcher.getOperation());
    }
    
    @Test
    public void testMixedMatchers() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"status\": \"200\",\n" +
                "      \"match\": {\n" +
                "        \"regex\": [\"success\"],\n" +
                "        \"time\": \"3\"\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertEquals("匹配器数量应为3", 3, step.getMatchers().size());
        assertEquals("匹配条件应为AND", PocObj.MatchersCondition.AND, step.getMatchersCondition());
        
        boolean hasRegex = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.REGEX);
        boolean hasTime = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.TIME);
        boolean hasStatus = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.STATUS);
        
        assertTrue("应包含REGEX匹配器", hasRegex);
        assertTrue("应包含TIME匹配器", hasTime);
        assertTrue("应包含STATUS匹配器", hasStatus);
    }
    
    @Test
    public void testEmptyRegex() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"match\": {\n" +
                "        \"regex\": []\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        // 空regex不应创建匹配器
        boolean hasRegex = step.getMatchers() != null && step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.REGEX);
        assertFalse("空regex不应创建匹配器", hasRegex);
    }
    
    @Test
    public void testEmptyTime() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"match\": {\n" +
                "        \"time\": \"\"\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        boolean hasTime = step.getMatchers() != null && step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.TIME);
        assertFalse("空time不应创建匹配器", hasTime);
    }
    
    // ==================== Result字段解析测试 ====================
    
    @Test
    public void testSimpleResult() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {\n" +
                "        \"AdminInfo\": {\n" +
                "          \"Username\": \"<regex>username:([^\\\\s]+)\",\n" +
                "          \"Password\": \"<regex>password:([^\\\\s]+)\"\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertNotNull("output不应为null", step.getOutput());
        assertNotNull("extractors不应为null", step.getExtractors());
        assertEquals("提取器数量应为2", 2, step.getExtractors().size());
        
        Map<String, Object> output = step.getOutput();
        assertTrue("应包含管理员信息", output.containsKey("管理员信息"));
        
        Object adminInfo = output.get("管理员信息");
        assertTrue("管理员信息应为Map", adminInfo instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> adminMap = (Map<String, Object>) adminInfo;
        assertTrue("应包含用户名", adminMap.containsKey("管理员用户名"));
        assertTrue("应包含密码", adminMap.containsKey("管理员密码"));
        
        // 验证提取器
        assertEquals("第一个提取器名称", "output_1", step.getExtractors().get(0).getName());
        assertEquals("第二个提取器名称", "output_2", step.getExtractors().get(1).getName());
    }
    
    @Test
    public void testDeepNestedResult() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {\n" +
                "        \"AdminInfo\": {\n" +
                "          \"Username\": \"<regex>username:([^\\\\s]+)\",\n" +
                "          \"Profile\": {\n" +
                "            \"Email\": \"<regex>email:([^\\\\s]+)\",\n" +
                "            \"Details\": {\n" +
                "              \"Address\": \"<regex>address:([^\\\\n]+)\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertEquals("提取器数量应为3", 3, step.getExtractors().size());
        
        Map<String, Object> output = step.getOutput();
        assertTrue("应包含管理员信息", output.containsKey("管理员信息"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> adminMap = (Map<String, Object>) output.get("管理员信息");
        assertTrue("应包含用户名", adminMap.containsKey("管理员用户名"));
        assertTrue("应包含Profile", adminMap.containsKey("Profile"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> profileMap = (Map<String, Object>) adminMap.get("Profile");
        assertTrue("Profile应包含Email", profileMap.containsKey("Email"));
        assertTrue("Profile应包含Details", profileMap.containsKey("Details"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> detailsMap = (Map<String, Object>) profileMap.get("Details");
        assertTrue("Details应包含Address", detailsMap.containsKey("Address"));
    }
    
    @Test
    public void testMultipleResultFields() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {\n" +
                "        \"AdminInfo\": {\n" +
                "          \"Username\": \"<regex>username:([^\\\\s]+)\"\n" +
                "        },\n" +
                "        \"DBInfo\": {\n" +
                "          \"Hostname\": \"<regex>hostname:([^\\\\s]+)\"\n" +
                "        },\n" +
                "        \"FileInfo\": {\n" +
                "          \"Filename\": \"<regex>filename:([^\\\\s]+)\"\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertEquals("提取器数量应为3", 3, step.getExtractors().size());
        
        Map<String, Object> output = step.getOutput();
        assertTrue("应包含管理员信息", output.containsKey("管理员信息"));
        assertTrue("应包含数据库内容", output.containsKey("数据库内容"));
        assertTrue("应包含文件信息", output.containsKey("文件信息"));
    }
    
    @Test
    public void testResultWithPlainValue() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {\n" +
                "        \"AdminInfo\": {\n" +
                "          \"Username\": \"<regex>username:([^\\\\s]+)\",\n" +
                "          \"Role\": \"admin\"\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> adminMap = (Map<String, Object>) step.getOutput().get("管理员信息");
        assertEquals("Role应为admin", "admin", adminMap.get("Role"));
        assertTrue("应包含模板变量", adminMap.get("管理员用户名").toString().startsWith("{{"));
    }
    
    @Test
    public void testEmptyResult() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {}\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertTrue("空result应返回空output", step.getOutput() == null || step.getOutput().isEmpty());
        assertTrue("空result应返回空extractors", step.getExtractors() == null || step.getExtractors().isEmpty());
    }
    
    // ==================== Verify和Attack步骤测试 ====================
    
    @Test
    public void testMultipleVerifySteps() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\"step\": \"0\", \"method\": \"GET\", \"vulPath\": \"/step1\"},\n" +
                "      {\"step\": \"1\", \"method\": \"GET\", \"vulPath\": \"/step2\"},\n" +
                "      {\"step\": \"2\", \"method\": \"GET\", \"vulPath\": \"/step3\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull(converted.getVerifySteps());
        assertEquals("verify步骤数量应为3", 3, converted.getVerifySteps().size());
        assertEquals("第一步path", "/step1", converted.getVerifySteps().get(0).getPath());
        assertEquals("第二步path", "/step2", converted.getVerifySteps().get(1).getPath());
        assertEquals("第三步path", "/step3", converted.getVerifySteps().get(2).getPath());
    }
    
    @Test
    public void testMultipleAttackSteps() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [\n" +
                "      {\"step\": \"0\", \"method\": \"GET\", \"vulPath\": \"/attack1\"},\n" +
                "      {\"step\": \"1\", \"method\": \"GET\", \"vulPath\": \"/attack2\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull(converted.getExploitSteps());
        assertEquals("attack步骤数量应为2", 2, converted.getExploitSteps().size());
    }
    
    @Test
    public void testEmptyVerifySteps() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": []\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertTrue("空verify应返回null或空列表",
            converted.getVerifySteps() == null || converted.getVerifySteps().isEmpty());
    }
    
    @Test
    public void testEmptyAttackSteps() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": []\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertTrue("空attack应返回null或空列表",
            converted.getExploitSteps() == null || converted.getExploitSteps().isEmpty());
    }
    
    // ==================== 文件测试用例 ====================
    
    @Test
    public void testDeepNestedResultFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/05-deep-nested-result.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertNotNull("attack步骤不应为null", converted.getExploitSteps());
        assertFalse("attack步骤不应为空", converted.getExploitSteps().isEmpty());
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertTrue("提取器数量应大于0", step.getExtractors() != null && step.getExtractors().size() > 0);
        
        // 验证深层嵌套结构
        Map<String, Object> output = step.getOutput();
        assertNotNull("output不应为null", output);
    }
    
    @Test
    public void testComplexMatchCombinationsFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/06-complex-match-combinations.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertNotNull("verify步骤不应为null", converted.getVerifySteps());
        assertEquals("verify步骤数量应为2", 2, converted.getVerifySteps().size());
        
        // 验证第一个步骤的复杂匹配
        PocObj.PocStep step0 = converted.getVerifySteps().get(0);
        assertTrue("应包含多个匹配器", step0.getMatchers().size() >= 2);
        
        boolean hasRegex = step0.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.REGEX);
        boolean hasTime = step0.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.TIME);
        boolean hasStatus = step0.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.STATUS);
        
        assertTrue("应包含REGEX匹配器", hasRegex);
        assertTrue("应包含TIME匹配器", hasTime);
        assertTrue("应包含STATUS匹配器", hasStatus);
    }
    
    @Test
    public void testMultipleStepsWithResultsFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/07-multiple-steps-with-results.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertNotNull("attack步骤不应为null", converted.getExploitSteps());
        assertEquals("attack步骤数量应为3", 3, converted.getExploitSteps().size());
        
        // 验证每个步骤都有result提取
        for (PocObj.PocStep step : converted.getExploitSteps()) {
            assertTrue("每个步骤都应包含output或extractors",
                (step.getOutput() != null && !step.getOutput().isEmpty()) ||
                (step.getExtractors() != null && !step.getExtractors().isEmpty()));
        }
    }
    
    @Test
    public void testEdgeCasesFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/08-edge-cases.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertNotNull("verify步骤不应为null", converted.getVerifySteps());
        assertNotNull("attack步骤不应为null", converted.getExploitSteps());
        
        // 验证边界情况处理
        // 空字段、空匹配、空结果等都应该被正确处理而不抛异常
    }
    
    @Test
    public void testComplexRegexExtractionFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/09-complex-regex-extraction.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        assertTrue("应包含多个提取器", step.getExtractors().size() > 0);
        
        // 验证复杂正则表达式被正确提取
        for (PocObj.Matcher extractor : step.getExtractors()) {
            assertNotNull("提取器值不应为null", extractor.getValues());
            assertFalse("提取器值不应为空", extractor.getValues().isEmpty());
            assertNotNull("提取器名称不应为null", extractor.getName());
        }
    }
    
    @Test
    public void testFullPocsuiteFeaturesFile() throws IOException {
        String filePath = "src/test/resources/pocsuite-samples/10-full-pocsuite-features.json";
        String jsonContent = readFile(filePath);
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(jsonContent, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        
        // 验证所有字段都被正确解析
        assertNotNull("ID不应为null", converted.getId());
        assertNotNull("名称不应为null", converted.getName());
        assertNotNull("作者不应为null", converted.getAuthor());
        assertNotNull("描述不应为null", converted.getDescription());
        assertNotNull("协议不应为null", converted.getProtocol());
        assertNotNull("漏洞类型不应为null", converted.getVulType());
        assertNotNull("版本不应为null", converted.getVersion());
        assertNotNull("创建时间不应为null", converted.getCreateTime());
        assertNotNull("更新时间不应为null", converted.getUpdateTime());
        assertNotNull("产品不应为null", converted.getProduct());
        assertNotNull("应用链接不应为null", converted.getAppPowerLink());
        assertNotNull("参考链接不应为null", converted.getReferences());
        
        // 验证步骤
        assertNotNull("verify步骤不应为null", converted.getVerifySteps());
        assertNotNull("attack步骤不应为null", converted.getExploitSteps());
        
        // 验证attack步骤的result提取
        PocObj.PocStep attackStep = converted.getExploitSteps().get(0);
        assertNotNull("attack步骤的output不应为null", attackStep.getOutput());
        assertTrue("attack步骤应包含多个提取器", attackStep.getExtractors().size() > 0);
    }
    
    // ==================== 空值和异常处理测试 ====================
    
    @Test
    public void testNullPocJson() {
        PocObj.Poc converted = converter.convert(null);
        assertNull("null输入应返回null", converted);
    }
    
    @Test
    public void testNullPocExecute() {
        PocsuiteJsonObj.PocJson pocJson = new PocsuiteJsonObj.PocJson();
        pocJson.setPocInfo(new PocsuiteJsonObj.PocInfo());
        pocJson.setPocExecute(null);
        
        PocObj.Poc converted = converter.convert(pocJson);
        assertNull("null pocExecute应返回null", converted);
    }
    
    @Test
    public void testNullPocInfo() {
        String pocJson = "{\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [{\"step\": \"0\", \"method\": \"GET\", \"vulPath\": \"/test\"}]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        // null pocInfo不应导致异常，应使用默认值
        assertNotNull("转换结果不应为null", converted);
    }
    
    // ==================== 字段映射测试 ====================
    
    @Test
    public void testFieldMapping() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\"vulID\": \"test\", \"name\": \"Test\"},\n" +
                "  \"pocExecute\": {\n" +
                "    \"attack\": [{\n" +
                "      \"step\": \"0\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"vulPath\": \"/test\",\n" +
                "      \"result\": {\n" +
                "        \"AdminInfo\": {\n" +
                "          \"Username\": \"<regex>username:([^\\\\s]+)\"\n" +
                "        },\n" +
                "        \"DBInfo\": {\n" +
                "          \"Hostname\": \"<regex>hostname:([^\\\\s]+)\"\n" +
                "        },\n" +
                "        \"FileInfo\": {\n" +
                "          \"Filename\": \"<regex>filename:([^\\\\s]+)\"\n" +
                "        },\n" +
                "        \"SiteAttr\": {\n" +
                "          \"Path\": \"<regex>path:([^\\\\s]+)\"\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        PocObj.PocStep step = converted.getExploitSteps().get(0);
        Map<String, Object> output = step.getOutput();
        
        // 验证字段名被正确映射为中文
        assertTrue("应包含管理员信息", output.containsKey("管理员信息"));
        assertTrue("应包含数据库内容", output.containsKey("数据库内容"));
        assertTrue("应包含文件信息", output.containsKey("文件信息"));
        assertTrue("应包含网站服务器信息", output.containsKey("网站服务器信息"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> adminMap = (Map<String, Object>) output.get("管理员信息");
        assertTrue("应包含管理员用户名", adminMap.containsKey("管理员用户名"));
        // 测试用例中只有Username字段，没有Password字段
    }
}
