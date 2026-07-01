package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    @FXML private TextField customPath;
    @FXML private VBox modeSection;
    @FXML private ComboBox<String> modeComboBox;
    @FXML private Button aiAnalyzeBtn;
    @FXML private Button downloadAiBtn;
    @FXML private Label aiSuccessBadge;
    @FXML private Label aiDisabledBadge;
    @FXML private Label aiHintLabel;

    private String sampleContent = null;
    private String lastDecryptedContent = "";

    public void initialize() {
        sPane.widthProperty().addListener((obs, ov, nv) ->
                virScrollPane.setMaxWidth(nv.doubleValue()));
        sPane.heightProperty().addListener((obs, ov, nv) ->
                virScrollPane.setMinHeight(nv.doubleValue() * 0.4 - 20));

        result.setContextMenu(new DefaultContextMenu());

        modeComboBox.getItems().addAll("AES", "DES", "XOR", "ALL");
        modeComboBox.getSelectionModel().selectFirst();

        // 规则 2/3 时显示 modeSection
        ruleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                int idx = ruleGroup.getToggles().indexOf(newT);
                boolean needsMode = idx >= 2;
                modeSection.setManaged(needsMode);
                modeSection.setVisible(needsMode);
                resetState();
            }
        });

        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    void chooseFile(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            customPath.setText(file.getAbsolutePath());
            sampleContent = null;
            resetAiState();
        }
    }

    @FXML
    void startDecrypt(ActionEvent e) {
        String pathText = customPath.getText();
        if (pathText == null || pathText.isEmpty()) {
            result.replaceText("// " + I18nUtils.getString("separate.status.nofile"));
            return;
        }

        result.replaceText("// " + I18nUtils.getString("separate.status.decrypting"));

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String content;
                if (sampleContent != null) {
                    content = sampleContent;
                } else {
                    File f = new File(pathText);
                    content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                }
                return decrypt(content);
            }
        };

        task.setOnSucceeded(ev -> {
            String res = task.getValue();
            result.replaceText(res);
            lastDecryptedContent = res;
            if (!res.startsWith("//")) {
                aiHintLabel.setVisible(true);
            }
        });

        task.setOnFailed(ev -> {
            Throwable err = task.getException();
            if (debugMode && err != null) err.printStackTrace();
            String msg = (err != null && err.getMessage() != null)
                    ? err.getMessage()
                    : I18nUtils.getString("separate.status.error");
            Platform.runLater(() -> result.replaceText("// " + msg));
        });

        new Thread(task).start();
    }

    private String decrypt(String content) {
        int idx = ruleGroup.getToggles().indexOf(ruleGroup.getSelectedToggle());
        switch (idx) {
            case 0: return safeBase64Decode(content.trim());
            case 1: return safeHexDecode(content.trim());
            case 2: return decryptCrypto(content.trim());
            case 3: return autoDecrypt(content.trim());
            default: return content;
        }
    }

    private String safeBase64Decode(String s) {
        try { return new StrUtils().base64Decode(s); }
        catch (Exception e) { return "// Base64 解码失败: " + e.getMessage(); }
    }

    private String safeHexDecode(String s) {
        try { return new StrUtils().hexDecode(s); }
        catch (Exception e) { return "// Hex 解码失败: " + e.getMessage(); }
    }

    private String decryptCrypto(String content) {
        String mode = modeComboBox.getValue();
        if ("XOR".equals(mode)) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < bytes.length; i++) bytes[i] ^= 0x42;
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if ("ALL".equals(mode)) return autoDecrypt(content);
        return "// " + mode + " 解密需要提供密钥配置\n// 模式: " + mode + "  内容长度: " + content.length() + " 字节";
    }

    private String autoDecrypt(String content) {
        try {
            String b64 = new StrUtils().base64Decode(content);
            if (b64 != null && !b64.equals(content)) return "// 自动识别: Base64\n" + b64;
        } catch (Exception ignored) {}
        try {
            String hex = new StrUtils().hexDecode(content);
            if (hex != null && !hex.equals(content)) return "// 自动识别: Hex\n" + hex;
        } catch (Exception ignored) {}
        try {
            String url = StrUtils.urlDecode(content);
            if (!url.equals(content)) return "// 自动识别: URL 编码\n" + url;
        } catch (Exception ignored) {}
        return "// 自动识别: 未知编码，原始输出\n" + content;
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
            task.setOnSucceeded(ev -> result.replaceText(content + "\n// " + I18nUtils.getString("separate.export.saved")));
            task.setOnFailed(ev -> {
                if (debugMode && task.getException() != null) task.getException().printStackTrace();
            });
            new Thread(task).start();
        }
    }

    @FXML
    void startAiAnalysis(ActionEvent e) {
        if (lastDecryptedContent.isEmpty()) return;
        aiAnalyzeBtn.setDisable(true);
        aiHintLabel.setVisible(false);
        result.replaceText(lastDecryptedContent + "\n// " + I18nUtils.getString("separate.ai.analyzing"));
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
        // stub: AI download
    }

    @FXML
    void loadSampleAes(ActionEvent e) {
        sampleContent = "U2FsdGVkX1+v/5kpQzALsJ0bGYKrGRjZ5NsTv5w=";
        customPath.setText("< AES 样本 >");
        selectRuleByIndex(2);
        resetAiState();
    }

    @FXML
    void loadSampleBase64(ActionEvent e) {
        sampleContent = "Y21kIC9jIHdob2FtaSAmJiBpcGNvbmZpZyAvYWxs";
        customPath.setText("< Base64 样本 >");
        selectRuleByIndex(0);
        resetAiState();
    }

    @FXML
    void loadSampleMixed(ActionEvent e) {
        sampleContent = "%63%6d%64%20%2f%63%20%77%68%6f%61%6d%69";
        customPath.setText("< 混合加密样本 >");
        selectRuleByIndex(3);
        resetAiState();
    }

    @FXML
    void copyResult(ActionEvent e) {
        String content = result.getText();
        if (content == null || content.isEmpty()) return;
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(content);
        cb.setContent(cc);
    }

    private void selectRuleByIndex(int idx) {
        if (idx >= 0 && idx < ruleGroup.getToggles().size()) {
            ruleGroup.selectToggle(ruleGroup.getToggles().get(idx));
        }
    }

    private void resetState() {
        sampleContent = null;
        customPath.clear();
        result.clear();
        lastDecryptedContent = "";
        resetAiState();
    }

    private void resetAiState() {
        aiSuccessBadge.setVisible(false);
        aiDisabledBadge.setVisible(true);
        downloadAiBtn.setDisable(true);
        aiHintLabel.setVisible(false);
        lastDecryptedContent = "";
    }
}
