package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.core.I18nTextUtils;
import com.potato.potatotool.utils.ui.ScrollOptimizationConstants;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.fxmisc.richtext.CodeArea;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Potato
 * @date 2023/5/11 09:11
 */

public class CodeAnalyzerUtils {
    private static final int STREAM_CONTINUATION_TAIL_LIMIT = 1200;

    public static void evilCodeAnalysis(String evilCode , String encodeModes, Object node) throws Exception {
        String result = buildEvilCodeAnalysisResult(evilCode, encodeModes);
        setNodeContent(node, result);
    }

    public static boolean streamEvilCodeAnalysis(String evilCode, String encodeModes, Object node) {
        return streamEvilCodeAnalysis(evilCode, encodeModes, new AiChatService(), createStreamAppender(node));
    }

    /**
     * 可取消的流式安全分析入口。
     *
     * @param streamGate 会话有效性判定，返回 false 时不再发起新的请求、不再写入任何内容
     */
    public static boolean streamEvilCodeAnalysis(String evilCode,
                                                 String encodeModes,
                                                 AiChatService aiService,
                                                 StreamAppender appender,
                                                 StreamGate streamGate) {
        if (appender == null) {
            throw new IllegalArgumentException("stream appender is required");
        }
        if (!isGateActive(streamGate)) {
            return false;
        }
        if (aiService != null) {
            // 让请求层在真正建连之前再判定一次，避免“已取消但请求仍发出”的窗口
            aiService.setRequestGate(() -> !isGateActive(streamGate));
        }

        appender.setText(I18nTextUtils.getString("webshell.ai.section.summary") + "\n");
        StreamPhaseResult result = streamPrompt(
                aiService,
                AiPromptUtils.securityAnalysisSystemPrompt(),
                buildUnifiedAnalysisPrompt(evilCode, encodeModes),
                appender,
                streamGate
        );
        if (!isGateActive(streamGate)) {
            return false;
        }
        if (result.hasError()) {
            if (result.hasContent()) {
                appender.append("\n\n");
                appender.append(I18nTextUtils.getString("webshell.ai.stream.error", result.getError()));
            } else {
                appender.setText(result.getError());
            }
            return false;
        }
        if (!result.hasContent()) {
            appender.setText(I18nTextUtils.getString("webshell.ai.empty"));
            return false;
        }
        return true;
    }

    //  优化代码，如反编译后的代码
    public static void optimizedCode(String code, Object node) throws Exception {
        AiChatService aiService = new AiChatService();
        String result = aiService.askNoStream(
                AiPromptUtils.codeOptimizationSystemPrompt(),
                buildOptimizedCodePrompt(code)
        );
        setNodeContent(node, result);
    }

    public static void streamOptimizedCode(String code, Object node) {
        streamOptimizedCode(code, new AiChatService(), createStreamAppender(node));
    }

    static String buildEvilCodeAnalysisResult(String evilCode, String encodeModes) {
        return buildEvilCodeAnalysisResult(evilCode, encodeModes, new AiChatService());
    }

    static String buildEvilCodeAnalysisResult(String evilCode, String encodeModes, AiChatService aiService) {
        return safeText(aiService.askNoStream(
                AiPromptUtils.securityAnalysisSystemPrompt(),
                buildUnifiedAnalysisPrompt(evilCode, encodeModes)
        ));
    }

    public static boolean streamEvilCodeAnalysis(String evilCode,
                                                 String encodeModes,
                                                 AiChatService aiService,
                                                 StreamAppender appender) {
        return streamEvilCodeAnalysis(evilCode, encodeModes, aiService, appender, ALWAYS_ACTIVE);
    }

