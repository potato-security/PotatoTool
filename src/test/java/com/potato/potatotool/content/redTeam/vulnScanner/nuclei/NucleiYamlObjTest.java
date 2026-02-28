package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NucleiYamlObj 数据模型单元测试
 * 测试 Nuclei YAML POC 的数据结构解析
 * 
 * @author Potato
 * @date 2025-11-02
 */
@DisplayName("NucleiYamlObj 数据模型测试")
public class NucleiYamlObjTest {
    
    @Test
    @DisplayName("测试基本POC结构解析")
    public void testBasicPocStructure() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/01-basic-http.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc, "POC解析结果不应为null");
        assertNotNull(poc.getInfo(), "Info字段不应为null");
        assertEquals("basic-http-test", poc.getId(), "ID字段不匹配");
        assertEquals("Basic HTTP Test POC", poc.getInfo().getName(), "Name字段不匹配");
        assertEquals("Potato", poc.getInfo().getAuthor(), "Author字段不匹配");
        assertEquals(NucleiYamlObj.Info.Severity.info, poc.getInfo().getSeverity(), "Severity字段不匹配");
    }
    
    @Test
    @DisplayName("测试Info块所有字段")
    public void testInfoBlockFields() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/01-basic-http.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        NucleiYamlObj.Info info = poc.getInfo();
        assertNotNull(info);
        assertNotNull(info.getDescription());
        assertNotNull(info.getTags());
        assertTrue(info.getTags().contains("test"));
        assertTrue(info.getTags().contains("http"));
    }
    
    @Test
    @DisplayName("测试HTTP协议解析")
    public void testHttpProtocolParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/01-basic-http.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp(), "HTTP字段不应为null");
        assertFalse(poc.getHttp().isEmpty(), "HTTP列表不应为空");
        
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertEquals("GET", http.getMethod(), "Method字段不匹配");
        assertNotNull(http.getPath(), "Path字段不应为null");
        assertEquals("/api/test", http.getPath().get(0), "Path值不匹配");
        assertNotNull(http.getHeaders(), "Headers字段不应为null");
        assertEquals("Mozilla/5.0", http.getHeaders().get("User-Agent"), "User-Agent不匹配");
    }
    
    @Test
    @DisplayName("测试所有匹配器类型解析")
    public void testAllMatchersParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/02-all-matchers.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getMatchers());
        assertEquals(8, http.getMatchers().size(), "应该有8个匹配器");
        
        // 验证各种匹配器类型
        boolean hasStatus = false, hasWord = false, hasRegex = false, hasBinary = false;
        boolean hasDsl = false, hasJson = false, hasKval = false, hasXpath = false;
        
        for (NucleiYamlObj.TemplateMatcher matcher : http.getMatchers()) {
            switch (matcher.getType()) {
                case "status":
                    hasStatus = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Status);
                    break;
                case "word":
                    hasWord = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Word);
                    break;
                case "regex":
                    hasRegex = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Regex);
                    break;
                case "binary":
                    hasBinary = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Binary);
                    break;
                case "dsl":
                    hasDsl = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Dsl);
                    break;
                case "json":
                    hasJson = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Json);
                    break;
                case "kval":
                    hasKval = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Kval);
                    break;
                case "xpath":
                    hasXpath = true;
                    assertTrue(matcher instanceof NucleiYamlObj.Xpath);
                    break;
            }
        }
        
        assertTrue(hasStatus && hasWord && hasRegex && hasBinary && 
                  hasDsl && hasJson && hasKval && hasXpath, 
                  "所有匹配器类型都应该被解析");
    }
    
    @Test
    @DisplayName("测试提取器解析")
    public void testExtractorsParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/03-extractors.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getExtractors());
        assertTrue(http.getExtractors().size() >= 5, "应该有至少5个提取器");
        
        // 验证提取器类型
        for (NucleiYamlObj.TemplateMatcher extractor : http.getExtractors()) {
            assertNotNull(extractor.getType(), "提取器类型不应为null");
            assertTrue(extractor.getType().equals("regex") || 
                      extractor.getType().equals("json") ||
                      extractor.getType().equals("xpath") ||
                      extractor.getType().equals("dsl") ||
                      extractor.getType().equals("kval"),
                      "提取器类型应该是支持的类型之一");
        }
    }
    
    @Test
    @DisplayName("测试TCP协议解析")
    public void testTcpProtocolParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/04-tcp-protocol.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getTcp(), "TCP字段不应为null");
        assertFalse(poc.getTcp().isEmpty(), "TCP列表不应为空");
        
        NucleiYamlObj.Tcp tcp = poc.getTcp().get(0);
        assertNotNull(tcp.getHost(), "Host字段不应为null");
        assertNotNull(tcp.getPort(), "Port字段不应为null");
        assertEquals("6379,6380", tcp.getPort(), "Port值不匹配");
        assertNotNull(tcp.getInputs(), "Inputs字段不应为null");
        assertFalse(tcp.getInputs().isEmpty(), "Inputs列表不应为空");
        
        NucleiYamlObj.Input input = tcp.getInputs().get(0);
        assertEquals("PING\r\n", input.getData(), "Input data不匹配");
        assertEquals(NucleiYamlObj.InputType.hex, input.getType(), "Input type不匹配");
    }
    
    @Test
    @DisplayName("测试变量解析")
    public void testVariablesParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/05-variables-flow.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getVariables(), "Variables字段不应为null");
        assertFalse(poc.getVariables().isEmpty(), "Variables不应为空");
        assertEquals("test-key-123", poc.getVariables().get("api_key"), "api_key变量不匹配");
        assertEquals("1.0", poc.getVariables().get("version"), "version变量不匹配");
    }
    
    @Test
    @DisplayName("测试Flow流程控制解析")
    public void testFlowParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/05-variables-flow.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getFlow(), "Flow字段不应为null");
        assertEquals("http(1) && http(2)", poc.getFlow(), "Flow值不匹配");
    }
    
    @Test
    @DisplayName("测试Payloads解析")
    public void testPayloadsParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/06-payloads-fuzzing.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getPayloads(), "Payloads字段不应为null");
        assertFalse(http.getPayloads().isEmpty(), "Payloads不应为空");
        
        Object payloadValue = http.getPayloads().get("payload");
        assertNotNull(payloadValue, "payload键不应为null");
        assertTrue(payloadValue instanceof List, "payload值应该是List类型");
        
        @SuppressWarnings("unchecked")
        List<String> payloadList = (List<String>) payloadValue;
        assertTrue(payloadList.contains("test1"), "payload应包含test1");
        assertTrue(payloadList.contains("test2"), "payload应包含test2");
    }
    
    @Test
    @DisplayName("测试Attack模式解析")
    public void testAttackModeParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/06-payloads-fuzzing.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getAttack(), "Attack字段不应为null");
        assertEquals(com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.VariablesType.clusterbomb, 
                    http.getAttack(), "Attack模式不匹配");
    }
    
    @Test
    @DisplayName("测试DSL表达式解析")
    public void testDslExpressionsParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/07-dsl-expressions.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getMatchers());
        
        boolean foundDsl = false;
        for (NucleiYamlObj.TemplateMatcher matcher : http.getMatchers()) {
            if (matcher instanceof NucleiYamlObj.Dsl) {
                foundDsl = true;
                NucleiYamlObj.Dsl dsl = (NucleiYamlObj.Dsl) matcher;
                assertNotNull(dsl.getDsl(), "DSL表达式列表不应为null");
                assertFalse(dsl.getDsl().isEmpty(), "DSL表达式列表不应为空");
                assertTrue(dsl.getDsl().contains("status_code == 200"), "应包含status_code表达式");
                break;
            }
        }
        assertTrue(foundDsl, "应该找到DSL匹配器");
    }
    
    @Test
    @DisplayName("测试高级特性解析")
    public void testAdvancedFeaturesParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/08-advanced-features.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertTrue(http.isCookie_reuse(), "cookie-reuse应为true");
        assertTrue(http.isRedirects(), "redirects应为true");
        assertFalse(http.isDisable_cookie(), "disable-cookie应为false");
        assertFalse(http.isUnsafe(), "unsafe应为false");
        assertTrue(http.isStop_at_first_match(), "stop-at-first-match应为true");
        assertEquals(5, http.getThreads(), "threads应为5");
    }
    
    @Test
    @DisplayName("测试多步骤解析")
    public void testMultiStepParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/09-multi-step.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        assertEquals(2, poc.getHttp().size(), "应该有2个HTTP步骤");
        
        // 验证第一步有提取器
        NucleiYamlObj.Http step1 = poc.getHttp().get(0);
        assertNotNull(step1.getExtractors(), "第一步应有提取器");
        assertFalse(step1.getExtractors().isEmpty(), "提取器列表不应为空");
        
        // 验证第二步有匹配器
        NucleiYamlObj.Http step2 = poc.getHttp().get(1);
        assertNotNull(step2.getMatchers(), "第二步应有匹配器");
    }
    
    @Test
    @DisplayName("测试Raw请求解析")
    public void testRawRequestParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/11-raw-request.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        assertNotNull(http.getRaw(), "Raw字段不应为null");
        assertFalse(http.getRaw().isEmpty(), "Raw列表不应为空");
        assertTrue(http.getRaw().get(0).contains("GET /api/test"), "Raw内容应包含请求行");
    }
    
    @Test
    @DisplayName("测试复杂嵌套结构解析")
    public void testComplexNestedParsing() throws IOException {
        String yamlPath = "src/test/resources/nuclei-poc-samples/12-complex-nested.yml";
        NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(yamlPath);
        
        assertNotNull(poc.getHttp());
        NucleiYamlObj.Http http = poc.getHttp().get(0);
        
        // 验证匹配器条件
        assertEquals(NucleiYamlObj.MatchersCondition.or, http.getMatchers_condition(), 
                    "matchers-condition应为or");
        
        // 验证提取器
        assertNotNull(http.getExtractors(), "应有提取器");
        assertTrue(http.getExtractors().size() >= 2, "应有至少2个提取器");
    }
    
    @Test
    @DisplayName("测试空POC处理")
    public void testEmptyPoc() {
        NucleiYamlObj.Poc poc = new NucleiYamlObj.Poc();
        assertNotNull(poc);
        assertNull(poc.getInfo());
        assertNull(poc.getHttp());
    }
    
    @Test
    @DisplayName("测试Metadata元数据解析")
    public void testMetadataParsing() throws IOException {
        // 创建一个包含metadata的测试文件
        String yamlContent = "id: metadata-test\n" +
            "info:\n" +
            "  name: Metadata Test\n" +
            "  severity: info\n" +
            "  metadata:\n" +
            "    max-request: 5\n" +
            "    fofa-query: 'title=\"test\"'\n" +
            "    shodan-query: 'product:apache'\n" +
            "http:\n" +
            "  - method: GET\n" +
            "    path:\n" +
            "      - /\n";
        
        java.nio.file.Path tempFile = Files.createTempFile("nuclei-test-", ".yml");
        Files.write(tempFile, yamlContent.getBytes());
        
        try {
            NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(tempFile.toString());
            assertNotNull(poc.getInfo());
            assertNotNull(poc.getInfo().getMetadata());
            assertEquals(5, poc.getInfo().getMetadata().getMax_request());
            assertNotNull(poc.getInfo().getMetadata().getFofa_query());
            assertNotNull(poc.getInfo().getMetadata().getShodan_query());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}

