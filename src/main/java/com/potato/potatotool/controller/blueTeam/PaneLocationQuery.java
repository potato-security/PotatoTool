package com.potato.potatotool.controller.blueTeam;

import com.google.gson.JsonObject;
import com.opencsv.CSVWriter;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.blueTeam.IdCardInfo.*;
import static com.potato.potatotool.content.blueTeam.BankCardInfo.*;
import static com.potato.potatotool.content.blueTeam.PhoneToRegionUtil.*;

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
    private TextField bankCardTF_position;

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
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    public void getIdCardInfoFx(ActionEvent event) {
        String input = idCardInput.getText();
        if(input.isEmpty()){
            idCardTF_born.setText(I18nUtils.getString("location.invalid.idcard"));
            idCardTF_sex.setText("");
            idCardTF_att.setText("");
            return;
        }
        idCardTF_born.setText(I18nUtils.getString("location.querying"));

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
                    idCardTF_born.setText(I18nUtils.getString("location.network.error"));
                    idCardTF_sex.setText("");
                    idCardTF_att.setText("");
                }
                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
            Platform.runLater(() -> {
                idCardTF_born.setText(I18nUtils.getString("common.task.failed", detail));
                idCardTF_sex.setText("");
                idCardTF_att.setText("");
            });
        });

        // 启动任务
        Thread idCardThread = new Thread(task);
        idCardThread.setDaemon(true);
        idCardThread.start();

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
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        idCardInput.setText(I18nUtils.getString("location.batch.querying"));

        String finalPath = path;
        String csvFile = StrUtils.getCurrentJarDir() + File.separator + "Location" + File.separator +"idCard.csv";
        Path outputDirPath = Paths.get(csvFile).getParent();
        if (!Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] header = {"idCard", "born", "sex", "att"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
                     CSVWriter writer = new CSVWriter(osw)) {
                    osw.write('\ufeff');

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
                                    idCardInput.setText(I18nUtils.getString("location.batch.progress", finalLineNumber, totalLines));
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
                    idCardInput.setText(I18nUtils.getString("location.batch.complete", csvFile));
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
            Platform.runLater(() -> idCardInput.setText(I18nUtils.getString("common.task.failed", detail)));
        });

        // 启动任务
        Thread idCardBatchThread = new Thread(task);
        idCardBatchThread.setDaemon(true);
        idCardBatchThread.start();

    }

    @FXML
    public void getBankCardInfoFx(ActionEvent event) {
        bankCardTF_type.setText("");
        bankCardTF_bank.setText("");
        bankCardTF_position.setText("");

        String input = bankCardInput.getText();
        if (input.isEmpty()) {
            bankCardTF_type.setText(I18nUtils.getString("location.invalid.bankcard"));
            bankCardTF_bank.setText("");
            return;
        }
        bankCardTF_bank.setText(I18nUtils.getString("location.querying"));

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject str = getBankCardInfo(input);

                if (str!=null && (str.has("cardType") || str.has("bank") || str.has("position"))) {
                    if(str.has("cardType")) bankCardTF_type.setText(str.get("cardType").getAsString());
                    if(str.has("bank")) bankCardTF_bank.setText(str.get("bank").getAsString());
                    if(str.has("position")) bankCardTF_position.setText(str.get("position").getAsString());
                } else {
                    bankCardTF_bank.setText(I18nUtils.getString("location.bankcard.error"));
                    bankCardTF_type.setText("");
                    bankCardTF_position.setText("");
                }
                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
            Platform.runLater(() -> {
                bankCardTF_bank.setText(I18nUtils.getString("common.task.failed", detail));
                bankCardTF_type.setText("");
                bankCardTF_position.setText("");
            });
        });

        // 启动任务
        Thread bankCardThread = new Thread(task);
        bankCardThread.setDaemon(true);
        bankCardThread.start();

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
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        bankCardInput.setText(I18nUtils.getString("location.batch.querying"));

        String finalPath = path;
        String csvFile = StrUtils.getCurrentJarDir() + File.separator + "Location" + File.separator +"bankCard.csv";
        Path outputDirPath = Paths.get(csvFile).getParent();
        if (!Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] header = {"bankCard", "cardType", "bank", "position"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
                     CSVWriter writer = new CSVWriter(osw)) {
                    osw.write('\ufeff');

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
                                if(str!=null && (str.has("cardType") || str.has("bank") || str.has("position"))) {
                                    data[1] =(str.has("cardType"))? str.get("cardType").getAsString():"-";
                                    data[2] =(str.has("bank"))? str.get("bank").getAsString():"-";
                                    data[2] =(str.has("position"))? str.get("position").getAsString() : "-";
                                }

                                writer.writeNext(data);
                                Platform.runLater(() -> {
                                    bankCardInput.setText(I18nUtils.getString("location.batch.progress", finalLineNumber, totalLines));
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
                    bankCardInput.setText(I18nUtils.getString("location.batch.complete", csvFile));
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
            Platform.runLater(() -> bankCardInput.setText(I18nUtils.getString("common.task.failed", detail)));
        });

        // 启动任务
        Thread bankCardBatchThread = new Thread(task);
        bankCardBatchThread.setDaemon(true);
        bankCardBatchThread.start();
    }

    @FXML
    public void getPhoneInfoFx(ActionEvent event) {
        String[] phoneList = {phoneInput.getText()};
        if(phoneInput.getText().isEmpty()){
            phoneTF_Province.setText(I18nUtils.getString("location.invalid.phone"));
            phoneTF_City.setText("");
            phoneTF_Operator.setText("");
            phoneTF_AreaCode.setText("");
            phoneTF_PostalCode.setText("");
            return;
        }
        JsonObject result = getPhoneInfo(phoneList).get(0).getAsJsonObject();

        phoneTF_Province.setText( (result.get("省份").getAsString().isEmpty())?  I18nUtils.getString("location.invalid.phone"):result.get("省份").getAsString() );
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
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        phoneInput.setText(I18nUtils.getString("location.batch.querying"));

        String finalPath = path;
        String csvFile = StrUtils.getCurrentJarDir() + File.separator + "Location" + File.separator +"phoneInfo.csv";
        Path outputDirPath = Paths.get(csvFile).getParent();
        if (!Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] header = {"phone", "Province", "City", "Operator" , "AreaCode", "PostalCode"};
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                long totalLines = Files.lines(Paths.get(finalPath)).count();

                try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
                     CSVWriter writer = new CSVWriter(osw)) {
                    osw.write('\ufeff');

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
                                    phoneInput.setText(I18nUtils.getString("location.batch.progress", finalLineNumber, totalLines));
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
                    phoneInput.setText(I18nUtils.getString("location.batch.complete", csvFile));
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
            Platform.runLater(() -> phoneInput.setText(I18nUtils.getString("common.task.failed", detail)));
        });

        // 启动任务
        Thread phoneBatchThread = new Thread(task);
        phoneBatchThread.setDaemon(true);
        phoneBatchThread.start();
    }

}