    static void streamOptimizedCode(String code,
                                    AiChatService aiService,
                                    StreamAppender appender) {
        if (appender == null) {
            throw new IllegalArgumentException("stream appender is required");
        }

        appender.setText("");
        StreamPhaseResult result = streamPrompt(
                aiService,
                AiPromptUtils.codeOptimizationSystemPrompt(),
                buildOptimizedCodePrompt(code),
                appender,
                ALWAYS_ACTIVE
        );
        if (result.hasError()) {
            if (result.hasContent()) {
                appender.append("\n\n");
                appender.append(I18nTextUtils.getString("ai.status.error"));
                appender.append(": ");
                appender.append(result.getError());
            } else {
                appender.setText(result.getError());
            }
            return;
        }
        if (!result.hasContent()) {
            appender.setText(I18nTextUtils.getString("decompile.ai.empty"));
        }
    }

    private static String buildUnifiedAnalysisPrompt(String evilCode, String encodeModes) {
        return AiPromptUtils.buildSecurityAnalysisPrompt(evilCode, encodeModes);
    }

    private static String buildOptimizedCodePrompt(String code) {
        return AiPromptUtils.buildCodeOptimizationPrompt(code);
    }

    private static StreamPhaseResult streamPrompt(AiChatService aiService,
                                                  String systemPrompt,
                                                  String prompt,
                                                  StreamAppender appender,
                                                  StreamGate streamGate) {
        StreamPhaseResult firstAttempt = streamPromptOnce(aiService, systemPrompt, prompt, appender, streamGate);
        if (!shouldContinueFromInterruption(firstAttempt)) {
            return firstAttempt;
        }
        if (!isGateActive(streamGate)) {
            return firstAttempt;
        }

        StreamPhaseResult continuationAttempt = streamPromptOnce(
                aiService,
                systemPrompt,
                buildContinuationPrompt(firstAttempt.getContent()),
                NoOpStreamAppender.INSTANCE,
                streamGate
        );
        if (!isGateActive(streamGate)) {
            return firstAttempt;
        }

        String mergedContinuation = trimRepeatedPrefix(firstAttempt.getContent(), continuationAttempt.getContent());
        if (!mergedContinuation.isEmpty()) {
            appender.append(mergedContinuation);
        }

        String mergedContent = firstAttempt.getContent() + mergedContinuation;
        String finalError = continuationAttempt.hasError() ? continuationAttempt.getError() : "";
        if (mergedContent.isEmpty()) {
            finalError = firstAttempt.getError();
        }
        return new StreamPhaseResult(mergedContent, finalError);
    }

    private static StreamPhaseResult streamPromptOnce(AiChatService aiService,
                                                      String systemPrompt,
                                                      String prompt,
                                                      StreamAppender appender,
                                                      StreamGate streamGate) {
        if (!isGateActive(streamGate)) {
            return new StreamPhaseResult("", "");
        }

        final StringBuilder buffer = new StringBuilder();
        final String[] errorHolder = new String[1];
        aiService.streamChat(systemPrompt, prompt, event ->
                handlePhaseEvent(event, buffer, appender, errorHolder, streamGate));
        return new StreamPhaseResult(buffer.toString(), errorHolder[0]);
    }

    private static void handlePhaseEvent(AiStreamEvent event,
                                         StringBuilder buffer,
                                         StreamAppender appender,
                                         String[] errorHolder,
                                         StreamGate streamGate) {
        if (event == null || !isGateActive(streamGate)) {
            return;
        }
        switch (event.getType()) {
            case TOKEN:
                String content = event.getContent();
                if (content != null && !content.isEmpty()) {
                    buffer.append(content);
                    appender.append(content);
                }
                break;
            case ERROR:
                errorHolder[0] = safeText(event.getContent());
                break;
            case THINKING_TOKEN:
            case DONE:
            default:
                break;
        }
    }

    private static String safeText(String content) {
        return content == null ? "" : content.trim();
    }

    private static boolean shouldContinueFromInterruption(StreamPhaseResult result) {
        if (result == null || !result.hasContent() || !result.hasError()) {
            return false;
        }
        String error = result.getError().toLowerCase();
        return error.contains("超时")
                || error.contains("中断")
                || error.contains("重置")
                || error.contains("502")
                || error.contains("eof")
                || error.contains("timeout")
                || error.contains("interrupted")
                || error.contains("reset")
                || error.contains("stream");
    }

