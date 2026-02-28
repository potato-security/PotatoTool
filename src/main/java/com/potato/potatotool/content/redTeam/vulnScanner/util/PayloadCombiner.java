package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Payload 组合器
 * 支持三种 Nuclei 攻击模式：batteringram、pitchfork、clusterbomb
 *
 * 功能：
 * - batteringram: 同步模式，所有变量使用同一个索引的值
 * - pitchfork: 配对模式，多个变量列表索引严格配对
 * - clusterbomb: 笛卡尔积模式，生成所有可能组合
 *
 * 示例 (batteringram):
 * ```yaml
 * payloads:
 *   user: [u1, u2, u3]
 *   pass: [p1, p2, p3]
 * ```
 * 生成组合: (u1,p1), (u2,p2), (u3,p3)
 *
 * 示例 (pitchfork):
 * ```yaml
 * payloads:
 *   user: [u1, u2, u3]
 *   pass: [p1, p2]
 * ```
 * 生成组合: (u1,p1), (u2,p2)
 *
 * 示例 (clusterbomb):
 * ```yaml
 * payloads:
 *   param1: [a, b]
 *   param2: [1, 2]
 * ```
 * 生成组合: (a,1), (a,2), (b,1), (b,2)
 *
 * @author Potato
 * @date 2025-10-29
 */
public class PayloadCombiner {
    
    /**
     * 生成 payloads 笛卡尔积
     * 
     * @param payloads 变量名到值列表的映射
     * @return 所有可能的变量组合列表
     */
    public static List<Map<String, String>> generateCombinations(
        Map<String, List<String>> payloads
    ) {
        List<Map<String, String>> result = new ArrayList<>();
        
        if (payloads == null || payloads.isEmpty()) {
            // 空 payloads，返回一个空映射
            result.add(new HashMap<>());
            return result;
        }
        
        // 提取变量名列表
        List<String> keys = new ArrayList<>(payloads.keySet());
        
        // 过滤掉空值列表的变量
        List<String> validKeys = new ArrayList<>();
        for (String key : keys) {
            List<String> values = payloads.get(key);
            if (values != null && !values.isEmpty()) {
                validKeys.add(key);
            }
        }
        
        if (validKeys.isEmpty()) {
            result.add(new HashMap<>());
            return result;
        }
        
        // 递归生成所有组合
        generateCombinationsRecursive(
            payloads,
            validKeys,
            0,
            new HashMap<>(),
            result
        );
        
        return result;
    }
    
    /**
     * 递归生成组合
     * 
     * @param payloads 变量名到值列表的映射
     * @param keys 有效的变量名列表
     * @param index 当前处理的变量索引
     * @param current 当前组合（正在构建）
     * @param result 结果列表
     */
    private static void generateCombinationsRecursive(
        Map<String, List<String>> payloads,
        List<String> keys,
        int index,
        Map<String, String> current,
        List<Map<String, String>> result
    ) {
        // 递归终止条件：所有变量都已处理
        if (index == keys.size()) {
            // 添加当前组合的副本
            result.add(new HashMap<>(current));
            return;
        }
        
        // 获取当前变量及其值列表
        String key = keys.get(index);
        List<String> values = payloads.get(key);
        
        // 遍历当前变量的所有可能值
        for (String value : values) {
            // 设置当前变量的值
            current.put(key, value);
            
            // 递归处理下一个变量
            generateCombinationsRecursive(
                payloads,
                keys,
                index + 1,
                current,
                result
            );
        }
        
        // 回溯：移除当前变量（可选，因为会被覆盖）
        current.remove(key);
    }
    
    /**
     * 计算组合总数
     * 用于预估生成的组合数量，避免组合爆炸
     * 
     * @param payloads 变量名到值列表的映射
     * @return 组合总数
     */
    public static long countCombinations(Map<String, List<String>> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return 1;
        }
        
