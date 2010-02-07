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
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

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
     * The main panel of this wizard step.
     */
    private VerticalResizePanel mainPanel = new VerticalResizePanel();

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
        Label helpLabel = new Label(getSelectHelpLabel());
        helpLabel.addStyleName("xHelpLabel");
        mainPanel.add(helpLabel);

        errorLabel.addStyleName("xErrorMsg");
        errorLabel.setVisible(false);
        mainPanel.add(errorLabel);

        list.addKeyUpHandler(this);
        list.addDoubleClickHandler(this);
        mainPanel.add(list);
        mainPanel.setExpandingWidget(list, false);
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
        list.clear();
        fetchData(new AsyncCallback<List<L>>()
        {
            public void onSuccess(List<L> result)
            {
                fillList(result);
                if (cb != null) {
                    cb.onSuccess(null);
                }
                // set this as active, by default setting focus on the list
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
     * Fills the preview list with image preview widgets.
     * 
     * @param itemsList the list of items to select from
     */
    protected void fillList(List<L> itemsList)
    {
        String oldSelection = getSelection();
        for (L item : itemsList) {
            ListItem<L> newItem = getListItem(item);
            list.addItem(newItem);
            // preserve selection
            if (matchesSelection(item, oldSelection)) {
                list.setSelectedItem(newItem);
            }
        }
        ListItem<L> newOptionListItem = getNewOptionListItem();
        if (newOptionListItem != null) {
            if (newOptionOnTop) {
                list.insertItem(newOptionListItem, 0);
            } else {
                list.addItem(newOptionListItem);
            }
            if (oldSelection == null) {
                list.setSelectedItem(newOptionListItem);
            }
        }
    }

    /**
     * @return the selection of this wizard step, as a string representation of the value selected in the list or from
     *         the other settings of the current wizard step. To be used in conjunction with
     *         {@link #matchesSelection(AbstractSelectorWizardStep, String)} to handle selection preserving upon update
     *         in this step's list.
     * @see #matchesSelection(AbstractSelectorWizardStep, String)
     */
    protected String getSelection()
    {
        // by default, toString(), handling nulls nicely
        return list.getSelectedItem().getData() + "";
    }

    /**
     * Compares the current item with the passed string representation of the wizard step's selection.
     * 
     * @param item the item to compare with the selection
     * @param selection the unique representation of the step's selection
     * @return {@code true} if the item matches the selection, {@code false} otherwise.
     * @see #getSelection()
     */
    protected boolean matchesSelection(L item, String selection)
    {
        // by default, equality between string representations, handling nulls nicely
        return (selection + "").equals(item + "");
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
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        hideError();
        // check selection
        if (getSelectedItem() == null) {
            displayError();
            async.onSuccess(false);
            // set focus on the errored field
            DeferredCommand.addCommand(new FocusCommand(list));
            return;
        }

        // all is fine
        saveSelectedValue();
        async.onSuccess(true);
    }

    /**
     * Saves the current selection in this panel in the data managed by this wizard step, if all validation goes well on
     * the sumbit time.
     * 
     * @see #onSubmit(AsyncCallback)
     */
    protected abstract void saveSelectedValue();

    /**
     * Displays the specified error message and error markers for this wizard step.
     */
    protected void displayError()
    {
        errorLabel.setVisible(true);
        list.addStyleName(FIELD_ERROR_STYLE);
        mainPanel.refreshHeights();
    }

    /**
     * Hides the error markers for this wizard step.
     */
    protected void hideError()
    {
        errorLabel.setVisible(false);
        list.removeStyleName(FIELD_ERROR_STYLE);
        mainPanel.refreshHeights();
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
     * @return the mainPanel of this wizard step, for the subclasses to append items or change its settings
     */
    protected VerticalResizePanel getMainPanel()
    {
        return mainPanel;
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
        DeferredCommand.addCommand(new FocusCommand(list));
    }

    /**
     * @return the errorLabel
     */
    protected Label getErrorLabel()
    {
        return errorLabel;
    }
}