    private static String buildContinuationPrompt(String content) {
        return I18nTextUtils.getString(
                "ai.prompt.stream.continuation",
                extractTail(content, STREAM_CONTINUATION_TAIL_LIMIT)
        );
    }

    private static String extractTail(String content, int maxChars) {
        String text = safeText(content);
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(text.length() - maxChars);
    }

    private static String trimRepeatedPrefix(String existingContent, String continuationContent) {
        String existing = existingContent == null ? "" : existingContent;
        String continuation = continuationContent == null ? "" : continuationContent;
        if (existing.isEmpty() || continuation.isEmpty()) {
            return continuation;
        }

        int maxOverlap = Math.min(Math.min(existing.length(), continuation.length()), 200);
        for (int overlap = maxOverlap; overlap >= 20; overlap--) {
            if (existing.regionMatches(existing.length() - overlap, continuation, 0, overlap)) {
                return continuation.substring(overlap);
            }
        }
        return continuation;
    }

    private static boolean isLikelyAiError(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        String text = content.trim();
        return text.startsWith("AI ")
                || text.startsWith("AI请求")
                || text.startsWith("AI配置")
                || text.startsWith("AI连接")
                || text.startsWith("AI request")
                || text.startsWith("AI configuration")
                || text.startsWith("AI connection");
    }

    private static void setNodeContent(Object node, String content) {
        if (content == null) {
            content = "";
        }
        String finalContent = content;
        if (node instanceof TextArea) {
            TextArea textArea = (TextArea) node;
            Platform.runLater(() -> textArea.setText(finalContent));
        } else if (node instanceof CodeArea) {
            CodeArea codeArea = (CodeArea) node;
            Platform.runLater(() -> codeArea.replaceText(finalContent));
        }
    }

    /**
     * 创建默认的流式接收器（不校验会话有效性）。
     */
    public static StreamAppender createStreamAppender(Object node) {
        return createStreamAppender(node, ALWAYS_ACTIVE);
    }

    /**
     * 创建带会话有效性校验的流式接收器。校验在写入线程与 FX 线程 flush 时都会执行，
     * 因此会话被取消或取代后，已经排队等待写入的残留分片也会被丢弃。
     */
    public static StreamAppender createStreamAppender(Object node, StreamGate streamGate) {
        if (node instanceof TextArea) {
            return new FxBufferedTextAreaAppender((TextArea) node, streamGate);
        }
        if (node instanceof CodeArea) {
            return new FxBufferedCodeAreaAppender((CodeArea) node, streamGate);
        }
        throw new IllegalArgumentException("Unsupported stream node: " + node);
    }

    /**
     * 流式写入接收器。调用方可以实现自己的接收器，用于在写入前做代次/取消校验。
     */
    public interface StreamAppender {
        void setText(String text);

        void append(String text);

        /**
         * 释放该接收器在目标节点上安装的事件监听。默认无操作，必须在 FX 线程调用。
         */
        default void dispose() {
        }
    }

    /**
     * 流式会话的有效性判定。返回 false 表示该会话已取消或已被新会话取代。
     */
    public interface StreamGate {
        boolean isActive();
    }

    private static final StreamGate ALWAYS_ACTIVE = new StreamGate() {
        @Override
        public boolean isActive() {
            return true;
        }
    };

    private static boolean isGateActive(StreamGate streamGate) {
        return streamGate == null || streamGate.isActive();
    }

    private static final class StreamPhaseResult {
        private final String content;
        private final String error;

        private StreamPhaseResult(String content, String error) {
            this.content = safeText(content);
            this.error = safeText(error);
        }

        private boolean hasContent() {
            return !content.isEmpty();
        }

        private boolean hasError() {
            return !error.isEmpty();
        }

        private String getError() {
            return error;
        }

        private String getContent() {
            return content;
        }
    }

    private enum NoOpStreamAppender implements StreamAppender {
        INSTANCE;

