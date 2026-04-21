package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.InfoGatheringStageMetric;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InfoGatheringStageRecorderTest {

    @Test
    public void recorder_shouldKeepStageOrderAndMetadata() {
        InfoGatheringStageRecorder recorder = new InfoGatheringStageRecorder();

        InfoGatheringStageRecorder.StageToken token = recorder.start("平台检索原始域名");
        recorder.finish(token, 3, "新增资产：12");
        recorder.addMetric("总流程", 1234L, 18, "最终资产数：18");

        List<InfoGatheringStageMetric> metrics = recorder.snapshot();
        assertEquals(2, metrics.size());
        assertEquals("平台检索原始域名", metrics.get(0).getStageName());
        assertEquals(Integer.valueOf(3), metrics.get(0).getItemCount());
        assertEquals("新增资产：12", metrics.get(0).getSummary());
        assertTrue(metrics.get(0).getDurationMs() >= 0L);

        assertEquals("总流程", metrics.get(1).getStageName());
        assertEquals(1234L, metrics.get(1).getDurationMs());
        assertEquals(Integer.valueOf(18), metrics.get(1).getItemCount());
        assertEquals("最终资产数：18", metrics.get(1).getSummary());
    }

    @Test
    public void recorder_snapshotShouldBeIndependentFromInternalState() {
        InfoGatheringStageRecorder recorder = new InfoGatheringStageRecorder();
        recorder.addMetric("第一波数据去重", 10L, 5, "去重前：8，去重后：5");

        List<InfoGatheringStageMetric> firstSnapshot = recorder.snapshot();
        firstSnapshot.clear();

        assertFalse(recorder.isEmpty());
        assertEquals(1, recorder.snapshot().size());
    }
}
