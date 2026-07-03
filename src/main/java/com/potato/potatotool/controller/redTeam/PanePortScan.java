package com.potato.potatotool.controller.redTeam;

import com.dlsc.gemsfx.CFCheckBox;
import com.dlsc.gemsfx.CFSwitch;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.portScanner.bridge.HandoffPolicy;
import com.potato.potatotool.content.redTeam.portScanner.bridge.HandoffResult;
import com.potato.potatotool.content.redTeam.portScanner.bridge.VulnScanBridge;
import com.potato.potatotool.content.redTeam.portScanner.core.PortScanService;
import com.potato.potatotool.content.redTeam.portScanner.event.PortFoundEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanCompletedEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanErrorEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventAdapter;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanProgressEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanStartedEvent;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.core.VulnScanService;
import com.potato.potatotool.content.redTeam.portScanner.port.PortPresets;
import com.potato.potatotool.content.redTeam.portScanner.report.PortScanReportWriter;
import com.potato.potatotool.content.redTeam.portScanner.storage.PortScanDatabase;
import com.potato.potatotool.content.redTeam.portScanner.targets.PortPlanner;
import com.potato.potatotool.content.redTeam.portScanner.targets.TargetExpander;
import com.potato.potatotool.content.redTeam.portScanner.util.HostInputParser;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class PanePortScan {
    @FXML private StackPane sPane;
    @FXML private TextArea targetField;
    @FXML private TextField customPortsField;
    @FXML private ComboBox<String> portPresetComboBox;
    @FXML private ToggleGroup presetGroup;
    private int presetIndex = 0;
    @FXML private TextField timeoutField;
    @FXML private TextField batchSizeField;
    @FXML private CFSwitch serviceProbeBox;
    @FXML private CFSwitch publicModeBox;
    @FXML private CFCheckBox autoHandoffBox;
    @FXML private Label scanStateLabel;
    @FXML private Label progressLabel;
    @FXML private Label openCountLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<PortResult> resultTable;
    @FXML private TableColumn<PortResult, String> hostColumn;
    @FXML private TableColumn<PortResult, String> portColumn;
    @FXML private TableColumn<PortResult, String> stateColumn;
    @FXML private TableColumn<PortResult, String> serviceColumn;
    @FXML private TableColumn<PortResult, String> rttColumn;
    @FXML private TableColumn<PortResult, String> bannerColumn;
    @FXML private TableColumn<PortResult, String> actionColumn;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button resumeButton;
    @FXML private Button stopButton;
    @FXML private Button handoffButton;
    @FXML private TitledPane configTitledPane;
    @FXML private ComboBox<String> exportFormatComboBox;
    @FXML private StackPane historyMask;
    @FXML private VBox historyPane;
    @FXML private ListView<PortScanDatabase.HistoryItem> historyListView;
    @FXML private Label historyCountLabel;
    @FXML private StackPane promptPane;
    @FXML private Label promptLabel;

    private final PortScanService service = PortScanService.getInstance();
    private final ObservableList<PortResult> resultItems = FXCollections.observableArrayList();
    private final PortScanReportWriter reportWriter = new PortScanReportWriter();
    private final PortScanDatabase database = PortScanDatabase.getInstance();
    private PortScanResult lastResult;

    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;
    private boolean promptAutoCloseEnabled;
    private Node promptDefaultContent;
    private boolean promptDialogVisible;

    @FXML
    void initialize() {
        if (portPresetComboBox != null) {
            portPresetComboBox.getItems().setAll("Top100", "Top1000", "All", "Custom");
            portPresetComboBox.getSelectionModel().select(0);
        }
        if (presetGroup != null) {
            presetGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null && oldT != null) presetGroup.selectToggle(oldT);
            });
        }
        timeoutField.setText("1500");
        batchSizeField.setText("1024");
        serviceProbeBox.setSelected(true);
        if (exportFormatComboBox != null) {
            exportFormatComboBox.getItems().setAll("JSON", "CSV", "TXT", "MD");
            exportFormatComboBox.getSelectionModel().select(0);
        }
        setupTable();
        setupHistoryTable();
        setupEvents();
        updateButtons(false, false);
        if (promptPane != null && promptLabel != null) {
            promptDefaultContent = promptLabel;
        }
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        PanePortScanPreviewSupport.PreviewState previewState =
                PanePortScanPreviewSupport.buildPreviewState(ToStart.getStartupTestPage());
        if (previewState == null) {
            return;
        }

        if (targetField != null) {
            targetField.setText(previewState.getTargetText());
        }
        if (portPresetComboBox != null && previewState.getPortPreset() != null) {
            portPresetComboBox.getSelectionModel().select(previewState.getPortPreset());
        }
        if (previewState.getPortPreset() != null && presetGroup != null) {
            int idx = java.util.Arrays.asList("Top100", "Top1000", "All", "Custom")
                    .indexOf(previewState.getPortPreset());
            if (idx >= 0 && idx < presetGroup.getToggles().size()) {
                presetGroup.selectToggle(presetGroup.getToggles().get(idx));
            }
        }
        if (customPortsField != null) {
            customPortsField.setText(previewState.getCustomPorts());
        }
        if (timeoutField != null) {
            timeoutField.setText(previewState.getTimeout());
        }
        if (batchSizeField != null) {
            batchSizeField.setText(previewState.getBatchSize());
        }
        if (serviceProbeBox != null) {
            serviceProbeBox.setSelected(previewState.isServiceProbe());
        }
        if (publicModeBox != null) {
            publicModeBox.setSelected(previewState.isPublicMode());
        }
        if (autoHandoffBox != null) {
            autoHandoffBox.setSelected(previewState.isAutoHandoff());
        }
        if (configTitledPane != null) {
            configTitledPane.setExpanded(previewState.isConfigExpanded());
        }
        if (scanStateLabel != null) {
            scanStateLabel.setText(previewState.getScanState());
        }
        if (progressLabel != null) {
            progressLabel.setText(previewState.getProgressText());
        }
        if (progressBar != null) {
            progressBar.setProgress(previewState.getProgressValue());
        }
        if (openCountLabel != null) {
            openCountLabel.setText(previewState.getOpenCount());
        }
        resultItems.setAll(previewState.getResultItems());
        lastResult = previewState.toResult();
        if (historyListView != null) {
            historyListView.getItems().setAll(previewState.getHistoryItems());
        }
        updateHistoryCountLabel(previewState.getHistoryItems().size());
        if (previewState.isHistoryVisible()) {
            showPreviewHistoryMask();
        } else {
            hideHistoryMask();
        }
        updateButtons(previewState.isScanning(), false);
    }

    private void showPreviewHistoryMask() {
        if (historyMask == null) {
            return;
        }
        historyMask.setVisible(true);
        historyMask.setManaged(true);
        historyMask.toFront();
        if (historyListView != null && !historyListView.getItems().isEmpty()) {
            historyListView.getSelectionModel().selectFirst();
        }
    }

    private void setupTable() {
        bindTableColumnTitles();
        hostColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHost()));
        portColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPort())));
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getState())));
        serviceColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getService())));
        rttColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getRttMs())));
        bannerColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getBanner())));
        actionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().toTargetUrl()));
        actionColumn.setCellFactory(column -> new TableCell<PortResult, String>() {
            private final Button button = new Button("▶");

            {
                button.getStyleClass().add("handoff-button");
                button.setOnMouseClicked(event -> {
                    PortResult row = getTableView().getItems().get(getIndex());
                    handoffTargets(java.util.Collections.singletonList(row.toTargetUrl()));
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        resultTable.setItems(resultItems);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void bindTableColumnTitles() {
        updateTableColumnTitles();
        I18nUtils.localeProperty().addListener((obs, oldValue, newValue) -> updateTableColumnTitles());
    }

    private void updateTableColumnTitles() {
        hostColumn.setText(I18nUtils.getString("portscan.table.host"));
        portColumn.setText(I18nUtils.getString("portscan.table.port"));
        stateColumn.setText(I18nUtils.getString("portscan.table.state"));
        serviceColumn.setText(I18nUtils.getString("portscan.table.service"));
        rttColumn.setText(I18nUtils.getString("portscan.table.rtt"));
        bannerColumn.setText(I18nUtils.getString("portscan.table.banner"));
        actionColumn.setText(I18nUtils.getString("portscan.table.action"));
    }

    private void setupEvents() {
        service.addEventListener(new PortScanEventAdapter() {
            @Override
            public void onScanStarted(PortScanStartedEvent event) {
                Platform.runLater(() -> {
                    resultItems.clear();
                    openCountLabel.setText("0");
                    progressBar.setProgress(0);
                    progressLabel.setText("0 / " + event.getTotalTasks());
                    scanStateLabel.setText(I18nUtils.getString("portscan.status.scanning"));
                    updateButtons(true, false);
                });
            }

            @Override
            public void onScanProgress(PortScanProgressEvent event) {
                Platform.runLater(() -> {
                    progressBar.setProgress(event.getProgressPercentage() / 100.0);
                    progressLabel.setText(event.getCompletedTasks() + " / " + event.getTotalTasks());
                    openCountLabel.setText(String.valueOf(event.getOpenCount()));
                });
            }

            @Override
            public void onPortFound(PortFoundEvent event) {
                Platform.runLater(() -> upsertResult(event.getResult()));
            }

            @Override
            public void onScanCompleted(PortScanCompletedEvent event) {
                Platform.runLater(() -> {
                    lastResult = event.getResult();
                    scanStateLabel.setText(I18nUtils.getString("portscan.status.completed"));
                    updateButtons(false, false);
                    try {
                        database.saveScan(event.getResult());
                    } catch (Exception e) {
                        showPrompt(e.getMessage());
                    }
                    if (autoHandoffBox.isSelected()) {
                        handoffOpenTargets(HandoffPolicy.CANCEL);
                    }
                });
            }

            @Override
            public void onScanError(PortScanErrorEvent event) {
                Platform.runLater(() -> showPrompt(event.getMessage()));
            }
        });
    }

    @FXML
    public void startScan(MouseEvent event) {
        List<String> targets = HostInputParser.parseLines(targetField.getText());
        if (targets.isEmpty()) {
            showPrompt(I18nUtils.getString("portscan.msg.target.empty"));
            return;
        }
        PortScanConfig config;
        try {
            config = buildConfig();
        } catch (Exception e) {
            showPrompt(e.getMessage());
            return;
        }
        long hosts = TargetExpander.estimateHosts(targets);
        long tasks = hosts * config.getPorts().length;
        if (hosts > config.getMaxHostsBeforeConfirm() || tasks > config.getMaxTasksBeforeConfirm()) {
            showPrompt(I18nUtils.getString("portscan.msg.large.task", hosts, tasks));
            return;
        }
        lastResult = null;
        service.startScan(targets, config);
    }

    @FXML
    public void pauseScan(MouseEvent event) {
        service.pauseScan();
        scanStateLabel.setText(I18nUtils.getString("portscan.status.paused"));
        updateButtons(true, true);
    }

    @FXML
    public void resumeScan(MouseEvent event) {
        service.resumeScan();
        scanStateLabel.setText(I18nUtils.getString("portscan.status.scanning"));
        updateButtons(true, false);
    }

    @FXML
    public void stopScan(MouseEvent event) {
        service.stopScan();
        scanStateLabel.setText(I18nUtils.getString("portscan.status.stopped"));
        updateButtons(false, false);
    }

    @FXML
    public void clearResults(MouseEvent event) {
        resultItems.clear();
        openCountLabel.setText("0");
        progressLabel.setText("0 / 0");
        progressBar.setProgress(0);
        lastResult = null;
    }

    @FXML
    public void exportReport(MouseEvent event) {
        exportReport("json");
    }

    @FXML
    public void exportJson(MouseEvent event) {
        exportReport("json");
    }

    @FXML
    public void exportCsv(MouseEvent event) {
        exportReport("csv");
    }

    @FXML
    public void exportTxt(MouseEvent event) {
        exportReport("txt");
    }

    @FXML
    public void exportMarkdown(MouseEvent event) {
        exportReport("md");
    }

    @FXML
    public void exportSelected(MouseEvent event) {
        String format = "json";
        if (exportFormatComboBox != null) {
            String selected = exportFormatComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.trim().isEmpty()) {
                format = selected.trim().toLowerCase();
            }
        }
        exportReport(format);
    }

    @FXML
    public void showHistory(MouseEvent event) {
        if (!loadHistoryIntoTable()) {
            return;
        }
        if (historyMask == null) {
            return;
        }
        historyMask.setVisible(true);
        historyMask.setManaged(true);
        historyMask.toFront();
    }

    @FXML
    public void closeHistory(MouseEvent event) {
        hideHistoryMask();
    }

    @FXML
    public void closeHistoryByMask(MouseEvent event) {
        if (event != null && event.getTarget() == historyMask) {
            hideHistoryMask();
        }
    }

    @FXML
    public void consumePromptEvent(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
    }

    @FXML
    public void openSelectedHistory(MouseEvent event) {
        if (historyListView == null) {
            return;
        }
        PortScanDatabase.HistoryItem selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("portscan.msg.history.select"), true);
            return;
        }
        try {
            PortScanResult loaded = database.loadScan(selected.getScanId());
            if (loaded == null) {
                showPrompt(I18nUtils.getString("portscan.msg.history.failed", selected.getScanId()), true);
                return;
            }
            lastResult = loaded;
            resultItems.setAll(loaded.getOpenResults());
            openCountLabel.setText(String.valueOf(loaded.getOpenResults().size()));
            progressLabel.setText(loaded.getCompletedTasks() + " / " + loaded.getTotalTasks());
            progressBar.setProgress(loaded.getTotalTasks() <= 0 ? 0 : Math.min(1.0, loaded.getCompletedTasks() * 1.0 / loaded.getTotalTasks()));
            scanStateLabel.setText(I18nUtils.getString("portscan.status.completed"));
            updateButtons(false, false);
            hideHistoryMask();
            showPrompt(I18nUtils.getString("portscan.msg.history.loaded"));
        } catch (Exception e) {
            showPrompt(I18nUtils.getString("portscan.msg.history.failed", e.getMessage()), true);
        }
    }

    @FXML
    public void deleteSelectedHistory(MouseEvent event) {
        if (historyListView == null) {
            return;
        }
        PortScanDatabase.HistoryItem selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPrompt(I18nUtils.getString("portscan.msg.history.select"), true);
            return;
        }
        try {
            database.deleteScan(selected.getScanId());
            historyListView.getItems().remove(selected);
            updateHistoryCountLabel(historyListView.getItems().size());
            if (!historyListView.getItems().isEmpty()) {
                historyListView.getSelectionModel().selectFirst();
            }
            showPrompt(I18nUtils.getString("portscan.msg.history.deleted"));
        } catch (Exception e) {
            showPrompt(I18nUtils.getString("portscan.msg.history.deletefailed", e.getMessage()), true);
        }
    }

    @FXML
    public void clearAllHistory(MouseEvent event) {
        showPromptChoice(
                I18nUtils.getString("portscan.history.clear"),
                I18nUtils.getString("portscan.history.clear.confirm"),
                null,
                java.util.Collections.singletonList(I18nUtils.getString("portscan.history.clear")),
                selected -> {
                    if (selected == null || selected < 0) {
                        return;
                    }
                    try {
                        database.clearHistory();
                        if (historyListView != null) {
                            historyListView.getItems().clear();
                        }
                        updateHistoryCountLabel(0);
                        showPrompt(I18nUtils.getString("portscan.msg.history.cleared"));
                    } catch (Exception e) {
                        showPrompt(I18nUtils.getString("portscan.msg.history.clearfailed", e.getMessage()), true);
                    }
                },
                null
        );
    }

    private boolean loadHistoryIntoTable() {
        if (historyListView == null) {
            return false;
        }
        try {
            List<PortScanDatabase.HistoryItem> history = database.loadHistory();
            historyListView.getItems().setAll(history);
            updateHistoryCountLabel(history.size());
            if (history.isEmpty()) {
                showPrompt(I18nUtils.getString("portscan.msg.history.empty"));
                return false;
            }
            historyListView.getSelectionModel().selectFirst();
            return true;
        } catch (Exception e) {
            showPrompt(I18nUtils.getString("portscan.msg.history.failed", e.getMessage()), true);
            return false;
        }
    }

    private void hideHistoryMask() {
        if (historyMask == null) {
            return;
        }
        historyMask.setVisible(false);
        historyMask.setManaged(false);
    }

    private void updateHistoryCountLabel(int total) {
        if (historyCountLabel == null) {
            return;
        }
        historyCountLabel.setText(I18nUtils.getString("portscan.history.count", total));
    }

    private void setupHistoryTable() {
        if (historyListView == null) {
            return;
        }
        historyListView.setPlaceholder(new Label(I18nUtils.getString("portscan.msg.history.empty")));
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        historyListView.setCellFactory(lv -> new ListCell<PortScanDatabase.HistoryItem>() {
            @Override
            protected void updateItem(PortScanDatabase.HistoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String time = formatter.format(new Date(item.getStartTime()));
                long durationMs = Math.max(0L, item.getEndTime() - item.getStartTime());
                String sub = item.getOpenCount() + " open  ·  " + formatDuration(durationMs);
                Label timeLabel = new Label(time);
                timeLabel.getStyleClass().add("ps-hist-time");
                Label subLabel = new Label(sub);
                subLabel.getStyleClass().add("ps-hist-sub");
                VBox card = new VBox(3, timeLabel, subLabel);
                card.getStyleClass().add("ps-hist-card");
                setGraphic(card);
            }
        });
        historyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !historyListView.getSelectionModel().isEmpty()) {
                openSelectedHistory(event);
            }
        });
        updateHistoryCountLabel(0);
    }

    private String formatDuration(long durationMs) {
        long totalSec = durationMs / 1000L;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%d:%02d", m, s);
    }

    private void exportReport(String format) {
        PortScanResult exportResult = lastResult != null ? lastResult : service.getResult();
        if (exportResult == null) {
            showPrompt(I18nUtils.getString("portscan.msg.no.result"));
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("portscan.export.title"));
        String normalized = format == null ? "json" : format.toLowerCase();
        if ("csv".equals(normalized)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        } else if ("txt".equals(normalized)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT", "*.txt"));
        } else if ("md".equals(normalized)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown", "*.md"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        }
        File defaultDir = new File(StrUtils.getCurrentJarDir(), "PortScanReports");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        chooser.setInitialFileName(buildExportFileName(exportResult, normalized));
        File file = chooser.showSaveDialog(sPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith("." + normalized)) {
            file = new File(file.getParentFile(), file.getName() + "." + normalized);
        }
        try {
            reportWriter.write(exportResult, normalized, file);
            showPrompt(I18nUtils.getString("portscan.msg.export.success"));
        } catch (Exception e) {
            showPrompt(I18nUtils.getString("portscan.msg.export.failed", e.getMessage()));
        }
    }

    @FXML
    public void handoffToVulnScan(MouseEvent event) {
        handoffOpenTargets(null);
    }

    private void handoffOpenTargets(HandoffPolicy policy) {
        List<String> targets = buildOpenTargets();
        if (targets.isEmpty()) {
            showPrompt(I18nUtils.getString("portscan.msg.no.open"));
            return;
        }
        handoffTargets(targets, policy);
    }

    private void handoffTargets(List<String> targets) {
        handoffTargets(targets, null);
    }

    private void handoffTargets(List<String> targets, HandoffPolicy policy) {
        if (policy != null) {
            performHandoff(targets, policy);
            return;
        }
        if (!VulnScanService.getInstance().isScanning()) {
            performHandoff(targets, HandoffPolicy.CANCEL);
            return;
        }
        showPromptChoice(
                I18nUtils.getString("portscan.dialog.busy.title"),
                I18nUtils.getString("portscan.dialog.busy.header"),
                I18nUtils.getString("portscan.dialog.busy.content"),
                Arrays.asList(
                        I18nUtils.getString("portscan.dialog.busy.append"),
                        I18nUtils.getString("portscan.dialog.busy.replace")
                ),
                selected -> {
                    if (selected == null || selected < 0) {
                        showPrompt(I18nUtils.getString("portscan.msg.handoff.canceled"));
                        return;
                    }
                    HandoffPolicy chosen = selected == 0 ? HandoffPolicy.APPEND_AFTER : HandoffPolicy.FORCE_REPLACE;
                    performHandoff(targets, chosen);
                },
                () -> showPrompt(I18nUtils.getString("portscan.msg.handoff.canceled"))
        );
    }

    private void performHandoff(List<String> targets, HandoffPolicy policy) {
        HandoffResult result = VulnScanBridge.getInstance().handoff(targets, policy);
        showPrompt(I18nUtils.getString("portscan.msg.handoff." + result.name().toLowerCase()));
    }

    private PortScanConfig buildConfig() {
        PortScanConfig config = new PortScanConfig();
        config.setPorts(resolvePorts());
        config.setConnectTimeoutMs(parseInt(timeoutField.getText(), 1500, 100, 60000));
        config.setProbeTimeoutMs(Math.max(1000, config.getConnectTimeoutMs() * 2));
        config.setBatchSize(parseInt(batchSizeField.getText(), 1024, 64, 65535));
        config.setServiceProbe(serviceProbeBox.isSelected());
        config.setPublicMode(publicModeBox.isSelected());
        config.setAutoHandoff(autoHandoffBox.isSelected());
        return config;
    }

    @FXML
    public void onPresetChip(ActionEvent e) {
        ToggleButton btn = (ToggleButton) e.getSource();
        try {
            presetIndex = Integer.parseInt((String) btn.getUserData());
        } catch (Exception ignored) {}
        if (portPresetComboBox != null) {
            portPresetComboBox.getSelectionModel().select(presetIndex);
        }
    }

    private int[] resolvePorts() {
        int index = portPresetComboBox != null
                ? portPresetComboBox.getSelectionModel().getSelectedIndex()
                : presetIndex;
        if (index == 1) {
            return PortPresets.top1000();
        }
        if (index == 2) {
            return PortPresets.all();
        }
        if (index == 3) {
            return PortPlanner.parsePorts(customPortsField.getText(), PortPresets.top100());
        }
        return PortPresets.top100();
    }

    private int parseInt(String value, int defaultValue, int min, int max) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void upsertResult(PortResult result) {
        for (int i = 0; i < resultItems.size(); i++) {
            PortResult current = resultItems.get(i);
            if (current.getHost().equals(result.getHost()) && current.getPort() == result.getPort()) {
                resultItems.set(i, result);
                return;
            }
        }
        resultItems.add(result);
    }

    private List<String> buildOpenTargets() {
        List<PortResult> open = lastResult != null ? lastResult.getOpenResults() : service.getOpenResults();
        List<String> targets = new ArrayList<>();
        for (PortResult result : open) {
            targets.add(result.toTargetUrl());
        }
        return targets;
    }

    private void updateButtons(boolean scanning, boolean paused) {
        startButton.setDisable(scanning);
        pauseButton.setDisable(!scanning || paused);
        resumeButton.setDisable(!paused);
        stopButton.setDisable(!scanning);
        handoffButton.setDisable(scanning || resultItems.isEmpty());
    }

    private void showPrompt(String message) {
        showPrompt(message, false);
    }

    private void showPrompt(String message, boolean isError) {
        Platform.runLater(() -> {
            if (promptLabel != null) {
                promptLabel.setText(message == null ? "" : message);
            }
            applyPromptStyle(isError);
            playPromptAnimation(true);
        });
    }

    private void applyPromptStyle(boolean isError) {
        if (promptPane == null) {
            return;
        }
        promptPane.getStyleClass().removeAll("portscan-prompt-info", "portscan-prompt-error", "portscan-prompt-dialog");
        promptPane.getStyleClass().add(isError ? "portscan-prompt-error" : "portscan-prompt-info");
    }

    private void playPromptAnimation(boolean autoClose) {
        if (promptPane == null) {
            return;
        }
        promptDialogVisible = false;
        restorePromptDefaultContent();
        promptAutoCloseEnabled = autoClose;

        if (promptFadeIn == null) {
            promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeIn.setFromValue(0);
            promptFadeIn.setToValue(1);
        }
        if (promptFadeOut == null) {
            promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeOut.setFromValue(1);
            promptFadeOut.setToValue(0);
            promptFadeOut.setDelay(Duration.seconds(2));
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
        promptPane.toFront();
        promptFadeIn.setOnFinished(event -> {
            if (promptAutoCloseEnabled && promptFadeOut != null) {
                promptFadeOut.playFromStart();
            }
        });
        promptFadeIn.playFromStart();
    }

    private void showPromptChoice(String title, String header, String content,
                                  List<String> choices, Consumer<Integer> onSelected, Runnable onCancel) {
        VBox body = new VBox(12);
        body.setFillWidth(true);
        body.getChildren().add(createPromptTextLabel(content, 620));

        HBox choiceBox = new HBox(10);
        choiceBox.getStyleClass().add("prompt-dialog-actions");
        for (int i = 0; i < choices.size(); i++) {
            final int index = i;
            Button button = createPromptButton(choices.get(i), i == choices.size() - 1);
            button.setOnAction(event -> {
                hidePromptPane();
                if (onSelected != null) {
                    onSelected.accept(index);
                }
            });
            choiceBox.getChildren().add(button);
        }
        body.getChildren().add(choiceBox);

        showPromptContent(title, header, body, null, I18nUtils.getString("app.cancel"), null, onCancel);
    }

    private void showPromptContent(String title, String header, Node body,
                                   String confirmText, String cancelText,
                                   Runnable onConfirm, Runnable onCancel) {
        Platform.runLater(() -> {
            if (promptPane == null) {
                return;
            }
            stopPromptTransitions();
            promptDialogVisible = true;
            promptPane.getStyleClass().removeAll("portscan-prompt-info", "portscan-prompt-error", "portscan-prompt-dialog");
            promptPane.getStyleClass().add("portscan-prompt-dialog");

            VBox dialog = new VBox(12);
            dialog.getStyleClass().add("prompt-dialog");
            dialog.setMaxWidth(760);
            dialog.setFillWidth(true);
            dialog.setOnMouseClicked(MouseEvent::consume);

            if (title != null && !title.trim().isEmpty()) {
                Label titleLabel = createPromptTextLabel(title, 700);
                titleLabel.getStyleClass().add("prompt-dialog-title");
                dialog.getChildren().add(titleLabel);
            }
            if (header != null && !header.trim().isEmpty()) {
                Label headerLabel = createPromptTextLabel(header, 700);
                headerLabel.getStyleClass().add("prompt-dialog-header");
                dialog.getChildren().add(headerLabel);
            }
            if (body != null) {
                dialog.getChildren().add(body);
            }

            if (confirmText != null || cancelText != null) {
                HBox actions = new HBox(10);
                actions.getStyleClass().add("prompt-dialog-actions");
                if (cancelText != null && !cancelText.trim().isEmpty()) {
                    Button cancelButton = createPromptButton(cancelText, false);
                    cancelButton.setOnAction(event -> {
                        hidePromptPane();
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    });
                    actions.getChildren().add(cancelButton);
                }
                if (confirmText != null && !confirmText.trim().isEmpty()) {
                    Button confirmButton = createPromptButton(confirmText, true);
                    confirmButton.setOnAction(event -> {
                        hidePromptPane();
                        if (onConfirm != null) {
                            onConfirm.run();
                        }
                    });
                    actions.getChildren().add(confirmButton);
                }
                dialog.getChildren().add(actions);
            }

            promptPane.getChildren().setAll(dialog);
            promptPane.setOpacity(0);
            promptPane.setVisible(true);
            promptPane.setManaged(true);
            promptPane.toFront();

            promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeIn.setFromValue(0);
            promptFadeIn.setToValue(1);
            promptFadeIn.playFromStart();
        });
    }

    private Label createPromptTextLabel(String text, double maxWidth) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMaxWidth(maxWidth);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add("prompt-dialog-text");
        return label;
    }

    private Button createPromptButton(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "prompt-dialog-primary" : "prompt-dialog-secondary");
        button.setMinWidth(92);
        button.setMinHeight(30);
        return button;
    }

    @FXML
    public void closePromptPane(MouseEvent event) {
        if (promptDialogVisible) {
            return;
        }
        hidePromptPane();
    }

    private void hidePromptPane() {
        if (promptPane == null) {
            return;
        }
        stopPromptTransitions();
        promptDialogVisible = false;
        promptPane.setVisible(false);
        promptPane.setManaged(false);
        restorePromptDefaultContent();
    }

    private void stopPromptTransitions() {
        if (promptFadeIn != null) {
            promptFadeIn.stop();
        }
        if (promptFadeOut != null) {
            promptFadeOut.stop();
        }
    }

    private void restorePromptDefaultContent() {
        if (promptPane == null || promptDefaultContent == null) {
            return;
        }
        if (promptPane.getChildren().size() != 1 || promptPane.getChildren().get(0) != promptDefaultContent) {
            promptPane.getChildren().setAll(promptDefaultContent);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String buildExportFileName(PortScanResult result, String ext) {
        long ts = result != null && result.getStartTime() > 0 ? result.getStartTime() : System.currentTimeMillis();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(ts));
        return "portscan_report_" + stamp + "." + (ext == null ? "json" : ext.toLowerCase());
    }
}
