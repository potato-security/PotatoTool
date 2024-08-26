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

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/3/21 17:12
 */
public class PaneCustomCommandGeneration {

    public void initialize() {
        initData();
    }

    //  初始化数据
    private void initData() {
        try {
            String tmpJsonStr = getResourceString("commandHelp");
            JsonObject tmpJsonObj = (new Gson()).fromJson(tmpJsonStr, JsonObject.class);
            // TODO 最后一个层级划分Windows命令和Linux命令

            for (String key : tmpJsonObj.keySet()) {

                JsonElement tmpValue = tmpJsonObj.get(key);

                if(tmpValue.isJsonNull()){
                    break;
                }

                if(tmpValue instanceof JsonObject){

                    VBox vBox = new VBox(5);
                    vBox.getStyleClass().add("vBox");

                    JsonObject newTmpJsonObj = (JsonObject) tmpValue;
                    for (String newKey : newTmpJsonObj.keySet()) {

                        Label label = new Label(newKey);
                        label.setPrefWidth(200);
                        label.setOnMouseClicked(even->redirect(newKey));
                        vBox.getChildren().add(label);

                        addNode(newTmpJsonObj, newKey);
                    }
                    TitledPane titledPane = new TitledPane(key, vBox);
                    titledPane.setPrefWidth(200);
                    accordionPane.getPanes().add(titledPane);
                } else if (tmpValue instanceof JsonArray) {

                    TitledPane titledPane = new TitledPane();
                    titledPane.setText(key);
                    titledPane.getStyleClass().add("noContentTitlePane");
                    titledPane.setPrefWidth(200);
                    titledPane.setOnMouseClicked(even->redirect(key));
                    accordionPane.getPanes().add(titledPane);

                    addNode(tmpJsonObj, key);
                }


            }
            listView.setItems(contentObj);
        }catch (Exception e){
            e.printStackTrace();
        }

        TitledPane titledPane = new TitledPane();
        titledPane.setText("渊龙导航");
        titledPane.setMouseTransparent(true);   // 禁止点击事件
        titledPane.getStyleClass().add("noContentTitlePane");
        titledPane.setPrefWidth(200);
        accordionPane.getPanes().add(titledPane);
    }

    public void searchInput(MouseEvent mouseEvent) {
    }
}
