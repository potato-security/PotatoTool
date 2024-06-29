package com.dlsc.gemsfx.skins;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;

public abstract class CFLabelSkin<C extends Control> extends SkinBase<C> {

    private final Label container;

    protected CFLabelSkin(C control) {
        super(control);
        container = new Label();
        container.getStyleClass().add("container");
        container.setGraphicTextGap(8);
        getChildren().setAll(container);
    }

    final protected void setGraphic(Node node) {
        container.setGraphic(node);
    }

    final protected StringProperty textProperty(){
        return container.textProperty();
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        super.layoutChildren(contentX, contentY, contentWidth, contentHeight);
    }
}
