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
     * The menu item used to insert an attached image.
     */
    private MenuItem attachedImage;

    /**
     * The menu item used to insert an external image.
     */
    private MenuItem urlImage;

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

        attachedImage =
            createMenuItem(Strings.INSTANCE.imageInsertAttachedImage(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onAttachedImage();
                }
            });
        if (Boolean.valueOf(plugin.getConfig().getParameter("allowExternalImages", "true"))) {
            urlImage =
                createMenuItem(Strings.INSTANCE.imageInsertURLImage(), null, new com.google.gwt.user.client.Command()
                {
                    public void execute()
                    {
                        plugin.onURLImage();
                    }
                });
        }
        edit = createMenuItem(Strings.INSTANCE.imageEditImage(), null, new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onImageEdit();
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
        addFeature("imageInsertAttached", attachedImage);
        if (urlImage != null) {
            addFeature("imageInsertURL", urlImage);
        }
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
        if (attachedImage.getParentMenu() == event.getSource()) {
            attachedImage.setEnabled(!editMode
                && plugin.getTextArea().getCommandManager().isEnabled(Command.INSERT_IMAGE));
            attachedImage.setVisible(!editMode);
        }
        if (urlImage != null && urlImage.getParentMenu() == event.getSource()) {
            urlImage.setEnabled(!editMode && plugin.getTextArea().getCommandManager().isEnabled(Command.INSERT_IMAGE));
            urlImage.setVisible(!editMode);
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
