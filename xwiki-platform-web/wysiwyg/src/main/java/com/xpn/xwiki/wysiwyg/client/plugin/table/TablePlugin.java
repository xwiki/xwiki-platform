/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.wysiwyg.client.plugin.table;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.util.Config;

public class TablePlugin extends AbstractPlugin implements ClickListener
{
    private PushButton insertTable;

    private PushButton insertRowBefore;

    private PushButton insertRowAfter;

    private PushButton deleteRow;

    private PushButton insertColBefore;

    private PushButton insertColAfter;

    private PushButton deleteCol;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        insertTable = new PushButton(Images.INSTANCE.insertTable().createImage(), this);
        insertTable.setTitle(Strings.INSTANCE.insertTable());
        toolBarExtension.addFeature("inserttable", insertTable);

        insertRowBefore = new PushButton(Images.INSTANCE.insertRowBefore().createImage(), this);
        insertRowBefore.setTitle(Strings.INSTANCE.insertRowBefore());
        toolBarExtension.addFeature("insertrowbefore", insertRowBefore);

        insertRowAfter = new PushButton(Images.INSTANCE.insertRowAfter().createImage(), this);
        insertRowAfter.setTitle(Strings.INSTANCE.insertRowAfter());
        toolBarExtension.addFeature("insertrowafter", insertRowAfter);

        deleteRow = new PushButton(Images.INSTANCE.deleteRow().createImage(), this);
        deleteRow.setTitle(Strings.INSTANCE.deleteRow());
        toolBarExtension.addFeature("deleterow", deleteRow);

        insertColBefore = new PushButton(Images.INSTANCE.insertColBefore().createImage(), this);
        insertColBefore.setTitle(Strings.INSTANCE.insertColBefore());
        toolBarExtension.addFeature("insertcolbefore", insertColBefore);

        insertColAfter = new PushButton(Images.INSTANCE.insertColAfter().createImage(), this);
        insertColAfter.setTitle(Strings.INSTANCE.insertColAfter());
        toolBarExtension.addFeature("insertcolafter", insertColAfter);

        deleteCol = new PushButton(Images.INSTANCE.deleteCol().createImage(), this);
        deleteCol.setTitle(Strings.INSTANCE.deleteCol());
        toolBarExtension.addFeature("deletecol", deleteCol);

        getUIExtensionList().add(toolBarExtension);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        insertTable.removeFromParent();
        insertTable.removeClickListener(this);
        insertTable = null;

        insertRowBefore.removeFromParent();
        insertRowBefore.removeClickListener(this);
        insertRowBefore = null;

        insertRowAfter.removeFromParent();
        insertRowAfter.removeClickListener(this);
        insertRowAfter = null;

        deleteRow.removeFromParent();
        deleteRow.removeClickListener(this);
        deleteRow = null;

        insertColBefore.removeFromParent();
        insertColBefore.removeClickListener(this);
        insertColBefore = null;

        insertColAfter.removeFromParent();
        insertColAfter.removeClickListener(this);
        insertColAfter = null;

        deleteCol.removeFromParent();
        deleteCol.removeClickListener(this);
        deleteCol = null;

        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == insertTable) {
            onInsertTable();
        } else if (sender == insertRowBefore) {
            onInsertRowBefore();
        } else if (sender == insertRowAfter) {
            onInsertRowAfter();
        } else if (sender == deleteRow) {
            onDeleteRow();
        } else if (sender == insertColBefore) {
            onInsertColBefore();
        } else if (sender == insertColAfter) {
            onInsertColAfter();
        } else if (sender == deleteCol) {
            onDeleteCol();
        }
    }

    public void onInsertTable()
    {
        // TODO
    }

    public void onInsertRowBefore()
    {
        // TODO
    }

    public void onInsertRowAfter()
    {
        // TODO
    }

    public void onDeleteRow()
    {
        // TODO
    }

    public void onInsertColBefore()
    {
        // TODO
    }

    public void onInsertColAfter()
    {
        // TODO
    }

    public void onDeleteCol()
    {
        // TODO
    }
}
