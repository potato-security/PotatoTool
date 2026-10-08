package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.CodeAnalyzerUtils;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("一键解密 AI 流式会话测试")
class PaneWebshellDecodeAiStreamTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @Test
    @DisplayName("会话失效后，已排队等待写入的残留分片应被丢弃")
    void shouldDropQueuedChunkAfterGateClosed() throws Exception {
        ensureFxStarted();

        final MutableGate gate = new MutableGate();
        final TextArea textArea = onFxThread(() -> new TextArea());
        final CodeAnalyzerUtils.StreamAppender appender =
                onFxThread(() -> CodeAnalyzerUtils.createStreamAppender(textArea, gate));

        onFxThread(() -> {
            appender.setText("会话头");
            return null;
        });
        drainFxQueue();
        assertEquals("会话头", onFxThread(textArea::getText));

        // append 之后立刻关掉会话门，flush 尚未执行，验证守卫是在 FX 队列执行时判定
        onFxThread(() -> {
            appender.append("旧会话残留分片");
            gate.close();
            return null;
        });
        drainFxQueue();

        assertEquals("会话头", onFxThread(textArea::getText), "会话失效后不应再写入残留分片");
    }

    @Test
    @DisplayName("pane_webshellDecode.fxml 应完成 AI 状态条与文本区装配")
    void shouldWireAiStatusStrip() throws Exception {
        ensureFxStarted();

        final Parent root = onFxThread(this::loadPaneWithoutHeavyInitialize);
        Node statusLabel = onFxThread(() -> root.lookup("#aiStatusLabel"));
        Node aiTextArea = onFxThread(() -> root.lookup("#aiTextArea"));

        assertNotNull(statusLabel, "AI 状态条未装配");
        assertNotNull(aiTextArea, "AI 文本区未装配");
        assertTrue(statusLabel instanceof Label, "AI 状态条类型应为 Label");
        assertTrue(statusLabel.isMouseTransparent(), "AI 状态条不应吞掉文本区的拖拽事件");
        assertTrue(statusLabel.getStyleClass().contains("ai-status-strip"), "AI 状态条缺少样式类");
        // 状态条与下载/刷新/关闭三个按钮同排，文本区保持在面板原位
        assertEquals(13.0, statusLabel.getLayoutY(), 0.001, "AI 状态条应与面板按钮同排");
        assertEquals(0.0, aiTextArea.getLayoutY(), 0.001, "AI 文本区不应被状态条顶下去");

        // 状态条压在文本区顶栏留白之上：必须比文本区后声明，否则会被不透明背景整块遮住
        boolean paintedAboveTextArea = onFxThread(() -> {
            Pane aiPane = (Pane) root.lookup("#aiPane");
            assertNotNull(aiPane, "未找到 AI 面板容器");
            return aiPane.getChildren().indexOf(statusLabel) > aiPane.getChildren().indexOf(aiTextArea);
        });
        assertTrue(paintedAboveTextArea, "AI 状态条被文本区遮挡，应声明在文本区之后");
    }

    @Test
    @DisplayName("AI 面板正文应让出顶栏空间，不遮挡状态条")
    void shouldReserveHeaderRowInsideAiPanel() throws Exception {
        ensureFxStarted();

        final Parent root = onFxThread(this::loadPaneWithoutHeavyInitialize);
        final TextArea aiTextArea = (TextArea) onFxThread(() -> root.lookup("#aiTextArea"));
        assertNotNull(aiTextArea, "AI 文本区未装配");

        double inset = onFxThread(() -> {
            aiTextArea.setText("第一行");
            root.applyCss();
            root.layout();
            Node textNode = aiTextArea.lookup(".text");
            assertNotNull(textNode, "未找到 AI 面板正文节点");
            return textNode.localToScene(0, 0).getY() - aiTextArea.localToScene(0, 0).getY();
        });

        double headerBottom = onFxThread(() -> {
            Node closeButton = root.lookup("#closeAI");
            assertNotNull(closeButton, "未找到关闭按钮");
            return closeButton.getLayoutY() + closeButton.getBoundsInParent().getHeight();
        });

        assertTrue(inset >= headerBottom,
                "AI 面板正文顶距=" + inset + " 小于顶栏底部=" + headerBottom + "，正文会与按钮重叠");

        // 顶栏（状态 + 工具按钮）与输出区之间需要一条分割线：在按钮之下、正文之上
        double dividerY = onFxThread(() -> {
            Node divider = root.lookup("#aiStatusDivider");
            assertNotNull(divider, "未找到顶栏分割线");
            return divider.getLayoutY() + divider.getBoundsInParent().getHeight();
        });
        assertTrue(dividerY > headerBottom, "分割线=" + dividerY + " 未落在顶栏之下（顶栏底部=" + headerBottom + "）");
        assertTrue(dividerY <= inset, "分割线=" + dividerY + " 压到了正文（正文顶距=" + inset + "）");
    }

    @Test
    @DisplayName("解密失败后不应残留上一份 AI 分析")
    void shouldDiscardPreviousAnalysisWhenDecryptFails() throws Exception {
        ensureFxStarted();

        final Parent root = onFxThread(this::loadPaneWithRealController);
        final PaneWebshellDecode pane = (PaneWebshellDecode) onFxThread(() -> root.getProperties().get("controller"));
        final TextArea aiTextArea = (TextArea) onFxThread(() -> root.lookup("#aiTextArea"));
        final TextArea inputText = (TextArea) onFxThread(() -> root.lookup("#inputText"));
        assertNotNull(pane, "控制器未取得");
        assertNotNull(aiTextArea, "AI 文本区未装配");
        assertNotNull(inputText, "输入框未装配");

        onFxThread(() -> {
            aiTextArea.setText("上一份分析结果");
            inputText.setText("vpIBjT3RT8mf6pBjiKJuqA==asdasd");
            return null;
        });
        onFxThread(() -> {
            pane.toDecode(null);
            return null;
        });

        boolean cleared = false;
        long deadline = System.currentTimeMillis() + 60000L;
        while (System.currentTimeMillis() < deadline) {
            if (onFxThread(aiTextArea::getText).isEmpty()) {
                cleared = true;
                break;
            }
            Thread.sleep(100L);
        }
        final String remaining = onFxThread(aiTextArea::getText);
        assertTrue(cleared, "解密失败后仍残留上一份 AI 分析: " + remaining);
        // 解密期间的置灰必须恢复，否则面板会一直发暗
        assertEquals(1.0, onFxThread(aiTextArea::getOpacity), 0.001, "解密结束后 AI 面板未恢复亮度");
    }

    private Parent loadPaneWithRealController() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/blueTeam/pane_webshellDecode.fxml"));
        Parent root = loader.load();
        root.getProperties().put("controller", loader.getController());
        JFXPanel panel = new JFXPanel();
        panel.setScene(new Scene(root, 1280, 900));
        root.applyCss();
        root.layout();
        return root;
    }

    private Parent loadPaneWithoutHeavyInitialize() throws Exception {
        String fxml = readClasspath("/fxml/blueTeam/pane_webshellDecode.fxml");
        String withoutController = fxml.replace(
                "fx:controller=\"com.potato.potatotool.controller.blueTeam.PaneWebshellDecode\"",
                "");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/blueTeam/pane_webshellDecode.fxml"));
        loader.setController(new NoopInitializePane());
        try (ByteArrayInputStream inputStream =
                     new ByteArrayInputStream(withoutController.getBytes(StandardCharsets.UTF_8))) {
            Parent root = loader.load(inputStream);
            JFXPanel panel = new JFXPanel();
            panel.setScene(new Scene(root));
            root.applyCss();
            root.layout();
            return root;
        }
    }

    private String readClasspath(String path) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertTrue(inputStream != null, "资源不存在: " + path);
            byte[] buffer = new byte[4096];
            StringBuilder builder = new StringBuilder();
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            return builder.toString();
        }
    }

    private void ensureFxStarted() throws Exception {
        if (FX_STARTED.compareAndSet(false, true)) {
            final CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                new JFXPanel();
                latch.countDown();
            }, "webshell-decode-fx-bootstrap").start();
            assertTrue(latch.await(15, TimeUnit.SECONDS), "JavaFX Toolkit 初始化超时");
        }
    }

    private void drainFxQueue() throws Exception {
        onFxThread(() -> null);
        Thread.sleep(80);
        onFxThread(() -> null);
    }

    private <T> T onFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> resultRef = new AtomicReference<T>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                resultRef.set(action.call());
            } catch (Throwable throwable) {
                errorRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX 线程任务超时");
        if (errorRef.get() != null) {
            throw new AssertionError("FX 线程任务执行失败", errorRef.get());
        }
        return resultRef.get();
    }

    public static class NoopInitializePane extends PaneWebshellDecode {
        @FXML
        void initialize() {
            // 离屏装配只验证 FXML 结构与绑定，不触发测试数据加载与国际化绑定。
        }
    }

    private static class MutableGate implements CodeAnalyzerUtils.StreamGate {
        private volatile boolean active = true;

        @Override
        public boolean isActive() {
            return active;
        }

        private void close() {
            active = false;
        }
    }
}
