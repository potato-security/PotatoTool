package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 报告增强功能测试
 * 验证HTML、Word、Excel报告生成器是否正确处理增强的漏洞数据
 *
 * @author Potato
 * @date 2025/01/06
 */
public class ReportEnhancementTest {

    @TempDir
    Path tempDir;

    private List<ScanResult> mockResults;

    @BeforeEach
    public void setUp() {
        mockResults = new ArrayList<>();

        // 创建模拟的扫描结果（包含增强数据）
        ScanResult result1 = createMockResult(
            "http://example.com/api/login",
            "SQL注入漏洞",
            "sql-injection-login",
            PocObj.Severity.HIGH,
            "SQL Injection",
            "/api/login",
            "username=admin' OR '1'='1",
            "POST /api/login HTTP/1.1\n" +
            "Host: example.com\n" +
            "Content-Type: application/x-www-form-urlencoded\n" +
            "Content-Length: 35\n\n" +
            "username=admin' OR '1'='1&password=test",
            "HTTP/1.1 200 OK\n" +
            "Content-Type: application/json\n" +
            "Server: Apache/2.4.41\n\n" +
            "{\"status\":\"success\",\"message\":\"Login successful\",\"user\":{\"id\":1,\"username\":\"admin\"}}"
        );

        ScanResult result2 = createMockResult(
            "http://example.com/api/user/1",
            "未授权访问漏洞",
            "unauth-access-api",
            PocObj.Severity.CRITICAL,
            "Unauthorized Access",
            "/api/user/1",
            "无需payload",
            "GET /api/user/1 HTTP/1.1\n" +
            "Host: example.com\n" +
            "User-Agent: Mozilla/5.0\n",
            "HTTP/1.1 200 OK\n" +
            "Content-Type: application/json\n\n" +
            "{\"id\":1,\"username\":\"admin\",\"email\":\"admin@example.com\",\"role\":\"administrator\",\"password_hash\":\"$2a$10$...\"}"
        );

        ScanResult result3 = createMockResult(
            "http://example.com/upload",
            "文件上传漏洞",
            "file-upload-bypass",
            PocObj.Severity.MEDIUM,
            "File Upload",
            "/upload",
            "<?php system($_GET['cmd']); ?>",
            "POST /upload HTTP/1.1\n" +
            "Host: example.com\n" +
            "Content-Type: multipart/form-data; boundary=----WebKitFormBoundary\n\n" +
            "------WebKitFormBoundary\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"shell.php\"\n\n" +
            "<?php system($_GET['cmd']); ?>\n" +
            "------WebKitFormBoundary--",
            "HTTP/1.1 200 OK\n" +
            "Content-Type: text/html\n\n" +
            "<html><body>File uploaded successfully: /uploads/shell.php</body></html>"
        );

        mockResults.add(result1);
        mockResults.add(result2);
        mockResults.add(result3);
    }

    @Test
    public void testHtmlReportGeneration() throws Exception {
        HtmlReportGenerator generator = new HtmlReportGenerator();
        File reportFile = tempDir.resolve("test-report.html").toFile();

        File result = generator.generate(mockResults, reportFile.getAbsolutePath());

        assertTrue(result.exists(), "HTML报告文件应该被创建");
        assertTrue(result.length() > 0, "HTML报告文件不应该为空");

        // 读取文件内容验证关键元素
        String content = new String(java.nio.file.Files.readAllBytes(result.toPath()), java.nio.charset.StandardCharsets.UTF_8);

        // 验证包含增强的字段
        assertTrue(content.contains("检测路径"), "应包含检测路径标题");
        assertTrue(content.contains("检测 Payload"), "应包含Payload标题");
        assertTrue(content.contains("HTTP 请求"), "应包含HTTP请求标题");
        assertTrue(content.contains("HTTP 响应片段"), "应包含HTTP响应标题");

        // 验证实际数据
        assertTrue(content.contains("/api/login"), "应包含检测路径数据");
        assertTrue(content.contains("username=admin&#x27; OR &#x27;1&#x27;=&#x27;1"), "应包含转义后的Payload");
        assertTrue(content.contains("POST /api/login HTTP/1.1"), "应包含HTTP请求数据");
        assertTrue(content.contains("HTTP/1.1 200 OK"), "应包含HTTP响应数据");

        // 验证CSS样式
        assertTrue(content.contains("detail-section"), "应包含detail-section样式");
        assertTrue(content.contains("code-block"), "应包含code-block样式");

        System.out.println("✅ HTML报告生成测试通过");
        System.out.println("   报告路径: " + result.getAbsolutePath());
    }

