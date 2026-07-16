package com.potato.potatotool.controller.redTeam;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.CommandHelp;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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

import static com.potato.potatotool.controller.MainController.clipRect;

/**
 * @author Potato
 * @date 2023/3/21 17:12
 */
public class PaneCommandQuery {

    @FXML
    private StackPane sPane;

    @FXML
    private TextField question;

    @FXML
    private VBox vBoxBar;

    @FXML
    private Accordion accordionPane;
    @FXML
    private ScrollPane accordionScroll;

    @FXML
    private ListView listView;

    @FXML
    private Label toBack;

    @FXML
    private Pane promptPane;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    @FXML private ScrollPane commandScrollPane;
    @FXML private Label commandTitle;
    @FXML private VBox commandVariantBox;
    @FXML private VBox commandStatesBox;
    private boolean commandPanelVisible = false;

    CommandHelp commandHelp = new CommandHelp();

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
        
        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == ToStart.StartupPage.RED_COMMAND_QUERY_RESULT) {
            question.setText("ssh");
            searchInput(null);
            // 常驻 COMMAND 面板: 默认选中首条命令填充(对齐设计稿 populated 抽屉)
            Platform.runLater(this::selectFirstCommand);
        }
    }

    /** 常驻面板默认态: 找列表首条可解析的命令并填充右侧 COMMAND 面板 */
    private void selectFirstCommand() {
        try {
            for (Object o : listView.getItems()) {
                if (o instanceof HBox) {
                    javafx.collections.ObservableList<Node> ch = ((HBox) o).getChildrenUnmodifiable();
                    if (ch.size() > 1 && ch.get(1) instanceof Label) {
                        String key = ((Label) ch.get(1)).getText();
                        if (findCommandData(key, jsonData) != null) {
                            redirect(key);
                            return;
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
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
            accordionScroll.setVvalue(0);

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
                titledPane.getStyleClass().add("titledPane_"+recursionCount);
                titledPane.setExpanded(false);
                titledPane.setGraphic(titleLabel);  // 使用 Graphic 代替 setText (使用setText超出部分无法变为省略号)

                List innerContentList = createDirectoryNode(tmpValue, recursionCount);
                VBox vBox = new VBox(5);
                vBox.getStyleClass().add("vBox");
                vBox.getChildren().addAll(innerContentList);
                titledPane.setContent(vBox);

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

    public static boolean isAllNotLabelList(List<?> innerContent) {
        if (innerContent.isEmpty()) {
            return false;
        }

        for (Object item : innerContent) {
            if (!(item instanceof Label)) {
                return true; // 如果发现一个非 Label 的元素，返回 true
            }
        }

        return false;
    }

    private void addNode(JsonObject tmpJsonObj) {

        for (String key : tmpJsonObj.keySet()) {
            JsonArray linuxOrWindowsArray = tmpJsonObj.getAsJsonArray(key);

            VBox comandVBox = new VBox();
            comandVBox.setSpacing(5);
            comandVBox.getStyleClass().add("comandVBox");

            Label sysTitle = new Label(key);
            sysTitle.getStyleClass().add("sysTitle");
            if ("Windows".equals(key)) {
                sysTitle.getStyleClass().add("sysTitle-windows");
            } else if ("Linux".equals(key)) {
                sysTitle.getStyleClass().add("sysTitle-linux");
            }
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
                commandLabel.setPrefHeight(30);
                Button commandCopyBt = new Button(I18nUtils.getString("cmdquery.copy.button"));
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
                describeLabel.getStyleClass().add("cmd-describe");

                vBox.setAlignment(Pos.CENTER);
                vBox.getChildren().addAll(commandSP, describeLabel);
                vBox.prefWidthProperty().bind(listView.widthProperty().multiply(0.75));
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
                    break;
                }
            }
        }
        JsonObject cmdData = findCommandData(newKey, jsonData);
        if (cmdData != null) {
            populateCommandPanel(newKey, cmdData);
            showCommandPanel();
        }
    }

    private JsonObject findCommandData(String key, JsonObject root) {
        for (String k : root.keySet()) {
            JsonElement el = root.get(k);
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (k.equals(key) && (obj.has("Windows") || obj.has("Linux"))) {
                return obj;
            }
            JsonObject found = findCommandData(key, obj);
            if (found != null) return found;
        }
        return null;
    }

    private void populateCommandPanel(String key, JsonObject cmdData) {
        commandTitle.setText(key);
        commandVariantBox.getChildren().clear();
        commandStatesBox.getChildren().clear();

        for (String platform : cmdData.keySet()) {
            if (!cmdData.get(platform).isJsonArray()) continue;
            JsonArray arr = cmdData.getAsJsonArray(platform);
            if (arr.size() == 0) continue;

            VBox item = new VBox(3);
            item.getStyleClass().add("cmd-variant-item");
            JsonObject first = arr.get(0).getAsJsonObject();
            String cmd = first.get("command").getAsString();
            String desc = first.get("describe").getAsString();
            Label platformLbl = new Label(platform);
            platformLbl.getStyleClass().add("cmd-variant-platform");
            platformLbl.getStyleClass().add("Windows".equals(platform) ? "cmd-variant-windows" : "cmd-variant-linux");
            Label cmdLbl = new Label(cmd.length() > 28 ? cmd.substring(0, 28) + "…" : cmd);
            cmdLbl.getStyleClass().add("cmd-variant-cmd");
            cmdLbl.setWrapText(false);
            item.getChildren().addAll(platformLbl, cmdLbl);
            commandVariantBox.getChildren().add(item);

            // States
            HBox stateChip = new HBox(6);
            stateChip.setAlignment(Pos.CENTER_LEFT);
            stateChip.getStyleClass().add("cmd-state-chip");
            Region dot = new Region();
            dot.getStyleClass().add("cmd-state-dot");
            dot.getStyleClass().add("Windows".equals(platform) ? "cmd-state-dot-windows" : "cmd-state-dot-linux");
            Label stateLbl = new Label(platform);
            stateLbl.getStyleClass().add("cmd-state-label");
            stateChip.getChildren().addAll(dot, stateLbl);
            commandStatesBox.getChildren().add(stateChip);
        }
    }

    private void showCommandPanel() {
        // COMMAND 面板改为常驻并排(对齐设计稿),始终可见,仅确保就位。
        if (commandScrollPane == null) return;
        commandPanelVisible = true;
        commandScrollPane.setTranslateX(0);
    }

    private void hideCommandPanel() {
        // 常驻并排: 不再隐藏, 保持可见。
        if (commandScrollPane == null) return;
        commandPanelVisible = true;
        commandScrollPane.setTranslateX(0);
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

    @FXML
    public void goBack(MouseEvent mouseEvent) {
        question.setText("");
        searchInput(null);

        toBack.setVisible(false);
        toBack.setManaged(false);
    }

    @FXML
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
            Label tips = new Label(I18nUtils.getString("cmdquery.notfound", query));
            tips.setAlignment(Pos.CENTER);
            tips.setId("tipTitle");
            tips.setPrefWidth(sPane.getWidth() - 215);
            tips.setMinWidth(sPane.getWidth() - 215);
            tips.setMaxWidth(sPane.getWidth() - 215);
            contentObj.add(tips);

            // AI 命令建议分区（对齐 Penpot 修正版：独立标题行 + 虚线边框）
            javafx.scene.control.Label aiSectionLabel = new javafx.scene.control.Label(
                    I18nUtils.getString("cmdquery.ai.section.title", "AI 命令建议"));
            aiSectionLabel.getStyleClass().add("cmd-ai-section-label");
            VBox vBox = new VBox(6);
            vBox.getStyleClass().addAll("commandInfoHBox", "cmd-ai-suggest-box");
            Label aiCommands = new Label();
            aiCommands.setPrefWidth(sPane.getWidth() - 285);
            aiCommands.setWrapText(true);
            aiCommands.getStyleClass().add("cmd-ai-result");
            vBox.getChildren().addAll(aiSectionLabel, aiCommands);
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
            Thread commandQueryThread = new Thread(task);
            commandQueryThread.setDaemon(true);
            commandQueryThread.start();

            listView.setItems(contentObj);
        }else {
            initData(searchJsonObj);
        }
    }

    public void getCommandsByAi(String query, Object node) throws Exception {
        if (!(node instanceof Label)) {
            return;
        }

        Label label = (Label) node;
        Platform.runLater(() -> label.setText(I18nUtils.getString("cmdquery.ai.analyzing")));

        AiChatService aiService = new AiChatService();
        final StringBuilder buffer = new StringBuilder();
        final String[] errorHolder = new String[1];
        aiService.streamChat("请告诉我关于```" + query + "```的系统命令，并按系统分类给出常用命令、参数说明和适用场景。", event -> {
            if (event == null) {
                return;
            }
            switch (event.getType()) {
                case TOKEN:
                    String content = event.getContent();
                    if (content != null && !content.isEmpty()) {
                        buffer.append(content);
                        String text = buffer.toString();
                        Platform.runLater(() -> label.setText(text));
                    }
                    break;
                case ERROR:
                    errorHolder[0] = event.getContent();
                    break;
                case THINKING_TOKEN:
                case DONE:
                default:
                    break;
            }
        });

        if (buffer.length() == 0) {
            String message = errorHolder[0];
            if (message == null || message.trim().isEmpty()) {
                message = I18nUtils.getString("cmdquery.ai.empty");
            }
            final String finalMessage = message;
            Platform.runLater(() -> label.setText(finalMessage));
            return;
        }

        if (errorHolder[0] != null && !errorHolder[0].trim().isEmpty()) {
            final String finalText = buffer.toString() + "\n\n" + errorHolder[0];
            Platform.runLater(() -> label.setText(finalText));
        }
    }

}
