package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.decoder.utils.BcelDecrypt;
import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * BCEL解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class BcelDecryptStrategy implements DecryptStrategy {

    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        try {
            BcelDecrypt bcel = new BcelDecrypt();
            byte[] result = bcel.decrypt(content);
            
            if (result != null) {
                encodeMode.add("BCEL编码");
                
                // 检查是否有额外的编码
                if (bcel.classCode) encodeMode.add("Class编译");
                if (bcel.serializeCode) encodeMode.add("反序列化");
                
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
        return content != null && content.toLowerCase().startsWith("$$bcel$$");
    }
}