package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.HighlighterConstants;
import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;
import com.potato.potatotool.utils.ui.highlighters.PatternUtils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavaScript代码高亮器
 * 支持JavaScript语法元素的高亮显示，包括关键字、内置对象、函数、字符串、注释等
 * @author Potato
 */
public class JavaScriptStyler implements MultiLanguageHighlighter.LanguageStyler {

    private static final Pattern PATTERN = PatternUtils.createJavaScriptDetailedPattern();

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("javascript");
        names.add("js");
        return Collections.unmodifiableSet(names);
    }


    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? HighlighterConstants.STYLE_KEYWORD :
                    matcher.group("BUILTIN") != null ? HighlighterConstants.STYLE_BUILTIN :
                    matcher.group("PAREN") != null ? HighlighterConstants.STYLE_PAREN :
                    matcher.group("BRACE") != null ? HighlighterConstants.STYLE_BRACE :
                    matcher.group("BRACKET") != null ? HighlighterConstants.STYLE_BRACKET :
                    matcher.group("SEMICOLON") != null ? HighlighterConstants.STYLE_SEMICOLON :
                    matcher.group("STRING") != null ? HighlighterConstants.STYLE_STRING :
                    matcher.group("COMMENT") != null ? HighlighterConstants.STYLE_COMMENT :
                    matcher.group("NUMBER") != null ? HighlighterConstants.STYLE_NUMBER :
                    matcher.group("FUNCTION") != null ? HighlighterConstants.STYLE_FUNCTION :
                    matcher.group("TEMPLATE") != null ? HighlighterConstants.STYLE_TEMPLATE :
                    null; 
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}