        long count = 1;
        for (Map.Entry<String, List<String>> entry : payloads.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                count *= values.size();
                
                // 防止整数溢出
                if (count > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 生成组合（带限制）
     * 限制生成的组合数量，防止组合爆炸导致内存溢出
     * 使用智能采样策略而非简单截断
     *
     * @param payloads 变量名到值列表的映射
     * @param maxCombinations 最大组合数
     * @return 组合列表（可能被采样）
     */
    public static List<Map<String, String>> generateCombinationsWithLimit(
        Map<String, List<String>> payloads,
        int maxCombinations
    ) {
        long totalCombinations = countCombinations(payloads);
        
        // 如果总数不超过限制，直接生成所有组合
        if (totalCombinations <= maxCombinations) {
            return generateCombinations(payloads);
        }
        
        // 使用智能采样策略
        return generateSmartSampledCombinations(payloads, maxCombinations);
    }
    
    /**
     * 智能采样生成组合
     * 策略：
     * 1. 保证每个变量的每个值至少出现一次（如果可能）
     * 2. 首尾优先：每个变量的第一个和最后一个值优先组合
     * 3. 随机采样：剩余配额随机采样
     * 
     * @param payloads 变量映射
     * @param maxCombinations 最大组合数
     * @return 采样后的组合列表
     */
    private static List<Map<String, String>> generateSmartSampledCombinations(
        Map<String, List<String>> payloads,
        int maxCombinations
    ) {
        List<Map<String, String>> result = new ArrayList<>();
        Set<String> addedCombinations = new HashSet<>();  // 用于去重
        Random random = new Random();
        
        List<String> keys = new ArrayList<>(payloads.keySet());
        if (keys.isEmpty()) {
            result.add(new HashMap<>());
            return result;
        }
        
        // 第一阶段：确保每个变量的首/尾值都被测试
        // 使用 pitchfork 模式保证每个变量的每个值至少出现一次
        int coverageAllocation = Math.min(maxCombinations / 3, getMaxValueCount(payloads));
        List<Map<String, String>> coverageCombos = generateCoverageCombinations(payloads, coverageAllocation);
        for (Map<String, String> combo : coverageCombos) {
            String key = generateComboKey(combo);
            if (!addedCombinations.contains(key)) {
                result.add(combo);
                addedCombinations.add(key);
            }
        }
        
        // 第二阶段：添加边界组合（所有第一个值、所有最后一个值）
        if (result.size() < maxCombinations) {
            // 所有变量使用第一个值
            Map<String, String> firstCombo = new HashMap<>();
            for (String key : keys) {
                List<String> values = payloads.get(key);
                if (values != null && !values.isEmpty()) {
                    firstCombo.put(key, values.get(0));
                }
            }
            String firstKey = generateComboKey(firstCombo);
            if (!addedCombinations.contains(firstKey)) {
                result.add(firstCombo);
                addedCombinations.add(firstKey);
            }
            
            // 所有变量使用最后一个值
            Map<String, String> lastCombo = new HashMap<>();
            for (String key : keys) {
                List<String> values = payloads.get(key);
                if (values != null && !values.isEmpty()) {
                    lastCombo.put(key, values.get(values.size() - 1));
                }
            }
            String lastKey = generateComboKey(lastCombo);
            if (!addedCombinations.contains(lastKey)) {
                result.add(lastCombo);
                addedCombinations.add(lastKey);
            }
        }
        
        // 第三阶段：随机采样填充剩余配额
        int maxAttempts = maxCombinations * 3; // 防止无限循环
        int attempts = 0;
        while (result.size() < maxCombinations && attempts < maxAttempts) {
            Map<String, String> randomCombo = new HashMap<>();
            for (String key : keys) {
                List<String> values = payloads.get(key);
                if (values != null && !values.isEmpty()) {
                    randomCombo.put(key, values.get(random.nextInt(values.size())));
                }
            }
            String comboKey = generateComboKey(randomCombo);
            if (!addedCombinations.contains(comboKey)) {
                result.add(randomCombo);
                addedCombinations.add(comboKey);
            }
            attempts++;
        }
        
        return result;
    }
    
    /**
     * 生成覆盖性组合 - 确保每个变量的每个值至少出现一次
     */
    private static List<Map<String, String>> generateCoverageCombinations(
        Map<String, List<String>> payloads, 
        int maxCombos
    ) {
        List<Map<String, String>> result = new ArrayList<>();
        List<String> keys = new ArrayList<>(payloads.keySet());
        
        if (keys.isEmpty()) {
            return result;
        }
        
        // 找出最大值列表长度
        int maxLength = getMaxValueCount(payloads);
        
        // 按索引生成组合
        for (int i = 0; i < Math.min(maxLength, maxCombos); i++) {
            Map<String, String> combo = new HashMap<>();
            for (String key : keys) {
                List<String> values = payloads.get(key);
                if (values != null && !values.isEmpty()) {
                    // 循环使用值
                    combo.put(key, values.get(i % values.size()));
                }
            }
            result.add(combo);
        }
        
        return result;
    }
    
    /**
     * 获取最大值列表长度
     */
    private static int getMaxValueCount(Map<String, List<String>> payloads) {
        int maxLength = 0;
        for (List<String> values : payloads.values()) {
            if (values != null && values.size() > maxLength) {
                maxLength = values.size();
            }
        }
        return maxLength;
    }
    
    /**
     * 生成组合唯一标识
     */
    private static String generateComboKey(Map<String, String> combo) {
        StringBuilder sb = new StringBuilder();
        combo.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("|"));
        return sb.toString();
    }
    
    /**
     * 合并两个 payload 映射
     * 用于合并 set 变量和 payloads 变量
     *
     * @param base 基础映射
     * @param additional 额外映射
     * @return 合并后的映射
     */
    public static Map<String, List<String>> mergePayloads(
        Map<String, List<String>> base,
        Map<String, List<String>> additional
    ) {
        Map<String, List<String>> result = new HashMap<>();

        if (base != null) {
            result.putAll(base);
        }

        if (additional != null) {
            result.putAll(additional);
        }

        return result;
    }

    /**
     * 生成 batteringram 模式组合（同步模式）
     * 所有变量使用同一个索引的值，同步循环
     *
     * ���例：
     * user=[u1,u2,u3], pass=[p1,p2,p3,p4]
     * 生成: (u1,p1), (u2,p2), (u3,p3), (u1,p4)
     *
     * 循环次数 = max(所有变量列表长度)
     * 如果某个变量列表长度不足，循环使用其值
     *
     * @param payloads 变量名到值列表的映射
     * @return 组合列表
     */
    public static List<Map<String, String>> generateBatteringRamCombinations(
        Map<String, List<String>> payloads
    ) {
        List<Map<String, String>> result = new ArrayList<>();

        if (payloads == null || payloads.isEmpty()) {
            result.add(new HashMap<>());
            return result;
        }

        // 找出最大列表长度
        int maxLength = 0;
        Map<String, List<String>> validPayloads = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : payloads.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                validPayloads.put(entry.getKey(), values);
                maxLength = Math.max(maxLength, values.size());
            }
        }