        @Override
        public void setText(String text) {
        }

        @Override
        public void append(String text) {
        }
    }

    private abstract static class FxBufferedAppenderSupport {
        private final StringBuilder pending = new StringBuilder();
        private final AtomicBoolean flushQueued = new AtomicBoolean(false);
        private final StreamGate streamGate;

        FxBufferedAppenderSupport(StreamGate streamGate) {
            this.streamGate = streamGate;
        }

        boolean isStreamActive() {
            return isGateActive(streamGate);
        }

        public void setText(String text) {
            if (!isStreamActive()) {
                return;
            }
            final String value = text == null ? "" : text;
            synchronized (pending) {
                pending.setLength(0);
            }
            Platform.runLater(() -> {
                if (!isStreamActive()) {
                    return;
                }
                applyText(value);
            });
        }

        public void append(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (!isStreamActive()) {
                return;
            }
            synchronized (pending) {
                pending.append(text);
            }
            scheduleFlush();
        }

        private void scheduleFlush() {
            if (!flushQueued.compareAndSet(false, true)) {
                return;
            }
            Platform.runLater(() -> {
                String chunk;
                synchronized (pending) {
                    chunk = pending.toString();
                    pending.setLength(0);
                }
                try {
                    if (!chunk.isEmpty() && isStreamActive()) {
                        appendText(chunk);
                    }
                } finally {
                    flushQueued.set(false);
                    synchronized (pending) {
                        if (pending.length() > 0) {
                            scheduleFlush();
                        }
                    }
                }
            });
        }

        protected abstract void applyText(String text);

        protected abstract void appendText(String text);
    }

    private static final class FxBufferedTextAreaAppender extends FxBufferedAppenderSupport implements StreamAppender {
        private final TextArea textArea;
        private boolean followOutput = true;
        private boolean programmaticScroll;
        private boolean scrollbarListenerInstalled;
        private volatile boolean disposed;
        private ScrollPane internalScrollPane;
        private ScrollPane trackedScrollPane;
        private ScrollBar verticalScrollBar;
        private double preservedVvalue;
        private double preservedHvalue;
        private double preservedScrollTop;
        private double preservedScrollLeft;
        private double preservedVerticalScrollValue;
        private boolean preservedVerticalScrollBar;
        private boolean detachedAppendRestoreGuard;
        private final EventHandler<ScrollEvent> userScrollFilter = this::handleUserScroll;
        private final EventHandler<MouseEvent> followRefreshFilter =
                event -> refreshFollowOutputFromCurrentPosition(true);
        private final ChangeListener<Skin<?>> skinListener = (observable, oldValue, newValue) -> {
            internalScrollPane = null;
            verticalScrollBar = null;
            Platform.runLater(this::installScrollTracking);
        };
        private final ChangeListener<Number> scrollPaneVvalueListener =
                (observable, oldValue, newValue) -> refreshFollowOutputFromCurrentPosition();
        private final ChangeListener<Number> scrollPaneHvalueListener = (observable, oldValue, newValue) -> {
            if (!programmaticScroll && !followOutput && newValue != null) {
                preservedHvalue = newValue.doubleValue();
            }
        };
        private final ChangeListener<Number> scrollBarValueListener =
                (observable, oldValue, newValue) -> refreshFollowOutputFromCurrentPosition();
        private final EventHandler<MouseEvent> scrollBarPressFilter = event -> detachFromAutoFollow();
        private final EventHandler<MouseEvent> scrollBarDragFilter =
                event -> Platform.runLater(() -> refreshFollowOutputFromCurrentPosition(true));
        private final EventHandler<MouseEvent> scrollBarReleaseFilter =
                event -> Platform.runLater(() -> refreshFollowOutputFromCurrentPosition(true));
        private int detachedRestoreGeneration;

        private FxBufferedTextAreaAppender(TextArea textArea, StreamGate streamGate) {
            super(streamGate);
            this.textArea = textArea;
            Platform.runLater(this::installScrollTracking);
        }

