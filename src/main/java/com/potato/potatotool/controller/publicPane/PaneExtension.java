package com.potato.potatotool.controller.publicPane;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.classObj.ExtensionConstants;
import com.potato.potatotool.content.redTeam.aiPentest.skill.SkillCategory;
import com.potato.potatotool.content.redTeam.aiPentest.skill.SkillRiskLevel;
import com.potato.potatotool.content.redTeam.aiPentest.skill.extension.ExtensionSkillCandidate;
import com.potato.potatotool.content.redTeam.aiPentest.skill.extension.ExtensionSkillDescriptorEditor;
import com.potato.potatotool.content.redTeam.aiPentest.skill.extension.ExtensionSkillIndexer;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.PaneFactory;
import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.utils.core.Constants.getResourceString;

public class PaneExtension {

    @FXML private StackPane sPane;
    @FXML private StackPane promptPane;
    @FXML private Label prompt;

    // 统计栏
    @FXML private Label statInstalled;
    @FXML private Label statEnabled;
    @FXML private Label statUpdates;

    // 搜索过滤
    @FXML private TextField searchField;
    @FXML private HBox filterChips;

    // 卡片网格
    @FXML private FlowPane cardGrid;

    // 右侧分类面板
    @FXML private VBox categoryNavList;
    @FXML private Label stateRunning;
    @FXML private Label stateUpdate;
    @FXML private Label stateError;

    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    private String currentFilterCategory = null;
    private final List<StackPane> allCardNodes = new ArrayList<>();
    private final Map<String, Image> imageCache = new HashMap<>();

