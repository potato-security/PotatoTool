package com.potato.potatotool.update.verifier;

import com.potato.potatotool.update.manifest.Manifest;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * 文件校验器 - 验证文件完整性
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class FileVerifier {
    
    /**
     * 验证文件校验和
     * @param file 要验证的文件
     * @param expected 期望的校验和
     * @return true if verification passed
     */
    public static boolean verifyChecksum(Path file, Manifest.Checksum expected) {
        if (!Files.exists(file)) {
            System.err.println("文件不存在: " + file);
            return false;
        }
        
        try {
            // 至少需要一种校验和
            if (expected == null || 
                (expected.getSha256() == null && expected.getMd5() == null)) {
                System.out.println("警告: 未提供校验和，跳过验证");
                return true;  // 如果没有提供校验和，认为通过
            }
            
            boolean sha256Match = true;
            boolean md5Match = true;
            
            // 验证SHA256
            if (expected.getSha256() != null && !expected.getSha256().isEmpty()) {
                String actualSha256 = calculateSHA256(file);
                sha256Match = expected.getSha256().equalsIgnoreCase(actualSha256);
                
                if (!sha256Match) {
                    System.err.println("SHA256校验失败!");
                    System.err.println("期望: " + expected.getSha256());
                    System.err.println("实际: " + actualSha256);
                }
            }
            
            // 验证MD5
            if (expected.getMd5() != null && !expected.getMd5().isEmpty()) {
                String actualMd5 = calculateMD5(file);
                md5Match = expected.getMd5().equalsIgnoreCase(actualMd5);
                
                if (!md5Match) {
                    System.err.println("MD5校验失败!");
                    System.err.println("期望: " + expected.getMd5());
                    System.err.println("实际: " + actualMd5);
                }
            }
            
            boolean result = sha256Match && md5Match;
            
            return result;
            
        } catch (Exception e) {
            System.err.println("文件校验出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 计算文件的SHA256
     */
    public static String calculateSHA256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hash = digest.digest();
        return bytesToHex(hash);
    }
    
    /**
     * 计算文件的MD5
     */
    public static String calculateMD5(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hash = digest.digest();
        return bytesToHex(hash);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 验证文件大小
     */
    public static boolean verifyFileSize(Path file, long expectedSize) {
        try {
            long actualSize = Files.size(file);
            boolean match = actualSize == expectedSize;
            
            if (!match) {
                System.err.println("文件大小不匹配!");
                System.err.println("期望: " + expectedSize + " 字节");
                System.err.println("实际: " + actualSize + " 字节");
            }
            
            return match;
        } catch (Exception e) {
            System.err.println("读取文件大小失败: " + e.getMessage());
            return false;
        }
    }
}