        @Override
        public void dispose() {
            disposed = true;
            textArea.removeEventFilter(ScrollEvent.SCROLL, userScrollFilter);
            textArea.removeEventFilter(MouseEvent.MOUSE_RELEASED, followRefreshFilter);
            textArea.skinProperty().removeListener(skinListener);
            if (trackedScrollPane != null) {
                trackedScrollPane.vvalueProperty().removeListener(scrollPaneVvalueListener);
                trackedScrollPane.hvalueProperty().removeListener(scrollPaneHvalueListener);
                trackedScrollPane = null;
            }
            if (verticalScrollBar != null) {
                verticalScrollBar.valueProperty().removeListener(scrollBarValueListener);
                verticalScrollBar.removeEventFilter(MouseEvent.MOUSE_PRESSED, scrollBarPressFilter);
                verticalScrollBar.removeEventFilter(MouseEvent.MOUSE_DRAGGED, scrollBarDragFilter);
                verticalScrollBar.removeEventFilter(MouseEvent.MOUSE_RELEASED, scrollBarReleaseFilter);
                verticalScrollBar = null;
            }
            internalScrollPane = null;
        }
        @Override
        protected void applyText(String text) {
            followOutput = true;
            programmaticScroll = true;
            textArea.setText(text);
            scrollToBottom();
            Platform.runLater(() -> {
                scrollToBottom();
                programmaticScroll = false;
            });
        }

        @Override
        protected void appendText(String text) {
            installScrollTracking();
            boolean shouldFollowOutput = followOutput;
            int preservedAnchor = textArea.getAnchor();
            int preservedCaret = textArea.getCaretPosition();
            if (!shouldFollowOutput && preservedAnchor == preservedCaret) {
                preservedAnchor = 0;
                preservedCaret = 0;
            }
            final int restoreAnchor = preservedAnchor;
            final int restoreCaret = preservedCaret;
            programmaticScroll = true;
            textArea.appendText(text);
            if (shouldFollowOutput) {
                scrollToBottom();
            } else {
                restoreSelection(restoreAnchor, restoreCaret);
                restorePreservedScroll();
            }
            Platform.runLater(() -> {
                if (shouldFollowOutput && followOutput) {
                    scrollToBottom();
                    programmaticScroll = false;
                } else if (!followOutput) {
                    restoreSelection(restoreAnchor, restoreCaret);
                    restorePreservedScrollUntilStable(8);
                } else {
                    programmaticScroll = false;
                }
            });
        }

        private void installScrollTracking() {
            if (textArea == null || disposed) {
                return;
            }
            if (!scrollbarListenerInstalled) {
                scrollbarListenerInstalled = true;
                textArea.addEventFilter(ScrollEvent.SCROLL, userScrollFilter);
                textArea.addEventFilter(MouseEvent.MOUSE_RELEASED, followRefreshFilter);
                textArea.skinProperty().addListener(skinListener);
            }
            resolveInternalScrollPane();
            resolveVerticalScrollBar();
        }

        private void handleUserScroll(ScrollEvent event) {
            if (event == null) {
                return;
            }
            detachFromAutoFollow(event);
            scheduleRefreshFollowOutputFromCurrentPosition(4, true);
        }

        private void scheduleRefreshFollowOutputFromCurrentPosition(int remainingPulses, boolean allowReattach) {
            if (disposed) {
                return;
            }
            Platform.runLater(() -> {
                if (disposed) {
                    return;
                }
                refreshFollowOutputFromCurrentPosition(allowReattach);
                if (remainingPulses > 0) {
                    scheduleRefreshFollowOutputFromCurrentPosition(remainingPulses - 1, allowReattach);
                }
            });
        }

        private void refreshFollowOutputFromCurrentPosition() {
            refreshFollowOutputFromCurrentPosition(false);
        }

