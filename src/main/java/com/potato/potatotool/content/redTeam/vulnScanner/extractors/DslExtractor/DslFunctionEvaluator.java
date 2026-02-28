package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * DSL函数评估器
 * 负责评估各种DSL函数调用
 */
public class DslFunctionEvaluator {

    /**
     * 评估contains函数
     */
    public static boolean evaluateContainsFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String searchValue = DslUtils.cleanStringValue(args[1]);

            if (target == null || searchValue == null) {
                return false;
            }

            boolean result = target.contains(searchValue);
            
            // 处理剩余的比较操作
            if (args.length > 2) {
                String remainingExpression = String.join(",", Arrays.copyOfRange(args, 2, args.length));
                return processComparisonOperator(String.valueOf(result), remainingExpression, context);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估contains函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估matches函数（正则匹配）
     */
    public static boolean evaluateMatchesFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String pattern = DslUtils.cleanStringValue(args[1]);

            if (target == null || pattern == null) {
                return false;
            }

            boolean result = Pattern.compile(pattern).matcher(target).find();
            
            // 处理剩余的比较操作
            if (args.length > 2) {
                String remainingExpression = String.join(",", Arrays.copyOfRange(args, 2, args.length));
                return processComparisonOperator(String.valueOf(result), remainingExpression, context);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估matches函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估len函数（长度）
     */
    public static boolean evaluateLenFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return false;
            }

            int length = target.length();
            context.put("__length_result", length);
            
            // 处理剩余的比较操作
            if (args.length > 1) {
                String remainingExpression = String.join(",", Arrays.copyOfRange(args, 1, args.length));
                return processComparisonOperator(String.valueOf(length), remainingExpression, context);
            }
            
            return length > 0;
            
        } catch (Exception e) {
            System.err.println("评估len函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估startsWith函数
     */
    public static boolean evaluateStartsWithFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String prefix = DslUtils.cleanStringValue(args[1]);

            if (target == null || prefix == null) {
                return false;
            }

            return target.startsWith(prefix);
            
        } catch (Exception e) {
            System.err.println("评估startsWith函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估endsWith函数
     */
    public static boolean evaluateEndsWithFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            String suffix = DslUtils.cleanStringValue(args[1]);

            if (target == null || suffix == null) {
                return false;
            }

            return target.endsWith(suffix);
            
        } catch (Exception e) {
            System.err.println("评估endsWith函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估toLowerCase函数并返回字符串值
     */
    public static String evaluateToLowerCaseFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.toLowerCase();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估toLowerCase函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估toUpperCase函数并返回字符串值
     */
    public static String evaluateToUpperCaseFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.toUpperCase();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估toUpperCase函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估base64函数并返回字符串值
     */
    public static String evaluateBase64Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = Base64.getEncoder().encodeToString(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return result;
            
        } catch (Exception e) {
            System.err.println("评估base64函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估md5函数并返回字符串值
     */
    public static String evaluateMd5Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(target.getBytes(DslConstants.DEFAULT_CHARSET));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            String result = sb.toString();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估md5函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估sha1函数
     */
    public static String evaluateSha1Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return bytesToHex(hashBytes);
            
        } catch (Exception e) {
            System.err.println("评估sha1函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估sha256函数
     */
    public static String evaluateSha256Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return bytesToHex(hashBytes);
            
        } catch (Exception e) {
            System.err.println("评估sha256函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估sha512函数
     */
    public static String evaluateSha512Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = md.digest(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return bytesToHex(hashBytes);
            
        } catch (Exception e) {
            System.err.println("评估sha512函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估mmh3函数（MurmurHash3）
     */
    public static String evaluateMmh3Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            // MurmurHash3 32位实现
            int hash = murmurHash3_32(target.getBytes(DslConstants.DEFAULT_CHARSET), 0);
            return String.valueOf(hash);
            
        } catch (Exception e) {
            System.err.println("评估mmh3函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估crc32函数
     */
    public static String evaluateCrc32Function(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            CRC32 crc32 = new CRC32();
            crc32.update(target.getBytes(DslConstants.DEFAULT_CHARSET));
            return String.valueOf(crc32.getValue());
            
        } catch (Exception e) {
            System.err.println("评估crc32函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * MurmurHash3 32位实现
     */
    private static int murmurHash3_32(byte[] data, int seed) {
        int m = 0x5bd1e995;
        int r = 24;
        int h = seed ^ data.length;
        int length = data.length;
        int length4 = length / 4;

        for (int i = 0; i < length4; i++) {
            int i4 = i * 4;
            int k = (data[i4] & 0xff) | ((data[i4 + 1] & 0xff) << 8)
                    | ((data[i4 + 2] & 0xff) << 16) | ((data[i4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        int offset = length4 * 4;
        switch (length % 4) {
            case 3:
                h ^= (data[offset + 2] & 0xff) << 16;
            case 2:
                h ^= (data[offset + 1] & 0xff) << 8;
            case 1:
                h ^= data[offset] & 0xff;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }



    /**
     * 评估substr函数并返回字符串值
     */
    public static String evaluateSubstrFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            int start = DslUtils.safeParseInt(args[1], 0);
            
            // 处理负数索引
            if (start < 0) {
                start = Math.max(0, target.length() + start);
            }
            
            if (start >= target.length()) {
                return "";
            }

            String result;
            if (args.length >= 3) {
                int length = DslUtils.safeParseInt(args[2], target.length() - start);
                int endIndex = Math.min(start + length, target.length());
                result = target.substring(start, endIndex);
            } else {
                result = target.substring(start);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估substr函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估trim函数并返回字符串值
     */
    public static String evaluateTrimFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result = target.trim();
            return result;
            
        } catch (Exception e) {
            System.err.println("评估trim函数失败: " + e.getMessage());
            return null;
        }
    }



    /**
     * 评估unixtime函数
     */
    public static boolean evaluateUnixtimeFunction(String expression, Map<String, Object> context) {
        try {
            long unixTime = System.currentTimeMillis() / 1000;
            
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length > 0) {
                String remainingExpression = String.join(",", args);
                return processComparisonOperator(String.valueOf(unixTime), remainingExpression, context);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("评估unixtime函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估urldecode函数并返回字符串值
     */
    public static String evaluateUrlDecodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            String result;
            try {
                result = URLDecoder.decode(target, DslConstants.DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException e) {
                System.err.println("URL解码失败: " + e.getMessage());
                return null;
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("评估urldecode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估base64_decode函数
     */
    public static String evaluateBase64DecodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            byte[] decodedBytes = Base64.getDecoder().decode(target);
            return new String(decodedBytes, DslConstants.DEFAULT_CHARSET);
            
        } catch (Exception e) {
            System.err.println("评估base64_decode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估url_encode函数
     */
    public static String evaluateUrlEncodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            return URLEncoder.encode(target, DslConstants.DEFAULT_CHARSET);
            
        } catch (Exception e) {
            System.err.println("评估url_encode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估hex_encode函数
     */
    public static String evaluateHexEncodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            return bytesToHex(target.getBytes(DslConstants.DEFAULT_CHARSET));
            
        } catch (Exception e) {
            System.err.println("评估hex_encode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估hex_decode函数
     */
    public static String evaluateHexDecodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            // 移除可能的空格和0x前缀
            String hex = target.replace(" ", "").replace("0x", "").replace("0X", "");
            byte[] bytes = hexToBytes(hex);
            return new String(bytes, DslConstants.DEFAULT_CHARSET);
            
        } catch (Exception e) {
            System.err.println("评估hex_decode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估html_escape函数
     */
    public static String evaluateHtmlEscapeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            return target
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
            
        } catch (Exception e) {
            System.err.println("评估html_escape函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估html_unescape函数
     */
    public static String evaluateHtmlUnescapeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            return target
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&#x2F;", "/")
                .replace("&#39;", "'");
            
        } catch (Exception e) {
            System.err.println("评估html_unescape函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估bin_to_dec函数（二进制转十进制）
     */
    public static String evaluateBinToDecFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            // 移除0b前缀
            String binary = target.replace("0b", "").replace("0B", "");
            int decimal = Integer.parseInt(binary, 2);
            return String.valueOf(decimal);
            
        } catch (Exception e) {
            System.err.println("评估bin_to_dec函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估dec_to_hex函数（十进制转十六进制）
     */
    public static String evaluateDecToHexFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            int decimal = Integer.parseInt(target);
            return Integer.toHexString(decimal);
            
        } catch (Exception e) {
            System.err.println("评估dec_to_hex函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();

        // 处理奇数长度：前面补 0
        if (len % 2 != 0) {
            hex = "0" + hex;
            len = hex.length();
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }



    /**
     * 评估regex函数
     */
    public static boolean evaluateRegexFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                System.err.println("regex函数需要至少2个参数: pattern, target");
                return false;
            }

            String pattern = args[0].trim();
            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
            
            System.out.println("[DEBUG] regex函数: pattern原始=" + pattern + ", target前30字符=" + 
                (target != null ? target.substring(0, Math.min(30, target.length())) : "null"));
            
            if (target == null) {
                return false;
            }

            // 移除引号
            if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            } else if (pattern.startsWith("'") && pattern.endsWith("'")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }
            
            System.out.println("[DEBUG] regex函数: pattern去引号后=" + pattern);
            
            // 编译并匹配正则表达式
            Pattern regexPattern = Pattern.compile(pattern);
            boolean matches = regexPattern.matcher(target).find();
            
            System.out.println("[DEBUG] regex函数: 匹配结果=" + matches);
            
            return matches;
            
        } catch (Exception e) {
            System.err.println("评估regex函数失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 评估contains_any函数 - 检查目标字符串是否包含任意一个搜索值
     * contains_any(target, "value1", "value2", "value3")
     */
    public static boolean evaluateContainsAnyFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return false;
            }

            // 检查是否包含任意一个搜索值
            for (int i = 1; i < args.length; i++) {
                String searchValue = DslUtils.cleanStringValue(args[i]);
                if (searchValue != null && target.contains(searchValue)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("评估contains_any函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估equals_any函数 - 检查目标字符串是否精确等于任意一个给定值
     * equals_any(target, "value1", "value2", "value3")
     */
    public static boolean evaluateEqualsAnyFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return false;
            }

            for (int i = 1; i < args.length; i++) {
                String value = DslUtils.cleanStringValue(args[i]);
                if (value != null && target.equals(value)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("评估equals_any函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估contains_all函数 - 检查目标字符串是否包含所有搜索值
     * contains_all(target, "value1", "value2", "value3")
     */
    public static boolean evaluateContainsAllFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                return false;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return false;
            }

            // 检查是否包含所有搜索值
            for (int i = 1; i < args.length; i++) {
                String searchValue = DslUtils.cleanStringValue(args[i]);
                if (searchValue == null || !target.contains(searchValue)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("评估contains_all函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估compare_versions函数 - 比较版本号
     * compare_versions(version, "<= 1.2.3")
     * compare_versions(version, ">= 1.0.0", "<= 2.0.0") - 支持范围比较
     */
    public static boolean evaluateCompareVersionsFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 2) {
                System.err.println("compare_versions函数需要至少2个参数: version, comparison");
                return false;
            }

            // 获取版本号
            String versionStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (versionStr == null || versionStr.isEmpty()) {
                return false;
            }

            // 处理多个比较条件（AND关系）
            for (int i = 1; i < args.length; i++) {
                String comparison = DslUtils.cleanStringValue(args[i]).trim();
                if (!compareVersion(versionStr, comparison)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("评估compare_versions函数失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 比较单个版本号条件
     * @param version 版本号字符串（如 "1.2.3"）
     * @param comparison 比较表达式（如 ">= 1.0.0" 或 "<= 2.0.0"）
     */
    private static boolean compareVersion(String version, String comparison) {
        try {
            // 解析比较操作符
            String operator = null;
            String targetVersion = null;
            
            if (comparison.startsWith("<=")) {
                operator = "<=";
                targetVersion = comparison.substring(2).trim();
            } else if (comparison.startsWith(">=")) {
                operator = ">=";
                targetVersion = comparison.substring(2).trim();
            } else if (comparison.startsWith("!=")) {
                operator = "!=";
                targetVersion = comparison.substring(2).trim();
            } else if (comparison.startsWith("==")) {
                operator = "==";
                targetVersion = comparison.substring(2).trim();
            } else if (comparison.startsWith("<")) {
                operator = "<";
                targetVersion = comparison.substring(1).trim();
            } else if (comparison.startsWith(">")) {
                operator = ">";
                targetVersion = comparison.substring(1).trim();
            } else {
                // 默认为相等比较
                operator = "==";
                targetVersion = comparison.trim();
            }

            // 比较版本号
            int compareResult = compareVersionStrings(version, targetVersion);
            
            switch (operator) {
                case "==": return compareResult == 0;
                case "!=": return compareResult != 0;
                case ">": return compareResult > 0;
                case "<": return compareResult < 0;
                case ">=": return compareResult >= 0;
                case "<=": return compareResult <= 0;
                default: return false;
            }
            
        } catch (Exception e) {
            System.err.println("版本比较失败: " + version + " " + comparison);
            return false;
        }
    }

    /**
     * 比较两个版本号字符串
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0表示相等
     */
    private static int compareVersionStrings(String v1, String v2) {
        // 清理版本号（移除前缀如 'v'）
        v1 = v1.replaceFirst("^[vV]", "").trim();
        v2 = v2.replaceFirst("^[vV]", "").trim();
        
        // 分割版本号
        String[] parts1 = v1.split("[.\\-_]");
        String[] parts2 = v2.split("[.\\-_]");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = 0;
            int num2 = 0;
            
            if (i < parts1.length) {
                String part1 = parts1[i].replaceAll("[^0-9]", "");
                if (!part1.isEmpty()) {
                    num1 = Integer.parseInt(part1);
                }
            }
            
            if (i < parts2.length) {
                String part2 = parts2[i].replaceAll("[^0-9]", "");
                if (!part2.isEmpty()) {
                    num2 = Integer.parseInt(part2);
                }
            }
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        
        return 0;
    }

    /**
     * 解析值（从上下文中获取或直接返回字面值）
     */
    public static String resolveValue(String value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        value = value.trim();
        
        // 如果是字符串字面值（被引号包围），直接返回内容
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // 检查是否是函数调用
        if (DslConstants.FUNCTION_PATTERN.matcher(value).find()) {
            return DslEvaluatorRefactored.evaluateFunctionForValue(value, context);
        }

        // 尝试从上下文中获取值
        Object contextValue = context.get(value);
        if (contextValue != null) {
            return contextValue.toString();
        }

        // 如果上下文中没有，返回原值
        return value;
    }
    

    
    /**
     * 统一的函数评估方法（优化版本 - 使用统一的函数类型管理器）
     */
    public static boolean evaluateFunction(String expression, Map<String, Object> context) {
        try {
            String functionName = DslUtils.extractFunctionName(expression);
            if (functionName == null) {
                return false;
            }

            String normalizedFunctionName = functionName.toLowerCase();
            
            // 检查是否为已知函数
            if (!DslFunctionTypeManager.isKnownFunction(normalizedFunctionName)) {
                System.err.println("未知的函数: " + functionName);
                return false;
            }
            
            // 使用统一的函数评估接口
            return DslFunctionTypeManager.evaluateFunctionAsBoolean(normalizedFunctionName, expression, context);
            
        } catch (Exception e) {
            System.err.println("函数评估失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理比较操作符
     */
    private static boolean processComparisonOperator(String leftValue, String expression, Map<String, Object> context) {
        try {
            String[] parts = DslExpressionParser.parseComparisonExpression(expression);
            if (parts.length != 3 || parts[1].isEmpty()) {
                return true; // 没有比较操作符，默认返回true
            }

            String operator = parts[1];
            String rightValue = DslEvaluatorRefactored.resolveValueOrFunction(parts[2], context);

            return compareValues(leftValue, operator, rightValue);
            
        } catch (Exception e) {
            System.err.println("处理比较操作符失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 比较两个值
     */
    private static boolean compareValues(String left, String operator, String right) {
        if (left == null || right == null) {
            return false;
        }

        // 尝试数值比较
        if (DslUtils.isNumeric(left) && DslUtils.isNumeric(right)) {
            double leftNum = DslUtils.safeParseDouble(left, 0);
            double rightNum = DslUtils.safeParseDouble(right, 0);
            
            switch (operator) {
                case "==": return leftNum == rightNum;
                case "!=": return leftNum != rightNum;
                case ">=": return leftNum >= rightNum;
                case "<=": return leftNum <= rightNum;
                case ">": return leftNum > rightNum;
                case "<": return leftNum < rightNum;
                default: return false;
            }
        }

        // 字符串比较
        switch (operator) {
            case "==": return left.equals(right);
            case "!=": return !left.equals(right);
            case ">=": return left.compareTo(right) >= 0;
            case "<=": return left.compareTo(right) <= 0;
            case ">": return left.compareTo(right) > 0;
            case "<": return left.compareTo(right) < 0;
            default: return false;
        }
    }

    /**
     * 评估rand_text_alpha函数 - 生成随机字母字符串
     */
    public static String evaluateRandTextAlphaFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            // 获取长度参数
            String lengthStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            int length = Integer.parseInt(lengthStr);
            
            if (length <= 0) {
                return "";
            }

            // 生成随机字母字符串（大小写字母）
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            return generateRandomString(chars, length);
            
        } catch (Exception e) {
            System.err.println("评估rand_text_alpha函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_text_alphanumeric函数 - 生成随机字母数字字符串
     */
    public static String evaluateRandTextAlphanumericFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            // 获取长度参数
            String lengthStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            int length = Integer.parseInt(lengthStr);
            
            if (length <= 0) {
                return "";
            }

            // 生成随机字母数字字符串
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            return generateRandomString(chars, length);
            
        } catch (Exception e) {
            System.err.println("评估rand_text_alphanumeric函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_text_numeric函数 - 生成随机数字字符串
     */
    public static String evaluateRandTextNumericFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            // 获取长度参数
            String lengthStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            int length = Integer.parseInt(lengthStr);

            if (length <= 0) {
                return "";
            }

            // 生成随机数字字符串
            String chars = "0123456789";
            return generateRandomString(chars, length);

        } catch (Exception e) {
            System.err.println("评估rand_text_numeric函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_base函数 - 生成基于指定字符集的随机字符串
     * rand_base(length) - 使用默认字母数字字符集
     * rand_base(length, charset) - 使用自定义字符集
     */
    public static String evaluateRandBaseFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            // 获取长度参数
            String lengthStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            int length = Integer.parseInt(lengthStr);

            if (length <= 0) {
                return "";
            }

            // 获取字符集参数(可选),默认为字母数字
            String charset;
            if (args.length >= 2) {
                charset = DslUtils.cleanStringValue(args[1]);
            } else {
                charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            }

            return generateRandomString(charset, length);

        } catch (Exception e) {
            System.err.println("评估rand_base函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_int函数 - 生成随机整数
     * rand_int(max) - 生成0到max之间的随机整数
     * rand_int(min, max) - 生成min到max之间的随机整数
     */
    public static String evaluateRandIntFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            long min = 0;
            long max;

            if (args.length == 1) {
                // 只有一个参数,表示0到max
                String maxStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                max = Long.parseLong(maxStr);

                // 限制最大值防止性能问题和整数溢出
                if (max > Integer.MAX_VALUE) {
                    System.err.println("警告: rand_int 参数过大 (" + max + "), 限制为 Integer.MAX_VALUE");
                    max = Integer.MAX_VALUE;
                }
            } else {
                // 两个参数,表示min到max
                String minStr = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
                String maxStr = DslEvaluatorRefactored.resolveValueOrFunction(args[1], context);
                min = Long.parseLong(minStr);
                max = Long.parseLong(maxStr);

                // 限制范围
                if (min > Integer.MAX_VALUE) {
                    min = Integer.MAX_VALUE;
                }
                if (max > Integer.MAX_VALUE) {
                    max = Integer.MAX_VALUE;
                }
            }

            // 生成随机整数
            Random random = new Random();
            long randomInt = Math.abs(random.nextLong()) % (max - min + 1) + min;
            return String.valueOf(randomInt);

        } catch (Exception e) {
            System.err.println("评估rand_int函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_char函数 - 生成单个随机字符
     * rand_char() - 使用默认字母数字字符集
     * rand_char(charset) - 使用自定义字符集
     */
    public static String evaluateRandCharFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);

            // 获取字符集参数(可选),默认为字母数字
            String charset;
            if (args.length >= 1) {
                charset = DslUtils.cleanStringValue(args[0]);
            } else {
                charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            }

            // 生成单个随机字符
            return generateRandomString(charset, 1);

        } catch (Exception e) {
            System.err.println("评估rand_char函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估rand_ip函数 - 生成随机IPv4地址
     */
    public static String evaluateRandIpFunction(String expression, Map<String, Object> context) {
        try {
            Random random = new Random();

            // 生成4个0-255之间的随机数
            int octet1 = random.nextInt(256);
            int octet2 = random.nextInt(256);
            int octet3 = random.nextInt(256);
            int octet4 = random.nextInt(256);

            return octet1 + "." + octet2 + "." + octet3 + "." + octet4;

        } catch (Exception e) {
            System.err.println("评估rand_ip函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估print函数 - 打印调试信息
     * print(value1, value2, ...) - 打印多个值并返回连接后的字符串
     */
    public static String evaluatePrintFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length == 0) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                String value = DslEvaluatorRefactored.resolveValueOrFunction(args[i], context);
                if (value != null) {
                    if (i > 0) {
                        result.append(" ");
                    }
                    result.append(value);
                }
            }

            String output = result.toString();
            System.out.println("[DSL Print] " + output);
            return output;

        } catch (Exception e) {
            System.err.println("评估print函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 评估unicode_decode函数 - Unicode解码
     */
    public static String evaluateUnicodeDecodeFunction(String expression, Map<String, Object> context) {
        try {
            String[] args = DslExpressionParser.parseContainsFunction(expression);
            if (args.length < 1) {
                return null;
            }

            String target = DslEvaluatorRefactored.resolveValueOrFunction(args[0], context);
            if (target == null) {
                return null;
            }

            // Unicode解码：将 Unicode 转义序列转换为实际字符
            return unescapeUnicode(target);
            
        } catch (Exception e) {
            System.err.println("评估unicode_decode函数失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成指定长度的随机字符串
     */
    private static String generateRandomString(String chars, int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Unicode解码辅助方法
     */
    private static String unescapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u' && i + 5 < input.length()) {
                try {
                    String hex = input.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                } catch (NumberFormatException e) {
                    // 如果解析失败，保留原始字符
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 私有构造函数，防止实例化
     */
    private DslFunctionEvaluator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}