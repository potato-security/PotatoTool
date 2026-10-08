package com.potato.potatotool.controller.publicPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("弹窗取景框层级测试")
class DialogFrameLayeringTest {

    private static final String[][] DIALOGS = {
            {"/fxml/publicPane/setting.fxml", "fx:id=\"promptPane\""},
            {"/fxml/publicPane/update_dialog.fxml", "fx:id=\"promptPane\""},
            {"/fxml/publicPane/addBarDialog.fxml", "fx:id=\"addBtn\""},
            {"/fxml/publicPane/deleteConfirmDialog.fxml", "fx:id=\"deleteBtn\""}
    };

    @Test
    @DisplayName("四个 L 形角标应绘制在弹窗内容之上")
    void shouldPaintFrameCornersAboveDialogContent() throws Exception {
        for (String[] dialog : DIALOGS) {
            String fxml = readClasspath(dialog[0]);
            int contentEnd = fxml.lastIndexOf(dialog[1]);
            int lastCorner = fxml.lastIndexOf("pt-corner-");

            assertTrue(contentEnd >= 0, "未找到弹窗内容标记: " + dialog[0]);
            assertTrue(lastCorner > contentEnd,
                    "L 形角标必须置于内容之后以避免被覆盖: " + dialog[0]);
        }
    }

    private String readClasspath(String path) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertTrue(inputStream != null, "资源不存在: " + path);
            byte[] bytes = new byte[4096];
            StringBuilder content = new StringBuilder();
            int count;
            while ((count = inputStream.read(bytes)) != -1) {
                content.append(new String(bytes, 0, count, StandardCharsets.UTF_8));
            }
            return content.toString();
        }
    }
}
