package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
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
    private static final String PREVIEW_NAME = "陈知远";
    private static final String PREVIEW_PINYIN = "chenzhiyuan";
    private static final String PREVIEW_PHONE = "中国电信-19973124568";
    private static final String PREVIEW_EMAIL = "chenzhiyuan.sec@proton.me";
    private static final String PREVIEW_ADDRESS = "浙江省杭州市西湖区文三路 478 号华星时代广场 A 座 1702";
    private static final String PREVIEW_ID_CARD = "33010619940817351X";
    private static final String PREVIEW_BANK_CARD = "6222021001123456789 | 中国工商银行";
    private static final String PREVIEW_POSTAL_CODE = "310012";
    private static final String PREVIEW_CREDIT_CODE = "91330106MA27XG019K";
    private static final String PREVIEW_ORG_CODE = "MA27XG019-K";
    
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
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        if (ToStart.getStartupTestPage() == ToStart.StartupPage.RED_INFO_GENERATION_RESULT) {
            nameTF.setText(PREVIEW_NAME);
            nameToPinYinTF.setText(PREVIEW_PINYIN);
            phoneTF.setText(PREVIEW_PHONE);
            emailTF.setText(PREVIEW_EMAIL);
            addressTF.setText(PREVIEW_ADDRESS);
            idCardTF.setText(PREVIEW_ID_CARD);
            bankCardTF.setText(PREVIEW_BANK_CARD);
            postalCodeTF.setText(PREVIEW_POSTAL_CODE);
            unifiedSocialCreditCodeTF.setText(PREVIEW_CREDIT_CODE);
            organizationCodeTF.setText(PREVIEW_ORG_CODE);
        }
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
