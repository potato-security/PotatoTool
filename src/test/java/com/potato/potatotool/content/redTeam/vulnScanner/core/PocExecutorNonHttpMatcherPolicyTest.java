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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("非HTTP matcher 策略测试")
public class PocExecutorNonHttpMatcherPolicyTest {

    @Test
    void testDnsStep_ShouldRecordDowngrade_WhenJsonMatcherUsed() {
        PocObj.Poc poc = buildFileUnsupportedPoc("dns-json-unsupported", PocObj.MatcherType.JSON, "$.ok");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertFalse(result.isVulnerable());
        List<Map<String, Object>> semanticWarnings = warningList(result, "semanticWarnings");
        assertTrue(hasWarningCode(semanticWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    @Test
    void testTcpStep_ShouldRecordDowngrade_WhenCelMatcherUsed() {
        PocObj.Poc poc = buildFileUnsupportedPoc("tcp-cel-unsupported", PocObj.MatcherType.CEL, "contains(body," + '"' + "x" + '"' + ")");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertFalse(result.isVulnerable());
        List<Map<String, Object>> semanticWarnings = warningList(result, "semanticWarnings");
        assertTrue(hasWarningCode(semanticWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    @Test
    void testWebSocketStep_ShouldDistinguishUnsupportedFromMismatch() {
        PocObj.Poc unsupportedPoc = buildFileUnsupportedPoc("ws-unsupported", PocObj.MatcherType.JSON, "$.ok");
        ScanResult unsupportedResult = execute(unsupportedPoc);
        List<Map<String, Object>> unsupportedWarnings = warningList(unsupportedResult, "semanticWarnings");
        assertTrue(hasWarningCode(unsupportedWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));

        PocObj.Poc mismatchPoc = buildFileMismatchPoc("ws-mismatch", "definitely-not-found");
        ScanResult mismatchResult = execute(mismatchPoc);
        List<Map<String, Object>> mismatchWarnings = warningList(mismatchResult, "semanticWarnings");

        assertFalse(mismatchResult.isVulnerable());
        assertFalse(hasWarningCode(mismatchWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    @Test
    void testNonHttpUnsupportedMatcher_ShouldNotSilentFalse() {
        PocObj.Poc poc = buildFileUnsupportedPoc("file-unsupported", PocObj.MatcherType.CEL, "contains(body," + '"' + "x" + '"' + ")");

        ScanResult result = execute(poc);

        assertNotNull(result);
        assertFalse(result.isVulnerable());
        List<Map<String, Object>> semanticWarnings = warningList(result, "semanticWarnings");
        assertFalse(semanticWarnings.isEmpty());
        assertTrue(hasWarningCode(semanticWarnings, "NON_HTTP_MATCHER_UNSUPPORTED"));
    }

    private ScanResult execute(PocObj.Poc poc) {
        try {
            return new PocExecutor(new ScanConfig()).execute("http://127.0.0.1:65535", poc);
        } catch (Exception e) {
            fail("执行不应抛异常: " + e.getMessage());
            return null;
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

    private PocObj.Poc buildFileMismatchPoc(String id, String word) {
        PocObj.Poc poc = basePoc(id, "file");
        PocObj.FileStep step = new PocObj.FileStep();
        step.setStepId("file_1");
        step.setPaths(Collections.singletonList(createTempTestFile(id)));
        step.setRecursive(false);
        step.setMatchers(Collections.singletonList(matcher(PocObj.MatcherType.WORD, "body", word)));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private String createTempTestFile(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".txt");
            f.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write("non-http-policy-test-content".getBytes(StandardCharsets.UTF_8));
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
        poc.setSeverity(PocObj.Severity.MEDIUM);
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
