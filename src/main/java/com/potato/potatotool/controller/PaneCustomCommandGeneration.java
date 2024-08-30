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
        initData();
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
