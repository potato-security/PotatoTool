package com.potato.potatotool.content.blueTeam.webshell.decoder.utils;

import com.potato.potatotool.utils.data.StrUtils;

/**
 * JWT解密工具类
 * 
 * @author Potato
 * @version 2.0
 */
public class JWTDecrypt {
    
    private static final StrUtils str = new StrUtils();
    
    /**
     * JWT解密方法
     * @param content 待解密的JWT内容
     * @return 解密后的内容，如果解密失败返回null
     */
    public static String decrypt(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // JWT格式检查：以eyJ开头且包含两个点分隔符
        if (content.startsWith("eyJ") && content.split("\\.").length == 3) {
            String[] contentList = content.split("\\.");
            String contextFirst = str.base64Decode(contentList[0]);
            String contextFinally = str.base64Decode(contentList[1]);
            
            if (contextFirst != null && contextFinally != null) {
                String tmpContext = "header:\n" + contextFirst + "\n\npayload:\n" + contextFinally;
                return tmpContext;
            }
        }
        
        return null;
    }
}