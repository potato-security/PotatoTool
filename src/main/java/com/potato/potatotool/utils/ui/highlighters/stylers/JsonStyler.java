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
 * JSON代码高亮器
 * 支持JSON语法元素的高亮显示
 * @author Potato
 */
public class JsonStyler extends AbstractStyler {

    private static final Pattern PATTERN = PatternUtils.createJsonPattern();

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("json");
        return Collections.unmodifiableSet(names);
    }

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }
    
    @Override
    protected String getStyleClass(Matcher matcher) {
        return matcher.group("PAREN") != null ? HighlighterConstants.STYLE_PAREN :
               matcher.group("BRACE") != null ? HighlighterConstants.STYLE_BRACE :
               matcher.group("BRACKET") != null ? HighlighterConstants.STYLE_BRACKET :
               matcher.group("DOUBLEQUOTES") != null ? HighlighterConstants.STYLE_STRING :
               matcher.group("SINGLEQUOTES") != null ? HighlighterConstants.STYLE_STRING :
               null;
    }
}