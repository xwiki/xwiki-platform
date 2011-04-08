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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.DeferredUpdater;
import org.xwiki.gwt.user.client.Updatable;
import org.xwiki.gwt.user.client.ui.MenuBar;
import org.xwiki.gwt.user.client.ui.MenuItem;
import org.xwiki.gwt.user.client.ui.MenuListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MenuItemUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Provides user interface for manipulating links through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class LinkMenuExtension extends MenuItemUIExtension implements Updatable, MenuListener
{
    /**
     * Schedules menu updates and executes only the most recent one. We use the minimum delay because we want the menu
     * to be update as soon as possible.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this, 1);

    /**
     * The link plug-in associated with this menu extension.
     */
    private final LinkPlugin plugin;

    /**
     * The list of menu options used to create links.
     */
    private List<UIObject> createLinkMenus;

    /**
     * The list of menu options used to edit links or to remove them.
     */
    private List<UIObject> editLinkMenus;

    /**
     * The submenu holding the various link options.
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
    public LinkMenuExtension(final LinkPlugin plugin)
    {
        super("menu");
        this.plugin = plugin;

        MenuItem webPageLink = new MenuItem(Strings.INSTANCE.linkToWebPage(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onLinkInsert(LinkType.EXTERNAL);
            }
        });

        MenuItem emailLink = new MenuItem(Strings.INSTANCE.linkToEmail(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onLinkInsert(LinkType.EMAIL);
            }
        });

        MenuItem wikiPageLink =
            new MenuItem(Strings.INSTANCE.linkToWikiPage(), new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.WIKIPAGE);
                }
            });

        MenuItem attachmentLink =
            new MenuItem(Strings.INSTANCE.linkToAttachment(), new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onLinkInsert(LinkType.ATTACHMENT);
                }
            });

        createLinkMenus = new ArrayList<UIObject>();
        createLinkMenus.add(wikiPageLink);
        createLinkMenus.add(attachmentLink);
        createLinkMenus.add(new MenuItemSeparator());
        createLinkMenus.add(webPageLink);
        createLinkMenus.add(emailLink);

        MenuItem editLink = new MenuItem(Strings.INSTANCE.linkEdit(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onLinkEdit();
            }
        });

        MenuItem removeLink = new MenuItem(Strings.INSTANCE.unlink(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onUnlink();
            }
        });
        removeLink.setIcon(Images.INSTANCE.unlink());

        editLinkMenus = new ArrayList<UIObject>();
        editLinkMenus.add(editLink);
        editLinkMenus.add(removeLink);

        submenu = new MenuBar(true);
        submenu.setAnimationEnabled(false);
        submenu.addAll(createLinkMenus);

        menu = new MenuItem(Strings.INSTANCE.link(), submenu);
        menu.setIcon(Images.INSTANCE.link());
        menu.addMenuListener(this);

        addFeature(LinkPluginFactory.getInstance().getPluginName(), menu);
    }

    /**
     * Cleans up this menu extension on destroy.
     */
    public void destroy()
    {
        createLinkMenus.clear();
        createLinkMenus = null;

        editLinkMenus.clear();
        editLinkMenus = null;

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
     * @see MenuListener#onMenuItemSelected(MenuItem)
     */
    public void onMenuItemSelected(MenuItem menuItem)
    {
        // update the list of shown options
        updater.deferUpdate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#update()
     */
    public void update()
    {
        // test first if unlink is enabled (i.e. we are inside a link)
        if (plugin.getTextArea().getCommandManager().isEnabled(Command.UNLINK)) {
            // activate the edit submenu
            if (submenu.getItem(0) != editLinkMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(editLinkMenus);
            }
        } else {
            // the create links list must be setup, and disabled if create link is not possible
            if (submenu.getItem(0) != createLinkMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(createLinkMenus);
            }
            boolean canCreateLink = plugin.getTextArea().getCommandManager().isEnabled(Command.CREATE_LINK);
            // set enabling state of the menu items in the submenu
            for (UIObject m : createLinkMenus) {
                if (m instanceof MenuItem) {
                    ((MenuItem) m).setEnabled(canCreateLink);
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
