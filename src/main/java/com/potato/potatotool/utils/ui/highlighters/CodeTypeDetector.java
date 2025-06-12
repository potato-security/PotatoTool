package com.potato.potatotool.utils.ui.highlighters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 代码类型检测器
 * 通过分析文件名、扩展名和代码内容来智能检测代码类型
 * @author Potato
 */
public class CodeTypeDetector {
    
    // 文件扩展名到语言的映射
    private static final Map<String, String> EXTENSION_MAP = new HashMap<>();
    
    // 初始化扩展名映射
    static {
        // Java相关
        EXTENSION_MAP.put("java", "java");
        EXTENSION_MAP.put("gradle", "groovy");
        
        // Web相关
        EXTENSION_MAP.put("js", "javascript");
        EXTENSION_MAP.put("html", "html");
        EXTENSION_MAP.put("htm", "html");
        EXTENSION_MAP.put("xml", "xml");
        EXTENSION_MAP.put("css", "css");
        
        // 数据格式
        EXTENSION_MAP.put("json", "json");
        EXTENSION_MAP.put("yaml", "yaml");
        EXTENSION_MAP.put("yml", "yaml");
        EXTENSION_MAP.put("properties", "properties");
        
        // 脚本语言
        EXTENSION_MAP.put("py", "python");
        EXTENSION_MAP.put("groovy", "groovy");
        EXTENSION_MAP.put("sh", "shell");
        EXTENSION_MAP.put("bash", "shell");
        EXTENSION_MAP.put("ijm", "imagej");

        // 其他语言
        EXTENSION_MAP.put("md", "markdown");
        EXTENSION_MAP.put("md-nomarkers", "markdown-nomarkers"); // 无标记符版本的Markdown
    }
    
    // 文件头部特征模式
    private static final Pattern PYTHON_SHEBANG = Pattern.compile("^#!/usr/bin/env\\s+python");
    private static final Pattern SHELL_SHEBANG = Pattern.compile("^#!/bin/(ba)?sh");
    private static final Pattern HTML_PATTERN = Pattern.compile("<!DOCTYPE\\s+html|<html", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_PATTERN = Pattern.compile("<\\?xml\\s+version");
    private static final Pattern JAVA_PACKAGE = Pattern.compile("package\\s+[\\w\\.]+;");

    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("(function\\s+\\w+\\s*\\(|const\\s+\\w+\\s*=|let\\s+\\w+\\s*=|var\\s+\\w+\\s*=|=>\\s*\\{|\\$\\(\\s*function\\s*\\(|export\\s+|import\\s+.*from\\s+)");
    private static final Pattern CSS_PATTERN = Pattern.compile("(\\w+\\s*\\{\\s*[\\w-]+\\s*:\\s*[^;]+;|\\.[\\w-]+\\s*\\{\\s*[\\w-]+\\s*:\\s*[^;]+;|@media\\s+[^{]+\\{|@import\\s+url\\(|@keyframes\\s+[\\w-]+\\s*\\{|@font-face\\s*\\{\\s*[\\w-]+\\s*:)");
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\s*\\{\\s*\"|\\[\\s*\\{");
    private static final Pattern YAML_PATTERN = Pattern.compile("^---\\s*$");
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("^#\\s+|^##\\s+|^\\*\\*|^\\*[^*]|^_[^_]|^\\[.*\\]\\(.*\\)|^>\\s+|^-\\s+|^\\d+\\.\\s+|^```");

    /**
     * 根据文件名检测代码类型
     * @param fileName 文件名
     * @return 检测到的语言类型，如果无法检测则返回null
     */
    public String detectFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "java";
        }
        
