package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HeadlessHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.UpdateInfo;
import com.potato.potatotool.update.UpdateManager;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.network.ProxyUtils;
import javafx.animation.FadeTransition;
import java.util.HashMap;

import static com.potato.potatotool.ToStart.debugMode;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
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
import javafx.stage.FileChooser;
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
    private ComboBox<String> aiProvider;
    @FXML
    private TextField aiTimeoutMs;
    @FXML
    private CFSwitch aiThinkingEnabled;
    @FXML
    private TextField aiThinkingBudgetTokens;

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
    private TitledPane vulnScanPane;
    @FXML
    private TitledPane updatePane;
    @FXML
    private TitledPane oobPane;

    // 漏洞扫描配置相关
    @FXML
    private TextField vulnScanCoreThreads;
    @FXML
    private TextField vulnScanMaxThreads;
    @FXML
    private TextField vulnScanQueueSize;
    @FXML
    private TextField vulnScanTimeout;
    @FXML
    private TextField vulnScanRetries;
    @FXML
    private CFSwitch vulnScanProxySwitch;
    @FXML
    private TextField vulnScanReportDir;
    @FXML
    private ComboBox<String> vulnScanReportFormat;
    @FXML
    private TextField vulnScanHeadlessBrowserPath;
    @FXML
    private CFSwitch vulnScanAutoExport;
    
    // HTTP Headers管理
    @FXML
    private TitledPane headerPane;
    @FXML
    private TextArea defaultHeadersArea;
    @FXML
    private FlowPane headerTemplatePane;

    // 更新设置相关
    @FXML
    private CFSwitch autoCheckUpdateSwitch;

    // OOB 配置
    @FXML
    private ComboBox<String> oobHttpPlatform;
    @FXML
    private TextField oobInteractshServer;
    @FXML
    private TextField oobInteractshToken;
    @FXML
    private TextField oobCustomServer;
    @FXML
    private TextField oobCustomToken;
    @FXML
    private TextField oobCacheTtl;
    @FXML
    private VBox oobInteractshGroup;
    @FXML
    private VBox oobCustomGroup;
    @FXML
    private VBox oobCeyeGroup;
    @FXML
    private ComboBox<String> oobDnsPlatform;
    @FXML
    private TextField oobCeyeIdentifier;
    @FXML
    private TextField oobCeyeToken;

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
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;
    private volatile boolean promptAutoCloseEnabled;
    private boolean proxyToggleUpdating;
    private boolean proxyServiceUpdating;
    private long proxyValidationSeq;

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
        proxyButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal == null || oldVal.equals(newVal)) {
                return;
            }
            handleProxyToggle(newVal);
        });
        initVulnScanData();
        bindProxyServiceListeners();
        initHeadersData();
        initOobSettings();

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
        proxyMap.put(vulnScanProxySwitch, ConfigConstants.VULNSCAN_SERVICE);
    }

    private void syncProxyChildrenState() {
        boolean mainProxyEnabled = proxyButton.isSelected();
        proxyMap.forEach((checkbox, key) -> checkbox.setDisable(!mainProxyEnabled));
    }

    private boolean persistMainProxyState(boolean enabled, String address) {
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        proxyConfigMap.put(ConfigConstants.PROXY_ENABLE, enabled);
        proxyConfigMap.put(ConfigConstants.PROXY_ADDRESS, address == null ? "" : address.trim());
        return Constants.saveConfig(proxyConfigMap, ConfigConstants.PROXY);
    }

    private void setProxyButtonSelectedSilently(boolean selected) {
        proxyToggleUpdating = true;
        try {
            proxyButton.setSelected(selected);
        } finally {
            proxyToggleUpdating = false;
        }
    }

    private void setServiceProxySelectedSilently(CFSwitch checkbox, boolean selected) {
        proxyServiceUpdating = true;
        try {
            checkbox.setSelected(selected);
        } finally {
            proxyServiceUpdating = false;
        }
    }

    private void bindProxyServiceListeners() {
        proxyMap.forEach((checkbox, key) -> checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (proxyServiceUpdating || oldVal == null || oldVal.equals(newVal)) {
                return;
            }
            if (ProxyUtils.saveServiceProxyEnabled(key, newVal)) {
                return;
            }
            setServiceProxySelectedSilently(checkbox, oldVal);
            showTip(i18n.getString("setting.save.failed"), false, true);
        }));
    }

    private void handleProxyToggle(boolean enabled) {
        if (proxyToggleUpdating) {
            return;
        }
        String proxyAddress = proxy.getText() == null ? "" : proxy.getText().trim();
        if (!enabled) {
            if (!persistMainProxyState(false, proxyAddress)) {
                setProxyButtonSelectedSilently(true);
                showTip(i18n.getString("setting.save.failed"), false, true);
                return;
            }
            syncProxyChildrenState();
            return;
        }

        if (proxyAddress.isEmpty()) {
            setProxyButtonSelectedSilently(false);
            persistMainProxyState(false, proxyAddress);
            syncProxyChildrenState();
            showTip(i18n.getString("setting.proxy.enable.address.required"), true, true);
            return;
        }

        long validationSeq = ++proxyValidationSeq;
        proxyButton.setDisable(true);
        showTip(i18n.getString("setting.proxy.checking"), true);
        Task<ProxyUtils.ProxyReachabilityResult> proxyCheckTask = new Task<ProxyUtils.ProxyReachabilityResult>() {
            @Override
            protected ProxyUtils.ProxyReachabilityResult call() {
                return ProxyUtils.checkProxyAddressReachability(proxyAddress, 2000);
            }
        };

        proxyCheckTask.setOnSucceeded(e -> {
            if (validationSeq != proxyValidationSeq || !proxyAddress.equals(proxy.getText() == null ? "" : proxy.getText().trim())) {
                proxyButton.setDisable(false);
                return;
            }
            proxyButton.setDisable(false);
            ProxyUtils.ProxyReachabilityResult result = proxyCheckTask.getValue();
            boolean reachable = result != null && result.isReachable();
            if (!reachable) {
                setProxyButtonSelectedSilently(false);
                persistMainProxyState(false, proxyAddress);
                syncProxyChildrenState();
                showTip(ProxyUtils.buildUnavailableMessage(result), true, true);
                return;
            }
            if (!persistMainProxyState(true, proxyAddress)) {
                setProxyButtonSelectedSilently(false);
                syncProxyChildrenState();
                showTip(i18n.getString("setting.save.failed"), false, true);
                return;
            }
            syncProxyChildrenState();
            showTip(i18n.getString("setting.proxy.available"));
        });

        proxyCheckTask.setOnFailed(e -> {
            if (validationSeq != proxyValidationSeq) {
                proxyButton.setDisable(false);
                return;
            }
            proxyButton.setDisable(false);
            setProxyButtonSelectedSilently(false);
            persistMainProxyState(false, proxyAddress);
            syncProxyChildrenState();
            showTip(ProxyUtils.buildUnavailableMessage(null), true, true);
        });

        Thread worker = new Thread(proxyCheckTask, "setting-proxy-check");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    public void proxyBtn(MouseEvent event) {
        handleProxyToggle(proxyButton.isSelected());
    }

    private void initData() {

        proxy.setFocusTraversable(false);

        //  初始化默认代理配置
        JsonObject tmpJsonObj_Proxy = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
        boolean isProxy = tmpJsonObj_Proxy.getAsJsonPrimitive(ConfigConstants.PROXY_ENABLE).getAsBoolean();
        String address = tmpJsonObj_Proxy.getAsJsonPrimitive(ConfigConstants.PROXY_ADDRESS).getAsString();
        proxy.setText(address);
        setProxyButtonSelectedSilently(isProxy);


        //  初始化默认反编译模式配置
        JsonObject tmpJsonObj_Decompile = (JsonObject) Constants.getOutsideConfig(ConfigConstants.DECOMPILE);
        String decompileMode = tmpJsonObj_Decompile.getAsJsonPrimitive(ConfigConstants.DECOMPILE_MODE).getAsString();
        decompileType.setValue(decompileMode);

        //  初始化默认AI配置（仅显示用户显式填写值，不回显内置加密默认值）
        tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig(ConfigConstants.AI);
        aiApiBase.setText(readAiPlainText(tmpJsonObj_AI, ConfigConstants.AI_BASE_URL));
        aiApiKey.setText(readAiPlainText(tmpJsonObj_AI, ConfigConstants.AI_API_KEY));
        aiModel.setText(readAiPlainText(tmpJsonObj_AI, ConfigConstants.AI_MODEL_NAME));
        String provider = readAiPlainText(tmpJsonObj_AI, ConfigConstants.AI_PROVIDER);
        aiProvider.setValue(provider.isEmpty() ? "OPENAI_COMPATIBLE" : provider);
        aiTimeoutMs.setText(readAiNumberText(tmpJsonObj_AI, ConfigConstants.AI_TIMEOUT_MS, 60000));
        JsonObject thinkingConfig = null;
        if (tmpJsonObj_AI.has(ConfigConstants.AI_THINKING) && tmpJsonObj_AI.get(ConfigConstants.AI_THINKING).isJsonObject()) {
            thinkingConfig = tmpJsonObj_AI.getAsJsonObject(ConfigConstants.AI_THINKING);
        }
        aiThinkingEnabled.setSelected(readAiBoolean(thinkingConfig, ConfigConstants.AI_THINKING_ENABLED, false));
        aiThinkingBudgetTokens.setText(readAiNumberText(thinkingConfig, ConfigConstants.AI_THINKING_BUDGET_TOKENS, 1024));

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
        proxyMap.forEach((checkbox, key) -> checkbox.setSelected(ProxyUtils.isServiceProxyEnabled(key)));
        syncProxyChildrenState();

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


    private String readAiPlainText(JsonObject aiConfig, String key) {
        if (aiConfig == null || !aiConfig.has(key) || aiConfig.get(key).isJsonNull()) {
            return "";
        }
        String value = aiConfig.get(key).getAsString();
        return value == null ? "" : value.trim();
    }

    private String readAiNumberText(JsonObject config, String key, int defaultValue) {
        if (config == null || !config.has(key) || config.get(key).isJsonNull()) {
            return String.valueOf(defaultValue);
        }
        try {
            return String.valueOf(config.get(key).getAsInt());
        } catch (Exception ignored) {
            return String.valueOf(defaultValue);
        }
    }

    private boolean readAiBoolean(JsonObject config, String key, boolean defaultValue) {
        if (config == null || !config.has(key) || config.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return config.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    static void fillAiConfigMap(Map<String, Object> aiMap,
                                        String providerValue,
                                        String plainBaseUrl,
                                        String plainApiKey,
                                        String plainModelName,
                                        int timeoutMs,
                                        boolean thinkingEnabled,
                                        int thinkingBudgetTokens) {
        aiMap.put(ConfigConstants.AI_PROVIDER, providerValue);
        aiMap.put(ConfigConstants.AI_BASE_URL, plainBaseUrl);
        aiMap.put(ConfigConstants.AI_API_KEY, plainApiKey);
        aiMap.put(ConfigConstants.AI_MODEL_NAME, plainModelName);
        aiMap.put(ConfigConstants.AI_TIMEOUT_MS, timeoutMs);

        Map<String, Object> thinkingMap = new LinkedHashMap<>();
        thinkingMap.put(ConfigConstants.AI_THINKING_ENABLED, thinkingEnabled);
        thinkingMap.put(ConfigConstants.AI_THINKING_BUDGET_TOKENS, thinkingBudgetTokens);
        aiMap.put(ConfigConstants.AI_THINKING, thinkingMap);
    }

    static boolean shouldBlockSaveForMainProxy(boolean proxyEnabled, String proxyAddress, ProxyUtils.ProxyReachabilityResult result) {
        if (!proxyEnabled) {
            return false;
        }
        return result == null || !result.isReachable();
    }

    private boolean validateMainProxyBeforeSave() {
        if (!proxyButton.isSelected()) {
            return true;
        }
        String proxyAddress = proxy.getText() == null ? "" : proxy.getText().trim();
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability(proxyAddress, 2000);
        if (!shouldBlockSaveForMainProxy(true, proxyAddress, result)) {
            return true;
        }
        setProxyButtonSelectedSilently(false);
        persistMainProxyState(false, proxyAddress);
        syncProxyChildrenState();
        showTip(ProxyUtils.buildUnavailableMessage(result), true, true);
        return false;
    }

    @FXML
    void save(){
        if (!validateMainProxyBeforeSave()) {
            return;
        }
        // 检查语言是否发生变化
        String selectedLanguage = languageComboBox != null ? languageComboBox.getValue() : null;
        String currentLanguageDisplay = "zh_CN".equals(i18n.getCurrentLanguageCode()) ? "简体中文" : "English";
        boolean languageChanged = selectedLanguage != null && !selectedLanguage.equals(currentLanguageDisplay);
        
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> proxyConfigMap = new LinkedHashMap<>();
        proxyConfigMap.put(ConfigConstants.PROXY_ENABLE, proxyButton.isSelected());
        proxyConfigMap.put(ConfigConstants.PROXY_ADDRESS, proxy.getText());
        configMap.put(ConfigConstants.PROXY, proxyConfigMap);

        Map<String, Object> decompileMap = new LinkedHashMap<>();
        decompileMap.put(ConfigConstants.DECOMPILE_MODE, (String)decompileType.getSelectionModel().getSelectedItem());
        decompileMap.put(ConfigConstants.DECOMPILE_AI_OPTIMIZATION, false);
        configMap.put(ConfigConstants.DECOMPILE, decompileMap);

        Map<String, Object> aiMap = convertToMap(tmpJsonObj_AI);
        String plainBaseUrl = aiApiBase.getText().trim();
        String plainApiKey = aiApiKey.getText().trim();
        String plainModelName = aiModel.getText().trim();
        String providerValue = aiProvider.getValue() == null ? "OPENAI_COMPATIBLE" : aiProvider.getValue().trim();
        if (providerValue.isEmpty()) {
            providerValue = "OPENAI_COMPATIBLE";
        }

        int timeoutMs;
        try {
            timeoutMs = Integer.parseInt(aiTimeoutMs.getText().trim());
            if (timeoutMs <= 0) {
                showTip(i18n.getString("setting.ai.timeout.invalid"), false, true);
                return;
            }
        } catch (Exception e) {
            showTip(i18n.getString("setting.ai.timeout.invalid"), false, true);
            return;
        }

        int thinkingBudgetTokens;
        try {
            thinkingBudgetTokens = Integer.parseInt(aiThinkingBudgetTokens.getText().trim());
            if (thinkingBudgetTokens <= 0) {
                showTip(i18n.getString("setting.ai.thinking.budget.invalid"), false, true);
                return;
            }
        } catch (Exception e) {
            showTip(i18n.getString("setting.ai.thinking.budget.invalid"), false, true);
            return;
        }

        fillAiConfigMap(
                aiMap,
                providerValue,
                plainBaseUrl,
                plainApiKey,
                plainModelName,
                timeoutMs,
                aiThinkingEnabled.isSelected(),
                thinkingBudgetTokens
        );

        if (!plainBaseUrl.isEmpty()) {
            aiMap.put(ConfigConstants.AI_LOCAL_BASE_URL, new AESUtils().encryptLocalConfig(plainBaseUrl));
        }
        if (!plainApiKey.isEmpty()) {
            aiMap.put(ConfigConstants.AI_LOCAL_API_KEY, new AESUtils().encryptLocalConfig(plainApiKey));
        }
        if (!plainModelName.isEmpty()) {
            aiMap.put(ConfigConstants.AI_LOCAL_MODEL_NAME, new AESUtils().encryptLocalConfig(plainModelName));
        }
        configMap.put(ConfigConstants.AI, aiMap);

        if(Constants.saveConfig(configMap)){
            syncProxyChildrenState();
            // 更新设置现在有独立的保存按钮，这里不再保存（避免重复）
            if (languageChanged && selectedLanguage != null) {
                // 如果语言发生了变化，则切换语言
                switchLanguage(selectedLanguage);
            }
            showTip(i18n.getString("setting.save.success"));
        }else {
            showTip(i18n.getString("setting.save.failed"), false, true);
        }
    }

    public static Map<String, Object> convertToMap(JsonObject jsonObject) {
        return new Gson().fromJson(
                jsonObject,
                new TypeToken<Map<String, Object>>(){}.getType()
        );
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

        if(Constants.saveConfig(configMap)){
            // 如果语言发生了变化，则切换语言
            if (languageChanged && selectedLanguage != null) {
                switchLanguage(selectedLanguage);
                showTip(i18n.getString("setting.save.success") + " - " + i18n.getString("setting.language.changed"));
            } else {
                showTip(i18n.getString("setting.save.success"));
            }
        }else {
            showTip(i18n.getString("setting.save.failed"), false, true);
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
        showTip(tip, false, false);
    }

    public void showTip(String tip, boolean keepVisible){
        showTip(tip, keepVisible, false);
    }

    public void showTip(String tip, boolean keepVisible, boolean isError){
        Platform.runLater(() -> {
            prompt.setText(tip);
            applyPromptStyle(isError);
            playPromptAnimation(!keepVisible);
        });
    }

    private void applyPromptStyle(boolean isError) {
        if (promptPane == null) {
            return;
        }
        promptPane.getStyleClass().removeAll("setting-prompt-info", "setting-prompt-error");
        promptPane.getStyleClass().add(isError ? "setting-prompt-error" : "setting-prompt-info");
    }

    private void playPromptAnimation(boolean autoClose) {
        promptAutoCloseEnabled = autoClose;

        if (promptFadeIn == null) {
            promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeIn.setFromValue(0);
            promptFadeIn.setToValue(1);
            promptFadeIn.setOnFinished(event -> {
                if (promptAutoCloseEnabled && promptFadeOut != null) {
                    promptFadeOut.playFromStart();
                }
            });
        }

        if (promptFadeOut == null) {
            promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeOut.setFromValue(1);
            promptFadeOut.setToValue(0);
            promptFadeOut.setDelay(Duration.seconds(1));
            promptFadeOut.setOnFinished(event -> {
                promptPane.setVisible(false);
                promptPane.setManaged(false);
            });
        }

        promptFadeIn.stop();
        promptFadeOut.stop();
        promptPane.setOpacity(0);
        promptPane.setVisible(true);
        promptPane.setManaged(true);
        promptFadeIn.playFromStart();
    }

    @FXML
    public void closePromptPane(MouseEvent event) {
        if (promptPane != null) {
            if (promptFadeIn != null) {
                promptFadeIn.stop();
            }
            if (promptFadeOut != null) {
                promptFadeOut.stop();
            }
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        }
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
            showTip(i18n.getString("setting.save.failed"), false, true);
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
            showTip(i18n.getString("update.storage.path.empty"), false, true);
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
                                        showTip(I18nUtils.getString("update.storage.migrate.failed", error), false, true);
                                        migrateButton.setDisable(false);
                                    });
                                }
                            }
                        );
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showTip(I18nUtils.getString("update.storage.migrate.failed", e.getMessage()), false, true);
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
                    showTip(i18n.getString("update.check.failed") + ": " + e.getMessage(), false, true);
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

    private void initOobSettings() {
        if (oobHttpPlatform != null) {
            oobHttpPlatform.getItems().setAll(
                    HttpLogService.Platform.INTERACTSH.name(),
                    HttpLogService.Platform.CUSTOM.name()
            );
        }
        if (oobDnsPlatform != null) {
            oobDnsPlatform.getItems().setAll(
                    DnsLogService.Platform.DNSLOG_CN.name(),
                    DnsLogService.Platform.CEYE_IO.name()
            );
        }

        JsonObject root = (JsonObject) Constants.getOutsideConfig(null);
        JsonObject oobConfig = root != null && root.has(ConfigConstants.OOB)
                ? root.getAsJsonObject(ConfigConstants.OOB)
                : new JsonObject();

        JsonObject httpConfig = oobConfig.has(ConfigConstants.OOB_HTTP)
                ? oobConfig.getAsJsonObject(ConfigConstants.OOB_HTTP)
                : new JsonObject();
        JsonObject dnsConfig = oobConfig.has(ConfigConstants.OOB_DNS)
                ? oobConfig.getAsJsonObject(ConfigConstants.OOB_DNS)
                : new JsonObject();

        String httpPlatform = httpConfig.has(ConfigConstants.OOB_HTTP_PLATFORM)
                ? httpConfig.get(ConfigConstants.OOB_HTTP_PLATFORM).getAsString()
                : HttpLogService.Platform.INTERACTSH.name();

        if (oobHttpPlatform != null) {
            oobHttpPlatform.setValue(httpPlatform);
        }
        if (oobInteractshServer != null) {
            oobInteractshServer.setText(httpConfig.has(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER)
                    ? httpConfig.get(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER).getAsString()
                    : "oast.pro");
        }
        if (oobInteractshToken != null) {
            oobInteractshToken.setText(httpConfig.has(ConfigConstants.OOB_HTTP_INTERACTSH_TOKEN)
                    ? httpConfig.get(ConfigConstants.OOB_HTTP_INTERACTSH_TOKEN).getAsString()
                    : "");
        }
        if (oobCustomServer != null) {
            oobCustomServer.setText(httpConfig.has(ConfigConstants.OOB_HTTP_CUSTOM_SERVER)
                    ? httpConfig.get(ConfigConstants.OOB_HTTP_CUSTOM_SERVER).getAsString()
                    : "");
        }
        if (oobCustomToken != null) {
            oobCustomToken.setText(httpConfig.has(ConfigConstants.OOB_HTTP_CUSTOM_TOKEN)
                    ? httpConfig.get(ConfigConstants.OOB_HTTP_CUSTOM_TOKEN).getAsString()
                    : "");
        }
        if (oobCacheTtl != null) {
            oobCacheTtl.setText(httpConfig.has(ConfigConstants.OOB_HTTP_CACHE_TTL_SECONDS)
                    ? String.valueOf(httpConfig.get(ConfigConstants.OOB_HTTP_CACHE_TTL_SECONDS).getAsLong())
                    : "3600");
        }

        String dnsPlatform = dnsConfig.has(ConfigConstants.OOB_DNS_PLATFORM)
                ? dnsConfig.get(ConfigConstants.OOB_DNS_PLATFORM).getAsString()
                : DnsLogService.Platform.DNSLOG_CN.name();
        if (oobDnsPlatform != null) {
            oobDnsPlatform.setValue(dnsPlatform);
        }
        if (oobCeyeIdentifier != null) {
            oobCeyeIdentifier.setText(dnsConfig.has(ConfigConstants.OOB_DNS_CEYE_IDENTIFIER)
                    ? dnsConfig.get(ConfigConstants.OOB_DNS_CEYE_IDENTIFIER).getAsString()
                    : "");
        }
        if (oobCeyeToken != null) {
            oobCeyeToken.setText(dnsConfig.has(ConfigConstants.OOB_DNS_CEYE_TOKEN)
                    ? dnsConfig.get(ConfigConstants.OOB_DNS_CEYE_TOKEN).getAsString()
                    : "");
        }

        updateOobFieldState();
        if (oobHttpPlatform != null) {
            oobHttpPlatform.valueProperty().addListener((obs, oldVal, newVal) -> updateOobFieldState());
        }
        if (oobDnsPlatform != null) {
            oobDnsPlatform.valueProperty().addListener((obs, oldVal, newVal) -> updateOobFieldState());
        }
    }

    private void updateOobFieldState() {
        boolean httpCustom = oobHttpPlatform != null
                && HttpLogService.Platform.CUSTOM.name().equalsIgnoreCase(oobHttpPlatform.getValue());

        if (oobInteractshServer != null) {
            oobInteractshServer.setDisable(httpCustom);
        }
        if (oobInteractshToken != null) {
            oobInteractshToken.setDisable(httpCustom);
        }
        if (oobCustomServer != null) {
            oobCustomServer.setDisable(!httpCustom);
        }
        if (oobCustomToken != null) {
            oobCustomToken.setDisable(!httpCustom);
        }
        if (oobInteractshGroup != null) {
            oobInteractshGroup.setVisible(!httpCustom);
            oobInteractshGroup.setManaged(!httpCustom);
        }
        if (oobCustomGroup != null) {
            oobCustomGroup.setVisible(httpCustom);
            oobCustomGroup.setManaged(httpCustom);
        }

        boolean dnsCeye = oobDnsPlatform != null
                && DnsLogService.Platform.CEYE_IO.name().equalsIgnoreCase(oobDnsPlatform.getValue());
        if (oobCeyeIdentifier != null) {
            oobCeyeIdentifier.setDisable(!dnsCeye);
        }
        if (oobCeyeToken != null) {
            oobCeyeToken.setDisable(!dnsCeye);
        }
        if (oobCeyeGroup != null) {
            oobCeyeGroup.setVisible(dnsCeye);
            oobCeyeGroup.setManaged(dnsCeye);
        }
    }

    @FXML
    public void saveOobSettings(ActionEvent event) {
        try {
            String selectedHttpPlatform = oobHttpPlatform == null || oobHttpPlatform.getValue() == null
                    ? HttpLogService.Platform.INTERACTSH.name()
                    : oobHttpPlatform.getValue().trim();
            String interactshServerValue = oobInteractshServer == null ? "" : oobInteractshServer.getText().trim();
            String interactshTokenValue = oobInteractshToken == null ? "" : oobInteractshToken.getText().trim();
            String customServerValue = oobCustomServer == null ? "" : oobCustomServer.getText().trim();
            String customTokenValue = oobCustomToken == null ? "" : oobCustomToken.getText().trim();

            long cacheTtlSeconds = 3600L;
            if (oobCacheTtl != null && !oobCacheTtl.getText().trim().isEmpty()) {
                try {
                    cacheTtlSeconds = Long.parseLong(oobCacheTtl.getText().trim());
                    if (cacheTtlSeconds <= 0) {
                        showTip(i18n.getString("setting.oob.http.cache.ttl.invalid"), false, true);
                        return;
                    }
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.oob.http.cache.ttl.invalid"), false, true);
                    return;
                }
            }

            String selectedDnsPlatform = oobDnsPlatform == null || oobDnsPlatform.getValue() == null
                    ? DnsLogService.Platform.DNSLOG_CN.name()
                    : oobDnsPlatform.getValue().trim();
            String ceyeIdentifierValue = oobCeyeIdentifier == null ? "" : oobCeyeIdentifier.getText().trim();
            String ceyeTokenValue = oobCeyeToken == null ? "" : oobCeyeToken.getText().trim();

            if (HttpLogService.Platform.CUSTOM.name().equalsIgnoreCase(selectedHttpPlatform)
                    && customServerValue.isEmpty()) {
                showTip(i18n.getString("setting.oob.http.custom.server.required"), false, true);
                return;
            }
            String dnsValidationKey = validateDnsCeye(selectedDnsPlatform, ceyeIdentifierValue, ceyeTokenValue, false);
            if (dnsValidationKey != null) {
                showTip(i18n.getString(dnsValidationKey));
                return;
            }

            Map<String, Object> httpMap = new LinkedHashMap<>();
            httpMap.put(ConfigConstants.OOB_HTTP_PLATFORM, selectedHttpPlatform);
            httpMap.put(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER, interactshServerValue.isEmpty() ? "oast.pro" : interactshServerValue);
            httpMap.put(ConfigConstants.OOB_HTTP_INTERACTSH_TOKEN, interactshTokenValue);
            httpMap.put(ConfigConstants.OOB_HTTP_CUSTOM_SERVER, customServerValue);
            httpMap.put(ConfigConstants.OOB_HTTP_CUSTOM_TOKEN, customTokenValue);
            httpMap.put(ConfigConstants.OOB_HTTP_CACHE_TTL_SECONDS, cacheTtlSeconds);

            Map<String, Object> dnsMap = new LinkedHashMap<>();
            dnsMap.put(ConfigConstants.OOB_DNS_PLATFORM, selectedDnsPlatform);
            dnsMap.put(ConfigConstants.OOB_DNS_CEYE_IDENTIFIER, ceyeIdentifierValue);
            dnsMap.put(ConfigConstants.OOB_DNS_CEYE_TOKEN, ceyeTokenValue);

            Map<String, Object> oobMap = new LinkedHashMap<>();
            oobMap.put(ConfigConstants.OOB_HTTP, httpMap);
            oobMap.put(ConfigConstants.OOB_DNS, dnsMap);

            Map<String, Object> configMap = new LinkedHashMap<>();
            configMap.put(ConfigConstants.OOB, oobMap);

            if (!Constants.saveConfig(configMap)) {
                showTip(i18n.getString("setting.save.failed"), false, true);
                return;
            }

            HttpLogService.configureInteractsh((String) httpMap.get(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER), interactshTokenValue);
            HttpLogService.configureCustom(customServerValue, customTokenValue);
            HttpLogService.setCacheDurationSeconds(cacheTtlSeconds);
            try {
                HttpLogService.setPlatform(HttpLogService.Platform.valueOf(selectedHttpPlatform.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                HttpLogService.setPlatform(HttpLogService.Platform.INTERACTSH);
            }

            try {
                DnsLogService.setPlatform(DnsLogService.Platform.valueOf(selectedDnsPlatform.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
            }
            DnsLogService.configureCeye(ceyeIdentifierValue, ceyeTokenValue);

            showTip(i18n.getString("setting.save.success"));
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
            showTip(i18n.getString("setting.save.failed"), false, true);
        }
    }

    static String validateDnsCeye(String selectedDnsPlatform,
                                  String ceyeIdentifierValue,
                                  String ceyeTokenValue,
                                  boolean forConnectivityTest) {
        if (!DnsLogService.Platform.CEYE_IO.name().equalsIgnoreCase(selectedDnsPlatform)) {
            return null;
        }
        if (ceyeIdentifierValue == null || ceyeIdentifierValue.trim().isEmpty()) {
            return forConnectivityTest
                    ? "setting.oob.dns.test.ceye.missing"
                    : "setting.oob.dns.ceye.identifier.required";
        }
        if (ceyeTokenValue == null || ceyeTokenValue.trim().isEmpty()) {
            return forConnectivityTest
                    ? "setting.oob.dns.test.ceye.missing"
                    : "setting.oob.dns.ceye.token.required";
        }
        return null;
    }

    static boolean shouldBlockDnsCeyeTest(String selectedDnsPlatform,
                                           String ceyeIdentifierValue,
                                           String ceyeTokenValue) {
        return validateDnsCeye(selectedDnsPlatform, ceyeIdentifierValue, ceyeTokenValue, true) != null;
    }

    @FXML
    public void testOobHttpConnectivity(ActionEvent event) {
        try {
            HttpLogService.TestResult result;
            if (oobHttpPlatform != null && HttpLogService.Platform.CUSTOM.name().equalsIgnoreCase(oobHttpPlatform.getValue())) {
                result = HttpLogService.testCustomConnectivity();
            } else {
                result = HttpLogService.testInteractshConnectivity();
            }
            showTip(result.message + " (" + result.responseTime + "ms)");
        } catch (Exception e) {
            showTip(i18n.getString("setting.oob.test.failed") + ": " + e.getMessage(), false, true);
        }
    }

    @FXML
    public void testOobDnsConnectivity(ActionEvent event) {
        try {
            String ceyeIdentifierValue = oobCeyeIdentifier == null ? "" : oobCeyeIdentifier.getText().trim();
            String ceyeTokenValue = oobCeyeToken == null ? "" : oobCeyeToken.getText().trim();
            String dnsValidationKey = validateDnsCeye(
                    oobDnsPlatform == null ? null : oobDnsPlatform.getValue(),
                    ceyeIdentifierValue,
                    ceyeTokenValue,
                    true
            );
            if (dnsValidationKey != null) {
                showTip(i18n.getString(dnsValidationKey));
                return;
            }

            String domain = DnsLogService.generateDnsLogDomain();
            if (DnsLogService.isRealDnsLogDomain(domain)) {
                showTip(i18n.getString("setting.oob.test.success") + ": " + domain);
            } else {
                showTip(i18n.getString("setting.oob.test.failed"), false, true);
            }
        } catch (Exception e) {
            showTip(i18n.getString("setting.oob.test.failed") + ": " + e.getMessage(), false, true);
        }
    }

    @FXML
    public void clearOobHttpCache(ActionEvent event) {
        HttpLogService.clearCache();
        showTip(i18n.getString("setting.oob.http.cache.cleared"));
    }

    
    /**
     * 恢复漏洞扫描配置为默认值
     */
    @FXML
    public void resetVulnScanDefaults(ActionEvent event) {
        // 设置默认值
        vulnScanCoreThreads.setText("10");
        vulnScanMaxThreads.setText("50");
        vulnScanQueueSize.setText("1000");
        vulnScanTimeout.setText("15");
        vulnScanRetries.setText("2");
        vulnScanProxySwitch.setSelected(false);
        vulnScanReportDir.setText("reports");
        vulnScanReportFormat.setValue("HTML");
        vulnScanAutoExport.setSelected(false);
        if (vulnScanHeadlessBrowserPath != null) {
            vulnScanHeadlessBrowserPath.setText("");
        }

        showTip(i18n.getString("setting.vulnscan.reset.done"));
    }
    
    /**
     * 初始化漏洞扫描配置数据
     */
    private void initVulnScanData() {
        try {
            vulnScanProxySwitch.setTooltip(new Tooltip(i18n.getString("setting.vulnscan.proxy.tip")));
            JsonObject vulnScanConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (vulnScanConfig == null) {
                vulnScanConfig = new JsonObject();
            }
            
            // 线程池配置
            JsonObject threadPoolConfig = vulnScanConfig.has(ConfigConstants.VULNSCAN_THREAD_POOL) ?
                vulnScanConfig.getAsJsonObject(ConfigConstants.VULNSCAN_THREAD_POOL) : new JsonObject();
            
            if (threadPoolConfig.has(ConfigConstants.VULNSCAN_CORE_THREADS)) {
                vulnScanCoreThreads.setText(String.valueOf(threadPoolConfig.get(ConfigConstants.VULNSCAN_CORE_THREADS).getAsInt()));
            }
            if (threadPoolConfig.has(ConfigConstants.VULNSCAN_MAX_THREADS)) {
                vulnScanMaxThreads.setText(String.valueOf(threadPoolConfig.get(ConfigConstants.VULNSCAN_MAX_THREADS).getAsInt()));
            }
            if (threadPoolConfig.has(ConfigConstants.VULNSCAN_QUEUE_SIZE)) {
                vulnScanQueueSize.setText(String.valueOf(threadPoolConfig.get(ConfigConstants.VULNSCAN_QUEUE_SIZE).getAsInt()));
            }
            
            // 网络配置
            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_TIMEOUT)) {
                vulnScanTimeout.setText(String.valueOf(vulnScanConfig.get(ConfigConstants.VULNSCAN_TIMEOUT).getAsInt()));
            }
            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_RETRIES)) {
                vulnScanRetries.setText(String.valueOf(vulnScanConfig.get(ConfigConstants.VULNSCAN_RETRIES).getAsInt()));
            }
            vulnScanProxySwitch.setSelected(ProxyUtils.isServiceProxyEnabled(ConfigConstants.VULNSCAN_SERVICE));
            
            // 报告配置
            JsonObject reportConfig = vulnScanConfig.has(ConfigConstants.VULNSCAN_REPORT) ?
                vulnScanConfig.getAsJsonObject(ConfigConstants.VULNSCAN_REPORT) : new JsonObject();
            
            if (reportConfig.has(ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR)) {
                vulnScanReportDir.setText(reportConfig.get(ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR).getAsString());
            }
            if (reportConfig.has(ConfigConstants.VULNSCAN_REPORT_DEFAULT_FORMAT)) {
                String format = reportConfig.get(ConfigConstants.VULNSCAN_REPORT_DEFAULT_FORMAT).getAsString();
                vulnScanReportFormat.setValue(format);
            } else {
                vulnScanReportFormat.setValue("HTML");
            }
            if (reportConfig.has(ConfigConstants.VULNSCAN_REPORT_AUTO_EXPORT)) {
                vulnScanAutoExport.setSelected(reportConfig.get(ConfigConstants.VULNSCAN_REPORT_AUTO_EXPORT).getAsBoolean());
            }

            JsonObject headlessConfig = vulnScanConfig.has(ConfigConstants.VULNSCAN_HEADLESS)
                    ? vulnScanConfig.getAsJsonObject(ConfigConstants.VULNSCAN_HEADLESS) : new JsonObject();
            if (headlessConfig.has(ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH) && vulnScanHeadlessBrowserPath != null) {
                vulnScanHeadlessBrowserPath.setText(headlessConfig.get(ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH).getAsString());
            }

        } catch (Exception e) {
            if (debugMode) {
                System.err.println("初始化漏洞扫描配置失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 保存漏洞扫描配置
     */
    @FXML
    public void saveVulnScan(ActionEvent event) {
        try {
            // 获取当前完整的VulnScan配置
            JsonObject currentVulnScan = (JsonObject) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (currentVulnScan == null) {
                currentVulnScan = new JsonObject();
            }
            
            Map<String, Object> configMap = new LinkedHashMap<>();
            Map<String, Object> vulnScanMap = new LinkedHashMap<>();
            
            // 保留原有配置项
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_POC_DIR)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_POC_DIR, currentVulnScan.get(ConfigConstants.VULNSCAN_POC_DIR).getAsString());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_REPORT_DIR)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_REPORT_DIR, currentVulnScan.get(ConfigConstants.VULNSCAN_REPORT_DIR).getAsString());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_THREADS)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_THREADS, currentVulnScan.get(ConfigConstants.VULNSCAN_THREADS).getAsInt());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_USER_AGENT)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_USER_AGENT, currentVulnScan.get(ConfigConstants.VULNSCAN_USER_AGENT).getAsString());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_DEBUG)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_DEBUG, currentVulnScan.get(ConfigConstants.VULNSCAN_DEBUG).getAsBoolean());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_VERBOSE)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_VERBOSE, currentVulnScan.get(ConfigConstants.VULNSCAN_VERBOSE).getAsBoolean());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_MAX_RESPONSE_SIZE)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_MAX_RESPONSE_SIZE, currentVulnScan.get(ConfigConstants.VULNSCAN_MAX_RESPONSE_SIZE).getAsInt());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_MAX_CONNECTIONS)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_MAX_CONNECTIONS, currentVulnScan.get(ConfigConstants.VULNSCAN_MAX_CONNECTIONS).getAsInt());
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_CONNECTION_TIMEOUT)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_CONNECTION_TIMEOUT, currentVulnScan.get(ConfigConstants.VULNSCAN_CONNECTION_TIMEOUT).getAsInt());
            }
            
            // 保留 defaultHeaders, headerTemplates, variables 配置
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_CUSTOM_HEADERS)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_CUSTOM_HEADERS,
                    new Gson().fromJson(currentVulnScan.get(ConfigConstants.VULNSCAN_CUSTOM_HEADERS), Object.class));
            }
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_VARIABLES)) {
                vulnScanMap.put(ConfigConstants.VULNSCAN_VARIABLES, 
                    new Gson().fromJson(currentVulnScan.get(ConfigConstants.VULNSCAN_VARIABLES), Object.class));
            }
            
            // 更新超时和重试配置
            String timeoutText = vulnScanTimeout.getText().trim();
            if (!timeoutText.isEmpty()) {
                try {
                    vulnScanMap.put(ConfigConstants.VULNSCAN_TIMEOUT, Integer.parseInt(timeoutText));
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.vulnscan.timeout.invalid"), false, true);
                    return;
                }
            }
            
            String retriesText = vulnScanRetries.getText().trim();
            if (!retriesText.isEmpty()) {
                try {
                    vulnScanMap.put(ConfigConstants.VULNSCAN_RETRIES, Integer.parseInt(retriesText));
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.vulnscan.retries.invalid"), false, true);
                    return;
                }
            }
            
            // 线程池配置
            Map<String, Object> threadPoolMap = new LinkedHashMap<>();
            String coreThreadsText = vulnScanCoreThreads.getText().trim();
            if (!coreThreadsText.isEmpty()) {
                try {
                    threadPoolMap.put(ConfigConstants.VULNSCAN_CORE_THREADS, Integer.parseInt(coreThreadsText));
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.vulnscan.core.threads.invalid"), false, true);
                    return;
                }
            }
            
            String maxThreadsText = vulnScanMaxThreads.getText().trim();
            if (!maxThreadsText.isEmpty()) {
                try {
                    threadPoolMap.put(ConfigConstants.VULNSCAN_MAX_THREADS, Integer.parseInt(maxThreadsText));
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.vulnscan.max.threads.invalid"), false, true);
                    return;
                }
            }
            
            String queueSizeText = vulnScanQueueSize.getText().trim();
            if (!queueSizeText.isEmpty()) {
                try {
                    threadPoolMap.put(ConfigConstants.VULNSCAN_QUEUE_SIZE, Integer.parseInt(queueSizeText));
                } catch (NumberFormatException e) {
                    showTip(i18n.getString("setting.vulnscan.queue.size.invalid"), false, true);
                    return;
                }
            }
            vulnScanMap.put(ConfigConstants.VULNSCAN_THREAD_POOL, threadPoolMap);
            
            // 报告配置
            Map<String, Object> reportMap = new LinkedHashMap<>();
            String reportDir = vulnScanReportDir.getText().trim();
            if (!reportDir.isEmpty()) {
                reportMap.put(ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR, reportDir);
            }
            
            // 保留原有的命名模板
            if (currentVulnScan.has(ConfigConstants.VULNSCAN_REPORT)) {
                JsonObject currentReport = currentVulnScan.getAsJsonObject(ConfigConstants.VULNSCAN_REPORT);
                if (currentReport.has(ConfigConstants.VULNSCAN_REPORT_NAME_TEMPLATE)) {
                    reportMap.put(ConfigConstants.VULNSCAN_REPORT_NAME_TEMPLATE, 
                        currentReport.get(ConfigConstants.VULNSCAN_REPORT_NAME_TEMPLATE).getAsString());
                }
            }
            
            reportMap.put(ConfigConstants.VULNSCAN_REPORT_AUTO_EXPORT, vulnScanAutoExport.isSelected());
            
            String format = vulnScanReportFormat.getValue();
            if (format != null && !format.isEmpty()) {
                reportMap.put(ConfigConstants.VULNSCAN_REPORT_DEFAULT_FORMAT, format);
            }
            Map<String, Object> headlessMap = new LinkedHashMap<>();
            if (vulnScanHeadlessBrowserPath != null) {
                String browserPath = vulnScanHeadlessBrowserPath.getText().trim();
                headlessMap.put(ConfigConstants.VULNSCAN_HEADLESS_BROWSER_PATH, browserPath);
            }
            vulnScanMap.put(ConfigConstants.VULNSCAN_HEADLESS, headlessMap);

            configMap.put(ConfigConstants.VULNSCAN, vulnScanMap);
            
            if (Constants.saveConfig(configMap)) {
                showTip(i18n.getString("setting.save.success"));
                // 刷新VulnScanConfig缓存
                com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.getInstance().reload();
            } else {
                showTip(i18n.getString("setting.save.failed"), false, true);
            }
            
        } catch (Exception e) {
            showTip(i18n.getString("setting.save.failed.detail", e.getMessage()), false, true);
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    public void testHeadlessCompatibility(ActionEvent event) {
        String configuredPath = vulnScanHeadlessBrowserPath != null ? vulnScanHeadlessBrowserPath.getText().trim() : "";
        showTip(i18n.getString("setting.vulnscan.headless.checking"), true);

        Task<HeadlessHandler.HeadlessCompatibilityResult> task = new Task<HeadlessHandler.HeadlessCompatibilityResult>() {
            @Override
            protected HeadlessHandler.HeadlessCompatibilityResult call() {
                return HeadlessHandler.checkCompatibility(configuredPath);
            }
        };

        task.setOnSucceeded(e -> {
            HeadlessHandler.HeadlessCompatibilityResult result = task.getValue();
            showTip(HeadlessHandler.buildCompatibilityErrorMessage(result), !result.isCompatible(), !result.isCompatible());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String message = ex == null ? i18n.getString("setting.save.failed") : ex.getMessage();
            showTip(i18n.getString("vulnscan.msg.headless.precheck.failed", message), true, true);
        });

        Thread worker = new Thread(task, "headless-compatibility-check");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    public void browseHeadlessBrowserPath(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.getString("setting.vulnscan.headless.browser.path"));
        File selected = chooser.showOpenDialog(an.getScene().getWindow());
        if (selected != null && vulnScanHeadlessBrowserPath != null) {
            vulnScanHeadlessBrowserPath.setText(selected.getAbsolutePath());
        }
    }

    // ==================== HTTP Headers 管理 ====================

    /**
     * 初始化Headers数据
     */
    private void initHeadersData() {
        com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager headerManager =
            com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager.getInstance();

        // 动态生成模板按钮
        if (headerTemplatePane != null) {
            headerTemplatePane.getChildren().clear();
            for (String templateName : headerManager.getTemplateNames()) {
                Button btn = new Button(templateName);
                btn.getStyleClass().add("preset-tag");
                btn.setOnAction(e -> {
                    Map<String, String> templateHeaders = headerManager.getTemplate(templateName);
                    if (!templateHeaders.isEmpty() && defaultHeadersArea != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, String> entry : templateHeaders.entrySet()) {
                            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                        defaultHeadersArea.setText(sb.toString().trim());
                    }
                    showTip(i18n.getString("setting.headers.template.applied", templateName));
                });
                headerTemplatePane.getChildren().add(btn);
            }
        }

        // 加载customHeaders到编辑区
        try {
            JsonObject vulnScanConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.VULNSCAN);
            if (vulnScanConfig == null) return;

            if (vulnScanConfig.has(ConfigConstants.VULNSCAN_CUSTOM_HEADERS)) {
                JsonObject headers = vulnScanConfig.getAsJsonObject(ConfigConstants.VULNSCAN_CUSTOM_HEADERS);
                StringBuilder sb = new StringBuilder();
                for (String key : headers.keySet()) {
                    sb.append(key).append(": ").append(headers.get(key).getAsString()).append("\n");
                }
                if (defaultHeadersArea != null) {
                    defaultHeadersArea.setText(sb.toString().trim());
                }
            }
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void resetHeaders(ActionEvent event) {
        com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager headerManager =
            com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager.getInstance();
        Map<String, String> builtinHeaders = headerManager.getBuiltinDefaultHeaders();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : builtinHeaders.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        if (defaultHeadersArea != null) {
            defaultHeadersArea.setText(sb.toString().trim());
        }
        // 自动持久化
        headerManager.saveCustomHeaders(builtinHeaders);
        showTip(i18n.getString("setting.headers.reset.saved"));
    }

    @FXML
    public void saveHeaders(ActionEvent event) {
        try {
            Map<String, String> headers = parseHeadersText(defaultHeadersArea != null ? defaultHeadersArea.getText() : "");

            com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager headerManager =
                com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager.getInstance();
            if (headerManager.saveCustomHeaders(headers)) {
                showTip(i18n.getString("setting.headers.save.success"));
            } else {
                showTip("保存失败", false, true);
            }
        } catch (Exception e) {
            showTip(i18n.getString("setting.save.failed.detail", e.getMessage()), false, true);
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析Headers文本为Map
     */
    private Map<String, String> parseHeadersText(String text) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) return headers;

        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || !line.contains(":")) continue;

            int colonIndex = line.indexOf(":");
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            if (!key.isEmpty()) {
                headers.put(key, value);
            }
        }
        return headers;
    }

}
