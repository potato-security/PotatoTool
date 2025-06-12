package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 响应匹配器，用于匹配HTTP响应是否符合条件
 */
public class ResponseMatcher {

    /**
     * 匹配HTTP响应是否符合条件列表
     * @param response HTTP响应对象
     * @param matchers 匹配器列表
     * @param condition 匹配条件（AND/OR）
     * @return 是否匹配成功
     */
    public static boolean matchResponse(CustomHttpResponse response, List<PocObj.Matcher> matchers, PocObj.MatchersCondition condition) {
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        // AND条件：所有匹配器都必须匹配成功
        if (condition == PocObj.MatchersCondition.AND) {
            for (PocObj.Matcher matcher : matchers) {
                if (!matchSingleMatcher(response, matcher)) {
                    return false;
                }
            }
            return true;
        } 
        // OR条件：任意一个匹配器匹配成功即可
        else if (condition == PocObj.MatchersCondition.OR) {
            for (PocObj.Matcher matcher : matchers) {
                if (matchSingleMatcher(response, matcher)) {
                    return true;
                }
            }
            return false;
        }
        
        return false;
    }

    /**
     * 匹配单个匹配器
     * @param response HTTP响应对象
     * @param matcher 匹配器
     * @return 是否匹配成功
     */
    private static boolean matchSingleMatcher(CustomHttpResponse response, PocObj.Matcher matcher) {
        if (matcher == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }

        String part = matcher.getPart();
        String content = VariableExtractor.getResponsePart(response, part);
        byte[] contentByte = response.getByteArray();
        
        if (content == null) {
            return false;
        }

        PocObj.MatcherType type = matcher.getType();
        List<String> values = matcher.getValues();
        boolean caseInsensitive = matcher.isCaseInsensitive();
        PocObj.OperationType operation = matcher.getOperation();

        switch (type) {
            case STATUS:
                try {
                    return matchStatus(response.getResponseCode(), values);
                }catch (Exception e){
                    return false;
                }
            case SIZE:
                int size = content.length();
                return matchSize(size, values, operation);
            case WORD:
                return matchWord(content, values, caseInsensitive);
            case REGEX:
                return matchRegex(content, values, caseInsensitive);
            case BINARY:
                return matchBinary(contentByte, values);
            case HASH:
                return matchHash(contentByte, values, operation);
            case JSON:
                return JsonExtractor.matchJson(content, values);
            case DSL:
                return DslEvaluatorRefactored.matchDslWithNestedMatchers(values, response, response);
            case TIME:
                return matchTime(response.getResponseTime(), values, operation);
            case GROUP:
                // 处理子匹配器组
                if (matcher.getSubMatchers() != null && !matcher.getSubMatchers().isEmpty()) {
                    List<PocObj.Matcher> subMatchers = matcher.getSubMatchers();
                    String condition = matcher.getCondition();
                    PocObj.MatchersCondition matchersCondition = PocObj.MatchersCondition.AND;
                    if (condition != null && condition.equalsIgnoreCase("OR")) {
                        matchersCondition = PocObj.MatchersCondition.OR;
                    }
                    return matchResponse(response, subMatchers, matchersCondition);
                }
                return false;
            default:
                return false;
        }
    }


    /**
     * 匹配HTTP状态码
     * @param statusCode 实际状态码
     * @param values 期望的状态码列表
     * @return 是否匹配成功
     */
    private static boolean matchStatus(int statusCode, List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String value : values) {
            try {
                int expectedStatus = Integer.parseInt(value.trim());
                if (statusCode == expectedStatus) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
            }
        }
        
