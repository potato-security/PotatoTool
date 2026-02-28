import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;

import java.io.File;

/**
 * 简单的DNS解析测试 - 不依赖JUnit，可以直接运行
 */
public class SimpleDnsTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("               DNS YAML 解析独立测试");
        System.out.println("================================================================================");
        System.out.println();

        // 测试1: 手动创建DNS对象
        test1_ManualDnsCreation();

        System.out.println();
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println();

        // 测试2: 解析实际的YAML文件
        test2_ParseWorksitesTakeover();

        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                           测试完成");
        System.out.println("================================================================================");
    }

    /**
     * 测试1: 手动创建DNS对象
     */
    private static void test1_ManualDnsCreation() {
        System.out.println("[测试1] 手动创建 DNS 对象");
        System.out.println();

        try {
            NucleiYamlObj.Dns dns = new NucleiYamlObj.Dns();

            // 测试 setName(String)
            System.out.println("  步骤1: 调用 setName(String)");
            dns.setName("{{FQDN}}");
            System.out.println("    ✅ 成功");

            // 验证 getName()
            System.out.println("  步骤2: 调用 getName()");
            String name = dns.getName();
            System.out.println("    返回值: " + name);
            System.out.println("    ✅ 成功");

            // 设置其他字段
            System.out.println("  步骤3: 设置其他字段");
            dns.setType("A");
            dns.setDns_class("INET");
            dns.setRecursion(true);
            dns.setRetries(3);
            System.out.println("    ✅ 成功");

            // 打印完整对象
            System.out.println();
            System.out.println("  完整 DNS 对象:");
            System.out.println("    name: " + dns.getName());
            System.out.println("    type: " + dns.getType());
            System.out.println("    dns_class: " + dns.getDns_class());
            System.out.println("    recursion: " + dns.isRecursion());
            System.out.println("    retries: " + dns.getRetries());

            System.out.println();
            System.out.println("  ✅ 测试1通过");

        } catch (Exception e) {
            System.err.println();
            System.err.println("  ❌ 测试1失败");
            System.err.println("  错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试2: 解析 worksites-takeover.yaml
     */
    private static void test2_ParseWorksitesTakeover() {
        System.out.println("[测试2] 解析 worksites-takeover.yaml");
        System.out.println();

        String pocPath = "src/main/resources/poc/nucleiPoc/http/takeovers/worksites-takeover.yaml";

        File file = new File(pocPath);
        if (!file.exists()) {
            System.err.println("  ❌ 文件不存在: " + pocPath);
            System.err.println("  绝对路径: " + file.getAbsolutePath());
            return;
        }

        System.out.println("  文件路径: " + pocPath);
        System.out.println("  文件存在: ✓");
        System.out.println();

        try {
            System.out.println("  步骤1: 调用 PocConverter.loadNucleiYamlPocFile()...");
            NucleiYamlObj.Poc poc = PocConverter.loadNucleiYamlPocFile(pocPath);

            if (poc == null) {
                System.err.println("  ❌ 解析结果为 null");
                return;
            }

            System.out.println("    ✅ YAML 解析成功");
            System.out.println();

            // 检查基本信息
            System.out.println("  步骤2: 检查 POC 基本信息");
            System.out.println("    ID: " + poc.getId());
            if (poc.getInfo() != null) {
                System.out.println("    Name: " + poc.getInfo().getName());
                System.out.println("    Severity: " + poc.getInfo().getSeverity());
            }
            System.out.println();

            // 检查DNS配置
            System.out.println("  步骤3: 检查 DNS 协议配置");
            if (poc.getDns() == null) {
                System.err.println("    ❌ DNS 配置为 null");
                return;
            }

            System.out.println("    DNS 请求数量: " + poc.getDns().size());
            System.out.println();

            // 遍历每个DNS请求
            for (int i = 0; i < poc.getDns().size(); i++) {
                NucleiYamlObj.Dns dns = poc.getDns().get(i);
                System.out.println("    DNS 请求 #" + (i + 1) + ":");
                System.out.println("      name: " + dns.getName());
                System.out.println("      type: " + dns.getType());
                System.out.println("      dns_class: " + dns.getDns_class());
                System.out.println("      recursion: " + dns.isRecursion());
                System.out.println("      retries: " + dns.getRetries());

                if (dns.getMatchers() != null) {
                    System.out.println("      matchers: " + dns.getMatchers().size() + " 个");
                }
            }

            // 检查HTTP配置
            System.out.println();
            System.out.println("  步骤4: 检查 HTTP 协议配置");
            if (poc.getHttp() != null && !poc.getHttp().isEmpty()) {
                System.out.println("    HTTP 请求数量: " + poc.getHttp().size());
            } else {
                System.out.println("    HTTP 配置为 null 或空");
            }

            System.out.println();
            System.out.println("  ✅ 测试2通过");

        } catch (Exception e) {
            System.err.println();
            System.err.println("  ❌ 测试2失败");
            System.err.println("  错误类型: " + e.getClass().getName());
            System.err.println("  错误信息: " + e.getMessage());
            System.err.println();
            System.err.println("  详细堆栈:");
            e.printStackTrace();

            // 分析错误类型
            System.err.println();
            if (e.getMessage() != null) {
                if (e.getMessage().contains("No writable property")) {
                    System.err.println("  >>> 错误类型: PropertyUtils 找不到可写属性");
                    System.err.println("  >>> 说明: setter 方法不存在或签名不匹配");
                } else if (e.getMessage().contains("No single argument constructor")) {
                    System.err.println("  >>> 错误类型: SnakeYAML 无法创建对象");
                    System.err.println("  >>> 说明: 字段类型与YAML值类型不匹配");
                } else if (e.getMessage().contains("Cannot create property")) {
                    System.err.println("  >>> 错误类型: 无法创建属性");
                    System.err.println("  >>> 说明: 字段定义或setter有问题");
                }
            }
        }
    }
}
