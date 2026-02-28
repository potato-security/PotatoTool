package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.functions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 哈希函数测试
 * 测试所有 Xray 哈希函数
 * 
 * @author Potato
 * @date 2025-10-30
 */
@DisplayName("Xray 哈希函数测试")
public class HashFunctionsTest {
    
    @Test
    @DisplayName("测试 MD5 哈希")
    public void testMd5() {
        // 已知测试向量
        assertEquals("098f6bcd4621d373cade4e832627b4f6", HashFunctions.md5("test"));
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", HashFunctions.md5("password"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", HashFunctions.md5(""));
        assertEquals("", HashFunctions.md5(null));
        
        // 中文测试
        String hash = HashFunctions.md5("测试");
        assertNotNull(hash);
        assertEquals(32, hash.length());
        
        // 大小写一致性
        String hash1 = HashFunctions.md5("Admin");
        String hash2 = HashFunctions.md5("admin");
        assertNotEquals(hash1, hash2);
    }
    
    @Test
    @DisplayName("测试 SHA1 哈希")
    public void testSha1() {
        // 已知测试向量
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", HashFunctions.sha1("test"));
        assertEquals("5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", HashFunctions.sha1("password"));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", HashFunctions.sha1(""));
        assertEquals("", HashFunctions.sha1(null));
        
        // 长度验证
        String hash = HashFunctions.sha1("hello");
        assertEquals(40, hash.length());
    }
    
    @Test
    @DisplayName("测试 SHA256 哈希")
    public void testSha256() {
        // 已知测试向量
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", 
                    HashFunctions.sha256("test"));
        assertEquals("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", 
                    HashFunctions.sha256("password"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 
                    HashFunctions.sha256(""));
        assertEquals("", HashFunctions.sha256(null));
        
        // 长度验证
        String hash = HashFunctions.sha256("hello");
        assertEquals(64, hash.length());
    }
    
    @Test
    @DisplayName("测试 SHA512 哈希")
    public void testSha512() {
        // 基本测试
        String hash = HashFunctions.sha512("test");
        assertNotNull(hash);
        assertEquals(128, hash.length());
        
        hash = HashFunctions.sha512("password");
        assertNotNull(hash);
        assertEquals(128, hash.length());
        
        assertEquals("", HashFunctions.sha512(null));
        
        // 验证空字符串哈希
        hash = HashFunctions.sha512("");
        assertEquals(128, hash.length());
    }
    
    @Test
    @DisplayName("测试 MMH3 哈希")
    public void testMmh3() {
        // MurmurHash3 测试
        String hash1 = HashFunctions.mmh3("test");
        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        
        String hash2 = HashFunctions.mmh3("test");
        assertEquals(hash1, hash2, "相同输入应产生相同哈希");
        
        String hash3 = HashFunctions.mmh3("Test");
        assertNotEquals(hash1, hash3, "不同输入应产生不同哈希");
        
        assertEquals("", HashFunctions.mmh3(null));
    }
    
    @Test
    @DisplayName("测试哈希唯一性")
    public void testHashUniqueness() {
        String[] inputs = {"admin", "Admin", "ADMIN", "admin ", " admin"};
        
        // MD5 唯一性
        String[] md5Hashes = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            md5Hashes[i] = HashFunctions.md5(inputs[i]);
        }
        for (int i = 0; i < md5Hashes.length; i++) {
            for (int j = i + 1; j < md5Hashes.length; j++) {
                assertNotEquals(md5Hashes[i], md5Hashes[j], 
                    "不同输入产生了相同的 MD5: " + inputs[i] + " vs " + inputs[j]);
            }
        }
        
        // SHA256 唯一性
        String[] sha256Hashes = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            sha256Hashes[i] = HashFunctions.sha256(inputs[i]);
        }
        for (int i = 0; i < sha256Hashes.length; i++) {
            for (int j = i + 1; j < sha256Hashes.length; j++) {
                assertNotEquals(sha256Hashes[i], sha256Hashes[j], 
                    "不同输入产生了相同的 SHA256");
            }
        }
    }
    
    @Test
    @DisplayName("测试哈希一致性")
    public void testHashConsistency() {
        String input = "consistency_test";
        
        // 多次计算应得到相同结果
        String md5_1 = HashFunctions.md5(input);
        String md5_2 = HashFunctions.md5(input);
        String md5_3 = HashFunctions.md5(input);
        assertEquals(md5_1, md5_2);
        assertEquals(md5_2, md5_3);
        
        String sha1_1 = HashFunctions.sha1(input);
        String sha1_2 = HashFunctions.sha1(input);
        assertEquals(sha1_1, sha1_2);
        
        String sha256_1 = HashFunctions.sha256(input);
        String sha256_2 = HashFunctions.sha256(input);
        assertEquals(sha256_1, sha256_2);
    }
    
    @Test
    @DisplayName("测试长文本哈希")
    public void testLongTextHash() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Long text line ").append(i).append("\n");
        }
        String longText = sb.toString();
        
        // 所有哈希函数都应该能处理长文本
        String md5 = HashFunctions.md5(longText);
        assertNotNull(md5);
        assertEquals(32, md5.length());
        
        String sha1 = HashFunctions.sha1(longText);
        assertNotNull(sha1);
        assertEquals(40, sha1.length());
        
        String sha256 = HashFunctions.sha256(longText);
        assertNotNull(sha256);
        assertEquals(64, sha256.length());
        
        String sha512 = HashFunctions.sha512(longText);
        assertNotNull(sha512);
        assertEquals(128, sha512.length());
    }
    
    @Test
    @DisplayName("测试特殊字符哈希")
    public void testSpecialCharactersHash() {
        String[] specialInputs = {
            "!@#$%^&*()",
            "中文测试",
            "Émoji😀",
            "\n\r\t",
            "{}[]<>",
            "\"'`"
        };
        
        for (String input : specialInputs) {
            String md5 = HashFunctions.md5(input);
            assertNotNull(md5, "MD5 失败: " + input);
            assertEquals(32, md5.length());
            
            String sha256 = HashFunctions.sha256(input);
            assertNotNull(sha256, "SHA256 失败: " + input);
            assertEquals(64, sha256.length());
        }
    }
    
    @Test
    @DisplayName("复杂场景 - 哈希链")
    public void testHashChain() {
        // 场景1: MD5(SHA256(text))
        String original = "admin";
        String sha256 = HashFunctions.sha256(original);
        String md5OfSha256 = HashFunctions.md5(sha256);
        assertNotNull(md5OfSha256);
        assertEquals(32, md5OfSha256.length());
        
        // 场景2: SHA1(MD5(text))
        String md5 = HashFunctions.md5(original);
        String sha1OfMd5 = HashFunctions.sha1(md5);
        assertNotNull(sha1OfMd5);
        assertEquals(40, sha1OfMd5.length());
    }
    
    @Test
    @DisplayName("性能测试 - 批量哈希")
    public void testBatchHashPerformance() {
        int count = 1000;
        String[] inputs = new String[count];
        for (int i = 0; i < count; i++) {
            inputs[i] = "test_input_" + i;
        }
        
        long start = System.currentTimeMillis();
        for (String input : inputs) {
            HashFunctions.md5(input);
        }
        long md5Time = System.currentTimeMillis() - start;
        
        start = System.currentTimeMillis();
        for (String input : inputs) {
            HashFunctions.sha256(input);
        }
        long sha256Time = System.currentTimeMillis() - start;
        
        assertTrue(md5Time < 5000, "MD5 批量哈希耗时过长: " + md5Time + "ms");
        assertTrue(sha256Time < 5000, "SHA256 批量哈希耗时过长: " + sha256Time + "ms");
    }
}




