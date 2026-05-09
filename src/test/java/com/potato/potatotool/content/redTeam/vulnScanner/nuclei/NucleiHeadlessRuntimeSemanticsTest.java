package com.potato.potatotool.content.redTeam.vulnScanner.nuclei;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HeadlessHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Nuclei Headless 运行时语义兼容测试")
public class NucleiHeadlessRuntimeSemanticsTest {

    @Test
    @DisplayName("headless 转换应保留命名 script 输出供 matcher/extractor.part 使用")
    void headlessConversionShouldPreserveNamedActions() throws Exception {
        String path = "src/main/resources/poc/nucleiPoc/headless/extract-urls.yaml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(path);
        assertNotNull(nucleiPoc);

        PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
        assertNotNull(poc);
        assertEquals("headless", poc.getProtocol());
        assertNotNull(poc.getVerifySteps());
        assertFalse(poc.getVerifySteps().isEmpty());

        PocObj.HeadlessStep step = (PocObj.HeadlessStep) poc.getVerifySteps().get(0);
        assertNotNull(step.getActions());
        assertEquals("extract", step.getActions().get(2).getName());
        assertNotNull(step.getExtractors());
        assertEquals("extract", step.getExtractors().get(0).getPart());
    }

    @Test
    @DisplayName("postmessage-tracker 模板应转换出 alerts 命名 part 与 kval extractor")
    void postmessageTrackerShouldKeepAlertsPart() throws Exception {
        String path = "src/main/resources/poc/nucleiPoc/headless/postmessage-tracker.yaml";
        NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(path);
        assertNotNull(nucleiPoc);

        PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
        assertNotNull(poc);
        PocObj.HeadlessStep step = (PocObj.HeadlessStep) poc.getVerifySteps().get(0);

        assertEquals("alerts", step.getActions().get(step.getActions().size() - 1).getName());
        assertEquals("alerts", step.getMatchers().get(0).getPart());
        assertEquals(PocObj.MatcherType.KVAL, step.getExtractors().get(0).getType());
        assertEquals("alerts", step.getExtractors().get(0).getPart());
    }

    @Test
    @DisplayName("真实 headless 模板集合中的命名 part 应都保留")
    void realHeadlessTemplatesShouldRetainNamedParts() throws Exception {
        String[] paths = new String[] {
                "src/main/resources/poc/nucleiPoc/headless/prototype-pollution-check.yaml",
                "src/main/resources/poc/nucleiPoc/headless/window-name-domxss.yaml",
                "src/main/resources/poc/nucleiPoc/headless/technologies/js-libraries-detect.yaml"
        };

        for (String path : paths) {
            File file = new File(path);
            assertTrue(file.exists(), "模板应存在: " + path);
            NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(path);
            PocObj.Poc poc = new NucleiPocConverter().convert(nucleiPoc);
            assertNotNull(poc, path);
            assertNotNull(poc.getVerifySteps(), path);
            assertFalse(poc.getVerifySteps().isEmpty(), path);

            PocObj.HeadlessStep step = (PocObj.HeadlessStep) poc.getVerifySteps().get(0);
            List<PocObj.BrowserAction> actions = step.getActions();
            assertNotNull(actions, path);
            boolean hasNamedAction = false;
            for (PocObj.BrowserAction action : actions) {
                if (action != null && action.getName() != null && !action.getName().trim().isEmpty()) {
                    hasNamedAction = true;
                    break;
                }
            }
            assertTrue(hasNamedAction, "应保留命名动作输出: " + path);
        }
    }

    @Test
    @DisplayName("headless BrowserStep 应保留 name")
    void browserStepShouldRetainName() {
        HeadlessHandler.BrowserStep browserStep = new HeadlessHandler.BrowserStep("script", "alerts", null);
        assertEquals("script", browserStep.getAction());
        assertEquals("alerts", browserStep.getName());
        assertNotNull(browserStep.getArgs());
    }
}
