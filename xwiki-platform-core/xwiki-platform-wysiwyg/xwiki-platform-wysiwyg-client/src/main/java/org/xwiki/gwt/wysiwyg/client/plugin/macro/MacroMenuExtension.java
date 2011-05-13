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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import org.xwiki.gwt.user.client.RichTextAreaCommand;
import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;

import com.google.gwt.event.logical.shared.AttachEvent;

/**
 * Provides a user interface extension to allow users to manipulate macros using the top-level menu of the WYSIWYG
 * editor.
 * 
 * @version $Id$
 */
public class MacroMenuExtension extends MenuItemUIExtensionAdaptor
{
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
        super("menu");

        setShortcutKeyManager(plugin.getShortcutKeyManager());
        this.plugin = plugin;

        MenuItem refresh =
            createMenuItem(Strings.INSTANCE.macroRefresh(), Strings.INSTANCE.macroRefreshShortcutKeyLabel(),
                Images.INSTANCE.macroRefresh(), new RichTextAreaCommand(plugin.getTextArea(), MacroPlugin.REFRESH,
                    null, false), 'R');
        collapse =
            createMenuItem(Strings.INSTANCE.macroCollapseAll(), Strings.INSTANCE.macroCollapseAllShortcutKeyLabel(),
                null, new RichTextAreaCommand(plugin.getTextArea(), MacroPlugin.COLLAPSE), 'C');
        expand =
            createMenuItem(Strings.INSTANCE.macroExpandAll(), Strings.INSTANCE.macroExpandAllShortcutKeyLabel(), null,
                new RichTextAreaCommand(plugin.getTextArea(), MacroPlugin.EXPAND), 'E');
        edit =
            createMenuItem(Strings.INSTANCE.macroEdit(), Strings.INSTANCE.macroEditShortcutKeyLabel(),
                Images.INSTANCE.macroEdit(), new com.google.gwt.user.client.Command()
                {
                    public void execute()
                    {
                        plugin.edit();
                    }
                }, (char) 0);
        insert =
            createMenuItem(Strings.INSTANCE.macroInsert(), Strings.INSTANCE.macroInsertShortcutKeyLabel(),
                Images.INSTANCE.macroInsert(), new com.google.gwt.user.client.Command()
                {
                    public void execute()
                    {
                        if (plugin.getSelector().getMacroCount() <= 0) {
                            plugin.insert();
                        }
                    }
                }, 'M');
        MenuItem macroMenu = createMenuItem(Strings.INSTANCE.macro(), Images.INSTANCE.macro());

        addFeature(MacroPluginFactory.getInstance().getPluginName(), macroMenu);
        addFeature("macroRefresh", refresh);
        addFeature("macroCollapse", collapse);
        addFeature("macroExpand", expand);
        addFeature("macroEdit", edit);
        addFeature("macroInsert", insert);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuItemUIExtensionAdaptor#onAttach(AttachEvent)
     */
    protected void onAttach(AttachEvent event)
    {
        boolean editMode = plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.INSERT);
        if (collapse.getParentMenu() == event.getSource()) {
            collapse.setEnabled(!plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.COLLAPSE));
            if (editMode) {
                collapse.setText(Strings.INSTANCE.macroCollapse());
                collapse.setShortcutKeyLabel(Strings.INSTANCE.macroCollapseShortcutKeyLabel());
            } else {
                collapse.setText(Strings.INSTANCE.macroCollapseAll());
                collapse.setShortcutKeyLabel(Strings.INSTANCE.macroCollapseAllShortcutKeyLabel());
            }
        }
        if (expand.getParentMenu() == event.getSource()) {
            expand.setEnabled(!plugin.getTextArea().getCommandManager().isExecuted(MacroPlugin.EXPAND));
            if (editMode) {
                expand.setText(Strings.INSTANCE.macroExpand());
                expand.setShortcutKeyLabel(Strings.INSTANCE.macroExpandShortcutKeyLabel());
            } else {
                expand.setText(Strings.INSTANCE.macroExpandAll());
                expand.setShortcutKeyLabel(Strings.INSTANCE.macroExpandAllShortcutKeyLabel());
            }
        }
        if (insert.getParentMenu() == event.getSource()) {
            insert.setEnabled(!editMode && plugin.getTextArea().getCommandManager().isEnabled(MacroPlugin.INSERT));
            insert.setVisible(!editMode);
        }
        if (edit.getParentMenu() == event.getSource()) {
            edit.setEnabled(editMode && plugin.getSelector().getMacroCount() == 1);
            edit.setVisible(editMode);
        }
    }
}
