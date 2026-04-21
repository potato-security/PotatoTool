package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.core.I18nTextUtils;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Potato
 * @date 2023/5/11 09:11
 */

public class CodeAnalyzerUtils {
    public static void evilCodeAnalysis(String evilCode , String encodeModes, Object node) throws Exception {
        String result = buildEvilCodeAnalysisResult(evilCode, encodeModes);
        setNodeContent(node, result);
    }

    public static void streamEvilCodeAnalysis(String evilCode, String encodeModes, Object node) {
        streamEvilCodeAnalysis(evilCode, encodeModes, new AiChatService(), createStreamAppender(node));
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

    static void streamEvilCodeAnalysis(String evilCode,
                                       String encodeModes,
                                       AiChatService aiService,
                                       StreamAppender appender) {
        if (appender == null) {
            throw new IllegalArgumentException("stream appender is required");
        }

        appender.setText(I18nTextUtils.getString("webshell.ai.section.summary") + "\n");
        StreamPhaseResult result = streamPrompt(
                aiService,
                AiPromptUtils.securityAnalysisSystemPrompt(),
                buildUnifiedAnalysisPrompt(evilCode, encodeModes),
                appender
        );
        if (result.hasError()) {
            if (result.hasContent()) {
                appender.append("\n\n");
                appender.append(I18nTextUtils.getString("webshell.ai.stream.error", result.getError()));
            } else {
                appender.setText(result.getError());
            }
            return;
        }
        if (!result.hasContent()) {
            appender.setText(I18nTextUtils.getString("webshell.ai.empty"));
        }
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
                appender
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
                                                  StreamAppender appender) {
        final StringBuilder buffer = new StringBuilder();
        final String[] errorHolder = new String[1];
        aiService.streamChat(systemPrompt, prompt, event -> handlePhaseEvent(event, buffer, appender, errorHolder));
        return new StreamPhaseResult(buffer.toString(), errorHolder[0]);
    }

    private static void handlePhaseEvent(AiStreamEvent event,
                                         StringBuilder buffer,
                                         StreamAppender appender,
                                         String[] errorHolder) {
        if (event == null) {
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

    private static boolean isLikelyAiError(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        String text = content.trim();
        return text.startsWith("AI ") || text.startsWith("AI请求") || text.startsWith("AI配置") || text.startsWith("AI连接");
    }

    public static void main(String[] args) {
        AiChatService aiService = new AiChatService();
        System.out.println(aiService.askNoStream("请告诉我关于```获取当前系统用户```的命令"));
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

    private static StreamAppender createStreamAppender(Object node) {
        if (node instanceof TextArea) {
            return new FxBufferedTextAreaAppender((TextArea) node);
        }
        if (node instanceof CodeArea) {
            return new FxBufferedCodeAreaAppender((CodeArea) node);
        }
        throw new IllegalArgumentException("Unsupported stream node: " + node);
    }

    interface StreamAppender {
        void setText(String text);

        void append(String text);
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
    }

    private abstract static class FxBufferedAppenderSupport {
        private final StringBuilder pending = new StringBuilder();
        private final AtomicBoolean flushQueued = new AtomicBoolean(false);

        public void setText(String text) {
            final String value = text == null ? "" : text;
            synchronized (pending) {
                pending.setLength(0);
            }
            Platform.runLater(() -> applyText(value));
        }

        public void append(String text) {
            if (text == null || text.isEmpty()) {
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
                    if (!chunk.isEmpty()) {
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

        private FxBufferedTextAreaAppender(TextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        protected void applyText(String text) {
            textArea.setText(text);
        }

        @Override
        protected void appendText(String text) {
            textArea.appendText(text);
        }
    }

    private static final class FxBufferedCodeAreaAppender extends FxBufferedAppenderSupport implements StreamAppender {
        private final CodeArea codeArea;

        private FxBufferedCodeAreaAppender(CodeArea codeArea) {
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
