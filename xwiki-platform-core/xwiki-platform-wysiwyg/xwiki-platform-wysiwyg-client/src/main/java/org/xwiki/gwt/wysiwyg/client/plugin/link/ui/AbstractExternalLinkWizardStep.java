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
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
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

        display().removeStyleName(DEFAULT_STYLE_NAME);
        display().addStyleName("xLinkToUrl");

        FlowPanel urlPanel = new FlowPanel();
        urlPanel.addStyleName("url");
        urlPanel.add(urlLabel);
        urlPanel.add(helpUrlLabel);
        urlPanel.add(urlErrorLabel);
        urlPanel.add(urlTextBox);

        display().insert(urlPanel, 0);
    }

    @Override
    public void init(Object data, final AsyncCallback< ? > cb)
    {
        super.init(data, new AsyncCallback<Boolean>()
        {
            public void onSuccess(Boolean result)
            {
                setURL(getData().getData().getUrl());
                cb.onSuccess(null);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    @Override
    protected void setFocus()
    {
        // Focus the link label input if it failed the validation, otherwise focus the URL input.
        Focusable focusable =
            getLabelErrorLabel().isVisible() && !urlErrorLabel.isVisible() ? getLabelTextBox() : urlTextBox;
        Scheduler.get().scheduleDeferred(new FocusCommand(focusable));
    }

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

    @Override
    protected void saveForm(final AsyncCallback<Boolean> callback)
    {
        EntityReference destinationEntityReference = new URIReference(getURL()).getEntityReference();
        if (!destinationEntityReference.equals(getData().getDestination().getEntityReference())) {
            getData().getDestination().setEntityReference(destinationEntityReference);
            // Reset the link configuration.
            getData().getData().setReference(null);
            getData().getData().setUrl(null);
        }
        super.saveForm(callback);
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
     * Subclasses can overwrite this method to adjust the URL the user has set.
     * 
     * @return the value of the URL text box
     */
    protected String getURL()
    {
        return urlTextBox.getText().trim();
    }

    /**
     * Fills the URL text box with the given URL. Subclasses can overwrite this method to adjust the URL.
     * 
     * @param url the URL to fill the text box with
     */
    protected void setURL(String url)
    {
        urlTextBox.setText(url);
    }

    /**
     * @return the tooltip for URL text box.
     */
    protected String getURLTextBoxTooltip()
    {
        return "";
    }

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
