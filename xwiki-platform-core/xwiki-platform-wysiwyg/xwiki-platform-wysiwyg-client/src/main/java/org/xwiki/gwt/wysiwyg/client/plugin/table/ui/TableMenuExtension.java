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
package org.xwiki.gwt.wysiwyg.client.plugin.table.ui;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.user.client.RichTextAreaCommand;
import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TableFeature;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteCol;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteRow;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.DeleteTable;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertColAfter;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertColBefore;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertRowAfter;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertRowBefore;
import org.xwiki.gwt.wysiwyg.client.plugin.table.feature.InsertTable;

import com.google.gwt.event.logical.shared.AttachEvent;

/**
 * Provides user interface for manipulating tables through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class TableMenuExtension extends MenuItemUIExtensionAdaptor
{
    /**
     * The Table plug-in associated with this menu extension.
     */
    private final TablePlugin plugin;

    /**
     * The menu item used to insert a table.
     */
    private final MenuItem insertTable;

    /**
     * The list of menu options that are available when there is a table selected.
     */
    private final List<MenuItem> editItems;

    /**
     * Builds the menu extension using the passed plugin.
     * 
     * @param plugin the plugin to use for the creation of this menu extension.
     */
    public TableMenuExtension(final TablePlugin plugin)
    {
        super("menu");

        this.plugin = plugin;

        insertTable =
            createMenuItem(Strings.INSTANCE.insertTable(), null, new RichTextAreaCommand(plugin.getTextArea(),
                new Command(InsertTable.NAME), null, false));
        MenuItem deleteCol =
            createMenuItem(Strings.INSTANCE.deleteCol(), Images.INSTANCE.deleteCol(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(DeleteCol.NAME)));
        MenuItem deleteRow =
            createMenuItem(Strings.INSTANCE.deleteRow(), Images.INSTANCE.deleteRow(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(DeleteRow.NAME)));
        MenuItem deleteTable =
            createMenuItem(Strings.INSTANCE.deleteTable(), Images.INSTANCE.deleteTable(), new RichTextAreaCommand(
                plugin.getTextArea(), new Command(DeleteTable.NAME)));
        MenuItem insertColAfter =
            createMenuItem(Strings.INSTANCE.insertColAfter(), Images.INSTANCE.insertColAfter(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(InsertColAfter.NAME)));
        MenuItem insertColBefore =
            createMenuItem(Strings.INSTANCE.insertColBefore(), Images.INSTANCE.insertColBefore(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(InsertColBefore.NAME)));
        MenuItem insertRowAfter =
            createMenuItem(Strings.INSTANCE.insertRowAfter(), Images.INSTANCE.insertRowAfter(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(InsertRowAfter.NAME)));
        MenuItem insertRowBefore =
            createMenuItem(Strings.INSTANCE.insertRowBefore(), Images.INSTANCE.insertRowBefore(),
                new RichTextAreaCommand(plugin.getTextArea(), new Command(InsertRowBefore.NAME)));
        MenuItem tableMenu = createMenuItem(Strings.INSTANCE.table(), Images.INSTANCE.insertTable());

        editItems =
            Arrays.asList(deleteCol, deleteRow, deleteTable, insertColAfter, insertColBefore, insertRowAfter,
                insertRowBefore);

        addFeature(TablePluginFactory.getInstance().getPluginName(), tableMenu);
        addFeature(InsertTable.NAME, insertTable);
        addFeature(DeleteCol.NAME, deleteCol);
        addFeature(DeleteRow.NAME, deleteRow);
        addFeature(DeleteTable.NAME, deleteTable);
        addFeature(InsertColAfter.NAME, insertColAfter);
        addFeature(InsertColBefore.NAME, insertColBefore);
        addFeature(InsertRowAfter.NAME, insertRowAfter);
        addFeature(InsertRowBefore.NAME, insertRowBefore);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuItemUIExtensionAdaptor#onAttach(AttachEvent)
     */
    protected void onAttach(AttachEvent event)
    {
        boolean editMode = plugin.getTextArea().getCommandManager().isEnabled(new Command(DeleteTable.NAME));
        List<TableFeature> features = plugin.getFeatures();
        for (TableFeature feature : features) {
            MenuItem item = (MenuItem) getUIObject(feature.getName());
            if (item.getParentMenu() == event.getSource() && editItems.contains(item)) {
                item.setEnabled(editMode && feature.isEnabled());
                item.setVisible(editMode);
            }
        }
        if (insertTable.getParentMenu() == event.getSource()) {
            insertTable.setEnabled(!editMode
                && plugin.getTextArea().getCommandManager().isEnabled(new Command(InsertTable.NAME)));
            insertTable.setVisible(!editMode);
        }
    }
}
