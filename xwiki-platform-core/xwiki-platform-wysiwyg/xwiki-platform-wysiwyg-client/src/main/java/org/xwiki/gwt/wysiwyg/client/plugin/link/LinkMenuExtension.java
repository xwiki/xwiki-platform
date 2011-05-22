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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtensionAdaptor;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

import com.google.gwt.event.logical.shared.AttachEvent;

/**
 * Provides user interface for manipulating links through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class LinkMenuExtension extends MenuItemUIExtensionAdaptor
{
    /**
     * The list of menu items available when there is no link selected.
     */
    private final List<MenuItem> insertItems;

    /**
     * The list of menu items available when there is a link selected.
     */
    private final List<MenuItem> editItems;

    /**
     * The link plug-in associated with this menu extension.
     */
    private final LinkPlugin plugin;

    /**
     * Builds the menu extension using the passed plugin.
     * 
     * @param plugin the plugin to use for the creation of this menu extension.
     */
    public LinkMenuExtension(final LinkPlugin plugin)
    {
        super("menu");

        this.plugin = plugin;

        MenuItem webPageLink =
            createMenuItem(Strings.INSTANCE.linkToWebPage(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.EXTERNAL);
                }
            });
        MenuItem emailLink =
            createMenuItem(Strings.INSTANCE.linkToEmail(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.EMAIL);
                }
            });
        MenuItem wikiPageLink =
            createMenuItem(Strings.INSTANCE.linkToWikiPage(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.WIKIPAGE);
                }
            });
        MenuItem attachmentLink =
            createMenuItem(Strings.INSTANCE.linkToAttachment(), null, new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.ATTACHMENT);
                }
            });
        MenuItem editLink = createMenuItem(Strings.INSTANCE.linkEdit(), null, new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onLinkEdit();
            }
        });
        MenuItem removeLink =
            createMenuItem(Strings.INSTANCE.unlink(), Images.INSTANCE.unlink(),
                new com.google.gwt.user.client.Command()
                {
                    public void execute()
                    {
                        plugin.onUnlink();
                    }
                });
        MenuItem linkMenu = createMenuItem(Strings.INSTANCE.link(), Images.INSTANCE.link());

        insertItems = Arrays.asList(wikiPageLink, attachmentLink, webPageLink, emailLink);
        editItems = Arrays.asList(editLink, removeLink);

        addFeature(LinkPluginFactory.getInstance().getPluginName(), linkMenu);
        addFeature("linkWikiPage", wikiPageLink);
        addFeature("linkAttachment", attachmentLink);
        addFeature("linkWebPage", webPageLink);
        addFeature("linkEmail", emailLink);
        addFeature("linkEdit", editLink);
        addFeature("linkRemove", removeLink);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MenuItemUIExtensionAdaptor#onAttach(AttachEvent)
     */
    protected void onAttach(AttachEvent event)
    {
        boolean editMode = plugin.getTextArea().getCommandManager().isEnabled(Command.UNLINK);
        boolean canCreateLink = plugin.getTextArea().getCommandManager().isEnabled(Command.CREATE_LINK);
        for (MenuItem item : insertItems) {
            if (item.getParentMenu() == event.getSource()) {
                item.setEnabled(!editMode && canCreateLink);
                item.setVisible(!editMode);
            }
        }
        for (MenuItem item : editItems) {
            if (item.getParentMenu() == event.getSource()) {
                item.setEnabled(editMode);
                item.setVisible(editMode);
            }
        }
    }
}
