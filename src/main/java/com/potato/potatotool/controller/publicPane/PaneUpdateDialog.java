package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFCheckBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.update.ResourceUpdate;
import com.potato.potatotool.update.UpdateChecker;
import com.potato.potatotool.update.UpdateInfo;
import com.potato.potatotool.update.UpdateManager;
import com.potato.potatotool.update.manifest.Manifest;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nManager;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;

import static com.potato.potatotool.ToStart.debugMode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 更新提示对话框Controller
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class PaneUpdateDialog {
    private enum PromptNoticeType {
        INFO,
        ERROR,
        WARNING
    }
    
    @FXML
    private AnchorPane an;

    @FXML
    private BorderPane topBar;

    @FXML
    private Label titleLabel;

    @FXML
    private Label versionInfoLabel;

    @FXML
    private Label updateCountLabel;

    @FXML
    private VBox changelogBox;

    @FXML
    private VBox changelogContent;

    @FXML
    private Label changelogArrow;

    @FXML
    private Label changelogToggleBtn;

    @FXML
    private TextArea changelogText;

    @FXML
    private VBox updateItemsBox;

    @FXML
    private VBox updateItemsContainer;

    // 更新项checkbox映射
    private Map<CFCheckBox, UpdateItem> updateItemsMap = new LinkedHashMap<>();

    private boolean isChangelogExpanded = false;
    
    @FXML
    private VBox progressBox;
    
    @FXML
    private Label progressLabel;
    
    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private Label sizeLabel;
    
    @FXML
    private Label speedLabel;
    
    @FXML
    private HBox buttonBox;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button laterButton;
    
    @FXML
    private Button skipButton;
    
    @FXML
    private HBox restartBox;
    
    @FXML
    private Label restartLabel;
    
    @FXML
    private Button restartNowButton;
    
    @FXML
    private Pane promptPane;
    
    @FXML
    private Label prompt;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;
    
    private double offsetX, offsetY;
    
    private UpdateInfo updateInfo;
    private UpdateManager updateManager;
    private I18nManager i18n = I18nManager.getInstance();
    
    public void initialize() {
        updateManager = UpdateManager.getInstance();
        
        // 应用国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(an);
        });

        titleLabel.setText(i18n.getString("update.available.title"));
    }
    
    /**
     * 设置更新信息并显示
     */
    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
        
        Platform.runLater(() -> {
            showAllUpdates();
        });
    }
    
    /**
     * 显示所有可用更新（软件+资源，使用checkbox）
     */
    private void showAllUpdates() {
        updateVersionInfoLabel();

        updateItemsBox.setVisible(true);
        updateItemsBox.setManaged(true);
        updateItemsContainer.getChildren().clear();
        updateItemsMap.clear();
        isChangelogExpanded = false;

        // 添加软件更新checkbox
        if (updateInfo.isAppNeedUpdate()) {
            String localVersion = updateInfo.getLocalAppVersion();
            String remoteVersion = updateInfo.getAppVersion().getVersion();
            boolean isSkipped = isAppVersionSkipped(remoteVersion);
            boolean isRequired = updateInfo.getAppVersion().isRequired();

            long appSize = getCurrentPlatformAppSize(updateInfo.getAppVersion());
            String sizeText = appSize > 0 ? formatSize(appSize) : "";

            String description = "";
            List<String> cl = updateInfo.getAppVersion().getChangelog();
            if (cl != null && !cl.isEmpty()) {
                description = cl.get(0);
            }

            CFCheckBox appCheckBox = new CFCheckBox();
            appCheckBox.setSelected(!isSkipped);

            UpdateItem appItem = new UpdateItem(
                    "app", null, i18n.getString("update.available.app"), remoteVersion,
                    isRequired, localVersion, sizeText, description);
            updateItemsMap.put(appCheckBox, appItem);
            updateItemsContainer.getChildren().add(createUpdateItemNode(appCheckBox, appItem));

            // 显示更新日志（折叠状态）
            if (cl != null && !cl.isEmpty()) {
                changelogBox.setVisible(true);
                changelogBox.setManaged(true);
                if (changelogContent != null) {
                    changelogContent.setVisible(false);
                    changelogContent.setManaged(false);
                }
                if (changelogArrow != null) changelogArrow.setText("▶");
                if (changelogToggleBtn != null) {
                    changelogToggleBtn.setText(i18n.getString("update.changelog.expand"));
                }
                StringBuilder changelog = new StringBuilder();
                for (String line : cl) {
                    changelog.append("• ").append(line).append("\n");
                }
                changelogText.setText(changelog.toString());
            }
        }

        // 添加资源更新checkbox
        if (updateInfo.hasResourceUpdates()) {
            List<ResourceUpdate> updates = updateInfo.getResourceUpdates();
            for (ResourceUpdate update : updates) {
                boolean isSkipped = isResourceVersionSkipped(update.getResourceName(), update.getRemoteVersion());
                boolean isRequired = update.getResource().isRequired();
                String sizeText = update.getFileSize() > 0 ? formatSize(update.getFileSize()) : "";
                String description = update.getDescription() != null ? update.getDescription().trim() : "";

                CFCheckBox resourceCheckBox = new CFCheckBox();
                resourceCheckBox.setSelected(!isSkipped);

                UpdateItem resourceItem = new UpdateItem(
                        "resource", update.getResourceName(), update.getDisplayName(),
                        update.getRemoteVersion(), isRequired, update.getLocalVersion(),
                        sizeText, description);
                updateItemsMap.put(resourceCheckBox, resourceItem);
                updateItemsContainer.getChildren().add(createUpdateItemNode(resourceCheckBox, resourceItem));
            }
        }
    }
    
    /**
     * 动态更新版本信息标签和数量徽章
     */
    private void updateVersionInfoLabel() {
        if (updateInfo.isAppNeedUpdate()) {
            String localVersion = updateInfo.getLocalAppVersion();
            String remoteVersion = updateInfo.getAppVersion().getVersion();
            versionInfoLabel.setText("PotatoTool " + localVersion + " → " + remoteVersion);
        } else if (updateInfo.hasResourceUpdates()) {
            int count = updateInfo.getResourceUpdates().size();
            versionInfoLabel.setText(I18nUtils.getString("update.available.resources.count", count));
        } else {
            versionInfoLabel.setText(i18n.getString("update.available.found"));
        }

        // 数量徽章
        int totalCount = (updateInfo.isAppNeedUpdate() ? 1 : 0)
                + (updateInfo.hasResourceUpdates() ? updateInfo.getResourceUpdates().size() : 0);
        if (updateCountLabel != null) {
            updateCountLabel.setText(I18nUtils.getString("update.available.count", totalCount));
            updateCountLabel.setVisible(totalCount > 0);
            updateCountLabel.setManaged(totalCount > 0);
        }
    }

    private VBox createUpdateItemNode(CFCheckBox checkBox, UpdateItem item) {
        checkBox.setText("");

        Label nameLabel = new Label(item.displayName != null ? item.displayName : "");
        nameLabel.getStyleClass().add("upd-item-name");

        String versionStr = (item.localVersion != null ? item.localVersion : "—")
                + " → " + (item.version != null ? item.version : "");
        Label versionLabel = new Label(versionStr);
        versionLabel.getStyleClass().add("upd-item-version");

        VBox nameBox = new VBox(2, nameLabel, versionLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label badge = new Label(item.isRequired
                ? i18n.getString("update.available.required")
                : i18n.getString("update.available.optional"));
        badge.getStyleClass().add(item.isRequired ? "upd-badge-required" : "upd-badge-optional");

        VBox rightBox = new VBox(4, badge);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        if (item.sizeText != null && !item.sizeText.isEmpty()) {
            Label szLabel = new Label(item.sizeText);
            szLabel.getStyleClass().add("upd-item-size");
            rightBox.getChildren().add(szLabel);
        }

        HBox mainRow = new HBox(10, checkBox, nameBox, rightBox);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setStyle("-fx-padding: 12 14 8 14;");

        VBox container = new VBox(0);
        container.getStyleClass().add("upd-item-row");
        if (item.isRequired) container.getStyleClass().add("upd-item-row-required");
        container.getChildren().add(mainRow);

        if (item.description != null && !item.description.trim().isEmpty()) {
            Label descLabel = new Label(item.description);
            descLabel.getStyleClass().add("upd-item-desc");
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-padding: 0 14 10 46;");
            container.getChildren().add(descLabel);
        }

        return container;
    }

    @FXML
    private void toggleChangelog() {
        isChangelogExpanded = !isChangelogExpanded;
        if (changelogContent != null) {
            changelogContent.setVisible(isChangelogExpanded);
            changelogContent.setManaged(isChangelogExpanded);
        }
        if (changelogArrow != null) {
            changelogArrow.setText(isChangelogExpanded ? "▼" : "▶");
        }
        if (changelogToggleBtn != null) {
            changelogToggleBtn.setText(i18n.getString(
                    isChangelogExpanded ? "update.changelog.collapse" : "update.changelog.expand"));
        }
    }

    private String buildAppDetailText(Manifest.AppVersion appVersion) {
        List<String> details = new ArrayList<>();
        details.add(appVersion.isRequired()
                ? i18n.getString("update.available.required")
                : i18n.getString("update.available.optional"));

        long size = getCurrentPlatformAppSize(appVersion);
        if (size > 0) {
            details.add(I18nUtils.getString("update.available.size", formatSize(size)));
        }
        return joinDetails(details);
    }

    private long getCurrentPlatformAppSize(Manifest.AppVersion appVersion) {
        if (appVersion == null || appVersion.getFiles() == null || appVersion.getFiles().isEmpty()) {
            return 0;
        }

        String platform = UpdateChecker.getPlatformIdentifier();
        Manifest.FileInfo fileInfo = appVersion.getFiles().get(platform);
        if (fileInfo == null) {
            fileInfo = appVersion.getFiles().values().iterator().next();
        }
        return fileInfo != null ? fileInfo.getSize() : 0;
    }

    private String buildResourceDetailText(ResourceUpdate update) {
        List<String> details = new ArrayList<>();
        details.add(update.getResource().isRequired()
                ? i18n.getString("update.available.required")
                : i18n.getString("update.available.optional"));

        if (update.getFileSize() > 0) {
            details.add(I18nUtils.getString("update.available.size", formatSize(update.getFileSize())));
        }
        if (update.getDescription() != null && !update.getDescription().trim().isEmpty()) {
            details.add(update.getDescription().trim());
        }
        return joinDetails(details);
    }

    private String joinDetails(List<String> details) {
        StringBuilder builder = new StringBuilder();
        for (String detail : details) {
            if (detail == null || detail.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("  |  ");
            }
            builder.append(detail.trim());
        }
        return builder.toString();
    }
    
    /**
     * 更新项数据类
     */
    private static class UpdateItem {
        String type; // "app" or "resource"
        String resourceName; // 资源名称（如"md5"）
        String displayName; // 显示名称
        String version; // 远程版本号（用于跳过逻辑）
        boolean isRequired;
        String localVersion;
        String sizeText;
        String description;

        UpdateItem(String type, String resourceName, String displayName, String version,
                   boolean isRequired, String localVersion, String sizeText, String description) {
            this.type = type;
            this.resourceName = resourceName;
            this.displayName = displayName;
            this.version = version;
            this.isRequired = isRequired;
            this.localVersion = localVersion;
            this.sizeText = sizeText;
            this.description = description;
        }
    }
    
    
    
    /**
     * 立即更新按钮点击（只更新选中的项）
     */
    @FXML
    private void onUpdateClick() {
        // 获取选中的更新项
        List<UpdateItem> selectedItems = new ArrayList<>();
        for (Map.Entry<CFCheckBox, UpdateItem> entry: updateItemsMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                selectedItems.add(entry.getValue());
            }
        }
        
        if (selectedItems.isEmpty()) {
            showTip(i18n.getString("update.select.none"));
            return;
        }

        if (debugMode) System.out.println("用户选中了" + selectedItems.size() + "个更新项");
        
        // 隐藏按钮，显示进度
        buttonBox.setVisible(false);
        buttonBox.setManaged(false);
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        
        // 检查是否选中了软件更新
        boolean hasSelectedApp = false;
        for (UpdateItem item : selectedItems) {
            if ("app".equals(item.type)) {
                hasSelectedApp = true;
                break;
            }
        }
        
        // 获取选中的资源更新
        List<ResourceUpdate> selectedResourceUpdates = new ArrayList<>();
        for (UpdateItem item : selectedItems) {
            if ("resource".equals(item.type)) {
                // 从原始resourceUpdates中找到对应的ResourceUpdate对象
                for (ResourceUpdate ru : updateInfo.getResourceUpdates()) {
                    if (ru.getResourceName().equals(item.resourceName)) {
                        selectedResourceUpdates.add(ru);
                        break;
                    }
                }
            }
        }
        
        // 执行更新
        if (hasSelectedApp && !selectedResourceUpdates.isEmpty()) {
            startSelectedResourceUpdate(selectedResourceUpdates, false, this::startAppUpdate);
        } else if (hasSelectedApp) {
            startAppUpdate();
        } else if (!selectedResourceUpdates.isEmpty()) {
            startSelectedResourceUpdate(selectedResourceUpdates);
        }
    }
    
    /**
     * 开始软件更新
     */
    private void startAppUpdate() {
        // 初始化进度UI
        progressLabel.setText(i18n.getString("update.download.title"));
        progressBar.setProgress(0);
        sizeLabel.setText("0 MB / 0 MB");
        speedLabel.setText("0 KB/s");
        
        updateManager.updateApp(new UpdateManager.UpdateCallback() {
            @Override
            public void onStart(String message) {
                Platform.runLater(() -> {
                    progressLabel.setText(message);
                    progressBar.setProgress(0);
                });
            }
            
            @Override
            public void onProgress(long current, long total, int percentage, String message) {
                Platform.runLater(() -> {
                    progressBar.setProgress(percentage / 100.0);
                    // 显示下载大小：15.2 MB / 50.0 MB
                    sizeLabel.setText(formatSize(current) + " / " + formatSize(total));
                    // 显示下载速度：message 已经是格式化好的速度字符串（如 "1.2 MB/s"）
                    speedLabel.setText(message != null ? message : "");
                });
            }
            
            @Override
            public void onComplete(String message) {
                Platform.runLater(() -> {
                    // 更新完成提示，1.5秒后自动关闭
                    showPromptNotice(PromptNoticeType.INFO,
                            i18n.getString("update.install.title"), 
                            message != null ? message : i18n.getString("update.install.app.success"));
                    // showPromptNotice 内部会自动关闭
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    // 先恢复UI，再显示错误信息
                    resetUI();
                    // 错误提示显示3秒，不自动关闭对话框，让用户看清错误
                    String errorMessage = error != null ? error : i18n.getString("update.error.download");
                    // 网络问题友好提示
                    if (error != null && (error.contains("UnknownHost") || error.contains("timeout") 
                            || error.contains("连接") || error.contains("网络"))) {
                        errorMessage = i18n.getString("update.error.network") + "\n" + error;
                    }
                    showPromptNotice(PromptNoticeType.ERROR,
                            i18n.getString("update.error.title"), errorMessage, false);
                });
            }
            
            @Override
            public void onCancel() {
                Platform.runLater(() -> resetUI());
            }
            
            @Override
            public void onReadyToInstall() {
                Platform.runLater(() -> showRestartOptions());
            }
        });
    }
    
    /**
     * 开始更新选中的资源
     */
    private void startSelectedResourceUpdate(List<ResourceUpdate> selectedResources) {
        startSelectedResourceUpdate(selectedResources, true, null);
    }

    private void startSelectedResourceUpdate(List<ResourceUpdate> selectedResources,
                                            boolean showSuccessAlert,
                                            Runnable onSuccess) {
        // 初始化进度UI
        progressLabel.setText(i18n.getString("update.download.title"));
        progressBar.setProgress(0);
        sizeLabel.setText("0 MB / 0 MB");
        speedLabel.setText("0 KB/s");
        
        // 用于计算速度
        final long[] lastTime = {System.currentTimeMillis()};
        final long[] lastBytes = {0};
        
        updateManager.updateResources(selectedResources, 
            new UpdateManager.BatchUpdateCallback() {
                @Override
                public void onStart(String message) {
                    Platform.runLater(() -> {
                        progressLabel.setText(message);
                        progressBar.setProgress(0);
                        lastTime[0] = System.currentTimeMillis();
                        lastBytes[0] = 0;
                    });
                }
                
                @Override
                public void onBatchProgress(int current, int total, String resourceName, 
                                          long downloaded, long totalSize, int percentage) {
                    Platform.runLater(() -> {
                        // 根据状态显示资源名称和进度/状态
                        String statusSuffix = "";
                        if (downloaded == -1) {
                            // 完成
                            statusSuffix = " - " + i18n.getString("update.message.success");
                            sizeLabel.setText(i18n.getString("update.message.success"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -2) {
                            // 错误
                            statusSuffix = " - " + i18n.getString("update.message.failed");
                            sizeLabel.setText(i18n.getString("update.message.failed"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -3) {
                            // 校验中
                            statusSuffix = " - " + i18n.getString("update.message.verifying");
                            sizeLabel.setText(i18n.getString("update.download.verifying"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -4) {
                            // 解压中
                            statusSuffix = " - " + i18n.getString("update.message.extracting");
                            sizeLabel.setText(i18n.getString("update.message.extracting.file"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -5) {
                            // 正在选择最佳下载源
                            statusSuffix = " - " + i18n.getString("update.message.selecting.source");
                            sizeLabel.setText(i18n.getString("update.message.selecting.source"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (totalSize > 0) {
                            // 正在下载，显示下载进度百分比
                            statusSuffix = String.format(" - %d%%", percentage);
                            sizeLabel.setText(formatSize(downloaded) + " / " + formatSize(totalSize));
                            
                            // 计算速度
                            long currentTime = System.currentTimeMillis();
                            long timeDiff = currentTime - lastTime[0];
                            if (timeDiff > 500) {  // 每500ms更新一次速度
                                long bytesDiff = downloaded - lastBytes[0];
                                long speed = (long) (bytesDiff * 1000.0 / timeDiff);  // 字节/秒
                                speedLabel.setText(formatSpeed(speed));
                                lastTime[0] = currentTime;
                                lastBytes[0] = downloaded;
                            }
                        } else {
                            // 开始下载
                            statusSuffix = " - 0%";
                            sizeLabel.setText("0 MB / 0 MB");
                            speedLabel.setText("0 KB/s");
                            lastTime[0] = System.currentTimeMillis();
                            lastBytes[0] = 0;
                        }
                        
                        // 显示资源名称和状态
                        progressLabel.setText(String.format("[%d/%d] %s%s", 
                                                          current, total, resourceName, statusSuffix));
                        
                        // 计算总体进度：已完成资源数量占比 + 当前资源进度占比
                        double baseProgress = (double) (current - 1) / total;
                        double currentResourceProgress = (percentage / 100.0) / total;
                        double overallProgress = baseProgress + currentResourceProgress;
                        progressBar.setProgress(Math.min(overallProgress, 1.0));
                    });
                }
                
                @Override
                public void onComplete(String message) {
                    Platform.runLater(() -> {
                        if (onSuccess != null) {
                            onSuccess.run();
                            return;
                        }

                        if (showSuccessAlert) {
                            showPromptNotice(PromptNoticeType.INFO,
                                    i18n.getString("update.install.title"),
                                    i18n.getString("update.install.resource.success"));
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        // 先恢复UI，再显示错误信息
                        resetUI();
                        // 错误提示显示3秒，不自动关闭对话框
                        String errorMessage = error != null ? error : i18n.getString("update.error.download");
                        // 网络问题友好提示
                        if (error != null && (error.contains("UnknownHost") || error.contains("timeout") 
                                || error.contains("连接") || error.contains("网络"))) {
                            errorMessage = i18n.getString("update.error.network") + "\n" + error;
                        }
                        showPromptNotice(PromptNoticeType.ERROR,
                                i18n.getString("update.error.title"), errorMessage, false);
                    });
                }
                
                @Override
                public void onCancel() {
                    Platform.runLater(() -> resetUI());
                }
            });
    }
    
    /**
     * 开始资源更新（所有资源）
     * 备用方法，保留用于未来可能的批量更新场景
     */
    private void startResourceUpdate() {
        // 初始化进度UI
        progressLabel.setText(i18n.getString("update.download.title"));
        progressBar.setProgress(0);
        sizeLabel.setText("0 MB / 0 MB");
        speedLabel.setText("0 KB/s");
        
        // 用于计算速度
        final long[] lastTime = {System.currentTimeMillis()};
        final long[] lastBytes = {0};
        
        updateManager.updateResources(updateInfo.getResourceUpdates(), 
            new UpdateManager.BatchUpdateCallback() {
                @Override
                public void onStart(String message) {
                    Platform.runLater(() -> {
                        progressLabel.setText(message);
                        progressBar.setProgress(0);
                        lastTime[0] = System.currentTimeMillis();
                        lastBytes[0] = 0;
                    });
                }
                
                @Override
                public void onBatchProgress(int current, int total, String resourceName, 
                                          long downloaded, long totalSize, int percentage) {
                    Platform.runLater(() -> {
                        // 根据状态显示资源名称和进度/状态
                        String statusSuffix = "";
                        if (downloaded == -1) {
                            // 完成
                            statusSuffix = " - " + i18n.getString("update.message.success");
                            sizeLabel.setText(i18n.getString("update.message.success"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -2) {
                            // 错误
                            statusSuffix = " - " + i18n.getString("update.message.failed");
                            sizeLabel.setText(i18n.getString("update.message.failed"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -3) {
                            // 校验中
                            statusSuffix = " - " + i18n.getString("update.message.verifying");
                            sizeLabel.setText(i18n.getString("update.download.verifying"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -4) {
                            // 解压中
                            statusSuffix = " - " + i18n.getString("update.message.extracting");
                            sizeLabel.setText(i18n.getString("update.message.extracting.file"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (downloaded == -5) {
                            // 正在选择最佳下载源
                            statusSuffix = " - " + i18n.getString("update.message.selecting.source");
                            sizeLabel.setText(i18n.getString("update.message.selecting.source"));
                            speedLabel.setText(String.format("%d/%d", current, total));
                        } else if (totalSize > 0) {
                            // 正在下载，显示下载进度百分比
                            statusSuffix = String.format(" - %d%%", percentage);
                            sizeLabel.setText(formatSize(downloaded) + " / " + formatSize(totalSize));
                            
                            // 计算速度
                            long currentTime = System.currentTimeMillis();
                            long timeDiff = currentTime - lastTime[0];
                            if (timeDiff > 500) {  // 每500ms更新一次速度
                                long bytesDiff = downloaded - lastBytes[0];
                                long speed = (long) (bytesDiff * 1000.0 / timeDiff);  // 字节/秒
                                speedLabel.setText(formatSpeed(speed));
                                lastTime[0] = currentTime;
                                lastBytes[0] = downloaded;
                            }
                        } else {
                            // 开始下载
                            statusSuffix = " - 0%";
                            sizeLabel.setText("0 MB / 0 MB");
                            speedLabel.setText("0 KB/s");
                            lastTime[0] = System.currentTimeMillis();
                            lastBytes[0] = 0;
                        }
                        
                        // 显示资源名称和状态
                        progressLabel.setText(String.format("[%d/%d] %s%s", 
                                                          current, total, resourceName, statusSuffix));
                        
                        // 计算总体进度：已完成资源数量占比 + 当前资源进度占比
                        double baseProgress = (double) (current - 1) / total;
                        double currentResourceProgress = (percentage / 100.0) / total;
                        double overallProgress = baseProgress + currentResourceProgress;
                        progressBar.setProgress(Math.min(overallProgress, 1.0));
                    });
                }
                
                @Override
                public void onComplete(String message) {
                    Platform.runLater(() -> {
                        // 资源更新完成提示，1.5秒后自动关闭
                        showPromptNotice(PromptNoticeType.INFO,
                                i18n.getString("update.install.title"),
                                i18n.getString("update.install.resource.success"));
                        // showPromptNotice 内部会自动关闭
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        // 先恢复UI，再显示错误信息
                        resetUI();
                        // 错误提示显示3秒，不自动关闭对话框
                        String errorMessage = error != null ? error : i18n.getString("update.error.download");
                        // 网络问题友好提示
                        if (error != null && (error.contains("UnknownHost") || error.contains("timeout") 
                                || error.contains("连接") || error.contains("网络"))) {
                            errorMessage = i18n.getString("update.error.network") + "\n" + error;
                        }
                        showPromptNotice(PromptNoticeType.ERROR,
                                i18n.getString("update.error.title"), errorMessage, false);
                    });
                }
                
                @Override
                public void onCancel() {
                    Platform.runLater(() -> resetUI());
                }
            });
    }
    
    /**
     * 显示重启选项
     */
    private void showRestartOptions() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
        restartBox.setVisible(true);
        restartBox.setManaged(true);
    }
    
    /**
     * 立即重启
     */
    @FXML
    private void onRestartNowClick() {
        try {
            updateManager.executeAppUpdate();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            // 安装失败，不自动关闭对话框
            showPromptNotice(PromptNoticeType.ERROR,
                    i18n.getString("update.error.title"),
                    i18n.getString("update.error.install") + ": " + e.getMessage(), false);
        }
    }
    
    /**
     * 稍后提醒按钮点击
     */
    @FXML
    private void onLaterClick() {
        closeDialog();
    }
    
    /**
     * 跳过选中项按钮点击
     */
    @FXML
    private void onSkipClick() {
        if (debugMode) System.out.println("=== onSkipClick() 被调用 ===");
        try {
            if (updateInfo == null) {
                if (debugMode) System.out.println("错误：updateInfo为null");
                closeDialog();
                return;
            }
            
            // 获取选中的更新项
            List<UpdateItem> selectedItems = new ArrayList<>();
            for (Map.Entry<CFCheckBox, UpdateItem> entry : updateItemsMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedItems.add(entry.getValue());
                }
            }
            
            if (selectedItems.isEmpty()) {
                showTip(i18n.getString("update.select.none"), this::closeDialog);
                return;
            }
            
            if (debugMode) System.out.println("用户选择跳过" + selectedItems.size() + "个更新项");
            
            // 跳过选中的项
            for (UpdateItem item : selectedItems) {
                if ("app".equals(item.type)) {
                    if (debugMode) System.out.println("跳过软件版本: " + item.version);
                    saveSkippedVersion(item.version);
                } else if ("resource".equals(item.type)) {
                    if (debugMode) System.out.println("跳过资源: " + item.resourceName + " v" + item.version);
                    saveSkippedResourceVersion(item.resourceName, item.version);
                }
            }
            
            showTip(i18n.getString("update.settings.skip.tip"), this::closeDialog);
            
        } catch (Exception e) {
            System.err.println("跳过版本时发生异常:");
            e.printStackTrace();
            closeDialog();
        }
    }
    
    /**
     * 检查软件版本是否已被跳过
     */
    private boolean isAppVersionSkipped(String version) {
        try {
            JsonObject updateConfig = (JsonObject) 
                    Constants.getOutsideConfig(ConfigConstants.UPDATE);
            
            if (updateConfig != null && updateConfig.has(
                    ConfigConstants.UPDATE_SKIPPED_VERSIONS)) {
                JsonArray skippedVersions = updateConfig.getAsJsonArray(
                        ConfigConstants.UPDATE_SKIPPED_VERSIONS);
                for (JsonElement elem : skippedVersions) {
                    if (elem.getAsString().equals(version)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 检查资源版本是否已被跳过
     */
    private boolean isResourceVersionSkipped(String resourceName, String version) {
        try {
            JsonObject updateConfig = (JsonObject) 
                    Constants.getOutsideConfig(ConfigConstants.UPDATE);
            
            if (updateConfig != null && updateConfig.has(
                    ConfigConstants.UPDATE_RESOURCES)) {
                JsonObject resources = updateConfig.getAsJsonObject(
                        ConfigConstants.UPDATE_RESOURCES);
                if (resources.has(resourceName)) {
                    JsonObject resourceConfig = resources.getAsJsonObject(resourceName);
                    if (resourceConfig.has(ConfigConstants.UPDATE_SKIPPED_VERSIONS)) {
                        JsonElement skippedElem = resourceConfig.get(
                                ConfigConstants.UPDATE_SKIPPED_VERSIONS);
                        if (skippedElem.isJsonArray()) {
                            for (JsonElement elem : skippedElem.getAsJsonArray()) {
                                if (elem.getAsString().equals(version)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 保存跳过的资源版本（使用合并模式，不会覆盖其他资源）
     * 使用skippedVersions数组存储，支持跳过多个版本
     */
    private void saveSkippedResourceVersion(String resourceName, String version) {
        try {
            JsonObject updateConfig = (JsonObject) 
                    Constants.getOutsideConfig(ConfigConstants.UPDATE);
            
            if (updateConfig == null) {
                updateConfig = new JsonObject();
            }
            
            // 获取resources配置
            JsonObject resources = null;
            if (updateConfig.has(ConfigConstants.UPDATE_RESOURCES)) {
                resources = updateConfig.getAsJsonObject(ConfigConstants.UPDATE_RESOURCES);
            }
            
            // 获取该资源的现有配置
            Map<String, Object> resourceMap = new LinkedHashMap<>();
            
            if (resources != null && resources.has(resourceName)) {
                JsonObject existingConfig = resources.getAsJsonObject(resourceName);
                // 复制现有字段（version、fileName、checksum等）
                for (String key : existingConfig.keySet()) {
                    JsonElement element = existingConfig.get(key);
                    if (element.isJsonPrimitive()) {
                        // 基本类型，直接获取字符串值
                        resourceMap.put(key, element.getAsString());
                    } else if (element.isJsonObject()) {
                        // 对于checksum等对象，转换为Map
                        Map<String, Object> objMap = new LinkedHashMap<>();
                        JsonObject jsonObj = element.getAsJsonObject();
                        for (String objKey : jsonObj.keySet()) {
                            JsonElement objValue = jsonObj.get(objKey);
                            if (objValue.isJsonPrimitive()) {
                                objMap.put(objKey, objValue.getAsString());
                            }
                        }
                        resourceMap.put(key, objMap);
                    } else if (element.isJsonArray()) {
                        // 复制数组（如skippedVersions）
                        List<String> list = new ArrayList<>();
                        for (JsonElement elem : element.getAsJsonArray()) {
                            if (elem.isJsonPrimitive()) {
                                list.add(elem.getAsString());
                            }
                        }
                        resourceMap.put(key, list);
                    }
                }
            }
            
            // 获取或创建skippedVersions数组
            @SuppressWarnings("unchecked")
            List<String> skippedVersions = (List<String>) 
                    resourceMap.getOrDefault(ConfigConstants.UPDATE_SKIPPED_VERSIONS, 
                                           new ArrayList<String>());
            
            // 添加新的跳过版本（避免重复）
            if (!skippedVersions.contains(version)) {
                skippedVersions.add(version);
            }
            
            resourceMap.put(ConfigConstants.UPDATE_SKIPPED_VERSIONS, skippedVersions);
            
            if (debugMode) {
                System.out.println("保存跳过的资源: " + resourceName + " v" + version);
                System.out.println("跳过版本列表: " + skippedVersions);
            }
            
            // 使用统一的合并保存方法，避免覆盖其他资源（如 md5Database、winKbInfo 等）
            Map<String, Object> resourcesMap = new LinkedHashMap<>();
            resourcesMap.put(resourceName, resourceMap);
            
            Constants.saveResourceConfigMerge(resourcesMap);
            
            // 清除缓存
            Constants.cachedConfig = null;
            
        } catch (Exception e) {
            System.err.println("保存跳过资源版本失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存跳过的软件版本号到配置
     */
    private void saveSkippedVersion(String version) {
        try {
            JsonObject updateConfig = (JsonObject) 
                    Constants.getOutsideConfig(ConfigConstants.UPDATE);
            
            if (updateConfig == null) {
                updateConfig = new JsonObject();
            }
            
            // 获取或创建跳过的版本列表
            JsonArray skippedVersions;
            if (updateConfig.has(ConfigConstants.UPDATE_SKIPPED_VERSIONS)) {
                skippedVersions = updateConfig.getAsJsonArray(
                        ConfigConstants.UPDATE_SKIPPED_VERSIONS);
            } else {
                skippedVersions = new JsonArray();
            }
            
            // 检查是否已存在
            boolean alreadySkipped = false;
            for (JsonElement elem : skippedVersions) {
                if (elem.getAsString().equals(version)) {
                    alreadySkipped = true;
                    break;
                }
            }
            
            if (!alreadySkipped) {
                skippedVersions.add(version);
            }
            
            // 使用正确的方式保存到UpDate节点下
            // 将JsonArray转换为List
            List<String> versionsList = new ArrayList<>();
            for (JsonElement elem : skippedVersions) {
                versionsList.add(elem.getAsString());
            }
            
            Map<String, Object> updateMap = new LinkedHashMap<>();
            updateMap.put(ConfigConstants.UPDATE_SKIPPED_VERSIONS, versionsList);
            
            if (debugMode) {
                System.out.println("准备保存跳过的版本: " + version);
                System.out.println("跳过版本列表: " + versionsList);
            }
            
            Constants.saveConfig(updateMap, ConfigConstants.UPDATE);
            
            // 清除缓存，确保下次读取最新配置
            Constants.cachedConfig = null;
            
            if (debugMode) System.out.println("已跳过版本，配置已保存: " + version);
            
        } catch (Exception e) {
            System.err.println("保存跳过版本失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示提示
     */
    private void showTip(String message) {
        showTip(message, null);
    }
    
    /**
     * 显示提示（带回调）
     * @param message 提示信息
     * @param onFinished 动画结束后的回调
     */
    private void showTip(String message, Runnable onFinished) {
        Platform.runLater(() -> {
            prompt.setText(message);
            playPromptAnimation(0.8, onFinished);
        });
    }
    
    /**
     * 重置UI
     */
    private void resetUI() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
        buttonBox.setVisible(true);
        buttonBox.setManaged(true);
        progressBar.setProgress(0);
        progressLabel.setText("");
        sizeLabel.setText("0 MB / 0 MB");
        speedLabel.setText("0 KB/s");
    }
    
    /**
     * 格式化文件大小
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化下载速度
     * @param bytesPerSecond 每秒字节数
     * @return 格式化后的速度字符串
     */
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 显示 promptPane 提示（统一方法，消除冗余）
     * @param type 提示类型
     * @param title 标题
     * @param content 内容
     */
    private void showPromptNotice(PromptNoticeType type, String title, String content) {
        showPromptNotice(type, title, content, true);
    }
    
    /**
     * 显示 promptPane 提示（可控制是否自动关闭）
     * @param type 提示类型
     * @param title 标题
     * @param content 内容
     * @param autoClose 是否自动关闭当前更新弹窗
     */
    private void showPromptNotice(PromptNoticeType type, String title, String content, boolean autoClose) {
        // 组合标题和内容作为提示消息
        String message = content;
        if (title != null && !title.isEmpty() && !title.equals(content)) {
            message = title + "\n" + content;
        }
        
        // 根据类型和自动关闭标志选择行为
        if (type == PromptNoticeType.INFO) {
            // 信息提示，延长显示时间，可选自动关闭
            if (autoClose) {
                showTipWithDelay(message, 1.5, this::closeDialog);
            } else {
                showTipWithDelay(message, 1.5, null);
            }
        } else if (type == PromptNoticeType.ERROR) {
            // 错误提示，显示更长时间，不自动关闭（让用户看清错误）
            showTipWithDelay(message, 3.0, null);
        } else if (type == PromptNoticeType.WARNING) {
            // 警告提示，中等延迟
            showTipWithDelay(message, 2.0, null);
        } else {
            // 其他类型，使用默认显示
            showTip(message);
        }
    }
    
    /**
     * 显示提示（带自定义延迟时间）
     * @param message 提示信息
     * @param delaySeconds 延迟秒数
     * @param onFinished 动画结束后的回调
     */
    private void showTipWithDelay(String message, double delaySeconds, Runnable onFinished) {
        Platform.runLater(() -> {
            prompt.setText(message);
            playPromptAnimation(delaySeconds, onFinished);
        });
    }

    private void playPromptAnimation(double delaySeconds, Runnable onFinished) {
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
        promptFadeOut.setDelay(Duration.seconds(delaySeconds));

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();

        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) an.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void exitAction() {
        closeDialog();
    }
    
    @FXML
    private void topBarDraggedAction(MouseEvent event) {
        Window window = topBar.getScene().getWindow();
        window.setX(event.getScreenX() - offsetX);
        window.setY(event.getScreenY() - offsetY);
    }
    
    @FXML
    private void topBarPressedAction(MouseEvent event) {
        offsetX = event.getSceneX();
        offsetY = event.getSceneY();
    }
}
