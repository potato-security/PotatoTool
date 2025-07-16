package com.potato.potatotool.content.blueTeam.webshellDecrypt.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.potato.potatotool.utils.data.StrUtils.containsAllElements;

/**
 * WebShell特征匹配器
 * 用于识别各种WebShell管理工具的特征
 * 支持哥斯拉、冰蝎、蚁剑、菜刀等主流WebShell工具的特征识别
 * 
 * @author Potato
 * @version 2.1
 */
public class WebShellMatcher {
    
    // 预定义特征数组，提高性能
    private static final String[] GESILA_BASE64_FEATURES = {
        "=eval(base64_decode(strrev(urldecode("
    };
    
    private static final String[] GESILA_JSP_FEATURES = {
        "string xc=",
        "getclass().getclassloader()"
    };
    
    private static final String[] BINGXIE2_FEATURES = {
        "success",
        "status",
        "msg"
    };
    
    private static final String[] BINGXIE3_FEATURES = {
        "txcwr1nnexzad0zaawmipazjh1bfbfththcjsluxwed",
        "dfaxqv1lorchrqtlrlwmahwftag/m"
    };
    
    private static final String[] BINGXIE4_FEATURES = {
        "$post=decrypt(file_get_contents(\"php://input\"));",
        "eval($post);"
    };
    
    private static final String[] YIJIAN_BASE64_FEATURES = {
        "@ini_set(",
        "display_error",
        "@set_time_limit"
    };
    
    private static final String[] YIJIAN_BASE64B_FEATURES = {
        "=@eval(@base64_decode($_post["
    };
    
    private static final String[] YIJIAN_CHR_FEATURES = {
        "=%40eval(chr("
    };
    
    private static final String[] YIJIAN_HEX_FEATURES = {
        "unsafe",
        "hexasciiconver("
    };
    
    private static final String[] YIJIAN_ROT13_FEATURES = {
        "=@eval(@str_tor13($_post["
    };
    
    private static final String[] YIJIAN_URL_FEATURES = {
        "unsafe",
        "response.write(",
        "response.end();"
    };
    
    private static final String[] CAIDAO_FEATURES = {
        "=@eval(base64_decode($_post["
    };
    
    private static final String[] HEADER_BINGXIE2_FEATURES = {
        "Accept: text/html, image/gif, image/jpeg, ; q=.2, /; q=.2",
        "display_error",
        "Cookie: PHPSESSID=; path=/"
    };
    
    private static final String[] HEADER_BINGXIE3_FEATURES = {
        "Accept: applicaation/json, text/javascript, */*; q=0.01",
        "Connection: Keep-Alive"
    };
    
    // 冰蝎默认密钥
    private static final String BINGXIE_DEFAULT_KEY = "e45e329feb5d925b";
    
    /**
     * WebShell管理工具特征匹配
     * 
     * @param originalContent 原始数据
     * @param decryptedContent 解密后数据
     * @param encodeModes 编码模式列表
     * @param aesKey AES解密使用的密钥（可选，用于冰蝎检测）
     */
    public void matchWebShellFeatures(String originalContent, String decryptedContent, 
                                    List<List<String>> encodeModes, String aesKey) {
        if (originalContent == null || decryptedContent == null || encodeModes == null) {
            return;
        }
        
        String lowerOriginal = originalContent.toLowerCase(Locale.ROOT);
        String lowerDecrypted = decryptedContent.toLowerCase(Locale.ROOT);
        
        List<String> matchedFeatures = new ArrayList<>();
        
        // 按优先级进行特征匹配
        if (matchGesila(lowerDecrypted)) {
            matchedFeatures.add("哥斯拉");
        } else if (matchBingxie(lowerDecrypted, aesKey)) {
            matchedFeatures.add("冰蝎");
        } else if (matchYijian(lowerOriginal, lowerDecrypted)) {
            matchedFeatures.add("蚁剑");
        } else if (matchCaidao(lowerDecrypted)) {
            matchedFeatures.add("菜刀");
        }
        
        // 弱特征匹配（仅在没有强特征时进行）
        if (matchedFeatures.isEmpty()) {
            String weakFeature = matchWeakFeatures(lowerOriginal);
            if (weakFeature != null) {
                matchedFeatures.add(weakFeature);
            }
        }
        
        // 将匹配结果添加到编码模式列表的开头
        if (!matchedFeatures.isEmpty()) {
            encodeModes.add(0, matchedFeatures);
        }
    }
    
