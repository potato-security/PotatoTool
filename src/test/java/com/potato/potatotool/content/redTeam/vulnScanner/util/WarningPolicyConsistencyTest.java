package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("告警策略一致性测试")
public class WarningPolicyConsistencyTest {

    @Test
    void testWarningCodeLevelAction_ShouldBeConsistent_ForUnsupportedMatcher() {
        ScanResult fileJson = execute(buildFileUnsupportedPoc("file-warning-json", PocObj.MatcherType.JSON, "$.ok"));
        ScanResult fileCel = execute(buildFileUnsupportedPoc("file-warning-cel", PocObj.MatcherType.CEL, "contains(body,\"x\")"));

        Map<String, Object> w1 = firstSemanticWarning(fileJson);
        Map<String, Object> w2 = firstSemanticWarning(fileCel);

        assertNotNull(w1);
        assertNotNull(w2);
        assertEquals("NON_HTTP_MATCHER_UNSUPPORTED", String.valueOf(w1.get("code")));
        assertEquals(String.valueOf(w1.get("code")), String.valueOf(w2.get("code")));
        assertEquals(String.valueOf(w1.get("level")), String.valueOf(w2.get("level")));
        assertEquals(String.valueOf(w1.get("action")), String.valueOf(w2.get("action")));
    }

    @Test
    void testWarningCodeLevelAction_ShouldBeConsistent_ForFlowFallback() {
        ScanResult result = execute(buildFlowFallbackPoc("flow-fallback-warning"));
        List<Map<String, Object>> warnings = semanticWarnings(result);

        boolean found = false;
        for (Map<String, Object> warning : warnings) {
            if ("FLOW_EXECUTION_FALLBACK".equals(String.valueOf(warning.get("code")))) {
                found = true;
                assertEquals("P1", String.valueOf(warning.get("level")));
                assertEquals("fallback", String.valueOf(warning.get("action")));
            }
        }
        assertTrue(found, "应存在 FLOW_EXECUTION_FALLBACK 告警");
    }

    @Test
    void testWarningAction_ShouldBeInWhitelist() {
        ScanResult result = execute(buildFileUnsupportedPoc("warning-action", PocObj.MatcherType.CEL, "contains(body,\"x\")"));
        List<Map<String, Object>> warnings = semanticWarnings(result);
        assertFalse(warnings.isEmpty());
        for (Map<String, Object> warning : warnings) {
            assertTrue(actionInWhitelist(String.valueOf(warning.get("action"))));
        }
    }

    @Test
    void testWarningLevel_ShouldMapToP0P1P2Deterministically() {
        ScanResult first = execute(buildFileUnsupportedPoc("level-1", PocObj.MatcherType.CEL, "contains(body,\"x\")"));
        ScanResult second = execute(buildFileUnsupportedPoc("level-2", PocObj.MatcherType.CEL, "contains(body,\"x\")"));

        assertEquals(
                String.valueOf(firstSemanticWarning(first).get("level")),
                String.valueOf(firstSemanticWarning(second).get("level"))
        );
    }

    @Test
    void testConversionAndSemanticWarnings_ShouldUseUnifiedSchema() {
        ScanResult semantic = execute(buildFileUnsupportedPoc("schema-check", PocObj.MatcherType.CEL, "contains(body,\"x\")"));
        List<Map<String, Object>> semanticWarnings = semanticWarnings(semantic);
        assertFalse(semanticWarnings.isEmpty());
        assertTrue(hasUnifiedSchema(semanticWarnings));
    }

    private ScanResult execute(PocObj.Poc poc) {
        try {
            return new PocExecutor(new ScanConfig()).execute("http://127.0.0.1:65535", poc);
        } catch (Exception e) {
            throw new AssertionError("执行失败: " + e.getMessage(), e);
        }
    }

    private PocObj.Poc buildFileUnsupportedPoc(String id, PocObj.MatcherType type, String value) {
        PocObj.Poc poc = basePoc(id, "file");
        PocObj.FileStep step = new PocObj.FileStep();
        step.setStepId("file_1");
        step.setPaths(Collections.singletonList(createTempTestFile(id)));
        step.setRecursive(false);
        step.setMatchers(Collections.singletonList(matcher(type, "body", value)));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private PocObj.Poc buildFlowFallbackPoc(String id) {
        PocObj.Poc poc = basePoc(id, "file");
        poc.setFlow("(file(1) && file(2)) || file(3)");

        PocObj.FileStep s1 = fileWordStep("file_1");
        PocObj.FileStep s2 = fileWordStep("file_2");
        PocObj.FileStep s3 = fileWordStep("file_3");

        poc.setVerifySteps(java.util.Arrays.asList(s1, s2, s3));
        poc.setStepsCondition(PocObj.MatchersCondition.OR);
        return poc;
    }

    private PocObj.FileStep fileWordStep(String stepId) {
        PocObj.FileStep step = new PocObj.FileStep();
        step.setStepId(stepId);
        step.setPaths(Collections.singletonList(createTempTestFile(stepId)));
        step.setRecursive(false);
        step.setMatchers(Collections.singletonList(matcher(PocObj.MatcherType.WORD, "body", "warning-policy-content")));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        return step;
    }

    private String createTempTestFile(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".txt");
            f.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write("warning-policy-content".getBytes(StandardCharsets.UTF_8));
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            throw new AssertionError("创建临时文件失败: " + e.getMessage(), e);
        }
    }

    private PocObj.Poc basePoc(String id, String protocol) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol(protocol);
        poc.setSeverity(PocObj.Severity.LOW);
        return poc;
    }

    private PocObj.Matcher matcher(PocObj.MatcherType type, String part, String... values) {
        PocObj.Matcher m = new PocObj.Matcher();
        m.setType(type);
        m.setPart(part);
        m.setValues(values == null ? Collections.<String>emptyList() : java.util.Arrays.asList(values));
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> semanticWarnings(ScanResult result) {
        Object value = result.getDetails().get("semanticWarnings");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> firstSemanticWarning(ScanResult result) {
        List<Map<String, Object>> warnings = semanticWarnings(result);
        if (warnings.isEmpty()) {
            return null;
        }
        return warnings.get(0);
    }

    private boolean hasUnifiedSchema(List<Map<String, Object>> warnings) {
        for (Map<String, Object> warning : warnings) {
            if (warning == null) {
                return false;
            }
            if (!warning.containsKey("code")
                    || !warning.containsKey("level")
                    || !warning.containsKey("protocol")
                    || !warning.containsKey("field")
                    || !warning.containsKey("value")
                    || !warning.containsKey("action")
                    || !warning.containsKey("message")) {
                return false;
            }
        }
        return true;
    }

    private boolean actionInWhitelist(String action) {
        return "drop".equals(action)
                || "downgrade".equals(action)
                || "fallback".equals(action)
                || "skip".equals(action);
    }
}