        if (validPayloads.isEmpty() || maxLength == 0) {
            result.add(new HashMap<>());
            return result;
        }

        // 生成组合：每次迭代所有变量使用同一个索引（循环使用）
        for (int i = 0; i < maxLength; i++) {
            Map<String, String> combination = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : validPayloads.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();

                // 使用模运算循环使用值
                int index = i % values.size();
                combination.put(key, values.get(index));
            }

            result.add(combination);
        }

        return result;
    }

    /**
     * 生成 pitchfork 模式组合（配对模式）
     * 多个变量列表索引严格配对，平行循环
     *
     * 示例：
     * user=[u1,u2,u3], pass=[p1,p2]
     * 生成: (u1,p1), (u2,p2)
     *
     * 循环次数 = min(所有变量列表长度)
     *
     * @param payloads 变量名到值列表的映射
     * @return 组合列表
     */
    public static List<Map<String, String>> generatePitchforkCombinations(
        Map<String, List<String>> payloads
    ) {
        List<Map<String, String>> result = new ArrayList<>();

        if (payloads == null || payloads.isEmpty()) {
            result.add(new HashMap<>());
            return result;
        }

        // 找出最小列表长度
        int minLength = Integer.MAX_VALUE;
        Map<String, List<String>> validPayloads = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : payloads.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                validPayloads.put(entry.getKey(), values);
                minLength = Math.min(minLength, values.size());
            }
        }

        if (validPayloads.isEmpty() || minLength == Integer.MAX_VALUE || minLength == 0) {
            result.add(new HashMap<>());
            return result;
        }

        // 生成组合：每次迭代所有变量使用同一个索引（不循环）
        for (int i = 0; i < minLength; i++) {
            Map<String, String> combination = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : validPayloads.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                combination.put(key, values.get(i));
            }

            result.add(combination);
        }

        return result;
    }
}

