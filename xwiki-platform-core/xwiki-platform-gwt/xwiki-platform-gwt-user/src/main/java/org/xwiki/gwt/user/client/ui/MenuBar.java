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
package org.xwiki.gwt.user.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Improves the default menu bar widget provided by GWT.
 * 
 * @version $Id$
 */
public class MenuBar extends com.google.gwt.user.client.ui.MenuBar
{
    /**
     * An empty menu item list. This is temporary used till GWT issue 3884 gets fixed.
     * <p>
     * NOTE: We don't use {@code Collections.emptyList()} because apparently it doesn't return an {@link ArrayList} and
     * the {@code items} field in GWT's MenuBar implementation has this concrete type, instead of being a generic list
     * of menu items. If we don't use the same concrete type we get a {@link ClassCastException} when running in hosted
     * mode.
     * 
     * @see #onBrowserEvent(Event)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3884
     */
    private static final List<com.google.gwt.user.client.ui.MenuItem> EMPTY_LIST =
        new ArrayList<com.google.gwt.user.client.ui.MenuItem>();

    /**
     * Creates an empty horizontal menu bar.
     */
    public MenuBar()
    {
        super();
    }

    /**
     * Creates an empty menu bar.
     * 
     * @param vertical {@code true} to orient the menu bar vertically
     */
    public MenuBar(boolean vertical)
    {
        super(vertical);
    }

    /**
     * Adds all the {@link MenuItem}s and {@link MenuItemSeparator}s from the given list to this menu bar.
     * 
     * @param items the list of {@link MenuItem}s and {@link MenuItemSeparator}s to be added to this menu bar
     */
    public void addAll(List<UIObject> items)
    {
        for (UIObject uiObject : items) {
            if (uiObject instanceof MenuItem) {
                addItem((MenuItem) uiObject);
            } else if (uiObject instanceof MenuItemSeparator) {
                addSeparator((MenuItemSeparator) uiObject);
            }
        }
    }

    /**
     * @param index the index of the menu item to return
     * @return the menu item at the specified index
     */
    public MenuItem getItem(int index)
    {
        return (MenuItem) getItems().get(0);
    }

    /**
     * NOTE: This is a hack required because #parentMenu is private.
     * 
     * @return {@code true} if this menu bar has a parent menu, {@code false} otherwise
     */
    protected native boolean hasParentMenu()
    /*-{
        return this.@com.google.gwt.user.client.ui.MenuBar::parentMenu != null;
    }-*/;

    /**
     * Finds the menu item containing the given element, which is the target of a DOM event.<br/>
     * NOTE: This is a hack required because #findItem(Element) is private.
     * 
     * @param target a DOM element, usually the target of an event
     * @return the menu item containing the given element if found, {@code null} otherwise
     */
    protected native MenuItem xFindItem(Element target)
    /*-{
        return this.@com.google.gwt.user.client.ui.MenuBar::findItem(Lcom/google/gwt/user/client/Element;)(target);
    }-*/;

    /**
     * Called when a menu item is being hovered.<br/>
     * NOTE: This is a hack required because #itemOver(MenuItem, boolean) is package-protected.
     * 
     * @param item the menu item being hovered
     * @param focus {@code true} to focus the specified menu item, {@code false} otherwise
     */
    protected native void xItemOver(MenuItem item, boolean focus)
    /*-{
        this.@com.google.gwt.user.client.ui.MenuBar::itemOver(Lcom/google/gwt/user/client/ui/MenuItem;Z)(item, focus);
    }-*/;

    /**
     * Sets the list of menu items displayed by this menu bar.<br/>
     * NOTE: This is just a hack required to overcome the fact that the GWT menu bar steals the focus when hovered.
     * 
     * @param items the list of {@code MenuItem} objects to be placed on the menu bar
     * @see #onBrowserEvent(Event)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3884
     */
    protected native void setItems(List<com.google.gwt.user.client.ui.MenuItem> items)
    /*-{
        this.@com.google.gwt.user.client.ui.MenuBar::items = items;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.MenuBar#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        List<com.google.gwt.user.client.ui.MenuItem> items = null;
        if (!hasParentMenu() && (event.getTypeInt() == Event.ONMOUSEOVER || event.getTypeInt() == Event.ONMOUSEOUT)) {
            MenuItem item = xFindItem((Element) event.getEventTarget().cast());
            // If the menu bar has no parent (it's a top level menu) then the selected menu item remains selected even
            // after its command has been fired or its sub-menu has been closed. Although this is the correct behavior
            // it prevents us from being notified when the selected menu is re-selected (it's command is fired again or
            // it's sub-menu is opened again). See #selectItem(MenuItem) for the reason. To overcome this we listen to
            // MoveOver and re-select the selected menu item.
            // NOTE: This is just a hack required because GWT doesn't offer menu events.
            if (event.getTypeInt() == Event.ONMOUSEOVER && getSelectedItem() != null && item == getSelectedItem()) {
                // The mouse is again over the selected item. We have to select it again (even if it's already
                // marked as selected) just so that registered menu listeners are notified again of the
                // MenuItemSelected event.
                item.xSetSelectionStyle(true);
            }
            // The following is just a hack to overcome GWT issue 3884 (MenuBar steals focus when hovered)
            // http://code.google.com/p/google-web-toolkit/issues/detail?id=3884
            if (item != null) {
                // Don't focus the menu items on mouse over if they are on a top menu bar.
                xItemOver(event.getTypeInt() == Event.ONMOUSEOVER ? item : null, false);
            }
            // Make sure the base class doesn't handle this event.
            items = getItems();
            // findItem will return null if the list of menu items is empty.
            setItems(EMPTY_LIST);
        }

        super.onBrowserEvent(event);

        if (items != null) {
            // Restore the list of menu items.
            setItems(items);
        }
    }
}
