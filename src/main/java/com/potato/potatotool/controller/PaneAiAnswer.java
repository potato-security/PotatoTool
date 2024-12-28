package com.potato.potatotool.controller;

import com.potato.potatotool.utils.aiUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

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

    aiUtil aiObj=new aiUtil();

    public void initialize() {

        question.skinProperty().addListener((ob,ov,nv)->{
            ScrollPane lookup = (ScrollPane) question.lookup(".scroll-pane");
            lookup.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group)((Region)lookup.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
            t.layoutBoundsProperty().addListener((o,n,v)->{
                if(v.getHeight()==0) return;
                double newHeight = v.getHeight() == 23 ? 35 : v.getHeight() + 35 ;  //不故意隐藏scroll 请修改为+44
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

        String req = question.getText().trim();

        if( req.isEmpty() || req.length() < 1 ) return;

        //  创建个人对话cell
        TextArea myTextArea = new TextArea();
        myTextArea.setEditable(false);
        myTextArea.setWrapText(true);
        myTextArea.setPrefHeight(0);
        myTextArea.setPrefWidth(0);
        myTextArea.setMinSize(40,44); // 单字24+padding=44
        myTextArea.setMaxSize(sPane.getWidth() - 170,500);
        myTextArea.getStyleClass().add("myMsg");
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

        Platform.runLater(() -> {
            question.clear();  // 在JavaFX应用线程中执行clear操作
        });

        //  创建AI对话cell
        Pane aiSpacer = new Pane();
        aiSpacer.setMinWidth(20);
        Region aiRegion = new Region();
        aiRegion.getStyleClass().add("aiAvatarImg");
        TextArea aiTextArea = new TextArea();
        aiTextArea.setEditable(false);
        aiTextArea.setWrapText(true);
        aiTextArea.setPrefHeight(0);
        aiTextArea.setPrefWidth(0);
        aiTextArea.setMinSize(40,44);
        aiTextArea.setMaxSize(sPane.getWidth() - 170,500);
        aiTextArea.getStyleClass().add("aiMsg");
        adaptiveSize(aiTextArea);
        HBox aiHBox = new HBox();
        aiHBox.setSpacing(10);
        aiHBox.setAlignment(Pos.TOP_LEFT);
        aiHBox.getChildren().addAll(aiSpacer, aiRegion, aiTextArea);
        aiHBox.setMaxWidth(sPane.getWidth() - 20);

        msgBox.getChildren().add(aiHBox);


        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                aiObj.askAi(req, aiTextArea);
                return null;
            }
        };
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            if (error != null) {
                error.printStackTrace();
            }
        });
        task.setOnSucceeded(event -> {
        });
        new Thread(task).start();
    }


    // TextArea大小自适应
    void adaptiveSize(TextArea textArea){

        Text tmpText = new Text();    //  克隆text用于判断开启自动换行后 宽度改变检测不到的问题
        textArea.skinProperty().addListener((ob,ov,nv)-> {
            double max_Width = sPane.getWidth() - 170;

            ScrollPane lookup_1 = (ScrollPane) textArea.lookup(".scroll-pane");
            lookup_1.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            lookup_1.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            Text t = (Text) ((Group) ((Region) lookup_1.getContent()).getChildrenUnmodifiable().get(1)).getChildren().get(0);
            lookup_1.getContent().setStyle("-fx-padding: 0");

            tmpText.setText(t.getText());
            tmpText.setFont(t.getFont());

            double init_width = tmpText.getLayoutBounds().getWidth() + 22 + 24; // 留一个占位，防止强行换行
            init_width = init_width > max_Width ? max_Width : init_width;
            textArea.setMaxWidth(init_width);  // 因为采用了自动换行，故部分布局方式不可用Pref控制
            textArea.setPrefWidth(init_width);
            textArea.setPrefHeight(tmpText.getLayoutBounds().getHeight() + 22);

            textArea.textProperty().addListener((observable, oldValue, newValue) -> {
                tmpText.setText(t.getText());
                tmpText.setFont(t.getFont());

                double final_Height = t.getLayoutBounds().getHeight() + 22;
                double final_Width = tmpText.getLayoutBounds().getWidth() + 22 + 24;
                final_Width = final_Width > max_Width ? max_Width : final_Width;

                textArea.setPrefHeight(final_Height); // 外部框padding=10 *2  + 2 边界差
                textArea.setMaxWidth(final_Width);
                textArea.setPrefWidth(final_Width);
            });

        });
    }

}
