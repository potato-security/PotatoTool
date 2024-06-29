package com.dlsc.gemsfx;

import com.dlsc.gemsfx.skins.CFCheckBoxSkin;
import javafx.scene.control.CheckBox;

public class CFCheckBox extends CheckBox {

    private static final String STYLE_SHEET = CFCheckBox.class.getResource("/css/cf-check-box.css").toExternalForm();
    private static final String STYLE_CLASS = "cf-check-box";

    public CFCheckBox() {
        initialize();
    }

    public CFCheckBox(String text) {
        setText(text);
        initialize();
    }

    public CFCheckBox(String text, boolean selected) {
        setText(text);
        setSelected(selected);
        initialize();
    }

    private void initialize() {
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        getStyleClass().add(STYLE_CLASS);
    }

    @Override
    protected CFCheckBoxSkin createDefaultSkin() {
        return new CFCheckBoxSkin(this);
    }

    @Override
    public String getUserAgentStylesheet() {
        return STYLE_SHEET;
    }

}
