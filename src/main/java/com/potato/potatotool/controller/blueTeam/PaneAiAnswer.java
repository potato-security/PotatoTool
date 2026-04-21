package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.AiPromptUtils;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiUiState;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneAiAnswer {
    private static final int MAX_ATTACHMENT_COUNT = 5;
    private static final int MAX_ATTACHMENT_READ_BYTES = 96 * 1024;
    private static final int MAX_TEXT_ATTACHMENT_CHARS = 24_000;
    private static final int MAX_BINARY_PREVIEW_BYTES = 4096;

    @FXML
    private StackPane sPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextArea question;

    @FXML
    private VBox msgBox;

    @FXML
    private Label sendAction;

    @FXML
    private HBox attachmentBar;

    @FXML
    private Button attachButton;

    @FXML
    private Label attachmentLabel;

    @FXML
    private Hyperlink clearAttachmentLink;

    @FXML
    private HBox statusBar;

    @FXML
    private Label statusLabel;

    @FXML
    private Hyperlink retryLink;

    private final AiChatService aiChatService;
    private final AiConfigReader aiConfigReader;
    private final List<AiPromptUtils.AttachmentContext> attachmentContexts =
            new ArrayList<AiPromptUtils.AttachmentContext>();

    private AiUiState uiState = AiUiState.IDLE;
    private Task<Void> runningTask;
    private Timeline statusAnimation;
    private String lastQuestion;

    public PaneAiAnswer() {
        this(new AiChatService(), new AiConfigReader());
    }

    PaneAiAnswer(AiChatService aiChatService, AiConfigReader aiConfigReader) {
        this.aiChatService = aiChatService;
        this.aiConfigReader = aiConfigReader;
    }

    public void initialize() {
        question.skinProperty().addListener((ob, ov, nv) -> {
            ScrollPane lookup = (ScrollPane) question.lookup(".scroll-pane");
            if (lookup == null) {
                return;
            }
            lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group) ((Region) lookup.getContent()).getChildrenUnmodifiable().get(1))
                    .getChildren().get(0);
            t.layoutBoundsProperty().addListener((o, n, v) -> {
                if (v.getHeight() == 0) {
                    return;
                }
                double newHeight = v.getHeight() == 23 ? 35 : v.getHeight() + 35;
                if (newHeight >= question.getMaxHeight()) {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    newHeight = question.getMaxHeight();
                } else {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
                double finalNewHeight = newHeight;
                Platform.runLater(() -> {
                    question.setPrefHeight(finalNewHeight);
                    question.requestLayout();
                    refreshScrollPaneHeight();
                });
            });
        });

        msgBox.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));
        sPane.heightProperty().addListener((observable, oldValue, newValue) -> refreshScrollPaneHeight());
        attachmentBar.heightProperty().addListener((observable, oldValue, newValue) -> refreshScrollPaneHeight());
        statusBar.heightProperty().addListener((observable, oldValue, newValue) -> refreshScrollPaneHeight());

        question.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER
                    && !event.isShiftDown()
                    && !event.isControlDown()
                    && !event.isAltDown()) {
                ask();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                question.appendText(System.getProperty("line.separator"));
            }
        });

        refreshAttachmentSummary();
        setState(AiUiState.IDLE, "");

        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            refreshAttachmentSummary();
            refreshScrollPaneHeight();
        });
    }

    @FXML
    void ask() {
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }

        String rawQuestion = question.getText().trim();
        if (rawQuestion.isEmpty() && attachmentContexts.isEmpty()) {
            return;
        }

        lastQuestion = rawQuestion;
        question.clear();

        String visibleQuestion = rawQuestion.isEmpty()
                ? I18nUtils.getString("ai.attach.default.question")
                : rawQuestion;
        String requestPrompt = AiPromptUtils.buildConversationPrompt(rawQuestion, attachmentContexts);
        String historyPrompt = AiPromptUtils.buildConversationHistoryLabel(rawQuestion, attachmentContexts);

        TextArea myTextArea = createBubbleTextArea("myMsg");
        myTextArea.setText(buildUserBubbleText(visibleQuestion));
        adaptiveSize(myTextArea);

        Region myRegion = new Region();
        myRegion.getStyleClass().add("myAvatarImg");
        Pane mySpacer = new Pane();
        mySpacer.setMinWidth(17);

        HBox myHBox = new HBox();
        myHBox.setSpacing(10);
        myHBox.setAlignment(Pos.TOP_RIGHT);
        myHBox.getChildren().addAll(myTextArea, myRegion, mySpacer);
        myHBox.setMaxWidth(sPane.getWidth() - 20);
        msgBox.getChildren().add(myHBox);

        TextArea aiTextArea = createBubbleTextArea("aiMsg");
        adaptiveSize(aiTextArea);

        VBox aiContent = new VBox();
        aiContent.setSpacing(6);

        TextArea thinkingTextArea = createBubbleTextArea("aiMsg");
        thinkingTextArea.setVisible(false);
        thinkingTextArea.setManaged(false);
        thinkingTextArea.setEditable(false);
        thinkingTextArea.setWrapText(true);
        adaptiveSize(thinkingTextArea);

        aiContent.getChildren().addAll(thinkingTextArea, aiTextArea);

        Pane aiSpacer = new Pane();
        aiSpacer.setMinWidth(20);
        Region aiRegion = new Region();
        aiRegion.getStyleClass().add("aiAvatarImg");

        HBox aiHBox = new HBox();
        aiHBox.setSpacing(10);
        aiHBox.setAlignment(Pos.TOP_LEFT);
        aiHBox.getChildren().addAll(aiSpacer, aiRegion, aiContent);
        aiHBox.setMaxWidth(sPane.getWidth() - 20);
        msgBox.getChildren().add(aiHBox);

        startAskTask(requestPrompt, historyPrompt, aiTextArea, thinkingTextArea);
    }

    @FXML
    void retryLastQuestion() {
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }
        question.setText(lastQuestion == null ? "" : lastQuestion);
        ask();
    }

    @FXML
    void chooseAttachments() {
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }

        int remaining = MAX_ATTACHMENT_COUNT - attachmentContexts.size();
        if (remaining <= 0) {
            setState(AiUiState.ERROR, I18nUtils.getString("ai.attach.limit", MAX_ATTACHMENT_COUNT), false);
            return;
        }

        Window owner = sPane == null || sPane.getScene() == null ? null : sPane.getScene().getWindow();
        if (owner == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("ai.attach"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        I18nUtils.getString("ai.attach.filter.text"),
                        "*.txt", "*.log", "*.md", "*.json", "*.xml", "*.yml", "*.yaml",
                        "*.ini", "*.conf", "*.config", "*.csv", "*.java", "*.js", "*.ts",
                        "*.py", "*.php", "*.jsp", "*.html", "*.htm", "*.css", "*.sql",
                        "*.sh", "*.bat", "*.ps1", "*.properties", "*.class"
                ),
                new FileChooser.ExtensionFilter(I18nUtils.getString("ai.attach.filter.all"), "*.*")
        );

        List<File> files = chooser.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) {
            return;
        }

        String errorMessage = null;
        int processed = 0;
        for (File file : files) {
            if (file == null || processed >= remaining) {
                break;
            }
            try {
                attachmentContexts.add(loadAttachmentContext(file));
                processed++;
            } catch (Exception e) {
                errorMessage = e.getMessage() == null ? file.getName() : e.getMessage();
                break;
            }
        }

        refreshAttachmentSummary();
        refreshScrollPaneHeight();

        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            setState(AiUiState.ERROR, I18nUtils.getString("ai.attach.read.failed", errorMessage), false);
            return;
        }
        if (files.size() > remaining) {
            setState(AiUiState.ERROR, I18nUtils.getString("ai.attach.limit", MAX_ATTACHMENT_COUNT), false);
            return;
        }
        setState(AiUiState.IDLE, "");
    }

    @FXML
    void clearAttachments() {
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }
        attachmentContexts.clear();
        refreshAttachmentSummary();
        refreshScrollPaneHeight();
        if (uiState == AiUiState.ERROR) {
            setState(AiUiState.IDLE, "");
        }
    }

    private void startAskTask(String requestPrompt,
                              String historyPrompt,
                              TextArea aiTextArea,
                              TextArea thinkingTextArea) {
        runningTask = new Task<Void>() {
            @Override
            protected Void call() {
                setState(AiUiState.LOADING, I18nUtils.getString("ai.status.loading"));
                aiChatService.streamChat(
                        AiPromptUtils.generalAssistantSystemPrompt(),
                        requestPrompt,
                        historyPrompt,
                        event -> handleStreamEvent(event, aiTextArea, thinkingTextArea)
                );
                return null;
            }
        };

        runningTask.setOnFailed(event -> {
            Throwable error = runningTask.getException();
            String message = error == null ? I18nUtils.getString("ai.status.error") : error.getMessage();
            setState(AiUiState.ERROR, message == null ? I18nUtils.getString("ai.status.error") : message);
        });

        runningTask.setOnSucceeded(event -> {
            if (uiState != AiUiState.ERROR) {
                setState(AiUiState.COMPLETED, I18nUtils.getString("ai.status.completed"));
                Platform.runLater(() -> setState(AiUiState.IDLE, ""));
            }
        });

        Thread thread = new Thread(runningTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleStreamEvent(AiStreamEvent event,
                                   TextArea aiTextArea,
                                   TextArea thinkingTextArea) {
        switch (event.getType()) {
            case THINKING_TOKEN:
                Platform.runLater(() -> {
                    if (!isThinkingEnabled()) {
                        return;
                    }
                    if (!thinkingTextArea.isManaged()) {
                        thinkingTextArea.setManaged(true);
                        thinkingTextArea.setVisible(true);
                    }
                    if (uiState == AiUiState.LOADING || uiState == AiUiState.IDLE) {
                        setState(AiUiState.THINKING, I18nUtils.getString("ai.status.thinking"));
                    }
                    thinkingTextArea.appendText(event.getContent());
                });
                break;
            case TOKEN:
                Platform.runLater(() -> {
                    if (uiState != AiUiState.STREAMING) {
                        setState(AiUiState.STREAMING, I18nUtils.getString("ai.status.streaming"));
                    }
                    aiTextArea.appendText(event.getContent());
                });
                break;
            case ERROR:
                setState(AiUiState.ERROR, event.getContent());
                break;
            case DONE:
                if (uiState != AiUiState.ERROR) {
                    setState(AiUiState.COMPLETED, I18nUtils.getString("ai.status.completed"));
                }
                break;
            default:
                break;
        }
    }

    private TextArea createBubbleTextArea(String styleClass) {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(0);
        textArea.setPrefWidth(0);
        textArea.setMinSize(40, 44);
        textArea.setMaxSize(sPane.getWidth() - 170, 500);
        textArea.getStyleClass().add(styleClass);
        return textArea;
    }

    private void setState(AiUiState newState, String message) {
        setState(newState, message, newState == AiUiState.ERROR);
    }

    private void setState(AiUiState newState, String message, boolean showRetry) {
        Platform.runLater(() -> {
            uiState = newState;
            boolean busy = newState == AiUiState.LOADING
                    || newState == AiUiState.THINKING
                    || newState == AiUiState.STREAMING;

            question.setDisable(busy);
            sendAction.setDisable(busy);
            if (attachButton != null) {
                attachButton.setDisable(busy);
            }
            if (clearAttachmentLink != null) {
                clearAttachmentLink.setDisable(busy || attachmentContexts.isEmpty());
            }

            boolean showStatus = message != null && !message.trim().isEmpty();
            statusLabel.setManaged(showStatus);
            statusLabel.setVisible(showStatus);

            statusLabel.getStyleClass().removeAll(Arrays.asList(
                    "ai-status-loading",
                    "ai-status-thinking",
                    "ai-status-streaming",
                    "ai-status-error",
                    "ai-status-completed"
            ));
            switch (newState) {
                case LOADING:
                    statusLabel.getStyleClass().add("ai-status-loading");
                    break;
                case THINKING:
                    statusLabel.getStyleClass().add("ai-status-thinking");
                    break;
                case STREAMING:
                    statusLabel.getStyleClass().add("ai-status-streaming");
                    break;
                case ERROR:
                    statusLabel.getStyleClass().add("ai-status-error");
                    break;
                case COMPLETED:
                    statusLabel.getStyleClass().add("ai-status-completed");
                    break;
                default:
                    break;
            }

            applyStatusMessage(newState, message);

            retryLink.setManaged(showRetry);
            retryLink.setVisible(showRetry);
        });
    }

    private void applyStatusMessage(AiUiState newState, String message) {
        boolean animate = (newState == AiUiState.LOADING
                || newState == AiUiState.THINKING
                || newState == AiUiState.STREAMING)
                && message != null
                && !message.trim().isEmpty();
        if (!animate) {
            stopStatusAnimation();
            statusLabel.setText(message == null ? "" : message);
            return;
        }

        final String baseText = stripTrailingDots(message);
        if (baseText.isEmpty()) {
            stopStatusAnimation();
            statusLabel.setText(message);
            return;
        }

        stopStatusAnimation();
        final String[] frames = new String[]{
                baseText,
                baseText + ".",
                baseText + "..",
                baseText + "..."
        };
        final int[] index = new int[]{0};
        statusLabel.setText(frames[0]);
        statusAnimation = new Timeline(new KeyFrame(Duration.millis(420), event -> {
            index[0] = (index[0] + 1) % frames.length;
            statusLabel.setText(frames[index[0]]);
        }));
        statusAnimation.setCycleCount(Animation.INDEFINITE);
        statusAnimation.playFromStart();
    }

    private void stopStatusAnimation() {
        if (statusAnimation != null) {
            statusAnimation.stop();
            statusAnimation = null;
        }
    }

    private String stripTrailingDots(String message) {
        if (message == null) {
            return "";
        }
        return message.trim().replaceAll("[.。…]+$", "");
    }

    boolean isThinkingEnabled() {
        return PaneAiAnswerSupport.isThinkingEnabled(aiConfigReader);
    }

    void adaptiveSize(TextArea textArea) {
        Text tmpText = new Text();
        textArea.skinProperty().addListener((ob, ov, nv) -> {
            double maxWidth = sPane.getWidth() - 170;

            ScrollPane lookup1 = (ScrollPane) textArea.lookup(".scroll-pane");
            if (lookup1 == null) {
                return;
            }
            lookup1.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            lookup1.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group) ((Region) lookup1.getContent()).getChildrenUnmodifiable().get(1))
                    .getChildren().get(0);
            lookup1.getContent().setStyle("-fx-padding: 0");

            tmpText.setText(t.getText());
            tmpText.setFont(t.getFont());

            double initWidth = tmpText.getLayoutBounds().getWidth() + 22 + 24;
            initWidth = initWidth > maxWidth ? maxWidth : initWidth;
            textArea.setMaxWidth(initWidth);
            textArea.setPrefWidth(initWidth);
            textArea.setPrefHeight(tmpText.getLayoutBounds().getHeight() + 22);

            textArea.textProperty().addListener((observable, oldValue, newValue) -> {
                tmpText.setText(t.getText());
                tmpText.setFont(t.getFont());

                double finalHeight = t.getLayoutBounds().getHeight() + 22;
                double finalWidth = tmpText.getLayoutBounds().getWidth() + 22 + 24;
                finalWidth = finalWidth > maxWidth ? maxWidth : finalWidth;

                textArea.setPrefHeight(finalHeight);
                textArea.setMaxWidth(finalWidth);
                textArea.setPrefWidth(finalWidth);
            });
        });
    }

    private void refreshAttachmentSummary() {
        boolean hasAttachments = !attachmentContexts.isEmpty();
        attachmentLabel.setText(hasAttachments
                ? I18nUtils.getString("ai.attach.summary", attachmentContexts.size(), summarizeAttachmentNames())
                : I18nUtils.getString("ai.attach.none"));
        clearAttachmentLink.setManaged(hasAttachments);
        clearAttachmentLink.setVisible(hasAttachments);
        clearAttachmentLink.setDisable((runningTask != null && runningTask.isRunning()) || !hasAttachments);
    }

    private void refreshScrollPaneHeight() {
        if (scrollPane == null || sPane == null || question == null) {
            return;
        }
        double reservedHeight = question.getPrefHeight() + safeHeight(attachmentBar) + safeHeight(statusBar) + 48;
        double targetHeight = sPane.getHeight() - reservedHeight;
        if (targetHeight > 120) {
            scrollPane.setPrefHeight(targetHeight);
        }
    }

    private double safeHeight(Region region) {
        if (region == null || !region.isManaged()) {
            return 0;
        }
        double height = region.getHeight();
        if (height > 0) {
            return height;
        }
        double prefHeight = region.prefHeight(-1);
        return prefHeight > 0 ? prefHeight : 0;
    }

    private String buildUserBubbleText(String visibleQuestion) {
        if (attachmentContexts.isEmpty()) {
            return visibleQuestion;
        }
        return visibleQuestion + "\n\n" + I18nUtils.getString(
                "ai.attach.summary",
                attachmentContexts.size(),
                summarizeAttachmentNames()
        );
    }

    private String summarizeAttachmentNames() {
        StringBuilder builder = new StringBuilder();
        int displayCount = Math.min(attachmentContexts.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(attachmentContexts.get(i).getFileName());
        }
        if (attachmentContexts.size() > displayCount) {
            builder.append(" ...");
        }
        return builder.toString();
    }

    private AiPromptUtils.AttachmentContext loadAttachmentContext(File file) throws IOException {
        byte[] previewBytes = readPreviewBytes(file, MAX_ATTACHMENT_READ_BYTES + 1);
        boolean truncated = previewBytes.length > MAX_ATTACHMENT_READ_BYTES;
        if (truncated) {
            previewBytes = Arrays.copyOf(previewBytes, MAX_ATTACHMENT_READ_BYTES);
        }

        boolean binary = looksLikeBinary(previewBytes);
        String content;
        if (binary) {
            int length = Math.min(previewBytes.length, MAX_BINARY_PREVIEW_BYTES);
            if (previewBytes.length > length) {
                truncated = true;
            }
            content = toHexPreview(previewBytes, length);
        } else {
            content = new String(previewBytes, StandardCharsets.UTF_8).replace("\u0000", "");
            if (content.length() > MAX_TEXT_ATTACHMENT_CHARS) {
                content = content.substring(0, MAX_TEXT_ATTACHMENT_CHARS);
                truncated = true;
            }
        }

        if (content.trim().isEmpty()) {
            content = "[文件内容为空]";
        }

        return new AiPromptUtils.AttachmentContext(
                file.getName(),
                file.getAbsolutePath(),
                Math.max(file.length(), 0L),
                content,
                binary,
                truncated
        );
    }

    private byte[] readPreviewBytes(File file, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int remaining = Math.max(1, maxBytes);
        try (InputStream inputStream = new FileInputStream(file)) {
            while (remaining > 0) {
                int read = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        }
        return output.toByteArray();
    }

    private boolean looksLikeBinary(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        int sampleSize = Math.min(bytes.length, 1024);
        int controlCount = 0;
        for (int i = 0; i < sampleSize; i++) {
            int value = bytes[i] & 0xFF;
            if (value == 0) {
                return true;
            }
            if (value < 0x09 || (value > 0x0D && value < 0x20)) {
                controlCount++;
            }
        }
        return controlCount > sampleSize / 8;
    }

    private String toHexPreview(byte[] bytes, int length) {
        int safeLength = Math.min(bytes == null ? 0 : bytes.length, Math.max(0, length));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safeLength; i++) {
            if (i > 0) {
                builder.append(i % 16 == 0 ? "\n" : " ");
            }
            String hex = Integer.toHexString(bytes[i] & 0xFF).toUpperCase(Locale.ROOT);
            if (hex.length() < 2) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }
}
