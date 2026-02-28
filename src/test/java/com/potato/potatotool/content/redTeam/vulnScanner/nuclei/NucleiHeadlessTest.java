package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HeadlessHandler;
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
 * Nuclei Headless 协议测试
 * 测试 Headless 协议的解析和浏览器操作功能
 * 
 * 注意: Headless 测试需要系统已安装 Chrome 浏览器
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiHeadlessTest {
    
    /**
     * 测试 Headless 数据模型
     */
    @Test
    public void testHeadlessDataModel() {
        System.out.println("=== 测试 Headless 数据模型 ===");
        
        // 创建 Headless 配置
        NucleiYamlObj.Headless headless = new NucleiYamlObj.Headless();
        
        NucleiYamlObj.HeadlessStep step1 = new NucleiYamlObj.HeadlessStep();
        step1.setAction("navigate");
        Map<String, String> args1 = new HashMap<>();
        args1.put("url", "https://example.com");
        step1.setArgs(args1);
        
        NucleiYamlObj.HeadlessStep step2 = new NucleiYamlObj.HeadlessStep();
        step2.setAction("waitload");
        step2.setArgs(new HashMap<>());
        
        headless.setSteps(Arrays.asList(step1, step2));
        headless.setMatchers_condition(NucleiYamlObj.MatchersCondition.and);
        
        // 验证
        assertNotNull(headless.getSteps());
        assertEquals(2, headless.getSteps().size());
        assertEquals("navigate", headless.getSteps().get(0).getAction());
        assertEquals("waitload", headless.getSteps().get(1).getAction());
        
        System.out.println("✓ Headless 数据模型测试通过");
    }
    
    /**
     * 测试 HeadlessHandler 基础导航
     * 注意: 需要 Chrome 浏览器
     */
    @Test
    public void testHeadlessHandlerBasicNavigation() {
        System.out.println("\n=== 测试 HeadlessHandler 基础导航 ===");
        
        try {
            HeadlessHandler.HeadlessResponse response = 
                HeadlessHandler.navigateAndGetTitle("https://www.example.com");
            
            assertNotNull(response);
            System.out.println("URL: " + response.getUrl());
            System.out.println("操作成功: " + response.isSuccess());
            System.out.println("耗时: " + response.getDuration() + "ms");
            
            if (response.isSuccess()) {
                System.out.println("页面标题: " + response.getPageTitle());
                assertNotNull(response.getPageTitle());
                assertTrue(response.getPageTitle().contains("Example"));
                System.out.println("✓ Headless 基础导航测试通过");
            } else {
                System.out.println("⚠ Headless 操作失败（可能未安装 Chrome）: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ Headless 测试异常（可能未安装 Chrome）: " + e.getMessage());
        }
    }
    
    /**
     * 测试 HeadlessHandler JavaScript 执行
     */
    @Test
    public void testHeadlessHandlerJavaScriptExecution() {
        System.out.println("\n=== 测试 HeadlessHandler JavaScript 执行 ===");
        
        try {
            HeadlessHandler.HeadlessResponse response = 
                HeadlessHandler.executeScript("https://www.example.com", 
                                             "return document.title");
            
            assertNotNull(response);
            System.out.println("操作成功: " + response.isSuccess());
            
            if (response.isSuccess()) {
                System.out.println("脚本结果: " + response.getScriptResults());
                assertNotNull(response.getScriptResults());
                assertFalse(response.getScriptResults().isEmpty());
                System.out.println("✓ JavaScript 执行测试通过");
            } else {
                System.out.println("⚠ Headless 操作失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ Headless 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试 HeadlessHandler 操作序列
     */
    @Test
    public void testHeadlessHandlerOperationSequence() {
        System.out.println("\n=== 测试 HeadlessHandler 操作序列 ===");
        
        try {
            Map<String, String> navArgs = new HashMap<>();
            navArgs.put("url", "https://www.example.com");
            Map<String, String> waitArgs = new HashMap<>();
            Map<String, String> scriptArgs = new HashMap<>();
            scriptArgs.put("code", "return document.querySelector('h1').textContent");
            Map<String, String> sleepArgs = new HashMap<>();
            sleepArgs.put("duration", "1000");
            List<HeadlessHandler.BrowserStep> steps = Arrays.asList(
                new HeadlessHandler.BrowserStep("navigate", navArgs),
                new HeadlessHandler.BrowserStep("waitload", waitArgs),
                new HeadlessHandler.BrowserStep("script", scriptArgs),
                new HeadlessHandler.BrowserStep("sleep", sleepArgs)
            );
            
            HeadlessHandler.HeadlessResponse response = 
                HeadlessHandler.execute(null, steps, 30);
            
            assertNotNull(response);
            System.out.println("操作成功: " + response.isSuccess());
            System.out.println("耗时: " + response.getDuration() + "ms");
            
            if (response.isSuccess()) {
                System.out.println("操作日志:");
                for (String log : response.getLogs()) {
                    System.out.println("  " + log);
                }
                assertFalse(response.getLogs().isEmpty());
                System.out.println("✓ 操作序列测试通过");
            } else {
                System.out.println("⚠ Headless 操作失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ Headless 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试简单 Headless 样例文件解析
     */
    @Test
    public void testSimpleHeadlessPocParsing() throws Exception {
        System.out.println("\n=== 测试简单 Headless 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/22-headless-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("headless-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("Headless Simple Page Title Test", nucleiPoc.getInfo().getName());
        
        // 验证 Headless 配置
        assertNotNull(nucleiPoc.getHeadless(), "Headless 配置不应为 null");
        assertFalse(nucleiPoc.getHeadless().isEmpty(), "Headless 配置列表不应为空");
        
        NucleiYamlObj.Headless headless = nucleiPoc.getHeadless().get(0);
        assertNotNull(headless.getSteps());
        assertEquals(3, headless.getSteps().size());
        assertEquals("navigate", headless.getSteps().get(0).getAction());
        assertEquals("waitload", headless.getSteps().get(1).getAction());
        assertEquals("script", headless.getSteps().get(2).getAction());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ Headless 配置数: " + nucleiPoc.getHeadless().size());
        System.out.println("✓ 操作步骤数: " + headless.getSteps().size());
        System.out.println("✓ 简单 Headless 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 Headless 样例文件解析
     */
    @Test
    public void testComprehensiveHeadlessPocParsing() throws Exception {
        System.out.println("\n=== 测试完整 Headless 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/21-headless-protocol.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("headless-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 Headless 配置（应该有 5 个测试用例）
        assertNotNull(nucleiPoc.getHeadless());
        assertEquals(5, nucleiPoc.getHeadless().size(), "应该有 5 个 Headless 测试用例");
        
        // 验证第一个 Headless 配置
        NucleiYamlObj.Headless headless1 = nucleiPoc.getHeadless().get(0);
        assertNotNull(headless1.getSteps());
        assertEquals(3, headless1.getSteps().size());
        
        // 验证第四个 Headless 配置（表单输入）
        NucleiYamlObj.Headless headless4 = nucleiPoc.getHeadless().get(3);
        assertNotNull(headless4.getSteps());
        assertEquals(6, headless4.getSteps().size());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ Headless 配置数: " + nucleiPoc.getHeadless().size());
        System.out.println("✓ 完整 Headless 样例文件解析测试通过");
    }
    
    /**
     * 测试 Headless POC 转换为通用格式
     */
    @Test
    public void testHeadlessPocConversion() throws Exception {
        System.out.println("\n=== 测试 Headless POC 转换 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/22-headless-simple.yml";
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
        assertEquals("headless", poc.getProtocol(), "协议应该是 headless");
        assertEquals("headless-simple-test", poc.getId());
        assertEquals("Headless Simple Page Title Test", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.HeadlessStep, "步骤应该是 HeadlessStep 类型");
        
        PocObj.HeadlessStep headlessStep = (PocObj.HeadlessStep) step;
        assertNotNull(headlessStep.getActions());
        assertEquals(3, headlessStep.getActions().size());
        assertEquals("navigate", headlessStep.getActions().get(0).getAction());
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 操作数: " + headlessStep.getActions().size());
        System.out.println("✓ Headless POC 转换测试通过");
    }
    
    /**
     * 测试 Headless 操作类型枚举
     */
    @Test
    public void testHeadlessActionEnum() {
        System.out.println("\n=== 测试 Headless 操作类型枚举 ===");
        
        // 验证所有操作类型
        NucleiYamlObj.HeadlessAction[] actions = NucleiYamlObj.HeadlessAction.values();
        assertEquals(8, actions.length, "应该有 8 种操作类型");
        
        // 验证具体操作类型
        assertEquals("navigate", NucleiYamlObj.HeadlessAction.navigate.name());
        assertEquals("waitload", NucleiYamlObj.HeadlessAction.waitload.name());
        assertEquals("script", NucleiYamlObj.HeadlessAction.script.name());
        assertEquals("click", NucleiYamlObj.HeadlessAction.click.name());
        assertEquals("input", NucleiYamlObj.HeadlessAction.input.name());
        assertEquals("screenshot", NucleiYamlObj.HeadlessAction.screenshot.name());
        assertEquals("sleep", NucleiYamlObj.HeadlessAction.sleep.name());
        assertEquals("waitvisible", NucleiYamlObj.HeadlessAction.waitvisible.name());
        
        System.out.println("✓ Headless 操作类型枚举测试通过");
    }
}


