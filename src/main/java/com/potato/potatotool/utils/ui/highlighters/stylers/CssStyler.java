package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.AbstractStyler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CSS语言代码高亮实现
 * 基于正则表达式匹配CSS语法元素
 * @author Potato
 */
public class CssStyler extends AbstractStyler {

    /**
     * CSS关键字列表
     */
    private static final String[] KEYWORDS = new String[] {
            "@media", "@import", "@charset", "@keyframes", "@font-face", "@supports",
            "!important", "from", "to"
    };

    /**
     * CSS属性列表
     */
    private static final String[] PROPERTIES = new String[] {
            "color", "background", "background-color", "font-size", "font-family", "font-weight",
            "margin", "padding", "border", "display", "position", "width", "height", "top", "right",
            "bottom", "left", "z-index", "float", "clear", "overflow", "text-align", "vertical-align",
            "line-height", "text-decoration", "text-transform", "white-space", "content", "cursor",
            "transition", "transform", "animation", "flex", "grid", "opacity", "visibility"
    };

    private static final String KEYWORD_PATTERN = createKeywordPattern(KEYWORDS);
    private static final String PROPERTY_PATTERN = createKeywordPattern(PROPERTIES);
    private static final String SELECTOR_PATTERN = "[.#]\\w+";
    private static final String HEX_COLOR_PATTERN = "#[0-9a-fA-F]{3,6}";
    private static final String FUNCTION_PATTERN = "\\b[a-zA-Z-]+\\(";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "/\\*(.|\\R)*?\\*/";
    private static final String UNIT_PATTERN = "\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|vmin|vmax|pt|pc|in|cm|mm|ex|ch)\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PROPERTY>" + PROPERTY_PATTERN + ")"
                    + "|(?<SELECTOR>" + SELECTOR_PATTERN + ")"
                    + "|(?<HEXCOLOR>" + HEX_COLOR_PATTERN + ")"
                    + "|(?<FUNCTION>" + FUNCTION_PATTERN + ")"
                    + "|(?<PAREN>\\(|\\))"
                    + "|(?<BRACE>\\{|\\})"
                    + "|(?<BRACKET>\\[|\\])"
                    + "|(?<SEMICOLON>\\;)"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<UNIT>" + UNIT_PATTERN + ")",
            Pattern.DOTALL
    );

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<String>();
        names.add("css");
        return Collections.unmodifiableSet(names);
    }

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getStyleClass(Matcher matcher) {
        return matcher.group("KEYWORD") != null ? "keyword" :
               matcher.group("PROPERTY") != null ? "paren" :
               matcher.group("SELECTOR") != null ? "brace" :
               matcher.group("HEXCOLOR") != null ? "brace" :
               matcher.group("FUNCTION") != null ? "function" :
               matcher.group("PAREN") != null ? "paren" :
               matcher.group("BRACE") != null ? "brace" :
               matcher.group("BRACKET") != null ? "bracket" :
               matcher.group("SEMICOLON") != null ? "semicolon" :
               matcher.group("STRING") != null ? "string" :
               matcher.group("COMMENT") != null ? "comment" :
               matcher.group("UNIT") != null ? "decorator" :
               null;
    }
}