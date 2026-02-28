package com.potato.potatotool.utils.ui;

import javafx.scene.control.TableView;

/**
 * 平滑滚动 TableView
 *
 * @author Potato
 * @since 2.5.1
 */
public class SmoothTableView<S> extends TableView<S> {

    public SmoothTableView() {
        VirtualScrollFix.apply(this);
    }

    /**
     * 设置滚动敏感度
     * @param sensitivity 敏感度（0.3-1.5）
     */
    public void setSensitivity(double sensitivity) {
        VirtualScrollFix.apply(this, sensitivity);
    }

    /**
     * 设置滚动参数（敏感度和摩擦系数）
     * @param sensitivity 敏感度（0.3-1.5）
     * @param friction 摩擦系数（0.85-0.98）
     */
    public void setScrollParams(double sensitivity, double friction) {
        VirtualScrollFix.apply(this, sensitivity, friction);
    }
}