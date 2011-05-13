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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;

import com.google.gwt.event.logical.shared.AttachEvent;

/**
 * Provides user interface for manipulating images through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class ImageMenuExtension extends MenuItemUIExtensionAdaptor
{
    /**
     * The menu item used to insert an image.
     */
    private MenuItem insert;

    /**
     * The menu item used to edit the selected image.
     */
    private MenuItem edit;

    /**
     * The menu item used to remove the selected image.
     */
    private MenuItem remove;

    /**
     * The link plug-in associated with this menu extension.
     */
    private final ImagePlugin plugin;

    /**
     * Builds the menu extension using the passed plugin.
     * 
     * @param plugin the plugin to use for the creation of this menu extension.
     */
    public ImageMenuExtension(final ImagePlugin plugin)
    {
        super("menu");

        this.plugin = plugin;

        insert = createMenuItem(Strings.INSTANCE.imageInsertImage(), null, new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onImage();
            }
        });
        edit = createMenuItem(Strings.INSTANCE.imageEditImage(), null, new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onImage();
            }
        });
        remove = createMenuItem(Strings.INSTANCE.imageRemoveImage(), null, new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onImageRemove();
            }
        });
        MenuItem imageMenu = createMenuItem(Strings.INSTANCE.image(), Images.INSTANCE.image());

        addFeature(ImagePluginFactory.getInstance().getPluginName(), imageMenu);
        addFeature("imageInsert", insert);
        addFeature("imageEdit", edit);
        addFeature("imageRemove", remove);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuItemUIExtensionAdaptor#onAttach(AttachEvent)
     */
    protected void onAttach(AttachEvent event)
    {
        boolean editMode = plugin.getTextArea().getCommandManager().isExecuted(Command.INSERT_IMAGE);
        if (insert.getParentMenu() == event.getSource()) {
            insert.setEnabled(!editMode && plugin.getTextArea().getCommandManager().isEnabled(Command.INSERT_IMAGE));
            insert.setVisible(!editMode);
        }
        if (edit.getParentMenu() == event.getSource()) {
            edit.setEnabled(editMode);
            edit.setVisible(editMode);
        }
        if (remove.getParentMenu() == event.getSource()) {
            remove.setEnabled(editMode);
            remove.setVisible(editMode);
        }
    }
}
