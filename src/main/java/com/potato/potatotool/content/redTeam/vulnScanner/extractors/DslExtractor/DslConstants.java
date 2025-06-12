package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.regex.Pattern;

/**
 * DSL常量和配置类
 * 包含所有DSL评估器使用的常量、已知函数列表、对象列表等
 */
public class DslConstants {

    /**
     * 已知可以处理的函数列表
     */
    public static final String[] KNOWN_FUNCTIONS = {
            "contains", "bcontains", "icontains", "ibcontains", "matches", "bmatches",
            "to_lower", "toLowerCase", "to_upper", "toUpperCase", "ignoreCase",
            "base64", "md5", "sha1", "sha256", "substr", "len", "regex", "rand",
            "string", "bytes", "reverse", "wait", "sleep", "submatch", "all_headers",
            "body", "body_string", "status_code", "content_length", "content_type", "latency",
            "header", "html_element", "html_attribute", "raw", "request", "response",
            "htmlelement", "jsonpath", "contains_all", "contains_any", "compare_versions",
            "startswith", "endswith", "mmh3", "base64_py", "base64_decode", "hex_encode",
            "hex_decode", "replace", "tolower", "toupper", "interactsh_protocol", "tostring",
            "json_minify", "concat", "to_number", "to_string", "to_unix_time", "trim",
            "trim_space", "trim_suffix", "unixtime", "urldecode", "version_compare"
    };

    /**
     * 已知的对象/属性列表
     */
    public static final String[] KNOWN_OBJECTS = {
            "response", "request", "status", "headers", "body", "content_type",
            "content_length", "raw", "time", "latency", "path", "host", "scheme", "port", "url",
            "header", "location", "body_1", "body_2", "body_3", "body_4", "body_5",
            "header_1", "header_2", "header_3", "header_4", "header_5", "status_code_1",
            "status_code_2", "location_1", "location_2", "content_type", "server", "set_cookie",
            "all_headers", "data", "BaseURL", "version", "internal_detected_version", "last_version"
    };

    /**
     * 内部结果对象名称（用于排除检查）
     */
    public static final String[] INTERNAL_RESULT_OBJECTS = {
            "__length_result", "__lower_case_result", "__upper_case_result",
            "__hash_result", "__base64_result", "__header_result", "__substr_result"
    };

    /**
     * 正则表达式模式
     */
    public static final Pattern FUNCTION_PATTERN = Pattern.compile("\\b(\\w+)\\s*\\(([^)]*)\\)");
    public static final Pattern OBJECT_PATTERN = Pattern.compile("\\b(\\w+)\\.(\\w+)");
    public static final Pattern AND_PATTERN = Pattern.compile("\\band\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern OR_PATTERN = Pattern.compile("\\bor\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern NOT_PATTERN = Pattern.compile("(\\bnot\\b|!)", Pattern.CASE_INSENSITIVE);

    /**
     * 语法替换规则
     */
    public static final String[][] SYNTAX_REPLACEMENTS = {
            // 替换Xray特有的语法为统一格式
            // 替换bcontains为contains
            {"bcontains\\s*\\(", "contains("},
            // 替换b"字符串"或b'字符串'为普通字符串
            {"b([\"'])", "$1"},
            {"icontains\\s*\\(", "ignoreCase(contains("},
            {"ibcontains\\s*\\(", "ignoreCase(contains("},
            {"bmatches\\s*\\(", "matches("},
            {"to_lower\\s*\\(", "toLowerCase("},
            {"to_upper\\s*\\(", "toUpperCase("},
            {"compare_versions\\s*\\(", "version_compare("},
            {"tolower\\s*\\(", "toLowerCase("},
            {"toupper\\s*\\(", "toUpperCase("}
    };

    /**
     * 错误日志文件名
     */
    public static final String ERROR_LOG_FILE = "errorPoc.txt";

    /**
     * 默认字符编码
     */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 比较操作符
     */
    public static final String[] COMPARISON_OPERATORS = {
            "==", "!=", ">=", "<=", ">", "<"
    };

    /**
     * 私有构造函数，防止实例化
     */
    private DslConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}