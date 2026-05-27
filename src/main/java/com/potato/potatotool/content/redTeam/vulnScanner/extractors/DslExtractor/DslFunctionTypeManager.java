package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.util.Map;
import java.util.Set;
import com.potato.potatotool.utils.data.GzipUtils;
import java.util.Base64;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * DSL函数类型管理器
 * 统一管理所有DSL函数的类型分类和处理逻辑
 * 提供高性能的函数类型查询和统一的处理接口
 */
public class DslFunctionTypeManager {

    /**
     * 函数类型枚举
     */
    public enum FunctionType {
        BOOLEAN_FUNCTION,    // 返回boolean值的函数
        STRING_FUNCTION,     // 返回String值的函数
        NUMERIC_FUNCTION,    // 返回数值的函数
        UNKNOWN_FUNCTION     // 未知函数
    }

    // 预定义的函数类型集合，提高查询性能
    private static final Set<String> BOOLEAN_FUNCTIONS = new HashSet<>();
    private static final Set<String> STRING_FUNCTIONS = new HashSet<>();
    private static final Set<String> NUMERIC_FUNCTIONS = new HashSet<>();

    static {
        // 初始化boolean函数集合（Nuclei官方函数 + 别名 + 变体）
        BOOLEAN_FUNCTIONS.add("contains");
        BOOLEAN_FUNCTIONS.add("bcontains");   // 二进制contains
        BOOLEAN_FUNCTIONS.add("icontains");   // 忽略大小写contains
        BOOLEAN_FUNCTIONS.add("ibcontains");  // 忽略大小写二进制contains
        BOOLEAN_FUNCTIONS.add("contains_any");
        BOOLEAN_FUNCTIONS.add("contains_all");
        BOOLEAN_FUNCTIONS.add("starts_with");
        BOOLEAN_FUNCTIONS.add("startswith");  // 别名
        BOOLEAN_FUNCTIONS.add("ends_with");
        BOOLEAN_FUNCTIONS.add("endswith");    // 别名
        BOOLEAN_FUNCTIONS.add("equals_any");
        BOOLEAN_FUNCTIONS.add("compare_versions");
        BOOLEAN_FUNCTIONS.add("regex");
        BOOLEAN_FUNCTIONS.add("matches");     // 正则匹配
        BOOLEAN_FUNCTIONS.add("bmatches");    // 二进制正则匹配

        // 初始化字符串函数集合（Nuclei官方函数 + 别名）
        STRING_FUNCTIONS.add("to_lower");
        STRING_FUNCTIONS.add("tolower");      // 别名
        STRING_FUNCTIONS.add("tolowercase");  // 别名
        STRING_FUNCTIONS.add("tolowercase");  // 别名（Java风格toLowerCase，统一转小写处理）
        STRING_FUNCTIONS.add("to_upper");
        STRING_FUNCTIONS.add("toupper");      // 别名
        STRING_FUNCTIONS.add("touppercase");  // 别名
        STRING_FUNCTIONS.add("to_string");
        STRING_FUNCTIONS.add("tostring");     // 别名
        STRING_FUNCTIONS.add("to_title");
        STRING_FUNCTIONS.add("substr");       // 子字符串提取
        STRING_FUNCTIONS.add("base64");
        STRING_FUNCTIONS.add("base64_decode");
        STRING_FUNCTIONS.add("base64_py");
        STRING_FUNCTIONS.add("md5");
        STRING_FUNCTIONS.add("sha1");
        STRING_FUNCTIONS.add("sha256");
        STRING_FUNCTIONS.add("sha512");
        STRING_FUNCTIONS.add("mmh3");
        STRING_FUNCTIONS.add("hex_encode");
        STRING_FUNCTIONS.add("hex_decode");
        STRING_FUNCTIONS.add("hex_to_dec");
        STRING_FUNCTIONS.add("dec_to_hex");
        STRING_FUNCTIONS.add("bin_to_dec");
        STRING_FUNCTIONS.add("url_encode");
        STRING_FUNCTIONS.add("urlencode");  // 别名
        STRING_FUNCTIONS.add("url_decode");
        STRING_FUNCTIONS.add("urldecode");  // 别名
        STRING_FUNCTIONS.add("trim");
        STRING_FUNCTIONS.add("trim_space");
        STRING_FUNCTIONS.add("trim_left");
        STRING_FUNCTIONS.add("trim_right");
        STRING_FUNCTIONS.add("trim_prefix");
        STRING_FUNCTIONS.add("trimprefix");  // 别名
        STRING_FUNCTIONS.add("trim_suffix");
        STRING_FUNCTIONS.add("replace");
        STRING_FUNCTIONS.add("replace_regex");
        STRING_FUNCTIONS.add("concat");
        STRING_FUNCTIONS.add("reverse");
        STRING_FUNCTIONS.add("repeat");
        STRING_FUNCTIONS.add("rand_text_alpha");
        STRING_FUNCTIONS.add("rand_text_alphanumeric");
        STRING_FUNCTIONS.add("rand_text_numeric");
        STRING_FUNCTIONS.add("rand_base");        // 随机字母数字字符串
        STRING_FUNCTIONS.add("randbase");         // 别名（无下划线）
        STRING_FUNCTIONS.add("rand_int");         // 随机整数
        STRING_FUNCTIONS.add("randint");          // 别名
        STRING_FUNCTIONS.add("rand_char");        // 随机字符
        STRING_FUNCTIONS.add("randchar");         // 别名
        STRING_FUNCTIONS.add("rand_ip");          // 随机IP地址
        STRING_FUNCTIONS.add("padding");          // 字符串填充
        STRING_FUNCTIONS.add("pad_left");         // 左填充
        STRING_FUNCTIONS.add("pad_right");        // 右填充
        STRING_FUNCTIONS.add("hmac");             // 通用 HMAC 函数
        STRING_FUNCTIONS.add("hmac_sha1");        // HMAC-SHA1
        STRING_FUNCTIONS.add("hmac_sha256");      // HMAC-SHA256
        STRING_FUNCTIONS.add("hmac_sha512");      // HMAC-SHA512
        STRING_FUNCTIONS.add("hmac_md5");         // HMAC-MD5
        STRING_FUNCTIONS.add("print");            // 打印调试信息
        STRING_FUNCTIONS.add("date_time");
        STRING_FUNCTIONS.add("json_minify");
        STRING_FUNCTIONS.add("json_prettify");
        STRING_FUNCTIONS.add("json_encode");
        STRING_FUNCTIONS.add("unicode_encode");
        STRING_FUNCTIONS.add("unicode_decode");
        STRING_FUNCTIONS.add("gzip");
        STRING_FUNCTIONS.add("gzip_decode");
        STRING_FUNCTIONS.add("zlib");
        STRING_FUNCTIONS.add("zlib_decode");
        STRING_FUNCTIONS.add("generate_java_gadget");
        STRING_FUNCTIONS.add("generate_jwt");
        STRING_FUNCTIONS.add("aes_gcm");
        STRING_FUNCTIONS.add("aes_cbc");
        STRING_FUNCTIONS.add("resolve");
        STRING_FUNCTIONS.add("ip_format");
        STRING_FUNCTIONS.add("public_ip");
        STRING_FUNCTIONS.add("zip");
        STRING_FUNCTIONS.add("html_unescape");
        STRING_FUNCTIONS.add("jarm");         // JARM指纹
        STRING_FUNCTIONS.add("wait_for");
        STRING_FUNCTIONS.add("sleep");
        STRING_FUNCTIONS.add("wait");

        // 初始化数值函数集合（Nuclei官方函数 + 别名）
        NUMERIC_FUNCTIONS.add("len");
        NUMERIC_FUNCTIONS.add("length");
        NUMERIC_FUNCTIONS.add("unix_time");
        NUMERIC_FUNCTIONS.add("unixtime");  // 别名
        NUMERIC_FUNCTIONS.add("now");
        NUMERIC_FUNCTIONS.add("to_unix_time");
        NUMERIC_FUNCTIONS.add("to_number");
        NUMERIC_FUNCTIONS.add("unpack");
    }

