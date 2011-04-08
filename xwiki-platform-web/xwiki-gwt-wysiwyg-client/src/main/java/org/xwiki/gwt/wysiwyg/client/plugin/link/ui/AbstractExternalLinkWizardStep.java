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
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.URIReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wizard step to collect the data about an external link (e.g. http: or mailto:). Extends the default link
 * configuration wizard step by adding the field to collect the external link URL.
 * 
 * @version $Id$
 */
public abstract class AbstractExternalLinkWizardStep extends LinkConfigWizardStep
{
    /**
     * The text box to store the URI of the created link.
     */
    private final TextBox urlTextBox = new TextBox();

    /**
     * The label to display the url label for the created link.
     */
    private final Label urlErrorLabel = new Label();

    /**
     * The main panel of this wizard step.
     */
    private FlowPanel mainPanel;

    /**
     * Creates a new wizard step for configuring links to external entities.
     * 
     * @param wikiService the service to be used for parsing the image reference when the link label is an image
     */
    public AbstractExternalLinkWizardStep(WikiServiceAsync wikiService)
    {
        super(wikiService);

        Panel urlLabel = new FlowPanel();
        urlLabel.setStyleName(INFO_LABEL_STYLE);
        urlLabel.add(new InlineLabel(getURLLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        urlLabel.add(mandatoryLabel);
        Label helpUrlLabel = new Label(getURLHelpLabel());
        helpUrlLabel.setStyleName(HELP_LABEL_STYLE);

        urlErrorLabel.addStyleName(ERROR_LABEL_STYLE);
        urlErrorLabel.setVisible(false);

        urlTextBox.setTitle(getURLTextBoxTooltip());
        urlTextBox.addKeyPressHandler(this);

        getLabelTextBox().setTitle(getLabelTextBoxTooltip());

        mainPanel = new FlowPanel();
        mainPanel.removeStyleName(DEFAULT_STYLE_NAME);
        mainPanel.addStyleName("xLinkToUrl");

        FlowPanel urlPanel = new FlowPanel();
        urlPanel.addStyleName("url");
        urlPanel.add(urlLabel);
        urlPanel.add(helpUrlLabel);
        urlPanel.add(urlErrorLabel);
        urlPanel.add(urlTextBox);

        mainPanel.add(urlPanel);
        mainPanel.add(getMainPanel());
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, final AsyncCallback< ? > cb)
    {
        super.init(data, new AsyncCallback<Boolean>()
        {
            public void onSuccess(Boolean result)
            {
                urlTextBox.setText(getData().getData().getUrl());
                cb.onSuccess(null);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setFocus()
    {
        // Focus the link label input if it failed the validation, otherwise focus the URL input.
        Focusable focusable =
            getLabelErrorLabel().isVisible() && !urlErrorLabel.isVisible() ? getLabelTextBox() : urlTextBox;
        Scheduler.get().scheduleDeferred(new FocusCommand(focusable));
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
    @Override
    protected boolean validateForm()
    {
        // validate everything: super first
        boolean result = super.validateForm();
        // then this form
        if (urlTextBox.getText().trim().length() == 0) {
            displayURLError(getURLErrorMessage());
            result = false;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see LinkConfigWizardStep#saveForm(AsyncCallback)
     */
    @Override
    protected void saveForm(final AsyncCallback<Boolean> callback)
    {
        final ResourceReference destination = getData().getDestination().clone();
        destination.setEntityReference(new URIReference(buildURL()).getEntityReference());
        getWikiService().getEntityConfig(getData().getOrigin(), destination, new AsyncCallback<EntityConfig>()
        {
            public void onFailure(Throwable caught)
            {
                callback.onFailure(caught);
            }

            public void onSuccess(EntityConfig result)
            {
                getData().setDestination(destination);
                getData().getData().setReference(result.getReference());
                getData().getData().setUrl(result.getUrl());
                AbstractExternalLinkWizardStep.super.saveForm(callback);
            }
        });
    }

    /**
     * @return the urlTextBox
     */
    public TextBox getUrlTextBox()
    {
        return urlTextBox;
    }

    /**
     * @return the label text for the particular external resource link to be created.
     */
    protected abstract String getURLLabel();

    /**
     * @return the label text for the help label for the url of the external link to be created.
     */
    protected abstract String getURLHelpLabel();

    /**
     * @return the error message to be displayed when the user uri is missing.
     */
    protected abstract String getURLErrorMessage();

    /**
     * Builds an URL to the external resource to be linked from the user input, adding protocols, parsing user input,
     * etc.
     * 
     * @return the URL to the external resource from the user input.
     */
    protected String buildURL()
    {
        return urlTextBox.getText().trim();
    }

    /**
     * @return the tooltip for URL text box.
     */
    protected String getURLTextBoxTooltip()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hideErrors()
    {
        super.hideErrors();
        // hide this dialog's specific errors
        urlErrorLabel.setVisible(false);
        urlTextBox.removeStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * Displays the URL error message and markers.
     * 
     * @param errorMessage the error message to display
     */
    protected void displayURLError(String errorMessage)
    {
        urlErrorLabel.setText(errorMessage);
        urlErrorLabel.setVisible(true);
        urlTextBox.addStyleName(FIELD_ERROR_STYLE);
    }
}
