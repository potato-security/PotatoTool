package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFCheckBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.update.ResourceUpdate;
import com.potato.potatotool.update.UpdateInfo;
import com.potato.potatotool.update.UpdateManager;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nManager;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.FadeTransition;

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
    
    @FXML
    private AnchorPane an;
    
    @FXML
    private BorderPane topBar;
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private VBox versionInfoBox;
    
    @FXML
    private Label versionInfoLabel;
    
    @FXML
    private VBox changelogBox;
    
    @FXML
    private TextArea changelogText;
    
    @FXML
    private VBox updateItemsBox;
    
    @FXML
    private VBox updateItemsContainer;
    
    // 更新项checkbox映射
    private Map<CFCheckBox, UpdateItem> updateItemsMap = new LinkedHashMap<>();
    
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
        // 动态设置版本信息标签
        updateVersionInfoLabel();
        
        updateItemsBox.setVisible(true);
        updateItemsBox.setManaged(true);
        updateItemsContainer.getChildren().clear();
        updateItemsMap.clear();
        
        // 添加软件更新checkbox
        if (updateInfo.isAppNeedUpdate()) {
            String localVersion = updateInfo.getLocalAppVersion();
            String remoteVersion = updateInfo.getAppVersion().getVersion();
            
            // 检查该版本是否已被跳过
            boolean isSkipped = isAppVersionSkipped(remoteVersion);

            CFCheckBox appCheckBox = new CFCheckBox();
            String appInfo = String.format("软件更新: %s → %s",
                    localVersion, remoteVersion);
            appCheckBox.setText(appInfo);
            appCheckBox.setSelected(!isSkipped); // 如果被跳过则默认不选中
            appCheckBox.setStyle("-fx-font-size: 13px;");

            UpdateItem appItem = new UpdateItem("app", null, null, remoteVersion);
            updateItemsMap.put(appCheckBox, appItem);
            updateItemsContainer.getChildren().add(appCheckBox);
            
            // 显示更新日志
            if (updateInfo.getAppVersion().getChangelog() != null && 
                !updateInfo.getAppVersion().getChangelog().isEmpty()) {
                changelogBox.setVisible(true);
                changelogBox.setManaged(true);
                
                StringBuilder changelog = new StringBuilder();
                for (String line : updateInfo.getAppVersion().getChangelog()) {
                    changelog.append("• ").append(line).append("\n");
                }
                changelogText.setText(changelog.toString());
            }
        }
        
        // 添加资源更新checkbox
        if (updateInfo.hasResourceUpdates()) {
            List<ResourceUpdate> updates = updateInfo.getResourceUpdates();
            for (ResourceUpdate update : updates) {
                // 检查该资源版本是否已被跳过
                boolean isSkipped = isResourceVersionSkipped(update.getResourceName(), update.getRemoteVersion());

                CFCheckBox resourceCheckBox = new CFCheckBox();
                String info = String.format("%s: %s → %s",
                        update.getDisplayName(),
                        update.getLocalVersion(),
                        update.getRemoteVersion()
                );
                resourceCheckBox.setText(info);
                resourceCheckBox.setSelected(!isSkipped); // 如果被跳过则默认不选中
                resourceCheckBox.setStyle("-fx-font-size: 13px;");
                
                UpdateItem resourceItem = new UpdateItem("resource", 
                        update.getResourceName(), update.getDisplayName(), 
                        update.getRemoteVersion());
                updateItemsMap.put(resourceCheckBox, resourceItem);
                updateItemsContainer.getChildren().add(resourceCheckBox);
            }
        }
    }
    
    /**
     * 动态更新版本信息标签
     */
    private void updateVersionInfoLabel() {
        StringBuilder info = new StringBuilder();
        
        // 如果有软件更新，显示软件版本信息
        if (updateInfo.isAppNeedUpdate()) {
            String localVersion = updateInfo.getLocalAppVersion();
            String remoteVersion = updateInfo.getAppVersion().getVersion();
            info.append(String.format("新版本 %s 可用，当前版本 %s", remoteVersion, localVersion));
        }
        
        // 如果有资源更新，添加资源更新信息
        if (updateInfo.hasResourceUpdates()) {
            if (info.length() > 0) {
                info.append("\n"); // 如果前面有软件更新信息，换行
            }
            int count = updateInfo.getResourceUpdates().size();
            info.append(String.format("发现 %d 个资源更新", count));
        }
        
        // 如果没有任何更新信息（理论上不会出现，因为有更新才会显示对话框）
        if (info.length() == 0) {
            info.append("发现可用更新");
        }
        
        versionInfoLabel.setText(info.toString());
    }
    
    /**
     * 更新项数据类
     */
    private static class UpdateItem {
        String type; // "app" or "resource"
        String resourceName; // 资源名称（如"md5"）
        String displayName; // 显示名称
        String version; // 版本号
        
        UpdateItem(String type, String resourceName, String displayName, String version) {
            this.type = type;
            this.resourceName = resourceName;
            this.displayName = displayName;
            this.version = version;
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
        if (hasSelectedApp) {
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
                    showAlert(Alert.AlertType.INFORMATION, 
                            i18n.getString("update.install.title"), 
                            message != null ? message : i18n.getString("update.install.app.success"));
                    // showAlert内部会自动关闭
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
                    showAlert(Alert.AlertType.ERROR, 
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
                        // 资源更新完成提示，1.5秒后自动关闭
                        showAlert(Alert.AlertType.INFORMATION,
                                i18n.getString("update.install.title"),
                                i18n.getString("update.install.resource.success"));
                        // showAlert内部会自动关闭
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
                        showAlert(Alert.AlertType.ERROR,
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
                        showAlert(Alert.AlertType.INFORMATION,
                                i18n.getString("update.install.title"),
                                i18n.getString("update.install.resource.success"));
                        // showAlert内部会自动关闭
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
                        showAlert(Alert.AlertType.ERROR,
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
            showAlert(Alert.AlertType.ERROR,
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
            // 设置提示文本
            prompt.setText(message);
            
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
            fadeOut.setDelay(Duration.seconds(0.8)); // 延迟0.8秒执行渐出动画

            // 播放渐入动画，完成后播放渐出动画
            fadeIn.setOnFinished(event -> fadeOut.play());
            fadeIn.play();

            fadeOut.setOnFinished(event -> {
                promptPane.setVisible(false);
                promptPane.setManaged(false);
                // 如果有回调，执行回调
                if (onFinished != null) {
                    onFinished.run();
                }
            });
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
     * 显示提示框（统一方法，消除冗余）
     * @param type 提示类型
     * @param title 标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        showAlert(type, title, content, true);
    }
    
    /**
     * 显示提示框（可控制是否自动关闭）
     * @param type 提示类型
     * @param title 标题
     * @param content 内容
     * @param autoClose 是否自动关闭对话框
     */
    private void showAlert(Alert.AlertType type, String title, String content, boolean autoClose) {
        // 组合标题和内容作为提示消息
        String message = content;
        if (title != null && !title.isEmpty() && !title.equals(content)) {
            message = title + "\n" + content;
        }
        
        // 根据类型和自动关闭标志选择行为
        if (type == Alert.AlertType.INFORMATION) {
            // 信息提示，延长显示时间，可选自动关闭
            if (autoClose) {
                showTipWithDelay(message, 1.5, this::closeDialog);
            } else {
                showTipWithDelay(message, 1.5, null);
            }
        } else if (type == Alert.AlertType.ERROR) {
            // 错误提示，显示更长时间，不自动关闭（让用户看清错误）
            showTipWithDelay(message, 3.0, null);
        } else if (type == Alert.AlertType.WARNING) {
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
            // 设置提示文本
            prompt.setText(message);
            
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
            fadeOut.setDelay(Duration.seconds(delaySeconds)); // 使用自定义延迟时间

            // 播放渐入动画，完成后播放渐出动画
            fadeIn.setOnFinished(event -> fadeOut.play());
            fadeIn.play();

            fadeOut.setOnFinished(event -> {
                promptPane.setVisible(false);
                promptPane.setManaged(false);
                // 如果有回调，执行回调
                if (onFinished != null) {
                    onFinished.run();
                }
            });
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

