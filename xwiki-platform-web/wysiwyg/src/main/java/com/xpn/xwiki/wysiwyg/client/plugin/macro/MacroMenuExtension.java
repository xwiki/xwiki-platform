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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MenuItemUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.DeferredUpdater;
import com.xpn.xwiki.wysiwyg.client.util.Updatable;
import com.xpn.xwiki.wysiwyg.client.widget.MenuBar;
import com.xpn.xwiki.wysiwyg.client.widget.MenuItem;
import com.xpn.xwiki.wysiwyg.client.widget.MenuListener;

/**
 * Provides a user interface extension to allow users to manipulate macros using the top-level menu of the WYSIWYG
 * editor.
 * 
 * @version $Id$
 */
public class MacroMenuExtension implements Updatable, MenuListener
{
    /**
     * The macro menu item that is placed on the top menu bar and which opens the {@link #macroSubMenu}.
     */
    private MenuItem macroMenuItem;

    /**
     * The macro sub-menu, including entries for macro specific operations.
     */
    private MenuBar macroSubMenu;

    /**
     * Menu entries for insert related operations.
     */
    private List<UIObject> insertSubMenuEntries;

    /**
     * Menu entries for edit related operations.
     */
    private List<UIObject> editSubMenuEntries;

    /**
     * The menu item used to collapse selected macros, or all the macros of no macro is selected.
     */
    private MenuItem collapse;

    /**
     * The menu item used to expand selected macros, or all the macros of no macro is selected.
     */
    private MenuItem expand;

    /**
     * The menu item used to edit the selected macro.
     */
    private MenuItem edit;

    /**
     * The menu item used to insert one of the available macros.
     */
    private MenuItem insert;

    /**
     * User interface extension for the editor menu bar.
     */
    private final MenuItemUIExtension menuExtension = new MenuItemUIExtension("menu");

    /**
     * Schedules menu updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * The macro plug-in associated with this menu extension.
     */
    private final MacroPlugin plugin;

    /**
     * Creates a new menu extension for the given macro plug-in.
     * 
     * @param plugin a macro plug-in instance
     */
    public MacroMenuExtension(final MacroPlugin plugin)
    {
        this.plugin = plugin;

        MenuItem refresh = new MenuItem(Strings.INSTANCE.macroRefresh(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.getTextArea().getCommandManager().execute(MacroPlugin.REFRESH);
            }
        });
        refresh.setIcon(Images.INSTANCE.macroRefresh().createElement());
        collapse = new MenuItem(Strings.INSTANCE.macroCollapseAll(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.getTextArea().setFocus(true);
                plugin.getTextArea().getCommandManager().execute(MacroPlugin.COLLAPSE);
            }
        });
        expand = new MenuItem(Strings.INSTANCE.macroExpandAll(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.getTextArea().setFocus(true);
                plugin.getTextArea().getCommandManager().execute(MacroPlugin.EXPAND);
            }
        });
        edit = new MenuItem(Strings.INSTANCE.macroEdit(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.edit();
            }
        });
        edit.setIcon(Images.INSTANCE.macroEdit().createElement());
        insert = new MenuItem(Strings.INSTANCE.macroInsert(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.insert();
            }
        });
        insert.setIcon(Images.INSTANCE.macroInsert().createElement());

        insertSubMenuEntries = new ArrayList<UIObject>();
        insertSubMenuEntries.add(insert);
        insertSubMenuEntries.add(new MenuItemSeparator());
        insertSubMenuEntries.add(refresh);
        insertSubMenuEntries.add(new MenuItemSeparator());
        insertSubMenuEntries.add(collapse);
        insertSubMenuEntries.add(expand);

        editSubMenuEntries = new ArrayList<UIObject>();
        editSubMenuEntries.add(edit);
        editSubMenuEntries.add(new MenuItemSeparator());
        editSubMenuEntries.add(collapse);
        editSubMenuEntries.add(expand);

        macroSubMenu = new MenuBar(true);
        macroSubMenu.setAnimationEnabled(false);
        macroSubMenu.addAll(insertSubMenuEntries);

        macroMenuItem = new MenuItem(Strings.INSTANCE.macro(), macroSubMenu);
        macroMenuItem.setIcon(Images.INSTANCE.macro().createElement());
        macroMenuItem.addMenuListener(this);

        menuExtension.addFeature(MacroPluginFactory.getInstance().getPluginName(), macroMenuItem);
    }

    /**
     * Destroy this extension.
     */
    public void destroy()
    {
        insertSubMenuEntries.clear();
        insertSubMenuEntries = null;

        editSubMenuEntries.clear();
        editSubMenuEntries = null;

        macroSubMenu.clearItems();
        macroSubMenu = null;

        macroMenuItem.getParentMenu().removeItem(macroMenuItem);
        macroMenuItem.removeMenuListener(this);
        macroMenuItem = null;

        menuExtension.clearFeatures();
    }

    /**
     * @return the menu extension
     */
    public MenuItemUIExtension getExtension()
    {
        return menuExtension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuListener#onMenuItemSelected(MenuItem)
     */
    public void onMenuItemSelected(MenuItem menuItem)
    {
        updater.deferUpdate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#update()
     */
    public void update()
    {
        collapse.setEnabled(!plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.COLLAPSE));
        expand.setEnabled(!plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.EXPAND));

        if (plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.INSERT)) {
            if (macroSubMenu.getItem(0) != editSubMenuEntries.get(0)) {
                macroSubMenu.clearItems();
                macroSubMenu.addAll(editSubMenuEntries);
            }
            edit.setEnabled(plugin.getSelector().getMacroCount() == 1);
            collapse.setText(Strings.INSTANCE.macroCollapse());
            expand.setText(Strings.INSTANCE.macroExpand());
        } else {
            if (macroSubMenu.getItem(0) != insertSubMenuEntries.get(0)) {
                macroSubMenu.clearItems();
                macroSubMenu.addAll(insertSubMenuEntries);
            }
            insert.setEnabled(plugin.getTextArea().getCommandManager().isEnabled(MacroPlugin.INSERT));
            collapse.setText(Strings.INSTANCE.macroCollapseAll());
            expand.setText(Strings.INSTANCE.macroExpandAll());
        }
    }
}
