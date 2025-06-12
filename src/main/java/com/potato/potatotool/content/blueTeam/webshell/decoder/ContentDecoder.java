package com.potato.potatotool.content.blueTeam.webshell.decoder;

import com.potato.potatotool.content.blueTeam.webshell.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshell.decoder.strategy.*;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.crypto.DESUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 内容解码器
 * 负责协调各种解密策略进行内容解密
 * 
 * @author Potato
 * @version 2.0
 */
public class ContentDecoder {
    
    private final List<DecryptStrategy> strategies;
    
    // 共享的工具类实例
    private final StrUtils sharedStrUtils;
    private final AESUtils sharedAESUtils;
    private final DESUtils sharedDesUtils;
    
    public ContentDecoder() {
        // 创建共享的工具类实例
        this.sharedStrUtils = new StrUtils();
        this.sharedAESUtils = new AESUtils();
        this.sharedDesUtils = new DESUtils();
        this.strategies = initializeStrategies();
    }
    
    /**
     * 初始化解密策略
     */
    private List<DecryptStrategy> initializeStrategies() {
        return Arrays.asList(
            new Log4jDecryptStrategy(),
            new BcelDecryptStrategy(),
            new ByteDecryptStrategy(),
            new JwtDecryptStrategy(),
            new CasDecryptStrategy(),
            new ShiroDecryptStrategy(),
            new Md5DecryptStrategy(),

            // 常见CMS数据库/软件配置解密
            new DatabaseDecryptStrategy(),

            // 组合解密 - 传入共享的工具类实例
            new ComboDecryptStrategy(sharedStrUtils, sharedAESUtils),

            // 单解密策略碰撞 - 传入共享的工具类实例
            new SingleDecryptStrategy(sharedStrUtils, sharedAESUtils, sharedDesUtils)
        );
    }
    
    /**
     * 解码内容
     * 
     * @param content 待解码内容
     * @param config 解密配置
     * @param encodeModes 编码模式列表，由调用方管理
     * @return 解码后的内容
     */
    public String decode(String content, DecryptConfig config, List<List<String>> encodeModes) {
        String currentContent = content;
        boolean foundPartialDecryption = false;
        List<String> currentEncodeMode = new ArrayList<>();

        for (DecryptStrategy strategy : strategies) {
            String result = strategy.decrypt(currentContent, config, currentEncodeMode);
            if (result != null && !result.equals(currentContent)) {
                currentContent = result;
                // 检查是否是组合解密策略
                if (strategy instanceof ComboDecryptStrategy) {
                    // 组合解密成功，继续用结果进行其他解密尝试，不跳出循环
                    foundPartialDecryption = true;
                    continue;
                }
                // 其他策略成功则直接停止其他解密尝试
                break;
            }
        }

        // 如果组合解密成功，处理passStr和md5PassKey
        if (foundPartialDecryption) {
            String passStr = findFirstMatchAndRemove(currentEncodeMode, "PassStr:");
            String md5PassKey = findFirstMatchAndRemove(currentEncodeMode, "Md5PassKey:");
            if(passStr != null) currentContent = passStr + currentContent;
            if(md5PassKey != null) currentContent = md5PassKey + currentContent;
        }

        if(currentEncodeMode.size() > 0) encodeModes.add(currentEncodeMode);
        
        return currentContent;
    }


    /**
     * 高效查找并提取第一个以指定前缀开头的字符串剩余部分，并剔除list中对应的元素
     * @param list 字符串列表
     * @param prefix 需要匹配的前缀（例如 "PassStr:"）
     * @return 提取后的字符串，若无匹配则返回 null
     */
    public static String findFirstMatchAndRemove(List<String> list, String prefix) {
        if (list == null || prefix == null) {
            return null;
        }

        for (String s : list) {
            if (s != null && s.startsWith(prefix)) {
                list.remove(s);
                return s.substring(prefix.length());
            }
        }
        return null;
    }

}