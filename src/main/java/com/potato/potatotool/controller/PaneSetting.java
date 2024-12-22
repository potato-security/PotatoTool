package com.potato.potatotool.controller;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.utils.Constants;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.content.update.checkResAndGetDownUrl;
import static com.potato.potatotool.content.update.downloadAndSaveResource;

/**
 * @author Potato
 * @date 2023/11/8 14:47
 */
public class PaneSetting {
    @FXML
    private AnchorPane an;

    @FXML
    private TextField proxy;
    @FXML
    private CFSwitch proxyButton;

    @FXML
    private StackPane promptPane;
    @FXML
    private Label prompt;

    @FXML
    private TextField fofaKey;
    @FXML
    private VBox hunterVBox;
    @FXML
    private VBox quakeVBox;
    @FXML
    private TextField shodanKey;
    @FXML
    private TextField zoomeyeKey;
    @FXML
    private VBox googleVBox;
    @FXML
    private VBox githubVBox;

    @FXML
    private ComboBox decompileType;

    @FXML
    private TextField gptModel;

    @FXML
    private TextField gptApiKey;

    @FXML
    private BorderPane topBar;

    @FXML
    private Label md5Tips;
    @FXML
    private Label kbTips;
    @FXML
    private Button md5CheckButton;
    @FXML
    private Button md5UpdateButton;
    @FXML
    private Button kbCheckButton;
    @FXML
    private Button kbUpdateButton;
    @FXML
    private Button md5StopUpdateButton;
    @FXML
    private Button kbStopUpdateButton;
    @FXML
    private VBox md5ProgressVBox;
    @FXML
    private ProgressBar md5ProgressBar;
    @FXML
    private Label md5ProgressLabel;
    @FXML
    private VBox kbProgressVBox;
    @FXML
    private ProgressBar kbProgressBar;
    @FXML
    private Label kbProgressLabel;
    @FXML
    private TextField chinazCookie;
    @FXML
    private TextField aiqichaCookie;
    @FXML
    private CFSwitch aiqichaProxy;
    @FXML
    private CFSwitch chinazProxy;
    @FXML
    private CFSwitch fofaProxy;
    @FXML
    private CFSwitch hunterProxy;
    @FXML
    private CFSwitch quakeProxy;
    @FXML
    private CFSwitch shodanProxy;
    @FXML
    private CFSwitch zoomeyeProxy;
    @FXML
    private CFSwitch googleProxy;
    @FXML
    private CFSwitch githubProxy;
    @FXML
    private CFSwitch crawlProxy;
    @FXML
    private CFSwitch sslProxy;

    private double offsetX,offsetY;

    // 初始化Map，建立checkbox和key的对应关系
    Map<CFSwitch, String> proxyMap = new HashMap<>();

    public void initialize() {
        initProxyMap();
        initData();
    }

    private void initProxyMap() {
        proxyMap.put(aiqichaProxy, AssetKeyConstants.AIQICHA_COOKIE);
        proxyMap.put(chinazProxy, AssetKeyConstants.CHINAZ_COOKIE);
        proxyMap.put(fofaProxy, AssetKeyConstants.FOFA_KEY);
        proxyMap.put(hunterProxy, AssetKeyConstants.HUNTER_KEY);
        proxyMap.put(quakeProxy, AssetKeyConstants.QUAKE_KEY);
        proxyMap.put(shodanProxy, AssetKeyConstants.SHODAN_KEY);
        proxyMap.put(zoomeyeProxy, AssetKeyConstants.ZOOMEYE_KEY);
        proxyMap.put(googleProxy, AssetKeyConstants.GOOGLE_API);
        proxyMap.put(githubProxy, AssetKeyConstants.GITHUB_TOKEN);
        proxyMap.put(sslProxy, AssetKeyConstants.SSL);
        proxyMap.put(crawlProxy, AssetKeyConstants.CRAWL);
    }