        return false;
    }

    /**
     * 匹配响应大小
     * @param size 实际大小
     * @param values 期望的大小列表
     * @param operation 比较操作类型
     * @return 是否匹配成功
     */
    private static boolean matchSize(int size, List<String> values, PocObj.OperationType operation) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String value : values) {
            try {
                int targetSize = Integer.parseInt(value.trim());

                switch (operation) {
                    case NOT_EQUAL:
                        if (size != targetSize) {
                            return true;
                        }
                        break;
                    case GREATER:
                        if (size > targetSize) {
                            return true;
                        }
                        break;
                    case LESS:
                        if (size < targetSize) {
                            return true;
                        }
                        break;
                    case GREATER_EQUAL:
                        if (size >= targetSize) {
                            return true;
                        }
                        break;
                    case LESS_EQUAL:
                        if (size <= targetSize) {
                            return true;
                        }
                        break;
                    default:
                        if (size == targetSize) {
                            return true;
                        }
                        break;
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
            }
        }
        
        return false;
    }

    /**
     * 匹配字符串是否包含指定单词
     * @param content 内容
     * @param values 期望包含的单词列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private static boolean matchWord(String content, List<String> values, boolean caseInsensitive) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        String contentToMatch = caseInsensitive ? content.toLowerCase() : content;
        
        for (String value : values) {
            String valueToMatch = caseInsensitive ? value.toLowerCase() : value;
            if (contentToMatch.contains(valueToMatch)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 匹配字符串是否符合正则表达式
     * @param content 内容
     * @param values 正则表达式列表
     * @param caseInsensitive 是否忽略大小写
     * @return 是否匹配成功
     */
    private static boolean matchRegex(String content, List<String> values, boolean caseInsensitive) {
        if (content == null || values == null || values.isEmpty()) {
            return false;
        }

        for (String regex : values) {
            try {
                Pattern pattern = caseInsensitive 
                        ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) 
                        : Pattern.compile(regex);
                
                if (pattern.matcher(content).find()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                // 忽略非法正则表达式
            }
        }
        
        return false;
    }

    /**
     * 匹配二进制数据是否存在
     * @param contentByte 内容
     * @param values 十六进制字符串列表
     * @return 是否匹配成功
     */
    private static boolean matchBinary(byte[] contentByte, List<String> values) {
        if (contentByte == null || values == null || values.isEmpty()) {
            return false;
        }

        for (String hexString : values) {
            try {
                String hexContent = bytesToHexString(contentByte);
                if (hexContent.contains(hexString.toLowerCase())) {
                    return true;
                }
            } catch (Exception e) {
                // 忽略非法十六进制字符串
            }
        }
        
        return false;
    }

    /**
     * 匹配内容的哈希值
     * @param contentByte 内容
     * @param values 期望的哈希值列表
     * @param operation 哈希算法类型
     * @return 是否匹配成功
     */
    private static boolean matchHash(byte[] contentByte, List<String> values, PocObj.OperationType operation) {
        if (contentByte == null || values == null || values.isEmpty()) {
            return false;
        }

        String md5Hash = calculateMD5(contentByte);
        String sha1Hash = calculateSHA1(contentByte);

        for (String value : values) {
            if (value.equalsIgnoreCase(md5Hash) || value.equalsIgnoreCase(sha1Hash)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 匹配响应时间
     * @param responseTime 实际响应时间
     * @param values 期望的响应时间列表
     * @param operation 比较操作类型
     * @return 是否匹配成功
     */
    private static boolean matchTime(long responseTime, List<String> values, PocObj.OperationType operation) {
        if (values == null || values.isEmpty()) {
            return false;
        }

        for (String value : values) {
            try {
                long targetTime = Long.parseLong(value.trim());

                switch (operation) {
                    case NOT_EQUAL:
                        if (responseTime != targetTime) {
                            return true;
                        }
                        break;
                    case GREATER:
                        if (responseTime > targetTime) {
                            return true;
                        }
                        break;
                    case LESS:
                        if (responseTime < targetTime) {
                            return true;
                        }
                        break;
                    case GREATER_EQUAL:
                        if (responseTime >= targetTime) {
                            return true;
                        }
                        break;
                    case LESS_EQUAL:
                        if (responseTime <= targetTime) {
                            return true;
                        }
                        break;
                    default:
                        if (responseTime == targetTime) {
                            return true;
                        }
                }
            } catch (NumberFormatException e) {
                // 忽略非法值
            }
        }
        
        return false;
    }


    /**
     * 计算字符串的MD5哈希值
     * @param contentByte 内容
     * @return MD5哈希值
     */
    private static String calculateMD5(byte[] contentByte) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(contentByte);
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * 计算字符串的SHA1哈希值
     * @param contentByte 内容
     * @return SHA1哈希值
     */
    private static String calculateSHA1(byte[] contentByte) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(contentByte);
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
} 