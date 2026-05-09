package com.potato.potatotool.controller.publicPane;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.classObj.ExtensionConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.PaneFactory;
import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
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
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.utils.core.Constants.getResourceString;

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
    
    // 缓存：key -> FlowPane 的映射，用于增量更新
    private final Map<String, FlowPane> flowPaneCache = new HashMap<>();

    @FXML
    private ListView listView;

    @FXML
    private Accordion accordionPane;

    @FXML
    private StackPane promptPane;
    @FXML
    private Label prompt;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    public void initialize() {

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initData();
        
        // 设置为当前活动控制器
        PaneFactory.setActiveController(this);
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    //  初始化数据
    private void initData() {
        try {
            JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.EXTENSION);

            if(tmpJsonObj == null){
                String tmpDataJsonStr = getResourceString("config");
                tmpJsonObj = (JsonObject) (new Gson()).fromJson(tmpDataJsonStr, JsonObject.class).get(ConfigConstants.EXTENSION);
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
        flowPaneCache.clear();
        // 注意：不清除图片缓存，因为图片资源可以复用

        initData();
    }
    
    /**
     * 增量更新指定key的内容
     * @param key 要更新的分类名称
     */
    public void updateCategory(String key) {
        FlowPane flowPane = flowPaneCache.get(key);
        if (flowPane == null) {
            // 如果找不到缓存，则完全重建
            reloadData();
            return;
        }
        
        // 清空该 FlowPane 的内容
        flowPane.getChildren().clear();
        
        // 重新加载该分类的数据
        try {
            JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.EXTENSION);
            if (tmpJsonObj == null) {
                String tmpDataJsonStr = getResourceString("config");
                tmpJsonObj = (JsonObject) (new Gson()).fromJson(tmpDataJsonStr, JsonObject.class).get(ConfigConstants.EXTENSION);
            }
            
            // 使用Constants.findKey来查找key，支持多层级结构
            JsonElement targetValue = Constants.findKey(tmpJsonObj, key);
            if (targetValue != null && targetValue.isJsonArray()) {
                JsonArray tmpArrayData = targetValue.getAsJsonArray();
                populateFlowPane(flowPane, tmpArrayData, key);
            } else {
                System.out.println("未找到分类数据: " + key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 图片缓存，避免重复加载
     */
    private final Map<String, Image> imageCache = new HashMap<>();
    
    /**
     * 加载图片，使用缓存优化
     */
    private Image loadImageWithCache(String icon, String type) {
        // 对于默认图标，直接从缓存读取
        if (imageCache.containsKey(icon)) {
            return imageCache.get(icon);
        }
        
        Image image = null;
        try {
            if (icon.equals("") && type.equals("cmd")) {
                icon = "/img/bar/cmd.png";
            } else if (icon.equals("") && type.equals("web")) {
                icon = "/img/bar/web.png";
            }
            
            if (icon.startsWith("/img/bar/")) {
                try (InputStream is = getClass().getResourceAsStream(icon)) {
                    image = new Image(is);
                    // 缓存内置图标
                    imageCache.put(icon, image);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                image = new Image(new File(icon).toURI().toString());
                if (image.isError()) {
                    if (type.equals("cmd")) {
                        icon = "/img/bar/cmd.png";
                    } else if (type.equals("web")) {
                        icon = "/img/bar/web.png";
                    }
                    if (icon.startsWith("/img/bar/")) {
                        try (InputStream is = getClass().getResourceAsStream(icon)) {
                            image = new Image(is);
                            imageCache.put(icon, image);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(icon + "加载失败，该图片存在问题");
            e.printStackTrace();
        }
        return image;
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
        Tooltip tooltip = new Tooltip(I18nUtils.getString("tooltip.add.element"));
        Tooltip.install(regionAdd, tooltip);
        regionAdd.setOnMouseClicked(even->addDialog(even));
        hBoxTitle.getChildren().addAll(regionTitle, labelTitle, regionAdd);
        hBoxTitle.setAlignment(Pos.CENTER_LEFT);
        contentObj.add(hBoxTitle);

        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setStyle("-fx-padding: 10");
        flowPane.setAlignment(Pos.TOP_LEFT);
        flowPane.setPrefWidth(sPane.getWidth() - 240);
        
        // 缓存 FlowPane
        flowPaneCache.put(key, flowPane);

        populateFlowPane(flowPane, tmpArrayData, key);
        contentObj.add(flowPane);
    }
    
    /**
     * 填充FlowPane的内容
     */
    private void populateFlowPane(FlowPane flowPane, JsonArray tmpArrayData, String key) {
        for (int i = 0; i < tmpArrayData.size(); i++) {
            JsonElement element = tmpArrayData.get(i);
            JsonObject tmpDictData = element.getAsJsonObject();
            String title = tmpDictData.get(ExtensionConstants.FIELD_TITLE).getAsString();
            String describe = tmpDictData.get(ExtensionConstants.FIELD_DESCRIBE).getAsString();
            String content = tmpDictData.get(ExtensionConstants.FIELD_CONTENT).getAsString();
            String type = tmpDictData.get(ExtensionConstants.FIELD_TYPE).getAsString();
            String icon = tmpDictData.get(ExtensionConstants.FIELD_ICON).getAsString();

            HBox hBox = new HBox();
            hBox.getStyleClass().add("cellHBox");
            hBox.setAlignment(Pos.CENTER);


            ImageView imageView = new ImageView();
            // 使用缓存加载图片
            Image image = loadImageWithCache(icon, type);
            imageView.setImage(image);

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
            vBox.setAlignment(Pos.CENTER_LEFT);
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
            // 优化：移除固定偏移，使用 margin 和 alignment
            regionVBox.setSpacing(15);
            StackPane.setMargin(regionVBox, new javafx.geometry.Insets(5, 5, 5, 5));
            Region regionChange = new Region();
            regionChange.getStyleClass().add("changeIcon");
            Tooltip tooltipChange = new Tooltip(I18nUtils.getString("tooltip.modify"));
            Tooltip.install(regionChange, tooltipChange);
            regionChange.setOnMouseClicked(even->changeDialog(even));

            Region regionDel = new Region();
            regionDel.getStyleClass().add("delIcon");
            Tooltip tooltipDel = new Tooltip(I18nUtils.getString("tooltip.delete"));
            Tooltip.install(regionDel, tooltipDel);
            regionDel.setOnMouseClicked(even->delDialog(even));
            regionVBox.getChildren().addAll(regionChange, regionDel);
            regionVBox.getStyleClass().add("regionVBox");
            StackPane stackPane = new StackPane(hBox,regionVBox);
            StackPane.setAlignment(regionVBox, Pos.TOP_RIGHT);  // 改为 TOP_RIGHT，与 card 对齐
            stackPane.getStyleClass().add("stackPane");
            Tooltip tooltipData = new Tooltip(describe+"\n"+type+"："+content);
            Tooltip.install(hBox, tooltipData);

            flowPane.getChildren().add(stackPane);
        }
    }


    //  点击元素事件
    public void start(String type, String content){

        if(content.isEmpty()) return;

        if(type.equals("cmd")){
            String[] command = parseCommand(content);
            try {
                new ProcessBuilder(command).start();
            } catch (Exception e) {
                System.out.println("执行的命令："+content);
                showTip(e.toString());
                e.printStackTrace();
            }
        }else if(type.equals("web")){
            HostServices services = MainApplication.letGetHostServices();
            services.showDocument(content);
        }
    }

    /**
     * 解析命令行
     */
    private String[] parseCommand(String command) {
        List<String> commands = new ArrayList<>();
        Matcher matcher = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
                .matcher(command);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // 双引号内的内容
                commands.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                // 单引号内的内容
                commands.add(matcher.group(2));
            } else {
                // 无引号的内容
                commands.add(matcher.group());
            }
        }
        return commands.toArray(new String[0]);
    }


    //  删除功能窗口
    private void delDialog(MouseEvent even) {
        // 设置当前控制器为活动控制器
        PaneFactory.setActiveController(this);
        
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

        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/publicPane/deleteConfirmDialog.fxml"));
            AnchorPane dialogRoot = loader.load();
            
            // 获取控制器并设置要删除的项目信息
            PaneDeleteConfirmDialog controller = loader.getController();
            controller.setDeleteInfo(title, describe);

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.setFill(null);    //  背景透明
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("delete.window.title"));
            stage.showAndWait();  // 使用 showAndWait 等待用户操作

            // 检查用户是否确认删除
            if (PaneDeleteConfirmDialog.deleteConfirmed) {
                JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.EXTENSION);
                JsonElement targetValue = Constants.findKey(tmpJsonObj, key);
                if (targetValue != null && targetValue.isJsonArray()) {
                    JsonArray targetArray = (JsonArray) targetValue;
                    for (int i = 0; i < targetArray.size(); i++) {
                        JsonObject obj = (JsonObject) targetArray.get(i);
                        if (obj.get(ExtensionConstants.FIELD_TITLE).getAsString().equals(title) && obj.get(ExtensionConstants.FIELD_DESCRIBE).getAsString().equals(describe)) {
                            targetArray.remove(i);
                            break;  // 找到后退出循环
                        }
                    }
                }
                Constants.saveConfig(ConfigConstants.EXTENSION, tmpJsonObj);
                // 优化：只更新该分类，不完全重建
                updateCategory(key);
                // 智能同步：只同步被修改的分类（使用明确的源控制器）
                PaneFactory.syncCategoryFrom(key, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //  修改功能窗口
    private void changeDialog(MouseEvent even) {
        // 设置当前控制器为活动控制器
        PaneFactory.setActiveController(this);
        
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

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/publicPane/addBarDialog.fxml")).load();
            dialogRoot.getChildren().addAll(newLabelKey, newLabelTitle, newLabelDescribe);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText(I18nUtils.getString("addbar.submit.modify"));

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.setFill(null);    //  背景透明
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("addbar.window.title.modify"));
            stage.show();

            // 优化：只在真正修改时才更新
            stage.setOnHidden(eventx -> {
                // 检查是否真的保存了数据
                if (PaneAddBarDialog.dataSaved) {
                    String key = newLabelKey.getText();
                    if (key != null && !key.isEmpty()) {
                        updateCategory(key);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  添加功能窗口
    private void addDialog(MouseEvent event) {
        // 设置当前控制器为活动控制器
        PaneFactory.setActiveController(this);
        
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

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/publicPane/addBarDialog.fxml")).load();
            dialogRoot.getChildren().addAll(newLabelKey);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText(I18nUtils.getString("addbar.submit.add"));

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.setFill(null);    //  背景透明
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("addbar.window.title.add"));
            stage.show();

            // 优化：只在真正添加时才更新
            stage.setOnHidden(eventx -> {
                // 检查是否真的保存了数据
                if (PaneAddBarDialog.dataSaved) {
                    String key = newLabelKey.getText();
                    if (key != null && !key.isEmpty()) {
                        updateCategory(key);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showTip(String tip){
        Platform.runLater(() -> {
            prompt.setText(tip);
            copyAnimation();
        });
    }

    public void copyAnimation() {
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
        promptFadeOut.setDelay(Duration.seconds(1));

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();

        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }
}
