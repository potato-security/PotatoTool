package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
        boolean hasStatus = false, hasSize = false, hasWord = false, hasRegex = false, hasBinary = false;
        boolean hasDsl = false, hasTime = false, hasJson = false, hasKval = false, hasXpath = false;

        for (PocObj.Matcher matcher : step.getMatchers()) {
            switch (matcher.getType()) {
                case STATUS:
                    hasStatus = true;
                    break;
                case SIZE:
                    hasSize = true;
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
                case TIME:
                    hasTime = true;
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

        assertTrue(hasStatus && hasSize && hasWord && hasRegex && hasBinary &&
                  hasDsl && hasTime && hasJson && hasKval && hasXpath,
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
    @DisplayName("测试Network协议按TCP兼容转换")
    public void testNetworkProtocolConversion() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/31-network-protocol.yml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);

        assertNotNull(nucleiPoc, "YAML解析结果不应为null");
        assertNotNull(nucleiPoc.getNetwork(), "Network字段应被解析");
        assertFalse(nucleiPoc.getNetwork().isEmpty(), "Network列表不应为空");

        PocObj.Poc poc = converter.convert(nucleiPoc);

        assertNotNull(poc);
        assertEquals("tcp", poc.getProtocol(), "Network协议应映射为tcp执行协议");
        assertNotNull(poc.getVerifySteps(), "VerifySteps不应为null");
        assertFalse(poc.getVerifySteps().isEmpty(), "VerifySteps不应为空");

        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.TcpStep, "Network步骤应转换为TcpStep");

        PocObj.TcpStep tcpStep = (PocObj.TcpStep) step;
        assertEquals("6379,6380", tcpStep.getPort(), "端口映射不正确");
        assertNotNull(tcpStep.getInputs(), "Inputs不应为null");
        assertFalse(tcpStep.getInputs().isEmpty(), "Inputs不应为空");
        assertNotNull(tcpStep.getMatchers(), "Matchers不应为null");
        assertFalse(tcpStep.getMatchers().isEmpty(), "Matchers不应为空");
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
    @DisplayName("测试空协议块POC降级诊断")
    public void testEmptyProtocolPoc() {
        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("test");
        nucleiPoc.setInfo(new NucleiYamlObj.Info());
        nucleiPoc.getInfo().setName("Test");
        // 不设置任何协议块

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc, "没有协议块时应返回带诊断信息的POC");
        assertNotNull(poc.getUnsupportedCapabilities(), "UnsupportedCapabilities不应为null");
        assertFalse(poc.getUnsupportedCapabilities().isEmpty(), "应记录协议缺失能力项");
        Object code = poc.getUnsupportedCapabilities().get(0).get("code");
        assertEquals("NO_PROTOCOL_BLOCK", String.valueOf(code), "应标记NO_PROTOCOL_BLOCK");
        assertNotNull(poc.getVerifySteps(), "VerifySteps不应为null");
        assertTrue(poc.getVerifySteps().isEmpty(), "没有协议块时不应生成执行步骤");
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
    @DisplayName("测试JSON/XPATH/KVAL匹配器字段映射(part/negative)")
    public void testMatcherPartAndNegativeMapping() {
        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("matcher-part-negative-test");

        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("matcher-part-negative-test");
        info.setSeverity(NucleiYamlObj.Info.Severity.low);
        nucleiPoc.setInfo(info);

        NucleiYamlObj.Http http = new NucleiYamlObj.Http();
        http.setMethod("GET");
        http.setPath(java.util.Collections.singletonList("/"));

        NucleiYamlObj.Json json = new NucleiYamlObj.Json();
        json.setJson(java.util.Collections.singletonList("$.data.token"));
        json.setPart("header");
        json.setNegative(true);
        json.setName("json_m");

        NucleiYamlObj.Xpath xpath = new NucleiYamlObj.Xpath();
        xpath.setXpath(java.util.Collections.singletonList("//user"));
        xpath.setPart("all");
        xpath.setNegative(true);
        xpath.setName("xpath_m");

        NucleiYamlObj.Kval kval = new NucleiYamlObj.Kval();
        kval.setKval(java.util.Collections.singletonList("set-cookie"));
        kval.setPart("header");
        kval.setNegative(true);
        kval.setName("kval_m");

        http.setMatchers(java.util.Arrays.asList(json, xpath, kval));
        nucleiPoc.setHttp(java.util.Collections.singletonList(http));

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());

        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getMatchers());

        PocObj.Matcher jsonMatcher = step.getMatchers().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.JSON)
                .findFirst()
                .orElse(null);
        PocObj.Matcher xpathMatcher = step.getMatchers().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.XPATH)
                .findFirst()
                .orElse(null);
        PocObj.Matcher kvalMatcher = step.getMatchers().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.KVAL)
                .findFirst()
                .orElse(null);

        assertNotNull(jsonMatcher);
        assertEquals("header", jsonMatcher.getPart());
        assertTrue(jsonMatcher.isNegative());

        assertNotNull(xpathMatcher);
        assertEquals("all", xpathMatcher.getPart());
        assertTrue(xpathMatcher.isNegative());

        assertNotNull(kvalMatcher);
        assertEquals("header", kvalMatcher.getPart());
        assertTrue(kvalMatcher.isNegative());
    }

    @Test
    @DisplayName("测试JSON/XPATH/KVAL提取器字段映射(part/negative)")
    public void testExtractorPartAndNegativeMapping() {
        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("extractor-part-negative-test");

        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("extractor-part-negative-test");
        info.setSeverity(NucleiYamlObj.Info.Severity.low);
        nucleiPoc.setInfo(info);

        NucleiYamlObj.Http http = new NucleiYamlObj.Http();
        http.setMethod("GET");
        http.setPath(java.util.Collections.singletonList("/"));

        NucleiYamlObj.Json json = new NucleiYamlObj.Json();
        json.setJson(java.util.Collections.singletonList("$.data.token"));
        json.setPart("header");
        json.setNegative(true);
        json.setName("json_e");

        NucleiYamlObj.Xpath xpath = new NucleiYamlObj.Xpath();
        xpath.setXpath(java.util.Collections.singletonList("//token"));
        xpath.setPart("all");
        xpath.setNegative(true);
        xpath.setName("xpath_e");

        NucleiYamlObj.Kval kval = new NucleiYamlObj.Kval();
        kval.setKval(java.util.Collections.singletonList("set-cookie"));
        kval.setPart("header");
        kval.setNegative(true);
        kval.setName("kval_e");

        http.setExtractors(java.util.Arrays.asList(json, xpath, kval));
        nucleiPoc.setHttp(java.util.Collections.singletonList(http));

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());

        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertNotNull(step.getExtractors());

        PocObj.Matcher jsonExtractor = step.getExtractors().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.JSON)
                .findFirst()
                .orElse(null);
        PocObj.Matcher xpathExtractor = step.getExtractors().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.XPATH)
                .findFirst()
                .orElse(null);
        PocObj.Matcher kvalExtractor = step.getExtractors().stream()
                .filter(m -> m.getType() == PocObj.MatcherType.KVAL)
                .findFirst()
                .orElse(null);

        assertNotNull(jsonExtractor);
        assertEquals("header", jsonExtractor.getPart());
        assertTrue(jsonExtractor.isNegative());

        assertNotNull(xpathExtractor);
        assertEquals("all", xpathExtractor.getPart());
        assertTrue(xpathExtractor.isNegative());

        assertNotNull(kvalExtractor);
        assertEquals("header", kvalExtractor.getPart());
        assertTrue(kvalExtractor.isNegative());
    }

    @Test
    @DisplayName("测试 network 模板中的 unpack 子串链路")
    public void testUnpackAndSubstrForNetworkTemplate_CVE_2022_24706() throws IOException {
        String yamlPath = "src/main/resources/poc/nucleiPoc/network/cves/2022/CVE-2022-24706.yaml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        assertNotNull(nucleiPoc, "真实模板应可解析");

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc, "真实模板应可转换");

        Map<String, Object> context = new HashMap<>();
        context.put("challenge", "000000000000000000000001");

        String unpacked = DslEvaluatorRefactored.evaluateFunctionForValue("unpack('>I',substr(challenge, 9, 13))", context);
        assertNotNull(unpacked, "unpack 应可执行");
        assertTrue(unpacked.matches("^-?\\d+$"), "unpack 结果应为数值字符串");
    }

    @Test
    @DisplayName("测试证据驱动函数 public_ip 与 zip")
    public void testEvidenceDrivenDslFunctions_PublicIpAndZip() {
        Map<String, Object> context = new HashMap<>();
        context.put("payload", "hello-zip");

        String publicIp = DslEvaluatorRefactored.evaluateFunctionForValue("public_ip()", context);
        assertEquals("{{ip}}", publicIp, "public_ip 应保持占位语义");

        String zippedRaw = DslEvaluatorRefactored.evaluateFunctionForValue("zip(\"hosts/main.xml\", payload)", context);
        assertNotNull(zippedRaw, "zip 结果不应为 null");

        byte[] zipBytes = zippedRaw.getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(zipBytes.length > 4, "zip 输出长度应大于文件头长度");
        assertEquals('P', zipBytes[0], "zip 文件头应为 PK");
        assertEquals('K', zipBytes[1], "zip 文件头应为 PK");

        String noPathZip = DslEvaluatorRefactored.evaluateFunctionForValue("zip(\"\", payload)", context);
        assertNotNull(noPathZip, "zip 空文件名场景应可降级执行");
    }

    @Test
    @DisplayName("测试 CVE-2024-55956 样例中 zip 能力可执行")
    public void testZipFunctionFromRealTemplate_CVE_2024_55956() throws IOException {
        String yamlPath = "src/main/resources/poc/nucleiPoc/http/cves/2024/CVE-2024-55956.yaml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        assertNotNull(nucleiPoc, "真实模板应可解析");

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc, "真实模板应可转换");

        Map<String, Object> context = new HashMap<>();
        context.put("payload", "zip-content-check");
        String zipBody = DslEvaluatorRefactored.evaluateFunctionForValue("zip(\"hosts/main.xml\", payload)", context);

        assertNotNull(zipBody, "CVE-2024-55956 所依赖的 zip 函数应可执行");
        assertTrue(zipBody.getBytes(StandardCharsets.ISO_8859_1).length > 10, "zip 输出应包含有效内容");
    }

    @Test
    @DisplayName("测试 gophish 样例中的 html_unescape 链路")
    public void testHtmlUnescapeFunctionFromRealTemplate_GophishDefaultLogin() throws IOException {
        String yamlPath = "src/main/resources/poc/nucleiPoc/http/default-logins/gophish/gophish-default-login.yaml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        assertNotNull(nucleiPoc, "真实模板应可解析");

        PocObj.Poc poc = converter.convert(nucleiPoc);
        assertNotNull(poc, "真实模板应可转换");

        Map<String, Object> context = new HashMap<>();
        context.put("csrf_token", "a&amp;b c");

        String htmlUnescaped = DslEvaluatorRefactored.evaluateFunctionForValue("html_unescape(csrf_token)", context);
        assertEquals("a&b c", htmlUnescaped, "html_unescape 应正确解码 HTML 实体");

        String chained = DslEvaluatorRefactored.evaluateFunctionForValue(
                "replace(url_encode(html_unescape(csrf_token)), \"+\", \"%2B\")", context);
        assertEquals("a%26b%2Bc", chained, "应与 gophish 模板中的链式表达式语义一致");
    }

    @Test
    @DisplayName("测试变量 DSL 表达式支持加法与拼接")
    public void testDslVariableExpressionsWithPlusArithmeticAndConcat() throws Exception {
        PocExecutor executor = new PocExecutor(new ScanConfig());
        Method method = PocExecutor.class.getDeclaredMethod("evaluateDslExpression", String.class, String.class, Map.class);
        method.setAccessible(true);

        Map<String, Object> context = new HashMap<>();
        context.put("oast", "abc");

        String lenPayload = (String) method.invoke(executor, "{{len(oast) + 5}}", "http://example.com", context);
        assertEquals("8", lenPayload, "len(oast) + 5 应按数值加法计算");

        Map<String, Object> context2 = new HashMap<>();
        context2.put("b64_marshal_data", "AAA");
        context2.put("digest", "BBB");

        String finalPayload = (String) method.invoke(
                executor,
                "{{b64_marshal_data + '--' + digest}}",
                "http://example.com",
                context2);
        assertEquals("AAA--BBB", finalPayload, "字符串拼接表达式应按模板语义计算");

        Map<String, Object> context3 = new HashMap<>();
        context3.put("num1", "6");
        context3.put("num2", "7");

        String multiplied = (String) method.invoke(
                executor,
                "{{to_number(num1)*to_number(num2)}}",
                "http://example.com",
                context3);
        assertEquals("42", multiplied, "to_number(num1)*to_number(num2) 应按数值乘法计算");

        Map<String, Object> context4 = new HashMap<>();
        context4.put("orig_iat", "1000");

        String plusWithoutSpace = (String) method.invoke(
                executor,
                "{{to_number(orig_iat)+4000}}",
                "http://example.com",
                context4);
        assertEquals("5000", plusWithoutSpace, "无空格加法表达式应按数值计算");

        Map<String, Object> context5 = new HashMap<>();
        context5.put("orig_iat", "1000");
        context5.put("num1", "6");
        context5.put("num2", "7");

        String precedence = (String) method.invoke(
                executor,
                "{{to_number(orig_iat)+to_number(num1)*to_number(num2)}}",
                "http://example.com",
                context5);
        assertEquals("1042", precedence, "应遵循先乘后加的算术优先级");
    }

    @Test
    @DisplayName("测试变量 DSL 嵌套函数中的符号不会被算术解析误判")
    public void testDslVariableNestedFunctionsWithSlashAndPlusMinusLiterals() throws Exception {
        PocExecutor executor = new PocExecutor(new ScanConfig());
        Method method = PocExecutor.class.getDeclaredMethod("evaluateDslExpression", String.class, String.class, Map.class);
        method.setAccessible(true);

        Map<String, Object> context = new HashMap<>();
        context.put("code_verifier", "7BhCLfrzYxLzq3XzrfiA8TplZBDciJ0RZepiiDujJKwOaMDzMZWcqGvrCfYH6s735tzxteIUH1vWLP1D2xXm88O9XFEnxcx2");

        String expression = "{{ trim_right(replace(replace(base64(hex_decode(sha256(code_verifier))),'/','_'),'+','-'),'=') }}";
        String expected = DslEvaluatorRefactored.evaluateFunctionForValue(
                "trim_right(replace(replace(base64(hex_decode(sha256(code_verifier))),'/','_'),'+','-'),'=')",
                context);

        String actual = (String) method.invoke(executor, expression, "http://example.com", context);
        assertEquals(expected, actual, "嵌套函数参数中的 '/' '+' '-' 字面量不应触发算术路径误解析");

        Map<String, Object> context2 = new HashMap<>();
        context2.put("sha1Hash", "abc");
        context2.put("passwordRandom", "xyz");

        String expression2 = "{{ sha256(sha1Hash+passwordRandom+'cid2016') }}";
        String expected2 = DslEvaluatorRefactored.evaluateFunctionForValue(
                "sha256(sha1Hash+passwordRandom+'cid2016')",
                context2);

        String actual2 = (String) method.invoke(executor, expression2, "http://example.com", context2);
        assertEquals(expected2, actual2, "函数参数中的 '+' 拼接应由 DSL 函数求值而不是被算术路径误拆分");
    }
}

