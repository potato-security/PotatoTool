package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
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

    private static JsonObject jsonData = new JsonObject();
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
        try {
            if(jsonData.size() == 0) {
                String tmpJsonStr = getResourceString("commandHelp");
                jsonData = (new Gson()).fromJson(tmpJsonStr, JsonObject.class);
            }

            initData(jsonData);
        }catch (Exception e){
            e.printStackTrace();
        }
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

    public void searchInput(MouseEvent mouseEvent) {
        String query = question.getText().trim();

        JsonObject searchJsonObj = new JsonObject();
        if(query.isEmpty()){
            searchJsonObj = jsonData;
            toBack.setVisible(false);
            toBack.setManaged(false);
        }else {
            searchJsonObj = searchCommands(query);
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

    // 搜索方法，支持双引号、OR逻辑、开头/结尾匹配，不区分大小写
    public static JsonObject searchCommands(String query) {
        // 分割查询条件
        List<SearchTerm> searchTerms = parseQuery(query);

        // 遍历JSON进行搜索，添加排序标记
        JsonObject results = searchJson(jsonData, searchTerms);

        // 递归处理JsonObject，进行排序、剔除无关元素、剔除排序标记符
        results = processJsonObjectRecursively(results);

        return results;
    }

    // 分析查询条件
    private static List<SearchTerm> parseQuery(String query) {
        List<SearchTerm> searchTerms = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]+)\"|\\S+").matcher(query);

        while (matcher.find()) {
            String term = matcher.group();
            boolean isExactMatch = false;
            if (term.startsWith("\"") && term.endsWith("\"")) {
                term = term.substring(1, term.length() - 1); // 去除双引号
                isExactMatch = true; // 必须存在的关键词
            }
            searchTerms.add(new SearchTerm(term.toLowerCase(), isExactMatch));
        }
        return searchTerms;
    }

    private static JsonObject searchJson(JsonObject jsonObject, List<SearchTerm> searchTerms) {
        JsonObject result = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonElement value = entry.getValue();

            int keyCount = matchesQuery(key, searchTerms);

            JsonElement originalValue = new JsonObject();

            // 递归搜索子对象
            if (value.isJsonObject()) {
                originalValue = searchJson(value.getAsJsonObject(), searchTerms);
            } else if (value.isJsonArray()) {
                // 处理数组中的command和describe字段
                JsonArray matchedArray = new JsonArray();
                value.getAsJsonArray().forEach(commandInfo -> {
                    JsonObject commandObj = commandInfo.getAsJsonObject();
                    String command = commandObj.get("command").getAsString().toLowerCase();
                    String describe = commandObj.get("describe").getAsString().toLowerCase();
                    String commandInfoStr = command + " " + describe;

                    // 合并匹配
                    int allMatchesKeyCount = matchesQuery(commandInfoStr, searchTerms);
                    // 对 command 和 describe 分别进行匹配（为兼容^以及$检索语法）
                    int commandMatchesKeyCount = matchesQuery(command, searchTerms);
                    int describeMatchesKeyCount = matchesQuery(describe, searchTerms);

                    int matchesKeyCount = Math.max(allMatchesKeyCount, Math.max(commandMatchesKeyCount, describeMatchesKeyCount));

                    JsonObject newValue = new JsonObject();
                    newValue.add("$$value$$", commandInfo);
                    newValue.addProperty("$$count$$", matchesKeyCount);
                    newValue.addProperty("$$valueMaxCount$$", matchesKeyCount);

                    matchedArray.add(newValue);

                });

                originalValue = matchedArray;
            }

            JsonObject newValue = new JsonObject();
            newValue.add("$$value$$", originalValue);
            newValue.addProperty("$$count$$", keyCount);
            newValue.addProperty("$$valueMaxCount$$", findMaxCount(originalValue));
            result.add(entry.getKey(), newValue);
        }

        return result;
    }

    private static int findMaxCount(JsonElement jsonElement) {
        int maxCount = Integer.MIN_VALUE;

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                JsonElement element = jsonObject.get(key);

                if (element.isJsonObject() || element.isJsonArray()) {
                    maxCount = Math.max(maxCount, findMaxCount(element));
                } else if (key.equals("$$count$$")) {
                    maxCount = Math.max(maxCount, element.getAsInt());
                }
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                if (element.isJsonObject() || element.isJsonArray()) {
                    maxCount = Math.max(maxCount, findMaxCount(element));
                }
            }
        }

        return maxCount;
    }

    // 匹配查询条件，不区分大小写
    private static int matchesQuery(String text, List<SearchTerm> searchTerms) {
        int keywordMatchCount = 0;

        for (SearchTerm term : searchTerms) {
            if (term.isExactMatch) {
                // 必须存在的关键词（支持开头和结尾匹配）
                if (matchesExact(term.word, text)) {
                    keywordMatchCount++; // 匹配到必须存在的关键词，增加计数
                } else {
                    return 0; // 如果某个必须匹配的关键词不存在，返回0
                }
            } else {
                if (matchesExact(term.word, text)) {
                    keywordMatchCount++; // 匹配到非必须的关键词，增加计数
                }
            }
        }

        return keywordMatchCount;
    }

    // 支持^xxx和xxx$的开头/结尾匹配
    private static boolean matchesExact(String term, String text) {
        if (term.startsWith("^")) {
            return text.startsWith(term.substring(1));
        } else if (term.endsWith("$")) {
            return text.endsWith(term.substring(0, term.length() - 1));
        } else {
            return text.contains(term); // 模糊匹配
        }
    }

    public void goBack(MouseEvent mouseEvent) {
        question.setText("");
        searchInput(null);

        toBack.setVisible(false);
        toBack.setManaged(false);
    }

    // SearchTerm类，用于保存关键词和其匹配逻辑
    private static class SearchTerm {
        String word;
        boolean isExactMatch;

        SearchTerm(String word, boolean isExactMatch) {
            this.word = word;
            this.isExactMatch = isExactMatch;
        }
    }

    // 进行排序、剔除无关元素、剔除排序标记符
    private static JsonObject processJsonObjectRecursively(JsonObject jsonObject) {
        // 创建一个键值对集合，用于存储原始的键值对
        List<Map.Entry<String, JsonElement>> entryList = new ArrayList<>(jsonObject.entrySet());

        // 根据 $$count$$ 值进行排序
        entryList.sort((e1, e2) -> {
            JsonObject obj1 = e1.getValue().isJsonObject() ? e1.getValue().getAsJsonObject() : null;
            JsonObject obj2 = e2.getValue().isJsonObject() ? e2.getValue().getAsJsonObject() : null;

            // 获取 $$count$$ 值
            int count1 = obj1 != null && obj1.has("$$count$$") ? obj1.get("$$count$$").getAsInt() : 0;
            int count2 = obj2 != null && obj2.has("$$count$$") ? obj2.get("$$count$$").getAsInt() : 0;

            // 如果 $$count$$ 相等，继续比较 $$valueMaxCount$$
            if (count1 == count2) {
                int valueMaxCount1 = obj1 != null && obj1.has("$$valueMaxCount$$") ? obj1.get("$$valueMaxCount$$").getAsInt() : 0;
                int valueMaxCount2 = obj2 != null && obj2.has("$$valueMaxCount$$") ? obj2.get("$$valueMaxCount$$").getAsInt() : 0;
                return Integer.compare(valueMaxCount2, valueMaxCount1);  // 按 $$valueMaxCount$$ 值降序排列
            }

            // 按降序排列
            return Integer.compare(count2, count1);
        });

        // 创建一个新的 JsonObject 用于存放排序后的结果
        JsonObject sortedJsonObject = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : entryList) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            JsonObject innerObject = value.getAsJsonObject();

            // 并判断是否要移除节点
            int count = innerObject.get("$$count$$").getAsInt();
            int valueMaxCount = innerObject.get("$$valueMaxCount$$").getAsInt();

            // 如果 $$count$$ 和 $$valueMaxCount$$ 都为 0，并且不是数组情况，跳过该节点
            if (count == 0 && valueMaxCount == 0 && !innerObject.get("$$value$$").isJsonArray()) {
                continue;
            }

            // 剔除排序标签，value=$$value$$
            JsonElement innerValue = innerObject.get("$$value$$");

            // 递归地处理嵌套的 JsonObject、JsonArray
            if(innerValue == null) {
                // value层级不存在key为$$value$$，直接递归value
                sortedJsonObject.add(key, processJsonObjectRecursively(value.getAsJsonObject()));
            }else {
                // value层级存在key为$$value$$，递归$$value$$的值
                if(innerValue.isJsonObject()) {
                    sortedJsonObject.add(key, processJsonObjectRecursively(innerValue.getAsJsonObject()));
                }else if(innerValue.isJsonArray()){
                    JsonArray sortedArray = sortJsonArrayByCount(innerValue.getAsJsonArray());
                    sortedJsonObject.add(key, sortedArray);
                }
            }
        }

        return sortedJsonObject;
    }

    private static JsonArray sortJsonArrayByCount(JsonArray jsonArray) {
        // 创建一个列表来存储 JsonArray 中的元素
        List<JsonElement> elementList = new ArrayList<>();
        jsonArray.forEach(elementList::add);

        // 对 JsonArray 进行排序
        elementList.sort((e1, e2) -> {
            JsonObject obj1 = e1.isJsonObject() ? e1.getAsJsonObject() : null;
            JsonObject obj2 = e2.isJsonObject() ? e2.getAsJsonObject() : null;

            // 获取 $$count$$ 值
            int count1 = obj1 != null && obj1.has("$$count$$") ? obj1.get("$$count$$").getAsInt() : 0;
            int count2 = obj2 != null && obj2.has("$$count$$") ? obj2.get("$$count$$").getAsInt() : 0;

            // 按 $$count$$ 值降序排列
            return Integer.compare(count2, count1);
        });

        // 创建排序后的 JsonArray，剔除排序标签
        JsonArray sortedArray = new JsonArray();
        for (JsonElement element : elementList) {
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                sortedArray.add(obj.get("$$value$$"));
            }
        }

        return sortedArray;
    }

    public static void main(String[] args) {
        new PaneCustomCommandGeneration().initData();
        // 示例1：单个关键词必须存在
        JsonObject results1 = searchCommands("ssh");
        System.out.println("搜索结果1: " + results1);

        // 示例2：模糊匹配，双引号必须匹配
        JsonObject results2 = searchCommands("ssh \"20020\"");
        System.out.println("搜索结果2: " + results2);

        // 示例3：多个关键词，OR逻辑
        JsonObject results3 = searchCommands("横向移动 mmcexec psexec mimikatz查看当前密码");
        System.out.println("搜索结果3: " + results3);

        // 示例4：以关键词开头
        JsonObject results4 = searchCommands("^ssh");
        System.out.println("搜索结果4: " + results4);

        // 示例5：以关键词结尾
        JsonObject results5 = searchCommands("127.0.0.1$");
        System.out.println("搜索结果5: " + results5);

        // 示例6：大小写
        JsonObject results6 = searchCommands("查看安装驱动 \"lSHw\"");
        System.out.println("搜索结果6: " + results6);

    }

}
