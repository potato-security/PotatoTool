package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NucleiPocConverter 转换器单元测试
 * 
 * @author Potato
 * @date 2025-11-02
 */
@DisplayName("NucleiPocConverter 转换器测试")
public class NucleiPocConverterTest {
    
    private NucleiPocConverter converter;
    
    @BeforeEach
    public void setUp() {
        converter = new NucleiPocConverter();
    }
    
    @Test
    @DisplayName("测试基本POC转换")
    public void testBasicPocConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/01-basic-http.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc, "转换结果不应为null");
        assertEquals("basic-http-test", poc.getId(), "ID不匹配");
        assertEquals("Basic HTTP Test POC", poc.getName(), "Name不匹配");
        assertEquals("Potato", poc.getAuthor(), "Author不匹配");
        assertEquals(PocObj.Severity.INFO, poc.getSeverity(), "Severity不匹配");
        assertNotNull(poc.getVerifySteps(), "VerifySteps不应为null");
        assertFalse(poc.getVerifySteps().isEmpty(), "VerifySteps不应为空");
        assertEquals("nuclei", poc.getOriginalFormat(), "OriginalFormat应为nuclei");
    }
    
    @Test
    @DisplayName("测试所有匹配器类型转换")
    public void testAllMatchersConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/02-all-matchers.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getMatchers());
        
        // 验证各种匹配器类型都被转换
        boolean hasStatus = false, hasWord = false, hasRegex = false, hasBinary = false;
        boolean hasDsl = false, hasJson = false, hasKval = false, hasXpath = false;
        
        for (PocObj.Matcher matcher : step.getMatchers()) {
            switch (matcher.getType()) {
                case STATUS:
                    hasStatus = true;
                    break;
                case WORD:
                    hasWord = true;
                    break;
                case REGEX:
                    hasRegex = true;
                    break;
                case BINARY:
                    hasBinary = true;
                    break;
                case DSL:
                    hasDsl = true;
                    break;
                case JSON:
                    hasJson = true;
                    break;
                case KVAL:
                    hasKval = true;
                    break;
                case XPATH:
                    hasXpath = true;
                    break;
                default:
                    // 其他类型忽略
                    break;
            }
        }
        
        assertTrue(hasStatus && hasWord && hasRegex && hasBinary && 
                  hasDsl && hasJson && hasKval && hasXpath,
                  "所有匹配器类型都应该被转换");
    }
    
    @Test
    @DisplayName("测试提取器转换")
    public void testExtractorsConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/03-extractors.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getExtractors(), "Extractors不应为null");
        assertTrue(step.getExtractors().size() >= 5, "应有至少5个提取器");
        
        // 验证提取器名称（用作变量名）
        boolean hasToken = false, hasUserId = false;
        for (PocObj.Matcher extractor : step.getExtractors()) {
            if ("token".equals(extractor.getName())) {
                hasToken = true;
            }
            if ("user_id".equals(extractor.getName())) {
                hasUserId = true;
            }
        }
        assertTrue(hasToken || hasUserId, "应至少有一个命名提取器");
    }
    
    @Test
    @DisplayName("测试TCP协议转换")
    public void testTcpProtocolConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/04-tcp-protocol.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertEquals("tcp", poc.getProtocol(), "Protocol应为tcp");
        assertNotNull(poc.getVerifySteps(), "VerifySteps不应为null");
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.TcpStep, "步骤应为TcpStep类型");
        
        PocObj.TcpStep tcpStep = (PocObj.TcpStep) step;
        assertNotNull(tcpStep.getPort(), "Port不应为null");
        assertNotNull(tcpStep.getInputs(), "Inputs不应为null");
    }
    
    @Test
    @DisplayName("测试变量和流程转换")
    public void testVariablesAndFlowConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/05-variables-flow.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVariables(), "Variables不应为null");
        assertTrue(poc.getVariables().containsKey("api_key"), "应包含api_key变量");
        assertNotNull(poc.getFlow(), "Flow不应为null");
        assertEquals("http(1) && http(2)", poc.getFlow(), "Flow值不匹配");
    }
    
    @Test
    @DisplayName("测试Payloads转换")
    public void testPayloadsConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/06-payloads-fuzzing.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVariables(), "Variables不应为null");
        assertTrue(poc.getVariables().containsKey("payload"), "应包含payload变量");
        assertTrue(poc.getVariables().containsKey("type"), "应包含type变量");
        
        // 验证payload值被转换为List格式
        assertTrue(poc.getVariables().get("payload") instanceof java.util.List, 
                  "payload值应为List类型");
        assertNotNull(poc.getVariablesType(), "VariablesType不应为null");
    }
    
    @Test
    @DisplayName("测试DSL表达式转换")
    public void testDslExpressionsConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/07-dsl-expressions.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getMatchers());
        
        // 验证DSL匹配器被转换
        boolean foundDsl = false;
        for (PocObj.Matcher matcher : step.getMatchers()) {
            if (matcher.getType() == PocObj.MatcherType.DSL) {
                foundDsl = true;
                assertNotNull(matcher.getValues(), "DSL表达式不应为null");
                assertFalse(matcher.getValues().isEmpty(), "DSL表达式列表不应为空");
                break;
            }
        }
        assertTrue(foundDsl, "应找到DSL匹配器");
    }
    
    @Test
    @DisplayName("测试高级特性转换")
    public void testAdvancedFeaturesConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/08-advanced-features.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getGlobalConfig(), "GlobalConfig不应为null");
        assertTrue(poc.getGlobalConfig().isCookieReuse(), "CookieReuse应为true");
        assertTrue(poc.getGlobalConfig().isStopAtFirstMatch(), "StopAtFirstMatch应为true");
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step.isFollowRedirect(), "FollowRedirect应为true");
        assertFalse(step.isDisableCookie(), "DisableCookie应为false");
    }
    
    @Test
    @DisplayName("测试多步骤转换")
    public void testMultiStepConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/09-multi-step.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps(), "VerifySteps不应为null");
        assertEquals(2, poc.getVerifySteps().size(), "应有2个验证步骤");
        
        // 验证第一步有提取器
        PocObj.PocStep step1 = poc.getVerifySteps().get(0);
        assertNotNull(step1.getExtractors(), "第一步应有提取器");
        assertFalse(step1.getExtractors().isEmpty(), "提取器列表不应为空");
        
        // 验证流程控制
        assertNotNull(poc.getFlow(), "Flow不应为null");
    }
    
    @Test
    @DisplayName("测试Raw请求转换")
    public void testRawRequestConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/11-raw-request.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getRaw(), "Raw字段不应为null");
        assertFalse(step.getRaw().isEmpty(), "Raw列表不应为空");
    }
    
    @Test
    @DisplayName("测试复杂嵌套转换")
    public void testComplexNestedConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/12-complex-nested.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc);
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getMatchers(), "Matchers不应为null");
        assertEquals(PocObj.MatchersCondition.OR, step.getMatchersCondition(), 
                    "MatchersCondition应为OR");
        assertNotNull(step.getExtractors(), "Extractors不应为null");
    }
    
    @Test
    @DisplayName("测试null POC处理")
    public void testNullPocConversion() {
        PocObj.Poc poc = converter.convert(null);
        assertNull(poc, "null输入应返回null");
    }
    
    @Test
    @DisplayName("测试空HTTP/TCP POC处理")
    public void testEmptyProtocolPoc() {
        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("test");
        nucleiPoc.setInfo(new NucleiYamlObj.Info());
        nucleiPoc.getInfo().setName("Test");
        // 不设置http、requests或tcp
        
        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNull(poc, "没有协议块的POC应返回null");
    }
    
    @Test
    @DisplayName("测试严重程度转换")
    public void testSeverityConversion() throws IOException {
        String[] severities = {"info", "low", "medium", "high", "critical", "unknown"};
        PocObj.Severity[] expected = {
            PocObj.Severity.INFO,
            PocObj.Severity.LOW,
            PocObj.Severity.MEDIUM,
            PocObj.Severity.HIGH,
            PocObj.Severity.CRITICAL,
            PocObj.Severity.UNKNOWN
        };
        
        for (int i = 0; i < severities.length; i++) {
            String yamlContent = String.format(
                "id: test\n" +
                "info:\n" +
                "  name: Test\n" +
                "  severity: %s\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - /\n",
                severities[i]
            );
            
            java.nio.file.Path tempFile = Files.createTempFile("nuclei-test-", ".yml");
            Files.write(tempFile, yamlContent.getBytes());
            
            try {
                NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(tempFile.toString());
                PocObj.Poc poc = converter.convert(nucleiPoc);
                assertEquals(expected[i], poc.getSeverity(), 
                            "Severity " + severities[i] + " 转换不正确");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }
    
    @Test
    @DisplayName("测试Reference字段转换（字符串和数组）")
    public void testReferenceConversion() throws IOException {
        // 测试字符串格式
        String yaml1 = "id: test\n" +
            "info:\n" +
            "  name: Test\n" +
            "  reference: https://example.com\n" +
            "http:\n" +
            "  - method: GET\n" +
            "    path:\n" +
            "      - /\n";
        
        java.nio.file.Path tempFile1 = Files.createTempFile("nuclei-test-", ".yml");
        Files.write(tempFile1, yaml1.getBytes());
        
        try {
            NucleiYamlObj.Poc nucleiPoc1 = PocConverter.loadNucleiYamlPocFile(tempFile1.toString());
            PocObj.Poc poc1 = converter.convert(nucleiPoc1);
            assertNotNull(poc1.getReferences(), "References不应为null");
            assertEquals(1, poc1.getReferences().size(), "应有1个参考链接");
        } finally {
            Files.deleteIfExists(tempFile1);
        }
        
        // 测试数组格式
        String yaml2 = "id: test\n" +
            "info:\n" +
            "  name: Test\n" +
            "  reference:\n" +
            "    - https://example.com\n" +
            "    - https://cve.mitre.org\n" +
            "http:\n" +
            "  - method: GET\n" +
            "    path:\n" +
            "      - /\n";
        
        java.nio.file.Path tempFile2 = Files.createTempFile("nuclei-test-", ".yml");
        Files.write(tempFile2, yaml2.getBytes());
        
        try {
            NucleiYamlObj.Poc nucleiPoc2 = PocConverter.loadNucleiYamlPocFile(tempFile2.toString());
            PocObj.Poc poc2 = converter.convert(nucleiPoc2);
            assertNotNull(poc2.getReferences(), "References不应为null");
            assertEquals(2, poc2.getReferences().size(), "应有2个参考链接");
        } finally {
            Files.deleteIfExists(tempFile2);
        }
    }
    
    @Test
    @DisplayName("测试搜索查询转换")
    public void testSearchQueriesConversion() throws IOException {
        String yamlContent = "id: test\n" +
            "info:\n" +
            "  name: Test\n" +
            "  metadata:\n" +
            "    fofa-query: 'title=\"test\"'\n" +
            "    shodan-query: 'product:apache'\n" +
            "    zoomeye-query: 'app:nginx'\n" +
            "http:\n" +
            "  - method: GET\n" +
            "    path:\n" +
            "      - /\n";
        
        java.nio.file.Path tempFile = Files.createTempFile("nuclei-test-", ".yml");
        Files.write(tempFile, yamlContent.getBytes());
        
        try {
            NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(tempFile.toString());
            PocObj.Poc poc = converter.convert(nucleiPoc);
            assertNotNull(poc.getSearchQueries(), "SearchQueries不应为null");
            assertTrue(poc.getSearchQueries().containsKey("fofa"), "应包含fofa查询");
            assertTrue(poc.getSearchQueries().containsKey("shodan"), "应包含shodan查询");
            assertTrue(poc.getSearchQueries().containsKey("zoomeye"), "应包含zoomeye查询");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}

