package com.dlsc.gemsfx;

import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.textfield.CustomTextField;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.util.Objects;

public class SearchTextField extends CustomTextField {

    public SearchTextField() {
        this(false);
    }

    public SearchTextField(boolean round) {
        if (round) {
            getStyleClass().add("round");
        }

        getStyleClass().add("search-text-field");

        setPromptText("Search...");

        FontIcon searchIcon = new FontIcon(MaterialDesign.MDI_MAGNIFY);
        searchIcon.getStyleClass().add("search-icon");

        StackPane searchIconWrapper = new StackPane(searchIcon);
        searchIconWrapper.getStyleClass().addAll("wrapper", "search-icon-wrapper");

        setLeft(searchIconWrapper);

        FontIcon clearIcon = new FontIcon(MaterialDesign.MDI_CLOSE);
        clearIcon.getStyleClass().add("clear-icon");
        clearIcon.setCursor(Cursor.DEFAULT);
        clearIcon.setOnMouseClicked(evt -> setText(""));
        clearIcon.visibleProperty().bind(textProperty().isNotEmpty());
        clearIcon.managedProperty().bind(textProperty().isNotEmpty());

        StackPane clearIconWrapper = new StackPane(clearIcon);
        clearIconWrapper.getStyleClass().addAll("wrapper", "clear-icon-wrapper");

        setRight(clearIconWrapper);
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(SearchTextField.class.getResource("/css/search-text-field.css")).toExternalForm();
    }
}
