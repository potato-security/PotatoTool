package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Nuclei DNS 协议测试
 * 测试 DNS 协议的解析和查询功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class NucleiDnsTest {

    private DnsHandler.DnsResponse requireReachableDns(DnsHandler.DnsResponse response, String scenario) {
        assertNotNull(response);
        assumeTrue(response.isSuccess(),
                scenario + " 依赖真实 DNS 出口；当前环境不可达，跳过验证。错误: " + response.getError());
        return response;
    }
    
    /**
     * 测试 DNS 数据模型
     */
    @Test
    public void testDnsDataModel() {
        System.out.println("=== 测试 DNS 数据模型 ===");
        
        // 创建 DNS 配置
        NucleiYamlObj.Dns dns = new NucleiYamlObj.Dns();
        dns.setName("www.example.com");
        dns.setType("A");
        dns.setRecursion(true);
        dns.setRetries(2);
        
        // 验证默认值
        assertEquals("A", dns.getType());
        assertEquals("INET", dns.getDns_class());
        assertTrue(dns.isRecursion());
        assertEquals(2, dns.getRetries());
        assertNotNull(dns.getName());
        
        System.out.println("✓ DNS 数据模型测试通过");
    }
    
    /**
     * 测试 DnsHandler A 记录查询
     */
    @Test
    public void testDnsHandlerARecord() {
        System.out.println("\n=== 测试 DnsHandler A 记录查询 ===");
        
        DnsHandler.DnsResponse response =
                requireReachableDns(DnsHandler.query("www.example.com", "A"), "A 记录查询");
        
        assertEquals("www.example.com", response.getDomain());
        assertEquals("A", response.getQueryType());
        assertFalse(response.getAnswers().isEmpty(), "应该有 DNS 应答");
        assertTrue(response.getDuration() > 0, "查询耗时应该大于 0");
        
        System.out.println("域名: " + response.getDomain());
        System.out.println("查询类型: " + response.getQueryType());
        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("耗时: " + response.getDuration() + "ms");
        System.out.println("应答: " + response.getAnswers());
        System.out.println("✓ A 记录查询测试通过");
    }
    
    /**
     * 测试 DnsHandler AAAA 记录查询
     */
    @Test
    public void testDnsHandlerAAAARecord() {
        System.out.println("\n=== 测试 DnsHandler AAAA 记录查询 ===");
        
        DnsHandler.DnsResponse response =
                requireReachableDns(DnsHandler.query("www.google.com", "AAAA"), "AAAA 记录查询");
        
        assertEquals("AAAA", response.getQueryType());
        
        System.out.println("域名: " + response.getDomain());
        System.out.println("查询类型: " + response.getQueryType());
        System.out.println("应答: " + response.getAnswers());
        System.out.println("✓ AAAA 记录查询测试通过");
    }
    
    /**
     * 测试 DnsHandler MX 记录查询
     */
    @Test
    public void testDnsHandlerMXRecord() {
        System.out.println("\n=== 测试 DnsHandler MX 记录查询 ===");
        
        DnsHandler.DnsResponse response =
                requireReachableDns(DnsHandler.query("gmail.com", "MX"), "MX 记录查询");
        
        assertEquals("MX", response.getQueryType());
        assertFalse(response.getAnswers().isEmpty(), "Gmail 应该有 MX 记录");
        
        System.out.println("域名: " + response.getDomain());
        System.out.println("查询类型: " + response.getQueryType());
        System.out.println("MX 记录: " + response.getAnswers());
        System.out.println("✓ MX 记录查询测试通过");
    }
    
    /**
     * 测试 DnsHandler TXT 记录查询
     */
    @Test
    public void testDnsHandlerTXTRecord() {
        System.out.println("\n=== 测试 DnsHandler TXT 记录查询 ===");
        
        DnsHandler.DnsResponse response =
                requireReachableDns(DnsHandler.query("google.com", "TXT"), "TXT 记录查询");
        
        assertEquals("TXT", response.getQueryType());
        
        System.out.println("域名: " + response.getDomain());
        System.out.println("查询类型: " + response.getQueryType());
        System.out.println("TXT 记录: " + response.getAnswers());
        System.out.println("✓ TXT 记录查询测试通过");
    }
    
    /**
     * 测试使用自定义 DNS 服务器
     */
    @Test
    public void testDnsHandlerCustomResolver() {
        System.out.println("\n=== 测试自定义 DNS 服务器 ===");
        
        // 使用 Google Public DNS
        DnsHandler.DnsResponse response = requireReachableDns(
                DnsHandler.query("www.baidu.com", "A", "8.8.8.8", true, 2),
                "自定义 DNS 服务器查询");
        
        assertFalse(response.getAnswers().isEmpty(), "应该有 DNS 应答");
        
        System.out.println("域名: " + response.getDomain());
        System.out.println("DNS 服务器: 8.8.8.8");
        System.out.println("应答: " + response.getAnswers());
        System.out.println("✓ 自定义 DNS 服务器测试通过");
    }
    
    /**
     * 测试简单 DNS 样例文件解析
     */
    @Test
    public void testSimpleDnsPocParsing() throws Exception {
        System.out.println("\n=== 测试简单 DNS 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/14-dns-simple.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("dns-simple-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        assertEquals("DNS Simple A Record Test", nucleiPoc.getInfo().getName());
        
        // 验证 DNS 配置
        assertNotNull(nucleiPoc.getDns(), "DNS 配置不应为 null");
        assertFalse(nucleiPoc.getDns().isEmpty(), "DNS 配置列表不应为空");
        
        NucleiYamlObj.Dns dns = nucleiPoc.getDns().get(0);
        assertNotNull(dns.getName());
        assertEquals("www.example.com", dns.getName());
        assertEquals("A", dns.getType());
        
        // 验证匹配器
        assertNotNull(dns.getMatchers());
        assertFalse(dns.getMatchers().isEmpty());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ 名称: " + nucleiPoc.getInfo().getName());
        System.out.println("✓ DNS 配置数: " + nucleiPoc.getDns().size());
        System.out.println("✓ 域名: " + dns.getName());
        System.out.println("✓ 类型: " + dns.getType());
        System.out.println("✓ 简单 DNS 样例文件解析测试通过");
    }
    
    /**
     * 测试完整 DNS 样例文件解析
     */
    @Test
    public void testComprehensiveDnsPocParsing() throws Exception {
        System.out.println("\n=== 测试完整 DNS 样例文件解析 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/13-dns-protocol.yml";
        File pocFile = new File(pocPath);
        
        if (!pocFile.exists()) {
            System.out.println("⚠ 测试文件不存在，跳过测试: " + pocPath);
            return;
        }
        
        // 加载 Nuclei YAML POC
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(pocPath);
        assertNotNull(nucleiPoc, "POC 对象不应为 null");
        
        // 验证基本信息
        assertEquals("dns-protocol-test", nucleiPoc.getId());
        assertNotNull(nucleiPoc.getInfo());
        
        // 验证 DNS 配置（应该有 7 个测试用例）
        assertNotNull(nucleiPoc.getDns());
        assertEquals(7, nucleiPoc.getDns().size(), "应该有 7 个 DNS 测试用例");
        
        // 验证第一个 DNS 配置（A 记录）
        NucleiYamlObj.Dns dns1 = nucleiPoc.getDns().get(0);
        assertEquals("A", dns1.getType());
        assertEquals("www.example.com", dns1.getName());
        assertTrue(dns1.isRecursion());
        assertEquals(2, dns1.getRetries());
        
        // 验证第二个 DNS 配置（AAAA 记录）
        NucleiYamlObj.Dns dns2 = nucleiPoc.getDns().get(1);
        assertEquals("AAAA", dns2.getType());
        assertEquals("www.google.com", dns2.getName());
        
        // 验证第四个 DNS 配置（MX 记录）
        NucleiYamlObj.Dns dns4 = nucleiPoc.getDns().get(3);
        assertEquals("MX", dns4.getType());
        assertEquals("gmail.com", dns4.getName());
        
        // 验证第七个 DNS 配置（自定义解析器）
        NucleiYamlObj.Dns dns7 = nucleiPoc.getDns().get(6);
        assertEquals("A", dns7.getType());
        assertEquals("www.baidu.com", dns7.getName());
        assertEquals("8.8.8.8", dns7.getResolvers());
        assertEquals(3, dns7.getRetries());
        
        System.out.println("✓ POC ID: " + nucleiPoc.getId());
        System.out.println("✓ DNS 配置数: " + nucleiPoc.getDns().size());
        System.out.println("✓ 完整 DNS 样例文件解析测试通过");
    }
    
    /**
     * 测试 DNS POC 转换为通用格式
     */
    @Test
    public void testDnsPocConversion() throws Exception {
        System.out.println("\n=== 测试 DNS POC 转换 ===");
        
        String pocPath = "src/test/resources/nuclei-poc-samples/14-dns-simple.yml";
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
        assertEquals("dns", poc.getProtocol(), "协议应该是 dns");
        assertEquals("dns-simple-test", poc.getId());
        assertEquals("DNS Simple A Record Test", poc.getName());
        
        // 验证验证步骤
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());
        
        PocObj.PocStep step = poc.getVerifySteps().get(0);
        assertTrue(step instanceof PocObj.DnsStep, "步骤应该是 DnsStep 类型");
        
        PocObj.DnsStep dnsStep = (PocObj.DnsStep) step;
        assertEquals("www.example.com", dnsStep.getDomain());
        assertEquals("A", dnsStep.getType());
        
        System.out.println("✓ 协议: " + poc.getProtocol());
        System.out.println("✓ POC ID: " + poc.getId());
        System.out.println("✓ 步骤数: " + poc.getVerifySteps().size());
        System.out.println("✓ 域名: " + dnsStep.getDomain());
        System.out.println("✓ DNS POC 转换测试通过");
    }
    
    /**
     * 测试批量 DNS 查询
     */
    @Test
    public void testBatchDnsQuery() {
        System.out.println("\n=== 测试批量 DNS 查询 ===");
        
        List<String> domains = Arrays.asList(
            "www.google.com",
            "www.baidu.com",
            "www.github.com"
        );
        
        List<DnsHandler.DnsResponse> responses = DnsHandler.batchQuery(domains, "A", null, true, 2);
        
        assertNotNull(responses);
        assertEquals(3, responses.size());
        
        for (DnsHandler.DnsResponse response : responses) {
            System.out.println("域名: " + response.getDomain() + 
                             " | 成功: " + response.isSuccess() + 
                             " | 应答: " + response.getAnswers());
        }
        
        System.out.println("✓ 批量 DNS 查询测试通过");
    }
}


