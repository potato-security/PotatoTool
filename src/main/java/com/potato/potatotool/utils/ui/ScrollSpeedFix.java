package com.potato.potatotool.utils.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.scene.input.ScrollEvent;

import static com.potato.potatotool.utils.ui.ScrollOptimizationConstants.*;

/**
 * ScrollPane 平滑滚动优化
 * 同时支持鼠标滚轮和触摸板，采用速度累积模式
 *
 * @author Potato
 * @since 2.5.1
 */
public class ScrollSpeedFix {

    private final ScrollPane scrollPane;
    private final double sensitivity;
    private final double friction;

    private AnimationTimer scrollTimer;
    private double velocityV = 0.0;
    private double velocityH = 0.0;
    private boolean isAnimating = false;

    public static void apply(ScrollPane scrollPane) {
        apply(scrollPane, DEFAULT_SENSITIVITY, DEFAULT_FRICTION);
    }

    public static void apply(ScrollPane scrollPane, double sensitivity, double friction) {
        new ScrollSpeedFix(scrollPane, sensitivity, friction);
    }

    private ScrollSpeedFix(ScrollPane scrollPane, double sensitivity, double friction) {
        this.scrollPane = scrollPane;
        this.sensitivity = sensitivity;
        this.friction = friction;
        init();
    }

    private void init() {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleScrollEvent);

        scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                dispose();
            }
        });
    }

    private void handleScrollEvent(ScrollEvent event) {
        if (isNestedScrollEvent(event)) {
            return;
        }
        event.consume();

        double deltaY = event.getDeltaY();
        double deltaX = event.getDeltaX();

        if (deltaY == 0 && deltaX == 0) {
            return;
        }

        if (scrollPane.getContent() == null) {
            return;
        }

        double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        double contentWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();

        double scrollableHeight = contentHeight - viewportHeight;
        double scrollableWidth = contentWidth - viewportWidth;

        if (scrollableHeight <= 0 && scrollableWidth <= 0) {
            return;
        }

        // 计算归一化的滚动增量
        double normalizedDeltaV = scrollableHeight > 0 ? -deltaY / scrollableHeight : 0;
        double normalizedDeltaH = scrollableWidth > 0 ? -deltaX / scrollableWidth : 0;

        if (event.isInertia()) {
            // 触摸板惯性事件：直接应用，让系统惯性自然衰减
            handleInertiaEvent(normalizedDeltaV, normalizedDeltaH);
        } else if (isWheelEvent(deltaY, deltaX)) {
            // 鼠标滚轮事件：累积速度，启动平滑动画
            handleWheelEvent(normalizedDeltaV, normalizedDeltaH);
        } else {
            // 触摸板滑动事件：直接应用增量，保持原生流畅
            handleTrackpadEvent(normalizedDeltaV, normalizedDeltaH);
        }
    }

    /**
     * 判断是否为鼠标滚轮事件
     * 鼠标滚轮的 delta 值通常较大且固定（40 的倍数）
     */
    private boolean isWheelEvent(double deltaY, double deltaX) {
        double maxDelta = Math.max(Math.abs(deltaY), Math.abs(deltaX));
        return maxDelta >= WHEEL_THRESHOLD;
    }

    /**
     * 处理鼠标滚轮事件
     * 使用速度累积 + 摩擦衰减模式，提供平滑滚动
     */
    private void handleWheelEvent(double normalizedDeltaV, double normalizedDeltaH) {
        // 累积速度（而非替换），让连续滚轮事件叠加
        velocityV += normalizedDeltaV * sensitivity * VELOCITY_MULTIPLIER;
        velocityH += normalizedDeltaH * sensitivity * VELOCITY_MULTIPLIER;

        // 限制最大速度
        velocityV = clamp(velocityV, -MAX_VELOCITY, MAX_VELOCITY);
        velocityH = clamp(velocityH, -MAX_VELOCITY, MAX_VELOCITY);

        // 启动动画（如果未运行）
        ensureAnimationRunning();
    }

    /**
     * 处理触摸板滑动事件
     * 直接应用增量，保持原生流畅性
     */
    private void handleTrackpadEvent(double normalizedDeltaV, double normalizedDeltaH) {
        // 触摸板滑动：直接设置位置，不使用动画
        // 因为触摸板事件本身就是连续的，不需要额外平滑
        double newV = scrollPane.getVvalue() + normalizedDeltaV * sensitivity;
        double newH = scrollPane.getHvalue() + normalizedDeltaH * sensitivity;

        scrollPane.setVvalue(clamp(newV, 0.0, 1.0));
        scrollPane.setHvalue(clamp(newH, 0.0, 1.0));
    }

    /**
     * 处理触摸板惯性事件
     * 让系统惯性自然衰减，只应用轻微的速度调整
     */
    private void handleInertiaEvent(double normalizedDeltaV, double normalizedDeltaH) {
        // 惯性事件：直接应用，不干扰系统惯性
        double newV = scrollPane.getVvalue() + normalizedDeltaV * sensitivity;
        double newH = scrollPane.getHvalue() + normalizedDeltaH * sensitivity;

        scrollPane.setVvalue(clamp(newV, 0.0, 1.0));
        scrollPane.setHvalue(clamp(newH, 0.0, 1.0));
    }

    /**
     * 确保动画正在运行
     */
    private void ensureAnimationRunning() {
        if (isAnimating) {
            return;
        }

        isAnimating = true;
        scrollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 应用摩擦衰减
                velocityV *= friction;
                velocityH *= friction;

                // 速度足够小时停止动画
                if (Math.abs(velocityV) < MIN_VELOCITY && Math.abs(velocityH) < MIN_VELOCITY) {
                    stopAnimation();
                    return;
                }

                // 更新位置
                double newV = scrollPane.getVvalue() + velocityV;
                double newH = scrollPane.getHvalue() + velocityH;

                // 边界检测
                if (newV < 0.0 || newV > 1.0) {
                    newV = clamp(newV, 0.0, 1.0);
                    velocityV = 0;
                }
                if (newH < 0.0 || newH > 1.0) {
                    newH = clamp(newH, 0.0, 1.0);
                    velocityH = 0;
                }

                scrollPane.setVvalue(newV);
                scrollPane.setHvalue(newH);

                // 两个方向都到达边界时停止
                if (velocityV == 0 && velocityH == 0) {
                    stopAnimation();
                }
            }
        };
        scrollTimer.start();
    }

    private void stopAnimation() {
        if (scrollTimer != null) {
            scrollTimer.stop();
            scrollTimer = null;
        }
        isAnimating = false;
        velocityV = 0;
        velocityH = 0;
    }

    private boolean isNestedScrollEvent(ScrollEvent event) {
        Node current = (Node) event.getTarget();
        while (current != null && current != scrollPane) {
            if (current instanceof ScrollPane ||
                current instanceof ListView ||
                current instanceof TableView ||
                current instanceof TreeView ||
                current instanceof TextArea) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void dispose() {
        stopAnimation();
    }
}
