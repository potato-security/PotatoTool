package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 组合解密策略
 * 处理URLDecode + Base64/XOR/AES等组合解密
 * 包含三种处理模式：直接处理、key=value格式、MD5前后缀格式
 * 
 * @author Potato
 * @version 2.0
 */
public class ComboDecryptStrategy implements DecryptStrategy {
    
    private final StrUtils str;
    private final AESUtils aes;
    
    /**
     * 构造函数 - 接收共享的工具类实例
     */
    public ComboDecryptStrategy(StrUtils sharedStrUtils, AESUtils sharedAesUtils) {
        this.str = sharedStrUtils;
        this.aes = sharedAesUtils;
    }
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        // 尝试组合解密-URLdecode+【(Base64+XOR)/AES】 【兼容+Gzip】
        // 使用原本的三种处理模式逻辑
        String result = tryComboDecryptWithThreeModes(content, config, encodeMode);
        
        // 常见key=base64(密文) 【小补丁】
        result = trySpecialKeyBase64(result, encodeMode);

        if (result != null) {
            return result;
        }
        
        return null;
    }
    
    /**
     * 尝试组合解密，使用原本的三种处理模式逻辑
     */
    private String tryComboDecryptWithThreeModes(String content, DecryptConfig config, List<String> encodeMode) {
        
        // 尝试组合解密-URLdecode+【(Base64+XOR)/AES】 【兼容+Gzip】
        for(int i = 0; i < 3; i++) {
            String tmpConText = content;
            
            // 针对开头可能存在pass=的情况
            String tmpPassStr = "";
            if(i == 1) {
                if(!content.contains("=")) continue;
                int indexEq = content.indexOf("=") + 1;
                int indexAnd = content.contains("&") ? content.indexOf("&") : content.length();
                if(indexEq < indexAnd) {
                    tmpConText = content.substring(indexEq, indexAnd);
                    tmpPassStr = content.substring(0, indexEq);
                    if (tmpConText.contains("=")) continue;
                }
            }
            
            // 针对开头结尾存在pass+key的情况
            String tempMd5PassKey = "";
            if(i == 2) {
                if(content.length() < 33 || content.contains("&")) continue;
                tempMd5PassKey = "流量中提取到md5(pass+md5(key))=" + content.substring(0,16) + content.substring(content.length()-16, content.length()) + "\n";
                tmpConText = content.substring(16, content.length()-16);
                if(!ReadabilityChecker.assessReadability(tempMd5PassKey) || !ReadabilityChecker.assessReadability(tmpConText)) continue;
            }
            
            String conText1 = str.urlDecode(tmpConText);
            if(!conText1.equals(tmpConText)) encodeMode.add("URLdecode");
            
            String tmp_conText3 = null;
            try {
                byte[] tmp_conText2 = str.base64Decode(conText1.getBytes(StandardCharsets.UTF_8));
                if(!ReadabilityChecker.assessReadability(tmp_conText2, 1, 0)) {   //  无乱码则继续进行xor解密尝试
                    tmp_conText3 = str.xorEncode(tmp_conText2, config.getInputKey(), config.getTraverseList(), config.getCustomPath());
                }
            } catch (Exception e) {
                // 忽略异常
            }
            
            if(tmp_conText3 != null) {
                encodeMode.add("Base64");
                encodeMode.add("XOR/" + str.xorKey);
                
                if(i == 1) {
                    encodeMode.add("PassStr:" + tmpPassStr);
                }
                if(i == 2) {
                    encodeMode.add("Md5PassKey:" + tempMd5PassKey);
                }
                
                // 解密成功，encodeMode已经通过参数传递
                content = tmp_conText3; // 直接返回解密内容，不处理passStr和md5PassKey
                break;
            } else {
                // 可能使用的AES
                String conText4 = new String(aes.aesWebShellDecode(conText1, config.getInputKey(), config.getInputIv(), config.getTraverseList(), config.getCustomPath()), StandardCharsets.UTF_8);
                
                if(!conText4.equals(conText1)) {
                    encodeMode.add("AES\\" + aes.mode_AES.get() + "\\" + aes.padding_AES.get() + "<key:iv>" + aes.key_AES.get() + ":" + aes.iv_AES.get());

                    if(i == 1) {
                        encodeMode.add("PassStr:" + tmpPassStr);
                    }
                    if(i == 2) {
                        encodeMode.add("Md5PassKey:" + tempMd5PassKey.trim());
                    }
                    
                    // 解密成功，encodeMode已经通过参数传递
                    content = conText4; // 直接返回解密内容，不处理passStr和md5PassKey
                    break;
                } else {
                    encodeMode.clear();
                }
            }
        }
        return content;
    }
    

    /**
     * 常见key=base64(密文) 【小补丁】
     */
    private String trySpecialKeyBase64(String content, List<String> encodeMode) {
        if(content.contains("fileName=") && content.contains("methodName=")) {
            String tmpConText = str.urlDecode(content);
            String[] parameters = tmpConText.split("&");
            String modifiedContent = content;
            boolean hasDecryption = false;

            for (String parameter : parameters) {
                String[] keyValue = parameter.split("=", 2);
                if (keyValue.length == 2) {
                    String newValue = str.base64Decode(keyValue[1]);
                    if(newValue != null) {
                        modifiedContent = modifiedContent.replace(keyValue[1], newValue);
                        hasDecryption = true;
                    }
                }
            }
            
            if(hasDecryption && !tmpConText.equals(modifiedContent)) {
                encodeMode.add("Base64");
                return modifiedContent;
            }
        }
        return content;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null;
    }
}