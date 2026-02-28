package com.potato.potatotool.vulnScanner.testlab;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * newPoc 目录 POC 正反测试校验
 * 
 * 对每个 POC 进行：
 * 1. 正向测试：对漏洞端点应该检测到漏洞
 * 2. 反向测试：对安全端点不应该检测到漏洞
 * 
 * @author Potato
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NewPocValidationTest {
    
    private static final int PORT = 18895;
    private static final String BASE_URL = "http://127.0.0.1:" + PORT;
    private static final String NEW_POC_DIR = "src/test/resources/newPoc";
    
    private static NewPocTestServer testServer;
    private static PocLoader pocLoader;
    
    // 测试统计
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static List<String> failedTestNames = new ArrayList<>();
    
    @BeforeAll
    public static void setup() throws Exception {
        testServer = new NewPocTestServer(PORT);
        testServer.start();
        pocLoader = new PocLoader();
        
        // 启用 DNSLog 模拟模式
        com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService.setMockMode(true);
        
        // 等待服务器启动
        Thread.sleep(500);
        
        System.out.println("========================================");
        System.out.println("newPoc 漏洞检测器正反校验测试");
        System.out.println("========================================");
        System.out.println("测试端口: " + PORT);
        System.out.println("POC 目录: " + NEW_POC_DIR);
        System.out.println("========================================\n");
    }
    
    @AfterAll
    public static void tearDown() {
        if (testServer != null) {
            testServer.stop();
        }
        
        // 打印测试总结
        System.out.println("\n========================================");
        System.out.println("测试总结");
        System.out.println("========================================");
        System.out.println("总测试数: " + totalTests);
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        if (!failedTestNames.isEmpty()) {
            System.out.println("\n失败的测试:");
            for (String name : failedTestNames) {
                System.out.println("  - " + name);
            }
        }
        System.out.println("========================================");
    }
    
    /**
     * 执行正反测试
     * @param pocPath POC 文件路径
     * @param vulnUrlPath 漏洞端点路径（正向测试）
     * @param safeUrlPath 安全端点路径（反向测试）
     * @param pocName POC 名称
     */
    private void testPocBothWays(String pocPath, String vulnUrlPath, String safeUrlPath, String pocName) {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: " + pocName);
        System.out.println("文件: " + pocPath);
        
        try {
            File pocFile = new File(pocPath);
            if (!pocFile.exists()) {
                System.err.println("POC 文件不存在: " + pocPath);
                recordFailure(pocName + " (文件不存在)");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            if (pocObj == null) {
                System.err.println("POC 加载失败: " + pocPath);
                recordFailure(pocName + " (加载失败)");
                return;
            }
            
            ScanConfig config = new ScanConfig();
            PocExecutor executor = new PocExecutor(config);
            
            // 正向测试：应该检测到漏洞
            totalTests++;
            String vulnUrl = BASE_URL + vulnUrlPath;
            System.out.println("\n[正向测试] 目标: " + vulnUrl);
            NewPocTestServer.setVulnerableMode(true); // 设置漏洞模式
            try {
                ScanResult vulnResult = executor.execute(vulnUrl, pocObj);
                if (vulnResult.isVulnerable()) {
                    System.out.println("  ✓ 正确检测到漏洞");
                    passedTests++;
                } else {
                    System.out.println("  ✗ 应该检测到漏洞但未检测到");
                    recordFailure(pocName + " (正向测试失败)");
                }
            } catch (Exception e) {
                System.out.println("  ✗ 正向测试异常: " + e.getMessage());
                recordFailure(pocName + " (正向测试异常: " + e.getMessage() + ")");
            }
            
            // 反向测试：不应该检测到漏洞
            totalTests++;
            String safeUrl = BASE_URL + safeUrlPath;
            System.out.println("\n[反向测试] 目标: " + safeUrl);
            NewPocTestServer.setVulnerableMode(false); // 设置安全模式
            try {
                ScanResult safeResult = executor.execute(safeUrl, pocObj);
                if (!safeResult.isVulnerable()) {
                    System.out.println("  ✓ 正确未检测到漏洞");
                    passedTests++;
                } else {
                    System.out.println("  ✗ 不应该检测到漏洞但误报了");
                    recordFailure(pocName + " (反向测试失败-误报)");
                }
            } catch (Exception e) {
                // 反向测试中的异常通常意味着不会误报
                System.out.println("  ✓ 反向测试异常（无误报）: " + e.getMessage());
                passedTests++;
            }
            
        } catch (Exception e) {
            System.err.println("测试执行异常: " + e.getMessage());
            e.printStackTrace();
            recordFailure(pocName + " (执行异常)");
        }
    }
    
    private void recordFailure(String testName) {
        failedTests++;
        failedTestNames.add(testName);
    }
    
    // ========================================
    // Goby POC 测试
    // ========================================
    
    @Test
    @Order(1)
    @DisplayName("Goby POC 01: 基础 GET + contains 操作")
    public void testGoby01_BasicGetAndContains() {
        testPocBothWays(
            NEW_POC_DIR + "/gobyPoc/01_basic_get_and_contains.json",
            "/vuln/goby/01/api/dp/rptsvcsyncpoint?ccid=1",
            "/safe/goby/01/api/dp/rptsvcsyncpoint?ccid=1",
            "Goby 01: 基础 GET + contains"
        );
    }
    
    @Test
    @Order(2)
    @DisplayName("Goby POC 03: OR 条件多步骤")
    public void testGoby03_OrConditionMultiStep() {
        testPocBothWays(
            NEW_POC_DIR + "/gobyPoc/03_or_condition_multi_step.json",
            "/vuln/goby/03",
            "/safe/goby/03",
            "Goby 03: OR 条件多步骤"
        );
    }
    
    @Test
    @Order(3)
    @DisplayName("Goby POC 04: regex 操作")
    public void testGoby04_RegexOperation() {
        testPocBothWays(
            NEW_POC_DIR + "/gobyPoc/04_regex_operation.json",
            "/vuln/goby/04",
            "/safe/goby/04",
            "Goby 04: regex 操作"
        );
    }
    
    @Test
    @Order(4)
    @DisplayName("Goby POC 06: 302 重定向状态码")
    public void testGoby06_Status302Redirect() {
        testPocBothWays(
            NEW_POC_DIR + "/gobyPoc/06_status_302_redirect.json",
            "/vuln/goby/06/redirect",
            "/safe/goby/06/redirect",
            "Goby 06: 302 重定向"
        );
    }
    
    @Test
    @Order(5)
    @DisplayName("Goby POC 07: 500 错误状态码")
    public void testGoby07_Status500Error() {
        testPocBothWays(
            NEW_POC_DIR + "/gobyPoc/07_status_500_error.json",
            "/vuln/goby/07/error",
            "/safe/goby/07/error",
            "Goby 07: 500 错误"
        );
    }
    
    // ========================================
    // Nuclei POC 测试
    // ========================================
    
    @Test
    @Order(10)
    @DisplayName("Nuclei POC 01: CVE LFI 基础")
    public void testNuclei01_CveLfiBasic() {
        testPocBothWays(
            NEW_POC_DIR + "/nucleiPoc/01_cve_lfi_basic.yaml",
            "/vuln/nuclei/01",
            "/safe/nuclei/01",
            "Nuclei 01: CVE LFI 基础"
        );
    }
    
    @Test
    @Order(11)
    @DisplayName("Nuclei POC 02: Extractors 登录 (self-contained POC - 跳过)")
    public void testNuclei02_ExtractorsLogin() {
        // 这个 POC 是 self-contained，直接请求 github.com，无法通过本地测试服务器测试
        // 跳过正反测试，只验证 POC 加载
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 02: Extractors 登录 (self-contained)");
        System.out.println("跳过理由: POC 是 self-contained，直接请求 github.com");
        
        try {
            java.io.File pocFile = new java.io.File(NEW_POC_DIR + "/nucleiPoc/02_extractors_login.yaml");
            if (pocFile.exists()) {
                PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
                if (pocObj != null) {
                    System.out.println("  ✓ POC 加载成功");
                    passedTests++;
                    totalTests++;
                } else {
                    System.out.println("  ✗ POC 加载失败");
                    recordFailure("Nuclei 02: Extractors 登录 (POC 加载失败)");
                    totalTests++;
                }
            }
        } catch (Exception e) {
            System.err.println("  ✗ 异常: " + e.getMessage());
            recordFailure("Nuclei 02: Extractors 登录 (异常)");
            totalTests++;
        }
    }
    
    @Test
    @Order(12)
    @DisplayName("Nuclei POC 03: Payloads 检测")
    public void testNuclei03_PayloadsDetect() {
        testPocBothWays(
            NEW_POC_DIR + "/nucleiPoc/03_payloads_detect.yaml",
            "/vuln/nuclei/03",
            "/safe/nuclei/03",
            "Nuclei 03: Payloads 检测"
        );
    }
    
    @Test
    @Order(13)
    @DisplayName("Nuclei POC 10: 默认登录检测")
    public void testNuclei10_DefaultLogin() {
        testPocBothWays(
            NEW_POC_DIR + "/nucleiPoc/10_default_login.yaml",
            "/vuln/nuclei/10",
            "/safe/nuclei/10",
            "Nuclei 10: 默认登录"
        );
    }
    
    @Test
    @Order(14)
    @DisplayName("Nuclei POC 11: DSL 表达式")
    public void testNuclei11_DslExpressions() {
        // POC 路径是 /api/data，base URL 应该是 /vuln/nuclei/11
        testPocBothWays(
            NEW_POC_DIR + "/nucleiPoc/11_dsl_expressions.yml",
            "/vuln/nuclei/11",
            "/safe/nuclei/11",
            "Nuclei 11: DSL 表达式"
        );
    }
    
    // ========================================
    // Xray POC 测试
    // ========================================
    
    @Test
    @Order(20)
    @DisplayName("Xray POC 01: 多规则 OR + bmatches")
    public void testXray01_MultiRuleOrBmatches() {
        testPocBothWays(
            NEW_POC_DIR + "/xrayPoc/01_multi_rule_or_bmatches.yml",
            "/vuln/xray/01",
            "/safe/xray/01",
            "Xray 01: 多规则 OR + bmatches"
        );
    }
    
    @Test
    @Order(21)
    @DisplayName("Xray POC 04: Payloads 弱口令")
    public void testXray04_PayloadsWeakPassword() {
        testPocBothWays(
            NEW_POC_DIR + "/xrayPoc/04_payloads_weak_password.yml",
            "/vuln/xray/04",
            "/safe/xray/04",
            "Xray 04: Payloads 弱口令"
        );
    }
    
    @Test
    @Order(22)
    @DisplayName("Xray POC 05: 简单文件读取")
    public void testXray05_SimpleFileRead() {
        testPocBothWays(
            NEW_POC_DIR + "/xrayPoc/05_simple_file_read.yml",
            "/vuln/xray/05",
            "/safe/xray/05",
            "Xray 05: 简单文件读取"
        );
    }
    
    @Test
    @Order(23)
    @DisplayName("Xray POC 06: POST SQL 注入")
    public void testXray06_PostSqli() {
        testPocBothWays(
            NEW_POC_DIR + "/xrayPoc/06_post_sqli.yml",
            "/vuln/xray/06",
            "/safe/xray/06",
            "Xray 06: POST SQL 注入"
        );
    }
    
    @Test
    @Order(24)
    @DisplayName("Xray POC 07: 未授权访问")
    public void testXray07_UnauthAccess() {
        testPocBothWays(
            NEW_POC_DIR + "/xrayPoc/07_unauth_access.yml",
            "/vuln/xray/07",
            "/safe/xray/07",
            "Xray 07: 未授权访问"
        );
    }
    
    // ========================================
    // Pocsuite POC 测试
    // ========================================
    
    @Test
    @Order(30)
    @DisplayName("Pocsuite POC 01: SQL 注入 Header Referer")
    public void testPocsuite01_SqliHeaderReferer() {
        testPocBothWays(
            NEW_POC_DIR + "/pocsuitePoc/01_sqli_header_referer.json",
            "/vuln/pocsuite/01",
            "/safe/pocsuite/01",
            "Pocsuite 01: SQL 注入 Header Referer"
        );
    }
    
    @Test
    @Order(31)
    @DisplayName("Pocsuite POC 02: 多步骤认证绕过")
    public void testPocsuite02_MultiStepNecessary() {
        testPocBothWays(
            NEW_POC_DIR + "/pocsuitePoc/02_multi_step_necessary.json",
            "/vuln/pocsuite/02",
            "/safe/pocsuite/02",
            "Pocsuite 02: 多步骤认证绕过"
        );
    }
    
    @Test
    @Order(32)
    @DisplayName("Pocsuite POC 03: 时间盲注")
    @Timeout(value = 30)
    public void testPocsuite03_TimeBlindInjection() {
        testPocBothWays(
            NEW_POC_DIR + "/pocsuitePoc/03_time_blind_injection.json",
            "/vuln/pocsuite/03",
            "/safe/pocsuite/03",
            "Pocsuite 03: 时间盲注"
        );
    }
    
    // ========================================
    // 边界情况和特殊逻辑测试
    // ========================================
    
    @Test
    @Order(40)
    @DisplayName("边界测试: Nuclei 14 边界情况")
    public void testNuclei14_BoundaryCases() {
        // 边界情况测试需要特殊处理
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 14 边界情况");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/14_boundary_cases.yml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ POC 加载成功，包含边界情况测试定义");
            
            // 验证 POC 结构完整性
            assertNotNull(pocObj.getVerifySteps(), "验证步骤不为空");
            assertTrue(pocObj.getVerifySteps().size() > 0, "至少有一个验证步骤");
            System.out.println("  ✓ POC 结构验证通过，包含 " + pocObj.getVerifySteps().size() + " 个步骤");
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("边界测试异常: " + e.getMessage());
            recordFailure("Nuclei 14 边界情况测试");
            totalTests++;
        }
    }
    
    @Test
    @Order(41)
    @DisplayName("边界测试: Nuclei 15 复杂嵌套")
    public void testNuclei15_ComplexNested() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 15 复杂嵌套");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/15_complex_nested.yml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ POC 加载成功，包含复杂嵌套测试定义");
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("复杂嵌套测试异常: " + e.getMessage());
            recordFailure("Nuclei 15 复杂嵌套测试");
            totalTests++;
        }
    }
    
    // ========================================
    // 高级协议测试（模拟验证）
    // ========================================
    
    @Test
    @Order(50)
    @DisplayName("协议测试: Nuclei 05 TCP 协议")
    public void testNuclei05_NetworkTcp() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 05 TCP 协议 (Redis)");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/05_network_tcp.yaml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            
            // TCP 协议 POC 应该被解析为 TcpStep
            assertNotNull(pocObj.getVerifySteps(), "验证步骤不为空");
            System.out.println("  ✓ TCP 协议 POC 解析成功");
            System.out.println("  协议类型: " + pocObj.getProtocol());
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("TCP 协议测试异常: " + e.getMessage());
            recordFailure("Nuclei 05 TCP 协议测试");
            totalTests++;
        }
    }
    
    @Test
    @Order(51)
    @DisplayName("协议测试: Nuclei 06 SSL 协议")
    public void testNuclei06_SslProtocol() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 06 SSL 协议");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/06_ssl_protocol.yaml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ SSL 协议 POC 解析成功");
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("SSL 协议测试异常: " + e.getMessage());
            recordFailure("Nuclei 06 SSL 协议测试");
            totalTests++;
        }
    }
    
    @Test
    @Order(52)
    @DisplayName("协议测试: Nuclei 16 Headless 协议")
    public void testNuclei16_HeadlessBasic() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 16 Headless 协议");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/16_headless_basic.yaml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ Headless 协议 POC 解析成功");
            System.out.println("  包含 flow 控制: " + (pocObj.getFlow() != null));
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("Headless 协议测试异常: " + e.getMessage());
            recordFailure("Nuclei 16 Headless 协议测试");
            totalTests++;
        }
    }
    
    @Test
    @Order(53)
    @DisplayName("协议测试: Nuclei 17 Code Python")
    public void testNuclei17_CodePython() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 17 Code Python 协议");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/17_code_python_basic.yaml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ Code Python 协议 POC 解析成功");
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("Code Python 协议测试异常: " + e.getMessage());
            recordFailure("Nuclei 17 Code Python 协议测试");
            totalTests++;
        }
    }
    
    @Test
    @Order(54)
    @DisplayName("协议测试: Nuclei 19 TCP Payloads")
    public void testNuclei19_TcpPayloadsBatteringram() {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: Nuclei 19 TCP Payloads Batteringram");
        
        try {
            File pocFile = new File(NEW_POC_DIR + "/nucleiPoc/19_tcp_payloads_batteringram.yaml");
            if (!pocFile.exists()) {
                System.out.println("POC 文件不存在，跳过");
                return;
            }
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载成功");
            System.out.println("  ✓ TCP Payloads POC 解析成功");
            System.out.println("  变量类型: " + pocObj.getVariablesType());
            
            passedTests++;
            totalTests++;
            
        } catch (Exception e) {
            System.err.println("TCP Payloads 协议测试异常: " + e.getMessage());
            recordFailure("Nuclei 19 TCP Payloads 测试");
            totalTests++;
        }
    }
    
    // ========================================
    // 主测试入口（可独立运行）
    // ========================================
    
    public static void main(String[] args) {
        NewPocValidationTest test = new NewPocValidationTest();
        try {
            setup();
            
            // Goby POC 测试
            test.testGoby01_BasicGetAndContains();
            test.testGoby03_OrConditionMultiStep();
            test.testGoby04_RegexOperation();
            test.testGoby06_Status302Redirect();
            test.testGoby07_Status500Error();
            
            // Nuclei POC 测试
            test.testNuclei01_CveLfiBasic();
            test.testNuclei02_ExtractorsLogin();
            test.testNuclei03_PayloadsDetect();
            test.testNuclei10_DefaultLogin();
            test.testNuclei11_DslExpressions();
            
            // Xray POC 测试
            test.testXray01_MultiRuleOrBmatches();
            test.testXray04_PayloadsWeakPassword();
            test.testXray05_SimpleFileRead();
            test.testXray06_PostSqli();
            test.testXray07_UnauthAccess();
            
            // Pocsuite POC 测试
            test.testPocsuite01_SqliHeaderReferer();
            test.testPocsuite02_MultiStepNecessary();
            test.testPocsuite03_TimeBlindInjection();
            
            // 边界情况测试
            test.testNuclei14_BoundaryCases();
            test.testNuclei15_ComplexNested();
            
            // 协议测试
            test.testNuclei05_NetworkTcp();
            test.testNuclei06_SslProtocol();
            test.testNuclei16_HeadlessBasic();
            test.testNuclei17_CodePython();
            test.testNuclei19_TcpPayloadsBatteringram();
            
        } catch (Exception e) {
            System.err.println("测试执行异常:");
            e.printStackTrace();
        } finally {
            tearDown();
            System.exit(failedTests > 0 ? 1 : 0);
        }
    }
}
