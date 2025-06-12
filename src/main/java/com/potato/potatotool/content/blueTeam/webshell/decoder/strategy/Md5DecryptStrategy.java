package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.decoder.utils.MD5Decrypt;
import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;

import java.util.List;

/**
 * MD5解密策略
 * 
 * @author Potato
 * @version 2.0
 */
public class Md5DecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        MD5Decrypt md5 = new MD5Decrypt();
        String md5Data = md5.decrypt(content);
        
        if (md5Data != null) {
            encodeMode.add(md5.mode);
            return md5Data;
        }
        
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null && ( content.length() == 16 ||  content.length() == 32 ||  content.length() == 40) && content.matches("[a-fA-F0-9]+");
    }
}