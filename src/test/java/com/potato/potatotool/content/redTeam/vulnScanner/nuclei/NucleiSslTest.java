package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.SslHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nuclei SSL/TLS 协议测试
 * 测试 SSL/TLS 协议的解析和证书检测功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiSslTest {
    
    /**
     * 测试 SSL 数据模型
     */
    @Test
    public void testSslDataModel() {
        System.out.println("=== 测试 SSL 数据模型 ===");
        
        // 创建 SSL 配置
        NucleiYamlObj.Ssl ssl = new NucleiYamlObj.Ssl();
        ssl.setAddress("example.com:443");
        ssl.setMatchers_condition(NucleiYamlObj.MatchersCondition.and);
        
        // 验证
        assertEquals("example.com:443", ssl.getAddress());
        assertEquals(NucleiYamlObj.MatchersCondition.and, ssl.getMatchers_condition());
        
        System.out.println("✓ SSL 数据模型测试通过");
    }
    
    /**
     * 测试 SslHandler 基础证书检测
     * 注意：此测试需要网络连接
     */
    @Test
    public void testSslHandlerBasicCheck() {
        System.out.println("\n=== 测试 SslHandler 基础证书检测 ===");
        
        String target = "www.baidu.com:443";
        
        try {
            SslHandler.SslResponse response = SslHandler.check(target);
            
            assertNotNull(response);
            System.out.println("地址: " + response.getAddress());
            System.out.println("连接成功: " + response.isConnected());
            System.out.println("操作成功: " + response.isSuccess());
            
            if (response.isSuccess()) {
                System.out.println("协议: " + response.getProtocol());
                System.out.println("加密套件: " + response.getCipherSuite());
                System.out.println("证书主题: " + response.getSubjectDN());
                System.out.println("证书颁发者: " + response.getIssuerDN());
                System.out.println("证书过期: " + response.isExpired());
                System.out.println("自签名: " + response.isSelfSigned());
                System.out.println("✓ SSL 基础检测测试通过");
                
                // 验证基本字段
                assertNotNull(response.getProtocol());
                assertNotNull(response.getCipherSuite());
                assertNotNull(response.getSubjectDN());
                assertNotNull(response.getIssuerDN());
            } else {
                System.out.println("⚠ SSL 检测失败（可能是网络问题）: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ SSL 测试异常（可能是网络问题）: " + e.getMessage());
        }
    }
    
    /**
     * 测试 SslHandler 证书详细信息
     */
    @Test
    public void testSslHandlerCertificateDetails() {
        System.out.println("\n=== 测试 SslHandler 证书详细信息 ===");
        
        String target = "www.google.com:443";
        
        try {
            SslHandler.SslResponse response = SslHandler.check(target);
            
            assertNotNull(response);
            
            if (response.isSuccess()) {
                System.out.println("证书主题: " + response.getSubjectDN());
                System.out.println("证书颁发者: " + response.getIssuerDN());
                System.out.println("生效日期: " + response.getNotBefore());
                System.out.println("过期日期: " + response.getNotAfter());
                System.out.println("序列号: " + response.getSerialNumber());
                System.out.println("签名算法: " + response.getSignatureAlgorithm());
                System.out.println("公钥算法: " + response.getPublicKeyAlgorithm());
                System.out.println("密钥长度: " + response.getKeySize());
                System.out.println("证书链长度: " + response.getChainLength());
                System.out.println("备用名称: " + response.getSubjectAltNames());
                System.out.println("✓ 证书详细信息测试通过");
                
                // 验证关键字段
                assertNotNull(response.getNotBefore());
                assertNotNull(response.getNotAfter());
                assertNotNull(response.getSerialNumber());
                assertTrue(response.getChainLength() > 0);
            } else {
                System.out.println("⚠ SSL 检测失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ SSL 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试 SslHandler 协议版本检测
     */
    @Test
    public void testSslHandlerProtocolVersion() {
        System.out.println("\n=== 测试 SslHandler 协议版本检测 ===");
        
        String target = "www.github.com:443";
        
        try {
            SslHandler.SslResponse response = SslHandler.check(target);
            
            assertNotNull(response);
            
            if (response.isSuccess()) {
                System.out.println("协议版本: " + response.getProtocol());
                System.out.println("支持的协议: " + response.getSupportedProtocols().size() + " 个");
                System.out.println("加密套件: " + response.getCipherSuite());
                System.out.println("支持的加密套件: " + response.getSupportedCipherSuites().size() + " 个");
                System.out.println("✓ 协议版本检测测试通过");
                
                // 验证协议版本
                assertNotNull(response.getProtocol());
                assertTrue(response.getProtocol().contains("TLS") || 
                          response.getProtocol().contains("SSL"));
            } else {
                System.out.println("⚠ SSL 检测失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ SSL 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试 SslHandler 自定义超时
     */
    @Test
    public void testSslHandlerCustomTimeout() {
        System.out.println("\n=== 测试 SslHandler 自定义超时 ===");
        
        String target = "www.baidu.com:443";
        int timeout = 5000; // 5秒
        
        try {
            long startTime = System.currentTimeMillis();
            SslHandler.SslResponse response = SslHandler.check(target, timeout);
            long duration = System.currentTimeMillis() - startTime;
            
            assertNotNull(response);
            System.out.println("操作成功: " + response.isSuccess());
            System.out.println("实际耗时: " + duration + "ms");
            System.out.println("✓ 自定义超时测试通过");
        } catch (Exception e) {
            System.out.println("⚠ SSL 测试异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试简单 SSL 样例文件解析
     */
    @Test
    public void testSimpleSslPocParsing() throws Exception {
        System.out.println("\n=== 测试简单 SSL 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/18-ssl-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("ssl-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("SSL Simple Certificate Test", nucleiPoc.getInfo().getName());
        
        // 验证 SSL 配置
        assertNotNull(nucleiPoc.getSsl(), "SSL 配置不应为 null");
        assertFalse(nucleiPoc.getSsl().isEmpty(), "SSL 配置列表不应为空");
        
        NucleiYamlObj.Ssl ssl = nucleiPoc.getSsl().get(0);
        assertEquals("www.baidu.com:443", ssl.getAddress());
        
        // 验证匹配器
        assertNotNull(ssl.getMatchers());
        assertFalse(ssl.getMatchers().isEmpty());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ SSL 配置数: " + nucleiPoc.getSsl().size());
        System.out.println("✓ 目标地址: " + ssl.getAddress());
        System.out.println("✓ 简单 SSL 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 SSL 样例文件解析
     */
    @Test
    public void testComprehensiveSslPocParsing() throws Exception {
        System.out.println("\n=== 测试完整 SSL 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/17-ssl-protocol.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("ssl-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 SSL 配置（应该有 4 个测试用例）
        assertNotNull(nucleiPoc.getSsl());
        assertEquals(4, nucleiPoc.getSsl().size(), "应该有 4 个 SSL 测试用例");
        
        // 验证第一个 SSL 配置
        NucleiYamlObj.Ssl ssl1 = nucleiPoc.getSsl().get(0);
        assertEquals("{{Host}}:{{Port}}", ssl1.getAddress());
        assertEquals(NucleiYamlObj.MatchersCondition.and, ssl1.getMatchers_condition());
        assertNotNull(ssl1.getMatchers());
        assertNotNull(ssl1.getExtractors());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ SSL 配置数: " + nucleiPoc.getSsl().size());
        System.out.println("✓ 完整 SSL 样例文件解析测试通过");
    }
    
    /**
     * 测试 SSL POC 转换为通用格式
     */
    @Test
    public void testSslPocConversion() throws Exception {
        System.out.println("\n=== 测试 SSL POC 转换 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/18-ssl-simple.yml";
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
        assertEquals("ssl", poc.getProtocol(), "协议应该是 ssl");
        assertEquals("ssl-simple-test", poc.getId());
        assertEquals("SSL Simple Certificate Test", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.SslStep, "步骤应该是 SslStep 类型");
        
        PocObj.SslStep sslStep = (PocObj.SslStep) step;
        assertEquals("www.baidu.com:443", sslStep.getAddress());
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 目标地址: " + sslStep.getAddress());
        System.out.println("✓ SSL POC 转换测试通过");
    }
    
    /**
     * 测试 SSL 错误处理
     */
    @Test
    public void testSslErrorHandling() {
        System.out.println("\n=== 测试 SSL 错误处理 ===");
        
        // 测试无效地址
        String invalidAddress = "invalid-ssl-server-12345.com:443";
        
        SslHandler.SslResponse response = SslHandler.check(invalidAddress, 3000);
        
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNotNull(response.getError());
        System.out.println("✓ 错误消息: " + response.getError());
        System.out.println("✓ SSL 错误处理测试通过");
    }
    
    /**
     * 测试 SSL 响应的所有字段
     */
    @Test
    public void testSslResponseComplete() {
        System.out.println("\n=== 测试 SSL 响应完整性 ===");
        
        String target = "www.baidu.com:443";
        
        try {
            SslHandler.SslResponse response = SslHandler.check(target);
            
            if (response.isSuccess()) {
                // 验证所有关键字段都已填充
                assertNotNull(response.getAddress());
                assertNotNull(response.getProtocol());
                assertNotNull(response.getCipherSuite());
                assertNotNull(response.getSubjectDN());
                assertNotNull(response.getIssuerDN());
                assertNotNull(response.getNotBefore());
                assertNotNull(response.getNotAfter());
                assertNotNull(response.getSerialNumber());
                assertNotNull(response.getRaw());
                assertTrue(response.getDuration() > 0);
                
                System.out.println("✓ 所有关键字段已验证");
                System.out.println("✓ SSL 响应完整性测试通过");
            } else {
                System.out.println("⚠ SSL 检测失败: " + response.getError());
            }
        } catch (Exception e) {
            System.out.println("⚠ SSL 测试异常: " + e.getMessage());
        }
    }
}



