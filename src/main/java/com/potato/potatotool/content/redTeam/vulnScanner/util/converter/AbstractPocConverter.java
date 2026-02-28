package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025-11-26
 * POC转换器抽象基类，提供通用的工具方法和公共逻辑
 */
public abstract class AbstractPocConverter<T> implements IPocConverter<T> {

    /**
     * 安全获取字符串，处理null情况
     */
    protected String safeGetString(String str) {
        return str != null ? str : "";
    }
    
    /**
     * 安全获取字符串，处理null情况
     */
    protected String safeGetString(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return "";
        }
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    /**
     * 安全获取布尔值
     */
    protected boolean safeGetBoolean(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }
    
    /**
     * 转换变量Map格式
     * 将 Map<String, String> 转换为 Map<String, List<String>>
     */
    protected void transformPayloadMap(Map<String, Object> source, Map<String, List<String>> target) {
        if (source == null) return;
        
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            List<String> list = new ArrayList<>();
            
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item != null) {
                        list.add(item.toString());
                    }
                }
            } else if (value != null) {
                list.add(value.toString());
            }
            
            if (!list.isEmpty()) {
                target.put(key, list);
            }
        }
    }

    /**
     * 通用的 Severity 解析逻辑
     * 支持数字(1-5)和字符串(critical, high, etc)
     */
    protected Severity parseSeverity(String level) {
        if (level == null) {
            return Severity.UNKNOWN;
        }
        
        switch (level.toLowerCase()) {
            case "info":
            case "5":
                return Severity.INFO;
            case "low":
            case "4":
                return Severity.LOW;
            case "medium":
            case "3":
                return Severity.MEDIUM;
            case "high":
            case "2":
                return Severity.HIGH;
            case "critical":
            case "1":
                return Severity.CRITICAL;
            default:
                return Severity.UNKNOWN;
        }
    }
    
    /**
     * 初始化POC的基本字段
     */
    protected void initBasicFields(PocObj.Poc poc) {
        if (poc.getVariables() == null) {
            poc.setVariables(new HashMap<>());
        }
        if (poc.getTags() == null) {
            poc.setTags(new ArrayList<>());
        }
        if (poc.getReferences() == null) {
            poc.setReferences(new ArrayList<>());
        }
    }
}
