package com.potato.potatotool.utils.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Potato
 * @date 2023/5/9 15:38
 */
public class MapUtils {

    /**
     * @param map   传入的原始Map
     * @param key   添加\指定的key
     * @param value key对应的value
     */
    public static void addKeyValuePair(LinkedHashMap<String, List<String>> map, String key, String value) {

        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {

            List<String> list = new ArrayList<String>();
            list.add(value);
            map.put(key, list);

        }

    }

}
