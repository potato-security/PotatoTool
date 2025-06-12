package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.decoder.utils.JWTDecrypt;
import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;

import java.util.List;

/**
 * JWT解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class JwtDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        String jwtData = JWTDecrypt.decrypt(content);
        
        if (jwtData != null && !jwtData.equals(content)) {
            encodeMode.add("JWT");
            return jwtData;
        }
        
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null && content.startsWith("eyJ") && content.split("\\.").length == 3;
    }
}