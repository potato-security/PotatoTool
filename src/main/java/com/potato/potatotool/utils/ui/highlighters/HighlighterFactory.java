package com.potato.potatotool.utils.ui.highlighters;
import org.fxmisc.richtext.CodeArea;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.potato.potatotool.utils.ui.highlighters.stylers.*;

/**
 * 代码高亮器工厂类，用于创建和管理不同语言的代码高亮器
 * 支持智能代码类型检测
 * @author Potato
 */
public class HighlighterFactory {
    
    private static final ConcurrentHashMap<String, MultiLanguageHighlighter.LanguageStyler> STYLERS = new ConcurrentHashMap<>();
    private static final CodeTypeDetector TYPE_DETECTOR = new CodeTypeDetector();
    
    static {
        // 自动注册所有语言高亮器
        registerStyler(new JavaStyler());
        registerStyler(new PythonStyler());
        registerStyler(new XmlStyler());
        registerStyler(new JavaScriptStyler());
        registerStyler(new JsonStyler());
        registerStyler(new MarkdownStyler());
        registerStyler(new MarkdownStylerWithoutMarkers()); // 注册无标记符版本的Markdown高亮器
        registerStyler(new GroovyStyler());
        registerStyler(new YamlStyler());
        registerStyler(new PropertiesStyler());
        registerStyler(new ImageJMacroStyler());
        registerStyler(new CssStyler());
    }
    
    /**
     * 注册语言高亮器
     * @param styler 要注册的语言高亮器
     * @throws IllegalArgumentException 如果styler为null
     */
    public static void registerStyler(MultiLanguageHighlighter.LanguageStyler styler) {
        if (styler == null) {
            throw new IllegalArgumentException("语言高亮器不能为空");
        }
        
        styler.getLanguageNames().stream()
            .filter(name -> name != null && !name.isEmpty())
            .forEach(name -> STYLERS.put(name.toLowerCase(), styler));
    }
    
