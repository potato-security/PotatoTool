package com.potato.potatotool.controller.blueTeam;

import javafx.scene.text.Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneAiAnswer 文本测量辅助测试")
class PaneAiAnswerTextMetricsSupportTest {

    @Test
    @DisplayName("会统一不同平台的换行符")
    void normalizeLineSeparatorsUnifiesDifferentNewlines() {
        assertEquals(
                "line1\nline2\nline3",
                PaneAiAnswerTextMetricsSupport.normalizeLineSeparators("line1\r\nline2\rline3")
        );
        assertEquals(
                "\n\n",
                PaneAiAnswerTextMetricsSupport.normalizeLineSeparators("\r\n\r")
        );
    }

    @Test
    @DisplayName("CRLF 高度会和 LF 保持一致并保留尾部空行")
    void computeTextHeightNormalizesCrLfWithoutDroppingTrailingLines() {
        Font font = Font.font(14);
        double lfHeight = PaneAiAnswerTextMetricsSupport.computeTextHeight(font, "line1\nline2\n", 80);
        double crlfHeight = PaneAiAnswerTextMetricsSupport.computeTextHeight(font, "line1\r\nline2\r\n", 80);
        double withoutTrailingBlankLine = PaneAiAnswerTextMetricsSupport.computeTextHeight(font, "line1\nline2", 80);

        assertEquals(lfHeight, crlfHeight, 0.0001);
        assertTrue(lfHeight > withoutTrailingBlankLine);
    }

    @Test
    @DisplayName("高度会被限制在最小值和最大值之间")
    void clampHeightRespectsBounds() {
        assertEquals(44, PaneAiAnswerTextMetricsSupport.clampHeight(18, 44, 220));
        assertEquals(96, PaneAiAnswerTextMetricsSupport.clampHeight(96, 44, 220));
        assertEquals(220, PaneAiAnswerTextMetricsSupport.clampHeight(260, 44, 220));
    }
}
