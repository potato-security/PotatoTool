package com.potato.potatotool.utils.ui.highlighters;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽象语法高亮器，提供基本的高亮功能
 * 子类需要实现特定语言的关键字和模式定义
 * @author Potato
 */
public abstract class AbstractStyler implements MultiLanguageHighlighter.LanguageStyler {
    


    /**
     * 获取语言的正则表达式模式
     * @return 编译好的Pattern对象
     */
    protected abstract Pattern getPattern();
    
    /**
     * 根据匹配组名获取样式类名
     * @param matcher 正则匹配器
     * @return 样式类名
     */
    protected abstract String getStyleClass(Matcher matcher);
    
    /**
     * 计算代码高亮样式
     * 实现LanguageStyler接口的方法
     * @param text 需要高亮的文本
     * @return 样式集合
     */
    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        if (text == null || text.isEmpty()) {
            // 处理空文本情况
            StyleSpansBuilder<Collection<String>> emptyBuilder = new StyleSpansBuilder<>();
            emptyBuilder.add(Collections.emptyList(), 0);
            return emptyBuilder.create();
        }
        
        try {
            Matcher matcher = getPattern().matcher(text);
            int lastKwEnd = 0;
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            
            while(matcher.find()) {
                String styleClass = getStyleClass(matcher);
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
            spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
            return spansBuilder.create();
        } catch (Exception e) {
            // 返回空样式
            StyleSpansBuilder<Collection<String>> errorBuilder = new StyleSpansBuilder<>();
            errorBuilder.add(Collections.emptyList(), text.length());
            return errorBuilder.create();
        }
    }
    
    /**
     * 从关键字数组创建关键字模式
     * @param keywords 关键字数组
     * @return 关键字正则表达式模式
     */
    protected static String createKeywordPattern(String[] keywords) {
        return PatternUtils.createKeywordPattern(keywords);
    }
    
    /**
     * 创建常见的括号和标点符号模式
     * @return 括号和标点符号模式映射
     */
    protected static Map<String, String> createCommonPatterns() {
        return PatternUtils.createCommonPatterns();
    }
    
    /**
     * 创建通用的字符串模式
     * @return 字符串模式
     */
    protected static String createStringPattern() {
        return PatternUtils.createStringPattern();
    }
    
    /**
     * 创建通用的单行注释模式
     * @param commentPrefix 注释前缀，如 // 或 #
     * @return 单行注释模式
     */
    protected static String createSingleLineCommentPattern(String commentPrefix) {
        return PatternUtils.createSingleLineCommentPattern(commentPrefix);
    }
}