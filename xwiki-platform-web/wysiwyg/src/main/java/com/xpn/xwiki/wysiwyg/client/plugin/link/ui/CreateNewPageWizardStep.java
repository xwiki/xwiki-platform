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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.EnumSet;

import org.xwiki.gwt.user.client.FocusCommand;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Wizard step to get the name of a new page from the user.
 * 
 * @version $Id$
 */
public class CreateNewPageWizardStep implements WizardStep, KeyPressHandler, SourcesNavigationEvents
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * Main panel of this wizard.
     */
    private final Panel mainPanel = new FlowPanel();

    /**
     * The text box to add the name of the new page.
     */
    private final TextBox pageNameTextBox = new TextBox();

    /**
     * The label to signal an error on the page name field.
     */
    private final Label pageNameErrorLabel = new Label();

    /**
     * Link data handled by this wizard step.
     */
    private LinkConfig linkData;

    /**
     * The resource edited by this wizard step, i.e. the wikipage being edited right now
     */
    private ResourceName editedResource;

    /**
     * Collection of {@link NavigationListener}s, to be notified by navigation events from this step. Used to handle
     * default buttons in this wizard step, by firing next event whenever enter key is hit in the step form.
     */
    private final NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * The service used to create a link to a wiki page.
     */
    private WikiServiceAsync wikiService;

    /**
     * Creates a new wizard step for the passed edited resource.
     * 
     * @param editedResource the resource being edited by this wizard step
     */
    public CreateNewPageWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;
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

        mainPanel.addStyleName("xLinkToNewPage");
        mainPanel.add(pageNameLabel);
        mainPanel.add(helpPageNameLabel);
        mainPanel.add(pageNameErrorLabel);
        pageNameTextBox.setTitle(Strings.INSTANCE.linkNewPageTextBoxTooltip());
        pageNameTextBox.addKeyPressHandler(this);
        mainPanel.add(pageNameTextBox);
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
    public String getDirectionName(NavigationDirection direction)
    {
        if (direction == NavigationDirection.NEXT) {
            return Strings.INSTANCE.select();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return linkData;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkCreateNewPageTitle();
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.NEXT, NavigationDirection.CANCEL, NavigationDirection.PREVIOUS);
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        linkData = (LinkConfig) data;
        hideError();
        cb.onSuccess(null);
        DeferredCommand.addCommand(new FocusCommand(pageNameTextBox));
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
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        hideError();
        // get the name of the page from the input
        String newPageName = pageNameTextBox.getText().trim();
        if (StringUtils.isEmpty(newPageName)) {
            displayError(Strings.INSTANCE.linkNewPageError());
            async.onSuccess(false);
            DeferredCommand.addCommand(new FocusCommand(pageNameTextBox));
        } else {
            // call the server to get the page URL and reference
            // FIXME: move the reference setting logic in a controller, along with the async fetching logic
            wikiService.getPageLink(linkData.getWiki(), linkData.getSpace(), newPageName, null, null,
                new AsyncCallback<LinkConfig>()
                {
                    public void onSuccess(LinkConfig result)
                    {
                        linkData.setUrl(result.getUrl());
                        // set relative reference
                        ResourceName ref = new ResourceName(result.getReference(), false);
                        linkData.setReference(ref.getRelativeTo(editedResource).toString());
                        async.onSuccess(true);
                    }

                    public void onFailure(Throwable caught)
                    {
                        async.onSuccess(false);
                        DeferredCommand.addCommand(new FocusCommand(pageNameTextBox));
                    }
                });
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

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to create links to wiki pages
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
