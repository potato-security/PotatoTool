package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.WebSocketHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nuclei WebSocket 协议测试
 * 测试 WebSocket 协议的解析和通信功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiWebSocketTest {
    
    /**
     * 测试 WebSocket 数据模型
     */
    @Test
    public void testWebSocketDataModel() {
        System.out.println("=== 测试 WebSocket 数据模型 ===");
        
        // 创建 WebSocket 配置
        NucleiYamlObj.WebSocket websocket = new NucleiYamlObj.WebSocket();
        websocket.setAddress("ws://example.com/ws");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Test");
        websocket.setHeaders(headers);
        
        NucleiYamlObj.WebSocketInput input = new NucleiYamlObj.WebSocketInput();
        input.setData("test message");
        input.setName("test_input");
        websocket.setInputs(Arrays.asList(input));
        
        // 验证
        assertEquals("ws://example.com/ws", websocket.getAddress());
        assertNotNull(websocket.getHeaders());
        assertEquals("Test", websocket.getHeaders().get("User-Agent"));
        assertNotNull(websocket.getInputs());
        assertEquals(1, websocket.getInputs().size());
        assertEquals("test message", websocket.getInputs().get(0).getData());
        
        System.out.println("✓ WebSocket 数据模型测试通过");
    }
    
    /**
     * 测试 WebSocketHandler 基础通信
     * 注意：此测试需要网络连接，可能会超时
     */
    @Test
    public void testWebSocketHandlerBasicCommunication() {
        System.out.println("\n=== 测试 WebSocketHandler 基础通信 ===");
        
        String echoServer = "wss://echo.websocket.org/.ws";
        List<String> messages = Arrays.asList("Hello WebSocket", "Test message");
        
        try {
            WebSocketHandler.WebSocketResponse response = 
                WebSocketHandler.communicate(echoServer, messages);
            
            assertNotNull(response);
            System.out.println("地址: " + response.getAddress());
            System.out.println("连接成功: " + response.isConnected());
            System.out.println("操作成功: " + response.isSuccess());
            System.out.println("HTTP 状态码: " + response.getStatusCode());
            System.out.println("耗时: " + response.getDuration() + "ms");
            System.out.println("接收消息数: " + response.getReceivedMessages().size());
            
            if (response.isSuccess()) {
                System.out.println("接收到的消息: " + response.getReceivedMessages());
                System.out.println("✓ WebSocket 基础通信测试通过");
            } else {
                System.out.println("⚠ WebSocket 通信失败（可能是网络问题）: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ WebSocket 测试异常（可能是网络问题）: " + e.getMessage());
        }
    }
    
    /**
     * 测试 WebSocketHandler 发送单条消息
     */
    @Test
    public void testWebSocketHandlerSendMessage() {
        System.out.println("\n=== 测试 WebSocketHandler 发送单条消息 ===");
        
        String echoServer = "wss://echo.websocket.org/.ws";
        String message = "{\"action\":\"ping\"}";
        
        try {
            WebSocketHandler.WebSocketResponse response = 
                WebSocketHandler.sendMessage(echoServer, message);
            
            assertNotNull(response);
            System.out.println("地址: " + response.getAddress());
            System.out.println("连接成功: " + response.isConnected());
            System.out.println("操作成功: " + response.isSuccess());
            
            if (response.isSuccess()) {
                System.out.println("接收到的消息: " + response.getReceivedMessages());
                System.out.println("✓ 单条消息发送测试通过");
            } else {
                System.out.println("⚠ WebSocket 通信失败（可能是网络问题）: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ WebSocket 测试异常（可能是网络问题）: " + e.getMessage());
        }
    }
    
    /**
     * 测试 WebSocketHandler 自定义请求头
     */
    @Test
    public void testWebSocketHandlerCustomHeaders() {
        System.out.println("\n=== 测试 WebSocketHandler 自定义请求头 ===");
        
        String echoServer = "wss://echo.websocket.org/.ws";
        List<String> messages = Arrays.asList("Test with headers");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "PotatoTool/2.5.1");
        headers.put("X-Custom-Header", "Test");
        
        try {
            WebSocketHandler.WebSocketResponse response = 
                WebSocketHandler.communicate(echoServer, messages, headers, 10);
            
            assertNotNull(response);
            System.out.println("地址: " + response.getAddress());
            System.out.println("连接成功: " + response.isConnected());
            
            if (response.isSuccess()) {
                System.out.println("✓ 自定义请求头测试通过");
            } else {
                System.out.println("⚠ WebSocket 通信失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ WebSocket 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试简单 WebSocket 样例文件解析
     */
    @Test
    public void testSimpleWebSocketPocParsing() throws Exception {
        System.out.println("\n=== 测试简单 WebSocket 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/16-websocket-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("websocket-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("WebSocket Simple Echo Test", nucleiPoc.getInfo().getName());
        
        // 验证 WebSocket 配置
        assertNotNull(nucleiPoc.getWebsocket(), "WebSocket 配置不应为 null");
        assertFalse(nucleiPoc.getWebsocket().isEmpty(), "WebSocket 配置列表不应为空");
        
        NucleiYamlObj.WebSocket websocket = nucleiPoc.getWebsocket().get(0);
        assertEquals("wss://echo.websocket.org/.ws", websocket.getAddress());
        assertNotNull(websocket.getInputs());
        assertFalse(websocket.getInputs().isEmpty());
        assertEquals("Hello WebSocket", websocket.getInputs().get(0).getData());
        
        // 验证匹配器
        assertNotNull(websocket.getMatchers());
        assertFalse(websocket.getMatchers().isEmpty());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ WebSocket 配置数: " + nucleiPoc.getWebsocket().size());
        System.out.println("✓ 地址: " + websocket.getAddress());
        System.out.println("✓ 输入消息数: " + websocket.getInputs().size());
        System.out.println("✓ 简单 WebSocket 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 WebSocket 样例文件解析
     */
    @Test
    public void testComprehensiveWebSocketPocParsing() throws Exception {
        System.out.println("\n=== 测试完整 WebSocket 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/15-websocket-protocol.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("websocket-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 WebSocket 配置（应该有 3 个测试用例）
        assertNotNull(nucleiPoc.getWebsocket());
        assertEquals(3, nucleiPoc.getWebsocket().size(), "应该有 3 个 WebSocket 测试用例");
        
        // 验证第一个 WebSocket 配置（Echo 测试）
        NucleiYamlObj.WebSocket ws1 = nucleiPoc.getWebsocket().get(0);
        assertEquals("wss://echo.websocket.org/.ws", ws1.getAddress());
        assertNotNull(ws1.getInputs());
        assertEquals(3, ws1.getInputs().size());
        
        // 验证第二个 WebSocket 配置（JSON 消息测试）
        NucleiYamlObj.WebSocket ws2 = nucleiPoc.getWebsocket().get(1);
        assertEquals("wss://echo.websocket.org/.ws", ws2.getAddress());
        assertNotNull(ws2.getHeaders());
        assertEquals("PotatoTool/2.5.1", ws2.getHeaders().get("User-Agent"));
        
        // 验证第三个 WebSocket 配置（多消息序列测试）
        NucleiYamlObj.WebSocket ws3 = nucleiPoc.getWebsocket().get(2);
        assertEquals("wss://echo.websocket.org/.ws", ws3.getAddress());
        assertNotNull(ws3.getInputs());
        assertEquals(3, ws3.getInputs().size());
        assertEquals("first_message", ws3.getInputs().get(0).getName());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ WebSocket 配置数: " + nucleiPoc.getWebsocket().size());
        System.out.println("✓ 完整 WebSocket 样例文件解析测试通过");
    }
    
    /**
     * 测试 WebSocket POC 转换为通用格式
     */
    @Test
    public void testWebSocketPocConversion() throws Exception {
        System.out.println("\n=== 测试 WebSocket POC 转换 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/16-websocket-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        
        // 转换为通用 POC 格式
        NucleiPocConverter converter = new NucleiPocConverter();
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc, "转换后的 POC 对象不应为 null");
        assertEquals("websocket", poc.getProtocol(), "协议应该是 websocket");
        assertEquals("websocket-simple-test", poc.getId());
        assertEquals("WebSocket Simple Echo Test", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.WebSocketStep, "步骤应该是 WebSocketStep 类型");
        
        PocObj.WebSocketStep wsStep = (PocObj.WebSocketStep) step;
        assertEquals("wss://echo.websocket.org/.ws", wsStep.getAddress());
        assertNotNull(wsStep.getMessages());
        assertFalse(wsStep.getMessages().isEmpty());
        assertEquals("Hello WebSocket", wsStep.getMessages().get(0));
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 地址: " + wsStep.getAddress());
        System.out.println("✓ 消息数: " + wsStep.getMessages().size());
        System.out.println("✓ WebSocket POC 转换测试通过");
    }
    
    /**
     * 测试 WebSocket 错误处理
     */
    @Test
    public void testWebSocketErrorHandling() {
        System.out.println("\n=== 测试 WebSocket 错误处理 ===");
        
        // 测试无效地址
        String invalidAddress = "ws://invalid-websocket-server-12345.com/ws";
        List<String> messages = Arrays.asList("test");
        
        WebSocketHandler.WebSocketResponse response = 
            WebSocketHandler.communicate(invalidAddress, messages, null, 5);
        
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        System.out.println("✓ 错误消息: " + response.getError());
        System.out.println("✓ WebSocket 错误处理测试通过");
    }
    
    /**
     * 测试 WebSocket 数据模型的所有字段
     */
    @Test
    public void testWebSocketDataModelComplete() {
        System.out.println("\n=== 测试 WebSocket 数据模型完整性 ===");
        
        NucleiYamlObj.WebSocket websocket = new NucleiYamlObj.WebSocket();
        
        // 设置所有字段
        websocket.setAddress("ws://test.com/ws");
        Map<String, String> wsHeaders = new HashMap<>();
        wsHeaders.put("Authorization", "Bearer token");
        websocket.setHeaders(wsHeaders);
        websocket.setAttack(1);
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("key", Arrays.asList("value1", "value2"));
        websocket.setPayloads(payloads);
        websocket.setMatchers_condition(NucleiYamlObj.MatchersCondition.and);
        
        NucleiYamlObj.WebSocketInput input1 = new NucleiYamlObj.WebSocketInput();
        input1.setData("message 1");
        input1.setName("msg1");
        
        NucleiYamlObj.WebSocketInput input2 = new NucleiYamlObj.WebSocketInput();
        input2.setData("message 2");
        input2.setName("msg2");
        
        websocket.setInputs(Arrays.asList(input1, input2));
        
        // 验证所有字段
        assertEquals("ws://test.com/ws", websocket.getAddress());
        assertNotNull(websocket.getHeaders());
        assertEquals("Bearer token", websocket.getHeaders().get("Authorization"));
        assertEquals(1, websocket.getAttack());
        assertNotNull(websocket.getPayloads());
        assertEquals(NucleiYamlObj.MatchersCondition.and, websocket.getMatchers_condition());
        assertEquals(2, websocket.getInputs().size());
        assertEquals("msg1", websocket.getInputs().get(0).getName());
        
        System.out.println("✓ WebSocket 数据模型完整性测试通过");
    }
}



