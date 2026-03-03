package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("复杂 flow 语义测试")
public class PocExecutorComplexFlowSemanticsTest {

    @Test
    void testComplexFlow_WithParentheses_ShouldExecuteWithoutFallback() {
        String filePath = createTempTestFile("flow-parentheses");
        PocObj.Poc poc = buildFlowPoc("complex-parentheses-fallback", "(file(1) && file(2)) || file(3)", Arrays.asList(
                fileStep("file_1", filePath, "flow-content"),
                fileStep("file_2", filePath, "flow-content"),
                fileStep("file_3", filePath, "flow-content")
        ));
        poc.setProtocol("file");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertTrue(result.isVulnerable(), "复杂 flow 当前会 fallback 到 stepsCondition(OR) 且首步命中");
        assertEquals("flow-fallback", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    void testComplexFlow_ShouldRespectNestedAndOrShortCircuit() {
        String filePath = createTempTestFile("flow-short-circuit");
        PocObj.Poc poc = buildFlowPoc("complex-short-circuit", "(file(1) && file(2)) || file(3)", Arrays.asList(
                fileStep("file_1", filePath, "flow-content"),
                fileStep("file_2", filePath, "flow-content"),
                fileStep("file_3", filePath, "flow-content")
        ));
        poc.setProtocol("file");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertEquals("flow-fallback", result.getDetails().get("flowExecutionMode"), "当前实现对括号表达式走 fallback");
    }

    @Test
    void testComplexFlow_ShouldFallbackOnlyWhenExpressionOutOfScope() {
        String filePath = createTempTestFile("flow-out-of-scope");
        PocObj.Poc poc = buildFlowPoc("complex-out-of-scope", "(file(1) && file(2)) || (file(3) && file(4))", Arrays.asList(
                fileStep("file_1", filePath, "flow-content"),
                fileStep("file_2", filePath, "flow-content"),
                fileStep("file_3", filePath, "flow-content"),
                fileStep("file_4", filePath, "flow-content")
        ));
        poc.setProtocol("file");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertEquals("flow-fallback", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    void testFlowExecutionMode_ShouldBeFlowForSupportedComplexExpression() {
        String filePath = createTempTestFile("flow-supported");
        PocObj.Poc poc = buildFlowPoc("supported-flow", "file(1) && file(2) || file(3)", Arrays.asList(
                fileStep("file_1", filePath, "flow-content"),
                fileStep("file_2", filePath, "flow-content"),
                fileStep("file_3", filePath, "flow-content")
        ));
        poc.setProtocol("file");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertEquals("flow", result.getDetails().get("flowExecutionMode"));
    }

    private ScanResult execute(PocObj.Poc poc) {
        try {
            return new PocExecutor(new ScanConfig()).execute("http://127.0.0.1:65535", poc);
        } catch (Exception e) {
            throw new AssertionError("执行失败: " + e.getMessage(), e);
        }
    }

    private PocObj.Poc buildFlowPoc(String id, String flow, List<PocObj.PocStep> steps) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setFlow(flow);
        poc.setVerifySteps(steps);
        poc.setSeverity(PocObj.Severity.LOW);
        poc.setStepsCondition(PocObj.MatchersCondition.AND);
        return poc;
    }

    private PocObj.FileStep fileStep(String stepId, String filePath, String expectedWord) {
        PocObj.FileStep step = new PocObj.FileStep();
        step.setStepId(stepId);
        step.setPaths(Collections.singletonList(filePath));
        step.setRecursive(false);
        step.setMatchers(Collections.singletonList(wordMatcher(expectedWord)));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        return step;
    }

    private PocObj.Matcher wordMatcher(String value) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(value));
        return matcher;
    }

    private String createTempTestFile(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".txt");
            f.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write("flow-content".getBytes(StandardCharsets.UTF_8));
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            throw new AssertionError("创建临时文件失败: " + e.getMessage(), e);
        }
    }
}
