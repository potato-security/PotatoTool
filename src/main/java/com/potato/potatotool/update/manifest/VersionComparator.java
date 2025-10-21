package com.potato.potatotool.update.manifest;

/**
 * 版本比对器 - 采用语义化版本号
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class VersionComparator {
    
    /**
     * 比较两个版本号
     * 支持格式：
     * - 标准格式：1.2.3
     * - 带修订号：1.2.3.4
     * - 不规则格式：2.5, 2.x
     * 
     * @param v1 版本号1
     * @param v2 版本号2
     * @return 1: v1 > v2, 0: v1 == v2, -1: v1 < v2
     */
    public static int compare(String v1, String v2) {
        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("版本号不能为null");
        }
        
        if (v1.equals(v2)) {
            return 0;
        }
        
        // 标准化版本号（移除非数字字符，除了点号）
        v1 = normalizeVersion(v1);
        v2 = normalizeVersion(v2);
        
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLen = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (num1 > num2) {
                return 1;
            }
            if (num1 < num2) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * 标准化版本号
     * 例如：2.x -> 2.0, v2.5.1 -> 2.5.1
     */
    private static String normalizeVersion(String version) {
        // 移除前缀 v 或 V
        version = version.replaceAll("^[vV]", "");
        
        // 替换 x 为 0
        version = version.replaceAll("[xX]", "0");
        
        // 移除其他非数字和点号的字符
        version = version.replaceAll("[^0-9.]", "");
        
        // 确保不以点号开头或结尾
        version = version.replaceAll("^\\.+|\\.+$", "");
        
        // 处理连续的点号
        version = version.replaceAll("\\.{2,}", ".");
        
        return version;
    }
    
    /**
     * 解析版本号部分
     */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 检查版本v1是否大于v2
     */
    public static boolean isNewer(String v1, String v2) {
        return compare(v1, v2) > 0;
    }
    
    /**
     * 检查版本v1是否等于v2
     */
    public static boolean isEqual(String v1, String v2) {
        return compare(v1, v2) == 0;
    }
    
    /**
     * 检查版本v1是否小于v2
     */
    public static boolean isOlder(String v1, String v2) {
        return compare(v1, v2) < 0;
    }
    
    /**
     * 检查版本号格式是否有效
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        // 至少包含一个数字
        return version.matches(".*\\d+.*");
    }
}

