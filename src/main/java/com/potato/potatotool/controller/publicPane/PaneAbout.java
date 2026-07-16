package com.potato.potatotool.controller.publicPane;

import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PaneAbout {

    @FXML private StackPane sPane;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox contentVBox;
    @FXML private ScrollPane contentsPane;
    @FXML private Label aboutStateVersion;

    @FXML private Label versionLabel;
    @FXML private Label buildLabel;
    @FXML private Label channelLabel;
    @FXML private Label channelInfoLabel;
    @FXML private Label buildInfoLabel;
    @FXML private Label updateStatusChip;
    @FXML private Label platformLabel;
    @FXML private Label jdkLabel;
    @FXML private Label dataPathLabel;
    @FXML private Button checkUpdateBtn;

    private boolean contentsPaneVisible = false;
    private final HostServices services = MainApplication.letGetHostServices();

    public void initialize() {
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            populateDynamicInfo();
            showContentsPanel();
        });
    }

    private void showContentsPanel() {
        // CONTENTS 面板改为常驻并排(对齐设计稿),始终可见,仅确保就位。
        if (contentsPane == null) return;
        contentsPaneVisible = true;
        contentsPane.setTranslateX(0);
    }

    private void populateDynamicInfo() {
        try {
            String configStr = Constants.getResourceString("config");
            if (configStr != null) {
                JsonObject config = new com.google.gson.Gson().fromJson(configStr, JsonObject.class);
                if (config != null && config.has(ConfigConstants.UPDATE_APP_VERSION)) {
                    String version = config.get(ConfigConstants.UPDATE_APP_VERSION).getAsString();
                    if (versionLabel != null) versionLabel.setText(version);
                    if (channelLabel != null) channelLabel.setText("stable");
                    if (channelInfoLabel != null) channelInfoLabel.setText("stable");
                    if (buildInfoLabel != null) buildInfoLabel.setText(version + " · build " + getBuildString());
                }
            }
        } catch (Exception ignored) {}

        String os = System.getProperty("os.name", "—") + " " + System.getProperty("os.arch", "");
        String jdk = System.getProperty("java.version", "—");
        String home = System.getProperty("user.home", "~");

        if (platformLabel != null) platformLabel.setText(os.trim());
        if (jdkLabel != null) jdkLabel.setText(jdk);
        if (dataPathLabel != null) dataPathLabel.setText(home + "/.potatotool");
        if (buildLabel != null) buildLabel.setText(getBuildString());
    }

    private String getBuildString() {
        try {
            String configStr = Constants.getResourceString("config");
            if (configStr != null) {
                JsonObject config = new com.google.gson.Gson().fromJson(configStr, JsonObject.class);
                if (config != null && config.has("buildDate")) {
                    return config.get("buildDate").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return "—";
    }

    public void startScrolling() {}
    public void pauseScrolling() {}

    @FXML void scrollToVersion(Event e) { if (mainScrollPane != null) mainScrollPane.setVvalue(0); }
    @FXML void scrollToChangelog(Event e) { if (mainScrollPane != null) mainScrollPane.setVvalue(0.3); }
    @FXML void scrollToFeedback(Event e) { if (mainScrollPane != null) mainScrollPane.setVvalue(0.7); }
    @FXML void scrollToLicense(Event e) { if (mainScrollPane != null) mainScrollPane.setVvalue(1.0); }
    @FXML void scrollToCredits(Event e) { if (mainScrollPane != null) mainScrollPane.setVvalue(1.0); }

    @FXML
    void checkUpdate() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool/releases");
    }

    @FXML
    void openGitHub() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool");
    }

    @FXML
    void openGitHubIssues() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool/issues");
    }

    @FXML
    void openDocs() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool/wiki");
    }

    @FXML
    void openQQGroup() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool");
    }

    @FXML
    void openTelegram() {
        services.showDocument("https://github.com/HotBoy-java/PotatoTool");
    }
}
