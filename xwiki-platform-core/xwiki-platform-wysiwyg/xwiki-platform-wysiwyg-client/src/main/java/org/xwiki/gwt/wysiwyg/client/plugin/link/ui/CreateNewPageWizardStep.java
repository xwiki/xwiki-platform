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
package org.xwiki.gwt.wysiwyg.client.plugin.link.ui;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Wizard step to get the name of a new page from the user.
 * 
 * @version $Id$
 */
public class CreateNewPageWizardStep extends AbstractInteractiveWizardStep implements KeyPressHandler,
    SourcesNavigationEvents
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The text box to add the name of the new page.
     */
    private final TextBox pageNameTextBox = new TextBox();

    /**
     * The label to signal an error on the page name field.
     */
    private final Label pageNameErrorLabel = new Label();

    /**
     * The entity link handled by this wizard step.
     */
    private EntityLink<LinkConfig> entityLink;

    /**
     * Collection of {@link NavigationListener}s, to be notified by navigation events from this step. Used to handle
     * default buttons in this wizard step, by firing next event whenever enter key is hit in the step form.
     */
    private final NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * Creates a new wizard step that allows the user to create a link to a new page.
     */
    public CreateNewPageWizardStep()
    {
        setStepTitle(Strings.INSTANCE.linkCreateNewPageTitle());

        Panel pageNameLabel = new FlowPanel();
        pageNameLabel.setStyleName("xInfoLabel");
        pageNameLabel.add(new InlineLabel(Strings.INSTANCE.linkNewPageLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        pageNameLabel.add(mandatoryLabel);
        Label helpPageNameLabel = new Label(Strings.INSTANCE.linkNewPageTextBoxTooltip());
        helpPageNameLabel.setStyleName("xHelpLabel");

        pageNameErrorLabel.addStyleName("xErrorMsg");
        pageNameErrorLabel.setVisible(false);

        display().addStyleName("xLinkToNewPage");
        display().add(pageNameLabel);
        display().add(helpPageNameLabel);
        display().add(pageNameErrorLabel);
        pageNameTextBox.setTitle(Strings.INSTANCE.linkNewPageTextBoxTooltip());
        pageNameTextBox.addKeyPressHandler(this);
        display().add(pageNameTextBox);
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return entityLink;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, AsyncCallback< ? > cb)
    {
        entityLink = (EntityLink<LinkConfig>) data;
        hideError();
        cb.onSuccess(null);
        Scheduler.get().scheduleDeferred(new FocusCommand(pageNameTextBox));
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
    public void onSubmit(final AsyncCallback<Boolean> callback)
    {
        hideError();

        String newPageName = pageNameTextBox.getText().trim();
        if (StringUtils.isEmpty(newPageName)) {
            displayError(Strings.INSTANCE.linkNewPageError());
            callback.onSuccess(false);
            Scheduler.get().scheduleDeferred(new FocusCommand(pageNameTextBox));
        } else {
            (new WikiPageReference(entityLink.getDestination().getEntityReference())).setPageName(newPageName);
            // Reset the link configuration.
            entityLink.getData().setReference(null);
            entityLink.getData().setUrl(null);
            callback.onSuccess(true);
        }
    }

    /**
     * Displays the error message and markers for this dialog.
     * 
     * @param errorMessage the error message to display
     */
    private void displayError(String errorMessage)
    {
        pageNameErrorLabel.setText(errorMessage);
        pageNameErrorLabel.setVisible(true);
        pageNameTextBox.addStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * Hides the error message and markers for this dialog.
     */
    private void hideError()
    {
        pageNameErrorLabel.setVisible(false);
        pageNameTextBox.removeStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyPressHandler#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
            // fire the event for the default direction
            listeners.fireNavigationEvent(getDefaultDirection());
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
     * @return the default navigation direction, to be fired automatically when enter is hit in an input in the form of
     *         this configuration wizard step.
     */
    public NavigationDirection getDefaultDirection()
    {
        return NavigationDirection.NEXT;
    }
}
