package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 真实 POC 集成测试
 * 模拟真实 Xray POC 场景，验证与官方行为一致性
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("真实 POC 集成测试")
public class RealPocIntegrationTest {
    
    private Map<String, Object> context;
    
    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
    }
    
    @Test
    @DisplayName("真实POC - SQL注入时间盲注检测")
    public void testSqlInjectionTimeBlind() {
        // 模拟第一次正常请求（基准）
        Map<String, Object> r0Response = new HashMap<>();
        r0Response.put("status", 200);
        r0Response.put("body", "statistics offworkstate");
        r0Response.put("latency", 100);
        context.put("response", r0Response);
        
        // r0 规则: 基准请求成功
        boolean r0 = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"statistics\") && contains(response.body, \"offworkstate\")",
            context
        );
        assertTrue(r0, "r0 规则应该通过");
        
        // 记录基准延时
        context.put("r0latency", r0Response.get("latency"));
        
        // 模拟注入延时请求
        context.put("sleepSecond1", 7);
        Map<String, Object> r1Response = new HashMap<>();
        r1Response.put("status", 200);
        r1Response.put("body", "statistics offworkstate");
        r1Response.put("latency", 7200); // 延时约7秒
        context.put("response", r1Response);
        
        // r1 规则: 检测延时是否符合预期
        boolean r1 = XrayCelParser.evaluateLogical(
            "response.latency - r0latency >= sleepSecond1 * 1000 - 1000 && contains(response.body, \"statistics\")",
            context
        );
        assertTrue(r1, "r1 规则应该通过（延时检测）");
        
        // 模拟第二次延时验证
        context.put("sleepSecond2", 4);
        Map<String, Object> r2Response = new HashMap<>();
        r2Response.put("status", 200);
        r2Response.put("body", "statistics offworkstate");
        r2Response.put("latency", 4100);
        context.put("response", r2Response);
        
        boolean r2 = XrayCelParser.evaluateLogical(
            "response.latency - r0latency >= sleepSecond2 * 1000 - 1000 && contains(response.body, \"statistics\")",
            context
        );
        assertTrue(r2, "r2 规则应该通过（第二次延时验证）");
        
        // 最终表达式: r0() && r1() && r2()
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("r0", r0);
        rules.put("r1", r1);
        rules.put("r2", r2);
        
        Map<String, Object> ruleContext = new HashMap<>();
        ruleContext.putAll(rules);
        
        boolean finalResult = XrayCelParser.evaluateLogical("r0 && r1 && r2", ruleContext);
        assertTrue(finalResult, "最终POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 弱密码检测")
    public void testWeakPasswordDetection() {
        // check 规则: 检查页面是否存在
        Map<String, Object> checkResponse = new HashMap<>();
        checkResponse.put("status", 200);
        checkResponse.put("body", "Login Page Reporter System");
        context.put("response", checkResponse);
        
        boolean check = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"Reporter\")",
            context
        );
        assertTrue(check, "check 规则应该通过");
        
        // auth 规则: 尝试登录
        context.put("username", "admin");
        context.put("password", "admin123");
        
        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("status", 200);
        authResponse.put("body", "window.location href=\"main.html\" success");
        context.put("response", authResponse);
        
        boolean auth = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"window.location\") && contains(response.body, \"main.html\")",
            context
        );
        assertTrue(auth, "auth 规则应该通过");
        
        // 最终表达式
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("check", check);
        rules.put("auth", auth);
        
        Map<String, Object> ruleContext = new HashMap<>();
        ruleContext.putAll(rules);
        
        boolean finalResult = XrayCelParser.evaluateLogical("check && auth", ruleContext);
        assertTrue(finalResult, "弱密码POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 文件读取检测")
    public void testFileReadDetection() {
        // 发送文件读取请求
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon");
        context.put("response", response);
        
        // 检测规则: 响应包含 /etc/passwd 特征
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"root:x:0:0\") && contains(response.body, \"/bin/bash\")",
            context
        );
        assertTrue(result, "文件读取POC检测应该成功");
        
        // 使用正则提取
        Object matches = XrayCelParser.evaluateForValue(
            "submatch(response.body, \"root:x:(\\\\d+):(\\\\d+)\")",
            context
        );
        assertNotNull(matches);
        assertTrue(matches instanceof List);
        @SuppressWarnings("unchecked")
        List<String> matchList = (List<String>) matches;
        assertEquals(2, matchList.size());
        assertEquals("0", matchList.get(0));
        assertEquals("0", matchList.get(1));
    }
    
    @Test
    @DisplayName("真实POC - XSS检测")
    public void testXssDetection() {
        // 生成随机标记
        String randomMarker = "xss" + System.currentTimeMillis();
        context.put("marker", randomMarker);
        
        // 模拟响应包含注入的XSS代码
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "<html><script>alert('" + randomMarker + "')</script></html>");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, marker) && contains(response.body, \"<script>\")",
            context
        );
        assertTrue(result, "XSS POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 任意文件上传")
    public void testFileUploadDetection() {
        // 生成随机文件名
        Object randomName = XrayCelParser.evaluateForValue("rand_text_alphanumeric(8)", context);
        context.put("filename", randomName.toString() + ".jsp");
        
        // 上传后访问文件
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "File uploaded successfully: " + context.get("filename"));
        context.put("response", response);
        
        // 检测规则
        boolean uploadCheck = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, filename)",
            context
        );
        assertTrue(uploadCheck, "文件上传检测应该成功");
        
        // 访问上传的文件
        Map<String, Object> accessResponse = new HashMap<>();
        accessResponse.put("status", 200);
        accessResponse.put("body", "<%@ page import=\"java.util.*\" %>");
        context.put("response", accessResponse);
        
        boolean accessCheck = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"<%@\")",
            context
        );
        assertTrue(accessCheck, "文件访问检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 命令执行检测")
    public void testCommandExecutionDetection() {
        // 生成随机命令标记
        Object randomInt = XrayCelParser.evaluateForValue("randomInt(10000, 99999)", context);
        context.put("marker", randomInt);
        
        // 模拟命令执行结果
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "uid=0(root) gid=0(root) marker=" + randomInt);
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, string(marker)) && contains(response.body, \"uid=\")",
            context
        );
        assertTrue(result, "命令执行POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 反序列化检测")
    public void testDeserializationDetection() {
        // 构造payload（Base64编码）
        String payload = "malicious_serialized_data";
        Object encodedPayload = XrayCelParser.evaluateForValue("base64(payload)", 
            Collections.singletonMap("payload", payload));
        context.put("encodedPayload", encodedPayload);
        
        // 发送请求
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "Deserialization executed successfully");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"Deserialization\")",
            context
        );
        assertTrue(result, "反序列化POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - SSRF检测")
    public void testSsrfDetection() {
        // 使用内网地址
        context.put("internalUrl", "http://127.0.0.1:8080/admin");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "Admin Panel - Internal Access Only");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"Admin Panel\")",
            context
        );
        assertTrue(result, "SSRF POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 信息泄露检测")
    public void testInformationLeakDetection() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "MySQL Error: Access denied for user 'root'@'localhost' (password: YES)");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Server", "Apache/2.4.41 (Ubuntu)");
        headers.put("X-Powered-By", "PHP/7.4.3");
        response.put("headers", headers);
        
        context.put("response", response);
        
        // 检测数据库错误信息
        boolean dbError = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"MySQL Error\")",
            context
        );
        assertTrue(dbError, "数据库错误信息泄露检测应该成功");
        
        // 检测服务器版本信息
        // Note: 需要实现 headers 访问
        @SuppressWarnings("unchecked")
        Map<String, Object> headersMap = (Map<String, Object>) response.get("headers");
        context.put("serverHeader", headersMap.get("Server"));
        
        boolean versionLeak = XrayCelParser.evaluateLogical(
            "contains(serverHeader, \"Apache\") && contains(serverHeader, \"Ubuntu\")",
            context
        );
        assertTrue(versionLeak, "服务器版本信息泄露检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - JWT认证绕过")
    public void testJwtBypassDetection() {
        // 原始JWT token
        String jwtHeader = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String jwtPayload = "{\"sub\":\"admin\",\"role\":\"administrator\"}";
        
        // 构造绕过token
        Object header64 = XrayCelParser.evaluateForValue("base64(header)", 
            Collections.singletonMap("header", jwtHeader));
        Object payload64 = XrayCelParser.evaluateForValue("base64(payload)", 
            Collections.singletonMap("payload", jwtPayload));
        
        String bypassToken = header64 + "." + payload64 + ".";
        context.put("token", bypassToken);
        
        // 使用token访问
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "{\"user\":\"admin\",\"permissions\":[\"read\",\"write\",\"delete\"]}");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"admin\") && contains(response.body, \"permissions\")",
            context
        );
        assertTrue(result, "JWT认证绕过POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - XXE检测")
    public void testXxeDetection() {
        // 构造XXE payload
        String xxePayload = "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY test SYSTEM \"file:///etc/passwd\">]><root>&test;</root>";
        context.put("payload", xxePayload);
        
        // 响应包含文件内容
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "root:x:0:0:root:/root:/bin/bash");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"root:x:0:0\")",
            context
        );
        assertTrue(result, "XXE POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 目录遍历检测")
    public void testPathTraversalDetection() {
        // 构造目录遍历路径
        context.put("traversalPath", "../../../../etc/passwd");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("body", "root:x:0:0:root:/root:/bin/bash\nbin:x:1:1:bin:/bin:/sbin/nologin");
        context.put("response", response);
        
        // 检测规则
        boolean result = XrayCelParser.evaluateLogical(
            "response.status == 200 && matches(response.body, \".*root:x:\\\\d+:\\\\d+.*\") && contains(response.body, \"/bin/bash\")",
            context
        );
        assertTrue(result, "目录遍历POC检测应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 完整漏洞利用链")
    public void testFullExploitChain() {
        // 步骤1: 信息收集
        Map<String, Object> infoResponse = new HashMap<>();
        infoResponse.put("status", 200);
        infoResponse.put("body", "Server Version: 2.4.41");
        context.put("response", infoResponse);
        
        boolean step1 = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"Server Version\")",
            context
        );
        assertTrue(step1, "步骤1: 信息收集应该成功");
        
        // 步骤2: 认证绕过
        context.put("username", "admin' OR '1'='1");
        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("status", 200);
        authResponse.put("body", "Login successful");
        context.put("response", authResponse);
        
        boolean step2 = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"successful\")",
            context
        );
        assertTrue(step2, "步骤2: 认证绕过应该成功");
        
        // 步骤3: 权限提升
        Map<String, Object> privResponse = new HashMap<>();
        privResponse.put("status", 200);
        privResponse.put("body", "Role changed to administrator");
        context.put("response", privResponse);
        
        boolean step3 = XrayCelParser.evaluateLogical(
            "response.status == 200 && contains(response.body, \"administrator\")",
            context
        );
        assertTrue(step3, "步骤3: 权限提升应该成功");
        
        // 最终验证
        Map<String, Boolean> steps = new HashMap<>();
        steps.put("step1", step1);
        steps.put("step2", step2);
        steps.put("step3", step3);
        
        Map<String, Object> stepsContext = new HashMap<>();
        stepsContext.putAll(steps);
        
        boolean finalResult = XrayCelParser.evaluateLogical("step1 && step2 && step3", stepsContext);
        assertTrue(finalResult, "完整漏洞利用链应该成功");
    }
    
    @Test
    @DisplayName("真实POC - 性能测试（批量POC检测）")
    public void testBatchPocPerformance() {
        int pocCount = 50;
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < pocCount; i++) {
            // 模拟POC检测
            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("body", "test response " + i);
            context.put("response", response);
            
            XrayCelParser.evaluateLogical(
                "response.status == 200 && contains(response.body, \"test\")",
                context
            );
        }
        
        long time = System.currentTimeMillis() - start;
        assertTrue(time < 5000, "批量POC检测耗时过长: " + time + "ms for " + pocCount + " POCs");
        
        System.out.println("批量POC检测性能: " + pocCount + " 个POC耗时 " + time + "ms, 平均 " + (time / pocCount) + "ms/POC");
    }
}

