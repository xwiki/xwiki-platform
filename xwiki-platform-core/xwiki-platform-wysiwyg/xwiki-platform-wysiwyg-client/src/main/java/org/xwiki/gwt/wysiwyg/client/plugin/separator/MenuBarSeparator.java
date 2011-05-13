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
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.gwt.user.client.HandlerRegistrationCollection;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

/**
 * User interface extension that provides ways of separating menu items.
 * 
 * @version $Id$
 */
public class MenuBarSeparator extends AbstractSeparator implements AttachEvent.Handler
{
    /**
     * The list of features exposed by this extension.
     */
    private static final String[] FEATURES = new String[] {"|"};

    /**
     * The collections of handler registrations.
     */
    private final HandlerRegistrationCollection handlerRegistrations = new HandlerRegistrationCollection();

    /**
     * The list of menu separators that have been created by this extension.
     */
    private final List<MenuItemSeparator> separators = new ArrayList<MenuItemSeparator>();

    /**
     * Creates a new menu bar separator.
     */
    public MenuBarSeparator()
    {
        super("menu");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSeparator#getFeatures()
     */
    public String[] getFeatures()
    {
        return FEATURES;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSeparator#getUIObject(String)
     */
    public UIObject getUIObject(String feature)
    {
        if (FEATURES[0].equals(feature)) {
            MenuItemSeparator separator = new MenuItemSeparator();
            separators.add(separator);
            return separator;
        }
        return null;
    }

    /**
     * Register attach handlers to update the visibility of the menu separators when a menu is displayed.
     */
    public void registerAttachHandlers()
    {
        handlerRegistrations.removeHandlers();
        Set<MenuBar> menus = new HashSet<MenuBar>();
        for (MenuItemSeparator separator : separators) {
            if (separator.getParentMenu() != null) {
                menus.add(separator.getParentMenu());
            }
        }
        for (MenuBar menu : menus) {
            menu.addAttachHandler(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AttachEvent.Handler#onAttachOrDetach(AttachEvent)
     */
    public void onAttachOrDetach(AttachEvent event)
    {
        if (event.isAttached()) {
            // Update the separators after the menu items are updated.
            final MenuBar menu = (MenuBar) event.getSource();
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
                public void execute()
                {
                    updateSeparators(menu);
                }
            });
        }
    }

    /**
     * Update the visibility state of all the separators from the given menu bar.
     * 
     * @param menuBar the menu bar whose separators should be updated
     */
    private void updateSeparators(MenuBar menuBar)
    {
        boolean visible = false;
        MenuItemSeparator lastVisibleSeparator = null;
        // Make sure the menu doesn't start with a separator and remove consecutive separators.
        for (UIObject item : getAllItems(menuBar)) {
            if (item instanceof MenuItemSeparator) {
                MenuItemSeparator separator = ((MenuItemSeparator) item);
                separator.setVisible(visible);
                if (visible) {
                    lastVisibleSeparator = separator;
                }
                visible = false;
            } else {
                visible = visible || ((MenuItem) item).isVisible();
            }
        }
        // Make sure the menu doesn't end with a separator.
        if (lastVisibleSeparator != null) {
            lastVisibleSeparator.setVisible(visible);
        }
    }

    /**
     * HACK: We need this native method because we don't have access to the menu items.
     * 
     * @param menuBar a menu bar
     * @return the list of all the items on the menu bar, including separators
     */
    private native List<UIObject> getAllItems(MenuBar menuBar)
    /*-{
        return menuBar.@com.google.gwt.user.client.ui.MenuBar::allItems;
    }-*/;

    /**
     * Destroys this extension.
     */
    public void destroy()
    {
        handlerRegistrations.removeHandlers();
        separators.clear();
    }
}
