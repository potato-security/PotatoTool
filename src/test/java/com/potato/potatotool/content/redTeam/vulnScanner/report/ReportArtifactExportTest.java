package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportArtifactExportTest {

    @Test
    void shouldExportStableReportArtifactsIntoTargetDirectory() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        File outputDir = new File("target/vulnscan-report-artifacts");
        assertTrue(outputDir.exists() || outputDir.mkdirs());

        List<ScanResult> results = Arrays.asList(
            buildResult("alpha", "alpha-tag"),
            buildResult("beta", "beta-tag")
        );

        List<File> files = generator.generateAllFormats(results, outputDir.getAbsolutePath());
        assertEquals(6, files.size());
        assertTrue(new File(outputDir, "report.html").isFile());
        assertTrue(new File(outputDir, "report.docx").isFile());
        assertTrue(new File(outputDir, "report.xlsx").isFile());
        assertTrue(new File(outputDir, "report.csv").isFile());
        assertTrue(new File(outputDir, "report.json").isFile());
        assertTrue(new File(outputDir, "report.txt").isFile());

        assertNoUnresolvedPlaceholdersInTextReport(new File(outputDir, "report.html"));
        assertNoUnresolvedPlaceholdersInTextReport(new File(outputDir, "report.csv"));
        assertNoUnresolvedPlaceholdersInTextReport(new File(outputDir, "report.json"));
        assertNoUnresolvedPlaceholdersInTextReport(new File(outputDir, "report.txt"));
        assertNoUnresolvedPlaceholdersInZipReport(new File(outputDir, "report.docx"));
        assertNoUnresolvedPlaceholdersInZipReport(new File(outputDir, "report.xlsx"));
    }

    private ScanResult buildResult(String suffix, String tag) {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/" + suffix);
        result.setVulnerable(true);
        result.setMatchedPath("/api/" + suffix + "?trace={{UUID}}");
        result.setMatchedPayload("cmd=id-" + suffix + "&marker={{random_uuid}}");
        result.setRawRequest("GET /api/" + suffix + "?trace={{UUID}} HTTP/1.1\nX-Asset: " + tag);
        result.setRawResponseSnippet("HTTP/1.1 200 OK\n\n{\"result\":\"" + suffix + "\",\"marker\":\"{{randstr}}\"}");
        result.setRecommendation("review-" + suffix + "-{{UNRESOLVED_FIXME}}");
        result.setStepRecords(Arrays.asList(buildStepRecord(suffix, tag)));

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
        outputData.put("assetTag", tag);
        outputData.put("evidenceId", "CNVD-" + suffix.toUpperCase() + "-{{UUID}}");
        result.setOutputData(outputData);
        return result;
    }

    private StepExecutionRecord buildStepRecord(String suffix, String tag) {
        StepExecutionRecord record = new StepExecutionRecord(1, "step-" + suffix);
        record.setStepType("http");
        record.setRequestMethod("GET");
        record.setRequestUrl("http://127.0.0.1/" + suffix + "?trace={{UUID}}");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Asset", tag);
        headers.put("X-Trace", "{{random_uuid}}");
        record.setRequestHeaders(headers);
        record.setRequestBody("payload={{randstr}}");
        record.setRawRequest("GET /" + suffix + "?trace={{UUID}} HTTP/1.1\nX-Trace: {{random_uuid}}");
        record.setResponseCode(200);
        record.setResponseBody("{\"trace\":\"{{UUID}}\"}");
        record.setRawResponse("HTTP/1.1 200 OK\n\n{\"trace\":\"{{UUID}}\"}");
        record.setMatched(true);
        record.setMatchedValues(Arrays.asList("{{UUID}}"));
        Map<String, Object> extracted = new LinkedHashMap<String, Object>();
        extracted.put("trace", "{{random_uuid}}");
        record.setExtractedVariables(extracted);
        return record;
    }

    private void assertNoUnresolvedPlaceholdersInTextReport(File file) throws IOException {
        Pattern unresolvedPlaceholder = Pattern.compile("\\{\\{[^{}\\r\\n]{1,160}\\}\\}");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertFalse(unresolvedPlaceholder.matcher(content).find(),
                "报告不应残留未解析模板占位符: " + file.getName());
    }

    private void assertNoUnresolvedPlaceholdersInZipReport(File file) throws IOException {
        Pattern unresolvedPlaceholder = Pattern.compile("\\{\\{[^{}\\r\\n]{1,160}\\}\\}");
        try (ZipFile zipFile = new ZipFile(file)) {
            for (ZipEntry entry : java.util.Collections.list(zipFile.entries())) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.endsWith(".xml") && !name.endsWith(".rels") && !name.endsWith(".txt")) {
                    continue;
                }
                String content = new String(readAllBytes(zipFile.getInputStream(entry)), StandardCharsets.UTF_8);
                assertFalse(unresolvedPlaceholder.matcher(content).find(),
                        "报告不应残留未解析模板占位符: " + file.getName() + "!" + name);
            }
        }
    }

    private byte[] readAllBytes(java.io.InputStream inputStream) throws IOException {
        try (java.io.InputStream in = inputStream;
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
