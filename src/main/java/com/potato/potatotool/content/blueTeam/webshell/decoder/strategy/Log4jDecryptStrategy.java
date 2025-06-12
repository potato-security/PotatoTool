package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshell.decoder.utils.Log4jDecrypt;

import java.util.List;

/**
 * Log4j解密策略
 * 处理Log4j混淆的解密
 * 
 * @author Potato
 * @version 2.0
 */
public class Log4jDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        try {
            String result = Log4jDecrypt.decrypt(content);
            
            if (result != null && !result.equals(content)) {
                encodeMode.add("Log4j混淆");
                return result;
            }
        } catch (Exception e) {
            // 解密失败，忽略异常
        }
        
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        final char[] TARGET = {'$', '{', 'j', 'n', 'd', 'i', ':'};
        return content != null && content.length() < 1000 && containsCharPattern(content, TARGET);
    }
    
    @Override
    public String getStrategyName() {
        return "Log4j解密";
    }

    /**
     * 忽略大小写，匹配是否顺序包含char[]->"${jndi:"
     * @param content
     * @return
     */
    public boolean containsCharPattern(String content, char[] TARGET) {
        // 如果内容比目标序列短，直接返回false
        if (content == null || content.length() < TARGET.length) {
            return false;
        }

        int foundIndex = 0;
        final int len = content.length();

        for (int i = 0; i < len; i++) {
            char c = content.charAt(i);
            // 检查当前字符是否匹配目标序列中的下一个字符（忽略大小写）
            if (Character.toLowerCase(c) == TARGET[foundIndex]) {
                foundIndex++;  // 移动到下一个目标字符
                // 如果已找到所有目标字符
                if (foundIndex == TARGET.length) {
                    return true;
                }
            }
        }
        return false;  // 未找到完整序列
    }
}