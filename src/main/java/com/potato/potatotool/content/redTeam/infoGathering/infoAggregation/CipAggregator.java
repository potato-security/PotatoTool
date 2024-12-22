package com.potato.potatotool.content.redTeam.infoGathering.infoAggregation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Potato
 * @date 2024/10/22 15:30
 */
public class CipAggregator {

    // 提取C段出现的次数
    public static Map<String, Integer> getFrequentCSegments(Set<String> ipList, int cipThreshold) {
        // 使用Map来统计每个C段出现的次数
        Map<String, Integer> cSegmentCountMap = new HashMap<>();

        // 遍历每个IP地址，统计C段的出现次数
        for (String ip : ipList) {
            try {
                String cSegment = getCSegment(ip);  // 获取C段
                cSegmentCountMap.put(cSegment, cSegmentCountMap.getOrDefault(cSegment, 0) + 1);
            }catch (Exception e){}
        }

        // 只保留出现次数大于等于【cipThreshold阈值】的C段
        return cSegmentCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= cipThreshold)  // 过滤出出现次数大于等于【cipThreshold阈值】的C段
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 提取C段
    private static String getCSegment(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("无效的IP地址: " + ip);
        }
        // 返回前三段，即C段部分
        return parts[0] + "." + parts[1] + "." + parts[2] + ".1/24";
    }

    public static void main(String[] args) {
        System.out.println(getCSegment("202.102.12.12"));
    }
}
