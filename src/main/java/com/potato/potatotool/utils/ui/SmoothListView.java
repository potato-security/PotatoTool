package com.potato.potatotool.utils.ui;

import javafx.scene.control.ListView;

/**
 * 平滑滚动 ListView
 * 内部不要嵌套ScrollPane、可以外侧换用ScrollPane
 *
 * @author Potato
 * @since 2.5.1
 */
public class SmoothListView<T> extends ListView<T> {

    public SmoothListView() {
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