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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a list of items allowing us to select one using the mouse or the keyboard.
 * 
 * @version $Id$
 */
public class ListBox extends Composite implements SourcesChangeEvents, ClickListener, KeyboardListener
{
    /**
     * The list of items from which we can choose one.
     */
    private final FlowPanel list = new FlowPanel();

    /**
     * The list of registered change listeners.
     */
    private final ChangeListenerCollection changeListeners = new ChangeListenerCollection();

    /**
     * The currently selected item; {@code null} if no item is selected.
     */
    private ListItem selectedItem;

    /**
     * Flag used to avoid updating the selected item on both KeyDown and KeyPress events. This flag is needed because of
     * the inconsistencies between browsers regarding keyboard events. For instance IE doesn't generate the KeyPress
     * event for navigation (arrow) keys and generates multiple KeyDown events while a key is hold down. On the
     * contrary, FF generates the KeyPress event for navigation (arrow) keys and generates just one KeyDown event while
     * a key is hold down. FF generates multiple KeyPress events when a key is hold down.
     */
    private boolean ignoreNextKeyPress;

    /**
     * Creates a new list box.
     */
    public ListBox()
    {
        FocusPanel panel = new FocusPanel(list);
        panel.addClickListener(this);
        panel.addKeyboardListener(this);

        initWidget(panel);
        setStylePrimaryName("xListBox");
    }

    /**
     * Adds a new item to the list.
     * 
     * @param item the item to be added
     */
    public void addItem(ListItem item)
    {
        item.setSelected(false);
        list.add(item);
    }

    /**
     * Removes an item from this list.
     * 
     * @param item the list item to be removed
     */
    public void removeItem(ListItem item)
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

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#addChangeListener(ChangeListener)
     */
    public void addChangeListener(ChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#removeChangeListener(ChangeListener)
     */
    public void removeChangeListener(ChangeListener listener)
    {
        changeListeners.remove(listener);
    }

    /**
     * @return the list item currently selected
     */
    public ListItem getSelectedItem()
    {
        return selectedItem;
    }

    /**
     * Selects the specified list item.
     * 
     * @param item the list item to be selected
     */
    public void setSelectedItem(ListItem item)
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
            changeListeners.fireChange(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == getWidget()) {
            setSelectedItem(findItem(Event.getCurrentEvent().getTarget()));
        }
    }

    /**
     * Finds the list item containing the given DOM element.
     * 
     * @param target the DOM element to look for, usually the target of a DOM event
     * @return the list item if found, {@code null} otherwise
     */
    private ListItem findItem(Element target)
    {
        for (Widget item : list) {
            if (item.getElement().isOrHasChild(target)) {
                return (ListItem) item;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getWidget()) {
            ignoreNextKeyPress = true;
            Event event = Event.getCurrentEvent();
            if (updateSelectedItem(event.getKeyCode())) {
                event.preventDefault();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getWidget()) {
            if (!ignoreNextKeyPress) {
                Event event = Event.getCurrentEvent();
                if (updateSelectedItem(event.getKeyCode())) {
                    event.preventDefault();
                }
            }
            ignoreNextKeyPress = false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        ignoreNextKeyPress = false;
    }

    /**
     * Updates the selected item based on the key pressed.
     * 
     * @param keyCode the code of the pressed key
     * @return {@code true} if the selected item has been changed
     */
    protected boolean updateSelectedItem(int keyCode)
    {
        ListItem oldItem = selectedItem;
        switch (keyCode) {
            case KeyboardListener.KEY_UP:
                selectPreviousItem();
                break;
            case KeyboardListener.KEY_DOWN:
                selectNextItem();
                break;
            case KeyboardListener.KEY_HOME:
                selectFirstItem();
                break;
            case KeyboardListener.KEY_END:
                selectLastItem();
                break;
            default:
                // ignore
        }
        return oldItem != selectedItem;
    }

    /**
     * Selects the previous list item with respect to the currently selected item.
     */
    protected void selectPreviousItem()
    {
        int selectedIndex = list.getWidgetIndex(selectedItem);
        if (selectedIndex > 0) {
            setSelectedItem((ListItem) list.getWidget(selectedIndex - 1));
        }
    }

    /**
     * Selects the next list item with respect to the currently selected item.
     */
    protected void selectNextItem()
    {
        int selectedIndex = list.getWidgetIndex(selectedItem);
        if (selectedIndex >= 0 && selectedIndex < list.getWidgetCount() - 1) {
            setSelectedItem((ListItem) list.getWidget(selectedIndex + 1));
        }
    }

    /**
     * Selects the first list item if the list is not empty.
     */
    protected void selectFirstItem()
    {
        if (list.getWidgetCount() > 0) {
            setSelectedItem((ListItem) list.getWidget(0));
        }
    }

    /**
     * Selects the last list item if the list of not empty.
     */
    protected void selectLastItem()
    {
        if (list.getWidgetCount() > 0) {
            setSelectedItem((ListItem) list.getWidget(list.getWidgetCount() - 1));
        }
    }
}