    /**
     * 获取指定语言的高亮器
     * @param language 语言名称
     * @return 语言高亮器，如果不存在则返回Java高亮器作为默认
     */
    public static MultiLanguageHighlighter.LanguageStyler getStylerForLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return STYLERS.get("java"); // 默认返回Java高亮器
        }
        
        return STYLERS.getOrDefault(language.toLowerCase(), STYLERS.get("java"));
    }
    
    /**
     * 为CodeArea创建并初始化高亮器
     * @param codeArea 代码区域组件
     * @param language 语言名称
     * @return 初始化好的高亮器
     * @throws IllegalArgumentException 如果codeArea为null
     */
    public static MultiLanguageHighlighter createHighlighter(CodeArea codeArea, String language) {
        if (codeArea == null) {
            throw new IllegalArgumentException("CodeArea不能为空");
        }
        
        MultiLanguageHighlighter highlighter = new MultiLanguageHighlighter();
        MultiLanguageHighlighter.LanguageStyler styler = getStylerForLanguage(language);
        highlighter.initialize(codeArea, styler);
        return highlighter;
    }
    
    /**
     * 使用Java高亮器为CodeArea创建并初始化高亮器
     * @param codeArea 代码区域组件
     * @return 初始化好的高亮器
     */
    public static MultiLanguageHighlighter createJavaHighlighter(CodeArea codeArea) {
        return createHighlighter(codeArea, "java");
    }
    
    /**
     * 使用智能代码类型检测为CodeArea创建并初始化高亮器
     * @param codeArea 代码区域组件
     * @param fileName 文件名（可选，用于辅助检测）
     * @param content 代码内容
     * @return 初始化好的高亮器
     * @throws IllegalArgumentException 如果codeArea为null或content为null
     */
    public static MultiLanguageHighlighter createHighlighterWithAutoDetection(CodeArea codeArea, String fileName, String content) {
        if (codeArea == null) {
            throw new IllegalArgumentException("CodeArea不能为空");
        }
        if (content == null) {
            throw new IllegalArgumentException("代码内容不能为空");
        }
        
        String detectedLanguage;
        try {
            detectedLanguage = TYPE_DETECTOR.detect(fileName, content);
        } catch (Exception e) {
            // 检测失败时使用Java作为默认
            detectedLanguage = "java";
        }
        
        return createHighlighter(codeArea, detectedLanguage);
    }
    
    /**
     * 使用智能代码类型检测为CodeArea创建并初始化高亮器
     * 仅基于代码内容进行检测
     * @param codeArea 代码区域组件
     * @param content 代码内容
     * @return 初始化好的高亮器
     */
    public static MultiLanguageHighlighter createHighlighterWithAutoDetection(CodeArea codeArea, String content) {
        return createHighlighterWithAutoDetection(codeArea, null, content);
    }
    
    /**
     * 创建一个动态检测内容的高亮器
     * 会监听CodeArea的内容变化，并根据内容自动切换合适的高亮器
     * @param codeArea 代码区域组件
     * @return 初始化好的高亮器
     */
    public static MultiLanguageHighlighter createDynamicHighlighter(CodeArea codeArea) {
        if (codeArea == null) {
            throw new IllegalArgumentException("CodeArea不能为空");
        }
        
        // 初始化为Java高亮器（默认）
        final MultiLanguageHighlighter highlighter = createJavaHighlighter(codeArea);
        
        // 添加内容变化监听器
        codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
//            // 内容变化较大时才进行检测（至少50个字符或内容变化超过20%）
//            if (isSignificantChange(oldValue, newValue)) {
//                updateHighlighterIfLanguageChanged(highlighter, newValue);
//            }
            updateHighlighterIfLanguageChanged(highlighter, newValue);
        });
        
        return highlighter;
    }
    
    /**
     * 判断文本变化是否足够大以触发语言检测
     */
    private static boolean isSignificantChange(String oldValue, String newValue) {
        int lengthDiff = Math.abs(newValue.length() - oldValue.length());
        return lengthDiff > 50 || 
               (oldValue.length() > 0 && 
               lengthDiff / (double)oldValue.length() > 0.2);
    }
    
    /**
     * 如果检测到语言变化，更新高亮器
     */
    private static void updateHighlighterIfLanguageChanged(MultiLanguageHighlighter highlighter, String content) {
        try {
            // 检测代码类型
            String detectedLanguage = TYPE_DETECTOR.detectFromContent(content);
            
            // 获取当前高亮器的语言名称
            Set<String> currentLanguages = highlighter.getCurrentStyler().getLanguageNames();
            
            // 如果检测到的语言与当前语言不同，则切换高亮器
            if (!currentLanguages.contains(detectedLanguage)) {
                MultiLanguageHighlighter.LanguageStyler newStyler = getStylerForLanguage(detectedLanguage);
                highlighter.changeLanguageStyler(newStyler);
            }
        } catch (Exception e) {
            // 检测失败时不改变当前高亮器
        }
    }
    
    /**
     * 获取代码类型检测器实例
     * @return 代码类型检测器
     */
    public static CodeTypeDetector getTypeDetector() {
        return TYPE_DETECTOR;
    }
    
    /**
     * 创建无标记符版本的Markdown高亮器
     * 会在渲染样式的同时删除标记符号（如#、##等，但保留---）
     * @param codeArea 代码区域组件
     * @return 初始化好的高亮器
     */
    public static MultiLanguageHighlighter createMarkdownWithoutMarkersHighlighter(CodeArea codeArea) {
        if (codeArea == null) {
            throw new IllegalArgumentException("CodeArea不能为空");
        }
        
        MultiLanguageHighlighter highlighter = new MultiLanguageHighlighter();
        MarkdownStylerWithoutMarkers styler = (MarkdownStylerWithoutMarkers) getStylerForLanguage("markdown-nomarkers");
        styler.setCodeArea(codeArea); // 设置CodeArea引用，用于删除标记符
        highlighter.initialize(codeArea, styler);
        return highlighter;
    }
}