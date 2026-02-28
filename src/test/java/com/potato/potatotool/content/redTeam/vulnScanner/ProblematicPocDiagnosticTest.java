package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.http.CodeHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.PocConverterRegistry;
import com.potato.potatotool.content.redTeam.vulnScanner.util.constructor.NucleiConstructor;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诊断有问题的 POC 测试用例
 * 
 * 问题1: JavaScript 代码执行时 source 为 null
 * 问题2: DSL 表达式括号不匹配
 */
public class ProblematicPocDiagnosticTest {

    /**
     * 测试问题1: JavaScript 类型 POC 的 code/source 字段解析
     * 
     * 重点诊断：Nuclei YAML 使用 'code:' 字段，但 NucleiYamlObj.Code 类定义的是 'source' 字段
     */
    @Test
    public void testJavaScriptPocParsing() {
        System.out.println("========== 测试 JavaScript POC 解析 ==========");
        
        String nucleiPocDir = "src/main/resources/poc/nucleiPoc/javascript";
        File jsDir = new File(nucleiPocDir);
        
        if (!jsDir.exists()) {
            System.out.println("JavaScript POC 目录不存在: " + nucleiPocDir);
            return;
        }
        
        // 测试几个典型的 JavaScript POC
        String[] testFiles = {
            "detection/mssql-detect.yaml",
            "detection/samba-detect.yaml",
            "cves/2023/CVE-2023-46604.yaml"
        };
        
        for (String testFile : testFiles) {
            File pocFile = new File(jsDir, testFile);
            if (!pocFile.exists()) {
                System.out.println("文件不存在: " + pocFile.getAbsolutePath());
                continue;
            }
            
            System.out.println("\n--- 测试文件: " + testFile + " ---");
            
            // 步骤1: 直接用 SnakeYAML 解析，检查原始数据
            testRawYamlParsing(pocFile);
            
            // 步骤2: 用完整转换器解析
            testFullConversion(pocFile);
        }
    }
    
