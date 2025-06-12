package com.potato.potatotool.utils.ui.highlighters.stylers;
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
 * YAML语言高亮器
 * @author Potato
 */
public class YamlStyler implements MultiLanguageHighlighter.LanguageStyler {

    private static final String KEY_PATTERN = "^\\s*([\\w\\-]+)\\s*:";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "#[^\\n]*";
    private static final String ANCHOR_PATTERN = "&[\\w]+|\\*[\\w]+";
    private static final String TAG_PATTERN = "![\\w/]+";
    private static final String NUMBER_PATTERN = "\\b\\d+[\\.]?\\d*\\b";
    private static final String BOOLEAN_PATTERN = "\\b(true|false|yes|no|on|off)\\b";
    private static final String NULL_PATTERN = "\\b(null|~)\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEY>" + KEY_PATTERN + ")" +
            "|(?<STRING>" + STRING_PATTERN + ")" +
            "|(?<COMMENT>" + COMMENT_PATTERN + ")" +
            "|(?<ANCHOR>" + ANCHOR_PATTERN + ")" +
            "|(?<TAG>" + TAG_PATTERN + ")" +
            "|(?<NUMBER>" + NUMBER_PATTERN + ")" +
            "|(?<BOOLEAN>" + BOOLEAN_PATTERN + ")" +
            "|(?<NULL>" + NULL_PATTERN + ")",
            Pattern.MULTILINE
    );

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<String>();
        names.add("yaml");
        names.add("yml");
        return Collections.unmodifiableSet(names);
    }

    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEY") != null ? "keyword" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("ANCHOR") != null ? "paren" :
                    matcher.group("TAG") != null ? "paren" :
                    matcher.group("NUMBER") != null ? "number" :
                    matcher.group("BOOLEAN") != null ? "keyword" :
                    matcher.group("NULL") != null ? "keyword" :
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