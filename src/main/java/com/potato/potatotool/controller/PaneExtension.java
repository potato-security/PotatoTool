package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.Util;
import javafx.application.HostServices;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneExtension {

    @FXML
    private StackPane sPane;

    @FXML
    private VBox vBoxBar;

    private final ObservableList<Node> contentObj = FXCollections.observableArrayList();

    @FXML
    private ListView listView;

    @FXML
    private Accordion accordionPane;

    public void initialize() {

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initData();


    }

    //  初始化数据
    private void initData() {
        try {
            JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Extension");

            if(tmpJsonObj == null){
                String tmpDataJsonStr = getResourceString("config");
                tmpJsonObj = (JsonObject) (new Gson()).fromJson(tmpDataJsonStr, JsonObject.class).get("Extension");
            }

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
                } else if (tmpValue instanceof JsonArray || tmpValue.isJsonNull()) {

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


    //  重新初始化数据
    public void reloadData() {
        accordionPane.getPanes().clear();
        listView.getItems().clear();

        initData();
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

    //  listView中寻找某元素的对应索引
    public int listViewFindNode(ListView listView, Node node){
        for (int i = 0; i < listView.getItems().size(); i++) {
            Node item = (Node) listView.getItems().get(i);
            if (item.equals(node)) {
                return i;
            }
        }
        return -1;
    }

    //  添加元素节点
    void addNode(JsonObject tmpJsonObj, String key){
        JsonArray tmpArrayData = (JsonArray) tmpJsonObj.get(key);

        HBox hBoxTitle = new HBox();
        hBoxTitle.setSpacing(10);
        hBoxTitle.getStyleClass().add("tipHBox");
        Region regionTitle = new Region();
        regionTitle.getStyleClass().add("tipIcon");
        Label labelTitle = new Label(key);
        Region regionAdd = new Region();
        regionAdd.getStyleClass().add("addIcon");
        Tooltip tooltip = new Tooltip("添加子元素");
        Tooltip.install(regionAdd, tooltip);
        regionAdd.setOnMouseClicked(even->addDialog(even));
        hBoxTitle.getChildren().addAll(regionTitle, labelTitle, regionAdd);
        hBoxTitle.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        contentObj.add(hBoxTitle);

        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setStyle("-fx-padding: 10");
        flowPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        flowPane.setPrefWidth(sPane.getWidth() - 240);

        for (int i = 0; i < tmpArrayData.size(); i++) {
            JsonElement element = tmpArrayData.get(i);
            JsonObject tmpDictData = element.getAsJsonObject();
            String title = tmpDictData.get("title").getAsString();
            String describe = tmpDictData.get("describe").getAsString();
            String content = tmpDictData.get("content").getAsString();
            String type = tmpDictData.get("type").getAsString();
            String icon = tmpDictData.get("icon").getAsString();

            HBox hBox = new HBox();
            hBox.getStyleClass().add("cellHBox");
            hBox.setAlignment(javafx.geometry.Pos.CENTER);


            ImageView imageView = new ImageView();

            try {
                Image image = null;
                if(icon.equals("") && type.equals("cmd")){
                    icon = "/img/bar/cmd.png";
                }else if(icon.equals("") && type.equals("web")){
                    icon = "/img/bar/web.png";
                }
                if(icon.startsWith("/img/bar/")){
                    try (InputStream is = getClass().getResourceAsStream(icon)) {
                        image = new Image(is);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    image = new Image(new File(icon).toURI().toString());
                    if(image.isError()){
                        if(type.equals("cmd")){
                            icon = "/img/bar/cmd.png";
                        }else if(type.equals("web")){
                            icon = "/img/bar/web.png";
                        }
                        if(icon.startsWith("/img/bar/")) {
                            try (InputStream is = getClass().getResourceAsStream(icon)) {
                                image = new Image(is);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                imageView.setImage(image);
            }catch (Exception e){
                System.out.println(icon+"加载失败，该图片存在问题");
                e.printStackTrace();
            }

            imageView.setFitHeight(35.0);
            imageView.setFitWidth(35.0);
            SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
            Rectangle clip = clipRect(
                    imageView, arcProperty
            );
            imageView.setClip(clip);
            imageView.setPreserveRatio(true);
            imageView.setPickOnBounds(true);

            VBox vBox = new VBox();
            vBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label label1 = new Label(title);
            Label label2 = new Label(describe);
            label2.getStyleClass().add("tips");
            vBox.getChildren().addAll(label1, label2);


            hBox.getChildren().addAll(imageView, vBox);
            hBox.setSpacing(10);
            hBox.setPrefWidth(200);
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setCursor(Cursor.HAND);
            hBox.setOnMouseClicked(event -> {
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws IOException{
                        start(type, content);
                        return null;
                    }
                };
                task.setOnFailed(e -> {
                    Throwable error = task.getException();
                    error.printStackTrace();
                });
                new Thread(task).start();
            });

            VBox regionVBox = new VBox();
            regionVBox.setMaxWidth(20);
            regionVBox.setTranslateX(-5);
            regionVBox.setTranslateY(10);
            regionVBox.setSpacing(15);
            Region regionChange = new Region();
            regionChange.getStyleClass().add("changeIcon");
            Tooltip tooltipChange = new Tooltip("修改");
            Tooltip.install(regionChange, tooltipChange);
            regionChange.setOnMouseClicked(even->changeDialog(even));

            Region regionDel = new Region();
            regionDel.getStyleClass().add("delIcon");
            Tooltip tooltipDel = new Tooltip("删除");
            Tooltip.install(regionDel, tooltipDel);
            regionDel.setOnMouseClicked(even->delDialog(even));
            regionVBox.getChildren().addAll(regionChange, regionDel);
            regionVBox.getStyleClass().add("regionVBox");
            StackPane stackPane = new StackPane(hBox,regionVBox);
            stackPane.setAlignment(regionVBox, Pos.CENTER_RIGHT);
            stackPane.getStyleClass().add("stackPane");
            Tooltip tooltipData = new Tooltip(describe+"\n"+type+"："+content);
            Tooltip.install(hBox, tooltipData);

            flowPane.getChildren().add(stackPane);
        }
        contentObj.add(flowPane);
    }


    //  点击元素事件
    public void start(String type, String content){

        if(content.isEmpty()) return;

        if(type.equals("cmd")){
            String[] command = content.split("\\s+");
            try {
                new ProcessBuilder(command).start();
            } catch (Exception e) {
                System.out.println(command);
                e.printStackTrace();
            }
        }else if(type.equals("web")){
            HostServices services = MainApplication.letGetHostServices();
            services.showDocument(content);
        }
    }


    //  删除功能窗口
    private void delDialog(MouseEvent even) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(sPane.getScene().getWindow());
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initStyle(StageStyle.UTILITY);
        alert.getDialogPane().getStylesheets().add(Util.getResourceUrl("/css/common.css"));
        alert.setTitle("删除");
        alert.setHeaderText("您确定要删除么？");
        alert.setContentText("请再次确认");

        Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e->{
            Region region = (Region) even.getSource();
            Parent flowpane = region.getParent();
            Parent vbox = flowpane.getParent();
            Parent stackPane = vbox.getParent();
            ListView contentObj = (ListView) stackPane.getParent().getParent().getParent().getParent().getParent(); //  ListView组件内部有多层嵌套

            HBox hbox = (HBox) vbox.getChildrenUnmodifiable().get(0);
            Label labelTitle = (Label) ((Parent)hbox.getChildrenUnmodifiable().get(1)).getChildrenUnmodifiable().get(0);
            Label labelDescribe = (Label) ((Parent)hbox.getChildrenUnmodifiable().get(1)).getChildrenUnmodifiable().get(1);

            int stackPaneIndex = listViewFindNode(contentObj, stackPane);
            if(stackPaneIndex==-1) return;
            Label labelKey = (Label) ((HBox) contentObj.getItems().get(stackPaneIndex-1)).getChildrenUnmodifiable().get(1);

            String key = labelKey.getText();
            String title = labelTitle.getText();
            String describe = labelDescribe.getText();

            JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Extension");
            JsonElement targetValue = Constants.findKey(tmpJsonObj, key);
            if (targetValue != null && targetValue.isJsonArray()) {
                JsonArray targetArray = (JsonArray) targetValue;
                for (int i = 0; i < targetArray.size(); i++) {
                    JsonObject obj = (JsonObject) targetArray.get(i);
                    if (obj.get("title").getAsString().equals(title) && obj.get("describe").getAsString().equals(describe)) {
                        targetArray.remove(i);
                    }
                }
            }
            Constants.saveConfig("Extension", tmpJsonObj);
            reloadData();
        });
        alert.show();
    }


    //  修改功能窗口
    private void changeDialog(MouseEvent even) {
        Region region = (Region) even.getSource();
        Parent flowpane = region.getParent();
        Parent vbox = flowpane.getParent();
        Parent stackPane = vbox.getParent();
        ListView contentObj = (ListView) stackPane.getParent().getParent().getParent().getParent().getParent(); //  ListView组件内部有多层嵌套

        HBox hbox = (HBox) vbox.getChildrenUnmodifiable().get(0);
        Label labelTitle = (Label) ((Parent)hbox.getChildrenUnmodifiable().get(1)).getChildrenUnmodifiable().get(0);
        Label labelDescribe = (Label) ((Parent)hbox.getChildrenUnmodifiable().get(1)).getChildrenUnmodifiable().get(1);

        int stackPaneIndex = listViewFindNode(contentObj, stackPane);
        if(stackPaneIndex==-1) return;
        Label labelKey = (Label) ((HBox) contentObj.getItems().get(stackPaneIndex-1)).getChildrenUnmodifiable().get(1);


        Label newLabelKey = new Label(labelKey.getText());
        Label newLabelTitle = new Label(labelTitle.getText());
        Label newLabelDescribe = new Label(labelDescribe.getText());
        newLabelKey.setVisible(false);
        newLabelKey.setManaged(false);
        newLabelTitle.setVisible(false);
        newLabelTitle.setManaged(false);
        newLabelDescribe.setVisible(false);
        newLabelDescribe.setManaged(false);
        newLabelKey.setId("labelKey");
        newLabelTitle.setId("labelTitle");
        newLabelDescribe.setId("labelDescribe");

        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/addBarDialog.fxml")).load();
            dialogRoot.getChildren().addAll(newLabelKey, newLabelTitle, newLabelDescribe);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText("修改");

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Util.getResourceUrl("/css/common.css"));
            scene.setFill(null);    //  背景透明
            stage.setScene(scene);
            stage.setTitle("修改子元素");
            stage.show();

            stage.setOnHidden(eventx -> {
                reloadData();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  添加功能窗口
    private void addDialog(MouseEvent event) {
        Region regionAdd = (Region) event.getSource();
        Parent parent = regionAdd.getParent();
        int regionAddIndex = parent.getChildrenUnmodifiable().indexOf(regionAdd);
        Label labelKey = (Label) parent.getChildrenUnmodifiable().get(regionAddIndex - 1);
        Label newLabelKey = new Label(labelKey.getText());
        newLabelKey.setVisible(false);
        newLabelKey.setManaged(false);
        newLabelKey.setId("labelKey");

        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/addBarDialog.fxml")).load();
            dialogRoot.getChildren().addAll(newLabelKey);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText("添加");

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Util.getResourceUrl("/css/common.css"));
            scene.setFill(null);    //  背景透明
            stage.setScene(scene);
            stage.setTitle("添加子元素");
            stage.show();

            stage.setOnHidden(eventx -> {
                reloadData();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
