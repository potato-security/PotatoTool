package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NucleiPocConverter 官方样例集成测试")
public class NucleiPocIntegrationTest {

    private final NucleiPocConverter converter = new NucleiPocConverter();

    @Test
    @DisplayName("raw-request 样例应成功转换")
    void testRawRequestSampleConversion() {
        PocObj.Poc poc = convert("nuclei-poc-samples/11-raw-request.yml");

        assertNotNull(poc, "raw-request 样例应转换成功");
        assertTrue(poc.getVerifySteps() != null && !poc.getVerifySteps().isEmpty(), "应存在可执行步骤");
        assertTrue("http".equalsIgnoreCase(poc.getProtocol()), "协议应识别为 http");
    }

    @Test
    @DisplayName("variables-flow 样例应保留 flow 表达式")
    void testVariablesFlowSampleConversion() {
        PocObj.Poc poc = convert("nuclei-poc-samples/05-variables-flow.yml");

        assertNotNull(poc, "variables-flow 样例应转换成功");
        assertNotNull(poc.getFlow(), "flow 表达式应保留");
        assertTrue(poc.getFlow().contains("http(1)"), "flow 应包含步骤引用");
    }

    @Test
    @DisplayName("payload-fuzzing 样例应识别 clusterbomb")
    void testPayloadFuzzingSampleConversion() {
        PocObj.Poc poc = convert("nuclei-poc-samples/06-payloads-fuzzing.yml");

        assertNotNull(poc, "payload-fuzzing 样例应转换成功");
        assertNotNull(poc.getVariablesType(), "变量组合模式应存在");
        assertTrue(poc.getVariablesType() == PocObj.VariablesType.clusterbomb,
                "attack: clusterbomb 应映射为 VariablesType.clusterbomb");
    }

    private PocObj.Poc convert(String resourcePath) {
        File file = new File("src/test/resources/" + resourcePath);
        assertTrue(file.exists(), "测试资源不存在: " + resourcePath);

        NucleiYamlObj.Poc nucleiPoc;
        try {
            nucleiPoc = PocConverter.loadNucleiYamlPocFile(file.getPath());
        } catch (Exception e) {
            throw new AssertionError("加载 YAML 失败: " + resourcePath, e);
        }

        assertNotNull(nucleiPoc, "YAML 解析失败: " + resourcePath);

        return converter.convert(nucleiPoc);
    }
}
