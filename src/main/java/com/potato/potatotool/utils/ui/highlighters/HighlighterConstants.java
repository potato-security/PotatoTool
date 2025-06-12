package com.potato.potatotool.utils.ui.highlighters;

/**
 * 代码高亮器常量类
 * 存储各种语言高亮器共用的常量
 * 支持Java、Python、JavaScript、Markdown等多种语言
 * @author Potato
 */
public class HighlighterConstants {
    
    /**
     * Java关键字
     */
    public static final String[] JAVA_KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while",
            "true", "false", "var", "null", "string"
    };
    
    /**
     * Python关键字
     */
    public static final String[] PYTHON_KEYWORDS = {
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "False", "finally", "for",
            "from", "global", "if", "import", "in", "is", "lambda", "None",
            "nonlocal", "not", "or", "pass", "raise", "return", "True", "try",
            "while", "with", "yield"
    };
    
    /**
     * JavaScript关键字
     */
    public static final String[] JAVASCRIPT_KEYWORDS = {
            "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "debugger", "default", "delete", "do",
            "double", "else", "enum", "eval", "export", "extends", "false", "final",
            "finally", "float", "for", "function", "goto", "if", "implements", "import",
            "in", "instanceof", "int", "interface", "let", "long", "native", "new",
            "null", "package", "private", "protected", "public", "return", "short", "static",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "typeof", "var", "void", "volatile", "while", "with", "yield"
    };
    
    /**
     * JavaScript内置对象和函数
     */
    public static final String[] JAVASCRIPT_BUILTINS = {
            "Array", "Boolean", "Date", "Error", "Function", "JSON", "Math", 
            "Map", "Number", "Object", "Promise", "Proxy", "RegExp", "Set", 
            "String", "Symbol", "WeakMap", "WeakSet", "console", "document", 
            "window", "setTimeout", "setInterval", "clearTimeout", "clearInterval"
    };
    
    /**
     * 常见的字符串模式
     */
    public static final String DOUBLE_QUOTE_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    public static final String SINGLE_QUOTE_STRING_PATTERN = "'([^'\\\\]|\\\\.)*'";
    public static final String JAVASCRIPT_STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`";
    
    /**
     * 常见的注释模式
     */
    public static final String SINGLE_LINE_COMMENT_PATTERN = "//[^\n]*";
    public static final String MULTI_LINE_COMMENT_PATTERN = "/\\*(.|\\R)*?\\*/";
    public static final String JAVASCRIPT_COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    public static final String PYTHON_COMMENT_PATTERN = "#[^\n]*";
    
    /**
     * 常见的括号和分隔符模式
     */
    public static final String PAREN_PATTERN = "\\(|\\)";
    public static final String BRACE_PATTERN = "\\{|\\}";
    public static final String BRACKET_PATTERN = "\\[|\\]";
    public static final String SEMICOLON_PATTERN = "\\;";
    public static final String COMMA_PATTERN = "\\,";
    
    /**
     * 常见的样式类名
     */
    public static final String STYLE_KEYWORD = "keyword";
    public static final String STYLE_STRING = "string";
    public static final String STYLE_COMMENT = "comment";
    public static final String STYLE_PAREN = "paren";
    public static final String STYLE_BRACE = "brace";
    public static final String STYLE_BRACKET = "bracket";
    public static final String STYLE_SEMICOLON = "semicolon";
    public static final String STYLE_NUMBER = "number";
    public static final String STYLE_FUNCTION = "function";
    public static final String STYLE_BUILTIN = "builtin";
    public static final String STYLE_TEMPLATE = "template";
    
    /**
     * Markdown样式类名
     */
    public static final String MD_STYLE = "md";
    public static final String MD_H1_STYLE = "h1";
    public static final String MD_H2_STYLE = "h2";
    public static final String MD_H3_STYLE = "h3";
    public static final String MD_H4_STYLE = "h4";
    public static final String MD_H5_STYLE = "h5";
    public static final String MD_H6_STYLE = "h6";
    public static final String MD_STRONG_STYLE = "strong";
    public static final String MD_EMPHASIS_STYLE = "emph";
    public static final String MD_LIST_STYLE = "list";
    public static final String MD_LINK_STYLE = "link";
    public static final String MD_CODE_STYLE = "code";
    public static final String MD_QUOTE_STYLE = "quote";
    public static final String MD_IMAGE_STYLE = "image";
    public static final String MD_HR_STYLE = "hr";
    
    /**
     * 语言特定样式类名
     * 用于在Markdown代码块中应用特定语言的高亮样式
     */
    public static final String JAVA_STYLE = "java";
    public static final String PYTHON_STYLE = "python";
    public static final String JS_STYLE = "javascript";
    public static final String XML_STYLE = "xml";
    public static final String HTML_STYLE = "html";
    public static final String JSON_STYLE = "json";
    public static final String CSS_STYLE = "css";
    public static final String YAML_STYLE = "yaml";
    public static final String GROOVY_STYLE = "groovy";
    public static final String PROPERTIES_STYLE = "properties";
}