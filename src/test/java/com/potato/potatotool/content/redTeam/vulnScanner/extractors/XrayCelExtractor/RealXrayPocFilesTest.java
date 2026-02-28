package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 Xray POC 文件测试
 * 
 * 直接加载项目中的真实 Xray POC 文件，验证 CEL 表达式解析功能
 * 
 * @author Potato
 * @date 2025-10-31
 */
@DisplayName("真实 Xray POC 文件测试")
public class RealXrayPocFilesTest {
    
    // Xray POC 目录路径（相对于项目根目录）
    private static final String XRAY_POC_DIR = "src/main/resources/poc/xrayPoc";
    
    /**
     * 提供所有 Xray POC 文件路径
     */
    static Stream<String> providePocFiles() {
        try {
            Path pocDir = Paths.get(XRAY_POC_DIR);
            if (!Files.exists(pocDir)) {
                System.err.println("POC 目录不存在: " + pocDir.toAbsolutePath());
                return Stream.empty();
            }
            
            return Files.walk(pocDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .map(Path::toString)
                    .sorted();
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }
    
    @Test
    @DisplayName("验证 POC 目录存在")
    public void testPocDirectoryExists() {
        File pocDir = new File(XRAY_POC_DIR);
        assertTrue(pocDir.exists(), "POC 目录应该存在: " + XRAY_POC_DIR);
        assertTrue(pocDir.isDirectory(), "应该是一个目录");
    }
    
    @Test
    @DisplayName("验证至少有一些 POC 文件")
    public void testPocFilesExist() {
        long count = providePocFiles().count();
        assertTrue(count > 0, "应该至少有一些 POC 文件，实际找到: " + count);
        System.out.println("找到 " + count + " 个 Xray POC 文件");
    }
    
    @ParameterizedTest
    @MethodSource("providePocFiles")
    @DisplayName("测试加载单个 POC 文件")
    public void testLoadPocFile(String pocPath) {
        assertDoesNotThrow(() -> {
            XrayYamlObj.Poc poc = PocConverter.loadXrayYamlPocFile(pocPath);
            assertNotNull(poc, "POC 对象不应该为 null: " + pocPath);
            
            // 打印基本信息
            System.out.println("\n========================================");
            System.out.println("POC 文件: " + new File(pocPath).getName());
            System.out.println("POC 名称: " + (poc.getName() != null ? poc.getName() : "未命名"));
            System.out.println("传输协议: " + (poc.getTransport() != null ? poc.getTransport() : "未指定"));
            
        }, "加载 POC 文件失败: " + pocPath);
    }
    
    @ParameterizedTest
    @MethodSource("providePocFiles")
    @DisplayName("测试解析 POC 中的 CEL 表达式")
    public void testParsePocCelExpressions(String pocPath) {
        try {
            XrayYamlObj.Poc poc = PocConverter.loadXrayYamlPocFile(pocPath);
            assertNotNull(poc, "POC 对象不应该为 null");
            
            // 收集所有的 CEL 表达式
            List<String> expressions = new ArrayList<>();
            
            // 1. 主表达式
            if (poc.getExpression() != null && !poc.getExpression().isEmpty()) {
                expressions.add(poc.getExpression());
            }
            
            // 2. 规则表达式
            if (poc.getRules() != null) {
                for (Map.Entry<String, Object> entry : poc.getRules().entrySet()) {
                    if (entry.getValue() instanceof LinkedHashMap) {
                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, Object> rule = (LinkedHashMap<String, Object>) entry.getValue();
                        Object exprObj = rule.get("expression");
                        if (exprObj instanceof String) {
                            String expr = (String) exprObj;
                            if (!expr.isEmpty()) {
                                expressions.add(expr);
                            }
                        }
                    }
                }
            }
            
            // 3. set 变量中的表达式
            if (poc.getSet() != null) {
                for (Object value : poc.getSet().values()) {
                    if (value instanceof String) {
                        String expr = (String) value;
                        // 检查是否包含函数调用
                        if (expr.contains("(") && expr.contains(")")) {
                            expressions.add(expr);
                        }
                    }
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("POC 文件: " + new File(pocPath).getName());
            System.out.println("找到 " + expressions.size() + " 个 CEL 表达式");
            
            // 验证每个表达式的语法
            int validCount = 0;
            int invalidCount = 0;
            
            for (String expr : expressions) {
                try {
                    // 只验证语法，不执行（因为没有上下文）
                    XrayCelParser.validateCelSyntax(expr);
                    validCount++;
                    System.out.println("  ✓ 有效: " + (expr.length() > 60 ? expr.substring(0, 60) + "..." : expr));
                } catch (Exception e) {
                    invalidCount++;
                    System.err.println("  ✗ 无效: " + expr);
                    System.err.println("    错误: " + e.getMessage());
                }
            }
            
            System.out.println("验证结果: " + validCount + " 有效, " + invalidCount + " 无效");
            
            // 如果有表达式，至少应该有一些是有效的
            if (expressions.size() > 0) {
                assertTrue(validCount > 0, 
                    "POC 文件中应该至少有一些有效的 CEL 表达式: " + pocPath);
            }
            
        } catch (IOException e) {
            fail("加载 POC 文件失败: " + pocPath + " - " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("统计 CEL 函数使用情况")
    public void testCelFunctionUsageStatistics() {
        Map<String, Integer> functionUsage = new HashMap<>();
        int totalPocs = 0;
        int totalExpressions = 0;
        
        try (Stream<String> pocFiles = providePocFiles()) {
            for (String pocPath : pocFiles.collect(Collectors.toList())) {
                try {
                    XrayYamlObj.Poc poc = PocConverter.loadXrayYamlPocFile(pocPath);
                    if (poc == null) continue;
                    
                    totalPocs++;
                    
                    // 收集表达式
                    List<String> expressions = new ArrayList<>();
                    if (poc.getExpression() != null) {
                        expressions.add(poc.getExpression());
                    }
                    if (poc.getRules() != null) {
                        for (Object ruleObj : poc.getRules().values()) {
                            if (ruleObj instanceof LinkedHashMap) {
                                @SuppressWarnings("unchecked")
                                LinkedHashMap<String, Object> rule = (LinkedHashMap<String, Object>) ruleObj;
                                Object exprObj = rule.get("expression");
                                if (exprObj instanceof String) {
                                    expressions.add((String) exprObj);
                                }
                            }
                        }
                    }
                    
                    totalExpressions += expressions.size();
                    
                    // 统计函数使用
                    for (String expr : expressions) {
                        // 简单的函数名提取（匹配 functionName( 格式）
                        String[] words = expr.split("\\s+|\\(|\\)|,|&&|\\|\\||==|!=|<=|>=|<|>");
                        for (String word : words) {
                            word = word.trim();
                            if (word.isEmpty()) continue;
                            
                            // 常见的 CEL 函数
                            if (isCommonCelFunction(word)) {
                                functionUsage.put(word, functionUsage.getOrDefault(word, 0) + 1);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    // 忽略解析失败的文件
                }
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("CEL 函数使用统计");
        System.out.println("========================================");
        System.out.println("总共分析: " + totalPocs + " 个 POC 文件");
        System.out.println("总共表达式: " + totalExpressions + " 个");
        System.out.println("\n函数使用频率 (Top 20):");
        
        functionUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> {
                    System.out.printf("  %-20s: %3d 次\n", entry.getKey(), entry.getValue());
                });
        
        // 至少应该统计到一些函数
        assertFalse(functionUsage.isEmpty(), "应该至少发现一些 CEL 函数使用");
    }
    
    /**
     * 判断是否为常见的 CEL 函数
     */
    private boolean isCommonCelFunction(String word) {
        Set<String> functions = new HashSet<>(Arrays.asList(
            // 字符串函数
            "contains", "startsWith", "endsWith", "matches", "submatch", 
            "len", "substr", "replaceAll", "toUpper", "toLower", "trim",
            
            // 编码函数
            "base64", "base64Decode", "urlencode", "urldecode", "hexEncode", "hexDecode",
            
            // 哈希函数
            "md5", "sha1", "sha256", "sha512", "mmh3",
            
            // 随机函数
            "randomInt", "randomLowercase", "randomUppercase", "rand_text_alpha",
            "rand_text_numeric", "rand_text_alphanumeric", "randomUUID",
            
            // 字节函数
            "bytes", "bcontains", "bmatches", "bsubmatch",
            
            // 高级函数
            "json_decode", "json_encode", "size", "in", "has", "get",
            "first", "last", "unique", "reverse", "sort", "keys", "values",
            "max", "min", "sum", "avg",
            
            // 工具函数
            "unixtime", "sleep", "string", "int", "icontains", "resolve", "uuid"
        ));
        
        return functions.contains(word);
    }
    
    @Test
    @DisplayName("测试特定 POC 文件的完整解析")
    public void testSpecificPocFile() throws IOException {
        // 选择一个具体的 POC 文件进行详细测试
        String pocPath = XRAY_POC_DIR + "/2024-7-xray/夏普Sharp 多功能打印机 任意文件读取漏洞/fileread.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("跳过测试: POC 文件不存在 " + pocPath);
            return;
        }
        
        XrayYamlObj.Poc poc = PocConverter.loadXrayYamlPocFile(pocPath);
        assertNotNull(poc);
        
        System.out.println("\n========================================");
        System.out.println("详细解析 POC: " + pocFile.getName());
        System.out.println("========================================");
        System.out.println("名称: " + poc.getName());
        System.out.println("手动: " + poc.isManual());
        System.out.println("传输: " + poc.getTransport());
        System.out.println("主表达式: " + poc.getExpression());
        
        // 解析规则
        if (poc.getRules() != null) {
            System.out.println("\n规则:");
            for (Map.Entry<String, Object> entry : poc.getRules().entrySet()) {
                System.out.println("  [" + entry.getKey() + "]");
                if (entry.getValue() instanceof LinkedHashMap) {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, Object> rule = (LinkedHashMap<String, Object>) entry.getValue();
                    
                    Object exprObj = rule.get("expression");
                    if (exprObj != null) {
                        System.out.println("    表达式: " + exprObj);
                        
                        // 验证表达式语法
                        try {
                            XrayCelParser.validateCelSyntax(exprObj.toString());
                            System.out.println("    ✓ 语法有效");
                        } catch (Exception e) {
                            System.err.println("    ✗ 语法错误: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // 验证主表达式
        if (poc.getExpression() != null && !poc.getExpression().isEmpty()) {
            try {
                XrayCelParser.validateCelSyntax(poc.getExpression());
                System.out.println("\n✓ 主表达式语法有效");
            } catch (Exception e) {
                System.err.println("\n✗ 主表达式语法错误: " + e.getMessage());
                fail("主表达式验证失败");
            }
        }
    }
}

