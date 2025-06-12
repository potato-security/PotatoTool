package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.AbstractStyler;
import com.potato.potatotool.utils.ui.highlighters.HighlighterConstants;
import com.potato.potatotool.utils.ui.highlighters.PatternUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java代码高亮器
 * 支持关键字、括号、字符串、注释等语法元素的高亮
 * @author Potato
 */
public class JavaStyler extends AbstractStyler {

    private static final Pattern PATTERN = PatternUtils.createJavaPattern();

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("java");
        return Collections.unmodifiableSet(names);
    }

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }
    
    @Override
    protected String getStyleClass(Matcher matcher) {
        return matcher.group("KEYWORD") != null ? HighlighterConstants.STYLE_KEYWORD :
               matcher.group("PAREN") != null ? HighlighterConstants.STYLE_PAREN :
               matcher.group("BRACE") != null ? HighlighterConstants.STYLE_BRACE :
               matcher.group("BRACKET") != null ? HighlighterConstants.STYLE_BRACKET :
               matcher.group("SEMICOLON") != null ? HighlighterConstants.STYLE_SEMICOLON :
               matcher.group("STRING") != null ? HighlighterConstants.STYLE_STRING :
               matcher.group("COMMENT") != null ? HighlighterConstants.STYLE_COMMENT :
               null;
    }
}