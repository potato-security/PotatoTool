package com.potato.potatotool.controller.redTeam;

import com.dlsc.gemsfx.CFCheckBox;
import com.dlsc.gemsfx.FilterView;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.kbRootQuery.classObj.KbInfo;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.SmoothTableView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.kbRootQuery.KbCheck.*;

/**
 * @author Potato
 * @date 2023/3/21 17:14
 */
public class PaneKbRootQuery {

    private static final String KB_RESULT_PREVIEW_TEXT = "主机名:           DESKTOP-EMGF5BE\n" +
            "OS 名称:          Microsoft Windows 10 专业版\n" +
            "OS 版本:          10.0.18363 暂缺 Build 18363\n" +
            "OS 制造商:        Microsoft Corporation\n" +
            "OS 配置:          独立工作站\n" +
            "OS 构建类型:      Multiprocessor Free\n" +
            "注册的所有人:     Windows 用户\n" +
            "注册的组织:\n" +
            "产品 ID:          00331-10000-00001-AA265\n" +
            "初始安装日期:     2021-9-17, 21:46:11\n" +
            "系统启动时间:     2024-4-24, 22:45:05\n" +
            "系统制造商:       VMware, Inc.\n" +
            "系统型号:         VMware7,1\n" +
            "系统类型:         x64-based PC\n" +
            "处理器:           安装了 2 个处理器。\n" +
            "                  [01]: Intel64 Family 6 Model 158 Stepping 10 GenuineIntel ~2592 Mhz\n" +
            "                  [02]: Intel64 Family 6 Model 158 Stepping 10 GenuineIntel ~2592 Mhz\n" +
            "BIOS 版本:        VMware, Inc. VMW71.00V.18452719.B64.2108091906, 2021-8-9\n" +
            "Windows 目录:     C:\\Windows\n" +
            "系统目录:         C:\\Windows\\system32\n" +
            "启动设备:         \\Device\\HarddiskVolume1\n" +
            "系统区域设置:     zh-cn;中文(中国)\n" +
            "输入法区域设置:   zh-cn;中文(中国)\n" +
            "时区:             (UTC+08:00) 北京，重庆，香港特别行政区，乌鲁木齐\n" +
            "物理内存总量:     5,119 MB\n" +
            "可用的物理内存:   2,885 MB\n" +
            "虚拟内存: 最大值: 5,439 MB\n" +
            "虚拟内存: 可用:   3,403 MB\n" +
            "虚拟内存: 使用中: 2,036 MB\n" +
            "页面文件位置:     C:\\pagefile.sys\n" +
            "域:               WORKGROUP\n" +
            "登录服务器:       \\\\DESKTOP-EMGF5BE\n" +
            "修补程序:         安装了 6 个修补程序。\n" +
            "                  [01]: KB4601556\n" +
            "                  [02]: KB4513661\n" +
            "                  [03]: KB4516115\n" +
            "                  [04]: KB4517245\n" +
            "                  [05]: KB4521863\n" +
            "                  [06]: KB4517389\n" +
            "网卡:             安装了 2 个 NIC。\n" +
            "                  [01]: Intel(R) 82574L Gigabit Network Connection\n" +
            "                      连接名:      Ethernet0\n" +
            "                      启用 DHCP:   是\n" +
            "                      DHCP 服务器: 172.16.246.254\n" +
            "                      IP 地址\n" +
            "                        [01]: 172.16.246.250\n" +
            "                        [02]: fe80::8403:21ce:5678:4ec6\n" +
            "                  [02]: Bluetooth Device (Personal Area Network)\n" +
            "                      连接名:      蓝牙网络连接\n" +
            "                      状态:        媒体连接已中断\n" +
            "Hyper-V 要求:     已检测到虚拟机监控程序。将不显示 Hyper-V 所需的功能。";

