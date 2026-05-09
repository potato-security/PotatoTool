package com.potato.potatotool.controller.redTeam;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.core.Constants.getResourceString;

/**
 * @author Potato
 * @date 2024/9/3 17:04
 */
public class PaneCustomCommandGeneration {

    @FXML
    private StackPane sPane;

    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private ComboBox shellComboBox;

    @FXML
    private VBox vBoxBar;

    @FXML
    private Accordion accordionPane;

    @FXML
    private ListView listView;

    @FXML
    private Pane promptPane;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    public JsonObject reverseShellCommands = new JsonObject();
    public JsonArray listenerCommands = new JsonArray();
    public JsonArray shells = new JsonArray();
    public JsonObject specialCommands = new JsonObject();

    {
        initData();
    }

    public void initData() {
        String tmpJsonStr = getResourceString("commandGeneration");
        tmpJsonStr = StrUtils.ROT13Decode(tmpJsonStr);
        JsonObject jsonObject = (new Gson()).fromJson(tmpJsonStr, JsonObject.class);

        reverseShellCommands = jsonObject.getAsJsonObject("reverseShellCommands");
        listenerCommands = jsonObject.getAsJsonArray("listenerCommands");
        shells = jsonObject.getAsJsonArray("shells");
        specialCommands = jsonObject.getAsJsonObject("specialCommands");
    }

