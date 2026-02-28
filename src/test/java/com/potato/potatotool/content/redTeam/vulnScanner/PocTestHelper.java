package com.potato.potatotool.content.redTeam.vulnScanner;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * POC 测试辅助工具类
 * 
 * 统一管理测试中的 POC 路径，支持从 resources 目录读取
 * 
 * @author Potato
 */
public class PocTestHelper {
    
    /** POC 资源根路径 */
    public static final String POC_RESOURCE_BASE = "poc/";
    
    /** 各格式 POC 子目录 */
    public static final String NUCLEI_POC_DIR = POC_RESOURCE_BASE + "nucleiPoc/";
    public static final String GOBY_POC_DIR = POC_RESOURCE_BASE + "gobyPoc/";
    public static final String XRAY_POC_DIR = POC_RESOURCE_BASE + "xrayPoc/";
    public static final String POCSUITE_POC_DIR = POC_RESOURCE_BASE + "pocsuitePoc/";
    
    /** 文件系统路径（用于开发时直接访问） */
    public static final String POC_FS_BASE = "src/main/resources/poc/";
    public static final String NUCLEI_POC_FS = POC_FS_BASE + "nucleiPoc/";
    public static final String GOBY_POC_FS = POC_FS_BASE + "gobyPoc/";
    public static final String XRAY_POC_FS = POC_FS_BASE + "xrayPoc/";
    public static final String POCSUITE_POC_FS = POC_FS_BASE + "pocsuitePoc/";
    
    /**
     * 获取 POC 文件的文件系统路径
     * 
     * @param relativePath 相对于 poc/ 的路径，如 "nucleiPoc/http/cves/CVE-2024-0001.yaml"
     * @return 完整的文件系统路径
     */
    public static String getPocPath(String relativePath) {
        return POC_FS_BASE + relativePath;
    }
    
    /**
     * 获取 Nuclei POC 路径
     */
    public static String getNucleiPocPath(String subPath) {
        return NUCLEI_POC_FS + subPath;
    }
    
    /**
     * 获取 Goby POC 路径
     */
    public static String getGobyPocPath(String subPath) {
        return GOBY_POC_FS + subPath;
    }
    
    /**
     * 获取 Xray POC 路径
     */
    public static String getXrayPocPath(String subPath) {
        return XRAY_POC_FS + subPath;
    }
    
    /**
     * 获取 Pocsuite POC 路径
     */
    public static String getPocsuitePocPath(String subPath) {
        return POCSUITE_POC_FS + subPath;
    }
    
    /**
     * 从 ClassLoader 资源读取 POC 内容
     * 
     * @param resourcePath 资源路径，如 "poc/nucleiPoc/http/cves/CVE-2024-0001.yaml"
     * @return POC 文件内容
     */
    public static String readPocContent(String resourcePath) throws IOException {
        try (InputStream is = PocTestHelper.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("资源不存在: " + resourcePath);
            }
            return readInputStream(is);
        }
    }
    
    /**
     * 从 ClassLoader 资源读取 POC 内容（使用相对路径）
     */
    public static String readPocContentRelative(String relativePath) throws IOException {
        return readPocContent(POC_RESOURCE_BASE + relativePath);
    }
    
    /**
     * 获取资源 URL
     */
    public static URL getPocResourceUrl(String resourcePath) {
        return PocTestHelper.class.getClassLoader().getResource(resourcePath);
    }
    
    /**
     * 检查 POC 文件是否存在（文件系统）
     */
    public static boolean pocExists(String relativePath) {
        File file = new File(POC_FS_BASE + relativePath);
        return file.exists() && file.isFile();
    }
    
    /**
     * 列出目录下的所有 POC 文件
     */
    public static List<File> listPocFiles(String dirPath) {
        List<File> result = new ArrayList<>();
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            listPocFilesRecursive(dir, result);
        }
        return result;
    }
    
    private static void listPocFilesRecursive(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                listPocFilesRecursive(file, result);
            } else if (isPocFile(file.getName())) {
                result.add(file);
            }
        }
    }
    
    /**
     * 判断是否是 POC 文件
     */
    public static boolean isPocFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
    }
    
    private static String readInputStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
