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
package org.xwiki.gwt.wysiwyg.client.plugin.internal;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.gwt.user.client.HandlerRegistrationCollection;
import org.xwiki.gwt.user.client.ShortcutKey;
import org.xwiki.gwt.user.client.ShortcutKey.ModifierKey;
import org.xwiki.gwt.user.client.ShortcutKeyCommand;
import org.xwiki.gwt.user.client.ShortcutKeyManager;
import org.xwiki.gwt.user.client.ui.MenuItem;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Helper class that should be extended in order to create custom {@link MenuItemUIExtension}s.
 * 
 * @version $Id$
 */
public class MenuItemUIExtensionAdaptor extends MenuItemUIExtension implements AttachEvent.Handler
{
    /**
     * The object used to manage the shortcut keys.
     */
    private ShortcutKeyManager shortcutKeyManager;

    /**
     * The collections of handler registrations.
     */
    private final HandlerRegistrationCollection handlerRegistrations = new HandlerRegistrationCollection();

    /**
     * Creates a new user interface extension that will extend the specified extension point, a menu, with menu items.
     * 
     * @param role the role of the newly created user interface extension.
     */
    public MenuItemUIExtensionAdaptor(String role)
    {
        super(role);
    }

    /**
     * @return the {@link ShortcutKeyManager} used by this extension
     */
    public ShortcutKeyManager getShortcutKeyManager()
    {
        return shortcutKeyManager;
    }

    /**
     * Sets the {@link ShortcutKeyManager} to be used by this extension.
     * 
     * @param shortcutKeyManager the object that can be used to manage the shortcut keys
     */
    public void setShortcutKeyManager(ShortcutKeyManager shortcutKeyManager)
    {
        this.shortcutKeyManager = shortcutKeyManager;
    }

    /**
     * @param label the label of the menu item
     * @param icon the icon of the menu item
     * @return a new menu item
     */
    protected MenuItem createMenuItem(String label, ImageResource icon)
    {
        return createMenuItem(label, icon, null);
    }

    /**
     * @param label the label of the menu item
     * @param icon the icon of the menu item
     * @param command the command triggered by the returned menu item
     * @return a new menu item
     */
    protected MenuItem createMenuItem(String label, ImageResource icon, Command command)
    {
        return createMenuItem(label, null, icon, command, (char) 0);
    }

    /**
     * @param label the label of the menu item
     * @param shortcutKeyLabel the text used to display the shortcut key associated with the created menu item
     * @param icon the icon of the menu item
     * @param command the command triggered by the returned menu item
     * @param keyCode the shortcut key used to trigger the command associated with the returned menu item
     * @return a new menu item
     */
    protected MenuItem createMenuItem(String label, String shortcutKeyLabel, ImageResource icon,
        com.google.gwt.user.client.Command command, char keyCode)
    {
        com.google.gwt.user.client.Command actualCommand = command;
        if (keyCode > 0 && shortcutKeyManager != null) {
            ShortcutKeyCommand shortcutKeyCommand = new ShortcutKeyCommand(command);
            shortcutKeyManager.put(new ShortcutKey(keyCode, EnumSet.of(ModifierKey.CTRL, ModifierKey.SHIFT)),
                shortcutKeyCommand);
            actualCommand = shortcutKeyCommand;
        }
        MenuItem menuItem = new MenuItem(label, actualCommand);
        if (icon != null) {
            menuItem.setIcon(icon);
        }
        if (shortcutKeyLabel != null) {
            menuItem.setShortcutKeyLabel(shortcutKeyLabel);
        }
        return menuItem;
    }

    /**
     * Register attach handlers to update the state of the menu items when a menu is displayed.
     */
    public void registerAttachHandlers()
    {
        handlerRegistrations.removeHandlers();

        Set<MenuBar> menus = new HashSet<MenuBar>();
        String[] features = getFeatures();
        for (int i = 0; i < getFeatures().length; i++) {
            UIObject uiObject = getUIObject(features[i]);
            if (uiObject instanceof MenuItem) {
                MenuItem menuItem = (MenuItem) uiObject;
                if (menuItem.getParentMenu() != null) {
                    menus.add(menuItem.getParentMenu());
                }
            }
        }
        for (MenuBar menu : menus) {
            handlerRegistrations.add(menu.addAttachHandler(this));
        }
    }

    @Override
    public void clearFeatures()
    {
        super.clearFeatures();
        handlerRegistrations.removeHandlers();
    }

    @Override
    public void onAttachOrDetach(AttachEvent event)
    {
        if (event.isAttached()) {
            onAttach(event);
        }
    }

    /**
     * A menu bar has been attached.
     * 
     * @param event the event that was fired
     */
    protected void onAttach(AttachEvent event)
    {
        // Do nothing.
    }
}
