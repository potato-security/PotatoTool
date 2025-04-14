package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

/**
 * @author Potato
 * @date 2025/3/19 16:30
 * POC格式转换接口，定义将特定格式的POC转换为通用PocObj的方法
 */
public interface IPocConverter<T> {
    
    /**
     * 将特定格式的POC转换为通用PocObj
     * @param poc 特定格式的POC对象
     * @return 通用PocObj对象
     */
    PocObj.Poc convert(T poc);
}