package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.VulnerabilityFoundEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
import com.potato.potatotool.content.redTeam.vulnScanner.model.TaskState;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScanEngineResumeAndServiceIsolationTest {

    private static final List<ScanEngine> ENGINES = new ArrayList<ScanEngine>();

    @AfterAll
    static void tearDown() {
        for (ScanEngine engine : ENGINES) {
            engine.shutdown();
        }
        ENGINES.clear();
    }

    @Test
    void testResumeScanDoesNotRedispatchHistoricalVulnerabilities() throws Exception {
        ScanEngine engine = new ScanEngine(new ScanConfig());
        ENGINES.add(engine);

        String scanId = "resume-no-redispatch-" + System.nanoTime();
        PocObj.Poc poc = createPoc("resume-poc", PocObj.Severity.HIGH);
        persistVulnerableTask(scanId, "http://resume.test", poc.getId(), 123456L);

        Field currentScanIdField = ScanEngine.class.getDeclaredField("currentScanId");
        currentScanIdField.setAccessible(true);
        currentScanIdField.set(engine, scanId);

        AtomicInteger vulnEvents = new AtomicInteger(0);
        engine.addEventListener(new ScanEventListener() {
            @Override
            public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
                vulnEvents.incrementAndGet();
            }
        });

        ScanState state = new ScanState();
        state.setScanId(scanId);
        state.setStartTime(System.currentTimeMillis() - 1000);
        state.setThreads(2);
        state.setTimeout(5);
        state.setTargets(Collections.singletonList("http://resume.test"));
        state.setPocIds(Collections.singletonList(poc.getId()));
        state.setTotalTasks(1);
        state.setCompletedTasks(1);

        engine.resumeScan(state, Collections.singletonList(poc));

        assertEquals(0, vulnEvents.get());
        assertEquals(1, engine.getVulnerabilityCount());
        assertEquals(1, engine.getScanResults().size());
        assertFalse(engine.isScanning());
    }

    @Test
    void testApplyScanConfigSnapshotIsIndependentFromLaterMutation() throws Exception {
        VulnScanService service = VulnScanService.getInstance();

        ScanConfig first = new ScanConfig();
        first.setThreads(8);
        first.setTimeout(21);
        first.setDebug(true);
        first.setProxy("http://127.0.0.1:8080");
        first.setEnableClustering(false);
        first.setEnableResponseCache(false);
        first.setRetries(4);
        first.setEnableHeadless(true);
        first.setEnableCode(true);
        first.setEnableFuzz(true);

        service.applyScanConfig(first);
        ScanConfig capturedConfig = service.getScanConfigSnapshot();

        ScanConfig second = new ScanConfig();
        second.setThreads(2);
        second.setTimeout(3);
        second.setDebug(false);
        second.setProxy(null);
        second.setEnableClustering(true);
        second.setEnableResponseCache(true);
        second.setRetries(1);
        second.setEnableHeadless(false);
        second.setEnableCode(false);
        second.setEnableFuzz(false);
        service.applyScanConfig(second);

        assertEquals(8, capturedConfig.getThreads());
        assertEquals(21, capturedConfig.getTimeout());
        assertTrue(capturedConfig.isDebug());
        assertEquals("http://127.0.0.1:8080", capturedConfig.getProxy());
        assertFalse(capturedConfig.isEnableClustering());
        assertFalse(capturedConfig.isEnableResponseCache());
        assertEquals(4, capturedConfig.getRetries());
        assertTrue(capturedConfig.isEnableHeadless());
        assertTrue(capturedConfig.isEnableCode());
        assertTrue(capturedConfig.isEnableFuzz());
    }

    private static void persistVulnerableTask(String scanId, String target, String pocId, long endTime) throws Exception {
        TaskState taskState = new TaskState();
        taskState.setTaskId(TaskState.generateTaskId(target, pocId));
        taskState.setScanId(scanId);
        taskState.setTarget(target);
        taskState.setPocId(pocId);
        taskState.setCompleted(true);
        taskState.setVulnerable(true);
        taskState.setStartTime(endTime - 1);
        taskState.setEndTime(endTime);
        VulnScanDatabase.getInstance().saveTaskState(taskState);
    }

    private static PocObj.Poc createPoc(String id, PocObj.Severity severity) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(severity);
        poc.setVerifySteps(Arrays.asList(new PocObj.PocStep()));
        return poc;
    }
}
