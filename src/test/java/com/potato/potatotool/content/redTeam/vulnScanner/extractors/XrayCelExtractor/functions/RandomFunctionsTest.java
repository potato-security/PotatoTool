package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 随机函数测试
 * 测试所有 Xray 随机生成函数
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray 随机函数测试")
public class RandomFunctionsTest {
    
    @Test
    @DisplayName("测试 randomInt() - 随机整数")
    public void testRandomInt() {
        // 基本范围测试
        for (int i = 0; i < 100; i++) {
            int random = RandomFunctions.randomInt(1, 10);
            assertTrue(random >= 1 && random <= 10, 
                "随机数超出范围: " + random);
        }
        
        // 单一值范围
        int random = RandomFunctions.randomInt(5, 5);
        assertEquals(5, random);
        
        // 大范围测试
        random = RandomFunctions.randomInt(1, 1000);
        assertTrue(random >= 1 && random <= 1000);
    }
    
    @Test
    @DisplayName("测试 randomInt() - 分布均匀性")
    public void testRandomIntDistribution() {
        int min = 1, max = 5;
        int[] counts = new int[max - min + 1];
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            int random = RandomFunctions.randomInt(min, max);
            counts[random - min]++;
        }
        
        // 每个数字至少出现过一次
        for (int count : counts) {
            assertTrue(count > 0, "某些数字从未出现");
        }
        
