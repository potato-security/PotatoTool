package com.dlsc.gemsfx.skins;

import com.dlsc.gemsfx.ChipView;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class ChipViewSkin<T> extends SkinBase<ChipView<T>> {

    public ChipViewSkin(ChipView<T> chip) {
        super(chip);

        Label label = new Label();
        label.textProperty().bind(chip.textProperty());
        label.graphicProperty().bind(chip.graphicProperty());
        label.contentDisplayProperty().bind(chip.contentDisplayProperty());

        FontIcon closeIcon = new FontIcon(MaterialDesign.MDI_CLOSE);
        StackPane.setAlignment(closeIcon, Pos.CENTER);

        StackPane iconWrapper = new StackPane(closeIcon);
        iconWrapper.getStyleClass().add("close-icon");
        iconWrapper.visibleProperty().bind(chip.onCloseProperty().isNotNull());
        iconWrapper.managedProperty().bind(chip.onCloseProperty().isNotNull());
        iconWrapper.setOnMouseClicked(evt -> chip.getOnClose().accept(chip.getValue()));

        HBox box = new HBox(label, iconWrapper);
        box.getStyleClass().add("chip-container");
        box.setAlignment(Pos.CENTER_LEFT);

        getChildren().setAll(box);
    }
}