    private void initData() {

        proxy.setFocusTraversable(false);

        //  初始化默认代理配置
        JsonObject tmpJsonObj_Proxy = (JsonObject) Constants.getOutsideConfig("Proxy");
        boolean isProxy = tmpJsonObj_Proxy.getAsJsonPrimitive("enable").getAsBoolean();
        String address = tmpJsonObj_Proxy.getAsJsonPrimitive("address").getAsString();
        proxy.setText(address);
        proxyButton.setSelected(isProxy);


        //  初始化默认反编译模式配置
        JsonObject tmpJsonObj_Decompile = (JsonObject) Constants.getOutsideConfig("Decompile");
        String decompileMode = tmpJsonObj_Decompile.getAsJsonPrimitive("decompileMode").getAsString();
        decompileType.setValue(decompileMode);

        //  初始化默认AI配置
        JsonObject tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig("AI");
        String GPT_Model = tmpJsonObj_AI.getAsJsonPrimitive("GPT_Model").getAsString();
        String GPT_API_Key = tmpJsonObj_AI.getAsJsonPrimitive("GPT_API_Key").getAsString();
        gptModel.setText(GPT_Model);
        gptApiKey.setText(GPT_API_Key);

        // 初始化资产测绘
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        String Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.CHINAZ_COOKIE).getAsString();
        String Aiqicha_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.AIQICHA_COOKIE).getAsString();
        String Fofa_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.FOFA_KEY).getAsString();
        String Zoomeye_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.ZOOMEYE_KEY).getAsString();
        String Shodan_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.SHODAN_KEY).getAsString();
        chinazCookie.setText(Chinaz_Cookie);
        aiqichaCookie.setText(Aiqicha_Cookie);
        fofaKey.setText(Fofa_Key);
        zoomeyeKey.setText(Zoomeye_Key);
        shodanKey.setText(Shodan_Key);
        JsonArray Hunter_Key_List = tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.HUNTER_KEY);
        JsonArray Quake_Key_List = tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.QUAKE_KEY);
        JsonArray GitHub_Token_List = tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.GITHUB_TOKEN);
        JsonArray Google_API_List = tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.GOOGLE_API);
        JsonArray Proxy_Key_List = tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.PROXY_KEY);
        setTextArrayData(Hunter_Key_List, hunterVBox);
        setTextArrayData(Quake_Key_List, quakeVBox);
        setTextArrayData(GitHub_Token_List, githubVBox);
        setTextArrayData(Google_API_List, googleVBox);
        // 设置子项代理状态
        List<String> savedKeys = new ArrayList<>();
        Proxy_Key_List.forEach(element -> savedKeys.add(element.getAsString()));
        proxyMap.forEach((checkbox, key) -> {
            checkbox.setSelected(savedKeys.contains(key));
            checkbox.setDisable(!proxyButton.isSelected());
        });

    }

    private void setTextArrayData(JsonArray jsonArray, VBox vBox){
        StackPane firstStackPane = (StackPane) vBox.getChildren().get(0);
        Label addLabel = (Label) firstStackPane.getChildren().get(1);
        if(jsonArray.size() > 1) {
            for (int i = 0; i < jsonArray.size() - 1; i++) {
                addLabel.fireEvent(new MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0,
                        javafx.scene.input.MouseButton.PRIMARY, 1,
                        true, true, true, true,
                        true, true, true, true,
                        true, true, null
                ));
            }
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement jsonElement = jsonArray.get(i);

            StackPane stackPane = (StackPane) vBox.getChildren().get(i);
            TextField textField = (TextField) stackPane.getChildren().get(0);

            if(jsonElement.isJsonPrimitive()) {
                textField.setText(jsonElement.getAsString());
            }else if(jsonElement.isJsonObject()){
                String Google_Key = jsonElement.getAsJsonObject().get("Google_Key").getAsString();
                String Google_Cx = jsonElement.getAsJsonObject().get("Google_Cx").getAsString();
                textField.setText(Google_Key + ":" + Google_Cx);
            }
        }
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
        Stage stage = (Stage) an.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void viewProfile() {
        String os = System.getProperty("os.name").toLowerCase();
        String startCommand = os.contains("windows") ? "start" :
                              os.contains("mac") ? "open" :
                              os.contains("linux") ? "xdg-open" : null;
        String command = Paths.get(System.getProperty("user.home"), ".PotatoTool", "config.json").toString();
        try {
            new ProcessBuilder(startCommand, command).start();
        } catch (Exception e) {
            System.out.println(startCommand + " " + command);
            e.printStackTrace();
        }
    }


    @FXML
    void save(){
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> proxyMap = new HashMap<>();
        proxyMap.put("enable", proxyButton.isSelected());
        proxyMap.put("address", proxy.getText());
        configMap.put("Proxy", proxyMap);

        Map<String, Object> decompileMap = new HashMap<>();
        decompileMap.put("decompileMode", (String)decompileType.getSelectionModel().getSelectedItem());
        decompileMap.put("AI_optimization", false);
        configMap.put("Decompile", decompileMap);

        Map<String, Object> aiMap = new HashMap<>();
        aiMap.put("GPT_Model", gptModel.getText());
        aiMap.put("GPT_API_Key", gptApiKey.getText());
        configMap.put("AI", aiMap);

        if(Constants.saveConfig(configMap)){
            showTip("保存成功");
        }else {
            showTip("保存失败，请查看日志");
        }
    }

    private Thread md5CheckThread;
    String md5DownUrl = null;
    @FXML
    public void checkMd5(ActionEvent event) {
        md5CheckButton.setText("检测中");

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }

        md5CheckThread = new Thread(() -> {
            md5DownUrl = checkResAndGetDownUrl("md5");

            Platform.runLater(() -> {
                if (md5DownUrl==null){
                    md5Tips.setText("已存在");
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(false);
                    md5CheckButton.setManaged(false);
                }else if (md5DownUrl.startsWith("[Error]")) {
                    md5Tips.setText(md5DownUrl.replace("[Error]",""));
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(true);
                    md5CheckButton.setManaged(true);
                }else {
                    md5Tips.setText("不存在");
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(false);
                    md5CheckButton.setManaged(false);
                    md5UpdateButton.setVisible(true);
                    md5UpdateButton.setManaged(true);
                }
                md5CheckButton.setText("检测");
            });
        });
        md5CheckThread.start();
    }

    private Thread kbCheckThread;
    String kbDownUrl = null;
    @FXML
    public void checkKb(ActionEvent event) {
        kbCheckButton.setText("检测中");

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }

        kbCheckThread = new Thread(() -> {
            kbDownUrl = checkResAndGetDownUrl("winKbInfo");

            Platform.runLater(() -> {
                if (kbDownUrl==null){
                    kbTips.setText("已为最新版");
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(false);
                    kbCheckButton.setManaged(false);
                }else if (kbDownUrl.startsWith("[Error]")) {
                    kbTips.setText(kbDownUrl.replace("[Error]",""));
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(true);
                    kbCheckButton.setManaged(true);
                }else {
                    kbTips.setText("不存在");
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(false);
                    kbCheckButton.setManaged(false);
                    kbUpdateButton.setVisible(true);
                    kbUpdateButton.setManaged(true);
                }
                kbCheckButton.setText("检测");
            });
        });
        kbCheckThread.start();
    }

    private Thread md5UpdateThread;
    @FXML
    public void updateMd5(ActionEvent event) {

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }

        md5ProgressLabel.setText("下载进度: 0%, 速度: -, 预估时间: -");
        md5ProgressBar.setProgress(0);
        md5Tips.setVisible(false);
        md5Tips.setManaged(false);
        md5UpdateButton.setManaged(false);
        md5UpdateButton.setVisible(false);
        md5ProgressVBox.setManaged(true);
        md5ProgressVBox.setVisible(true);
        md5StopUpdateButton.setManaged(true);
        md5StopUpdateButton.setVisible(true);

        md5UpdateThread = new Thread(() -> {
            downloadAndSaveResource("md5", md5DownUrl, md5ProgressBar, md5ProgressLabel);

            Platform.runLater(() -> {
                md5Tips.setText("已存在");
                md5ProgressVBox.setManaged(false);
                md5ProgressVBox.setVisible(false);
                md5StopUpdateButton.setManaged(false);
                md5StopUpdateButton.setVisible(false);
                md5Tips.setManaged(true);
                md5Tips.setVisible(true);
            });
        });
        md5UpdateThread.start();
    }

    private Thread kbUpdateThread;
    @FXML
    public void updateKb(ActionEvent event) {

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }

        kbProgressLabel.setText("下载进度: 0%, 速度: -, 预估时间: -");
        kbProgressBar.setProgress(0);
        kbTips.setVisible(false);
        kbTips.setManaged(false);
        kbUpdateButton.setManaged(false);
        kbUpdateButton.setVisible(false);
        kbProgressVBox.setManaged(true);
        kbProgressVBox.setVisible(true);
        kbStopUpdateButton.setManaged(true);
        kbStopUpdateButton.setVisible(true);

        kbUpdateThread = new Thread(() -> {
            downloadAndSaveResource("winKbInfo", kbDownUrl, kbProgressBar, kbProgressLabel);

            Platform.runLater(() -> {
                kbTips.setText("已为最新版");
                kbProgressVBox.setManaged(false);
                kbProgressVBox.setVisible(false);
                kbStopUpdateButton.setManaged(false);
                kbStopUpdateButton.setVisible(false);
                kbTips.setManaged(true);
                kbTips.setVisible(true);
            });
        });
        kbUpdateThread.start();
    }

    @FXML
    public void stopUpdateMd5(ActionEvent event) {
        md5StopUpdateButton.setVisible(false);
        md5StopUpdateButton.setManaged(false);
        md5ProgressVBox.setManaged(false);
        md5ProgressVBox.setVisible(false);
        md5CheckButton.setVisible(true);
        md5CheckButton.setManaged(true);

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }
    }

    @FXML
    public void stopUpdateKb(ActionEvent event) {

        kbStopUpdateButton.setVisible(false);
        kbStopUpdateButton.setManaged(false);
        kbProgressVBox.setManaged(false);
        kbProgressVBox.setVisible(false);
        kbCheckButton.setVisible(true);
        kbCheckButton.setManaged(true);

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }
    }

    @FXML
    public void delTextField(MouseEvent event) {
        Node source = (Node) event.getSource();
        Parent stackPane = source.getParent();

        if (stackPane != null && stackPane.getParent() instanceof VBox) {
            VBox parentPane = (VBox) stackPane.getParent();
            parentPane.getChildren().remove(stackPane);
        }
    }

    @FXML
    public void addTextField(MouseEvent event) {
        StackPane newStackPane = new StackPane();
        newStackPane.setOnMouseEntered(this::showDel);
        newStackPane.setOnMouseExited(this::hiddenDel);

        // 创建新的 TextField，并添加鼠标进入和离开事件
        TextField newTextField = new TextField();
        newTextField.getStyleClass().add("paddingRightButton");
        newTextField.setPrefWidth(250);
        StackPane.setAlignment(newTextField, Pos.CENTER);

        // 创建删除按钮的 Label
        Label delLabel = new Label();
        delLabel.setAlignment(Pos.CENTER);
        delLabel.setManaged(false);
        delLabel.setVisible(false);
        delLabel.getStyleClass().add("rightButton");
        delLabel.setOnMouseClicked(this::delTextField);
        StackPane.setAlignment(delLabel, Pos.CENTER_RIGHT);
        // 添加删除按钮的图标
        Region delIcon = new Region();
        delIcon.getStyleClass().add("delIcon");
        delLabel.setGraphic(delIcon);
        delLabel.setTooltip(new Tooltip("删除"));

        // 将 TextField 和删除按钮 Label 添加到新的 StackPane
        newStackPane.getChildren().addAll(newTextField, delLabel);

        Node source = (Node) event.getSource();
        Parent stackPane = source.getParent();
        VBox parentPane = (VBox) stackPane.getParent();
        // 将新 StackPane 添加到 VBox
        parentPane.getChildren().add(newStackPane);
        // 获取焦点
        newTextField.requestFocus();
    }

    public void showDel(MouseEvent event) {
        Parent parent = (Parent) event.getSource();
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                node.setManaged(true);
                node.setVisible(true);
                break;
            }
        }
    }

    public void hiddenDel(MouseEvent event) {
        Parent parent = (Parent) event.getSource();
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                node.setManaged(false);
                node.setVisible(false);
                break;
            }
        }
    }

    @FXML
    public void saveAsset(ActionEvent event) {
        String Chinaz_Cookie = chinazCookie.getText().trim();
        String Aiqicha_Cookie = aiqichaCookie.getText().trim();
        String Fofa_Key = fofaKey.getText().trim();
        String Zoomeye_Key = zoomeyeKey.getText().trim();
        String Shodan_Key = shodanKey.getText().trim();
        JsonArray Hunter_Key = getContentTextFieldValue(hunterVBox);
        JsonArray Quake_Key = getContentTextFieldValue(quakeVBox);
        JsonArray GitHub_Token = getContentTextFieldValue(githubVBox);
        JsonArray Google_API = transformGoogleApi(getContentTextFieldValue(googleVBox));
        JsonArray Proxy_Key = new JsonArray();
        proxyMap.forEach((checkbox, key) -> {
            if (checkbox.isSelected()) {
                Proxy_Key.add(key);
            }
        });

        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> assetMap = new HashMap<>();
        assetMap.put(AssetKeyConstants.CHINAZ_COOKIE, Chinaz_Cookie);
        assetMap.put(AssetKeyConstants.AIQICHA_COOKIE, Aiqicha_Cookie);
        assetMap.put(AssetKeyConstants.FOFA_KEY, Fofa_Key);
        assetMap.put(AssetKeyConstants.HUNTER_KEY, Hunter_Key);
        assetMap.put(AssetKeyConstants.QUAKE_KEY, Quake_Key);
        assetMap.put(AssetKeyConstants.ZOOMEYE_KEY, Zoomeye_Key);
        assetMap.put(AssetKeyConstants.SHODAN_KEY, Shodan_Key);
        assetMap.put(AssetKeyConstants.GITHUB_TOKEN, GitHub_Token);
        assetMap.put(AssetKeyConstants.GOOGLE_API, Google_API);
        assetMap.put(AssetKeyConstants.PROXY_KEY, Proxy_Key);
        configMap.put(AssetKeyConstants.ASSET, assetMap);

        if(Constants.saveConfig(configMap)){
            showTip("保存成功");
        }else {
            showTip("保存失败，请查看日志");
        }
    }

    private JsonArray transformGoogleApi(JsonArray originalArray){
        JsonArray newArray = new JsonArray();
        for (int i = 0; i < originalArray.size(); i++) {
            String item = originalArray.get(i).getAsString();

            // 使用冒号分隔字符串
            String[] parts = item.split(":");
            if (parts.length == 2) {
                // 创建一个新的JsonObject，并将分割后的值添加到对象中
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("Google_Key", parts[0]);
                jsonObject.addProperty("Google_Cx", parts[1]);

                // 将JsonObject添加到新的JsonArray
                newArray.add(jsonObject);
            }
        }
        return newArray;
    }

    private JsonArray getContentTextFieldValue(VBox vbox){
        JsonArray jsonArray = new JsonArray();
        for (Node node : vbox.getChildren()) {
            if (node instanceof StackPane) {
                // 进一步检查StackPane中的子节点
                StackPane stackPane = (StackPane) node;
                for (Node innerNode : stackPane.getChildren()) {
                    if (innerNode instanceof TextField) {
                        // 获取TextField的值
                        TextField textField = (TextField) innerNode;
                        String textValue = textField.getText().trim();

                        // 将TextField的值添加到JsonArray中
                        if(!textValue.isEmpty()) jsonArray.add(textValue);
                    }
                }
            }
        }
        return jsonArray;
    }

    public void showTip(String tip){
        prompt.setText(tip);
        copyAnimation();
    }

    public void copyAnimation() {
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
        fadeOut.setDelay(Duration.seconds(1)); // 延迟1秒执行渐出动画

        // 播放渐入动画，完成后播放渐出动画
        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();

        fadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }

    @FXML
    public void proxyBtn(MouseEvent event) {
        proxyButton.setSelected(!proxyButton.isSelected());
        proxyMap.forEach((checkbox, key) -> {
            checkbox.setDisable(!proxyButton.isSelected());
        });
    }
}