        private void refreshFollowOutputFromCurrentPosition(boolean allowReattach) {
            if (programmaticScroll) {
                return;
            }
            if (isAtBottom()) {
                if (allowReattach || followOutput) {
                    followOutput = true;
                    detachedAppendRestoreGuard = false;
                    detachedRestoreGeneration++;
                } else if (detachedAppendRestoreGuard) {
                    programmaticScroll = true;
                    restorePreservedScrollUntilStable(4);
                }
            } else {
                if (followOutput && !allowReattach) {
                    return;
                }
                followOutput = false;
                if (allowReattach) {
                    detachedAppendRestoreGuard = false;
                    detachedRestoreGeneration++;
                }
                if (allowReattach || !detachedAppendRestoreGuard) {
                    preserveCurrentScroll();
                }
            }
        }

        private void preserveCurrentScroll() {
            ScrollPane scrollPane = resolveInternalScrollPane();
            if (scrollPane != null) {
                preservedVvalue = scrollPane.getVvalue();
                preservedHvalue = scrollPane.getHvalue();
                preservedScrollTop = textArea.getScrollTop();
                preservedScrollLeft = textArea.getScrollLeft();
                ScrollBar scrollBar = resolveVerticalScrollBar();
                preservedVerticalScrollBar = scrollBar != null;
                if (scrollBar != null) {
                    preservedVerticalScrollValue = scrollBar.getValue();
                }
                return;
            }
            preservedVvalue = 1.0;
            preservedHvalue = 0.0;
            preservedScrollTop = textArea.getScrollTop();
            preservedScrollLeft = textArea.getScrollLeft();
            preservedVerticalScrollValue = 0.0;
            preservedVerticalScrollBar = false;
        }

        private void restorePreservedScroll() {
            ScrollPane scrollPane = resolveInternalScrollPane();
            if (scrollPane != null) {
                scrollPane.setVvalue(preservedVvalue);
                scrollPane.setHvalue(preservedHvalue);
                ScrollBar scrollBar = resolveVerticalScrollBar();
                if (preservedVerticalScrollBar && scrollBar != null) {
                    scrollBar.setValue(clamp(
                            preservedVerticalScrollValue,
                            scrollBar.getMin(),
                            scrollBar.getMax()
                    ));
                }
                textArea.setScrollTop(preservedScrollTop);
                textArea.setScrollLeft(preservedScrollLeft);
                return;
            }
            textArea.setScrollTop(preservedScrollTop);
            textArea.setScrollLeft(preservedScrollLeft);
        }

        private void restoreSelection(int anchor, int caret) {
            int length = textArea.getLength();
            int safeAnchor = (int) clamp(anchor, 0, length);
            int safeCaret = (int) clamp(caret, 0, length);
            textArea.selectRange(safeAnchor, safeCaret);
        }

        private void restorePreservedScrollUntilStable(int remainingPulses) {
            int generation = ++detachedRestoreGeneration;
            restorePreservedScrollUntilStable(remainingPulses, generation);
        }

        private void restorePreservedScrollUntilStable(int remainingPulses, int generation) {
            if (disposed) {
                return;
            }
            detachedAppendRestoreGuard = true;
            if (!followOutput) {
                // Keep the caret away from the newly appended tail so TextAreaSkin does not force it into view.
                int caret = textArea.getCaretPosition();
                int anchor = textArea.getAnchor();
                restoreSelection(anchor, caret);
            }
            restorePreservedScroll();
            if (remainingPulses <= 0) {
                programmaticScroll = false;
                scheduleClearDetachedRestoreGuard(8, generation);
                return;
            }
            Platform.runLater(() -> restorePreservedScrollUntilStable(remainingPulses - 1, generation));
        }

        private void scheduleClearDetachedRestoreGuard(int remainingPulses, int generation) {
            Platform.runLater(() -> {
                if (generation != detachedRestoreGeneration) {
                    return;
                }
                if (remainingPulses <= 0) {
                    detachedAppendRestoreGuard = false;
                    return;
                }
                scheduleClearDetachedRestoreGuard(remainingPulses - 1, generation);
            });
        }

