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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wizard step to collect the data about an external link (e.g. http: or mailto:). Extends the default link
 * configuration wizard step by adding the field to collect the external link URL.
 * 
 * @version $Id$
 */
public abstract class AbstractExternalLinkWizardStep extends LinkConfigWizardStep implements FocusListener
{
    /**
     * The text box to store the URI of the created link.
     */
    private TextBox urlTextBox = new TextBox();

    /**
     * The main panel of this wizard step.
     */
    private FlowPanel mainPanel;

    /**
     * Default constructor.
     */
    public AbstractExternalLinkWizardStep()
    {
        super();
        Label urlLabel = new Label(getURLLabel());

        urlTextBox.addFocusListener(this);
        urlTextBox.setTitle(getURLTextBoxTooltip());
        urlTextBox.addKeyboardListener(this);

        getLabelTextBox().setTitle(getLabelTextBoxTooltip());

        mainPanel = new FlowPanel();
        mainPanel.removeStyleName(DEFAULT_STYLE_NAME);
        mainPanel.addStyleName("xLinkToUrl");

        FlowPanel urlPanel = new FlowPanel();
        urlPanel.addStyleName("url");
        urlPanel.add(urlLabel);
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
                urlTextBox.setText(getLinkData().getUrl() == null ? getInputDefaultText() : getLinkData().getUrl());
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
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // call the super to process label
        super.onSubmit(new AsyncCallback<Boolean>()
        {
            public void onSuccess(Boolean result)
            {
                if (!result) {
                    async.onSuccess(false);
                } else {
                    // everything is fine and saved on superclass, validate and save this form
                    validateAndSaveData(async);
                }
            }

            public void onFailure(Throwable caught)
            {
                async.onFailure(caught);
            }
        });
    }

    /**
     * Validates the inputs of this form and saves the data inserted in the form of this wizard step. To be called at
     * {@link #onSubmit(AsyncCallback)} time, to handle the positive response from the superclass' {@code onSubmit}.
     * 
     * @param async the callback used to pass asynchronously the result of this validation.
     */
    protected void validateAndSaveData(AsyncCallback<Boolean> async)
    {
        // validate this data
        if (urlTextBox.getText().trim().length() == 0 || urlTextBox.getText().equals(getInputDefaultText())) {
            Window.alert(getErrorMessage());
            async.onSuccess(false);
        } else {
            String linkUri = buildURL();
            getLinkData().setUrl(linkUri);
            getLinkData().setReference(linkUri);
            async.onSuccess(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        if (sender == urlTextBox && urlTextBox.getText().trim().equals(getInputDefaultText())) {
            urlTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        // ignore
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
     * @return the default input text for the text box holding the external url.
     */
    protected abstract String getInputDefaultText();

    /**
     * @return the error message to be displayed when the user uri is missing.
     */
    protected abstract String getErrorMessage();

    /**
     * Builds an URL to the external resource to be linked from the user input, adding protocols, parsing user input,
     * etc.
     * 
     * @return the URL to the external resource from the user input.
     */
    protected abstract String buildURL();

    /**
     * @return the tooltip for URL text box.
     */
    protected String getURLTextBoxTooltip()
    {
        return "";
    }
}
