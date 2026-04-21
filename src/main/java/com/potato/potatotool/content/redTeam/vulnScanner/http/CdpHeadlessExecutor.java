package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.browser.BrowserRuntimeConfig;
import com.potato.potatotool.utils.browser.BrowserRuntimeResolver;
import com.potato.potatotool.utils.browser.CdpBrowserSession;
import com.potato.potatotool.utils.browser.CdpException;
import com.potato.potatotool.utils.browser.CdpPage;
import com.potato.potatotool.utils.browser.CdpPageSnapshot;
import com.potato.potatotool.utils.browser.CdpTargetSelector;
import com.potato.potatotool.utils.browser.ManagedChromeSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared CDP-backed Headless executor.
 */
final class CdpHeadlessExecutor {
    static final String SUPPORT_POLICY = "浏览器可启动且 DevTools 可连接，无需额外驱动";
    static final String SUPPORT_POLICY_UPDATED_AT = "2026-04-20";

    private static final Gson GSON = new Gson();
    private static final long WAIT_POLL_INTERVAL_MILLIS = 200L;
    private static final long MANAGED_BROWSER_READY_WAIT_MILLIS = 15000L;
    private static final String VISIBILITY_CHECK_JS =
            "var style=window.getComputedStyle(el);"
                    + "var rect=el.getBoundingClientRect();"
                    + "var opacity=style&&style.opacity?parseFloat(style.opacity):1;"
                    + "var visible=!!(rect.width>0&&rect.height>0&&style&&style.display!=='none'&&style.visibility!=='hidden'&&style.visibility!=='collapse'&&opacity>0);";

    HeadlessHandler.HeadlessCompatibilityResult checkCompatibility() {
        return checkCompatibility(BrowserRuntimeConfig.resolveBrowserPath(), false);
    }

    HeadlessHandler.HeadlessCompatibilityResult checkCompatibility(String configuredBrowserPath) {
        return checkCompatibility(configuredBrowserPath, true);
    }

    private HeadlessHandler.HeadlessCompatibilityResult checkCompatibility(String configuredBrowserPath,
                                                                           boolean strictConfiguredPath) {
        HeadlessHandler.HeadlessCompatibilityResult result = new HeadlessHandler.HeadlessCompatibilityResult();
        result.setEngine("cdp");
        result.setRuntimeDependency("Chrome/Chromium 浏览器（内置 DevTools 连接）");
        result.setSupportPolicySummary(getSupportPolicySummary());

        String browserPath = strictConfiguredPath
                ? BrowserRuntimeResolver.resolveConfiguredBrowserPath(configuredBrowserPath)
                : BrowserRuntimeResolver.resolveBrowserPath(configuredBrowserPath);
        result.setBrowserPath(browserPath);
        if (browserPath == null) {
            result.setStatus(HeadlessHandler.CompatibilityStatus.NOT_FOUND);
            result.setMessage("未检测到 Chrome/Chromium 浏览器，请在设置页配置浏览器路径。");
            return result;
        }

        String browserVersion = BrowserRuntimeResolver.readCommandVersion(browserPath, "--version");
        Integer browserMajor = BrowserRuntimeResolver.parseMajorVersion(browserVersion);
        result.setBrowserVersion(browserVersion);
        result.setBrowserMajor(browserMajor);
        if (browserMajor == null) {
            result.setStatus(HeadlessHandler.CompatibilityStatus.NOT_FOUND);
            result.setMessage("浏览器版本解析失败，请检查浏览器路径是否正确。\n当前路径: " + browserPath);
            return result;
        }

        result.setStatus(HeadlessHandler.CompatibilityStatus.COMPATIBLE);
        result.setMessage("浏览器运行时可用，可执行 Headless 扫描。");
        return result;
    }