        private void scrollToBottom() {
            ScrollPane scrollPane = resolveInternalScrollPane();
            if (scrollPane != null) {
                scrollPane.setHvalue(0.0);
                scrollPane.setVvalue(1.0);
                ScrollBar scrollBar = resolveVerticalScrollBar();
                if (scrollBar != null) {
                    scrollBar.setValue(scrollBar.getMax());
                }
                return;
            }
            textArea.setScrollLeft(0);
            textArea.setScrollTop(Double.MAX_VALUE);
        }

        private boolean isAtBottom() {
            ScrollPane scrollPane = resolveInternalScrollPane();
            if (scrollPane != null && !scrollPane.isVisible()) {
                return true;
            }
            ScrollBar scrollBar = resolveVerticalScrollBar();
            if (scrollBar != null && scrollBar.isVisible()) {
                double range = Math.max(0, scrollBar.getMax() - scrollBar.getMin());
                double threshold = range <= 1.0 ? 0.02 : Math.max(2.0, range * 0.02);
                return scrollBar.getValue() >= scrollBar.getMax() - threshold;
            }
            return isScrollPaneAtBottom(scrollPane);
        }

        private boolean isScrollPaneAtBottom(ScrollPane scrollPane) {
            if (scrollPane == null || scrollPane.getContent() == null) {
                return true;
            }
            double scrollableHeight = getScrollableHeight(scrollPane);
            if (scrollableHeight <= 1.0) {
                return true;
            }
            double range = Math.max(0, scrollPane.getVmax() - scrollPane.getVmin());
            double threshold = range <= 1.0 ? 0.02 : Math.max(2.0, range * 0.02);
            return scrollPane.getVvalue() >= scrollPane.getVmax() - threshold;
        }

        private ScrollPane resolveInternalScrollPane() {
            if (disposed) {
                return null;
            }
            if (internalScrollPane != null) {
                return internalScrollPane;
            }
            Node node = textArea.lookup(".scroll-pane");
            if (!(node instanceof ScrollPane)) {
                return null;
            }
            internalScrollPane = (ScrollPane) node;
            installScrollPanePositionTracking(internalScrollPane);
            return internalScrollPane;
        }

        private void installScrollPanePositionTracking(ScrollPane scrollPane) {
            if (disposed || scrollPane == null || scrollPane == trackedScrollPane) {
                return;
            }
            trackedScrollPane = scrollPane;
            scrollPane.vvalueProperty().addListener(scrollPaneVvalueListener);
            scrollPane.hvalueProperty().addListener(scrollPaneHvalueListener);
        }

        private ScrollBar resolveVerticalScrollBar() {
            if (disposed) {
                return null;
            }
            if (verticalScrollBar != null) {
                return verticalScrollBar;
            }
            Node node = textArea.lookup(".scroll-bar:vertical");
            if (!(node instanceof ScrollBar)) {
                return null;
            }
            verticalScrollBar = (ScrollBar) node;
            verticalScrollBar.valueProperty().addListener(scrollBarValueListener);
            verticalScrollBar.addEventFilter(MouseEvent.MOUSE_PRESSED, scrollBarPressFilter);
            verticalScrollBar.addEventFilter(MouseEvent.MOUSE_DRAGGED, scrollBarDragFilter);
            verticalScrollBar.addEventFilter(MouseEvent.MOUSE_RELEASED, scrollBarReleaseFilter);
            return verticalScrollBar;
        }

        private void detachFromAutoFollow() {
            if (disposed) {
                return;
            }
            followOutput = false;
            detachedAppendRestoreGuard = false;
            detachedRestoreGeneration++;
            Platform.runLater(this::preserveCurrentScroll);
        }

        private void detachFromAutoFollow(ScrollEvent event) {
            if (disposed) {
                return;
            }
            followOutput = false;
            detachedAppendRestoreGuard = false;
            detachedRestoreGeneration++;
            preserveCurrentScroll(event);
        }

