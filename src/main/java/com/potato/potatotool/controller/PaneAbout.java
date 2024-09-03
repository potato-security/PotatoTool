package com.potato.potatotool.controller;

import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.MainApplication;
import javafx.animation.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.*;
import java.util.function.Function;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneAbout {

    @FXML
    private StackPane sPane;

    @FXML
    private VBox webBox;

    @FXML
    private VBox noWebBox;

    @FXML
    private ListView listView;

    @FXML
    private Text info;

    @FXML
    private Text thanks;

    @FXML
    private ScrollPane thanksScrollPane;

    @FXML
    private VBox thanksVBox;

    @FXML
    private Text bug;

    @FXML
    private Text group;


    public void initialize() {

        scrollThankPane();

    }

    private Timeline scrollTimeline;

    private void scrollThankPane() {
        scrollTimeline = new Timeline(new KeyFrame(Duration.millis(16), event -> {
            double currentScroll = thanksScrollPane.getVvalue();
            double newScroll = currentScroll + 0.003;
            if (newScroll > 1.0) {
                newScroll = 0.0;
            }
            thanksScrollPane.setVvalue(newScroll);
        }));
        scrollTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    @FXML
    public void stopScrolling() {
        if (scrollTimeline != null) {
            scrollTimeline.stop();
        }
    }

    @FXML
    public void startScrolling() {
        if (scrollTimeline != null && thanksVBox.getBoundsInLocal().getHeight() > thanksScrollPane.getViewportBounds().getHeight()) {
            scrollTimeline.play();
        }
    }

    HostServices services = MainApplication.letGetHostServices();
    @FXML
    void openGithub(){
        services.showDocument("https://github.com/ljy1058318852");
    }

    @FXML
    void openBlog(){
        services.showDocument("https://potato.gold");
    }

    @FXML
    void openCSDN(){
        services.showDocument("https://blog.csdn.net/weixin_43526443");
    }

    @FXML
    void openWJX(){
        services.showDocument("https://www.wjx.cn/vm/hsIQ1et.aspx");
    }

    @FXML
    void redirect(ActionEvent event) {
        String newKey = ((RXLineButton) event.getSource()).getText();
        for (int i = 0; i < listView.getItems().size(); i++) {
            Node node = (Node) listView.getItems().get(i);
            if (node instanceof Text) {
                String key = ((Text)node).getText();
                if(key.equals(newKey)){
                    listView.scrollTo(i);
                    return;
                }
            }
        }
    }

}
