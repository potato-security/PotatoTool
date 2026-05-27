package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
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

    private final Map<String, Function<String, String>> decodeMap = new HashMap<>();
    private final Map<String, Function<String, String>> encodeMap = new HashMap<>();

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
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    void toDecode(ActionEvent e){
        if(!checkContent()) return;

        result.clear();
        String content = inputText.getText();
        String mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();

        executeTask(decodeMap.get(mode), content, mode, "解密");

    }

    @FXML
    void toEncode(ActionEvent e){
        if(!checkContent()) return;

        result.clear();
        String content = inputText.getText();
        String mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();

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
            mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();
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
