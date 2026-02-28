package com.potato.potatotool.controller.redTeam;

import com.dlsc.gemsfx.CFCheckBox;
import com.sun.javafx.scene.control.skin.DatePickerSkin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.PocSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.MultiTargetSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.core.VulnScanService;
import com.potato.potatotool.content.redTeam.vulnScanner.event.*;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanState;
import com.potato.potatotool.content.redTeam.vulnScanner.model.TaskState;
import com.potato.potatotool.content.redTeam.vulnScanner.report.ReportGenerator;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.PocDatabaseManager;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnDetail;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocUpdater;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 漏洞扫描控制器
 * @author Potato
 * @date 2025/11/03
 */
public class PaneVulScan {
    
    // ==================== UI组件 ====================
    @FXML private StackPane sPane;
    
    // 顶部操作区
    @FXML private TextArea targetField;
    @FXML private Label uploadLabel;
    @FXML private Label startScanLabel;
    @FXML private Label pauseScanLabel;
    @FXML private Label resumeScanLabel;
    @FXML private Label stopScanLabel;
    @FXML private Label scanStateLabel;
    
    // POC格式选择
    @FXML private CFCheckBox nucleiCheckBox;
    @FXML private CFCheckBox gobyCheckBox;
    @FXML private CFCheckBox xrayCheckBox;
    @FXML private CFCheckBox pocsuiteCheckBox;

    // 扫描选项
    @FXML private CFCheckBox enableProxyBox;
    @FXML private CFCheckBox verboseLogBox;
    @FXML private CFCheckBox autoSaveBox;

    // 高级参数（标签过滤）
    @FXML private TextField tagsField;
    
    // 智能扫描选项
    @FXML private ComboBox<String> scanModeComboBox;

    // 特殊模板选项
    @FXML private CFCheckBox enableHeadlessBox;
    @FXML private CFCheckBox enableCodeBox;
    @FXML private CFCheckBox enableFuzzBox;
    @FXML private ComboBox<String> inputTypeComboBox;

    // 特殊分类选项
    @FXML private CFCheckBox includeOsintBox;
    @FXML private CFCheckBox includeTokenSprayBox;

    // POC统计
    @FXML private Label pocTotalLabel;
    @FXML private Label pocNucleiLabel;
    @FXML private Label pocGobyLabel;
    @FXML private Label pocXrayLabel;
    @FXML private Label pocPocsuiteLabel;
    
    // 进度显示
    @FXML private HBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label timeLabel;
    
    // 结果区域
    @FXML private ScrollPane resultScroll;
    @FXML private VBox resultVBox;
    @FXML private Label vulnCountLabel;
    @FXML private ComboBox<String> exportFormatComboBox;
    
    // 统计面板
    @FXML private HBox statsPane;
    @FXML private Label criticalCountLabel;
    @FXML private Label highCountLabel;
    @FXML private Label mediumCountLabel;
    @FXML private Label lowCountLabel;
    @FXML private Label infoCountLabel;
    
    // POC管理弹窗
    @FXML private StackPane pocManageMask;
    @FXML private VBox pocManagePane;
    @FXML private TextField pocSearchField;
    @FXML private ComboBox<String> pocFormatFilter;
    @FXML private ComboBox<String> pocSeverityFilter;
    @FXML private ComboBox<String> pocStatusFilter;
    @FXML private Label pocCountLabel;
    @FXML private Label selectedPocLabel;
    @FXML private MenuItem togglePocMenuItem;
    @FXML private TableView<PocItem> pocTableView;
    @FXML private TableColumn<PocItem, String> pocStatusColumn;
    @FXML private TableColumn<PocItem, String> pocIdColumn;
    @FXML private TableColumn<PocItem, String> pocNameColumn;
    @FXML private TableColumn<PocItem, String> pocFormatColumn;
    @FXML private TableColumn<PocItem, String> pocSeverityColumn;
    @FXML private TableColumn<PocItem, String> pocTagsColumn;
    @FXML private TableColumn<PocItem, String> pocProtocolColumn;
    @FXML private CFCheckBox selectAllCheckBox;
    
    // POC详情弹窗
    @FXML private StackPane pocDetailMask;
    @FXML private VBox pocDetailPane;
    @FXML private VBox pocDetailContent;
    
    // 日志查看弹窗
    @FXML private StackPane logViewerMask;
    @FXML private VBox logViewerPane;
    @FXML private ComboBox<String> logLevelFilter;
    @FXML private TextField logSearchField;
    @FXML private TextArea logTextArea;
    @FXML private Label logCountLabel;
    
    // 历史记录弹窗
    @FXML private StackPane historyMask;
    @FXML private VBox historyPane;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<HistoryItem> historyTableView;
    @FXML private TableColumn<HistoryItem, String> historyTimeColumn;
    @FXML private TableColumn<HistoryItem, String> historyTargetColumn;
    @FXML private TableColumn<HistoryItem, String> historyVulnCountColumn;
    @FXML private TableColumn<HistoryItem, String> historyStatusColumn;
    
    // 提示框
    @FXML private StackPane promptPane;
    @FXML private Label promptLabel;
    
    // ==================== 服务层 ====================
    private VulnScanService scanService;
    private VulnScanDatabase database;
    private ReportGenerator reportGenerator;
    
    // ==================== 任务管理 ====================
    private Task<Void> currentTask;
    private Thread currentThread;
    private List<ScanResult> scanResults = new ArrayList<>();
    private long scanStartTime = 0;
    private String currentScanDuration = null; // 当前扫描耗时（用于历史记录导出）
    private HBox currentLoadingBox = null;
    private RotateTransition rotateTransition = null;
    private ScanEventListener scanEventListener; // 扫描事件监听器
    private ScanLogger.LogListener logListener; // 日志监听器
    private javafx.animation.Timeline scanTimer; // 扫描计时器（每秒更新时间显示）
    
    // ==================== 数据 ====================
    private ObservableList<PocItem> allPocItems = FXCollections.observableArrayList();
    
    @FXML
    void initialize() {
        // 初始化服务
        scanService = VulnScanService.getInstance();
        database = VulnScanDatabase.getInstance();
        reportGenerator = new ReportGenerator();

        // 初始化UI
        initializeUI();
        
        // 初始化POC管理增强功能
        initPocManageEnhanced();
        
        // 加载POC
        loadPocs();
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
        
        // 检查未完成的扫描-暂时跳过
        // checkPausedScans();
    }
    
