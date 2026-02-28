package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.CASDecrypt;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.utils.crypto.AESUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CAS解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class CasDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        AESUtils aes = new AESUtils();
        byte[] casBytes = CASDecrypt.decrypt(content, aes);
        if (casBytes != null) {
            String casData = new String(casBytes, StandardCharsets.UTF_8);

            encodeMode.add("CAS(Base64+AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get()+")");
            if(aes.gzipCode) encodeMode.add("Gzip");
            if(aes.classCode) encodeMode.add("Class编译");
            if(aes.javaSerializeCode) encodeMode.add("反序列化");
            // encodeMode已经通过参数传递

            return casData;
        }

        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null && content.length() > 100;
    }
}