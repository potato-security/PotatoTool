package com.potato.potatotool.content.redTeam.vulnScanner.correctness;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("金标准目标扫描结果正确性测试")
public class GoldenTargetScanCorrectnessTest {

    private static final Path EVIDENCE_ROOT = Paths.get("target/vulnscan-correctness");
    private static final List<TargetReport> TARGET_REPORTS = Collections.synchronizedList(new ArrayList<TargetReport>());

    private HttpServer server;

    @Test
    @DisplayName("安全基线页不应命中第一批任何指纹或面板")
    public void shouldReportNoFindingsForSafeBaseline() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "safe-http-baseline",
                "<html><head><title>Safe Control Page</title></head><body>safe page</body></html>",
                Collections.<String, String>emptyMap(),
                Collections.<String>emptySet()
        );

        assertGoldenScan(target, loadFirstBatchPocs());
    }

    @Test
    @DisplayName("只存在 OWASP Juice Shop 指纹时不应误报其他技术")
    public void shouldOnlyReportExpectedJuiceShopFinding() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "juice-shop-only",
                "<html><head><title>OWASP Juice Shop</title></head><body>safe page</body></html>",
                Collections.<String, String>emptyMap(),
                Collections.singleton("owasp-juice-shop-detect")
        );

        assertGoldenScan(target, loadFirstBatchPocs());
    }

    @Test
    @DisplayName("只存在 H2 Console 面板时不应误报其他技术")
    public void shouldOnlyReportExpectedH2Finding() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "h2-console-unacc",
                "<html><head><title>Safe Control Page</title></head><body>safe page</body></html>",
                Collections.<String, String>emptyMap(),
                Collections.singleton("h2console-panel")
        );
        target.pathBodies.put("/h2-console/login.jsp",
                "<html><head><title>H2 Console</title></head><body>login</body></html>");

        assertGoldenScan(target, loadFirstBatchPocs());
    }

    @Test
    @DisplayName("只存在 InfluxDB header 时不应误报其他技术")
    public void shouldOnlyReportExpectedInfluxFinding() throws Exception {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Influxdb-Version", "1.8.10");
        GoldenTarget target = startGoldenTarget(
                "influxdb-unacc",
                "<html><head><title>Safe Control Page</title></head><body>safe page</body></html>",
                headers,
                Collections.singleton("influxdb-version-detect")
        );

        assertGoldenScan(target, loadFirstBatchPocs());
    }

    @Test
    @DisplayName("只存在 Gitea 安装页时不应误报其他技术或高危漏洞")
    public void shouldOnlyReportExpectedGiteaInstallerFinding() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "gitea-installer",
                "<html><head><title>Installation - Gitea: Git with a cup of tea</title></head><body>Database Name</body></html>",
                Collections.<String, String>emptyMap(),
                Collections.singleton("gitea-installer")
        );

        assertGoldenScan(target, loadFirstBatchPocs());
    }

    @Test
    @DisplayName("只存在 phpMyAdmin Setup 页时不应误报其他 phpMyAdmin 家族问题")
    public void shouldOnlyReportExpectedPhpMyAdminSetupFinding() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "phpmyadmin-setup",
                "<html><head><title>Safe Control Page</title></head><body>safe page</body></html>",
                Collections.<String, String>emptyMap(),
                Collections.singleton("phpmyadmin-setup")
        );
        target.pathBodies.put("/phpmyadmin/scripts/setup.php",
                "<html><head><title>phpMyAdmin setup</title></head>"
                        + "<body>You want to configure phpMyAdmin using web interface</body></html>");

        assertGoldenScan(target, loadPhpMyAdminSecondBatchPocs());
    }

    @Test
    @DisplayName("只存在 Jenkins 基础指纹时不应误报登录页、开放注册或弱口令")
    public void shouldOnlyReportExpectedJenkinsDetectFinding() throws Exception {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Jenkins", "2.426.3");
        GoldenTarget target = startGoldenTarget(
                "jenkins-detect",
                "<html><head><title>Jenkins Controller</title></head><body>Jenkins controller ready</body></html>",
                headers,
                Collections.singleton("jenkins-detect")
        );

        assertGoldenScan(target, loadJenkinsSecondBatchPocs());
    }

    @Test
    @DisplayName("只存在 Grafana 登录面板且默认口令成功时不应误报公开注册")
    public void shouldOnlyReportExpectedGrafanaAuthFindings() throws Exception {
        GoldenTarget target = startGoldenTarget(
                "grafana-auth",
                "<html><head><title>Safe Control Page</title></head><body>safe page</body></html>",
                Collections.<String, String>emptyMap(),
                new LinkedHashSet<String>(Arrays.asList("grafana-detect", "grafana-default-login"))
        );
        target.methodPathResponses.put("GET /login",
                htmlResponse("<html><head><title>Grafana</title></head>"
                        + "<body>{\"subTitle\":\"Grafana v10.4.1\"}</body></html>"));
        target.dynamicResponders.put("POST /login", new GoldenResponder() {
            @Override
            public GoldenResponse respond(HttpExchange exchange) throws IOException {
                String requestBody = readRequestBody(exchange);
                if (requestBody.contains("\"user\":\"admin\"")
                        && requestBody.contains("\"password\":\"admin\"")) {
                    GoldenResponse response = jsonResponse(200, "{\"message\":\"Logged in\"}");
                    response.headers.put("Set-Cookie", "grafana_session=golden-session; Path=/; HttpOnly");
                    return response;
                }
                return jsonResponse(401, "{\"message\":\"Invalid username or password\"}");
            }
        });
        target.methodPathResponses.put("POST /api/user/signup/step2",
                jsonResponse(403, "{\"message\":\"Sign up is disabled\"}"));

        assertGoldenScan(target, loadGrafanaSecondBatchPocs());
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @BeforeAll
    public static void prepareEvidenceRoot() throws IOException {
        TARGET_REPORTS.clear();
        if (Files.exists(EVIDENCE_ROOT)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(EVIDENCE_ROOT)) {
                for (Path path : stream) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(EVIDENCE_ROOT);
    }

    @AfterAll
    public static void writeBatchReports() throws IOException {
        Files.createDirectories(EVIDENCE_ROOT);
        Files.write(EVIDENCE_ROOT.resolve("target-summary.tsv"), buildTargetSummaryRows(), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("target-results.tsv"), buildTargetResultRows(), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("fp.tsv"), buildFalsePositiveRows(), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("fn.tsv"), buildFalseNegativeRows(), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("blocked.tsv"), Collections.singletonList("target\tpoc_id\treason"), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("suspected-layer-summary.tsv"), buildSuspectedLayerRows(), StandardCharsets.UTF_8);
    }

    private GoldenTarget startGoldenTarget(String name,
                                           String rootBody,
                                           Map<String, String> rootHeaders,
                                           Set<String> expectedFindings) throws IOException {
        final GoldenTarget target = new GoldenTarget();
        target.name = name;
        target.rootBody = rootBody;
        target.rootHeaders.putAll(rootHeaders);
        target.expectedFindingIds.addAll(expectedFindings);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleGoldenRequest(target, exchange));
        server.start();
        target.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return target;
    }

    private void handleGoldenRequest(GoldenTarget target, HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String routeKey = exchange.getRequestMethod() + " " + path;
        GoldenResponse response = null;
        GoldenResponder responder = target.dynamicResponders.get(routeKey);
        if (responder != null) {
            response = responder.respond(exchange);
        }
        if (response == null && target.methodPathResponses.containsKey(routeKey)) {
            response = target.methodPathResponses.get(routeKey);
        }
        if (response == null) {
            String body = target.pathBodies.containsKey(path) ? target.pathBodies.get(path) : target.rootBody;
            response = htmlResponse(body);
        }
        for (Map.Entry<String, String> header : target.rootHeaders.entrySet()) {
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        }
        for (Map.Entry<String, String> header : response.headers.entrySet()) {
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        }
        exchange.getResponseHeaders().set("Content-Type", response.contentType);
        byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private List<PocObj.Poc> loadPocs(List<String> paths) throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        List<PocObj.Poc> pocs = new ArrayList<PocObj.Poc>();
        for (String path : paths) {
            PocObj.Poc poc = loader.loadFromFile(path);
            assertTrue(poc != null, "POC 加载失败: " + path);
            pocs.add(poc);
        }
        return pocs;
    }

    private List<PocObj.Poc> loadFirstBatchPocs() throws Exception {
        return loadPocs(Arrays.asList(
                "src/main/resources/poc/nucleiPoc/http/technologies/owasp-juice-shop-detected.yaml",
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/h2console-panel.yaml",
                "src/main/resources/poc/nucleiPoc/http/technologies/influxdb-version-detect.yaml",
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/installer/gitea-installer.yaml",
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/gitea-login.yaml",
                "src/main/resources/poc/nucleiPoc/http/vulnerabilities/gitea/gitea-rce.yaml",
                "src/main/resources/poc/nucleiPoc/http/technologies/wordpress-detect.yaml",
                "src/main/resources/poc/nucleiPoc/http/technologies/springboot-actuator.yaml",
                "src/main/resources/poc/nucleiPoc/http/technologies/nginx/default-nginx-page.yaml",
                "src/main/resources/poc/nucleiPoc/http/technologies/apache/tomcat-detect.yaml"
        ));
    }

    private List<PocObj.Poc> loadPhpMyAdminSecondBatchPocs() throws Exception {
        return loadPocs(Arrays.asList(
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/phpmyadmin/phpmyadmin-setup.yaml",
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/phpmyadmin-panel.yaml",
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/phpmyadmin/phpmyadmin-server-import.yaml",
                "src/main/resources/poc/nucleiPoc/http/default-logins/phpmyadmin/phpmyadmin-default-login.yaml",
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/phpmyadmin/phpmyadmin-misconfiguration.yaml",
                "src/main/resources/poc/nucleiPoc/http/vulnerabilities/phpmyadmin-unauth.yaml"
        ));
    }

    private List<PocObj.Poc> loadJenkinsSecondBatchPocs() throws Exception {
        return loadPocs(Arrays.asList(
                "src/main/resources/poc/nucleiPoc/http/technologies/jenkins-detect.yaml",
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/jenkins-login.yaml",
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/jenkins-api-panel.yaml",
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/jenkins/jenkins-openuser-register.yaml",
                "src/main/resources/poc/nucleiPoc/http/default-logins/jenkins/jenkins-default.yaml",
                "src/main/resources/poc/nucleiPoc/http/vulnerabilities/jenkins/unauthenticated-jenkins.yaml"
        ));
    }

    private List<PocObj.Poc> loadGrafanaSecondBatchPocs() throws Exception {
        return loadPocs(Arrays.asList(
                "src/main/resources/poc/nucleiPoc/http/exposed-panels/grafana-detect.yaml",
                "src/main/resources/poc/nucleiPoc/http/default-logins/grafana/grafana-default-login.yaml",
                "src/main/resources/poc/nucleiPoc/http/misconfiguration/grafana-public-signup.yaml"
        ));
    }

    private void assertGoldenScan(GoldenTarget target, List<PocObj.Poc> pocs) throws Exception {
        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setDebug(false);
        PocExecutor executor = new PocExecutor(config);

        List<ScanResult> results = new ArrayList<ScanResult>();
        Set<String> actualFindingIds = new LinkedHashSet<String>();
        List<String> rows = new ArrayList<String>();
        rows.add("target\tpoc_id\tpoc_name\tvulnerable\tmatched_path\tresponse_code\tmatched");

        for (PocObj.Poc poc : pocs) {
            ScanResult result = executor.execute(target.baseUrl, poc);
            results.add(result);
            if (result.isVulnerable()) {
                actualFindingIds.add(poc.getId());
            }
            rows.add(target.name + "\t"
                    + clean(poc.getId()) + "\t"
                    + clean(poc.getName()) + "\t"
                    + result.isVulnerable() + "\t"
                    + clean(result.getMatchedPath()) + "\t"
                    + firstResponseCode(result) + "\t"
                    + firstMatchedFlag(result));
        }

        Set<String> missingFindingIds = difference(target.expectedFindingIds, actualFindingIds);
        Set<String> unexpectedFindingIds = difference(actualFindingIds, target.expectedFindingIds);

        Files.createDirectories(EVIDENCE_ROOT);
        Files.write(EVIDENCE_ROOT.resolve(target.name + "-results.tsv"), rows, StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve(target.name + "-summary.tsv"),
                buildSummary(target, actualFindingIds, missingFindingIds, unexpectedFindingIds).getBytes(StandardCharsets.UTF_8));

        TARGET_REPORTS.add(new TargetReport(
                target.name,
                target.baseUrl,
                target.expectedFindingIds,
                actualFindingIds,
                missingFindingIds,
                unexpectedFindingIds,
                rows.subList(1, rows.size())
        ));

        assertEquals(target.expectedFindingIds, actualFindingIds,
                "金标准目标扫描结果不匹配，说明存在误报或漏报。证据: "
                        + EVIDENCE_ROOT.resolve(target.name + "-results.tsv"));
    }

    private String buildSummary(GoldenTarget target,
                                Set<String> actualFindingIds,
                                Set<String> missingFindingIds,
                                Set<String> unexpectedFindingIds) {
        return "target\tbase_url\texpected_findings\tactual_findings\tmissing_findings\tunexpected_findings\n"
                + target.name + "\t"
                + target.baseUrl + "\t"
                + join(target.expectedFindingIds) + "\t"
                + join(actualFindingIds) + "\t"
                + join(missingFindingIds) + "\t"
                + join(unexpectedFindingIds) + "\n";
    }

    private static List<String> buildTargetSummaryRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("target\tbase_url\texpected_findings\tactual_findings\tmissing_findings\tunexpected_findings");
        for (TargetReport report : TARGET_REPORTS) {
            rows.add(report.targetName + "\t"
                    + report.baseUrl + "\t"
                    + join(report.expectedFindingIds) + "\t"
                    + join(report.actualFindingIds) + "\t"
                    + join(report.missingFindingIds) + "\t"
                    + join(report.unexpectedFindingIds));
        }
        return sortBody(rows);
    }

    private static List<String> buildTargetResultRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("target\tpoc_id\tpoc_name\tvulnerable\tmatched_path\tresponse_code\tmatched");
        for (TargetReport report : TARGET_REPORTS) {
            rows.addAll(report.resultRows);
        }
        return sortBody(rows);
    }

    private static List<String> buildFalsePositiveRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("target\tpoc_id");
        for (TargetReport report : TARGET_REPORTS) {
            for (String findingId : report.unexpectedFindingIds) {
                rows.add(report.targetName + "\t" + findingId);
            }
        }
        return sortBody(rows);
    }

    private static List<String> buildFalseNegativeRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("target\tpoc_id");
        for (TargetReport report : TARGET_REPORTS) {
            for (String findingId : report.missingFindingIds) {
                rows.add(report.targetName + "\t" + findingId);
            }
        }
        return sortBody(rows);
    }

    private static List<String> buildSuspectedLayerRows() {
        List<String> rows = new ArrayList<String>();
        rows.add("target\tsymptom\tsuspected_layer\tfinding_ids");
        for (TargetReport report : TARGET_REPORTS) {
            if (!report.unexpectedFindingIds.isEmpty()) {
                rows.add(report.targetName + "\tfalse_positive\t"
                        + suspectedLayer(report.targetName, true) + "\t"
                        + join(report.unexpectedFindingIds));
            }
            if (!report.missingFindingIds.isEmpty()) {
                rows.add(report.targetName + "\tfalse_negative\t"
                        + suspectedLayer(report.targetName, false) + "\t"
                        + join(report.missingFindingIds));
            }
        }
        return sortBody(rows);
    }

    private static List<String> sortBody(List<String> rows) {
        if (rows.size() <= 2) {
            return rows;
        }
        List<String> body = new ArrayList<String>(rows.subList(1, rows.size()));
        Collections.sort(body);
        List<String> sorted = new ArrayList<String>();
        sorted.add(rows.get(0));
        sorted.addAll(body);
        return sorted;
    }

    private static String suspectedLayer(String targetName, boolean falsePositive) {
        if ("influxdb-unacc".equals(targetName)) {
            return "handler/extractor";
        }
        if ("gitea-installer".equals(targetName)) {
            return falsePositive ? "matcher/executor" : "matcher/handler";
        }
        if ("phpmyadmin-setup".equals(targetName)) {
            return falsePositive ? "matcher/handler" : "matcher/executor";
        }
        if ("jenkins-detect".equals(targetName)) {
            return falsePositive ? "matcher/extractor" : "handler/extractor";
        }
        if ("grafana-auth".equals(targetName)) {
            return "config/executor";
        }
        return "matcher/handler";
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        if (inputStream == null) {
            return "";
        }

        byte[] buffer = new byte[1024];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = inputStream.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static GoldenResponse htmlResponse(String body) {
        GoldenResponse response = new GoldenResponse();
        response.contentType = "text/html; charset=utf-8";
        response.body = body;
        return response;
    }

    private static GoldenResponse jsonResponse(int status, String body) {
        GoldenResponse response = new GoldenResponse();
        response.status = status;
        response.contentType = "application/json; charset=utf-8";
        response.body = body;
        return response;
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> values = new LinkedHashSet<String>(left);
        values.removeAll(right);
        return values;
    }

    private int firstResponseCode(ScanResult result) {
        if (result.getStepRecords() == null || result.getStepRecords().isEmpty()) {
            return -1;
        }
        return result.getStepRecords().get(0).getResponseCode();
    }

    private boolean firstMatchedFlag(ScanResult result) {
        return result.getStepRecords() != null
                && !result.getStepRecords().isEmpty()
                && result.getStepRecords().get(0).isMatched();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    private static String join(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static class GoldenTarget {
        String name;
        String baseUrl;
        String rootBody;
        Map<String, String> rootHeaders = new LinkedHashMap<String, String>();
        Map<String, String> pathBodies = new LinkedHashMap<String, String>();
        Map<String, GoldenResponse> methodPathResponses = new LinkedHashMap<String, GoldenResponse>();
        Map<String, GoldenResponder> dynamicResponders = new LinkedHashMap<String, GoldenResponder>();
        Set<String> expectedFindingIds = new LinkedHashSet<String>();
    }

    private interface GoldenResponder {
        GoldenResponse respond(HttpExchange exchange) throws IOException;
    }

    private static class GoldenResponse {
        int status = 200;
        String contentType = "text/html; charset=utf-8";
        String body = "";
        Map<String, String> headers = new LinkedHashMap<String, String>();
    }

    private static class TargetReport {
        String targetName;
        String baseUrl;
        Set<String> expectedFindingIds;
        Set<String> actualFindingIds;
        Set<String> missingFindingIds;
        Set<String> unexpectedFindingIds;
        List<String> resultRows;

        private TargetReport(String targetName,
                             String baseUrl,
                             Set<String> expectedFindingIds,
                             Set<String> actualFindingIds,
                             Set<String> missingFindingIds,
                             Set<String> unexpectedFindingIds,
                             List<String> resultRows) {
            this.targetName = targetName;
            this.baseUrl = baseUrl;
            this.expectedFindingIds = new LinkedHashSet<String>(expectedFindingIds);
            this.actualFindingIds = new LinkedHashSet<String>(actualFindingIds);
            this.missingFindingIds = new LinkedHashSet<String>(missingFindingIds);
            this.unexpectedFindingIds = new LinkedHashSet<String>(unexpectedFindingIds);
            this.resultRows = new ArrayList<String>(resultRows);
        }
    }
}
