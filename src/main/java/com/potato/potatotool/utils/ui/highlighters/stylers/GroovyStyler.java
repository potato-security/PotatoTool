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
 * Groovy语言高亮器
 * @author Potato
 */
public class GroovyStyler implements MultiLanguageHighlighter.LanguageStyler {

    private static final String[] KEYWORDS = {
        "as", "assert", "break", "case", "catch", "class", "const", "continue",
        "def", "default", "do", "else", "enum", "extends", "false", "finally",
        "for", "goto", "if", "implements", "import", "in", "instanceof", "interface",
        "new", "null", "package", "return", "super", "switch", "this", "throw",
        "throws", "trait", "true", "try", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "[()]";
    private static final String BRACE_PATTERN = "[{}]";
    private static final String BRACKET_PATTERN = "[\\[\\]]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "//[^\\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String ANNOTATION_PATTERN = "@[\\w]+";
    private static final String NUMBER_PATTERN = "\\b\\d+[\\.]?\\d*\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")" +
            "|(?<PAREN>" + PAREN_PATTERN + ")" +
            "|(?<BRACE>" + BRACE_PATTERN + ")" +
            "|(?<BRACKET>" + BRACKET_PATTERN + ")" +
            "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" +
            "|(?<STRING>" + STRING_PATTERN + ")" +
            "|(?<COMMENT>" + COMMENT_PATTERN + ")" +
            "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")" +
            "|(?<NUMBER>" + NUMBER_PATTERN + ")"
    );

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<String>();
        names.add("groovy");
        return Collections.unmodifiableSet(names);
    }

    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("ANNOTATION") != null ? "paren" :
                    matcher.group("NUMBER") != null ? "number" :
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