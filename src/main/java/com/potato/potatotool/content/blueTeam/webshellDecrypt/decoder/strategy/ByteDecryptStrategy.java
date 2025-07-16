package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.ByteDecrypt;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Byte解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class ByteDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        try {
            ByteDecrypt byteD = new ByteDecrypt();
            byte[] result = byteD.decrypt(content);
            
            if (result != null) {
                encodeMode.add("Byte转换");
                
                // 检查是否有额外的编码
                if (byteD.gzipCode) encodeMode.add("Gzip");
                if (byteD.classCode) encodeMode.add("Class编译");
                if (byteD.serializeCode) encodeMode.add("反序列化");
                
                // encodeMode已经通过参数传递
                return new String(result, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // 解密失败，忽略异常
        }
        
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null && content.length() > 100 && !isAllZero(content);
    }
    
    /**
     * 检查字符串是否全为0
     */
    private boolean isAllZero(String input) {
        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (input.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}