package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.ai.AiAttachmentDispatchPlanner;
import com.potato.potatotool.utils.ai.AiAttachmentUtils;
import com.potato.potatotool.utils.ai.AiPromptUtils;
import com.potato.potatotool.utils.ai.config.AiConfigReader;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentDispatchPlan;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.ai.model.AiThinkingConfig;
import com.potato.potatotool.utils.ai.model.AiUiState;
import com.potato.potatotool.utils.ai.provider.AiProviderAdapter;
import com.potato.potatotool.utils.ai.provider.AiProviderRegistry;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneAiAnswer {
    private static final double AUTO_SCROLL_BOTTOM_THRESHOLD = 0.02;
    private static final double QUESTION_MIN_HEIGHT = 44;
    private static final double QUESTION_HEIGHT_UPDATE_THRESHOLD = 2.0;
    private static final double MESSAGE_BUBBLE_MAX_WIDTH = 620;
    private static final double COMPOSER_MENU_VERTICAL_GAP = 8;
    private static final double MESSAGE_BUBBLE_MIN_WIDTH = 72;
    private static final double THINKING_BUBBLE_MIN_WIDTH = 140;
    private static final double BUBBLE_WIDTH_SAFETY = 10;
    private static final double BUBBLE_HEIGHT_SAFETY = 6;

    @FXML
    private StackPane sPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private StackPane composerCard;

    @FXML
    private VBox dropHintBox;

    @FXML
    private Label dropHintTitle;

    @FXML
    private Label dropHintText;

    @FXML
    private TextArea question;

    @FXML
    private VBox msgBox;

    @FXML
    private Button sendAction;

    @FXML
    private Region sendActionIcon;

    @FXML
    private HBox attachmentBar;

    @FXML
    private Button attachButton;

    @FXML
    private HBox composerMetaRow;

    @FXML
    private Label thinkingBadge;

    @FXML
    private Hyperlink clearAttachmentLink;

    @FXML
    private FlowPane attachmentPreviewPane;

    @FXML
    private Label statusLabel;

    @FXML
    private Hyperlink retryLink;

    private final AiChatService aiChatService;
    private final AiConfigReader aiConfigReader;
    private final AiProviderRegistry aiProviderRegistry;
    private final List<AiAttachment> attachmentContexts = new ArrayList<AiAttachment>();
    private final List<AiAttachment> lastRequestAttachments = new ArrayList<AiAttachment>();
    private final StringBuilder pendingAiText = new StringBuilder();
    private final StringBuilder pendingThinkingText = new StringBuilder();

    private AiUiState uiState = AiUiState.IDLE;
    private Task<Void> runningTask;
    private Timeline statusAnimation;
    private String lastQuestion;
    private boolean localeListenerRegistered;
    private boolean followStreamToBottom = true;
    private boolean programmaticScrollUpdate;
    private boolean stopRequested;
    private boolean questionHeightRefreshScheduled;
    private boolean streamFlushScheduled;
    private boolean forceScrollToBottomPending;
    private boolean scrollToBottomScheduled;
    private long chatScrollLayoutVersion;
    private AiThinkingConfig sessionThinkingConfig = new AiThinkingConfig(false, AiThinkingConfig.DEFAULT_BUDGET_TOKENS);
    private ContextMenu composerMenu;
    private Label composerAttachMenuLabel;
    private Label composerThinkingMenuLabel;
    private HBox composerThinkingMenuItemContainer;
    private ScrollPane questionScrollPane;
    private Region questionContentRegion;
    private ScrollBar questionVerticalScrollBar;
    private Text questionTextNode;
    private TextArea bufferedAiLabel;
    private TextArea bufferedThinkingLabel;

    public PaneAiAnswer() {
        this(new AiChatService(), new AiConfigReader(), new AiProviderRegistry());
    }

    PaneAiAnswer(AiChatService aiChatService, AiConfigReader aiConfigReader) {
        this(aiChatService, aiConfigReader, new AiProviderRegistry());
    }

    PaneAiAnswer(AiChatService aiChatService, AiConfigReader aiConfigReader, AiProviderRegistry aiProviderRegistry) {
        this.aiChatService = aiChatService;
        this.aiConfigReader = aiConfigReader;
        this.aiProviderRegistry = aiProviderRegistry;
    }

    public void initialize() {
        registerLocaleRefresh();
        initializeSessionThinking();
        configureQuestionInput();
        configureAttachmentInteractions();
        configureChatScrollBehavior();
        configureComposerMenu();

        refreshAttachmentSummary();
        refreshThinkingToggle();
        refreshPrimaryActionButton();
        refreshDropHintText();
        setState(AiUiState.IDLE, "");

        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            refreshQuestionHeight();
            refreshAttachmentSummary();
            refreshThinkingToggle();
            refreshPrimaryActionButton();
            refreshDropHintText();
        });
    }

    @FXML
    void ask() {
        if (isBusy()) {
            return;
        }
        if (composerMenu != null) {
            composerMenu.hide();
        }

        String rawQuestion = question.getText().trim();
        if (rawQuestion.isEmpty() && attachmentContexts.isEmpty()) {
            return;
        }

        String attachmentSupportError = validateAttachmentApiSupport(attachmentContexts);
        if (attachmentSupportError != null && !attachmentSupportError.trim().isEmpty()) {
            setState(AiUiState.ERROR, attachmentSupportError, false);
            return;
        }

        List<AiAttachment> currentAttachments = new ArrayList<AiAttachment>(attachmentContexts);

        lastQuestion = rawQuestion;
        lastRequestAttachments.clear();
        lastRequestAttachments.addAll(currentAttachments);

        question.clear();
        attachmentContexts.clear();
        refreshQuestionHeight();
        refreshAttachmentSummary();

        String visibleQuestion = rawQuestion.isEmpty()
                ? I18nUtils.getString("ai.attach.default.question")
                : rawQuestion;
        String requestPrompt = AiPromptUtils.buildConversationPrompt(rawQuestion, currentAttachments);
        String historyPrompt = AiPromptUtils.buildConversationHistoryLabel(rawQuestion, currentAttachments);
        followStreamToBottom = true;

        TextArea myLabel = createBubbleTextArea("myMsg");
        myLabel.setText(buildUserBubbleText(visibleQuestion, currentAttachments));

        Region myAvatar = new Region();
        myAvatar.getStyleClass().add("myAvatarImg");

        Pane mySpacer = new Pane();
        HBox.setHgrow(mySpacer, Priority.ALWAYS);

        HBox myHBox = new HBox();
        myHBox.setSpacing(10);
        myHBox.setAlignment(Pos.TOP_RIGHT);
        myHBox.getChildren().addAll(mySpacer, myLabel, myAvatar);
        msgBox.getChildren().add(myHBox);

        TextArea aiLabel = createBubbleTextArea("aiMsg");
        TextArea thinkingLabel = createBubbleTextArea("aiThinkingMsg");
        thinkingLabel.setVisible(false);
        thinkingLabel.setManaged(false);

        VBox aiContent = new VBox();
        aiContent.setSpacing(8);
        aiContent.getChildren().addAll(thinkingLabel, aiLabel);

        Pane aiSpacer = new Pane();
        HBox.setHgrow(aiSpacer, Priority.ALWAYS);

        Region aiAvatar = new Region();
        aiAvatar.getStyleClass().add("aiAvatarImg");

        HBox aiHBox = new HBox();
        aiHBox.setSpacing(10);
        aiHBox.setAlignment(Pos.TOP_LEFT);
        aiHBox.getChildren().addAll(aiAvatar, aiContent, aiSpacer);
        msgBox.getChildren().add(aiHBox);
        forceScrollToBottom();

        startAskTask(requestPrompt, historyPrompt, currentAttachments, aiLabel, thinkingLabel);
    }

    @FXML
    void retryLastQuestion() {
        if (isBusy()) {
            return;
        }
        question.setText(lastQuestion == null ? "" : lastQuestion);
        attachmentContexts.clear();
        attachmentContexts.addAll(lastRequestAttachments);
        refreshQuestionHeight();
        refreshAttachmentSummary();
        ask();
    }

    @FXML
    void chooseAttachments() {
        if (isBusy()) {
            return;
        }
        if (composerMenu != null) {
            composerMenu.hide();
        }

        Window owner = sPane == null || sPane.getScene() == null ? null : sPane.getScene().getWindow();
        if (owner == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("ai.attach"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        I18nUtils.getString("ai.attach.filter.supported"),
                        AiAttachmentUtils.buildSupportedChooserPatterns()
                ),
                new FileChooser.ExtensionFilter(I18nUtils.getString("ai.attach.filter.all"), "*.*")
        );

        addAttachments(chooser.showOpenMultipleDialog(owner));
    }

    @FXML
    void clearAttachments() {
        if (isBusy()) {
            return;
        }
        attachmentContexts.clear();
        refreshAttachmentSummary();
        if (uiState == AiUiState.ERROR) {
            setState(AiUiState.IDLE, "");
        }
    }

    @FXML
    void toggleComposerMenu() {
        if (isBusy()) {
            return;
        }
        if (composerMenu == null || attachButton == null) {
            return;
        }
        if (composerMenu.isShowing()) {
            composerMenu.hide();
            return;
        }
        refreshThinkingToggle();
        composerMenu.show(attachButton, Side.TOP, 0, COMPOSER_MENU_VERTICAL_GAP);
        relocateComposerMenu();
        Platform.runLater(this::relocateComposerMenu);
    }

    @FXML
    void handlePrimaryAction() {
        if (isBusy()) {
            stopGenerating();
            return;
        }
        ask();
    }

    private void configureQuestionInput() {
        question.textProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        question.promptTextProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        question.fontProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        question.paddingProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        question.maxHeightProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        attachmentBar.widthProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        attachButton.widthProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        sendAction.widthProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
        question.focusedProperty().addListener((observable, oldValue, newValue) -> updateComposerFocusState(Boolean.TRUE.equals(newValue)));
        question.skinProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            questionScrollPane = null;
            questionContentRegion = null;
            questionVerticalScrollBar = null;
            questionTextNode = null;
            updateQuestionScrollPolicy(false);
            refreshQuestionHeight();
        }));

        question.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }
            if (event.isShiftDown()
                    && !event.isControlDown()
                    && !event.isMetaDown()
                    && !event.isAltDown()) {
                question.replaceSelection("\n");
                event.consume();
                return;
            }
            if (!event.isShiftDown()
                    && !event.isControlDown()
                    && !event.isMetaDown()
                    && !event.isAltDown()) {
                ask();
                event.consume();
            }
        });

        sPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePasteShortcut);
    }

    private void scheduleQuestionHeightRefresh() {
        if (questionHeightRefreshScheduled) {
            return;
        }
        questionHeightRefreshScheduled = true;
        Platform.runLater(() -> {
            questionHeightRefreshScheduled = false;
            refreshQuestionHeight();
        });
    }

    private void refreshQuestionHeight() {
        if (question == null) {
            return;
        }
        ensureQuestionSkinNodes();

        double wrappingWidth = resolveQuestionWrappingWidth();
        if (wrappingWidth <= 0) {
            return;
        }

        String displayedQuestionText = resolveQuestionDisplayedText();
        double chromeHeight = resolveQuestionVerticalChromeHeight();
        double maxHeight = question.getMaxHeight() > 0 ? question.getMaxHeight() : QUESTION_MIN_HEIGHT;
        double roundedDesiredHeight = Math.ceil(chromeHeight + PaneAiAnswerTextMetricsSupport.computeTextHeight(
                question.getFont(),
                displayedQuestionText,
                wrappingWidth,
                resolveQuestionLineSpacing(),
                resolveQuestionBoundsType()
        ));
        boolean allowVerticalScroll = roundedDesiredHeight > maxHeight;
        if (allowVerticalScroll) {
            double scrollbarBreadth = resolveQuestionVerticalScrollBarBreadth();
            if (scrollbarBreadth > 0) {
                double scrollingWrappingWidth = Math.max(0, wrappingWidth - scrollbarBreadth);
                roundedDesiredHeight = Math.ceil(chromeHeight + PaneAiAnswerTextMetricsSupport.computeTextHeight(
                        question.getFont(),
                        displayedQuestionText,
                        scrollingWrappingWidth,
                        resolveQuestionLineSpacing(),
                        resolveQuestionBoundsType()
                ));
            }
        }
        double targetHeight = PaneAiAnswerTextMetricsSupport.clampHeight(
                roundedDesiredHeight,
                QUESTION_MIN_HEIGHT,
                maxHeight
        );
        if (Math.abs(question.getPrefHeight() - targetHeight) >= QUESTION_HEIGHT_UPDATE_THRESHOLD) {
            question.setPrefHeight(targetHeight);
            question.requestLayout();
        }
        updateQuestionScrollPolicy(allowVerticalScroll);
    }

    private double resolveQuestionVerticalChromeHeight() {
        return snappedVerticalInsets(question)
                + snappedVerticalInsets(questionScrollPane)
                + snappedVerticalInsets(questionContentRegion);
    }

    private double resolveQuestionWrappingWidth() {
        return Math.max(0, resolveQuestionControlWidth()
                - snappedHorizontalInsets(question)
                - snappedHorizontalInsets(questionScrollPane)
                - snappedHorizontalInsets(questionContentRegion));
    }

    private double resolveQuestionControlWidth() {
        if (question != null && question.getWidth() > 0) {
            return question.getWidth();
        }
        if (attachmentBar != null && attachmentBar.getWidth() > 0) {
            double spacingTotal = attachmentBar.getSpacing() * Math.max(0, attachmentBar.getChildren().size() - 1);
            double controlWidth = attachmentBar.getWidth()
                    - snappedHorizontalInsets(attachmentBar)
                    - resolveStableRegionWidth(attachButton)
                    - resolveStableRegionWidth(sendAction)
                    - spacingTotal;
            if (controlWidth > 0) {
                return controlWidth;
            }
        }
        return question == null ? 0 : question.prefWidth(-1);
    }

    private double resolveQuestionVerticalScrollBarBreadth() {
        ensureQuestionSkinNodes();
        if (questionVerticalScrollBar == null) {
            return 0;
        }
        double prefWidth = questionVerticalScrollBar.prefWidth(-1);
        if (prefWidth > 0) {
            return prefWidth;
        }
        double width = questionVerticalScrollBar.getWidth();
        return width > 0 ? width : 0;
    }

    private void ensureQuestionSkinNodes() {
        if (question == null) {
            return;
        }
        Node scrollPaneNode = question.lookup(".scroll-pane");
        if (scrollPaneNode instanceof ScrollPane) {
            ScrollPane resolvedScrollPane = (ScrollPane) scrollPaneNode;
            if (questionScrollPane != resolvedScrollPane) {
                questionScrollPane = resolvedScrollPane;
                questionScrollPane.paddingProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
            }
        }
        Node contentNode = question.lookup(".content");
        if (contentNode instanceof Region) {
            Region resolvedContentRegion = (Region) contentNode;
            if (questionContentRegion != resolvedContentRegion) {
                questionContentRegion = resolvedContentRegion;
                questionContentRegion.paddingProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
            }
        }
        Node verticalScrollBarNode = question.lookup(".scroll-bar:vertical");
        if (verticalScrollBarNode instanceof ScrollBar) {
            questionVerticalScrollBar = (ScrollBar) verticalScrollBarNode;
        }
        Node textNode = question.lookup(".text");
        if (textNode instanceof Text) {
            Text resolvedTextNode = (Text) textNode;
            if (questionTextNode != resolvedTextNode) {
                questionTextNode = resolvedTextNode;
                questionTextNode.boundsTypeProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
                questionTextNode.lineSpacingProperty().addListener((observable, oldValue, newValue) -> scheduleQuestionHeightRefresh());
            }
        }
    }

    private void updateQuestionScrollPolicy(boolean allowVerticalScroll) {
        if (question == null) {
            return;
        }
        ensureQuestionSkinNodes();
        if (questionScrollPane == null) {
            return;
        }
        questionScrollPane.setFitToWidth(true);
        questionScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ScrollPane.ScrollBarPolicy targetPolicy = allowVerticalScroll
                ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                : ScrollPane.ScrollBarPolicy.NEVER;
        if (questionScrollPane.getVbarPolicy() != targetPolicy) {
            questionScrollPane.setVbarPolicy(targetPolicy);
        }
    }

    private String resolveQuestionDisplayedText() {
        if (question == null) {
            return "";
        }
        String text = question.getText();
        if (text != null && !text.isEmpty()) {
            return text;
        }
        String promptText = question.getPromptText();
        return promptText == null ? "" : promptText;
    }

    private void updateComposerFocusState(boolean focused) {
        if (composerCard == null) {
            return;
        }
        toggleStyleClass(composerCard, "ai-composer-card-focused", focused);
    }

    private void configureAttachmentInteractions() {
        sPane.setOnDragEntered(this::handleDragEntered);
        sPane.setOnDragExited(this::handleDragExited);
        sPane.setOnDragOver(this::handleDragOver);
        sPane.setOnDragDropped(this::handleDragDropped);
    }

    private void configureChatScrollBehavior() {
        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (programmaticScrollUpdate || forceScrollToBottomPending || newValue == null) {
                return;
            }
            followStreamToBottom = isNearBottom(newValue.doubleValue());
        });
        msgBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            chatScrollLayoutVersion++;
            requestScrollToBottom();
        });
        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            chatScrollLayoutVersion++;
            requestScrollToBottom();
        });
    }

    private void initializeSessionThinking() {
        AiThinkingConfig currentThinkingConfig = PaneAiAnswerSupport.readThinkingConfig(aiConfigReader);
        boolean supported = PaneAiAnswerSupport.supportsThinking(aiProviderRegistry, aiConfigReader);
        sessionThinkingConfig = new AiThinkingConfig(
                supported && currentThinkingConfig.isEnabled(),
                currentThinkingConfig.getBudgetTokens()
        );
    }

    private void configureComposerMenu() {
        composerMenu = new ContextMenu();
        composerMenu.getStyleClass().add("ai-composer-menu");
        composerMenu.getItems().add(createAttachMenuItem());
        composerMenu.getItems().add(createThinkingMenuItem());
        composerMenu.setOnShowing(event -> updateComposerTriggerState(true));
        composerMenu.setOnShown(event -> relocateComposerMenu());
        composerMenu.setOnHidden(event -> updateComposerTriggerState(false));
        refreshAttachButtonTooltip();
    }

    private void relocateComposerMenu() {
        if (composerMenu == null || !composerMenu.isShowing() || attachButton == null) {
            return;
        }
        Bounds anchorBounds = attachButton.localToScreen(attachButton.getBoundsInLocal());
        if (anchorBounds == null) {
            return;
        }
        double menuHeight = composerMenu.getHeight();
        if (menuHeight <= 0) {
            return;
        }
        composerMenu.setY(anchorBounds.getMinY() - menuHeight - COMPOSER_MENU_VERTICAL_GAP);
    }

    private void updateComposerTriggerState(boolean open) {
        toggleStyleClass(attachButton, "ai-plus-button-open", open);
    }

    private CustomMenuItem createAttachMenuItem() {
        Region icon = new Region();
        icon.getStyleClass().addAll("ai-composer-menu-icon", "ai-composer-menu-icon-attach");

        composerAttachMenuLabel = new Label();
        composerAttachMenuLabel.getStyleClass().add("ai-composer-menu-title");

        HBox content = new HBox();
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("ai-composer-menu-item");
        content.getChildren().addAll(icon, composerAttachMenuLabel);

        CustomMenuItem item = new CustomMenuItem(content, true);
        item.setOnAction(event -> chooseAttachments());
        return item;
    }

    private CustomMenuItem createThinkingMenuItem() {
        Region icon = new Region();
        icon.getStyleClass().addAll("ai-composer-menu-icon", "ai-composer-menu-icon-thinking");

        composerThinkingMenuLabel = new Label();
        composerThinkingMenuLabel.getStyleClass().add("ai-composer-menu-title");

        composerThinkingMenuItemContainer = new HBox();
        composerThinkingMenuItemContainer.setAlignment(Pos.CENTER_LEFT);
        composerThinkingMenuItemContainer.getStyleClass().add("ai-composer-menu-item");
        composerThinkingMenuItemContainer.getChildren().addAll(icon, composerThinkingMenuLabel);

        CustomMenuItem item = new CustomMenuItem(composerThinkingMenuItemContainer, true);
        item.setOnAction(event -> toggleThinkingModeFromMenu());
        return item;
    }

    private void toggleThinkingModeFromMenu() {
        if (isBusy()) {
            refreshThinkingToggle();
            return;
        }
        if (!supportsThinkingMode()) {
            sessionThinkingConfig = new AiThinkingConfig(false, readCurrentThinkingBudgetTokens());
            refreshThinkingToggle();
            refreshAttachmentSummary();
            return;
        }
        boolean enabled = sessionThinkingConfig != null && sessionThinkingConfig.isEnabled();
        sessionThinkingConfig = new AiThinkingConfig(!enabled, readCurrentThinkingBudgetTokens());
        refreshThinkingToggle();
        refreshAttachmentSummary();
    }

    private void stopGenerating() {
        if (!isBusy() || stopRequested) {
            return;
        }
        stopRequested = true;
        setState(AiUiState.STREAMING, I18nUtils.getString("ai.status.stopping"), false);
        aiChatService.cancelActiveStream();
        if (runningTask != null) {
            runningTask.cancel();
        }
    }

    private void startAskTask(String requestPrompt,
                              String historyPrompt,
                              List<AiAttachment> attachments,
                              TextArea aiLabel,
                              TextArea thinkingLabel) {
        final AiThinkingConfig requestThinkingConfig = buildRequestThinkingConfig();
        final boolean requestThinkingEnabled = requestThinkingConfig.isEnabled();
        stopRequested = false;
        resetStreamBuffer(aiLabel, thinkingLabel);
        refreshPrimaryActionButton();

        runningTask = new Task<Void>() {
            @Override
            protected Void call() {
                setState(AiUiState.LOADING, I18nUtils.getString("ai.status.loading"));
                aiChatService.streamChat(
                        AiPromptUtils.generalAssistantSystemPrompt(),
                        requestPrompt,
                        historyPrompt,
                        attachments,
                        requestThinkingConfig,
                        event -> handleStreamEvent(event, aiLabel, thinkingLabel, requestThinkingEnabled)
                );
                return null;
            }
        };

        runningTask.setOnFailed(event -> {
            stopRequested = false;
            Task<Void> task = runningTask;
            runningTask = null;
            flushPendingStreamContent();
            stopStreamFlush();
            Throwable error = task == null ? null : task.getException();
            if (task != null && task.isCancelled()) {
                setState(AiUiState.STOPPED, I18nUtils.getString("ai.status.stopped"), false);
                Platform.runLater(() -> setState(AiUiState.IDLE, ""));
                return;
            }
            String message = error == null ? I18nUtils.getString("ai.status.error") : error.getMessage();
            setState(AiUiState.ERROR, message == null ? I18nUtils.getString("ai.status.error") : message);
        });

        runningTask.setOnSucceeded(event -> {
            stopRequested = false;
            runningTask = null;
            flushPendingStreamContent();
            stopStreamFlush();
            if (uiState != AiUiState.ERROR) {
                setState(AiUiState.COMPLETED, I18nUtils.getString("ai.status.completed"));
                Platform.runLater(() -> setState(AiUiState.IDLE, ""));
            }
        });

        runningTask.setOnCancelled(event -> {
            stopRequested = false;
            runningTask = null;
            flushPendingStreamContent();
            stopStreamFlush();
            if (uiState != AiUiState.ERROR) {
                setState(AiUiState.STOPPED, I18nUtils.getString("ai.status.stopped"), false);
                Platform.runLater(() -> setState(AiUiState.IDLE, ""));
            }
        });

        Thread thread = new Thread(runningTask);
        thread.setDaemon(true);
        thread.start();
    }

    private AiThinkingConfig buildRequestThinkingConfig() {
        boolean enabled = supportsThinkingMode() && sessionThinkingConfig != null && sessionThinkingConfig.isEnabled();
        int budgetTokens = readCurrentThinkingBudgetTokens();
        sessionThinkingConfig = new AiThinkingConfig(enabled, budgetTokens);
        return sessionThinkingConfig;
    }

    private int readCurrentThinkingBudgetTokens() {
        AiThinkingConfig currentThinkingConfig = PaneAiAnswerSupport.readThinkingConfig(aiConfigReader);
        if (currentThinkingConfig != null && currentThinkingConfig.getBudgetTokens() > 0) {
            return currentThinkingConfig.getBudgetTokens();
        }
        return sessionThinkingConfig == null ? AiThinkingConfig.DEFAULT_BUDGET_TOKENS : sessionThinkingConfig.getBudgetTokens();
    }

    private void handleStreamEvent(AiStreamEvent event,
                                   TextArea aiLabel,
                                   TextArea thinkingLabel,
                                   boolean requestThinkingEnabled) {
        if (stopRequested && event.getType() != AiStreamEvent.Type.ERROR) {
            return;
        }
        switch (event.getType()) {
            case THINKING_TOKEN:
                if (!requestThinkingEnabled || stopRequested) {
                    return;
                }
                queueStreamText(thinkingLabel, event.getContent(), true);
                break;
            case TOKEN:
                queueStreamText(aiLabel, event.getContent(), false);
                break;
            case ERROR:
                stopRequested = false;
                aiChatService.cancelActiveStream();
                Platform.runLater(() -> {
                    flushPendingStreamContent();
                    stopStreamFlush();
                    setState(AiUiState.ERROR, event.getContent());
                });
                break;
            case DONE:
                Platform.runLater(() -> {
                    flushPendingStreamContent();
                    stopStreamFlush();
                    if (uiState != AiUiState.ERROR) {
                        setState(AiUiState.COMPLETED, I18nUtils.getString("ai.status.completed"));
                    }
                });
                break;
            default:
                break;
        }
    }

    private TextArea createBubbleTextArea(String styleClass) {
        TextArea textArea = new TextArea();
        final double minBubbleWidth = resolveBubbleMinWidth(styleClass);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setFocusTraversable(true);
        textArea.setMinWidth(Region.USE_PREF_SIZE);
        textArea.setPrefWidth(minBubbleWidth);
        textArea.setMinHeight(Region.USE_PREF_SIZE);
        textArea.setPrefRowCount(1);
        textArea.getStyleClass().addAll("chatBubbleTextArea", styleClass);
        textArea.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(minBubbleWidth, Math.min(
                        MESSAGE_BUBBLE_MAX_WIDTH,
                        sPane == null ? MESSAGE_BUBBLE_MAX_WIDTH : sPane.getWidth() - 280.0
                )),
                sPane.widthProperty()
        ));
        textArea.textProperty().addListener((observable, oldValue, newValue) -> refreshBubbleSize(textArea));
        textArea.widthProperty().addListener((observable, oldValue, newValue) -> refreshBubbleSize(textArea));
        textArea.maxWidthProperty().addListener((observable, oldValue, newValue) -> refreshBubbleSize(textArea));
        textArea.fontProperty().addListener((observable, oldValue, newValue) -> refreshBubbleSize(textArea));
        textArea.paddingProperty().addListener((observable, oldValue, newValue) -> refreshBubbleSize(textArea));
        textArea.skinProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> refreshBubbleSize(textArea)));
        textArea.setContextMenu(createBubbleContextMenu(textArea));
        Platform.runLater(() -> refreshBubbleSize(textArea));
        return textArea;
    }

    private void refreshBubbleSize(TextArea textArea) {
        if (textArea == null) {
            return;
        }
        double maxWidth = textArea.getMaxWidth();
        if (maxWidth <= 0) {
            Platform.runLater(() -> refreshBubbleSize(textArea));
            return;
        }

        TextAreaSkinMetrics metrics = resolveTextAreaSkinMetrics(textArea);
        boolean skinMetricsReady = isTextAreaSkinMetricsReady(metrics);
        if (skinMetricsReady) {
            applyBubbleViewportPolicy(metrics);
        }
        double horizontalPadding = resolveBubbleHorizontalChrome(textArea, metrics);
        double verticalPadding = resolveBubbleVerticalChrome(textArea, metrics);

        double minWidth = resolveBubbleMinWidth(textArea);
        double baseDesiredWidth = PaneAiAnswerTextMetricsSupport.computeTextWidth(textArea.getFont(), textArea.getText())
                + horizontalPadding;
        double widthSafety = baseDesiredWidth > minWidth && baseDesiredWidth < maxWidth
                ? BUBBLE_WIDTH_SAFETY
                : 0;
        double targetWidth = Math.max(minWidth, Math.min(maxWidth, Math.ceil(baseDesiredWidth + widthSafety)));
        if (Math.abs(textArea.getPrefWidth() - targetWidth) >= QUESTION_HEIGHT_UPDATE_THRESHOLD) {
            textArea.setPrefWidth(targetWidth);
            textArea.requestLayout();
        }

        double wrappingWidth = Math.max(0, targetWidth - horizontalPadding);
        double textHeight = PaneAiAnswerTextMetricsSupport.computeTextHeight(
                textArea.getFont(),
                textArea.getText(),
                wrappingWidth,
                skinMetricsReady ? resolveTextAreaLineSpacing(metrics) : 0,
                skinMetricsReady ? resolveTextAreaBoundsType(metrics) : TextBoundsType.LOGICAL
        );
        double minHeight = textArea.getStyleClass().contains("aiThinkingMsg") ? 36 : 44;
        double baseDesiredHeight = Math.ceil(textHeight + verticalPadding);
        double heightSafety = baseDesiredHeight > minHeight
                ? BUBBLE_HEIGHT_SAFETY
                : 0;
        double targetHeight = Math.max(minHeight, Math.ceil(baseDesiredHeight + heightSafety));
        if (Math.abs(textArea.getPrefHeight() - targetHeight) >= QUESTION_HEIGHT_UPDATE_THRESHOLD) {
            textArea.setPrefHeight(targetHeight);
            textArea.requestLayout();
        }
        textArea.setScrollLeft(0);
        textArea.setScrollTop(0);
    }

    private ContextMenu createBubbleContextMenu(TextArea textArea) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem(I18nUtils.getString("contextmenu.copy"));
        copyItem.textProperty().bind(I18nUtils.createBinding("contextmenu.copy"));
        copyItem.setOnAction(event -> copyTextToClipboard(resolveBubbleCopyText(textArea)));
        contextMenu.getItems().add(copyItem);
        return contextMenu;
    }

    private String resolveBubbleCopyText(TextArea textArea) {
        if (textArea == null) {
            return "";
        }
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            return selectedText;
        }
        return textArea.getText();
    }

    private void copyTextToClipboard(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(text);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private void queueStreamText(TextArea label, String text, boolean thinking) {
        if (text == null || text.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (thinking) {
                bufferedThinkingLabel = label;
                pendingThinkingText.append(text);
            } else {
                bufferedAiLabel = label;
                pendingAiText.append(text);
            }
        }
        scheduleStreamFlush();
    }

    private void scheduleStreamFlush() {
        if (Platform.isFxApplicationThread()) {
            scheduleStreamFlushOnFxThread();
            return;
        }
        Platform.runLater(this::scheduleStreamFlushOnFxThread);
    }

    private void scheduleStreamFlushOnFxThread() {
        if (streamFlushScheduled) {
            return;
        }
        streamFlushScheduled = true;
        Platform.runLater(this::flushPendingStreamContent);
    }

    private void flushPendingStreamContent() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::flushPendingStreamContent);
            return;
        }

        String aiChunk;
        String thinkingChunk;
        TextArea aiLabel;
        TextArea thinkingLabel;
        synchronized (this) {
            aiChunk = pendingAiText.toString();
            pendingAiText.setLength(0);
            thinkingChunk = pendingThinkingText.toString();
            pendingThinkingText.setLength(0);
            aiLabel = bufferedAiLabel;
            thinkingLabel = bufferedThinkingLabel;
            streamFlushScheduled = false;
        }

        if (thinkingChunk.isEmpty() && aiChunk.isEmpty()) {
            return;
        }

        if (thinkingLabel != null && !thinkingChunk.isEmpty()) {
            if (!thinkingLabel.isManaged()) {
                thinkingLabel.setManaged(true);
                thinkingLabel.setVisible(true);
            }
            if (uiState == AiUiState.LOADING || uiState == AiUiState.IDLE) {
                setState(AiUiState.THINKING, I18nUtils.getString("ai.status.thinking"));
            }
            thinkingLabel.setText(thinkingLabel.getText() + thinkingChunk);
        }

        if (aiLabel != null && !aiChunk.isEmpty()) {
            if (uiState != AiUiState.STREAMING) {
                setState(AiUiState.STREAMING, I18nUtils.getString("ai.status.streaming"));
            }
            aiLabel.setText(aiLabel.getText() + aiChunk);
        }

        requestScrollToBottom();
        synchronized (this) {
            if ((pendingAiText.length() > 0 || pendingThinkingText.length() > 0) && !streamFlushScheduled) {
                streamFlushScheduled = true;
                Platform.runLater(this::flushPendingStreamContent);
            }
        }
    }

    private void resetStreamBuffer(TextArea aiLabel, TextArea thinkingLabel) {
        stopStreamFlush();
        synchronized (this) {
            pendingAiText.setLength(0);
            pendingThinkingText.setLength(0);
            bufferedAiLabel = aiLabel;
            bufferedThinkingLabel = thinkingLabel;
        }
    }

    private void stopStreamFlush() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::stopStreamFlush);
            return;
        }
        synchronized (this) {
            pendingAiText.setLength(0);
            pendingThinkingText.setLength(0);
            bufferedAiLabel = null;
            bufferedThinkingLabel = null;
            streamFlushScheduled = false;
        }
    }

    private void setState(AiUiState newState, String message) {
        setState(newState, message, newState == AiUiState.ERROR);
    }

    private void setState(AiUiState newState, String message, boolean showRetry) {
        uiState = newState;
        Platform.runLater(() -> {
            boolean busy = newState == AiUiState.LOADING
                    || newState == AiUiState.THINKING
                    || newState == AiUiState.STREAMING;

            question.setDisable(busy);
            attachButton.setDisable(busy);
            sendAction.setDisable(stopRequested);
            clearAttachmentLink.setDisable(busy || attachmentContexts.isEmpty());
            refreshPrimaryActionButton();

            boolean showStatus = message != null && !message.trim().isEmpty();
            statusLabel.setManaged(showStatus);
            statusLabel.setVisible(showStatus);

            statusLabel.getStyleClass().removeAll(Arrays.asList(
                    "ai-status-loading",
                    "ai-status-thinking",
                    "ai-status-streaming",
                    "ai-status-stopped",
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
                case STOPPED:
                    statusLabel.getStyleClass().add("ai-status-stopped");
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
            refreshComposerMetaRow();
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

        String baseText = stripTrailingDots(message);
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
        return message.trim().replaceAll("[.。]+$", "");
    }

    private void refreshAttachmentSummary() {
        boolean hasAttachments = !attachmentContexts.isEmpty();
        refreshAttachmentPreview();
        boolean thinkingEnabled = supportsThinkingMode() && sessionThinkingConfig != null && sessionThinkingConfig.isEnabled();

        thinkingBadge.setManaged(thinkingEnabled);
        thinkingBadge.setVisible(thinkingEnabled);

        clearAttachmentLink.setManaged(hasAttachments);
        clearAttachmentLink.setVisible(hasAttachments);
        clearAttachmentLink.setDisable(isBusy() || !hasAttachments);
        refreshComposerMetaRow();
    }

    private void refreshAttachmentPreview() {
        boolean hasAttachments = !attachmentContexts.isEmpty();
        attachmentPreviewPane.getChildren().clear();
        attachmentPreviewPane.setManaged(hasAttachments);
        attachmentPreviewPane.setVisible(hasAttachments);
        if (!hasAttachments) {
            return;
        }

        for (AiAttachment attachmentContext : attachmentContexts) {
            attachmentPreviewPane.getChildren().add(createAttachmentChip(attachmentContext));
        }
    }

    private HBox createAttachmentChip(AiAttachment attachmentContext) {
        Region fileIcon = new Region();
        fileIcon.getStyleClass().add("ai-attachment-chip-icon");

        Label nameLabel = new Label(attachmentContext.getFileName());
        nameLabel.getStyleClass().add("ai-attachment-chip-name");
        nameLabel.setMaxWidth(220);
        nameLabel.setWrapText(false);

        Label metaLabel = new Label(buildAttachmentMeta(attachmentContext));
        metaLabel.getStyleClass().add("ai-attachment-chip-meta");

        VBox textBox = new VBox();
        textBox.setSpacing(2);
        textBox.getChildren().addAll(nameLabel, metaLabel);

        Region removeIcon = new Region();
        removeIcon.getStyleClass().add("ai-attachment-chip-remove-icon");

        Button removeButton = new Button();
        removeButton.setGraphic(removeIcon);
        removeButton.getStyleClass().add("ai-attachment-chip-remove");
        removeButton.setDisable(isBusy());
        removeButton.setOnAction(event -> removeAttachment(attachmentContext));
        removeButton.setTooltip(new Tooltip(I18nUtils.getString("ai.attach.remove.tooltip")));

        HBox chip = new HBox();
        chip.getStyleClass().add("ai-attachment-chip");
        chip.getChildren().addAll(fileIcon, textBox, removeButton);

        Tooltip.install(chip, new Tooltip(attachmentContext.getAbsolutePath()));
        return chip;
    }

    private void removeAttachment(AiAttachment attachmentContext) {
        if (isBusy()) {
            return;
        }
        attachmentContexts.remove(attachmentContext);
        refreshAttachmentSummary();
        if (uiState == AiUiState.ERROR) {
            setState(AiUiState.IDLE, "");
        }
    }

    private String buildAttachmentMeta(AiAttachment attachmentContext) {
        return formatFileSize(attachmentContext.getFileSize());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = new String[]{"KB", "MB", "GB"};
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value = value / 1024;
            unitIndex++;
        }
        if (unitIndex < 0) {
            return bytes + " B";
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    private void registerLocaleRefresh() {
        if (localeListenerRegistered) {
            return;
        }
        localeListenerRegistered = true;
        I18nUtils.localeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    refreshAttachmentSummary();
                    refreshThinkingToggle();
                    refreshPrimaryActionButton();
                    refreshDropHintText();
                });
            }
        });
    }

    private void refreshThinkingToggle() {
        boolean supported = supportsThinkingMode();
        boolean enabled = supported && sessionThinkingConfig != null && sessionThinkingConfig.isEnabled();
        String thinkingTooltip = buildThinkingTooltip(enabled);
        if (composerThinkingMenuLabel != null) {
            composerThinkingMenuLabel.setText(I18nUtils.getString(
                    !supported ? "ai.compose.menu.thinking.unsupported"
                            : enabled ? "ai.compose.menu.thinking.disable" : "ai.compose.menu.thinking.enable"
            ));
            setTooltipText(composerThinkingMenuLabel, thinkingTooltip);
        }
        if (composerThinkingMenuItemContainer != null) {
            toggleStyleClass(composerThinkingMenuItemContainer, "ai-composer-menu-item-active", enabled);
            composerThinkingMenuItemContainer.setDisable(!supported);
            setTooltipText(composerThinkingMenuItemContainer, thinkingTooltip);
        }
        refreshAttachButtonTooltip();
    }

    private void refreshPrimaryActionButton() {
        if (sendAction == null || sendActionIcon == null) {
            return;
        }
        boolean busy = isBusy();
        sendActionIcon.getStyleClass().removeAll("send", "ai-icon-stop");
        sendActionIcon.getStyleClass().add(busy ? "ai-icon-stop" : "send");

        sendAction.getStyleClass().remove("ai-send-button-stop");
        if (busy) {
            sendAction.getStyleClass().add("ai-send-button-stop");
        }

        setTooltipText(sendAction, I18nUtils.getString(busy ? "ai.stop.tooltip" : "ai.send"));
    }

    private void refreshAttachButtonTooltip() {
        if (attachButton == null) {
            return;
        }
        setTooltipText(attachButton, I18nUtils.getString("ai.compose.menu.tooltip"));
        if (composerAttachMenuLabel != null) {
            composerAttachMenuLabel.setText(I18nUtils.getString("ai.attach"));
        }
        if (thinkingBadge != null) {
            setLabeledText(thinkingBadge, I18nUtils.getString("ai.thinking.badge"));
            setTooltipText(thinkingBadge, buildThinkingTooltip(supportsThinkingMode() && sessionThinkingConfig != null && sessionThinkingConfig.isEnabled()));
        }
    }

    private String buildThinkingTooltip(boolean enabled) {
        if (!supportsThinkingMode()) {
            return I18nUtils.getString("ai.thinking.tooltip.unsupported");
        }
        int budgetTokens = readCurrentThinkingBudgetTokens();
        if (!supportsThinkingBudget()) {
            return I18nUtils.getString(
                    enabled ? "ai.thinking.tooltip.on.no_budget" : "ai.thinking.tooltip.off.no_budget"
            );
        }
        return I18nUtils.getString(
                enabled ? "ai.thinking.tooltip.on" : "ai.thinking.tooltip.off",
                budgetTokens
        );
    }

    private boolean supportsThinkingMode() {
        return PaneAiAnswerSupport.supportsThinking(aiProviderRegistry, aiConfigReader);
    }

    private boolean supportsThinkingBudget() {
        return PaneAiAnswerSupport.supportsThinkingBudget(aiProviderRegistry, aiConfigReader);
    }

    private void setTooltipText(Control control, String text) {
        if (control == null) {
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            control.setTooltip(null);
            return;
        }
        if (control.getTooltip() == null) {
            control.setTooltip(new Tooltip(text));
        } else {
            control.getTooltip().setText(text);
        }
    }

    private void setTooltipText(HBox box, String text) {
        if (box == null) {
            return;
        }
        Tooltip tooltip = (Tooltip) box.getProperties().get("tooltip");
        if (text == null || text.trim().isEmpty()) {
            if (tooltip != null) {
                Tooltip.uninstall(box, tooltip);
                box.getProperties().remove("tooltip");
            }
            return;
        }
        if (tooltip == null) {
            tooltip = new Tooltip(text);
            box.getProperties().put("tooltip", tooltip);
            Tooltip.install(box, tooltip);
        } else {
            tooltip.setText(text);
        }
    }

    private void refreshComposerMetaRow() {
        if (composerMetaRow == null) {
            return;
        }
        boolean visible = isNodeVisible(thinkingBadge)
                || isNodeVisible(clearAttachmentLink)
                || isNodeVisible(statusLabel)
                || isNodeVisible(retryLink);
        composerMetaRow.setManaged(visible);
        composerMetaRow.setVisible(visible);
    }

    private boolean isNodeVisible(Region region) {
        return region != null && region.isManaged() && region.isVisible();
    }

    private boolean isNodeVisible(Label label) {
        return label != null && label.isManaged() && label.isVisible();
    }

    private boolean isNodeVisible(Hyperlink hyperlink) {
        return hyperlink != null && hyperlink.isManaged() && hyperlink.isVisible();
    }

    private void toggleStyleClass(Region region, String styleClass, boolean enabled) {
        if (region == null || styleClass == null || styleClass.isEmpty()) {
            return;
        }
        if (enabled) {
            if (!region.getStyleClass().contains(styleClass)) {
                region.getStyleClass().add(styleClass);
            }
        } else {
            region.getStyleClass().remove(styleClass);
        }
    }

    private void toggleStyleClass(HBox box, String styleClass, boolean enabled) {
        if (box == null || styleClass == null || styleClass.isEmpty()) {
            return;
        }
        if (enabled) {
            if (!box.getStyleClass().contains(styleClass)) {
                box.getStyleClass().add(styleClass);
            }
        } else {
            box.getStyleClass().remove(styleClass);
        }
    }

    private void refreshDropHintText() {
        setLabeledText(dropHintTitle, I18nUtils.getString("ai.attach.drop.title"));
        setLabeledText(dropHintText, I18nUtils.getString("ai.attach.drop.desc", AiAttachmentUtils.MAX_ATTACHMENT_COUNT));
    }

    private boolean isNearBottom(double vvalue) {
        return vvalue >= 1.0 - AUTO_SCROLL_BOTTOM_THRESHOLD;
    }

    private void requestScrollToBottom() {
        if (!followStreamToBottom && !forceScrollToBottomPending) {
            return;
        }
        if (scrollToBottomScheduled) {
            return;
        }
        scrollToBottomScheduled = true;
        programmaticScrollUpdate = true;
        Platform.runLater(this::scrollToBottom);
    }

    private void forceScrollToBottom() {
        followStreamToBottom = true;
        forceScrollToBottomPending = true;
        requestScrollToBottom();
    }

    private void scrollToBottom() {
        scrollToBottomScheduled = false;
        if (scrollPane == null || (!followStreamToBottom && !forceScrollToBottomPending)) {
            finishScrollToBottomSequence(-1L);
            return;
        }
        long layoutVersion = chatScrollLayoutVersion;
        scrollPane.setVvalue(1.0);
        Platform.runLater(() -> finishScrollToBottomSequence(layoutVersion));
    }

    private void finishScrollToBottomSequence(long layoutVersion) {
        if (forceScrollToBottomPending && layoutVersion >= 0 && chatScrollLayoutVersion != layoutVersion) {
            requestScrollToBottom();
            return;
        }
        if (forceScrollToBottomPending) {
            forceScrollToBottomPending = false;
        }
        if (!scrollToBottomScheduled) {
            programmaticScrollUpdate = false;
        }
    }

    private void setLabeledText(Labeled labeled, String text) {
        if (labeled == null) {
            return;
        }
        if (labeled.textProperty().isBound()) {
            labeled.textProperty().unbind();
        }
        labeled.setText(text == null ? "" : text);
    }

    private double snappedHorizontalInsets(Region region) {
        if (region == null) {
            return 0;
        }
        return region.snappedLeftInset() + region.snappedRightInset();
    }

    private double resolveStableRegionWidth(Region region) {
        if (region == null || !region.isManaged()) {
            return 0;
        }
        double width = region.getWidth();
        if (width > 0) {
            return width;
        }
        double prefWidth = region.prefWidth(-1);
        return prefWidth > 0 ? prefWidth : 0;
    }

    private double snappedVerticalInsets(Region region) {
        if (region == null) {
            return 0;
        }
        return region.snappedTopInset() + region.snappedBottomInset();
    }

    private double resolveQuestionLineSpacing() {
        ensureQuestionSkinNodes();
        return questionTextNode == null ? 0 : questionTextNode.getLineSpacing();
    }

    private TextBoundsType resolveQuestionBoundsType() {
        ensureQuestionSkinNodes();
        return questionTextNode == null ? TextBoundsType.LOGICAL : questionTextNode.getBoundsType();
    }

    private TextAreaSkinMetrics resolveTextAreaSkinMetrics(TextArea textArea) {
        if (textArea == null) {
            return TextAreaSkinMetrics.EMPTY;
        }

        ScrollPane internalScrollPane = null;
        Region contentRegion = null;
        Text textNode = null;

        Node scrollPaneNode = textArea.lookup(".scroll-pane");
        if (scrollPaneNode instanceof ScrollPane) {
            internalScrollPane = (ScrollPane) scrollPaneNode;
        }

        Node contentNode = textArea.lookup(".content");
        if (contentNode instanceof Region) {
            contentRegion = (Region) contentNode;
        }

        Node paragraphTextNode = textArea.lookup(".text");
        if (paragraphTextNode instanceof Text) {
            textNode = (Text) paragraphTextNode;
        }

        return new TextAreaSkinMetrics(internalScrollPane, contentRegion, textNode);
    }

    private double resolveTextAreaHorizontalChrome(TextArea textArea, TextAreaSkinMetrics metrics) {
        return snappedHorizontalInsets(textArea)
                + snappedHorizontalInsets(metrics.getScrollPane())
                + snappedHorizontalInsets(metrics.getContentRegion());
    }

    private double resolveTextAreaVerticalChrome(TextArea textArea, TextAreaSkinMetrics metrics) {
        return snappedVerticalInsets(textArea)
                + snappedVerticalInsets(metrics.getScrollPane())
                + snappedVerticalInsets(metrics.getContentRegion());
    }

    private double resolveBubbleHorizontalChrome(TextArea textArea, TextAreaSkinMetrics metrics) {
        double chrome = resolveTextAreaHorizontalChrome(textArea, metrics);
        return chrome > 0 ? chrome : resolveFallbackBubbleHorizontalChrome(textArea);
    }

    private double resolveBubbleVerticalChrome(TextArea textArea, TextAreaSkinMetrics metrics) {
        double chrome = resolveTextAreaVerticalChrome(textArea, metrics);
        return chrome > 0 ? chrome : resolveFallbackBubbleVerticalChrome(textArea);
    }

    private double resolveFallbackBubbleHorizontalChrome(TextArea textArea) {
        return textArea != null && textArea.getStyleClass().contains("aiThinkingMsg") ? 20 : 28;
    }

    private double resolveFallbackBubbleVerticalChrome(TextArea textArea) {
        return textArea != null && textArea.getStyleClass().contains("aiThinkingMsg") ? 16 : 24;
    }

    private double resolveTextAreaLineSpacing(TextAreaSkinMetrics metrics) {
        return metrics.getTextNode() == null ? 0 : metrics.getTextNode().getLineSpacing();
    }

    private TextBoundsType resolveTextAreaBoundsType(TextAreaSkinMetrics metrics) {
        return metrics.getTextNode() == null ? TextBoundsType.LOGICAL : metrics.getTextNode().getBoundsType();
    }

    private boolean isTextAreaSkinMetricsReady(TextAreaSkinMetrics metrics) {
        return metrics != null
                && metrics.getScrollPane() != null
                && metrics.getContentRegion() != null
                && metrics.getTextNode() != null;
    }

    private void applyBubbleViewportPolicy(TextAreaSkinMetrics metrics) {
        if (metrics == null || metrics.getScrollPane() == null) {
            return;
        }
        ScrollPane internalScrollPane = metrics.getScrollPane();
        internalScrollPane.setFitToWidth(true);
        if (internalScrollPane.getHbarPolicy() != ScrollPane.ScrollBarPolicy.NEVER) {
            internalScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        if (internalScrollPane.getVbarPolicy() != ScrollPane.ScrollBarPolicy.NEVER) {
            internalScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
    }

    private double resolveBubbleMinWidth(TextArea textArea) {
        return textArea != null && textArea.getStyleClass().contains("aiThinkingMsg")
                ? THINKING_BUBBLE_MIN_WIDTH
                : MESSAGE_BUBBLE_MIN_WIDTH;
    }

    private double resolveBubbleMinWidth(String styleClass) {
        return "aiThinkingMsg".equals(styleClass)
                ? THINKING_BUBBLE_MIN_WIDTH
                : MESSAGE_BUBBLE_MIN_WIDTH;
    }

    private static final class TextAreaSkinMetrics {
        private static final TextAreaSkinMetrics EMPTY = new TextAreaSkinMetrics(null, null, null);

        private final ScrollPane scrollPane;
        private final Region contentRegion;
        private final Text textNode;

        private TextAreaSkinMetrics(ScrollPane scrollPane,
                                    Region contentRegion,
                                    Text textNode) {
            this.scrollPane = scrollPane;
            this.contentRegion = contentRegion;
            this.textNode = textNode;
        }

        private ScrollPane getScrollPane() {
            return scrollPane;
        }

        private Region getContentRegion() {
            return contentRegion;
        }

        private Text getTextNode() {
            return textNode;
        }
    }

    private String buildUserBubbleText(String visibleQuestion,
                                       List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return visibleQuestion;
        }
        return visibleQuestion + "\n\n" + I18nUtils.getString(
                "ai.attach.summary",
                attachments.size(),
                summarizeAttachmentNames(attachments)
        );
    }

    private String summarizeAttachmentNames(List<AiAttachment> attachments) {
        StringBuilder builder = new StringBuilder();
        int displayCount = Math.min(attachments.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(attachments.get(i).getFileName());
        }
        if (attachments.size() > displayCount) {
            builder.append(" ...");
        }
        return builder.toString();
    }

    private void addAttachments(List<File> files) {
        if (isBusy() || files == null || files.isEmpty()) {
            return;
        }

        String attachmentSupportError = validateAttachmentApiSupport();
        if (attachmentSupportError != null && !attachmentSupportError.trim().isEmpty()) {
            setState(AiUiState.ERROR, attachmentSupportError, false);
            return;
        }

        AiRuntimeConfig runtimeConfig = PaneAiAnswerSupport.readRuntimeConfig(aiConfigReader);
        AiAttachmentSupportResult currentSupport = resolveAttachmentSupport(runtimeConfig, attachmentContexts);
        int maxAttachmentCount = currentSupport.getMaxAttachmentCount() <= 0
                ? AiAttachmentUtils.MAX_ATTACHMENT_COUNT
                : currentSupport.getMaxAttachmentCount();
        int remaining = maxAttachmentCount - attachmentContexts.size();
        if (remaining <= 0) {
            setState(AiUiState.ERROR, I18nUtils.getString("ai.attach.limit", maxAttachmentCount), false);
            return;
        }

        String errorMessage = null;
        int processed = 0;
        int duplicateCount = 0;
        int overflowCount = 0;
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (!file.exists() || !file.isFile()) {
                errorMessage = I18nUtils.getString("ai.attach.invalid", file == null ? "" : file.getName());
                break;
            }
            if (isAttachmentDuplicate(file)) {
                duplicateCount++;
                continue;
            }
            if (processed >= remaining) {
                overflowCount++;
                continue;
            }
            try {
                AiAttachment attachment = AiAttachmentUtils.createAttachment(file);
                String validationMessage = validateAttachment(attachment, runtimeConfig);
                if (validationMessage != null && !validationMessage.trim().isEmpty()) {
                    errorMessage = validationMessage;
                    break;
                }
                attachmentContexts.add(attachment);
                processed++;
            } catch (Exception e) {
                errorMessage = I18nUtils.getString(
                        "ai.attach.read.failed",
                        e.getMessage() == null ? file.getName() : e.getMessage()
                );
                break;
            }
        }

        refreshAttachmentSummary();

        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            setState(AiUiState.ERROR, errorMessage, false);
            return;
        }
        if (overflowCount > 0) {
            setState(AiUiState.ERROR, I18nUtils.getString("ai.attach.limit", maxAttachmentCount), false);
            return;
        }
        if (duplicateCount > 0) {
            String duplicateMessage = processed > 0
                    ? I18nUtils.getString("ai.attach.duplicate.skipped", duplicateCount)
                    : I18nUtils.getString("ai.attach.duplicate");
            setState(AiUiState.IDLE, duplicateMessage, false);
            return;
        }
        setState(AiUiState.IDLE, "");
    }

    private boolean isAttachmentDuplicate(File file) {
        if (file == null) {
            return false;
        }
        String absolutePath = file.getAbsolutePath();
        for (AiAttachment attachmentContext : attachmentContexts) {
            if (absolutePath.equals(attachmentContext.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    private String validateAttachment(AiAttachment attachment, AiRuntimeConfig runtimeConfig) {
        if (attachment == null) {
            return I18nUtils.getString("ai.attach.invalid", "");
        }
        List<AiAttachment> candidateAttachments = new ArrayList<AiAttachment>(attachmentContexts);
        candidateAttachments.add(attachment);
        AiAttachmentDispatchPlan dispatchPlan = resolveAttachmentDispatchPlan(runtimeConfig, candidateAttachments);
        return dispatchPlan.isSupported() ? null : dispatchPlan.getMessage();
    }

    private long calculateTotalAttachmentSize() {
        long totalSize = 0L;
        for (AiAttachment attachment : attachmentContexts) {
            if (attachment != null) {
                totalSize += Math.max(0L, attachment.getFileSize());
            }
        }
        return totalSize;
    }

    private String validateAttachmentApiSupport(List<AiAttachment> attachments) {
        List<AiAttachment> targetAttachments = attachments == null ? attachmentContexts : attachments;
        if (targetAttachments == null || targetAttachments.isEmpty()) {
            return null;
        }
        AiRuntimeConfig runtimeConfig = PaneAiAnswerSupport.readRuntimeConfig(aiConfigReader);
        if (runtimeConfig == null) {
            return null;
        }

        AiAttachmentDispatchPlan dispatchPlan = resolveAttachmentDispatchPlan(runtimeConfig, targetAttachments);
        return dispatchPlan.isSupported() ? null : dispatchPlan.getMessage();
    }

    private String validateAttachmentApiSupport() {

        AiRuntimeConfig runtimeConfig = PaneAiAnswerSupport.readRuntimeConfig(aiConfigReader);
        if (runtimeConfig == null) {
            return null;
        }

        AiAttachmentDispatchPlan dispatchPlan = resolveAttachmentDispatchPlan(runtimeConfig, attachmentContexts);
        if (dispatchPlan.isSupported()) {
            return null;
        }
        return dispatchPlan.getMessage();
    }

    private AiAttachmentSupportResult resolveAttachmentSupport(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
        AiProviderType providerType = runtimeConfig == null ? PaneAiAnswerSupport.readProviderType(aiConfigReader) : runtimeConfig.getProviderType();
        AiProviderAdapter providerAdapter = aiProviderRegistry.get(providerType);
        return providerAdapter.resolveAttachmentSupport(runtimeConfig, attachments);
    }

    private AiAttachmentDispatchPlan resolveAttachmentDispatchPlan(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
        AiProviderType providerType = runtimeConfig == null ? PaneAiAnswerSupport.readProviderType(aiConfigReader) : runtimeConfig.getProviderType();
        AiProviderAdapter providerAdapter = aiProviderRegistry.get(providerType);
        return AiAttachmentDispatchPlanner.plan(runtimeConfig, providerAdapter, attachments);
    }

    private void handlePasteShortcut(KeyEvent event) {
        if (event.getCode() != KeyCode.V || !event.isShortcutDown()) {
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard == null || !clipboard.hasFiles()) {
            return;
        }
        List<File> files = clipboard.getFiles();
        if (files == null || files.isEmpty()) {
            return;
        }
        addAttachments(files);
        event.consume();
    }

    private void handleDragEntered(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard != null && dragboard.hasFiles() && !isBusy()) {
            setDragActive(true);
        }
    }

    private void handleDragExited(DragEvent event) {
        setDragActive(false);
    }

    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard != null && dragboard.hasFiles() && !isBusy()) {
            event.acceptTransferModes(TransferMode.COPY);
            setDragActive(true);
        } else {
            setDragActive(false);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        boolean success = false;
        Dragboard dragboard = event.getDragboard();
        if (dragboard != null && dragboard.hasFiles() && !isBusy()) {
            addAttachments(dragboard.getFiles());
            success = true;
        }
        setDragActive(false);
        event.setDropCompleted(success);
        event.consume();
    }

    private void setDragActive(boolean active) {
        if (composerCard == null || dropHintBox == null) {
            return;
        }
        toggleStyleClass(composerCard, "ai-composer-card-drag", active);
        dropHintBox.setManaged(active);
        dropHintBox.setVisible(active);
    }

    private boolean isBusy() {
        return uiState == AiUiState.LOADING
                || uiState == AiUiState.THINKING
                || uiState == AiUiState.STREAMING;
    }
}