    /**
     * 检查是否有未完成的扫描
     */
    private void checkPausedScans() {
        // 延迟检查，确保UI完全加载
        Platform.runLater(() -> {
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待1秒
                    List<ScanState> pausedScans = scanService.loadPausedScans();
                    
                    if (!pausedScans.isEmpty()) {
                        Platform.runLater(() -> showPausedScansNotification(pausedScans));
                    }
                } catch (Exception e) {
                    if (debugMode) {
                        System.err.println("检查未完成扫描失败: " + e.getMessage());
                    }
                }
            }).start();
        });
    }
    
    /**
     * 显示未完成扫描的通知
     */
    private void showPausedScansNotification(List<ScanState> pausedScans) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtils.getString("vulnscan.dialog.pausedscan.title"));
        alert.setHeaderText(I18nUtils.getString("vulnscan.dialog.pausedscan.header", pausedScans.size()));
        
        StringBuilder content = new StringBuilder();
        content.append(I18nUtils.getString("vulnscan.dialog.pausedscan.content")).append("\n\n");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < Math.min(pausedScans.size(), 5); i++) {
            ScanState state = pausedScans.get(i);
            String timeStr = sdf.format(new Date(state.getStartTime()));
            String progress = String.format("%.1f%%", state.getProgress() * 100);
            content.append(String.format("%d. %s - %s (%d, %d)\n",
                i + 1, timeStr, progress, 
                state.getTargets().size(), state.getVulnerabilitiesFound()));
        }
        
        if (pausedScans.size() > 5) {
            content.append("\n...").append(pausedScans.size() - 5).append(" more\n");
        }
        
        content.append("\n").append(I18nUtils.getString("vulnscan.dialog.pausedscan.ask"));
        
        alert.setContentText(content.toString());
        
        ButtonType resumeButton = new ButtonType(I18nUtils.getString("vulnscan.dialog.pausedscan.resume"), ButtonBar.ButtonData.OK_DONE);
        ButtonType laterButton = new ButtonType(I18nUtils.getString("vulnscan.dialog.pausedscan.later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(resumeButton, laterButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == resumeButton) {
            showResumeDialog();
        }
    }
    
    /**
     * 从 UI 构建扫描配置
     */
    private ScanConfig buildScanConfig() {
        ScanConfig config = new ScanConfig();

        // 基本参数 - 从设置模块读取
        VulnScanConfig vulnConfig = VulnScanConfig.getInstance();
        config.setThreads(vulnConfig.getDefaultThreads());
        config.setTimeout(vulnConfig.getDefaultTimeout());
        config.setRetries(vulnConfig.getDefaultRetries());

        // POC 格式筛选
        Set<String> enabledFormats = new HashSet<>();
        if (nucleiCheckBox.isSelected()) enabledFormats.add("nuclei");
        if (gobyCheckBox.isSelected()) enabledFormats.add("goby");
        if (xrayCheckBox.isSelected()) enabledFormats.add("xray");
        if (pocsuiteCheckBox.isSelected()) enabledFormats.add("pocsuite");
        config.setEnabledPocFormats(enabledFormats);

        // 智能扫描选项
        String scanModeStr = scanModeComboBox.getValue();
        ScanConfig.ScanMode scanMode = parseScanMode(scanModeStr);
        config.setScanMode(scanMode);

        // 智能特性默认全部启用
        config.setSkipFingerprint(false);
        config.setEnableDeduplication(true);
        config.setEnableResponseCache(true);
        config.setEnableClustering(true);

        // 特殊模板选项
        config.setEnableHeadless(enableHeadlessBox.isSelected());
        config.setEnableCode(enableCodeBox.isSelected());
        config.setEnableFuzz(enableFuzzBox != null && enableFuzzBox.isSelected());

        // 特殊分类选项 - 根据用户选择从排除列表中移除
        Set<PocObj.PocCategory> excludedCategories = config.getExcludedCategories();
        if (includeOsintBox != null && includeOsintBox.isSelected()) {
            excludedCategories.remove(PocObj.PocCategory.OSINT);
        }
        if (includeTokenSprayBox != null && includeTokenSprayBox.isSelected()) {
            excludedCategories.remove(PocObj.PocCategory.TOKEN_SPRAY);
            excludedCategories.remove(PocObj.PocCategory.CREDENTIAL_STUFFING);
        }

        // 输入类型
        int inputIdx = inputTypeComboBox.getSelectionModel().getSelectedIndex();
        switch (inputIdx) {
            case 1: // URL
                config.setInputType(InputType.URL);
                config.setAutoDetectInputType(false);
                break;
            case 2: // IP:Port
                config.setInputType(InputType.IP_PORT);
                config.setAutoDetectInputType(false);
                break;
            case 4: // Local Path
                config.setInputType(InputType.LOCAL_PATH);
                config.setAutoDetectInputType(false);
                break;
            default: // Auto Detect / Domain
                config.setAutoDetectInputType(true);
                break;
        }

        // 标签过滤
        String tags = tagsField.getText();
        if (tags != null && !tags.trim().isEmpty()) {
            config.setProtocol(tags.trim().toLowerCase());
        }

        // 代理设置
        if (enableProxyBox.isSelected()) {
            // TODO: 从系统配置中获取代理设置
        }

        // 调试模式
        config.setDebug(debugMode || verboseLogBox.isSelected());

        // 模式特定配置
        switch (scanMode) {
            case QUICK:
                config.setMinSeverity(PocObj.Severity.HIGH);
                break;
            case DEEP:
                config.setEnableHeadless(true);
                config.setEnableCode(true);
                config.setEnableFuzz(true);
                break;
            case OSINT:
                config.getExcludedCategories().remove(PocObj.PocCategory.OSINT);
                break;
            default:
                break;
        }

        return config;
    }

    /**
     * 解析扫描模式字符串
     */
    private ScanConfig.ScanMode parseScanMode(String modeStr) {
        int idx = scanModeComboBox.getSelectionModel().getSelectedIndex();
        switch (idx) {
            case 0: return ScanConfig.ScanMode.DISCOVERY;
            case 1: return ScanConfig.ScanMode.QUICK;
            case 2: return ScanConfig.ScanMode.STANDARD;
            case 3: return ScanConfig.ScanMode.DEEP;
            case 4: return ScanConfig.ScanMode.OSINT;
            case 5: return ScanConfig.ScanMode.COMPLIANCE;
            case 6: return ScanConfig.ScanMode.CUSTOM;
            default: return ScanConfig.ScanMode.STANDARD;
        }
    }

    private void initializeUI() {
        // 扫描模式切换联动
        scanModeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            ScanConfig.ScanMode mode = parseScanMode(null);
            updateOptionsForMode(mode);
            String hintKey = "vulnscan.mode." + mode.name().toLowerCase() + ".hint";
            updateScanStateLabel(I18nUtils.getString(hintKey));
        });

        // 输入类型切换联动
        inputTypeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateModeForInputType(newVal.intValue());
        });

        // 初始化默认模式：STANDARD (index=2)
        scanModeComboBox.getSelectionModel().select(2);
        updateOptionsForMode(ScanConfig.ScanMode.STANDARD);
        // 初始化导出格式
        exportFormatComboBox.getSelectionModel().select(0);
        
        // 初始化POC管理表格
        pocStatusColumn.setText(I18nUtils.getString("vulnscan.column.status"));
        pocIdColumn.setText(I18nUtils.getString("vulnscan.column.id"));
        pocNameColumn.setText(I18nUtils.getString("vulnscan.column.name"));
        pocFormatColumn.setText(I18nUtils.getString("vulnscan.column.format"));
        pocSeverityColumn.setText(I18nUtils.getString("vulnscan.column.severity"));
        pocTagsColumn.setText(I18nUtils.getString("vulnscan.column.tags"));
        pocProtocolColumn.setText(I18nUtils.getString("vulnscan.column.protocol"));
        pocIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        pocNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        pocFormatColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormat()));
        pocSeverityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity()));
        pocTagsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTags()));
        pocProtocolColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProtocol()));
        pocTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pocTableView.setItems(allPocItems);

        // 初始化历史记录表格
        historyTimeColumn.setText(I18nUtils.getString("vulnscan.column.scantime"));
        historyTargetColumn.setText(I18nUtils.getString("vulnscan.column.target"));
        historyVulnCountColumn.setText(I18nUtils.getString("vulnscan.column.vulncount"));
        historyStatusColumn.setText(I18nUtils.getString("vulnscan.column.status"));
        historyTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScanTime()));
        historyTargetColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargets()));
        historyVulnCountColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVulns()));
        historyStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        historyTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 动态文本组件初始化（不能用FXML userData绑定，因为后续会setText）
        scanStateLabel.setText(I18nUtils.getString("vulnscan.status.ready"));
        selectAllCheckBox.setText(I18nUtils.getString("vulnscan.selectall"));
        togglePocMenuItem.setText(I18nUtils.getString("vulnscan.ctx.poc.3"));

        // 设置默认日期
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());

        // DatePicker popup 样式注入
        String vulScanCss = getClass().getResource("/css/PaneVulScan.css").toExternalForm();
        String commonCss = getClass().getResource("/css/common.css").toExternalForm();
        for (DatePicker dp : new DatePicker[]{startDatePicker, endDatePicker}) {
            dp.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                if (isShowing) {
                    Platform.runLater(() -> {
                        try {
                            DatePickerSkin skin = (DatePickerSkin) dp.getSkin();
                            Node popupContent = skin.getPopupContent();
                            if (popupContent != null && popupContent.getScene() != null) {
                                ObservableList<String> sheets = popupContent.getScene().getStylesheets();
                                if (!sheets.contains(commonCss)) sheets.add(commonCss);
                                if (!sheets.contains(vulScanCss)) sheets.add(vulScanCss);
                            }
                        } catch (Exception ignored) {}
                    });
                }
            });
        }

        // POC搜索监听
        pocSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterPocs(newVal));

        // 日志筛选监听 - 级别过滤
        logLevelFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (logViewerMask != null && logViewerMask.isVisible()) {
                refreshLogs(null);
            }
        });

        // 日志筛选监听 - 关键词搜索（使用延迟刷新避免频繁更新）
        if (logSearchField != null) {
            logSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (logViewerMask != null && logViewerMask.isVisible()) {
                    refreshLogs(null);
                }
            });
        }
    }

    // ==================== POC加载 ====================
    
    private void loadPocs() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    scanService.loadDefaultPocs();
                    Platform.runLater(() -> {
                        updatePocStats();
                        showPrompt(I18nUtils.getString("vulnscan.msg.poc.loaded"), false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showPrompt(I18nUtils.getString("vulnscan.msg.poc.loadfailed", e.getMessage()), true);
                    });
                    if (debugMode) e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    private void updatePocStats() {
        try {
            // 从数据库获取所有POC实体（包括禁用的）
            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
            List<PocDatabaseManager.PocEntity> entities = dbManager.getAllPocEntities();

            if (entities == null || entities.isEmpty()) {
                if (debugMode) System.err.println("updatePocStats: 数据库中无POC");
                return;
            }

            int total = entities.size();
            int nucleiCount = 0, gobyCount = 0, xrayCount = 0, pocsuiteCount = 0;

            // 更新POC列表，保留启用状态
            if (allPocItems != null) {
                allPocItems.clear();
                for (PocDatabaseManager.PocEntity entity : entities) {
                    PocObj.Poc poc = entity.getParsedContent();
                    if (poc != null) {
                        // 确保POC的ID与数据库主键一致（用于后续查询详情）
                        poc.setId(entity.getId());
                        allPocItems.add(new PocItem(poc, entity.isEnabled()));

                        // 统计格式
                        String format = entity.getOriginalFormat();
                        if ("nuclei".equals(format)) nucleiCount++;
                        else if ("goby".equals(format)) gobyCount++;
                        else if ("xray".equals(format)) xrayCount++;
                        else if ("pocsuite".equals(format)) pocsuiteCount++;
                    }
                }
            }

            // 更新UI组件
            if (pocTotalLabel != null) pocTotalLabel.setText(String.valueOf(total));
            if (pocNucleiLabel != null) pocNucleiLabel.setText(String.valueOf(nucleiCount));
            if (pocGobyLabel != null) pocGobyLabel.setText(String.valueOf(gobyCount));
            if (pocXrayLabel != null) pocXrayLabel.setText(String.valueOf(xrayCount));
            if (pocPocsuiteLabel != null) pocPocsuiteLabel.setText(String.valueOf(pocsuiteCount));

            // 更新POC计数标签
            updatePocCountLabel();
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("updatePocStats失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    public void refreshPocList(ActionEvent event) {
        loadPocs();
    }
    
    @FXML
    public void managePoc(ActionEvent event) {
        pocManageMask.setVisible(true);
        pocManageMask.setManaged(true);
        pocManageMask.toFront();
    }
    
    @FXML
    public void closePocManage(MouseEvent event) {
        pocManageMask.setVisible(false);
        pocManageMask.setManaged(false);
    }
    
    @FXML
    public void closePocManageByMask(MouseEvent event) {
        // 点击遮罩层关闭弹窗
        closePocManage(event);
    }
    
    @FXML
    public void consumeEvent(MouseEvent event) {
        // 阻止事件冒泡，防止点击弹窗内容时关闭
        event.consume();
    }
    
    @FXML
    public void importPoc(ActionEvent event) {
        // 创建选择类型对话框
        Alert typeAlert = new Alert(Alert.AlertType.CONFIRMATION);
        typeAlert.setTitle(I18nUtils.getString("vulnscan.dialog.import.title"));
        typeAlert.setHeaderText(I18nUtils.getString("vulnscan.dialog.import.header"));
        typeAlert.setContentText(I18nUtils.getString("vulnscan.dialog.import.content"));

        ButtonType fileButton = new ButtonType(I18nUtils.getString("vulnscan.dialog.import.file"));
        ButtonType dirButton = new ButtonType(I18nUtils.getString("vulnscan.dialog.import.dir"));
        ButtonType cancelButton = new ButtonType(I18nUtils.getString("vulnscan.dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        
        typeAlert.getButtonTypes().setAll(fileButton, dirButton, cancelButton);
        
        Optional<ButtonType> result = typeAlert.showAndWait();
        if (!result.isPresent() || result.get() == cancelButton) {
            return;
        }
        
        Stage stage = (Stage) sPane.getScene().getWindow();
        List<File> filesToImport = new ArrayList<>();
        
        if (result.get() == fileButton) {
            // 选择文件
            FileChooser chooser = new FileChooser();
            chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.pocfile"));
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.pocfiles"), "*.yaml", "*.yml", "*.json"),
                new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.allfiles"), "*.*")
            );
            List<File> files = chooser.showOpenMultipleDialog(stage);
            if (files != null) {
                filesToImport.addAll(files);
            }
        } else {
            // 选择目录
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(I18nUtils.getString("vulnscan.filechooser.pocdir"));
            File dir = dirChooser.showDialog(stage);
            if (dir != null) {
                filesToImport.add(dir);
            }
        }
        
        if (filesToImport.isEmpty()) {
            return;
        }
        
        // 在后台线程执行导入
        showPrompt(I18nUtils.getString("vulnscan.msg.importing"), false);
        
        new Thread(() -> {
            try {
                PocDatabaseManager pocDbManager = PocDatabaseManager.getInstance();
                PocLoader pocLoader = new PocLoader();
                final int[] totalImported = {0};
                final int[] totalSkipped = {0};
                final int[] totalFailed = {0};

                for (File file : filesToImport) {
                    try {
                        if (file.isDirectory()) {
                            // 递归加载目录中的所有POC文件
                            Files.walk(file.toPath())
                                .filter(Files::isRegularFile)
                                .filter(p -> {
                                    String name = p.getFileName().toString().toLowerCase();
                                    return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
                                })
                                .forEach(pocFile -> {
                                    try {
                                        // 读取原始内容
                                        String originalContent = new String(Files.readAllBytes(pocFile), StandardCharsets.UTF_8);
                                        // 解析POC
                                        PocObj.Poc poc = pocLoader.loadFromFile(pocFile.toString());
                                        if (poc != null) {
                                            PocDatabaseManager.ImportResult importResult =
                                                pocDbManager.insertPoc(poc, originalContent, pocFile.getFileName().toString(), "import");
                                            if (importResult.isSuccess()) {
                                                totalImported[0]++;
                                                scanService.getPocRepository().addPoc(poc);
                                            } else if (importResult.getDuplicateType() != null) {
                                                totalSkipped[0]++;
                                            } else {
                                                totalFailed[0]++;
                                            }
                                        } else {
                                            totalFailed[0]++;
                                        }
                                    } catch (Exception e) {
                                        totalFailed[0]++;
                                        if (debugMode) {
                                            System.err.println("导入POC失败: " + pocFile + " - " + e.getMessage());
                                        }
                                    }
                                });
                        } else {
                            // 单个文件
                            // 读取原始内容
                            String originalContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                            // 解析POC
                            PocObj.Poc poc = pocLoader.loadFromFile(file.getAbsolutePath());
                            if (poc != null) {
                                try {
                                    PocDatabaseManager.ImportResult importResult =
                                        pocDbManager.insertPoc(poc, originalContent, file.getName(), "import");
                                    if (importResult.isSuccess()) {
                                        totalImported[0]++;
                                        scanService.getPocRepository().addPoc(poc);
                                    } else if (importResult.getDuplicateType() != null) {
                                        totalSkipped[0]++;
                                    } else {
                                        totalFailed[0]++;
                                    }
                                } catch (Exception e) {
                                    totalFailed[0]++;
                                    if (debugMode) {
                                        System.err.println("导入POC失败: " + file.getName() + " - " + e.getMessage());
                                    }
                                }
                            } else {
                                totalFailed[0]++;
                            }
                        }
                    } catch (Exception e) {
                        totalFailed[0]++;
                        if (debugMode) {
                            System.err.println("加载POC失败: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }

                showPrompt(I18nUtils.getString("vulnscan.msg.import.result",
                    totalImported[0], totalSkipped[0], totalFailed[0]), false);
                // 刷新POC列表
                loadPocs();

            } catch (Exception e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.import.failed", e.getMessage()), true);
                if (debugMode) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void filterPocs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            pocTableView.setItems(allPocItems);
            return;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        ObservableList<PocItem> filtered = FXCollections.observableArrayList();
        for (PocItem item : allPocItems) {
            if (item.getId().toLowerCase().contains(lowerKeyword) ||
                item.getName().toLowerCase().contains(lowerKeyword) ||
                item.getTags().toLowerCase().contains(lowerKeyword)) {
                filtered.add(item);
            }
        }
        pocTableView.setItems(filtered);
    }
    
    // ==================== CheckBox事件 ====================
    
    @FXML
    public void checkNuclei(MouseEvent event) {
        nucleiCheckBox.setSelected(!nucleiCheckBox.isSelected());
    }
    
    @FXML
    public void checkGoby(MouseEvent event) {
        gobyCheckBox.setSelected(!gobyCheckBox.isSelected());
    }
    
    @FXML
    public void checkXray(MouseEvent event) {
        xrayCheckBox.setSelected(!xrayCheckBox.isSelected());
    }
    
    @FXML
    public void checkPocsuite(MouseEvent event) {
        pocsuiteCheckBox.setSelected(!pocsuiteCheckBox.isSelected());
    }
    
    @FXML
    public void checkProxy(MouseEvent event) {
        enableProxyBox.setSelected(!enableProxyBox.isSelected());
    }
    
    @FXML
    public void selectAllFormats(ActionEvent event) {
        nucleiCheckBox.setSelected(true);
        gobyCheckBox.setSelected(true);
        xrayCheckBox.setSelected(true);
        pocsuiteCheckBox.setSelected(true);
    }
    
    @FXML
    public void invertFormats(ActionEvent event) {
        nucleiCheckBox.setSelected(!nucleiCheckBox.isSelected());
        gobyCheckBox.setSelected(!gobyCheckBox.isSelected());
        xrayCheckBox.setSelected(!xrayCheckBox.isSelected());
        pocsuiteCheckBox.setSelected(!pocsuiteCheckBox.isSelected());
    }
    
    @FXML
    public void clearResults(ActionEvent event) {
        resultVBox.getChildren().clear();
        scanResults.clear();
        vulnCountLabel.setText(I18nUtils.getString("vulnscan.vuln.count.zero"));
        currentLoadingBox = null;
        if (rotateTransition != null) {
            rotateTransition.stop();
        }
        showPrompt(I18nUtils.getString("vulnscan.msg.results.cleared"), false);
    }
    
    // ==================== 扫描执行 ====================
    
    @FXML
    public void uploadTargets(MouseEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.target"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.txt"), "*.txt")
        );
        
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                String content = String.join("\n", lines);
                targetField.setText(content);
                showPrompt(I18nUtils.getString("vulnscan.msg.imported", lines.size()), false);
            } catch (Exception e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.file.importfailed", e.getMessage()), true);
            }
        }
    }
    
    @FXML
    public void startQuickScan(MouseEvent event) {
        startScan(null);
    }
    
    @FXML
    public void startScan(ActionEvent event) {
        String targetText = targetField.getText().trim();
        if (targetText.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.target.empty"), true);
            return;
        }
        
        // 解析目标
        List<String> targets = Arrays.stream(targetText.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith("#"))
            .collect(Collectors.toList());
        
        if (targets.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.target.novalid"), true);
            return;
        }
        
        // 清空结果
        scanResults.clear();
        resultVBox.getChildren().clear();
        currentLoadingBox = null;
        vulnCountLabel.setText(I18nUtils.getString("vulnscan.vuln.count.zero"));
        
        // 显示进度区域
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(0);
        progressLabel.setText("0%");
        scanStateLabel.setText(I18nUtils.getString("vulnscan.scan.preparing"));

        // 更新按钮状态为运行中
        updateButtonsForStatus(ScanState.Status.RUNNING);

        // 添加loading状态
        updateResultVBox(I18nUtils.getString("vulnscan.scan.loading"), false, null);

        // 开始扫描
        scanStartTime = System.currentTimeMillis();
        currentScanDuration = null; // 重置扫描耗时（用于新扫描）
        startScanTimer(); // 启动计时器（每秒更新时间显示）
        startScanTask(targets);
    }
    
    private void startScanTask(List<String> targets) {
        // 构建扫描配置（在UI线程读取UI组件状态）
        ScanConfig config = buildScanConfig();

        // 移除旧的事件监听器（如果存在）
        if (scanEventListener != null) {
            scanService.removeEventListener(scanEventListener);
        }

        // 创建并注册新的事件监听器
        scanEventListener = new ScanEventListener() {
            @Override
            public void onScanStarted(ScanStartedEvent event) {
                Platform.runLater(() -> {
                    updateScanStateLabel(I18nUtils.getString("vulnscan.scan.scanning"));
                    updateResultVBox(I18nUtils.getString("vulnscan.scan.started", event.getTotalTasks()), true, null);
                });
            }

            @Override
            public void onScanProgress(ScanProgressEvent event) {
                Platform.runLater(() -> {
                    double progress = event.getProgressPercentage() / 100.0;
                    progressBar.setProgress(progress);
                    progressLabel.setText(String.format("%.1f%%", progress * 100));
                    // 时间更新由独立的 scanTimer 计时器处理，每秒更新一次
                });
            }

            @Override
            public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
                Platform.runLater(() -> {
                    ScanResult result = event.getResult();
                    scanResults.add(result);
                    addVulnResultToUI(result);
                    updateStatistics(scanResults);
                });
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                Platform.runLater(() -> {
                    // 检查是否是因为暂停而触发的完成事件
                    if (!scanService.isPaused()) {
                        stopScanTimer(); // 停止计时器
                        updateButtonsForStatus(ScanState.Status.COMPLETED);
                        updateScanStateLabel(I18nUtils.getString("vulnscan.status.completed"));
                        updateResultVBox(I18nUtils.getString("vulnscan.scan.completed", scanResults.size()), true, null);
                        updateStatistics(scanResults);

                        // 自动保存
                        if (autoSaveBox.isSelected() && !scanResults.isEmpty()) {
                            saveToDatabase(null);
                        }
                    }
                    // 如果是暂停状态，保持暂停按钮的显示状态
                });
            }

            @Override
            public void onScanError(ScanErrorEvent event) {
                Platform.runLater(() -> {
                    if (verboseLogBox.isSelected()) {
                        ScanLogger.getInstance().error("SCAN", event.getErrorMessage());
                    }
                });
            }

            @Override
            public void onScanInfo(ScanInfoEvent event) {
                Platform.runLater(() -> updateResultVBox(event.getMessage(), false, null));
            }
        };

        scanService.addEventListener(scanEventListener);

        // 4. 同步扫描配置到 ScanEngine
        scanService.applyScanConfig(config);

        // 5. 设置目标
        scanService.clearTargets();
        for (String target : targets) {
            scanService.addTarget(target);
        }

        // 6. 启动异步扫描任务（POC筛选+扫描都在后台线程执行，避免阻塞UI）
        currentTask = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    List<PocObj.Poc> selectedPocs;

                    // 指纹识别阶段：更新UI状态
                    Platform.runLater(() ->
                        updateScanStateLabel(I18nUtils.getString("vulnscan.scan.fingerprinting")));

                    if (isCancelled()) return null;

                    if (targets.size() == 1) {
                        // 单目标：原有路径
                        PocSelectionResult selection = scanService.selectAndFilterPocs(targets.get(0), config);
                        if (isCancelled()) return null;
                        selectedPocs = selection.getPocs();

                        if (selectedPocs.isEmpty()) {
                            Platform.runLater(() -> {
                                stopScanTimer();
                                updateButtonsForStatus(ScanState.Status.STOPPED);
                                progressBox.setVisible(false);
                                progressBox.setManaged(false);
                                showPrompt(I18nUtils.getString("vulnscan.msg.scan.nopoc"), true);
                            });
                            return null;
                        }

                        Platform.runLater(() ->
                            updateResultVBox(I18nUtils.getString("vulnscan.scan.selected", selectedPocs.size()), false, null));

                        scanService.startScan(selectedPocs, selection.getFingerprint());
                    } else {
                        // 多目标：逐目标指纹识别，POC 取并集
                        MultiTargetSelectionResult selection = scanService.selectAndFilterPocsMultiTarget(targets, config);
                        if (isCancelled()) return null;
                        selectedPocs = selection.getPocs();

                        if (selectedPocs.isEmpty()) {
                            Platform.runLater(() -> {
                                stopScanTimer();
                                updateButtonsForStatus(ScanState.Status.STOPPED);
                                progressBox.setVisible(false);
                                progressBox.setManaged(false);
                                showPrompt(I18nUtils.getString("vulnscan.msg.scan.nopoc"), true);
                            });
                            return null;
                        }

                        Platform.runLater(() ->
                            updateResultVBox(I18nUtils.getString("vulnscan.scan.selected", selectedPocs.size()), false, null));

                        scanService.startScan(selectedPocs, selection.getFingerprintMap());
                    }
                } catch (Exception e) {
                    if (isCancelled()) return null;
                    Platform.runLater(() -> {
                        showPrompt(I18nUtils.getString("vulnscan.msg.scan.error", e.getMessage()), true);
                        if (debugMode) e.printStackTrace();
                    });
                }
                return null;
            }
        };

        currentTask.setOnFailed(e -> {
            updateButtonsForStatus(ScanState.Status.STOPPED);
            updateScanStateLabel(I18nUtils.getString("vulnscan.scan.failed"));
            Throwable exception = currentTask.getException();
            if (exception != null) {
                showPrompt(I18nUtils.getString("vulnscan.msg.scan.failed", exception.getMessage()), true);
                if (debugMode) exception.printStackTrace();
            }
        });

        currentThread = new Thread(currentTask);
        currentThread.setDaemon(true);
        currentThread.start();
    }

    @FXML
    public void pauseScan(MouseEvent event) {
        // 如果在指纹识别阶段（currentTask运行中但scanEngine未启动），取消任务
        if (currentTask != null && currentTask.isRunning() && !scanService.isScanning()) {
            currentTask.cancel(true);
            if (currentThread != null) currentThread.interrupt();
            stopScanTimer();
            updateButtonsForStatus(ScanState.Status.STOPPED);
            updateScanStateLabel(I18nUtils.getString("vulnscan.status.stopped"));
            updateResultVBox(I18nUtils.getString("vulnscan.msg.scan.stopped"), true, null);
            return;
        }

        // 检查是否正在扫描且未暂停
        if (!scanService.isScanning()) {
            return;
        }

        if (scanService.isPaused()) {
            // 已经暂停，确保UI状态正确
            updateButtonsForStatus(ScanState.Status.PAUSED);
            updateScanStateLabel(I18nUtils.getString("vulnscan.status.paused"));
            return;
        }

        // 执行暂停操作
        scanService.pauseScan();
        stopScanTimer(); // 停止计时器
        updateButtonsForStatus(ScanState.Status.PAUSED);
        updateScanStateLabel(I18nUtils.getString("vulnscan.status.paused"));
        updateResultVBox(I18nUtils.getString("vulnscan.scan.paused.saved"), true, null);
        showPrompt(I18nUtils.getString("vulnscan.msg.scan.paused"), false);
    }

    @FXML
    public void resumeFromPause(MouseEvent event) {
        // 优先恢复当前正在暂停的扫描
        if (scanService.isPaused() && scanService.isScanning()) {
            try {
                String currentScanId = scanService.getCurrentScanId();
                if (currentScanId != null) {
                    ScanState currentState = scanService.loadScanTask(currentScanId);
                    if (currentState != null) {
                        resumeScanFromState(currentState);
                        return;
                    }
                }
            } catch (Exception e) {
                if (debugMode) {
                    System.err.println("恢复当前扫描失败: " + e.getMessage());
                }
            }
        }

        // 其次使用已加载的任务状态（从历史记录加载的）
        if (loadedTaskState != null && loadedTaskState.getStatus() == ScanState.Status.PAUSED) {
            resumeScanFromState(loadedTaskState);
            loadedTaskState = null; // 清除已使用的状态
            loadedScanId = null;
        }

        // 最后显示未完成扫描列表让用户选择
        // showResumeDialog();
    }

    @FXML
    public void stopScan(MouseEvent event) {
        // 使用 VulnScanService 的停止功能
        if (scanService.isScanning()) {
            scanService.stopScan();
        }

        // 取消当前任务
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        if (currentThread != null) {
            currentThread.interrupt();
        }

        // 停止加载动画
        if (rotateTransition != null) {
            rotateTransition.stop();
        }
        currentLoadingBox = null;

        // 停止计时器
        stopScanTimer();

        // 重置按钮状态
        updateButtonsForStatus(ScanState.Status.STOPPED);

        // 清空进度条和时间
        // progressBar.setProgress(0);
        // progressLabel.setText("0%");
        // timeLabel.setText("00:00:00");

        // 更新状态标签
        updateScanStateLabel(I18nUtils.getString("vulnscan.status.stopped"));
        updateResultVBox(I18nUtils.getString("vulnscan.msg.scan.stopped"), true, null);
        showPrompt(I18nUtils.getString("vulnscan.msg.scan.stopped"), false);
    }

    /**
     * 根据任务状态更新按钮可见性（统一按钮管理入口）
     * RUNNING: 显示暂停、停止
     * PAUSED: 显示恢复、停止
     * STOPPED/COMPLETED/null: 显示开始扫描、上传
     */
    private void updateButtonsForStatus(ScanState.Status status) {
        switch (status) {
            case RUNNING:
                uploadLabel.setVisible(false);
                startScanLabel.setVisible(false);
                startScanLabel.setManaged(false);
                resumeScanLabel.setVisible(false);
                resumeScanLabel.setManaged(false);

                pauseScanLabel.setVisible(true);
                pauseScanLabel.setManaged(true);
                stopScanLabel.setVisible(true);
                stopScanLabel.setManaged(true);
                break;

            case PAUSED:
                uploadLabel.setVisible(false);
                startScanLabel.setVisible(false);
                startScanLabel.setManaged(false);
                pauseScanLabel.setVisible(false);
                pauseScanLabel.setManaged(false);

                resumeScanLabel.setVisible(true);
                resumeScanLabel.setManaged(true);
                stopScanLabel.setVisible(true);
                stopScanLabel.setManaged(true);
                break;

            case STOPPED:
            case COMPLETED:
            default:
                // 默认状态：显示上传和开始扫描按钮
                uploadLabel.setVisible(true);
                startScanLabel.setVisible(true);
                startScanLabel.setManaged(true);

                pauseScanLabel.setVisible(false);
                pauseScanLabel.setManaged(false);
                resumeScanLabel.setVisible(false);
                resumeScanLabel.setManaged(false);
                stopScanLabel.setVisible(false);
                stopScanLabel.setManaged(false);
                break;
        }
    }

    /**
     * 从任务恢复 UI 上下文
     */
    private void restoreUIFromTask(ScanState task) {
        if (task == null) return;

        // 恢复目标
        List<String> targets = task.getTargets();
        if (targets != null && !targets.isEmpty()) {
            targetField.setText(String.join("\n", targets));
        }

        // 恢复进度显示
        int completed = task.getCompletedTasks();
        int total = task.getTotalTasks();
        if (total > 0) {
            double progress = (double) completed / total;
            progressBar.setProgress(progress);
            progressLabel.setText(String.format("%d/%d (%.1f%%)", completed, total, progress * 100));
            progressBox.setVisible(true);
            progressBox.setManaged(true);
        }

        // 更新统计标签
        vulnCountLabel.setText(I18nUtils.getString("vulnscan.vuln.count", task.getVulnerabilitiesFound()));

        // 更新严重度统计
        if (criticalCountLabel != null) criticalCountLabel.setText(String.valueOf(task.getCriticalCount()));
        if (highCountLabel != null) highCountLabel.setText(String.valueOf(task.getHighCount()));
        if (mediumCountLabel != null) mediumCountLabel.setText(String.valueOf(task.getMediumCount()));
        if (lowCountLabel != null) lowCountLabel.setText(String.valueOf(task.getLowCount()));
        if (infoCountLabel != null) infoCountLabel.setText(String.valueOf(task.getInfoCount()));

        // 显示统计面板（如果有漏洞）
        if (task.getVulnerabilitiesFound() > 0 && statsPane != null) {
            statsPane.setVisible(true);
            statsPane.setManaged(true);
        }

        // 更新扫描状态标签
        updateScanStateLabel(getStatusDisplayName(task.getStatus()));
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(ScanState.Status status) {
        if (status == null) return "";
        switch (status) {
            case RUNNING: return I18nUtils.getString("vulnscan.status.running");
            case PAUSED: return I18nUtils.getString("vulnscan.status.paused");
            case STOPPED: return I18nUtils.getString("vulnscan.status.stopped");
            case COMPLETED: return I18nUtils.getString("vulnscan.status.completed");
            default: return "";
        }
    }

    private void showResumeDialog() {
        // 查询未完成的扫描
        List<ScanState> pausedScans = scanService.loadPausedScans();
        
        if (pausedScans.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.scan.noresume"), true);
            return;
        }
        
        // 创建对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.getString("vulnscan.dialog.resume.title"));
        alert.setHeaderText(I18nUtils.getString("vulnscan.dialog.resume.header", pausedScans.size()));
        
        // 创建选择列表
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < pausedScans.size(); i++) {
            ScanState state = pausedScans.get(i);
            String timeStr = sdf.format(new Date(state.getStartTime()));
            String statusStr = getStatusDisplayName(state.getStatus());
            String progress = String.format("%.1f%%", state.getProgress() * 100);
            
            items.add(String.format("[%d] %s | %s | %s | %d | %d",
                i + 1, timeStr, statusStr, progress,
                state.getTargets().size(), state.getVulnerabilitiesFound()));
        }
        
        listView.setItems(items);
        listView.setPrefHeight(200);
        listView.getSelectionModel().select(0);
        
        VBox dialogContent = new VBox(10);
        dialogContent.getChildren().addAll(
            new Label(I18nUtils.getString("vulnscan.dialog.resume.select")),
            listView
        );
        
        alert.getDialogPane().setContent(dialogContent);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < pausedScans.size()) {
                ScanState selectedState = pausedScans.get(selectedIndex);
                resumeScanFromState(selectedState);
            }
        }
    }
    
    private void resumeScanFromState(ScanState state) {
        // 清空当前结果
        scanResults.clear();
        resultVBox.getChildren().clear();
        currentLoadingBox = null;

        // ========== 完整还原UI配置状态 ==========
        try {
            // 1. 还原目标列表
            List<String> targets = state.getTargets();
            if (targets != null && !targets.isEmpty()) {
                targetField.setText(String.join("\n", targets));
            }

            // 2. 还原POC格式选择
            List<String> selectedFormats = state.getSelectedFormats();
            if (selectedFormats != null) {
                nucleiCheckBox.setSelected(selectedFormats.contains("nuclei"));
                gobyCheckBox.setSelected(selectedFormats.contains("goby"));
                xrayCheckBox.setSelected(selectedFormats.contains("xray"));
                pocsuiteCheckBox.setSelected(selectedFormats.contains("pocsuite"));
            }

            // 3. 还原代理设置
            String proxy = state.getProxy();
            if (proxy != null && !proxy.trim().isEmpty()) {
                enableProxyBox.setSelected(true);
            }

            // 4. 还原严重度统计
            criticalCountLabel.setText(String.valueOf(state.getCriticalCount()));
            highCountLabel.setText(String.valueOf(state.getHighCount()));
            mediumCountLabel.setText(String.valueOf(state.getMediumCount()));
            lowCountLabel.setText(String.valueOf(state.getLowCount()));
            infoCountLabel.setText(String.valueOf(state.getInfoCount()));

            // 5. 还原漏洞计数
            int vulnCount = state.getVulnerabilitiesFound();
            vulnCountLabel.setText(I18nUtils.getString("vulnscan.vuln.count", vulnCount));

        } catch (Exception e) {
            if (debugMode) {
                System.err.println("还原UI配置失败: " + e.getMessage());
            }
        }

        // 显示进度区域
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(state.getProgress());
        progressLabel.setText(String.format("%.1f%%", state.getProgress() * 100));
        scanStateLabel.setText(I18nUtils.getString("vulnscan.scan.resuming"));

        // 更新按钮状态为运行中
        updateButtonsForStatus(ScanState.Status.RUNNING);

        // 添加loading状态
        updateResultVBox(I18nUtils.getString("vulnscan.scan.resuming.skip"), false, null);

        // 确保事件监听器存在（应用重启后可能为null）
        ensureEventListenerRegistered();

        // 开始扫描
        scanStartTime = state.getStartTime();
        startScanTimer(); // 启动计时器（每秒更新时间显示）

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateResultVBox(I18nUtils.getString("vulnscan.scan.resuming.remain",
                    state.getTotalTasks() - state.getCompletedTasks()), true, null);

                // 恢复扫描（异步执行，不会阻塞）
                // 扫描完成后会通过 ScanEventListener.onScanCompleted 事件通知
                scanService.resumeScan(state);

                return null;
            }
        };

        // 注意：不使用 setOnSucceeded 处理扫描完成
        // 因为 resumeScan() 是异步的，call() 方法会立即返回
        // 实际的扫描完成逻辑由 ScanEventListener.onScanCompleted 处理

        task.setOnFailed(e -> {
            updateButtonsForStatus(ScanState.Status.STOPPED);
            Throwable exception = task.getException();
            if (exception != null) {
                showPrompt(I18nUtils.getString("vulnscan.msg.scan.resume.failed", exception.getMessage()), true);
                if (debugMode) exception.printStackTrace();
            }
        });

        currentTask = task;
        currentThread = new Thread(task);
        currentThread.setDaemon(true);
        currentThread.start();
    }
    
    // ==================== 结果展示 (参考PaneInfoSearch的updateEchoVBox) ====================
    
    public void updateResultVBox(String message, boolean isComplete, Map<String, Object> dataMap) {
        if (Platform.isFxApplicationThread()) {
            updateResultUI(message, isComplete, dataMap);
        } else {
            Platform.runLater(() -> updateResultUI(message, isComplete, dataMap));
        }
    }
    
    private void updateResultUI(String message, boolean isComplete, Map<String, Object> dataMap) {
        if (currentLoadingBox == null) {
            currentLoadingBox = new HBox(10);
            currentLoadingBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            
            Label loadingImg = new Label();
            loadingImg.setMaxSize(20, 20);
            loadingImg.setMinSize(20, 20);
            loadingImg.getStyleClass().add("loaddingImg");
            
            rotateTransition = new RotateTransition(Duration.seconds(1), loadingImg);
            rotateTransition.setByAngle(360);
            rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
            rotateTransition.play();
            
            Label textLabel = new Label(message);
            currentLoadingBox.getChildren().addAll(loadingImg, textLabel);
            resultVBox.getChildren().add(currentLoadingBox);
        }
        
        Label loadingImg = (Label) currentLoadingBox.getChildren().get(0);
        Node dynamicContent = currentLoadingBox.getChildren().get(1);
        
        if (isComplete) {
            rotateTransition.stop();
            loadingImg.getStyleClass().remove("loaddingImg");
            loadingImg.getStyleClass().add("endImg");
            loadingImg.setRotate(0);
            
            if (dynamicContent instanceof Label) {
                ((Label) dynamicContent).setText(message);
            }
            
            currentLoadingBox = null;
        } else {
            rotateTransition.play();
            if (dynamicContent instanceof Label) {
                ((Label) dynamicContent).setText(message);
            }
        }
        
        resultScroll.setVvalue(1.0);
    }
    
    private void addVulnResultToUI(ScanResult result) {
        Platform.runLater(() -> {
            TitledPane pane = createVulnDetailPane(result);
            resultVBox.getChildren().add(pane);
            vulnCountLabel.setText(I18nUtils.getString("vulnscan.vuln.count", scanResults.size()));
            resultScroll.setVvalue(1.0);
        });
    }
    
    private TitledPane createVulnDetailPane(ScanResult result) {
        PocObj.Poc poc = result.getPoc();
        String severity = poc.getSeverity().name();

        String title = String.format("[%s] %s - %s", severity, result.getTarget(), poc.getName());

        VBox content = new VBox(8);
        content.getStyleClass().add("vuln-detail-content");

        // 基本信息
        content.getChildren().addAll(
            createDetailRow(I18nUtils.getString("vulnscan.detail.target"), result.getTarget()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.pocname"), poc.getName()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.pocid"), poc.getId()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.severity"), severity),
            createDetailRow(I18nUtils.getString("vulnscan.detail.pocformat"), poc.getOriginalFormat()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.protocol"), poc.getProtocol()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.vulntype"), poc.getVulType() != null ? poc.getVulType() : ""),
            createDetailRow(I18nUtils.getString("vulnscan.detail.description"), poc.getDescription() != null ? poc.getDescription() : "")
        );

        // 检测详情（新增）
        if (result.getMatchedPath() != null && !result.getMatchedPath().isEmpty()) {
            content.getChildren().add(createDetailRow(I18nUtils.getString("vulnscan.detail.matchpath"), result.getMatchedPath()));
        }

        if (result.getMatchedPayload() != null && !result.getMatchedPayload().isEmpty()) {
            content.getChildren().add(createDetailRow("Payload", result.getMatchedPayload()));
        }

        // HTTP 请求数据（新增）
        if (result.getRawRequest() != null && !result.getRawRequest().isEmpty()) {
            content.getChildren().add(createDetailTextArea(I18nUtils.getString("vulnscan.detail.httprequest"), result.getRawRequest(), 6));
        }

        // HTTP 响应数据（新增）
        if (result.getRawResponseSnippet() != null && !result.getRawResponseSnippet().isEmpty()) {
            content.getChildren().add(createDetailTextArea(I18nUtils.getString("vulnscan.detail.httpresponse"), result.getRawResponseSnippet(), 8));
        }

        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.getStyleClass().add("vuln-titled-pane");
        pane.getStyleClass().add("severity-" + severity.toLowerCase());
        return pane;
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        Label labelNode = new Label(label + ":");
        labelNode.setMinWidth(80);
        labelNode.getStyleClass().add("detail-label");
        Label valueNode = new Label(value);
        valueNode.setWrapText(true);
        valueNode.getStyleClass().add("detail-value");
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    /**
     * 创建多行文本详情区域（用于显示请求/响应数据）
     */
    private VBox createDetailTextArea(String label, String value, int rows) {
        VBox box = new VBox(5);

        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().add("detail-label");
        labelNode.setStyle("-fx-font-weight: bold;");

        TextArea textArea = new TextArea(value);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(rows);
        textArea.setMaxHeight(rows * 20 + 20); // 限制最大高度
        textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");

        box.getChildren().addAll(labelNode, textArea);
        return box;
    }
    
    // ==================== 报告导出 ====================
    
    @FXML
    public void exportReport(ActionEvent event) {
        String format = exportFormatComboBox.getValue();
        if (format == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.export.noformat"), true);
            return;
        }

        if (scanResults.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.export.noresult"), true);
            return;
        }

        // 获取扫描耗时
        final String scanDuration;
        if (currentScanDuration != null) {
            // 使用历史记录的耗时
            scanDuration = currentScanDuration;
        } else {
            // 计算实时扫描的耗时
            long elapsedSeconds = 0;
            if (scanStartTime > 0) {
                elapsedSeconds = (System.currentTimeMillis() - scanStartTime) / 1000;
            }
            scanDuration = formatDuration(elapsedSeconds);
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    SimpleDateFormat fileSdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String timestamp = fileSdf.format(new Date());
                    String baseDir = StrUtils.getCurrentJarDir() + File.separator + "VulnScanReports";
                    new File(baseDir).mkdirs();

                    String fileName = "scan_report_" + timestamp;
                    String extension = format.equalsIgnoreCase("Word") ? ".docx" :
                                     format.equalsIgnoreCase("Excel") ? ".xlsx" :
                                     "." + format.toLowerCase();

                    String outputPath = baseDir + File.separator + fileName + extension;
                    // 传递扫描耗时参数
                    File reportFile = reportGenerator.generateReport(format, scanResults, outputPath, scanDuration);

                    showPrompt(I18nUtils.getString("vulnscan.msg.report.exported", reportFile.getAbsolutePath()), false);
                } catch (Exception e) {
                    showPrompt(I18nUtils.getString("vulnscan.msg.report.failed", e.getMessage()), true);
                    if (debugMode) e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    @FXML
    public void saveToDatabase(ActionEvent event) {
        if (scanResults.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.save.noresult"), true);
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Map<String, Object> config = new HashMap<>();
                    config.put("threads", "20");
                    config.put("timeout", "15");
                    config.put("formats", getSelectedFormats());

                    long duration = (System.currentTimeMillis() - scanStartTime) / 1000;

                    // 获取当前扫描任务ID，使用saveOrUpdateScanRecord确保同一扫描只有一条历史记录
                    String currentScanId = scanService.getCurrentScanId();

                    long recordId = database.saveOrUpdateScanRecord(
                        currentScanId,
                        scanResults,
                        config,
                        duration,
                        "completed"
                    );

                    showPrompt(I18nUtils.getString("vulnscan.msg.save.success", recordId), false);
                } catch (Exception e) {
                    showPrompt(I18nUtils.getString("vulnscan.msg.save.failed", e.getMessage()), true);
                    if (debugMode) e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    private String getSelectedFormats() {
        List<String> formats = new ArrayList<>();
        if (nucleiCheckBox.isSelected()) formats.add("Nuclei");
        if (gobyCheckBox.isSelected()) formats.add("Goby");
        if (xrayCheckBox.isSelected()) formats.add("Xray");
        if (pocsuiteCheckBox.isSelected()) formats.add("Pocsuite");
        return String.join(",", formats);
    }
    
    // ==================== 历史记录 ====================
    
    @FXML
    public void showHistory(ActionEvent event) {
        historyMask.setVisible(true);
        historyMask.setManaged(true);
        historyMask.toFront();
        refreshHistoryList();
    }
    
    @FXML
    public void closeHistory(MouseEvent event) {
        historyMask.setVisible(false);
        historyMask.setManaged(false);
    }
    
    @FXML
    public void closeHistoryByMask(MouseEvent event) {
        // 点击遮罩层关闭弹窗
        closeHistory(event);
    }
    
    @FXML
    public void queryHistory(ActionEvent event) {
        refreshHistoryList();
    }
    
    private void refreshHistoryList() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // 从 scan_states 表加载所有扫描任务
                    List<ScanState> scanTasks = scanService.loadAllScanTasks();

                    Platform.runLater(() -> {
                        ObservableList<HistoryItem> items = FXCollections.observableArrayList();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        for (ScanState state : scanTasks) {
                            HistoryItem item = new HistoryItem();
                            // 使用 scan_id 作为 ID（而不是数字 ID）
                            item.setId(state.getScanId());
                            // 格式化开始时间
                            item.setScanTime(sdf.format(new Date(state.getStartTime())));
                            // 目标数量
                            List<String> targets = state.getTargets();
                            item.setTargets(String.valueOf(targets != null ? targets.size() : 0));
                            // POC数量
                            List<String> pocIds = state.getPocIds();
                            item.setPocs(String.valueOf(pocIds != null ? pocIds.size() : 0));
                            // 漏洞数量
                            item.setVulns(String.valueOf(state.getVulnerabilitiesFound()));
                            // 扫描耗时
                            item.setDuration(formatDuration(state.getTotalDuration()));
                            // 状态
                            item.setStatus(getStatusDisplayName(state.getStatus()));
                            items.add(item);
                        }
                        historyTableView.setItems(items);
                        showPrompt(I18nUtils.getString("vulnscan.msg.history.loaded", scanTasks.size()), false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showPrompt(I18nUtils.getString("vulnscan.msg.history.queryfailed", e.getMessage()), true);
                    });
                    if (debugMode) e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    public void loadHistoryResult(ActionEvent event) {
        HistoryItem selected = historyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.history.noselect"), true);
            return;
        }

        // 获取 scan_id（现在是字符串格式的 UUID）
        String scanId = selected.getId();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // 1. 加载扫描任务状态
                    ScanState taskState = scanService.loadScanTask(scanId);
                    if (taskState == null) {
                        Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.history.notfound"), true));
                        return null;
                    }

                    // 2. 加载漏洞结果（从 task_states 表）
                    List<TaskState> vulnerableTasks = database.loadVulnerableTasks(scanId);

                    Platform.runLater(() -> {
                        // 保存历史记录的扫描耗时（用于导出报告）
                        currentScanDuration = selected.getDuration();

                        // 清空当前结果和加载状态
                        resultVBox.getChildren().clear();
                        scanResults.clear();
                        currentLoadingBox = null;
                        if (rotateTransition != null) {
                            rotateTransition.stop();
                        }

                        // 隐藏统计面板
                        hideStatistics();

                        // 恢复 UI 上下文（目标、进度、统计）
                        restoreUIFromTask(taskState);

                        // 更新按钮状态
                        updateButtonsForStatus(taskState.getStatus());

                        // 加载漏洞详情
                        if (vulnerableTasks.isEmpty()) {
                            Label emptyLabel = new Label(I18nUtils.getString("vulnscan.msg.history.novuln"));
                            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 14px; -fx-padding: 20px;");
                            resultVBox.getChildren().add(emptyLabel);
                            showPrompt(I18nUtils.getString("vulnscan.msg.history.loaded.novuln"), false);
                        } else {
                            // 将 TaskState 转换为显示
                            for (TaskState vulnTask : vulnerableTasks) {
                                PocObj.Poc poc = scanService.getPocRepository().getPoc(vulnTask.getPocId());
                                if (poc == null) {
                                    // 尝试从数据库加载
                                    try {
                                        PocDatabaseManager.PocEntity pocEntity = PocDatabaseManager.getInstance().getPocById(vulnTask.getPocId());
                                        if (pocEntity != null) {
                                            poc = pocEntity.getParsedContent();
                                        }
                                    } catch (Exception e) {
                                        if (debugMode) System.err.println("加载POC失败: " + vulnTask.getPocId());
                                    }
                                }

                                if (poc != null) {
                                    // 构建 ScanResult
                                    ScanResult result = new ScanResult();
                                    result.setTarget(vulnTask.getTarget());
                                    result.setPoc(poc);
                                    result.setVulnerable(true);
                                    result.setTimestamp(vulnTask.getEndTime());
                                    scanResults.add(result);

                                    // 创建显示面板
                                    VulnDetail detail = new VulnDetail();
                                    detail.setTarget(vulnTask.getTarget());
                                    detail.setPocId(poc.getId());
                                    detail.setPocName(poc.getName());
                                    detail.setPocFormat(poc.getOriginalFormat());
                                    detail.setSeverity(poc.getSeverity() != null ? poc.getSeverity().name() : "INFO");
                                    detail.setVulnType(poc.getVulType());
                                    detail.setProtocol(poc.getProtocol());
                                    detail.setDescription(poc.getDescription());

                                    TitledPane pane = createHistoryDetailPane(detail);
                                    resultVBox.getChildren().add(pane);
                                }
                            }

                            showPrompt(I18nUtils.getString("vulnscan.msg.history.loaded.vuln", vulnerableTasks.size()), false);
                            updateStatistics(scanResults);
                        }

                        // 关闭历史记录弹窗
                        historyMask.setVisible(false);
                        historyMask.setManaged(false);

                        // 保存当前加载的任务ID（用于后续恢复扫描）
                        loadedScanId = scanId;
                        loadedTaskState = taskState;
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showPrompt(I18nUtils.getString("vulnscan.msg.history.loadfailed", e.getMessage()), true);
                    });
                    if (debugMode) e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    // 保存当前加载的任务状态（用于恢复扫描）
    private String loadedScanId;
    private ScanState loadedTaskState;

    private TitledPane createHistoryDetailPane(VulnDetail detail) {
        String title = String.format("[%s] %s - %s",
            detail.getSeverity(), detail.getTarget(), detail.getPocName());
        
        VBox content = new VBox(8);
        content.getStyleClass().add("vuln-detail-content");
        content.getChildren().addAll(
            createDetailRow(I18nUtils.getString("vulnscan.detail.target"), detail.getTarget()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.poc"), detail.getPocName()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.severity"), detail.getSeverity()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.format"), detail.getPocFormat()),
            createDetailRow(I18nUtils.getString("vulnscan.detail.description"), detail.getDescription() != null ? detail.getDescription() : "")
        );
        
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.getStyleClass().add("vuln-titled-pane");
        return pane;
    }
    
    @FXML
    public void deleteHistory(ActionEvent event) {
        HistoryItem selected = historyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.history.noselect.delete"), true);
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.getString("vulnscan.dialog.delete.title"));
        alert.setHeaderText(I18nUtils.getString("vulnscan.dialog.delete.header"));
        alert.setContentText(I18nUtils.getString("vulnscan.dialog.delete.content"));
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        long scanId = Long.parseLong(selected.getId());
                        database.deleteHistory(scanId);
                        Platform.runLater(() -> {
                            historyTableView.getItems().remove(selected);
                            showPrompt(I18nUtils.getString("vulnscan.msg.history.deleted"), false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showPrompt(I18nUtils.getString("vulnscan.msg.history.deletefailed", e.getMessage()), true);
                        });
                        if (debugMode) e.printStackTrace();
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }
    
    @FXML
    public void clearHistory(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.getString("vulnscan.dialog.clearhistory.title"));
        alert.setHeaderText(I18nUtils.getString("vulnscan.dialog.clearhistory.header"));
        alert.setContentText(I18nUtils.getString("vulnscan.dialog.clearhistory.content"));
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        database.clearAllHistory();
                        Platform.runLater(() -> {
                            historyTableView.getItems().clear();
                            showPrompt(I18nUtils.getString("vulnscan.msg.history.cleared"), false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showPrompt(I18nUtils.getString("vulnscan.msg.history.clearfailed", e.getMessage()), true);
                        });
                        if (debugMode) e.printStackTrace();
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }
    
    @FXML
    public void exportHistory(ActionEvent event) {
        HistoryItem selected = historyTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.history.noselect.export"), true);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.exporthistory"));
        chooser.setInitialFileName("scan_history_" + selected.getId() + ".json");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.json"), "*.json"),
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.csv"), "*.csv"),
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.txt"), "*.txt")
        );

        Stage stage = (Stage) sPane.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            // 在后台线程执行文件写入
            new Thread(() -> {
                try {
                    String fileName = file.getName().toLowerCase();
                    StringBuilder content = new StringBuilder();

                    if (fileName.endsWith(".json")) {
                        // JSON 格式导出
                        Gson gson = new GsonBuilder()
                            .setPrettyPrinting().create();
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", selected.getId());
                        data.put("scanTime", selected.getScanTime());
                        data.put("targets", selected.getTargets());
                        data.put("pocs", selected.getPocs());
                        data.put("vulns", selected.getVulns());
                        data.put("duration", selected.getDuration());
                        data.put("status", selected.getStatus());
                        content.append(gson.toJson(data));
                    } else if (fileName.endsWith(".csv")) {
                        // CSV 格式导出
                        content.append(I18nUtils.getString("vulnscan.export.csv.header")).append("\n");
                        content.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            selected.getId(),
                            selected.getScanTime(),
                            selected.getTargets(),
                            selected.getPocs(),
                            selected.getVulns(),
                            selected.getDuration(),
                            selected.getStatus()
                        ));
                    } else {
                        // 文本格式导出
                        content.append(I18nUtils.getString("vulnscan.export.txt.header")).append("\n\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.id")).append(selected.getId()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.scantime")).append(selected.getScanTime()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.targets")).append(selected.getTargets()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.pocs")).append(selected.getPocs()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.vulns")).append(selected.getVulns()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.duration")).append(selected.getDuration()).append("\n");
                        content.append(I18nUtils.getString("vulnscan.export.txt.status")).append(selected.getStatus()).append("\n");
                        content.append("\n").append(I18nUtils.getString("vulnscan.export.txt.footer")).append("\n");
                    }

                    Files.write(file.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
                    Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.history.exported", file.getName()), false));
                } catch (Exception e) {
                    Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.history.exportfailed", e.getMessage()), true));
                    if (debugMode) e.printStackTrace();
                }
            }).start();
        }
    }
    
    // ==================== 工具方法 ====================
    
    private void showPrompt(String message, boolean isError) {
        Platform.runLater(() -> {
            promptLabel.setText(message);
            promptPane.setVisible(true);
            promptPane.setManaged(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(2));
            
            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeOut.setOnFinished(e -> {
                promptPane.setVisible(false);
                promptPane.setManaged(false);
            });
            
            fadeIn.play();
        });
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    // ==================== POC管理增强功能 ====================
    
    /**
     * 初始化POC管理增强功能
     */
    private void initPocManageEnhanced() {
        // 设置状态列
        if (pocStatusColumn != null) {
            pocStatusColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getStatusText()));
        }
        
        // 设置表格选择模式为多选
        pocTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 监听选择变化 - 使用getSelectedItems()监听多选变化
        pocTableView.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<PocItem>) change -> {
                updateSelectedPocLabel();
                updateToggleMenuItem();
                // 更新全选复选框状态
                if (selectAllCheckBox != null) {
                    int selectedCount = pocTableView.getSelectionModel().getSelectedItems().size();
                    int totalCount = pocTableView.getItems().size();
                    selectAllCheckBox.setSelected(selectedCount == totalCount && totalCount > 0);
                }
            });
        
        // 添加过滤器监听
        if (pocFormatFilter != null) {
            pocFormatFilter.setOnAction(e -> applyPocFilters());
        }
        if (pocSeverityFilter != null) {
            pocSeverityFilter.setOnAction(e -> applyPocFilters());
        }
        if (pocStatusFilter != null) {
            pocStatusFilter.setOnAction(e -> applyPocFilters());
        }
        
        // 搜索框实时过滤
        if (pocSearchField != null) {
            pocSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyPocFilters());
        }

        // 初始化时更新计数
        updatePocCountLabel();
    }
    
    /**
     * 应用POC过滤器
     */
    private void applyPocFilters() {
        String keyword = pocSearchField != null ? pocSearchField.getText() : "";
        int formatIdx = pocFormatFilter != null ? pocFormatFilter.getSelectionModel().getSelectedIndex() : 0;
        int severityIdx = pocSeverityFilter != null ? pocSeverityFilter.getSelectionModel().getSelectedIndex() : 0;
        int statusIdx = pocStatusFilter != null ? pocStatusFilter.getSelectionModel().getSelectedIndex() : 0;
        
        String[] formatNames = {"nuclei", "goby", "xray", "pocsuite"};
        String[] severityNames = {"critical", "high", "medium", "low", "info"};
        
        ObservableList<PocItem> filtered = FXCollections.observableArrayList();
        
        for (PocItem item : allPocItems) {
            // 关键词过滤
            if (!keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                if (!item.getId().toLowerCase().contains(lowerKeyword) &&
                    !item.getName().toLowerCase().contains(lowerKeyword) &&
                    !item.getTags().toLowerCase().contains(lowerKeyword)) {
                    continue;
                }
            }
            
            // 格式过滤
            if (formatIdx > 0 && !formatNames[formatIdx - 1].equalsIgnoreCase(item.getFormat())) {
                continue;
            }
            
            // 严重度过滤
            if (severityIdx > 0 && !severityNames[severityIdx - 1].equalsIgnoreCase(item.getSeverity())) {
                continue;
            }
            
            // 状态过滤
            if (statusIdx == 1 && !item.isEnabled()) {
                continue;
            }
            if (statusIdx == 2 && item.isEnabled()) {
                continue;
            }
            
            filtered.add(item);
        }
        
        pocTableView.setItems(filtered);
        updatePocCountLabel();
    }
    
    /**
     * 更新POC计数标签
     */
    private void updatePocCountLabel() {
        if (pocCountLabel != null) {
            int total = allPocItems.size();
            int showing = pocTableView.getItems().size();
            if (total == showing) {
                pocCountLabel.setText(I18nUtils.getString("vulnscan.poc.count.total", total));
            } else {
                pocCountLabel.setText(I18nUtils.getString("vulnscan.poc.count.filtered", showing, total));
            }
        }
    }
    
    /**
     * 更新选中POC计数
     */
    private void updateSelectedPocLabel() {
        if (selectedPocLabel != null) {
            int count = pocTableView.getSelectionModel().getSelectedItems().size();
            selectedPocLabel.setText(I18nUtils.getString("vulnscan.poc.selected", count));
        }
    }
    
    /**
     * 更新切换状态菜单项文字
     */
    private void updateToggleMenuItem() {
        if (togglePocMenuItem != null) {
            PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                togglePocMenuItem.setText(I18nUtils.getString(selected.isEnabled() ? "vulnscan.dialog.togglepoc.disable" : "vulnscan.dialog.togglepoc.enable"));
            }
        }
    }
    
    @FXML
    public void viewPocDetail(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect"), true);
            return;
        }
        
        showPocDetailDialog(selected);
    }
    
    /**
     * 显示POC详情对话框
     */
    private void showPocDetailDialog(PocItem item) {
        if (pocDetailMask == null || pocDetailContent == null) return;

        pocDetailContent.getChildren().clear();

        // 显示加载提示
        Label loadingLabel = new Label(I18nUtils.getString("vulnscan.msg.poc.loading"));
        loadingLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 14px; -fx-padding: 20px;");
        pocDetailContent.getChildren().add(loadingLabel);

        // 先显示对话框
        pocDetailMask.setVisible(true);
        pocDetailMask.setManaged(true);
        pocDetailMask.toFront();

        // 在后台线程读取数据库
        new Thread(() -> {
            try {
                // 从数据库按需查询原始内容
                PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
                PocDatabaseManager.PocEntity pocEntity = dbManager.getPocById(item.getId());

                Platform.runLater(() -> {
                    pocDetailContent.getChildren().clear();

                    if (pocEntity == null || pocEntity.getOriginalContent() == null) {
                        // POC原始内容不存在，在对话框中显示提示信息
                        Label warningLabel = new Label(I18nUtils.getString("vulnscan.msg.poc.nocontent"));
                        warningLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 14px; -fx-padding: 20px;");

                        Label tipLabel = new Label(I18nUtils.getString("vulnscan.msg.poc.nocontent.tip"));
                        tipLabel.setStyle("-fx-text-fill: gray; -fx-padding: 20px;");
                        tipLabel.setWrapText(true);

                        pocDetailContent.getChildren().addAll(warningLabel, tipLabel);
                    } else {
                        // 显示原始POC文件内容
                        addDetailField(I18nUtils.getString("vulnscan.detail.originalfile"), pocEntity.getOriginalFilename());
                        addDetailField(I18nUtils.getString("vulnscan.detail.pocformat"), pocEntity.getOriginalFormat());

                        // 创建一个可滚动的文本区域显示原始内容
                        Label contentLabel = new Label(I18nUtils.getString("vulnscan.detail.originalcontent"));
                        contentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent;");

                        TextArea contentArea = new TextArea(pocEntity.getOriginalContent());
                        contentArea.setWrapText(false);
                        contentArea.setEditable(false);
                        contentArea.setPrefRowCount(20);
                        contentArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

                        VBox contentBox = new VBox(3);
                        contentBox.getChildren().addAll(contentLabel, contentArea);
                        pocDetailContent.getChildren().add(contentBox);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pocDetailContent.getChildren().clear();

                    // 发生异常时在对话框中显示错误信息
                    Label errorLabel = new Label(I18nUtils.getString("vulnscan.msg.poc.readfailed"));
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20px;");

                    Label errorMsg = new Label(I18nUtils.getString("vulnscan.msg.poc.error", e.getMessage()));
                    errorMsg.setStyle("-fx-text-fill: gray; -fx-padding: 20px;");
                    errorMsg.setWrapText(true);

                    pocDetailContent.getChildren().addAll(errorLabel, errorMsg);
                });
                if (debugMode) e.printStackTrace();
            }
        }).start();
    }
    
    private void addDetailField(String label, String value) {
        if (value == null || value.isEmpty()) return;
        
        VBox fieldBox = new VBox(3);
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent;");
        
        Label valueNode = new Label(value);
        valueNode.setWrapText(true);
        valueNode.setStyle("-fx-padding: 0 0 0 10;");
        
        fieldBox.getChildren().addAll(labelNode, valueNode);
        pocDetailContent.getChildren().add(fieldBox);
    }
    
    @FXML
    public void closePocDetail(MouseEvent event) {
        if (pocDetailMask != null) {
            pocDetailMask.setVisible(false);
            pocDetailMask.setManaged(false);
        }
    }
    
    @FXML
    public void closePocDetailByMask(MouseEvent event) {
        closePocDetail(event);
    }
    
    @FXML
    public void copyPocId(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            copyToClipboard(selected.getId());
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.idcopied", selected.getId()), false);
        }
    }
    
    @FXML
    public void copyPocContent(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect"), true);
            return;
        }

        // 在后台线程读取数据库
        new Thread(() -> {
            try {
                // 从数据库读取原始内容
                PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
                PocDatabaseManager.PocEntity pocEntity = dbManager.getPocById(selected.getId());

                Platform.runLater(() -> {
                    if (pocEntity != null && pocEntity.getOriginalContent() != null) {
                        copyToClipboard(pocEntity.getOriginalContent());
                        showPrompt(I18nUtils.getString("vulnscan.msg.poc.contentcopied"), false);
                    } else {
                        showPrompt(I18nUtils.getString("vulnscan.msg.poc.contentreadfailed"), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showPrompt(I18nUtils.getString("vulnscan.msg.poc.copyfailed", e.getMessage()), true);
                });
                if (debugMode) e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    public void exportPocFile(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect"), true);
            return;
        }

        // 在后台线程读取数据库
        new Thread(() -> {
            try {
                // 从数据库读取原始内容
                PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
                PocDatabaseManager.PocEntity pocEntity = dbManager.getPocById(selected.getId());

                if (pocEntity == null) {
                    Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.poc.dbreadfailed"), true));
                    return;
                }

                String originalContent = pocEntity.getOriginalContent();
                if (originalContent == null || originalContent.trim().isEmpty()) {
                    Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.poc.contentempty"), true));
                    return;
                }

                // 使用数据库中存储的原始文件名（已包含后缀）
                String defaultFilename = pocEntity.getOriginalFilename();
                if (defaultFilename == null || defaultFilename.trim().isEmpty()) {
                    defaultFilename = selected.getId() + ".yaml";
                }

                // 在UI线程显示文件选择器
                String finalFilename = defaultFilename;
                Platform.runLater(() -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.exportpoc"));
                    chooser.setInitialFileName(finalFilename);
                    chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.pocfiles"), "*.yaml", "*.yml", "*.json"),
                        new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.allfiles"), "*.*")
                    );

                    Stage stage = (Stage) sPane.getScene().getWindow();
                    File file = chooser.showSaveDialog(stage);

                    if (file != null) {
                        // 在后台线程写入文件
                        new Thread(() -> {
                            try {
                                Files.write(file.toPath(), originalContent.getBytes(StandardCharsets.UTF_8));
                                Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.poc.exported", file.getName()), false));
                            } catch (Exception e) {
                                Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.poc.writefailed", e.getMessage()), true));
                                if (debugMode) e.printStackTrace();
                            }
                        }).start();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showPrompt(I18nUtils.getString("vulnscan.msg.poc.exportfailed", e.getMessage()), true));
                if (debugMode) e.printStackTrace();
            }
        }).start();
    }
    
    private void copyToClipboard(String text) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
    
    @FXML
    public void togglePocStatus(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean newState = !selected.isEnabled();
            selected.setEnabled(newState);
            pocTableView.refresh();
            updateToggleMenuItem();

            // 同步到数据库
            new Thread(() -> {
                try {
                    PocDatabaseManager.getInstance().setEnabled(selected.getId(), newState);
                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }).start();

            showPrompt(I18nUtils.getString("vulnscan.msg.poc.toggled", selected.getId(), I18nUtils.getString(newState ? "vulnscan.msg.poc.toggled.enable" : "vulnscan.msg.poc.toggled.disable")), false);
        }
    }
    
    @FXML
    public void deletePoc(ActionEvent event) {
        PocItem selected = pocTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect"), true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18nUtils.getString("vulnscan.dialog.deletepoc.title"));
        confirm.setHeaderText(I18nUtils.getString("vulnscan.dialog.deletepoc.header"));
        confirm.setContentText(I18nUtils.getString("vulnscan.dialog.deletepoc.content", selected.getId(), selected.getName()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String pocId = selected.getId();
                allPocItems.remove(selected);
                scanService.getPocRepository().removePoc(pocId);
                applyPocFilters();

                // 同步到数据库
                new Thread(() -> {
                    try {
                        PocDatabaseManager.getInstance().deletePoc(pocId);
                    } catch (Exception e) {
                        if (debugMode) e.printStackTrace();
                    }
                }).start();

                showPrompt(I18nUtils.getString("vulnscan.msg.poc.deleted", pocId), false);
            }
        });
    }
    
    @FXML
    public void deleteSelectedPocs(ActionEvent event) {
        ObservableList<PocItem> selected = pocTableView.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect.delete"), true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18nUtils.getString("vulnscan.dialog.batchdelete.title"));
        confirm.setHeaderText(I18nUtils.getString("vulnscan.dialog.batchdelete.header", selected.size()));
        confirm.setContentText(I18nUtils.getString("vulnscan.dialog.batchdelete.content"));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<PocItem> toRemove = new ArrayList<>(selected);
                List<String> ids = new ArrayList<>();
                for (PocItem item : toRemove) {
                    ids.add(item.getId());
                    scanService.getPocRepository().removePoc(item.getId());
                }
                allPocItems.removeAll(toRemove);
                applyPocFilters();

                // 同步到数据库
                new Thread(() -> {
                    try {
                        PocDatabaseManager.getInstance().batchDelete(ids);
                    } catch (Exception e) {
                        if (debugMode) e.printStackTrace();
                    }
                }).start();

                showPrompt(I18nUtils.getString("vulnscan.msg.poc.batchdeleted", toRemove.size()), false);
            }
        });
    }

    @FXML
    public void exportSelectedPocs(ActionEvent event) {
        ObservableList<PocItem> selectedItems = pocTableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showPrompt(I18nUtils.getString("vulnscan.msg.poc.noselect.export"), true);
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.exportdir"));
        Stage stage = (Stage) sPane.getScene().getWindow();
        File dir = chooser.showDialog(stage);

        if (dir == null) return;

        List<PocItem> toExport = new ArrayList<>(selectedItems);
        showPrompt(I18nUtils.getString("vulnscan.msg.poc.exporting", toExport.size()), false);

        new Thread(() -> {
            PocDatabaseManager dbManager = PocDatabaseManager.getInstance();
            int success = 0, failed = 0;

            for (PocItem item : toExport) {
                try {
                    PocDatabaseManager.PocEntity entity = dbManager.getPocById(item.getId());
                    if (entity != null && entity.getOriginalContent() != null) {
                        String filename = entity.getOriginalFilename();
                        if (filename == null || filename.trim().isEmpty()) {
                            filename = item.getId() + ".yaml";
                        }

                        File outFile = new File(dir, filename);
                        Files.write(outFile.toPath(), entity.getOriginalContent().getBytes(StandardCharsets.UTF_8));
                        success++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    if (debugMode) e.printStackTrace();
                }
            }

            int finalSuccess = success;
            int finalFailed = failed;
            Platform.runLater(() ->
                showPrompt(I18nUtils.getString("vulnscan.msg.poc.batchexported", finalSuccess, finalFailed), false));
        }).start();
    }

    @FXML
    public void enableAllPocs(ActionEvent event) {
        // 如果有选中项，只操作选中的；否则操作全部
        ObservableList<PocItem> selectedItems = pocTableView.getSelectionModel().getSelectedItems();
        List<PocItem> targetItems = selectedItems.isEmpty() ? allPocItems : new ArrayList<>(selectedItems);

        List<String> ids = new ArrayList<>();
        for (PocItem item : targetItems) {
            item.setEnabled(true);
            ids.add(item.getId());
            // 同步更新内存仓库中的POC状态
            PocObj.Poc poc = scanService.getPocRepository().getPoc(item.getId());
            if (poc == null) {
                scanService.getPocRepository().addPoc(item.getPoc());
            }
        }
        pocTableView.refresh();

        // 同步到数据库
        new Thread(() -> {
            try {
                PocDatabaseManager.getInstance().batchSetEnabled(ids, true);
            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }).start();

        showPrompt(I18nUtils.getString("vulnscan.msg.poc.enabled", ids.size()), false);
    }

    @FXML
    public void disableAllPocs(ActionEvent event) {
        // 如果有选中项，只操作选中的；否则操作全部
        ObservableList<PocItem> selectedItems = pocTableView.getSelectionModel().getSelectedItems();
        List<PocItem> targetItems = selectedItems.isEmpty() ? allPocItems : new ArrayList<>(selectedItems);

        List<String> ids = new ArrayList<>();
        for (PocItem item : targetItems) {
            item.setEnabled(false);
            ids.add(item.getId());
            // 从内存仓库中移除禁用的POC
            scanService.getPocRepository().removePoc(item.getId());
        }
        pocTableView.refresh();

        // 同步到数据库
        new Thread(() -> {
            try {
                PocDatabaseManager.getInstance().batchSetEnabled(ids, false);
            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }).start();

        showPrompt(I18nUtils.getString("vulnscan.msg.poc.disabled", ids.size()), false);
    }

    /**
     * 全选/取消全选表格中的POC
     */
    @FXML
    public void toggleSelectAll(MouseEvent event) {
        if (selectAllCheckBox.isSelected()) {
            pocTableView.getSelectionModel().selectAll();
        } else {
            pocTableView.getSelectionModel().clearSelection();
        }
        updateSelectedPocLabel();
    }

    /**
     * 反选表格中的POC
     */
    @FXML
    public void invertSelection(ActionEvent event) {
        ObservableList<PocItem> items = pocTableView.getItems();
        Set<Integer> currentlySelected = new HashSet<>(pocTableView.getSelectionModel().getSelectedIndices());

        pocTableView.getSelectionModel().clearSelection();
        for (int i = 0; i < items.size(); i++) {
            if (!currentlySelected.contains(i)) {
                pocTableView.getSelectionModel().select(i);
            }
        }
        updateSelectedPocLabel();

        // 更新全选复选框状态
        if (selectAllCheckBox != null) {
            int selectedCount = pocTableView.getSelectionModel().getSelectedItems().size();
            selectAllCheckBox.setSelected(selectedCount == items.size() && !items.isEmpty());
        }
    }
    
    // ==================== 统计功能 ====================
    
    /**
     * 更新统计面板
     */
    private void updateStatistics(List<ScanResult> results) {
        if (results == null || results.isEmpty()) {
            hideStatistics();
            return;
        }
        
        int critical = 0, high = 0, medium = 0, low = 0, info = 0;
        
        for (ScanResult result : results) {
            if (!result.isVulnerable()) continue;
            
            PocObj.Severity severity = result.getPoc().getSeverity();
            if (severity == null) continue;
            
            switch (severity) {
                case CRITICAL: critical++; break;
                case HIGH: high++; break;
                case MEDIUM: medium++; break;
                case LOW: low++; break;
                case INFO: info++; break;
                default: break;
            }
        }
        
        int finalCritical = critical;
        int finalHigh = high;
        int finalMedium = medium;
        int finalLow = low;
        int finalInfo = info;
        
        Platform.runLater(() -> {
            if (criticalCountLabel != null) criticalCountLabel.setText(String.valueOf(finalCritical));
            if (highCountLabel != null) highCountLabel.setText(String.valueOf(finalHigh));
            if (mediumCountLabel != null) mediumCountLabel.setText(String.valueOf(finalMedium));
            if (lowCountLabel != null) lowCountLabel.setText(String.valueOf(finalLow));
            if (infoCountLabel != null) infoCountLabel.setText(String.valueOf(finalInfo));
            
            if (statsPane != null && (finalCritical + finalHigh + finalMedium + finalLow + finalInfo) > 0) {
                statsPane.setVisible(true);
                statsPane.setManaged(true);
            }
        });
    }
    
    /**
     * 隐藏统计面板
     */
    private void hideStatistics() {
        Platform.runLater(() -> {
            if (statsPane != null) {
                statsPane.setVisible(false);
                statsPane.setManaged(false);
            }
        });
    }
    
    /**
     * 显示详细统计
     */
    @FXML
    public void showStatistics(ActionEvent event) {
        List<ScanResult> results = this.scanResults;
        
        int total = 0, vulnerable = 0;
        int critical = 0, high = 0, medium = 0, low = 0, info = 0;
        Set<String> uniqueTargets = new HashSet<>();
        Set<String> uniquePocs = new HashSet<>();
        
        for (ScanResult result : results) {
            total++;
            uniqueTargets.add(result.getTarget());
            uniquePocs.add(result.getPoc().getId());
            
            if (result.isVulnerable()) {
                vulnerable++;
                PocObj.Severity severity = result.getPoc().getSeverity();
                if (severity != null) {
                    switch (severity) {
                        case CRITICAL: critical++; break;
                        case HIGH: high++; break;
                        case MEDIUM: medium++; break;
                        case LOW: low++; break;
                        case INFO: info++; break;
                        default: break;
                    }
                }
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(I18nUtils.getString("vulnscan.stats.title")).append("\n");
        sb.append("═══════════════════════════\n\n");
        sb.append(I18nUtils.getString("vulnscan.stats.basic")).append("\n");
        sb.append(I18nUtils.getString("vulnscan.stats.targets")).append(uniqueTargets.size()).append("\n");
        sb.append(I18nUtils.getString("vulnscan.stats.pocs")).append(uniquePocs.size()).append("\n");
        sb.append(I18nUtils.getString("vulnscan.stats.total")).append(total).append("\n");
        sb.append(I18nUtils.getString("vulnscan.stats.vulns")).append(vulnerable).append("\n\n");
        
        sb.append(I18nUtils.getString("vulnscan.stats.distribution")).append("\n");
        sb.append("  • [C] Critical: ").append(critical).append("\n");
        sb.append("  • [H] High: ").append(high).append("\n");
        sb.append("  • [M] Medium: ").append(medium).append("\n");
        sb.append("  • [L] Low: ").append(low).append("\n");
        sb.append("  • [I] Info: ").append(info).append("\n\n");
        
        double vulnRate = total > 0 ? (vulnerable * 100.0 / total) : 0;
        sb.append(I18nUtils.getString("vulnscan.stats.vulnrate")).append(String.format("%.2f%%", vulnRate));
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtils.getString("vulnscan.stats.title"));
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinWidth(400);
        alert.showAndWait();
    }
    
    // ==================== 配置导入导出 ====================
    
    @FXML
    public void exportScanConfig(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.exportconfig"));
        chooser.setInitialFileName("vulnscan_config_" + System.currentTimeMillis() + ".json");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.json"), "*.json")
        );

        Stage stage = (Stage) sPane.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Map<String, Object> config = new HashMap<>();

                // POC格式
                Map<String, Boolean> formats = new HashMap<>();
                formats.put("nuclei", nucleiCheckBox.isSelected());
                formats.put("goby", gobyCheckBox.isSelected());
                formats.put("xray", xrayCheckBox.isSelected());
                formats.put("pocsuite", pocsuiteCheckBox.isSelected());
                config.put("formats", formats);

                // 其他选项
                config.put("enableProxy", enableProxyBox.isSelected());
                config.put("verboseLog", verboseLogBox.isSelected());
                config.put("autoSave", autoSaveBox.isSelected());
                config.put("tags", tagsField.getText());
                config.put("enableHeadless", enableHeadlessBox.isSelected());
                config.put("enableCode", enableCodeBox.isSelected());
                config.put("enableFuzz", enableFuzzBox != null && enableFuzzBox.isSelected());
                config.put("includeOsint", includeOsintBox != null && includeOsintBox.isSelected());
                config.put("includeTokenSpray", includeTokenSprayBox != null && includeTokenSprayBox.isSelected());

                // 写入文件
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(file.toPath(), gson.toJson(config).getBytes(StandardCharsets.UTF_8));

                showPrompt(I18nUtils.getString("vulnscan.msg.config.exported", file.getName()), false);
            } catch (Exception e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.config.exportfailed", e.getMessage()), true);
            }
        }
    }

    @FXML
    public void importScanConfig(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.importconfig"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.json"), "*.json")
        );

        Stage stage = (Stage) sPane.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            try {
                String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                @SuppressWarnings("unchecked")
                Map<String, Object> config = gson.fromJson(json, Map.class);

                // POC格式
                if (config.containsKey("formats")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> formats = (Map<String, Boolean>) config.get("formats");
                    if (formats.containsKey("nuclei")) nucleiCheckBox.setSelected(formats.get("nuclei"));
                    if (formats.containsKey("goby")) gobyCheckBox.setSelected(formats.get("goby"));
                    if (formats.containsKey("xray")) xrayCheckBox.setSelected(formats.get("xray"));
                    if (formats.containsKey("pocsuite")) pocsuiteCheckBox.setSelected(formats.get("pocsuite"));
                }

                // 其他选项
                if (config.containsKey("enableProxy")) {
                    enableProxyBox.setSelected((Boolean) config.get("enableProxy"));
                }
                if (config.containsKey("verboseLog")) {
                    verboseLogBox.setSelected((Boolean) config.get("verboseLog"));
                }
                if (config.containsKey("autoSave")) {
                    autoSaveBox.setSelected((Boolean) config.get("autoSave"));
                }
                if (config.containsKey("tags")) {
                    tagsField.setText(String.valueOf(config.get("tags")));
                }
                if (config.containsKey("enableHeadless")) {
                    enableHeadlessBox.setSelected((Boolean) config.get("enableHeadless"));
                }
                if (config.containsKey("enableCode")) {
                    enableCodeBox.setSelected((Boolean) config.get("enableCode"));
                }
                if (config.containsKey("enableFuzz") && enableFuzzBox != null) {
                    enableFuzzBox.setSelected((Boolean) config.get("enableFuzz"));
                }
                if (config.containsKey("includeOsint") && includeOsintBox != null) {
                    includeOsintBox.setSelected((Boolean) config.get("includeOsint"));
                }
                if (config.containsKey("includeTokenSpray") && includeTokenSprayBox != null) {
                    includeTokenSprayBox.setSelected((Boolean) config.get("includeTokenSpray"));
                }

                showPrompt(I18nUtils.getString("vulnscan.msg.config.imported", file.getName()), false);
            } catch (Exception e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.config.importfailed", e.getMessage()), true);
            }
        }
    }
    
    // ==================== POC在线更新 ====================
    
    @FXML
    public void showPocUpdateDialog(ActionEvent event) {
        // 创建选择对话框
        List<String> choices = new ArrayList<>();
        PocUpdater updater = new PocUpdater();
        for (PocUpdater.PocSource source : updater.getAvailableSources()) {
            choices.add(source.getName() + " - " + source.getDescription());
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle(I18nUtils.getString("vulnscan.dialog.pocupdate.title"));
        dialog.setHeaderText(I18nUtils.getString("vulnscan.dialog.pocupdate.header"));
        dialog.setContentText(I18nUtils.getString("vulnscan.dialog.pocupdate.content"));
        
        dialog.showAndWait().ifPresent(selected -> {
            // 获取选中的源key
            String sourceKey = null;
            for (Map.Entry<String, PocUpdater.PocSource> entry : PocUpdater.POC_SOURCES.entrySet()) {
                if (selected.startsWith(entry.getValue().getName())) {
                    sourceKey = entry.getKey();
                    break;
                }
            }
            
            if (sourceKey != null) {
                startPocUpdate(sourceKey);
            }
        });
    }
    
    private void startPocUpdate(String sourceKey) {
        showPrompt(I18nUtils.getString("vulnscan.msg.update.downloading"), false);
        
        String finalSourceKey = sourceKey;
        Task<PocUpdater.UpdateResult> task = new Task<PocUpdater.UpdateResult>() {
            @Override
            protected PocUpdater.UpdateResult call() {
                PocUpdater updater = new PocUpdater();
                updater.setProgressCallback(msg -> 
                    Platform.runLater(() -> showPrompt(msg, false)));
                return updater.updateFromSource(finalSourceKey);
            }
        };
        
        task.setOnSucceeded(e -> {
            PocUpdater.UpdateResult result = task.getValue();
            if (result.isSuccess()) {
                showPrompt(I18nUtils.getString("vulnscan.msg.update.success", result.getNewCount(), result.getUpdatedCount()), false);
                // 刷新POC列表
                refreshPocList(null);
            } else {
                showPrompt(result.getMessage(), true);
            }
        });
        
        task.setOnFailed(e -> {
            showPrompt(I18nUtils.getString("vulnscan.msg.update.failed", task.getException().getMessage()), true);
        });
        
        new Thread(task).start();
    }
    
    // ==================== 日志查看功能 ====================
    
    @FXML
    public void showScanLogs(ActionEvent event) {
        if (logViewerMask == null) return;

        refreshLogs(null);
        logViewerMask.setVisible(true);
        logViewerMask.setManaged(true);
        logViewerMask.toFront();

        // 注册日志监听器，实时刷新 UI
        if (logListener == null) {
            logListener = entry -> Platform.runLater(() -> refreshLogs(null));
            ScanLogger.getInstance().addListener(logListener);
        }
    }

    @FXML
    public void closeLogViewer(MouseEvent event) {
        if (logViewerMask != null) {
            logViewerMask.setVisible(false);
            logViewerMask.setManaged(false);
        }

        // 移除日志监听器
        if (logListener != null) {
            ScanLogger.getInstance().removeListener(logListener);
            logListener = null;
        }
    }
    
    @FXML
    public void closeLogViewerByMask(MouseEvent event) {
        closeLogViewer(event);
    }
    
    @FXML
    public void refreshLogs(ActionEvent event) {
        ScanLogger logger = ScanLogger.getInstance();
        List<ScanLogger.LogEntry> logs = logger.getMemoryLogs();
        
        // 应用过滤
        int levelIdx = logLevelFilter != null ? logLevelFilter.getSelectionModel().getSelectedIndex() : 0;
        String[] levels = {"INFO", "ERROR", "WARN", "DEBUG", "SUCCESS"};
        String searchKeyword = logSearchField != null ? logSearchField.getText() : "";
        
        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        for (ScanLogger.LogEntry entry : logs) {
            // 级别过滤
            if (levelIdx > 0 && !levels[levelIdx - 1].equals(entry.getLevel().name())) {
                continue;
            }
            // 关键词过滤
            if (!searchKeyword.isEmpty() && !entry.toString().toLowerCase().contains(searchKeyword.toLowerCase())) {
                continue;
            }
            
            sb.append(entry.toString()).append("\n");
            count++;
        }
        
        if (logTextArea != null) {
            logTextArea.setText(sb.toString());
            logTextArea.setScrollTop(Double.MAX_VALUE); // 滚动到底部
        }
        
        if (logCountLabel != null) {
            logCountLabel.setText(I18nUtils.getString("vulnscan.log.count", count));
        }
    }
    
    @FXML
    public void clearLogs(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18nUtils.getString("vulnscan.dialog.clearlog.title"));
        confirm.setHeaderText(I18nUtils.getString("vulnscan.dialog.clearlog.header"));
        confirm.setContentText(I18nUtils.getString("vulnscan.dialog.clearlog.content"));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ScanLogger.getInstance().clearMemoryLogs();
                refreshLogs(null);
                showPrompt(I18nUtils.getString("vulnscan.msg.log.cleared"), false);
            }
        });
    }
    
    @FXML
    public void exportLogs(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("vulnscan.filechooser.exportlog"));
        chooser.setInitialFileName("scan_log_" + System.currentTimeMillis() + ".txt");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtils.getString("vulnscan.filechooser.txt"), "*.txt")
        );
        
        Stage stage = (Stage) sPane.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        
        if (file != null && logTextArea != null) {
            try {
                Files.write(file.toPath(), logTextArea.getText().getBytes(StandardCharsets.UTF_8));
                showPrompt(I18nUtils.getString("vulnscan.msg.log.exported", file.getName()), false);
            } catch (Exception e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.log.exportfailed", e.getMessage()), true);
            }
        }
    }
    
    @FXML
    public void cleanOldLogs(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("7");
        dialog.setTitle(I18nUtils.getString("vulnscan.dialog.cleanlog.title"));
        dialog.setHeaderText(I18nUtils.getString("vulnscan.dialog.cleanlog.header"));
        dialog.setContentText(I18nUtils.getString("vulnscan.dialog.cleanlog.content"));
        
        dialog.showAndWait().ifPresent(days -> {
            try {
                int keepDays = Integer.parseInt(days);
                int deleted = ScanLogger.getInstance().cleanOldLogs(keepDays);
                showPrompt(I18nUtils.getString("vulnscan.msg.log.cleaned", deleted), false);
            } catch (NumberFormatException e) {
                showPrompt(I18nUtils.getString("vulnscan.msg.log.invalidnum"), true);
            }
        });
    }

    // ==================== 扫描控制 ====================

    /**
     * 确保事件监听器已注册
     * 用于恢复扫描时，应用重启后 scanEventListener 可能为 null
     */
    private void ensureEventListenerRegistered() {
        if (scanEventListener == null) {
            // 创建新的事件监听器
            scanEventListener = new ScanEventListener() {
                @Override
                public void onScanStarted(ScanStartedEvent event) {
                    Platform.runLater(() -> {
                        updateScanStateLabel(I18nUtils.getString("vulnscan.scan.scanning"));
                        updateResultVBox(I18nUtils.getString("vulnscan.scan.started", event.getTotalTasks()), true, null);
                    });
                }

                @Override
                public void onScanProgress(ScanProgressEvent event) {
                    Platform.runLater(() -> {
                        double progress = event.getProgressPercentage() / 100.0;
                        progressBar.setProgress(progress);
                        progressLabel.setText(String.format("%.1f%%", progress * 100));

                        // 更新扫描时间
                        long elapsed = System.currentTimeMillis() - scanStartTime;
                        timeLabel.setText(formatDuration(elapsed / 1000));
                    });
                }

                @Override
                public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
                    Platform.runLater(() -> {
                        ScanResult result = event.getResult();
                        scanResults.add(result);
                        addVulnResultToUI(result);
                        updateStatistics(scanResults);
                    });
                }

                @Override
                public void onScanCompleted(ScanCompletedEvent event) {
                    Platform.runLater(() -> {
                        // 检查是否是因为暂停而触发的完成事件
                        if (!scanService.isPaused()) {
                            updateButtonsForStatus(ScanState.Status.COMPLETED);
                            updateScanStateLabel(I18nUtils.getString("vulnscan.status.completed"));
                            updateResultVBox(I18nUtils.getString("vulnscan.scan.completed", scanResults.size()), true, null);
                            updateStatistics(scanResults);

                            // 自动保存
                            if (autoSaveBox.isSelected() && !scanResults.isEmpty()) {
                                saveToDatabase(null);
                            }
                        }
                        // 如果是暂停状态，保持暂停按钮的显示状态
                    });
                }

                @Override
                public void onScanError(ScanErrorEvent event) {
                    Platform.runLater(() -> {
                        if (verboseLogBox.isSelected()) {
                            ScanLogger.getInstance().error("SCAN", event.getErrorMessage());
                        }
                    });
                }
            };

            scanService.addEventListener(scanEventListener);
        }
    }

    /**
     * 启动扫描计时器（每秒更新时间显示）
     */
    private void startScanTimer() {
        stopScanTimer(); // 先停止旧的计时器

        scanTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.seconds(1), event -> {
                if (scanStartTime > 0) {
                    long elapsed = System.currentTimeMillis() - scanStartTime;
                    timeLabel.setText(formatDuration(elapsed / 1000));
                }
            })
        );
        scanTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        scanTimer.play();
    }

    /**
     * 停止扫描计时器
     */
    private void stopScanTimer() {
        if (scanTimer != null) {
            scanTimer.stop();
            scanTimer = null;
        }
    }

    /**
     * 根据扫描模式更新选项复选框的启用/禁用状态
     */
    private void updateOptionsForMode(ScanConfig.ScanMode mode) {
        boolean enableSpecial = (mode == ScanConfig.ScanMode.STANDARD || mode == ScanConfig.ScanMode.CUSTOM);
        boolean forceSpecial = (mode == ScanConfig.ScanMode.DEEP);
        boolean enableOsint = (mode == ScanConfig.ScanMode.STANDARD || mode == ScanConfig.ScanMode.DEEP || mode == ScanConfig.ScanMode.CUSTOM);
        boolean forceOsint = (mode == ScanConfig.ScanMode.OSINT);

        setCheckBoxState(enableHeadlessBox, enableSpecial || forceSpecial, forceSpecial);
        setCheckBoxState(enableCodeBox, enableSpecial || forceSpecial, forceSpecial);
        setCheckBoxState(enableFuzzBox, enableSpecial || forceSpecial, forceSpecial);
        setCheckBoxState(includeOsintBox, enableOsint || forceOsint, forceOsint);
        setCheckBoxState(includeTokenSprayBox, enableOsint, false);
    }

    /**
     * 设置复选框状态：enabled=是否可交互，forceChecked=是否强制勾选
     */
    private void setCheckBoxState(CFCheckBox box, boolean enabled, boolean forceChecked) {
        if (box == null) return;
        box.setDisable(!enabled);
        if (!enabled) {
            box.setSelected(false);
        } else if (forceChecked) {
            box.setSelected(true);
        }
    }

    /**
     * 输入类型与模式联动：LOCAL_PATH 只能用 CUSTOM 模式
     */
    private void updateModeForInputType(int inputIdx) {
        if (inputIdx == 4) { // LOCAL_PATH
            scanModeComboBox.getSelectionModel().select(6); // CUSTOM
            scanModeComboBox.setDisable(true);
        } else {
            scanModeComboBox.setDisable(false);
        }
    }

    /**
     * 更新扫描状态标签
     */
    private void updateScanStateLabel(String state) {
        Platform.runLater(() -> {
            if (scanStateLabel != null) {
                scanStateLabel.setText(state);
            }
        });
    }
    
    // ==================== 数据类 ====================
    
    public static class PocItem {
        private final PocObj.Poc poc;
        private boolean enabled = true;
        
        public PocItem(PocObj.Poc poc) {
            this.poc = poc;
        }
        
        public PocItem(PocObj.Poc poc, boolean enabled) {
            this.poc = poc;
            this.enabled = enabled;
        }
        
        public String getId() { return poc.getId(); }
        public String getName() { return poc.getName(); }
        public String getFormat() { return poc.getOriginalFormat(); }
        public String getSeverity() { return poc.getSeverity() != null ? poc.getSeverity().name() : ""; }
        public String getTags() { 
            return poc.getTags() != null ? String.join(", ", poc.getTags()) : ""; 
        }
        public String getProtocol() { return poc.getProtocol(); }
        public PocObj.Poc getPoc() { return poc; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getStatusText() { return enabled ? I18nUtils.getString("vulnscan.status.enabled") : I18nUtils.getString("vulnscan.status.disabled"); }
    }
    
    public static class HistoryItem {
        private String id;
        private String scanTime;
        private String targets;
        private String pocs;
        private String vulns;
        private String duration;
        private String status;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getScanTime() { return scanTime; }
        public void setScanTime(String scanTime) { this.scanTime = scanTime; }
        public String getTargets() { return targets; }
        public void setTargets(String targets) { this.targets = targets; }
        public String getPocs() { return pocs; }
        public void setPocs(String pocs) { this.pocs = pocs; }
        public String getVulns() { return vulns; }
        public void setVulns(String vulns) { this.vulns = vulns; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
