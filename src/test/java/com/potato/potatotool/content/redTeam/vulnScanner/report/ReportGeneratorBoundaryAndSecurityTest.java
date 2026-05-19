package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorBoundaryAndSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateEmptyReportsAcrossAllFormats() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("empty-report");

        List<File> files = generator.generateAllFormats(Collections.<ScanResult>emptyList(), outputDir.toString());

        assertEquals(6, files.size());
        assertTrue(readUtf8(outputDir.resolve("report.html")).contains("当前未发现可导出的漏洞结果"));
        assertTrue(readUtf8(outputDir.resolve("report.csv")).contains("目标URL"));
        assertTrue(readUtf8(outputDir.resolve("report.txt")).contains("漏洞结果数: 0"));
        assertTrue(readWord(outputDir.resolve("report.docx")).contains("当前未发现可导出的漏洞结果"));

        JsonObject json = JsonParser.parseString(readUtf8(outputDir.resolve("report.json"))).getAsJsonObject();
        assertEquals(0, json.getAsJsonObject("statistics").get("vulnCount").getAsInt());
        assertEquals(0, json.getAsJsonArray("vulnerabilities").size());

        Sheet detailsSheet = readSheet(outputDir.resolve("report.xlsx"), "漏洞详情");
        Sheet summarySheet = readSheet(outputDir.resolve("report.xlsx"), "扫描摘要");
        assertEquals(0, detailsSheet.getLastRowNum());
        assertEquals("0", summarySheet.getRow(6).getCell(1).getStringCellValue());
    }

    @Test
    void shouldMaskSensitiveValuesAcrossAllFormats() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("sensitive-report");

        generator.generateAllFormats(Collections.singletonList(buildSensitiveResult()), outputDir.toString());

        String html = readUtf8(outputDir.resolve("report.html"));
        String csv = readUtf8(outputDir.resolve("report.csv"));
        String json = readUtf8(outputDir.resolve("report.json"));
        String txt = readUtf8(outputDir.resolve("report.txt"));
        String word = readWord(outputDir.resolve("report.docx"));
        String excel = readWorkbookText(outputDir.resolve("report.xlsx"));

        assertMasked(html);
        assertMasked(csv);
        assertMasked(json);
        assertMasked(txt);
        assertMasked(word);
        assertMasked(excel);

        JsonObject vuln = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonArray("vulnerabilities")
                .get(0)
                .getAsJsonObject();
        assertEquals(ReportDataSanitizer.REDACTED, vuln.getAsJsonObject("variableValues").get("accessToken").getAsString());
        assertEquals(ReportDataSanitizer.REDACTED, vuln.getAsJsonObject("outputData").get("apiToken").getAsString());
        assertTrue(vuln.get("rawRequest").getAsString().contains(ReportDataSanitizer.REDACTED));
        assertFalse(vuln.get("rawRequest").getAsString().contains("auth-secret"));
    }

    @Test
    void shouldGenerateLargeReportsAcrossAllFormats() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("large-report");
        List<ScanResult> results = new ArrayList<ScanResult>();
        for (int i = 0; i < 18; i++) {
            results.add(buildLargeResult(i));
        }

        List<File> files = generator.generateAllFormats(results, outputDir.toString());

        assertEquals(6, files.size());
        assertTrue(readUtf8(outputDir.resolve("report.html")).contains("批量漏洞-17"));
        assertTrue(readUtf8(outputDir.resolve("report.csv")).contains("poc-bulk-17"));
        assertTrue(readUtf8(outputDir.resolve("report.txt")).contains("漏洞结果数: 18"));
        assertTrue(readWord(outputDir.resolve("report.docx")).contains("批量漏洞-17"));

        JsonObject json = JsonParser.parseString(readUtf8(outputDir.resolve("report.json"))).getAsJsonObject();
        assertEquals(18, json.getAsJsonObject("statistics").get("vulnCount").getAsInt());
        assertEquals(18, json.getAsJsonArray("vulnerabilities").size());

        Sheet detailsSheet = readSheet(outputDir.resolve("report.xlsx"), "漏洞详情");
        assertEquals(18, detailsSheet.getLastRowNum());
    }

    @Test
    void shouldExcludeNegativeResultsFromAllExportFormats() throws Exception {
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("mixed-results-report");

        ScanResult positive = buildLargeResult(1);
        positive.setVulnerable(true);
        ScanResult negative = buildLargeResult(2);
        negative.setVulnerable(false);
        negative.getPoc().setId("poc-negative");
        negative.getPoc().setName("负样本不应导出");

        List<File> files = generator.generateAllFormats(Arrays.asList(positive, negative), outputDir.toString());

        assertEquals(6, files.size());
        assertTrue(readUtf8(outputDir.resolve("report.txt")).contains("漏洞结果数: 1"));
        assertFalse(readUtf8(outputDir.resolve("report.txt")).contains("负样本不应导出"));
        assertFalse(readUtf8(outputDir.resolve("report.csv")).contains("poc-negative"));
        assertFalse(readUtf8(outputDir.resolve("report.html")).contains("负样本不应导出"));

        JsonObject json = JsonParser.parseString(readUtf8(outputDir.resolve("report.json"))).getAsJsonObject();
        assertEquals(1, json.getAsJsonObject("statistics").get("vulnCount").getAsInt());
        assertEquals(1, json.getAsJsonArray("vulnerabilities").size());
        assertEquals("poc-bulk-1", json.getAsJsonArray("vulnerabilities").get(0).getAsJsonObject().get("pocId").getAsString());

        Sheet detailsSheet = readSheet(outputDir.resolve("report.xlsx"), "漏洞详情");
        Sheet summarySheet = readSheet(outputDir.resolve("report.xlsx"), "扫描摘要");
        assertEquals(1, detailsSheet.getLastRowNum());
        assertEquals("1", summarySheet.getRow(6).getCell(1).getStringCellValue());
        assertFalse(readWord(outputDir.resolve("report.docx")).contains("负样本不应导出"));
    }

    private void assertMasked(String content) {
        List<String> secrets = Arrays.asList(
                "query-secret",
                "payload-secret",
                "auth-secret",
                "cookie-secret",
                "proxy-secret",
                "audit-secret",
                "ai-secret",
                "internal-secret",
                "body-secret",
                "body-cookie-secret",
                "resp-secret",
                "var-secret",
                "var-cookie-secret",
                "output-secret",
                "output-proxy-secret",
                "step-query-secret",
                "step-auth-secret",
                "step-cookie-secret",
                "step-internal-secret",
                "step-body-secret",
                "step-response-cookie",
                "step-response-secret",
                "matched-secret",
                "extracted-secret",
                "response-secret"
        );
        for (String secret : secrets) {
            assertFalse(content.contains(secret), "报告中不应泄露敏感值: " + secret);
        }
        assertTrue(content.contains(ReportDataSanitizer.REDACTED), "报告中应包含脱敏占位符");
    }

    private ScanResult buildSensitiveResult() {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/sensitive?accessToken=query-secret");
        result.setVulnerable(true);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-sensitive");
        poc.setName("敏感信息脱敏验证");
        poc.setSeverity(PocObj.Severity.CRITICAL);
        poc.setVulType("AUTH");
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("验证报告导出脱敏边界");
        result.setPoc(poc);

        result.setMatchedPath("/api/secure");
        result.setMatchedPayload("Bearer payload-secret");
        result.setRecommendation("轮换密钥并清理旧认证信息");
        result.setRawRequest("POST /api/secure HTTP/1.1\n"
                + "Authorization: Bearer auth-secret\n"
                + "Cookie: SID=cookie-secret\n"
                + "Proxy-Authorization: Basic proxy-secret\n"
                + "X-Security-Audit-Token: audit-secret\n"
                + "X-AI-Session: ai-secret\n"
                + "X-Internal-Token: internal-secret\n\n"
                + "{\"accessToken\":\"body-secret\",\"cookie\":\"body-cookie-secret\"}");
        result.setRawResponseSnippet("HTTP/1.1 200 OK\n"
                + "Set-Cookie: SESSION=response-secret\n"
                + "Content-Type: application/json\n\n"
                + "{\"token\":\"resp-secret\"}");
        result.setParamKeys(Arrays.asList("tenant", "accessToken"));

        Map<String, String> variableValues = new LinkedHashMap<String, String>();
        variableValues.put("tenant", "A楼");
        variableValues.put("accessToken", "var-secret");
        variableValues.put("sessionCookie", "var-cookie-secret");
        result.setVariableValues(variableValues);

        Map<String, Object> outputData = new LinkedHashMap<String, Object>();
        outputData.put("evidenceId", "CNVD-SENSITIVE");
        outputData.put("apiToken", "output-secret");
        outputData.put("proxyAuthorization", "output-proxy-secret");
        result.setOutputData(outputData);

        StepExecutionRecord record = new StepExecutionRecord();
        record.setStepIndex(1);
        record.setRequestUrl("http://127.0.0.1/api/secure?accessToken=step-query-secret");
        record.setRequestMethod("POST");
        record.setResponseCode(200);
        record.setResponseTime(12L);
        record.setMatched(true);

        Map<String, String> requestHeaders = new LinkedHashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer step-auth-secret");
        requestHeaders.put("Cookie", "SID=step-cookie-secret");
        requestHeaders.put("X-Internal-Token", "step-internal-secret");
        record.setRequestHeaders(requestHeaders);
        record.setRequestBody("{\"accessToken\":\"step-body-secret\",\"normal\":\"ok\"}");

        Map<String, String> responseHeaders = new LinkedHashMap<String, String>();
        responseHeaders.put("Set-Cookie", "STEP=step-response-cookie");
        responseHeaders.put("X-Trace-Id", "trace-safe");
        record.setResponseHeaders(responseHeaders);
        record.setResponseBody("{\"refreshToken\":\"step-response-secret\",\"status\":\"ok\"}");

        Map<String, Object> extractedVariables = new LinkedHashMap<String, Object>();
        extractedVariables.put("apiToken", "extracted-secret");
        extractedVariables.put("tenant", "A楼");
        record.setExtractedVariables(extractedVariables);
        record.setMatchedValues(Arrays.asList("Bearer matched-secret", "trace-safe"));
        record.setRawRequest("GET /api/secure?accessToken=step-query-secret HTTP/1.1\nAuthorization: Bearer step-auth-secret");
        record.setRawResponse("HTTP/1.1 200 OK\nSet-Cookie: STEP=step-response-cookie\n\n{\"refreshToken\":\"step-response-secret\"}");
        result.setStepRecords(Collections.singletonList(record));
        return result;
    }

    private ScanResult buildLargeResult(int index) {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/bulk/" + index);
        result.setVulnerable(true);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-bulk-" + index);
        poc.setName("批量漏洞-" + index);
        poc.setSeverity(index % 2 == 0 ? PocObj.Severity.HIGH : PocObj.Severity.MEDIUM);
        poc.setVulType("RCE");
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("用于验证大量结果导出场景-" + index);
        result.setPoc(poc);

        result.setMatchedPath("/bulk/" + index);
        result.setMatchedPayload("cmd=id-" + index);
        result.setRawRequest("POST /bulk/" + index + " HTTP/1.1\nX-Trace-Id: bulk-" + index + "\n\n" + repeat("request-" + index + "-", 240));
        result.setRawResponseSnippet("HTTP/1.1 200 OK\nContent-Type: application/json\n\n" + repeat("{\"result\":" + index + "}", 120));
        result.setRecommendation("批量修复建议-" + index);

        Map<String, Object> outputData = new LinkedHashMap<String, Object>();
        outputData.put("evidenceId", "CNVD-BULK-" + index);
        outputData.put("responseHash", "hash-" + index);
        result.setOutputData(outputData);

        return result;
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private String readUtf8(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String readWord(Path path) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(path.toFile()))) {
            StringBuilder builder = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
                builder.append(paragraph.getText()).append('\n');
            }
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : document.getTables()) {
                builder.append(table.getText()).append('\n');
            }
            return builder.toString();
        }
    }

    private Sheet readSheet(Path path, String sheetName) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(path.toFile()));
        Sheet sheet = workbook.getSheet(sheetName);
        workbook.close();
        return sheet;
    }

    private String readWorkbookText(Path path) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(path.toFile()));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    builder.append(cell.toString()).append('\n');
                }
            }
        }
        workbook.close();
        return builder.toString();
    }
}
