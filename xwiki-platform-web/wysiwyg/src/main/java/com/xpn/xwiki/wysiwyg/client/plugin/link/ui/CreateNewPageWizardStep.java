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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to get the name of a new page from the user.
 * 
 * @version $Id$
 */
public class CreateNewPageWizardStep implements WizardStep
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xFieldError";

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

        pageNameErrorLabel.addStyleName("xPageNameError");
        pageNameErrorLabel.setVisible(false);

        mainPanel.addStyleName("xLinkToNewPage");
        mainPanel.add(pageNameLabel);
        mainPanel.add(helpPageNameLabel);
        mainPanel.add(pageNameErrorLabel);
        pageNameTextBox.setTitle(Strings.INSTANCE.linkNewPageTextBoxTooltip());
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
        } else {
            // call the server to get the page URL and reference
            // FIXME: move the reference setting logic in a controller, along with the async fetching logic
            WysiwygService.Singleton.getInstance().getPageLink(linkData.getWiki(), linkData.getSpace(), newPageName,
                null, null, new AsyncCallback<LinkConfig>()
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
}
