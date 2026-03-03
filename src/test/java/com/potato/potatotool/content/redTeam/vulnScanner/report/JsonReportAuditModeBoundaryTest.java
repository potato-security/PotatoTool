package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonReportAuditModeBoundaryTest {

    @TempDir
    Path tempDir;

    @Test
    void testJsonReport_DefaultMode_ShouldNotExportInternalWarnings() throws Exception {
        ScanResult result = mockResult();
        List<ScanResult> results = new ArrayList<>();
        results.add(result);

        JsonReportGenerator generator = new JsonReportGenerator();
        File reportFile = tempDir.resolve("default-report.json").toFile();
        File output = generator.generate(results, reportFile.getAbsolutePath());

        String content = new String(java.nio.file.Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertFalse(content.contains("semanticWarnings"));
        assertFalse(content.contains("conversionWarnings"));
        assertFalse(content.contains("unsupportedCapabilities"));
        assertFalse(content.contains("warningSummary"));
        assertFalse(content.contains("diagnosticCodes"));
        assertFalse(content.contains("matcherPolicy"));
        assertFalse(content.contains("flowExecutionMode"));
    }

    @Test
    void testJsonReport_AuditMode_ShouldExportWarningSummary() throws Exception {
        ScanResult result = mockResult();
        List<ScanResult> results = new ArrayList<>();
        results.add(result);

        JsonReportGenerator generator = new JsonReportGenerator();
        File reportFile = tempDir.resolve("audit-report.json").toFile();
        File output = generator.generate(results, reportFile.getAbsolutePath(), true);

        String content = new String(java.nio.file.Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("warningSummary"));
        assertTrue(content.contains("\"P1\": 1"));
    }

    @Test
    void testJsonReport_AuditMode_ShouldExportDiagnosticCodesOnly() throws Exception {
        ScanResult result = mockResult();
        List<ScanResult> results = new ArrayList<>();
        results.add(result);

        JsonReportGenerator generator = new JsonReportGenerator();
        File reportFile = tempDir.resolve("audit-code-report.json").toFile();
        File output = generator.generate(results, reportFile.getAbsolutePath(), true);

        String content = new String(java.nio.file.Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("diagnosticCodes"));
        assertTrue(content.contains("SEMANTIC_CODE"));
        assertTrue(content.contains("CONVERSION_CODE"));
        assertTrue(content.contains("UNSUPPORTED_CODE"));

        assertFalse(content.contains("semanticWarnings"));
        assertFalse(content.contains("conversionWarnings"));
        assertFalse(content.contains("unsupportedCapabilities"));
    }

    @Test
    void testJsonReport_DefaultAndAuditMode_ShouldHaveDifferentBoundary() throws Exception {
        ScanResult result = mockResult();
        List<ScanResult> results = new ArrayList<>();
        results.add(result);

        JsonReportGenerator generator = new JsonReportGenerator();

        File defaultFile = tempDir.resolve("boundary-default-report.json").toFile();
        File auditFile = tempDir.resolve("boundary-audit-report.json").toFile();

        File defaultOutput = generator.generate(results, defaultFile.getAbsolutePath(), false);
        File auditOutput = generator.generate(results, auditFile.getAbsolutePath(), true);

        String defaultContent = new String(java.nio.file.Files.readAllBytes(defaultOutput.toPath()), StandardCharsets.UTF_8);
        String auditContent = new String(java.nio.file.Files.readAllBytes(auditOutput.toPath()), StandardCharsets.UTF_8);

        assertFalse(defaultContent.contains("warningSummary"));
        assertFalse(defaultContent.contains("diagnosticCodes"));

        assertTrue(auditContent.contains("warningSummary"));
        assertTrue(auditContent.contains("diagnosticCodes"));
    }

    private ScanResult mockResult() {
        ScanResult result = new ScanResult();
        result.setTarget("http://example.com");
        result.setVulnerable(true);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("boundary-test");
        poc.setName("boundary-test");
        poc.setSeverity(PocObj.Severity.MEDIUM);
        poc.setVulType("test");
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("boundary test");
        result.setPoc(poc);

        Map<String, Object> details = new HashMap<>();
        details.put("semanticWarnings", java.util.Arrays.asList(mapWithCode("SEMANTIC_CODE"), mapWithCode("SEMANTIC_CODE")));
        details.put("conversionWarnings", java.util.Arrays.asList(mapWithCode("CONVERSION_CODE")));
        details.put("unsupportedCapabilities", java.util.Arrays.asList(mapWithCode("UNSUPPORTED_CODE")));
        details.put("warningSummary", java.util.Collections.singletonMap("P1", 1));
        details.put("matcherPolicy", "legacy_guarded");
        details.put("flowExecutionMode", "flow-fallback");
        result.setDetails(details);

        return result;
    }

    private Map<String, Object> mapWithCode(String code) {
        Map<String, Object> item = new HashMap<>();
        item.put("code", code);
        return item;
    }
}
