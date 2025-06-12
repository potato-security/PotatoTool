package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslTest;

import java.util.regex.Pattern;

public class RegexDebugTest {
    public static void main(String[] args) {
        String text = "Hello World! This is a test response with version 2.1.0 and status success";
        
        // 测试不同的正则表达式模式
        String[] patterns = {
            "version \\d+\\.\\d+\\.\\d+",  // 双重转义
            "version \\d+\\.\\d+\\.\\d+",   // 四重转义
            "version [0-9]+\\.[0-9]+\\.[0-9]+", // 使用字符类
            "version \\d+\\.\\d+\\.\\d+"     // 标准转义
        };
        
        System.out.println("测试文本: " + text);
        System.out.println();
        
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            System.out.println("模式 " + (i+1) + ": " + pattern);
            
            try {
                Pattern p = Pattern.compile(pattern);
                boolean matches = p.matcher(text).find();
                System.out.println("匹配结果: " + matches);
                
                if (matches) {
                    java.util.regex.Matcher matcher = p.matcher(text);
                    if (matcher.find()) {
                        System.out.println("匹配内容: " + matcher.group());
                    }
                }
            } catch (Exception e) {
                System.out.println("错误: " + e.getMessage());
            }
            System.out.println();
        }
        
        // 测试简单的模式
        System.out.println("=== 简单测试 ===");
        String simplePattern = "version";
        Pattern p = Pattern.compile(simplePattern);
        System.out.println("简单模式 'version' 匹配: " + p.matcher(text).find());
        
        // 测试数字模式
        String numberPattern = "\\d+\\.\\d+\\.\\d+";
        Pattern np = Pattern.compile(numberPattern);
        System.out.println("数字模式 '" + numberPattern + "' 匹配: " + np.matcher(text).find());
    }
}