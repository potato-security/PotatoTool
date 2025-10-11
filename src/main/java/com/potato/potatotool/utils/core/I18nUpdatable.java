package com.potato.potatotool.utils.core;

/**
 * 国际化更新接口
 * 所有需要响应语言切换的界面控制器都应该实现此接口
 * @author Potato
 * @date 2025/10/10
 */
public interface I18nUpdatable {
    
    /**
     * 当语言切换时，此方法会被调用
     * 实现类应该在这个方法中更新所有UI文本
     */
    void updateLanguage();
}

