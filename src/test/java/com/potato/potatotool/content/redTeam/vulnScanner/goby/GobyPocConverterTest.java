package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.GobyPocConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GobyPocConverter 转换器单元测试
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("GobyPocConverter 转换器测试")
public class GobyPocConverterTest {
    
    private GobyPocConverter converter;
    private Gson gson;
    
    @BeforeEach
    public void setUp() {
        converter = new GobyPocConverter();
        gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    @Test
    @DisplayName("测试基本POC转换")
    public void testBasicPocConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/01-basic-poc.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertEquals("Basic Test POC", poc.getName());
        assertEquals("Potato", poc.getAuthor());
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        assertEquals("goby", poc.getOriginalFormat());
    }
    
    @Test
    @DisplayName("测试嵌套检查转换")
    public void testNestedChecksConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/02-nested-checks.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertTrue(poc.getVerifySteps().size() > 0);
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getMatchers());
        assertTrue(step.getMatchers().size() > 0);
    }
    
    @Test
    @DisplayName("测试raw报文转换")
    public void testRawRequestConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/03-raw-request.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        // raw报文应该被解析并填充到对应字段
        assertNotNull(step.getMethod());
        assertNotNull(step.getPath());
    }
    
    @Test
    @DisplayName("测试变量提取转换")
    public void testVariableExtractionConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/04-variable-extraction.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertTrue(poc.getVerifySteps().size() >= 2);
        
        // 第一步应该有提取器
        PocObj.PocStep step1 = poc.getVerifySteps().get(0);
        assertNotNull(step1.getExtractors());
        assertFalse(step1.getExtractors().isEmpty());
    }

    @Test
    @DisplayName("测试 Goby request.set_variable 保留为请求前变量定义")
    public void testRequestSetVariablePreservedAsRequestVariables() throws Exception {
        String pocPath = "src/main/resources/poc/gobyPoc/ZhongXinJingDun_Default_administrator_password.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));

        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);

        assertNotNull(poc);
        assertTrue(poc.getVerifySteps().size() >= 2);

        PocObj.PocStep firstStep = poc.getVerifySteps().get(0);
        PocObj.PocStep secondStep = poc.getVerifySteps().get(1);

        assertTrue(firstStep.getRequestVariables().isEmpty(), "第一步 request.set_variable 为空时不应生成请求前变量");
        assertEquals(1, secondStep.getRequestVariables().size(), "第二步 request.set_variable 应保留在请求前变量定义中");
        assertEquals("check_code|lastheader|regex|check_code=(.*?);", secondStep.getRequestVariables().get(0));
    }
    
    @Test
    @DisplayName("测试多路径转换")
    public void testMultiPathConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/05-multi-path.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        // 多路径应该被展开为多个步骤
        assertTrue(poc.getVerifySteps().size() >= 4);
    }
    
    @Test
    @DisplayName("测试认证配置转换")
    public void testAuthenticationConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/06-authentication.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getGlobalConfig());
        assertNotNull(poc.getGlobalConfig().getAuthConfig());
        assertEquals("basic", poc.getGlobalConfig().getAuthConfig().get("type"));
    }
    
    @Test
    @DisplayName("测试全局变量转换")
    public void testGlobalVariablesConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/07-global-variables.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVariables());
        assertTrue(poc.getVariables().size() > 0);
        // 全局变量应该被转换为统一的variables格式
        assertTrue(poc.getVariables().containsKey("api_key"));
    }
    
    @Test
    @DisplayName("测试复杂嵌套POC转换")
    public void testComplexNestedConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/08-complex-nested.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertTrue(poc.getVerifySteps().size() > 0);
        assertTrue(poc.getExploitSteps().size() > 0);
        assertNotNull(poc.getVariables());
    }
    
    @Test
    @DisplayName("测试null POC处理")
    public void testNullPocConversion() {
        PocObj.Poc poc = converter.convert(null);
        assertNull(poc);
    }
    
    @Test
    @DisplayName("测试空ScanSteps POC处理")
    public void testEmptyScanStepsPoc() {
        GobyJsonObj.PocJson gobyPoc = new GobyJsonObj.PocJson();
        gobyPoc.setName("Empty POC");
        gobyPoc.setScanSteps(null);
        
        PocObj.Poc poc = converter.convert(gobyPoc);
        assertNull(poc);
    }
    
    @Test
    @DisplayName("测试严重等级转换")
    public void testSeverityConversion() {
        GobyJsonObj.PocJson gobyPoc = new GobyJsonObj.PocJson();
        gobyPoc.setName("Test");
        gobyPoc.setLevel("1"); // Critical
        gobyPoc.setScanSteps(java.util.Collections.emptyList());
        
        PocObj.Poc poc = converter.convert(gobyPoc);
        assertNotNull(poc);
        assertEquals(PocObj.Severity.CRITICAL, poc.getSeverity());
        
        // 测试其他等级
        gobyPoc.setLevel("high");
        poc = converter.convert(gobyPoc);
        assertEquals(PocObj.Severity.HIGH, poc.getSeverity());
        
        gobyPoc.setLevel("medium");
        poc = converter.convert(gobyPoc);
        assertEquals(PocObj.Severity.MEDIUM, poc.getSeverity());
        
        gobyPoc.setLevel("low");
        poc = converter.convert(gobyPoc);
        assertEquals(PocObj.Severity.LOW, poc.getSeverity());
        
        gobyPoc.setLevel("info");
        poc = converter.convert(gobyPoc);
        assertEquals(PocObj.Severity.INFO, poc.getSeverity());
    }
    
    @Test
    @DisplayName("测试ExpParams转换")
    public void testExpParamsConversion() throws Exception {
        String pocPath = "src/test/resources/goby-poc-samples/08-complex-nested.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));
        
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVariables());
        assertTrue(poc.getVariables().containsKey("cmd"));
    }
    
    @Test
    @DisplayName("测试真实Goby POC - Apache ActiveMQ")
    public void testRealGobySample1() throws Exception {
        String pocPath = "src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/gobyAllPoc/Apache_ActiveMQ_default_admin_account.json";
        File file = new File(pocPath);
        
        if (!file.exists()) {
            System.out.println("跳过测试：文件不存在 - " + pocPath);
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(file.toPath()));
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc, "POC转换结果不应为null");
        assertNotNull(poc.getName(), "POC名称不应为null");
        assertNotNull(poc.getVerifySteps(), "验证步骤不应为null");
    }
    
    @Test
    @DisplayName("测试真实Goby POC - Hikvision")
    public void testRealGobySample2() throws Exception {
        String pocPath = "src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/gobyAllPoc/HIKVISION.json";
        File file = new File(pocPath);
        
        if (!file.exists()) {
            System.out.println("跳过测试：文件不存在 - " + pocPath);
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(file.toPath()));
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc, "POC转换结果不应为null");
        assertNotNull(poc.getName(), "POC名称不应为null");
    }
    
    @Test
    @DisplayName("测试真实Goby POC - Spring Cloud Gateway")
    public void testRealGobySample3() throws Exception {
        String pocPath = "src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/gobyAllPoc/Spring_Cloud_Gateway_Actuator_API_SpEL_Code_Injection_CVE_2022_22947.json";
        File file = new File(pocPath);
        
        if (!file.exists()) {
            System.out.println("跳过测试：文件不存在 - " + pocPath);
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(file.toPath()));
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc, "POC转换结果不应为null");
    }
    
    @Test
    @DisplayName("测试Recommendation字段两种拼写转换")
    public void testRecommendationSpellingConversion() {
        GobyJsonObj.PocJson gobyPoc1 = new GobyJsonObj.PocJson();
        gobyPoc1.setName("Test1");
        gobyPoc1.setRecommendation("Update now");
        gobyPoc1.setScanSteps(java.util.Collections.emptyList());
        
        PocObj.Poc poc1 = converter.convert(gobyPoc1);
        assertEquals("Update now", poc1.getRecommendation());
        
        GobyJsonObj.PocJson gobyPoc2 = new GobyJsonObj.PocJson();
        gobyPoc2.setName("Test2");
        gobyPoc2.setRecommandation("Update now");
        gobyPoc2.setScanSteps(java.util.Collections.emptyList());
        
        PocObj.Poc poc2 = converter.convert(gobyPoc2);
        assertEquals("Update now", poc2.getRecommendation());
    }

    @Test
    @DisplayName("测试lastbody text提取器转换为正则提取")
    public void testLastBodyTextExtractorConversion() {
        String jsonContent = "{"
                + "\"Name\":\"LastBody Text Extractor\","
                + "\"ScanSteps\":[{"
                + "\"Request\":{\"method\":\"GET\",\"uri\":\"/\"},"
                + "\"SetVariable\":[\"dnstest|lastbody|text|\"],"
                + "\"ResponseTest\":{\"type\":\"item\",\"variable\":\"$code\",\"operation\":\"==\",\"value\":\"200\"}"
                + "}],"
                + "\"ExploitSteps\":[]"
                + "}";

        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);

        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        assertNotNull(poc.getVerifySteps().get(0).getExtractors());
        assertFalse(poc.getVerifySteps().get(0).getExtractors().isEmpty());

        PocObj.Matcher extractor = poc.getVerifySteps().get(0).getExtractors().get(0);
        assertEquals("dnstest", extractor.getName());
        assertEquals(PocObj.MatcherType.REGEX, extractor.getType());
        assertEquals(PocObj.OperationType.REGEX_MATCH, extractor.getOperation());
        assertEquals("body", extractor.getPart());
        assertEquals(-1, extractor.getGroup());
        assertEquals("(?s).*", extractor.getValues().get(0));
    }

    @Test
    @DisplayName("测试regex提取器默认提取首个分组")
    public void testRegexExtractorUsesFirstCaptureGroupByDefault() {
        String jsonContent = "{"
                + "\"Name\":\"Regex Extractor Group\","
                + "\"ScanSteps\":[{"
                + "\"Request\":{\"method\":\"GET\",\"uri\":\"/users/sign_in\"},"
                + "\"SetVariable\":[\"X-CSRF-Token|lastbody|regex|name=\\\"csrf-token\\\" content=\\\"([a-z0-9_-]+)\\\"\"],"
                + "\"ResponseTest\":{\"type\":\"item\",\"variable\":\"$code\",\"operation\":\"==\",\"value\":\"200\"}"
                + "}],"
                + "\"ExploitSteps\":[]"
                + "}";

        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);

        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        assertNotNull(poc.getVerifySteps().get(0).getExtractors());
        assertFalse(poc.getVerifySteps().get(0).getExtractors().isEmpty());

        PocObj.Matcher extractor = poc.getVerifySteps().get(0).getExtractors().get(0);
        assertEquals("X-CSRF-Token", extractor.getName());
        assertEquals(PocObj.MatcherType.REGEX, extractor.getType());
        assertEquals(-1, extractor.getGroup());
    }

    @Test
    @DisplayName("测试 Goby 状态码不等于操作转换")
    public void testStatusNotEqualConversion() throws Exception {
        String pocPath = "src/main/resources/poc/gobyPoc/cve_2022_1388_goby.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(pocPath)));

        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);

        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());

        List<PocObj.Matcher> matchers = poc.getVerifySteps().get(0).getMatchers();
        assertNotNull(matchers);
        assertEquals(2, matchers.size());

        PocObj.Matcher statusMatcher = matchers.get(0);
        assertEquals(PocObj.MatcherType.STATUS, statusMatcher.getType());
        assertEquals(PocObj.OperationType.NOT_EQUAL, statusMatcher.getOperation());
        assertEquals("status", statusMatcher.getPart());
        assertEquals("204", statusMatcher.getValues().get(0));
    }
}
