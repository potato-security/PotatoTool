package com.potato.potatotool.utils.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;

import static com.potato.potatotool.utils.ui.ScrollOptimizationConstants.*;

/**
 * 虚拟化控件（ListView、TableView）滚动优化
 * 同时支持鼠标滚轮和触摸板，采用速度累积模式
 *
 * @author Potato
 * @since 2.5.1
 */
public class VirtualScrollFix {

    private final Control control;
    private final double sensitivity;
    private final double friction;

    private AnimationTimer scrollTimer;
    private ScrollBar verticalScrollBar;
    private ScrollBar horizontalScrollBar;

    private double velocityV = 0.0;
    private double velocityH = 0.0;
    private boolean isAnimating = false;

    public static void apply(Control control) {
        apply(control, VIRTUAL_CONTROL_SENSITIVITY, VIRTUAL_CONTROL_FRICTION);
    }

    public static void apply(Control control, double sensitivity) {
        apply(control, sensitivity, VIRTUAL_CONTROL_FRICTION);
    }

    public static void apply(Control control, double sensitivity, double friction) {
        new VirtualScrollFix(control, sensitivity, friction);
    }

    private VirtualScrollFix(Control control, double sensitivity, double friction) {
        this.control = control;
        this.sensitivity = sensitivity;
        this.friction = friction;

        if (control.getSkin() != null) {
            init();
        } else {
            control.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    init();
                }
            });
        }
    }

    private void init() {
        verticalScrollBar = findScrollBar(control, Orientation.VERTICAL);
        horizontalScrollBar = findScrollBar(control, Orientation.HORIZONTAL);

        if (verticalScrollBar != null || horizontalScrollBar != null) {
            control.addEventFilter(ScrollEvent.SCROLL, this::handleScrollEvent);
        }

        control.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                dispose();
            }
        });
    }

    private void handleScrollEvent(ScrollEvent event) {
        event.consume();

        double deltaY = event.getDeltaY();
        double deltaX = event.getDeltaX();

        if (deltaY == 0 && deltaX == 0) {
            return;
        }

        double controlHeight = control.getHeight();
        double controlWidth = control.getWidth();

        if (controlHeight <= 0 && controlWidth <= 0) {
            return;
        }

        // 计算归一化的滚动增量
        double normalizedDeltaV = 0;
        double normalizedDeltaH = 0;

        if (verticalScrollBar != null && controlHeight > 0) {
            double range = verticalScrollBar.getMax() - verticalScrollBar.getMin();
            if (range > 0) {
                normalizedDeltaV = -deltaY / controlHeight;
            }
        }

        if (horizontalScrollBar != null && controlWidth > 0) {
            double range = horizontalScrollBar.getMax() - horizontalScrollBar.getMin();
            if (range > 0) {
                normalizedDeltaH = -deltaX / controlWidth;
            }
        }

        if (event.isInertia()) {
            // 触摸板惯性事件：直接应用
            handleInertiaEvent(normalizedDeltaV, normalizedDeltaH);
        } else if (isWheelEvent(deltaY, deltaX)) {
            // 鼠标滚轮事件：累积速度，启动平滑动画
            handleWheelEvent(normalizedDeltaV, normalizedDeltaH);
        } else {
            // 触摸板滑动事件：直接应用增量
            handleTrackpadEvent(normalizedDeltaV, normalizedDeltaH);
        }
    }

    private boolean isWheelEvent(double deltaY, double deltaX) {
        double maxDelta = Math.max(Math.abs(deltaY), Math.abs(deltaX));
        return maxDelta >= WHEEL_THRESHOLD;
    }

    /**
     * 处理鼠标滚轮事件
     */
    private void handleWheelEvent(double normalizedDeltaV, double normalizedDeltaH) {
        // 累积速度
        velocityV += normalizedDeltaV * sensitivity * VELOCITY_MULTIPLIER;
        velocityH += normalizedDeltaH * sensitivity * VELOCITY_MULTIPLIER;

        // 限制最大速度
        velocityV = clamp(velocityV, -MAX_VELOCITY, MAX_VELOCITY);
        velocityH = clamp(velocityH, -MAX_VELOCITY, MAX_VELOCITY);

        ensureAnimationRunning();
    }

    /**
     * 处理触摸板滑动事件
     */
    private void handleTrackpadEvent(double normalizedDeltaV, double normalizedDeltaH) {
        applyScrollDelta(normalizedDeltaV * sensitivity, normalizedDeltaH * sensitivity);
    }

    /**
     * 处理触摸板惯性事件
     */
    private void handleInertiaEvent(double normalizedDeltaV, double normalizedDeltaH) {
        applyScrollDelta(normalizedDeltaV * sensitivity, normalizedDeltaH * sensitivity);
    }

    /**
     * 直接应用滚动增量
     */
    private void applyScrollDelta(double deltaV, double deltaH) {
        if (verticalScrollBar != null && deltaV != 0) {
            double range = verticalScrollBar.getMax() - verticalScrollBar.getMin();
            double delta = deltaV * range;
            double newV = verticalScrollBar.getValue() + delta;
            verticalScrollBar.setValue(clamp(newV, verticalScrollBar.getMin(), verticalScrollBar.getMax()));
        }

        if (horizontalScrollBar != null && deltaH != 0) {
            double range = horizontalScrollBar.getMax() - horizontalScrollBar.getMin();
            double delta = deltaH * range;
            double newH = horizontalScrollBar.getValue() + delta;
            horizontalScrollBar.setValue(clamp(newH, horizontalScrollBar.getMin(), horizontalScrollBar.getMax()));
        }
    }

    private void ensureAnimationRunning() {
        if (isAnimating) {
            return;
        }

        isAnimating = true;
        scrollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                velocityV *= friction;
                velocityH *= friction;

                if (Math.abs(velocityV) < MIN_VELOCITY && Math.abs(velocityH) < MIN_VELOCITY) {
                    stopAnimation();
                    return;
                }

                boolean vUpdated = false;
                boolean hUpdated = false;

                if (verticalScrollBar != null && Math.abs(velocityV) >= MIN_VELOCITY) {
                    double range = verticalScrollBar.getMax() - verticalScrollBar.getMin();
                    double delta = velocityV * range;
                    double newV = verticalScrollBar.getValue() + delta;

                    if (newV < verticalScrollBar.getMin() || newV > verticalScrollBar.getMax()) {
                        newV = clamp(newV, verticalScrollBar.getMin(), verticalScrollBar.getMax());
                        velocityV = 0;
                    }

                    verticalScrollBar.setValue(newV);
                    vUpdated = true;
                }

                if (horizontalScrollBar != null && Math.abs(velocityH) >= MIN_VELOCITY) {
                    double range = horizontalScrollBar.getMax() - horizontalScrollBar.getMin();
                    double delta = velocityH * range;
                    double newH = horizontalScrollBar.getValue() + delta;

                    if (newH < horizontalScrollBar.getMin() || newH > horizontalScrollBar.getMax()) {
                        newH = clamp(newH, horizontalScrollBar.getMin(), horizontalScrollBar.getMax());
                        velocityH = 0;
                    }

                    horizontalScrollBar.setValue(newH);
                    hUpdated = true;
                }

                if (!vUpdated && !hUpdated) {
                    stopAnimation();
                }

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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ScrollBar findScrollBar(Node node, Orientation orientation) {
        if (node instanceof ScrollBar) {
            ScrollBar scrollBar = (ScrollBar) node;
            if (scrollBar.getOrientation() == orientation) {
                return scrollBar;
            }
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                ScrollBar result = findScrollBar(child, orientation);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void dispose() {
        stopAnimation();
    }
}
