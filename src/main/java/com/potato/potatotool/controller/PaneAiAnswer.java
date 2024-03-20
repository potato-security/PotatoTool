package com.potato.potatotool.controller;

import com.potato.potatotool.utils.DefaultContextMenu;
import com.potato.potatotool.utils.aiUtil;
import com.potato.potatotool.utils.codeAnalyzerUtils;
import com.potato.potatotool.utils.strUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

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
    private HBox aiMsgDiv;

    @FXML
    private HBox myMsgDiv;

    static aiUtil aiObj=new aiUtil();

    public void initialize() {

        question.skinProperty().addListener((ob,ov,nv)->{
            ScrollPane lookup = (ScrollPane) question.lookup(".scroll-pane");
            lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group)((Region)lookup.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
            t.layoutBoundsProperty().addListener((o,n,v)->{
                if(v.getHeight()==0) return;
                double newHeight = v.getHeight() == 23 ? 67 : v.getHeight() + 59 ;  //不故意隐藏scroll 请修改为+44
                if(newHeight >= question.getMaxHeight()) {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    newHeight = question.getMaxHeight();
                }else {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
                // TextArea修改宽高后不会立马重新布局，需要主动触发
                double finalNewHeight = newHeight;
                Platform.runLater(() -> {
                    question.setPrefHeight(finalNewHeight);
                    question.requestLayout();
                    // TextArea变大后，上方ScrollPane变小
                    scrollPane.setPrefHeight(sPane.getHeight() - finalNewHeight - 47);
                });
            });
        });

        //  ScrollPane内容发生高度变化，触发滚动栏置底
        msgBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        question.setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.ENTER
                    && !event.isShiftDown()
                    && !event.isControlDown()
                    && !event.isAltDown()
                    && event.getCode() != KeyCode.META
            ) {
                question.deleteText(question.getLength() - 1, question.getLength());;
                ask();
            }else if(
                    (event.getCode() == KeyCode.ENTER && event.isShiftDown())
                    || (event.getCode() == KeyCode.ENTER && event.isShiftDown())
                    || (event.getCode() == KeyCode.ENTER && event.isControlDown())
                    || (event.getCode() == KeyCode.ENTER && event.isAltDown())
                    || (event.getCode() == KeyCode.ENTER && event.getCode() == KeyCode.META)
            ){
                question.appendText(System.getProperty("line.separator"));
            }
        });
    }

    @FXML
    void ask(){

        String req = question.getText();

        if( req.isEmpty() || req.length() < 1 ) return;

        //  创建个人对话cell
        TextArea myTextArea = new TextArea();
        myTextArea.setEditable(false);
        myTextArea.setWrapText(false);
        myTextArea.setPrefHeight(0);
        myTextArea.setPrefWidth(0);
        myTextArea.setMinSize(40,40);
        myTextArea.setMaxSize(sPane.getWidth() - 170,500);
        myTextArea.getStyleClass().add("myMsg");
        myTextArea.setText(req+" ");
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
        adaptiveSize(myTextArea);

        question.clear();

        //  创建AI对话cell
        Pane aiSpacer = new Pane();
        aiSpacer.setMinWidth(20);
        Region aiRegion = new Region();
        aiRegion.getStyleClass().add("aiAvatarImg");
        TextArea aiTextArea = new TextArea();
        aiTextArea.setEditable(false);
        aiTextArea.setWrapText(false);
        aiTextArea.setPrefHeight(0);
        aiTextArea.setPrefWidth(0);
        aiTextArea.setText(" ");
        aiTextArea.setMinSize(40,40);
        aiTextArea.setMaxSize(sPane.getWidth() - 170,500);
        aiTextArea.getStyleClass().add("aiMsg");
        HBox aiHBox = new HBox();
        aiHBox.setSpacing(10);
        aiHBox.setAlignment(Pos.TOP_LEFT);
        aiHBox.getChildren().addAll(aiSpacer, aiRegion, aiTextArea);
        aiHBox.setMaxWidth(sPane.getWidth() - 20);

        msgBox.getChildren().add(aiHBox);
        adaptiveSize(aiTextArea);


        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                aiObj.askAi(req, aiTextArea);
                return null;
            }
        };
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });
        task.setOnSucceeded(event -> {
        });
        new Thread(task).start();
    }


    void adaptiveSize(TextArea textArea){
        textArea.skinProperty().addListener((ob,ov,nv)->{
            ScrollPane lookup = (ScrollPane) textArea.lookup(".scroll-pane");
            lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            lookup.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text tmpText = new Text();
            Text t = (Text) ((Group)((Region)lookup.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
            t.layoutBoundsProperty().addListener((o,n,v)->{

                if(v.getHeight()==0) return;
                if(v.getWidth()==0) return;

                tmpText.setText(textArea.getText());    //  克隆text用于判断开启自动换行后 宽度改变检测不到的问题
                tmpText.setFont(t.getFont());

                double newHeight = v.getHeight() == 23 ? 67 : v.getHeight() + 54 ;  //不故意隐藏scroll 请修改为+44
                double newWidth = textArea.getPrefWidth() == textArea.getMaxWidth()? v.getWidth() + 54  : v.getWidth() + 44;  // 44 并开启自动换行将疯狂换行  54 无法检测宽度缩减
                if(newHeight >= textArea.getMaxHeight()) {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    newHeight = textArea.getMaxHeight();
                }else {
                    lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                }
                if(newWidth >= textArea.getMaxWidth()) {
                    newWidth = textArea.getMaxWidth();
                }
                // TextArea修改宽高后不会立马重新布局，需要主动触发
                double finalNewHeight = newHeight;
                double finalNewWidth = newWidth;
                Platform.runLater(() -> {
                    textArea.setPrefWidth(finalNewWidth);
                    textArea.setPrefHeight(finalNewHeight);
                    if( finalNewWidth == textArea.getMaxWidth()){
                        textArea.setWrapText(true);
                    }
                    if(finalNewWidth > tmpText.getLayoutBounds().getWidth() + 50){  // 大概差45
                        textArea.setPrefWidth(finalNewWidth-1);
                        textArea.setWrapText(false);
                    }
                    textArea.requestLayout();
                });
            });
        });
    }

}
