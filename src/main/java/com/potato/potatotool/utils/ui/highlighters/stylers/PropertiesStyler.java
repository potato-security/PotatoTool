package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.HighlighterConstants;
import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Properties文件高亮器
 * @author Potato
 */
public class PropertiesStyler implements MultiLanguageHighlighter.LanguageStyler {

    private static final String KEY_PATTERN = "^\\s*([\\w\\-\\.]+)\\s*";
    private static final String VALUE_PATTERN = "=\\s*(.+)$";
    private static final String COMMENT_PATTERN = "^\\s*(#|!)[^\\n]*";
    
    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEY>" + KEY_PATTERN + ")(?==)" +
            "|(?<VALUE>" + VALUE_PATTERN + ")" +
            "|(?<COMMENT>" + COMMENT_PATTERN + ")",
            Pattern.MULTILINE
    );

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("properties");
        names.add("props");
        return Collections.unmodifiableSet(names);
    }

    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEY") != null ? HighlighterConstants.STYLE_KEYWORD :
                    matcher.group("VALUE") != null ? HighlighterConstants.STYLE_STRING :
                    matcher.group("COMMENT") != null ? HighlighterConstants.STYLE_COMMENT :
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