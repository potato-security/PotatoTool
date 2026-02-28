package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * File 处理器
 * 用于扫描本地文件并检测内容
 * 支持 Nuclei File 协议的所有功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class FileHandler {
    
    /**
     * 文件扫描结果
     */
    public static class FileResponse {
        private String path;                    // 文件路径
        private String name;                    // 文件名
        private long size;                      // 文件大小（字节）
        private String extension;               // 文件扩展名
        private String content;                 // 文件内容
        private String md5;                     // MD5 哈希
        private String sha256;                  // SHA256 哈希
        private long lastModified;              // 最后修改时间
        private boolean readable;               // 是否可读
        private boolean writable;               // 是否可写
        private boolean executable;             // 是否可执行
        private boolean success;                // 操作是否成功
        private String error;                   // 错误信息
        
        public FileResponse() {}
        
        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }
        
        public String getSha256() { return sha256; }
        public void setSha256(String sha256) { this.sha256 = sha256; }
        
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
        
        public boolean isReadable() { return readable; }
        public void setReadable(boolean readable) { this.readable = readable; }
        
        public boolean isWritable() { return writable; }
        public void setWritable(boolean writable) { this.writable = writable; }
        
        public boolean isExecutable() { return executable; }
        public void setExecutable(boolean executable) { this.executable = executable; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        @Override
        public String toString() {
            return "FileResponse{" +
                    "path='" + path + '\'' +
                    ", name='" + name + '\'' +
                    ", size=" + size +
                    ", extension='" + extension + '\'' +
                    ", success=" + success +
                    '}';
        }
    }
    
    /**
     * 扫描单个文件
     * 
     * @param filePath 文件路径
     * @return 文件响应
     */
    public static FileResponse scanFile(String filePath) {
        return scanFile(filePath, 104857600); // 默认 100MB
    }
    
    /**
     * 扫描单个文件（完整参数）
     * 
     * @param filePath 文件路径
     * @param maxSize 最大文件大小（字节）
     * @return 文件响应
     */
    public static FileResponse scanFile(String filePath, long maxSize) {
        FileResponse response = new FileResponse();
        
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                response.setSuccess(false);
                response.setError("文件不存在: " + filePath);
                return response;
            }
            
            if (!file.isFile()) {
                response.setSuccess(false);
                response.setError("不是文件: " + filePath);
                return response;
            }
            
            // 基本信息
            response.setPath(file.getAbsolutePath());
            response.setName(file.getName());
            response.setSize(file.length());
            response.setLastModified(file.lastModified());
            response.setReadable(file.canRead());
            response.setWritable(file.canWrite());
            response.setExecutable(file.canExecute());
            
            // 提取扩展名
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                response.setExtension(fileName.substring(dotIndex + 1));
            }
            
            // 检查文件大小
            if (file.length() > maxSize) {
                response.setSuccess(false);
                response.setError("文件过大: " + file.length() + " 字节（限制: " + maxSize + " 字节）");
                return response;
            }
            
            // 读取文件内容
            if (file.canRead()) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    response.setContent(new String(fileBytes, "UTF-8"));
                    
                    // 计算哈希
                    response.setMd5(calculateMD5(fileBytes));
                    response.setSha256(calculateSHA256(fileBytes));
                } catch (IOException e) {
                    response.setError("读取文件失败: " + e.getMessage());
                }
            }
            
            response.setSuccess(true);
            System.out.println("✓ 文件扫描成功: " + filePath);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("文件扫描异常: " + e.getMessage());
            System.err.println("文件扫描失败: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    /**
     * 扫描目录中的文件
     * 
     * @param dirPath 目录路径
     * @param extensions 文件扩展名列表（null 表示所有文件）
     * @param recursive 是否递归扫描子目录
     * @param maxSize 最大文件大小
     * @return 文件响应列表
     */
    public static List<FileResponse> scanDirectory(String dirPath, List<String> extensions, 
                                                   boolean recursive, long maxSize) {
        List<FileResponse> results = new ArrayList<>();
        
        try {
            File dir = new File(dirPath);
            
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("目录不存在或不是目录: " + dirPath);
                return results;
            }
            
            // 使用 Files.walkFileTree 进行文件遍历
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;

            Files.walkFileTree(dir.toPath(),
                EnumSet.noneOf(FileVisitOption.class),
                maxDepth,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            // 检查是否为普通文件
                            if (!attrs.isRegularFile()) {
                                return FileVisitResult.CONTINUE;
                            }
                            
                            // 检查扩展名
                            if (extensions != null && !extensions.isEmpty()) {
                                String fileName = file.getFileName().toString();
                                int dotIndex = fileName.lastIndexOf('.');
                                if (dotIndex < 0) {
                                    return FileVisitResult.CONTINUE; // 无扩展名
                                }
                                String ext = fileName.substring(dotIndex + 1);
                                if (!extensions.contains(ext)) {
                                    return FileVisitResult.CONTINUE; // 扩展名不匹配
                                }
                            }
                            
                            // 扫描文件
                            FileResponse response = scanFile(file.toString(), maxSize);
                            if (response.isSuccess()) {
                                results.add(response);
                            }
                        } catch (Exception e) {
                            System.err.println("处理文件时出错: " + file + " - " + e.getMessage());
                        }
                        
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.println("访问文件失败: " + file + " - " + exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                }
            );
            
            System.out.println("✓ 目录扫描完成: " + dirPath + "，找到 " + results.size() + " 个文件");
            
        } catch (Exception e) {
            System.err.println("目录扫描失败: " + dirPath + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * 扫描多个路径
     * 
     * @param paths 路径列表
     * @param extensions 文件扩展名列表
     * @param recursive 是否递归扫描
     * @param maxSize 最大文件大小
     * @return 文件响应列表
     */
    public static List<FileResponse> scanPaths(List<String> paths, List<String> extensions, 
                                               boolean recursive, long maxSize) {
        List<FileResponse> results = new ArrayList<>();
        
        for (String path : paths) {
            File file = new File(path);
            if (file.isFile()) {
                FileResponse response = scanFile(path, maxSize);
                if (response.isSuccess()) {
                    results.add(response);
                }
            } else if (file.isDirectory()) {
                List<FileResponse> dirResults = scanDirectory(path, extensions, recursive, maxSize);
                results.addAll(dirResults);
            }
        }
        
        return results;
    }
    
    /**
     * 计算 MD5 哈希
     */
    private static String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 计算 SHA256 哈希
     */
    private static String calculateSHA256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            return null;
        }
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
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试单个文件扫描
        System.out.println("=== 测试单个文件扫描 ===");
        String testFile = "pom.xml";
        FileResponse response = scanFile(testFile);
        
        System.out.println(response);
        if (response.isSuccess()) {
            System.out.println("文件名: " + response.getName());
            System.out.println("大小: " + response.getSize() + " 字节");
            System.out.println("扩展名: " + response.getExtension());
            System.out.println("MD5: " + response.getMd5());
            System.out.println("SHA256: " + response.getSha256());
            System.out.println("可读: " + response.isReadable());
            System.out.println("可写: " + response.isWritable());
            System.out.println("内容长度: " + (response.getContent() != null ? response.getContent().length() : 0) + " 字符");
        }
        
        // 测试目录扫描
        System.out.println("\n=== 测试目录扫描 ===");
        List<String> extensions = Arrays.asList("java", "xml");
        List<FileResponse> results = scanDirectory("src/test/resources/nuclei-poc-samples", 
                                                   extensions, false, 1048576);
        
        System.out.println("找到 " + results.size() + " 个文件:");
        for (FileResponse file : results) {
            System.out.println("  - " + file.getName() + " (" + file.getSize() + " 字节)");
        }
    }
}