    @Test
    public void testWordReportGeneration() throws Exception {
        WordReportGenerator generator = new WordReportGenerator();
        File reportFile = tempDir.resolve("test-report.docx").toFile();

        File result = generator.generate(mockResults, reportFile.getAbsolutePath());

        assertTrue(result.exists(), "Word报告文件应该被创建");
        assertTrue(result.length() > 0, "Word报告文件不应该为空");

        // Word文件格式验证（检查文件头）
        byte[] header = new byte[4];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(result)) {
            fis.read(header);
        }
        // DOCX文件是ZIP格式，以PK开头
        assertEquals('P', (char)header[0], "DOCX文件应该是ZIP格式");
        assertEquals('K', (char)header[1], "DOCX文件应该是ZIP格式");

        System.out.println("✅ Word报告生成测试通过");
        System.out.println("   报告路径: " + result.getAbsolutePath());
    }

    @Test
    public void testExcelReportGeneration() throws Exception {
        ExcelReportGenerator generator = new ExcelReportGenerator();
        File reportFile = tempDir.resolve("test-report.xlsx").toFile();

        File result = generator.generate(mockResults, reportFile.getAbsolutePath());

        assertTrue(result.exists(), "Excel报告文件应该被创建");
        assertTrue(result.length() > 0, "Excel报告文件不应该为空");

        // Excel文件格式验证（检查文件头）
        byte[] header = new byte[4];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(result)) {
            fis.read(header);
        }
        // XLSX文件也是ZIP格式，以PK开头
        assertEquals('P', (char)header[0], "XLSX文件应该是ZIP格式");
        assertEquals('K', (char)header[1], "XLSX文件应该是ZIP格式");

        System.out.println("✅ Excel报告生成测试通过");
        System.out.println("   报告路径: " + result.getAbsolutePath());
    }

    @Test
    public void testAllReportFormats() throws Exception {
        System.out.println("\n========== 完整报告生成测试 ==========");

        // 生成所有格式的报告
        HtmlReportGenerator htmlGen = new HtmlReportGenerator();
        WordReportGenerator wordGen = new WordReportGenerator();
        ExcelReportGenerator excelGen = new ExcelReportGenerator();

        File htmlFile = tempDir.resolve("full-report.html").toFile();
        File wordFile = tempDir.resolve("full-report.docx").toFile();
        File excelFile = tempDir.resolve("full-report.xlsx").toFile();

        File html = htmlGen.generate(mockResults, htmlFile.getAbsolutePath());
        File word = wordGen.generate(mockResults, wordFile.getAbsolutePath());
        File excel = excelGen.generate(mockResults, excelFile.getAbsolutePath());

        assertTrue(html.exists() && html.length() > 0, "HTML报告生成失败");
        assertTrue(word.exists() && word.length() > 0, "Word报告生成失败");
        assertTrue(excel.exists() && excel.length() > 0, "Excel报告生成失败");

        System.out.println("✅ 所有格式报告生成成功");
        System.out.println("   HTML: " + html.getAbsolutePath() + " (" + html.length() + " bytes)");
        System.out.println("   Word: " + word.getAbsolutePath() + " (" + word.length() + " bytes)");
        System.out.println("   Excel: " + excel.getAbsolutePath() + " (" + excel.length() + " bytes)");
        System.out.println("=====================================\n");
    }

    @Test
    public void testEmptyDataHandling() throws Exception {
        // 测试空数据处理
        ScanResult emptyResult = createMockResult(
            "http://example.com/test",
            "测试漏洞",
            "test-vuln",
            PocObj.Severity.LOW,
            "Test",
            null,  // 无检测路径
            null,  // 无payload
            null,  // 无请求
            null   // 无响应
        );

        List<ScanResult> emptyList = new ArrayList<>();
        emptyList.add(emptyResult);

        HtmlReportGenerator htmlGen = new HtmlReportGenerator();
        File htmlFile = tempDir.resolve("empty-data-report.html").toFile();

        // 应该不抛出异常
        assertDoesNotThrow(() -> htmlGen.generate(emptyList, htmlFile.getAbsolutePath()));

        assertTrue(htmlFile.exists(), "即使数据为空也应该生成报告");
        System.out.println("✅ 空数据处理测试通过");
    }

    @Test
    public void testLargePayloadHandling() throws Exception {
        // 测试大payload的处理
        StringBuilder largePayload = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largePayload.append("AAAA");
        }

        StringBuilder largeRequest = new StringBuilder();
        largeRequest.append("POST /test HTTP/1.1\nHost: example.com\n\n");
        for (int i = 0; i < 500; i++) {
            largeRequest.append("data=").append(i).append("&");
        }

        StringBuilder largeResponse = new StringBuilder();
        largeResponse.append("HTTP/1.1 200 OK\n\n");
        for (int i = 0; i < 1000; i++) {
            largeResponse.append("{\"item\":").append(i).append("}");
        }

        ScanResult largeResult = createMockResult(
            "http://example.com/large",
            "大数据测试",
            "large-data-test",
            PocObj.Severity.INFO,
            "Test",
            "/large",
            largePayload.toString(),
            largeRequest.toString(),
            largeResponse.toString()
        );

        List<ScanResult> largeList = new ArrayList<>();
        largeList.add(largeResult);

        HtmlReportGenerator htmlGen = new HtmlReportGenerator();
        WordReportGenerator wordGen = new WordReportGenerator();
        ExcelReportGenerator excelGen = new ExcelReportGenerator();

        File htmlFile = tempDir.resolve("large-data-report.html").toFile();
        File wordFile = tempDir.resolve("large-data-report.docx").toFile();
        File excelFile = tempDir.resolve("large-data-report.xlsx").toFile();

        assertDoesNotThrow(() -> {
            htmlGen.generate(largeList, htmlFile.getAbsolutePath());
            wordGen.generate(largeList, wordFile.getAbsolutePath());
            excelGen.generate(largeList, excelFile.getAbsolutePath());
        });

        assertTrue(htmlFile.exists() && wordFile.exists() && excelFile.exists(),
                  "大数据报告应该成功生成");
        System.out.println("✅ 大数据处理测试通过");
    }

    @Test
    public void testJsonReportShouldNotExportInternalWarnings() throws Exception {
        ScanResult result = createMockResult(
            "http://example.com/internal-warning",
            "内部告警字段导出边界测试",
            "internal-warning-boundary-test",
            PocObj.Severity.MEDIUM,
            "Boundary Test",
            "/api/test",
            "test-payload",
            "GET /api/test HTTP/1.1\nHost: example.com\n",
            "HTTP/1.1 200 OK\nContent-Type: application/json\n\n{\"ok\":true}"
        );

        // 模拟内部字段（应只存在运行态，不应进入导出报告）
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("semanticWarnings", java.util.Arrays.asList("semantic-warning-1"));
        details.put("conversionWarnings", java.util.Arrays.asList("conversion-warning-1"));
        details.put("unsupportedCapabilities", java.util.Arrays.asList("unsupported-cap-1"));
        result.setDetails(details);

        // outputData 是业务输出，应该保留
        java.util.Map<String, Object> outputData = new java.util.HashMap<>();
        outputData.put("businessKey", "businessValue");
        result.setOutputData(outputData);

        // POC内部诊断字段（同样不应导出）
        PocObj.Poc poc = result.getPoc();
        java.util.Map<String, Object> semWarn = new java.util.HashMap<>();
        semWarn.put("code", "semantic-warning-2");
        java.util.Map<String, Object> convWarn = new java.util.HashMap<>();
        convWarn.put("code", "conversion-warning-2");
        java.util.Map<String, Object> unsupWarn = new java.util.HashMap<>();
        unsupWarn.put("code", "unsupported-cap-2");

        poc.setSemanticWarnings(java.util.Arrays.asList(semWarn));
        poc.setConversionWarnings(java.util.Arrays.asList(convWarn));
        poc.setUnsupportedCapabilities(java.util.Arrays.asList(unsupWarn));

        java.util.List<ScanResult> results = new java.util.ArrayList<>();
        results.add(result);

        JsonReportGenerator generator = new JsonReportGenerator();
        File reportFile = tempDir.resolve("json-warning-boundary-report.json").toFile();
        File output = generator.generate(results, reportFile.getAbsolutePath());

        assertTrue(output.exists(), "JSON报告文件应该被创建");

        String content = new String(java.nio.file.Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);

        // 内部诊断字段不应导出
        assertFalse(content.contains("semanticWarnings"), "报告不应包含 semanticWarnings");
        assertFalse(content.contains("conversionWarnings"), "报告不应包含 conversionWarnings");
        assertFalse(content.contains("unsupportedCapabilities"), "报告不应包含 unsupportedCapabilities");
        assertFalse(content.contains("warningSummary"), "报告不应包含 warningSummary");
        assertFalse(content.contains("matcherPolicy"), "报告不应包含 matcherPolicy");
        assertFalse(content.contains("flowExecutionMode"), "报告不应包含 flowExecutionMode");

        // 业务字段应保留
        assertTrue(content.contains("outputData"), "报告应包含 outputData");
        assertTrue(content.contains("businessKey"), "报告应包含 outputData 业务键");
        assertTrue(content.contains("businessValue"), "报告应包含 outputData 业务值");

        System.out.println("✅ JSON报告内部字段导出边界测试通过");
    }

    /**
     * 创建模拟的扫描结果
     */
    private ScanResult createMockResult(String target, String pocName, String pocId,
                                       PocObj.Severity severity, String vulType,
                                       String matchedPath, String matchedPayload,
                                       String rawRequest, String rawResponseSnippet) {
        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setVulnerable(true);
        result.setTimestamp(System.currentTimeMillis());

        // 创建POC对象
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(pocId);
        poc.setName(pocName);
        poc.setSeverity(severity);
        poc.setVulType(vulType);
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("这是一个测试漏洞描述: " + pocName);

        result.setPoc(poc);

        // 设置增强数据
        result.setMatchedPath(matchedPath);
        result.setMatchedPayload(matchedPayload);
        result.setRawRequest(rawRequest);
        result.setRawResponseSnippet(rawResponseSnippet);

        return result;
    }
}
