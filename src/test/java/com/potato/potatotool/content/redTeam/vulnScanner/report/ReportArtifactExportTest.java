package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportArtifactExportTest {

    @Test
    void shouldExportStableReportArtifactsIntoTargetDirectory() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        File outputDir = new File("target/vulnscan-report-artifacts");
        assertTrue(outputDir.exists() || outputDir.mkdirs());

        List<ScanResult> results = Arrays.asList(
            buildResult("alpha", "A-BASELINE"),
            buildResult("beta", "B-PROXY")
        );

        List<File> files = generator.generateAllFormats(results, outputDir.getAbsolutePath());
        assertEquals(6, files.size());
        assertTrue(new File(outputDir, "report.html").isFile());
        assertTrue(new File(outputDir, "report.docx").isFile());
        assertTrue(new File(outputDir, "report.xlsx").isFile());
        assertTrue(new File(outputDir, "report.csv").isFile());
        assertTrue(new File(outputDir, "report.json").isFile());
        assertTrue(new File(outputDir, "report.txt").isFile());
    }

    private ScanResult buildResult(String suffix, String profile) {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/" + suffix);
        result.setVulnerable(true);
        result.setMatchedPath("/api/" + suffix);
        result.setMatchedPayload("cmd=id-" + suffix);
        result.setRawRequest("GET /api/" + suffix + " HTTP/1.1\nX-Profile: " + profile);
        result.setRawResponseSnippet("HTTP/1.1 200 OK\n\n{\"result\":\"" + suffix + "\"}");
        result.setRecommendation("review-" + suffix);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-" + suffix);
        poc.setName("artifact-" + suffix);
        poc.setSeverity(PocObj.Severity.HIGH);
        poc.setProtocol("http");
        poc.setOriginalFormat("nuclei");
        poc.setVulType("INFOLEAK");
        poc.setDescription("artifact-export-" + suffix);
        result.setPoc(poc);

        Map<String, Object> outputData = new LinkedHashMap<String, Object>();
        outputData.put("buildingProfile", profile);
        outputData.put("evidenceId", "CNVD-" + suffix.toUpperCase());
        result.setOutputData(outputData);
        return result;
    }
}
