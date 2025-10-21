package com.potato.potatotool.utils.ui;

import com.leewyatt.rxcontrols.pane.RXCarouselPane;
import com.potato.potatotool.controller.publicPane.PaneAbout;
import com.potato.potatotool.controller.publicPane.PaneExtension;
import com.potato.potatotool.utils.core.Constants;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import static com.potato.potatotool.ToStart.debugMode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面板工厂类 - 解决 JavaFX 节点不能重复添加的问题
 * @author Potato
 * @date 2025/10/09
 */
public class PaneFactory {
    
    // 存储所有创建的 Extension 控制器实例，用于数据同步
    private static final List<PaneExtension> extensionControllers = new ArrayList<>();
    
    // 存储所有创建的 About 控制器实例，用于数据同步
    private static final List<PaneAbout> aboutControllers = new ArrayList<>();
    
    // 标记哪些控制器需要更新（懒同步机制）
    private static final Set<PaneExtension> pendingUpdateControllers = ConcurrentHashMap.newKeySet();
    
    // 标记哪些分类需要更新（智能同步）
    private static final Map<PaneExtension, Set<String>> pendingCategoryUpdates = new ConcurrentHashMap<>();
    
    // 当前活动的控制器（避免同步自己）
    private static PaneExtension currentActiveController = null;
    
