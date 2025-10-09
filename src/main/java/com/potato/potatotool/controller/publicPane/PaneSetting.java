package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.crypto.AESUtils;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.isBlueMode;
import static com.potato.potatotool.content.Update.checkResAndGetDownUrl;
import static com.potato.potatotool.content.Update.downloadAndSaveResource;
import static com.potato.potatotool.controller.MainController.clipRect;

/**
 * @author Potato
 * @date 2023/11/8 14:47
 */
public class PaneSetting {
    @FXML
    private AnchorPane an;

    @FXML
    private VBox vBoxBar;

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
    private TextField aiApiBase;
    @FXML
    private TextField aiApiKey;
    @FXML
    private TextField aiModel;

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
    private CFSwitch aiProxy;
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
    
    @FXML
    private Accordion settingAccordion;
    @FXML
    private TitledPane basicPane;
    @FXML
    private TitledPane aiPane;
    @FXML
    private TitledPane assetPane;
    @FXML
    private TitledPane updatePane;

    private double offsetX,offsetY;

    private JsonObject tmpJsonObj_AI = new JsonObject();

    // 初始化Map，建立checkbox和key的对应关系
    Map<CFSwitch, String> proxyMap = new LinkedHashMap<>();

    public void initialize() {
        if(isBlueMode){
            an.getStyleClass().remove("redStyle");
            an.getStyleClass().add("blueStyle");
        }else {
            an.getStyleClass().remove("blueStyle");
            an.getStyleClass().add("redStyle");
        }

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initProxyMap();
        initData();
        
        // 默认只展开第一项
        settingAccordion.setExpandedPane(basicPane);
    }
    
    /**
     * 定位到指定配置项并展开对应面板
     * @param configKey 配置项的 key（如 "Fofa_Key", "Hunter_Key" 等）
     */
    public void navigateToConfig(String configKey) {
        Platform.runLater(() -> {
            TextField targetField = null;
            TitledPane targetPane = null;
            
            // 根据 configKey 定位到对应的输入框和面板
            switch (configKey) {
                case ConfigConstants.FOFA_KEY:
                    targetField = fofaKey;
                    targetPane = assetPane;
                    break;
                case ConfigConstants.HUNTER_KEY:
                    targetPane = assetPane;
                    // Hunter 是 VBox，需要聚焦到第一个 TextField
                    if (hunterVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) hunterVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case ConfigConstants.QUAKE_KEY:
                    targetPane = assetPane;
                    if (quakeVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) quakeVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case ConfigConstants.ZOOMEYE_KEY:
                    targetField = zoomeyeKey;
                    targetPane = assetPane;
                    break;
                case ConfigConstants.SHODAN_KEY:
                    targetField = shodanKey;
                    targetPane = assetPane;
                    break;
                case ConfigConstants.GOOGLE_API:
                    targetPane = assetPane;
                    if (googleVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) googleVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case ConfigConstants.GITHUB_TOKEN:
                    targetPane = assetPane;
                    if (githubVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) githubVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case ConfigConstants.CHINAZ_COOKIE:
                    targetField = chinazCookie;
                    targetPane = assetPane;
                    break;
                case ConfigConstants.AIQICHA_COOKIE:
                    targetField = aiqichaCookie;
                    targetPane = assetPane;
                    break;
                default:
                    // 如果没有匹配，默认展开资产测绘面板
                    targetPane = assetPane;
                    break;
            }
            
            // 展开目标面板
            if (targetPane != null) {
                settingAccordion.setExpandedPane(targetPane);
            }
            
            // 聚焦到目标输入框
            if (targetField != null) {
                TextField finalTargetField = targetField;
                Platform.runLater(() -> {
                    finalTargetField.requestFocus();
                    // 选中所有文本，方便用户直接输入
                    finalTargetField.selectAll();
                });
            }
        });
    }

