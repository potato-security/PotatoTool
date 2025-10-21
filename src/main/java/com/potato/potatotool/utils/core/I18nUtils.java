package com.potato.potatotool.utils.core;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.potato.potatotool.utils.ui.DefaultContextMenu;

/**
 * 国际化工具类 - 简化UI组件的国际化绑定
 * 通过递归遍历Scene Graph自动绑定所有标记了userData的组件
 * 
 * 使用方法：
 * 1. 在FXML中为需要国际化的组件设置userData，格式为 "i18n:资源键"
 *    例如：<Label text="保存" userData="i18n:app.save"/>
 * 
 * 2. 在Controller的initialize方法中调用：
 *    I18nUtils.bindComponents(rootNode);
 * 
 * @author Potato
 * @date 2025/10/10
 */
public class I18nUtils {
    
    private static final I18nManager i18n = I18nManager.getInstance();
    private static final String I18N_PREFIX = "i18n:";
    private static final String I18N_PROMPT_PREFIX = "i18n-prompt:";
    private static final String I18N_COMBOBOX_PREFIX = "i18n-combobox:";
    private static final String I18N_COMBOBOX_VALUE_PREFIX = "i18n-combobox-value:";
    private static final String I18N_CONTEXTMENU_PREFIX = "i18n-contextmenu:";
    
    // 存储已绑定的组件，避免重复绑定
    private static final Map<Node, Boolean> boundComponents = new HashMap<>();
    
    /**
     * 自动绑定节点及其所有子节点的国际化文本
     * @param node 根节点
     */
    public static void bindComponents(Node node) {
        if (node == null) {
            return;
        }
        
        // 避免重复绑定
        if (boundComponents.containsKey(node)) {
            return;
        }
        
        // 处理当前节点
        bindSingleComponent(node);
        boundComponents.put(node, true);
        
        // 特殊处理：Accordion的TitledPane
        if (node instanceof Accordion) {
            Accordion accordion = (Accordion) node;
            for (TitledPane pane : accordion.getPanes()) {
                bindSingleComponent(pane);
                boundComponents.put(pane, true);
                // 处理 TitledPane 的 graphic（通常是Label）
                if (pane.getGraphic() != null) {
                    bindComponents(pane.getGraphic());
                }
                // 处理 TitledPane 的 content
                if (pane.getContent() != null) {
                    bindComponents(pane.getContent());
                }
            }
        }
        
        // 特殊处理：ScrollPane 内的内容（确保扫描到延迟加载的内容）
        if (node instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) node;
            if (scrollPane.getContent() != null) {
                bindComponents(scrollPane.getContent());
            }
        }
        
        // 特殊处理：TabPane的Tab
        if (node instanceof TabPane) {
            TabPane tabPane = (TabPane) node;
            for (Tab tab : tabPane.getTabs()) {
                bindTab(tab);
                if (tab.getContent() != null) {
                    bindComponents(tab.getContent());
                }
            }
        }
        
        // 特殊处理：MenuBar和Menu
        if (node instanceof MenuBar) {
            MenuBar menuBar = (MenuBar) node;
            for (Menu menu : menuBar.getMenus()) {
                bindMenu(menu);
            }
        }
        
        // 特殊处理：ListView 的 items
        if (node instanceof ListView) {
            ListView<?> listView = (ListView<?>) node;
            if (listView.getItems() != null) {
                for (Object item : listView.getItems()) {
                    if (item instanceof Node) {
                        bindComponents((Node) item);
                    }
                }
            }
        }
        
