package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XML语言代码高亮实现
 * 基于正则表达式匹配XML语法元素
 * @author Potato
 */
public class XmlStyler implements MultiLanguageHighlighter.LanguageStyler {

    private static final Pattern XML_TAG = Pattern.compile("</?\\w+|/?>|<!--|-->|<!\\[CDATA\\[|\\]\\]>");
    private static final Pattern XML_ATTRIBUTE = Pattern.compile("\\s+[\\w\\-:]+\\s*=\\s*\"");
    private static final Pattern XML_ATTRIBUTE_VALUE = Pattern.compile("\"[^\"]*\"");
    private static final Pattern XML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern XML_CDATA = Pattern.compile("<!\\[CDATA\\[.*?\\]\\]>", Pattern.DOTALL);
    private static final Pattern XML_PROCESSING = Pattern.compile("<\\?.*?\\?>", Pattern.DOTALL);

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<String>();
        names.add("xml");
        names.add("html");
        names.add("xhtml");
        return Collections.unmodifiableSet(names);
    }

    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastKwEnd = 0;

        // 处理XML标签
        Matcher tagMatcher = XML_TAG.matcher(text);
        while (tagMatcher.find()) {
            spansBuilder.add(Collections.emptyList(), tagMatcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("keyword"), tagMatcher.end() - tagMatcher.start());
            lastKwEnd = tagMatcher.end();
        }

        // 处理XML属性
        Matcher attrMatcher = XML_ATTRIBUTE.matcher(text);
        while (attrMatcher.find()) {
            if (attrMatcher.start() >= lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), attrMatcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton("attribute"), attrMatcher.end() - attrMatcher.start());
                lastKwEnd = attrMatcher.end();
            }
        }

        // 处理XML属性值
        Matcher valueMatcher = XML_ATTRIBUTE_VALUE.matcher(text);
        while (valueMatcher.find()) {
            if (valueMatcher.start() >= lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), valueMatcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton("string"), valueMatcher.end() - valueMatcher.start());
                lastKwEnd = valueMatcher.end();
            }
        }

        // 处理XML注释
        Matcher commentMatcher = XML_COMMENT.matcher(text);
        while (commentMatcher.find()) {
            if (commentMatcher.start() >= lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), commentMatcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton("comment"), commentMatcher.end() - commentMatcher.start());
                lastKwEnd = commentMatcher.end();
            }
        }

        // 处理CDATA部分
        Matcher cdataMatcher = XML_CDATA.matcher(text);
        while (cdataMatcher.find()) {
            if (cdataMatcher.start() >= lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), cdataMatcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton("cdata"), cdataMatcher.end() - cdataMatcher.start());
                lastKwEnd = cdataMatcher.end();
            }
        }

        // 处理XML处理指令
        Matcher processingMatcher = XML_PROCESSING.matcher(text);
        while (processingMatcher.find()) {
            if (processingMatcher.start() >= lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), processingMatcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton("processing"), processingMatcher.end() - processingMatcher.start());
                lastKwEnd = processingMatcher.end();
            }
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}