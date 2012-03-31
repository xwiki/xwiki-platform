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

import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.user.client.KeyboardAdaptor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Focusable;

/**
 * Displays a list of items allowing us to select one using the mouse or the keyboard.
 * 
 * @param <T> the data type that can be attached to list items
 * @version $Id$
 */
public class ListBox<T> extends Composite implements HasSelectionHandlers<ListItem<T>>, HasDoubleClickHandlers,
    HasAllKeyHandlers, ClickHandler, Focusable
{
    /**
     * The list of items from which we can choose one.
     */
    private final FlowPanel list = new FlowPanel();

    /**
     * The object used to handle keyboard events.
     */
    private final KeyboardAdaptor keyboardAdaptor = new KeyboardAdaptor()
    {
        protected void handleRepeatableKey(Event event)
        {
            updateSelectedItem(event);
        }
    };

    /**
     * The currently selected item; {@code null} if no item is selected.
     */
    private ListItem<T> selectedItem;

    /**
     * Creates a new list box.
     */
    public ListBox()
    {
        FocusPanel panel = new FocusPanel(list);
        panel.addClickHandler(this);
        panel.addKeyDownHandler(keyboardAdaptor);
        panel.addKeyPressHandler(keyboardAdaptor);
        panel.addKeyUpHandler(keyboardAdaptor);

        initWidget(panel);
        setStylePrimaryName("xListBox");
    }

    /**
     * Adds a new item to the list.
     * 
     * @param item the item to be added
     */
    public void addItem(ListItem<T> item)
    {
        item.setSelected(false);
        list.add(item);
    }

    /**
     * @param index a valid list item index
     * @return the list item at the specified index in this list
     */
    @SuppressWarnings("unchecked")
    public ListItem<T> getItem(int index)
    {
        return (ListItem<T>) list.getWidget(index);
    }

    /**
     * Inserts an item before the specified position.
     * 
     * @param item the item to be inserted
     * @param beforeIndex the index before which to insert the item
     */
    public void insertItem(ListItem<T> item, int beforeIndex)
    {
        item.setSelected(false);
        list.insert(item, beforeIndex);
    }

    /**
     * Removes an item from this list.
     * 
     * @param item the list item to be removed
     */
    public void removeItem(ListItem<T> item)
    {
        list.remove(item);
        if (item == selectedItem) {
            setSelectedItem(null);
        }
    }

    /**
     * Removes all the items from this list.
     */
    public void clear()
    {
        list.clear();
        setSelectedItem(null);
    }

    @Override
    public HandlerRegistration addSelectionHandler(SelectionHandler<ListItem<T>> handler)
    {
        return addHandler(handler, SelectionEvent.getType());
    }

    @Override
    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler)
    {
        return addDomHandler(handler, DoubleClickEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
    {
        return addDomHandler(handler, KeyDownEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
    {
        return addDomHandler(handler, KeyPressEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
    {
        return addDomHandler(handler, KeyUpEvent.getType());
    }

    /**
     * @return the list item currently selected
     */
    public ListItem<T> getSelectedItem()
    {
        return selectedItem;
    }

    /**
     * Selects the specified list item.
     * 
     * @param item the list item to be selected
     */
    public void setSelectedItem(ListItem<T> item)
    {
        if (item != selectedItem && (item == null || item.getParent() == list)) {
            if (selectedItem != null) {
                selectedItem.setSelected(false);
            }
            selectedItem = item;
            if (selectedItem != null) {
                selectedItem.setSelected(true);
                selectedItem.getElement().scrollIntoView();
            }
            SelectionEvent.fire(this, selectedItem);
        }
    }

    @Override
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == getWidget()) {
            setSelectedItem(getItemForEvent(event));
        }
    }

    /**
     * Finds the list item that is the target of the specified DOM event.
     * 
     * @param event the DOM event that was fired
     * @return the target list item if found, {@code null} otherwise
     */
    public ListItem<T> getItemForEvent(DomEvent< ? > event)
    {
        Element target = Element.as(event.getNativeEvent().getEventTarget());
        for (int i = 0; i < list.getWidgetCount(); i++) {
            ListItem<T> item = getItem(i);
            if (item.getElement().isOrHasChild(target)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Updates the selected item based on the native keyboard event that was fired.
     * 
     * @param event the native event that was fired
     */
    protected void updateSelectedItem(Event event)
    {
        ListItem<T> oldItem = selectedItem;
        switch (event.getKeyCode()) {
            case KeyCodes.KEY_UP:
                if (getSelectedItem() == null) {
                    // key up enters list through the last item
                    selectLastItem();
                } else {
                    selectPreviousItem();
                }
                break;
            case KeyCodes.KEY_DOWN:
                if (getSelectedItem() == null) {
                    // key down enters list through the first item
                    selectFirstItem();
                } else {
                    selectNextItem();
                }
                break;
            case KeyCodes.KEY_HOME:
                selectFirstItem();
                break;
            case KeyCodes.KEY_END:
                selectLastItem();
                break;
            default:
                // ignore
        }
        if (oldItem != selectedItem) {
            // We have to prevent the default browser behavior which scrolls the list.
            event.xPreventDefault();
        }
    }

    /**
     * Selects the previous list item with respect to the currently selected item.
     */
    protected void selectPreviousItem()
    {
        int selectedIndex = list.getWidgetIndex(selectedItem);
        if (selectedIndex > 0) {
            setSelectedItem(getItem(selectedIndex - 1));
        }
    }

    /**
     * Selects the next list item with respect to the currently selected item.
     */
    protected void selectNextItem()
    {
        int selectedIndex = list.getWidgetIndex(selectedItem);
        if (selectedIndex >= 0 && selectedIndex < list.getWidgetCount() - 1) {
            setSelectedItem(getItem(selectedIndex + 1));
        }
    }

    /**
     * Selects the first list item if the list is not empty.
     */
    protected void selectFirstItem()
    {
        if (list.getWidgetCount() > 0) {
            setSelectedItem(getItem(0));
        }
    }

    /**
     * Selects the last list item if the list of not empty.
     */
    protected void selectLastItem()
    {
        if (list.getWidgetCount() > 0) {
            setSelectedItem(getItem(list.getWidgetCount() - 1));
        }
    }

    /**
     * @return the number of list items in this list box
     */
    public int getItemCount()
    {
        return list.getWidgetCount();
    }

    @Override
    public int getTabIndex()
    {
        return ((Focusable) getWidget()).getTabIndex();
    }

    @Override
    public void setTabIndex(int index)
    {
        ((Focusable) getWidget()).setTabIndex(index);
    }

    @Override
    public void setFocus(boolean focused)
    {
        ((Focusable) getWidget()).setFocus(focused);
    }

    @Override
    public void setAccessKey(char key)
    {
        ((Focusable) getWidget()).setAccessKey(key);
    }
}
