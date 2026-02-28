package com.potato.potatotool.content.redTeam.vulnScanner.http;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless 浏览器处理器
 * 用于执行无头浏览器操作并检测动态页面
 * 支持 Nuclei Headless 协议的所有功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class HeadlessHandler {
    
    /**
     * Headless 操作结果
     */
    public static class HeadlessResponse {
        private String url;                     // 访问的 URL
        private boolean success;                // 操作是否成功
        private long duration;                  // 总耗时（毫秒）
        private String pageTitle;               // 页面标题
        private String pageSource;              // 页面源代码
        private String screenshot;              // 截图 Base64（可选）
        private Map<String, Object> scriptResults;  // JavaScript 执行结果
        private List<String> logs;              // 操作日志
        private String error;                   // 错误信息
        
        public HeadlessResponse() {
            this.scriptResults = new HashMap<>();
            this.logs = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public String getPageTitle() { return pageTitle; }
        public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
        
        public String getPageSource() { return pageSource; }
        public void setPageSource(String pageSource) { this.pageSource = pageSource; }
        
        public String getScreenshot() { return screenshot; }
        public void setScreenshot(String screenshot) { this.screenshot = screenshot; }
        
        public Map<String, Object> getScriptResults() { return scriptResults; }
        public void setScriptResults(Map<String, Object> scriptResults) { this.scriptResults = scriptResults; }
        
        public List<String> getLogs() { return logs; }
        public void setLogs(List<String> logs) { this.logs = logs; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public void addLog(String log) {
            this.logs.add(log);
        }
        
        @Override
        public String toString() {
            return "HeadlessResponse{" +
                    "url='" + url + '\'' +
                    ", success=" + success +
                    ", duration=" + duration + "ms" +
                    ", pageTitle='" + pageTitle + '\'' +
                    ", scriptResults=" + scriptResults.size() +
                    ", logs=" + logs.size() +
                    '}';
        }
    }
    
    /**
     * 浏览器操作步骤
     */
    public static class BrowserStep {
        private String action;              // 操作类型
        private Map<String, String> args;   // 操作参数
        
        public BrowserStep(String action, Map<String, String> args) {
            this.action = action;
            this.args = args != null ? args : new HashMap<>();
        }
        
        public String getAction() { return action; }
        public Map<String, String> getArgs() { return args; }
    }
    
    /**
     * 执行 Headless 浏览器操作
     * 
     * @param url 起始 URL
     * @param steps 操作步骤列表
     * @return Headless 响应
     */
    public static HeadlessResponse execute(String url, List<BrowserStep> steps) {
        return execute(url, steps, 30);
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
        HeadlessResponse response = new HeadlessResponse();
        response.setUrl(url);
        
        long startTime = System.currentTimeMillis();
        WebDriver driver = null;
        
        try {
            // 初始化无头 Chrome 浏览器
            response.addLog("初始化无头浏览器...");
            driver = initHeadlessChrome();
            response.addLog("✓ 浏览器初始化成功");
            
            // 设置隐式等待（Java 8兼容）
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            
            // 导航到初始 URL
            if (url != null && !url.isEmpty()) {
                response.addLog("导航到: " + url);
                driver.get(url);
                response.addLog("✓ 页面加载完成");
            }
            
            // 执行操作步骤
            if (steps != null && !steps.isEmpty()) {
                executeSteps(driver, steps, response, timeoutSeconds);
            }
            
            // 获取最终页面信息
            response.setPageTitle(driver.getTitle());
            response.setPageSource(driver.getPageSource());
            response.addLog("✓ 页面标题: " + driver.getTitle());
            
            response.setSuccess(true);
            System.out.println("✓ Headless 操作成功完成");
            
        } catch (SessionNotCreatedException e) {
            response.setSuccess(false);

            String errorMsg = e.getMessage();

            // 检测版本不匹配问题
            if (errorMsg != null && errorMsg.contains("This version of ChromeDriver only supports Chrome version")) {
                // 提取版本号信息
                String supportedVersion = extractVersion(errorMsg, "Chrome version (\\d+)");
                String currentVersion = extractVersion(errorMsg, "Current browser version is ([\\d.]+)");

                String friendlyMsg = "⚠️  ChromeDriver 版本不匹配\n" +
                                   "   - ChromeDriver 支持的版本: Chrome " + (supportedVersion != null ? supportedVersion : "未知") + "\n" +
                                   "   - 当前 Chrome 版本: " + (currentVersion != null ? currentVersion : "未知") + "\n" +
                                   "\n解决方案:\n" +
                                   "   1. 更新 ChromeDriver: 删除旧版本，让 WebDriverManager 自动下载最新版\n" +
                                   "   2. 或安装匹配的 Chrome 版本\n" +
                                   "   3. 如果不需要 Headless 协议，可在配置中禁用\n" +
                                   "\n详细错误: " + errorMsg;

                response.setError(friendlyMsg);
                response.addLog("✗ " + friendlyMsg);
                System.err.println("\n" + friendlyMsg);
            } else {
                // 其他会话创建错误
                response.setError("浏览器会话创建失败，可能需要安装 Chrome 浏览器: " + errorMsg);
                response.addLog("✗ 错误: " + errorMsg);
                System.err.println("Headless 操作失败 - 浏览器会话创建失败: " + errorMsg);
            }
        } catch (TimeoutException e) {
            response.setSuccess(false);
            response.setError("操作超时: " + e.getMessage());
            response.addLog("✗ 超时: " + e.getMessage());
            System.err.println("Headless 操作超时: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Headless 操作异常: " + e.getMessage());
            response.addLog("✗ 异常: " + e.getMessage());
            System.err.println("Headless 操作失败: " + url + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭浏览器
            if (driver != null) {
                try {
                    driver.quit();
                    response.addLog("✓ 浏览器已关闭");
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            response.setDuration(System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
    
    /**
     * 初始化无头 Chrome 浏览器
     */
    private static WebDriver initHeadlessChrome() {
        // 使用 WebDriverManager 自动管理 ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // 配置 Chrome 选项
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");                 // 无头模式
        options.addArguments("--disable-gpu");              // 禁用 GPU
        options.addArguments("--no-sandbox");               // 禁用沙箱
        options.addArguments("--disable-dev-shm-usage");    // 禁用 /dev/shm 使用
        options.addArguments("--window-size=1920,1080");    // 窗口大小
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"); // UA
        options.addArguments("--disable-blink-features=AutomationControlled"); // 禁用自动化检测
        
        return new ChromeDriver(options);
    }
    
    /**
     * 执行操作步骤列表
     */
    private static void executeSteps(WebDriver driver, List<BrowserStep> steps, 
                                     HeadlessResponse response, int timeoutSeconds) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Java 8兼容：WebDriverWait使用int类型的超时参数
        WebDriverWait wait = new WebDriverWait(driver, timeoutSeconds);
        
        for (int i = 0; i < steps.size(); i++) {
            BrowserStep step = steps.get(i);
            String action = step.getAction();
            Map<String, String> args = step.getArgs();
            
            try {
                response.addLog("执行步骤 " + (i + 1) + ": " + action);
                
                switch (action.toLowerCase()) {
                    case "navigate":
                        // 导航到 URL
                        String targetUrl = args.get("url");
                        driver.get(targetUrl);
                        response.addLog("  ✓ 导航到: " + targetUrl);
                        break;
                    
                    case "waitload":
                        // 等待页面加载完成
                        wait.until(webDriver ->
                            js.executeScript("return document.readyState").equals("complete"));
                        response.addLog("  ✓ 页面加载完成");
                        break;
                    
                    case "script":
                        // 执行 JavaScript
                        String code = args.get("code");
                        Object result = js.executeScript(code);
                        response.getScriptResults().put("script_" + i, result);
                        response.addLog("  ✓ 脚本执行完成，结果: " + result);
                        break;
                    
                    case "click":
                        // 点击元素
                        String selector = args.get("selector");
                        WebElement element = driver.findElement(By.cssSelector(selector));
                        element.click();
                        response.addLog("  ✓ 点击元素: " + selector);
                        break;
                    
                    case "input":
                        // 输入文本
                        String inputSelector = args.get("selector");
                        String text = args.get("value");
                        WebElement inputElement = driver.findElement(By.cssSelector(inputSelector));
                        inputElement.clear();
                        inputElement.sendKeys(text);
                        response.addLog("  ✓ 输入文本到: " + inputSelector);
                        break;
                    
                    case "screenshot":
                        // 截图
                        if (driver instanceof TakesScreenshot) {
                            String screenshot = ((TakesScreenshot) driver)
                                .getScreenshotAs(OutputType.BASE64);
                            response.setScreenshot(screenshot);
                            response.addLog("  ✓ 截图完成");
                        }
                        break;
                    
                    case "sleep":
                        // 等待指定时间
                        int sleepMs = Integer.parseInt(args.getOrDefault("duration", "1000"));
                        Thread.sleep(sleepMs);
                        response.addLog("  ✓ 等待 " + sleepMs + "ms");
                        break;
                    
                    case "waitvisible":
                        // 等待元素可见
                        String waitSelector = args.get("selector");
                        wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(waitSelector)));
                        response.addLog("  ✓ 元素可见: " + waitSelector);
                        break;
                    
                    default:
                        response.addLog("  ⚠ 未知操作: " + action);
                }
                
            } catch (NoSuchElementException e) {
                response.addLog("  ✗ 元素未找到: " + e.getMessage());
                throw new RuntimeException("元素未找到", e);
            } catch (Exception e) {
                response.addLog("  ✗ 操作失败: " + e.getMessage());
                throw new RuntimeException("步骤执行失败: " + action, e);
            }
        }
    }
    
    /**
     * 简单导航并获取页面标题
     */
    public static HeadlessResponse navigateAndGetTitle(String url) {
        Map<String, String> navigateArgs = new HashMap<>();
        navigateArgs.put("url", url);
        Map<String, String> waitloadArgs = new HashMap<>();
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
        Map<String, String> navigateArgs = new HashMap<>();
        navigateArgs.put("url", url);
        Map<String, String> waitloadArgs = new HashMap<>();
        Map<String, String> scriptArgs = new HashMap<>();
        scriptArgs.put("code", script);
        List<BrowserStep> steps = Arrays.asList(
            new BrowserStep("navigate", navigateArgs),
            new BrowserStep("waitload", waitloadArgs),
            new BrowserStep("script", scriptArgs)
        );
        return execute(null, steps, 30);
    }

    /**
     * 从错误消息中提取版本号
     *
     * @param errorMsg 错误消息
     * @param pattern 正则表达式模式（需包含一个捕获组）
     * @return 提取的版本号，未找到返回 null
     */
    private static String extractVersion(String errorMsg, String pattern) {
        if (errorMsg == null || pattern == null) {
            return null;
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(errorMsg);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== 测试 Headless 浏览器 ===\n");
        
        try {
            // 测试1: 简单导航
            System.out.println("测试1: 导航到百度首页");
            HeadlessResponse response1 = navigateAndGetTitle("https://www.baidu.com");
            
            System.out.println(response1);
            if (response1.isSuccess()) {
                System.out.println("页面标题: " + response1.getPageTitle());
                System.out.println("✓ 测试1通过\n");
            } else {
                System.out.println("✗ 测试1失败: " + response1.getError() + "\n");
            }
            
            // 测试2: 执行 JavaScript
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
            
            // 测试3: 复杂操作序列
            System.out.println("测试3: 复杂操作序列");
            Map<String, String> navArgs = new HashMap<>();
            navArgs.put("url", "https://www.example.com");
            Map<String, String> waitArgs = new HashMap<>();
            Map<String, String> scriptArgs = new HashMap<>();
            scriptArgs.put("code", "return document.querySelector('h1').textContent");
            Map<String, String> sleepArgs = new HashMap<>();
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


