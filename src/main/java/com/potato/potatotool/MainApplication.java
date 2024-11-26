package com.potato.potatotool;

import com.potato.potatotool.controller.MainController;
import com.potato.potatotool.controller.PaneLoad;
import com.potato.potatotool.controller.PanePasswd;
import com.potato.potatotool.utils.*;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.stage.Screen;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.*;

public class MainApplication extends Application {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void start(Stage stage) throws IOException {
        hostServices = getHostServices();

//        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
//        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
//        Screen screen = Screen.getPrimary();
//        double dpi = screen.getDpi();
//        double scale = dpi / 151; // 测试机DPI为151

        // 初始化配置及BC.jar文件
        Task<Void> taskInit = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                initEnvFile();
                return null;
            }
        };

        executor.submit(taskInit);

        //  输入密码界面stage
        Stage passwdStage = new Stage();
        passwdStage.setAlwaysOnTop(true);
        passwdStage.initStyle(StageStyle.TRANSPARENT);
        FXMLLoader passwdLoader = new FXMLLoader(getClass().getResource("/fxml/passwd.fxml"));
        Scene passwdScene = new Scene(passwdLoader.load());
        passwdScene.setCamera(new PerspectiveCamera());
        passwdScene.setFill(null);
        passwdStage.setScene(passwdScene);
        passwdStage.show();


        BooleanProperty preload = new SimpleBooleanProperty(false);
        AtomicReference<FadeTransition> fadeTransition1 = new AtomicReference<FadeTransition>();

        PanePasswd pwdController = passwdLoader.getController();
        pwdController.passwdProperty().addListener((obs_x, oldValue_x, newValue_x) -> {
            // 提前隐藏展示，防止动画卡顿
            if(preload.get()) {
                stage.show();
                loadMainStage(passwdStage, fadeTransition1);
            }else {
                preload.addListener((observable_y, oldValue_y, newValue_y) -> {
                    stage.show();
                    loadMainStage(passwdStage, fadeTransition1);
                });
            }
        });


        // 提前加载主界面
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                Platform.runLater(() -> {
                    try {
//                        scene.getRoot().setScaleX(scale);
//                        scene.getRoot().setScaleY(scale);

                        stage.setFullScreenExitHint("");

                        scene.getStylesheets().add(Util.getResourceUrl("/css/common.css"));
                        scene.setCamera(new PerspectiveCamera());   //  添加摄像机
                        stage.initStyle(StageStyle.TRANSPARENT);    //  边框透明
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
            ExecutorServiceManager.getInstance().forceShutdown();
            Throwable error = task.getException();
            error.printStackTrace();
            System.exit(0);
        });
        task.setOnSucceeded(e -> {
            preload.set(true);
        });
        executor.submit(task);
    }

    private void loadMainStage(Stage passwdStage, AtomicReference<FadeTransition> fadeTransition1) {
        try {
            Stage loadStage = new Stage();
            loadStage.initStyle(StageStyle.TRANSPARENT);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/load.fxml"));
            Scene loadScene = new Scene(loader.load());
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
            if(debugMode)e.printStackTrace();
        }
    }


    private void initEnvFile() {
        String TMP_FOLDER = ".PotatoTool";
        Path configFolder = Paths.get(System.getProperty("user.home"), TMP_FOLDER);

        try {
            Files.createDirectories(configFolder);
            Path configFile = configFolder.resolve("config.json");

            if (!Files.exists(configFile)) {
                String tmpDataJsonStr = getResourceString("config");
                Files.write(configFile, tmpDataJsonStr.getBytes(StandardCharsets.UTF_8));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            String propertyName = "bcprov";
            String fileNmae = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            copyResourceToFile(propertyName, configFolder.resolve(fileNmae), (long) (7.9 * 1024 * 1024));
            SecurityInitializer.initializeSecurityProvider();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String propertyName = "ip2region";
            String fileNmae = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            copyResourceToFile(propertyName, configFolder.resolve(fileNmae), (long) (10.5 * 1024 * 1024));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String propertyName = "winKbInfo";
            String fileNmae = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            if(!hasFileWithPrefix(configFolder, propertyName)) {
                copyResourceToFile(propertyName, configFolder.resolve(fileNmae), (long) (70.5 * 1024 * 1024));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Path md5GzipPath = configFolder.resolve("md5_database.db.gzip");
            if(Files.exists(md5GzipPath)){
                String gzipAbsolutePath = md5GzipPath.toString();
                String absolutePath = gzipAbsolutePath.replace(".gzip","");

                GzipUtils.unGzipFile(gzipAbsolutePath, absolutePath, true);
            }
        } catch (Exception e) {
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