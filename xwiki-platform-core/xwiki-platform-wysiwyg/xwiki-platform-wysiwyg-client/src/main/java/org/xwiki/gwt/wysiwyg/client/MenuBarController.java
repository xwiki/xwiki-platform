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
package org.xwiki.gwt.wysiwyg.client;

import java.util.List;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginManager;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

/**
 * {@link MenuBar} controller.
 * 
 * @version $Id$
 */
public class MenuBarController
{
    /**
     * The string used to identify the menu bar extension point.
     */
    public static final String MENU_ROLE = "menu";

    /**
     * The underlying menu bar that is managed by this object.
     */
    private final MenuBar menuBar;

    /**
     * The object used to parse the menu bar configuration.
     */
    private final MenuItemDescriptorJSONParser menuItemDescriptorJSONParser = new MenuItemDescriptorJSONParser();

    /**
     * Creates a new menu bar controller.
     * 
     * @param menuBar the menu bar to be managed
     */
    public MenuBarController(MenuBar menuBar)
    {
        this.menuBar = menuBar;
    }

    /**
     * Fills the menu bar with the features specified in the configuration.
     * 
     * @param config the configuration object
     * @param pluginManager the object used to access the menu bar {@link UIExtension}s
     */
    public void fill(Config config, PluginManager pluginManager)
    {
        fillMenuBar(menuBar, menuItemDescriptorJSONParser.parse(config.getParameter(MENU_ROLE, "[]")), pluginManager);
    }

    /**
     * Fills the given menu bar with items matching the given list of descriptors.
     * 
     * @param menuBar the menu bar to be filled
     * @param descriptors the list of menu item descriptors
     * @param pluginManager the object used to access the menu bar {@link UIExtension}s
     */
    private void fillMenuBar(MenuBar menuBar, List<MenuItemDescriptor> descriptors, PluginManager pluginManager)
    {
        for (MenuItemDescriptor descriptor : descriptors) {
            UIExtension uie = pluginManager.getUIExtension(MENU_ROLE, descriptor.getFeature());
            if (uie != null) {
                // We have to handle menu items and menu item separators differently because MenuItemSeparator doesn't
                // extends MenuItem.
                UIObject uiObject = uie.getUIObject(descriptor.getFeature());
                if (uiObject instanceof MenuItemSeparator) {
                    menuBar.addSeparator((MenuItemSeparator) uiObject);
                } else if (uiObject instanceof MenuItem) {
                    MenuItem menuItem = (MenuItem) uiObject;
                    if (!descriptor.getSubMenu().isEmpty()) {
                        menuItem.setSubMenu(new MenuBar(true));
                        fillMenuBar((MenuBar) menuItem.getSubMenu(), descriptor.getSubMenu(), pluginManager);
                    }
                    menuBar.addItem(menuItem);
                }
            }
        }
    }

    /**
     * Destroys this menu bar controller.
     */
    public void destroy()
    {
        // Do nothing.
    }
}
