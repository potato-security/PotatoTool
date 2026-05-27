package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("多类型命中报告审计")
class ReportMultiTypeHitAuditTest {

    private static final Path AUDIT_DIR = Paths.get("target/vulnscan-report-audit");
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("应导出 30 种命中类型 JSON 报告且不泄漏运行时占位符")
    void shouldExportJsonReportForManyVulnerabilityTypesWithoutRuntimePlaceholders() throws Exception {
        List<AuditCase> cases = buildCases();
        server = startAuditServer(cases);
        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(true);
        config.setEnableResponseCache(true);
        config.setRetries(0);
        config.setTimeout(3);

        PocExecutor executor = new PocExecutor(config);
        List<ScanResult> results = new ArrayList<ScanResult>();
        for (int i = 0; i < cases.size(); i++) {
            AuditCase auditCase = cases.get(i);
            ScanResult result = executor.execute(target, buildPoc(auditCase, i));
            assertTrue(result.isVulnerable(), "预期命中失败: " + auditCase.vulnType);
            results.add(result);
        }

        Files.createDirectories(AUDIT_DIR);
        Path reportPath = AUDIT_DIR.resolve("multi-type-report.json");
        new JsonReportGenerator().generate(results, reportPath.toString(), true);

        String json = new String(Files.readAllBytes(reportPath), StandardCharsets.UTF_8);
        assertFalse(json.contains("{{UUID}}"));
        assertFalse(json.contains("{{randomAgent}}"));
        assertFalse(json.contains("{{LAZY_INTERACTSH}}"));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray vulnerabilities = root.getAsJsonArray("vulnerabilities");
        assertEquals(cases.size(), vulnerabilities.size());

        Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
        List<String> summaryRows = new ArrayList<String>();
        summaryRows.add("index\tpoc_id\tvuln_type\tseverity\tmatched_path\tstep_count");

        for (int i = 0; i < vulnerabilities.size(); i++) {
            JsonObject vuln = vulnerabilities.get(i).getAsJsonObject();
            String type = vuln.get("vulnType").getAsString();
            typeCounts.put(type, typeCounts.containsKey(type) ? typeCounts.get(type) + 1 : 1);
            assertTrue(vuln.has("matchedPath"), "报告缺少 matchedPath: " + type);
            assertTrue(vuln.has("stepRecords"), "报告缺少 stepRecords: " + type);
            assertTrue(vuln.getAsJsonArray("stepRecords").size() >= 1, "stepRecords 为空: " + type);

            summaryRows.add((i + 1) + "\t"
                    + vuln.get("pocId").getAsString() + "\t"
                    + type + "\t"
                    + vuln.get("severity").getAsString() + "\t"
                    + vuln.get("matchedPath").getAsString() + "\t"
                    + vuln.getAsJsonArray("stepRecords").size());
        }

        assertEquals(cases.size(), typeCounts.size(), "应覆盖互不重复的漏洞类型");
        Files.write(AUDIT_DIR.resolve("multi-type-summary.tsv"), summaryRows, StandardCharsets.UTF_8);
    }

    private HttpServer startAuditServer(List<AuditCase> cases) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        for (AuditCase auditCase : cases) {
            httpServer.createContext("/audit/" + auditCase.slug, exchange -> {
                exchange.getResponseHeaders().add("X-Audit-Type", auditCase.vulnType);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                write(exchange, 200, "VULN_TOKEN:" + auditCase.slug + "\nTYPE:" + auditCase.vulnType);
            });
        }
        httpServer.start();
        return httpServer;
    }

    private PocObj.Poc buildPoc(AuditCase auditCase, int index) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("audit-" + auditCase.slug);
        poc.setName("审计命中-" + auditCase.vulnType);
        poc.setProtocol("http");
        poc.setSeverity(auditCase.severity);
        poc.setVulType(auditCase.vulnType);
        poc.setOriginalFormat("nuclei");
        poc.setDescription("用于报告审计的可控命中类型: " + auditCase.vulnType);
        poc.setRecommendation("按 " + auditCase.vulnType + " 类型进行加固");
        poc.setCweId("CWE-" + (100 + index));
        poc.setTags(Arrays.asList("report-audit", auditCase.slug));

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath("/audit/" + auditCase.slug + "?case=" + index);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Audit-Case", auditCase.slug);
        if (index == 0) {
            headers.put("User-Agent", "{{randomAgent}}; ReportAudit");
        }
        if (index == 1) {
            headers.put("X-Request-ID", "{{UUID}}");
        }
        step.setHeaders(headers);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList("VULN_TOKEN:" + auditCase.slug));
        step.setMatchers(Collections.singletonList(matcher));
        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private List<AuditCase> buildCases() {
        return Arrays.asList(
                c("rce", "Remote Code Execution", PocObj.Severity.CRITICAL),
                c("cmd-injection", "Command Injection", PocObj.Severity.CRITICAL),
                c("sqli", "SQL Injection", PocObj.Severity.HIGH),
                c("xss", "Cross-Site Scripting", PocObj.Severity.MEDIUM),
                c("ssrf", "Server-Side Request Forgery", PocObj.Severity.HIGH),
                c("lfi", "Local File Inclusion", PocObj.Severity.HIGH),
                c("rfi", "Remote File Inclusion", PocObj.Severity.CRITICAL),
                c("path-traversal", "Path Traversal", PocObj.Severity.HIGH),
                c("file-upload", "Arbitrary File Upload", PocObj.Severity.CRITICAL),
                c("default-login", "Default Login", PocObj.Severity.HIGH),
                c("weak-password", "Weak Password", PocObj.Severity.HIGH),
                c("info-disclosure", "Information Disclosure", PocObj.Severity.MEDIUM),
                c("exposed-panel", "Exposed Admin Panel", PocObj.Severity.MEDIUM),
                c("directory-listing", "Directory Listing", PocObj.Severity.LOW),
                c("open-redirect", "Open Redirect", PocObj.Severity.LOW),
                c("crlf", "CRLF Injection", PocObj.Severity.MEDIUM),
                c("xxe", "XML External Entity", PocObj.Severity.HIGH),
                c("deserialization", "Insecure Deserialization", PocObj.Severity.CRITICAL),
                c("ssti", "Server-Side Template Injection", PocObj.Severity.CRITICAL),
                c("prototype-pollution", "Prototype Pollution", PocObj.Severity.HIGH),
                c("csrf", "Cross-Site Request Forgery", PocObj.Severity.MEDIUM),
                c("idor", "Insecure Direct Object Reference", PocObj.Severity.HIGH),
                c("cors", "CORS Misconfiguration", PocObj.Severity.MEDIUM),
                c("missing-headers", "Missing Security Headers", PocObj.Severity.LOW),
                c("backup", "Exposed Backup File", PocObj.Severity.MEDIUM),
                c("git-exposure", "Git Repository Exposure", PocObj.Severity.HIGH),
                c("env-exposure", "Environment File Exposure", PocObj.Severity.HIGH),
                c("swagger", "Swagger API Exposure", PocObj.Severity.LOW),
                c("jwt", "JWT Secret Exposure", PocObj.Severity.CRITICAL),
                c("debug", "Debug Mode Exposure", PocObj.Severity.MEDIUM)
        );
    }

    private AuditCase c(String slug, String vulnType, PocObj.Severity severity) {
        return new AuditCase(slug, vulnType, severity);
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static class AuditCase {
        private final String slug;
        private final String vulnType;
        private final PocObj.Severity severity;

        private AuditCase(String slug, String vulnType, PocObj.Severity severity) {
            this.slug = slug;
            this.vulnType = vulnType;
            this.severity = severity;
        }
    }
}
