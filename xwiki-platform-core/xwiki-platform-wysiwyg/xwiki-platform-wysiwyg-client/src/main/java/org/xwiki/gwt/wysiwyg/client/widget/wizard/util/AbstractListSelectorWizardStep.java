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
package org.xwiki.gwt.wysiwyg.client.widget.wizard.util;

import java.util.List;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.ui.ListBox;
import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

/**
 * Generic wizard step for selecting from a list of items: it handles list creation and population along with item
 * selection on double click and enter. Subclasses should only handle the operations specific to retrieving the items to
 * put in the list and the UI of the list items.
 * 
 * @param <D> the type of data edited by this wizard step, i.e. expected on initialization and returned as result
 * @param <L> the type of data to add in the list handled by this wizard step
 * @version $Id$
 */
public abstract class AbstractListSelectorWizardStep<D, L> extends AbstractSelectorWizardStep<D> implements
    DoubleClickHandler, KeyUpHandler, SourcesNavigationEvents
{
    /**
     * The style for an field in error.
     */
    private static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The list of items to select from.
     */
    private ListBox<L> list = new ListBox<L>();

    /**
     * Label to display the selection error in this wizard step.
     */
    private final Label errorLabel = new Label(getSelectErrorMessage());

    /**
     * Specifies whether the new item option should be shown on top (by default) or at bottom of the list.
     */
    private boolean newOptionOnTop = true;

    /**
     * Navigation listeners to be notified by navigation events from this step. It generates navigation to the next step
     * when an item is double clicked in the list, or enter key is pressed on a selected item.
     */
    private final NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * Default constructor, creating the UI of this wizard step.
     */
    public AbstractListSelectorWizardStep()
    {
        super(new VerticalResizePanel());

        Label helpLabel = new Label(getSelectHelpLabel());
        helpLabel.addStyleName("xHelpLabel");
        display().add(helpLabel);

        errorLabel.addStyleName("xErrorMsg");
        errorLabel.setVisible(false);
        display().add(errorLabel);

        list.addKeyUpHandler(this);
        list.addDoubleClickHandler(this);
        display().add(list);
        display().setExpandingWidget(list, false);
    }

    /**
     * @return the help label for this selector step.
     */
    protected abstract String getSelectHelpLabel();

    /**
     * @return the error message to be displayed in case of an error in selection
     */
    protected abstract String getSelectErrorMessage();

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#init(Object, AsyncCallback)
     */
    public void init(final Object data, final AsyncCallback< ? > cb)
    {
        hideError();
        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                refreshList(cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Reloads the list of image previews in asynchronous manner.
     * 
     * @param cb the callback to handle server call
     */
    protected void refreshList(final AsyncCallback< ? > cb)
    {
        // Save the selection before clearing the list and restore it after the list is refilled.
        final L selectedData = list.getSelectedItem() == null ? null : list.getSelectedItem().getData();
        list.clear();
        fetchData(new AsyncCallback<List<L>>()
        {
            public void onSuccess(List<L> result)
            {
                ListItem<L> selectedItem = fillList(result, selectedData);
                if (cb != null) {
                    cb.onSuccess(null);
                }
                // Select the item after the step is displayed, to be able to scroll the item into view.
                list.setSelectedItem(selectedItem);
                // Set this as active, by default setting focus on the list.
                setActive();
            }

            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            }
        });
    }

    /**
     * Fetches the list of items to be displayed in this list from the server.
     * 
     * @param callback the callback to notify when the fetching is done.
     */
    protected abstract void fetchData(AsyncCallback<List<L>> callback);

    /**
     * Fills the list with the given data and returns the item that matches the specified item.
     * 
     * @param dataList the list of data to fill the list
     * @param selectedData the data to be matched
     * @return the list item that matches the given data
     */
    protected ListItem<L> fillList(List<L> dataList, L selectedData)
    {
        ListItem<L> selectedItem = null;
        int selectedPriority = 0;
        for (L data : dataList) {
            ListItem<L> item = getListItem(data);
            list.addItem(item);
            // Restore the selection.
            int priority = (isSelectedByDefault(data) ? 2 : 0) + (data.equals(selectedData) ? 1 : 0);
            if (priority > selectedPriority) {
                selectedPriority = priority;
                selectedItem = item;
            }
        }
        ListItem<L> newOptionListItem = getNewOptionListItem();
        if (newOptionListItem != null) {
            if (newOptionOnTop) {
                list.insertItem(newOptionListItem, 0);
            } else {
                list.addItem(newOptionListItem);
            }
            if (selectedItem == null) {
                selectedItem = newOptionListItem;
            }
        }
        return selectedItem;
    }

    /**
     * @param listItemData a list item data
     * @return {@code true} of the list item with the given data should be selected by default, {@code false} otherwise
     */
    protected boolean isSelectedByDefault(L listItemData)
    {
        return false;
    }

    /**
     * @param data the data to build a list item representation for.
     * @return a list item for the passed data.
     */
    protected abstract ListItem<L> getListItem(L data);

    /**
     * @return an list item representation for a new item to add in the managed list, or {@code null} if no such item
     *         should be appended to the list.
     */
    protected ListItem<L> getNewOptionListItem()
    {
        // no such item by default
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#onCancel()
     */
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        hideError();
        // check selection
        if (getSelectedItem() == null) {
            displayError();
            async.onSuccess(false);
            // set focus on the errored field
            Scheduler.get().scheduleDeferred(new FocusCommand(list));
            return;
        }

        // all is fine
        saveSelectedValue(async);
    }

    /**
     * Saves the current selection in this panel in the data managed by this wizard step, if all validation goes well on
     * the submit time.
     * 
     * @param async the object to be notified after the selected value is saved
     * @see #onSubmit(AsyncCallback)
     */
    protected abstract void saveSelectedValue(AsyncCallback<Boolean> async);

    /**
     * Displays the specified error message and error markers for this wizard step.
     */
    protected void displayError()
    {
        errorLabel.setVisible(true);
        list.addStyleName(FIELD_ERROR_STYLE);
        display().refreshHeights();
    }

    /**
     * Hides the error markers for this wizard step.
     */
    protected void hideError()
    {
        errorLabel.setVisible(false);
        list.removeStyleName(FIELD_ERROR_STYLE);
        display().refreshHeights();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DoubleClickHandler#onDoubleClick(DoubleClickEvent)
     */
    public void onDoubleClick(DoubleClickEvent event)
    {
        if (event.getSource() == list && list.getSelectedItem() != null) {
            listeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getSource() == list && event.getNativeKeyCode() == KeyCodes.KEY_ENTER
            && list.getSelectedItem() != null) {
            listeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addNavigationListener(NavigationListener listener)
    {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * @return the underlying managed list, to allow subclasses to add extra elements or to style
     */
    protected ListBox<L> getList()
    {
        return list;
    }

    /**
     * Helper method to get the selected item in the list managed by this wizard step.
     * 
     * @return the selected item in the list managed by this wizard step
     */
    protected ListItem<L> getSelectedItem()
    {
        return list.getSelectedItem();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorWizardStep#display()
     */
    @Override
    public VerticalResizePanel display()
    {
        return (VerticalResizePanel) super.display();
    }

    /**
     * @param newOptionOnTop set whether the option to add a new item to the list should be added at the top of the list
     *            or at its bottom.
     */
    public void setNewOptionOnTop(boolean newOptionOnTop)
    {
        this.newOptionOnTop = newOptionOnTop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActive()
    {
        // schedule focus set on the list
        Scheduler.get().scheduleDeferred(new FocusCommand(list));
    }

    /**
     * @return the errorLabel
     */
    protected Label getErrorLabel()
    {
        return errorLabel;
    }
}
