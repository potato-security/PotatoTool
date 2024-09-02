package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.content.redTeam.commandHelp;
import com.potato.potatotool.utils.aiUtil;
import com.potato.potatotool.utils.codeAnalyzerUtils;
import com.potato.potatotool.utils.strUtils;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/3/21 17:12
 */
public class PaneCustomCommandGeneration {

    @FXML
    private StackPane sPane;

    @FXML
    private TextField question;

    @FXML
    private VBox vBoxBar;

    @FXML
    private Accordion accordionPane;

    @FXML
    private ListView listView;

    @FXML
    private Label toBack;

    @FXML
    private Pane promptPane;

    commandHelp commandHelp = new commandHelp();

    private JsonObject jsonData = commandHelp.jsonData;
    private final ObservableList<Node> contentObj = FXCollections.observableArrayList();

    public void initialize() {
        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initData();

        listenSearch();
    }

    //  监听输入时回车
    private void listenSearch() {
        question.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                searchInput(null);
            }
        });
    }

    // 初始化数据
    private void initData() {
        initData(jsonData);
    }

    // 初始化搜索结果
    private void initData(JsonObject searchJsonObj) {
        try {
            contentObj.clear();

            List directoryNodeList= createDirectoryNode(searchJsonObj);  // 递归创建目录节点

            accordionPane.getPanes().addAll(directoryNodeList);

            listView.setItems(contentObj);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // 不带递归次数参数的重载方法，默认从 0 开始
    private List createDirectoryNode(JsonObject jsonData) {
        return createDirectoryNode(jsonData, 0); // 默认从0开始递归
    }

    private List createDirectoryNode(JsonObject jsonData, int recursionCount) {
        recursionCount++;
        List ObjList = new ArrayList<>();

        int keyIndex = 0;
        for (String key : jsonData.keySet()) {
            keyIndex++;

            // 右侧添加对应标题
            HBox hBoxTitle = new HBox();
            hBoxTitle.setSpacing(10);
            if(keyIndex == 1) {
                hBoxTitle.getStyleClass().add("tipFirstHBox");
            }else {
                hBoxTitle.getStyleClass().add("tipHBox");
            }
            Label labelTitle = new Label(key);
            labelTitle.setAlignment(Pos.CENTER_LEFT);
//            Region regionAdd = new Region();
//            regionAdd.getStyleClass().add("addIcon");
//            Tooltip tooltips = new Tooltip("修改内容");
//            Tooltip.install(regionAdd, tooltips);
//            regionAdd.setOnMouseClicked(even->modify(even));
            if(recursionCount == 1){
                Region regionTitle = new Region();
                regionTitle.getStyleClass().add("tipIcon");
                hBoxTitle.getChildren().add(regionTitle);
            }else {
                HBox regionTitleHbox =new HBox();
                for (int i = 0; i < recursionCount; i++) {
                    Region regionTitle = new Region();
                    regionTitle.getStyleClass().add("tipNextIcon");
                    regionTitleHbox.getChildren().add(regionTitle);
                }
                hBoxTitle.getChildren().add(regionTitleHbox);
                regionTitleHbox.setAlignment(Pos.CENTER_LEFT);
            }
            hBoxTitle.getChildren().add(labelTitle);
            hBoxTitle.setAlignment(Pos.CENTER_LEFT);
            contentObj.add(hBoxTitle);

            JsonObject tmpValue = jsonData.getAsJsonObject(key);

            if(jsonData.get(key).isJsonArray()){
                return ObjList;
            }
            if(tmpValue.isJsonNull()){
                break;
            }

            if((tmpValue.has("Windows") && tmpValue.get("Windows").isJsonArray())
                    || (tmpValue.has("Linux") && tmpValue.get("Linux").isJsonArray())
            ) {
                Label label = new Label(key);
                label.setPrefWidth(180);
                label.setOnMouseClicked(even->redirect(key));
                Tooltip tooltip = new Tooltip(key);
                Tooltip.install(label, tooltip);

                addNode(tmpValue);

                ObjList.add(label);
            }else {

                Label titleLabel = new Label(key);
                titleLabel.setPrefWidth(150);
                TitledPane titledPane = new TitledPane();
                titledPane.setGraphic(titleLabel);  // 使用 Graphic 代替 setText (使用setText超出部分无法变为省略号)

                List innerContentList = createDirectoryNode(tmpValue, recursionCount);
                if(isLabelList(innerContentList)) {
                    VBox vBox = new VBox(5);
                    vBox.getStyleClass().add("vBox");
                    vBox.getChildren().addAll(innerContentList);
                    titledPane.setContent(vBox);
                }else {
                    Accordion innerAccordion = new Accordion();
                    innerAccordion.getPanes().addAll(innerContentList);
                    titledPane.setContent(innerAccordion);
                }

                titleLabel.setOnMouseClicked(even->redirect(key));
                Tooltip tooltip = new Tooltip(key);
                Tooltip.install(titleLabel, tooltip);

                ObjList.add(titledPane);

            }
        }
        return ObjList;
    }

    public static boolean isLabelList(List<?> innerContent) {
        boolean isLabelList = false;

        if (innerContent.size() > 0 && innerContent.get(0) instanceof Label) {
            isLabelList = true;
        }

        return isLabelList;
    }

    private void addNode(JsonObject tmpJsonObj) {

        for (String key : tmpJsonObj.keySet()) {
            JsonArray linuxOrWindowsArray = tmpJsonObj.getAsJsonArray(key);

            VBox comandVBox = new VBox();
            comandVBox.setSpacing(5);
            comandVBox.getStyleClass().add("comandVBox");

            Label sysTitle = new Label(key);
            sysTitle.getStyleClass().add("sysTitle");
            contentObj.add(sysTitle);

            VBox vBoxs = new VBox();
            vBoxs.setSpacing(15);
            linuxOrWindowsArray.forEach(commandInfo -> {
                JsonObject commandObj = commandInfo.getAsJsonObject();
                String command = commandObj.get("command").getAsString();
                String describe = commandObj.get("describe").getAsString();

                VBox vBox = new VBox();
                vBox.getStyleClass().add("commandInfoHBox");

                StackPane commandSP = new StackPane();
                RXLineButton commandLabel = new RXLineButton(command);
                commandLabel.setWrapText(true);
                commandLabel.setPrefHeight(25);
                Button commandCopyBt = new Button("复制");
                commandCopyBt.getStyleClass().add("copyButton");
                commandCopyBt.setVisible(false);
                commandCopyBt.setManaged(false);
                commandCopyBt.setOnAction(event -> copyTip(event, commandLabel));
                commandSP.getChildren().addAll(commandLabel, commandCopyBt);
                commandSP.setAlignment(commandCopyBt, Pos.CENTER_RIGHT);
                commandSP.setOnMouseEntered(event -> handleMouseEntered(event, commandCopyBt));
                commandSP.setOnMouseExited(event -> handleMouseExited(event, commandCopyBt));

                Label describeLabel = new Label(describe);
                describeLabel.setWrapText(true);
                describeLabel.setStyle("-fx-text-fill: -main-linenoText-color;");

                vBox.setAlignment(Pos.CENTER);
                vBox.getChildren().addAll(commandSP, describeLabel);
                vBoxs.getChildren().add(vBox);
            });
            comandVBox.getChildren().addAll(sysTitle, vBoxs);

            contentObj.add(comandVBox);
        }

    }

    //  左侧导航转跳逻辑
    private void redirect(String newKey) {
        for (int i = 0; i < listView.getItems().size(); i++) {
            Node node = (Node) listView.getItems().get(i);
            if (node instanceof HBox) {
                String key = ((Label) ((HBox) node).getChildrenUnmodifiable().get(1)).getText();
                if(key.equals(newKey)){
                    listView.scrollTo(i);
                    return;
                }
            }
        }
    }

    private void handleMouseEntered(MouseEvent event, Button tipTitleCopy) {
        tipTitleCopy.setVisible(true);
        tipTitleCopy.setManaged(true);
    }

    private void handleMouseExited(MouseEvent event, Button tipTitleCopy) {
        tipTitleCopy.setVisible(false);
        tipTitleCopy.setManaged(false);
    }

    private void copyTip(ActionEvent event, RXLineButton tipTitle){
        String tip = tipTitle.getText();
        strUtils.setClipboardString(tip);
        copyAnimation();
    }

    void copyAnimation() {
        // 显示提示组件
        promptPane.setVisible(true);
        promptPane.setManaged(true);

        // 创建渐入动画
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // 创建渐出动画
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(0.5)); // 延迟1秒执行渐出动画

        // 播放渐入动画，完成后播放渐出动画
        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();

        fadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }

    public void goBack(MouseEvent mouseEvent) {
        question.setText("");
        searchInput(null);

        toBack.setVisible(false);
        toBack.setManaged(false);
    }

    public void searchInput(MouseEvent mouseEvent) {
        String query = question.getText().trim();

        JsonObject searchJsonObj = new JsonObject();
        if(query.isEmpty()){
            searchJsonObj = jsonData;
            toBack.setVisible(false);
            toBack.setManaged(false);
        }else {
            searchJsonObj = commandHelp.searchCommands(query);
            toBack.setVisible(true);
            toBack.setManaged(true);
        }

        accordionPane.getPanes().clear();
        listView.getItems().clear();

        if(searchJsonObj.size() == 0){
            Label tips = new Label("库中未找到有关\"" + query + "\"的命令，AI帮你分析……");
            tips.setAlignment(Pos.CENTER);
            tips.setId("tipTitle");
            tips.setPrefWidth(sPane.getWidth() - 215);
            tips.setMinWidth(sPane.getWidth() - 215);
            tips.setMaxWidth(sPane.getWidth() - 215);
            contentObj.add(tips);

            VBox vBox = new VBox();
            vBox.getStyleClass().add("commandInfoHBox");
            Label aiCommands = new Label();
            aiCommands.setPrefWidth(sPane.getWidth() - 265);
            aiCommands.setWrapText(true);
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().add(aiCommands);
            contentObj.add(vBox);

            // 另起线程调用AI接口
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    getCommandsByAi(query, aiCommands);
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable error = task.getException();
                error.printStackTrace();
            });
            new Thread(task).start();

            listView.setItems(contentObj);
        }else {
            initData(searchJsonObj);
        }
    }

    public void getCommandsByAi(String query, Object node) throws Exception {
        aiUtil aiObj = new aiUtil();
        aiObj.askAi("请告诉我关于```" + query + "```的系统命令", node);
    }

}