        // 递归处理子节点
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                bindComponents(child);
            }
        }
    }
    
    /**
     * 绑定单个组件
     */
    private static void bindSingleComponent(Node node) {
        // 自动为 TextArea/TextField/CodeArea 添加国际化右键菜单（优先处理，不依赖 userData）
        autoAddContextMenu(node);
        
        Object userData = node.getUserData();
        if (userData == null) {
            return;
        }
        
        String userDataStr = userData.toString();
        
        // 处理文本绑定 (i18n:key)
        if (userDataStr.startsWith(I18N_PREFIX)) {
            String key = userDataStr.substring(I18N_PREFIX.length());
            bindText(node, key);
        }
        
        // 处理PromptText绑定 (i18n-prompt:key)
        if (userDataStr.contains(I18N_PROMPT_PREFIX)) {
            String[] parts = userDataStr.split(";");
            for (String part : parts) {
                if (part.trim().startsWith(I18N_PROMPT_PREFIX)) {
                    String key = part.trim().substring(I18N_PROMPT_PREFIX.length());
                    bindPromptText(node, key);
                }
            }
        }
        
        // 处理ComboBox选项绑定 (i18n-combobox:key)
        if (userDataStr.contains(I18N_COMBOBOX_PREFIX)) {
            String[] parts = userDataStr.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                // 排除 i18n-combobox-value: 前缀
                if (trimmed.startsWith(I18N_COMBOBOX_PREFIX) && !trimmed.startsWith(I18N_COMBOBOX_VALUE_PREFIX)) {
                    String key = trimmed.substring(I18N_COMBOBOX_PREFIX.length());
                    bindComboBoxItems(node, key);
                }
            }
        }
        
        // 处理ComboBox默认值绑定 (i18n-combobox-value:key)
        if (userDataStr.contains(I18N_COMBOBOX_VALUE_PREFIX)) {
            String[] parts = userDataStr.split(";");
            for (String part : parts) {
                if (part.trim().startsWith(I18N_COMBOBOX_VALUE_PREFIX)) {
                    String key = part.trim().substring(I18N_COMBOBOX_VALUE_PREFIX.length());
                    bindComboBoxValue(node, key);
                }
            }
        }
        
        // 处理ContextMenu绑定 (i18n-contextmenu:key)
        if (userDataStr.contains(I18N_CONTEXTMENU_PREFIX)) {
            String[] parts = userDataStr.split(";");
            for (String part : parts) {
                if (part.trim().startsWith(I18N_CONTEXTMENU_PREFIX)) {
                    String key = part.trim().substring(I18N_CONTEXTMENU_PREFIX.length());
                    bindContextMenu(node, key);
                }
            }
        }
        
        // 处理组件的Tooltip
        if (node instanceof Control) {
            Control control = (Control) node;
            Tooltip tooltip = control.getTooltip();
            if (tooltip != null) {
                bindTooltipComponent(tooltip);
            }
        }
    }
    
    /**
     * 绑定Tooltip组件本身
     */
    private static void bindTooltipComponent(Tooltip tooltip) {
        Object userData = tooltip.getUserData();
        if (userData != null) {
            String userDataStr = userData.toString();
            if (userDataStr.startsWith(I18N_PREFIX)) {
                String key = userDataStr.substring(I18N_PREFIX.length());
                tooltip.textProperty().bind(i18n.createBinding(key));
            }
        }
    }
    
    /**
     * 绑定文本属性
     */
    private static void bindText(Node node, String key) {
        if (node instanceof Labeled) {
            ((Labeled) node).textProperty().bind(i18n.createBinding(key));
        } else if (node instanceof TitledPane) {
            ((TitledPane) node).textProperty().bind(i18n.createBinding(key));
        } else if (node instanceof Text) {
            ((Text) node).textProperty().bind(i18n.createBinding(key));
        }
        // 注意：Tab 不是 Node 的子类，通过 bindTab 方法单独处理
    }
    
    /**
     * 为Control创建并绑定Tooltip（用于动态创建的组件）
     */
    private static void bindTooltip(Node node, String key) {
        if (node instanceof Control) {
            Control control = (Control) node;
            Tooltip tooltip = control.getTooltip();
            if (tooltip == null) {
                tooltip = new Tooltip();
                control.setTooltip(tooltip);
            }
            tooltip.textProperty().bind(i18n.createBinding(key));
        }
    }
    
    /**
     * 绑定PromptText
     */
    private static void bindPromptText(Node node, String key) {
        if (node instanceof TextField) {
            ((TextField) node).promptTextProperty().bind(i18n.createBinding(key));
        } else if (node instanceof TextArea) {
            ((TextArea) node).promptTextProperty().bind(i18n.createBinding(key));
        } else if (node instanceof ComboBox) {
            ((ComboBox<?>) node).promptTextProperty().bind(i18n.createBinding(key));
        }
    }
    
    /**
     * 绑定Tab
     */
    private static void bindTab(Tab tab) {
        Object userData = tab.getUserData();
        if (userData != null && userData.toString().startsWith(I18N_PREFIX)) {
            String key = userData.toString().substring(I18N_PREFIX.length());
            tab.textProperty().bind(i18n.createBinding(key));
        }
    }
    
    /**
     * 绑定Menu（递归）
     */
    private static void bindMenu(Menu menu) {
        Object userData = menu.getUserData();
        if (userData != null && userData.toString().startsWith(I18N_PREFIX)) {
            String key = userData.toString().substring(I18N_PREFIX.length());
            menu.textProperty().bind(i18n.createBinding(key));
        }
        
        for (MenuItem item : menu.getItems()) {
            if (item instanceof Menu) {
                bindMenu((Menu) item);
            } else {
                bindMenuItem(item);
            }
        }
    }
    
    /**
     * 绑定MenuItem
     */
    private static void bindMenuItem(MenuItem item) {
        Object userData = item.getUserData();
        if (userData != null && userData.toString().startsWith(I18N_PREFIX)) {
            String key = userData.toString().substring(I18N_PREFIX.length());
            item.textProperty().bind(i18n.createBinding(key));
        }
    }
    
    /**
     * 清除绑定缓存（用于测试或重新绑定）
     */
    public static void clearBindingCache() {
        boundComponents.clear();
    }
    
    /**
     * 手动为单个组件绑定国际化文本
     * @param node 组件
     * @param key 资源键
     */
    public static void bind(Node node, String key) {
        if (node != null && key != null) {
            bindText(node, key);
        }
    }
    
    /**
     * 手动为单个组件绑定文本和Tooltip
     * @param node 组件
     * @param key 文本资源键
     * @param tooltipKey Tooltip资源键
     */
    public static void bindWithTooltip(Node node, String key, String tooltipKey) {
        if (node != null) {
            if (key != null) {
                bindText(node, key);
            }
            if (tooltipKey != null) {
                bindTooltip(node, tooltipKey);
            }
        }
    }
    
    /**
     * 获取国际化文本（快捷方法）
     */
    public static String getString(String key) {
        return i18n.getString(key);
    }
    
    /**
     * 获取格式化的国际化文本（快捷方法）
     */
    public static String getString(String key, Object... args) {
        return i18n.getString(key, args);
    }
    
    /**
     * 为 TableColumn 绑定国际化文本
     * @param column TableColumn对象
     * @param key 资源键
     */
    public static void bindTableColumn(TableColumn<?, ?> column, String key) {
        if (column != null && key != null) {
            column.textProperty().bind(i18n.createBinding(key));
        }
    }
    
    /**
     * 为 Button/Label/等 Labeled 组件绑定国际化文本（支持后缀拼接）
     * @param labeled Labeled组件
     * @param key 资源键
     * @param suffix 要拼接的后缀（可为null）
     */
    public static void bindTextWithSuffix(Labeled labeled, String key, String suffix) {
        if (labeled != null && key != null) {
            if (suffix != null && !suffix.isEmpty()) {
                labeled.textProperty().bind(i18n.createBinding(key).concat(suffix));
            } else {
                labeled.textProperty().bind(i18n.createBinding(key));
            }
        }
    }
    
    /**
     * 为组件的 text 属性绑定国际化（通过反射支持各种组件）
     * @param component 组件对象
     * @param key 资源键
     */
    public static void bindTextProperty(Object component, String key) {
        if (component == null || key == null) {
            return;
        }
        
        try {
            // 尝试获取 textProperty 方法
            Method method = component.getClass().getMethod("textProperty");
            Object property = method.invoke(component);
            
            if (property instanceof StringProperty) {
                ((StringProperty) property).bind(i18n.createBinding(key));
            }
        } catch (Exception e) {
            System.err.println("无法为组件绑定国际化: " + component.getClass().getName());
        }
    }
    
    /**
     * 创建一个国际化字符串绑定（快捷方法）
     * @param key 资源键
     * @return StringBinding对象
     */
    public static StringBinding createBinding(String key) {
        return i18n.createBinding(key);
    }
    
    /**
     * 创建一个带格式化参数的国际化字符串绑定（快捷方法）
     * @param key 资源键
     * @param args 格式化参数
     * @return StringBinding对象
     */
    public static StringBinding createBinding(String key, Object... args) {
        return i18n.createBinding(key, args);
    }
    
    /**
     * 获取语言环境属性（用于监听语言变化）
     * @return ObjectProperty<Locale>
     */
    public static ObjectProperty<Locale> localeProperty() {
        return i18n.localeProperty();
    }
    
    /**
     * 获取 I18nManager 实例（用于高级场景）
     * @return I18nManager实例
     */
    public static I18nManager getManager() {
        return i18n;
    }
    
    /**
     * 创建支持国际化的 FilterGroup（自动响应语言切换）
     * @param key 资源键
     * @return FilterGroup对象
     */
    public static <T> com.dlsc.gemsfx.FilterView.FilterGroup<T> createI18nFilterGroup(String key) {
        com.dlsc.gemsfx.FilterView.FilterGroup<T> group = 
            new com.dlsc.gemsfx.FilterView.FilterGroup<>(getString(key));
        
        // 自动注册监听器，响应语言切换
        localeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                group.setName(getString(key));
            }
        });
        
        return group;
    }
    
    /**
     * 为 Labeled 组件绑定动态国际化文本（支持后缀动态更新）
     * @param labeled Labeled组件
     * @param key 资源键
     * @param suffixSupplier 后缀提供者（可为null）
     */
    public static void bindDynamicText(Labeled labeled, String key, 
                                       Supplier<String> suffixSupplier) {
        if (labeled == null || key == null) {
            return;
        }
        
        Runnable update = () -> {
            String text = getString(key);
            if (suffixSupplier != null) {
                text += suffixSupplier.get();
            }
            labeled.setText(text);
        };
        
        // 初始更新
        update.run();
        
        // 监听语言切换
        localeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                update.run();
            }
        });
    }
    
    /**
     * 创建支持国际化的 Label
     * @param key 资源键
     * @return Label对象
     */
    public static Label createI18nLabel(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.createBinding(key));
        return label;
    }
    
    /**
     * 创建支持国际化的 Text
     * @param key 资源键
     * @return Text对象
     */
    public static Text createI18nText(String key) {
        Text text = new Text();
        text.textProperty().bind(i18n.createBinding(key));
        return text;
    }
    
    /**
     * 绑定ComboBox的选项列表
     * 资源文件格式：
     *   key.option1=选项1
     *   key.option2=选项2
     *   key.option3=选项3
     * 或者使用 .count 指定数量：
     *   key.count=3
     *   key.1=选项1
     *   key.2=选项2
     *   key.3=选项3
     * 
     * @param node ComboBox节点
     * @param baseKey 基础资源键
     */
    private static void bindComboBoxItems(Node node, String baseKey) {
        if (!(node instanceof ComboBox)) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        ComboBox<String> comboBox = (ComboBox<String>) node;
        
        // 保存当前选中的索引
        int selectedIndex = comboBox.getSelectionModel().getSelectedIndex();
        
        // 创建可观察列表来存储选项
        ObservableList<String> items = FXCollections.observableArrayList();
        
        // 尝试两种格式：
        // 格式1: key.count + key.1, key.2, ...
        String countKey = baseKey + ".count";
        String countStr = i18n.getString(countKey);
        
        if (!countStr.equals(countKey)) {
            // 找到了 .count，使用数字索引
            try {
                int count = Integer.parseInt(countStr);
                for (int i = 1; i <= count; i++) {
                    String optionKey = baseKey + "." + i;
                    items.add(i18n.getString(optionKey));
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid count format for key: " + countKey);
            }
        } else {
            // 格式2: key.option1, key.option2, ...
            int i = 1;
            while (true) {
                String optionKey = baseKey + ".option" + i;
                String value = i18n.getString(optionKey);
                if (value.equals(optionKey)) {
                    // 没有找到更多选项
                    break;
                }
                items.add(value);
                i++;
            }
        }
        
        // 如果没有找到任何选项，尝试旧格式（兼容性）
        if (items.isEmpty()) {
            // 格式3: key.1, key.2, ... (不需要 .count)
            int i = 1;
            while (i <= 20) { // 最多尝试20个选项
                String optionKey = baseKey + "." + i;
                String value = i18n.getString(optionKey);
                if (value.equals(optionKey)) {
                    break;
                }
                items.add(value);
                i++;
            }
        }
        
        // 设置选项
        comboBox.setItems(items);
        
        // 恢复之前的选中项
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            comboBox.getSelectionModel().select(selectedIndex);
        } else if (!items.isEmpty()) {
            comboBox.getSelectionModel().select(0);
        }
        
        // 监听语言切换事件，自动更新
        i18n.localeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                bindComboBoxItems(node, baseKey);
            }
        });
    }
    
    /**
     * 为Control绑定ContextMenu（右键菜单）
     * 资源文件格式：
     *   key.count=7
     *   key.1=剪切
     *   key.2=复制
     *   ...
     * 
     * @param node Control节点
     * @param baseKey 基础资源键
     */
    private static void bindContextMenu(Node node, String baseKey) {
        if (!(node instanceof Control)) {
            return;
        }
        
        Control control = (Control) node;
        ContextMenu contextMenu = new ContextMenu();
        
        // 读取菜单项数量
        String countKey = baseKey + ".count";
        String countStr = i18n.getString(countKey);
        
        try {
            int count = Integer.parseInt(countStr);
            for (int i = 1; i <= count; i++) {
                String itemKey = baseKey + "." + i;
                MenuItem menuItem = new MenuItem();
                menuItem.textProperty().bind(i18n.createBinding(itemKey));
                
                // 可以在这里设置菜单项的动作（需要额外的机制来传递）
                contextMenu.getItems().add(menuItem);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid count format for context menu: " + countKey);
            return;
        }
        
        control.setContextMenu(contextMenu);
    }
    
    /**
     * 手动为ComboBox绑定国际化选项
     * @param comboBox ComboBox控件
     * @param baseKey 基础资源键
     */
    public static void bindComboBox(ComboBox<String> comboBox, String baseKey) {
        if (comboBox != null && baseKey != null) {
            bindComboBoxItems(comboBox, baseKey);
        }
    }
    
    /**
     * 自动为 TextField、TextArea 和 CodeArea 添加国际化右键菜单
     * @param node 节点
     */
    private static void autoAddContextMenu(Node node) {
        // 处理 TextField
        if (node instanceof TextField) {
            TextField textField = (TextField) node;
            
            // 检查是否已有自定义菜单（避免重复设置）
            ContextMenu existingMenu = textField.getContextMenu();
            if (existingMenu instanceof DefaultContextMenu) {
                return;
            }
            
            // 创建并设置国际化右键菜单（传入控件引用以确保功能正常）
            boolean readOnly = !textField.isEditable();
            DefaultContextMenu contextMenu = 
                new DefaultContextMenu(readOnly, textField);
            textField.setContextMenu(contextMenu);
        }
        // 处理 TextArea
        else if (node instanceof TextArea) {
            TextArea textArea = (TextArea) node;
            
            // 检查是否已有自定义菜单（避免重复设置）
            ContextMenu existingMenu = textArea.getContextMenu();
            if (existingMenu instanceof DefaultContextMenu) {
                return;
            }
            
            // 创建并设置国际化右键菜单（传入控件引用以确保功能正常）
            boolean readOnly = !textArea.isEditable();
            DefaultContextMenu contextMenu = 
                new DefaultContextMenu(readOnly, textArea);
            textArea.setContextMenu(contextMenu);
        }
        // 处理 CodeArea (GenericStyledArea)
        else if (node instanceof org.fxmisc.richtext.GenericStyledArea) {
            org.fxmisc.richtext.GenericStyledArea<?, ?, ?> codeArea = 
                (org.fxmisc.richtext.GenericStyledArea<?, ?, ?>) node;
            
            // 检查是否已有自定义菜单（避免重复设置）
            javafx.scene.control.ContextMenu existingMenu = codeArea.getContextMenu();
            if (existingMenu instanceof DefaultContextMenu) {
                return;
            }
            
            // 创建并设置国际化右键菜单（传入控件引用以确保功能正常）
            boolean readOnly = !codeArea.isEditable();
            DefaultContextMenu contextMenu = 
                new DefaultContextMenu(readOnly, codeArea);
            codeArea.setContextMenu(contextMenu);
        }
    }
    
    /**
     * 绑定ComboBox的默认选中值
     * 资源文件格式：
     *   key=不保护
     * 
     * @param node ComboBox节点
     * @param key 资源键
     */
    private static void bindComboBoxValue(Node node, String key) {
        if (!(node instanceof ComboBox)) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        ComboBox<String> comboBox = (ComboBox<String>) node;
        
        // 获取翻译值并设置为默认选中
        String value = i18n.getString(key);
        if (!value.equals(key)) {
            // 尝试在items中查找匹配项
            for (int i = 0; i < comboBox.getItems().size(); i++) {
                if (value.equals(comboBox.getItems().get(i))) {
                    comboBox.getSelectionModel().select(i);
                    break;
                }
            }
        }
        
        // 监听语言切换，自动更新选中值
        i18n.localeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String newValue = i18n.getString(key);
                for (int i = 0; i < comboBox.getItems().size(); i++) {
                    if (newValue.equals(comboBox.getItems().get(i))) {
                        comboBox.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        });
    }
}

