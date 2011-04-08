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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.DeferredUpdater;
import org.xwiki.gwt.user.client.RichTextAreaCommand;
import org.xwiki.gwt.user.client.Updatable;
import org.xwiki.gwt.user.client.ui.MenuBar;
import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.MenuListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtension;
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

import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Provides user interface for manipulating tables through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class TableMenuExtension extends MenuItemUIExtension implements Updatable, MenuListener
{
    /**
     * Schedules menu updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * The Table plug-in associated with this menu extension.
     */
    private final TablePlugin plugin;

    /**
     * The list of menu options used to create tables.
     */
    private List<UIObject> createTableMenus = new ArrayList<UIObject>();

    /**
     * The list of menu options used to edit tables or to remove them.
     */
    private List<UIObject> editTableMenus = new ArrayList<UIObject>();

    /**
     * Map of the menu items.
     */
    private Map<String, MenuItem> editMenuItems = new HashMap<String, MenuItem>();

    /**
     * The submenu holding the various table options.
     */
    private MenuBar submenu;

    /**
     * The toplevel menu item corresponding to this menu extension.
     */
    private MenuItem menu;

    /**
     * Builds the menu extension using the passed plugin.
     * 
     * @param plugin the plugin to use for the creation of this menu extension.
     */
    public TableMenuExtension(final TablePlugin plugin)
    {
        super("menu");
        this.plugin = plugin;

        MenuItem insertTable =
            new MenuItem(Strings.INSTANCE.insertTable(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                InsertTable.NAME), null, false));

        createTableMenus.add(insertTable);

        createEditMenuItems();

        submenu = new MenuBar(true);
        submenu.setAnimationEnabled(false);
        submenu.addAll(createTableMenus);

        menu = new MenuItem(Strings.INSTANCE.table(), submenu);
        menu.setIcon(Images.INSTANCE.insertTable());
        menu.addMenuListener(this);

        addFeature(TablePluginFactory.getInstance().getPluginName(), menu);
    }

    /**
     * Creates the menu items used for editing a table.
     */
    private void createEditMenuItems()
    {
        MenuItem deleteCol =
            new MenuItem(Strings.INSTANCE.deleteCol(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                DeleteCol.NAME)));
        deleteCol.setIcon(Images.INSTANCE.deleteCol());

        MenuItem deleteRow =
            new MenuItem(Strings.INSTANCE.deleteRow(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                DeleteRow.NAME)));
        deleteRow.setIcon(Images.INSTANCE.deleteRow());

        MenuItem deleteTable =
            new MenuItem(Strings.INSTANCE.deleteTable(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                DeleteTable.NAME)));
        deleteTable.setIcon(Images.INSTANCE.deleteTable());

        MenuItem insertColAfter =
            new MenuItem(Strings.INSTANCE.insertColAfter(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                InsertColAfter.NAME)));
        insertColAfter.setIcon(Images.INSTANCE.insertColAfter());

        MenuItem insertColBefore =
            new MenuItem(Strings.INSTANCE.insertColBefore(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                InsertColBefore.NAME)));
        insertColBefore.setIcon(Images.INSTANCE.insertColBefore());

        MenuItem insertRowAfter =
            new MenuItem(Strings.INSTANCE.insertRowAfter(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                InsertRowAfter.NAME)));
        insertRowAfter.setIcon(Images.INSTANCE.insertRowAfter());

        MenuItem insertRowBefore =
            new MenuItem(Strings.INSTANCE.insertRowBefore(), new RichTextAreaCommand(plugin.getTextArea(), new Command(
                InsertRowBefore.NAME)));
        insertRowBefore.setIcon(Images.INSTANCE.insertRowBefore());

        editMenuItems.put(DeleteCol.NAME, deleteCol);
        editMenuItems.put(DeleteRow.NAME, deleteRow);
        editMenuItems.put(DeleteTable.NAME, deleteTable);
        editMenuItems.put(InsertColAfter.NAME, insertColAfter);
        editMenuItems.put(InsertColBefore.NAME, insertColBefore);
        editMenuItems.put(InsertRowAfter.NAME, insertRowAfter);
        editMenuItems.put(InsertRowBefore.NAME, insertRowBefore);

        editTableMenus.add(insertColBefore);
        editTableMenus.add(insertColAfter);
        editTableMenus.add(deleteCol);
        editTableMenus.add(new MenuItemSeparator());
        editTableMenus.add(insertRowBefore);
        editTableMenus.add(insertRowAfter);
        editTableMenus.add(deleteRow);
        editTableMenus.add(new MenuItemSeparator());
        editTableMenus.add(deleteTable);
    }

    /**
     * Cleans up this menu extension on destroy.
     */
    void destroy()
    {
        createTableMenus.clear();
        createTableMenus = null;

        editTableMenus.clear();
        editTableMenus = null;

        submenu.clearItems();
        submenu = null;

        menu.getParentMenu().removeItem(menu);
        menu.removeMenuListener(this);
        menu = null;

        this.clearFeatures();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.gwt.user.client.ui.MenuListener#onMenuItemSelected(org.xwiki.gwt.user.client.ui.MenuItem)
     */
    public void onMenuItemSelected(MenuItem menuItem)
    {
        // update the list of shown options
        updater.deferUpdate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.gwt.user.client.Updatable#update()
     */
    public void update()
    {
        // test first if deleteTable is enabled (i.e. we are inside a table)
        if (plugin.getTextArea().getCommandManager().isEnabled(new Command(DeleteTable.NAME))) {
            // activate the edit submenu
            if (submenu.getItem(0) != editTableMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(editTableMenus);
            }

            // Enable / disable menu entries.
            List<TableFeature> features = plugin.getFeatures();
            for (TableFeature feature : features) {
                if (editMenuItems.containsKey(feature.getName())) {
                    editMenuItems.get(feature.getName()).setEnabled(feature.isEnabled());
                }
            }
        } else {
            // the create table list must be setup, and disabled if create table is not possible
            if (submenu.getItem(0) != createTableMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(createTableMenus);
            }
            boolean canCreateTable = plugin.getTextArea().getCommandManager().isEnabled(new Command(InsertTable.NAME));
            // set enabling state of the menu items in the submenu
            for (UIObject m : createTableMenus) {
                if (m instanceof MenuItem) {
                    ((MenuItem) m).setEnabled(canCreateTable);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#canUpdate()
     */
    public boolean canUpdate()
    {
        return plugin.getTextArea().isAttached() && plugin.getTextArea().isEnabled();
    }
}
