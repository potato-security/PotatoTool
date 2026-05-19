package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PayloadCombiner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocExecutor 流程与步骤语义测试")
public class PocExecutorFlowExecutionTest {

    private static HttpServer server;
    private static String baseUrl;
    private static AtomicInteger continueCounter;
    private static AtomicInteger pitchforkCounter;
    private static AtomicInteger batteringramCounter;
    private static AtomicInteger shortCircuitCounter;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/flow/s1", exchange -> write(exchange, 200, "step-1-ok"));
        server.createContext("/flow/s2", exchange -> write(exchange, 200, "step-2-ok"));

        server.createContext("/fallback/s1", exchange -> write(exchange, 404, "fallback-step-1-fail"));
        server.createContext("/fallback/s2", exchange -> write(exchange, 500, "fallback-step-2-fail"));
        server.createContext("/fallback/s3", exchange -> write(exchange, 200, "fallback-step-3-pass"));

        server.createContext("/or/1/fail", exchange -> write(exchange, 404, "fail"));
        server.createContext("/or/2/fail", exchange -> write(exchange, 404, "fail"));
        server.createContext("/or/1/pass", exchange -> write(exchange, 200, "pass"));
        server.createContext("/or/2/pass", exchange -> write(exchange, 200, "pass"));

        continueCounter = new AtomicInteger(0);
        server.createContext("/continue/check", exchange -> {
            int count = continueCounter.incrementAndGet();
            write(exchange, 200, "continue-ok-" + count);
        });

        pitchforkCounter = new AtomicInteger(0);
        server.createContext("/pitchfork", exchange -> {
            int count = pitchforkCounter.incrementAndGet();
            write(exchange, 200, "pitchfork-hit-" + count);
        });

        batteringramCounter = new AtomicInteger(0);
        server.createContext("/batteringram", exchange -> {
            int count = batteringramCounter.incrementAndGet();
            write(exchange, 200, "batteringram-hit-" + count);
        });

