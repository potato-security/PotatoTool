package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGeneratorCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateAllFormatsWithConsistentCoreFields() throws Exception {
        List<ScanResult> results = Arrays.asList(buildResult("alpha"), buildResult("beta"));
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("中文 报告目录").resolve("scan#result");

        List<File> files = generator.generateAllFormats(results, outputDir.toString());

        assertEquals(6, files.size());
        assertTrue(outputDir.resolve("report.txt").toFile().exists());

        String html = readUtf8(outputDir.resolve("report.html"));
        String csv = readUtf8(outputDir.resolve("report.csv"));
        String json = readUtf8(outputDir.resolve("report.json"));
        String txt = readUtf8(outputDir.resolve("report.txt"));
        String wordText = readWord(outputDir.resolve("report.docx"));
        Sheet detailsSheet = readDetailsSheet(outputDir.resolve("report.xlsx"));

        assertContainsCoreFields(html, "alpha");
        assertContainsCoreFields(csv, "alpha");
        assertContainsCoreFields(txt, "alpha");
        assertContainsCoreFields(wordText, "alpha");

        JsonObject reportJson = JsonParser.parseString(json).getAsJsonObject();
        JsonArray vulns = reportJson.getAsJsonArray("vulnerabilities");
        assertEquals(2, vulns.size());
        assertEquals("HIGH", vulns.get(0).getAsJsonObject().get("severity").getAsString());
        assertEquals("http", vulns.get(0).getAsJsonObject().get("protocol").getAsString());
        assertEquals("CNVD-ALPHA", vulns.get(0).getAsJsonObject().get("outputData").getAsJsonObject().get("evidenceId").getAsString());
        assertEquals("2", detailsSheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals("poc-beta", detailsSheet.getRow(2).getCell(3).getStringCellValue());
        assertEquals("HIGH", detailsSheet.getRow(1).getCell(4).getStringCellValue());
        assertEquals("http", detailsSheet.getRow(1).getCell(10).getStringCellValue());
        assertEquals("/api/beta", detailsSheet.getRow(2).getCell(12).getStringCellValue());
    }

    @Test
    void shouldPreserveChineseEncodingAndSpecialOutputPathAcrossFormats() throws Exception {
        List<ScanResult> results = Arrays.asList(buildChineseResult());
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("测试 空格").resolve("特殊#目录");

        generator.generateAllFormats(results, outputDir.toString());

        assertTrue(readUtf8(outputDir.resolve("report.txt")).contains("中文漏洞验证"));
        assertTrue(readUtf8(outputDir.resolve("report.csv")).startsWith("\uFEFF"));
        String html = readUtf8(outputDir.resolve("report.html"));
        assertTrue(html.contains("中文漏洞验证"));
        assertTrue(html.contains("/api/中文"));
        assertTrue(readUtf8(outputDir.resolve("report.json")).contains("中文漏洞验证"));
        assertTrue(readWord(outputDir.resolve("report.docx")).contains("中文漏洞验证"));
        assertEquals("中文漏洞验证", readDetailsSheet(outputDir.resolve("report.xlsx")).getRow(1).getCell(2).getStringCellValue());
    }

    @Test
    void shouldSupportChineseAndSpecialCharacterFileNames() throws Exception {
        List<ScanResult> results = Arrays.asList(buildChineseResult());
        ReportGenerator generator = new ReportGenerator();
        Path outputDir = tempDir.resolve("文件名验证");
        Files.createDirectories(outputDir);

        File txtFile = generator.generateReport("TXT", results, outputDir.resolve("扫描 结果#一.txt").toString(), "00:00:03");
        File jsonFile = generator.generateReport("JSON", results, outputDir.resolve("扫描 结果#一.json").toString(), "00:00:03");
        File htmlFile = generator.generateReport("HTML", results, outputDir.resolve("扫描 结果#一.html").toString(), "00:00:03");

        assertTrue(txtFile.exists());
        assertTrue(jsonFile.exists());
        assertTrue(htmlFile.exists());
        assertTrue(readUtf8(txtFile.toPath()).contains("中文漏洞验证"));
        assertTrue(readUtf8(jsonFile.toPath()).contains("中文漏洞验证"));
        assertTrue(readUtf8(htmlFile.toPath()).contains("中文漏洞验证"));
    }

    private void assertContainsCoreFields(String content, String suffix) {
        assertTrue(content.contains("http://127.0.0.1/" + suffix));
        assertTrue(content.contains("poc-" + suffix));
        assertTrue(content.contains("示例漏洞-" + suffix));
        assertTrue(content.contains("HIGH"));
        assertTrue(content.contains("http"));
        assertTrue(content.contains("/api/" + suffix));
        assertTrue(content.contains("CNVD-" + suffix.toUpperCase()));
        assertTrue(content.contains("X-Trace-Id: trace-" + suffix));
        assertTrue(content.contains("HTTP/1.1 200 OK"));
    }

    private ScanResult buildChineseResult() {
        ScanResult result = buildResult("中文");
        result.getPoc().setName("中文漏洞验证");
        result.getPoc().setDescription("用于验证中文路径、UTF-8 编码和特殊目录输出");
        result.setRecommendation("请在边界网关同步修复策略");
        return result;
    }

    private ScanResult buildResult(String suffix) {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/" + suffix);
        result.setVulnerable(true);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-" + suffix);
        poc.setName("示例漏洞-" + suffix);
        poc.setSeverity(PocObj.Severity.HIGH);
        poc.setVulType("RCE");
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("描述-" + suffix);
        result.setPoc(poc);

        result.setMatchedPath("/api/" + suffix);
        result.setMatchedPayload("cmd=id-" + suffix);
        result.setCveId("CVE-2026-0001");
        result.setCweId("CWE-79");
        result.setCvssScore("8.8");
        result.setRecommendation("立即修复-" + suffix);
        result.setRawRequest("POST /api/" + suffix + " HTTP/1.1\nX-Trace-Id: trace-" + suffix);
        result.setRawResponseSnippet("HTTP/1.1 200 OK\nContent-Type: application/json\n\n{\"result\":\"" + suffix + "\"}");
        result.setParamKeys(Arrays.asList("cmd", "tenant"));

        Map<String, String> variableValues = new LinkedHashMap<String, String>();
        variableValues.put("tenant", "building-" + suffix);
        variableValues.put("trace", "trace-" + suffix);
        result.setVariableValues(variableValues);

        Map<String, Object> outputData = new LinkedHashMap<String, Object>();
        outputData.put("evidenceId", "CNVD-" + suffix.toUpperCase());
        outputData.put("responseHash", "hash-" + suffix);
        result.setOutputData(outputData);

        StepExecutionRecord record = new StepExecutionRecord();
        record.setStepIndex(1);
        record.setRequestUrl("http://127.0.0.1/api/" + suffix);
        record.setRequestMethod("POST");
        record.setResponseCode(200);
        record.setResponseTime(21L);
        record.setMatched(true);
        result.setStepRecords(Arrays.asList(record));

        return result;
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

    private Sheet readDetailsSheet(Path path) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(path.toFile()));
        Sheet sheet = workbook.getSheet("漏洞详情");
        workbook.close();
        return sheet;
    }
}
