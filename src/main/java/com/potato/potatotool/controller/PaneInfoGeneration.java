package com.potato.potatotool.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import static com.potato.potatotool.content.blueTeam.infoGeneration.*;

/**
 * @author Potato
 * @date 2023/3/21 17:16
 */
public class PaneInfoGeneration {

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
