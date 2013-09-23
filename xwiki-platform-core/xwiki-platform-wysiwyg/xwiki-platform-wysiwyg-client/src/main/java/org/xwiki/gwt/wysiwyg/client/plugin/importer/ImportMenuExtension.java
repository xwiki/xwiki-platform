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

import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;

import com.google.gwt.user.client.Command;

/**
 * Provides access to various content importers through the top-level menu.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportMenuExtension extends MenuItemUIExtensionAdaptor
{
    /**
     * Creates a new import menu extension.
     * 
     * @param importPlugin import plugin instance.
     */
    public ImportMenuExtension(final ImportPlugin importPlugin)
    {
        super("menu");

        MenuItem importOfficeFile =
            createMenuItem(Strings.INSTANCE.importOfficeFileMenuItemCaption(),
                Images.INSTANCE.importOfficeFileMenuEntryIcon(), new Command()
                {
                    public void execute()
                    {
                        importPlugin.onImportOfficeFile();
                    }
                });
        MenuItem importMenu =
            createMenuItem(Strings.INSTANCE.importMenuEntryCaption(), Images.INSTANCE.importMenuEntryIcon());

        addFeature(ImportPluginFactory.getInstance().getPluginName(), importMenu);
        addFeature("importOffice", importOfficeFile);
    }
}