        shortCircuitCounter = new AtomicInteger(0);
        server.createContext("/flow/shortcircuit", exchange -> {
            int count = shortCircuitCounter.incrementAndGet();
            write(exchange, 200, "shortcircuit-hit-" + count);
        });
        server.createContext("/goby-empty/trigger", exchange -> write(exchange, 200, "token=ok"));
        server.createContext("/goby-empty/md5", exchange -> write(exchange, 200, "94a08da1fecbb6e8b46990538c7b50b2"));
        server.createContext("/goby-empty/chain", exchange -> {
            String seed = extractQueryValue(exchange.getRequestURI().getRawQuery(), "seed");
            write(exchange, 200, md5(seed == null ? "" : seed));
        });
        server.createContext("/goby-empty/ok", exchange -> write(exchange, 200, "final-ok"));
        server.createContext("/goby-empty/header-source", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "check_code=abcd1234; Path=/");
            write(exchange, 200, "header-source");
        });
        server.createContext("/goby-empty/header-consumer", exchange -> {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            write(exchange, "check_code=abcd1234".equals(cookie) ? 200 : 401, cookie == null ? "missing" : cookie);
        });

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("简单 flow 应按 flow 模式执行")
    void testSimpleFlowExecutionMode() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-simple-test");
        poc.setName("flow-simple-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) && http(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "简单 flow 应命中漏洞");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"), "应标记为 flow 执行模式");
    }

    @Test
    @DisplayName("复杂 flow 应回退并标记 flow-fallback")
    void testComplexFlowFallbackMode() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-fallback-test");
        poc.setName("flow-fallback-test");
        poc.setProtocol("http");
        poc.setFlow("(http(1) && http(2)) || http(3)");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);

        PocObj.PocStep s1 = step("http_1", "/fallback/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/fallback/s2", statusMatcher(200));
        PocObj.PocStep s3 = step("http_3", "/fallback/s3", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2, s3));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "复杂 flow 回退后应按 OR 命中第三步");
        assertEquals("flow-fallback", result.getDetails().get("flowExecutionMode"), "应标记为 flow-fallback");
    }

    @Test
    @DisplayName("payload 组合路径应遵循 stepsCondition=OR")
    void testPayloadCombinationRespectsStepsConditionOr() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("payload-or-condition-test");
        poc.setName("payload-or-condition-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("v", Arrays.asList("1", "2"));
        poc.setVariables(variables);

        PocObj.PocStep failStep = step("http_1", "/or/{{v}}/fail", statusMatcher(200));
        PocObj.PocStep passStep = step("http_2", "/or/{{v}}/pass", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(failStep, passStep));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "变量组合执行时应保留 OR 语义，任一步命中即可");
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"), "无 flow 时应标记 stepsCondition 模式");
    }

    @Test
    @DisplayName("continueOnMatch=true 时 clusterbomb 应继续尝试后续组合")
    void testContinueOnMatchForClusterbomb() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("continue-on-match-clusterbomb-test");
        poc.setName("continue-on-match-clusterbomb-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);
        poc.setContinueOnMatch(true);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("a", Arrays.asList("1", "2"));
        variables.put("b", Arrays.asList("x", "y"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/continue/check?a={{a}}&b={{b}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "应至少命中一次");
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"));
        assertEquals(4, continueCounter.get(), "continueOnMatch=true 时 clusterbomb 应执行全部组合");
    }

    @Test
    @DisplayName("pitchfork 组合应可执行并命中")
    void testPitchforkCombinationExecution() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("pitchfork-execution-test");
        poc.setName("pitchfork-execution-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.pitchfork);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("u", Arrays.asList("u1", "u2"));
        variables.put("p", Arrays.asList("p1", "p2"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/pitchfork?u={{u}}&p={{p}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "pitchfork 模式应命中");
        assertEquals(1, pitchforkCounter.get(), "默认命中即停时应在首个组合即停止");
    }

    @Test
    @DisplayName("pitchfork 不应被单值 helper 变量压缩组合数")
    void testPitchforkCombinationWithSingleValueHelpers() {
        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("username", Arrays.asList("admin", "admin"));
        variables.put("password", Arrays.asList("prom-operator", "admin"));
        variables.put("Hostname", Collections.singletonList("127.0.0.1"));
        variables.put("BaseURL", Collections.singletonList(baseUrl));

        java.util.List<Map<String, String>> combinations = PayloadCombiner.generatePitchforkCombinations(variables);

        assertEquals(2, combinations.size(), "单值 helper 变量不应把 pitchfork 组合数压成 1");
        assertEquals("admin", combinations.get(0).get("username"));
        assertEquals("prom-operator", combinations.get(0).get("password"));
        assertEquals("admin", combinations.get(1).get("username"));
        assertEquals("admin", combinations.get(1).get("password"));
        assertEquals("127.0.0.1", combinations.get(0).get("Hostname"));
        assertEquals("127.0.0.1", combinations.get(1).get("Hostname"));
    }

    @Test
    @DisplayName("batteringram 组合应可执行并命中")
    void testBatteringramCombinationExecution() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("batteringram-execution-test");
        poc.setName("batteringram-execution-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.batteringram);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("k1", Arrays.asList("v1", "v2"));
        variables.put("k2", Arrays.asList("v1", "v2"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/batteringram?k1={{k1}}&k2={{k2}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "batteringram 模式应命中");
        assertEquals(1, batteringramCounter.get(), "默认命中即停时应在首个组合即停止");
    }

    @Test
    @DisplayName("flow OR 组内 AND 语义应正确执行")
    void testFlowOrWithAndGroupSemantics() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-or-and-group-test");
        poc.setName("flow-or-and-group-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) && http(2) || http(3)");

        PocObj.PocStep s1 = step("http_1", "/fallback/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        PocObj.PocStep s3 = step("http_3", "/flow/s1", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2, s3));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "应按 (s1&&s2)||s3 语义命中 s3");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"), "该表达式应走 flow 模式而不是 fallback");
    }

    @Test
    @DisplayName("flow OR 命中首组后应短路后续组")
    void testFlowOrShortCircuitAfterFirstGroupMatch() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-or-shortcircuit-test");
        poc.setName("flow-or-shortcircuit-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) || http(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/shortcircuit", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "首组命中时应整体命中");
        assertEquals(0, shortCircuitCounter.get(), "OR 短路应避免执行后续组");
    }

    @Test
    @DisplayName("flow token 大小写混合应可归一化匹配 stepId")
    void testFlowTokenNormalizationWithMixedCase() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-token-normalize-test");
        poc.setName("flow-token-normalize-test");
        poc.setProtocol("http");
        poc.setFlow("HTTP(1) && HTTP(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "大小写混合 token 应能命中对应步骤");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    @DisplayName("stepsCondition=OR 时失败步骤提取变量不应污染后续步骤")
    void testOrStepsShouldNotLeakVariablesAcrossFailedBranches() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("or-variable-isolation-test");
        poc.setName("or-variable-isolation-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);

        PocObj.PocStep failStep = step("http_1", "/or/1/fail", statusMatcher(200));
        failStep.setExtractors(Collections.singletonList(regexExtractor("token", "fail")));

        PocObj.PocStep passStep = step("http_2", "/or/1/pass", wordMatcher("{{token}}"));
        poc.setVerifySteps(Arrays.asList(failStep, passStep));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertFalse(result.isVulnerable(), "失败分支提取的变量不应泄漏到后续 OR 步骤");
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    @DisplayName("flow OR 失败组提取变量不应污染后续组")
    void testFlowOrGroupsShouldNotLeakVariablesAcrossFailedGroups() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-group-variable-isolation-test");
        poc.setName("flow-group-variable-isolation-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) && http(2) || http(3)");

        PocObj.PocStep s1 = step("http_1", "/or/1/pass", statusMatcher(200));
        s1.setExtractors(Collections.singletonList(regexExtractor("token", "pass")));
        PocObj.PocStep s2 = step("http_2", "/or/1/fail", statusMatcher(200));
        PocObj.PocStep s3 = step("http_3", "/or/1/pass", wordMatcher("{{token}}"));
        poc.setVerifySteps(Arrays.asList(s1, s2, s3));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertFalse(result.isVulnerable(), "失败 flow 组提取的变量不应泄漏到后续 OR 组");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    @DisplayName("Goby 空 checks 触发步骤应继续执行后续步骤")
    void testGobyEmptyChecksStepShouldContinue() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("goby-empty-checks-step-test");
        poc.setName("goby-empty-checks-step-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);

        PocObj.PocStep trigger = new PocObj.PocStep();
        trigger.setStepId("http_1");
        trigger.setMethod("GET");
        trigger.setPath("/goby-empty/trigger");
        PocObj.Matcher tokenExtractor = regexExtractor("token", "token=([a-z]+)");
        tokenExtractor.setGroup(-1);
        trigger.setExtractors(Collections.singletonList(tokenExtractor));

        PocObj.PocStep verify = step("http_2", "/goby-empty/{{token}}", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(trigger, verify));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "空 checks 的中间请求应作为触发步骤继续执行");
    }

    @Test
    @DisplayName("仅在 matcher 中引用的 Goby 变量不应被裁剪")
    void testGobyVariableUsedOnlyInMatcherShouldBePreserved() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("goby-matcher-variable-preserve-test");
        poc.setName("goby-matcher-variable-preserve-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("seed", Collections.singletonList("token"));
        variables.put("seedMd5", Collections.singletonList("@@md5(seed)"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/goby-empty/md5", wordMatcher("{{{seedMd5}}}"));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "仅在 matcher 中引用的 Goby 变量也应保留并完成运行时计算");
    }

    @Test
    @DisplayName("Goby 随机变量应先物化再参与哈希函数")
    void testGobyRandomDependencyShouldResolveBeforeHash() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("goby-random-hash-chain-test");
        poc.setName("goby-random-hash-chain-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("seed", Collections.singletonList("@@random(8)"));
        variables.put("seedMd5", Collections.singletonList("@@md5(seed)"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/goby-empty/chain?seed={{{seed}}}", wordMatcher("{{{seedMd5}}}"));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "Goby 应先生成随机变量，再对其结果执行哈希");
    }

    @Test
    @DisplayName("Goby request.set_variable 应可在下一请求前读取上一响应头")
    void testGobyRequestSetVariableCanReadLastHeaderBeforeNextRequest() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("goby-lastheader-request-variable-test");
        poc.setName("goby-lastheader-request-variable-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);

        PocObj.PocStep first = step("http_1", "/goby-empty/header-source", statusMatcher(200));

        PocObj.PocStep second = new PocObj.PocStep();
        second.setStepId("http_2");
        second.setMethod("GET");
        second.setPath("/goby-empty/header-consumer");
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "check_code={{{check_code}}}");
        second.setHeaders(headers);
        second.setRequestVariables(Collections.singletonList("check_code|lastheader|regex|check_code=(.*?);"));
        second.setMatchers(Collections.singletonList(statusMatcher(200)));
        second.setMatchersCondition(PocObj.MatchersCondition.AND);

        poc.setVerifySteps(Arrays.asList(first, second));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "上一响应头提取出的变量应在下一请求发送前完成绑定");
    }

    private static PocObj.PocStep step(String stepId, String path, PocObj.Matcher matcher) {
        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId(stepId);
        step.setMethod("GET");
        step.setPath(path);
        step.setMatchers(Collections.singletonList(matcher));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        return step;
    }

    private static PocObj.Matcher statusMatcher(int code) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.STATUS);
        matcher.setValues(Collections.singletonList(String.valueOf(code)));
        return matcher;
    }

    private static PocObj.Matcher wordMatcher(String value) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(value));
        return matcher;
    }

    private static PocObj.Matcher regexExtractor(String name, String regex) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setName(name);
        matcher.setType(PocObj.MatcherType.REGEX);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(regex));
        matcher.setGroup(0);
        return matcher;
    }

    private static void write(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String extractQueryValue(String rawQuery, String key) throws IOException {
        if (rawQuery == null || rawQuery.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        String[] parts = rawQuery.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) {
                return URLDecoder.decode(pair[1], "UTF-8");
            }
        }
        return null;
    }

    private static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashed = digest.digest((input == null ? "" : input).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法计算 MD5", e);
        }
    }
}
