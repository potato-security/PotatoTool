package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("指定 Nuclei 真实模板执行测试")
public class SelectedNucleiRealTemplateExecutionTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ajax-api/2.0/mlflow/experiments/create", exchange ->
                writeJson(exchange, 200, "{\"experiment_id\":\"exp-001\"}"));
        server.createContext("/api/2.0/mlflow/runs/create", exchange -> {
            assertBodyContains(exchange, "\"experiment_id\": \"exp-001\"", "\"experiment_id\":\"exp-001\"");
            writeJson(exchange, 200, "{\"run\":{\"info\":{\"run_id\":\"run-001\"}}}");
        });
        server.createContext("/ajax-api/2.0/mlflow/upload-artifact", exchange ->
                write(exchange, 200, "ok", "text/plain"));
        server.createContext("/ajax-api/2.0/mlflow/experiments/delete", exchange ->
                write(exchange, 200, "ok", "text/plain"));
        server.createContext("/ajax-api/2.0/mlflow/registered-models/create", exchange ->
                write(exchange, 200, "ok", "text/plain"));
        server.createContext("/ajax-api/2.0/mlflow/model-versions/create", exchange -> {
            assertBodyContains(exchange, "dbfs:/run-001/artifacts/a%3f/../../../../../../../../../../../../");
            write(exchange, 200, "ok", "text/plain");
        });
        server.createContext("/model-versions/get-artifact", exchange ->
                write(exchange, 200, "root:x:0:0:", "text/plain"));

        server.createContext("/api/user_login", exchange ->
                write(exchange, 200, "{\"success\":true}", "application/json"));
        server.createContext("/plupload", exchange -> {
            String rawBody = readBodyRaw(exchange);
            assertTrue(rawBody.contains("bytes_uploaded") || rawBody.contains(".xml"));
            write(exchange, 200, "bytes_uploaded", "text/plain");
        });
        server.createContext("/userfiles/media/default", exchange ->
                write(exchange, 200, "<x:script xmlns:x=\"http://www.w3.org/1999/xhtml\">alert(document.domain)</x:script>", "application/xml"));

        server.createContext("/wp-json/", exchange ->
                write(exchange, 200, "{\"home\":\"https://example.test\",}", "application/json"));
        server.createContext("/wp-json/notificationx/v1/notification/1", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String expected = md5Unchecked("https://example.test");
            assertTrue(query != null && query.contains("api_key=" + expected));
            if (query.contains("SLEEP(6)")) {
                try {
                    Thread.sleep(6100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("notificationx 延迟模拟被中断", e);
                }
            }
            write(exchange, 401, "There is no notification created with this id", "application/json");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("CVE-2024-8859 真实模板应命中")
    void cve20248859ShouldExecuteSuccessfully() throws Exception {
        ScanResult result = executeTemplate("src/main/resources/poc/nucleiPoc/http/cves/2024/CVE-2024-8859.yaml");
        assertTrue(result.isVulnerable());
    }

    @Test
    @DisplayName("CVE-2022-0963 真实模板应命中")
    void cve20220963ShouldExecuteSuccessfully() throws Exception {
        ScanResult result = executeTemplate("src/main/resources/poc/nucleiPoc/http/cves/2022/CVE-2022-0963.yaml");
        assertTrue(result.isVulnerable());
    }

    @Test
    @DisplayName("notificationx-sqli 真实模板应命中")
    void notificationxTemplateShouldExecuteSuccessfully() throws Exception {
        ScanResult result = executeTemplate("src/main/resources/poc/nucleiPoc/http/vulnerabilities/wordpress/notificationx-sqli.yaml");
        assertTrue(result.isVulnerable());
    }

    @Test
    @DisplayName("CVE-2020-14144 模板应保留多 raw internal extractor 与 interactsh matcher 语义")
    void cve202014144ShouldPreserveSemanticChain() throws Exception {
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(
                "src/main/resources/poc/nucleiPoc/http/cves/2020/CVE-2020-14144.yaml");
        assertNotNull(nucleiPoc);
        PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
        assertNotNull(poc);
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());

        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertEquals(7, step.getRaw().size(), "应保留 7 个 raw 请求块");
        assertNotNull(step.getExtractors());
        assertEquals(3, step.getExtractors().size(), "应保留 csrf/auth_csrf/last_commit 提取链");
        assertNotNull(step.getMatchers());
        assertEquals("interactsh_protocol", step.getMatchers().get(0).getPart());
        assertEquals("body_1", step.getMatchers().get(1).getPart());
    }

    private ScanResult executeTemplate(String path) throws Exception {
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(path);
        assertNotNull(nucleiPoc, path);
        PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
        assertNotNull(poc, path);
        ScanConfig config = new ScanConfig();
        return new PocExecutor(config).execute(baseUrl, poc);
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        write(exchange, status, body, "application/json");
    }

    private static void write(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private static void assertBodyContains(HttpExchange exchange, String... expectedValues) throws IOException {
        String body = readBodyRaw(exchange);
        for (String expectedValue : expectedValues) {
            if (expectedValue != null && body.contains(expectedValue)) {
                return;
            }
        }
        throw new IOException("请求体不包含预期内容: " + body);
    }

    private static String readBodyRaw(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), "UTF-8");
        }
    }

    private static String md5(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] bytes = digest.digest(value.getBytes("UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static String md5Unchecked(String value) {
        try {
            return md5(value);
        } catch (Exception e) {
            throw new IllegalStateException("md5 计算失败", e);
        }
    }
}
