package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import static com.potato.potatotool.content.redTeam.InfoGeneration.*;

/**
 * @author Potato
 * @date 2023/3/21 17:16
 */
public class PaneInfoGeneration {
    
    @FXML
    private StackPane sPane;

    @FXML
    private TextField nameTF;

    @FXML
    private TextField nameToPinYinTF;

    @FXML
    private TextField phoneTF;

    @FXML
    private TextField emailTF;

    @FXML
    private TextField addressTF;

    @FXML
    private TextField idCardTF;

    @FXML
    private TextField bankCardTF;

    @FXML
    private TextField postalCodeTF;

    @FXML
    private TextField unifiedSocialCreditCodeTF;

    @FXML
    private TextField organizationCodeTF;
    
    @FXML
    void initialize() {
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    public void generation(ActionEvent event){
        String name = name();
        nameTF.setText(name);

        String nameToPinYin = nameToPinYin(name);
        nameToPinYinTF.setText(nameToPinYin);

        String phone = phone();
        phoneTF.setText(phone);

        String email = email(nameToPinYin);
        emailTF.setText(email);

        String address = address();
        addressTF.setText(address);

        String idCard = idCard();
        idCardTF.setText(idCard);

        String bankCard = bankCard();
        bankCardTF.setText(bankCard);

        String postalCode = postalCode();
        postalCodeTF.setText(postalCode);

        String unifiedSocialCreditCode = unifiedSocialCreditCode();
        unifiedSocialCreditCodeTF.setText(unifiedSocialCreditCode);

        String organizationCode = organizationCode();
        organizationCodeTF.setText(organizationCode);

    }

}
