package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneSeparateDecode {
    private static final String SEPARATE_ENCODE_PREVIEW_INPUT = "cmd /c whoami && ipconfig /all";
    private static final String SEPARATE_DECODE_PREVIEW_INPUT = "Y21kIC9jIHdob2FtaSAmJiBpcGNvbmZpZyAvYWxs";
    private static final String SEPARATE_CHECKED_PREVIEW_INPUT = "%63%6d%64%20%2f%63%20%77%68%6f%61%6d%69";

    @FXML
    private StackPane sPane;

    @FXML
    private TextArea inputText;

    @FXML
    private CodeArea result;
    @FXML
    private VirtualizedScrollPane virScrollPane;

    @FXML
    private ToggleGroup checkboxGroup;

    @FXML
    private Label modeHint;
    @FXML
    private Label statusLabel;

    private final Map<String, Function<String, String>> decodeMap = new HashMap<>();
    private final Map<String, Function<String, String>> encodeMap = new HashMap<>();
    private final Map<String, String> hintKeyMap = new HashMap<>();

    public void initialize() {

        //  CodeArea添加宽高自适应
        sPane.widthProperty().addListener((obs, oldValue, newValue) -> {
            double prefWidth = newValue.doubleValue();
            virScrollPane.setMaxWidth(prefWidth);   //setMinWidth存在bug
        });
        sPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            double prefHeight = newValue.doubleValue() * 0.4 - 20;
            virScrollPane.setMinHeight(prefHeight);
        });

        //  CodeArea添加右键菜单
        result.setContextMenu(new DefaultContextMenu());

        decodeMap.put("Base64", new StrUtils()::base64Decode);
        decodeMap.put("URL", StrUtils::urlDecode);
        decodeMap.put("Rot13", StrUtils::ROT13Decode);
        decodeMap.put("Unicode", StrUtils::decodeUnicode);
        decodeMap.put("Chr", StrUtils::chrFuncDecode);
        decodeMap.put("strRev", StrUtils::strRev);
        decodeMap.put("Hex", new StrUtils()::hexDecode);
        decodeMap.put("Html", StrUtils::htmlDecode);

        encodeMap.put("Base64", StrUtils::base64Encode);
        encodeMap.put("URL", StrUtils::urlEncode);
        encodeMap.put("Rot13", StrUtils::ROT13Encode);
        encodeMap.put("Unicode", StrUtils::toUnicode);
        encodeMap.put("Chr", StrUtils::chrEncode);
        encodeMap.put("strRev", StrUtils::strRev);
        encodeMap.put("Hex", StrUtils::hexEncode);
        encodeMap.put("Html", StrUtils::htmlEncode);

        // 编码方式说明 (随选中动态更新)
        hintKeyMap.put("Base64", "separate.hint.base64");
        hintKeyMap.put("URL", "separate.hint.url");
        hintKeyMap.put("Unicode", "separate.hint.unicode");
        hintKeyMap.put("Hex", "separate.hint.hex");
        hintKeyMap.put("Html", "separate.hint.html");
        hintKeyMap.put("Rot13", "separate.hint.rot13");
        hintKeyMap.put("Chr", "separate.hint.chr");
        hintKeyMap.put("strRev", "separate.hint.strrev");

        // 选中编码方式时更新说明条
        checkboxGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> updateModeHint());

        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            // 默认选中 Base64, 同步说明条
            if (checkboxGroup.getSelectedToggle() == null) {
                selectMode("Base64");
            }
            updateModeHint();
            if (statusLabel != null) statusLabel.setText(I18nUtils.getString("separate.status.ready"));
            applyStartupPreviewState();
        });
    }

    private void updateModeHint() {
        if (modeHint == null) return;
        Toggle sel = checkboxGroup.getSelectedToggle();
        if (sel instanceof ToggleButton) {
            String mode = ((ToggleButton) sel).getText();
            String key = hintKeyMap.get(mode);
            if (key != null) modeHint.setText(I18nUtils.getString(key));
        }
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage != ToStart.StartupPage.BLUE_SEPARATE_DECODE_CHECKED
                && startupPage != ToStart.StartupPage.BLUE_SEPARATE_DECODE_ENCODE_RESULT
                && startupPage != ToStart.StartupPage.BLUE_SEPARATE_DECODE_DECODE_RESULT) {
            return;
        }

        if (startupPage == ToStart.StartupPage.BLUE_SEPARATE_DECODE_CHECKED) {
            selectMode("URL");
            inputText.setText(SEPARATE_CHECKED_PREVIEW_INPUT);
        } else if (startupPage == ToStart.StartupPage.BLUE_SEPARATE_DECODE_ENCODE_RESULT) {
            selectMode("Base64");
            inputText.setText(SEPARATE_ENCODE_PREVIEW_INPUT);
            toEncode(null);
        } else {
            selectMode("Base64");
            inputText.setText(SEPARATE_DECODE_PREVIEW_INPUT);
            toDecode(null);
        }
    }

    private void selectMode(String modeText) {
        for (Toggle toggle : checkboxGroup.getToggles()) {
            if (toggle instanceof ToggleButton) {
                ToggleButton btn = (ToggleButton) toggle;
                if (modeText.equals(btn.getText())) {
                    checkboxGroup.selectToggle(btn);
                    return;
                }
            }
        }
    }

    @FXML
    void toDecode(ActionEvent e){
        if(!checkContent()) return;

        result.clear();
        String content = inputText.getText();
        String mode = ((ToggleButton) checkboxGroup.getSelectedToggle()).getText();

        executeTask(decodeMap.get(mode), content, mode, "解密");

    }

    @FXML
    void toEncode(ActionEvent e){
        if(!checkContent()) return;

        result.clear();
        String content = inputText.getText();
        String mode = ((ToggleButton) checkboxGroup.getSelectedToggle()).getText();

        executeTask(encodeMap.get(mode), content, mode, "加密");

    }

    private void executeTask(Function<String, String> function, String content, String mode, String operation) {
        if (function == null) {
            result.replaceText(I18nUtils.getString("separate.error.notmode", mode, operation));
            return;
        }

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                return function.apply(content);
            }

            @Override
            protected void succeeded() {
                try {
                    result.replaceText((String) getValue());
                    if (statusLabel != null) {
                        statusLabel.setText(I18nUtils.getString(
                                "解密".equals(operation) ? "separate.status.decoded" : "separate.status.encoded"));
                    }
                }catch (Exception e){
                    result.replaceText(I18nUtils.getString("separate.error.notmode", mode, operation));
                }
            }

            @Override
            protected void failed() {
                result.replaceText(I18nUtils.getString("separate.error.failed"));
            }
        };

        Thread separateDecodeThread = new Thread(task);
        separateDecodeThread.setDaemon(true);
        separateDecodeThread.start();
    }

    // 检查内容及模式
    boolean checkContent(){

        String mode = null;
        try {
            mode = ((ToggleButton) checkboxGroup.getSelectedToggle()).getText();
        }catch (Exception e){
            result.replaceText(I18nUtils.getString("separate.error.selectmode"));
            return false;
        }
        String Content = inputText.getText();

        if (mode == null) {
            result.replaceText(I18nUtils.getString("separate.error.selectmode"));
            return false;
        }else if(Content.length() < 1){
            result.replaceText(I18nUtils.getString("separate.error.inputtext"));
            return false;
        }
        return true;
    }

}
