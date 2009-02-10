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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.MenuItemUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * WYSIWYG editor plug-in for inserting macros and for editing macro parameters.
 * 
 * @version $Id$
 */
public class MacroPlugin extends AbstractPlugin implements ClickListener
{
    /**
     * The tool bar button that opens the insert macro dialog.
     */
    private PushButton insertButton;

    /**
     * The macro menu, including sub-entries for macro specific operations.
     */
    private MenuItem macroMenu;

    /**
     * Menu entries for insert related operations.
     */
    private MenuBar insertMenu;

    /**
     * Menu entries for edit related operations.
     */
    private MenuBar editMenu;

    /**
     * Hides macro meta data and displays macro output in a read only text box.
     */
    private MacroDisplayer displayer;

    /**
     * Controls the currently selected macros.
     */
    private MacroSelector selector;

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * User interface extension for the editor menu bar.
     */
    private final MenuItemUIExtension menuExtension = new MenuItemUIExtension("menu");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        insertButton = new PushButton(Images.INSTANCE.macro().createImage(), this);
        insertButton.setTitle(Strings.INSTANCE.macro());
        toolBarExtension.addFeature(MacroPluginFactory.getInstance().getPluginName(), insertButton);

        displayer = new MacroDisplayer(getTextArea());
        selector = new MacroSelector(displayer);

        insertMenu = new MenuBar(true);
        insertMenu.addItem("Browse Macros...", (com.google.gwt.user.client.Command) null);
        insertMenu.addSeparator();
        insertMenu.addItem("Table Of Contents", (com.google.gwt.user.client.Command) null);
        insertMenu.addItem("Information Box", (com.google.gwt.user.client.Command) null);
        insertMenu.addItem("More...", (com.google.gwt.user.client.Command) null);

        editMenu = new MenuBar(true);
        editMenu.addItem("Edit Macro Properties", (com.google.gwt.user.client.Command) null);
        editMenu.addItem("Delete Macro", (com.google.gwt.user.client.Command) null);

        macroMenu = new MenuItem("Macro", insertMenu);
        menuExtension.addFeature(MacroPluginFactory.getInstance().getPluginName(), macroMenu);

        // getUIExtensionList().add(menuExtension);
        // getUIExtensionList().add(toolBarExtension);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        insertButton.removeFromParent();
        insertButton.removeClickListener(this);
        insertButton = null;

        selector.destroy();
        selector = null;

        displayer.destroy();
        displayer = null;

        insertMenu.clearItems();
        insertMenu = null;

        editMenu.clearItems();
        editMenu = null;

        macroMenu.getParentMenu().removeItem(macroMenu);
        macroMenu = null;

        toolBarExtension.clearFeatures();
        menuExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == insertButton) {
            onMacro();
        }
    }

    /**
     * Inserts the macro selected through a dialog in place of the current selection or at the current caret position.
     */
    public void onMacro()
    {
        // TODO
        // The following is just a proof of concept.
        if (macroMenu.getSubMenu() == insertMenu) {
            macroMenu.setSubMenu(editMenu);
        } else {
            macroMenu.setSubMenu(insertMenu);
        }
    }
}
