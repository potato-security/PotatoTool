package com.potato.potatotool.utils.ui;

import com.potato.potatotool.utils.core.I18nManager;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.undo.UndoManager;

/**
 * 支持国际化的右键菜单
 * 自动适配 TextField、TextArea 和 CodeArea
 * 
 * @author Potato
 * @date 2023/10/16 10:31
 */
public class DefaultContextMenu extends ContextMenu
{
    private MenuItem cut, copy, delete, paste, undo, redo, selectAll;
    private GenericStyledArea area;
    private TextInputControl textInputControl;
    private IndexRange range;
    private static final I18nManager i18n = I18nManager.getInstance();
    private boolean isReadOnly = false;

    public DefaultContextMenu()
    {
        this(false);
    }
    
    /**
     * 构造函数
     * @param readOnly 是否为只读模式（只读时只显示复制和全选）
     */
    public DefaultContextMenu(boolean readOnly)
    {
        this(readOnly, null);
    }
    
    /**
     * 构造函数（推荐使用）
     * @param readOnly 是否为只读模式
     * @param owner 关联的控件（TextInputControl 或 GenericStyledArea）
     */
    public DefaultContextMenu(boolean readOnly, Object owner)
    {
        this.isReadOnly = readOnly;
        
        // 提前保存控件引用，避免 getOwnerNode() 可能返回 null 的问题
        if (owner instanceof GenericStyledArea) {
            this.area = (GenericStyledArea) owner;
            this.textInputControl = null;
        } else if (owner instanceof TextInputControl) {
            this.textInputControl = (TextInputControl) owner;
            this.area = null;
        }
        
        // 监听菜单显示事件，动态更新菜单项状态
        showingProperty().addListener( (ob, ov, showing) -> {
            if (showing) {
                updateMenuVisibility();
                checkMenuItems();
            }
        });

        // 创建菜单项并绑定国际化文本
        initMenuItems();

        // 设置字体大小
        MenuItem[] allMenuItems = {undo, redo, cut, copy, paste, delete, selectAll};
        for (MenuItem menuItem : allMenuItems) {
            menuItem.setStyle("-fx-font-size: 16;");
        }

        // 初始添加菜单项（根据只读状态）
        if (readOnly) {
            getItems().addAll(copy, selectAll);
        } else {
            getItems().addAll(undo, redo, cut, copy, paste, delete, selectAll);
        }
    }
    
    /**
     * 初始化所有菜单项
     */
    private void initMenuItems() {
        // 复制
        copy = new MenuItem();
        copy.textProperty().bind(i18n.createBinding("contextmenu.copy"));
        copy.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.copy();
            } else if (textInputControl != null) {
                textInputControl.copy();
            }
        });

        // 全选
        selectAll = new MenuItem();
        selectAll.textProperty().bind(i18n.createBinding("contextmenu.selectall"));
        selectAll.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.selectAll();
            } else if (textInputControl != null) {
                textInputControl.selectAll();
            }
        });

        // 撤销
        undo = new MenuItem();
        undo.textProperty().bind(i18n.createBinding("contextmenu.undo"));
        undo.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.undo();
            } else if (textInputControl != null) {
                textInputControl.undo();
            }
        });

        // 重做
        redo = new MenuItem();
        redo.textProperty().bind(i18n.createBinding("contextmenu.redo"));
        redo.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.redo();
            } else if (textInputControl != null) {
                textInputControl.redo();
            }
        });

        // 剪切
        cut = new MenuItem();
        cut.textProperty().bind(i18n.createBinding("contextmenu.cut"));
        cut.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.cut();
            } else if (textInputControl != null) {
                textInputControl.cut();
            }
        });

        // 粘贴
        paste = new MenuItem();
        paste.textProperty().bind(i18n.createBinding("contextmenu.paste"));
        paste.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.paste();
            } else if (textInputControl != null) {
                textInputControl.paste();
            }
        });

        // 删除
        delete = new MenuItem();
        delete.textProperty().bind(i18n.createBinding("contextmenu.delete"));
        delete.setOnAction( event -> { 
            hide(); 
            if (area != null) {
                area.deleteText(range);
            } else if (textInputControl != null) {
                IndexRange selection = textInputControl.getSelection();
                textInputControl.deleteText(selection);
            }
        });
    }
    
    /**
     * 根据控件的实际可编辑状态更新菜单显示
     */
    private void updateMenuVisibility() {
        boolean actualReadOnly = false;
        
        // 检测实际的只读状态
        if (area != null) {
            actualReadOnly = !area.isEditable();
        } else if (textInputControl != null) {
            actualReadOnly = !textInputControl.isEditable();
        } else {
            // 如果构造时没有传入控件引用，尝试从 ownerNode 获取
            Object owner = getOwnerNode();
            if (owner instanceof GenericStyledArea) {
                actualReadOnly = !((GenericStyledArea) owner).isEditable();
            } else if (owner instanceof TextInputControl) {
                actualReadOnly = !((TextInputControl) owner).isEditable();
            }
        }
        
        // 如果状态改变，重新构建菜单
        if (actualReadOnly != isReadOnly) {
            isReadOnly = actualReadOnly;
            getItems().clear();
            
            if (isReadOnly) {
                // 只读模式：只显示复制和全选
                getItems().addAll(copy, selectAll);
            } else {
                // 可编辑模式：显示完整菜单
                getItems().addAll(undo, redo, cut, copy, paste, delete, selectAll);
            }
        }
    }

    /**
     * 检查并更新菜单项的启用/禁用状态
     */
    private void checkMenuItems()
    {
        // 如果构造时没有传入控件引用，尝试从 ownerNode 获取
        if (area == null && textInputControl == null) {
            Object owner = getOwnerNode();
            if (owner instanceof GenericStyledArea) {
                area = (GenericStyledArea) owner;
            } else if (owner instanceof TextInputControl) {
                textInputControl = (TextInputControl) owner;
            }
        }
        
        // 处理 GenericStyledArea (CodeArea)
        if (area != null && textInputControl == null) {
            boolean actualReadOnly = !area.isEditable();
            
            if (!actualReadOnly) {
                // 可编辑模式
                UndoManager history = area.getUndoManager();
                undo.setDisable(!history.isUndoAvailable());
                redo.setDisable(!history.isRedoAvailable());

                range = area.getSelection();
                boolean noSelection = range.getLength() == 0;
                delete.setDisable(noSelection);
                copy.setDisable(noSelection);
                cut.setDisable(noSelection);
                
                selectAll.setDisable(area.getText().isEmpty());
            } else {
                // 只读模式
                boolean noSelection = area.getSelection().getLength() == 0;
                copy.setDisable(noSelection);
                selectAll.setDisable(area.getText().isEmpty());
            }
        } 
        // 处理 TextInputControl (TextArea, TextField)
        else if (textInputControl != null && area == null) {
            boolean actualReadOnly = !textInputControl.isEditable();
            
            if (!actualReadOnly) {
                // 可编辑模式
                undo.setDisable(!textInputControl.isUndoable());
                redo.setDisable(!textInputControl.isRedoable());

                IndexRange selection = textInputControl.getSelection();
                boolean noSelection = selection.getLength() == 0;
                delete.setDisable(noSelection);
                copy.setDisable(noSelection);
                cut.setDisable(noSelection);
                
                selectAll.setDisable(textInputControl.getText().isEmpty());
            } else {
                // 只读模式
                IndexRange selection = textInputControl.getSelection();
                boolean noSelection = selection.getLength() == 0;
                copy.setDisable(noSelection);
                selectAll.setDisable(textInputControl.getText().isEmpty());
            }
        }
    }
}
