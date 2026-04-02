package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiUiState;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
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

import java.util.Arrays;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneAiAnswer {

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
    private Label statusLabel;

    @FXML
    private Hyperlink retryLink;

    private final AiChatService aiChatService;
    private final AiConfigReader aiConfigReader;

    private AiUiState uiState = AiUiState.IDLE;
    private Task<Void> runningTask;
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
            lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group) ((Region) lookup.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
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
                    scrollPane.setPrefHeight(sPane.getHeight() - finalNewHeight - 47);
                });
            });
        });

        msgBox.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));

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

        setState(AiUiState.IDLE, "");

        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    @FXML
    void ask() {
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }

        String req = question.getText().trim();
        if (req.isEmpty()) {
            return;
        }

        lastQuestion = req;
        question.clear();

        TextArea myTextArea = createBubbleTextArea("myMsg");
        myTextArea.setText(req);
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

        startAskTask(req, aiTextArea, thinkingTextArea);
    }

    @FXML
    void retryLastQuestion() {
        if (lastQuestion == null || lastQuestion.trim().isEmpty()) {
            return;
        }
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }
        question.setText(lastQuestion);
        ask();
    }

    private void startAskTask(String req, TextArea aiTextArea, TextArea thinkingTextArea) {
        runningTask = new Task<Void>() {
            @Override
            protected Void call() {
                setState(AiUiState.LOADING, I18nUtils.getString("ai.status.loading"));
                aiChatService.streamChat(req, event -> handleStreamEvent(event, aiTextArea, thinkingTextArea));
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
        Platform.runLater(() -> {
            uiState = newState;
            boolean busy = newState == AiUiState.LOADING
                    || newState == AiUiState.THINKING
                    || newState == AiUiState.STREAMING;

            question.setDisable(busy);
            sendAction.setDisable(busy);

            boolean showStatus = message != null && !message.trim().isEmpty();
            statusLabel.setManaged(showStatus);
            statusLabel.setVisible(showStatus);
            statusLabel.setText(showStatus ? message : "");

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

            boolean showRetry = newState == AiUiState.ERROR;
            retryLink.setManaged(showRetry);
            retryLink.setVisible(showRetry);
        });
    }

    boolean isThinkingEnabled() {
        try {
            return aiConfigReader.read().getThinkingConfig().isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    void adaptiveSize(TextArea textArea) {

        Text tmpText = new Text();
        textArea.skinProperty().addListener((ob, ov, nv) -> {
            double maxWidth = sPane.getWidth() - 170;

            ScrollPane lookup1 = (ScrollPane) textArea.lookup(".scroll-pane");
            lookup1.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            lookup1.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group) ((Region) lookup1.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
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
}