    /**
     * 直接解析 YAML 原始数据，检查字段名称
     */
    private void testRawYamlParsing(File pocFile) {
        System.out.println("\n[步骤1] 原始 YAML 解析:");
        try (FileInputStream fis = new FileInputStream(pocFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> rawData = yaml.load(fis);
            
            // 检查 javascript 字段
            Object jsField = rawData.get("javascript");
            if (jsField == null) {
                System.out.println("  javascript 字段: null");
                return;
            }
            
            System.out.println("  javascript 字段类型: " + jsField.getClass().getSimpleName());
            
            if (jsField instanceof List) {
                List<?> jsList = (List<?>) jsField;
                for (int i = 0; i < jsList.size(); i++) {
                    Object item = jsList.get(i);
                    System.out.println("  javascript[" + i + "] 类型: " + item.getClass().getSimpleName());
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        System.out.println("    字段列表: " + itemMap.keySet());
                        
                        // 关键检查: 'code' 还是 'source'?
                        if (itemMap.containsKey("code")) {
                            Object code = itemMap.get("code");
                            System.out.println("    [发现] 'code' 字段存在, 值长度: " + 
                                (code != null ? code.toString().length() : "null"));
                        }
                        if (itemMap.containsKey("source")) {
                            Object source = itemMap.get("source");
                            System.out.println("    [发现] 'source' 字段存在, 值长度: " + 
                                (source != null ? source.toString().length() : "null"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("  原始 YAML 解析异常: " + e.getMessage());
        }
    }
    
    /**
     * 使用完整转换器解析
     */
    private void testFullConversion(File pocFile) {
        System.out.println("\n[步骤2] NucleiYamlObj 解析:");
        try (FileInputStream fis = new FileInputStream(pocFile)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new NucleiConstructor(NucleiYamlObj.Poc.class, loaderOptions));
            NucleiYamlObj.Poc nucleiPoc = yaml.load(fis);
            
            if (nucleiPoc == null) {
                System.out.println("  NucleiYamlObj.Poc 解析结果为 null");
                return;
            }
            
            System.out.println("  POC ID: " + nucleiPoc.getId());
            
            // 检查 javascript 字段
            List<NucleiYamlObj.Code> jsList = nucleiPoc.getJavascript();
            if (jsList == null || jsList.isEmpty()) {
                System.out.println("  [问题] javascript 列表为空或 null");
                return;
            }
            
            System.out.println("  javascript 列表大小: " + jsList.size());
            for (int i = 0; i < jsList.size(); i++) {
                NucleiYamlObj.Code codeItem = jsList.get(i);
                System.out.println("  Code[" + i + "]:");
                System.out.println("    engine: " + codeItem.getEngine());
                System.out.println("    source: " + (codeItem.getSource() == null ? "[NULL - 这是问题根源!]" : 
                    "长度=" + codeItem.getSource().length()));
            }
            
            // 步骤3: 转换为 PocObj
            System.out.println("\n[步骤3] 转换为 PocObj:");
            NucleiPocConverter converter = new NucleiPocConverter();
            PocObj.Poc poc = converter.convert(nucleiPoc);
            
            if (poc == null) {
                System.out.println("  PocObj 转换结果为 null");
                return;
            }
            
            List<PocObj.PocStep> steps = poc.getVerifySteps();
            if (steps == null || steps.isEmpty()) {
                System.out.println("  [问题] verifySteps 为空");
                return;
            }
            
            for (PocObj.PocStep step : steps) {
                if (step instanceof PocObj.CodeStep) {
                    PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
                    System.out.println("  CodeStep:");
                    System.out.println("    engine: " + codeStep.getEngine());
                    System.out.println("    source: " + (codeStep.getSource() == null ? 
                        "[NULL - JavaScript 执行会失败!]" : "长度=" + codeStep.getSource().length()));
                }
            }
            
        } catch (Exception e) {
            System.err.println("  解析异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试问题2: DSL 表达式括号不匹配 - contains_any 函数
     */
    @Test
    public void testDslContainsAnyParsing() {
        System.out.println("========== 测试 DSL contains_any 解析 ==========");
        
        // 问题表达式来自 hyperplanning-panel.yaml
        String[] problematicExpressions = {
            // 原始问题表达式
            "contains_any(to_lower(body), \"hyperplanning</title>\", \"content=\\\"hyperplanning\\\"\")",
            // 简化测试
            "contains_any(body, \"test</title>\")",
            "contains_any(body, \"a\", \"b\", \"c\")",
            "contains_any(to_lower(body), \"a\", \"b\")",
            // 包含特殊字符的
            "contains_any(body, \"<title>test</title>\")",
            "contains_any(body, \"content=\\\"value\\\"\")",
        };
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("body", "<html><title>HYPERPLANNING</title><meta content=\"hyperplanning\"></html>");
        variables.put("status_code", 200);
        
        for (String expr : problematicExpressions) {
            System.out.println("\n--- 测试表达式: ---");
            System.out.println("  " + expr);
            try {
                boolean result = DslEvaluatorRefactored.evaluateDslExpression(expr, variables);
                System.out.println("  结果: " + result);
            } catch (Exception e) {
                System.err.println("  [异常] " + e.getMessage());
            }
        }
    }

    /**
     * 测试问题2扩展: 直接解析 hyperplanning-panel.yaml
     */
    @Test
    public void testHyperplanningPocParsing() {
        System.out.println("========== 测试 hyperplanning-panel.yaml 解析 ==========");
        
        String pocPath = "src/main/resources/poc/nucleiPoc/http/exposed-panels/hyperplanning-panel.yaml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("文件不存在: " + pocPath);
            return;
        }
        
        try {
            // 使用 PocConverterRegistry 加载 POC
            PocObj.Poc poc = PocConverterRegistry.getInstance().convertPocFile(pocPath);
            if (poc == null) {
                System.out.println("解析结果为 null");
                return;
            }
            
            System.out.println("POC ID: " + poc.getId());
            System.out.println("POC Name: " + poc.getName());
            
            // 检查 verify steps
            List<PocObj.PocStep> steps = poc.getVerifySteps();
            if (steps != null) {
                for (int i = 0; i < steps.size(); i++) {
                    PocObj.PocStep step = steps.get(i);
                    System.out.println("\nPocStep[" + i + "]: " + step.getClass().getSimpleName());
                    
                    // 检查 matchers
                    List<PocObj.Matcher> matchers = step.getMatchers();
                    if (matchers != null) {
                        for (int j = 0; j < matchers.size(); j++) {
                            PocObj.Matcher matcher = matchers.get(j);
                            System.out.println("  Matcher[" + j + "]:");
                            System.out.println("    type: " + matcher.getType());
                            if (matcher.getValues() != null && matcher.getType() == PocObj.MatcherType.DSL) {
                                System.out.println("    dsl expressions:");
                                for (String dsl : matcher.getValues()) {
                                    System.out.println("      - " + dsl);
                                }
                            }
                        }
                    }
                }
            }
            
            // 模拟执行 DSL 匹配
            System.out.println("\n--- 模拟执行 DSL 匹配 ---");
            Map<String, Object> variables = new HashMap<>();
            variables.put("body", "<html><title>HYPERPLANNING</title><meta content=\"hyperplanning\"></html>");
            variables.put("status_code", 200);
            
            if (steps != null && !steps.isEmpty()) {
                PocObj.PocStep step = steps.get(0);
                if (step.getMatchers() != null) {
                    for (PocObj.Matcher matcher : step.getMatchers()) {
                        if (matcher.getType() == PocObj.MatcherType.DSL && matcher.getValues() != null) {
                            for (String dslExpr : matcher.getValues()) {
                                System.out.println("执行 DSL: " + dslExpr);
                                try {
                                    boolean result = DslEvaluatorRefactored.evaluateDslExpression(dslExpr, variables);
                                    System.out.println("  结果: " + result);
                                } catch (Exception e) {
                                    System.err.println("  [异常] " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("解析异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试 JavaScript 执行 null code 的情况
     */
    @Test
    public void testJavaScriptNullCode() {
        System.out.println("========== 测试 JavaScript null code 处理 ==========");
        
        Map<String, Object> context = new HashMap<>();
        context.put("Host", "localhost");
        context.put("Port", 8080);
        
        // 测试 null code
        System.out.println("\n--- 测试 null code ---");
        try {
            CodeHandler.CodeResponse response = CodeHandler.executeJavaScript(null, context);
            System.out.println("执行结果: success=" + response.isSuccess() + ", error=" + response.getError());
        } catch (Exception e) {
            System.err.println("[异常] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        
        // 测试空字符串 code
        System.out.println("\n--- 测试空字符串 code ---");
        try {
            CodeHandler.CodeResponse response = CodeHandler.executeJavaScript("", context);
            System.out.println("执行结果: success=" + response.isSuccess() + ", error=" + response.getError());
        } catch (Exception e) {
            System.err.println("[异常] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        
        // 测试正常 code
        System.out.println("\n--- 测试正常 code ---");
        try {
            CodeHandler.CodeResponse response = CodeHandler.executeJavaScript("1 + 1", context);
            System.out.println("执行结果: success=" + response.isSuccess() + ", result=" + response.getResult());
        } catch (Exception e) {
            System.err.println("[异常] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 扫描所有可能有问题的 POC
     */
    @Test
    public void scanProblematicPocs() {
        System.out.println("========== 扫描可能有问题的 POC ==========");
        
        String basePath = "src/main/resources/poc/nucleiPoc";
        
        // 1. 扫描 JavaScript POCs
        System.out.println("\n=== JavaScript POCs ===");
        scanDirectory(new File(basePath, "javascript"), this::checkJavaScriptPoc);
        
        // 2. 扫描包含复杂 DSL 的 POCs
        System.out.println("\n=== 包含 contains_any 的 HTTP POCs (采样) ===");
        String[] samplePaths = {
            "http/exposed-panels/hyperplanning-panel.yaml",
            "http/exposed-panels/thruk-panel.yaml",
            "http/technologies/graylog/graylog-api-exposure.yaml"
        };
        for (String path : samplePaths) {
            File f = new File(basePath, path);
            if (f.exists()) {
                checkDslPoc(f);
            }
        }
    }
    
    private void scanDirectory(File dir, java.util.function.Consumer<File> checker) {
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("目录不存在: " + dir.getAbsolutePath());
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, checker);
            } else if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
                checker.accept(file);
                count++;
                if (count >= 10) { // 限制每个目录最多检查10个
                    System.out.println("  (达到限制，跳过剩余文件)");
                    break;
                }
            }
        }
    }
    
    private void checkJavaScriptPoc(File file) {
        try {
            PocObj.Poc poc = PocConverterRegistry.getInstance().convertPocFile(file.getAbsolutePath());
            if (poc == null) {
                System.out.println("[WARN] " + file.getName() + " - 解析返回 null");
                return;
            }
            
            List<PocObj.PocStep> steps = poc.getVerifySteps();
            if (steps == null || steps.isEmpty()) {
                System.out.println("[WARN] " + file.getName() + " - 没有 verifySteps");
                return;
            }
            
            boolean hasIssue = false;
            for (PocObj.PocStep step : steps) {
                if (step instanceof PocObj.CodeStep) {
                    PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
                    if (codeStep.getSource() == null || codeStep.getSource().trim().isEmpty()) {
                        hasIssue = true;
                        System.out.println("[ERROR] " + file.getName() + " - source 为空");
                        break;
                    }
                }
            }
            
            if (!hasIssue) {
                System.out.println("[OK] " + file.getName());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + file.getName() + " - " + e.getMessage());
        }
    }
    
    private void checkDslPoc(File file) {
        try {
            PocObj.Poc poc = PocConverterRegistry.getInstance().convertPocFile(file.getAbsolutePath());
            if (poc == null) {
                System.out.println("[WARN] " + file.getName() + " - 解析返回 null");
                return;
            }
            
            System.out.println("\n检查: " + file.getName());
            
            List<PocObj.PocStep> steps = poc.getVerifySteps();
            if (steps == null) return;
            
            Map<String, Object> testVars = new HashMap<>();
            testVars.put("body", "test content");
            testVars.put("status_code", 200);
            testVars.put("header", "test header");
            
            for (PocObj.PocStep step : steps) {
                if (step.getMatchers() == null) continue;
                
                for (PocObj.Matcher matcher : step.getMatchers()) {
                    if (matcher.getType() != PocObj.MatcherType.DSL || matcher.getValues() == null) continue;
                    
                    for (String dsl : matcher.getValues()) {
                        if (dsl.contains("contains_any")) {
                            System.out.println("  DSL: " + dsl);
                            try {
                                DslEvaluatorRefactored.evaluateDslExpression(dsl, testVars);
                                System.out.println("    [OK]");
                            } catch (Exception e) {
                                System.out.println("    [ERROR] " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + file.getName() + " - " + e.getMessage());
        }
    }
}
