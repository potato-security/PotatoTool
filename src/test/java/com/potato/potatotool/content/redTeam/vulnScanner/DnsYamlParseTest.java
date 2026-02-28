package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * DNS YAML解析单元测试
 * 专门用于调试 worksites-takeover.yaml 解析问题
 */
public class DnsYamlParseTest {

    @Test
    public void testWorksitesTakeoverParsing() {
        String pocPath = "src/main/resources/poc/nucleiPoc/http/takeovers/worksites-takeover.yaml";

        File file = new File(pocPath);
        if (!file.exists()) {
            System.err.println("❌ POC文件不存在: " + pocPath);
            System.err.println("   绝对路径: " + file.getAbsolutePath());
            return;
        }

        System.out.println("=== 开始测试 DNS YAML 解析 ===");
        System.out.println("文件路径: " + pocPath);
        System.out.println("文件存在: " + file.exists());
        System.out.println();

        try {
            // 尝试加载并解析YAML文件
            System.out.println("[步骤1] 加载 YAML 文件...");
            NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(pocPath);

            if (poc == null) {
                System.err.println("❌ 解析结果为 null");
                return;
            }

            System.out.println("✅ YAML 解析成功！");
            System.out.println();

            // 打印POC基本信息
            System.out.println("[步骤2] POC 基本信息:");
            System.out.println("  ID: " + poc.getId());
            System.out.println("  Info: " + (poc.getInfo() != null ? poc.getInfo().getName() : "null"));
            System.out.println();

            // 检查DNS协议配置
            System.out.println("[步骤3] 检查 DNS 协议配置:");
            if (poc.getDns() == null) {
                System.err.println("❌ DNS 配置为 null");
                return;
            }

            System.out.println("  DNS 请求数量: " + poc.getDns().size());

            // 遍历每个DNS请求
            for (int i = 0; i < poc.getDns().size(); i++) {
                NucleiYamlObj.Dns dns = poc.getDns().get(i);
                System.out.println();
                System.out.println("  DNS 请求 #" + (i + 1) + ":");
                System.out.println("    - name: " + dns.getName());
                System.out.println("    - type: " + dns.getType());
                System.out.println("    - dns_class: " + dns.getDns_class());
                System.out.println("    - recursion: " + dns.isRecursion());
                System.out.println("    - retries: " + dns.getRetries());
                System.out.println("    - matchers: " + (dns.getMatchers() != null ? dns.getMatchers().size() : 0) + " 个");
            }

            // 检查HTTP协议配置
            System.out.println();
            System.out.println("[步骤4] 检查 HTTP 协议配置:");
            if (poc.getHttp() == null) {
                System.out.println("  HTTP 配置为 null");
            } else {
                System.out.println("  HTTP 请求数量: " + poc.getHttp().size());
            }

            System.out.println();
            System.out.println("=== 测试完成 ===");

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ 解析失败！");
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误信息: " + e.getMessage());
            System.err.println();
            System.err.println("详细堆栈:");
            e.printStackTrace();

            // 检查是否是特定的SnakeYAML错误
            if (e.getMessage() != null) {
                if (e.getMessage().contains("No writable property")) {
                    System.err.println();
                    System.err.println(">>> 分析: 这是 PropertyUtils 找不到可写属性的错误");
                    System.err.println(">>> 可能原因: setter方法签名不匹配或Lombok注解配置问题");
                } else if (e.getMessage().contains("No single argument constructor")) {
                    System.err.println();
                    System.err.println(">>> 分析: SnakeYAML无法创建对象实例");
                    System.err.println(">>> 可能原因: 需要List类型但只提供了String值，且没有合适的转换方法");
                }
            }
        }
    }

    /**
     * 测试简单的DNS配置
     */
    @Test
    public void testSimpleDnsConfig() {
        System.out.println("=== 测试简单 DNS 配置 ===");

        // 手动创建一个DNS配置对象
        NucleiYamlObj.Dns dns = new NucleiYamlObj.Dns();

        System.out.println("[测试1] 尝试设置 name 为字符串:");
        try {
            dns.setName("example.com");
            System.out.println("  ✅ setName(String) 调用成功");
            System.out.println("  name 值: " + dns.getName());
        } catch (Exception e) {
            System.err.println("  ❌ setName(String) 调用失败: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[测试2] 尝试设置其他字段:");
        dns.setType("A");
        dns.setRecursion(true);
        System.out.println("  ✅ 其他字段设置成功");
        System.out.println("  type: " + dns.getType());
        System.out.println("  recursion: " + dns.isRecursion());

        System.out.println();
        System.out.println("=== 简单测试完成 ===");
    }
}
