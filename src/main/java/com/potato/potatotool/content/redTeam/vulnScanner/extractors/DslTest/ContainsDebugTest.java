package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslExpressionParser;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslFunctionEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslUtils;

import java.util.HashMap;
import java.util.Map;

public class ContainsDebugTest {
    public static void main(String[] args) {
        // 创建测试上下文
        Map<String, Object> context = new HashMap<>();
        context.put("body", "Hello World! This is a test response with version 2.1.0 and status success");
        
        System.out.println("=== 调试contains(to_lower(body), \"hello\")问题 ===");
        
        String body = (String) context.get("body");
        System.out.println("原始body: " + body);
        System.out.println("原始body长度: " + body.length());
        
        // 手动测试
        String lowerBody = body.toLowerCase();
        System.out.println("手动转换后的body: " + lowerBody);
        System.out.println("手动检查是否包含'hello': " + lowerBody.contains("hello"));
        
        // 测试DslEvaluatorRefactored.resolveValueOrFunction
        String resolvedToLower = DslEvaluatorRefactored.resolveValueOrFunction("to_lower(body)", context);
        System.out.println("resolveValueOrFunction(\"to_lower(body)\") 结果: " + resolvedToLower);
        System.out.println("resolveValueOrFunction结果是否包含'hello': " + (resolvedToLower != null && resolvedToLower.contains("hello")));
        
        // 测试DslFunctionEvaluator.evaluateContainsFunction
        boolean containsResult = DslFunctionEvaluator.evaluateContainsFunction("contains(to_lower(body), \"hello\")", context);
        System.out.println("DslFunctionEvaluator.evaluateContainsFunction结果: " + containsResult);
        
        // 测试DslEvaluatorRefactored.evaluateDslExpression
        boolean dslResult = DslEvaluatorRefactored.evaluateDslExpression("contains(to_lower(body), \"hello\")", context);
        System.out.println("DslEvaluatorRefactored.evaluateDslExpression结果: " + dslResult);
        
        // 分步调试DslFunctionEvaluator.evaluateContainsFunction
        System.out.println("\n--- 分步调试DslFunctionEvaluator.evaluateContainsFunction ---");
        String[] args1 = DslExpressionParser.parseContainsFunction("contains(to_lower(body), \"hello\")");
        System.out.println("parseContainsFunction结果:");
        for (int i = 0; i < args1.length; i++) {
            System.out.println("  args[" + i + "]: " + args1[i]);
        }
        
        if (args1.length >= 2) {
            String target = DslFunctionEvaluator.resolveValue(args1[0], context);
            String searchValue = DslUtils.cleanStringValue(args1[1]);
            System.out.println("DslFunctionEvaluator.resolveValue(\"" + args1[0] + "\") 结果: " + target);
            System.out.println("DslUtils.cleanStringValue(\"" + args1[1] + "\") 结果: " + searchValue);
            
            if (target != null && searchValue != null) {
                boolean result = target.contains(searchValue);
                System.out.println("target.contains(searchValue): " + result);
            }
        }
        
        // 测试DslUtils.cleanStringValue
        System.out.println("\n--- 测试DslUtils.cleanStringValue ---");
        String[] testValues = {"\"hello\"", "'hello'", "hello"};
        for (String testValue : testValues) {
            String cleaned = DslUtils.cleanStringValue(testValue);
            System.out.println("cleanStringValue(\"" + testValue + "\") = \"" + cleaned + "\"");
        }
    }
}