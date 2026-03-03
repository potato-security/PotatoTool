package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nuclei 无协议块诊断测试")
public class NucleiNoProtocolDiagnosticTest {

    private final NucleiPocConverter converter = new NucleiPocConverter();

    @Test
    void testNoProtocolBlock_ShouldReturnStructuredDiagnosticInsteadOfNull() {
        NucleiYamlObj.Poc nucleiPoc = buildNoProtocolPoc();
        PocObj.Poc poc = converter.convert(nucleiPoc);

        assertNotNull(poc, "无协议块模板应返回结构化 Poc 对象");
        assertEquals("nuclei", poc.getOriginalFormat(), "应标记原始格式为 nuclei");
    }

    @Test
    void testNoProtocolBlock_ShouldEmitUnsupportedCapabilityCode() {
        NucleiYamlObj.Poc nucleiPoc = buildNoProtocolPoc();
        PocObj.Poc poc = converter.convert(nucleiPoc);

        assertNotNull(poc);
        List<Map<String, Object>> unsupported = poc.getUnsupportedCapabilities();
        assertNotNull(unsupported, "unsupportedCapabilities 不应为 null");
        assertFalse(unsupported.isEmpty(), "unsupportedCapabilities 应至少包含一条诊断");

        boolean found = false;
        for (Map<String, Object> item : unsupported) {
            if (item == null) {
                continue;
            }
            if ("NO_PROTOCOL_BLOCK".equals(String.valueOf(item.get("code")))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "应包含 code=NO_PROTOCOL_BLOCK 的诊断");
    }

    @Test
    void testNoProtocolBlock_ShouldContainActionDropAndP0() {
        NucleiYamlObj.Poc nucleiPoc = buildNoProtocolPoc();
        PocObj.Poc poc = converter.convert(nucleiPoc);

        assertNotNull(poc);
        List<Map<String, Object>> unsupported = poc.getUnsupportedCapabilities();
        assertNotNull(unsupported);

        Map<String, Object> target = null;
        for (Map<String, Object> item : unsupported) {
            if (item != null && "NO_PROTOCOL_BLOCK".equals(String.valueOf(item.get("code")))) {
                target = item;
                break;
            }
        }

        assertNotNull(target, "应找到 NO_PROTOCOL_BLOCK 诊断项");
        assertEquals("P0", String.valueOf(target.get("level")), "level 应为 P0");
        assertEquals("drop", String.valueOf(target.get("action")), "action 应为 drop");
        assertEquals("协议检测结果: none", String.valueOf(target.get("value")), "无协议块时 value 应为 none 快照");
    }

    private NucleiYamlObj.Poc buildNoProtocolPoc() {
        NucleiYamlObj.Poc poc = new NucleiYamlObj.Poc();
        poc.setId("no-protocol-poc");

        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("no-protocol-poc");
        info.setSeverity(NucleiYamlObj.Info.Severity.low);
        poc.setInfo(info);

        return poc;
    }
}
