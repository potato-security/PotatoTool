package com.potato.potatotool.controller;

import com.potato.potatotool.utils.DefaultContextMenu;
import com.potato.potatotool.utils.strUtils;
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

        decodeMap.put("Base64", new strUtils()::base64Decode);
        decodeMap.put("URL", strUtils::urlDecode);
        decodeMap.put("Rot13", strUtils::ROT13Decode);
        decodeMap.put("Unicode", strUtils::decodeUnicode);
        decodeMap.put("Chr", strUtils::chrFuncDecode);
        decodeMap.put("strRev", strUtils::strRev);
        decodeMap.put("Hex", new strUtils()::hexDecode);
        decodeMap.put("Html", strUtils::htmlDecode);

        encodeMap.put("Base64", strUtils::base64Encode);
        encodeMap.put("URL", strUtils::urlEncode);
        encodeMap.put("Rot13", strUtils::ROT13Encode);
        encodeMap.put("Unicode", strUtils::toUnicode);
        encodeMap.put("Chr", strUtils::chrEncode);
        encodeMap.put("strRev", strUtils::strRev);
        encodeMap.put("Hex", strUtils::hexEncode);
        encodeMap.put("Html", strUtils::htmlEncode);
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
            result.replaceText("该密文非" + mode + operation + "模式");
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
                    result.replaceText("该密文非" + mode + operation + "模式");
                }
            }

            @Override
            protected void failed() {
                result.replaceText("处理失败，请重试");
            }
        };

        new Thread(task).start();
    }

    // 检查内容及模式
    boolean checkContent(){

        String mode = null;
        try {
            mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();
        }catch (Exception e){
            result.replaceText("请在上方选择加密/解密模式");
            return false;
        }
        String Content = inputText.getText();

        if (mode == null) {
            result.replaceText("请在上方选择加密/解密模式");
            return false;
        }else if(Content.length() < 1){
            result.replaceText("请在上方输入明文/密文");
            return false;
        }
        return true;
    }

}