    public void initialize() {
        initData();
        PaneFactory.setActiveController(this);
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));

        cardGrid.widthProperty().addListener((ChangeListener<Number>) (obs, o, n) ->
                resizeCards(n.doubleValue()));
    }

    private void resizeCards(double gridWidth) {
        if (gridWidth <= 0) return;
        double cardW = Math.max((gridWidth - 14 - 32) / 2, 200);
        for (StackPane card : allCardNodes) {
            card.setPrefWidth(cardW);
            card.setMaxWidth(cardW);
        }
    }

    private void initData() {
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(ConfigConstants.EXTENSION);
            if (config == null) {
                String raw = getResourceString("config");
                config = (JsonObject) (new Gson()).fromJson(raw, JsonObject.class).get(ConfigConstants.EXTENSION);
            }

            int total = 0;
            List<String> categoryKeys = new ArrayList<>();

            for (String key : config.keySet()) {
                JsonElement val = config.get(key);
                if (val.isJsonNull()) continue;

                if (val.isJsonArray()) {
                    JsonArray arr = val.getAsJsonArray();
                    categoryKeys.add(key);
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject item = arr.get(i).getAsJsonObject();
                        StackPane card = buildExtCard(key, item);
                        cardGrid.getChildren().add(card);
                        allCardNodes.add(card);
                        total++;
                    }
                } else if (val.isJsonObject()) {
                    JsonObject sub = val.getAsJsonObject();
                    for (String subKey : sub.keySet()) {
                        JsonElement subVal = sub.get(subKey);
                        if (!subVal.isJsonArray()) continue;
                        JsonArray arr = subVal.getAsJsonArray();
                        categoryKeys.add(subKey);
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject item = arr.get(i).getAsJsonObject();
                            StackPane card = buildExtCard(subKey, item);
                            cardGrid.getChildren().add(card);
                            allCardNodes.add(card);
                            total++;
                        }
                    }
                }
            }

            buildFilterChips(categoryKeys);
            buildCategoryNav(config, categoryKeys);

            int finalTotal = total;
            statInstalled.setText(String.valueOf(finalTotal));
            statEnabled.setText(String.valueOf(finalTotal));
            statUpdates.setText("0");
            stateRunning.setText("运行中: " + finalTotal);
            stateUpdate.setText("可更新: 0");
            stateError.setText("错误: 0");

            Platform.runLater(() -> resizeCards(cardGrid.getWidth()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildFilterChips(List<String> keys) {
        filterChips.getChildren().clear();

        Label allChip = new Label("全部");
        allChip.getStyleClass().addAll("ext-filter-chip", "ext-filter-chip-selected");
        allChip.setUserData(null);
        allChip.setOnMouseClicked(e -> filterByCategory(null));
        filterChips.getChildren().add(allChip);

        for (String key : keys) {
            Label chip = new Label(key);
            chip.getStyleClass().add("ext-filter-chip");
            chip.setUserData(key);
            chip.setOnMouseClicked(e -> filterByCategory(key));
            filterChips.getChildren().add(chip);
        }
    }

    private void buildCategoryNav(JsonObject config, List<String> keys) {
        categoryNavList.getChildren().clear();

        // 全部扩展 item (no add icon)
        int total = allCardNodes.size();
        HBox allItem = makeCategoryNavItem("全部扩展", null, total, true);
        categoryNavList.getChildren().add(allItem);

        for (String key : keys) {
            int count = countItemsForKey(config, key);
            HBox navItem = makeCategoryNavItem(key, key, count, false);
            categoryNavList.getChildren().add(navItem);
        }
    }

    private HBox makeCategoryNavItem(String displayName, String key, int count, boolean selected) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("ext-cat-nav-item");
        if (selected) row.getStyleClass().add("ext-cat-nav-selected");
        HBox.setHgrow(row, Priority.ALWAYS);

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("ext-cat-nav-label");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().add("ext-cat-nav-count");

        row.getChildren().addAll(nameLabel, spacer, countLabel);

        if (key != null) {
            // 隐藏 label（存储 key，用于 addDialog 定位）
            Label keyLabel = new Label(key);
            keyLabel.setVisible(false);
            keyLabel.setManaged(false);

            Region addIcon = new Region();
            addIcon.getStyleClass().add("addIcon");
            addIcon.setPrefSize(16, 16);
            addIcon.setMaxSize(16, 16);
            Tooltip.install(addIcon, new Tooltip(I18nUtils.getString("tooltip.add.element")));
            addIcon.setOnMouseClicked(this::addDialog);

            row.getChildren().addAll(keyLabel, addIcon);

            String finalKey = key;
            row.setOnMouseClicked(e -> {
                if (e.getTarget() != addIcon) {
                    filterByCategory(finalKey);
                }
            });
        } else {
            row.setOnMouseClicked(e -> filterByCategory(null));
        }

        return row;
    }

    private int countItemsForKey(JsonObject config, String key) {
        JsonElement found = Constants.findKey(config, key);
        if (found != null && found.isJsonArray()) return found.getAsJsonArray().size();
        return 0;
    }

    private StackPane buildExtCard(String key, JsonObject item) {
        String title   = item.has(ExtensionConstants.FIELD_TITLE)    ? item.get(ExtensionConstants.FIELD_TITLE).getAsString()    : "";
        String describe= item.has(ExtensionConstants.FIELD_DESCRIBE) ? item.get(ExtensionConstants.FIELD_DESCRIBE).getAsString() : "";
        String content = item.has(ExtensionConstants.FIELD_CONTENT)  ? item.get(ExtensionConstants.FIELD_CONTENT).getAsString()  : "";
        String type    = item.has(ExtensionConstants.FIELD_TYPE)     ? item.get(ExtensionConstants.FIELD_TYPE).getAsString()     : "";
        String icon    = item.has(ExtensionConstants.FIELD_ICON)     ? item.get(ExtensionConstants.FIELD_ICON).getAsString()     : "";

        // ---- 图标容器 ----
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("ext-card-icon-box");
        iconBox.setMinSize(38, 38);
        iconBox.setMaxSize(38, 38);

        Image img = loadImageWithCache(icon, type);
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(26);
            iv.setFitHeight(26);
            iv.setPreserveRatio(true);
            Rectangle clip = new Rectangle(26, 26);
            clip.setArcWidth(6);
            clip.setArcHeight(6);
            iv.setClip(clip);
            iconBox.getChildren().add(iv);
        } else {
            Label initial = new Label(title.isEmpty() ? "E" : String.valueOf(title.charAt(0)).toUpperCase());
            initial.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -pt-accent-teal;");
            iconBox.getChildren().add(initial);
        }

        // ---- 名称 + 版本 ----
        Label nameLabel = new Label(title);
        nameLabel.getStyleClass().add("ext-card-name");
        Label versionLabel = new Label("v1.0 · " + type);
        versionLabel.getStyleClass().add("ext-card-version");
        VBox nameBox = new VBox(2, nameLabel, versionLabel);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox headerRow = new HBox(10, iconBox, nameBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // ---- 描述 ----
        Label descLabel = new Label(describe);
        descLabel.getStyleClass().add("ext-card-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        // ---- 底部 footer ----
        Label catChip = new Label(key);
        catChip.getStyleClass().add("ext-cat-chip");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label toggleLabel = new Label("已启用");
        toggleLabel.getStyleClass().add("ext-toggle-on");

        HBox footerRow = new HBox(8, catChip, footerSpacer, toggleLabel);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        // ---- 卡片 body ----
        VBox cardBody = new VBox(10, headerRow, descLabel, footerRow);
        cardBody.getStyleClass().add("ext-card");
        cardBody.setMaxWidth(Double.MAX_VALUE);
        cardBody.setCursor(Cursor.HAND);

        final String finalType = type;
        final String finalContent = content;
        cardBody.setOnMouseClicked(event -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws IOException {
                    start(finalType, finalContent);
                    return null;
                }
            };
            task.setOnFailed(e -> task.getException().printStackTrace());
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });

        // ---- 操作图标叠层 ----
        Region skillIcon  = makeActionIcon("skillIcon",  I18nUtils.getString("tooltip.generate.skill"));
        Region changeIcon = makeActionIcon("changeIcon", I18nUtils.getString("tooltip.modify"));
        Region delIcon    = makeActionIcon("delIcon",    I18nUtils.getString("tooltip.delete"));

        JsonObject skillItem = item.deepCopy();
        String finalKey = key;
        skillIcon.setOnMouseClicked(e  -> { e.consume(); generateSkillDialog(finalKey, skillItem); });
        changeIcon.setOnMouseClicked(this::changeDialog);
        delIcon.setOnMouseClicked(this::delDialog);

        VBox actionOverlay = new VBox(10, skillIcon, changeIcon, delIcon);
        actionOverlay.getStyleClass().add("regionVBox");
        actionOverlay.setMaxWidth(20);

        // ---- 根节点 ----
        StackPane cardRoot = new StackPane(cardBody, actionOverlay);
        StackPane.setAlignment(actionOverlay, Pos.TOP_RIGHT);
        StackPane.setMargin(actionOverlay, new Insets(6, 8, 0, 0));
        cardRoot.getStyleClass().add("stackPane");

        Map<String, String> meta = new HashMap<>();
        meta.put("key",      key);
        meta.put("title",    title);
        meta.put("describe", describe);
        meta.put("content",  content);
        meta.put("type",     type);
        cardRoot.setUserData(meta);

        Tooltip.install(cardRoot, new Tooltip(describe + "\n" + type + ": " + content));

        return cardRoot;
    }

    private Region makeActionIcon(String styleClass, String tooltipText) {
        Region r = new Region();
        r.getStyleClass().add(styleClass);
        r.setPrefSize(20, 20);
        r.setMaxSize(20, 20);
        Tooltip.install(r, new Tooltip(tooltipText));
        return r;
    }

    @FXML
    private void filterExtensions(KeyEvent e) {
        String query = searchField.getText().trim().toLowerCase();
        for (StackPane card : allCardNodes) {
            @SuppressWarnings("unchecked")
            Map<String, String> meta = (Map<String, String>) card.getUserData();
            String inCategory = meta != null ? meta.get("key") : "";
            boolean categoryMatch = currentFilterCategory == null || currentFilterCategory.equals(inCategory);
            if (query.isEmpty()) {
                card.setVisible(categoryMatch);
                card.setManaged(categoryMatch);
            } else {
                String title    = meta != null ? meta.getOrDefault("title", "").toLowerCase()    : "";
                String describe = meta != null ? meta.getOrDefault("describe", "").toLowerCase() : "";
                boolean textMatch = title.contains(query) || describe.contains(query)
                        || (inCategory != null && inCategory.toLowerCase().contains(query));
                boolean show = categoryMatch && textMatch;
                card.setVisible(show);
                card.setManaged(show);
            }
        }
    }

    @FXML
    private void addGlobalExtension(ActionEvent e) {
        PaneFactory.setActiveController(this);
        String key = currentFilterCategory;
        if (key == null || key.isEmpty()) {
            // 使用第一个可用分类
            for (StackPane card : allCardNodes) {
                @SuppressWarnings("unchecked")
                Map<String, String> meta = (Map<String, String>) card.getUserData();
                if (meta != null && meta.get("key") != null) {
                    key = meta.get("key");
                    break;
                }
            }
        }
        if (key == null) return;
        openAddDialog(key);
    }

    @FXML
    private void refreshData(ActionEvent e) {
        reloadData();
    }

    private void filterByCategory(String categoryKey) {
        currentFilterCategory = categoryKey;
        searchField.clear();

        // 更新 chip 高亮
        for (Node chip : filterChips.getChildren()) {
            if (chip instanceof Label) {
                Object ud = chip.getUserData();
                boolean selected = (categoryKey == null && ud == null)
                        || (categoryKey != null && categoryKey.equals(ud));
                chip.getStyleClass().remove("ext-filter-chip-selected");
                if (selected) chip.getStyleClass().add("ext-filter-chip-selected");
            }
        }

        // 更新分类导航高亮
        for (Node navItem : categoryNavList.getChildren()) {
            if (navItem instanceof HBox) {
                HBox hbox = (HBox) navItem;
                hbox.getStyleClass().remove("ext-cat-nav-selected");
                for (Node child : hbox.getChildren()) {
                    if (child instanceof Label && child.isVisible()) {
                        String text = ((Label) child).getText();
                        boolean isAll = "全部扩展".equals(text) && categoryKey == null;
                        boolean isCat = categoryKey != null && categoryKey.equals(text);
                        if (isAll || isCat) {
                            hbox.getStyleClass().add("ext-cat-nav-selected");
                        }
                        break;
                    }
                }
            }
        }

        // 显示/隐藏卡片
        for (StackPane card : allCardNodes) {
            @SuppressWarnings("unchecked")
            Map<String, String> meta = (Map<String, String>) card.getUserData();
            String cardKey = meta != null ? meta.get("key") : null;
            boolean show = categoryKey == null || categoryKey.equals(cardKey);
            card.setVisible(show);
            card.setManaged(show);
        }
    }

    public void reloadData() {
        cardGrid.getChildren().clear();
        categoryNavList.getChildren().clear();
        filterChips.getChildren().clear();
        allCardNodes.clear();
        currentFilterCategory = null;
        initData();
    }

    public void updateCategory(String key) {
        reloadData();
    }

    // ──────────────────────────────────────────────────────────────
    // 启动扩展
    // ──────────────────────────────────────────────────────────────

    public void start(String type, String content) {
        if (content.isEmpty()) return;
        if (type.equals("cmd")) {
            String[] command = parseCommand(content);
            try {
                new ProcessBuilder(command).start();
            } catch (Exception e) {
                showTip(e.toString());
                e.printStackTrace();
            }
        } else if (type.equals("web")) {
            HostServices services = MainApplication.letGetHostServices();
            services.showDocument(content);
        }
    }

    private String[] parseCommand(String command) {
        List<String> commands = new ArrayList<>();
        Matcher matcher = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(command);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                commands.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                commands.add(matcher.group(2));
            } else {
                commands.add(matcher.group());
            }
        }
        return commands.toArray(new String[0]);
    }

    // ──────────────────────────────────────────────────────────────
    // 图片缓存
    // ──────────────────────────────────────────────────────────────

    private Image loadImageWithCache(String icon, String type) {
        if (imageCache.containsKey(icon)) return imageCache.get(icon);
        Image image = null;
        try {
            if (icon.equals("") && type.equals("cmd"))      icon = "/img/bar/cmd.png";
            else if (icon.equals("") && type.equals("web")) icon = "/img/bar/web.png";

            if (icon.startsWith("/img/bar/")) {
                try (InputStream is = getClass().getResourceAsStream(icon)) {
                    if (is != null) {
                        image = new Image(is);
                        imageCache.put(icon, image);
                    }
                }
            } else {
                image = new Image(new File(icon).toURI().toString());
                if (image.isError()) {
                    icon = type.equals("cmd") ? "/img/bar/cmd.png" : "/img/bar/web.png";
                    if (icon.startsWith("/img/bar/")) {
                        try (InputStream is = getClass().getResourceAsStream(icon)) {
                            if (is != null) { image = new Image(is); imageCache.put(icon, image); }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    // ──────────────────────────────────────────────────────────────
    // Skill 对话框
    // ──────────────────────────────────────────────────────────────

    private void generateSkillDialog(String key, JsonObject item) {
        try {
            ExtensionSkillCandidate candidate = new ExtensionSkillIndexer().candidateFromItem(key, item);
            if (candidate == null) {
                showTip(I18nUtils.getString("extension.skill.invalid"));
                return;
            }
            ExtensionSkillDescriptorEditor editor = new ExtensionSkillDescriptorEditor();
            if (!reviewSkillDraftDialog(candidate, editor)) return;
            if (editor.saveDraftDefinition(candidate)) {
                showTip(I18nUtils.getString("extension.skill.saved", candidate.getSkillId()));
            } else {
                showTip(I18nUtils.getString("extension.skill.failed"));
            }
        } catch (Exception e) {
            showTip(I18nUtils.getString("extension.skill.failed"));
            e.printStackTrace();
        }
    }

    private boolean reviewSkillDraftDialog(ExtensionSkillCandidate candidate, ExtensionSkillDescriptorEditor editor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(sPane.getScene().getWindow());
        dialog.setTitle(I18nUtils.getString("extension.skill.dialog.title"));
        dialog.setHeaderText(I18nUtils.getString("extension.skill.dialog.header", candidate.getTitle()));
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<SkillCategory> categoryBox = new ComboBox<>(FXCollections.observableArrayList(SkillCategory.values()));
        categoryBox.setValue(candidate.getSuggestedCategory());
        ComboBox<SkillRiskLevel> riskBox = new ComboBox<>(FXCollections.observableArrayList(SkillRiskLevel.values()));
        riskBox.setValue(candidate.getSuggestedRiskLevel());
        CheckBox scopeRequired    = new CheckBox(I18nUtils.getString("extension.skill.review.scopeRequired"));
        scopeRequired.setSelected(candidate.isScopeRequired());
        CheckBox approvalRequired = new CheckBox(I18nUtils.getString("extension.skill.review.approvalRequired"));
        approvalRequired.setSelected(candidate.isApprovalRequired());

        TextField matchHints  = new TextField(joinValues(candidate.getMatchHints(), ", "));
        TextArea  inputSchema = smallTextArea(schemaToText(candidate.getInputSchema()));
        TextArea  outputSchema= smallTextArea(schemaToText(candidate.getOutputSchema()));
        TextArea  reviewNote  = smallTextArea(candidate.getReviewNote());
        reviewNote.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 0, 0));
        int row = 0;
        grid.add(new Label(I18nUtils.getString("extension.skill.review.category")), 0, row);
        grid.add(categoryBox, 1, row++);
        grid.add(new Label(I18nUtils.getString("extension.skill.review.risk")), 0, row);
        grid.add(riskBox, 1, row++);
        grid.add(scopeRequired, 1, row++);
        grid.add(approvalRequired, 1, row++);
        grid.add(new Label(I18nUtils.getString("extension.skill.review.matchHints")), 0, row);
        grid.add(matchHints, 1, row++);
        grid.add(new Label(I18nUtils.getString("extension.skill.review.inputSchema")), 0, row);
        grid.add(inputSchema, 1, row++);
        grid.add(new Label(I18nUtils.getString("extension.skill.review.outputSchema")), 0, row);
        grid.add(outputSchema, 1, row++);
        grid.add(new Label(I18nUtils.getString("extension.skill.review.note")), 0, row);
        grid.add(reviewNote, 1, row);

        Label boundary = new Label(I18nUtils.getString("extension.skill.dialog.content"));
        boundary.setWrapText(true);
        Label schemaHelp = new Label(I18nUtils.getString("extension.skill.review.schemaHelp"));
        schemaHelp.setWrapText(true);
        TextArea preview = smallTextArea(new Gson().toJson(editor.toDraftRow(candidate)));
        preview.setEditable(false);
        preview.setPrefRowCount(6);
        VBox content = new VBox(8, boundary, grid, schemaHelp,
                new Label(I18nUtils.getString("extension.skill.review.preview")), preview);
        content.setPrefWidth(720);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (!result.orElse(ButtonType.CANCEL).equals(ButtonType.OK)) return false;
        editor.applyReviewMetadata(candidate, categoryBox.getValue(), riskBox.getValue(),
                scopeRequired.isSelected(), approvalRequired.isSelected(),
                splitValues(matchHints.getText()), parseSchemaText(inputSchema.getText()),
                parseSchemaText(outputSchema.getText()), reviewNote.getText());
        return true;
    }

    private TextArea smallTextArea(String value) {
        TextArea ta = new TextArea(value == null ? "" : value);
        ta.setWrapText(true);
        ta.setPrefRowCount(3);
        ta.setPrefColumnCount(48);
        return ta;
    }

    private String schemaToText(Map<String, String> schema) {
        StringBuilder b = new StringBuilder();
        if (schema != null) {
            for (Map.Entry<String, String> e : schema.entrySet()) {
                if (b.length() > 0) b.append("\n");
                b.append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return b.toString();
    }

    private Map<String, String> parseSchemaText(String text) {
        Map<String, String> schema = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) return schema;
        for (String line : text.split("\\r?\\n")) {
            String t = line == null ? "" : line.trim();
            if (t.isEmpty()) continue;
            int idx = t.indexOf('=');
            if (idx <= 0) schema.put(t, "string");
            else schema.put(t.substring(0, idx).trim(), t.substring(idx + 1).trim());
        }
        return schema;
    }

    private List<String> splitValues(String text) {
        List<String> values = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return values;
        for (String v : Arrays.asList(text.split(","))) {
            String t = v == null ? "" : v.trim();
            if (!t.isEmpty()) values.add(t);
        }
        return values;
    }

    private String joinValues(List<String> values, String delimiter) {
        StringBuilder b = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                if (v == null || v.trim().isEmpty()) continue;
                if (b.length() > 0) b.append(delimiter);
                b.append(v.trim());
            }
        }
        return b.toString();
    }

    // ──────────────────────────────────────────────────────────────
    // 删除 / 修改 / 添加 对话框
    // ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, String> getCardMeta(MouseEvent event) {
        Node node = (Node) event.getSource();
        // source → regionVBox (VBox) → cardRoot (StackPane)
        if (node.getParent() != null && node.getParent().getParent() instanceof StackPane) {
            Object ud = node.getParent().getParent().getUserData();
            if (ud instanceof Map) return (Map<String, String>) ud;
        }
        return null;
    }

    private void delDialog(MouseEvent even) {
        PaneFactory.setActiveController(this);
        Map<String, String> meta = getCardMeta(even);
        if (meta == null) return;

        String key      = meta.get("key");
        String title    = meta.get("title");
        String describe = meta.get("describe");

        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/publicPane/deleteConfirmDialog.fxml"));
            AnchorPane dialogRoot = loader.load();
            PaneDeleteConfirmDialog controller = loader.getController();
            controller.setDeleteInfo(title, describe);

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
            scene.setFill(null);
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("delete.window.title"));
            stage.showAndWait();

            if (PaneDeleteConfirmDialog.deleteConfirmed) {
                JsonObject config = (JsonObject) Constants.getOutsideConfig(ConfigConstants.EXTENSION);
                JsonElement targetValue = Constants.findKey(config, key);
                if (targetValue != null && targetValue.isJsonArray()) {
                    JsonArray arr = (JsonArray) targetValue;
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = (JsonObject) arr.get(i);
                        if (obj.get(ExtensionConstants.FIELD_TITLE).getAsString().equals(title)
                                && obj.get(ExtensionConstants.FIELD_DESCRIBE).getAsString().equals(describe)) {
                            arr.remove(i);
                            break;
                        }
                    }
                }
                Constants.saveConfig(ConfigConstants.EXTENSION, config);
                updateCategory(key);
                PaneFactory.syncCategoryFrom(key, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeDialog(MouseEvent even) {
        PaneFactory.setActiveController(this);
        Map<String, String> meta = getCardMeta(even);
        if (meta == null) return;

        Label newLabelKey      = makeHiddenLabel(meta.get("key"),     "labelKey");
        Label newLabelTitle    = makeHiddenLabel(meta.get("title"),   "labelTitle");
        Label newLabelDescribe = makeHiddenLabel(meta.get("describe"),"labelDescribe");

        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/publicPane/addBarDialog.fxml")).load();
            dialogRoot.getChildren().addAll(newLabelKey, newLabelTitle, newLabelDescribe);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText(I18nUtils.getString("addbar.submit.modify"));

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
            scene.setFill(null);
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("addbar.window.title.modify"));
            stage.show();

            stage.setOnHidden(eventx -> {
                if (PaneAddBarDialog.dataSaved) {
                    String key = newLabelKey.getText();
                    if (key != null && !key.isEmpty()) updateCategory(key);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 被分类导航 addIcon 调用（保留原有 addDialog 签名）
    private void addDialog(MouseEvent event) {
        PaneFactory.setActiveController(this);
        Region regionAdd = (Region) event.getSource();
        Parent parent = regionAdd.getParent();
        int regionAddIndex = parent.getChildrenUnmodifiable().indexOf(regionAdd);
        Label labelKey = (Label) parent.getChildrenUnmodifiable().get(regionAddIndex - 1);
        openAddDialog(labelKey.getText());
    }

    private void openAddDialog(String key) {
        Label newLabelKey = makeHiddenLabel(key, "labelKey");
        try {
            Stage stage = new Stage();
            stage.initOwner(sPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/publicPane/addBarDialog.fxml")).load();
            dialogRoot.getChildren().add(newLabelKey);

            Button addBtn = (Button) dialogRoot.lookup("#addBtn");
            addBtn.setText(I18nUtils.getString("addbar.submit.add"));

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
            scene.setFill(null);
            stage.setScene(scene);
            stage.setTitle(I18nUtils.getString("addbar.window.title.add"));
            stage.show();

            stage.setOnHidden(eventx -> {
                if (PaneAddBarDialog.dataSaved) {
                    String k = newLabelKey.getText();
                    if (k != null && !k.isEmpty()) updateCategory(k);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Label makeHiddenLabel(String text, String id) {
        Label l = new Label(text);
        l.setVisible(false);
        l.setManaged(false);
        l.setId(id);
        return l;
    }

    // ──────────────────────────────────────────────────────────────
    // 提示 Toast
    // ──────────────────────────────────────────────────────────────

    public void showTip(String tip) {
        Platform.runLater(() -> {
            prompt.setText(tip);
            copyAnimation();
        });
    }

    public void copyAnimation() {
        if (promptPane == null) return;
        if (promptFadeIn  != null) promptFadeIn.stop();
        if (promptFadeOut != null) promptFadeOut.stop();

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
        promptFadeOut.setDelay(Duration.seconds(1));

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();
        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }
}
