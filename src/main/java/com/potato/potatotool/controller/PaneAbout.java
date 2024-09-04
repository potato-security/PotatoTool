package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.utils.jsonUtils;
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
import org.json.JSONObject;

import java.io.InputStream;
import java.net.*;
import java.util.function.Function;

import static com.potato.potatotool.utils.Constants.getResourceStream;
import static com.potato.potatotool.utils.Constants.getResourceString;

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

        initRenderThanksData();
        scrollThankPane();

    }

    public void initRenderThanksData() {
        String tmpJsonStr = getResourceString("thanks");
        JsonArray thanksJsonObject = (new Gson()).fromJson(tmpJsonStr, JsonArray.class);

        for (JsonElement element : thanksJsonObject) {
            JsonObject jsonObject = element.getAsJsonObject();

            String version = jsonObject.get("version").getAsString();
            String type = jsonObject.get("type").getAsString();
            String content = jsonObject.get("content").getAsString();

            HBox hBox = new HBox();
            Label versionLabel = new Label("· V" + version + " ");
            versionLabel.getStyleClass().add("tipsLabel");
            hBox.getChildren().add(versionLabel);

            JsonArray names = jsonObject.get("name").getAsJsonArray();
            for (JsonElement nameElement : names) {
                String name = nameElement.getAsString();
                Label text1 = new Label("@");
                Label nameLabel = new Label(name);
                nameLabel.getStyleClass().add("nameLable");
                hBox.getChildren().addAll(text1, nameLabel);
            }

            Label typeLabel = new Label("「" + type + "」");
            typeLabel.setStyle("-fx-font-size: 12;");
            Label text2 = new Label("：");
            Label contentLabel = new Label(content);
            contentLabel.prefWidthProperty().bind(thanksScrollPane.widthProperty().subtract(200));
            contentLabel.setWrapText(true);
            hBox.getChildren().addAll(typeLabel, text2, contentLabel);

            thanksVBox.getChildren().add(hBox);
        }
    }

    private Timeline scrollTimeline=new Timeline();

    private void scrollThankPane() {
        double contentHeight = thanksScrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = thanksScrollPane.getViewportBounds().getHeight();

        double time = (contentHeight - viewportHeight)/ 0.05;
        System.out.println(time);

        KeyValue kv = new KeyValue(thanksScrollPane.vvalueProperty(), 1);
        KeyFrame kf = new KeyFrame(Duration.seconds(time), kv);

        scrollTimeline.getKeyFrames().add(kf);
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
