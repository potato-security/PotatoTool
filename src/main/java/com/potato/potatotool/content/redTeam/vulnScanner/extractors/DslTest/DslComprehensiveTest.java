package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.*;

public class DslComprehensiveTest {
    public static void main(String[] args) {
        System.out.println("=== DSL 函数全面测试 ===");
        
        // 创建模拟的响应和请求对象
        Map<String, Object> response = new HashMap<>();
        response.put("body", "Hello World! Version 2.1.0 encoded: SGVsbG8gV29ybGQ=");
        response.put("headers", "Content-Type: application/json");
        response.put("status_code", 200);
        response.put("content_length", 42);
        
        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://example.com/api");
        request.put("method", "GET");
        
        // 测试所有基本函数
        testBasicFunctions(response, request);
        
        // 测试否定函数
        testNegationFunctions(response, request);
        
        // 测试复合表达式
        testCompositeExpressions(response, request);
        
        System.out.println("\n=== 所有测试完成 ===");
    }
    
    private static void testBasicFunctions(Object response, Object request) {
        System.out.println("\n--- 基本函数测试 ---");
        
        // contains 函数
        testExpression("contains(body, \"Hello\")", response, request, "contains函数");
        
        // contains_all 函数
        testExpression("contains_all(body, \"Hello\", \"World\")", response, request, "contains_all函数");
        
        // contains_any 函数
        testExpression("contains_any(body, \"Hello\", \"Goodbye\")", response, request, "contains_any函数");
        
        // regex 函数
        testExpression("regex(\"Version \\\\d+\\.\\\\d+\\.\\\\d+\", body)", response, request, "regex函数");
        
        // compare_versions 函数
        testExpression("compare_versions(\"2.1.0\", \">=2.0.0\")", response, request, "compare_versions函数");
        
        // concat 函数
        testExpression("concat(\"Hello\", \" World\")", response, request, "concat函数");
        
        // len 函数
        testExpression("len(body)", response, request, "len函数");
        
        // md5 函数
        testExpression("md5(\"test\")", response, request, "md5函数");
        
        // mmh3 函数
        testExpression("mmh3(\"test\")", response, request, "mmh3函数");
        
        // base64_decode 函数
        testExpression("base64_decode(\"SGVsbG8gV29ybGQ=\")", response, request, "base64_decode函数");
        
        // base64_py 函数
        testExpression("base64_py(\"Hello World\")", response, request, "base64_py函数");
        
        // replace 函数
        testExpression("replace(body, \"Hello\", \"Hi\")", response, request, "replace函数");
        
        // startswith 函数
        testExpression("startswith(body, \"Hello\")", response, request, "startswith函数");
        
        // to_lower 函数
        testExpression("to_lower(\"HELLO\")", response, request, "to_lower函数");
        
        // tolower 函数
        testExpression("tolower(\"HELLO\")", response, request, "tolower函数");
        
        // toupper 函数
        testExpression("toupper(\"hello\")", response, request, "toupper函数");
        
        // to_number 函数
        testExpression("to_number(\"123\")", response, request, "to_number函数");
        
        // to_string 函数
        testExpression("to_string(123)", response, request, "to_string函数");
        
        // tostring 函数
        testExpression("tostring(123)", response, request, "tostring函数");
        
        // to_unix_time 函数
        testExpression("to_unix_time(\"2023-01-01 00:00:00\", \"yyyy-MM-dd HH:mm:ss\")", response, request, "to_unix_time函数");
        
        // trim 函数
        testExpression("trim(\"  hello  \", \" \")", response, request, "trim函数");
        
        // trim_space 函数
        testExpression("trim_space(\"  hello  \")", response, request, "trim_space函数");
        
        // trim_suffix 函数
        testExpression("trim_suffix(\"hello.txt\", \".txt\")", response, request, "trim_suffix函数");
        
        // unixtime 函数
        testExpression("unixtime()", response, request, "unixtime函数");
        
        // urldecode 函数
        testExpression("urldecode(\"hello%20world\")", response, request, "urldecode函数");
    }
    