        // 获取文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            String detectedType = EXTENSION_MAP.get(extension);
            return detectedType != null ? detectedType : "java";
        }
        
        return "java";
    }
    
    /**
     * 根据代码内容检测代码类型
     * @param content 代码内容
     * @return 检测到的语言类型，如果无法检测则返回null
     */
    public String detectFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "java";
        }
        
        // 获取前几行进行分析
        String firstLines = getFirstLines(content, 10);
        
        // 检查文件头部特征
        if (PYTHON_SHEBANG.matcher(firstLines).find()) {
            return "python";
        } else if (SHELL_SHEBANG.matcher(firstLines).find()) {
            return "shell";
        } else if (HTML_PATTERN.matcher(firstLines).find()) {
            return "html";
        } else if (XML_PATTERN.matcher(firstLines).find()) {
            return "xml";
        } else if (JAVA_PACKAGE.matcher(firstLines).find()) {
            return "java";
        } else if (MARKDOWN_PATTERN.matcher(firstLines).find()) {
            return "markdown";
        }
        
        // 基于内容特征的统计分析
        return analyzeContentFeatures(content);
    }
    
    /**
     * 综合文件名和内容检测代码类型
     * @param fileName 文件名
     * @param content 代码内容
     * @return 检测到的语言类型，默认返回"java"
     */
    public String detect(String fileName, String content) {
        try {
            // 首先尝试从文件名检测
            String typeFromName = detectFromFileName(fileName);
            if (typeFromName != null && !typeFromName.isEmpty()) {
                return typeFromName;
            }
            
            // 然后尝试从内容检测
            String typeFromContent = detectFromContent(content);
            if (typeFromContent != null && !typeFromContent.isEmpty()) {
                return typeFromContent;
            }
            
            // 默认返回Java
            return "java";
        } catch (Exception e) {
            return "java";
        }
    }
    
    /**
     * 获取文本的前n行
     * @param text 文本内容
     * @param lineCount 行数
     * @return 前n行文本
     */
    private String getFirstLines(String text, int lineCount) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\n", lineCount + 1);
        
        for (int i = 0; i < Math.min(lines.length, lineCount); i++) {
            result.append(lines[i]).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * 分析代码内容特征
     * @param content 代码内容
     * @return 检测到的语言类型，如果无法确定则返回null
     */
    private String analyzeContentFeatures(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // 获取前几行进行分析
        String firstLines = getFirstLines(content, 10);
        
        // 使用正则模式匹配
        if (JAVASCRIPT_PATTERN.matcher(firstLines).find()) {
            return "javascript";
        } else if (JSON_PATTERN.matcher(firstLines).find()) {
            return "json";
        } else if (CSS_PATTERN.matcher(firstLines).find()) {
            return "css";
        } else if (YAML_PATTERN.matcher(firstLines).find()) {
            return "yaml";
        } else if (MARKDOWN_PATTERN.matcher(firstLines).find()) {
            return "markdown";
        }
        
        // 特征计数器
        Map<String, Integer> scores = new HashMap<>();
        scores.put("java", 0);
        scores.put("python", 0);
        scores.put("javascript", 0);
        scores.put("xml", 0);
        scores.put("yaml", 0);
        scores.put("json", 0);
        scores.put("markdown", 0);
        
        // Java特征
        if (content.contains("public class") || content.contains("private static") || content.contains("protected")) {
            scores.put("java", scores.get("java") + 5);
        }
        if (content.contains("import java.") || content.contains("extends") || content.contains("implements")) {
            scores.put("java", scores.get("java") + 4);
        }
        if (content.contains("@Override") || content.contains("public void") || content.contains("public static")) {
            scores.put("java", scores.get("java") + 4);
        }
        if (content.contains("new ") && content.contains(";")) {
            scores.put("java", scores.get("java") + 3);
        }
        if (content.contains("System.out.") || content.contains("try {") || content.contains("catch (")) {
            scores.put("java", scores.get("java") + 3);
        }
        
        // Python特征
        if (content.contains("def ") || content.contains("import ")) {
            scores.put("python", scores.get("python") + 2);
        }
        if (content.contains(":") && content.contains("    ")) { // 缩进和冒号
            scores.put("python", scores.get("python") + 2);
        }
        
        // JavaScript特征
        if (content.contains("function") || content.contains("var ") || content.contains("const ")) {
            scores.put("javascript", scores.get("javascript") + 2);
        }
        if (content.contains("document.") || content.contains("window.")) {
            scores.put("javascript", scores.get("javascript") + 3);
        }

        // XML特征
        if (content.contains("<") && content.contains(">") && content.contains("</")) {
            scores.put("xml", scores.get("xml") + 2);
        }
        
        // Markdown特征
        if (content.contains("# ") || content.contains("## ") || content.contains("### ")) {
            scores.put("markdown", scores.get("markdown") + 3);
        }
        if (content.contains("**") || content.contains("*") || content.contains("_")) {
            scores.put("markdown", scores.get("markdown") + 2);
        }
        if (content.contains("- ") || content.contains("1. ") || content.contains("> ")) {
            scores.put("markdown", scores.get("markdown") + 2);
        }
        if (content.contains("```") || content.contains("---")) {
            scores.put("markdown", scores.get("markdown") + 3);
        }
        
        // 返回得分最高的语言
        String maxLanguage = "java"; // 默认为Java
        int maxScore = 0;
        
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                maxLanguage = entry.getKey();
            }
        }

        return maxScore > 0 ? maxLanguage : "java";
    }
}