    /**
     * 获取函数类型
     * @param functionName 函数名（已转换为小写）
     * @return 函数类型
     */
    public static FunctionType getFunctionType(String functionName) {
        if (functionName == null) {
            return FunctionType.UNKNOWN_FUNCTION;
        }

        String normalizedName = functionName.toLowerCase();

        if (BOOLEAN_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.BOOLEAN_FUNCTION;
        }
        if (STRING_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.STRING_FUNCTION;
        }
        if (NUMERIC_FUNCTIONS.contains(normalizedName)) {
            return FunctionType.NUMERIC_FUNCTION;
        }

        return FunctionType.UNKNOWN_FUNCTION;
    }

    /**
     * 检查是否为已知的DSL函数
     * @param functionName 函数名
     * @return 是否为已知函数
     */
    public static boolean isKnownFunction(String functionName) {
        return getFunctionType(functionName) != FunctionType.UNKNOWN_FUNCTION;
    }

    /**
     * 检查是否为返回boolean的函数
     * @param functionName 函数名
     * @return 是否为boolean函数
     */
    public static boolean isBooleanFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.BOOLEAN_FUNCTION;
    }

    /**
     * 检查是否为返回String的函数
     * @param functionName 函数名
     * @return 是否为String函数
     */
    public static boolean isStringFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.STRING_FUNCTION;
    }

    /**
     * 检查是否为返回数值的函数
     * @param functionName 函数名
     * @return 是否为数值函数
     */
    public static boolean isNumericFunction(String functionName) {
        return getFunctionType(functionName) == FunctionType.NUMERIC_FUNCTION;
    }

    /**
     * 统一的函数评估接口 - 返回boolean结果
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return boolean结果
     */
    public static boolean evaluateFunctionAsBoolean(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                return evaluateBooleanFunction(functionName, expression, context);
            case STRING_FUNCTION:
                String stringResult = evaluateStringFunction(functionName, expression, context);
                return isValidStringForBoolean(stringResult);
            case NUMERIC_FUNCTION:
                String numericResult = evaluateNumericFunction(functionName, expression, context);
                return isValidNumericForBoolean(numericResult);
            default:
                return false;
        }
    }

    /**
     * 统一的函数评估接口 - 返回字符串值
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return 字符串结果
     */
    public static String evaluateFunctionForStringValue(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                boolean boolResult = evaluateBooleanFunction(functionName, expression, context);
                return String.valueOf(boolResult);
            case STRING_FUNCTION:
                return evaluateStringFunction(functionName, expression, context);
            case NUMERIC_FUNCTION:
                return evaluateNumericFunction(functionName, expression, context);
            default:
                return null;
        }
    }

    /**
     * 统一的函数评估接口 - 返回String结果
     * @param functionName 函数名
     * @param expression 完整表达式
     * @param context 上下文
     * @return String结果
     */
    public static String evaluateFunctionAsString(String functionName, String expression, Map<String, Object> context) {
        FunctionType type = getFunctionType(functionName);
        
        switch (type) {
            case BOOLEAN_FUNCTION:
                boolean boolResult = evaluateBooleanFunction(functionName, expression, context);
                return String.valueOf(boolResult);
            case STRING_FUNCTION:
                return evaluateStringFunction(functionName, expression, context);
            case NUMERIC_FUNCTION:
                return evaluateNumericFunction(functionName, expression, context);
            default:
                return null;
        }
    }

    /**
     * 评估boolean函数（Nuclei官方函数）
     */
    private static boolean evaluateBooleanFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "contains":
            case "bcontains":   // 二进制contains（Java String.contains 等价于字节级比较）
            case "icontains":   // 忽略大小写contains（DSL上下文已做tolower处理）
            case "ibcontains":  // 忽略大小写二进制contains（同上）
                return DslFunctionEvaluator.evaluateContainsFunction(expression, context);
            case "contains_any":
                return DslFunctionEvaluator.evaluateContainsAnyFunction(expression, context);
            case "contains_all":
                return DslFunctionEvaluator.evaluateContainsAllFunction(expression, context);
            case "starts_with":
            case "startswith":  // 别名
                return DslFunctionEvaluator.evaluateStartsWithFunction(expression, context);
            case "ends_with":
            case "endswith":    // 别名
                return DslFunctionEvaluator.evaluateEndsWithFunction(expression, context);
            case "equals_any":
                return DslFunctionEvaluator.evaluateEqualsAnyFunction(expression, context);
            case "regex":
            case "matches":     // 正则匹配（matches是regex的别名）
            case "bmatches":   // 二进制正则匹配（Java regex 在 String 上操作，行为等价）
                return DslFunctionEvaluator.evaluateRegexFunction(expression, context);
            case "compare_versions":
                return DslFunctionEvaluator.evaluateCompareVersionsFunction(expression, context);
            default:
                return false;
        }
    }

    /**
     * 评估String函数（Nuclei官方函数）
     */
    private static String evaluateStringFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            // 字符串转换函数（支持别名）
            case "to_lower":
            case "tolower":      // 别名
            case "tolowercase":  // 别名
                return DslFunctionEvaluator.evaluateToLowerCaseFunction(expression, context);
            case "to_upper":
            case "toupper":      // 别名
            case "touppercase":  // 别名
                return DslFunctionEvaluator.evaluateToUpperCaseFunction(expression, context);
            case "to_string":
            case "tostring":    // 别名
                // to_string 函数：转换为字符串
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String value = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return value != null ? value.toString() : "";
                    }
                } catch (Exception e) {
                    System.err.println("评估to_string函数失败: " + e.getMessage());
                }
                return "";
            case "to_title":
                // to_title 函数：标题格式（首字母大写）
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String value = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return value != null ? DslFunctionExtended.toTitle(value) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估to_title函数失败: " + e.getMessage());
                }
                return null;
            
            // Base64函数
            case "base64":
            case "base64_py":
                return DslFunctionEvaluator.evaluateBase64Function(expression, context);
            case "base64_decode":
                return DslFunctionEvaluator.evaluateBase64DecodeFunction(expression, context);
            
            // 哈希函数
            case "md5":
                return DslFunctionEvaluator.evaluateMd5Function(expression, context);
            case "sha1":
                return DslFunctionEvaluator.evaluateSha1Function(expression, context);
            case "sha256":
                return DslFunctionEvaluator.evaluateSha256Function(expression, context);
            case "sha512":
                return DslFunctionEvaluator.evaluateSha512Function(expression, context);
            case "mmh3":
                return DslFunctionEvaluator.evaluateMmh3Function(expression, context);
            
            // 十六进制函数
            case "hex_encode":
                return DslFunctionEvaluator.evaluateHexEncodeFunction(expression, context);
            case "hex_decode":
                return DslFunctionEvaluator.evaluateHexDecodeFunction(expression, context);
            case "hex_to_dec":
                // hex_to_dec 函数：十六进制转十进制
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String hex = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (hex != null) {
                            hex = hex.replaceFirst("^0[xX]", ""); // 移除0x前缀
                            return String.valueOf(Long.parseLong(hex, 16));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估hex_to_dec函数失败: " + e.getMessage());
                }
                return null;
            case "dec_to_hex":
                return DslFunctionEvaluator.evaluateDecToHexFunction(expression, context);
            case "bin_to_dec":
                return DslFunctionEvaluator.evaluateBinToDecFunction(expression, context);
            
            // URL函数
            case "url_encode":
            case "urlencode":  // 别名
                return DslFunctionEvaluator.evaluateUrlEncodeFunction(expression, context);
            case "url_decode":
            case "urldecode":  // 别名
                return DslFunctionEvaluator.evaluateUrlDecodeFunction(expression, context);
            
            // 字符串处理函数
            case "trim":
            case "trim_space":
                return DslFunctionEvaluator.evaluateTrimFunction(expression, context);
            case "trim_left":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String cutset = DslUtils.cleanStringValue(args[1]);
                        return str != null && cutset != null ? DslFunctionExtended.trimLeft(str) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估trim_left函数失败: " + e.getMessage());
                }
                return null;
            case "trim_right":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String cutset = DslUtils.cleanStringValue(args[1]);
                        return str != null && cutset != null ? DslFunctionExtended.trimRight(str) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估trim_right函数失败: " + e.getMessage());
                }
                return null;
            case "trim_prefix":
            case "trimprefix":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String prefix = DslUtils.cleanStringValue(args[1]);
                        return str != null && prefix != null ? DslFunctionExtended.trimPrefix(str, prefix) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估trim_prefix函数失败: " + e.getMessage());
                }
                return null;
            case "trim_suffix":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String suffix = DslUtils.cleanStringValue(args[1]);
                        return str != null && suffix != null ? DslFunctionExtended.trimSuffix(str, suffix) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估trim_suffix函数失败: " + e.getMessage());
                }
                return null;
            case "replace":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 3) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String old = DslUtils.cleanStringValue(args[1]);
                        String newStr = DslUtils.cleanStringValue(args[2]);
                        return str != null ? DslFunctionExtended.replace(str, old, newStr) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估replace函数失败: " + e.getMessage());
                }
                return null;
            case "replace_regex":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 3) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String pattern = DslUtils.cleanStringValue(args[1]);
                        String replacement = DslUtils.cleanStringValue(args[2]);
                        return str != null ? DslFunctionExtended.replaceRegex(str, pattern, replacement) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估replace_regex函数失败: " + e.getMessage());
                }
                return null;
            case "concat":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    String[] values = new String[args.length];
                    for (int i = 0; i < args.length; i++) {
                        values[i] = DslEvaluatorRefactored.resolveValueOrFunction(args[i], context);
                    }
                    return DslFunctionExtended.concat(values);
                } catch (Exception e) {
                    System.err.println("评估concat函数失败: " + e.getMessage());
                }
                return null;
            case "reverse":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return str != null ? DslFunctionExtended.reverse(str) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估reverse函数失败: " + e.getMessage());
                }
                return null;
            case "repeat":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        int count = Integer.parseInt(DslEvaluatorRefactored.resolveValueOrFunction(args[1], context));
                        return str != null ? DslFunctionExtended.repeat(str, count) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估repeat函数失败: " + e.getMessage());
                }
                return null;
            
            // 字符串截取函数
            case "substr":
                return DslFunctionEvaluator.evaluateSubstrFunction(expression, context);
            
            // 随机函数
            case "rand_text_alpha":
                return DslFunctionEvaluator.evaluateRandTextAlphaFunction(expression, context);
            case "rand_text_alphanumeric":
                return DslFunctionEvaluator.evaluateRandTextAlphanumericFunction(expression, context);
            case "rand_text_numeric":
                return DslFunctionEvaluator.evaluateRandTextNumericFunction(expression, context);
            case "rand_base":
            case "randbase":
                return DslFunctionEvaluator.evaluateRandBaseFunction(expression, context);
            case "rand_int":
            case "randint":
                return DslFunctionEvaluator.evaluateRandIntFunction(expression, context);
            case "rand_char":
            case "randchar":
                return DslFunctionEvaluator.evaluateRandCharFunction(expression, context);
            case "rand_ip":
                return DslFunctionEvaluator.evaluateRandIpFunction(expression, context);
            
            // 填充函数
            case "padding":
            case "pad_left":
            case "pad_right":
                return evaluatePaddingFunction(expression, context, functionName);
            
            // HMAC 函数
            case "hmac":
            case "hmac_sha1":
            case "hmac_sha256":
            case "hmac_sha512":
            case "hmac_md5":
                return evaluateHmacFunction(expression, context, functionName);
            case "print":
                return DslFunctionEvaluator.evaluatePrintFunction(expression, context);

            // 时间函数
            case "date_time":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length == 1) {
                        String format = DslUtils.cleanStringValue(args[0]);
                        return format != null ? DslFunctionExtended.dateTime(format) : null;
                    } else if (args.length >= 2) {
                        String arg0Format = DslUtils.cleanStringValue(args[0]);
                        String arg1Format = DslUtils.cleanStringValue(args[1]);

                        String format;
                        String rawTime;

                        // 兼容两种写法：
                        // 1) date_time(time, format)
                        // 2) date_time(format, time)
                        if (looksLikeDateFormat(arg0Format)) {
                            format = arg0Format;
                            rawTime = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                        } else {
                            format = arg1Format;
                            rawTime = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        }

                        if (rawTime != null && format != null) {
                            long unixSeconds;
                            try {
                                unixSeconds = Long.parseLong(rawTime.trim());
                            } catch (NumberFormatException nfe) {
                                unixSeconds = DslFunctionExtended.toUnixTime(rawTime);
                            }
                            return DslFunctionExtended.dateTime(unixSeconds, format);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估date_time函数失败: " + e.getMessage());
                }
                return null;
            case "now":
                return String.valueOf(DslFunctionExtended.now());

            // JSON函数
            case "json_minify":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String json = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return json != null ? DslFunctionExtended.jsonMinify(json) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估json_minify函数失败: " + e.getMessage());
                }
                return null;
            case "json_prettify":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String json = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return json != null ? DslFunctionExtended.jsonPrettify(json) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估json_prettify函数失败: " + e.getMessage());
                }
                return null;
            case "json_encode":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String value = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return value != null ? DslFunctionExtended.jsonEncode(value) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估json_encode函数失败: " + e.getMessage());
                }
                return null;
            case "unicode_encode":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String value = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return value != null ? DslFunctionExtended.unicodeEncode(value) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估unicode_encode函数失败: " + e.getMessage());
                }
                return null;
            case "unicode_decode":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String value = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return value != null ? DslFunctionExtended.unicodeDecode(value) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估unicode_decode函数失败: " + e.getMessage());
                }
                return null;
            
            // 压缩函数
            case "gzip":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String data = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (data != null) {
                            byte[] compressed = GzipUtils.GzipGetCompressedData(data.getBytes("UTF-8"));
                            return compressed != null ? Base64.getEncoder().encodeToString(compressed) : null;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估gzip函数失败: " + e.getMessage());
                }
                return null;
            case "gzip_decode":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String data = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (data != null) {
                            // 尝试Base64解码
                            byte[] compressed;
                            try {
                                compressed = Base64.getDecoder().decode(data);
                            } catch (Exception e) {
                                // 如果不是Base64，直接使用原始数据
                                compressed = data.getBytes("UTF-8");
                            }
                            byte[] decompressed = GzipUtils.GzipDecompress(compressed);
                            return decompressed != null ? new String(decompressed, "UTF-8") : null;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估gzip_decode函数失败: " + e.getMessage());
                }
                return null;
            case "zlib":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String data = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return data != null ? DslFunctionExtended.zlibCompress(data) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估zlib函数失败: " + e.getMessage());
                }
                return null;
            case "zlib_decode":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String data = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        return data != null ? DslFunctionExtended.zlibDecompress(data) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估zlib_decode函数失败: " + e.getMessage());
                }
                return null;
            
            // 反序列化函数
            case "generate_java_gadget":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String gadgetType = DslUtils.cleanStringValue(args[0]);
                        String cmd = resolveStringArgument(args[1], context);
                        String encoding = args.length >= 3 ? DslUtils.cleanStringValue(args[2]) : "base64";
                        // 使用DslFunctionExtended中的实现
                        String result = DslFunctionExtended.generateJavaGadget(gadgetType);
                        // 如果返回占位符，尝试生成实际的gadget
                        if ("JAVA_GADGET_PLACEHOLDER".equals(result)) {
                            // 这里可以集成ysoserial或使用其他方法生成
                            String normalizedCmd = normalizeCallbackCommand(cmd);
                            System.out.println("generate_java_gadget: gadget=" + gadgetType + ", cmd=" + normalizedCmd + ", encoding=" + encoding);
                            return result;
                        }
                        return result;
                    }
                } catch (Exception e) {
                    System.err.println("评估generate_java_gadget函数失败: " + e.getMessage());
                }
                return null;
            
            // JWT函数
            case "generate_jwt":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 1) {
                        String json = DslUtils.cleanStringValue(args[0]);
                        String algorithm = args.length >= 2 ? DslUtils.cleanStringValue(args[1]) : "HS256";
                        String signature = args.length >= 3 ? DslUtils.cleanStringValue(args[2]) : "";
                        // 当前仅支持HS256算法（覆盖绝大多数Nuclei POC场景）
                        return DslFunctionExtended.generateJwt(json, signature);
                    }
                } catch (Exception e) {
                    System.err.println("评估generate_jwt函数失败: " + e.getMessage());
                }
                return null;
            
            // 加密函数
            case "aes_gcm":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String plaintext = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String key = DslUtils.cleanStringValue(args[1]);
                        return plaintext != null && key != null ? DslFunctionExtended.aesGcm(plaintext, key) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估aes_gcm函数失败: " + e.getMessage());
                }
                return null;
            case "aes_cbc":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 3) {
                        String plaintext = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        String key = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                        String iv = DslEvaluatorRefactored.resolveValueOrFunction(args[2], context);
                        return plaintext != null && key != null && iv != null
                                ? DslFunctionExtended.aesCbc(plaintext, key, iv)
                                : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估aes_cbc函数失败: " + e.getMessage());
                }
                return null;
            
            // 网络函数
            case "resolve":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String host = DslUtils.cleanStringValue(args[0]);
                        return host != null ? DslFunctionExtended.resolve(host) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估resolve函数失败: " + e.getMessage());
                }
                return null;
            case "ip_format":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String ip = DslUtils.cleanStringValue(args[0]);
                        String format = DslUtils.cleanStringValue(args[1]);
                        if (ip != null && format != null) {
                            // ip_format 根据format索引转换IP格式
                            // format: 1-11 对应不同的IP格式（参考mapcidr IP Format Index）
                            int formatIndex = Integer.parseInt(format);
                            return formatIpAddress(ip, formatIndex);
                        }
                    }
                } catch (Exception e) {
                    // IP格式化失败，返回原始IP
                }
                return null;
            
            case "public_ip":
                try {
                    return DslFunctionExtended.publicIp();
                } catch (Exception e) {
                    System.err.println("评估public_ip函数失败: " + e.getMessage());
                }
                return null;
            case "zip":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String filename = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (filename == null) {
                            filename = DslUtils.cleanStringValue(args[0]);
                        } else {
                            filename = DslUtils.cleanStringValue(filename);
                        }

                        String content = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                        if (content == null) {
                            content = DslUtils.cleanStringValue(args[1]);
                        }

                        if (filename == null || filename.trim().isEmpty()) {
                            filename = "payload.bin";
                        }
                        return content != null ? DslFunctionExtended.zip(filename, content) : null;
                    }
                } catch (Exception e) {
                    System.err.println("评估zip函数失败: " + e.getMessage());
                }
                return null;

            case "html_unescape":
                return DslFunctionEvaluator.evaluateHtmlUnescapeFunction(expression, context);

            // TLS指纹函数
            case "jarm":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String hostname = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);

                        // 优先从上下文获取预计算的JARM值（用于批量扫描优化）
                        Object jarmValue = context.get("jarm");
                        if (jarmValue != null) {
                            return jarmValue.toString();
                        }

                        // 实时计算JARM指纹
                        String[] parts = hostname.split(":");
                        String host = parts[0];
                        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;

                        // 调用JarmFingerprinter计算指纹
                        String fingerprint = com.potato.potatotool.content.redTeam.vulnScanner.http.JarmFingerprinter.computeJarm(host, port);

                        if (fingerprint.isEmpty()) {
                            System.err.println("JARM指纹计算失败: " + hostname);
                        }

                        return fingerprint;
                    }
                } catch (Exception e) {
                    System.err.println("评估jarm函数失败: " + e.getMessage());
                    e.printStackTrace();
                }
                return "";
            
            // 等待函数
            case "wait_for":
            case "sleep":
            case "wait":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        int seconds = Integer.parseInt(DslEvaluatorRefactored.resolveValueOrFunction(args[0], context));
                        DslFunctionExtended.waitFor(seconds);
                        return "true";
                    }
                } catch (Exception e) {
                    System.err.println("评估wait_for函数失败: " + e.getMessage());
                }
                return null;
            
            default:
                // 尝试使用扩展函数
                return evaluateExtendedFunction(functionName, expression, context);
        }
    }
    
    /**
     * 评估扩展函数（DslFunctionExtended中的函数）
     * 用于处理一些复杂的Nuclei官方函数
     */
    private static String evaluateExtendedFunction(String functionName, String expression, Map<String, Object> context) {
        // 这些函数在DslFunctionExtended中实现
                System.out.println("未实现的扩展函数: " + functionName);
        return null;
    }

    private static String normalizeCallbackCommand(String cmd) {
        if (cmd == null) {
            return null;
        }
        String normalized = DslUtils.cleanStringValue(cmd).trim();
        normalized = normalized.replaceAll("(?i)^(https?://)(?:https?://)+", "$1");
        while (Pattern.compile("(?i)^https?://https?://").matcher(normalized).find()) {
            normalized = normalized.replaceFirst("(?i)^(https?://)https?://", "$1");
        }
        return normalized;
    }

    /**
     * 评估数值函数并返回boolean结果
     */
    private static boolean evaluateNumericFunctionAsBoolean(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "len":
            case "length":
                return DslFunctionEvaluator.evaluateLenFunction(expression, context);
            case "unixtime":
                return DslFunctionEvaluator.evaluateUnixtimeFunction(expression, context);
            default:
                return false;
        }
    }

    /**
     * 评估数值函数并返回字符串结果（Nuclei官方函数）
     */
    private static String evaluateNumericFunction(String functionName, String expression, Map<String, Object> context) {
        switch (functionName) {
            case "len":
            case "length":
                // 计算实际长度值
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String arg = args[0].trim();
                        String value = DslUtils.resolveValue(arg, context);
                        if (value != null) {
                            return String.valueOf(value.length());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("计算长度失败: " + e.getMessage());
                }
                return "0";
            case "unix_time":
            case "unixtime":  // 别名
                long unixTime = System.currentTimeMillis() / 1000;
                return String.valueOf(unixTime);
            case "now":
                return String.valueOf(DslFunctionExtended.now());
            case "to_unix_time":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String datetime = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (datetime != null) {
                            long unixTimestamp = DslFunctionExtended.toUnixTime(datetime);
                            return String.valueOf(unixTimestamp);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估to_unix_time函数失败: " + e.getMessage());
                }
                return null;
            case "to_number":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length > 0) {
                        String str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                        if (str != null) {
                            double num = DslFunctionExtended.toNumber(str);
                            return String.valueOf(num);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估to_number函数失败: " + e.getMessage());
                }
                return null;
            case "unpack":
                try {
                    String[] args = DslUtils.extractFunctionArgs(expression);
                    if (args.length >= 2) {
                        String format = DslUtils.cleanStringValue(args[0]);
                        String raw = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                        if (raw == null) {
                            raw = DslUtils.cleanStringValue(args[1]);
                        }
                        if (format != null && raw != null) {
                            return evaluateUnpackFunction(format, raw);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("评估unpack函数失败: " + e.getMessage());
                }
                return null;
            default:
                return null;
        }
    }

    /**
     * 检查字符串结果是否可以转换为true
     */
    private static boolean isValidStringForBoolean(String result) {
        return result != null && !result.isEmpty();
    }

    /**
     * 检查数值结果是否可以转换为true
     */
    private static boolean isValidNumericForBoolean(String result) {
        if (result == null) {
            return false;
        }
        try {
            double value = Double.parseDouble(result);
            return value != 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String evaluateUnpackFunction(String format, String raw) {
        try {
            String normalized = raw == null ? null : raw.trim();
            if (normalized == null || normalized.isEmpty()) {
                return null;
            }

            byte[] bytes;
            String cleaned = normalized;
            if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
                cleaned = cleaned.substring(2);
            }
            cleaned = cleaned.replaceAll("\\s+", "");

            if (cleaned.matches("(?i)^[0-9a-f]+$")) {
                if ((cleaned.length() & 1) == 1) {
                    cleaned = "0" + cleaned;
                }
                bytes = hexToBytes(cleaned);
            } else {
                bytes = normalized.getBytes("ISO-8859-1");
            }

            if (">I".equals(format)) {
                if (bytes.length < 4) {
                    return null;
                }
                long v = ((bytes[0] & 0xFFL) << 24)
                        | ((bytes[1] & 0xFFL) << 16)
                        | ((bytes[2] & 0xFFL) << 8)
                        | (bytes[3] & 0xFFL);
                return String.valueOf(v);
            }

            if (bytes.length == 0) {
                return null;
            }
            long first = bytes[0] & 0xFFL;
            return String.valueOf(first);
        } catch (Exception e) {
            System.err.println("unpack解析失败: " + e.getMessage());
            return null;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    private static boolean looksLikeDateFormat(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.contains("%") || value.contains("yyyy") || value.contains("yy")
                || value.contains("MM") || value.contains("dd") || value.contains("HH")
                || value.contains("mm") || value.contains("ss") || value.contains("2006")
                || value.contains("01") || value.contains("02") || value.contains("15")
                || value.contains("04") || value.contains("05") || value.contains("-")
                || value.contains("/") || value.contains(":");
    }

    /**
     * 获取所有支持的函数名称
     * @return 所有支持的函数名称集合
     */
    public static Set<String> getAllSupportedFunctions() {
        Set<String> allFunctions = new HashSet<>();
        allFunctions.addAll(BOOLEAN_FUNCTIONS);
        allFunctions.addAll(STRING_FUNCTIONS);
        allFunctions.addAll(NUMERIC_FUNCTIONS);
        return allFunctions;
    }

    /**
     * 评估 padding 填充函数
     * padding(str, length, padChar) - 填充字符串到指定长度
     * pad_left(str, length, padChar) - 左填充
     * pad_right(str, length, padChar) - 右填充
     */
    private static String evaluatePaddingFunction(String expression, Map<String, Object> context, String functionName) {
        try {
            String[] args = DslUtils.extractFunctionArgs(expression);
            if (args.length < 2) {
                System.err.println("padding 函数需要至少 2 个参数: str, length");
                return null;
            }
            
            String str;
            String padChar = " ";
            String direction = "";
            int length;

            // Nuclei 语义兼容：
            // padding(str, padChar, length[, direction])
            // pad_left(str, padChar, length)
            // pad_right(str, padChar, length)
            if (args.length >= 3 && looksLikeInteger(DslEvaluatorRefactored.resolveValueOrFunction(args[2], context))) {
                str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                padChar = resolvePaddingArgument(args[1], context);
                length = Integer.parseInt(DslEvaluatorRefactored.resolveValueOrFunction(args[2], context));
                if (args.length >= 4) {
                    direction = DslUtils.cleanStringValue(args[3]);
                }
            } else {
                str = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                length = Integer.parseInt(DslEvaluatorRefactored.resolveValueOrFunction(args[1], context));
                if (args.length >= 3) {
                    padChar = resolvePaddingArgument(args[2], context);
                }
                if (args.length >= 4) {
                    direction = DslUtils.cleanStringValue(args[3]);
                }
            }

            if (str == null) str = "";
            if (padChar == null || padChar.isEmpty()) padChar = " ";
            
            if (str.length() >= length) {
                return str;
            }
            
            int padCount = length - str.length();
            StringBuilder padding = new StringBuilder();
            while (padding.length() < padCount) {
                padding.append(padChar);
            }
            String padStr = padding.substring(0, padCount);
            
            // 根据函数名决定填充方向
            if ("pad_right".equals(functionName) || "suffix".equalsIgnoreCase(direction)) {
                return str + padStr;
            } else {
                // pad_left 或 padding 默认左填充
                return padStr + str;
            }
            
        } catch (Exception e) {
            System.err.println("评估 padding 函数失败: " + e.getMessage());
            return null;
        }
    }

    private static String resolvePaddingArgument(String arg, Map<String, Object> context) {
        String resolved = DslEvaluatorRefactored.resolveValueOrFunction(arg, context);
        if (resolved != null) {
            return resolved;
        }
        return DslUtils.cleanStringValue(arg);
    }

    private static String resolveStringArgument(String arg, Map<String, Object> context) {
        String resolved = DslEvaluatorRefactored.resolveValueOrFunction(arg, context);
        if (resolved == null) {
            resolved = DslUtils.cleanStringValue(arg);
        }
        if (resolved == null || context == null || context.isEmpty()) {
            return resolved;
        }

        String result = resolved;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private static boolean looksLikeInteger(String value) {
        if (value == null) {
            return false;
        }
        return value.trim().matches("^-?\\d+$");
    }
    
    /**
     * 评估 HMAC 函数
     * hmac(algorithm, data, key) - 通用 HMAC
     * hmac_sha1(data, key) - HMAC-SHA1
     * hmac_sha256(data, key) - HMAC-SHA256
     * hmac_sha512(data, key) - HMAC-SHA512
     * hmac_md5(data, key) - HMAC-MD5
     */
    private static String evaluateHmacFunction(String expression, Map<String, Object> context, String functionName) {
        try {
            String[] args = DslUtils.extractFunctionArgs(expression);
            
            String algorithm;
            String data;
            String key;
            
            if ("hmac".equals(functionName)) {
                // hmac(algorithm, data, key)
                if (args.length < 3) {
                    System.err.println("hmac 函数需要 3 个参数: algorithm, data, key");
                    return null;
                }
                algorithm = DslUtils.cleanStringValue(args[0]);
                data = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                key = DslEvaluatorRefactored.resolveValueOrFunction(args[2], context);
            } else {
                // hmac_xxx(data, key)
                if (args.length < 2) {
                    System.err.println(functionName + " 函数需要 2 个参数: data, key");
                    return null;
                }
                data = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                key = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                
                // 根据函数名确定算法
                switch (functionName) {
                    case "hmac_sha1":
                        algorithm = "sha1";
                        break;
                    case "hmac_sha256":
                        algorithm = "sha256";
                        break;
                    case "hmac_sha512":
                        algorithm = "sha512";
                        break;
                    case "hmac_md5":
                        algorithm = "md5";
                        break;
                    default:
                        algorithm = "sha256";
                }
            }
            
            if (data == null || key == null) {
                System.err.println("HMAC 函数参数不能为空");
                return null;
            }
            
            // 调用 DslFunctionExtended 中的 HMAC 实现
            switch (algorithm.toLowerCase()) {
                case "sha1":
                    return DslFunctionExtended.hmacSha1(data, key);
                case "sha256":
                    return DslFunctionExtended.hmacSha256(data, key);
                case "sha512":
                    return DslFunctionExtended.hmacSha512(data, key);
                case "md5":
                    return DslFunctionExtended.hmacMd5(data, key);
                default:
                    System.err.println("不支持的 HMAC 算法: " + algorithm);
                    return null;
            }
            
        } catch (Exception e) {
            System.err.println("评估 HMAC 函数失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * IP地址格式化（支持11种格式）
     * 参考mapcidr的IP Format Index：
     * 1: 十进制 (2130706433)
     * 2: 十六进制 (0x7f000001)
     * 3: 八进制 (017700000001)
     * 4: 二进制 (01111111000000000000000000000001)
     * 5: IPv6 (::ffff:127.0.0.1)
     * 6: IPv6压缩 (::ffff:7f00:1)
     * 7: 点分十六进制 (0x7f.0x00.0x00.0x01)
     * 8: 点分八进制 (0177.0000.0000.0001)
     * 9: 混合表示 (127.0.0x0001)
     * 10: URL编码 (127%2E0%2E0%2E1)
     * 11: IPv4映射IPv6 (0:0:0:0:0:ffff:7f00:0001)
     */
    private static String formatIpAddress(String ip, int formatIndex) {
        try {
            // 解析IPv4地址
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return ip; // 不是标准IPv4，返回原值
            }

            long ipNum = 0;
            for (int i = 0; i < 4; i++) {
                ipNum = ipNum * 256 + Long.parseLong(parts[i]);
            }

            switch (formatIndex) {
                case 1: // 十进制
                    return String.valueOf(ipNum);
                case 2: // 十六进制
                    return "0x" + String.format("%08x", ipNum);
                case 3: // 八进制
                    return "0" + String.format("%011o", ipNum);
                case 4: // 二进制
                    return String.format("%32s", Long.toBinaryString(ipNum)).replace(' ', '0');
                case 5: // IPv6
                    return "::ffff:" + ip;
                case 6: // IPv6压缩
                    return String.format("::ffff:%02x%02x:%02x%02x",
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                case 7: // 点分十六进制
                    return String.format("0x%02x.0x%02x.0x%02x.0x%02x",
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                case 8: // 点分八进制
                    return String.format("0%03o.0%03o.0%03o.0%03o",
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                case 9: // 混合表示 (127.0.0x0001)
                    int lastTwo = Integer.parseInt(parts[2]) * 256 + Integer.parseInt(parts[3]);
                    return parts[0] + "." + parts[1] + ".0x" + String.format("%04x", lastTwo);
                case 10: // URL编码
                    return ip.replace(".", "%2E");
                case 11: // IPv4映射IPv6长格式
                    return String.format("0:0:0:0:0:ffff:%02x%02x:%02x%02x",
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                default:
                    return ip; // 未知格式，返回原值
            }
        } catch (Exception e) {
            return ip; // 格式化失败，返回原值
        }
    }
}