    /**
     * 创建扩展面板
     * @param styleSheet 样式表路径（可选）
     * @param widthBinding 宽度绑定倍数
     * @param heightProperty 高度属性
     * @param widthProperty 宽度属性
     * @return RXCarouselPane
     */
    public static RXCarouselPane createExtensionPane(String styleSheet, double widthBinding, ReadOnlyDoubleProperty heightProperty, ReadOnlyDoubleProperty widthProperty) {
        try {
            FXMLLoader loader = new FXMLLoader(PaneFactory.class.getResource("/fxml/publicPane/pane_extension.fxml"));
            Pane pane = loader.load();
            PaneExtension controller = loader.getController();
            
            // 添加到控制器列表
            extensionControllers.add(controller);
            
            // 设置样式
            if (styleSheet != null && !styleSheet.isEmpty()) {
                RXCarouselPane carouselPane = new RXCarouselPane(pane);
                carouselPane.getStylesheets().add(Constants.getResourceUrl(styleSheet));
                
                // 设置尺寸绑定
                pane.prefWidthProperty().bind(widthProperty.multiply(widthBinding));
                pane.prefHeightProperty().bind(heightProperty);
                
                return carouselPane;
            } else {
                RXCarouselPane carouselPane = new RXCarouselPane(pane);
                
                // 设置尺寸绑定
                pane.prefWidthProperty().bind(widthProperty.multiply(widthBinding));
                pane.prefHeightProperty().bind(heightProperty);
                
                return carouselPane;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 创建关于面板
     * @param styleSheet 样式表路径（可选）
     * @param widthBinding 宽度绑定倍数
     * @param heightBinding 高度绑定
     * @param widthProperty 宽度属性
     * @return RXCarouselPane 和 控制器的包装类
     */
    public static AboutPaneResult createAboutPane(String styleSheet, double widthBinding, DoubleBinding heightBinding, ReadOnlyDoubleProperty widthProperty) {
        try {
            FXMLLoader loader = new FXMLLoader(PaneFactory.class.getResource("/fxml/publicPane/pane_about.fxml"));
            Pane pane = loader.load();
            PaneAbout controller = loader.getController();
            
            // 添加到控制器列表
            aboutControllers.add(controller);
            
            // 设置样式
            if (styleSheet != null && !styleSheet.isEmpty()) {
                RXCarouselPane carouselPane = new RXCarouselPane(pane);
                carouselPane.getStylesheets().add(Constants.getResourceUrl(styleSheet));
                
                // 设置尺寸绑定
                pane.prefWidthProperty().bind(widthProperty.multiply(widthBinding));
                pane.prefHeightProperty().bind(heightBinding);
                
                return new AboutPaneResult(carouselPane, controller);
            } else {
                RXCarouselPane carouselPane = new RXCarouselPane(pane);
                
                // 设置尺寸绑定
                pane.prefWidthProperty().bind(widthProperty.multiply(widthBinding));
                pane.prefHeightProperty().bind(heightBinding);
                
                return new AboutPaneResult(carouselPane, controller);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 设置当前活动的控制器
     */
    public static void setActiveController(PaneExtension controller) {
        currentActiveController = controller;
    }
    
    /**
     * 优化1：异步同步所有 Extension 面板
     * 当一个面板的数据发生变化时，异步同步到其他面板
     */
    public static void syncAllExtensionPanes() {
        if (extensionControllers.size() <= 1) return;
        
        // 标记所有非活动控制器需要更新
        for (PaneExtension controller : extensionControllers) {
            if (controller != null && controller != currentActiveController) {
                pendingUpdateControllers.add(controller);
            }
        }
        
        // 异步执行同步，避免阻塞用户操作
        Platform.runLater(() -> {
            for (PaneExtension controller : new ArrayList<>(pendingUpdateControllers)) {
                if (controller != null) {
                    controller.reloadData();
                }
            }
            // 清空待更新列表
            pendingUpdateControllers.clear();
        });
    }
    
    /**
     * 优化2：智能分类同步 - 只同步特定分类
     * @param category 需要同步的分类名称
     */
    public static void syncCategory(String category) {
        if (extensionControllers.size() <= 1) return;
        if (category == null || category.isEmpty()) {
            syncAllExtensionPanes();
            return;
        }
        
        // 标记特定分类需要更新
        for (PaneExtension controller : extensionControllers) {
            if (controller != null && controller != currentActiveController) {
                pendingCategoryUpdates.computeIfAbsent(controller, k -> ConcurrentHashMap.newKeySet()).add(category);
            }
        }
        
        // 异步执行智能同步（稍微延迟，确保配置文件已保存）
        Platform.runLater(() -> {
            // 复制一份待更新的映射，避免并发修改
            Map<PaneExtension, Set<String>> updates = new HashMap<>(pendingCategoryUpdates);
            pendingCategoryUpdates.clear();
            
            for (Map.Entry<PaneExtension, Set<String>> entry : updates.entrySet()) {
                PaneExtension controller = entry.getKey();
                Set<String> categories = entry.getValue();
                
                if (controller != null) {
                    // 只更新需要的分类
                    for (String cat : categories) {
                        if(debugMode) System.out.println("同步分类到其他面板: " + cat + " (控制器: " + controller.hashCode() + ")");
                        controller.updateCategory(cat);
                    }
                }
            }
        });
    }
    
    /**
     * 同步特定分类（明确指定源控制器）
     * @param category 需要同步的分类名称
     * @param sourceController 发起同步的源控制器（不会同步到自己）
     */
    public static void syncCategoryFrom(String category, PaneExtension sourceController) {
        if (extensionControllers.size() <= 1) return;
        if (category == null || category.isEmpty()) return;
        
        if(debugMode) System.out.println("从控制器 " + sourceController.hashCode() + " 同步分类: " + category);
        
        // 标记特定分类需要更新（排除源控制器）
        for (PaneExtension controller : extensionControllers) {
            if (controller != null && controller != sourceController) {
                pendingCategoryUpdates.computeIfAbsent(controller, k -> ConcurrentHashMap.newKeySet()).add(category);
            }
        }
        
        // 异步执行智能同步
        Platform.runLater(() -> {
            Map<PaneExtension, Set<String>> updates = new HashMap<>(pendingCategoryUpdates);
            pendingCategoryUpdates.clear();
            
            for (Map.Entry<PaneExtension, Set<String>> entry : updates.entrySet()) {
                PaneExtension controller = entry.getKey();
                Set<String> categories = entry.getValue();
                
                if (controller != null) {
                    for (String cat : categories) {
                        if(debugMode) System.out.println("  → 同步到控制器 " + controller.hashCode() + ", 分类: " + cat);
                        controller.updateCategory(cat);
                    }
                }
            }
        });
    }
    
    /**
     * 优化3：懒同步 - 在面板变为可见时才真正同步
     * @param controller 需要同步的控制器
     */
    public static void syncOnVisible(PaneExtension controller) {
        if (controller == null) return;
        
        // 设置为当前活动控制器
        setActiveController(controller);
        
        // 如果该控制器在待更新列表中，立即同步
        if (pendingUpdateControllers.contains(controller)) {
            controller.reloadData();
            pendingUpdateControllers.remove(controller);
        }
        
        // 如果有待更新的分类，立即同步
        Set<String> categories = pendingCategoryUpdates.remove(controller);
        if (categories != null && !categories.isEmpty()) {
            for (String category : categories) {
                controller.updateCategory(category);
            }
        }
    }
    
    /**
     * 懒同步当前可见的面板（从MainController调用）
     */
    public static void syncOnVisibleForCurrentPane() {
        // 对所有待更新的控制器执行懒同步
        // 实际上只有当前可见的那个会被真正同步
        if (currentActiveController != null) {
            syncOnVisible(currentActiveController);
        }
    }
    
    /**
     * 同步更新所有 About 面板的滚动状态
     */
    public static void syncAboutScrolling(boolean shouldStart) {
        for (PaneAbout controller : aboutControllers) {
            if (controller != null) {
                if (shouldStart) {
                    controller.startScrolling();
                } else {
                    controller.pauseScrolling();
                }
            }
        }
    }
    
    /**
     * 停止所有 About 面板的滚动
     */
    public static void stopAllAboutScrolling() {
        for (PaneAbout controller : aboutControllers) {
            if (controller != null) {
                controller.pauseScrolling();
            }
        }
    }
    
    /**
     * 关于面板结果包装类
     */
    public static class AboutPaneResult {
        public final RXCarouselPane pane;
        public final PaneAbout controller;
        
        public AboutPaneResult(RXCarouselPane pane, PaneAbout controller) {
            this.pane = pane;
            this.controller = controller;
        }
    }
    
    /**
     * 清理资源
     */
    public static void cleanup() {
        extensionControllers.clear();
        aboutControllers.clear();
    }
}
