package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WebGoat 登录态 POC 执行闭环测试")
class WebGoatAuthenticatedPocExecutionIntegrationTest {

    private static final String DEFAULT_ORIGIN = "http://127.0.0.1:8081";
    private static final Pattern SESSION_PATTERN = Pattern.compile("JSESSIONID=([^;]+)");

    @Test
    @DisplayName("路径类 访问控制类 注入类 POC 应在登录态下命中且匿名访问被门禁拦截")
    void shouldExecuteAuthenticatedPocsAgainstRunningWebGoat() throws Exception {
        String origin = resolveOrigin();
        Assumptions.assumeTrue(waitForReady(origin + "/WebGoat/", 30),
                "本机 WebGoat 未就绪，跳过登录态执行测试: " + origin);

        String username = "pt" + System.currentTimeMillis();
        String password = "Potato123!";
        String sessionCookie = registerUser(origin, username, password);
        assertTrue(hasText(sessionCookie), "注册后应拿到 JSESSIONID");

        ScanResult pathAnonymous = execute(buildPathTraversalLikePoc(null), origin);
        ScanResult pathAuthenticated = execute(buildPathTraversalLikePoc(sessionCookie), origin);
        ScanResult accessAnonymous = execute(buildAccessControlPoc(null), origin);
        ScanResult accessAuthenticated = execute(buildAccessControlPoc(sessionCookie), origin);
        ScanResult injectionAnonymous = execute(buildInjectionPoc(null), origin);
        ScanResult injectionAuthenticated = execute(buildInjectionPoc(sessionCookie), origin);

        assertProtectedByLogin(pathAnonymous);
        assertAuthenticatedHit(pathAuthenticated, "What is WebGoat?", sessionCookie);

        assertProtectedByLogin(accessAnonymous);
        assertAuthenticatedHit(accessAuthenticated, "\"lessonTitle\"", sessionCookie);

        assertProtectedByLogin(injectionAnonymous);
        assertAuthenticatedHit(injectionAuthenticated, "DOMCrossSiteScripting", sessionCookie);
        assertTrue(injectionAuthenticated.getRawRequest().contains("webgoat-requested-by: dom-xss-vuln"));
        assertTrue(injectionAuthenticated.getRawRequest().contains("param1=42&param2=24"));

        Path evidenceDir = Paths.get("target", "webgoat-authenticated-poc-evidence");
        Files.createDirectories(evidenceDir);
        writeEvidence(evidenceDir, "path", pathAnonymous, pathAuthenticated);
        writeEvidence(evidenceDir, "access-control", accessAnonymous, accessAuthenticated);
        writeEvidence(evidenceDir, "injection", injectionAnonymous, injectionAuthenticated);
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(origin, username, pathAnonymous, pathAuthenticated,
                        accessAnonymous, accessAuthenticated,
                        injectionAnonymous, injectionAuthenticated).getBytes(StandardCharsets.UTF_8));
    }

    private PocObj.Poc buildPathTraversalLikePoc(String sessionCookie) {
        PocObj.Poc poc = basePoc("webgoat-auth-path", "WebGoat 路径受保护页面");
        poc.getVerifySteps().add(step("http_1", "GET",
                "/WebGoat/WebGoatIntroduction.lesson", null, null,
                Arrays.asList(statusMatcher(200), wordMatcher("What is WebGoat?"))));
        applyCookieAuth(poc, sessionCookie);
        return poc;
    }

    private PocObj.Poc buildAccessControlPoc(String sessionCookie) {
        PocObj.Poc poc = basePoc("webgoat-auth-access", "WebGoat 访问控制接口");
        poc.getVerifySteps().add(step("http_1", "GET",
                "/WebGoat/service/lessoninfo.mvc/WebGoatIntroduction.lesson", null, null,
                Arrays.asList(statusMatcher(200), wordMatcher("\"lessonTitle\""))));
        applyCookieAuth(poc, sessionCookie);
        return poc;
    }

    private PocObj.Poc buildInjectionPoc(String sessionCookie) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("webgoat-requested-by", "dom-xss-vuln");

        PocObj.Poc poc = basePoc("webgoat-auth-injection", "WebGoat 注入课程接口");
        poc.getVerifySteps().add(step("http_1", "POST",
                "/WebGoat/CrossSiteScripting/phone-home-xss",
                headers,
                "param1=42&param2=24",
                Arrays.asList(statusMatcher(200), wordMatcher("DOMCrossSiteScripting"))));
        applyCookieAuth(poc, sessionCookie);
        return poc;
    }

    private PocObj.Poc basePoc(String id, String name) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(name);
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);
        return poc;
    }

    private void applyCookieAuth(PocObj.Poc poc, String sessionCookie) {
        if (!hasText(sessionCookie)) {
            return;
        }
        Map<String, String> authConfig = new HashMap<String, String>();
        authConfig.put("type", "cookie");
        authConfig.put("cookies", "{\"JSESSIONID\":\"" + sessionCookie + "\"}");
        poc.getGlobalConfig().setAuthConfig(authConfig);
    }

    private PocObj.PocStep step(String stepId, String method, String path,
                                Map<String, String> headers, String body,
                                List<PocObj.Matcher> matchers) {
        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId(stepId);
        step.setMethod(method);
        step.setPath(path);
        step.setMatchers(matchers);
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        if (headers != null && !headers.isEmpty()) {
            step.setHeaders(headers);
        }
        if (body != null) {
            step.setBody(body);
        }
        return step;
    }

    private PocObj.Matcher statusMatcher(int statusCode) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.STATUS);
        matcher.setValues(Collections.singletonList(String.valueOf(statusCode)));
        return matcher;
    }

    private PocObj.Matcher wordMatcher(String word) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(word));
        return matcher;
    }

    private ScanResult execute(PocObj.Poc poc, String origin) throws Exception {
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(10);
        config.setRetries(0);
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setDebug(false);
        return new PocExecutor(config).execute(origin, poc);
    }

    private void assertProtectedByLogin(ScanResult result) {
        assertFalse(result.isVulnerable(), "匿名访问应被登录门禁拦截");
        StepExecutionRecord record = firstRecord(result);
        assertEquals(302, record.getResponseCode(), "匿名访问应返回 302");
        assertTrue(result.getRawResponseSnippet().contains("/WebGoat/login"),
                "匿名访问应跳转到登录页");
    }

    private void assertAuthenticatedHit(ScanResult result, String marker, String sessionCookie) {
        assertTrue(result.isVulnerable(), "携带登录态后应命中受保护端点");
        StepExecutionRecord record = firstRecord(result);
        assertEquals(200, record.getResponseCode(), "已登录访问应返回 200");
        assertTrue(result.getRawRequest().contains("Cookie: JSESSIONID=" + sessionCookie),
                "扫描请求应携带登录态 Cookie");
        assertTrue(result.getRawResponseSnippet().contains(marker),
                "已登录响应应包含业务标记: " + marker);
    }

    private StepExecutionRecord firstRecord(ScanResult result) {
        assertTrue(result.getStepRecords() != null && !result.getStepRecords().isEmpty(),
                "应记录至少一个步骤执行详情");
        return result.getStepRecords().get(0);
    }

    private void writeEvidence(Path evidenceDir, String prefix,
                               ScanResult anonymousResult,
                               ScanResult authenticatedResult) throws IOException {
        Files.write(evidenceDir.resolve(prefix + "-anonymous-response.txt"),
                safeBytes(anonymousResult.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve(prefix + "-authenticated-request.txt"),
                safeBytes(authenticatedResult.getRawRequest()));
        Files.write(evidenceDir.resolve(prefix + "-authenticated-response.txt"),
                safeBytes(authenticatedResult.getRawResponseSnippet()));
    }

    private String buildSummary(String origin, String username,
                                ScanResult pathAnonymous, ScanResult pathAuthenticated,
                                ScanResult accessAnonymous, ScanResult accessAuthenticated,
                                ScanResult injectionAnonymous, ScanResult injectionAuthenticated) {
        return "case\tanonymous_vulnerable\tanonymous_status\tauthenticated_vulnerable\tauthenticated_status\tmarker\n"
                + "instance\t-\t-\t-\t-\t" + origin + "/WebGoat/ user=" + username + "\n"
                + summaryLine("path", pathAnonymous, pathAuthenticated, "What is WebGoat?")
                + summaryLine("access-control", accessAnonymous, accessAuthenticated, "\"lessonTitle\"")
                + summaryLine("injection", injectionAnonymous, injectionAuthenticated, "DOMCrossSiteScripting")
                + "auth_cookie\t-\t-\tpresent\t-\tJSESSIONID\n";
    }

    private String summaryLine(String name, ScanResult anonymousResult,
                               ScanResult authenticatedResult, String marker) {
        return name + "\t"
                + anonymousResult.isVulnerable() + "\t"
                + firstRecord(anonymousResult).getResponseCode() + "\t"
                + authenticatedResult.isVulnerable() + "\t"
                + firstRecord(authenticatedResult).getResponseCode() + "\t"
                + marker + "\n";
    }

    private String registerUser(String origin, String username, String password) throws Exception {
        String body = "username=" + urlEncode(username)
                + "&password=" + urlEncode(password)
                + "&matchingPassword=" + urlEncode(password)
                + "&agree=agree";
        HttpResult result = request("POST", origin + "/WebGoat/register.mvc", null,
                "application/x-www-form-urlencoded", body);
        assertEquals(302, result.code, "注册应返回 302");
        assertTrue(result.location.contains("/WebGoat/attack?username=" + username),
                "注册成功后应跳到 attack");
        Matcher matcher = SESSION_PATTERN.matcher(result.setCookie);
        assertTrue(matcher.find(), "注册响应应返回 JSESSIONID");
        return matcher.group(1);
    }

    private boolean waitForReady(String url, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResult result = request("GET", url, null, null, null);
                if (result.code == 302 && hasText(result.location) && result.location.contains("/WebGoat/login")) {
                    return true;
                }
                if (result.code == 200 && result.body.contains("/WebGoat/login")) {
                    return true;
                }
            } catch (IOException ignored) {
            }
            Thread.sleep(1000L);
        }
        return false;
    }

    private HttpResult request(String method, String targetUrl, String cookie,
                               String contentType, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod(method);
        if (hasText(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        }
        if (hasText(contentType)) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        if (body != null) {
            connection.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        String setCookie = connection.getHeaderField("Set-Cookie");
        String responseBody = "";
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream != null && !"HEAD".equalsIgnoreCase(method)) {
            responseBody = readStream(stream);
        }
        connection.disconnect();
        return new HttpResult(code, location, setCookie, responseBody);
    }

    private String resolveOrigin() {
        String value = System.getProperty("potatotool.webgoat.origin");
        if (!hasText(value)) {
            value = DEFAULT_ORIGIN;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private byte[] safeBytes(String text) {
        return (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static final class HttpResult {
        private final int code;
        private final String location;
        private final String setCookie;
        private final String body;

        private HttpResult(int code, String location, String setCookie, String body) {
            this.code = code;
            this.location = location;
            this.setCookie = setCookie == null ? "" : setCookie;
            this.body = body == null ? "" : body;
        }
    }
}
