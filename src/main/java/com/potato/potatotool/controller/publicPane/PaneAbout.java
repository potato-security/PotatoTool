package com.potato.potatotool.controller.publicPane;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.potato.potatotool.utils.core.Constants.getResourceString;

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
    private ScrollPane mainScrollPane;

    @FXML
    private VBox contentVBox;

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
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
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
            typeLabel.setStyle("-fx-font-size: 10;");
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
        thanksScrollPane.getContent().layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            double contentHeight = newValue.getHeight();
            double viewportHeight = thanksScrollPane.getViewportBounds().getHeight();

            double speed = 14;
            double time = (contentHeight - viewportHeight) / speed;

            if(time<0) return;

            scrollTimeline.getKeyFrames().clear();

            Duration pauseBeforeDuration = Duration.seconds(1);
            Duration scrollDuration = Duration.seconds(time);
            Duration pauseAfterDuration = Duration.seconds(1);

            KeyFrame pauseBeforeKF = new KeyFrame(pauseBeforeDuration, new KeyValue(thanksScrollPane.vvalueProperty(), 0));
            KeyFrame scrollKF = new KeyFrame(pauseBeforeDuration.add(scrollDuration), new KeyValue(thanksScrollPane.vvalueProperty(), 1)); // Scroll
            KeyFrame pauseAfterKF = new KeyFrame(pauseBeforeDuration.add(scrollDuration).add(pauseAfterDuration));

            scrollTimeline.getKeyFrames().addAll(pauseBeforeKF, scrollKF, pauseAfterKF);
            scrollTimeline.setCycleCount(Timeline.INDEFINITE);
        });

        thanksScrollPane.setOnMouseEntered(event -> {
            scrollTimeline.pause();
        });

        thanksScrollPane.setOnMouseExited(event -> {
            scrollTimeline.play();
        });
    }

    @FXML
    public void pauseScrolling() {
        if (scrollTimeline != null) {
            scrollTimeline.pause();
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
    void openGitHub(){
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
    void openGitHubIssues(){
        services.showDocument("https://github.com/HotBoy-java/PotatoTool/issues");
    }

    @FXML
    void redirect(ActionEvent event) {
        String newKey = ((RXLineButton) event.getSource()).getText();
        for (Node node : contentVBox.getChildren()) {
            if (node instanceof Text) {
                String key = ((Text) node).getText();
                if (key.equals(newKey)) {
                    // 计算目标位置并滚动
                    double contentHeight = contentVBox.getBoundsInLocal().getHeight();
                    double viewportHeight = mainScrollPane.getViewportBounds().getHeight();
                    double nodeY = node.getBoundsInParent().getMinY();
                    double scrollValue = nodeY / (contentHeight - viewportHeight);
                    mainScrollPane.setVvalue(Math.min(1.0, Math.max(0.0, scrollValue)));
                    return;
                }
            }
        }
    }

}
