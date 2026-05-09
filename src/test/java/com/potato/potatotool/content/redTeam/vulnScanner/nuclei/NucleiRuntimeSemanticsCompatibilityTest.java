package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Nuclei 运行时语义兼容测试")
public class NucleiRuntimeSemanticsCompatibilityTest {

    private static HttpServer server;
    private static String baseUrl;
    private static AtomicInteger stopAtFirstCounter;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/mlflow/one", exchange -> writeJson(exchange, 200, "{\"experiment_id\":\"exp-001\"}"));
        server.createContext("/mlflow/two", exchange -> {
            assertBodyContains(exchange, "\"experiment_id\": \"exp-001\"");
            writeJson(exchange, 200, "{\"run\":{\"info\":{\"run_id\":\"run-001\"}}}");
        });
        server.createContext("/mlflow/three", exchange -> {
            assertQueryContains(exchange, "run_uuid=run-001");
            write(exchange, 200, "root:x:0:0:", "text/plain");
        });

        server.createContext("/cookie/login", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "i_like_gitea=session-001; Path=/; HttpOnly");
            write(exchange, 200, "<input name=\"_csrf\" value=\"csrf-001\">", "text/html");
        });
        server.createContext("/cookie/auth", exchange -> {
            assertHeaderContains(exchange, "Cookie", "i_like_gitea=session-001");
            String rawBody = readBodyRaw(exchange);
            if (!rawBody.contains("password=p%40ss+word")) {
                write(exchange, 400, "expected encoded password missing: " + rawBody, "text/plain");
                throw new IOException("expected encoded password missing");
            }
            write(exchange, 200, "<input name=\"last_commit\" value=\"commit-001\">", "text/html");
        });

        server.createContext("/rand/upload", exchange -> {
            String body = readBody(exchange);
            boolean lowerCaseName = body.matches("(?s).*name=\"[a-z0-9]+\\.xml\".*");
            write(exchange, lowerCaseName ? 200 : 400, "bytes_uploaded", "text/plain");
        });
        server.createContext("/rand/fetch", exchange -> {
            boolean lowerCasePath = exchange.getRequestURI().getPath().matches(".*/[a-z0-9]+\\.xml$");
            write(exchange, lowerCasePath ? 200 : 404, "alert(document.domain)", "text/plain");
        });

        server.createContext("/wp-json/", exchange -> write(exchange, 200, "{\"home\":\"https://example.test\",}", "application/json"));
        server.createContext("/wp-json/notificationx/v1/notification/1", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String expected = md5("https://example.test");
            boolean ok = query != null && query.contains("api_key=" + expected);
            write(exchange, ok ? 401 : 400,
                    "There is no notification created with this id",
                    "application/json");
        });
        server.createContext("/flow/one", exchange -> write(exchange, 200, "flow-one-ok", "text/plain"));
        server.createContext("/flow/two", exchange -> write(exchange, 200, "flow-two-ok", "text/plain"));
        server.createContext("/paths/fail", exchange -> write(exchange, 404, "path-fail", "text/plain"));
        server.createContext("/paths/pass", exchange -> write(exchange, 200, "path-pass", "text/plain"));
        stopAtFirstCounter = new AtomicInteger(0);
        server.createContext("/paths/stop-pass", exchange -> {
            stopAtFirstCounter.incrementAndGet();
            write(exchange, 200, "stop-pass", "text/plain");
        });
        server.createContext("/paths/stop-next", exchange -> {
            stopAtFirstCounter.incrementAndGet();
            write(exchange, 200, "stop-next", "text/plain");
        });
        server.createContext("/iterate/index", exchange -> writeJson(exchange, 200, "[\"miss\",\"hit\"]"));
        server.createContext("/iterate/miss", exchange -> write(exchange, 404, "iterate-miss", "text/plain"));
        server.createContext("/iterate/hit", exchange -> write(exchange, 200, "iterate-hit", "text/plain"));
        server.createContext("/iterate/raw-index", exchange -> write(exchange, 200, "item=miss\nitem=hit\n", "text/plain"));
        server.createContext("/iterate/raw/miss", exchange -> write(exchange, 404, "raw-miss", "text/plain"));
        server.createContext("/iterate/raw/hit", exchange -> write(exchange, 200, "raw-hit", "text/plain"));
        server.createContext("/code/output-from-code", exchange -> write(exchange, 200, "code-response-ok", "text/plain"));
        server.createContext("/javascript/output-from-js", exchange -> write(exchange, 200, "javascript-response-ok", "text/plain"));

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
    @DisplayName("internal JSON extractor 和 body_N part 应参与多 raw 变量链")
    void internalJsonExtractorAndIndexedPartShouldDriveLaterRawRequests() throws Exception {
        String yaml =
                "id: runtime-indexed-json\n" +
                "info:\n" +
                "  name: runtime-indexed-json\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - raw:\n" +
                "      - |\n" +
                "        GET /mlflow/one HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "      - |\n" +
                "        POST /mlflow/two HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "        Content-Type: application/json\n" +
                "\n" +
                "        {\"experiment_id\": \"{{EXPERIMENT_ID}}\"}\n" +
                "      - |\n" +
                "        GET /mlflow/three?run_uuid={{RUN_ID}} HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body_1\n" +
                "        words:\n" +
                "          - experiment_id\n" +
                "      - type: regex\n" +
                "        regex:\n" +
                "          - \"root:.*:0:0:\"\n" +
                "      - type: status\n" +
                "        status:\n" +
                "          - 200\n" +
                "    matchers-condition: and\n" +
                "    extractors:\n" +
                "      - type: json\n" +
                "        part: body_1\n" +
                "        name: EXPERIMENT_ID\n" +
                "        json:\n" +
                "          - '.experiment_id'\n" +
                "        internal: true\n" +
                "      - type: json\n" +
                "        part: body_2\n" +
                "        name: RUN_ID\n" +
                "        json:\n" +
                "          - '.run.info.run_id'\n" +
                "        internal: true\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "indexed JSON internal extractor 应能驱动后续 raw 请求命中");
    }

    @Test
    @DisplayName("cookie-reuse 与 url_encode(password) 应符合 Nuclei 请求语义")
    void cookieReuseAndBareVariableHelperShouldWork() throws Exception {
        String yaml =
                "id: runtime-cookie-helper\n" +
                "info:\n" +
                "  name: runtime-cookie-helper\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - cookie-reuse: true\n" +
                "    payloads:\n" +
                "      username:\n" +
                "        - admin\n" +
                "      password:\n" +
                "        - p@ss word\n" +
                "    raw:\n" +
                "      - |\n" +
                "        GET /cookie/login HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "      - |\n" +
                "        POST /cookie/auth HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "        Content-Type: application/x-www-form-urlencoded\n" +
                "\n" +
                "        _csrf={{csrf}}&user_name={{username}}&password={{url_encode(password)}}\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body_2\n" +
                "        words:\n" +
                "          - last_commit\n" +
                "    extractors:\n" +
                "      - type: regex\n" +
                "        name: csrf\n" +
                "        group: 1\n" +
                "        regex:\n" +
                "          - 'name=\"_csrf\" value=\"(.*?)\"'\n" +
                "        internal: true\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "cookie-reuse 与裸变量 helper 表达式应正常替换");
    }

    @Test
    @DisplayName("randstr 应保持单次执行稳定且 helper 表达式可嵌套求值")
    void randstrAndNestedHelperShouldWorkAcrossRawRequests() throws Exception {
        String yaml =
                "id: runtime-randstr-helper\n" +
                "info:\n" +
                "  name: runtime-randstr-helper\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - raw:\n" +
                "      - |\n" +
                "        POST /rand/upload HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "        Content-Type: text/plain\n" +
                "\n" +
                "        name=\"{{to_lower(\"{{randstr}}\")}}.xml\"\n" +
                "      - |\n" +
                "        GET /rand/fetch/{{to_lower(\"{{randstr}}\")}}.xml HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "    matchers:\n" +
                "      - type: dsl\n" +
                "        dsl:\n" +
                "          - 'contains(body_2,\"alert(document.domain)\")'\n" +
                "          - 'status_code_2==200'\n" +
                "          - 'contains(body_1,\"bytes_uploaded\")'\n" +
                "        condition: and\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "randstr 与 to_lower 嵌套 helper 应在多 raw 中保持一致");
    }

    @Test
    @DisplayName("md5('{{var}}') helper 应在第二个 raw 请求中求值")
    void md5NestedQuotedVariableShouldWork() throws Exception {
        String yaml =
                "id: runtime-md5-helper\n" +
                "info:\n" +
                "  name: runtime-md5-helper\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - raw:\n" +
                "      - |\n" +
                "        GET /wp-json/ HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "      - |\n" +
                "        GET /wp-json/notificationx/v1/notification/1?api_key={{md5('{{apikey}}')}} HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "    matchers:\n" +
                "      - type: dsl\n" +
                "        dsl:\n" +
                "          - 'status_code == 401'\n" +
                "          - 'contains(content_type, \"application/json\")'\n" +
                "          - 'contains(body, \"There is no notification created with this id\")'\n" +
                "        condition: and\n" +
                "    extractors:\n" +
                "      - type: regex\n" +
                "        name: apikey\n" +
                "        group: 1\n" +
                "        regex:\n" +
                "          - '\"home\":\"(.*?)\",'\n" +
                "        internal: true\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "md5('{{apikey}}') 应被求值后进入请求 URL");
    }

    @Test
    @DisplayName("flow: http(1) && http(2) 应按 Nuclei 1-based 协议索引执行")
    void nucleiFlowShouldUseOneBasedProtocolIndexes() throws Exception {
        String yaml =
                "id: runtime-flow-index\n" +
                "info:\n" +
                "  name: runtime-flow-index\n" +
                "  severity: info\n" +
                "flow: http(1) && http(2)\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/flow/one'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body\n" +
                "        words:\n" +
                "          - flow-one-ok\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/flow/two'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body\n" +
                "        words:\n" +
                "          - flow-two-ok\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "http(1)/http(2) 应能命中转换后的 http_1/http_2 步骤");
    }

    @Test
    @DisplayName("同一 Nuclei HTTP 块中的多个 path 应按候选路径执行")
    void nucleiMultiplePathsShouldNotBecomeAndSteps() throws Exception {
        String yaml =
                "id: runtime-multi-path\n" +
                "info:\n" +
                "  name: runtime-multi-path\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/paths/fail'\n" +
                "      - '{{BaseURL}}/paths/pass'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body\n" +
                "        words:\n" +
                "          - path-pass\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "Nuclei 同一 request 的 path 列表应任一候选命中即可");
    }

    @Test
    @DisplayName("stop-at-first-match 应在候选 path 首次命中后停止")
    void nucleiStopAtFirstMatchShouldStopPathCandidatesAfterMatch() throws Exception {
        stopAtFirstCounter.set(0);

        String yaml =
                "id: runtime-stop-at-first-path\n" +
                "info:\n" +
                "  name: runtime-stop-at-first-path\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    stop-at-first-match: true\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/paths/stop-pass'\n" +
                "      - '{{BaseURL}}/paths/stop-next'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body\n" +
                "        words:\n" +
                "          - stop\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "首个候选 path 命中后应判定成功");
        assertEquals(1, stopAtFirstCounter.get(), "stop-at-first-match=true 时不应继续请求后续候选 path");
    }

    @Test
    @DisplayName("iterate-all 应遍历 internal extractor 的全部值用于候选 path")
    void nucleiIterateAllShouldExpandExtractorValuesForPaths() throws Exception {
        String yaml =
                "id: runtime-iterate-all-path\n" +
                "info:\n" +
                "  name: runtime-iterate-all-path\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    iterate-all: true\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/iterate/index'\n" +
                "      - '{{BaseURL}}/iterate/{{item}}'\n" +
                "    extractors:\n" +
                "      - type: json\n" +
                "        name: item\n" +
                "        json:\n" +
                "          - '.[]'\n" +
                "        internal: true\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body\n" +
                "        words:\n" +
                "          - iterate-hit\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "iterate-all 应使用第二个提取值继续请求并命中");
    }

    @Test
    @DisplayName("iterate-all 应遍历 internal extractor 的全部值用于后续 raw")
    void nucleiIterateAllShouldExpandExtractorValuesForRawRequests() throws Exception {
        String yaml =
                "id: runtime-iterate-all-raw\n" +
                "info:\n" +
                "  name: runtime-iterate-all-raw\n" +
                "  severity: info\n" +
                "http:\n" +
                "  - iterate-all: true\n" +
                "    raw:\n" +
                "      - |\n" +
                "        GET /iterate/raw-index HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "      - |\n" +
                "        GET /iterate/raw/{{item}} HTTP/1.1\n" +
                "        Host: {{Hostname}}\n" +
                "    extractors:\n" +
                "      - type: regex\n" +
                "        name: item\n" +
                "        regex:\n" +
                "          - 'item=(\\w+)'\n" +
                "        internal: true\n" +
                "        part: body_1\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        part: body_2\n" +
                "        words:\n" +
                "          - raw-hit\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "iterate-all raw 链应遍历提取值并命中后续请求");
    }

    @Test
    @DisplayName("duration 应按秒语义进入 DSL 上下文")
    void dslDurationShouldUseSeconds() {
        Request request = new Request.Builder().url("http://example.test/").get().build();
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("text/plain"), "ok"))
                .build();
        CustomHttpResponse customHttpResponse = new CustomHttpResponse(response);
        customHttpResponse.setResponseTime(120L);

        Map<String, Object> context = DslContextBuilder.createDslContext(customHttpResponse, customHttpResponse);

        assertEquals(120L, context.get("duration_ms"));
        assertEquals(0.12d, (Double) context.get("duration"), 0.0001d);
    }

    @Test
    @DisplayName("HttpHandler helper 表达式不应残留模板占位符")
    void httpHandlerShouldResolveHelperExpressions() throws Exception {
        Map<String, String> variables = new HashMap<>();
        variables.put("apikey", "abc");
        variables.put("password", "p@ss word");
        variables.put("randstr", "AbC123");

        String result = HttpHandler.replaceVariables(
                "{{md5('{{apikey}}')}}|{{url_encode(password)}}|{{to_lower(\"{{randstr}}\")}}",
                variables);

        assertFalse(result.contains("{{"));
        assertEquals(md5("abc") + "|p%40ss+word|abc123", result);
    }

    @Test
    @DisplayName("code_response 应传递给后续 HTTP 请求")
    void codeResponseShouldDriveFollowingHttpRequest() throws Exception {
        String yaml =
                "id: runtime-code-response-chain\n" +
                "info:\n" +
                "  name: runtime-code-response-chain\n" +
                "  severity: info\n" +
                "flow: code() && http()\n" +
                "code:\n" +
                "  - engine:\n" +
                "      - javascript\n" +
                "    code: |\n" +
                "      'output-from-code'\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/code/{{code_response}}'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        words:\n" +
                "          - code-response-ok\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "code_response 应作为后续 HTTP 请求变量使用");
    }

    @Test
    @DisplayName("javascript_response 应传递给后续 HTTP 请求")
    void javascriptResponseShouldDriveFollowingHttpRequest() throws Exception {
        String yaml =
                "id: runtime-javascript-response-chain\n" +
                "info:\n" +
                "  name: runtime-javascript-response-chain\n" +
                "  severity: info\n" +
                "flow: javascript() && http()\n" +
                "javascript:\n" +
                "  - code: |\n" +
                "      'output-from-js'\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - '{{BaseURL}}/javascript/{{javascript_response}}'\n" +
                "    matchers:\n" +
                "      - type: word\n" +
                "        words:\n" +
                "          - javascript-response-ok\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "javascript_response 应作为后续 HTTP 请求变量使用");
    }

    @Test
    @DisplayName("Code DSL matcher 应能引用前序 code_N_response")
    void codeDslMatcherShouldSeePreviousCodeResponseVariables() throws Exception {
        String yaml =
                "id: runtime-code-indexed-response-dsl\n" +
                "info:\n" +
                "  name: runtime-code-indexed-response-dsl\n" +
                "  severity: info\n" +
                "flow: code(1) && code(2)\n" +
                "code:\n" +
                "  - engine:\n" +
                "      - javascript\n" +
                "    code: |\n" +
                "      'first-code-output'\n" +
                "  - engine:\n" +
                "      - javascript\n" +
                "    code: |\n" +
                "      'second-code-output'\n" +
                "    matchers:\n" +
                "      - type: dsl\n" +
                "        dsl:\n" +
                "          - 'contains(code_1_response, \"first-code-output\")'\n" +
                "          - 'contains(code_2_response, \"second-code-output\")'\n" +
                "        condition: and\n";

        ScanResult result = executeYaml(yaml);
        assertTrue(result.isVulnerable(), "Code DSL matcher 应可读取 code_1_response/code_2_response");
    }

    private static ScanResult executeYaml(String yaml) throws Exception {
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlFromContent(yaml);
        assertNotNull(nucleiPoc, "测试 YAML 应可解析");
        PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
        assertNotNull(poc, "测试 YAML 应可转换");

        ScanConfig config = new ScanConfig();
        config.setTimeout(2);
        config.setRetries(0);
        return new PocExecutor(config).execute(baseUrl, poc);
    }

    private static void assertBodyContains(HttpExchange exchange, String expected) throws IOException {
        String body = readBody(exchange);
        if (!body.contains(expected)) {
            write(exchange, 400, "expected body token missing: " + expected + " body=" + body, "text/plain");
            throw new IOException("expected body token missing: " + expected);
        }
    }

    private static void assertQueryContains(HttpExchange exchange, String expected) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || !query.contains(expected)) {
            write(exchange, 400, "expected query token missing: " + expected + " query=" + query, "text/plain");
            throw new IOException("expected query token missing: " + expected);
        }
    }

    private static void assertHeaderContains(HttpExchange exchange, String header, String expected) throws IOException {
        String value = exchange.getRequestHeaders().getFirst(header);
        if (value == null || !value.contains(expected)) {
            write(exchange, 400, "expected header token missing: " + expected + " header=" + value, "text/plain");
            throw new IOException("expected header token missing: " + expected);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return URLDecoder.decode(readBodyRaw(exchange), "UTF-8");
    }

    private static String readBodyRaw(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), "UTF-8");
    }

    private static void writeJson(HttpExchange exchange, int code, String body) throws IOException {
        write(exchange, code, body, "application/json");
    }

    private static void write(HttpExchange exchange, int code, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
