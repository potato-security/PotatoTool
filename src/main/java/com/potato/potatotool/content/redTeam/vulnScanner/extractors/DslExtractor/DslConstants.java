package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.regex.Pattern;

/**
 * DSL常量和配置类
 * 包含所有Nuclei DSL评估器使用的常量、已知函数列表、对象列表等
 */
public class DslConstants {

    /**
     * Nuclei官方支持的Helper Functions列表
     * 包含官方函数及其常用别名
     */
    public static final String[] KNOWN_FUNCTIONS = {
            // 布尔函数（官方 + 别名 + 变体）
            "contains", "bcontains", "icontains", "ibcontains",  // contains变体：二进制、忽略大小写
            "contains_all", "contains_any", 
            "starts_with", "startswith",  // 别名
            "ends_with", "endswith",      // 别名
            "equals_any", "compare_versions", 
            "regex", "matches", "bmatches",  // 正则匹配变体
            
            // 字符串转换函数（官方 + 别名）
            "to_lower", "tolower", "tolowercase", "toLowerCase",  // 别名
            "to_upper", "toupper", "touppercase", "toUpperCase",  // 别名
            "to_string", "tostring",  // 别名
            "to_number", "to_title",
            
            // 字符串截取函数
            "substr",  // 子字符串提取
            
            // Base64函数
            "base64", "base64_decode", "base64_py",
            
            // 哈希函数
            "md5", "sha1", "sha256", "sha512", "mmh3",
            
            // 十六进制函数
            "hex_encode", "hex_decode", "hex_to_dec", "dec_to_hex", "bin_to_dec",
            
            // URL函数
            "url_encode", "url_decode", "urldecode",  // 别名
            
            // 字符串处理函数
            "trim", "trim_space", "trim_left", "trim_right", "trim_prefix", "trim_suffix",
            "replace", "replace_regex", "concat", "reverse", "repeat",
            
            // 长度函数
            "len", "length",
            
            // 时间函数（官方 + 别名）
            "unix_time", "unixtime",  // 别名
            "to_unix_time", "date_time",
            
            // 等待函数
            "wait_for", "sleep", "wait",
            
            // 随机函数
            "rand_text_alpha", "rand_text_alphanumeric", "rand_text_numeric",
            "rand_base", "rand_int", "rand_char", "rand_ip",  // 添加缺失的随机函数
            
            // JSON函数
            "json_minify", "json_prettify",
            
            // 压缩函数
            "gzip", "gzip_decode", "zlib", "zlib_decode",
            
            // 反序列化函数
            "generate_java_gadget",
            
            // JWT函数
            "generate_jwt",
            
            // 加密函数
            "aes_gcm",
            
            // 网络函数
            "resolve", "ip_format", "public_ip", "zip", "html_unescape",
            
            // TLS指纹函数
            "jarm"  // JARM指纹（TLS指纹识别）
    };

    /**
     * 已知的对象/属性列表
     */
    public static final String[] KNOWN_OBJECTS = {
            "response", "request", "status", "headers", "body", "content_type",
            "content_length", "raw", "time", "latency", "path", "host", "scheme", "port", "url",
            "header", "location", "server", "set_cookie", "all_headers", "data", 
            // 带下标的变量
            "body_1", "body_2", "body_3", "body_4", "body_5",
            "header_1", "header_2", "header_3", "header_4", "header_5",
            "status_code_1", "status_code_2", "status_code_3", "status_code_4", "status_code_5",
            "location_1", "location_2", "location_3", "location_4", "location_5",
            "response_1", "response_2", "response_3", "response_4", "response_5",
            // 版本相关
            "version", "internal_detected_version", "last_version", "phpversion",
            // 其他变量
            "BaseURL", "Hostname", "username", "interactsh_request", "interactsh_protocol",
            "status_code", "content_type_1", "content_type_2"
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
    // 修改 NOT_PATTERN 排除 != 运算符，使用负向前瞻 (?!=) 确保 ! 后面不跟 =
    public static final Pattern NOT_PATTERN = Pattern.compile("(\\bnot\\b|!(?!=))", Pattern.CASE_INSENSITIVE);

    /**
     * 语法替换规则
     * 用于标准化Nuclei DSL表达式
     */
    public static final String[][] SYNTAX_REPLACEMENTS = {
            // 替换b"字符串"或b'字符串'为普通字符串（Nuclei支持二进制字符串前缀）
            {"b([\"'])", "$1"}
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