    @FXML private StackPane sPane;
    @FXML private VBox vBox;
    @FXML private TextArea inputText;
    @FXML private CFCheckBox mucFilter;
    @FXML private ScrollPane cveDetailScrollPane;
    @FXML private Label cvePanelId;
    @FXML private VBox cvePanelInfo;
    @FXML private VBox cvePanelStates;
    private boolean cvePanelVisible = false;

    FilterView<KbInfo> filterView;


    FilterView.FilterGroup<KbInfo> productGroup;
    FilterView.FilterGroup<KbInfo> componentGroup;
    FilterView.FilterGroup<KbInfo> severityGroup;
    FilterView.FilterGroup<KbInfo> impactGroup;
    FilterView.FilterGroup<KbInfo> pocGroup;

    TableView<KbInfo> tableView = new SmoothTableView<>();

    @FXML
    void initialize(){
        // 使用工厂方法创建支持国际化的 FilterGroup
        productGroup = I18nUtils.createI18nFilterGroup("kb.product");
        componentGroup = I18nUtils.createI18nFilterGroup("kb.component");
        severityGroup = I18nUtils.createI18nFilterGroup("kb.severity");
        impactGroup = I18nUtils.createI18nFilterGroup("kb.impact");
        pocGroup = I18nUtils.createI18nFilterGroup("kb.poc");
        
        filterView = new FilterView<>();
        filterView.setShowHeader(false);

        tableView.setEditable(true);
        tableView.setSortPolicy(table -> false);

        filterView.getFilterGroups().setAll(productGroup, componentGroup, severityGroup, impactGroup, pocGroup);

        SortedList<KbInfo> sortedList = new SortedList<>(filterView.getFilteredItems());
        tableView.setItems(sortedList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

//        filterView.getItems().add(new KbInfo("20170314","","4014329","Security Update for Adobe Flash Player","Windows 10 Version 1511 for x64-based Systems","Adobe Flash Player","Critical","Remote Code Execution","4010250",""));
//        filterView.getItems().add(new KbInfo("20170314","","4014329","Security Update for Adobe Flash Player","Windows 10 Version 1607 for 32-bit Systems","Adobe Flash Player","Critical","Remote Code Execution","4010250",""));


        TableColumn<KbInfo, String> dateColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(dateColumn, "kb.date");
        TableColumn<KbInfo, String> cveColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(cveColumn, "kb.cve");
        TableColumn<KbInfo, String> kbColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(kbColumn, "kb.kb");
        TableColumn<KbInfo, String> titleColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(titleColumn, "kb.title");
        TableColumn<KbInfo, String> productColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(productColumn, "kb.product");
        TableColumn<KbInfo, String> componentColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(componentColumn, "kb.component");
        TableColumn<KbInfo, String> severityColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(severityColumn, "kb.severity");
        TableColumn<KbInfo, String> impactColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(impactColumn, "kb.impact");
        TableColumn<KbInfo, String> repKbColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(repKbColumn, "kb.repkb");
        TableColumn<KbInfo, String> pocColumn = new TableColumn<>();
        I18nUtils.bindTableColumn(pocColumn, "kb.poc");

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        // 发布日期规范为 YYYY-MM-DD（对齐 Penpot 稿的连字符 ISO 格式）；原始为 8 位 YYYYMMDD 时插入连字符，其余原样
        dateColumn.setCellFactory(col -> new TableCell<KbInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.matches("\\d{8}")) {
                    setText(item.substring(0, 4) + "-" + item.substring(4, 6) + "-" + item.substring(6, 8));
                } else {
                    setText(item);
                }
            }
        });
        // 发布日期完整显示，避免被 CVE 列挤成 20240…
        dateColumn.setMinWidth(84);
        dateColumn.setPrefWidth(84);
        cveColumn.setCellValueFactory(new PropertyValueFactory<>("cve"));
        // CVE 编号：强调色 + 等宽，对齐 Penpot 稿（红队页 -pt-accent 解析为红）
        cveColumn.setCellFactory(col -> new TableCell<KbInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -pt-accent; -fx-font-family: 'Menlo','Consolas','monospace'; -fx-font-weight: bold;");
                }
            }
        });
        // CVE 编号是行主键，需完整显示（对齐 Penpot 稿 CVE-2024-21338 全展示，避免约束列策略挤成 CVE-20…）
        cveColumn.setMinWidth(120);
        cveColumn.setPrefWidth(120);
        kbColumn.setCellValueFactory(new PropertyValueFactory<>("kb"));
        kbColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        // KB 编号是标识列，完整显示（约束列策略下文本列吸收截断，标识列固定）
        kbColumn.setMinWidth(72);
        kbColumn.setPrefWidth(72);
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        productColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        componentColumn.setCellValueFactory(new PropertyValueFactory<>("component"));
        componentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severityColumn.setCellFactory(col -> new TableCell<KbInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    return;
                }
                String color;
                String shortLabel;
                switch (item.toLowerCase()) {
                    case "critical":                color = "#FF5C66"; shortLabel = "CRIT"; break;
                    case "important": case "high":   color = "#F48C42"; shortLabel = "HIGH"; break;
                    case "moderate": case "medium":  color = "#F4B740"; shortLabel = "MED";  break;
                    case "low":                      color = "#5BF0B4"; shortLabel = "LOW";  break;
                    default:                         color = null;      shortLabel = item;   break;
                }
                if (color == null) {
                    Label plain = new Label(item);
                    plain.setStyle("-fx-text-fill: -pt-text-secondary;");
                    setGraphic(plain);
                    return;
                }
                Label badge = new Label(shortLabel);
                badge.setStyle("-fx-background-color: " + color + ";"
                        + " -fx-text-fill: #0A141C; -fx-font-weight: bold; -fx-font-size: 11px;"
                        + " -fx-background-radius: 6; -fx-padding: 2 8 2 8;");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });
        // 严重性药丸列固定宽度，避免药丸被挤裁
        severityColumn.setMinWidth(60);
        severityColumn.setPrefWidth(60);
        impactColumn.setCellValueFactory(new PropertyValueFactory<>("impact"));
        impactColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        repKbColumn.setCellValueFactory(new PropertyValueFactory<>("repKb"));
        repKbColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        repKbColumn.setMinWidth(72);
        repKbColumn.setPrefWidth(72);
        pocColumn.setCellValueFactory(new PropertyValueFactory<>("poc"));
        pocColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        tableView.getColumns().setAll(dateColumn, cveColumn, kbColumn, titleColumn, productColumn, componentColumn, severityColumn, impactColumn, repKbColumn, pocColumn);

        VBox box = new VBox(filterView, tableView);
        box.setPadding(new Insets(0,0,10,0));
        box.setSpacing(10);

        VBox.setVgrow(tableView, Priority.ALWAYS);
        vBox.getChildren().add(box);

        // 列宽自动
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 选中行时显示 CVE DETAIL 面板
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof KbInfo) {
                populateCvePanel((KbInfo) newVal);
                showCvePanel();
            }
        });

        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void populateCvePanel(KbInfo info) {
        cvePanelId.setText(info.getCve() != null && !info.getCve().isEmpty() ? info.getCve() : "N/A");
        cvePanelInfo.getChildren().clear();
        cvePanelStates.getChildren().clear();

        addCveInfoRow("kb.severity", info.getSeverity(), "cve-info-severity");
        addCveInfoRow("kb.component", info.getComponent(), null);
        addCveInfoRow("kb.impact", info.getImpact(), null);
        String poc = info.getPoc();
        if (poc != null && !poc.isEmpty() && !poc.equals("-")) {
            addCveInfoRow("kb.poc", poc, "存在".equals(poc) ? "cve-info-poc-danger" : "cve-info-poc-safe");
        }
        String kb = info.getKb();
        if (kb != null && !kb.isEmpty() && !kb.equals("-")) {
            addCveInfoRow("kb.kb", kb, null);
        }
        String repKb = info.getRepKb();
        if (repKb != null && !repKb.isEmpty() && !repKb.equals("-")) {
            addCveInfoRow("kb.repkb", repKb, null);
        }

        // States
        String pocState = info.getPoc();
        if ("存在".equals(pocState)) {
            addCveStateChip(I18nUtils.getString("kb.state.poc.exists"), "cve-state-dot-danger");
        } else {
            addCveStateChip(I18nUtils.getString("kb.state.poc.none"), "cve-state-dot-safe");
        }
    }

    private void addCveInfoRow(String i18nKey, String value, String extraStyle) {
        if (value == null || value.isEmpty()) return;
        VBox row = new VBox(2);
        row.getStyleClass().add("cve-info-row");
        Label keyLbl = new Label(I18nUtils.getString(i18nKey));
        keyLbl.getStyleClass().add("cve-info-key");
        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("cve-info-val");
        valLbl.setWrapText(true);
        if (extraStyle != null) valLbl.getStyleClass().add(extraStyle);
        row.getChildren().addAll(keyLbl, valLbl);
        cvePanelInfo.getChildren().add(row);
    }

    private void addCveStateChip(String label, String dotStyle) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("cve-state-chip");
        Region dot = new Region();
        dot.getStyleClass().addAll("cve-state-dot", dotStyle);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("cve-state-label");
        chip.getChildren().addAll(dot, lbl);
        cvePanelStates.getChildren().add(chip);
    }

    private void showCvePanel() {
        if (cveDetailScrollPane == null || cvePanelVisible) return;
        cvePanelVisible = true;
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(cveDetailScrollPane.translateXProperty(), 210)),
            new KeyFrame(Duration.millis(200), new KeyValue(cveDetailScrollPane.translateXProperty(), 0))
        ).play();
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == ToStart.StartupPage.RED_KB_ROOT_QUERY_RESULT) {
            inputText.setText(KB_RESULT_PREVIEW_TEXT);
            searchKb(null);
        }
    }

    private Thread currentThread;
    @FXML
    public void searchKb(ActionEvent event) {
        String input = inputText.getText();
        boolean isMucFilter = mucFilter.isSelected();

        if(!input.toLowerCase().contains("kb") && !input.toLowerCase().contains("]: q")){
            inputText.setText(I18nUtils.getString("kb.error.invalid"));
            return;
        }

        if (currentThread != null && currentThread.isAlive()) {
            currentThread.stop();   // 强行中断当前线程
        }

        List<String> poolList = ExecutorServiceManager.ExecutorPoolNames.KB_ARRAY;
        ExecutorServiceManager.shutdownExecutor(poolList);

        currentThread = new Thread(() -> {
            try {
                Platform.runLater(() -> {

                    Label tipTitle = new Label();
                    if(isMucFilter) {
                        tipTitle.setText(I18nUtils.getString("kb.checking.alt"));
                    }else {
                        tipTitle.setText(I18nUtils.getString("kb.querying"));
                    }
                    tipTitle.setId("tipTitle");
                    tableView.setPlaceholder(tipTitle);
                    filterView.getItems().clear();
                    productGroup.getFilters().clear();
                    componentGroup.getFilters().clear();
                    severityGroup.getFilters().clear();
                    impactGroup.getFilters().clear();
                    pocGroup.getFilters().clear();
                });

                List<Map<String, String>> filteredKB = filterKB(input, isMucFilter);
                List<KbInfo> kbInfo = new ArrayList<>();

                List<FilterView.Filter<KbInfo>> productFilterView = new ArrayList<>();
                List<String> productList = new ArrayList<>();
                List<FilterView.Filter<KbInfo>> componentFilterView = new ArrayList<>();
                List<String> componentList = new ArrayList<>();
                List<FilterView.Filter<KbInfo>> severityFilterView = new ArrayList<>();
                List<String> severityList = new ArrayList<>();
                List<FilterView.Filter<KbInfo>> impactFilterView = new ArrayList<>();
                List<String> impactList = new ArrayList<>();

                filteredKB.forEach(map -> {
                    KbInfo info = new KbInfo(map.get("发布日期"), map.get("CVE编号"), map.get("KB编号"), map.get("标题"), map.get("影响产品"), map.get("影响组件"), map.get("严重性"), map.get("漏洞影响"), map.get("替代KB编号"), map.get("漏洞利用"));
                    kbInfo.add(info);

                    String product = map.get("影响产品");
                    String component = map.get("影响组件");
                    String severity = map.get("严重性");
                    String impact = map.get("漏洞影响");

                    if (!productList.contains(product) && !product.isEmpty()) {
                        productList.add(product);
                        FilterView.Filter tmpFilter = new FilterView.Filter<KbInfo>(product) {
                            @Override
                            public boolean test(KbInfo kbinfo) {
                                    return kbinfo.getProduct().equals(product);
                                }
                        };
                        productFilterView.add(tmpFilter);
                    }
                    if (!componentList.contains(component) && !component.isEmpty()) {
                        componentList.add(component);
                        FilterView.Filter tmpFilter = new FilterView.Filter<KbInfo>(component) {
                            @Override
                            public boolean test(KbInfo kbinfo) {
                                    return kbinfo.getComponent().equals(component);
                                }
                        };
                        componentFilterView.add(tmpFilter);
                    }
                    if (!severityList.contains(severity) && !severity.isEmpty()) {
                        severityList.add(severity);
                        FilterView.Filter tmpFilter = new FilterView.Filter<KbInfo>(severity) {
                            @Override
                            public boolean test(KbInfo kbinfo) {
                                    return kbinfo.getSeverity().equals(severity);
                                }
                        };
                        severityFilterView.add(tmpFilter);
                    }
                    if (!impactList.contains(impact) && !impact.isEmpty()) {
                        impactList.add(impact);
                        FilterView.Filter tmpFilter = new FilterView.Filter<KbInfo>(impact) {
                            @Override
                            public boolean test(KbInfo kbinfo) {
                                    return kbinfo.getImpact().equals(impact);
                                }
                        };
                        impactFilterView.add(tmpFilter);
                    }
                });

                Platform.runLater(() -> {
                    filterView.getItems().addAll(kbInfo);

                    productGroup.getFilters().addAll(productFilterView);
                    componentGroup.getFilters().addAll(componentFilterView);
                    severityGroup.getFilters().addAll(severityFilterView);
                    impactGroup.getFilters().addAll(impactFilterView);

                    pocGroup.getFilters().add(new FilterView.Filter<KbInfo>("存在漏洞利用") {
                        @Override
                        public boolean test(KbInfo KbInfo) {
                            return !KbInfo.getPoc().isEmpty();
                        }
                    });
                    pocGroup.getFilters().add(new FilterView.Filter<KbInfo>("不存在漏洞利用") {
                        @Override
                        public boolean test(KbInfo KbInfo) {
                            return KbInfo.getPoc().isEmpty();
                        }
                    });
                    filterView.getFilterGroups().setAll(productGroup, componentGroup, severityGroup, impactGroup, pocGroup);

                    Label tipTitle = new Label(I18nUtils.getString("kb.novuln"));
                    tipTitle.setId("tipTitle");
                    tableView.setPlaceholder(tipTitle);
                });
            } catch (Exception e) {
                if(debugMode) e.printStackTrace();
                Platform.runLater(() -> {
                    Label tipTitle = new Label(I18nUtils.getString("kb.checkinfo"));
                    tipTitle.setId("tipTitle");
                    tableView.setPlaceholder(tipTitle);
                });
            }
        });
        currentThread.setDaemon(true);
        currentThread.start();
    }


    public static void main(String []args) {
        List<Map<String, String>> filteredKB = filterKB(KB_RESULT_PREVIEW_TEXT, true);
        System.out.println(filteredKB);
    }

}
