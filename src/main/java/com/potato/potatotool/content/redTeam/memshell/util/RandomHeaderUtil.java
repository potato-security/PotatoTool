package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.utils.strUtils;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Potato
 * @date 2024/8/5 10:47
 */
public class RandomHeaderUtil {
    private static final Random RANDOM = new Random();
    private static final String[] HEADER_KEYS = {"Referer", "User-Agent"};

    // 生成一个随机的HTTP头部键值对
    public static Map.Entry<String, String> generateRandomHeader() {
        String key = getRandomHeaderKey();
        String value = generateRandomHeaderValue(key);
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    // 从预定义的HTTP头部键数组中随机选择一个键
    private static String getRandomHeaderKey() {
        return HEADER_KEYS[RANDOM.nextInt(HEADER_KEYS.length)];
    }

    // 根据提供的键生成一个随机的值
    private static String generateRandomHeaderValue(String key) {
        switch (key) {
            case "Referer":
            case "User-Agent":
                return strUtils.generateRandomString(4,10);
            default:
                return "";
        }
    }
}
