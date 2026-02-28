package com.potato.potatotool.utils.ui;

/**
 * 滚动优化统一参数常量
 *
 * @author Potato
 * @since 2.5.1
 */
public final class ScrollOptimizationConstants {

    private ScrollOptimizationConstants() {
    }

    // ==================== 用户可调参数 ====================

    /**
     * 默认滚动敏感度（速度倍率）
     * 适用 ScrollPane、TextArea，范围：0.5-2.0
     */
    public static final double DEFAULT_SENSITIVITY = 3.0;

    /**
     * 虚拟化控件滚动敏感度（速度倍率）
     * 适用 ListView、TableView，范围：0.3-1.5
     * 由于虚拟化控件的滚动机制不同，需要更低的灵敏度
     */
    public static final double VIRTUAL_CONTROL_SENSITIVITY = 0.3;

    /**
     * 默认摩擦系数（惯性衰减速度）
     * 适用 ScrollPane、TextArea，范围：0.85-0.98
     */
    public static final double DEFAULT_FRICTION = 0.91;

    /**
     * 虚拟化控件摩擦系数（惯性衰减速度）
     * 适用 ListView、TableView，范围：0.85-0.98
     */
    public static final double VIRTUAL_CONTROL_FRICTION = 0.91;

    // ==================== 输入类型识别 ====================

    /**
     * 鼠标滚轮事件判断阈值
     * deltaY/deltaX 绝对值 >= 此值判定为鼠标滚轮
     * 鼠标滚轮通常产生较大的 delta 值（40, 80, 120 等）
     * 触摸板通常产生较小的连续 delta 值（1-20）
     */
    public static final double WHEEL_THRESHOLD = 25.0;

    // ==================== 动画控制参数 ====================

    /**
     * 惯性滚动最小速度阈值
     */
    public static final double MIN_VELOCITY = 0.001;

    /**
     * 惯性滚动最大速度限制
     */
    public static final double MAX_VELOCITY = 0.4;

    /**
     * 速度乘数
     */
    public static final double VELOCITY_MULTIPLIER = 0.3;
}
