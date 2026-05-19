package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.FileHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nuclei File 协议测试
 * 测试 File 协议的解析和文件扫描功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiFileTest {
    private static final String TEST_RESOURCE_ROOT = "src/test/resources";
    private static final String TEST_SAMPLE_DIR = "src/test/resources/nuclei-poc-samples";
    private static final String TEST_SAMPLE_FILE = "src/test/resources/nuclei-poc-samples/20-file-simple.yml";
    private static final String TEST_PROTOCOL_FILE = "src/test/resources/nuclei-poc-samples/19-file-protocol.yml";
    
    /**
     * 测试 File 数据模型
     */
    @Test
    public void testFileDataModel() {
        System.out.println("=== 测试 File 数据模型 ===");
        
        // 创建 File 配置
        NucleiYamlObj.FileProtocol file = new NucleiYamlObj.FileProtocol();
        file.setExtensions(Arrays.asList("log", "txt"));
        file.setPaths(Arrays.asList("/var/log"));
        file.setRecursive(true);
        file.setMax_size(10485760);
        
        // 验证默认值
        assertNotNull(file.getExtensions());
        assertEquals(2, file.getExtensions().size());
        assertEquals("log", file.getExtensions().get(0));
        assertTrue(file.isRecursive());
        assertEquals(10485760, file.getMax_size());
        
        System.out.println("✓ File 数据模型测试通过");
    }
    
    /**
     * 测试 FileHandler 单文件扫描
     */
    @Test
    public void testFileHandlerSingleFile() {
        System.out.println("\n=== 测试 FileHandler 单文件扫描 ===");
        
        String testFile = TEST_SAMPLE_FILE;
        FileHandler.FileResponse response = FileHandler.scanFile(testFile);
        
        assertNotNull(response);
        assertTrue(response.isSuccess(), "文件扫描应该成功");
        assertEquals("20-file-simple.yml", response.getName());
        assertEquals("yml", response.getExtension());
        assertTrue(response.getSize() > 0);
        assertNotNull(response.getContent());
        assertNotNull(response.getMd5());
        assertNotNull(response.getSha256());
        assertPathUnderTestResources(response.getPath());
        
        System.out.println("文件名: " + response.getName());
        System.out.println("大小: " + response.getSize() + " 字节");
        System.out.println("扩展名: " + response.getExtension());
        System.out.println("MD5: " + response.getMd5());
        System.out.println("SHA256: " + response.getSha256());
        System.out.println("可读: " + response.isReadable());
        System.out.println("✓ 单文件扫描测试通过");
    }
    
    /**
     * 测试 FileHandler 目录扫描
     */
    @Test
    public void testFileHandlerDirectoryScan() {
        System.out.println("\n=== 测试 FileHandler 目录扫描 ===");
        
        String testDir = TEST_SAMPLE_DIR;
        List<String> extensions = Arrays.asList("yml", "yaml");
        
        List<FileHandler.FileResponse> results = FileHandler.scanDirectory(
            testDir, extensions, false, 1048576
        );
        
        assertNotNull(results);
        System.out.println("找到 " + results.size() + " 个文件");
        
        for (FileHandler.FileResponse file : results) {
            System.out.println("  - " + file.getName() + " (" + file.getSize() + " 字节)");
            assertTrue(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"));
            assertPathUnderTestResources(file.getPath());
        }
        
        System.out.println("✓ 目录扫描测试通过");
    }
    
    /**
     * 测试 FileHandler 递归扫描
     */
    @Test
    public void testFileHandlerRecursiveScan() {
        System.out.println("\n=== 测试 FileHandler 递归扫描 ===");
        
        String testDir = TEST_RESOURCE_ROOT;
        List<String> extensions = Arrays.asList("yml");
        
        List<FileHandler.FileResponse> results = FileHandler.scanDirectory(
            testDir, extensions, true, 1048576
        );
        
        assertNotNull(results);
        System.out.println("递归找到 " + results.size() + " 个 .yml 文件");
        
        if (!results.isEmpty()) {
            System.out.println("示例文件:");
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                System.out.println("  - " + results.get(i).getPath());
                assertPathUnderTestResources(results.get(i).getPath());
            }
        }
        
        System.out.println("✓ 递归扫描测试通过");
    }
    
    /**
     * 测试 FileHandler 文件大小限制
     */
    @Test
    public void testFileHandlerSizeLimit() {
        System.out.println("\n=== 测试 FileHandler 文件大小限制 ===");
        
        String testFile = TEST_SAMPLE_FILE;
        long maxSize = 100; // 限制为 100 字节
        
        FileHandler.FileResponse response = FileHandler.scanFile(testFile, maxSize);
        
        assertNotNull(response);
        assertFalse(response.isSuccess()); // 应该失败（文件太大）
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("文件过大"));
        
        System.out.println("✓ 错误消息: " + response.getError());
        System.out.println("✓ 文件大小限制测试通过");
    }
    
    /**
     * 测试简单 File 样例文件解析
     */
    @Test
    public void testSimpleFilePocParsing() throws Exception {
        System.out.println("\n=== 测试简单 File 样例文件解析 ===");
        
        String pocPath = TEST_SAMPLE_FILE;
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("file-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("File Simple Password Search", nucleiPoc.getInfo().getName());
        
        // 验证 File 配置
        assertNotNull(nucleiPoc.getFile(), "File 配置不应为 null");
        assertFalse(nucleiPoc.getFile().isEmpty(), "File 配置列表不应为空");
        
        NucleiYamlObj.FileProtocol file = nucleiPoc.getFile().get(0);
        assertNotNull(file.getExtensions());
        assertEquals(2, file.getExtensions().size());
        assertEquals("log", file.getExtensions().get(0));
        assertEquals("txt", file.getExtensions().get(1));
        assertFalse(file.isRecursive());
        
        // 验证匹配器
        assertNotNull(file.getMatchers());
        assertFalse(file.getMatchers().isEmpty());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ File 配置数: " + nucleiPoc.getFile().size());
        System.out.println("✓ 扩展名: " + file.getExtensions());
        System.out.println("✓ 简单 File 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 File 样例文件解析
     */
    @Test
    public void testComprehensiveFilePocParsing() throws Exception {
        System.out.println("\n=== 测试完整 File 样例文件解析 ===");
        
        String pocPath = TEST_PROTOCOL_FILE;
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("file-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 File 配置（应该有 4 个测试用例）
        assertNotNull(nucleiPoc.getFile());
        assertEquals(4, nucleiPoc.getFile().size(), "应该有 4 个 File 测试用例");
        
        // 验证第一个 File 配置（日志文件扫描）
        NucleiYamlObj.FileProtocol file1 = nucleiPoc.getFile().get(0);
        assertNotNull(file1.getExtensions());
        assertEquals(2, file1.getExtensions().size());
        assertTrue(file1.isRecursive());
        assertEquals(10485760, file1.getMax_size());
        
        // 验证第二个 File 配置（配置文件扫描）
        NucleiYamlObj.FileProtocol file2 = nucleiPoc.getFile().get(1);
        assertNotNull(file2.getExtensions());
        assertEquals(5, file2.getExtensions().size());
        assertFalse(file2.isRecursive());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ File 配置数: " + nucleiPoc.getFile().size());
        System.out.println("✓ 完整 File 样例文件解析测试通过");
    }
    
    /**
     * 测试 File POC 转换为通用格式
     */
    @Test
    public void testFilePocConversion() throws Exception {
        System.out.println("\n=== 测试 File POC 转换 ===");
        
        String pocPath = TEST_SAMPLE_FILE;
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        
        // 转换为通用 POC 格式
        NucleiPocConverter converter = new NucleiPocConverter();
        PocObj.Poc poc = converter.convert(nucleiPoc);
        
        assertNotNull(poc, "转换后的 POC 对象不应为 null");
        assertEquals("file", poc.getProtocol(), "协议应该是 file");
        assertEquals("file-simple-test", poc.getId());
        assertEquals("File Simple Password Search", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.FileStep, "步骤应该是 FileStep 类型");
        
        PocObj.FileStep fileStep = (PocObj.FileStep) step;
        assertNotNull(fileStep.getExtensions());
        assertEquals(2, fileStep.getExtensions().size());
        assertFalse(fileStep.isRecursive());
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 扩展名: " + fileStep.getExtensions());
        System.out.println("✓ File POC 转换测试通过");
    }
    
    /**
     * 测试文件哈希计算
     */
    @Test
    public void testFileHashCalculation() {
        System.out.println("\n=== 测试文件哈希计算 ===");
        
        String testFile = TEST_PROTOCOL_FILE;
        File file = new File(testFile);
        
        if (!file.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + testFile);
            return;
        }
        
        FileHandler.FileResponse response = FileHandler.scanFile(testFile);
        
        assertNotNull(response);
        if (response.isSuccess()) {
            assertNotNull(response.getMd5());
            assertNotNull(response.getSha256());
            assertEquals(32, response.getMd5().length()); // MD5 是 32 位十六进制
            assertEquals(64, response.getSha256().length()); // SHA256 是 64 位十六进制
            
            System.out.println("文件: " + response.getName());
            System.out.println("MD5: " + response.getMd5());
            System.out.println("SHA256: " + response.getSha256());
            System.out.println("✓ 文件哈希计算测试通过");
        } else {
            System.out.println("⚠ 文件扫描失败: " + response.getError());
        }
    }
    
    /**
     * 测试文件权限检测
     */
    @Test
    public void testFilePermissions() {
        System.out.println("\n=== 测试文件权限检测 ===");
        
        String testFile = TEST_SAMPLE_FILE;
        FileHandler.FileResponse response = FileHandler.scanFile(testFile);
        
        if (response.isSuccess()) {
            System.out.println("文件: " + response.getName());
            System.out.println("可读: " + response.isReadable());
            System.out.println("可写: " + response.isWritable());
            System.out.println("可执行: " + response.isExecutable());
            
            // 测试资源文件应该是可读的
            assertTrue(response.isReadable());
            assertPathUnderTestResources(response.getPath());
            
            System.out.println("✓ 文件权限检测测试通过");
        } else {
            System.out.println("⚠ 文件扫描失败: " + response.getError());
        }
    }
    
    /**
     * 测试批量路径扫描
     */
    @Test
    public void testBatchPathScan() {
        System.out.println("\n=== 测试批量路径扫描 ===");
        
        List<String> paths = Arrays.asList(
            TEST_SAMPLE_FILE,
            TEST_PROTOCOL_FILE,
            TEST_SAMPLE_DIR
        );
        
        List<String> extensions = Arrays.asList("yml", "yaml", "xml", "md");
        
        List<FileHandler.FileResponse> results = FileHandler.scanPaths(
            paths, extensions, false, 1048576
        );
        
        assertNotNull(results);
        System.out.println("扫描 " + paths.size() + " 个路径，找到 " + results.size() + " 个文件");
        
        for (FileHandler.FileResponse file : results) {
            System.out.println("  - " + file.getName() + " (" + file.getSize() + " 字节, " + file.getExtension() + ")");
            assertPathUnderTestResources(file.getPath());
        }
        
        System.out.println("✓ 批量路径扫描测试通过");
    }

    @Test
    public void testFileScansShouldStayWithinTestResourceDirectory() {
        System.out.println("\n=== 测试 File 仅扫描测试资源目录 ===");

        List<FileHandler.FileResponse> results = FileHandler.scanPaths(
                Arrays.asList(TEST_SAMPLE_FILE, TEST_SAMPLE_DIR),
                Arrays.asList("yml", "yaml"),
                true,
                1048576
        );

        assertFalse(results.isEmpty());
        for (FileHandler.FileResponse file : results) {
            assertPathUnderTestResources(file.getPath());
        }
    }

    private void assertPathUnderTestResources(String actualPath) {
        assertNotNull(actualPath);
        Path expectedRoot = Paths.get(TEST_RESOURCE_ROOT).toAbsolutePath().normalize();
        Path actual = Paths.get(actualPath).toAbsolutePath().normalize();
        assertTrue(actual.startsWith(expectedRoot), "文件扫描应限制在测试资源目录内: " + actual);
    }
}

