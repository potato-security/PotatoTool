package com.potato.potatotool.controller.blueTeam;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.controller.MainController.clipRect;
import static com.potato.potatotool.content.blueTeam.BlockchainTraceability.*;
import static com.potato.potatotool.utils.core.Constants.getConfigInfo;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneBlockchain {

    @FXML
    private StackPane sPane;

    @FXML
    private TextField question;

    @FXML
    private VBox vBoxBar;

    public static String blockUrl = getConfigInfo("blockchainUrl");

    private static final ObservableList<Node> contentObj = FXCollections.observableArrayList();
    private static final ObservableList<Node> detailedContentObj = FXCollections.observableArrayList();

    @FXML
    private ListView listViewCell;
    @FXML
    private Label backClearLabel;

    @FXML
    private ListView detailedListView;

    @FXML
    private Pane detailedPane;

    @FXML
    private Accordion accordionPane;

    public void initialize() {

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initTextData();

        listenSearch();
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    // 初始化样本数据
    private void initTextData() {
        ArrayList<String> testDataList = new ArrayList<>();
        testDataList.add("TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi");
        testDataList.add("18245784");
        testDataList.add("0x038ec2f5565bdfebab754cf6f8f94c5116313ebbe8b1197e87cef257525b8d97");
        testDataList.add("比特币");
        testDataList.add("TRX");

        for(int i = 0 ; i < testDataList.size() ; i ++){
            String testData = testDataList.get(i);
            final int index = i + 1;

            RXLineButton rxLineButton = new RXLineButton();
            rxLineButton.setOnMouseClicked(event -> writeTestData(testData));
            rxLineButton.setPrefWidth(100);
            rxLineButton.setPrefHeight(40);
            rxLineButton.setSpacing(3);

            // 使用便捷方法绑定国际化，支持动态语言切换
            I18nUtils.bindTextWithSuffix(rxLineButton, "app.sample.data", String.valueOf(index));

            VBox vBoxConent = new VBox();
            vBoxConent.setPrefWidth(sPane.getWidth() - 280);
            vBoxConent.getStyleClass().add("cellVBox");
            vBoxConent.setAlignment(Pos.CENTER);
            vBoxConent.setCursor(Cursor.HAND);
            vBoxConent.getChildren().add(rxLineButton);

            contentObj.add(vBoxConent);
        }
        listViewCell.setItems(contentObj);
    }

    void writeTestData(String data) {
        question.setText(data);
    }


    //  监听输入时回车
    private void listenSearch() {
        question.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                searchInput();
            }
        });
    }


    @FXML
    void searchInput(){
        accordionPane.getPanes().clear();
        detailedPane.setVisible(false);
        listViewCell.getItems().clear();
        detailedListView.getItems().clear();
        contentObj.clear();
        detailedContentObj.clear();

        String input = question.getText();
        if( input.isEmpty() || input.length() < 1 ) {
            initTextData();
            backClearLabel.setVisible(false);
            return;
        }
        backClearLabel.setVisible(true);
        Label tips = new Label(I18nUtils.getString("blockchain.querying", input));
        tips.setAlignment(Pos.CENTER);
        tips.setId("tipTitle");
        tips.setPrefWidth(sPane.getWidth() - 50);
        tips.setMinWidth(sPane.getWidth() - 50);
        tips.setMaxWidth(sPane.getWidth() - 50);
        contentObj.add(tips);
        listViewCell.setItems(contentObj);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject searchData = search(input);
                if(!searchData.has("data")){
                    return null;
                }

                JsonArray dataJsonArray = (JsonArray) searchData.get("data");
                Platform.runLater(() -> {
                    contentObj.clear();
                });
                for (int i = 0; i < dataJsonArray.size(); i++) {
                    JsonObject jsonObject = (JsonObject) dataJsonArray.get(i);

                    String type = jsonObject.get("type").getAsString();

                    if(jsonObject.has("indexName")){

                        String indexName = jsonObject.get("indexName").getAsString();
                        String icon = jsonObject.get("icon").getAsString();
                        String coinFullNameCn = jsonObject.has("coinFullNameCn")? jsonObject.get("coinFullNameCn").getAsString() : "";
                        String coinFullName = !coinFullNameCn.equals("") ? coinFullNameCn + " - " + jsonObject.get("coinFullName").getAsString() : jsonObject.get("coinFullName").getAsString();
                        String summaryCn = jsonObject.get("summaryCn").getAsString();
                        String summary = !summaryCn.equals("") ? summaryCn : jsonObject.get("summaryEn").getAsString();
                        String wpCn = jsonObject.get("wpCn").getAsString();
                        String wp =  !wpCn.equals("") ? wpCn : jsonObject.get("wpEn").getAsString();

                        summary = summary.replace("<b>","").replace("</b>","");
                        wp = wp.replace("<b>","").replace("</b>","");

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = new HBox();
                        hBox.getStyleClass().add("cellHBox");
                        hBox.setAlignment(Pos.CENTER);

                        Image image = new Image(blockUrl + "/icon/" + icon + ".png", true);
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(35.0);
                        imageView.setFitWidth(35.0);
                        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
                        Rectangle clip = clipRect(
                                imageView, arcProperty
                        );
                        imageView.setClip(clip);
                        imageView.setPreserveRatio(true);
                        imageView.setPickOnBounds(true);

                        Label label1 = new Label(coinFullName);

                        hBox.getChildren().addAll(imageView, label1);
                        hBox.setSpacing(10);

                        Label label2 = new Label(summary);
                        label2.setWrapText(true);
                        if(!wp.equals("")){
                            label2.setText(wp);
                        }
                        vBoxConent.getChildren().addAll(hBox, label2);

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("address")){

                        String network = jsonObject.get("network").getAsString();
                        String icon = network.toLowerCase();
                        String hash = jsonObject.get("hash").getAsString();
                        String addrAlias = jsonObject.has("addrAlias")? jsonObject.get("addrAlias").getAsString() : "";
                        String txCount = jsonObject.has("txCount")? jsonObject.get("txCount").getAsString() : "";
                        String spend = jsonObject.has("spend")? jsonObject.get("spend").getAsString() : "";
                        String receive = jsonObject.has("receive")? jsonObject.get("receive").getAsString() : "";
                        String balance = jsonObject.has("balance")? jsonObject.get("balance").getAsString() : "";
                        String normalTxCount = jsonObject.has("normalTxCount")? jsonObject.get("normalTxCount").getAsString() : "";

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = new HBox();
                        hBox.getStyleClass().add("cellHBox");
                        hBox.setAlignment(Pos.CENTER);

                        Image image = new Image(blockUrl + "/icon/" + icon + ".png", true);
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(35.0);
                        imageView.setFitWidth(35.0);
                        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
                        Rectangle clip = clipRect(
                                imageView, arcProperty
                        );
                        imageView.setClip(clip);
                        imageView.setPreserveRatio(true);
                        imageView.setPickOnBounds(true);

                        Label label1 = new Label(network);

                        hBox.getChildren().addAll(imageView, label1);
                        hBox.setSpacing(10);

                        Label label2 = new Label(I18nUtils.getString("blockchain.addr.hash", hash));
                        vBoxConent.getChildren().addAll(hBox, label2);

                        if(!addrAlias.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.name", addrAlias));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!txCount.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.txcount", txCount));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!spend.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.spend", spend));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!receive.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.receive", receive));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!balance.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.balance", balance, network));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!normalTxCount.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.addr.normaltx", normalTxCount));
                            vBoxConent.getChildren().add(label);
                        }

                        vBoxConent.setOnMouseClicked(event->getDetailedInfo(jsonObject));

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("block")) {
                        String network = jsonObject.get("network").getAsString();
                        String icon = network;
                        String block_no = jsonObject.get("block_no").getAsString();

                        String fee = jsonObject.has("fee")? jsonObject.get("fee").getAsString() : "";
                        String txCnt = jsonObject.has("txCnt")? jsonObject.get("txCnt").getAsString() : "";
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String time = jsonObject.has("time")? Instant.ofEpochSecond(jsonObject.get("time").getAsLong()).atZone(ZoneId.systemDefault()).format(formatter) : "";
                        String blockhash = jsonObject.has("blockhash")? jsonObject.get("blockhash").getAsString() : "";
                        String confirmations = jsonObject.has("confirmations")? jsonObject.get("confirmations").getAsString() : "";

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = new HBox();
                        hBox.getStyleClass().add("cellHBox");
                        hBox.setAlignment(Pos.CENTER);

                        Image image = new Image(blockUrl + "/icon/" + icon + ".png", true);
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(35.0);
                        imageView.setFitWidth(35.0);
                        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
                        Rectangle clip = clipRect(
                                imageView, arcProperty
                        );
                        imageView.setClip(clip);
                        imageView.setPreserveRatio(true);
                        imageView.setPickOnBounds(true);

                        Label label1 = new Label(network);

                        hBox.getChildren().addAll(imageView, label1);
                        hBox.setSpacing(10);

                        Label label2 = new Label(I18nUtils.getString("blockchain.block.id", block_no));
                        vBoxConent.getChildren().addAll(hBox, label2);

                        if(!blockhash.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.hash", blockhash));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!time.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.time", time));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!txCnt.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.txcount", txCnt));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!confirmations.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.confirm", confirmations));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!fee.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.fee", fee));
                            vBoxConent.getChildren().add(label);
                        }

                        vBoxConent.setOnMouseClicked(event->getDetailedInfo(jsonObject));

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("tx")){

                        String network = jsonObject.get("network").getAsString();
                        String icon = network;
                        String block_no = jsonObject.get("block_no").getAsString();
                        String txid = jsonObject.get("txid").getAsString();

                        String from = jsonObject.has("from")? jsonObject.get("from").getAsString() : "";
                        String to = jsonObject.has("to")? jsonObject.get("to").getAsString() : "";
                        String fee = jsonObject.has("fee")? jsonObject.get("fee").getAsString() : "";
                        String txCnt = jsonObject.has("txCnt")? jsonObject.get("txCnt").getAsString() : "";
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String time = jsonObject.has("time")? Instant.ofEpochSecond(jsonObject.get("time").getAsLong()).atZone(ZoneId.systemDefault()).format(formatter) : "";
                        String blockhash = jsonObject.has("blockhash")? jsonObject.get("blockhash").getAsString() : "";
                        String confirmations = jsonObject.has("confirmations")? jsonObject.get("confirmations").getAsString() : "";

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = new HBox();
                        hBox.getStyleClass().add("cellHBox");
                        hBox.setAlignment(Pos.CENTER);

                        Image image = new Image(blockUrl + "/icon/" + icon + ".png", true);
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(35.0);
                        imageView.setFitWidth(35.0);
                        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
                        Rectangle clip = clipRect(
                                imageView, arcProperty
                        );
                        imageView.setClip(clip);
                        imageView.setPreserveRatio(true);
                        imageView.setPickOnBounds(true);

                        Label label1 = new Label(network);

                        hBox.getChildren().addAll(imageView, label1);
                        hBox.setSpacing(10);

                        Label label2 = new Label(I18nUtils.getString("blockchain.block.id", block_no));
                        Label label3 = new Label(I18nUtils.getString("blockchain.tx.hash", txid));
                        vBoxConent.getChildren().addAll(hBox, label2, label3);

                        if(!from.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.tx.from", from));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!to.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.tx.to", to));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!blockhash.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.hash", blockhash));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!time.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.time", time));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!txCnt.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.txcount", txCnt));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!confirmations.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.confirm", confirmations));
                            vBoxConent.getChildren().add(label);
                        }
                        if(!fee.equals("")){
                            Label label = new Label(I18nUtils.getString("blockchain.block.fee", fee));
                            vBoxConent.getChildren().add(label);
                        }

                        vBoxConent.setOnMouseClicked(event->showRegionTokentrans());

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });
                    }
                }
                Platform.runLater(() -> {
                    listViewCell.setItems(contentObj);

                    if(contentObj.isEmpty()) {
                        contentObj.clear();
                        listViewCell.getItems().clear();

                        Label tips = new Label(I18nUtils.getString("blockchain.error.query"));
                        tips.setAlignment(Pos.CENTER);
                        tips.setId("tipTitle");
                        tips.setPrefWidth(sPane.getWidth() - 280);
                        contentObj.add(tips);
                        listViewCell.setItems(contentObj);
                    }
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

    private void getDetailedInfo(JsonObject jsonObject) {
        detailedPane.setVisible(true);
        detailedContentObj.clear();
        detailedListView.getItems().clear();

        Label tips = new Label(I18nUtils.getString("blockchain.querying.detail"));
        tips.setAlignment(Pos.CENTER);
        tips.setId("tipTitle");
        tips.setPrefWidth(sPane.getWidth() - 80);
        tips.setMinWidth(sPane.getWidth() - 80);
        tips.setMaxWidth(sPane.getWidth() - 80);
        detailedContentObj.add(tips);
        detailedListView.setItems(detailedContentObj);

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(detailedListView.prefWidthProperty(), 0)),
                new KeyFrame(Duration.seconds(0.2), new KeyValue(detailedListView.prefWidthProperty(), listViewCell.getWidth()))
        );
        animation.play();

        String type = jsonObject.get("type").getAsString();

        accordionPane.getPanes().clear();
        accordionPane.setVisible(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    if (type.equals("address") && jsonObject.has("network")) {
                        String address = jsonObject.get("hash").getAsString();
                        String network = jsonObject.get("network").getAsString();

                        try {
                            //  获取交易地址基础信息
                            JsonObject addressInfo = address(network, address);
                            if (addressInfo != null) {
                                String key = I18nUtils.getString("blockchain.nav.addrinfo");
                                JsonObject baseInfo = (JsonObject) ((JsonObject) addressInfo.get("data")).get("baseInfo");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                VBox vBox = new VBox();
                                vBox.setStyle("-fx-padding: 10 10 20 20;");
                                for (String jsonKey : baseInfo.keySet()) {
                                    String value = baseInfo.get(jsonKey).getAsString();
                                    Label keyLabel = new Label(jsonKey);
                                    keyLabel.setPrefWidth(150);
                                    Label valueLabel = new Label("：" + value);
                                    HBox hBox = new HBox(keyLabel, valueLabel);
                                    vBox.getChildren().add(hBox);
                                }
                                Platform.runLater(() -> {
                                    detailedContentObj.addAll(title, vBox);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                        try {
                            //  近180天余额变化
                            JsonObject balancetrend = balancetrend(network, address);
                            if (balancetrend != null) {

                                String key = I18nUtils.getString("blockchain.nav.balance180");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                JsonArray tmpJArray = (JsonArray) balancetrend.get("data");

                                CategoryAxis xAxis = new CategoryAxis();
                                NumberAxis yAxis = new NumberAxis();
                                xAxis.setLabel("时间");
                                yAxis.setLabel("余额");
                                LineChart<String, Number> lineChart = new LineChart<String, Number>(xAxis, yAxis);
                                lineChart.setTitle(key);

                                XYChart.Series series = new XYChart.Series();
                                series.setName(key);
                                for (int i = 0; i < tmpJArray.size(); i++) {
                                    JsonObject tmpJObj = (JsonObject) tmpJArray.get(i);
                                    String jsonKey = tmpJObj.keySet().iterator().next();
                                    Double value = tmpJObj.get(jsonKey).getAsDouble();
                                    series.getData().add(new XYChart.Data(jsonKey, value));
                                }
                                lineChart.getData().add(series);


                                Platform.runLater(() -> {
                                    detailedContentObj.addAll(title, lineChart);
                                    detailedListView.setItems(detailedContentObj);
                                });
                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                        try {
                            //  代币余额
                            JsonObject tokenbalance = tokenbalance(network, address);
                            if (tokenbalance != null) {

                                String key = I18nUtils.getString("blockchain.nav.tokenbalance");
                                JsonArray tokenbalanceInfo = (JsonArray) tokenbalance.get("data");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                double tabelWidth = detailedListView.getPrefWidth() - 80;

                                ListView newListView = new ListView();
                                newListView.getStyleClass().add("tableListView");
                                ObservableList<Node> newContentObj = FXCollections.observableArrayList();
                                newListView.setPrefWidth(tabelWidth);
                                HBox hBox = new HBox();
                                hBox.getStyleClass().add("tableTitle");
                                Label label1 = new Label(I18nUtils.getString("blockchain.table.fullname"));
                                label1.setAlignment(Pos.CENTER);
                                label1.setPrefWidth(tabelWidth / 3);
                                Label label2 = new Label(I18nUtils.getString("blockchain.table.balance"));
                                label2.setAlignment(Pos.CENTER);
                                label2.setPrefWidth(tabelWidth / 3);
                                Label label3 = new Label(I18nUtils.getString("blockchain.table.txdetail"));
                                label3.setAlignment(Pos.CENTER);
                                label3.setPrefWidth(tabelWidth / 3);
                                hBox.getChildren().addAll(label1, label2, label3);
                                newContentObj.add(hBox);

                                for (int i = 0; i < tokenbalanceInfo.size(); i++) {

                                    JsonObject tmpJObj = (JsonObject) tokenbalanceInfo.get(i);
                                    JsonObject tokenInfo = tmpJObj.get("tokenInfo").getAsJsonObject();

                                    String fromName = tokenInfo.get("f").getAsString();
                                    int decimals = tokenInfo.get("d").getAsInt();
                                    String balanceNetwork = tokenInfo.get("s").getAsString();

                                    String balanceHash = tmpJObj.get("hash").getAsString();
                                    String transferCnt = tmpJObj.get("transferCnt").getAsString();
                                    String balance = new BigDecimal(tmpJObj.get("balance").getAsString())
                                            .divide(BigDecimal.TEN.pow((int) decimals))
                                            .toPlainString();
                                    String spendTransferCnt = tmpJObj.get("spendTransferCnt").getAsString();
                                    String receiveTransferCnt = tmpJObj.get("receiveTransferCnt").getAsString();

                                    HBox tmpHBox = new HBox();
                                    tmpHBox.getStyleClass().add("tableContent");
                                    Label label4 = new Label(fromName);
                                    label4.setAlignment(Pos.CENTER);
                                    label4.setPrefWidth(tabelWidth / 3);
                                    Label label5 = new Label(balance + " " + balanceNetwork);
                                    label5.setAlignment(Pos.CENTER);
                                    label5.setPrefWidth(tabelWidth / 3);
                                    Label label6 = new Label(I18nUtils.getString("blockchain.tx.count", transferCnt));
                                    label6.setAlignment(Pos.CENTER);
                                    label6.setStyle("-fx-border-width: 0;-fx-border-width:0;");

                                    Region regionTokentrans = new Region();
                                    regionTokentrans.getStyleClass().add("tokentransIcon");
                                    Tooltip tooltip = new Tooltip(I18nUtils.getString("tooltip.view.txdetail"));
                                    Tooltip.install(regionTokentrans, tooltip);
                                    regionTokentrans.setOnMouseClicked(even -> showRegionTokentrans());
                                    HBox finalyHBox = new HBox(label6, regionTokentrans);
                                    finalyHBox.setPrefWidth(tabelWidth / 3);
                                    finalyHBox.setAlignment(Pos.CENTER);

                                    tmpHBox.getChildren().addAll(label4, label5, finalyHBox);

                                    newContentObj.add(tmpHBox);
                                }

                                Platform.runLater(() -> {
                                    newListView.setItems(newContentObj);
                                    detailedContentObj.addAll(title, newListView);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                    } else if (type.equals("block") && jsonObject.has("network")) {
                        String block = jsonObject.get("block_no").getAsString();
                        String network = jsonObject.get("network").getAsString();

                        //  获取交易地址基础信息
                        JsonObject blockInfo = block(network, block);
                        JsonObject baseInfo = (JsonObject) ((JsonObject) blockInfo.get("data")).get("baseInfo");
                        if (blockInfo != null && baseInfo != null && baseInfo.isJsonNull()) {
                            String key = I18nUtils.getString("blockchain.nav.blockinfo");

                            TitledPane titledPane = new TitledPane();
                            titledPane.setText(key);
                            titledPane.getStyleClass().add("noContentTitlePane");
                            titledPane.setPrefWidth(200);
                            titledPane.setOnMouseClicked(even -> redirect(key));
                            Platform.runLater(() -> {
                                accordionPane.getPanes().add(titledPane);
                            });

                            Text title = new Text(key);
                            title.getStyleClass().add("titleText");

                            VBox vBox = new VBox();
                            vBox.setStyle("-fx-padding: 10 10 20 20;");
                            for (String jsonKey : baseInfo.keySet()) {
                                String value = baseInfo.get(jsonKey).getAsString();
                                Label keyLabel = new Label(jsonKey);
                                keyLabel.setPrefWidth(150);
                                Label valueLabel = new Label("：" + value);
                                HBox hBox = new HBox(keyLabel, valueLabel);
                                vBox.getChildren().add(hBox);
                            }
                            Platform.runLater(() -> {
                                detailedContentObj.addAll(title, vBox);
                                detailedListView.setItems(detailedContentObj);
                            });

                        }

                        try {
                            //  获取交易
                            int num1;
                            try {
                                String numStr = ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).get("合约调用转帐").getAsString();
                                num1 = Integer.parseInt(numStr);
                            } catch (Exception e) {
                                num1 = 0;
                            }
                            if (
                                    ((JsonObject) blockInfo.get("data")).has("count")
                                            && ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).has("合约调用转帐")
                                            && num1 != 0
                            ) {
                                JsonObject getTxData = getTxData(network, block, "1", "20");

                                String key = I18nUtils.getString("blockchain.nav.tx", num1);
                                JsonArray tmpDataInfo = (JsonArray) getTxData.get("data");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                double tabelWidth = detailedListView.getPrefWidth() - 80;

                                ListView newListView = new ListView();
                                newListView.getStyleClass().add("tableListView");
                                ObservableList<Node> newContentObj = FXCollections.observableArrayList();
                                newListView.setPrefWidth(tabelWidth);
                                HBox hBox = new HBox();
                                hBox.getStyleClass().add("tableTitle");
                                Label label1 = new Label(I18nUtils.getString("blockchain.table.txhash"));
                                label1.setAlignment(Pos.CENTER);
                                label1.setPrefWidth(tabelWidth * 0.13);
                                Label label2 = new Label(I18nUtils.getString("blockchain.table.function"));
                                label2.setAlignment(Pos.CENTER);
                                label2.setPrefWidth(tabelWidth * 0.12);
                                Label label3 = new Label(I18nUtils.getString("blockchain.table.blockheight"));
                                label3.setAlignment(Pos.CENTER);
                                label3.setPrefWidth(tabelWidth * 0.12);
                                Label label4 = new Label(I18nUtils.getString("blockchain.table.time"));
                                label4.setAlignment(Pos.CENTER);
                                label4.setPrefWidth(tabelWidth * 0.12);
                                Label label5 = new Label(I18nUtils.getString("blockchain.table.from"));
                                label5.setAlignment(Pos.CENTER);
                                label5.setPrefWidth(tabelWidth * 0.13);
                                Label label6 = new Label(I18nUtils.getString("blockchain.table.to"));
                                label6.setAlignment(Pos.CENTER);
                                label6.setPrefWidth(tabelWidth * 0.13);
                                Label label7 = new Label(I18nUtils.getString("blockchain.table.amount"));
                                label7.setAlignment(Pos.CENTER);
                                label7.setPrefWidth(tabelWidth * 0.125);
                                Label label8 = new Label(I18nUtils.getString("blockchain.table.fee"));
                                label8.setAlignment(Pos.CENTER);
                                label8.setPrefWidth(tabelWidth * 0.125);
                                hBox.getChildren().addAll(label1, label2, label3, label4, label5, label6, label7, label8);
                                newContentObj.add(hBox);

                                for (int i = 0; i < tmpDataInfo.size(); i++) {
                                    JsonObject tmpJObj = (JsonObject) tmpDataInfo.get(i);
                                    String txid = tmpJObj.get("txid").getAsString();
                                    String block_no = tmpJObj.get("block_no").getAsString();
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                    String time = Instant.ofEpochSecond(tmpJObj.get("time").getAsLong()).atZone(ZoneId.systemDefault()).format(formatter);
                                    String from = tmpJObj.get("from").getAsString();
                                    String to = tmpJObj.get("to").getAsString();
                                    String value = tmpJObj.get("value").getAsString();
                                    String fee = tmpJObj.get("fee").getAsString();
                                    String networkFree = tmpJObj.get("network").getAsString();

                                    HBox tmpHBox = new HBox();
                                    tmpHBox.getStyleClass().add("tableContent");
                                    Label label9 = new Label(txid);
                                    label9.setAlignment(Pos.CENTER);
                                    label9.setPrefWidth(tabelWidth * 0.13);
                                    Label label10 = new Label(block_no);
                                    label10.setAlignment(Pos.CENTER);
                                    label10.setPrefWidth(tabelWidth * 0.12);
                                    Label label11 = new Label(block_no);
                                    label11.setAlignment(Pos.CENTER);
                                    label11.setPrefWidth(tabelWidth * 0.12);
                                    Label label12 = new Label(time);
                                    label12.setAlignment(Pos.CENTER);
                                    label12.setPrefWidth(tabelWidth * 0.12);
                                    Label label13 = new Label(from);
                                    label13.setAlignment(Pos.CENTER);
                                    label13.setPrefWidth(tabelWidth * 0.13);
                                    Label label14 = new Label(to);
                                    label14.setAlignment(Pos.CENTER);
                                    label14.setPrefWidth(tabelWidth * 0.13);
                                    Label label15 = new Label(value);
                                    label15.setAlignment(Pos.CENTER);
                                    label15.setPrefWidth(tabelWidth * 0.125);
                                    Label label16 = new Label(fee + " " + networkFree);
                                    label16.setAlignment(Pos.CENTER);
                                    label16.setPrefWidth(tabelWidth * 0.125);
                                    tmpHBox.getChildren().addAll(label9, label10, label11, label12, label13, label14, label15, label16);
                                    newContentObj.add(tmpHBox);

                                }

                                Platform.runLater(() -> {
                                    newListView.setItems(newContentObj);
                                    detailedContentObj.addAll(title, newListView);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            }

                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }


                        try {

                            //  获取代币交易
                            int num2;
                            try {
                                String numStr = ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).get("代币交易").getAsString();
                                num2 = Integer.parseInt(numStr);
                            } catch (Exception e) {
                                num2 = 0;
                            }
                            if (
                                    ((JsonObject) blockInfo.get("data")).has("count")
                                            && ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).has("代币交易")
                                            && num2 != 0
                            ) {
                                JsonObject getTokentransferData = getTokentransferData(network, block, "1", "20");

                                String key = I18nUtils.getString("blockchain.nav.tokentx", num2);
                                JsonObject tmpDataInfo = (JsonObject) getTokentransferData.get("data");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                double tabelWidth = detailedListView.getPrefWidth() - 80;

                                ListView newListView = new ListView();
                                newListView.getStyleClass().add("tableListView");
                                ObservableList<Node> newContentObj = FXCollections.observableArrayList();
                                newListView.setPrefWidth(tabelWidth);
                                HBox hBox = new HBox();
                                hBox.getStyleClass().add("tableTitle");
                                Label label1 = new Label(I18nUtils.getString("blockchain.table.txhash"));
                                label1.setAlignment(Pos.CENTER);
                                label1.setPrefWidth(tabelWidth * 0.15);
                                Label label2 = new Label(I18nUtils.getString("blockchain.table.blockheight"));
                                label2.setAlignment(Pos.CENTER);
                                label2.setPrefWidth(tabelWidth * 0.14);
                                Label label3 = new Label(I18nUtils.getString("blockchain.table.time"));
                                label3.setAlignment(Pos.CENTER);
                                label3.setPrefWidth(tabelWidth * 0.14);
                                Label label4 = new Label(I18nUtils.getString("blockchain.table.from"));
                                label4.setAlignment(Pos.CENTER);
                                label4.setPrefWidth(tabelWidth * 0.15);
                                Label label5 = new Label(I18nUtils.getString("blockchain.table.to"));
                                label5.setAlignment(Pos.CENTER);
                                label5.setPrefWidth(tabelWidth * 0.15);
                                Label label6 = new Label(I18nUtils.getString("blockchain.table.amount"));
                                label6.setAlignment(Pos.CENTER);
                                label6.setPrefWidth(tabelWidth * 0.17);
                                Label label66 = new Label(I18nUtils.getString("blockchain.table.token"));
                                label66.setAlignment(Pos.CENTER);
                                label66.setPrefWidth(tabelWidth * 0.1);
                                hBox.getChildren().addAll(label1, label2, label3, label4, label5, label6, label66);
                                newContentObj.add(hBox);

                                for (Map.Entry<String, JsonElement> entry : tmpDataInfo.entrySet()) {
                                    String entryKey = entry.getKey();
                                    JsonArray entryValue = (JsonArray) entry.getValue();

                                    for (int i = 0; i < entryValue.size(); i++) {
                                        JsonObject tmpJObj = (JsonObject) entryValue.get(i);
                                        String block_no = tmpJObj.get("block_no").getAsString();
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                        String time = Instant.ofEpochSecond(tmpJObj.get("time").getAsLong()).atZone(ZoneId.systemDefault()).format(formatter);
                                        String from = tmpJObj.get("from").getAsString();
                                        String to = tmpJObj.get("to").getAsString();
                                        String value = tmpJObj.get("value").getAsString();
                                        String icon = tmpJObj.get("tokenSymbol").getAsString();


                                        HBox tmpHBox = new HBox();
                                        tmpHBox.getStyleClass().add("tableContent");
                                        Label label7 = new Label(entryKey);
                                        label7.setAlignment(Pos.CENTER);
                                        label7.setPrefWidth(tabelWidth * 0.15);
                                        Label label8 = new Label(block_no);
                                        label8.setAlignment(Pos.CENTER);
                                        label8.setPrefWidth(tabelWidth * 0.14);
                                        Label label9 = new Label(time);
                                        label9.setAlignment(Pos.CENTER);
                                        label9.setPrefWidth(tabelWidth * 0.14);
                                        Label label10 = new Label(from);
                                        label10.setAlignment(Pos.CENTER);
                                        label10.setPrefWidth(tabelWidth * 0.15);
                                        Label label11 = new Label(to);
                                        label11.setAlignment(Pos.CENTER);
                                        label11.setPrefWidth(tabelWidth * 0.15);
                                        Label label12 = new Label(value);
                                        label12.setAlignment(Pos.CENTER);
                                        label12.setPrefWidth(tabelWidth * 0.17);

                                        Label label1212 = new Label(icon);
                                        label1212.setStyle("-fx-padding: 0;-fx-border-width: 0;");

                                        Image image = new Image(blockUrl + "/icon/" + icon + ".png", true);
                                        ImageView imageView = new ImageView(image);
                                        imageView.setFitHeight(20.0);
                                        imageView.setFitWidth(20.0);
                                        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(35.0);
                                        Rectangle clip = clipRect(
                                                imageView, arcProperty
                                        );
                                        imageView.setClip(clip);
                                        imageView.setPreserveRatio(true);
                                        imageView.setPickOnBounds(true);
                                        label1212.setAlignment(Pos.CENTER);
                                        HBox hBox1 = new HBox(imageView, label1212);
                                        hBox1.setAlignment(Pos.CENTER);
                                        label1212.setPrefWidth(tabelWidth * 0.1);
                                        tmpHBox.getChildren().addAll(label7, label8, label9, label10, label11, label12, hBox1);
                                        newContentObj.add(tmpHBox);

                                    }
                                }

                                Platform.runLater(() -> {
                                    newListView.setItems(newContentObj);
                                    detailedContentObj.addAll(title, newListView);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            }

                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }


                        try {

                            //  获取合约调用转帐
                            int num3;
                            try {
                                String numStr = ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).get("合约调用转帐").getAsString();
                                num3 = Integer.parseInt(numStr);
                            } catch (Exception e) {
                                num3 = 0;
                            }
                            if (
                                    ((JsonObject) blockInfo.get("data")).has("count")
                                            && ((JsonObject) ((JsonObject) blockInfo.get("data")).get("count")).has("合约调用转帐")
                                            && num3 != 0
                            ) {
                                JsonObject getInternalData = getInternalData(network, block, "1", "20");

                                String key = I18nUtils.getString("blockchain.nav.contracttx", num3);
                                JsonObject tmpDataInfo = (JsonObject) getInternalData.get("data");

                                TitledPane titledPane = new TitledPane();
                                titledPane.setText(key);
                                titledPane.getStyleClass().add("noContentTitlePane");
                                titledPane.setPrefWidth(200);
                                titledPane.setOnMouseClicked(even -> redirect(key));
                                Platform.runLater(() -> {
                                    accordionPane.getPanes().add(titledPane);
                                });

                                Text title = new Text(key);
                                title.getStyleClass().add("titleText");

                                double tabelWidth = detailedListView.getPrefWidth() - 80;

                                ListView newListView = new ListView();
                                newListView.getStyleClass().add("tableListView");
                                ObservableList<Node> newContentObj = FXCollections.observableArrayList();
                                newListView.setPrefWidth(tabelWidth);
                                HBox hBox = new HBox();
                                hBox.getStyleClass().add("tableTitle");
                                Label label1 = new Label(I18nUtils.getString("blockchain.table.txhash"));
                                label1.setAlignment(Pos.CENTER);
                                label1.setPrefWidth(tabelWidth * 0.16);
                                Label label2 = new Label(I18nUtils.getString("blockchain.table.blockheight"));
                                label2.setAlignment(Pos.CENTER);
                                label2.setPrefWidth(tabelWidth * 0.16);
                                Label label3 = new Label(I18nUtils.getString("blockchain.table.time"));
                                label3.setAlignment(Pos.CENTER);
                                label3.setPrefWidth(tabelWidth * 0.16);
                                Label label4 = new Label(I18nUtils.getString("blockchain.table.from"));
                                label4.setAlignment(Pos.CENTER);
                                label4.setPrefWidth(tabelWidth * 0.16);
                                Label label5 = new Label(I18nUtils.getString("blockchain.table.to"));
                                label5.setAlignment(Pos.CENTER);
                                label5.setPrefWidth(tabelWidth * 0.16);
                                Label label6 = new Label(I18nUtils.getString("blockchain.table.amount"));
                                label6.setAlignment(Pos.CENTER);
                                label6.setPrefWidth(tabelWidth * 0.2);
                                hBox.getChildren().addAll(label1, label2, label3, label4, label5, label6);
                                newContentObj.add(hBox);

                                for (Map.Entry<String, JsonElement> entry : tmpDataInfo.entrySet()) {
                                    String entryKey = entry.getKey();
                                    JsonArray entryValue = (JsonArray) entry.getValue();

                                    for (int i = 0; i < entryValue.size(); i++) {
                                        JsonObject tmpJObj = (JsonObject) entryValue.get(i);
                                        String block_no = tmpJObj.get("block_no").getAsString();
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                        String time = Instant.ofEpochSecond(tmpJObj.get("time").getAsLong()).atZone(ZoneId.systemDefault()).format(formatter);
                                        String from = tmpJObj.get("from").getAsString();
                                        String to = tmpJObj.get("to").getAsString();
                                        String value = tmpJObj.get("value").getAsString();


                                        HBox tmpHBox = new HBox();
                                        tmpHBox.getStyleClass().add("tableContent");
                                        Label label7 = new Label(entryKey);
                                        label7.setAlignment(Pos.CENTER);
                                        label7.setPrefWidth(tabelWidth * 0.16);
                                        Label label8 = new Label(block_no);
                                        label8.setAlignment(Pos.CENTER);
                                        label8.setPrefWidth(tabelWidth * 0.16);
                                        Label label9 = new Label(time);
                                        label9.setAlignment(Pos.CENTER);
                                        label9.setPrefWidth(tabelWidth * 0.16);
                                        Label label10 = new Label(from);
                                        label10.setAlignment(Pos.CENTER);
                                        label10.setPrefWidth(tabelWidth * 0.16);
                                        Label label11 = new Label(to);
                                        label11.setAlignment(Pos.CENTER);
                                        label11.setPrefWidth(tabelWidth * 0.16);
                                        Label label12 = new Label(value + " " + network);
                                        label12.setAlignment(Pos.CENTER);
                                        label12.setPrefWidth(tabelWidth * 0.2);
                                        tmpHBox.getChildren().addAll(label7, label8, label9, label10, label11, label12);
                                        newContentObj.add(tmpHBox);

                                    }
                                }

                                Platform.runLater(() -> {
                                    newListView.setItems(newContentObj);
                                    detailedContentObj.addAll(title, newListView);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                    }

                    Platform.runLater(() -> {
                        detailedListView.setItems(detailedContentObj);

                        if (detailedContentObj.isEmpty()) {
                            detailedContentObj.clear();
                            detailedListView.getItems().clear();

                            Label tips = new Label(I18nUtils.getString("blockchain.error.query"));
                            tips.setAlignment(Pos.CENTER);
                            tips.setId("tipTitle");
                            tips.setPrefWidth(sPane.getWidth() - 280);
                            Platform.runLater(() -> {
                                detailedContentObj.add(tips);
                                detailedListView.setItems(detailedContentObj);
                            });
                        }
                    });

                }catch (Exception e){} finally {
                    tips.setManaged(false);
                    tips.setVisible(false);
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

    private void showRegionTokentrans() {
        System.out.println("第一版先到这里哦~ 第一版暂没写批量查询及导出，工作比较忙");
    }

    //  导航转跳
    private void redirect(String key) {
        for (int i = 0; i < detailedListView.getItems().size(); i++) {
            Node node = (Node) detailedListView.getItems().get(i);
            if (node instanceof Text) {
                String newKey = ((Text)node).getText();
                if(key.equals(newKey)){
                    detailedListView.scrollTo(i);
                    return;
                }
            }
        }
    }


    @FXML
    void goBack(){
        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(detailedListView.prefWidthProperty(), listViewCell.getWidth())),
                new KeyFrame(Duration.seconds(0.2), new KeyValue(detailedListView.prefWidthProperty(), 0))
        );
        animation.setOnFinished(event -> {
            accordionPane.getPanes().clear();
            accordionPane.setVisible(false);
            detailedPane.setVisible(false);
            detailedListView.getItems().clear();
        });
        animation.play();

    }

    @FXML
    public void goBackAndClear(MouseEvent event) {
        question.setText("");
        searchInput();
        backClearLabel.setVisible(false);
    }
}