        // 验证分布相对均匀（每个数字出现次数应该在期望值的 50%-200% 之间）
        int expected = iterations / (max - min + 1);
        for (int count : counts) {
            assertTrue(count > expected * 0.5 && count < expected * 2, 
                "分布不均匀，出现次数: " + count + ", 期望: " + expected);
        }
    }
    
    @Test
    @DisplayName("测试 randomLowercase() - 随机小写字母")
    public void testRandomLowercase() {
        String random = RandomFunctions.randomLowercase(10);
        assertEquals(10, random.length());
        assertTrue(random.matches("[a-z]+"), "包含非小写字母: " + random);
        
        // 不同长度测试
        random = RandomFunctions.randomLowercase(5);
        assertEquals(5, random.length());
        
        random = RandomFunctions.randomLowercase(20);
        assertEquals(20, random.length());
        
        // 边界情况
        random = RandomFunctions.randomLowercase(0);
        assertEquals("", random);
        
        random = RandomFunctions.randomLowercase(1);
        assertEquals(1, random.length());
    }
    
    @Test
    @DisplayName("测试 randomUppercase() - 随机大写字母")
    public void testRandomUppercase() {
        String random = RandomFunctions.randomUppercase(10);
        assertEquals(10, random.length());
        assertTrue(random.matches("[A-Z]+"), "包含非大写字母: " + random);
        
        random = RandomFunctions.randomUppercase(15);
        assertEquals(15, random.length());
        assertTrue(random.matches("[A-Z]+"));
    }
    
    @Test
    @DisplayName("测试 rand_text_alpha() - 随机字母")
    public void testRandTextAlpha() {
        String random = RandomFunctions.rand_text_alpha(10);
        assertEquals(10, random.length());
        assertTrue(random.matches("[a-zA-Z]+"), "包含非字母字符: " + random);
        
        // 验证包含大小写混合
        Set<Character> chars = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            random = RandomFunctions.rand_text_alpha(10);
            for (char c : random.toCharArray()) {
                chars.add(c);
            }
        }
        
        // 应该包含大写和小写字母
        boolean hasLower = chars.stream().anyMatch(Character::isLowerCase);
        boolean hasUpper = chars.stream().anyMatch(Character::isUpperCase);
        assertTrue(hasLower && hasUpper, "缺少大写或小写字母");
    }
    
    @Test
    @DisplayName("测试 rand_text_numeric() - 随机数字")
    public void testRandTextNumeric() {
        String random = RandomFunctions.rand_text_numeric(10);
        assertEquals(10, random.length());
        assertTrue(random.matches("\\d+"), "包含非数字字符: " + random);
        
        random = RandomFunctions.rand_text_numeric(20);
        assertEquals(20, random.length());
        assertTrue(random.matches("\\d+"));
    }
    
    @Test
    @DisplayName("测试 rand_text_alphanumeric() - 随机字母数字")
    public void testRandTextAlphanumeric() {
        String random = RandomFunctions.rand_text_alphanumeric(15);
        assertEquals(15, random.length());
        assertTrue(random.matches("[a-zA-Z0-9]+"), "包含非字母数字字符: " + random);
        
        // 验证包含字母和数字
        Set<Character> chars = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            random = RandomFunctions.rand_text_alphanumeric(10);
            for (char c : random.toCharArray()) {
                chars.add(c);
            }
        }
        
        boolean hasLetter = chars.stream().anyMatch(Character::isLetter);
        boolean hasDigit = chars.stream().anyMatch(Character::isDigit);
        assertTrue(hasLetter && hasDigit, "缺少字母或数字");
    }
    
    @Test
    @DisplayName("测试 randomUUID() - 随机 UUID")
    public void testRandomUUID() {
        String uuid1 = RandomFunctions.randomUUID();
        String uuid2 = RandomFunctions.randomUUID();
        
        // UUID 格式验证 (8-4-4-4-12)
        String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        assertTrue(uuid1.matches(uuidPattern), "UUID 格式错误: " + uuid1);
        assertTrue(uuid2.matches(uuidPattern), "UUID 格式错误: " + uuid2);
        
        // UUID 应该不同
        assertNotEquals(uuid1, uuid2, "生成了相同的 UUID");
        
        // 批量测试唯一性
        Set<String> uuids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String uuid = RandomFunctions.randomUUID();
            assertFalse(uuids.contains(uuid), "生成了重复的 UUID");
            uuids.add(uuid);
        }
    }
    
    @Test
    @DisplayName("测试随机性 - 多次调用产生不同结果")
    public void testRandomness() {
        // randomInt
        Set<Integer> ints = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ints.add(RandomFunctions.randomInt(1, 1000));
        }
        assertTrue(ints.size() > 50, "randomInt 随机性不足");
        
        // randomLowercase
        Set<String> lowers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            lowers.add(RandomFunctions.randomLowercase(10));
        }
        assertTrue(lowers.size() > 90, "randomLowercase 随机性不足");
        
        // rand_text_alphanumeric
        Set<String> alphanums = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            alphanums.add(RandomFunctions.rand_text_alphanumeric(10));
        }
        assertTrue(alphanums.size() > 90, "rand_text_alphanumeric 随机性不足");
    }
    
    @Test
    @DisplayName("边界测试 - 零长度和负数")
    public void testBoundary() {
        // 零长度
        assertEquals("", RandomFunctions.randomLowercase(0));
        assertEquals("", RandomFunctions.randomUppercase(0));
        assertEquals("", RandomFunctions.rand_text_alpha(0));
        assertEquals("", RandomFunctions.rand_text_numeric(0));
        assertEquals("", RandomFunctions.rand_text_alphanumeric(0));
        
        // 负数（应该返回空字符串）
        assertEquals("", RandomFunctions.randomLowercase(-1));
        assertEquals("", RandomFunctions.randomUppercase(-5));
    }
    
    @Test
    @DisplayName("性能测试 - 批量生成")
    public void testPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            RandomFunctions.randomInt(1, 100);
        }
        long intTime = System.currentTimeMillis() - start;
        
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            RandomFunctions.rand_text_alphanumeric(20);
        }
        long textTime = System.currentTimeMillis() - start;
        
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            RandomFunctions.randomUUID();
        }
        long uuidTime = System.currentTimeMillis() - start;
        
        assertTrue(intTime < 1000, "randomInt 性能不佳: " + intTime + "ms");
        assertTrue(textTime < 2000, "rand_text_alphanumeric 性能不佳: " + textTime + "ms");
        assertTrue(uuidTime < 1000, "randomUUID 性能不佳: " + uuidTime + "ms");
    }
    
    @Test
    @DisplayName("复杂场景 - 组合使用")
    public void testComplexScenarios() {
        // 场景1: 生成随机长度的随机字符串
        int length = RandomFunctions.randomInt(5, 15);
        String text = RandomFunctions.rand_text_alphanumeric(length);
        assertEquals(length, text.length());
        
        // 场景2: 生成多个随机部分组合
        String part1 = RandomFunctions.randomLowercase(5);
        String part2 = RandomFunctions.rand_text_numeric(3);
        String part3 = RandomFunctions.randomUppercase(5);
        String combined = part1 + part2 + part3;
        assertEquals(13, combined.length());
        
        // 场景3: 带 UUID 的随机字符串
        String prefix = RandomFunctions.rand_text_alpha(5);
        String uuid = RandomFunctions.randomUUID();
        String suffix = RandomFunctions.rand_text_numeric(3);
        String full = prefix + "-" + uuid + "-" + suffix;
        assertTrue(full.length() > 40);
    }
}




