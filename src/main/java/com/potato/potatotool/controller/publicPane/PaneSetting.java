package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.UpdateInfo;
import com.potato.potatotool.update.UpdateManager;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.crypto.AESUtils;
import javafx.animation.FadeTransition;

import static com.potato.potatotool.ToStart.debugMode;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.isBlueMode;
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
    private ComboBox<String> decompileType;
    
    @FXML
    private ComboBox<String> languageComboBox;

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
    
    // 更新设置相关
    @FXML
    private CFSwitch autoCheckUpdateSwitch;
    
    // 存储位置管理相关
    @FXML
    private Label configPathLabel;
    @FXML
    private TextField resourcePathField;
    @FXML
    private Label currentSizeLabel;
    @FXML
    private Label availableSizeLabel;
    @FXML
    private Button migrateButton;
    @FXML
    private Button resetPathButton;
    @FXML
    private Button checkAppUpdateButton;
    
    @FXML
    private Button saveUpdateSettingsButton;

    private double offsetX,offsetY;

    private JsonObject tmpJsonObj_AI = new JsonObject();

    // 初始化Map，建立checkbox和key的对应关系
    Map<CFSwitch, String> proxyMap = new LinkedHashMap<>();
    
    // 国际化管理器
    private I18nManager i18n = I18nManager.getInstance();

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
        
        // 在所有组件初始化完成后再绑定国际化
        Platform.runLater(() -> {
            initLanguage();
            I18nUtils.bindComponents(an);
            // 国际化绑定后再初始化存储管理（避免按钮文本被绑定）
            initStorageManagement();
        });
    }
    
    /**
     * 初始化存储位置管理和更新设置
     */
    private void initStorageManagement() {
        PathManager pathManager = PathManager.getInstance();
        
        // 初始化自动检查更新开关
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            if (config != null && config.has("UpDate")) {
                JsonObject updateConfig = config.getAsJsonObject("UpDate");
                if (updateConfig.has("autoCheck")) {
                    boolean autoCheck = updateConfig.get("autoCheck").getAsBoolean();
                    autoCheckUpdateSwitch.setSelected(autoCheck);
                } else {
                    autoCheckUpdateSwitch.setSelected(true); // 默认开启
                }
            } else {
                autoCheckUpdateSwitch.setSelected(true); // 默认开启
            }
        } catch (Exception e) {
            autoCheckUpdateSwitch.setSelected(true); // 默认开启
        }
        
        // 显示配置文件路径
        configPathLabel.setText(pathManager.getConfigBasePath().toString());
        
        // 显示资源文件路径
        resourcePathField.setText(pathManager.getResourceBasePath().toString());
        
        // 显示初始状态
        currentSizeLabel.setText(i18n.getString("update.storage.calculating"));
        availableSizeLabel.setText(i18n.getString("update.storage.calculating"));
        
        // 初始化"检查软件更新"按钮文本（手动国际化，因为需要动态改变）
        if (checkAppUpdateButton != null) {
            checkAppUpdateButton.setText(i18n.getString("update.check.title"));
        }
        
        // 异步计算占用空间
        new Thread(() -> {
            long currentSize = pathManager.getDirectorySize(pathManager.getResourceBasePath());
            long availableSpace = pathManager.getAvailableSpace(pathManager.getResourceBasePath());
            
            Platform.runLater(() -> {
                currentSizeLabel.setText(PathManager.formatSize(currentSize));
                availableSizeLabel.setText(PathManager.formatSize(availableSpace));
            });
        }).start();
    }
    
    /**
     * 初始化语言选择器
     */
    private void initLanguage() {
        // 初始化语言下拉框
        if (languageComboBox != null) {
            // 获取当前语言并设置
            String currentLang = i18n.getCurrentLanguageCode();
            if ("zh_CN".equals(currentLang)) {
                languageComboBox.setValue("简体中文");
            } else {
                languageComboBox.setValue("English");
            }
            // 注意：不再监听valueProperty，而是在save()方法中检查是否有变化
        }
    }
    
    /**
     * 切换语言（只在保存时调用）
     */
    private void switchLanguage(String language) {
        if ("简体中文".equals(language)) {
            i18n.switchLanguage("zh_CN");
        } else if ("English".equals(language)) {
            i18n.switchLanguage("en_US");
        }
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
                case AssetConstants.FOFA_KEY:
                    targetField = fofaKey;
                    targetPane = assetPane;
                    break;
                case AssetConstants.HUNTER_KEY:
                    targetPane = assetPane;
                    // Hunter 是 VBox，需要聚焦到第一个 TextField
                    if (hunterVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) hunterVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case AssetConstants.QUAKE_KEY:
                    targetPane = assetPane;
                    if (quakeVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) quakeVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case AssetConstants.ZOOMEYE_KEY:
                    targetField = zoomeyeKey;
                    targetPane = assetPane;
                    break;
                case AssetConstants.SHODAN_KEY:
                    targetField = shodanKey;
                    targetPane = assetPane;
                    break;
                case AssetConstants.GOOGLE_API:
                    targetPane = assetPane;
                    if (googleVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) googleVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case AssetConstants.GITHUB_TOKEN:
                    targetPane = assetPane;
                    if (githubVBox.getChildren().size() > 0) {
                        StackPane firstStack = (StackPane) githubVBox.getChildren().get(0);
                        targetField = (TextField) firstStack.getChildren().get(0);
                    }
                    break;
                case AssetConstants.CHINAZ_COOKIE:
                    targetField = chinazCookie;
                    targetPane = assetPane;
                    break;
                case AssetConstants.AIQICHA_COOKIE:
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
        proxyMap.put(aiqichaProxy, AssetConstants.AIQICHA_COOKIE);
        proxyMap.put(chinazProxy, AssetConstants.CHINAZ_COOKIE);
        proxyMap.put(fofaProxy, AssetConstants.FOFA_KEY);
        proxyMap.put(hunterProxy, AssetConstants.HUNTER_KEY);
        proxyMap.put(quakeProxy, AssetConstants.QUAKE_KEY);
        proxyMap.put(shodanProxy, AssetConstants.SHODAN_KEY);
        proxyMap.put(zoomeyeProxy, AssetConstants.ZOOMEYE_KEY);
        proxyMap.put(googleProxy, AssetConstants.GOOGLE_API);
        proxyMap.put(githubProxy, AssetConstants.GITHUB_TOKEN);
        proxyMap.put(sslProxy, AssetConstants.SSL);
        proxyMap.put(crawlProxy, AssetConstants.CRAWL);
    }

    private void initData() {

        proxy.setFocusTraversable(false);

        //  初始化默认代理配置
        JsonObject tmpJsonObj_Proxy = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
        boolean isProxy = tmpJsonObj_Proxy.getAsJsonPrimitive(ConfigConstants.PROXY_ENABLE).getAsBoolean();
        String address = tmpJsonObj_Proxy.getAsJsonPrimitive(ConfigConstants.PROXY_ADDRESS).getAsString();
        proxy.setText(address);
        proxyButton.setSelected(isProxy);


        //  初始化默认反编译模式配置
        JsonObject tmpJsonObj_Decompile = (JsonObject) Constants.getOutsideConfig(ConfigConstants.DECOMPILE);
        String decompileMode = tmpJsonObj_Decompile.getAsJsonPrimitive(ConfigConstants.DECOMPILE_MODE).getAsString();
        decompileType.setValue(decompileMode);

        //  初始化默认AI配置
        tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig(ConfigConstants.AI);
        String AI_API_Base = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive(ConfigConstants.AI_API_BASE).getAsString());
        String AI_API_Key = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive(ConfigConstants.AI_API_KEY).getAsString());
        String AI_Model = new AESUtils().decryptLocalConfig(tmpJsonObj_AI.getAsJsonPrimitive(ConfigConstants.AI_MODEL).getAsString());
        aiApiBase.setText(AI_API_Base);
        aiApiKey.setText(AI_API_Key);
        aiModel.setText(AI_Model);

        // 初始化资产测绘
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetConstants.ASSET);
        String Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.CHINAZ_COOKIE).getAsString();
        String Aiqicha_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.AIQICHA_COOKIE).getAsString();
        String Fofa_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.FOFA_KEY).getAsString();
        String Zoomeye_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.ZOOMEYE_KEY).getAsString();
        String Shodan_Key = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.SHODAN_KEY).getAsString();
        chinazCookie.setText(Chinaz_Cookie);
        aiqichaCookie.setText(Aiqicha_Cookie);
        fofaKey.setText(Fofa_Key);
        zoomeyeKey.setText(Zoomeye_Key);
        shodanKey.setText(Shodan_Key);
        JsonArray Hunter_Key_List = tmpJsonObj_Asset.getAsJsonArray(AssetConstants.HUNTER_KEY);
        JsonArray Quake_Key_List = tmpJsonObj_Asset.getAsJsonArray(AssetConstants.QUAKE_KEY);
        JsonArray GitHub_Token_List = tmpJsonObj_Asset.getAsJsonArray(AssetConstants.GITHUB_TOKEN);
        JsonArray Google_API_List = tmpJsonObj_Asset.getAsJsonArray(AssetConstants.GOOGLE_API);
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
        
        // 使用PathManager获取配置文件路径
        PathManager pathManager = PathManager.getInstance();
        String command = pathManager.getConfigFilePath().toString();
        
        try {
            new ProcessBuilder(startCommand, command).start();
        } catch (Exception e) {
            System.out.println(startCommand + " " + command);
            e.printStackTrace();
        }
    }


    @FXML
    void save(){
        // 检查语言是否发生变化
        String selectedLanguage = languageComboBox != null ? languageComboBox.getValue() : null;
        String currentLanguageDisplay = "zh_CN".equals(i18n.getCurrentLanguageCode()) ? "简体中文" : "English";
        boolean languageChanged = selectedLanguage != null && !selectedLanguage.equals(currentLanguageDisplay);
        
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        proxyConfigMap.put(ConfigConstants.PROXY_ENABLE, proxyButton.isSelected());
        proxyConfigMap.put(ConfigConstants.PROXY_ADDRESS, proxy.getText());
        
        // 保存各服务的代理状态到 Proxy.services
        Map<String, Object> servicesMap = new LinkedHashMap<>();
        proxyMap.forEach((checkbox, key) -> {
            servicesMap.put(key, checkbox.isSelected());
        });
        proxyConfigMap.put(ConfigConstants.PROXY_SERVICES, servicesMap);
        configMap.put(ConfigConstants.PROXY, proxyConfigMap);

        Map<String, Object> decompileMap = new LinkedHashMap<>();
        decompileMap.put(ConfigConstants.DECOMPILE_MODE, (String)decompileType.getSelectionModel().getSelectedItem());
        decompileMap.put(ConfigConstants.DECOMPILE_AI_OPTIMIZATION, false);
        configMap.put(ConfigConstants.DECOMPILE, decompileMap);

        Map<String, Object> aiMap = convertToMap(tmpJsonObj_AI);
        aiMap.put(ConfigConstants.AI_API_BASE, new AESUtils().encryptLocalConfig(aiApiBase.getText()));
        aiMap.put(ConfigConstants.AI_API_KEY, new AESUtils().encryptLocalConfig(aiApiKey.getText()));
        aiMap.put(ConfigConstants.AI_MODEL, new AESUtils().encryptLocalConfig(aiModel.getText()));
        configMap.put(ConfigConstants.AI, aiMap);

        if(Constants.saveConfig(configMap)){
            // 更新设置现在有独立的保存按钮，这里不再保存（避免重复）
            if (languageChanged && selectedLanguage != null) {
                // 如果语言发生了变化，则切换语言
                switchLanguage(selectedLanguage);
            }
            showTip(i18n.getString("setting.save.success"));
        }else {
            showTip(i18n.getString("setting.save.failed"));
        }
    }

    public static Map<String, Object> convertToMap(JsonObject jsonObject) {
        return new Gson().fromJson(
                jsonObject,
                new TypeToken<Map<String, Object>>(){}.getType()
        );
    }

    // ========== 以下为旧的MD5/KB独立更新逻辑（已废弃，统一使用"检查软件更新"） ==========
    // 注意：这些方法保留是为了兼容setting.fxml中的按钮绑定
    // 建议：后续可以移除这些方法，统一使用checkAppUpdate
    
    @FXML
    public void checkMd5(ActionEvent event) {
        showTip(i18n.getString("update.storage.use.unified"));
    }

    @FXML
    public void checkKb(ActionEvent event) {
        showTip(i18n.getString("update.storage.use.unified"));
    }

    @FXML
    public void updateMd5(ActionEvent event) {
        showTip(i18n.getString("update.storage.use.unified"));
    }

    @FXML
    public void updateKb(ActionEvent event) {
        showTip(i18n.getString("update.storage.use.unified"));
    }

    @FXML
    public void stopUpdateMd5(ActionEvent event) {
        // 已废弃
    }

    @FXML
    public void stopUpdateKb(ActionEvent event) {
        // 已废弃
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
        // 使用国际化绑定Tooltip
        Tooltip delTooltip = new Tooltip();
        delTooltip.textProperty().bind(i18n.createBinding("tooltip.delete"));
        delLabel.setTooltip(delTooltip);

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
        // 检查语言是否发生变化
        String selectedLanguage = languageComboBox != null ? languageComboBox.getValue() : null;
        String currentLanguageDisplay = "zh_CN".equals(i18n.getCurrentLanguageCode()) ? "简体中文" : "English";
        boolean languageChanged = selectedLanguage != null && !selectedLanguage.equals(currentLanguageDisplay);
        
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
        assetMap.put(AssetConstants.CHINAZ_COOKIE, Chinaz_Cookie);
        assetMap.put(AssetConstants.AIQICHA_COOKIE, Aiqicha_Cookie);
        assetMap.put(AssetConstants.FOFA_KEY, Fofa_Key);
        assetMap.put(AssetConstants.HUNTER_KEY, Hunter_Key);
        assetMap.put(AssetConstants.QUAKE_KEY, Quake_Key);
        assetMap.put(AssetConstants.ZOOMEYE_KEY, Zoomeye_Key);
        assetMap.put(AssetConstants.SHODAN_KEY, Shodan_Key);
        assetMap.put(AssetConstants.GITHUB_TOKEN, GitHub_Token);
        assetMap.put(AssetConstants.GOOGLE_API, Google_API);
        configMap.put(AssetConstants.ASSET, assetMap);
        
        // 保存代理配置到 Proxy.services（与 save() 方法保持一致）
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        JsonObject currentProxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
        proxyConfigMap.put(ConfigConstants.PROXY_ENABLE, currentProxyConfig.get(ConfigConstants.PROXY_ENABLE).getAsBoolean());
        proxyConfigMap.put(ConfigConstants.PROXY_ADDRESS, currentProxyConfig.get(ConfigConstants.PROXY_ADDRESS).getAsString());
        
        Map<String, Object> servicesMap = new LinkedHashMap<>();
        proxyMap.forEach((checkbox, key) -> {
            servicesMap.put(key, checkbox.isSelected());
        });
        proxyConfigMap.put(ConfigConstants.PROXY_SERVICES, servicesMap);
        configMap.put(ConfigConstants.PROXY, proxyConfigMap);

        if(Constants.saveConfig(configMap)){
            // 如果语言发生了变化，则切换语言
            if (languageChanged && selectedLanguage != null) {
                switchLanguage(selectedLanguage);
                showTip(i18n.getString("setting.save.success") + " - " + i18n.getString("setting.language.changed"));
            } else {
                showTip(i18n.getString("setting.save.success"));
            }
        }else {
            showTip(i18n.getString("setting.save.failed"));
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
    
    /**
     * 浏览资源路径
     */
    @FXML
    public void browseResourcePath(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(i18n.getString("update.storage.button.browse"));
        
        // 设置初始目录
        File currentPath = new File(resourcePathField.getText());
        if (currentPath.exists()) {
            directoryChooser.setInitialDirectory(currentPath);
        }
        
        Stage stage = (Stage) an.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            resourcePathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    /**
     * 保存更新设置（独立按钮）
     */
    @FXML
    public void saveUpdateSettings(ActionEvent event) {
        try {
            Map<String, Object> updateMap = new LinkedHashMap<>();
            updateMap.put(ConfigConstants.UPDATE_AUTO_CHECK, autoCheckUpdateSwitch.isSelected());
            
            if (debugMode) System.out.println("保存更新设置: autoCheck=" + autoCheckUpdateSwitch.isSelected());
            
            Constants.saveConfig(updateMap, ConfigConstants.UPDATE);
            Constants.cachedConfig = null;
            
            if (debugMode) System.out.println("更新设置已保存");
            showTip(i18n.getString("setting.save.success"));
            
        } catch (Exception e) {
            System.err.println("保存更新设置失败: " + e.getMessage());
            e.printStackTrace();
            showTip(i18n.getString("setting.save.failed"));
        }
    }
    
    /**
     * 迁移资源文件
     */
    @FXML
    public void migrateResources(ActionEvent event) {
        String newPath = resourcePathField.getText();
        PathManager pathManager = 
                PathManager.getInstance();
        
        String currentPath = pathManager.getResourceBasePath().toString();
        
        // 检查新路径是否为空
        if (newPath == null || newPath.trim().isEmpty()) {
            showTip(i18n.getString("update.storage.path.empty"));
            return;
        }
        
        // 检查路径是否改变
        if (newPath.trim().equals(currentPath)) {
            showTip(i18n.getString("update.storage.path.same"));
            return;
        }
        
        // 确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(i18n.getString("update.storage.migrate.title"));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(
            I18nUtils.getString("update.storage.migrate.message", newPath) + "\n\n" +
            i18n.getString("update.storage.migrate.warning")
        );
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 禁用按钮
                migrateButton.setDisable(true);
                
                // 异步迁移
                new Thread(() -> {
                    try {
                        pathManager.migrateResources(
                            java.nio.file.Paths.get(newPath),
                            new PathManager.MigrationCallback() {
                                @Override
                                public void onProgress(int current, int total, String fileName) {
                                    Platform.runLater(() -> {
                                        String msg = I18nUtils.getString("update.storage.migrate.progress", 
                                                                        current + 1, total) + " - " + fileName;
                                        showTip(msg);
                                    });
                                }
                                
                                @Override
                                public void onComplete() {
                                    Platform.runLater(() -> {
                                        showTip(i18n.getString("update.storage.migrate.success"));
                                        migrateButton.setDisable(false);
                                        initStorageManagement();
                                    });
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Platform.runLater(() -> {
                                        showTip(I18nUtils.getString("update.storage.migrate.failed", error));
                                        migrateButton.setDisable(false);
                                    });
                                }
                            }
                        );
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showTip(I18nUtils.getString("update.storage.migrate.failed", e.getMessage()));
                            migrateButton.setDisable(false);
                        });
                    }
                }).start();
            }
        });
    }
    
    /**
     * 恢复默认路径
     */
    @FXML
    public void resetResourcePath(ActionEvent event) {
        PathManager pathManager = PathManager.getInstance();
        
        String defaultPath = pathManager.getConfigBasePath().resolve("resources").toString();
        resourcePathField.setText(defaultPath);
        
        showTip(i18n.getString("update.storage.reset.tip"));
    }
    
    /**
     * 检查软件更新
     */
    @FXML
    public void checkAppUpdate(ActionEvent event) {
        // 保存原始文本
        final String originalText = checkAppUpdateButton.getText();
        
        checkAppUpdateButton.setDisable(true);
        checkAppUpdateButton.setText(i18n.getString("update.check.checking"));
        
        new Thread(() -> {
            try {
                UpdateManager updateManager = UpdateManager.getInstance();
                
                UpdateInfo updateInfo = updateManager.checkForUpdates();
                
                Platform.runLater(() -> {
                    checkAppUpdateButton.setDisable(false);
                    checkAppUpdateButton.setText(originalText);
                    
                    if (updateInfo.hasAnyUpdate()) {
                        // 显示更新对话框
                        showUpdateDialog(updateInfo);
                    } else {
                        showTip(i18n.getString("update.no.update.message"));
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    checkAppUpdateButton.setDisable(false);
                    checkAppUpdateButton.setText(originalText);
                    showTip(i18n.getString("update.check.failed") + ": " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    /**
     * 显示更新对话框
     */
    private void showUpdateDialog(UpdateInfo updateInfo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/publicPane/update_dialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            scene.setFill(null);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            
            Stage updateStage = new Stage();
            updateStage.initStyle(StageStyle.TRANSPARENT);
            updateStage.setScene(scene);
            updateStage.setTitle("PotatoTool - " + i18n.getString("update.check.title"));
            updateStage.setAlwaysOnTop(true);  // 更新对话框置顶
            
            // 设置更新信息
            PaneUpdateDialog controller = loader.getController();
            controller.setUpdateInfo(updateInfo);
            
            updateStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showTip(I18nUtils.getString("update.storage.dialog.error", e.getMessage()));
        }
    }
}