    HeadlessHandler.HeadlessResponse execute(String url,
                                             List<HeadlessHandler.BrowserStep> steps,
                                             int timeoutSeconds) {
        HeadlessHandler.HeadlessResponse response = new HeadlessHandler.HeadlessResponse();
        response.setUrl(url);

        long startTime = System.currentTimeMillis();
        int timeoutMillis = Math.max(1000, timeoutSeconds * 1000);

        try {
            HeadlessHandler.HeadlessCompatibilityResult compatibilityResult = checkCompatibility();
            if (!compatibilityResult.isCompatible()) {
                String message = HeadlessHandler.buildCompatibilityErrorMessage(compatibilityResult);
                response.setSuccess(false);
                response.setError(message);
                response.addLog("✗ " + message);
                return response;
            }

            response.addLog("初始化无头浏览器...");

            try (ManagedChromeSession chromeSession = ManagedChromeSession.start(
                    compatibilityResult.getBrowserPath(),
                    null,
                    false,
                    MANAGED_BROWSER_READY_WAIT_MILLIS);
                 CdpBrowserSession browserSession = CdpBrowserSession.attach(
                         chromeSession.getDevToolsPort(),
                         CdpTargetSelector.firstPage())) {
                response.addLog("✓ 浏览器初始化成功");

                CdpPage page = browserSession.getPage();
                if (shouldPerformInitialNavigation(url, steps)) {
                    response.addLog("导航到: " + url);
                    navigateAndWait(page, url, timeoutMillis);
                    response.addLog("✓ 页面加载完成");
                }

                if (steps != null && !steps.isEmpty()) {
                    executeSteps(page, steps, response, timeoutMillis);
                }

                response.setPageTitle(page.readTitle(timeoutMillis));
                CdpPageSnapshot snapshot = page.snapshot(timeoutMillis);
                response.setPageSource(snapshot.getHtml());
                if (snapshot.getUrl() != null && !snapshot.getUrl().isEmpty()) {
                    response.setUrl(snapshot.getUrl());
                }
                response.addLog("✓ 页面标题: " + response.getPageTitle());
                response.setSuccess(true);
            }
        } catch (CdpException e) {
            response.setSuccess(false);
            response.setError(buildCdpErrorMessage(e));
            response.addLog("✗ 异常: " + response.getError());
        } catch (IOException e) {
            response.setSuccess(false);
            response.setError("浏览器会话创建失败，可能需要安装 Chrome 浏览器: " + e.getMessage());
            response.addLog("✗ 错误: " + response.getError());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Headless 操作异常: " + e.getMessage());
            response.addLog("✗ 异常: " + e.getMessage());
        } finally {
            response.addLog("✓ 浏览器已关闭");
            response.setDuration(System.currentTimeMillis() - startTime);
        }

        return response;
    }

    private void executeSteps(CdpPage page,
                              List<HeadlessHandler.BrowserStep> steps,
                              HeadlessHandler.HeadlessResponse response,
                              int timeoutMillis) throws Exception {
        for (int i = 0; i < steps.size(); i++) {
            HeadlessHandler.BrowserStep step = steps.get(i);
            String action = step.getAction();
            Map<String, String> args = step.getArgs();
            String actionName = action == null ? "" : action.toLowerCase();

            try {
                response.addLog("执行步骤 " + (i + 1) + ": " + action);

                if ("navigate".equals(actionName)) {
                    String targetUrl = readArg(args, "url");
                    navigateAndWait(page, targetUrl, timeoutMillis);
                    response.addLog("  ✓ 导航到: " + targetUrl);
                } else if ("waitload".equals(actionName)) {
                    page.waitForDocumentReadyState("complete", timeoutMillis, WAIT_POLL_INTERVAL_MILLIS);
                    response.addLog("  ✓ 页面加载完成");
                } else if ("script".equals(actionName)) {
                    String code = readArg(args, "code");
                    JsonElement result = page.evaluateValue(normalizeScriptExpression(code), timeoutMillis);
                    response.getScriptResults().put("script_" + i, toJavaValue(result));
                    response.addLog("  ✓ 脚本执行完成，结果: " + safeDisplayValue(result));
                } else if ("click".equals(actionName)) {
                    String selector = readArg(args, "selector");
                    executeSelectorAction(page, selector, null, timeoutMillis, SelectorAction.CLICK);
                    response.addLog("  ✓ 点击元素: " + selector);
                } else if ("input".equals(actionName)) {
                    String selector = readArg(args, "selector");
                    String value = readArg(args, "value");
                    executeSelectorAction(page, selector, value, timeoutMillis, SelectorAction.INPUT);
                    response.addLog("  ✓ 输入文本到: " + selector);
                } else if ("screenshot".equals(actionName)) {
                    response.setScreenshot(page.captureScreenshotBase64(timeoutMillis));
                    response.addLog("  ✓ 截图完成");
                } else if ("sleep".equals(actionName)) {
                    int sleepMs = Integer.parseInt(args != null ? args.getOrDefault("duration", "1000") : "1000");
                    Thread.sleep(sleepMs);
                    response.addLog("  ✓ 等待 " + sleepMs + "ms");
                } else if ("waitvisible".equals(actionName)) {
                    String selector = readArg(args, "selector");
                    waitForVisible(page, selector, timeoutMillis);
                    response.addLog("  ✓ 元素可见: " + selector);
                } else {
                    response.addLog("  ⚠ 未知操作: " + action);
                }
            } catch (Exception e) {
                response.addLog("  ✗ 操作失败: " + e.getMessage());
                throw new RuntimeException("步骤执行失败: " + action, e);
            }
        }
    }