    /**
     * 匹配哥斯拉特征
     * 检测哥斯拉WebShell的多种特征模式
     * 
     * @param content 待检测内容（已转小写）
     * @return 是否匹配哥斯拉特征
     */
    private boolean matchGesila(String content) {
        // 检查MD5特征
        if (content.startsWith("流量中提取到md5(pass+md5(key)") || content.startsWith("pass=")) {
            return true;
        }
        
        // 检查methodName特征
        if (content.startsWith("methodname=")) {
            return true;
        }
        
        // 哥斯拉base64特征
        if (containsAllElements(content, GESILA_BASE64_FEATURES)) {
            return true;
        }
        
        // 哥斯拉JSP特征
        return containsAllElements(content, GESILA_JSP_FEATURES);
    }
    
    /**
     * 匹配冰蝎特征
     * 检测冰蝎WebShell的多个版本特征
     * 
     * @param content 待检测内容（已转小写）
     * @param aesKey AES解密使用的密钥
     * @return 是否匹配冰蝎特征
     */
    private boolean matchBingxie(String content, String aesKey) {
        // 检查AES密钥是否为冰蝎默认密钥
        if (BINGXIE_DEFAULT_KEY.equals(aesKey)) {
            return true;
        }
        
        // 检查内容中是否包含默认密钥
        if (content.contains(BINGXIE_DEFAULT_KEY)) {
            return true;
        }
        
        // 冰蝎2特征
        if (containsAllElements(content, BINGXIE2_FEATURES)) {
            return true;
        }
        
        // 冰蝎3特征
        if (containsAllElements(content, BINGXIE3_FEATURES)) {
            return true;
        }
        
        // 冰蝎4特征
        return containsAllElements(content, BINGXIE4_FEATURES);
    }
    
    /**
     * 匹配蚁剑特征
     * 检测蚁剑WebShell的多种编码方式特征
     * 
     * @param originalContent 原始内容（已转小写）
     * @param decryptedContent 解密后内容（已转小写）
     * @return 是否匹配蚁剑特征
     */
    private boolean matchYijian(String originalContent, String decryptedContent) {
        // 蚁剑base64特征
        if (containsAllElements(decryptedContent, YIJIAN_BASE64_FEATURES)) {
            return true;
        }
        
        // 蚁剑base64B特征
        if (containsAllElements(decryptedContent, YIJIAN_BASE64B_FEATURES)) {
            return true;
        }
        
        // 蚁剑chr特征（检查原始内容）
        if (containsAllElements(originalContent, YIJIAN_CHR_FEATURES)) {
            return true;
        }
        
        // 蚁剑hex特征
        if (containsAllElements(decryptedContent, YIJIAN_HEX_FEATURES)) {
            return true;
        }
        
        // 蚁剑rot13特征
        if (containsAllElements(decryptedContent, YIJIAN_ROT13_FEATURES)) {
            return true;
        }
        
        // 蚁剑url特征
        return containsAllElements(decryptedContent, YIJIAN_URL_FEATURES);
    }
    
    /**
     * 匹配菜刀特征
     * 检测中国菜刀WebShell特征
     * 
     * @param content 待检测内容（已转小写）
     * @return 是否匹配菜刀特征
     */
    private boolean matchCaidao(String content) {
        return containsAllElements(content, CAIDAO_FEATURES);
    }
    
    /**
     * 匹配弱特征
     * 基于HTTP头部信息进行弱特征匹配
     * 
     * @param originalContent 原始内容（已转小写）
     * @return 匹配的弱特征名称，如果没有匹配则返回null
     */
    private String matchWeakFeatures(String originalContent) {
        // 冰蝎2弱特征
        if (containsAllElements(originalContent, HEADER_BINGXIE2_FEATURES)) {
            return "弱特征:冰蝎2";
        }
        
        // 冰蝎3弱特征
        if (containsAllElements(originalContent, HEADER_BINGXIE3_FEATURES)) {
            return "弱特征:冰蝎3";
        }
        
        return null;
    }
}