    public void initialize() {
        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        renderData();

        // 默认第一个导航选项
        redirect(((Label)accordionPane.getPanes().get(0).getGraphic()).getText());

        // 主动带入选项变量
        replaceTips();

        // 监听选项变量变动
        ipField.textProperty().addListener((observable, oldValue, newValue) -> {
            replaceTips();
        });
        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            replaceTips();
        });
        shellComboBox.setOnAction(event -> {
            replaceTips();
        });
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    private void replaceTips(){
        String ip = ipField.getText();
        String port = portField.getText();
        String shell = shellComboBox.getValue().toString();
        String payload = specialCommands.get("PowerShell payload").getAsString().replace("{ip}", ip).replace("{port}", port).replace("{shell}", shell);

        for (Map.Entry<String, JsonElement> entry : reverseShellCommands.entrySet()) {
            String key = entry.getKey();
            JsonArray valueArray = entry.getValue().getAsJsonArray();

            for (JsonElement element : valueArray) {
                JsonObject commandObject = element.getAsJsonObject();
                String name = commandObject.get("name").getAsString();
                String command;
                if(key.equals("ReverseShell") && name.equals("PowerShell #3 (Base64)")){
                    command = "powershell -e " + StrUtils.base64Encode(payload);
                }else {
                    command = commandObject.get("command").getAsString();
                    if(key.equals("Listener")) {
                        try {
                            if (Integer.parseInt(port) < 1024) {
                                command = "sudo " + command;
                            }
                        } catch (NumberFormatException e) {}
                    }
                    command = command.replace("{ip}", ip).replace("{port}", port).replace("{shell}", shell);
                }
                updateCommandLabels(key, name, command);
            }
        }
    }

    // 更新标签的方法
    private void updateCommandLabels(String key, String name, String command) {
        ObservableList<Node> nodeList = contentObjList.get(key);

        for (Node node : nodeList) {
            if (node instanceof StackPane && name.equals(node.getUserData())) {
                StackPane stackPane = (StackPane) node;
                VBox outerVBox = (VBox) ((HBox) stackPane.getChildren().get(0)).getChildren().get(0);
                Label commandLabel = (Label) outerVBox.getChildren().get(1);
                commandLabel.setText(command);
            }
        }
    }

    ObservableList<Node> contentObjReverseShell = FXCollections.observableArrayList();
    Map<String, ObservableList<Node>> contentObjList = new HashMap<String, ObservableList<Node>>() {{
        put("ReverseShell", FXCollections.observableArrayList());
        put("BindShell", FXCollections.observableArrayList());
        put("MSFVenom", FXCollections.observableArrayList());
        put("Listener", FXCollections.observableArrayList());
    }};
    private void renderData() {
        for (Map.Entry<String, JsonElement> entry : reverseShellCommands.entrySet()) {
            String key = entry.getKey();
            JsonArray valueArray = entry.getValue().getAsJsonArray();

            Label titleLabel = new Label(key);
            titleLabel.setPrefWidth(150);
            TitledPane titledPane = new TitledPane();
            titledPane.setGraphic(titleLabel);
            VBox innerVBox = new VBox(5);

            for (JsonElement element : valueArray) {
                JsonObject commandObject = element.getAsJsonObject();
                String name = commandObject.get("name").getAsString();

                innerVBox.getStyleClass().add("vBox");
                Label label = new Label(name);
                label.setPrefWidth(180);
                label.setOnMouseClicked(even->redirectContent(name));
                Tooltip tooltip = new Tooltip(name);
                Tooltip.install(label, tooltip);
                innerVBox.getChildren().add(label);

                HBox hBox = new HBox();
                hBox.getStyleClass().add("commandInfoHBox");
                VBox vBox = new VBox();

                String command = "";
                if(!name.equals("PowerShell #3 (Base64)")){
                    command = commandObject.get("command").getAsString();
                }
                VBox typeBox = new VBox();
                typeBox.setSpacing(5);
                if(commandObject.has("type")) {
                    JsonArray typeArray = commandObject.getAsJsonArray("type");

                    for (JsonElement typeElement : typeArray) {
                        String type = typeElement.getAsString();

                        Label sysTitle = new Label(type);
                        sysTitle.getStyleClass().add("sysTitle");
                        typeBox.getChildren().add(sysTitle);
                    }
                }

                Label nameLabel = new Label(name);
                nameLabel.setWrapText(true);

                Label commandLabel = new Label(command);
                commandLabel.setWrapText(true);
                commandLabel.setStyle("-fx-text-fill: -main-linenoText-color;");

                vBox.setAlignment(Pos.CENTER_LEFT);
                vBox.getChildren().addAll(nameLabel, commandLabel);
                vBox.setSpacing(10);
                vBox.prefWidthProperty().bind(listView.widthProperty().multiply(0.75));

                hBox.getChildren().addAll(vBox, typeBox);

                StackPane commandSP = new StackPane();
                commandSP.setUserData(name);

                VBox commandCopyBtBox = new VBox();
                commandCopyBtBox.setSpacing(5);
                commandCopyBtBox.setStyle("-fx-padding: 5;");
                commandCopyBtBox.setMaxWidth(110);

                Button commandCopyBt = new Button(I18nUtils.getString("cmdquery.copy.button"));
                commandCopyBt.getStyleClass().add("copyButton");
                commandCopyBt.setOnAction(event -> copyTip(event, commandLabel, ""));
                Button commandCopyBt_Base64 = new Button("Base64");
                commandCopyBt_Base64.getStyleClass().add("copyButton");
                commandCopyBt_Base64.setOnAction(event -> copyTip(event, commandLabel, "Base64"));
                Button commandCopyBt_URL = new Button("URL");
                commandCopyBt_URL.getStyleClass().add("copyButton");
                commandCopyBt_URL.setOnAction(event -> copyTip(event, commandLabel, "URL"));
                Button commandCopyBt_DURL = new Button("Double URL");
                commandCopyBt_DURL.getStyleClass().add("copyButton");
                commandCopyBt_DURL.setOnAction(event -> copyTip(event, commandLabel, "Double URL"));

                commandCopyBtBox.getChildren().addAll(commandCopyBt, commandCopyBt_Base64, commandCopyBt_URL, commandCopyBt_DURL);
                commandCopyBtBox.setVisible(false);
                commandCopyBtBox.setManaged(false);
                commandSP.getChildren().addAll(hBox, commandCopyBtBox);
                commandSP.setAlignment(commandCopyBtBox, Pos.TOP_RIGHT);
                commandSP.setOnMouseEntered(event -> handleMouseEntered(event, commandCopyBtBox));
                commandSP.setOnMouseExited(event -> handleMouseExited(event, commandCopyBtBox));

                contentObjList.get(key).add(commandSP);
            }

            titledPane.setContent(innerVBox);
            titledPane.setOnMouseClicked(even -> redirect(key));
            Tooltip tooltip = new Tooltip(key);
            Tooltip.install(titleLabel, tooltip);

            accordionPane.getPanes().add(titledPane);
        }

        listView.setItems(contentObjReverseShell);

        for (JsonElement shell : shells) {
            shellComboBox.getItems().add(shell.getAsString());
        }
        shellComboBox.getSelectionModel().selectFirst();;
    }

    private void redirectContent(String name) {
        for (int i = 0; i < listView.getItems().size(); i++) {
            Node node = (Node) listView.getItems().get(i);
            if (node instanceof StackPane) {
                VBox outerVBox = (VBox) ((HBox) ((StackPane)node).getChildren().get(0)).getChildren().get(0);
                Label nameLabel = (Label) outerVBox.getChildren().get(0);
                if(nameLabel.getText().equals(name)){
                    listView.scrollTo(i);
                    return;
                }
            }
        }
    }


    private void redirect(String key) {
        listView.setItems(contentObjList.get(key));
    }


    private void handleMouseEntered(MouseEvent event, VBox tipTitleCopy) {
        tipTitleCopy.setVisible(true);
        tipTitleCopy.setManaged(true);
    }

    private void handleMouseExited(MouseEvent event, VBox tipTitleCopy) {
        tipTitleCopy.setVisible(false);
        tipTitleCopy.setManaged(false);
    }

    private void copyTip(ActionEvent event, Label tipTitle, String mode){
        String tip = tipTitle.getText();
        if(mode.equals("Base64")){
            tip = StrUtils.base64Encode(tip);
        } else if(mode.equals("URL")){
            tip = StrUtils.urlEncode(tip);
        } else if(mode.equals("Double URL")){
            tip = StrUtils.urlEncode(StrUtils.urlEncode(tip));
        }
        StrUtils.setClipboardString(tip);
        copyAnimation();
    }

    void copyAnimation() {
        if (promptPane == null) {
            return;
        }
        if (promptFadeIn != null) {
            promptFadeIn.stop();
        }
        if (promptFadeOut != null) {
            promptFadeOut.stop();
        }

        promptPane.setVisible(true);
        promptPane.setManaged(true);
        promptPane.setOpacity(0);
        promptPane.toFront();

        promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeIn.setFromValue(0);
        promptFadeIn.setToValue(1);

        promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeOut.setFromValue(1);
        promptFadeOut.setToValue(0);
        promptFadeOut.setDelay(Duration.seconds(0.5));

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();

        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }


    public static void main(String[] args) {

    }
}
