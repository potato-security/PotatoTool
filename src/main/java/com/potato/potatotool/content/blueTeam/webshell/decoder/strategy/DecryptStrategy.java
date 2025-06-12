package com.potato.potatotool.content.blueTeam.webshell.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;

import java.util.List;

/**
 * 解密策略接口
 * 定义各种解密策略的统一接口
 * 
 * @author Potato
 * @version 2.0
 */
public interface DecryptStrategy {
    
    /**
     * 解密内容
     * 
     * @param content 待解密内容
     * @param config 解密配置
     * @param encodeMode 当前参数的编码模式（用于记录解密过程）
     * @return 解密后的内容，如果无法解密则返回null
     */
    String decrypt(String content, DecryptConfig config, List<String> encodeMode);
    
    /**
     * 获取策略名称
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 检查是否适用于当前内容
     */
    default boolean isApplicable(String content) {
        return content != null && !content.trim().isEmpty();
    }
}