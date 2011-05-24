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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;

import com.google.gwt.event.logical.shared.AttachEvent;

/**
 * Extends the top-level menu of the WYSIWYG editor with entries for inserting Alfresco links and images.
 * 
 * @version $Id$
 */
public class AlfrescoMenuExtension extends MenuItemUIExtensionAdaptor
{
    /**
     * The menu item used to insert or edit a link to an Alfresco file.
     */
    private MenuItem linkMenuItem;

    /**
     * The menu item used to insert or edit a link to an Alfresco image.
     */
    private MenuItem imageMenuItem;

    /**
     * The Alfresco plug-in associated with this menu extension.
     */
    private final AlfrescoPlugin plugin;

    /**
     * Creates a new menu extension for the given Alfresco plug-in.
     * 
     * @param plugin a Alfresco plug-in instance
     */
    public AlfrescoMenuExtension(final AlfrescoPlugin plugin)
    {
        super("menu");

        this.plugin = plugin;

        linkMenuItem =
            createMenuItem(AlfrescoConstants.INSTANCE.insertLink(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.link();
                }
            });
        imageMenuItem =
            createMenuItem(AlfrescoConstants.INSTANCE.insertImage(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.image();
                }
            });

        addFeature("alfrescoLink", linkMenuItem);
        addFeature("alfrescoImage", imageMenuItem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuItemUIExtensionAdaptor#onAttach(AttachEvent)
     */
    protected void onAttach(AttachEvent event)
    {
        if (linkMenuItem.getParentMenu() == event.getSource()) {
            boolean editMode = plugin.getTextArea().getCommandManager().isEnabled(Command.UNLINK);
            linkMenuItem.setText(editMode ? AlfrescoConstants.INSTANCE.editLink() : AlfrescoConstants.INSTANCE
                .insertLink());
            linkMenuItem.setEnabled(plugin.getTextArea().getCommandManager().isEnabled(Command.CREATE_LINK));
        }
        if (imageMenuItem.getParentMenu() == event.getSource()) {
            boolean editMode = plugin.getTextArea().getCommandManager().isExecuted(Command.INSERT_IMAGE);
            imageMenuItem.setText(editMode ? AlfrescoConstants.INSTANCE.editImage() : AlfrescoConstants.INSTANCE
                .insertImage());
            imageMenuItem.setEnabled(plugin.getTextArea().getCommandManager().isEnabled(Command.INSERT_IMAGE));
        }
    }
}
