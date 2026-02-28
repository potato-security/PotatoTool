package com.potato.potatotool.content.redTeam.vulnScanner.goby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.GobyPocConverter;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Goby POC 集成测试
 * 测试完整的POC解析和转换流程，使用真实的Goby POC文件
 * 
 * @author Potato
 * @date 2025-11-01
 */
@DisplayName("Goby POC 集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GobyPocIntegrationTest {
    
    private static final String GOBY_POC_DIR = "src/main/resources/poc/gobyPoc";
    private static final int SAMPLE_SIZE = 50; // 测试前50个POC
    
    private static Gson gson;
    private static GobyPocConverter converter;
    private static List<TestResult> results;
    
    @BeforeAll
    public static void setUpAll() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        converter = new GobyPocConverter();
        results = new ArrayList<>();
        
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        Goby JsonPoc 集成测试套件                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }
    
    @AfterAll
    public static void tearDownAll() {
        printTestSummary();
    }
    
    /**
     * Java 8 兼容的字符串重复方法
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    @Test
    @Order(1)
    @DisplayName("测试真实Goby POC目录是否存在")
    public void testGobyPocDirectoryExists() {
        File dir = new File(GOBY_POC_DIR);
        assertTrue(dir.exists(), "Goby POC目录不存在: " + GOBY_POC_DIR);
        assertTrue(dir.isDirectory(), "路径不是目录: " + GOBY_POC_DIR);
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        assertNotNull(files, "无法读取目录");
        assertTrue(files.length > 0, "目录中没有JSON文件");
        
        System.out.println("✓ 找到 " + files.length + " 个Goby POC文件");
    }
    
    @Test
    @Order(2)
    @DisplayName("批量解析和转换真实Goby POC")
    public void testBatchRealGobyPocParsing() {
        File dir = new File(GOBY_POC_DIR);
        File[] pocFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        
        assertNotNull(pocFiles);
        
        System.out.println("\n开始批量测试，共 " + Math.min(SAMPLE_SIZE, pocFiles.length) + " 个POC文件");
        System.out.println(repeat("-", 80));
        
        int testCount = 0;
        int successCount = 0;
        int parseErrorCount = 0;
        int convertErrorCount = 0;
        
        for (File pocFile : pocFiles) {
            if (testCount >= SAMPLE_SIZE) {
                break;
            }
            
            testCount++;
            String fileName = pocFile.getName();
            TestResult result = new TestResult(fileName);
            
            try {
                // 读取JSON文件
                String jsonContent = new String(Files.readAllBytes(pocFile.toPath()));
                
                // 解析为Goby POC对象
                GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
                
                if (gobyPoc == null) {
                    result.setError("解析结果为null");
                    result.setStatus(TestStatus.PARSE_FAILED);
                    parseErrorCount++;
                    results.add(result);
                    continue;
                }
                
                // 转换为通用POC对象
                PocObj.Poc poc = converter.convert(gobyPoc);
                
                if (poc == null) {
                    result.setError("转换结果为null");
                    result.setStatus(TestStatus.CONVERT_FAILED);
                    convertErrorCount++;
                    results.add(result);
                    continue;
                }
                
                // 验证转换后的POC
                String validationError = validatePoc(poc);
                if (validationError != null) {
                    result.setError("验证失败: " + validationError);
                    result.setStatus(TestStatus.VALIDATION_FAILED);
                    results.add(result);
                    continue;
                }
                
                // 测试成功
                result.setStatus(TestStatus.SUCCESS);
                result.setPocName(poc.getName());
                result.setSeverity(poc.getSeverity());
                result.setStepCount(poc.getVerifySteps() != null ? poc.getVerifySteps().size() : 0);
                successCount++;
                results.add(result);
                
                if (testCount % 10 == 0) {
                    System.out.printf("进度: %d/%d (成功: %d, 失败: %d)\n", 
                        testCount, Math.min(SAMPLE_SIZE, pocFiles.length),
                        successCount, testCount - successCount);
                }
                
            } catch (com.google.gson.JsonSyntaxException e) {
                result.setError("JSON语法错误: " + e.getMessage());
                result.setStatus(TestStatus.PARSE_FAILED);
                parseErrorCount++;
                results.add(result);
            } catch (Exception e) {
                result.setError("未知错误: " + e.getMessage());
                result.setStatus(TestStatus.OTHER_ERROR);
                results.add(result);
            }
        }
        
        System.out.println(repeat("-", 80));
        System.out.println("\n批量测试完成:");
        System.out.println("  总计: " + testCount);
        System.out.println("  成功: " + successCount);
        System.out.println("  解析失败: " + parseErrorCount);
        System.out.println("  转换失败: " + convertErrorCount);
        System.out.println("  其他错误: " + (testCount - successCount - parseErrorCount - convertErrorCount));
        
        // 断言成功率至少80%
        double successRate = (double) successCount / testCount * 100;
        assertTrue(successRate >= 80.0, 
            String.format("成功率过低: %.2f%% (期望 >= 80%%)", successRate));
    }
    
    @Test
    @Order(3)
    @DisplayName("测试特定复杂POC - 多步骤")
    public void testComplexMultiStepPoc() throws Exception {
        File pocFile = findPocByPattern("VMWare.*CVE");
        if (pocFile == null) {
            System.out.println("跳过测试：未找到匹配的POC文件");
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(pocFile.toPath()));
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        System.out.println("✓ 成功转换复杂多步骤POC: " + poc.getName());
        
        if (poc.getVerifySteps() != null) {
            System.out.println("  验证步骤数: " + poc.getVerifySteps().size());
        }
        if (poc.getExploitSteps() != null) {
            System.out.println("  利用步骤数: " + poc.getExploitSteps().size());
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("测试特定复杂POC - Log4Shell")
    public void testLog4ShellPoc() throws Exception {
        File pocFile = findPocByPattern(".*Log4[Ss]hell.*");
        if (pocFile == null) {
            System.out.println("跳过测试：未找到Log4Shell POC");
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(pocFile.toPath()));
        GobyJsonObj.PocJson gobyPoc = gson.fromJson(jsonContent, GobyJsonObj.PocJson.class);
        PocObj.Poc poc = converter.convert(gobyPoc);
        
        assertNotNull(poc);
        System.out.println("✓ 成功转换Log4Shell POC: " + poc.getName());
        // 注意：实际POC的严重等级可能不同，这里只验证转换成功
        System.out.println("  严重等级: " + poc.getSeverity());
    }
    
    @Test
    @Order(5)
    @DisplayName("测试统计分析 - 严重等级分布")
    public void testSeverityDistribution() {
        Map<PocObj.Severity, Integer> distribution = new HashMap<>();
        
        for (TestResult result : results) {
            if (result.getStatus() == TestStatus.SUCCESS && result.getSeverity() != null) {
                distribution.put(result.getSeverity(), 
                    distribution.getOrDefault(result.getSeverity(), 0) + 1);
            }
        }
        
        System.out.println("\n严重等级分布:");
        distribution.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> {
                System.out.printf("  %s: %d\n", entry.getKey(), entry.getValue());
            });
    }
    
    @Test
    @Order(6)
    @DisplayName("测试统计分析 - 步骤数量分布")
    public void testStepCountDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();
        
        for (TestResult result : results) {
            if (result.getStatus() == TestStatus.SUCCESS) {
                int stepCount = result.getStepCount();
                distribution.put(stepCount, 
                    distribution.getOrDefault(stepCount, 0) + 1);
            }
        }
        
        System.out.println("\n步骤数量分布:");
        distribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.printf("  %d步: %d个POC\n", entry.getKey(), entry.getValue());
            });
    }
    
    /**
     * 验证POC对象的完整性
     */
    private String validatePoc(PocObj.Poc poc) {
        if (poc.getId() == null || poc.getId().isEmpty()) {
            return "缺少ID";
        }
        
        if (poc.getName() == null || poc.getName().isEmpty()) {
            return "缺少Name";
        }
        
        if ((poc.getVerifySteps() == null || poc.getVerifySteps().isEmpty()) &&
            (poc.getExploitSteps() == null || poc.getExploitSteps().isEmpty())) {
            return "既没有验证步骤也没有利用步骤";
        }
        
        return null;
    }
    
    /**
     * 查找匹配模式的POC文件
     */
    private File findPocByPattern(String pattern) {
        File dir = new File(GOBY_POC_DIR);
        File[] files = dir.listFiles((d, name) -> 
            name.endsWith(".json") && name.matches(pattern));
        
        return (files != null && files.length > 0) ? files[0] : null;
    }
    
    /**
     * 打印测试摘要
     */
    private static void printTestSummary() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("集成测试摘要");
        System.out.println(repeat("=", 80));
        
        long successCount = results.stream()
            .filter(r -> r.getStatus() == TestStatus.SUCCESS)
            .count();
        
        long parseErrors = results.stream()
            .filter(r -> r.getStatus() == TestStatus.PARSE_FAILED)
            .count();
        
        long convertErrors = results.stream()
            .filter(r -> r.getStatus() == TestStatus.CONVERT_FAILED)
            .count();
        
        long validationErrors = results.stream()
            .filter(r -> r.getStatus() == TestStatus.VALIDATION_FAILED)
            .count();
        
        System.out.println("总测试数: " + results.size());
        System.out.println("成功: " + successCount);
        System.out.println("解析失败: " + parseErrors);
        System.out.println("转换失败: " + convertErrors);
        System.out.println("验证失败: " + validationErrors);
        
        if (!results.isEmpty()) {
            double successRate = (double) successCount / results.size() * 100;
            System.out.printf("成功率: %.2f%%\n", successRate);
        }
        
        System.out.println(repeat("=", 80));
    }
    
    /**
     * 测试结果
     */
    static class TestResult {
        private final String fileName;
        private TestStatus status;
        private String error;
        private String pocName;
        private PocObj.Severity severity;
        private int stepCount;
        
        public TestResult(String fileName) {
            this.fileName = fileName;
        }
        
        // Getters and Setters
        public String getFileName() { return fileName; }
        public TestStatus getStatus() { return status; }
        public void setStatus(TestStatus status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getPocName() { return pocName; }
        public void setPocName(String pocName) { this.pocName = pocName; }
        public PocObj.Severity getSeverity() { return severity; }
        public void setSeverity(PocObj.Severity severity) { this.severity = severity; }
        public int getStepCount() { return stepCount; }
        public void setStepCount(int stepCount) { this.stepCount = stepCount; }
    }
    
    /**
     * 测试状态
     */
    enum TestStatus {
        SUCCESS,
        PARSE_FAILED,
        CONVERT_FAILED,
        VALIDATION_FAILED,
        OTHER_ERROR
    }
}