    private void initProxyMap() {
        proxyMap.put(aiProxy, ConfigConstants.AI);
        proxyMap.put(aiqichaProxy, ConfigConstants.AIQICHA_COOKIE);
        proxyMap.put(chinazProxy, ConfigConstants.CHINAZ_COOKIE);
        proxyMap.put(fofaProxy, ConfigConstants.FOFA_KEY);
        proxyMap.put(hunterProxy, ConfigConstants.HUNTER_KEY);
        proxyMap.put(quakeProxy, ConfigConstants.QUAKE_KEY);
        proxyMap.put(shodanProxy, ConfigConstants.SHODAN_KEY);
        proxyMap.put(zoomeyeProxy, ConfigConstants.ZOOMEYE_KEY);
        proxyMap.put(googleProxy, ConfigConstants.GOOGLE_API);
        proxyMap.put(githubProxy, ConfigConstants.GITHUB_TOKEN);
        proxyMap.put(sslProxy, ConfigConstants.SSL);
        proxyMap.put(crawlProxy, ConfigConstants.CRAWL);
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
        tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig("AI");
        String AI_API_Base = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive("AI_API_Base").getAsString());
        String AI_API_Key = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive("AI_API_Key").getAsString());
        String AI_Model = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive("AI_Model").getAsString());
        aiApiBase.setText(AI_API_Base);
        aiApiKey.setText(AI_API_Key);
        aiModel.setText(AI_Model);

        // 初始化资产测绘
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(ConfigConstants.ASSET);
        String Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(ConfigConstants.CHINAZ_COOKIE).getAsString();
        String Aiqicha_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(ConfigConstants.AIQICHA_COOKIE).getAsString();
        String Fofa_Key = tmpJsonObj_Asset.getAsJsonPrimitive(ConfigConstants.FOFA_KEY).getAsString();
        String Zoomeye_Key = tmpJsonObj_Asset.getAsJsonPrimitive(ConfigConstants.ZOOMEYE_KEY).getAsString();
        String Shodan_Key = tmpJsonObj_Asset.getAsJsonPrimitive(ConfigConstants.SHODAN_KEY).getAsString();
        chinazCookie.setText(Chinaz_Cookie);
        aiqichaCookie.setText(Aiqicha_Cookie);
        fofaKey.setText(Fofa_Key);
        zoomeyeKey.setText(Zoomeye_Key);
        shodanKey.setText(Shodan_Key);
        JsonArray Hunter_Key_List = tmpJsonObj_Asset.getAsJsonArray(ConfigConstants.HUNTER_KEY);
        JsonArray Quake_Key_List = tmpJsonObj_Asset.getAsJsonArray(ConfigConstants.QUAKE_KEY);
        JsonArray GitHub_Token_List = tmpJsonObj_Asset.getAsJsonArray(ConfigConstants.GITHUB_TOKEN);
        JsonArray Google_API_List = tmpJsonObj_Asset.getAsJsonArray(ConfigConstants.GOOGLE_API);
        setTextArrayData(Hunter_Key_List, hunterVBox);
        setTextArrayData(Quake_Key_List, quakeVBox);
        setTextArrayData(GitHub_Token_List, githubVBox);
        setTextArrayData(Google_API_List, googleVBox);
        
        // 设置各服务的代理状态 - 从 Proxy.services 读取
        JsonObject proxyServices = new JsonObject();
        if (tmpJsonObj_Proxy.has(ConfigConstants.PROXY_SERVICES) && 
            tmpJsonObj_Proxy.get(ConfigConstants.PROXY_SERVICES).isJsonObject()) {
            proxyServices = tmpJsonObj_Proxy.getAsJsonObject(ConfigConstants.PROXY_SERVICES);
        }
        
        JsonObject finalProxyServices = proxyServices;
        proxyMap.forEach((checkbox, key) -> {
            boolean proxyEnabled = finalProxyServices.has(key) && finalProxyServices.get(key).getAsBoolean();
            checkbox.setSelected(proxyEnabled);
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
        stage.hide();
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
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        proxyConfigMap.put("enable", proxyButton.isSelected());
        proxyConfigMap.put("address", proxy.getText());
        
        // 保存各服务的代理状态到 Proxy.services
        Map<String, Object> servicesMap = new LinkedHashMap<>();
        proxyMap.forEach((checkbox, key) -> {
            servicesMap.put(key, checkbox.isSelected());
        });
        proxyConfigMap.put(ConfigConstants.PROXY_SERVICES, servicesMap);
        configMap.put("Proxy", proxyConfigMap);

        Map<String, Object> decompileMap = new LinkedHashMap<>();
        decompileMap.put("decompileMode", (String)decompileType.getSelectionModel().getSelectedItem());
        decompileMap.put("AI_optimization", false);
        configMap.put("Decompile", decompileMap);

        Map<String, Object> aiMap = convertToMap(tmpJsonObj_AI);
        aiMap.put("AI_API_Base", new AESUtils().encryptLocalConfig(aiApiBase.getText()));
        aiMap.put("AI_API_Key", new AESUtils().encryptLocalConfig(aiApiKey.getText()));
        aiMap.put("AI_Model", new AESUtils().encryptLocalConfig(aiModel.getText()));
        configMap.put("AI", aiMap);

        if(Constants.saveConfig(configMap)){
            showTip("保存成功");
        }else {
            showTip("保存失败，请查看日志");
        }
    }

    public static Map<String, Object> convertToMap(JsonObject jsonObject) {
        return new Gson().fromJson(
                jsonObject,
                new TypeToken<Map<String, Object>>(){}.getType()
        );
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

        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> assetMap = new LinkedHashMap<>();
        assetMap.put(ConfigConstants.CHINAZ_COOKIE, Chinaz_Cookie);
        assetMap.put(ConfigConstants.AIQICHA_COOKIE, Aiqicha_Cookie);
        assetMap.put(ConfigConstants.FOFA_KEY, Fofa_Key);
        assetMap.put(ConfigConstants.HUNTER_KEY, Hunter_Key);
        assetMap.put(ConfigConstants.QUAKE_KEY, Quake_Key);
        assetMap.put(ConfigConstants.ZOOMEYE_KEY, Zoomeye_Key);
        assetMap.put(ConfigConstants.SHODAN_KEY, Shodan_Key);
        assetMap.put(ConfigConstants.GITHUB_TOKEN, GitHub_Token);
        assetMap.put(ConfigConstants.GOOGLE_API, Google_API);
        configMap.put(ConfigConstants.ASSET, assetMap);
        
        // 保存代理配置到 Proxy.services（与 save() 方法保持一致）
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        JsonObject currentProxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
        proxyConfigMap.put("enable", currentProxyConfig.get("enable").getAsBoolean());
        proxyConfigMap.put("address", currentProxyConfig.get("address").getAsString());
        
        Map<String, Object> servicesMap = new LinkedHashMap<>();
        proxyMap.forEach((checkbox, key) -> {
            servicesMap.put(key, checkbox.isSelected());
        });
        proxyConfigMap.put(ConfigConstants.PROXY_SERVICES, servicesMap);
        configMap.put("Proxy", proxyConfigMap);

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
