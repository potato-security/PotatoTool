package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneSeparateDecode {

    @FXML private StackPane sPane;
    @FXML private CodeArea result;
    @FXML private VirtualizedScrollPane virScrollPane;
    @FXML private ToggleGroup ruleGroup;
    @FXML private TextArea inputText;
    @FXML private Label hintLabel;
    @FXML private Label statusLabel;
    @FXML private Button aiAnalyzeBtn;
    @FXML private Button downloadAiBtn;
    @FXML private Label aiSuccessBadge;
    @FXML private Label aiDisabledBadge;
    @FXML private Label aiHintLabel;
    @FXML private ScrollPane decodeScrollPane;
    @FXML private VBox decodePanelItems;
    @FXML private VBox decodePanelStates;
    private boolean decodePanelVisible = false;

    // hint keys for each tab index (0-7: Base64, URL, Rot13, Unicode, Chr, strRev, Hex, Html)
    private static final String[] HINT_KEYS = {
        "separate.hint.base64", "separate.hint.url", "separate.hint.rot13", "separate.hint.unicode",
        "separate.hint.chr", "separate.hint.strrev", "separate.hint.hex", "separate.hint.html"
    };

    private static final String[][] DECODE_TYPE_DATA = {
        {"Base64", "RFC 4648 标准编码"},
        {"URL", "Percent 百分号编码"},
        {"Rot13", "Caesar 位移13"},
        {"Unicode", "\\uXXXX 转义格式"},
        {"Chr", "Chr() 函数编码"},
        {"strRev", "字符串逆序"},
        {"Hex", "十六进制编码"},
        {"Html", "HTML 实体编码"},
    };

    private String lastContent = "";

    public void initialize() {
        sPane.widthProperty().addListener((obs, ov, nv) ->
                virScrollPane.setMaxWidth(nv.doubleValue()));
        sPane.heightProperty().addListener((obs, ov, nv) ->
                virScrollPane.setMinHeight(nv.doubleValue() * 0.4 - 20));

        result.setContextMenu(new DefaultContextMenu());

        ruleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                int idx = ruleGroup.getToggles().indexOf(newT);
                if (idx >= 0 && idx < HINT_KEYS.length) {
                    hintLabel.setText(I18nUtils.getString(HINT_KEYS[idx]));
                }
                buildDecodePanelItems(idx);
                resetOutput();
            }
        });

        // set initial hint and panel items
        hintLabel.setText(I18nUtils.getString(HINT_KEYS[0]));
        buildDecodePanelItems(0);

        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    void startDecrypt(ActionEvent e) {
        String text = inputText.getText();
        if (text == null || text.isEmpty()) {
            result.replaceText("// " + I18nUtils.getString("separate.error.inputtext"));
            return;
        }
        final String content = text.trim();
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return applyDecode(content);
            }
        };
        task.setOnSucceeded(ev -> {
            String res = task.getValue();
            result.replaceText(res);
            lastContent = res;
            setStatus(I18nUtils.getString("separate.status.decoded"));
            if (!res.startsWith("//")) {
                showAiHint();
                int idx = ruleGroup.getToggles().indexOf(ruleGroup.getSelectedToggle());
                slideInDecodePanel();
                refreshDecodePanelStates("separate.status.decoded", idx);
            }
        });
        task.setOnFailed(ev -> {
            Throwable err = task.getException();
            if (debugMode && err != null) err.printStackTrace();
            String msg = (err != null && err.getMessage() != null)
                    ? err.getMessage()
                    : I18nUtils.getString("separate.error.failed");
            Platform.runLater(() -> result.replaceText("// " + msg));
        });
        new Thread(task).start();
    }

    @FXML
    void startEncode(ActionEvent e) {
        String text = inputText.getText();
        if (text == null || text.isEmpty()) {
            result.replaceText("// " + I18nUtils.getString("separate.error.inputtext"));
            return;
        }
        final String content = text;
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return applyEncode(content);
            }
        };
        task.setOnSucceeded(ev -> {
            String res = task.getValue();
            result.replaceText(res);
            lastContent = res;
            setStatus(I18nUtils.getString("separate.status.encoded"));
            int idx = ruleGroup.getToggles().indexOf(ruleGroup.getSelectedToggle());
            slideInDecodePanel();
            refreshDecodePanelStates("separate.status.encoded", idx);
        });
        task.setOnFailed(ev -> {
            Throwable err = task.getException();
            if (debugMode && err != null) err.printStackTrace();
            Platform.runLater(() -> result.replaceText("// " + I18nUtils.getString("separate.error.failed")));
        });
        new Thread(task).start();
    }

    private String applyDecode(String content) {
        int idx = ruleGroup.getToggles().indexOf(ruleGroup.getSelectedToggle());
        try {
            switch (idx) {
                case 0: return new StrUtils().base64Decode(content);
                case 1: return StrUtils.urlDecode(content);
                case 2: return StrUtils.ROT13Decode(content);
                case 3: return StrUtils.decodeUnicode(content);
                case 4: return StrUtils.chrFuncDecode(content);
                case 5: return StrUtils.strRev(content);
                case 6: return new StrUtils().hexDecode(content);
                case 7: return StrUtils.htmlDecode(content);
                default: return content;
            }
        } catch (Exception e) {
            return "// " + I18nUtils.getString("separate.error.failed") + ": " + e.getMessage();
        }
    }

    private String applyEncode(String content) {
        int idx = ruleGroup.getToggles().indexOf(ruleGroup.getSelectedToggle());
        try {
            switch (idx) {
                case 0: return StrUtils.base64Encode(content);
                case 1: return StrUtils.urlEncode(content);
                case 2: return StrUtils.ROT13Encode(content);
                case 3: return StrUtils.toUnicodeUnify(content);
                case 4: return StrUtils.chrEncode(content);
                case 5: return StrUtils.strRev(content);
                case 6: return StrUtils.hexEncode(content);
                case 7: return StrUtils.htmlEncode(content);
                default: return content;
            }
        } catch (Exception e) {
            return "// " + I18nUtils.getString("separate.error.failed") + ": " + e.getMessage();
        }
    }

    @FXML
    void exportResult(ActionEvent e) {
        String content = result.getText();
        if (content == null || content.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                    return null;
                }
            };
            task.setOnFailed(ev -> {
                if (debugMode && task.getException() != null) task.getException().printStackTrace();
            });
            new Thread(task).start();
        }
    }

    @FXML
    void startAiAnalysis(ActionEvent e) {
        if (lastContent.isEmpty()) return;
        aiAnalyzeBtn.setDisable(true);
        hideAiHint();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(500);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            aiSuccessBadge.setVisible(true);
            aiAnalyzeBtn.setDisable(false);
            downloadAiBtn.setDisable(false);
            aiDisabledBadge.setVisible(false);
        });
        task.setOnFailed(ev -> Platform.runLater(() -> aiAnalyzeBtn.setDisable(false)));
        new Thread(task).start();
    }

    @FXML
    void downloadAi(ActionEvent e) {
        // stub
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showAiHint() {
        aiHintLabel.setVisible(true);
        aiHintLabel.setManaged(true);
    }

    private void hideAiHint() {
        aiHintLabel.setVisible(false);
        aiHintLabel.setManaged(false);
    }

    private void resetOutput() {
        result.clear();
        lastContent = "";
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        resetAiState();
    }

    private void resetAiState() {
        aiSuccessBadge.setVisible(false);
        aiDisabledBadge.setVisible(true);
        downloadAiBtn.setDisable(true);
        hideAiHint();
        lastContent = "";
    }

    private void buildDecodePanelItems(int selectedIdx) {
        if (decodePanelItems == null) return;
        decodePanelItems.getChildren().clear();
        for (int i = 0; i < DECODE_TYPE_DATA.length; i++) {
            VBox item = new VBox(2);
            item.getStyleClass().add("decode-panel-item");
            if (i == selectedIdx) item.getStyleClass().add("decode-panel-item-active");
            Label name = new Label(DECODE_TYPE_DATA[i][0]);
            name.getStyleClass().add("decode-panel-item-name");
            Label sub = new Label(DECODE_TYPE_DATA[i][1]);
            sub.getStyleClass().add("decode-panel-item-sub");
            item.getChildren().addAll(name, sub);
            decodePanelItems.getChildren().add(item);
        }
    }

    private void slideInDecodePanel() {
        if (decodeScrollPane == null || decodePanelVisible) return;
        decodePanelVisible = true;
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(decodeScrollPane.translateXProperty(), 210)),
            new KeyFrame(Duration.millis(200), new KeyValue(decodeScrollPane.translateXProperty(), 0))
        ).play();
    }

    private void refreshDecodePanelStates(String statusKey, int typeIdx) {
        if (decodePanelStates == null) return;
        decodePanelStates.getChildren().clear();
        addDecodeStateChip(I18nUtils.getString(statusKey), "decode-state-dot-success");
        int safeIdx = (typeIdx >= 0 && typeIdx < DECODE_TYPE_DATA.length) ? typeIdx : 0;
        addDecodeStateChip(DECODE_TYPE_DATA[safeIdx][0], "decode-state-dot-neutral");
    }

    private void addDecodeStateChip(String label, String dotStyle) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("decode-state-chip");
        Region dot = new Region();
        dot.getStyleClass().addAll("decode-state-dot", dotStyle);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("decode-state-label");
        chip.getChildren().addAll(dot, lbl);
        decodePanelStates.getChildren().add(chip);
    }
}
