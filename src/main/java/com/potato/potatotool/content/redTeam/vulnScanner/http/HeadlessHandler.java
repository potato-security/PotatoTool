package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless 浏览器处理器
 * 用于执行无头浏览器操作并检测动态页面
 * 支持 Nuclei Headless 协议的所有功能
 *
 * @author Potato
 * @date 2025-11-02
 */
public class HeadlessHandler {
    private static final String ENGINE_VALUE = "cdp";
    private static final String SETTINGS_ENTRY = "设置 -> 基础配置 -> 浏览器运行时";
    private static final CdpHeadlessExecutor CDP_EXECUTOR = new CdpHeadlessExecutor();

    public enum CompatibilityStatus {
        COMPATIBLE,
        INCOMPATIBLE,
        NOT_FOUND
    }

    public static class HeadlessCompatibilityResult {
        private CompatibilityStatus status;
        private String engine;
        private String runtimeDependency;
        private String supportPolicySummary;
        private String browserPath;
        private String browserVersion;
        private Integer browserMajor;
        private String message;

        public CompatibilityStatus getStatus() {
            return status;
        }

        public void setStatus(CompatibilityStatus status) {
            this.status = status;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public String getRuntimeDependency() {
            return runtimeDependency;
        }

        public void setRuntimeDependency(String runtimeDependency) {
            this.runtimeDependency = runtimeDependency;
        }

        public String getSupportPolicySummary() {
            return supportPolicySummary;
        }

        public void setSupportPolicySummary(String supportPolicySummary) {
            this.supportPolicySummary = supportPolicySummary;
        }

        public String getBrowserPath() {
            return browserPath;
        }

        public void setBrowserPath(String browserPath) {
            this.browserPath = browserPath;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
        }

        public Integer getBrowserMajor() {
            return browserMajor;
        }

        public void setBrowserMajor(Integer browserMajor) {
            this.browserMajor = browserMajor;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isCompatible() {
            return status == CompatibilityStatus.COMPATIBLE;
        }

        public String buildDisplayMessage() {
            String engineInfo = hasText(engine) ? engine : ENGINE_VALUE;
            String dependencyInfo = hasText(runtimeDependency)
                    ? runtimeDependency
                    : defaultRuntimeDependency();
            String policyInfo = hasText(supportPolicySummary)
                    ? supportPolicySummary
                    : getSupportPolicySummary();
            String browserInfo = hasText(browserVersion) ? browserVersion : "未检测到";
            StringBuilder builder = new StringBuilder();
            builder.append("[Headless 预检] 引擎=").append(engineInfo)
                    .append(" 结果=").append(status)
                    .append("\n浏览器版本: ").append(browserInfo)
                    .append("\n运行时依赖: ").append(dependencyInfo)
                    .append("\n策略: ").append(policyInfo)
                    .append("\n")
                    .append(message == null ? "" : message);
            return builder.toString();
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }

    /**
     * Headless 操作结果
     */
    public static class HeadlessResponse {
        private String url;
        private boolean success;
        private long duration;
        private String pageTitle;
        private String pageSource;
        private String screenshot;
        private Map<String, Object> scriptResults;
        private List<String> logs;
        private String error;

        public HeadlessResponse() {
            this.scriptResults = new HashMap<String, Object>();
            this.logs = new ArrayList<String>();
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public void setPageTitle(String pageTitle) {
            this.pageTitle = pageTitle;
        }

        public String getPageSource() {
            return pageSource;
        }

        public void setPageSource(String pageSource) {
            this.pageSource = pageSource;
        }

        public String getScreenshot() {
            return screenshot;
        }

        public void setScreenshot(String screenshot) {
            this.screenshot = screenshot;
        }

        public Map<String, Object> getScriptResults() {
            return scriptResults;
        }

        public void setScriptResults(Map<String, Object> scriptResults) {
            this.scriptResults = scriptResults;
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public void addLog(String log) {
            this.logs.add(log);
        }

        @Override
        public String toString() {
            return "HeadlessResponse{"
                    + "url='" + url + '\''
                    + ", success=" + success
                    + ", duration=" + duration + "ms"
                    + ", pageTitle='" + pageTitle + '\''
                    + ", scriptResults=" + scriptResults.size()
                    + ", logs=" + logs.size()
                    + '}';
        }
    }

    /**
     * 浏览器操作步骤
     */
    public static class BrowserStep {
        private String action;
        private Map<String, String> args;

        public BrowserStep(String action, Map<String, String> args) {
            this.action = action;
            this.args = args != null ? args : new HashMap<String, String>();
        }

        public String getAction() {
            return action;
        }

        public Map<String, String> getArgs() {
            return args;
        }
    }

    public static HeadlessCompatibilityResult checkCompatibility() {
        return CDP_EXECUTOR.checkCompatibility();
    }

    public static HeadlessCompatibilityResult checkCompatibility(String configuredBrowserPath) {
        return CDP_EXECUTOR.checkCompatibility(configuredBrowserPath);
    }

    public static String buildCompatibilityErrorMessage(HeadlessCompatibilityResult result) {
        if (result == null) {
            return "Headless 兼容性检测失败：未获取到检测结果。";
        }
        return result.buildDisplayMessage() + "\n设置入口：" + SETTINGS_ENTRY;
    }

    public static String getSupportPolicySummary() {
        return CDP_EXECUTOR.getSupportPolicySummary();
    }

    private static String defaultRuntimeDependency() {
        return "Chrome/Chromium 浏览器（内置 DevTools 连接）";
    }

    private static void prependEngineLog(HeadlessResponse response) {
        if (response == null) {
            return;
        }
        List<String> logs = response.getLogs() == null
                ? new ArrayList<String>()
                : new ArrayList<String>(response.getLogs());
        logs.add(0, "[Headless 引擎] 当前执行引擎: " + ENGINE_VALUE);
        response.setLogs(logs);
    }

    /**
     * 执行 Headless 浏览器操作（完整参数）
     *
     * @param url 起始 URL
     * @param steps 操作步骤列表
     * @param timeoutSeconds 超时时间（秒）
     * @return Headless 响应
     */
    public static HeadlessResponse execute(String url, List<BrowserStep> steps, int timeoutSeconds) {
        HeadlessResponse response = CDP_EXECUTOR.execute(url, steps, timeoutSeconds);
        prependEngineLog(response);
        return response;
    }

    /**
     * 简单导航并获取页面标题
     */
    public static HeadlessResponse navigateAndGetTitle(String url) {
        Map<String, String> navigateArgs = new HashMap<String, String>();
        navigateArgs.put("url", url);
        Map<String, String> waitloadArgs = new HashMap<String, String>();
        List<BrowserStep> steps = Arrays.asList(
                new BrowserStep("navigate", navigateArgs),
                new BrowserStep("waitload", waitloadArgs)
        );
        return execute(null, steps, 30);
    }

    /**
     * 执行 JavaScript 并获取结果
     */
    public static HeadlessResponse executeScript(String url, String script) {
        Map<String, String> navigateArgs = new HashMap<String, String>();
        navigateArgs.put("url", url);
        Map<String, String> waitloadArgs = new HashMap<String, String>();
        Map<String, String> scriptArgs = new HashMap<String, String>();
        scriptArgs.put("code", script);
        List<BrowserStep> steps = Arrays.asList(
                new BrowserStep("navigate", navigateArgs),
                new BrowserStep("waitload", waitloadArgs),
                new BrowserStep("script", scriptArgs)
        );
        return execute(null, steps, 30);
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== 测试 Headless 浏览器 ===\n");

        try {
            System.out.println("测试1: 导航到百度首页");
            HeadlessResponse response1 = navigateAndGetTitle("https://www.baidu.com");

            System.out.println(response1);
            if (response1.isSuccess()) {
                System.out.println("页面标题: " + response1.getPageTitle());
                System.out.println("✓ 测试1通过\n");
            } else {
                System.out.println("✗ 测试1失败: " + response1.getError() + "\n");
            }

            System.out.println("测试2: 执行 JavaScript");
            HeadlessResponse response2 = executeScript(
                    "https://www.example.com",
                    "return document.title"
            );

            System.out.println(response2);
            if (response2.isSuccess()) {
                System.out.println("脚本结果: " + response2.getScriptResults());
                System.out.println("✓ 测试2通过\n");
            } else {
                System.out.println("✗ 测试2失败: " + response2.getError() + "\n");
            }

            System.out.println("测试3: 复杂操作序列");
            Map<String, String> navArgs = new HashMap<String, String>();
            navArgs.put("url", "https://www.example.com");
            Map<String, String> waitArgs = new HashMap<String, String>();
            Map<String, String> scriptArgs = new HashMap<String, String>();
            scriptArgs.put("code", "return document.querySelector('h1').textContent");
            Map<String, String> sleepArgs = new HashMap<String, String>();
            sleepArgs.put("duration", "1000");
            List<BrowserStep> steps = Arrays.asList(
                    new BrowserStep("navigate", navArgs),
                    new BrowserStep("waitload", waitArgs),
                    new BrowserStep("script", scriptArgs),
                    new BrowserStep("sleep", sleepArgs)
            );

            HeadlessResponse response3 = execute(null, steps, 30);
            System.out.println(response3);
            if (response3.isSuccess()) {
                System.out.println("操作日志:");
                for (String log : response3.getLogs()) {
                    System.out.println("  " + log);
                }
                System.out.println("✓ 测试3通过\n");
            } else {
                System.out.println("✗ 测试3失败: " + response3.getError() + "\n");
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== 测试完成 ===");
    }
}
