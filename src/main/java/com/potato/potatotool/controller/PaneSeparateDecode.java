package com.potato.potatotool.controller;

import com.potato.potatotool.utils.DefaultContextMenu;
import com.potato.potatotool.utils.strUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

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

    }

    @FXML
    void toDecode(ActionEvent e){
        if(!checkContent()) return;

        String Content = inputText.getText();
        String mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();
        String res = null;

        if(mode.equals("Base64")){
            res = strUtils.base64Decode(Content);
        }else if(mode.equals("URL")){
            res = strUtils.urlDecode(Content);
        }else if(mode.equals("Rot13")){
            res = strUtils.ROT13Decode(Content);
        }else if(mode.equals("Unicode")){
            res = strUtils.decodeUnicode(Content);
        }else if(mode.equals("Chr")){
            res = strUtils.chrFuncDecode(Content);
        }else if(mode.equals("strRev")){
            res = strUtils.strRev(Content);
        }else if(mode.equals("Hex")){
            res = strUtils.hexDecode(Content);
        }else if(mode.equals("Html")){
            res = strUtils.htmlDecode(Content);
        }

        if(res == null) res = "该密文非" + mode + "加密模式";

        result.replaceText(res);

    }

    @FXML
    void toEncode(ActionEvent e){
        if(!checkContent()) return;

        String Content = inputText.getText();
        String mode = ((RadioButton) checkboxGroup.getSelectedToggle()).getText();
        String res = "";

        if(mode.equals("Base64")){
            res = strUtils.base64Encode(Content);
        }else if(mode.equals("URL")){
            res = strUtils.urlEncode(Content);
        }else if(mode.equals("Rot13")){
            res = strUtils.ROT13Encode(Content);
        }else if(mode.equals("Unicode")){
            res = strUtils.toUnicode(Content);
        }else if(mode.equals("Chr")){
            res = strUtils.chrEncode(Content);
        }else if(mode.equals("strRev")){
            res = strUtils.strRev(Content);
        }else if(mode.equals("Hex")){
            res = strUtils.hexEncode(Content);
        }else if(mode.equals("Html")){
            res = strUtils.htmlEncode(Content);
        }

        result.replaceText(res);

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
