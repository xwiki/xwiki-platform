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
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Tab to add to the link dialog to get user data and produce a link to an external web page.
 * 
 * @version $Id$
 */
public class LinkToWebPageTab extends AbstractExternalLinkTab
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#buildUri()
     */
    protected String buildUri()
    {
        String webPageAddress = getUriTextBox().getText().trim();
        // If no protocol is specified add http by default
        if (!webPageAddress.contains("://")) {
            webPageAddress = "http://" + webPageAddress;
        }

        return webPageAddress;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getCreateLinkButtonTooltip()
     */
    protected String getCreateLinkButtonTooltip()
    {
        return Strings.INSTANCE.linkToWebPageButtonTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getUriTextBoxTooltip()
     */
    protected String getUriTextBoxTooltip()
    {
        return Strings.INSTANCE.linkUriToWebPageTextBoxTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getLabelTextBoxTooltip()
     */
    protected String getLabelTextBoxTooltip()
    {
        return Strings.INSTANCE.linkWebPageLabelTextBoxTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getErrorMessage()
     */
    protected String getErrorMessage()
    {
        return Strings.INSTANCE.linkWebPageAddressError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getInputDefaultText()
     */
    protected String getInputDefaultText()
    {
        return Strings.INSTANCE.linkWebPageTextBox();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getURILabel()
     */
    protected String getURILabel()
    {
        return Strings.INSTANCE.linkWebPageLabel();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkTab#getLinkType()
     */
    public LinkType getLinkType()
    {
        return LinkType.EXTERNAL;
    }
}
