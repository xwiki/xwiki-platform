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

import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Tab to add to the link dialog to get user data and produce an URL to an email address.
 * 
 * @version $Id$
 */
public class LinkToEmailAddressTab extends AbstractExternalLinkTab
{
    /**
     * URL protocol constant.
     */
    private static final String MAILTO = "mailto:";

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#buildUri()
     */
    protected String buildUri()
    {
        String emailAddress = getUriTextBox().getText().trim();
        // If url does not start with the desired protocol, add it
        if (!emailAddress.startsWith(MAILTO)) {
            emailAddress = MAILTO + emailAddress;
        }
        return emailAddress;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getCreateLinkButtonTooltip()
     */
    protected String getCreateLinkButtonTooltip()
    {
        return Strings.INSTANCE.linkToEmailAddressButtonTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getUriTextBoxTooltip()
     */
    protected String getUriTextBoxTooltip()
    {
        return Strings.INSTANCE.linkUriToEmailAddressTextBoxTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getLabelTextBoxTooltip()
     */
    protected String getLabelTextBoxTooltip()
    {
        return Strings.INSTANCE.linkEmailAddressLabelTextBoxTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getErrorMessage()
     */
    protected String getErrorMessage()
    {
        return Strings.INSTANCE.linkEmailAddressError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getInputDefaultText()
     */
    protected String getInputDefaultText()
    {
        return Strings.INSTANCE.linkEmailAddressTextBox();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getURILabel()
     */
    protected String getURILabel()
    {
        return Strings.INSTANCE.linkEmailLabel();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#setLinkConfig(LinkConfig)
     */
    public void setLinkConfig(LinkConfig config)
    {
        if (config.getType() == getLinkType()) {
            super.setLinkConfig(config);
            // strip protocol out of the external url
            if (config.getUrl() != null) {
                String url = config.getUrl();
                if (url.startsWith(MAILTO)) {
                    getUriTextBox().setText(url.substring(MAILTO.length()));
                }
            }
        } else {
            setLinkLabel(config);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getLinkType()
     */
    public LinkType getLinkType()
    {
        return LinkType.EMAIL;
    }
}
