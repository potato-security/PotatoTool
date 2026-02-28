package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.ShiroDecrypt;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Shiro解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class ShiroDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        int indeShiro = content.indexOf("rememberMe=")!=-1 ? content.indexOf("rememberMe=") + 11 : 0 ;
        int indeAnd_1 = content.contains("&") ? content.indexOf("&") : content.length(); // 针对奇葩的写入
        int indeAnd_2 = content.contains(";") ? content.indexOf(";") : content.length(); // 针对奇葩的写入
        int indeAnd = indeAnd_1 < indeAnd_2 ? indeAnd_1 : indeAnd_2;

        String tmpData = content.substring(indeShiro,indeAnd);

        ShiroDecrypt shiro = new ShiroDecrypt();
        AESUtils aes = new AESUtils();
        StrUtils str = new StrUtils();
        byte[] shiroBytes = shiro.decrypt(tmpData, config.getInputKey(), aes, str);
        if (shiroBytes != null) {
            String shiroData = new String(shiroBytes, StandardCharsets.UTF_8);
            String Shiro = content.replace(tmpData, shiroData);

            encodeMode.add("Shiro(Base64+AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get()+")");
            if( aes.gzipCode || str.gzipCode) encodeMode.add("Gzip");
            if( aes.classCode || str.classCode) encodeMode.add("Class编译");
            if( aes.javaSerializeCode || str.javaSerializeCode) encodeMode.add("反序列化");
            // encodeMode已经通过参数传递

            return Shiro;
        }
        
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null && content.length() > 10;
    }
}