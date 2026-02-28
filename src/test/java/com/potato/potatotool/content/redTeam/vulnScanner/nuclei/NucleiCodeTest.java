package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.CodeHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nuclei Code 协议测试
 * 测试 Code 协议的解析和代码执行功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiCodeTest {
    
    /**
     * 测试 Code 数据模型
     */
    @Test
    public void testCodeDataModel() {
        System.out.println("=== 测试 Code 数据模型 ===");
        
        // 创建 Code 配置
        NucleiYamlObj.Code code = new NucleiYamlObj.Code();
        code.setEngine(Arrays.asList("javascript"));
        code.setSource("1 + 2");
        code.setMatchers_condition(NucleiYamlObj.MatchersCondition.and);
        
        // 验证
        assertNotNull(code.getEngine());
        assertEquals(1, code.getEngine().size());
        assertEquals("javascript", code.getEngine().get(0));
        assertEquals("1 + 2", code.getSource());
        
        System.out.println("✓ Code 数据模型测试通过");
    }
    
    /**
     * 测试 CodeHandler 基础执行
     */
    @Test
    public void testCodeHandlerBasicExecution() {
        System.out.println("\n=== 测试 CodeHandler 基础执行 ===");
        
        CodeHandler.CodeResponse response = CodeHandler.executeJavaScript("1 + 2 + 3");
        
        assertNotNull(response);
        assertTrue(response.isSuccess(), "代码执行应该成功");
        assertEquals("javascript", response.getEngine());
        assertEquals(6, response.getResult());
        assertTrue(response.getDuration() >= 0);
        
        System.out.println("引擎: " + response.getEngine());
        System.out.println("结果: " + response.getResult());
        System.out.println("耗时: " + response.getDuration() + "ms");
        System.out.println("✓ 基础执行测试通过");
    }
    
    /**
     * 测试 CodeHandler 字符串操作
     */
    @Test
    public void testCodeHandlerStringOperations() {
        System.out.println("\n=== 测试 CodeHandler 字符串操作 ===");
        
        String code = "'Hello, ' + 'World!'";
        CodeHandler.CodeResponse response = CodeHandler.executeJavaScript(code);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Hello, World!", response.getResult());
        
        System.out.println("代码: " + code);
        System.out.println("结果: " + response.getResult());
        System.out.println("✓ 字符串操作测试通过");
    }
    
    /**
     * 测试 CodeHandler 上下文变量
     */
    @Test
    public void testCodeHandlerContextVariables() {
        System.out.println("\n=== 测试 CodeHandler 上下文变量 ===");
        
        Map<String, Object> context = new HashMap<>();
        context.put("url", "https://example.com");
        context.put("status", 200);
        context.put("isVulnerable", true);
        
        String code = "status === 200 && url.includes('example') && isVulnerable";
        CodeHandler.CodeResponse response = CodeHandler.executeJavaScript(code, context);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(true, response.getResult());
        
        System.out.println("上下文: " + context);
        System.out.println("代码: " + code);
        System.out.println("结果: " + response.getResult());
        System.out.println("✓ 上下文变量测试通过");
    }
    
    /**
     * 测试 CodeHandler 复杂逻辑
     */
    @Test
    public void testCodeHandlerComplexLogic() {
        System.out.println("\n=== 测试 CodeHandler 复杂逻辑 ===");
        
        String code = 
            "var result = [];\n" +
            "for (var i = 1; i <= 5; i++) {\n" +
            "    result.push(i * 2);\n" +
            "}\n" +
            "result";
        
        CodeHandler.CodeResponse response = CodeHandler.executeJavaScript(code);
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getResult() instanceof java.util.List);
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> resultList = (java.util.List<Object>) response.getResult();
        assertEquals(5, resultList.size());
        assertEquals(2, resultList.get(0));
        assertEquals(10, resultList.get(4));
        
        System.out.println("结果: " + response.getResult());
        System.out.println("✓ 复杂逻辑测试通过");
    }
    
    /**
     * 测试简单 Code 样例文件解析
     */
    @Test
    public void testSimpleCodePocParsing() throws Exception {
        System.out.println("\n=== 测试简单 Code 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/24-code-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("code-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("Code Simple Calculation Test", nucleiPoc.getInfo().getName());
        
        // 验证 Code 配置
        assertNotNull(nucleiPoc.getCode(), "Code 配置不应为 null");
        assertFalse(nucleiPoc.getCode().isEmpty(), "Code 配置列表不应为空");
        
        NucleiYamlObj.Code code = nucleiPoc.getCode().get(0);
        assertNotNull(code.getEngine());
        assertEquals("javascript", code.getEngine().get(0));
        assertNotNull(code.getSource());
        assertTrue(code.getSource().contains("1 + 2 + 3"));
        
        // 验证匹配器
        assertNotNull(code.getMatchers());
        assertFalse(code.getMatchers().isEmpty());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ Code 配置数: " + nucleiPoc.getCode().size());
        System.out.println("✓ 引擎: " + code.getEngine().get(0));
        System.out.println("✓ 简单 Code 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 Code 样例文件解析
     */
    @Test
    public void testComprehensiveCodePocParsing() throws Exception {
        System.out.println("\n=== 测试完整 Code 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/23-code-protocol.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("code-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 Code 配置（应该有 5 个测试用例）
        assertNotNull(nucleiPoc.getCode());
        assertEquals(5, nucleiPoc.getCode().size(), "应该有 5 个 Code 测试用例");
        
        // 验证第一个 Code 配置
        NucleiYamlObj.Code code1 = nucleiPoc.getCode().get(0);
        assertEquals("javascript", code1.getEngine().get(0));
        assertTrue(code1.getSource().contains("result"));
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ Code 配置数: " + nucleiPoc.getCode().size());
        System.out.println("✓ 完整 Code 样例文件解析测试通过");
    }
    
    /**
     * 测试 Code POC 转换为通用格式
     */
    @Test
    public void testCodePocConversion() throws Exception {
        System.out.println("\n=== 测试 Code POC 转换 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/24-code-simple.yml";
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
        assertEquals("code", poc.getProtocol(), "协议应该是 code");
        assertEquals("code-simple-test", poc.getId());
        assertEquals("Code Simple Calculation Test", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.CodeStep, "步骤应该是 CodeStep 类型");
        
        PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
        assertEquals("javascript", codeStep.getEngine());
        assertNotNull(codeStep.getSource());
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 引擎: " + codeStep.getEngine());
        System.out.println("✓ Code POC 转换测试通过");
    }
    
    /**
     * 测试 Code 引擎枚举
     */
    @Test
    public void testCodeEngineEnum() {
        System.out.println("\n=== 测试 Code 引擎枚举 ===");
        
        // 验证引擎类型
        NucleiYamlObj.CodeEngine[] engines = NucleiYamlObj.CodeEngine.values();
        assertEquals(2, engines.length, "应该有 2 种引擎类型");
        
        assertEquals("javascript", NucleiYamlObj.CodeEngine.javascript.name());
        assertEquals("python", NucleiYamlObj.CodeEngine.python.name());
        
        System.out.println("✓ Code 引擎枚举测试通过");
    }
    
    /**
     * 测试 CodeHandler 安全沙箱
     */
    @Test
    public void testCodeHandlerSandbox() {
        System.out.println("\n=== 测试 CodeHandler 安全沙箱 ===");
        
        // 测试不允许的操作（应该失败或受限）
        String maliciousCode = "java.lang.System.exit(0)";  // 尝试访问 Java 系统类
        
        CodeHandler.CodeResponse response = CodeHandler.executeJavaScript(maliciousCode);
        
        assertNotNull(response);
        // 沙箱应该阻止这种操作
        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        
        System.out.println("✓ 恶意代码: " + maliciousCode);
        System.out.println("✓ 执行失败（预期）: " + response.getError());
        System.out.println("✓ 安全沙箱测试通过");
    }
}


