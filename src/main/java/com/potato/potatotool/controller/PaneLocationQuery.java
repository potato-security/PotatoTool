package com.potato.potatotool.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import static com.potato.potatotool.content.blueTeam.idCardInfo.*;
import static com.potato.potatotool.content.blueTeam.bankCardInfo.*;
import static com.potato.potatotool.content.blueTeam.phoneToRegionUtil.*;

/**
 * @author Potato
 * @date 2024/4/21 16:23
 */
public class PaneLocationQuery {

    @FXML
    private StackPane sPane;

    @FXML
    private TextField idCardInput;
    @FXML
    private TextField idCardTF_born;
    @FXML
    private TextField idCardTF_sex;
    @FXML
    private TextField idCardTF_att;

    @FXML
    private TextField bankCardInput;
    @FXML
    private TextField bankCardTF_type;
    @FXML
    private TextField bankCardTF_bank;

    @FXML
    private TextField phoneInput;
    @FXML
    private TextField phoneTF_Province;
    @FXML
    private TextField phoneTF_City;
    @FXML
    private TextField phoneTF_Operator;
    @FXML
    private TextField phoneTF_AreaCode;
    @FXML
    private TextField phoneTF_PostalCode;

    @FXML
    void initialize(){

    }

    @FXML
    public void getIdCardInfoFx(ActionEvent event) {
        String input = idCardInput.getText();
        JsonObject str= getIdCardInfo(input);

        if(str.has("born") && str.has("sex") && str.has("att")){
            if(!str.get("born").getAsString().isEmpty()){
                idCardTF_born.setText(str.get("born").getAsString());
                idCardTF_sex.setText(str.get("sex").getAsString());
                idCardTF_att.setText(str.get("att").getAsString());
            }else {
                idCardTF_born.setText(str.get("msg").getAsString());
                idCardTF_sex.setText("");
                idCardTF_att.setText("");
            }
        }else {
            idCardTF_born.setText("网络存在问题，请查看命令窗口debug日志");
            idCardTF_sex.setText("");
            idCardTF_att.setText("");
        }

    }

    @FXML
    public void getBankCardInfoFx(ActionEvent event) {
        String input = bankCardInput.getText();
        JsonObject str= getBankCardInfo(input);

        try {
            bankCardTF_type.setText(str.get("cardType").getAsString());
            bankCardTF_bank.setText(str.get("bank").getAsString());
        }catch (Exception e){
            bankCardTF_type.setText("银行卡号/网络存在问题，请查看命令窗口debug日志");
            bankCardTF_bank.setText("");
        }
    }

    @FXML
    public void getPhoneInfoFx(ActionEvent event) {
        String[] phoneList = {phoneInput.getText()};
        JsonObject result = getPhoneInfo(phoneList).get(0).getAsJsonObject();

        phoneTF_Province.setText(result.get("省份").getAsString());
        phoneTF_City.setText(result.get("城市").getAsString());
        phoneTF_Operator.setText(result.get("运营商").getAsString());
        phoneTF_AreaCode.setText(result.get("区号").getAsString());
        phoneTF_PostalCode.setText(result.get("邮编").getAsString());

    }

}
