package com.potato.potatotool.utils.ui.highlighters;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 多语言代码高亮器
 * 支持动态切换语言和异步高亮处理
 * @author Potato
 */
public class MultiLanguageHighlighter implements AutoCloseable {
    
    private CodeArea codeArea;
    private ExecutorService executor;
    private LanguageStyler currentStyler;
    private Subscription subscription;
    private static final Duration HIGHLIGHTING_DELAY = Duration.ofMillis(500);
    
    /**
     * 语言高亮器接口，定义了不同语言高亮的基本方法
     */
    public interface LanguageStyler {
        /**
         * 获取支持的语言名称
         * @return 语言名称集合
         */
        Set<String> getLanguageNames();
        
        /**
         * 计算代码高亮样式
         * @param text 需要高亮的文本
         * @return 样式集合
         */
        StyleSpans<Collection<String>> computeStyles(String text);
    }
    
    /**
     * 初始化代码高亮
     * @param codeArea 代码区域组件
     * @param styler 语言高亮器
     * @throws IllegalArgumentException 如果参数为空
     */
    public void initialize(CodeArea codeArea, LanguageStyler styler) {
        if (codeArea == null) {
            throw new IllegalArgumentException("CodeArea不能为空");
        }
        if (styler == null) {
            throw new IllegalArgumentException("LanguageStyler不能为空");
        }
        
        // 如果已经初始化过，先清理资源
        close();

        this.codeArea = codeArea;
        this.currentStyler = styler;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "highlighter-thread");
            thread.setDaemon(true);
            return thread;
        });

        try {
            Platform.runLater(() -> codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea)));
            
            this.subscription = codeArea.multiPlainChanges()
                    .successionEnds(HIGHLIGHTING_DELAY)
                    .retainLatestUntilLater(executor)
                    .supplyTask(this::computeHighlightingAsync)
                    .awaitLatest(codeArea.multiPlainChanges())
                    .filterMap(t -> t.isSuccess() ? Optional.of(t.get()) : Optional.empty())
                    .subscribe(this::applyHighlighting);

        } catch (Exception e) {
            // 确保初始化失败时资源被正确清理
            close();
            throw new RuntimeException("高亮器初始化失败", e);
        }
    }
    
    /**
     * 更改当前使用的语言高亮器
     * @param styler 新的语言高亮器
     */
    public void changeLanguageStyler(LanguageStyler styler) {

        this.currentStyler = styler;
        // 重新应用高亮
        if (codeArea != null && !codeArea.getText().isEmpty()) {
            Task<StyleSpans<Collection<String>>> task = computeHighlightingAsync();
            task.setOnSucceeded(e -> applyHighlighting(task.getValue()));
            executor.execute(task);
        }
    }
    
    /**
     * 获取当前使用的语言高亮器
     * @return 当前语言高亮器
     */
    public LanguageStyler getCurrentStyler() {
        return currentStyler;
    }
    
    /**
     * 停止高亮服务并释放资源
     * 实现AutoCloseable接口，支持try-with-resources语法
     */
    @Override
    public void close() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                // 等待终止，但不超过1秒
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        
        codeArea = null;
        currentStyler = null;
    }
    
    /**
     * 异步计算高亮样式
     * @return 包含高亮样式的任务
     */
    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        final String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                try {
                    return currentStyler.computeStyles(text);
                } catch (Exception e) {
                    // 返回空样式，避免任务失败
                    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
                    spansBuilder.add(Collections.emptyList(), text.length());
                    return spansBuilder.create();
                }
            }
        };
        
        if (executor != null && !executor.isShutdown()) {
            executor.execute(task);
        }
        return task;
    }
    
    /**
     * 应用高亮样式到代码区域
     * @param highlighting 高亮样式
     */
    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        if (codeArea != null && highlighting != null) {
            Platform.runLater(() -> {
                try {
                    codeArea.setStyleSpans(0, highlighting);
                } catch (Exception ignored) {
                    // 忽略应用样式时的异常
                }
            });
        }
    }
}