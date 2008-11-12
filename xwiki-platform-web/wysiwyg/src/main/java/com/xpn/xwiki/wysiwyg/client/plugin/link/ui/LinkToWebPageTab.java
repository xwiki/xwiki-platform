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
 * Tab to add to the link dialog to get user data and produce a link to an external web page.
 * 
 * @version $Id$
 */
public class LinkToWebPageTab extends AbstractHasLinkTab implements ClickListener
{
    /**
     * The text box where the user will insert the address of the web page.
     */
    private final TextBox urlTextBox;

    /**
     * The link creation button.
     */
    private final Button createLinkButton;

    /**
     * Class constructor.
     */
    public LinkToWebPageTab()
    {
        Label urlLabel = new Label(Strings.INSTANCE.linkWebPageLabel());
        Label labelLabel = new Label(Strings.INSTANCE.linkLabelLabel());        
        createLinkButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        createLinkButton.addClickListener(this);

        EnterListener enterListener = new EnterListener(createLinkButton);
        urlTextBox = new TextBox();
        urlTextBox.setText(Strings.INSTANCE.linkWebPageTextBox());
        urlTextBox.addClickListener(this);
        urlTextBox.addKeyboardListener(enterListener);
        
        getLabelTextBox().addKeyboardListener(enterListener);

        FlowPanel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xLinkToUrl");
        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("label");
        labelPanel.add(labelLabel);
        labelPanel.add(getLabelTextBox());
        FlowPanel urlPanel = new FlowPanel();
        urlPanel.addStyleName("url");
        urlPanel.add(urlLabel);
        urlPanel.add(urlTextBox);
        
        mainPanel.add(labelPanel);
        mainPanel.add(urlPanel);
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
            String url;
            String webPageAddress = urlTextBox.getText();
            if (!validateUserInput()) {
                url = null;
            } else {
                // check if any protocol is use
                if (webPageAddress.contains("://")) {
                    url = webPageAddress;
                } else {
                    url = "http://" + webPageAddress;
                }
                setLink(LinkGenerator.getInstance().getExternalLink(getLinkLabel(), url));
                getClickListeners().fireClick(this);
            }
        }
        if (sender == urlTextBox) {
            urlTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#initialize()
     */
    public void initialize()
    {
        urlTextBox.setText(Strings.INSTANCE.linkWebPageTextBox());
        if (getLabelTextBox().getText().trim().length() == 0) {
            getLabelTextBox().setFocus(true);
        } else {
            urlTextBox.setFocus(true);
        }
        urlTextBox.selectAll();
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
        // The url inserted by the user must not be void. Check that
        if (this.urlTextBox.getText().trim().length() == 0
            || this.urlTextBox.getText().equals(Strings.INSTANCE.linkWebPageTextBox())) {
            Window.alert(Strings.INSTANCE.linkWebPageAddressError());
            return false;
        }
        return true;
    }
}
