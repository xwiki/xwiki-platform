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
package com.xpn.xwiki.wysiwyg.client.widget;

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
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.MenuBar#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (!hasParentMenu() && getSelectedItem() != null && event.getTypeInt() == Event.ONMOUSEOVER) {
            // If the menu bar has no parent (it's a top level menu) then the selected menu item remains selected even
            // after its command has been fired or its sub-menu has been closed. Although this is the correct behavior
            // it prevents us from being notified when the selected menu is re-selected (it's command is fired again or
            // it's sub-menu is opened again). See #selectItem(MenuItem) for the reason. To overcome this we listen to
            // MoveOver and re-select the selected menu item.
            // NOTE: This is just a hack required because GWT doesn't offer menu events.
            MenuItem item = xFindItem((Element) event.getTarget());
            if (item == getSelectedItem()) {
                // The mouse is again over the selected item. We have to select it again (even if it's already marked as
                // selected) just so that registered menu listeners are notified again of the MenuItemSelected event.
                item.xSetSelectionStyle(true);
            }
        }
        super.onBrowserEvent(event);
    }
}