    private void navigateAndWait(CdpPage page, String url, int timeoutMillis) throws CdpException {
        page.navigate(url, timeoutMillis);
        page.waitForDocumentReadyState("complete", timeoutMillis, WAIT_POLL_INTERVAL_MILLIS);
    }

    static boolean shouldPerformInitialNavigation(String url, List<HeadlessHandler.BrowserStep> steps) {
        return url != null && !url.isEmpty() && !hasLeadingNavigateStep(steps);
    }

    static boolean hasLeadingNavigateStep(List<HeadlessHandler.BrowserStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        for (HeadlessHandler.BrowserStep step : steps) {
            if (step == null) {
                continue;
            }

            String actionName = normalizeActionName(step.getAction());
            if (actionName.isEmpty()) {
                continue;
            }
            return "navigate".equals(actionName);
        }
        return false;
    }

    private static String normalizeActionName(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ENGLISH);
    }

    private void waitForVisible(final CdpPage page,
                                final String selector,
                                long timeoutMillis) throws CdpException {
        boolean matched = page.waitUntil(new CdpPage.WaitCondition() {
            @Override
            public boolean test(CdpPage currentPage) throws CdpException {
                JsonObject state = readSelectorState(currentPage, selector, 5000);
                return state != null && readJsonBoolean(state, "visible");
            }
        }, timeoutMillis, WAIT_POLL_INTERVAL_MILLIS);

        if (!matched) {
            throw CdpException.timeout("Timed out waiting for visible element: " + selector);
        }
    }

    private void executeSelectorAction(CdpPage page,
                                       String selector,
                                       String value,
                                       int timeoutMillis,
                                       SelectorAction action) throws CdpException {
        JsonObject result = readSelectorActionResult(page, selector, value, timeoutMillis, action);
        if (result == null || !readJsonBoolean(result, "ok")) {
            throw new RuntimeException(readJsonString(result, "error"));
        }
    }

    private JsonObject readSelectorActionResult(CdpPage page,
                                                String selector,
                                                String value,
                                                int timeoutMillis,
                                                SelectorAction action) throws CdpException {
        String expression = action == SelectorAction.CLICK
                ? buildClickExpression(selector)
                : buildInputExpression(selector, value);
        JsonElement result = page.evaluateValue(expression, timeoutMillis);
        return result != null && result.isJsonObject() ? result.getAsJsonObject() : null;
    }

    private JsonObject readSelectorState(CdpPage page, String selector, int timeoutMillis) throws CdpException {
        JsonElement result = page.evaluateValue(buildVisibilityExpression(selector), timeoutMillis);
        return result != null && result.isJsonObject() ? result.getAsJsonObject() : null;
    }

    static String normalizeScriptExpression(String script) {
        if (script == null || script.trim().isEmpty()) {
            return "undefined";
        }
        String trimmed = script.trim();
        if (trimmed.startsWith("return ")
                || trimmed.contains(";")
                || trimmed.contains("\n")
                || trimmed.startsWith("var ")
                || trimmed.startsWith("let ")
                || trimmed.startsWith("const ")
                || trimmed.startsWith("if ")
                || trimmed.startsWith("for ")
                || trimmed.startsWith("while ")
                || trimmed.startsWith("function ")
                || trimmed.startsWith("{")) {
            return "(function(){\n" + trimmed + "\n})()";
        }
        return trimmed;
    }

    static Object toJavaValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
            if (element.getAsJsonPrimitive().isNumber()) {
                BigDecimal decimal = element.getAsBigDecimal();
                if (decimal.scale() <= 0) {
                    try {
                        return decimal.longValueExact();
                    } catch (ArithmeticException ignored) {
                    }
                }
                return decimal.doubleValue();
            }
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> values = new ArrayList<Object>();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                values.add(toJavaValue(item));
            }
            return values;
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            map.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return map;
    }

    String getSupportPolicySummary() {
        return SUPPORT_POLICY + "（更新于 " + SUPPORT_POLICY_UPDATED_AT + "）";
    }

    private String buildCdpErrorMessage(CdpException e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "未知错误";
        }
        if (message.toLowerCase().contains("timeout")) {
            return "操作超时: " + message;
        }
        return "Headless 操作异常: " + message;
    }

    private String readArg(Map<String, String> args, String key) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException("缺少参数: " + key);
        }
        return args.get(key);
    }

    private String safeDisplayValue(JsonElement result) {
        if (result == null || result.isJsonNull()) {
            return "null";
        }
        if (result.isJsonPrimitive()) {
            return result.getAsString();
        }
        return result.toString();
    }

    private String buildVisibilityExpression(String selector) {
        String selectorLiteral = GSON.toJson(selector);
        return "(function(){"
                + "var selector=" + selectorLiteral + ";"
                + "var el=document.querySelector(selector);"
                + "if(!el){return {exists:false,visible:false,error:'元素未找到: ' + selector};}"
                + VISIBILITY_CHECK_JS
                + "return {exists:true,visible:visible};"
                + "})()";
    }

    private String buildClickExpression(String selector) {
        String selectorLiteral = GSON.toJson(selector);
        return "(function(){"
                + "var selector=" + selectorLiteral + ";"
                + "var el=document.querySelector(selector);"
                + "if(!el){return {ok:false,error:'元素未找到: ' + selector};}"
                + VISIBILITY_CHECK_JS
                + "if(!visible){return {ok:false,error:'元素不可见: ' + selector};}"
                + "if(el.scrollIntoView){el.scrollIntoView({block:'center',inline:'center'});}"
                + "rect=el.getBoundingClientRect();"
                + "var x=rect.left+rect.width/2;"
                + "var y=rect.top+rect.height/2;"
                + "var topElement=document.elementFromPoint(x,y);"
                + "if(topElement&&topElement!==el&&!el.contains(topElement)){return {ok:false,error:'元素被遮挡: ' + selector};}"
                + "el.click();"
                + "return {ok:true};"
                + "})()";
    }

    private String buildInputExpression(String selector, String value) {
        String selectorLiteral = GSON.toJson(selector);
        String valueLiteral = GSON.toJson(value == null ? "" : value);
        return "(function(){"
                + "var selector=" + selectorLiteral + ";"
                + "var value=" + valueLiteral + ";"
                + "var el=document.querySelector(selector);"
                + "if(!el){return {ok:false,error:'元素未找到: ' + selector};}"
                + VISIBILITY_CHECK_JS
                + "if(!visible){return {ok:false,error:'元素不可见: ' + selector};}"
                + "var tag=(el.tagName||'').toLowerCase();"
                + "if(!(tag==='input'||tag==='textarea'||el.isContentEditable)){return {ok:false,error:'元素不可输入: ' + selector};}"
                + "if(el.focus){el.focus();}"
                + "if(el.isContentEditable){el.textContent='';el.textContent=value;}"
                + "else{el.value='';el.value=value;}"
                + "el.dispatchEvent(new Event('input',{bubbles:true}));"
                + "el.dispatchEvent(new Event('change',{bubbles:true}));"
                + "return {ok:true};"
                + "})()";
    }

    private boolean readJsonBoolean(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return false;
        }
        try {
            return jsonObject.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String readJsonString(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private enum SelectorAction {
        CLICK,
        INPUT
    }
}
