package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 JavaScript 协议字段解析
 * 验证 NucleiYamlObj.Poc 类是否正确支持 javascript 字段
 */
public class JavascriptFieldTest {

    @Test
    public void testJavascriptFieldParsing() {
        // 测试文件：db2-discover.yaml (使用 javascript: 字段)
        String pocPath = "src/main/resources/poc/nucleiPoc/javascript/udp/detection/db2-discover.yaml";

        try {
            PocLoader loader = new PocLoader();
            PocObj.Poc poc = loader.loadFromFile(pocPath);

            // 验证POC成功转换
            assertNotNull(poc, "POC应该成功转换");
            assertEquals("db2-discover", poc.getId(), "POC ID应该正确");

            // 验证协议类型
            assertEquals("javascript", poc.getProtocol(), "协议类型应该是 javascript");

            // 验证有验证步骤
            assertNotNull(poc.getVerifySteps(), "应该有 verify steps");
            assertFalse(poc.getVerifySteps().isEmpty(), "verify steps 不应为空");

            System.out.println("✅ JavaScript 字段解析测试通过");
            System.out.println("   POC ID: " + poc.getId());
            System.out.println("   协议: " + poc.getProtocol());
            System.out.println("   Verify Steps: " + poc.getVerifySteps().size());

        } catch (Exception e) {
            fail("解析 JavaScript POC 时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleJavascriptPocs() {
        // 测试多个使用 javascript 字段的POC
        String[] pocPaths = {
            "src/main/resources/poc/nucleiPoc/javascript/udp/detection/db2-discover.yaml",
            "src/main/resources/poc/nucleiPoc/javascript/udp/detection/tftp-detect.yaml"
        };

        PocLoader loader = new PocLoader();
        loader.setSkipInvalidPocs(true);
        int successCount = 0;

        for (String pocPath : pocPaths) {
            try {
                PocObj.Poc poc = loader.loadFromFile(pocPath);
                if (poc != null && "javascript".equals(poc.getProtocol())) {
                    successCount++;
                    System.out.println("✅ " + poc.getId() + " - 成功解析");
                }
            } catch (Exception e) {
                System.err.println("❌ " + pocPath + " - 解析失败: " + e.getMessage());
            }
        }

        assertTrue(successCount > 0, "至少应该有一个POC成功解析");
        System.out.println("\n总计: " + successCount + "/" + pocPaths.length + " 个POC成功解析");
    }
}
