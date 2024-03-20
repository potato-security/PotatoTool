package com.potato.potatotool.utils;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.undo.UndoManager;

/**
 * @author Potato
 * @date 2023/10/16 10:31
 */
public class DefaultContextMenu extends ContextMenu
{
    private MenuItem cut, copy, delete, paste, undo, redo, selectAll;
    private GenericStyledArea area;
    private IndexRange range;

    public DefaultContextMenu()
    {
        showingProperty().addListener( (ob,ov,showing) -> checkMenuItems( showing ) );

        cut = new MenuItem( "剪切" );
        cut.setOnAction( AE -> { hide(); area.cut(); } );

        copy = new MenuItem( "复制" );
        copy.setOnAction( AE -> { hide(); area.copy(); } );

        delete = new MenuItem( "删除" );
        delete.setOnAction( AE -> { hide(); area.deleteText( range ); } );

        paste = new MenuItem( "粘贴" );
        paste.setOnAction( AE -> { hide(); area.paste(); } );

        redo = new MenuItem( "重做" );
        redo.setOnAction( AE -> { hide(); area.redo(); } );

        undo = new MenuItem( "撤销" );
        undo.setOnAction( AE -> { hide(); area.undo(); } );

        selectAll = new MenuItem( "全选" );
        selectAll.setOnAction( AE -> { hide(); area.selectAll(); } );

        MenuItem[] menuItems = {undo, redo, cut, copy, paste, delete, selectAll};

        for (MenuItem menuItem : menuItems) {
            menuItem.setStyle("-fx-font-size: 18;");
        }

        getItems().addAll( menuItems );
    }

    private void checkMenuItems( boolean showing )
    {
        if ( ! showing ) return;

        area = (GenericStyledArea) getOwnerNode();
        UndoManager history = area.getUndoManager();
        undo.setDisable( ! history.isUndoAvailable() );
        redo.setDisable( ! history.isRedoAvailable() );

        range = area.getSelection();
        boolean noSelection = range.getLength() == 0;
        delete.setDisable( noSelection );
        copy.setDisable( noSelection );
        cut.setDisable( noSelection );

        selectAll.setDisable(area.getText().isEmpty() );
    }
}