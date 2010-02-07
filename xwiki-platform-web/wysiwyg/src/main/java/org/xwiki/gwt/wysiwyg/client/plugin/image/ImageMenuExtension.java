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

import com.google.gwt.user.client.ui.UIObject;

/**
 * Provides user interface for manipulating images through the WYSIWYG top-level menu.
 * 
 * @version $Id$
 */
public class ImageMenuExtension extends MenuItemUIExtension implements Updatable, MenuListener
{
    /**
     * Schedules menu updates and executes only the most recent one. We use the minimum delay because we want the menu
     * to be update as soon as possible.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this, 1);

    /**
     * The link plug-in associated with this menu extension.
     */
    private final ImagePlugin plugin;

    /**
     * The list of menu options used to create links.
     */
    private List<UIObject> createImageMenus;

    /**
     * The list of menu options used to edit links or to remove them.
     */
    private List<UIObject> editImageMenus;

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
    public ImageMenuExtension(final ImagePlugin plugin)
    {
        super("menu");
        this.plugin = plugin;

        MenuItem insertImage =
            new MenuItem(Strings.INSTANCE.imageInsertImage(), new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onImage();
                }
            });

        createImageMenus = new ArrayList<UIObject>();
        createImageMenus.add(insertImage);

        MenuItem editImage = new MenuItem(Strings.INSTANCE.imageEditImage(), new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                plugin.onImage();
            }
        });

        MenuItem removeImage =
            new MenuItem(Strings.INSTANCE.imageRemoveImage(), new com.google.gwt.user.client.Command()
            {
                public void execute()
                {
                    plugin.onImageRemove();
                }
            });

        editImageMenus = new ArrayList<UIObject>();
        editImageMenus.add(editImage);
        editImageMenus.add(removeImage);

        submenu = new MenuBar(true);
        submenu.setAnimationEnabled(false);
        submenu.addAll(createImageMenus);

        menu = new MenuItem(Strings.INSTANCE.image(), submenu);
        menu.setIcon(Images.INSTANCE.image());
        menu.addMenuListener(this);

        addFeature(ImagePluginFactory.getInstance().getPluginName(), menu);
    }

    /**
     * Cleans up this menu extension on destroy.
     */
    public void destroy()
    {
        createImageMenus.clear();
        createImageMenus = null;

        editImageMenus.clear();
        editImageMenus = null;

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
        // test if the image command is executed
        if (plugin.getTextArea().getCommandManager().isExecuted(Command.INSERT_IMAGE)) {
            // activate the edit submenu
            if (submenu.getItem(0) != editImageMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(editImageMenus);
            }
        } else {
            // the create images list must be setup, and disabled if create image is not possible
            if (submenu.getItem(0) != createImageMenus.get(0)) {
                submenu.clearItems();
                submenu.addAll(createImageMenus);
            }
            boolean canCreateImage = plugin.getTextArea().getCommandManager().isEnabled(Command.INSERT_IMAGE);
            // set enabling state of the menu items in the submenu
            for (UIObject m : createImageMenus) {
                if (m instanceof MenuItem) {
                    ((MenuItem) m).setEnabled(canCreateImage);
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