        private void preserveCurrentScroll(ScrollEvent event) {
            ScrollPane scrollPane = resolveInternalScrollPane();
            if (scrollPane == null) {
                preserveCurrentScroll();
                return;
            }
            preservedVvalue = estimateVvalueAfterScroll(scrollPane, event);
            preservedHvalue = estimateHvalueAfterScroll(scrollPane, event);
            preservedScrollTop = estimateScrollTopAfterScroll(event);
            preservedScrollLeft = textArea.getScrollLeft();
            ScrollBar scrollBar = resolveVerticalScrollBar();
            preservedVerticalScrollBar = scrollBar != null;
            if (scrollBar != null) {
                preservedVerticalScrollValue = estimateVerticalScrollValueAfterScroll(scrollBar, event);
            }
        }

        private double estimateVvalueAfterScroll(ScrollPane scrollPane, ScrollEvent event) {
            if (event == null || scrollPane.getContent() == null) {
                return scrollPane.getVvalue();
            }
            double scrollableHeight = getScrollableHeight(scrollPane);
            if (scrollableHeight <= 0 || event.getDeltaY() == 0) {
                return scrollPane.getVvalue();
            }
            double normalizedDelta = -event.getDeltaY() / scrollableHeight;
            double estimated = scrollPane.getVvalue() + normalizedDelta * ScrollOptimizationConstants.DEFAULT_SENSITIVITY;
            return clamp(estimated, 0.0, 1.0);
        }

        private double estimateHvalueAfterScroll(ScrollPane scrollPane, ScrollEvent event) {
            if (event == null || scrollPane.getContent() == null) {
                return scrollPane.getHvalue();
            }
            double scrollableWidth = getScrollableWidth(scrollPane);
            if (scrollableWidth <= 0 || event.getDeltaX() == 0) {
                return scrollPane.getHvalue();
            }
            double normalizedDelta = -event.getDeltaX() / scrollableWidth;
            double estimated = scrollPane.getHvalue() + normalizedDelta * ScrollOptimizationConstants.DEFAULT_SENSITIVITY;
            return clamp(estimated, 0.0, 1.0);
        }

        private double estimateVerticalScrollValueAfterScroll(ScrollBar scrollBar, ScrollEvent event) {
            if (event == null || event.getDeltaY() == 0) {
                return scrollBar.getValue();
            }
            double estimated = scrollBar.getValue()
                    + (-event.getDeltaY() * ScrollOptimizationConstants.DEFAULT_SENSITIVITY);
            return clamp(estimated, scrollBar.getMin(), scrollBar.getMax());
        }

        private double estimateScrollTopAfterScroll(ScrollEvent event) {
            if (event == null || event.getDeltaY() == 0) {
                return textArea.getScrollTop();
            }
            double estimated = textArea.getScrollTop()
                    + (-event.getDeltaY() * ScrollOptimizationConstants.DEFAULT_SENSITIVITY);
            return Math.max(0.0, estimated);
        }

        private double getScrollableHeight(ScrollPane scrollPane) {
            Bounds contentBounds = scrollPane.getContent().getBoundsInLocal();
            Bounds viewportBounds = scrollPane.getViewportBounds();
            return contentBounds.getHeight() - viewportBounds.getHeight();
        }

        private double getScrollableWidth(ScrollPane scrollPane) {
            Bounds contentBounds = scrollPane.getContent().getBoundsInLocal();
            Bounds viewportBounds = scrollPane.getViewportBounds();
            return contentBounds.getWidth() - viewportBounds.getWidth();
        }

        private double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class FxBufferedCodeAreaAppender extends FxBufferedAppenderSupport implements StreamAppender {
        private final CodeArea codeArea;

        private FxBufferedCodeAreaAppender(CodeArea codeArea, StreamGate streamGate) {
            super(streamGate);
            this.codeArea = codeArea;
        }

        @Override
        protected void applyText(String text) {
            codeArea.replaceText(text);
        }

        @Override
        protected void appendText(String text) {
            codeArea.appendText(text);
        }
    }
}
