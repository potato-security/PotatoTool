package com.potato.potatotool.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.opencsv.CSVWriter;
import com.potato.potatotool.utils.decompileUtils;
import com.potato.potatotool.utils.strUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        if(input.isEmpty()){
            idCardTF_born.setText("请输入正确的身份证号");
            idCardTF_sex.setText("");
            idCardTF_att.setText("");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject str = getIdCardInfo(input);

                if (str!=null && str.has("born") && str.has("sex") && str.has("att")) {
                    if (!str.get("born").getAsString().isEmpty()) {
                        idCardTF_born.setText(str.get("born").getAsString());
                        idCardTF_sex.setText(str.get("sex").getAsString());
                        idCardTF_att.setText(str.get("att").getAsString());
                    } else {
                        idCardTF_born.setText(str.get("msg").getAsString());
                        idCardTF_sex.setText("");
                        idCardTF_att.setText("");
                    }
                } else {
                    idCardTF_born.setText("网络存在问题，请查看命令窗口debug日志");
                    idCardTF_sex.setText("");
                    idCardTF_att.setText("");
                }
                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();

    }

    @FXML
    public void getIdCardInfoFx_batch(ActionEvent event) {
        idCardInput.clear();

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("TXT文件", "*.txt");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            System.out.println("没有文件被选择");
            return;
        }

        idCardInput.setText("批量查询中，请稍等……");

        String finalPath = path;
        String csvFile = strUtils.getCurrentJarDir() + File.separator +"idCard.csv";
        String[] header = {"idCard", "born", "sex", "att"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
                    writer.writeNext(header);
                    try (BufferedReader br = new BufferedReader(new FileReader(finalPath))) {
                        String line;
                        long lineNumber = 1;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                JsonObject str = getIdCardInfo(line);
                                long finalLineNumber = lineNumber;

                                String[] data = {line,"","",""};
                                if(str!=null && str.has("born") && str.has("sex") && str.has("att")) {
                                    data[1] = str.get("born").getAsString();
                                    data[2] = str.get("sex").getAsString();
                                    data[3] = str.get("att").getAsString();
                                }

                                writer.writeNext(data);
                                Platform.runLater(() -> {
                                    idCardInput.setText("批量查询中-" + finalLineNumber + "/" + totalLines);
                                });
                            }
                            lineNumber++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    idCardInput.setText("批量查询完毕-导出位置：" + csvFile);
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();

    }

    @FXML
    public void getBankCardInfoFx(ActionEvent event) {

        String input = bankCardInput.getText();
        if (input.isEmpty()) {
            bankCardTF_type.setText("请输入正确的银行卡号");
            bankCardTF_bank.setText("");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject str = getBankCardInfo(input);

                if (str!=null && str.has("cardType") && str.has("bank")) {
                    bankCardTF_type.setText(str.get("cardType").getAsString());
                    bankCardTF_bank.setText(str.get("bank").getAsString());
                } else {
                    bankCardTF_type.setText("银行卡号/网络存在问题，请查看命令窗口debug日志");
                    bankCardTF_bank.setText("");
                }
                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();

    }

    @FXML
    public  void getBankCardInfoFx_batch(ActionEvent event){
        bankCardInput.clear();

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("TXT文件", "*.txt");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            System.out.println("没有文件被选择");
            return;
        }

        bankCardInput.setText("批量查询中，请稍等……");

        String finalPath = path;
        String csvFile = strUtils.getCurrentJarDir() + File.separator +"bankCard.csv";
        String[] header = {"bankCard", "cardType", "bank"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
                    writer.writeNext(header);
                    try (BufferedReader br = new BufferedReader(new FileReader(finalPath))) {
                        String line;
                        long lineNumber = 1;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                JsonObject str = getBankCardInfo(line);
                                long finalLineNumber = lineNumber;

                                String[] data = {line,"",""};
                                if(str!=null && str.has("cardType") && str.has("bank")) {
                                    data[1] = str.get("cardType").getAsString();
                                    data[2] = str.get("bank").getAsString();
                                }

                                writer.writeNext(data);
                                Platform.runLater(() -> {
                                    bankCardInput.setText("批量查询中-" + finalLineNumber + "/" + totalLines);
                                });
                            }
                            lineNumber++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    bankCardInput.setText("批量查询完毕-导出位置：" + csvFile);
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();
    }

    @FXML
    public void getPhoneInfoFx(ActionEvent event) {
        String[] phoneList = {phoneInput.getText()};
        if(phoneInput.getText().isEmpty()){
            phoneTF_Province.setText("请输入正确的手机号");
            phoneTF_City.setText("");
            phoneTF_Operator.setText("");
            phoneTF_AreaCode.setText("");
            phoneTF_PostalCode.setText("");
            return;
        }
        JsonObject result = getPhoneInfo(phoneList).get(0).getAsJsonObject();

        phoneTF_Province.setText( (result.get("省份").getAsString().isEmpty())?  "请输入正确的手机号":result.get("省份").getAsString() );
        phoneTF_City.setText(result.get("城市").getAsString());
        phoneTF_Operator.setText(result.get("运营商").getAsString());
        phoneTF_AreaCode.setText(result.get("区号").getAsString());
        phoneTF_PostalCode.setText(result.get("邮编").getAsString());

    }

    @FXML
    public  void getPhoneInfoFx_batch(ActionEvent event){
        phoneInput.clear();

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("TXT文件", "*.txt");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            System.out.println("没有文件被选择");
            return;
        }

        phoneInput.setText("批量查询中，请稍等……");

        String finalPath = path;
        String csvFile = strUtils.getCurrentJarDir() + File.separator +"phoneInfo.csv";
        String[] header = {"phone", "Province", "City", "Operator" , "AreaCode", "PostalCode"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
                    writer.writeNext(header);
                    try (BufferedReader br = new BufferedReader(new FileReader(finalPath))) {
                        String line;
                        long lineNumber = 1;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                JsonObject str = getPhoneInfo(new String[]{line}).get(0).getAsJsonObject();
                                long finalLineNumber = lineNumber;

                                String[] data = {line, "", "", "", "", ""};
                                data[1] = str.get("省份").getAsString();
                                data[2] = str.get("城市").getAsString();
                                data[3] = str.get("运营商").getAsString();
                                data[4] = str.get("区号").getAsString();
                                data[5] = str.get("邮编").getAsString();

                                writer.writeNext(data);
                                Platform.runLater(() -> {
                                    phoneInput.setText("批量查询中-" + finalLineNumber + "/" + totalLines);
                                });
                            }
                            lineNumber++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    phoneInput.setText("批量查询完毕-导出位置：" + csvFile);
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();
    }

}
