package com.dlsc.gemsfx;

import com.dlsc.gemsfx.skins.ChipViewSkin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A small "badge-style" view representing a model object. One usage inside GemsFX
 * is the display of the currently active filters inside {@link FilterView}.
 *
 * @param <T> the model object represented by the chip
 */
public class ChipView<T> extends Control {

    public ChipView() {
        getStyleClass().add("chip-view");

        setMinWidth(Region.USE_PREF_SIZE);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ChipViewSkin<>(this);
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(ChipView.class.getResource("/css/chip-view.css")).toExternalForm();
    }

    // value

    private final ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");

    /**
     * The value / model object represented by the chip view.
     *
     * @return the value of the view
     */
    public final ObjectProperty<T> valueProperty() {
        return value;
    }

    public final T getValue() {
        return value.get();
    }

    public final void setValue(T value) {
        this.value.set(value);
    }

    // text

    private final StringProperty text = new SimpleStringProperty(this, "text", "Untitled");

    public final StringProperty textProperty() {
        return text;
    }

    /**
     * The text shown by the view.
     *
     * @return the chip view's text
     */
    public final String getText() {
        return text.get();
    }

    public final void setText(String text) {
        this.text.set(text);
    }

    // graphic

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");

    /**
     * The graphic node shown by the chip view.
     *
     * @return the chip view's graphic
     */
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public final Node getGraphic() {
        return graphic.get();
    }

    public final void setGraphic(Node graphic) {
        this.graphic.set(graphic);
    }

    // content display

    private final ObjectProperty<ContentDisplay> contentDisplay = new SimpleObjectProperty<>(this, "contentDisplay", ContentDisplay.LEFT);

    public final ContentDisplay getContentDisplay() {
        return contentDisplay.get();
    }

    /**
     * The content display property of the chip will be bound to the same property
     * of the label used by the chip's skin. This property allows applications to
     * switch to a "graphics only" mode (see {@link ContentDisplay#GRAPHIC_ONLY}).
     *
     * @return the content display value
     */
    public final ObjectProperty<ContentDisplay> contentDisplayProperty() {
        return contentDisplay;
    }

    public final void setContentDisplay(ContentDisplay contentDisplay) {
        this.contentDisplay.set(contentDisplay);
    }

    // on close

    private final ObjectProperty<Consumer<T>> onClose = new SimpleObjectProperty<>(this, "onClose");

    /**
     * A callback consumer that will be invoked when the user clicks on the
     * close icon of the chip. Applications can use this property / consumer
     * to update the UI as a result of the closing.
     *
     * @return the on-close consumer
     */
    public final ObjectProperty<Consumer<T>> onCloseProperty() {
        return onClose;
    }

    public final Consumer<T> getOnClose() {
        return onClose.get();
    }

    public final void setOnClose(Consumer<T> onClose) {
        this.onClose.set(onClose);
    }
}
