package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PocExecutorHeadlessExecutionTest {

    @Test
    public void testResolveHeadlessStartUrlReplacesVariables() {
        ScanConfig config = new ScanConfig();
        config.setEnableHeadless(true);
        PocExecutor executor = new PocExecutor(config);

        PocObj.HeadlessStep step = new PocObj.HeadlessStep();
        step.setUrl("http://{{host}}/login");

        Map<String, Object> variables = new HashMap<>();
        variables.put("host", "example.com");

        assertEquals("http://example.com/login", executor.resolveHeadlessStartUrl(step, variables));
    }

    @Test
    public void testResolveHeadlessTimeoutUsesStepTimeoutThenScanConfig() {
        ScanConfig config = new ScanConfig();
        config.setEnableHeadless(true);
        config.setTimeout(9);
        PocExecutor executor = new PocExecutor(config);

        PocObj.HeadlessStep step = new PocObj.HeadlessStep();
        step.setTimeout(4);

        assertEquals(4, executor.resolveHeadlessTimeout(step, new PocObj.GlobalConfig()));

        step.setTimeout(0);
        assertEquals(9, executor.resolveHeadlessTimeout(step, new PocObj.GlobalConfig()));
    }

    @Test
    public void testExecuteHeadlessStepReturnsFalseWhenHeadlessDisabled() throws Exception {
        ScanConfig config = new ScanConfig();
        config.setEnableHeadless(false);
        PocExecutor executor = new PocExecutor(config);

        PocObj.HeadlessStep step = new PocObj.HeadlessStep();
        step.setUrl("http://{{host}}/login");

        Map<String, Object> variables = new HashMap<>();
        variables.put("host", "example.com");

        Method method = PocExecutor.class.getDeclaredMethod(
                "executeHeadlessStep", PocObj.HeadlessStep.class, Map.class, PocObj.GlobalConfig.class);
        method.setAccessible(true);

        boolean result = (Boolean) method.invoke(executor, step, variables, new PocObj.GlobalConfig());

        assertFalse(result);
    }
}
