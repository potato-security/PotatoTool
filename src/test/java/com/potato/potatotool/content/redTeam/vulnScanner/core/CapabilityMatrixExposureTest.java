package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("能力矩阵暴露测试")
public class CapabilityMatrixExposureTest {

    @Test
    void testCapabilityMatrix_ShouldReflectHttpAndNonHttpDifferences() {
        String filePath = createTempTestFile("cap-matrix");

        PocObj.Poc supportedPoc = buildFilePoc("file-supported", filePath, PocObj.MatcherType.WORD, "capability-matrix-content");
        ScanResult supportedResult = execute(supportedPoc);

        PocObj.Poc unsupportedPoc = buildFilePoc("file-unsupported", filePath, PocObj.MatcherType.CEL, "contains(body,\"ok\")");
        ScanResult unsupportedResult = execute(unsupportedPoc);

        assertNotNull(supportedResult);
        assertNotNull(unsupportedResult);

        assertEquals("legacy_guarded", supportedResult.getDetails().get("matcherPolicy"));
        assertEquals("legacy_guarded", unsupportedResult.getDetails().get("matcherPolicy"));

        List<Map<String, Object>> unsupportedWarnings = warningList(unsupportedResult, "semanticWarnings");
        assertTrue(hasWarningCode(unsupportedWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));

        List<Map<String, Object>> supportedWarnings = warningList(supportedResult, "semanticWarnings");
        assertFalse(hasWarningCode(supportedWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    @Test
    void testCapabilityMatrix_ShouldExposeMatcherPolicyMarker() {
        String filePath = createTempTestFile("cap-marker");
        PocObj.Poc poc = buildFilePoc("policy-marker", filePath, PocObj.MatcherType.WORD, "capability-matrix-content");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertEquals("legacy_guarded", result.getDetails().get("matcherPolicy"));
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"));
    }

    @Test
    void testCapabilityMatrix_ShouldKeepCurrentBehavior_ForNonHttpUnsupportedMatchers() {
        String filePath = createTempTestFile("cap-current");
        PocObj.Poc poc = buildFilePoc("current-behavior", filePath, PocObj.MatcherType.CEL, "contains(body,\"x\")");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertFalse(result.isVulnerable());
        List<Map<String, Object>> warnings = warningList(result, "semanticWarnings");
        assertTrue(hasWarningCode(warnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    @Test
    void testCapabilityMatrix_Todo_ShouldUpgradeToStructuredCapabilityReportLater() {
        String filePath = createTempTestFile("cap-todo");
        PocObj.Poc poc = buildFilePoc("todo-cap-report", filePath, PocObj.MatcherType.CEL, "contains(body,\"x\")");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertEquals("legacy_guarded", result.getDetails().get("matcherPolicy"), "TODO: 后续切到 capability matrix v2 时反转此断言");
    }

    private ScanResult execute(PocObj.Poc poc) {
        try {
            return new PocExecutor(new ScanConfig()).execute("http://127.0.0.1:65535", poc);
        } catch (Exception e) {
            throw new AssertionError("执行失败: " + e.getMessage(), e);
        }
    }

    private PocObj.Poc buildHttpPoc(String id, String path, PocObj.MatcherType matcherType, String value) {
        PocObj.Poc poc = basePoc(id, "http");
        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setMatchers(Collections.singletonList(matcher(matcherType, "body", value)));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private PocObj.Poc buildFilePoc(String id, String filePath, PocObj.MatcherType matcherType, String value) {
        PocObj.Poc poc = basePoc(id, "file");
        PocObj.FileStep step = new PocObj.FileStep();
        step.setStepId("file_1");
        step.setPaths(Collections.singletonList(filePath));
        step.setRecursive(false);
        step.setMatchers(Collections.singletonList(matcher(matcherType, "body", value)));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private PocObj.Poc basePoc(String id, String protocol) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol(protocol);
        poc.setSeverity(PocObj.Severity.LOW);
        poc.setStepsCondition(PocObj.MatchersCondition.AND);
        return poc;
    }

    private PocObj.Matcher matcher(PocObj.MatcherType type, String part, String... values) {
        PocObj.Matcher m = new PocObj.Matcher();
        m.setType(type);
        m.setPart(part);
        m.setValues(values == null ? Collections.<String>emptyList() : java.util.Arrays.asList(values));
        return m;
    }

    private String createTempTestFile(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".txt");
            f.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write("capability-matrix-content".getBytes(StandardCharsets.UTF_8));
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            throw new AssertionError("创建临时文件失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> warningList(ScanResult result, String key) {
        Object value = result.getDetails().get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }

    private boolean hasWarningCode(List<Map<String, Object>> warnings, String code) {
        for (Map<String, Object> warning : warnings) {
            if (warning != null && code.equals(String.valueOf(warning.get("code")))) {
                return true;
            }
        }
        return false;
    }
}
