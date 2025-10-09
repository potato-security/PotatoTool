package com.potato.potatotool.controller.publicPane;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.ui.PaneFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.ToStart.isBlueMode;
import static com.potato.potatotool.utils.core.Constants.getResourceString;

/**
 * @author Potato
 * @date 2023/11/8 14:47
 */
public class PaneAddBarDialog {
    @FXML
    private AnchorPane an;

    @FXML
    private TextField title;

    @FXML
    private TextField describe;

    @FXML
    private ComboBox type;

    @FXML
    private TextField content;

    @FXML
    private TextField icon;

    @FXML
    private Button addBtn;

    @FXML
    private BorderPane topBar;

    private double offsetX,offsetY;

    private String KeyData;
    private String TitleData;
    private String DescribeData;
    private JsonObject tmpJsonObj;
    private JsonElement targetValue;
    private boolean isAdd = true;// 模式：保存/修改
    private ChangeListener<String> modeListener;
    
    // 标志位：是否真正保存了数据
    public static boolean dataSaved = false;

    public void initialize() {
        // 初始化时重置标志位
        dataSaved = false;
        
        if(isBlueMode){
            an.getStyleClass().remove("redStyle");
            an.getStyleClass().add("blueStyle");
        }else {
            an.getStyleClass().remove("blueStyle");
            an.getStyleClass().add("redStyle");
        }

        //  设置默认第一个选项
        type.getSelectionModel().selectFirst();

        //  监听是否模式发生改变（是否切换到修改模式）
        modeListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldText, String newText) {
                KeyData = ((Label) an.lookup("#labelKey")).getText();
                Node nodeTitle = an.lookup("#labelTitle");
                Node nodeDescribe = an.lookup("#labelDescribe");

                tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Extension");
                if(tmpJsonObj == null){
                    String tmpDataJsonStr = getResourceString("config");
                    tmpJsonObj = (JsonObject) (new Gson()).fromJson(tmpDataJsonStr, JsonObject.class).get("Extension");
                }

                targetValue = Constants.findKey(tmpJsonObj, KeyData);


                if(newText.equals("修改")){
                    isAdd = false;
                    TitleData = ((Label)nodeTitle).getText();
                    DescribeData = ((Label)nodeDescribe).getText();
                    if (targetValue != null && targetValue.isJsonArray()) {
                        JsonArray array = targetValue.getAsJsonArray();
                        array.forEach(jsonElement -> {
                            JsonObject obj = jsonElement.getAsJsonObject();
                            if (obj.get("title").getAsString().equals(TitleData) && obj.get("describe").getAsString().equals(DescribeData)) {
                                title.setText(obj.get("title").getAsString());
                                describe.setText(obj.get("describe").getAsString());
                                type.setValue(obj.get("type").getAsString());
                                content.setText(obj.get("content").getAsString());
                                icon.setText(obj.get("icon").getAsString());
                            }
                        });
                    }
                }else if(newText.equals("添加")){
                    isAdd = true;
                }

                addBtn.textProperty().removeListener(this);
            }
        };
        addBtn.textProperty().addListener(modeListener);
    }

    @FXML
    void getImg(ActionEvent e){
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("选择图片", "*.png","*.jpg","*.jpeg");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
        }else {
            icon.setText(path);
        }

        return;
    }


    @FXML
    void addBar(){
        String titleData = title.getText();
        String describeData = describe.getText();;
        String typeData = ((String) type.getSelectionModel().getSelectedItem()).equals("执行命令") ? "cmd" : ((String) type.getSelectionModel().getSelectedItem()).equals("打开Web") ? "web" : (String) type.getSelectionModel().getSelectedItem();
        String contentData = content.getText();
        String iconData = icon.getText();

        if(titleData.equals("")||describeData.equals("")||contentData.equals("")){
            System.out.println("不可为空");
            return;
        }

        if (targetValue != null && targetValue.isJsonArray()) {
            JsonArray array = targetValue.getAsJsonArray();
            if(isAdd){
                JsonObject elem = new JsonObject();
                elem.addProperty("title", titleData);
                elem.addProperty("describe", describeData);
                elem.addProperty("content", contentData);
                elem.addProperty("type", typeData);
                elem.addProperty("icon",iconData);
                array.add(elem);
            }else{
                for (JsonElement jsonElement : array) {
                    JsonObject obj = jsonElement.getAsJsonObject();
                    if ( obj.get("title").getAsString().equals(TitleData) && obj.get("describe").getAsString().equals(DescribeData)) {
                        obj.addProperty("title", titleData);
                        obj.addProperty("describe", describeData);
                        obj.addProperty("content", contentData);
                        obj.addProperty("type", typeData);
                        obj.addProperty("icon", iconData);
                        break;
                    }
                };
            }

        }
        Constants.saveConfig("Extension", tmpJsonObj);
        
        // 智能同步：只同步被修改的分类（使用当前活动控制器）
        PaneFactory.syncCategory(KeyData);
        
        // 设置保存成功标志
        dataSaved = true;

        if (modeListener != null) {
            addBtn.textProperty().removeListener(modeListener);
        }

        ((Stage) an.getScene().getWindow()).close();
    }

    @FXML
    void topBarDraggedAction(MouseEvent event) {
        Window window = topBar.getScene().getWindow();
        window.setX(event.getScreenX()-offsetX);
        window.setY(event.getScreenY()-offsetY);
    }

    @FXML
    void topBarPressedAction(MouseEvent event) {
        offsetX = event.getSceneX();
        offsetY = event.getSceneY();
    }

    @FXML
    public void exitAction(){
        if (modeListener != null) {
            addBtn.textProperty().removeListener(modeListener);
        }
        ((Stage) an.getScene().getWindow()).close();
    }

}