    private static void testNegationFunctions(Object response, Object request) {
        System.out.println("\n--- 否定函数测试 ---");
        
        // !contains 函数
        testExpression("!contains(body, \"Goodbye\")", response, request, "!contains函数");
        
        // !contains_all 函数
        testExpression("!contains_all(body, \"Hello\", \"Goodbye\")", response, request, "!contains_all函数");
        
        // !contains_any 函数
        testExpression("!contains_any(body, \"Goodbye\", \"Farewell\")", response, request, "!contains_any函数");
        
        // !regex 函数
        testExpression("!regex(\"Version \\\\d+\\.\\\\d+\\.\\\\d+\\.\\\\d+\", body)", response, request, "!regex函数");
    }
    
    private static void testCompositeExpressions(Object response, Object request) {
        System.out.println("\n--- 复合表达式测试 ---");
        
        // AND 复合表达式
        testExpression("contains(body, \"Hello\") and contains(body, \"World\")", response, request, "AND复合表达式");
        
        // OR 复合表达式
        testExpression("contains(body, \"Hello\") or contains(body, \"Goodbye\")", response, request, "OR复合表达式");
        
        // 否定 + AND 复合表达式
        testExpression("!contains(body, \"Goodbye\") and contains(body, \"Hello\")", response, request, "否定+AND复合表达式");
        
        // 否定 + OR 复合表达式
        testExpression("!contains(body, \"Goodbye\") or !contains(body, \"Farewell\")", response, request, "否定+OR复合表达式");
        
        // 括号分组表达式
        testExpression("(contains(body, \"Hello\") and contains(body, \"World\")) or contains(body, \"Version\")", response, request, "括号分组表达式");
        
        // 复杂嵌套表达式
        testExpression("contains(body, \"Hello\") and (regex(\"Version \\\\d+\\.\\\\d+\\.\\\\d+\", body) or !contains(body, \"Error\"))", response, request, "复杂嵌套表达式");
        
        // 多函数组合表达式
        testExpression("len(body) > 10 and startswith(body, \"Hello\") and !contains(body, \"Error\")", response, request, "多函数组合表达式");
        
        // 字符串处理函数组合
        testExpression("contains(to_lower(body), \"hello\") and len(trim_space(body)) > 0", response, request, "字符串处理函数组合");
    }
    
    private static void testExpression(String expression, Object response, Object request, String testName) {
        try {
            boolean result = DslEvaluatorRefactored.evaluateSingleDsl(expression, response, request);
            System.out.println(String.format("✓ %s: %s -> %s", testName, expression, result));
        } catch (Exception e) {
            System.out.println(String.format("✗ %s: %s -> 错误: %s", testName, expression, e.getMessage()));
        }
    }

    public static class DslFunctionSupportReport {
        public static void main(String[] args) {
            System.out.println("=== DSL 函数支持情况报告 ===");

            // 要测试的函数列表
            String[] testFunctions = {
                "contains", "contains_all", "contains_any", "regex", "compare_versions",
                "concat", "len", "md5", "mmh3", "base64_decode", "base64_py",
                "replace", "startswith", "to_lower", "tolower", "toupper",
                "to_number", "to_string", "tostring", "to_unix_time", "trim",
                "trim_space", "trim_suffix", "unixtime", "urldecode"
            };

            // 当前已知支持的函数列表
            List<String> knownFunctions = Arrays.asList(DslConstants.KNOWN_FUNCTIONS);

            System.out.println("\n--- 函数支持状态检查 ---");

            List<String> supportedFunctions = new ArrayList<>();
            List<String> unsupportedFunctions = new ArrayList<>();
            List<String> aliasIssues = new ArrayList<>();

            for (String function : testFunctions) {
                if (knownFunctions.contains(function)) {
                    supportedFunctions.add(function);
                    System.out.println("✓ " + function + " - 已支持");
                } else {
                    // 检查是否有别名
                    String alias = checkFunctionAlias(function, knownFunctions);
                    if (alias != null) {
                        aliasIssues.add(function + " (别名: " + alias + ")");
                        System.out.println("⚠ " + function + " - 未直接支持，但有别名: " + alias);
                    } else {
                        unsupportedFunctions.add(function);
                        System.out.println("✗ " + function + " - 未支持");
                    }
                }
            }

            // 测试否定函数支持
            System.out.println("\n--- 否定函数支持测试 ---");
            testNegationSupport();

            // 生成总结报告
            generateSummaryReport(supportedFunctions, unsupportedFunctions, aliasIssues);

            // 实际运行测试验证
            System.out.println("\n--- 实际功能验证 ---");
            performActualTests();
        }

