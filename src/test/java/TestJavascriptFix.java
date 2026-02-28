import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.List;

/**
 * 独立测试程序 - 验证 JavaScript 字段修复
 */
public class TestJavascriptFix {

    public static void main(String[] args) {
        System.out.println("=== 测试 JavaScript 字段支持 ===\n");

        // 测试目录：javascript POC所在目录
        String javascriptDir = "src/main/resources/poc/nucleiPoc/javascript";

        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        loader.setSkipInvalidPocs(true);

        try {
            System.out.println("正在加载 JavaScript 目录下的POC...");
            List<PocObj.Poc> pocs = loader.loadFromDirectory(javascriptDir);

            System.out.println("\n加载结果统计:");
            System.out.println("总计成功加载: " + pocs.size() + " 个POC");

            // 统计协议类型分布
            long javascriptCount = pocs.stream()
                .filter(p -> "javascript".equals(p.getProtocol()))
                .count();
            long codeCount = pocs.stream()
                .filter(p -> "code".equals(p.getProtocol()))
                .count();
            long otherCount = pocs.size() - javascriptCount - codeCount;

            System.out.println("\n协议类型分布:");
            System.out.println("  - JavaScript 协议: " + javascriptCount);
            System.out.println("  - Code 协议: " + codeCount);
            System.out.println("  - 其他协议: " + otherCount);

            // 显示前5个成功加载的POC
            System.out.println("\n成功加载的POC示例 (前5个):");
            pocs.stream()
                .limit(5)
                .forEach(poc -> {
                    System.out.println("  ✅ " + poc.getId() +
                        " [协议: " + poc.getProtocol() +
                        ", 步骤数: " + (poc.getVerifySteps() != null ? poc.getVerifySteps().size() : 0) + "]");
                });

            if (javascriptCount > 0) {
                System.out.println("\n✅ 修复成功! JavaScript 字段现在可以被正确识别了!");
            } else {
                System.out.println("\n⚠️ 警告: 没有检测到使用 JavaScript 协议的POC");
            }

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
