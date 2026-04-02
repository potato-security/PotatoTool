package com.potato.potatotool.content.redTeam.vulnScanner;

import com.google.gson.Gson;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.PocsuitePocConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

/**
 * Pocsuite JsonPoc 转换器测试
 * 重点测试时间盲注功能的实现
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class PocsuitePocConverterTest {
    
    private Gson gson;
    private PocsuitePocConverter converter;
    
    @BeforeEach
    public void setUp() {
        gson = new Gson();
        converter = new PocsuitePocConverter();
    }
    
    /**
     * 测试基本的 POC 信息解析
     */
    @Test
    public void testBasicPocInfoParsing() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"test-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"Test POC\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"SQL Injection\",\n" +
                "    \"author\": \"TestAuthor\",\n" +
                "    \"desc\": \"Test description\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/test\",\n" +
                "        \"status\": \"200\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertEquals("test-001", converted.getId());
        assertEquals("Test POC", converted.getName());
        assertEquals("http", converted.getProtocol());
        assertEquals("SQL Injection", converted.getVulType());
        assertEquals("TestAuthor", converted.getAuthor());
        assertEquals("pocsuite", converted.getOriginalFormat());
    }
    
    /**
     * 测试时间盲注匹配器的转换
     * 这是核心测试：验证 time 字段被正确转换为 TIME 类型的 Matcher
     */
    @Test
    public void testTimeBasedBlindInjection() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"time-blind-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"SQL Time-Based Blind Injection Test\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"SQL Injection\",\n" +
                "    \"author\": \"Potato\",\n" +
                "    \"desc\": \"测试时间盲注 POC\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/api/search\",\n" +
                "        \"params\": \"id=1' AND SLEEP(5)--\",\n" +
                "        \"match\": {\n" +
                "          \"time\": \"5\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        assertNotNull("verify步骤不应为null", converted.getVerifySteps());
        assertFalse("verify步骤不应为空", converted.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull("步骤的匹配器列表不应为null", step.getMatchers());
        
        // 查找 TIME 类型的匹配器
        PocObj.Matcher timeMatcher = step.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.TIME)
            .findFirst()
            .orElse(null);
        
        assertNotNull("应该存在 TIME 类型的匹配器", timeMatcher);
        assertNotNull("TIME 匹配器的值不应为null", timeMatcher.getValues());
        assertFalse("TIME 匹配器的值不应为空", timeMatcher.getValues().isEmpty());
        assertEquals("时间阈值应为5秒", "5", timeMatcher.getValues().get(0));
        assertEquals("操作类型应为 GREATER_EQUAL", PocObj.OperationType.GREATER_EQUAL, timeMatcher.getOperation());
        assertEquals("匹配部分应为 response_time", "response_time", timeMatcher.getPart());
    }
    
    /**
     * 测试同时包含正则匹配和时间匹配的 POC
     */
    @Test
    public void testMixedMatchersWithTime() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"mixed-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"Mixed Matchers Test\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"SQL Injection\",\n" +
                "    \"author\": \"Potato\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/api/test\",\n" +
                "        \"status\": \"200\",\n" +
                "        \"match\": {\n" +
                "          \"regex\": [\"success\", \"ok\"],\n" +
                "          \"time\": \"3.5\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull("步骤的匹配器列表不应为null", step.getMatchers());
        
        // 应该有3个匹配器：REGEX、TIME、STATUS
        assertTrue("应该至少有2个匹配器", step.getMatchers().size() >= 2);
        
        // 验证 REGEX 匹配器
        boolean hasRegexMatcher = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.REGEX);
        assertTrue("应该存在 REGEX 匹配器", hasRegexMatcher);
        
        // 验证 TIME 匹配器
        PocObj.Matcher timeMatcher = step.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.TIME)
            .findFirst()
            .orElse(null);
        assertNotNull("应该存在 TIME 匹配器", timeMatcher);
        assertEquals("时间阈值应为3.5秒", "3.5", timeMatcher.getValues().get(0));
        
        // 验证 STATUS 匹配器
        boolean hasStatusMatcher = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.STATUS);
        assertTrue("应该存在 STATUS 匹配器", hasStatusMatcher);
    }
    
    /**
     * 测试没有 time 字段的 POC（向后兼容性测试）
     */
    @Test
    public void testPocWithoutTimeMatcher() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"no-time-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"POC Without Time Matcher\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"XSS\",\n" +
                "    \"author\": \"Potato\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/xss\",\n" +
                "        \"status\": \"200\",\n" +
                "        \"match\": {\n" +
                "          \"regex\": [\"<script>\"]\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        
        // 不应该有 TIME 匹配器
        boolean hasTimeMatcher = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.TIME);
        assertFalse("不应该存在 TIME 匹配器", hasTimeMatcher);
    }
    
    /**
     * 测试 attack 模式中的时间盲注
     */
    @Test
    public void testTimeMatcherInAttackMode() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"attack-time-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"Time-Based Attack Test\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"SQL Injection\",\n" +
                "    \"author\": \"Potato\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/verify\",\n" +
                "        \"match\": {\n" +
                "          \"time\": \"3\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"attack\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/attack\",\n" +
                "        \"params\": \"id=1' AND SLEEP(10)--\",\n" +
                "        \"match\": {\n" +
                "          \"time\": \"10\"\n" +
                "        },\n" +
                "        \"result\": {\n" +
                "          \"VerifyInfo\": {\n" +
                "            \"Payload\": \"SLEEP(10)\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        
        // 验证 verify 步骤
        assertNotNull("verify步骤不应为null", converted.getVerifySteps());
        PocObj.PocStep verifyStep = converted.getVerifySteps().get(0);
        PocObj.Matcher verifyTimeMatcher = verifyStep.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.TIME)
            .findFirst()
            .orElse(null);
        assertNotNull("verify步骤应该有 TIME 匹配器", verifyTimeMatcher);
        assertEquals("verify时间阈值应为3秒", "3", verifyTimeMatcher.getValues().get(0));
        
        // 验证 attack 步骤
        assertNotNull("attack步骤不应为null", converted.getExploitSteps());
        PocObj.PocStep attackStep = converted.getExploitSteps().get(0);
        PocObj.Matcher attackTimeMatcher = attackStep.getMatchers().stream()
            .filter(m -> m.getType() == PocObj.MatcherType.TIME)
            .findFirst()
            .orElse(null);
        assertNotNull("attack步骤应该有 TIME 匹配器", attackTimeMatcher);
        assertEquals("attack时间阈值应为10秒", "10", attackTimeMatcher.getValues().get(0));
    }
    
    /**
     * 测试空 time 字段的处理
     */
    @Test
    public void testEmptyTimeField() {
        String pocJson = "{\n" +
                "  \"pocInfo\": {\n" +
                "    \"vulID\": \"empty-time-001\",\n" +
                "    \"version\": \"1\",\n" +
                "    \"name\": \"Empty Time Field Test\",\n" +
                "    \"protocol\": \"http\",\n" +
                "    \"vulType\": \"Test\",\n" +
                "    \"author\": \"Potato\"\n" +
                "  },\n" +
                "  \"pocExecute\": {\n" +
                "    \"verify\": [\n" +
                "      {\n" +
                "        \"step\": \"0\",\n" +
                "        \"method\": \"GET\",\n" +
                "        \"vulPath\": \"/test\",\n" +
                "        \"match\": {\n" +
                "          \"regex\": [\"test\"],\n" +
                "          \"time\": \"\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        PocsuiteJsonObj.PocJson pocsuiteObj = gson.fromJson(pocJson, PocsuiteJsonObj.PocJson.class);
        PocObj.Poc converted = converter.convert(pocsuiteObj);
        
        assertNotNull("转换结果不应为null", converted);
        PocObj.PocStep step = converted.getVerifySteps().get(0);
        
        // 空的 time 字段不应该创建 TIME 匹配器
        boolean hasTimeMatcher = step.getMatchers().stream()
            .anyMatch(m -> m.getType() == PocObj.MatcherType.TIME);
        assertFalse("空的 time 字段不应该创建 TIME 匹配器", hasTimeMatcher);
    }
}

