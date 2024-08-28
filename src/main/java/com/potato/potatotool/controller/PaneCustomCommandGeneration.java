package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/3/21 17:12
 */
public class PaneCustomCommandGeneration {
    private static JsonObject jsonData;

    public void initialize() {
        initData();
    }

    //  初始化数据
    private static void initData() {
        try {
            String tmpJsonStr = getResourceString("commandHelp");
            jsonData = (new Gson()).fromJson(tmpJsonStr, JsonObject.class);
            // TODO 最后一个层级划分Windows命令和Linux命令

//            for (String key : jsonData.keySet()) {
//
//                JsonElement tmpValue = jsonData.get(key);
//
//                if(tmpValue.isJsonNull()){
//                    break;
//                }
//
//                if(tmpValue instanceof JsonObject){
//
//                    VBox vBox = new VBox(5);
//                    vBox.getStyleClass().add("vBox");
//
//                    JsonObject newTmpJsonObj = (JsonObject) tmpValue;
//                    for (String newKey : newTmpJsonObj.keySet()) {
//
//                        Label label = new Label(newKey);
//                        label.setPrefWidth(200);
//                        label.setOnMouseClicked(even->redirect(newKey));
//                        vBox.getChildren().add(label);
//
//                        addNode(newTmpJsonObj, newKey);
//                    }
//                    TitledPane titledPane = new TitledPane(key, vBox);
//                    titledPane.setPrefWidth(200);
//                    accordionPane.getPanes().add(titledPane);
//                } else if (tmpValue instanceof JsonArray) {
//
//                    TitledPane titledPane = new TitledPane();
//                    titledPane.setText(key);
//                    titledPane.getStyleClass().add("noContentTitlePane");
//                    titledPane.setPrefWidth(200);
//                    titledPane.setOnMouseClicked(even->redirect(key));
//                    accordionPane.getPanes().add(titledPane);
//
//                    addNode(tmpJsonObj, key);
//                }
//
//
//            }
//            listView.setItems(contentObj);
        }catch (Exception e){
            e.printStackTrace();
        }

//        TitledPane titledPane = new TitledPane();
//        titledPane.setText("渊龙导航");
//        titledPane.setMouseTransparent(true);   // 禁止点击事件
//        titledPane.getStyleClass().add("noContentTitlePane");
//        titledPane.setPrefWidth(200);
//        accordionPane.getPanes().add(titledPane);
    }

    public void searchInput(MouseEvent mouseEvent) {
    }

    // 搜索方法，支持双引号、OR逻辑、开头/结尾匹配，不区分大小写
    public static JsonObject searchCommands(String query) {
        // 分割查询条件
        List<SearchTerm> searchTerms = parseQuery(query);

        // 遍历JSON进行搜索
        JsonObject results = searchJson(jsonData, searchTerms);

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

    // JSON搜索逻辑
    private static JsonObject searchJson(JsonObject jsonObject, List<SearchTerm> searchTerms) {
        JsonObject result = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonElement value = entry.getValue();

            // 递归搜索子对象
            if (matchesQuery(key, searchTerms)) {
                result.add(entry.getKey(), value);
            } else if (value.isJsonObject()) {
                JsonObject subResult = searchJson(value.getAsJsonObject(), searchTerms);
                if (!subResult.entrySet().isEmpty()) {
                    result.add(entry.getKey(), subResult);
                }
            } else if (value.isJsonArray()) {
                // 处理数组中的command和describe字段
                JsonArray matchedArray = new JsonArray();
                value.getAsJsonArray().forEach(commandInfo -> {
                    JsonObject commandObj = commandInfo.getAsJsonObject();
                    String command = commandObj.get("command").getAsString().toLowerCase();
                    String describe = commandObj.get("describe").getAsString().toLowerCase();

                    // 分别对 command 和 describe 进行匹配
                    boolean commandMatches = matchesQuery(command, searchTerms);
                    boolean describeMatches = matchesQuery(describe, searchTerms);

                    if (commandMatches || describeMatches) {
                        matchedArray.add(commandObj.get("command").getAsString() + " - " + commandObj.get("describe").getAsString());
                    }
                });

                // 如果有匹配的数组项，则保留该数组
                if (matchedArray.size() > 0) {
                    result.add(entry.getKey(), matchedArray);
                }
            }
        }

        return result;
    }

    // 匹配查询条件，不区分大小写
    private static boolean matchesQuery(String text, List<SearchTerm> searchTerms) {
        boolean orMatched = false;

        for (SearchTerm term : searchTerms) {
            if (term.isExactMatch) {
                // 必须存在的关键词（支持开头和结尾匹配）
                if (!matchesExact(term.word, text)) {
                    return false; // 如果某个必须匹配的关键词不存在，返回false
                }
            } else {
                if (matchesExact(term.word, text)) {
                    orMatched = true; // 只要有一个匹配即可
                }
            }
        }

        // 如果有必须匹配的关键词，OR逻辑则可有可无；否则必须有至少一个OR匹配
        return searchTerms.stream().anyMatch(t -> t.isExactMatch) || orMatched;
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

    // SearchTerm类，用于保存关键词和其匹配逻辑
    private static class SearchTerm {
        String word;
        boolean isExactMatch;

        SearchTerm(String word, boolean isExactMatch) {
            this.word = word;
            this.isExactMatch = isExactMatch;
        }
    }

    public static void main(String[] args) {
        initData();
        // 示例1：单个关键词必须存在
        JsonObject results1 = searchCommands("我的");
        System.out.println("搜索结果1: " + results1);

        // 示例2：模糊匹配，双引号必须匹配
        JsonObject results2 = searchCommands("我的 \"祖国\"");
        System.out.println("搜索结果2: " + results2);

        // 示例3：多个关键词，OR逻辑
        JsonObject results3 = searchCommands("我的 祖国");
        System.out.println("搜索结果3: " + results3);

        // 示例4：以关键词开头
        JsonObject results4 = searchCommands("^我的");
        System.out.println("搜索结果4: " + results4);

        // 示例5：以关键词结尾
        JsonObject results5 = searchCommands("祖国$");
        System.out.println("搜索结果5: " + results5);

        // 示例6：大小写
        JsonObject results6 = searchCommands("查看安装驱动 \"lshw\"");
        System.out.println("搜索结果6: " + results6);
    }

}
