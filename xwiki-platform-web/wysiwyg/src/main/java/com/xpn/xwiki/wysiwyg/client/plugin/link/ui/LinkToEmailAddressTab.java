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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkGenerator;

/**
 * Tab to add to the link dialog to get user data and produce an URL to an email address.
 * 
 * @version $Id$
 */
public class LinkToEmailAddressTab extends AbstractHasLinkTab implements ClickListener
{
    /**
     * URL protocol constant.
     */
    private static final String MAILTO = "mailto:";

    /**
     * The text box where the user will insert the address of the web page.
     */
    private final TextBox emailTextBox;

    /**
     * The link creation button.
     */
    private final Button createLinkButton;

    /**
     * Default constructor.
     */
    public LinkToEmailAddressTab()
    {
        Label emailLabel = new Label(Strings.INSTANCE.linkEmailLabel());
        Label labelLabel = new Label(Strings.INSTANCE.linkLabelLabel());
        createLinkButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        createLinkButton.addClickListener(this);

        EnterListener enterListener = new EnterListener(createLinkButton);
        emailTextBox = new TextBox();
        emailTextBox.setText(Strings.INSTANCE.linkEmailAddressTextBox());
        emailTextBox.addClickListener(this);
        emailTextBox.addKeyboardListener(enterListener);

        getLabelTextBox().addKeyboardListener(enterListener);

        FlowPanel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xLinkToUrl");
        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("label");
        labelPanel.add(labelLabel);
        labelPanel.add(getLabelTextBox());
        FlowPanel emailPanel = new FlowPanel();
        emailPanel.addStyleName("url");
        emailPanel.add(emailLabel);
        emailPanel.add(emailTextBox);
        
        mainPanel.add(labelPanel);
        mainPanel.add(emailPanel);
        mainPanel.add(createLinkButton);

        initWidget(mainPanel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == createLinkButton) {
            // try to create the link and close the dialog. Fail if the input does not validate
            String url;
            String emailAddress = emailTextBox.getText();
            if (!validateUserInput()) {
                url = null;
            } else {
                if (emailAddress.startsWith(MAILTO)) {
                    url = emailAddress;
                } else {
                    url = MAILTO + emailAddress;
                }
                setLink(LinkGenerator.getInstance().getExternalLink(getLinkLabel(), url));
                getClickListeners().fireClick(this);
            }
        }
        if (sender == emailTextBox) {
            emailTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#initialize()
     */
    public void initialize()
    {
        emailTextBox.setText(Strings.INSTANCE.linkEmailAddressTextBox());
        // If there is label to set, set focus in the label field
        if (getLabelTextBox().getText().trim().length() == 0) {
            getLabelTextBox().setFocus(true);
        } else {
            emailTextBox.setFocus(true);
        }
        emailTextBox.selectAll();
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#validateUserInput()
     */
    public boolean validateUserInput()
    {
        // Check the super class validation result
        if (!super.validateUserInput()) {
            return false;
        }
        // the email address must not be void. Check if this happens
        if (this.emailTextBox.getText().trim().length() == 0
            || this.emailTextBox.getText().equals(Strings.INSTANCE.linkEmailAddressTextBox())) {
            Window.alert(Strings.INSTANCE.linkEmailAddressError());
            return false;
        }
        return true;
    }
}
