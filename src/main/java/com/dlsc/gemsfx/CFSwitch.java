package com.dlsc.gemsfx;

import com.dlsc.gemsfx.skins.CFSwitchSkin;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Potato
 * @date 2024/7/5 09:33
 */
public class CFSwitch extends ToggleButton {

    private static final String STYLE_SHEET = CFSwitch.class.getResource("/css/cf-switch.css").toExternalForm();
    private static final String STYLE_CLASS = "cf-switch";

    public CFSwitch() {
        initialize();
    }

    public CFSwitch(boolean value) {
        initialize();
        setSelected(value);
    }

    public CFSwitch(boolean value, String text) {
        initialize();
        setSelected(value);
        setText(text);
    }

    private void initialize() {
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        getStyleClass().setAll(STYLE_CLASS);
    }

    private final StyleableObjectProperty<Color> cfColor = new SimpleStyleableObjectProperty<>(StyleableProperties.CF_COLOR,
            CFSwitch.this, "cfColor", Color.TRANSPARENT);

    public final StyleableObjectProperty<Color> cfColorProperty() {
        return cfColor;
    }

    public final Color getCfColor() {
        return cfColor.get();
    }

    /**
     * 自定义CSS属性
     */
    private static class StyleableProperties {
        private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

        private static final CssMetaData<CFSwitch, Color> CF_COLOR =
                new CssMetaData<>("-cf-color",
                        new StyleConverter<>(), Color.TRANSPARENT) {
                    @Override
                    public boolean isSettable(CFSwitch styleable) {
                        return styleable.cfColor == null || !styleable.cfColor.isBound();
                    }

                    @Override
                    public StyleableProperty<Color> getStyleableProperty(CFSwitch cfSwitch) {
                        return cfSwitch.cfColor;// 返回一个StyleableProperty，其他地方绑定这个属性即可
                    }
                };

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            Collections.addAll(styleables, CF_COLOR);
            CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
        }

    }

    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaDataList() {
        return StyleableProperties.CSS_META_DATA_LIST;
    }

    @Override
    protected CFSwitchSkin createDefaultSkin() {
        return new CFSwitchSkin(this);
    }

    @Override
    public String getUserAgentStylesheet() {
        return STYLE_SHEET;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getControlCssMetaDataList();
    }

}
