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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import org.xwiki.gwt.user.client.ui.MenuBar;
import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtension;

import com.google.gwt.user.client.Command;

/**
 * Provides access to various content importers through the top-level wysiwyg menu.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportMenuExtension extends MenuItemUIExtension
{
    /**
     * The top level menu entry.
     */
    private MenuItem importMenuEntry;

    /**
     * The sub-menu that gets expanded once clicked on importMenuEntry.
     */
    private MenuBar importMenu;

    /**
     * Creates a new import menu extension.
     * 
     * @param importPlugin import plugin instance.
     */
    public ImportMenuExtension(final ImportPlugin importPlugin)
    {
        super("menu");

        // Office File Import.
        MenuItem importOfficeFileMenuItem =
            new MenuItem(Strings.INSTANCE.importOfficeFileMenuItemCaption(), new Command()
            {
                public void execute()
                {
                    importPlugin.onImportOfficeFile();
                }
            });
        importOfficeFileMenuItem.setIcon(Images.INSTANCE.importOfficeFileMenuEntryIcon());

        importMenu = new MenuBar(true);
        importMenu.setAnimationEnabled(false);
        importMenu.addItem(importOfficeFileMenuItem);

        importMenuEntry = new MenuItem(Strings.INSTANCE.importMenuEntryCaption(), importMenu);
        importMenuEntry.setIcon(Images.INSTANCE.importMenuEntryIcon());

        addFeature(ImportPluginFactory.getInstance().getPluginName(), importMenuEntry);
    }

    /**
     * Cleans up this menu extension on destroy.
     */
    public void destroy()
    {
        importMenu.clearItems();
        importMenu = null;

        importMenuEntry.getParentMenu().removeItem(importMenuEntry);
        importMenuEntry = null;

        this.clearFeatures();
    }
}
