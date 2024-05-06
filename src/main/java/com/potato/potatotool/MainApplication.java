package com.potato.potatotool;

import com.potato.potatotool.controller.PaneLoad;
import com.potato.potatotool.controller.PanePasswd;
import com.potato.potatotool.utils.SecurityInitializer;
import com.potato.potatotool.utils.Util;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.potato.potatotool.utils.Constants.*;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        hostServices = getHostServices();

        // 初始化配置及BC.jar文件
        initEnvFile();

        // 初始化BC算法支持
        SecurityInitializer.initializeSecurityProvider();

        System.setProperty("prism.lcdtext", "false");// 关闭字体锯齿效果

        //  输入密码界面stage
        Stage passwdStage = new Stage();
        passwdStage.initStyle(StageStyle.TRANSPARENT);
        FXMLLoader passwdLoader = new FXMLLoader(getClass().getResource("/fxml/passwd.fxml"));
        Scene passwdScene = new Scene(passwdLoader.load());
        passwdScene.setCamera(new PerspectiveCamera());
        passwdScene.setFill(null);
        passwdStage.setScene(passwdScene);
        passwdStage.show();

        stage.initStyle(StageStyle.TRANSPARENT);    //  边框透明

        // 提前加载主界面
        AtomicReference<FadeTransition> fadeTransition1 = new AtomicReference<FadeTransition>();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                Platform.runLater(() -> {
                    try {
                        scene.getStylesheets().add(Util.getResourceUrl("/css/common.css"));
                        scene.setCamera(new PerspectiveCamera());   //  添加摄像机
                        // stage.initStyle(StageStyle.TRANSPARENT);    //  边框透明
                        scene.setFill(null);    //  背景透明
                        scene.getRoot().getStyleClass().add("blueStyle");   //  默认蓝队样式
                        stage.setScene(scene);
                        scene.getRoot().setOpacity(0);
                        fadeTransition1.set(new FadeTransition(Duration.seconds(0.3), scene.getRoot()));
                        fadeTransition1.get().setFromValue(0);
                        fadeTransition1.get().setToValue(1);
                        fadeTransition1.get().setCycleCount(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
            System.exit(0);
        });
        new Thread(task).start();

        PanePasswd pwdController = passwdLoader.getController();
        AtomicBoolean isPasswdCorrect = new AtomicBoolean(false);
        pwdController.passwdProperty().addListener((obs_x, oldValue_x, newValue_x) -> {
            // 提前隐藏展示，防止动画卡顿
            stage.show();

            try {
                isPasswdCorrect.set(true);

                //  加载动画界面stage
                Stage loadStage = new Stage();
                loadStage.initStyle(StageStyle.TRANSPARENT);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/load.fxml"));
                Scene loadScene = null;
                loadScene = new Scene(loader.load());
                loadScene.setCamera(new PerspectiveCamera());
                loadScene.setFill(null);
                loadStage.setScene(loadScene);
                passwdStage.close();
                loadStage.show();

                PaneLoad controller = loader.getController();

                FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.3), loadScene.getRoot());
                fadeTransition.setFromValue(1);
                fadeTransition.setToValue(0);
                fadeTransition.setCycleCount(1);

                fadeTransition.setOnFinished(event -> {
                    loadStage.close();
                    controller.stopAnimations();
                    fadeTransition1.get().play();
                });

                controller.loadedProperty().addListener((obs, oldValue, newValue) -> {
                    fadeTransition.play();
                });


            }catch (Exception e){
                e.printStackTrace();
            }
        });




    }

    private void initEnvFile() {
        String TMP_FOLDER = ".PotatoTool";
        String CONFIG_FILE = "config.json";
        String BcJAR_FILE = "bcprov.jar";
        String ip2Region_FILE = "ip2region.xdb";

        Path configFolder = Paths.get(System.getProperty("user.home"), TMP_FOLDER);

        try {
            Files.createDirectories(configFolder);
            Path configFile = configFolder.resolve(CONFIG_FILE);

            if (!Files.exists(configFile)) {
                String tmpDataJsonStr = getResourceString("config");
                Files.write(configFile, tmpDataJsonStr.getBytes(StandardCharsets.UTF_8));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            Path bcJarFile = configFolder.resolve(BcJAR_FILE);
            if (!Files.exists(bcJarFile)) {
                InputStream inputStream = getResourceStream("bcprov");
                FileOutputStream outputStream = new FileOutputStream(bcJarFile.toString());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            Path ip2RegionFile = configFolder.resolve(ip2Region_FILE);
            if (!Files.exists(ip2RegionFile)) {
                InputStream inputStream = getResourceStream("ip2region");
                FileOutputStream outputStream = new FileOutputStream(ip2RegionFile.toString());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static HostServices hostServices;

    public static void setHostServices(HostServices services) {
        hostServices = services;
    }
    public static HostServices letGetHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch();
    }
}