        private static String checkFunctionAlias(String function, List<String> knownFunctions) {
            // 检查常见的别名映射
            Map<String, String> aliasMap = new HashMap<>();
            aliasMap.put("compare_versions", "version_compare");
            aliasMap.put("to_lower", "toLowerCase");
            aliasMap.put("to_upper", "toUpperCase");

            String alias = aliasMap.get(function);
            if (alias != null && knownFunctions.contains(alias)) {
                return alias;
            }
            return null;
        }

        private static void testNegationSupport() {
            // 创建测试数据
            Map<String, Object> response = new HashMap<>();
            response.put("body", "Hello World");

            Map<String, Object> request = new HashMap<>();
            request.put("url", "https://example.com");

            // 测试基本否定函数
            String[] negationTests = {
                "!contains(body, \"Goodbye\")",
                "!contains_all(body, \"Hello\", \"Goodbye\")",
                "!contains_any(body, \"Goodbye\", \"Farewell\")",
                "!regex(\"\\\\d+\", body)"
            };

            for (String test : negationTests) {
                try {
                    boolean result = DslEvaluatorRefactored.evaluateSingleDsl(test, response, request);
                    System.out.println("✓ 否定函数测试: " + test + " -> " + result);
                } catch (Exception e) {
                    System.out.println("✗ 否定函数测试失败: " + test + " -> " + e.getMessage());
                }
            }
        }

        private static void generateSummaryReport(List<String> supported, List<String> unsupported, List<String> aliases) {
            System.out.println("\n=== 总结报告 ===");
            System.out.println("总测试函数数量: " + (supported.size() + unsupported.size() + aliases.size()));
            System.out.println("直接支持的函数: " + supported.size());
            System.out.println("有别名的函数: " + aliases.size());
            System.out.println("未支持的函数: " + unsupported.size());

            if (!supported.isEmpty()) {
                System.out.println("\n✓ 直接支持的函数:");
                for (String func : supported) {
                    System.out.println("  - " + func);
                }
            }

            if (!aliases.isEmpty()) {
                System.out.println("\n⚠ 有别名的函数:");
                for (String func : aliases) {
                    System.out.println("  - " + func);
                }
            }

            if (!unsupported.isEmpty()) {
                System.out.println("\n✗ 未支持的函数:");
                for (String func : unsupported) {
                    System.out.println("  - " + func);
                }
            }
        }

        private static void performActualTests() {
            Map<String, Object> response = new HashMap<>();
            response.put("body", "Hello World Version 2.1.0");
            response.put("status_code", 200);

            Map<String, Object> request = new HashMap<>();
            request.put("url", "https://example.com");

            // 测试确认支持的函数
            String[] workingTests = {
                "contains(body, \"Hello\")",
                "len(body)",
                "md5(\"test\")",
                "startswith(body, \"Hello\")",
                "to_lower(\"HELLO\")",
                "tolower(\"HELLO\")",
                "toupper(\"hello\")"
            };

            System.out.println("\n验证确认工作的函数:");
            for (String test : workingTests) {
                try {
                    boolean result = DslEvaluatorRefactored.evaluateSingleDsl(test, response, request);
                    System.out.println("✓ " + test + " -> " + result);
                } catch (Exception e) {
                    System.out.println("✗ " + test + " -> 错误: " + e.getMessage());
                }
            }

            // 测试复合表达式
            System.out.println("\n验证复合表达式:");
            String[] compositeTests = {
                "contains(body, \"Hello\") and len(body) > 5",
                "!contains(body, \"Goodbye\") or startswith(body, \"Hello\")",
                "(contains(body, \"Hello\") and contains(body, \"World\")) or contains(body, \"Version\")",
                    "(contains(body, \"Hello\") and (contains(body, \"World\")) or contains(body, \"Version\"))",
                    "(contains(body, \"Hello\") and contains(body, \"xx\")) or contains(body, \"Version\")",
                    "(contains(body, \"Hello\") and (contains(body, \"xx\")) or contains(body, \"Version\"))"
            };

            for (String test : compositeTests) {
                try {
                    boolean result = DslEvaluatorRefactored.evaluateSingleDsl(test, response, request);
                    System.out.println("✓ " + test + " -> " + result);
                } catch (Exception e) {
                    System.out.println("✗ " + test + " -> 错误: " + e.getMessage());
                }
            }
        }
    }
}