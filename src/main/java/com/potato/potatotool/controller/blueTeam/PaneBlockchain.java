package com.potato.potatotool.controller.blueTeam;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.blueTeam.blockchain.report.BlockchainWordReportGenerator;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
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

    @FXML
    private Button exportReportButton;

    @FXML
    private StackPane promptPane;

    @FXML
    private Label prompt;

    @FXML
    private ScrollPane accordionScrollPane;

    private final BlockchainWordReportGenerator reportGenerator = new BlockchainWordReportGenerator();
    private JsonObject currentReportData;
    private String currentReportType = "";
    private String currentReportNetwork = "";
    private String currentReportTarget = "";
    private String currentReportAiAnalysis = "";
    private long currentReportVersion = 0L;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    private enum PromptTone {
        INFO,
        SUCCESS,
        ERROR
    }

    private static class BlockchainSampleCase {
        private final String target;
        private final String network;
        private final String reportType;
        private final String titleKey;
        private final String badgeKey;
        private final String descriptionKey;
        private final String capabilityKey;

        private BlockchainSampleCase(String target, String network, String reportType,
                                     String titleKey, String badgeKey,
                                     String descriptionKey, String capabilityKey) {
            this.target = target;
            this.network = network;
            this.reportType = reportType;
            this.titleKey = titleKey;
            this.badgeKey = badgeKey;
            this.descriptionKey = descriptionKey;
            this.capabilityKey = capabilityKey;
        }
    }

    private String getAsString(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        return jsonObject.get(key).getAsString();
    }

    private JsonObject getAsObject(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()
                || !jsonObject.get(key).isJsonObject()) {
            return null;
        }
        return jsonObject.getAsJsonObject(key);
    }

    private JsonArray getAsArray(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()
                || !jsonObject.get(key).isJsonArray()) {
            return null;
        }
        return jsonObject.getAsJsonArray(key);
    }

    private boolean isSuccess(JsonObject jsonObject) {
        return jsonObject != null
                && jsonObject.has("code")
                && !jsonObject.get("code").isJsonNull()
                && "1".equals(jsonObject.get("code").getAsString());
    }

    private String getResponseMessage(JsonObject jsonObject) {
        String message = getAsString(jsonObject, "msg");
        if (message.isEmpty()) {
            return I18nUtils.getString("blockchain.error.query");
        }
        String lower = message.toLowerCase();
        if (lower.contains("unauthorized")) {
            return I18nUtils.getString("blockchain.error.unauthorized");
        }
        if (lower.contains("404") || lower.contains("not found") || lower.contains("路径不存在") || lower.contains("未部署")) {
            return I18nUtils.getString("blockchain.error.api.missing");
        }
        return message;
    }

    private String getDisplayText(JsonObject jsonObject, String key) {
        String value = getAsString(jsonObject, key).trim();
        if (value.isEmpty()) {
            return "";
        }
        String lowerValue = value.toLowerCase();
        if (lowerValue.startsWith("http://") || lowerValue.startsWith("https://")
                || lowerValue.endsWith(".pdf")) {
            return "";
        }
        return value;
    }

    private String sanitizeRichText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<b>", "").replace("</b>", "").trim();
    }

    private double getDouble(JsonObject jsonObject, String key, double defaultValue) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return jsonObject.get(key).getAsDouble();
        } catch (Exception e) {
            try {
                return new BigDecimal(jsonObject.get(key).getAsString()).doubleValue();
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
    }

    private LinkedHashMap<String, Double> buildTrendData(JsonArray trendArray, String currentBalance) {
        LinkedHashMap<String, Double> trendData = new LinkedHashMap<>();
        double fallbackBalance;
        try {
            fallbackBalance = new BigDecimal(currentBalance == null || currentBalance.trim().isEmpty() ? "0" : currentBalance.trim()).doubleValue();
        } catch (Exception e) {
            fallbackBalance = 0D;
        }

        if (trendArray != null) {
            for (int i = 0; i < trendArray.size(); i++) {
                JsonElement trendElement = trendArray.get(i);
                if (trendElement == null || !trendElement.isJsonObject()) {
                    continue;
                }
                JsonObject trendObject = trendElement.getAsJsonObject();
                String date = getAsString(trendObject, "date");
                if (!date.equals("")) {
                    double balanceValue = getDouble(trendObject, "balance",
                            getDouble(trendObject, "value", fallbackBalance));
                    trendData.put(date, balanceValue);
                    continue;
                }
                for (String jsonKey : trendObject.keySet()) {
                    trendData.put(jsonKey, getDouble(trendObject, jsonKey, fallbackBalance));
                }
            }
        }
        return trendData;
    }

    private void initReportControls() {
        if (exportReportButton != null) {
            exportReportButton.setGraphic(createToolbarButtonIcon(MaterialDesign.MDI_DOWNLOAD));
            exportReportButton.setDisable(true);
        }
        if (promptPane != null) {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        }
    }

    private FontIcon createToolbarButtonIcon(MaterialDesign iconCode) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.web("#D9F6FF"));
        return icon;
    }

    private void clearCurrentReportContext() {
        currentReportData = null;
        currentReportType = "";
        currentReportNetwork = "";
        currentReportTarget = "";
        currentReportAiAnalysis = "";
        if (exportReportButton != null) {
            exportReportButton.setDisable(true);
        }
    }

    private long beginReportSession(String reportType, String network, String target) {
        currentReportVersion++;
        currentReportType = reportType == null ? "" : reportType;
        currentReportNetwork = network == null ? "" : network;
        currentReportTarget = target == null ? "" : target;
        currentReportData = null;
        currentReportAiAnalysis = "";
        if (exportReportButton != null) {
            exportReportButton.setDisable(true);
        }
        return currentReportVersion;
    }

    private void cacheCurrentReportData(long reportVersion, String reportType, String network, String target, JsonObject reportData) {
        if (reportVersion != currentReportVersion) {
            return;
        }
        currentReportType = reportType == null ? "" : reportType;
        currentReportNetwork = network == null ? "" : network;
        currentReportTarget = target == null ? "" : target;
        currentReportData = reportData;
        currentReportAiAnalysis = "";
        if (exportReportButton != null) {
            exportReportButton.setDisable(reportData == null);
        }
    }

    private void cacheCurrentAiAnalysis(long reportVersion, String aiAnalysis) {
        if (reportVersion != currentReportVersion) {
            return;
        }
        currentReportAiAnalysis = aiAnalysis == null ? "" : aiAnalysis.trim();
    }

    private Label createValueLabel(String value) {
        Label label = new Label(value == null ? "" : value);
        label.setWrapText(true);
        label.setMaxWidth(620);
        return label;
    }

    private void addInfoRow(VBox container, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setPrefWidth(150);
        Label valueLabel = createValueLabel("：" + (value == null ? "" : value));
        HBox hBox = new HBox(keyLabel, valueLabel);
        container.getChildren().add(hBox);
    }

    private Image loadBlockchainIcon(String icon) {
        String safeIcon = icon == null ? "" : icon.trim().toLowerCase().replaceAll("[^a-z0-9_.-]", "");
        if (!safeIcon.endsWith(".png")) {
            safeIcon = safeIcon + ".png";
        }
        try (java.io.InputStream iconStream = getClass().getResourceAsStream("/img/blockchain/" + safeIcon)) {
            if (iconStream != null) {
                return new Image(iconStream);
            }
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
        return new Image(blockUrl + "/icon/" + safeIcon, true);
    }

    private void showWarningDialog(String message) {
        Platform.runLater(() -> showErrorPrompt(message));
    }

    private boolean isServiceUnavailableMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.equals(I18nUtils.getString("blockchain.error.api.missing"))
                || message.equals(I18nUtils.getString("blockchain.error.unauthorized"));
    }

    private JsonObject createAddressFallbackItem(String network, String address) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "address");
        item.addProperty("network", network);
        item.addProperty("hash", address);
        return item;
    }

    private void showListTip(String message) {
        Platform.runLater(() -> {
            contentObj.clear();
            listViewCell.getItems().clear();

            Label tips = new Label(message);
            tips.setAlignment(Pos.CENTER);
            tips.setId("tipTitle");
            tips.setPrefWidth(sPane.getWidth() - 280);
            contentObj.add(tips);
            listViewCell.setItems(contentObj);
        });
    }

    private void showDetailTip(String message) {
        Platform.runLater(() -> {
            detailedContentObj.clear();
            detailedListView.getItems().clear();

            Label tips = new Label(message);
            tips.setAlignment(Pos.CENTER);
            tips.setId("tipTitle");
            tips.setPrefWidth(sPane.getWidth() - 80);
            detailedContentObj.add(tips);
            detailedListView.setItems(detailedContentObj);
        });
    }

    private int getCount(JsonObject blockInfo, String key) {
        try {
            JsonObject data = getAsObject(blockInfo, "data");
            if (data == null || !data.has("count")) {
                return 0;
            }
            JsonObject count = getAsObject(data, "count");
            if (count == null || !count.has(key) || count.get(key).isJsonNull()) {
                return 0;
            }
            return Integer.parseInt(count.get(key).getAsString());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean hasData(JsonObject jsonObject) {
        return jsonObject != null && isSuccess(jsonObject) && jsonObject.has("data") && !jsonObject.get("data").isJsonNull();
    }

    private long getEpochSecond(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return 0L;
        }
        try {
            return jsonObject.get(key).getAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String formatEpochSecond(JsonObject jsonObject, String key) {
        long epochSecond = getEpochSecond(jsonObject, key);
        if (epochSecond <= 0L) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault()).format(formatter);
    }

    private String joinSources(JsonArray markets) {
        if (markets == null || markets.size() == 0) {
            return "";
        }
        ArrayList<String> sources = new ArrayList<>();
        for (int i = 0; i < markets.size(); i++) {
            JsonElement element = markets.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            String exchange = getAsString(element.getAsJsonObject(), "exchange");
            if (!exchange.equals("") && !sources.contains(exchange)) {
                sources.add(exchange);
            }
            if (sources.size() >= 4) {
                break;
            }
        }
        return String.join(", ", sources);
    }

    private void addCoinMarketInfo(VBox container, JsonObject jsonObject) {
        JsonObject market = getAsObject(jsonObject, "market");
        JsonArray markets = getAsArray(jsonObject, "markets");
        if (market == null && (markets == null || markets.size() == 0)) {
            return;
        }

        String last = getAsString(market, "last");
        String median = getAsString(market, "median");
        String displayPrice = !last.equals("") ? last : median;
        if (!displayPrice.equals("")) {
            Label label = new Label(I18nUtils.getString("blockchain.coin.price", displayPrice));
            label.setWrapText(true);
            container.getChildren().add(label);
        }

        String spread = getAsString(market, "spreadPercent");
        if (!spread.equals("")) {
            Label label = new Label(I18nUtils.getString("blockchain.coin.spread", spread));
            label.setWrapText(true);
            container.getChildren().add(label);
        }

        if (markets != null && markets.size() > 0) {
            Label countLabel = new Label(I18nUtils.getString("blockchain.coin.market.count", String.valueOf(markets.size())));
            countLabel.setWrapText(true);
            container.getChildren().add(countLabel);

            String sources = joinSources(markets);
            if (!sources.equals("")) {
                Label sourceLabel = new Label(I18nUtils.getString("blockchain.coin.sources", sources));
                sourceLabel.setWrapText(true);
                container.getChildren().add(sourceLabel);
            }
        }
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    private String objectValue(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception e) {
            return jsonObject.get(key).toString();
        }
    }

    private String objectBooleanValue(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsBoolean()
                    ? I18nUtils.getString("blockchain.report.yes")
                    : I18nUtils.getString("blockchain.report.no");
        } catch (Exception e) {
            return objectValue(jsonObject, key);
        }
    }

    private String valueWithToken(String value, String token) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        if (token == null || token.trim().isEmpty()) {
            return value;
        }
        return value + " " + token;
    }

    private String shortIdentifier(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= 24) {
            return text;
        }
        return text.substring(0, 10) + "..." + text.substring(text.length() - 8);
    }

    private boolean supportsEvmReport(String network) {
        if (network == null) {
            return false;
        }
        String value = network.trim().toLowerCase();
        return value.equals("eth")
                || value.equals("bsc")
                || value.equals("polygon")
                || value.equals("arb")
                || value.equals("op")
                || value.equals("base")
                || value.equals("trx")
                || value.equals("btc");
    }

    private Label createAnalysisLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMaxWidth(detailContentWidth());
        label.getStyleClass().add("analysisLabel");
        return label;
    }

    private Label createAnalysisHeadline(String text) {
        Label label = createAnalysisLabel(text);
        label.getStyleClass().add("analysisHeadline");
        return label;
    }

    private double detailContentWidth() {
        double width = detailedListView == null ? 0D : detailedListView.getWidth();
        if (width <= 0D && detailedListView != null) {
            width = detailedListView.getPrefWidth();
        }
        if (width <= 0D && listViewCell != null) {
            width = listViewCell.getWidth();
        }
        if (width <= 0D && sPane != null) {
            width = sPane.getWidth() - 260D;
        }
        if (width <= 0D) {
            width = 720D;
        }
        return Math.max(360D, width - 115D);
    }

    private VBox createReportSectionBody(String loadingText) {
        VBox body = new VBox();
        body.setSpacing(8);
        body.getStyleClass().add("analysisSection");
        body.getChildren().add(createAnalysisLabel(loadingText));
        return body;
    }

    private void replaceSection(VBox section, Node... nodes) {
        section.getChildren().clear();
        section.getChildren().addAll(nodes);
    }

    private void replaceSection(VBox section, ArrayList<Node> nodes) {
        section.getChildren().clear();
        section.getChildren().addAll(nodes);
    }

    private void appendNamedLines(VBox container, String title, ArrayList<String> lines) {
        if (container == null || lines == null || lines.isEmpty()) {
            return;
        }
        container.getChildren().add(createAnalysisLabel(title));
        for (String line : lines) {
            container.getChildren().add(createAnalysisLabel("• " + line));
        }
    }

    private void appendNamedLines(ArrayList<Node> nodes, String title, ArrayList<String> lines, int limit) {
        if (nodes == null || lines == null || lines.isEmpty()) {
            return;
        }
        VBox box = new VBox();
        box.setSpacing(6);
        box.getStyleClass().add("analysisWarningBox");
        box.getChildren().add(createAnalysisLabel(title));
        int max = Math.min(lines.size(), Math.max(1, limit));
        for (int i = 0; i < max; i++) {
            box.getChildren().add(createAnalysisLabel("• " + lines.get(i)));
        }
        nodes.add(box);
    }

    private void appendNamedLine(ArrayList<String> lines, String key, String value) {
        if (lines == null) {
            return;
        }
        String safeValue = valueOrDash(value);
        if (safeValue.equals("-")) {
            return;
        }
        lines.add(key + "：" + safeValue);
    }

    private void appendNamedLine(ArrayList<String> lines, String key, Object value) {
        appendNamedLine(lines, key, value == null ? "" : String.valueOf(value));
    }

    private void addReportNav(String key) {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(key);
        titledPane.getStyleClass().add("noContentTitlePane");
        titledPane.setPrefWidth(200);
        titledPane.setOnMouseClicked(even -> redirect(key));
        accordionPane.getPanes().add(titledPane);
    }

    private void addReportSection(String key, VBox section) {
        Text title = new Text(key);
        title.getStyleClass().add("titleText");
        detailedContentObj.addAll(title, section);
        addReportNav(key);
    }

    private void slideAccordionIn() {
        if (accordionScrollPane == null) return;
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(accordionScrollPane.translateXProperty(), 210)),
            new KeyFrame(Duration.millis(200), new KeyValue(accordionScrollPane.translateXProperty(), 0))
        );
        t.play();
    }

    private void slideAccordionOut(Runnable onFinished) {
        if (accordionScrollPane == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(accordionScrollPane.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(200), new KeyValue(accordionScrollPane.translateXProperty(), 210))
        );
        if (onFinished != null) t.setOnFinished(e -> onFinished.run());
        t.play();
    }

    private void resetAccordionPosition() {
        if (accordionScrollPane != null) accordionScrollPane.setTranslateX(210);
    }

    private void openReportDetailPane() {
        detailedPane.setVisible(true);
        detailedContentObj.clear();
        detailedListView.getItems().clear();
        accordionPane.getPanes().clear();
        accordionPane.setVisible(true);
        detailedPane.setMaxWidth(listViewCell.getWidth());

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(detailedPane.maxWidthProperty(), 0),
                        new KeyValue(detailedListView.prefWidthProperty(), 0)),
                new KeyFrame(Duration.seconds(0.2),
                        new KeyValue(detailedPane.maxWidthProperty(), listViewCell.getWidth()),
                        new KeyValue(detailedListView.prefWidthProperty(), listViewCell.getWidth()))
        );
        animation.play();
        slideAccordionIn();
    }

    private HBox buildCardHeader(String typeLabel, String networkLabel, String chipStyle) {
        Label typeChip = new Label(typeLabel);
        typeChip.getStyleClass().addAll("bc-chip", chipStyle);

        Label networkChip = new Label(networkLabel);
        networkChip.getStyleClass().addAll("bc-chip", "bc-chip-network");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label("›");
        arrow.getStyleClass().add("bc-card-arrow");

        HBox header = new HBox(8, typeChip, networkChip, spacer, arrow);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("bc-card-header");
        return header;
    }

    private HBox createMetricRow(String key, String value) {
        double rowWidth = detailContentWidth();
        double keyWidth = Math.min(150D, Math.max(112D, rowWidth * 0.24D));
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("analysisKey");
        keyLabel.setPrefWidth(keyWidth);
        keyLabel.setMinWidth(keyWidth);

        Label valueLabel = createAnalysisLabel(valueOrDash(value));
        valueLabel.setMaxWidth(Math.max(220D, rowWidth - keyWidth - 12D));
        valueLabel.getStyleClass().add("analysisValue");

        HBox row = new HBox(keyLabel, valueLabel);
        row.setSpacing(8);
        row.setMaxWidth(rowWidth);
        row.getStyleClass().add("analysisRow");
        return row;
    }

    private Label createTableCell(String text, double width) {
        Label label = new Label(valueOrDash(text));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPrefWidth(width);
        label.setMinWidth(width);
        label.setMaxWidth(width);
        label.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        return label;
    }

    private HBox createTableRow(String styleClass, double tableWidth, String[] values, double[] weights) {
        HBox row = new HBox();
        row.getStyleClass().add(styleClass);
        double total = 0D;
        for (double weight : weights) {
            total += weight;
        }
        for (int i = 0; i < values.length; i++) {
            double width = tableWidth / total * weights[Math.min(i, weights.length - 1)];
            row.getChildren().add(createTableCell(values[i], width));
        }
        return row;
    }

    private ListView<Node> createAnalysisTable(String[] headers, double[] weights, ArrayList<String[]> rows) {
        double tableWidth = Math.max(320D, detailContentWidth() - 10D);
        ListView<Node> listView = new ListView<>();
        listView.getStyleClass().add("tableListView");
        listView.setPrefWidth(tableWidth);
        listView.setMaxWidth(tableWidth);
        listView.setMinWidth(0);
        listView.setMaxHeight(Math.max(96, Math.min((rows.size() + 1) * 52, 420)));
        ObservableList<Node> items = FXCollections.observableArrayList();
        items.add(createTableRow("tableTitle", tableWidth, headers, weights));
        for (String[] row : rows) {
            items.add(createTableRow("tableContent", tableWidth, row, weights));
        }
        listView.setItems(items);
        return listView;
    }

    private JsonObject firstObject(JsonArray array) {
        if (array == null) {
            return null;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private String metricFromAny(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = objectValue(object, key);
            if (!value.equals("")) {
                return value;
            }
        }
        return "";
    }

    private void addObjectRows(VBox box, JsonObject object) {
        if (box == null || object == null) {
            return;
        }
        for (String key : object.keySet()) {
            box.getChildren().add(createMetricRow(key, objectValue(object, key)));
        }
    }

    private String jsonPretty(JsonObject object) {
        return object == null ? "" : new GsonBuilder().setPrettyPrinting().create().toJson(object);
    }

    private String jsonPretty(JsonArray array) {
        return array == null ? "" : new GsonBuilder().setPrettyPrinting().create().toJson(array);
    }

    private VBox createUnavailableBox(ArrayList<String> lines) {
        VBox box = new VBox();
        box.setSpacing(6);
        box.getStyleClass().add("analysisWarningBox");
        if (lines == null || lines.isEmpty()) {
            box.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.no.extra")));
            return box;
        }
        for (String line : lines) {
            box.getChildren().add(createAnalysisLabel("• " + line));
        }
        return box;
    }

    private Label createFindingLabel(JsonObject finding) {
        String title = objectValue(finding, "title");
        String description = objectValue(finding, "description");
        String evidence = objectValue(finding, "evidence");
        String text = "• " + valueOrDash(title);
        if (!description.equals("")) {
            text += "\n  " + description;
        }
        if (!evidence.equals("")) {
            text += "\n  " + I18nUtils.getString("blockchain.report.evidence") + "：" + evidence;
        }
        return createAnalysisLabel(text);
    }

    private void showBasicEvidenceDetail(JsonObject searchItem) {
        clearCurrentReportContext();
        openReportDetailPane();

        VBox analysisSection = createReportSectionBody(I18nUtils.getString("blockchain.basic.loading.analysis"));
        VBox overviewSection = createReportSectionBody(I18nUtils.getString("blockchain.basic.loading.overview"));
        VBox evidenceSection = createReportSectionBody(I18nUtils.getString("blockchain.basic.loading.evidence"));
        VBox rawSection = createReportSectionBody(I18nUtils.getString("blockchain.basic.loading.raw"));

        addReportSection(I18nUtils.getString("blockchain.basic.section.analysis"), analysisSection);
        addReportSection(I18nUtils.getString("blockchain.basic.section.overview"), overviewSection);
        addReportSection(I18nUtils.getString("blockchain.basic.section.evidence"), evidenceSection);
        addReportSection(I18nUtils.getString("blockchain.basic.section.raw"), rawSection);
        detailedListView.setItems(detailedContentObj);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String type = getAsString(searchItem, "type");
                JsonObject detailResponse = null;
                JsonObject extraResponse = null;
                JsonObject secondExtraResponse = null;
                if ("address".equals(type)) {
                    String network = getAsString(searchItem, "network");
                    String addressHash = getAsString(searchItem, "hash");
                    detailResponse = address(network, addressHash);
                    extraResponse = tokenbalance(network, addressHash);
                    secondExtraResponse = addressTransaction(network, addressHash, "1", "20");
                } else if ("block".equals(type)) {
                    String network = getAsString(searchItem, "network");
                    String blockNo = getAsString(searchItem, "block_no");
                    detailResponse = block(network, blockNo);
                    extraResponse = getTxData(network, blockNo, "1", "20");
                    secondExtraResponse = getTokentransferData(network, blockNo, "1", "20");
                } else if ("tx".equals(type)) {
                    String network = getAsString(searchItem, "network");
                    String txid = getAsString(searchItem, "txid");
                    detailResponse = txDetail(network, txid);
                }
                final JsonObject detail = detailResponse;
                final JsonObject extra = extraResponse;
                final JsonObject secondExtra = secondExtraResponse;
                Platform.runLater(() -> {
                    fillBasicAnalysisSection(analysisSection, searchItem, detail, extra, secondExtra);
                    fillBasicOverviewSection(overviewSection, searchItem, detail);
                    fillBasicEvidenceSection(evidenceSection, searchItem, extra, secondExtra);
                    fillBasicRawSection(rawSection, searchItem, detail, extra, secondExtra);
                });
                return null;
            }
        };
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String message = error == null ? I18nUtils.getString("blockchain.error.query") : error.getMessage();
            Platform.runLater(() -> {
                replaceSection(analysisSection, createAnalysisLabel(message));
                replaceSection(overviewSection, createAnalysisLabel(I18nUtils.getString("blockchain.basic.no.overview")));
                replaceSection(evidenceSection, createAnalysisLabel(I18nUtils.getString("blockchain.basic.no.extra")));
                replaceSection(rawSection, createDrawer(I18nUtils.getString("blockchain.report.drawer.raw"), createRawTextArea(jsonPretty(searchItem))));
                showWarningDialog(message);
            });
            if (debugMode && error != null) {
                error.printStackTrace();
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showReportFailureState(String reportType, String network, String target, String message) {
        clearCurrentReportContext();
        openReportDetailPane();
        VBox statusSection = createReportSectionBody("");
        VBox rawSection = createReportSectionBody("");
        addReportSection(I18nUtils.getString("blockchain.report.section.failure"), statusSection);
        addReportSection(I18nUtils.getString("blockchain.basic.section.raw"), rawSection);

        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.basic.object.type"), objectTypeLabel(reportType)));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.network"), network));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.export.meta.target"), target));
        nodes.add(createAnalysisLabel(valueOrDash(message)));
        nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.failure.next")));
        replaceSection(statusSection, nodes);

        JsonObject raw = new JsonObject();
        raw.addProperty("type", reportType);
        raw.addProperty("network", network);
        raw.addProperty("target", target);
        raw.addProperty("message", message);
        replaceSection(rawSection, createDrawer(I18nUtils.getString("blockchain.report.drawer.raw"), createRawTextArea(jsonPretty(raw))));
        detailedListView.setItems(detailedContentObj);
        showWarningDialog(message);
    }

    private void fillBasicAnalysisSection(VBox section, JsonObject searchItem, JsonObject detail, JsonObject extra, JsonObject secondExtra) {
        ArrayList<Node> nodes = new ArrayList<>();
        String type = getAsString(searchItem, "type");
        String network = getAsString(searchItem, "network");
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.basic.object.type"), objectTypeLabel(type)));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.network"), network.toUpperCase()));

        if ("address".equals(type)) {
            nodes.add(createAnalysisLabel("• " + I18nUtils.getString("blockchain.basic.analysis.address")));
        } else if ("block".equals(type)) {
            nodes.add(createAnalysisLabel("• " + I18nUtils.getString("blockchain.basic.analysis.block")));
        } else if ("tx".equals(type)) {
            nodes.add(createAnalysisLabel("• " + I18nUtils.getString("blockchain.basic.analysis.tx")));
        }

        ArrayList<String> warnings = new ArrayList<>();
        collectResponseWarning(warnings, detail);
        collectResponseWarning(warnings, extra);
        collectResponseWarning(warnings, secondExtra);
        if (!supportsEvmReport(network)) {
            warnings.add(I18nUtils.getString("blockchain.basic.non.evm.note"));
        }
        if (!warnings.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.coverage.title")));
            nodes.add(createUnavailableBox(warnings));
        }
        replaceSection(section, nodes);
    }

    private String objectTypeLabel(String type) {
        if ("transaction".equals(type)) {
            return I18nUtils.getString("blockchain.basic.type.tx");
        }
        if ("address".equals(type)) {
            return I18nUtils.getString("blockchain.basic.type.address");
        }
        if ("block".equals(type)) {
            return I18nUtils.getString("blockchain.basic.type.block");
        }
        if ("tx".equals(type)) {
            return I18nUtils.getString("blockchain.basic.type.tx");
        }
        return valueOrDash(type);
    }

    private void collectResponseWarning(ArrayList<String> warnings, JsonObject response) {
        if (warnings == null || response == null || hasData(response)) {
            return;
        }
        String message = getResponseMessage(response);
        if (!message.equals("") && !warnings.contains(message)) {
            warnings.add(message);
        }
    }

    private void fillBasicOverviewSection(VBox section, JsonObject searchItem, JsonObject detail) {
        VBox box = new VBox();
        box.setSpacing(6);
        String type = getAsString(searchItem, "type");
        JsonObject data = responseDataAllowObject(detail);
        JsonObject baseInfo = getAsObject(data, "baseInfo");

        if ("address".equals(type)) {
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.address"), getAsString(searchItem, "hash")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.balance"), valueWithToken(getAsString(searchItem, "balance"), getAsString(searchItem, "network").toUpperCase())));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.txcount"), getAsString(searchItem, "txCount")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.basic.address.alias"), getAsString(searchItem, "addrAlias")));
        } else if ("block".equals(type)) {
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.block"), getAsString(searchItem, "block_no")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.basic.block.hash"), getAsString(searchItem, "blockhash")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.time"), formatEpochSecond(searchItem, "time")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.txcount"), getAsString(searchItem, "txCnt")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.fee"), getAsString(searchItem, "fee")));
        } else if ("tx".equals(type)) {
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.basic.tx.hash"), getAsString(searchItem, "txid")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.status"), getAsString(searchItem, "status")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.block"), getAsString(searchItem, "block_no")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.time"), formatEpochSecond(searchItem, "time")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.from"), getAsString(searchItem, "from")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.to"), getAsString(searchItem, "to")));
            box.getChildren().add(createMetricRow(I18nUtils.getString("blockchain.report.fee"), getAsString(searchItem, "fee")));
        }
        if (baseInfo != null) {
            addObjectRows(box, baseInfo);
        }
        if (box.getChildren().isEmpty()) {
            box.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.no.overview")));
        }
        replaceSection(section, box);
    }

    private JsonObject responseDataAllowObject(JsonObject response) {
        if (!hasData(response) || !response.has("data") || !response.get("data").isJsonObject()) {
            return null;
        }
        return response.getAsJsonObject("data");
    }

    private void fillBasicEvidenceSection(VBox section, JsonObject searchItem, JsonObject extra, JsonObject secondExtra) {
        ArrayList<Node> nodes = new ArrayList<>();
        String type = getAsString(searchItem, "type");
        if ("address".equals(type)) {
            appendAddressEvidence(nodes, extra, secondExtra);
        } else if ("block".equals(type)) {
            appendBlockEvidence(nodes, extra, secondExtra);
        } else if ("tx".equals(type)) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.tx.no.report")));
        }
        if (nodes.isEmpty()) {
            ArrayList<String> warnings = new ArrayList<>();
            collectResponseWarning(warnings, extra);
            collectResponseWarning(warnings, secondExtra);
            if (!warnings.isEmpty()) {
                nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.coverage.title")));
                nodes.add(createUnavailableBox(warnings));
            } else {
                nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.no.extra")));
            }
        }
        replaceSection(section, nodes);
    }

    private void appendAddressEvidence(ArrayList<Node> nodes, JsonObject tokenResponse, JsonObject txResponse) {
        JsonArray tokens = getAsArray(tokenResponse, "data");
        if (tokens != null && tokens.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(tokens.size(), 20); i++) {
                JsonObject item = tokens.get(i).isJsonObject() ? tokens.get(i).getAsJsonObject() : null;
                JsonObject tokenInfo = getAsObject(item, "tokenInfo");
                if (item == null || tokenInfo == null) {
                    continue;
                }
                String symbol = objectValue(tokenInfo, "s");
                String name = objectValue(tokenInfo, "f");
                String balance = formatTokenBalance(objectValue(item, "balance"), objectValue(tokenInfo, "d"));
                rows.add(new String[]{name, valueWithToken(balance, symbol), objectValue(item, "transferCnt")});
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.tokens.title")));
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.table.fullname"), I18nUtils.getString("blockchain.table.balance"), I18nUtils.getString("blockchain.table.txdetail")},
                        new double[]{0.38, 0.34, 0.28},
                        rows
                ));
            }
        }

        JsonArray txs = getAsArray(txResponse, "data");
        if (txs != null && txs.size() > 0) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.transactions.title")));
            nodes.add(createTransactionTable(txs));
        }
        addCoverageNotice(nodes, tokenResponse, I18nUtils.getString("blockchain.basic.token.transfer.limit"));
    }

    private void addCoverageNotice(ArrayList<Node> nodes, JsonObject response, String fallbackMessage) {
        if (nodes == null || response == null || hasData(response)) {
            return;
        }
        String message = getResponseMessage(response);
        if (message.equals("") || message.equals(I18nUtils.getString("blockchain.error.query"))) {
            message = fallbackMessage;
        }
        if (message.equals("")) {
            return;
        }
        nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.coverage.title")));
        ArrayList<String> lines = new ArrayList<>();
        lines.add(message);
        nodes.add(createUnavailableBox(lines));
    }

    private String formatTokenBalance(String rawBalance, String decimalsText) {
        try {
            int decimals = decimalsText == null || decimalsText.trim().isEmpty() ? 0 : Integer.parseInt(decimalsText);
            return new BigDecimal(rawBalance == null || rawBalance.trim().isEmpty() ? "0" : rawBalance)
                    .divide(BigDecimal.TEN.pow(Math.max(decimals, 0)))
                    .toPlainString();
        } catch (Exception e) {
            return valueOrDash(rawBalance);
        }
    }

    private void appendBlockEvidence(ArrayList<Node> nodes, JsonObject txResponse, JsonObject tokenResponse) {
        JsonArray txs = getAsArray(txResponse, "data");
        if (txs != null && txs.size() > 0) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.transactions.title")));
            nodes.add(createTransactionTable(txs));
        }
        JsonObject groupedTransfers = getAsObject(tokenResponse, "data");
        if (groupedTransfers != null && groupedTransfers.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : groupedTransfers.entrySet()) {
                if (entry.getValue() == null || !entry.getValue().isJsonArray()) {
                    continue;
                }
                JsonArray transfers = entry.getValue().getAsJsonArray();
                for (int i = 0; i < Math.min(transfers.size(), 20); i++) {
                    JsonElement element = transfers.get(i);
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject transfer = element.getAsJsonObject();
                    rows.add(new String[]{
                            entry.getKey(),
                            formatEpochSecond(transfer, "time"),
                            objectValue(transfer, "from"),
                            objectValue(transfer, "to"),
                            valueWithToken(objectValue(transfer, "value"), objectValue(transfer, "tokenSymbol"))
                    });
                }
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.basic.token.transfers.title")));
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.table.txhash"), I18nUtils.getString("blockchain.table.time"), I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.table.amount")},
                        new double[]{0.22, 0.16, 0.22, 0.22, 0.18},
                        rows
                ));
            }
        }
        addCoverageNotice(nodes, tokenResponse, I18nUtils.getString("blockchain.basic.token.transfer.limit"));
    }

    private ListView<Node> createTransactionTable(JsonArray txs) {
        ArrayList<String[]> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(txs.size(), 30); i++) {
            JsonElement element = txs.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject tx = element.getAsJsonObject();
            rows.add(new String[]{
                    metricFromAny(tx, "txid", "hash"),
                    metricFromAny(tx, "methodId", "funcid", "status"),
                    metricFromAny(tx, "block_no", "block"),
                    !objectValue(tx, "timeText").equals("") ? objectValue(tx, "timeText") : formatEpochSecond(tx, "time"),
                    objectValue(tx, "from"),
                    objectValue(tx, "to"),
                    valueWithToken(objectValue(tx, "value"), objectValue(tx, "nativeToken")),
                    valueWithToken(objectValue(tx, "fee"), objectValue(tx, "network"))
            });
        }
        return createAnalysisTable(
                new String[]{I18nUtils.getString("blockchain.table.txhash"), I18nUtils.getString("blockchain.table.function"), I18nUtils.getString("blockchain.table.blockheight"), I18nUtils.getString("blockchain.table.time"), I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.table.amount"), I18nUtils.getString("blockchain.table.fee")},
                new double[]{0.18, 0.10, 0.10, 0.14, 0.16, 0.16, 0.08, 0.08},
                rows
        );
    }

    private void fillBasicRawSection(VBox section, JsonObject searchItem, JsonObject detail, JsonObject extra, JsonObject secondExtra) {
        VBox box = new VBox();
        box.setSpacing(8);
        box.getChildren().add(createDrawer(I18nUtils.getString("blockchain.basic.drawer.search.raw"), createRawTextArea(jsonPretty(searchItem))));
        if (detail != null) {
            box.getChildren().add(createDrawer(I18nUtils.getString("blockchain.basic.drawer.detail.raw"), createRawTextArea(jsonPretty(detail))));
        }
        if (extra != null) {
            box.getChildren().add(createDrawer(I18nUtils.getString("blockchain.basic.drawer.extra.raw"), createRawTextArea(jsonPretty(extra))));
        }
        if (secondExtra != null) {
            box.getChildren().add(createDrawer(I18nUtils.getString("blockchain.basic.drawer.more.raw"), createRawTextArea(jsonPretty(secondExtra))));
        }
        replaceSection(section, box);
    }

    private TitledPane createDrawer(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.getStyleClass().add("analysisDrawer");
        return pane;
    }

    private TextArea createRawTextArea(String text) {
        TextArea textArea = new TextArea(text == null ? "" : text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(14);
        textArea.getStyleClass().add("analysisRawText");
        return textArea;
    }

    private JsonObject responseData(JsonObject response) {
        if (!hasData(response) || !response.has("data") || !response.get("data").isJsonObject()) {
            return null;
        }
        return response.getAsJsonObject("data");
    }

    private String formatCompleteness(JsonObject meta) {
        JsonObject completeness = getAsObject(meta, "dataCompleteness");
        if (completeness == null) {
            return "";
        }
        String description = objectValue(completeness, "description");
        if (!description.equals("")) {
            return description;
        }
        return I18nUtils.getString(
                "blockchain.report.completeness.value",
                valueOrDash(objectValue(completeness, "level")),
                valueOrDash(objectValue(completeness, "score"))
        );
    }

    private void fillAnalysisSection(VBox section, JsonObject reportData) {
        JsonObject meta = getAsObject(reportData, "meta");
        JsonObject processed = getAsObject(reportData, "processedAnalysis");
        JsonArray summaries = getAsArray(processed, "summary");
        ArrayList<Node> nodes = new ArrayList<>();

        String completeness = formatCompleteness(meta);
        if (!completeness.equals("")) {
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.completeness"), completeness));
        }

        if (summaries != null && summaries.size() > 0) {
            for (int i = 0; i < summaries.size(); i++) {
                nodes.add(createAnalysisLabel("• " + summaries.get(i).getAsString()));
            }
        } else {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.analysis")));
        }

        JsonArray warnings = getAsArray(reportData, "warnings");
        if (warnings != null && warnings.size() > 0) {
            VBox warningBox = new VBox();
            warningBox.setSpacing(6);
            warningBox.getStyleClass().add("analysisWarningBox");
            warningBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.warning.title")));
            for (int i = 0; i < Math.min(warnings.size(), 5); i++) {
                warningBox.getChildren().add(createAnalysisLabel("• " + warnings.get(i).getAsString()));
            }
            nodes.add(warningBox);
        }
        replaceSection(section, nodes);
    }

    private JsonObject incidentNarrative(JsonObject reportData) {
        return getAsObject(reportData, "incidentNarrative");
    }

    private void fillNarrativeMissing(VBox section) {
        replaceSection(section, createAnalysisLabel(I18nUtils.getString("blockchain.report.no.narrative")));
    }

    private void fillInvestigationSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        ArrayList<Node> nodes = new ArrayList<>();
        if (narrative != null) {
            String title = objectValue(narrative, "title");
            if (!title.equals("")) {
                nodes.add(createAnalysisHeadline(title));
            }
            String subtitle = objectValue(narrative, "subtitle");
            if (!subtitle.equals("")) {
                nodes.add(createAnalysisLabel(subtitle));
            }
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.narrative.statement"),
                    objectValue(narrative, "statementType")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.narrative.classification"),
                    objectValue(narrative, "classification")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.evidence.malicious"),
                    objectValue(narrative, "maliciousConclusion")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.evidence.confidence"),
                    objectValue(narrative, "confidence")));
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.narrative.executive"),
                    collectTextLines(getAsArray(narrative, "executiveSummary")), 8);
        }
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillEntitiesSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        JsonArray entities = getAsArray(narrative, "involvedEntities");
        ArrayList<Node> nodes = new ArrayList<>();
        if (entities != null && entities.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(entities.size(), 18); i++) {
                JsonElement element = entities.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject entity = element.getAsJsonObject();
                rows.add(new String[]{
                        shortIdentifier(objectValue(entity, "address")),
                        objectValue(entity, "role"),
                        objectValue(entity, "entityType"),
                        objectValue(entity, "confidence"),
                        objectValue(entity, "evidence")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{
                                I18nUtils.getString("blockchain.report.address"),
                                I18nUtils.getString("blockchain.report.role"),
                                I18nUtils.getString("blockchain.report.entity.type"),
                                I18nUtils.getString("blockchain.report.evidence.confidence"),
                                I18nUtils.getString("blockchain.report.evidence")
                        },
                        new double[]{0.20, 0.26, 0.16, 0.12, 0.26},
                        rows
                ));
            }
        }
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillAttackStepsSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        JsonArray steps = getAsArray(narrative, "attackSteps");
        ArrayList<Node> nodes = new ArrayList<>();
        if (steps != null && steps.size() > 0) {
            for (int i = 0; i < Math.min(steps.size(), 18); i++) {
                JsonElement element = steps.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject step = element.getAsJsonObject();
                String text = valueOrDash(objectValue(step, "order")) + ". "
                        + valueOrDash(objectValue(step, "stage"))
                        + "\n  " + valueOrDash(objectValue(step, "action"))
                        + "\n  " + I18nUtils.getString("blockchain.report.evidence") + "："
                        + valueOrDash(objectValue(step, "evidence"));
                String note = objectValue(step, "note");
                if (!note.equals("")) {
                    text += "\n  " + note;
                }
                nodes.add(createAnalysisLabel(text));
            }
        }
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillAssetMovementSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        JsonArray movements = getAsArray(narrative, "assetMovementDetails");
        JsonObject impact = getAsObject(narrative, "impactEstimate");
        ArrayList<Node> nodes = new ArrayList<>();
        if (impact != null) {
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.impact.scope"), objectValue(impact, "scope")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.impact.transfer.count"), objectValue(impact, "transferCount")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.impact.swap.count"), objectValue(impact, "swapCount")));
            String boundary = objectValue(impact, "boundary");
            if (!boundary.equals("")) {
                nodes.add(createAnalysisLabel(boundary));
            }
        }
        if (movements != null && movements.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(movements.size(), 24); i++) {
                JsonElement element = movements.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject movement = element.getAsJsonObject();
                rows.add(new String[]{
                        objectValue(movement, "direction"),
                        valueWithToken(objectValue(movement, "amount"), objectValue(movement, "asset")),
                        shortIdentifier(objectValue(movement, "source")),
                        shortIdentifier(objectValue(movement, "target")),
                        objectValue(movement, "evidence")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{
                                I18nUtils.getString("blockchain.report.movement.direction"),
                                I18nUtils.getString("blockchain.table.amount"),
                                I18nUtils.getString("blockchain.table.from"),
                                I18nUtils.getString("blockchain.table.to"),
                                I18nUtils.getString("blockchain.report.evidence")
                        },
                        new double[]{0.20, 0.22, 0.20, 0.20, 0.18},
                        rows
                ));
            }
        }
        appendNamedLines(nodes, I18nUtils.getString("blockchain.report.impact.notes"),
                collectTextLines(getAsArray(impact, "notes")), 6);
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillNarrativeDiagramSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        JsonObject fundFlow = getAsObject(narrative, "fundFlowDiagram");
        JsonObject interaction = getAsObject(narrative, "interactionDiagram");
        ArrayList<Node> nodes = new ArrayList<>();

        JsonArray edges = getAsArray(fundFlow, "edges");
        if (edges != null && edges.size() > 0) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.diagram.fund")));
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(edges.size(), 30); i++) {
                JsonElement element = edges.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject edge = element.getAsJsonObject();
                rows.add(new String[]{
                        shortIdentifier(objectValue(edge, "source")),
                        shortIdentifier(objectValue(edge, "target")),
                        valueWithToken(objectValue(edge, "amount"), objectValue(edge, "symbol")),
                        objectValue(edge, "category"),
                        objectValue(edge, "evidence")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.table.amount"), I18nUtils.getString("blockchain.basic.category"), I18nUtils.getString("blockchain.report.evidence")},
                        new double[]{0.20, 0.20, 0.22, 0.16, 0.22},
                        rows
                ));
            }
        }

        JsonArray messages = getAsArray(interaction, "messages");
        if (messages != null && messages.size() > 0) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.diagram.interaction")));
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(messages.size(), 30); i++) {
                JsonElement element = messages.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject message = element.getAsJsonObject();
                rows.add(new String[]{
                        objectValue(message, "order"),
                        shortIdentifier(objectValue(message, "from")),
                        shortIdentifier(objectValue(message, "to")),
                        objectValue(message, "label")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.report.order"), I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.report.interaction")},
                        new double[]{0.10, 0.20, 0.20, 0.50},
                        rows
                ));
            }
        }

        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillAttackAnalysisSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        JsonArray analysis = getAsArray(narrative, "attackAnalysis");
        ArrayList<Node> nodes = new ArrayList<>();
        if (analysis != null && analysis.size() > 0) {
            for (int i = 0; i < Math.min(analysis.size(), 18); i++) {
                JsonElement element = analysis.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String text = "• [" + valueOrDash(objectValue(item, "level")) + "] "
                        + valueOrDash(objectValue(item, "title"))
                        + "\n  " + valueOrDash(objectValue(item, "description"))
                        + "\n  " + I18nUtils.getString("blockchain.report.pattern.confidence") + "："
                        + valueOrDash(objectValue(item, "confidence"))
                        + "\n  " + I18nUtils.getString("blockchain.report.evidence") + "："
                        + valueOrDash(objectValue(item, "evidence"));
                nodes.add(createAnalysisLabel(text));
            }
        }
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillConclusionSection(VBox section, JsonObject reportData) {
        JsonObject narrative = incidentNarrative(reportData);
        ArrayList<Node> nodes = new ArrayList<>();
        if (narrative != null) {
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.narrative.conclusion"),
                    collectTextLines(getAsArray(narrative, "conclusion")), 12);
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.narrative.recommendations"),
                    collectTextLines(getAsArray(narrative, "recommendations")), 12);
        }
        if (nodes.isEmpty()) {
            fillNarrativeMissing(section);
            return;
        }
        replaceSection(section, nodes);
    }

    private void fillFindingsSection(VBox section, JsonObject reportData) {
        JsonArray findings = getAsArray(reportData, "keyFindings");
        ArrayList<Node> nodes = new ArrayList<>();
        if (findings != null && findings.size() > 0) {
            for (int i = 0; i < findings.size(); i++) {
                JsonElement element = findings.get(i);
                if (element != null && element.isJsonObject()) {
                    nodes.add(createFindingLabel(element.getAsJsonObject()));
                }
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.findings")));
        }
        replaceSection(section, nodes);
    }

    private void fillTransactionSection(VBox section, JsonObject reportData) {
        JsonObject tx = getAsObject(reportData, "transaction");
        JsonObject summary = getAsObject(reportData, "summary");
        if (tx == null && getAsObject(reportData, "address") != null) {
            fillAddressOverviewSection(section, reportData);
            return;
        }
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.network"), objectValue(summary, "network")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.status"), objectValue(tx, "status")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.block"), objectValue(tx, "block_no")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.time"), objectValue(tx, "timeText")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.from"), objectValue(tx, "from")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.to"), objectValue(tx, "to")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.value"), objectValue(tx, "value") + " " + objectValue(tx, "nativeToken")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.fee"), objectValue(tx, "fee") + " " + objectValue(tx, "nativeToken")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.method"), objectValue(tx, "methodId")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.gas"), objectValue(tx, "gasUsed") + "/" + objectValue(tx, "gas")));
        replaceSection(section, nodes);
    }

    private void fillAddressOverviewSection(VBox section, JsonObject reportData) {
        JsonObject summary = getAsObject(reportData, "summary");
        JsonObject address = getAsObject(reportData, "address");
        JsonObject contract = getAsObject(reportData, "contract");
        JsonObject token = getAsObject(contract, "token");
        JsonObject portfolio = getAsObject(reportData, "portfolio");
        JsonArray assets = getAsArray(portfolio, "assets");

        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.network"), objectValue(summary, "network")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.address"), objectValue(summary, "address")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.address.type"), objectValue(summary, "addressType")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.balance"), objectValue(summary, "balance")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.txcount"), objectValue(summary, "txCount")));
        nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.iscontract"), objectBooleanValue(summary, "isContract")));

        String codeSize = objectValue(contract, "codeSize");
        if (!codeSize.equals("")) {
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.code.size"), codeSize));
        }
        if (token != null) {
            String tokenName = objectValue(token, "name");
            String tokenSymbol = objectValue(token, "symbol");
            if (!tokenName.equals("") || !tokenSymbol.equals("")) {
                nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.token"), valueOrDash(tokenName) + " / " + valueOrDash(tokenSymbol)));
            }
            String decimals = objectValue(token, "decimals");
            if (!decimals.equals("")) {
                nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.token.decimals"), decimals));
            }
            String totalSupply = objectValue(token, "totalSupplyFormatted");
            if (totalSupply.equals("")) {
                totalSupply = objectValue(token, "totalSupply");
            }
            if (!totalSupply.equals("")) {
                nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.token.supply"), valueWithToken(totalSupply, tokenSymbol)));
            }
        }
        if (assets != null && assets.size() > 0) {
            VBox assetBox = new VBox();
            assetBox.setSpacing(6);
            assetBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.asset.list")));
            for (int i = 0; i < Math.min(assets.size(), 12); i++) {
                JsonElement element = assets.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject asset = element.getAsJsonObject();
                assetBox.getChildren().add(createAnalysisLabel("• "
                        + valueWithToken(objectValue(asset, "amount"), objectValue(asset, "symbol"))
                        + " [" + valueOrDash(objectValue(asset, "assetType")) + "]"));
            }
            nodes.add(assetBox);
        }
        if (nodes.size() <= 1 && address != null) {
            for (String key : address.keySet()) {
                nodes.add(createMetricRow(key, objectValue(address, key)));
            }
        }
        replaceSection(section, nodes);
    }

    private void fillRolesSection(VBox section, JsonObject reportData) {
        JsonArray roles = getAsArray(reportData, "addressRoles");
        ArrayList<Node> nodes = new ArrayList<>();
        if (roles != null) {
            for (int i = 0; i < Math.min(roles.size(), 18); i++) {
                JsonElement element = roles.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject role = element.getAsJsonObject();
                String address = objectValue(role, "address");
                String label = objectValue(role, "label");
                String type = objectValue(role, "type");
                JsonArray roleNames = getAsArray(role, "roles");
                ArrayList<String> roleText = new ArrayList<>();
                if (roleNames != null) {
                    for (int j = 0; j < roleNames.size(); j++) {
                        roleText.add(roleNames.get(j).getAsString());
                    }
                }
                nodes.add(createAnalysisLabel("• " + valueOrDash(label) + " [" + valueOrDash(type) + "]\n  "
                        + address + "\n  " + I18nUtils.getString("blockchain.report.role") + "："
                        + String.join(", ", roleText)));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.roles")));
        }
        replaceSection(section, nodes);
    }

    private void fillTimelineSection(VBox section, JsonObject reportData) {
        JsonArray timeline = getAsArray(reportData, "timeline");
        ArrayList<Node> nodes = new ArrayList<>();
        if (timeline != null) {
            for (int i = 0; i < Math.min(timeline.size(), 30); i++) {
                JsonElement element = timeline.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String title = objectValue(item, "title");
                String description = objectValue(item, "description");
                String evidence = objectValue(item, "evidence");
                nodes.add(createAnalysisLabel((i + 1) + ". " + valueOrDash(title)
                        + "\n  " + valueOrDash(description)
                        + "\n  " + I18nUtils.getString("blockchain.report.evidence") + "：" + valueOrDash(evidence)));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.timeline")));
        }
        replaceSection(section, nodes);
    }

    private void fillFlowSection(VBox section, JsonObject reportData) {
        JsonObject graph = getAsObject(reportData, "fundFlowGraph");
        JsonArray edges = getAsArray(graph, "edges");
        ArrayList<Node> nodes = new ArrayList<>();
        if (edges != null && edges.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(edges.size(), 30); i++) {
                JsonElement element = edges.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject edge = element.getAsJsonObject();
                rows.add(new String[]{
                        objectValue(edge, "source"),
                        objectValue(edge, "target"),
                        valueWithToken(objectValue(edge, "amount"), objectValue(edge, "symbol")),
                        objectValue(edge, "category"),
                        objectValue(edge, "evidence")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.table.amount"), I18nUtils.getString("blockchain.basic.category"), I18nUtils.getString("blockchain.report.evidence")},
                        new double[]{0.24, 0.24, 0.18, 0.12, 0.22},
                        rows
                ));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.flow")));
        }
        replaceSection(section, nodes);
    }

    private void fillSequenceSection(VBox section, JsonObject reportData) {
        JsonObject sequence = getAsObject(reportData, "sequenceDiagram");
        JsonArray messages = getAsArray(sequence, "messages");
        ArrayList<Node> nodes = new ArrayList<>();
        if (messages != null && messages.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(messages.size(), 30); i++) {
                JsonElement element = messages.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject message = element.getAsJsonObject();
                rows.add(new String[]{
                        objectValue(message, "order"),
                        objectValue(message, "from"),
                        objectValue(message, "to"),
                        objectValue(message, "label")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.report.order"), I18nUtils.getString("blockchain.table.from"), I18nUtils.getString("blockchain.table.to"), I18nUtils.getString("blockchain.report.interaction")},
                        new double[]{0.10, 0.25, 0.25, 0.40},
                        rows
                ));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.sequence")));
        }
        replaceSection(section, nodes);
    }

    private void fillPatternSection(VBox section, JsonObject reportData) {
        JsonObject semantic = getAsObject(reportData, "semanticAnalysis");
        JsonArray patterns = getAsArray(semantic, "riskPatterns");
        ArrayList<Node> nodes = new ArrayList<>();
        if (patterns != null && patterns.size() > 0) {
            ArrayList<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(patterns.size(), 12); i++) {
                JsonElement element = patterns.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject pattern = element.getAsJsonObject();
                rows.add(new String[]{
                        objectValue(pattern, "level"),
                        objectValue(pattern, "title"),
                        objectValue(pattern, "confidence"),
                        objectValue(pattern, "description"),
                        objectValue(pattern, "evidence")
                });
            }
            if (!rows.isEmpty()) {
                nodes.add(createAnalysisTable(
                        new String[]{
                                I18nUtils.getString("blockchain.report.pattern.level"),
                                I18nUtils.getString("blockchain.report.pattern.name"),
                                I18nUtils.getString("blockchain.report.pattern.confidence"),
                                I18nUtils.getString("blockchain.report.pattern.description"),
                                I18nUtils.getString("blockchain.report.evidence")
                        },
                        new double[]{0.10, 0.18, 0.12, 0.42, 0.18},
                        rows
                ));
            }
        }

        JsonObject assetActivity = getAsObject(semantic, "assetActivity");
        if (assetActivity != null) {
            VBox summaryBox = new VBox();
            summaryBox.setSpacing(6);
            summaryBox.getStyleClass().add("analysisWarningBox");
            summaryBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.pattern.activity")));
            JsonObject eventTypeCounts = getAsObject(assetActivity, "eventTypeCounts");
            JsonObject tokenFamilyCounts = getAsObject(assetActivity, "tokenFamilyCounts");
            if (eventTypeCounts != null) {
                summaryBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.pattern.event.types")
                        + "：" + compactObject(eventTypeCounts, 8)));
            }
            if (tokenFamilyCounts != null) {
                summaryBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.pattern.token.families")
                        + "：" + compactObject(tokenFamilyCounts, 8)));
            }
            nodes.add(summaryBox);
        }

        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.patterns")));
        }
        replaceSection(section, nodes);
    }

    private void fillEvidenceAssessmentSection(VBox section, JsonObject reportData) {
        JsonObject semantic = getAsObject(reportData, "semanticAnalysis");
        JsonObject assessment = getAsObject(semantic, "evidenceAssessment");
        ArrayList<Node> nodes = new ArrayList<>();
        if (assessment != null) {
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.evidence.classification"),
                    objectValue(assessment, "classification")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.evidence.confidence"),
                    objectValue(assessment, "confidence")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.evidence.malicious"),
                    objectValue(assessment, "maliciousConclusion")));
            JsonObject publicIncident = getAsObject(assessment, "publicIncident");
            if (publicIncident != null) {
                nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.public.incident"),
                        objectValue(publicIncident, "title")));
                nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.public.scenario"),
                        objectValue(publicIncident, "scenario")));
                String basis = objectValue(publicIncident, "basis");
                if (!basis.equals("")) {
                    nodes.add(createAnalysisLabel(basis));
                }
                appendNamedLines(nodes, I18nUtils.getString("blockchain.report.public.references"),
                        collectTextLines(getAsArray(publicIncident, "references")), 6);
            }
            String conclusionNote = objectValue(assessment, "maliciousConclusionNote");
            if (!conclusionNote.equals("")) {
                nodes.add(createAnalysisLabel(conclusionNote));
            }
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.evidence.confirmed"),
                    collectTextLines(getAsArray(assessment, "confirmedFacts")), 8);
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.evidence.inferences"),
                    collectTextLines(getAsArray(assessment, "reasonableInferences")), 8);
            appendNamedLines(nodes, I18nUtils.getString("blockchain.report.evidence.gaps"),
                    collectTextLines(getAsArray(assessment, "evidenceGaps")), 10);
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.evidence.assessment")));
        }
        replaceSection(section, nodes);
    }

    private void fillContextSection(VBox section, JsonObject reportData) {
        JsonObject semantic = getAsObject(reportData, "semanticAnalysis");
        JsonObject context = getAsObject(semantic, "contextWindow");
        ArrayList<Node> nodes = new ArrayList<>();
        if (context != null) {
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.context.scope"), objectValue(context, "scope")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.context.status"), objectValue(context, "status")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.block"), objectValue(context, "block")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.context.inspected"), objectValue(context, "inspectedTxCount")));
            nodes.add(createMetricRow(I18nUtils.getString("blockchain.report.context.related"), objectValue(context, "relatedTxCount")));
            String note = objectValue(context, "note");
            if (!note.equals("")) {
                nodes.add(createAnalysisLabel(note));
            }
            JsonArray related = getAsArray(context, "relatedTransactions");
            if (related != null && related.size() > 0) {
                ArrayList<String[]> rows = new ArrayList<>();
                for (int i = 0; i < Math.min(related.size(), 12); i++) {
                    JsonElement element = related.get(i);
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = element.getAsJsonObject();
                    rows.add(new String[]{
                            objectValue(item, "transactionIndex"),
                            objectValue(item, "txid"),
                            objectValue(item, "status"),
                            objectValue(item, "overlapCount"),
                            objectValue(item, "evidence")
                    });
                }
                if (!rows.isEmpty()) {
                    nodes.add(createAnalysisTable(
                            new String[]{
                                    I18nUtils.getString("blockchain.report.order"),
                                    I18nUtils.getString("blockchain.table.txhash"),
                                    I18nUtils.getString("blockchain.report.status"),
                                    I18nUtils.getString("blockchain.report.context.overlap"),
                                    I18nUtils.getString("blockchain.report.evidence")
                            },
                            new double[]{0.08, 0.42, 0.12, 0.12, 0.26},
                            rows
                    ));
                }
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.context")));
        }
        replaceSection(section, nodes);
    }

    private void fillRiskSection(VBox section, JsonObject reportData) {
        JsonArray flags = getAsArray(reportData, "riskFlags");
        ArrayList<Node> nodes = new ArrayList<>();
        if (flags != null) {
            for (int i = 0; i < flags.size(); i++) {
                JsonElement element = flags.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject flag = element.getAsJsonObject();
                nodes.add(createAnalysisLabel("• [" + valueOrDash(objectValue(flag, "level")) + "] "
                        + valueOrDash(objectValue(flag, "title"))
                        + "\n  " + valueOrDash(objectValue(flag, "description"))
                        + "\n  " + I18nUtils.getString("blockchain.report.evidence") + "："
                        + valueOrDash(objectValue(flag, "evidence"))));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.risk")));
        }
        replaceSection(section, nodes);
    }

    private void fillFollowUpSection(VBox section, JsonObject reportData) {
        JsonObject processed = getAsObject(reportData, "processedAnalysis");
        JsonArray suggestions = getAsArray(processed, "followUpSuggestions");
        ArrayList<Node> nodes = new ArrayList<>();
        if (suggestions != null) {
            for (int i = 0; i < suggestions.size(); i++) {
                JsonElement element = suggestions.get(i);
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                String text = element.isJsonPrimitive() ? element.getAsString() : element.toString();
                if (!text.trim().equals("")) {
                    nodes.add(createAnalysisLabel("• " + text.trim()));
                }
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.followup")));
        }
        replaceSection(section, nodes);
    }

    private void fillEvidenceSection(VBox section, JsonObject reportData) {
        ArrayList<Node> nodes = new ArrayList<>();

        JsonObject evidence = getAsObject(reportData, "evidence");
        JsonArray events = getAsArray(evidence, "events");
        VBox eventBox = new VBox();
        eventBox.setSpacing(6);
        if (events != null && events.size() > 0) {
            ArrayList<String[]> eventRows = new ArrayList<>();
            for (int i = 0; i < Math.min(events.size(), 40); i++) {
                JsonElement element = events.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject event = element.getAsJsonObject();
                eventRows.add(new String[]{
                        objectValue(event, "logIndex"),
                        objectValue(event, "eventType"),
                        objectValue(event, "category"),
                        objectValue(event, "summary")
                });
            }
            eventBox.getChildren().add(createAnalysisTable(
                    new String[]{I18nUtils.getString("blockchain.basic.log.index"), I18nUtils.getString("blockchain.basic.event.type"), I18nUtils.getString("blockchain.basic.category"), I18nUtils.getString("blockchain.basic.summary")},
                    new double[]{0.10, 0.16, 0.14, 0.60},
                    eventRows
            ));
        } else {
            eventBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.events")));
        }
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.events"), eventBox));

        JsonObject processed = getAsObject(reportData, "processedAnalysis");
        JsonArray evidenceNotes = getAsArray(processed, "evidenceNotes");
        VBox noteBox = new VBox();
        noteBox.setSpacing(6);
        if (evidenceNotes != null) {
            for (int i = 0; i < evidenceNotes.size(); i++) {
                noteBox.getChildren().add(createAnalysisLabel("• " + evidenceNotes.get(i).getAsString()));
            }
        }
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.warnings"),
                collectIssueLines(getAsArray(reportData, "warnings")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.errors"),
                collectIssueLines(getAsArray(reportData, "errors")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.missing"),
                collectTextLines(getAsArray(reportData, "missingFields")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.sources"),
                collectSourceLines(getAsArray(reportData, "sources")));
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.notes"), noteBox));

        JsonObject aiContext = getAsObject(reportData, "aiContext");
        JsonArray facts = getAsArray(aiContext, "facts");
        VBox aiBox = new VBox();
        aiBox.setSpacing(6);
        aiBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.ai.fallback")));
        if (facts != null) {
            for (int i = 0; i < Math.min(facts.size(), 30); i++) {
                aiBox.getChildren().add(createAnalysisLabel("• " + facts.get(i).getAsString()));
            }
        }
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.ai"), aiBox));

        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.raw"), createRawTextArea(jsonPretty(reportData))));
        replaceSection(section, nodes);
    }

    private void fillAddressEvidenceSection(VBox section, JsonObject reportData) {
        ArrayList<Node> nodes = new ArrayList<>();
        JsonObject evidence = getAsObject(reportData, "evidence");
        JsonObject baseInfo = getAsObject(evidence, "baseInfo");
        JsonObject contract = getAsObject(evidence, "contract");
        JsonObject portfolio = getAsObject(evidence, "portfolio");

        VBox baseBox = new VBox();
        baseBox.setSpacing(6);
        if (baseInfo != null) {
            addObjectRows(baseBox, baseInfo);
        } else {
            baseBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.no.address.evidence")));
        }
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.address"), baseBox));

        if (contract != null) {
            VBox contractBox = new VBox();
            contractBox.setSpacing(6);
            addObjectRows(contractBox, contract);
            nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.contract"), contractBox));
        }
        if (portfolio != null) {
            VBox portfolioBox = new VBox();
            portfolioBox.setSpacing(6);
            JsonArray assets = getAsArray(portfolio, "assets");
            if (assets != null && assets.size() > 0) {
                ArrayList<String[]> rows = new ArrayList<>();
                for (int i = 0; i < Math.min(assets.size(), 20); i++) {
                    JsonElement element = assets.get(i);
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject asset = element.getAsJsonObject();
                    rows.add(new String[]{
                            objectValue(asset, "name"),
                            objectValue(asset, "symbol"),
                            objectValue(asset, "assetType"),
                            objectValue(asset, "amount"),
                            objectValue(asset, "source")
                    });
                }
                portfolioBox.getChildren().add(createAnalysisTable(
                        new String[]{I18nUtils.getString("blockchain.table.fullname"), I18nUtils.getString("blockchain.table.token"), I18nUtils.getString("blockchain.basic.category"), I18nUtils.getString("blockchain.table.balance"), I18nUtils.getString("blockchain.basic.source")},
                        new double[]{0.24, 0.14, 0.16, 0.22, 0.24},
                        rows
                ));
            } else {
                addObjectRows(portfolioBox, portfolio);
            }
            nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.portfolio"), portfolioBox));
        }

        JsonObject processed = getAsObject(reportData, "processedAnalysis");
        JsonArray evidenceNotes = getAsArray(processed, "evidenceNotes");
        VBox noteBox = new VBox();
        noteBox.setSpacing(6);
        if (evidenceNotes != null) {
            for (int i = 0; i < evidenceNotes.size(); i++) {
                noteBox.getChildren().add(createAnalysisLabel("• " + evidenceNotes.get(i).getAsString()));
            }
        }
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.warnings"),
                collectIssueLines(getAsArray(reportData, "warnings")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.errors"),
                collectIssueLines(getAsArray(reportData, "errors")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.missing"),
                collectTextLines(getAsArray(reportData, "missingFields")));
        appendNamedLines(noteBox, I18nUtils.getString("blockchain.export.subsection.sources"),
                collectSourceLines(getAsArray(reportData, "sources")));
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.notes"), noteBox));

        JsonObject aiContext = getAsObject(reportData, "aiContext");
        JsonArray facts = getAsArray(aiContext, "facts");
        VBox aiBox = new VBox();
        aiBox.setSpacing(6);
        aiBox.getChildren().add(createAnalysisLabel(I18nUtils.getString("blockchain.report.ai.fallback")));
        if (facts != null) {
            for (int i = 0; i < Math.min(facts.size(), 30); i++) {
                aiBox.getChildren().add(createAnalysisLabel("• " + facts.get(i).getAsString()));
            }
        }
        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.ai"), aiBox));

        nodes.add(createDrawer(I18nUtils.getString("blockchain.report.drawer.raw"), createRawTextArea(jsonPretty(reportData))));
        replaceSection(section, nodes);
    }

    private String buildAiQuestionText(JsonObject reportData) {
        JsonObject aiContext = getAsObject(reportData, "aiContext");
        JsonArray facts = getAsArray(aiContext, "facts");
        StringBuilder builder = new StringBuilder();
        builder.append(I18nUtils.getString("blockchain.report.ai.question")).append("\n\n");
        if (facts != null) {
            for (int i = 0; i < facts.size(); i++) {
                builder.append("- ").append(facts.get(i).getAsString()).append("\n");
            }
        } else {
            JsonObject processed = getAsObject(reportData, "processedAnalysis");
            JsonArray summaries = getAsArray(processed, "summary");
            if (summaries != null) {
                for (int i = 0; i < summaries.size(); i++) {
                    builder.append("- ").append(summaries.get(i).getAsString()).append("\n");
                }
            }
        }
        return builder.toString();
    }

    private boolean looksLikeAiUnavailable(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return true;
        }
        String lower = answer.toLowerCase();
        return lower.contains("ai 配置")
                || lower.contains("ai configuration")
                || lower.contains("configuration is missing")
                || lower.contains("api key")
                || lower.contains("不能为空")
                || lower.contains("connect timed out")
                || lower.contains("read timed out")
                || lower.contains("timeout");
    }

    private void startOptionalAiAnalysis(VBox aiSection, JsonObject reportData, long reportVersion) {
        JsonObject aiContext = getAsObject(reportData, "aiContext");
        String systemPrompt = objectValue(aiContext, "prompt");
        if (systemPrompt.equals("")) {
            systemPrompt = I18nUtils.getString("blockchain.report.ai.system");
        }
        final String finalSystemPrompt = systemPrompt;
        final String questionText = buildAiQuestionText(reportData);

        Label statusLabel = createAnalysisLabel(I18nUtils.getString("blockchain.report.ai.streaming"));
        Label outputLabel = createAnalysisLabel("");
        replaceSection(aiSection, statusLabel, outputLabel);

        final StringBuilder answerBuffer = new StringBuilder();
        final StringBuilder errorBuffer = new StringBuilder();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                new AiChatService().streamChat(finalSystemPrompt, questionText, questionText, event -> {
                    if (reportVersion != currentReportVersion || event == null) {
                        return;
                    }
                    if (event.getType() == AiStreamEvent.Type.TOKEN) {
                        String content = event.getContent();
                        if (content == null || content.isEmpty()) {
                            return;
                        }
                        synchronized (answerBuffer) {
                            answerBuffer.append(content);
                        }
                        Platform.runLater(() -> {
                            if (reportVersion != currentReportVersion) {
                                return;
                            }
                            String current;
                            synchronized (answerBuffer) {
                                current = answerBuffer.toString();
                            }
                            outputLabel.setText(current);
                            cacheCurrentAiAnalysis(reportVersion, current);
                        });
                    } else if (event.getType() == AiStreamEvent.Type.ERROR) {
                        String content = valueOrDash(event.getContent());
                        synchronized (errorBuffer) {
                            errorBuffer.append(content);
                        }
                        Platform.runLater(() -> {
                            if (reportVersion != currentReportVersion) {
                                return;
                            }
                            cacheCurrentAiAnalysis(reportVersion, "");
                            statusLabel.setText(I18nUtils.getString("blockchain.report.ai.unavailable"));
                            outputLabel.setText(content);
                        });
                    }
                });
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            if (reportVersion != currentReportVersion) {
                return;
            }
            String answer;
            synchronized (answerBuffer) {
                answer = answerBuffer.toString();
            }
            String error;
            synchronized (errorBuffer) {
                error = errorBuffer.toString();
            }
            if (looksLikeAiUnavailable(answer)) {
                cacheCurrentAiAnalysis(reportVersion, "");
                statusLabel.setText(I18nUtils.getString("blockchain.report.ai.unavailable"));
                outputLabel.setText(valueOrDash(error.equals("") ? answer : error));
            } else {
                cacheCurrentAiAnalysis(reportVersion, answer);
                statusLabel.setText(I18nUtils.getString("blockchain.report.ai.notice"));
                outputLabel.setText(answer);
            }
        });
        task.setOnFailed(event -> {
            if (reportVersion != currentReportVersion) {
                return;
            }
            Throwable error = task.getException();
            String detail = error == null ? "" : error.getMessage();
            cacheCurrentAiAnalysis(reportVersion, "");
            replaceSection(aiSection,
                    createAnalysisLabel(I18nUtils.getString("blockchain.report.ai.unavailable")),
                    createAnalysisLabel(valueOrDash(detail)));
            if (debugMode && error != null) {
                error.printStackTrace();
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void initialize() {

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(10.0);
        Rectangle clip = clipRect(
                vBoxBar, arcProperty
        );
        vBoxBar.setClip(clip);

        initReportControls();
        initTextData();

        listenSearch();

        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == null) {
            return;
        }
        if (startupPage == ToStart.StartupPage.BLUE_BLOCKCHAIN_RESULT_LIST) {
            renderStartupResultList();
            return;
        }
        if (startupPage == ToStart.StartupPage.BLUE_BLOCKCHAIN_DETAIL) {
            renderStartupDetail();
            return;
        }
        if (startupPage == ToStart.StartupPage.BLUE_BLOCKCHAIN_EMPTY) {
            renderStartupEmpty();
        }
    }

    // 初始化样本数据
    private void initTextData() {
        contentObj.clear();

        VBox gallery = new VBox();
        gallery.setSpacing(12);
        gallery.setPrefWidth(sampleContentWidth());
        gallery.setMaxWidth(sampleContentWidth());
        gallery.getStyleClass().add("sampleGallery");

        VBox intro = new VBox();
        intro.setSpacing(6);
        intro.getStyleClass().add("sampleIntro");
        intro.getChildren().addAll(
                createBoundSampleLabel("blockchain.samples.title", "sampleTitle"),
                createBoundSampleLabel("blockchain.samples.subtitle", "sampleSubtitle")
        );
        gallery.getChildren().add(intro);

        ArrayList<BlockchainSampleCase> sampleCases = buildSampleCases();
        for (BlockchainSampleCase sampleCase : sampleCases) {
            gallery.getChildren().add(createSampleCaseCard(sampleCase));
        }
        contentObj.add(gallery);
        listViewCell.setItems(contentObj);
    }

    private void renderStartupResultList() {
        question.setText("Nomad Bridge 样例案件");
        backClearLabel.setVisible(true);
        contentObj.clear();
        listViewCell.getItems().clear();

        contentObj.add(createPreviewResultCard(
                "ETH",
                I18nUtils.getString("blockchain.sample.nomad.title"),
                "交易哈希：0xa5fe9d044e4f3e5aa5bc4c0709333cd2190cba0f4e7f16bcf73f49f83e4a5460",
                new String[]{
                        "标签：Nomad Bridge 攻击复核",
                        "类型：transaction",
                        "关键点：跨链桥复制型攻击 / 证据边界 / 多段资金流",
                        "状态：样例预置结果，适合截图查询结果列表态"
                }
        ));
        contentObj.add(createPreviewResultCard(
                "ETH",
                "Euler Finance 攻击追踪",
                "交易哈希：0x47ac3527d02e6b9631c77fad1cdee7bfa77a8a7bfd4880dccbda5146ace4088f",
                new String[]{
                        "标签：公开恶意场景",
                        "类型：transaction",
                        "关键点：闪电贷 / 多协议交互 / 风险提示"
                }
        ));
        contentObj.add(createPreviewResultCard(
                "ETH",
                "可疑地址画像",
                "地址：0x3f5CE5FBFe3E9af3971dD833D26BA9b5C936f0bE",
                new String[]{
                        "标签：address",
                        "余额：125.42 ETH",
                        "交易数：482",
                        "说明：用于展示混合列表中的地址卡片样式"
                }
        ));
        listViewCell.setItems(contentObj);
    }

    private VBox createPreviewResultCard(String network, String title, String targetLine, String[] detailLines) {
        VBox card = new VBox();
        card.setSpacing(8);
        card.setPrefWidth(sampleContentWidth());
        card.setMaxWidth(sampleContentWidth());
        card.getStyleClass().addAll("cellVBox", "sampleCard");

        HBox header = new HBox();
        header.setSpacing(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("sampleCardTitle");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        Label badgeLabel = new Label(network);
        badgeLabel.getStyleClass().add("sampleBadge");
        header.getChildren().addAll(titleLabel, badgeLabel);

        Label targetLabel = new Label(targetLine);
        targetLabel.setWrapText(true);
        targetLabel.getStyleClass().add("sampleDescription");
        card.getChildren().addAll(header, targetLabel);
        if (detailLines != null) {
            for (String line : detailLines) {
                Label lineLabel = new Label(line);
                lineLabel.setWrapText(true);
                lineLabel.getStyleClass().add("sampleCapability");
                card.getChildren().add(lineLabel);
            }
        }
        return card;
    }

    private void renderStartupDetail() {
        question.setText("0xa5fe9d044e4f3e5aa5bc4c0709333cd2190cba0f4e7f16bcf73f49f83e4a5460");
        backClearLabel.setVisible(true);
        clearCurrentReportContext();
        detailedPane.setVisible(false);
        accordionPane.getPanes().clear();
        accordionPane.setVisible(true);
        if (accordionScrollPane != null) accordionScrollPane.setTranslateX(0);

        contentObj.clear();
        listViewCell.getItems().clear();
        listViewCell.setVisible(true);
        listViewCell.setManaged(true);

        String analysisKey = I18nUtils.getString("blockchain.report.section.analysis");
        String findingsKey = I18nUtils.getString("blockchain.report.section.findings");
        String overviewKey = I18nUtils.getString("blockchain.report.section.overview");
        String timelineKey = I18nUtils.getString("blockchain.report.section.timeline");
        String evidenceKey = I18nUtils.getString("blockchain.report.section.evidence");
        addReportNav(analysisKey);
        addReportNav(findingsKey);
        addReportNav(overviewKey);
        addReportNav(timelineKey);
        addReportNav(evidenceKey);

        VBox page = new VBox();
        page.setSpacing(16);
        page.setPrefWidth(sampleContentWidth());
        page.setMaxWidth(sampleContentWidth());
        page.getStyleClass().addAll("cellVBox", "sampleCard");
        page.getChildren().add(createAnalysisHeadline("Nomad Bridge 攻击复核"));
        page.getChildren().add(createAnalysisLabel("该预置态用于展示区块链详情页排版，固定展示摘要、发现、概览、时间线和证据。"));

        page.getChildren().add(createPreviewSection(analysisKey, new String[]{
                "网络：ETH",
                "对象类型：transaction",
                "标签：跨链桥复制型异常",
                "摘要：目标交易与公开披露案例一致，适合详情页截图。"
        }));
        page.getChildren().add(createPreviewSection(findingsKey, new String[]{
                "关键发现 1：攻击面命中跨链桥复制路径。",
                "关键发现 2：资产路径跨越多个交互节点。",
                "关键发现 3：链上事件日志形成连续证据链。"
        }));
        page.getChildren().add(createPreviewSection(overviewKey, new String[]{
                "交易哈希：0xa5fe9d044e4f3e5aa5bc4c0709333cd2190cba0f4e7f16bcf73f49f83e4a5460",
                "区块高度：15259101",
                "时间：2022-08-02 09:32:11 UTC",
                "From：0x4f3a120e72c76c22ae802d129f599bfdbcfb1c17",
                "To：0x5d94309e5a0090f0fc8317d0c0ad6f6d34c4c3bb",
                "金额：100.00 WBTC"
        }));
        page.getChildren().add(createPreviewSection(timelineKey, new String[]{
                "1. 触发跨链桥消息处理逻辑。",
                "2. 目标地址接收桥接资产后快速分发。",
                "3. 多个事件日志与资金流节点形成连续证据链。"
        }));
        page.getChildren().add(createPreviewSection(evidenceKey, new String[]{
                "Event[0] Transfer / Bridge / 跨链桥资产转出到目标地址",
                "Event[1] Swap / Dex / 目标地址随后发起兑换行为",
                "Event[2] Transfer / Routing / 部分资产继续分发到二级地址",
                "Raw: {\"preview\":true,\"network\":\"eth\",\"type\":\"transaction\"}"
        }));

        contentObj.add(page);
        listViewCell.setItems(contentObj);
    }

    private VBox createPreviewSection(String title, String[] lines) {
        VBox section = new VBox();
        section.setSpacing(8);
        section.getStyleClass().add("analysisSection");

        Label titleLabel = createAnalysisHeadline(title);
        section.getChildren().add(titleLabel);
        if (lines != null) {
            for (String line : lines) {
                section.getChildren().add(createAnalysisLabel(line));
            }
        }
        return section;
    }

    private JsonObject createPreviewFinding(String title, String description, String evidence) {
        JsonObject finding = new JsonObject();
        finding.addProperty("title", title);
        finding.addProperty("description", description);
        finding.addProperty("evidence", evidence);
        return finding;
    }

    private void renderStartupEmpty() {
        question.setText("0x0000000000000000000000000000000000000000");
        backClearLabel.setVisible(true);
        showListTip(I18nUtils.getString("blockchain.error.query"));
    }

    void writeTestData(String data) {
        question.setText(data);
    }

    private ArrayList<BlockchainSampleCase> buildSampleCases() {
        ArrayList<BlockchainSampleCase> cases = new ArrayList<>();
        cases.add(new BlockchainSampleCase(
                "0x47ac3527d02e6b9631c77fad1cdee7bfa77a8a7bfd4880dccbda5146ace4088f",
                "eth",
                "transaction",
                "blockchain.sample.euler.title",
                "blockchain.sample.euler.badge",
                "blockchain.sample.euler.desc",
                "blockchain.sample.euler.capability"
        ));
        cases.add(new BlockchainSampleCase(
                "0xcd314668aaa9bbfebaf1a0bd2b6553d01dd58899c508d4729fa7311dc5d33ad7",
                "eth",
                "transaction",
                "blockchain.sample.beanstalk.title",
                "blockchain.sample.beanstalk.badge",
                "blockchain.sample.beanstalk.desc",
                "blockchain.sample.beanstalk.capability"
        ));
        cases.add(new BlockchainSampleCase(
                "0x0fe2542079644e107cbf13690eb9c2c65963ccb79089ff96bfaf8dced2331c92",
                "eth",
                "transaction",
                "blockchain.sample.cream.title",
                "blockchain.sample.cream.badge",
                "blockchain.sample.cream.desc",
                "blockchain.sample.cream.capability"
        ));
        cases.add(new BlockchainSampleCase(
                "0xa5fe9d044e4f3e5aa5bc4c0709333cd2190cba0f4e7f16bcf73f49f83e4a5460",
                "eth",
                "transaction",
                "blockchain.sample.nomad.title",
                "blockchain.sample.nomad.badge",
                "blockchain.sample.nomad.desc",
                "blockchain.sample.nomad.capability"
        ));
        cases.add(new BlockchainSampleCase(
                "0x038ec2f5565bdfebab754cf6f8f94c5116313ebbe8b1197e87cef257525b8d97",
                "eth",
                "transaction",
                "blockchain.sample.complex.title",
                "blockchain.sample.complex.badge",
                "blockchain.sample.complex.desc",
                "blockchain.sample.complex.capability"
        ));
        return cases;
    }

    private VBox createSampleCaseCard(BlockchainSampleCase sampleCase) {
        VBox card = new VBox();
        card.setSpacing(8);
        card.setPrefWidth(sampleContentWidth());
        card.setMaxWidth(sampleContentWidth());
        card.getStyleClass().addAll("cellVBox", "sampleCard");
        card.setCursor(Cursor.HAND);

        HBox header = new HBox();
        header.setSpacing(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = createBoundSampleLabel(sampleCase.titleKey, "sampleCardTitle");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label badge = createBoundSampleLabel(sampleCase.badgeKey, "sampleBadge");
        header.getChildren().addAll(title, badge);

        Label description = createBoundSampleLabel(sampleCase.descriptionKey, "sampleDescription");
        Label capability = createBoundSampleLabel(sampleCase.capabilityKey, "sampleCapability");

        HBox targetRow = new HBox();
        targetRow.setSpacing(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        Label targetLabel = createBoundSampleLabel("blockchain.samples.target", "sampleMeta");
        Label target = new Label(shortenSampleTarget(sampleCase.target));
        target.setWrapText(true);
        target.setMaxWidth(Math.max(220D, sampleContentWidth() - 140D));
        target.getStyleClass().add("sampleTarget");
        HBox.setHgrow(target, Priority.ALWAYS);
        targetRow.getChildren().addAll(targetLabel, target);

        Label runHint = createBoundSampleLabel("blockchain.samples.run", "sampleRunHint");
        card.getChildren().addAll(header, description, capability, targetRow, runHint);
        card.setOnMouseClicked(event -> runSampleCase(sampleCase));
        return card;
    }

    private Label createBoundSampleLabel(String key, String styleClass) {
        Label label = new Label();
        label.textProperty().bind(I18nUtils.createBinding(key));
        label.setWrapText(true);
        label.setMaxWidth(sampleContentWidth());
        if (styleClass != null && !styleClass.equals("")) {
            label.getStyleClass().add(styleClass);
        }
        return label;
    }

    private double sampleContentWidth() {
        double width = listViewCell == null ? 0D : listViewCell.getWidth();
        if (width <= 0D && sPane != null) {
            width = sPane.getWidth();
        }
        if (width <= 0D) {
            width = 900D;
        }
        return Math.max(520D, width - 280D);
    }

    private String shortenSampleTarget(String target) {
        if (target == null) {
            return "";
        }
        if (target.length() <= 34) {
            return target;
        }
        return target.substring(0, 18) + "..." + target.substring(target.length() - 12);
    }

    private void runSampleCase(BlockchainSampleCase sampleCase) {
        if (sampleCase == null) {
            return;
        }
        writeTestData(sampleCase.target);
        backClearLabel.setVisible(true);
        if ("transaction".equals(sampleCase.reportType)) {
            showTransactionReport(sampleCase.network, sampleCase.target);
        } else if ("address".equals(sampleCase.reportType)) {
            showAddressReport(sampleCase.network, sampleCase.target);
        } else {
            searchInput();
        }
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
        clearCurrentReportContext();
        accordionPane.getPanes().clear();
        detailedPane.setVisible(false);
        resetAccordionPosition();
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
                if(!hasData(searchData)){
                    String message = getResponseMessage(searchData);
                    showListTip(message);
                    showWarningDialog(message);
                    return null;
                }

                JsonArray dataJsonArray = getAsArray(searchData, "data");
                if (dataJsonArray == null || dataJsonArray.size() == 0) {
                    String message = getResponseMessage(searchData);
                    showListTip(message);
                    showWarningDialog(message);
                    return null;
                }
                Platform.runLater(() -> {
                    contentObj.clear();
                });
                for (int i = 0; i < dataJsonArray.size(); i++) {
                    JsonElement element = dataJsonArray.get(i);
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject jsonObject = element.getAsJsonObject();

                    String type = getAsString(jsonObject, "type");

                    if(jsonObject.has("indexName")){

                        String icon = getAsString(jsonObject, "icon");
                        String coinFullNameCn = getAsString(jsonObject, "coinFullNameCn");
                        String coinFullNameSource = getAsString(jsonObject, "coinFullName");
                        String coinFullName = !coinFullNameCn.equals("") ? coinFullNameCn + " - " + coinFullNameSource : coinFullNameSource;
                        String summaryCn = getDisplayText(jsonObject, "summaryCn");
                        String summary = !summaryCn.equals("") ? summaryCn : getDisplayText(jsonObject, "summaryEn");
                        String wpCn = getDisplayText(jsonObject, "wpCn");
                        String wp =  !wpCn.equals("") ? wpCn : getDisplayText(jsonObject, "wpEn");

                        summary = sanitizeRichText(summary);
                        wp = sanitizeRichText(wp);

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = new HBox();
                        hBox.getStyleClass().add("cellHBox");
                        hBox.setAlignment(Pos.CENTER);

                        Image image = loadBlockchainIcon(icon);
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
                        if(summary.equals("") && !wp.equals("")){
                            label2.setText(wp);
                        }
                        vBoxConent.getChildren().addAll(hBox, label2);
                        addCoinMarketInfo(vBoxConent, jsonObject);

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("address")){

                        String network = getAsString(jsonObject, "network");
                        String hash = getAsString(jsonObject, "hash");
                        String addrAlias = getAsString(jsonObject, "addrAlias");
                        String txCount = getAsString(jsonObject, "txCount");
                        String spend = getAsString(jsonObject, "spend");
                        String receive = getAsString(jsonObject, "receive");
                        String balance = getAsString(jsonObject, "balance");
                        String normalTxCount = getAsString(jsonObject, "normalTxCount");

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = buildCardHeader(
                                I18nUtils.getString("blockchain.type.address"),
                                network.toUpperCase(),
                                "bc-chip-address"
                        );

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

                        vBoxConent.setOnMouseClicked(event -> {
                            if (supportsEvmReport(network)) {
                                showAddressReport(network, hash);
                            } else {
                                showBasicEvidenceDetail(jsonObject);
                            }
                        });

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("block")) {
                        String network = getAsString(jsonObject, "network");
                        String block_no = getAsString(jsonObject, "block_no");

                        String fee = getAsString(jsonObject, "fee");
                        String txCnt = getAsString(jsonObject, "txCnt");
                        String time = formatEpochSecond(jsonObject, "time");
                        String blockhash = getAsString(jsonObject, "blockhash");
                        String confirmations = getAsString(jsonObject, "confirmations");

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = buildCardHeader(
                                I18nUtils.getString("blockchain.type.block"),
                                network.toUpperCase(),
                                "bc-chip-block"
                        );

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

                        vBoxConent.setOnMouseClicked(event -> showBasicEvidenceDetail(jsonObject));

                        Platform.runLater(() -> {
                            contentObj.add(vBoxConent);
                        });

                    } else if(type.equals("tx")){

                        String network = getAsString(jsonObject, "network");
                        String block_no = getAsString(jsonObject, "block_no");
                        String txid = getAsString(jsonObject, "txid");

                        String from = getAsString(jsonObject, "from");
                        String to = getAsString(jsonObject, "to");
                        String fee = getAsString(jsonObject, "fee");
                        String txCnt = getAsString(jsonObject, "txCnt");
                        String time = formatEpochSecond(jsonObject, "time");
                        String blockhash = getAsString(jsonObject, "blockhash");
                        String confirmations = getAsString(jsonObject, "confirmations");

                        VBox vBoxConent = new VBox();
                        vBoxConent.setPrefWidth(sPane.getWidth() - 280);
                        vBoxConent.getStyleClass().add("cellVBox");
                        vBoxConent.setAlignment(Pos.CENTER);
                        vBoxConent.setCursor(Cursor.HAND);

                        HBox hBox = buildCardHeader(
                                I18nUtils.getString("blockchain.type.tx"),
                                network.toUpperCase(),
                                "bc-chip-tx"
                        );

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

                        vBoxConent.setOnMouseClicked(event -> {
                            if (supportsEvmReport(network)) {
                                showTransactionReport(network, txid);
                            } else {
                                showBasicEvidenceDetail(jsonObject);
                            }
                        });

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
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null
                ? I18nUtils.getString("blockchain.error.query")
                : error.getMessage();
            showErrorPrompt(I18nUtils.getString("common.task.failed", detail));
        });
        // 启动任务
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    private void getDetailedInfo(JsonObject jsonObject) {
        clearCurrentReportContext();
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

        String type = getAsString(jsonObject, "type");

        accordionPane.getPanes().clear();
        accordionPane.setVisible(true);
        slideAccordionIn();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    if (type.equals("address") && jsonObject.has("network")) {
                        String address = getAsString(jsonObject, "hash");
                        String network = getAsString(jsonObject, "network");

                        try {
                            //  获取交易地址基础信息
                            JsonObject addressInfo = address(network, address);
                            if (hasData(addressInfo)) {
                                String key = I18nUtils.getString("blockchain.nav.addrinfo");
                                JsonObject baseInfo = getAsObject(getAsObject(addressInfo, "data"), "baseInfo");
                                if (baseInfo == null) {
                                    throw new IllegalStateException("地址基础信息为空");
                                }

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
                                    addInfoRow(vBox, jsonKey, value);
                                }
                                Platform.runLater(() -> {
                                    detailedContentObj.addAll(title, vBox);
                                    detailedListView.setItems(detailedContentObj);
                                });

                            } else if (addressInfo != null) {
                                String message = getResponseMessage(addressInfo);
                                showDetailTip(message);
                                showWarningDialog(message);
                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                        try {
                            //  近180天余额变化
                            JsonObject balancetrend = balancetrend(network, address);
                            if (hasData(balancetrend)) {

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

                                JsonArray tmpJArray = getAsArray(balancetrend, "data");
                                LinkedHashMap<String, Double> trendData = buildTrendData(tmpJArray, getAsString(jsonObject, "balance"));

                                CategoryAxis xAxis = new CategoryAxis();
                                NumberAxis yAxis = new NumberAxis();
                                xAxis.setLabel("时间");
                                yAxis.setLabel("余额");
                                LineChart<String, Number> lineChart = new LineChart<String, Number>(xAxis, yAxis);
                                lineChart.setTitle(key);
                                lineChart.setCreateSymbols(trendData.size() <= 1);

                                XYChart.Series series = new XYChart.Series();
                                series.setName(key);
                                double minValue = Double.MAX_VALUE;
                                double maxValue = -Double.MAX_VALUE;
                                for (Map.Entry<String, Double> entry : trendData.entrySet()) {
                                    Double value = entry.getValue();
                                    minValue = Math.min(minValue, value);
                                    maxValue = Math.max(maxValue, value);
                                    series.getData().add(new XYChart.Data(entry.getKey(), value));
                                }
                                if (minValue != Double.MAX_VALUE && Double.compare(minValue, maxValue) == 0) {
                                    double padding = Math.max(Math.abs(maxValue) * 0.05, 1D);
                                    yAxis.setAutoRanging(false);
                                    yAxis.setLowerBound(maxValue - padding);
                                    yAxis.setUpperBound(maxValue + padding);
                                    yAxis.setTickUnit(padding);
                                }
                                lineChart.getData().add(series);


                                Platform.runLater(() -> {
                                    detailedContentObj.addAll(title, lineChart);
                                    detailedListView.setItems(detailedContentObj);
                                });
                            } else if (balancetrend != null) {
                                String key = I18nUtils.getString("blockchain.nav.balance180");
                                String message = getResponseMessage(balancetrend);

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
                                VBox messageBox = new VBox();
                                messageBox.setSpacing(8);
                                messageBox.setStyle("-fx-padding: 10 10 20 20;");
                                messageBox.getChildren().add(createAnalysisLabel(message));
                                Platform.runLater(() -> {
                                    detailedContentObj.addAll(title, messageBox);
                                    detailedListView.setItems(detailedContentObj);
                                });
                            }
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }

                        try {
                            //  代币余额
                            JsonObject tokenbalance = tokenbalance(network, address);
                            if (hasData(tokenbalance) && getAsArray(tokenbalance, "data") != null) {

                                String key = I18nUtils.getString("blockchain.nav.tokenbalance");
                                JsonArray tokenbalanceInfo = getAsArray(tokenbalance, "data");

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

                                    JsonElement tokenElement = tokenbalanceInfo.get(i);
                                    if (tokenElement == null || !tokenElement.isJsonObject()) {
                                        continue;
                                    }
                                    JsonObject tmpJObj = tokenElement.getAsJsonObject();
                                    JsonObject tokenInfo = getAsObject(tmpJObj, "tokenInfo");
                                    if (tokenInfo == null) {
                                        continue;
                                    }

                                    String fromName = getAsString(tokenInfo, "f");
                                    int decimals;
                                    try {
                                        decimals = tokenInfo.has("d") && !tokenInfo.get("d").isJsonNull() ? tokenInfo.get("d").getAsInt() : 0;
                                    } catch (Exception e) {
                                        decimals = 0;
                                    }
                                    String balanceNetwork = getAsString(tokenInfo, "s");

                                    String transferCnt = getAsString(tmpJObj, "transferCnt");
                                    String balance = new BigDecimal(getAsString(tmpJObj, "balance").isEmpty() ? "0" : getAsString(tmpJObj, "balance"))
                                            .divide(BigDecimal.TEN.pow((int) decimals))
                                            .toPlainString();

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
                        String block = getAsString(jsonObject, "block_no");
                        String network = getAsString(jsonObject, "network");

                        //  获取交易地址基础信息
                        JsonObject blockInfo = block(network, block);
                        if (!hasData(blockInfo)) {
                            String message = getResponseMessage(blockInfo);
                            showDetailTip(message);
                            showWarningDialog(message);
                            return null;
                        }
                        JsonObject baseInfo = getAsObject(getAsObject(blockInfo, "data"), "baseInfo");
                        if (baseInfo != null) {
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
                                addInfoRow(vBox, jsonKey, value);
                            }
                            Platform.runLater(() -> {
                                detailedContentObj.addAll(title, vBox);
                                detailedListView.setItems(detailedContentObj);
                            });

                        }

                        try {
                            //  获取交易
                            int num1 = getCount(blockInfo, "交易");
                            if (num1 != 0) {
                                JsonObject getTxData = getTxData(network, block, "1", "20");
                                JsonArray tmpDataInfo = getAsArray(getTxData, "data");
                                if (!hasData(getTxData) || tmpDataInfo == null) {
                                    String message = getResponseMessage(getTxData);
                                    showDetailTip(message);
                                    showWarningDialog(message);
                                    return null;
                                }

                                String key = I18nUtils.getString("blockchain.nav.tx", num1);

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
                                    JsonElement txElement = tmpDataInfo.get(i);
                                    if (txElement == null || !txElement.isJsonObject()) {
                                        continue;
                                    }
                                    JsonObject tmpJObj = txElement.getAsJsonObject();
                                    String txid = getAsString(tmpJObj, "txid");
                                    String block_no = getAsString(tmpJObj, "block_no");
                                    String time = formatEpochSecond(tmpJObj, "time");
                                    String from = getAsString(tmpJObj, "from");
                                    String to = getAsString(tmpJObj, "to");
                                    String value = getAsString(tmpJObj, "value");
                                    String fee = getAsString(tmpJObj, "fee");
                                    String networkFree = getAsString(tmpJObj, "network");

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
                            int num2 = getCount(blockInfo, "代币交易");
                            if (num2 != 0) {
                                JsonObject getTokentransferData = getTokentransferData(network, block, "1", "20");
                                JsonObject tmpDataInfo = getAsObject(getTokentransferData, "data");
                                if (!hasData(getTokentransferData) || tmpDataInfo == null) {
                                    String message = getResponseMessage(getTokentransferData);
                                    showDetailTip(message);
                                    showWarningDialog(message);
                                    return null;
                                }

                                String key = I18nUtils.getString("blockchain.nav.tokentx", num2);

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
                                    if (entry.getValue() == null || !entry.getValue().isJsonArray()) {
                                        continue;
                                    }
                                    JsonArray entryValue = entry.getValue().getAsJsonArray();

                                    for (int i = 0; i < entryValue.size(); i++) {
                                        JsonElement transferElement = entryValue.get(i);
                                        if (transferElement == null || !transferElement.isJsonObject()) {
                                            continue;
                                        }
                                        JsonObject tmpJObj = transferElement.getAsJsonObject();
                                        String block_no = getAsString(tmpJObj, "block_no");
                                        String time = formatEpochSecond(tmpJObj, "time");
                                        String from = getAsString(tmpJObj, "from");
                                        String to = getAsString(tmpJObj, "to");
                                        String value = getAsString(tmpJObj, "value");
                                        String icon = getAsString(tmpJObj, "tokenSymbol");


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

                                        Image image = loadBlockchainIcon(icon);
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
                            int num3 = getCount(blockInfo, "合约调用转帐");
                            if (num3 != 0) {
                                JsonObject getInternalData = getInternalData(network, block, "1", "20");
                                JsonObject tmpDataInfo = getAsObject(getInternalData, "data");
                                if (!hasData(getInternalData) || tmpDataInfo == null) {
                                    String message = getResponseMessage(getInternalData);
                                    showDetailTip(message);
                                    showWarningDialog(message);
                                    return null;
                                }

                                String key = I18nUtils.getString("blockchain.nav.contracttx", num3);

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
                                    if (entry.getValue() == null || !entry.getValue().isJsonArray()) {
                                        continue;
                                    }
                                    JsonArray entryValue = entry.getValue().getAsJsonArray();

                                    for (int i = 0; i < entryValue.size(); i++) {
                                        JsonElement internalElement = entryValue.get(i);
                                        if (internalElement == null || !internalElement.isJsonObject()) {
                                            continue;
                                        }
                                        JsonObject tmpJObj = internalElement.getAsJsonObject();
                                        String block_no = getAsString(tmpJObj, "block_no");
                                        String time = formatEpochSecond(tmpJObj, "time");
                                        String from = getAsString(tmpJObj, "from");
                                        String to = getAsString(tmpJObj, "to");
                                        String value = getAsString(tmpJObj, "value");


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
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            String detail = error == null || error.getMessage() == null
                ? I18nUtils.getString("blockchain.error.query")
                : error.getMessage();
            showErrorPrompt(I18nUtils.getString("common.task.failed", detail));
        });
        // 启动任务
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    private void showRegionTokentrans() {
        System.out.println("第一版先到这里哦~ 第一版暂没写批量查询及导出，工作比较忙");
    }

    private void showTransactionReport(String network, String txid) {
        openReportDetailPane();
        final long reportVersion = beginReportSession("transaction", network, txid);

        VBox investigationSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.investigation"));
        VBox entitiesSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.entities"));
        VBox attackStepsSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.attack.steps"));
        VBox assetMovementSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.asset.movement"));
        VBox diagramSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.diagram"));
        VBox attackAnalysisSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.attack.analysis"));
        VBox conclusionSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.conclusion"));
        VBox aiAnalysisSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.ai"));
        VBox evidenceAssessmentSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.evidence.assessment"));
        VBox contextSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.context"));
        VBox evidenceSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.evidence"));

        addReportSection(I18nUtils.getString("blockchain.report.section.investigation"), investigationSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.entities"), entitiesSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.attack.steps"), attackStepsSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.asset.movement"), assetMovementSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.diagram"), diagramSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.attack.analysis"), attackAnalysisSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.conclusion"), conclusionSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.ai.analysis"), aiAnalysisSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.evidence.assessment"), evidenceAssessmentSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.context"), contextSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.evidence"), evidenceSection);
        detailedListView.setItems(detailedContentObj);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject response = reportTransaction(network, txid);
                JsonObject reportData = responseData(response);
                if (reportData == null) {
                    String message = getResponseMessage(response);
                    Platform.runLater(() -> {
                        if (reportVersion != currentReportVersion) {
                            return;
                        }
                        showReportFailureState("transaction", network, txid, message);
                    });
                    return null;
                }
                if (incidentNarrative(reportData) == null) {
                    Platform.runLater(() -> {
                        if (reportVersion != currentReportVersion) {
                            return;
                        }
                        showReportFailureState("transaction", network, txid,
                                I18nUtils.getString("blockchain.report.no.narrative"));
                    });
                    return null;
                }

                Platform.runLater(() -> cacheCurrentReportData(reportVersion, "transaction", network, txid, reportData));
                Platform.runLater(() -> fillInvestigationSection(investigationSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillEntitiesSection(entitiesSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillAttackStepsSection(attackStepsSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillAssetMovementSection(assetMovementSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillNarrativeDiagramSection(diagramSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillAttackAnalysisSection(attackAnalysisSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillConclusionSection(conclusionSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> startOptionalAiAnalysis(aiAnalysisSection, reportData, reportVersion));
                Thread.sleep(60);
                Platform.runLater(() -> fillEvidenceAssessmentSection(evidenceAssessmentSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillContextSection(contextSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillEvidenceSection(evidenceSection, reportData));
                return null;
            }
        };
        task.setOnFailed(e -> {
            if (reportVersion != currentReportVersion) {
                return;
            }
            Throwable error = task.getException();
            String message = error == null ? I18nUtils.getString("blockchain.error.query") : error.getMessage();
            Platform.runLater(() -> {
                showReportFailureState("transaction", network, txid, message);
            });
            if (debugMode && error != null) {
                error.printStackTrace();
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showAddressReport(String network, String address) {
        openReportDetailPane();
        final long reportVersion = beginReportSession("address", network, address);

        VBox analysisSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.analysis"));
        VBox aiAnalysisSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.ai"));
        VBox findingSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.findings"));
        VBox overviewSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.overview"));
        VBox rolesSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.roles"));
        VBox timelineSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.timeline"));
        VBox flowSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.flow"));
        VBox sequenceSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.sequence"));
        VBox riskSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.risk"));
        VBox followUpSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.followup"));
        VBox evidenceSection = createReportSectionBody(I18nUtils.getString("blockchain.report.loading.evidence"));

        addReportSection(I18nUtils.getString("blockchain.report.section.analysis"), analysisSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.ai.analysis"), aiAnalysisSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.findings"), findingSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.overview"), overviewSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.roles"), rolesSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.timeline"), timelineSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.flow"), flowSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.sequence"), sequenceSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.risk"), riskSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.followup"), followUpSection);
        addReportSection(I18nUtils.getString("blockchain.report.section.evidence"), evidenceSection);
        detailedListView.setItems(detailedContentObj);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject response = reportAddress(network, address);
                JsonObject reportData = responseData(response);
                if (reportData == null) {
                    String message = getResponseMessage(response);
                    if (isServiceUnavailableMessage(message)) {
                        Platform.runLater(() -> {
                            if (reportVersion != currentReportVersion) {
                                return;
                            }
                            showWarningDialog(message);
                            clearCurrentReportContext();
                            showBasicEvidenceDetail(createAddressFallbackItem(network, address));
                        });
                        return null;
                    }
                    Platform.runLater(() -> {
                        if (reportVersion != currentReportVersion) {
                            return;
                        }
                        showReportFailureState("address", network, address, message);
                    });
                    return null;
                }

                Platform.runLater(() -> cacheCurrentReportData(reportVersion, "address", network, address, reportData));
                Platform.runLater(() -> fillAnalysisSection(analysisSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> startOptionalAiAnalysis(aiAnalysisSection, reportData, reportVersion));
                Thread.sleep(60);
                Platform.runLater(() -> fillFindingsSection(findingSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillAddressOverviewSection(overviewSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillRolesSection(rolesSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillTimelineSection(timelineSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillFlowSection(flowSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillSequenceSection(sequenceSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillRiskSection(riskSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillFollowUpSection(followUpSection, reportData));
                Thread.sleep(60);
                Platform.runLater(() -> fillAddressEvidenceSection(evidenceSection, reportData));
                return null;
            }
        };
        task.setOnFailed(e -> {
            if (reportVersion != currentReportVersion) {
                return;
            }
            Throwable error = task.getException();
            String message = error == null ? I18nUtils.getString("blockchain.error.query") : error.getMessage();
            Platform.runLater(() -> {
                showReportFailureState("address", network, address, message);
            });
            if (debugMode && error != null) {
                error.printStackTrace();
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private ArrayList<String> collectTextLines(JsonArray array) {
        ArrayList<String> lines = new ArrayList<>();
        if (array == null) {
            return lines;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            String line = element.isJsonPrimitive() ? element.getAsString() : element.toString();
            if (!line.trim().equals("")) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private String formatKeyValueObject(JsonObject object, String... orderedKeys) {
        ArrayList<String> parts = new ArrayList<>();
        if (object == null) {
            return "";
        }
        if (orderedKeys != null) {
            for (String key : orderedKeys) {
                String value = objectValue(object, key);
                if (!value.equals("")) {
                    parts.add(key + "=" + value);
                }
            }
        }
        if (parts.isEmpty()) {
            for (String key : object.keySet()) {
                String value = objectValue(object, key);
                if (!value.equals("")) {
                    parts.add(key + "=" + value);
                }
            }
        }
        return String.join(" | ", parts);
    }

    private String compactObject(JsonObject object, int limit) {
        if (object == null) {
            return "";
        }
        ArrayList<String> parts = new ArrayList<>();
        int count = 0;
        for (String key : object.keySet()) {
            String value = objectValue(object, key);
            if (!value.equals("")) {
                parts.add(key + "=" + value);
                count++;
            }
            if (count >= limit) {
                break;
            }
        }
        return String.join(" | ", parts);
    }

    private ArrayList<String> collectIssueLines(JsonArray array) {
        ArrayList<String> lines = new ArrayList<>();
        if (array == null) {
            return lines;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            if (element.isJsonObject()) {
                String line = formatKeyValueObject(element.getAsJsonObject(), "stage", "message", "status");
                if (!line.equals("")) {
                    lines.add(line);
                }
            } else if (element.isJsonPrimitive()) {
                String line = element.getAsString().trim();
                if (!line.equals("")) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private ArrayList<String> collectSourceLines(JsonArray array) {
        ArrayList<String> lines = new ArrayList<>();
        if (array == null) {
            return lines;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            String line = formatKeyValueObject(element.getAsJsonObject(),
                    "type", "method", "network", "status", "latencyMs", "message");
            if (!line.equals("")) {
                lines.add(line);
            }
        }
        return lines;
    }

    private ArrayList<String> collectFindingLines(JsonArray findings) {
        ArrayList<String> lines = new ArrayList<>();
        if (findings == null) {
            return lines;
        }
        for (int i = 0; i < findings.size(); i++) {
            JsonElement element = findings.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject finding = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            String level = objectValue(finding, "level");
            if (!level.equals("")) {
                builder.append("[").append(level).append("] ");
            }
            builder.append(valueOrDash(objectValue(finding, "title")));
            String description = objectValue(finding, "description");
            if (!description.equals("")) {
                builder.append(" | ").append(description);
            }
            String evidence = objectValue(finding, "evidence");
            if (!evidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.evidence")).append("=")
                        .append(evidence);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectOverviewLines(JsonObject reportData) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject summary = getAsObject(reportData, "summary");
        if ("transaction".equals(currentReportType)) {
            JsonObject tx = getAsObject(reportData, "transaction");
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.network"), objectValue(summary, "network"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.status"), objectValue(tx, "status"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.block"), objectValue(tx, "block_no"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.time"), objectValue(tx, "timeText"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.from"), objectValue(tx, "from"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.to"), objectValue(tx, "to"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.value"), valueWithToken(objectValue(tx, "value"), objectValue(tx, "nativeToken")));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.fee"), valueWithToken(objectValue(tx, "fee"), objectValue(tx, "nativeToken")));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.method"), objectValue(tx, "methodId"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.gas"), objectValue(tx, "gasUsed") + "/" + objectValue(tx, "gas"));
        } else {
            JsonObject contract = getAsObject(reportData, "contract");
            JsonObject token = getAsObject(contract, "token");
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.network"), objectValue(summary, "network"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.address"), objectValue(summary, "address"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.address.type"), objectValue(summary, "addressType"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.balance"), objectValue(summary, "balance"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.txcount"), objectValue(summary, "txCount"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.iscontract"), objectBooleanValue(summary, "isContract"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.code.size"), objectValue(contract, "codeSize"));
            if (token != null) {
                appendNamedLine(lines, I18nUtils.getString("blockchain.report.token"),
                        valueOrDash(objectValue(token, "name")) + " / " + valueOrDash(objectValue(token, "symbol")));
                appendNamedLine(lines, I18nUtils.getString("blockchain.report.token.decimals"), objectValue(token, "decimals"));
                String supply = objectValue(token, "totalSupplyFormatted");
                if (supply.equals("")) {
                    supply = objectValue(token, "totalSupply");
                }
                appendNamedLine(lines, I18nUtils.getString("blockchain.report.token.supply"), valueWithToken(supply, objectValue(token, "symbol")));
            }
        }
        return lines;
    }

    private ArrayList<String> collectRoleLines(JsonArray roles) {
        ArrayList<String> lines = new ArrayList<>();
        if (roles == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(roles.size(), 20); i++) {
            JsonElement element = roles.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject role = element.getAsJsonObject();
            ArrayList<String> roleNames = collectTextLines(getAsArray(role, "roles"));
            StringBuilder builder = new StringBuilder();
            builder.append(valueOrDash(objectValue(role, "label")));
            builder.append(" [").append(valueOrDash(objectValue(role, "type"))).append("]");
            builder.append(" | ").append(valueOrDash(objectValue(role, "address")));
            if (!roleNames.isEmpty()) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.role")).append("=")
                        .append(String.join(", ", roleNames));
            }
            String evidence = objectValue(role, "evidence");
            if (!evidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.evidence")).append("=")
                        .append(evidence);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectTimelineLines(JsonArray timeline) {
        ArrayList<String> lines = new ArrayList<>();
        if (timeline == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(timeline.size(), 40); i++) {
            JsonElement element = timeline.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            builder.append(i + 1).append(". ");
            String time = objectValue(item, "timeText");
            if (!time.equals("")) {
                builder.append(time).append(" | ");
            }
            builder.append(valueOrDash(objectValue(item, "title")));
            String description = objectValue(item, "description");
            if (!description.equals("")) {
                builder.append(" | ").append(description);
            }
            String evidence = objectValue(item, "evidence");
            if (!evidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.evidence")).append("=")
                        .append(evidence);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectFlowLines(JsonObject graph) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray edges = getAsArray(graph, "edges");
        if (edges == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(edges.size(), 40); i++) {
            JsonElement element = edges.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject edge = element.getAsJsonObject();
            String amount = valueWithToken(objectValue(edge, "amount"), objectValue(edge, "symbol"));
            lines.add(valueOrDash(objectValue(edge, "source"))
                    + " -> " + valueOrDash(objectValue(edge, "target"))
                    + " | " + I18nUtils.getString("blockchain.table.amount") + "=" + valueOrDash(amount)
                    + " | " + I18nUtils.getString("blockchain.basic.category") + "=" + valueOrDash(objectValue(edge, "category"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence") + "=" + valueOrDash(objectValue(edge, "evidence")));
        }
        return lines;
    }

    private ArrayList<String> collectSequenceLines(JsonObject sequence) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray messages = getAsArray(sequence, "messages");
        if (messages == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(messages.size(), 40); i++) {
            JsonElement element = messages.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject message = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            String order = objectValue(message, "order");
            builder.append(order.equals("") ? String.valueOf(i + 1) : order).append(". ");
            builder.append(valueOrDash(objectValue(message, "from")))
                    .append(" -> ")
                    .append(valueOrDash(objectValue(message, "to")));
            String label = objectValue(message, "label");
            if (!label.equals("")) {
                builder.append(" | ").append(label);
            }
            String eventType = objectValue(message, "eventType");
            if (!eventType.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.basic.event.type"))
                        .append("=").append(eventType);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectRiskLines(JsonArray flags) {
        ArrayList<String> lines = new ArrayList<>();
        if (flags == null) {
            return lines;
        }
        for (int i = 0; i < flags.size(); i++) {
            JsonElement element = flags.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject flag = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            String level = objectValue(flag, "level");
            if (!level.equals("")) {
                builder.append("[").append(level).append("] ");
            }
            builder.append(valueOrDash(objectValue(flag, "title")));
            String description = objectValue(flag, "description");
            if (!description.equals("")) {
                builder.append(" | ").append(description);
            }
            String evidence = objectValue(flag, "evidence");
            if (!evidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.evidence")).append("=")
                        .append(evidence);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectPatternLines(JsonObject semantic) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray patterns = getAsArray(semantic, "riskPatterns");
        if (patterns == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(patterns.size(), 20); i++) {
            JsonElement element = patterns.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject pattern = element.getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            String level = objectValue(pattern, "level");
            if (!level.equals("")) {
                builder.append("[").append(level).append("] ");
            }
            builder.append(valueOrDash(objectValue(pattern, "title")));
            String confidence = objectValue(pattern, "confidence");
            if (!confidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.pattern.confidence"))
                        .append("=").append(confidence);
            }
            String description = objectValue(pattern, "description");
            if (!description.equals("")) {
                builder.append(" | ").append(description);
            }
            String evidence = objectValue(pattern, "evidence");
            if (!evidence.equals("")) {
                builder.append(" | ").append(I18nUtils.getString("blockchain.report.evidence"))
                        .append("=").append(evidence);
            }
            lines.add(builder.toString());
        }
        return lines;
    }

    private ArrayList<String> collectInvestigationLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        if (narrative == null) {
            return lines;
        }
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.narrative.title"), objectValue(narrative, "title"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.narrative.statement"), objectValue(narrative, "statementType"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.narrative.classification"), objectValue(narrative, "classification"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence.malicious"), objectValue(narrative, "maliciousConclusion"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence.confidence"), objectValue(narrative, "confidence"));
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.narrative.executive"),
                collectTextLines(getAsArray(narrative, "executiveSummary")), 8);
        return lines;
    }

    private ArrayList<String> collectNarrativeEntityLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray entities = getAsArray(narrative, "involvedEntities");
        if (entities == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(entities.size(), 18); i++) {
            JsonElement element = entities.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject entity = element.getAsJsonObject();
            lines.add(valueOrDash(objectValue(entity, "role"))
                    + " | " + valueOrDash(objectValue(entity, "entityType"))
                    + " | " + valueOrDash(objectValue(entity, "address"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence.confidence")
                    + "=" + valueOrDash(objectValue(entity, "confidence"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence")
                    + "=" + valueOrDash(objectValue(entity, "evidence")));
        }
        return lines;
    }

    private ArrayList<String> collectNarrativeStepLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray steps = getAsArray(narrative, "attackSteps");
        if (steps == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(steps.size(), 24); i++) {
            JsonElement element = steps.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject step = element.getAsJsonObject();
            String line = valueOrDash(objectValue(step, "order"))
                    + ". " + valueOrDash(objectValue(step, "stage"))
                    + " | " + valueOrDash(objectValue(step, "action"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence")
                    + "=" + valueOrDash(objectValue(step, "evidence"));
            String note = objectValue(step, "note");
            if (!note.equals("")) {
                line += " | " + note;
            }
            lines.add(line);
        }
        return lines;
    }

    private ArrayList<String> collectNarrativeAssetMovementLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject impact = getAsObject(narrative, "impactEstimate");
        if (impact != null) {
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.impact.scope"), objectValue(impact, "scope"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.impact.transfer.count"), objectValue(impact, "transferCount"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.impact.swap.count"), objectValue(impact, "swapCount"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.impact.boundary"), objectValue(impact, "boundary"));
        }
        JsonArray movements = getAsArray(narrative, "assetMovementDetails");
        if (movements != null) {
            for (int i = 0; i < Math.min(movements.size(), 30); i++) {
                JsonElement element = movements.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject movement = element.getAsJsonObject();
                lines.add(valueOrDash(objectValue(movement, "direction"))
                        + " | " + valueWithToken(objectValue(movement, "amount"), objectValue(movement, "asset"))
                        + " | " + valueOrDash(objectValue(movement, "source"))
                        + " -> " + valueOrDash(objectValue(movement, "target"))
                        + " | " + I18nUtils.getString("blockchain.report.evidence")
                        + "=" + valueOrDash(objectValue(movement, "evidence")));
            }
        }
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.impact.notes"),
                collectTextLines(getAsArray(impact, "notes")), 6);
        return lines;
    }

    private ArrayList<String> collectNarrativeFlowLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject diagram = getAsObject(narrative, "fundFlowDiagram");
        JsonArray edges = getAsArray(diagram, "edges");
        if (edges == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(edges.size(), 40); i++) {
            JsonElement element = edges.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject edge = element.getAsJsonObject();
            String amount = valueWithToken(objectValue(edge, "amount"), objectValue(edge, "symbol"));
            lines.add(valueOrDash(objectValue(edge, "source"))
                    + " -> " + valueOrDash(objectValue(edge, "target"))
                    + " | " + I18nUtils.getString("blockchain.table.amount") + "=" + valueOrDash(amount)
                    + " | " + I18nUtils.getString("blockchain.basic.category") + "=" + valueOrDash(objectValue(edge, "category"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence") + "=" + valueOrDash(objectValue(edge, "evidence")));
        }
        return lines;
    }

    private ArrayList<String> collectNarrativeSequenceLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject diagram = getAsObject(narrative, "interactionDiagram");
        JsonArray messages = getAsArray(diagram, "messages");
        if (messages == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(messages.size(), 40); i++) {
            JsonElement element = messages.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject message = element.getAsJsonObject();
            String order = objectValue(message, "order");
            lines.add((order.equals("") ? String.valueOf(i + 1) : order) + ". "
                    + valueOrDash(objectValue(message, "from"))
                    + " -> " + valueOrDash(objectValue(message, "to"))
                    + " | " + valueOrDash(objectValue(message, "label")));
        }
        return lines;
    }

    private ArrayList<String> collectNarrativeAttackAnalysisLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray analysis = getAsArray(narrative, "attackAnalysis");
        if (analysis == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(analysis.size(), 24); i++) {
            JsonElement element = analysis.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            lines.add("[" + valueOrDash(objectValue(item, "level")) + "] "
                    + valueOrDash(objectValue(item, "title"))
                    + " | " + valueOrDash(objectValue(item, "description"))
                    + " | " + I18nUtils.getString("blockchain.report.pattern.confidence")
                    + "=" + valueOrDash(objectValue(item, "confidence"))
                    + " | " + I18nUtils.getString("blockchain.report.evidence")
                    + "=" + valueOrDash(objectValue(item, "evidence")));
        }
        return lines;
    }

    private ArrayList<String> collectNarrativeConclusionLines(JsonObject narrative) {
        ArrayList<String> lines = new ArrayList<>();
        if (narrative == null) {
            return lines;
        }
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.narrative.conclusion"),
                collectTextLines(getAsArray(narrative, "conclusion")), 12);
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.narrative.recommendations"),
                collectTextLines(getAsArray(narrative, "recommendations")), 12);
        return lines;
    }

    private ArrayList<String> collectEvidenceAssessmentLines(JsonObject semantic) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject assessment = getAsObject(semantic, "evidenceAssessment");
        if (assessment == null) {
            return lines;
        }
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence.classification"), objectValue(assessment, "classification"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence.confidence"), objectValue(assessment, "confidence"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence.malicious"), objectValue(assessment, "maliciousConclusion"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.evidence"), objectValue(assessment, "maliciousConclusionNote"));
        JsonObject publicIncident = getAsObject(assessment, "publicIncident");
        if (publicIncident != null) {
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.public.incident"), objectValue(publicIncident, "title"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.public.scenario"), objectValue(publicIncident, "scenario"));
            appendNamedLine(lines, I18nUtils.getString("blockchain.report.public.basis"), objectValue(publicIncident, "basis"));
            appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.public.references"),
                    collectTextLines(getAsArray(publicIncident, "references")), 6);
        }
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.evidence.confirmed"), collectTextLines(getAsArray(assessment, "confirmedFacts")), 8);
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.evidence.inferences"), collectTextLines(getAsArray(assessment, "reasonableInferences")), 8);
        appendPrefixedLines(lines, I18nUtils.getString("blockchain.report.evidence.gaps"), collectTextLines(getAsArray(assessment, "evidenceGaps")), 10);
        return lines;
    }

    private ArrayList<String> collectContextLines(JsonObject semantic) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject context = getAsObject(semantic, "contextWindow");
        if (context == null) {
            return lines;
        }
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.context.scope"), objectValue(context, "scope"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.context.status"), objectValue(context, "status"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.block"), objectValue(context, "block"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.context.inspected"), objectValue(context, "inspectedTxCount"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.report.context.related"), objectValue(context, "relatedTxCount"));
        appendNamedLine(lines, I18nUtils.getString("blockchain.basic.summary"), objectValue(context, "note"));
        JsonArray related = getAsArray(context, "relatedTransactions");
        if (related != null) {
            for (int i = 0; i < Math.min(related.size(), 12); i++) {
                JsonElement element = related.get(i);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                lines.add(valueOrDash(objectValue(item, "txid"))
                        + " | " + I18nUtils.getString("blockchain.report.order") + "=" + valueOrDash(objectValue(item, "transactionIndex"))
                        + " | " + I18nUtils.getString("blockchain.report.context.overlap") + "=" + valueOrDash(objectValue(item, "overlapCount"))
                        + " | " + I18nUtils.getString("blockchain.report.evidence") + "=" + valueOrDash(objectValue(item, "evidence")));
            }
        }
        return lines;
    }

    private void appendPrefixedLines(ArrayList<String> target, String prefix, ArrayList<String> source, int limit) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        int max = Math.min(source.size(), Math.max(1, limit));
        for (int i = 0; i < max; i++) {
            target.add(prefix + "：" + source.get(i));
        }
    }

    private ArrayList<String> collectEventLines(JsonObject evidence) {
        ArrayList<String> lines = new ArrayList<>();
        JsonArray events = getAsArray(evidence, "events");
        if (events == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(events.size(), 60); i++) {
            JsonElement element = events.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject event = element.getAsJsonObject();
            lines.add("#" + valueOrDash(objectValue(event, "logIndex"))
                    + " | " + valueOrDash(objectValue(event, "eventType"))
                    + " | " + valueOrDash(objectValue(event, "category"))
                    + " | " + valueOrDash(objectValue(event, "summary")));
        }
        return lines;
    }

    private ArrayList<String> collectAssetLines(JsonObject reportData) {
        ArrayList<String> lines = new ArrayList<>();
        JsonObject portfolio = getAsObject(reportData, "portfolio");
        JsonArray assets = getAsArray(portfolio, "assets");
        if (assets == null) {
            return lines;
        }
        for (int i = 0; i < Math.min(assets.size(), 30); i++) {
            JsonElement element = assets.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject asset = element.getAsJsonObject();
            lines.add(valueOrDash(objectValue(asset, "name"))
                    + " | " + valueOrDash(objectValue(asset, "symbol"))
                    + " | " + valueOrDash(objectValue(asset, "assetType"))
                    + " | " + valueOrDash(objectValue(asset, "amount"))
                    + " | " + valueOrDash(objectValue(asset, "source")));
        }
        return lines;
    }

    private ArrayList<String> deriveMissingFields(JsonObject reportData) {
        ArrayList<String> lines = collectTextLines(getAsArray(reportData, "missingFields"));
        if (!lines.isEmpty()) {
            return lines;
        }

        JsonObject evidence = getAsObject(reportData, "evidence");
        if (getAsObject(evidence, "receipt") == null && "transaction".equals(currentReportType)) {
            lines.add("receipt");
            lines.add("events");
        }
        if (getAsObject(evidence, "block") == null && "transaction".equals(currentReportType)) {
            lines.add("block");
            lines.add("timestamp");
        }
        if (getAsObject(reportData, "portfolio") == null && "address".equals(currentReportType)) {
            lines.add("portfolio");
        }
        JsonArray warnings = getAsArray(reportData, "warnings");
        if (warnings != null) {
            for (int i = 0; i < warnings.size(); i++) {
                String warning = warnings.get(i).getAsString();
                String lower = warning.toLowerCase();
                if (lower.contains("token portfolio")) {
                    lines.add("fullTokenPortfolio");
                } else if (lower.contains("地址级完整历史交易") || lower.contains("history")) {
                    lines.add("fullAddressTransactions");
                } else if (lower.contains("历史余额") || lower.contains("historical balance")) {
                    lines.add("historicalBalance");
                } else if (lower.contains("对手方") || lower.contains("counterparty")) {
                    lines.add("counterpartyGraph");
                }
            }
        }
        return lines;
    }

    private String resolveReportTypeLabel() {
        if ("transaction".equals(currentReportType)) {
            return I18nUtils.getString("blockchain.export.type.transaction");
        }
        if ("address".equals(currentReportType)) {
            return I18nUtils.getString("blockchain.export.type.address");
        }
        return I18nUtils.getString("main.blue.blockchain");
    }

    private String buildExecutiveSummary(JsonObject reportData, ArrayList<String> analysisLines) {
        JsonObject narrative = getAsObject(reportData, "incidentNarrative");
        ArrayList<String> executiveLines = collectTextLines(getAsArray(narrative, "executiveSummary"));
        if (!executiveLines.isEmpty()) {
            return executiveLines.get(0);
        }
        if (analysisLines != null && !analysisLines.isEmpty()) {
            return analysisLines.get(0);
        }
        JsonObject summary = getAsObject(reportData, "summary");
        if ("transaction".equals(currentReportType)) {
            return I18nUtils.getString("blockchain.export.summary.transaction",
                    valueOrDash(objectValue(summary, "network")),
                    valueOrDash(objectValue(summary, "status")),
                    valueOrDash(objectValue(summary, "time")));
        }
        return I18nUtils.getString("blockchain.export.summary.address",
                valueOrDash(objectValue(summary, "network")),
                valueOrDash(objectValue(summary, "addressType")),
                valueOrDash(objectValue(summary, "balance")));
    }

    private String safeFileNamePart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "report";
        }
        String compact = value.trim();
        if (compact.length() > 30) {
            compact = compact.substring(0, 12) + "_" + compact.substring(compact.length() - 8);
        }
        return compact.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String buildExportFileName() {
        String typePart = "transaction".equals(currentReportType) ? "transaction" : "address";
        return "blockchain_" + typePart + "_" + safeFileNamePart(currentReportNetwork) + "_"
                + safeFileNamePart(currentReportTarget) + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".docx";
    }

    private BlockchainWordReportGenerator.ReportData buildCurrentReportExportData() {
        JsonObject reportData = currentReportData;
        JsonObject meta = getAsObject(reportData, "meta");
        JsonObject processed = getAsObject(reportData, "processedAnalysis");
        JsonObject narrative = getAsObject(reportData, "incidentNarrative");

        ArrayList<String> analysisLines = collectTextLines(getAsArray(narrative, "executiveSummary"));
        if (analysisLines.isEmpty()) {
            analysisLines = collectTextLines(getAsArray(processed, "summary"));
            ArrayList<String> evidenceNotes = collectTextLines(getAsArray(processed, "evidenceNotes"));
            for (String line : evidenceNotes) {
                if (!analysisLines.contains(line)) {
                    analysisLines.add(line);
                }
            }
        }

        ArrayList<String> findingLines = collectFindingLines(getAsArray(reportData, "keyFindings"));
        ArrayList<String> warningLines = collectIssueLines(getAsArray(reportData, "warnings"));
        ArrayList<String> errorLines = collectIssueLines(getAsArray(reportData, "errors"));
        ArrayList<String> sourceLines = collectSourceLines(getAsArray(reportData, "sources"));
        ArrayList<String> missingFieldLines = deriveMissingFields(reportData);
        JsonObject semantic = getAsObject(reportData, "semanticAnalysis");
        ArrayList<String> patternLines = collectPatternLines(semantic);
        ArrayList<String> evidenceAssessmentLines = collectEvidenceAssessmentLines(semantic);
        ArrayList<String> contextLines = collectContextLines(semantic);
        ArrayList<String> investigationLines = collectInvestigationLines(narrative);
        ArrayList<String> narrativeEntityLines = collectNarrativeEntityLines(narrative);
        ArrayList<String> narrativeStepLines = collectNarrativeStepLines(narrative);
        ArrayList<String> narrativeAssetMovementLines = collectNarrativeAssetMovementLines(narrative);
        ArrayList<String> narrativeAttackAnalysisLines = collectNarrativeAttackAnalysisLines(narrative);
        ArrayList<String> narrativeConclusionLines = collectNarrativeConclusionLines(narrative);
        ArrayList<String> eventLines = collectEventLines(getAsObject(reportData, "evidence"));

        String networkLabel = objectValue(meta, "networkName");
        if (networkLabel.equals("")) {
            networkLabel = currentReportNetwork;
        }
        String completeness = formatCompleteness(meta);
        String rawJson = new GsonBuilder().setPrettyPrinting().create().toJson(reportData);
        String reportTitle = objectValue(narrative, "title");
        if (reportTitle.equals("")) {
            reportTitle = I18nUtils.getString("blockchain.export.title");
        }

        return new BlockchainWordReportGenerator.ReportData(
                currentReportType,
                resolveReportTypeLabel(),
                reportTitle,
                valueOrDash(networkLabel),
                valueOrDash(currentReportTarget),
                new Date(),
                completeness,
                buildExecutiveSummary(reportData, analysisLines),
                currentReportAiAnalysis,
                rawJson,
                findingLines.size(),
                analysisLines,
                findingLines,
                warningLines,
                errorLines,
                sourceLines,
                missingFieldLines,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                collectNarrativeFlowLines(narrative),
                collectNarrativeSequenceLines(narrative),
                new ArrayList<>(),
                patternLines,
                evidenceAssessmentLines,
                contextLines,
                investigationLines,
                narrativeEntityLines,
                narrativeStepLines,
                narrativeAssetMovementLines,
                narrativeAttackAnalysisLines,
                narrativeConclusionLines,
                eventLines,
                new ArrayList<>()
        );
    }

    private void showPrompt(String message) {
        showPrompt(message, PromptTone.INFO, Duration.seconds(1.2));
    }

    private void showSuccessPrompt(String message) {
        showPrompt(message, PromptTone.SUCCESS, Duration.seconds(2.0));
    }

    private void showErrorPrompt(String message) {
        showPrompt(message, PromptTone.ERROR, Duration.seconds(1.8));
    }

    private void showPrompt(String message, PromptTone tone, Duration visibleDuration) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showPrompt(message, tone, visibleDuration));
            return;
        }
        if (prompt != null) {
            prompt.setText(message);
            applyPromptTone(tone);
        }
        if (promptPane == null) {
            return;
        }
        if (promptFadeIn != null) {
            promptFadeIn.stop();
        }
        if (promptFadeOut != null) {
            promptFadeOut.stop();
        }

        promptPane.setVisible(true);
        promptPane.setManaged(true);
        promptPane.setOpacity(0);
        promptPane.toFront();

        promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeIn.setFromValue(0);
        promptFadeIn.setToValue(1);

        promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeOut.setFromValue(1);
        promptFadeOut.setToValue(0);
        promptFadeOut.setDelay(visibleDuration);

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();

        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }

    private void applyPromptTone(PromptTone tone) {
        if (promptPane == null) {
            return;
        }
        promptPane.getStyleClass().removeAll("prompt-info", "prompt-success", "prompt-error");
        if (prompt != null) {
            prompt.setStyle(null);
        }
        if (tone == PromptTone.SUCCESS) {
            promptPane.getStyleClass().add("prompt-success");
        } else if (tone == PromptTone.ERROR) {
            promptPane.getStyleClass().add("prompt-error");
        } else {
            promptPane.getStyleClass().add("prompt-info");
        }
    }

    @FXML
    void exportCurrentReport(ActionEvent event) {
        if (currentReportData == null) {
            showPrompt(I18nUtils.getString("blockchain.export.not.ready"));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("blockchain.export.dialog.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18nUtils.getString("blockchain.export.filetype"), "*.docx")
        );

        File defaultDir = new File(StrUtils.getCurrentJarDir(), "BlockchainReports");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        chooser.setInitialFileName(buildExportFileName());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = chooser.showSaveDialog(stage);
        if (selectedFile == null) {
            return;
        }
        if (!selectedFile.getName().toLowerCase().endsWith(".docx")) {
            selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".docx");
        }

        final File targetFile = selectedFile;
        final BlockchainWordReportGenerator.ReportData exportData = buildCurrentReportExportData();
        exportReportButton.setDisable(true);

        Task<File> exportTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                return reportGenerator.generate(exportData, targetFile.getAbsolutePath());
            }
        };
        exportTask.setOnSucceeded(e -> {
            if (currentReportData != null && exportReportButton != null) {
                exportReportButton.setDisable(false);
            }
            File exportedFile = exportTask.getValue();
            showSuccessPrompt(I18nUtils.getString("blockchain.export.success.named",
                    exportedFile.getName(), exportedFile.getAbsolutePath()));
        });
        exportTask.setOnFailed(e -> {
            if (currentReportData != null && exportReportButton != null) {
                exportReportButton.setDisable(false);
            }
            Throwable error = exportTask.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            showErrorPrompt(I18nUtils.getString("blockchain.export.failed",
                    error == null ? I18nUtils.getString("app.unknown") : error.getMessage()));
        });
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
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
        clearCurrentReportContext();
        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(detailedPane.maxWidthProperty(), listViewCell.getWidth()),
                        new KeyValue(detailedListView.prefWidthProperty(), listViewCell.getWidth())),
                new KeyFrame(Duration.seconds(0.2),
                        new KeyValue(detailedPane.maxWidthProperty(), 0),
                        new KeyValue(detailedListView.prefWidthProperty(), 0))
        );
        animation.setOnFinished(event -> {
            accordionPane.getPanes().clear();
            accordionPane.setVisible(false);
            detailedPane.setVisible(false);
            detailedListView.getItems().clear();
        });
        animation.play();
        slideAccordionOut(null);
    }

    @FXML
    public void goBackAndClear(MouseEvent event) {
        question.setText("");
        searchInput();
        backClearLabel.setVisible(false);
    }
}
