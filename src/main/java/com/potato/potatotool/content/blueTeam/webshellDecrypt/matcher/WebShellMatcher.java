package com.potato.potatotool.content.blueTeam.webshellDecrypt.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.potato.potatotool.utils.data.StrUtils.*;

/**
 * WebShell特征匹配器
 * 用于识别各种WebShell管理工具的特征
 * 支持哥斯拉、冰蝎、蚁剑、菜刀、Cknife等主流WebShell工具的特征识别
 * 支持phpspy、silic等主流WebShell大马的特征识别
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
    
    private static final String[] BINGXIE3_OR_FEATURES = {
        "txcwr1nnexzad0zaawmipazjh1bfbfththcjsluxwed",  // 响应
        "dfaxqv1lorchrqtlrlwmahwftag/m" // 请求
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

    private static final String[] YIJIAN_RESPONSE_FEATURES = {
        "server",   // header
        "date",
        "content-type",
        "connection",
        "vary",
        "x-powered-by",
        "content-length",
        "transfer-encoding",
        "->|",  // body
        "|<-"
    };

    private static final String[] NO_YIJIAN_RESPONSE_FEATURES = {
        "charset=utf-8"
    };

    private static final String[] YIJIAN_RESPONSE_OR_FEATURES = {
        "antsword/v",   // request header
        "antsuccess",   // response body
        "antoutput",
        "<b>notice</b>: use of undefined constant"
    };

    
    private static final String[] CAIDAO_OR_FEATURES = {
        "=@eval(base64_decode($_post[",
        ";z0=",
        ";z1="
    };

    private static final String[] CAIDAO_RESPONSE_FEATURES = {
        "[s]",
        "[e]",
        "x@y"
    };

    private static final String[] CKNIFE_OR_FEATURES = {
        "=@eval(bse64_decode($_post",
        ";z1=",
        ";z2=",
        ";z3="
    };

    private static final String[] NO_CKNIFE_FEATURES = {
        ";z0=",
    };

    private static final String[] CKNIFE_RESPONSE_FEATURES = {
        "server",   // header
        "date",
        "content-type",
        "connection",
        "vary",
        "x-powered-by",
        "content-length",
        "->|",  // body
        "|<-"
    };

    private static final String[] NO_CKNIFE_RESPONSE_FEATURES = {
        "charset=utf-8" // header
    };

    private static final String[] PHPSPY_FEATURES = {
        "loginpass=phpspy", // cookie
        "badlog="
    };

    private static final String[] PHPSPY_RESPONSE_FEATURES = {
        "www.4ngel.net",
        "phpspy 2014 final"
    };

    private static final String[] SILIC_FEATURES = {
        "postpass", // cookie字段
        "serveru",
        "serverp"
    };

    private static final String[] SILIC_RESPONSE_FEATURES = {
        "1212.ip138.com/ic.asp"
    };
    
    private static final String[] HEADER_BINGXIE2_FEATURES = {
        "accept: text/html, image/gif, image/jpeg, ; q=.2, /; q=.2",
        "display_error",
        "cookie: phpsessid=; path=/"
    };
    
    private static final String[] HEADER_BINGXIE3_FEATURES = {
        "accept: applicaation/json, text/javascript, */*; q=0.01",
        "connection: keep-alive"
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
        } else if (matchBingxie(lowerOriginal, lowerDecrypted, aesKey)) {
            matchedFeatures.add("冰蝎");
        } else if (matchYijian(lowerOriginal, lowerDecrypted)) {
            matchedFeatures.add("蚁剑");
        } else if (matchCaidao(lowerDecrypted)) {
            matchedFeatures.add("菜刀");
        } else if (matchCknife(lowerDecrypted)) {
            matchedFeatures.add("Cknife");
        } else if (matchPhpspy(lowerDecrypted)) {
            matchedFeatures.add("phpspy大马");
        } else if (matchSilic(lowerDecrypted)) {
            matchedFeatures.add("silic大马");
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
    private boolean matchBingxie(String lowerOriginal, String content, String aesKey) {
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

        // 冰蝎3、4特征
        if (lowerOriginal.contains("yc8MNtAHgYXhSjykK5u2E")) {
            return true;
        }

        // 冰蝎3特征
        if (containsAnyElements(lowerOriginal, BINGXIE3_OR_FEATURES)) {
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
        if (containsAllElements(decryptedContent, YIJIAN_URL_FEATURES)) {
            return true;
        }

        if (containsAllElements(decryptedContent, YIJIAN_RESPONSE_FEATURES) && !containsAllElements(decryptedContent, NO_YIJIAN_RESPONSE_FEATURES)){
            return true;
        }

        return containsAnyElements(decryptedContent, YIJIAN_RESPONSE_OR_FEATURES);
    }
    
    /**
     * 匹配菜刀特征
     * 检测中国菜刀WebShell特征
     * 
     * @param content 待检测内容（已转小写）
     * @return 是否匹配菜刀特征
     */
    private boolean matchCaidao(String content) {
        if (containsAnyElements(content, CAIDAO_OR_FEATURES)) {
            return true;
        }

        return containsAllElements(content, CAIDAO_RESPONSE_FEATURES);
    }

    /**
     * 匹配Cknife特征
     * 检测CknifeWebShell特征
     *
     * @param content 待检测内容（已转小写）
     * @return 是否匹配Cknife特征
     */
    private boolean matchCknife(String content) {
        if (containsAnyElements(content, CKNIFE_OR_FEATURES) && !containsAllElements(content, NO_CKNIFE_FEATURES)) {
            return true;
        }

        return containsAllElements(content, CKNIFE_RESPONSE_FEATURES) && !containsAllElements(content, NO_CKNIFE_RESPONSE_FEATURES);
    }

    /**
     * 匹配phpspy大马特征
     *
     * @param content 待检测内容（已转小写）
     * @return 是否匹配phpspy大马特征
     */
    private boolean matchPhpspy(String content) {
        if (containsAllElements(content, PHPSPY_FEATURES)) {
            return true;
        }

        return containsAllElements(content, PHPSPY_RESPONSE_FEATURES);
    }

    /**
     * 匹配silic大马特征
     *
     * @param content 待检测内容（已转小写）
     * @return 是否匹配silic大马特征
     */
    private boolean matchSilic(String content) {
        if (containsAllElements(content, SILIC_FEATURES)) {
            return true;
        }

        return containsAllElements(content, SILIC_RESPONSE_FEATURES);
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