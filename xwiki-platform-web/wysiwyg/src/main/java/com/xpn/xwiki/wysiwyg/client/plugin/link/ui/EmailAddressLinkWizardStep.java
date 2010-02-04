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

import com.xpn.xwiki.wysiwyg.client.Strings;

/**
 * Wizard step to create a link to an email address (the mailto: protocol).
 * 
 * @version $Id$
 */
public class EmailAddressLinkWizardStep extends AbstractExternalLinkWizardStep
{
    /**
     * URL protocol constant.
     */
    private static final String MAILTO = "mailto:";

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkWizardStep#buildURL()
     */
    protected String buildURL()
    {
        String emailAddress = getUrlTextBox().getText().trim();
        // If url does not start with the desired protocol, add it
        if (!emailAddress.startsWith(MAILTO)) {
            emailAddress = MAILTO + emailAddress;
        }
        return emailAddress;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkWizardStep#getURLTextBoxTooltip()
     */
    protected String getURLTextBoxTooltip()
    {
        return Strings.INSTANCE.linkURLToEmailAddressTextBoxTooltip();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkWizardStep#getURLErrorMessage()
     */
    protected String getURLErrorMessage()
    {
        return Strings.INSTANCE.linkEmailAddressError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExternalLinkWizardStep#getURLLabel()
     */
    protected String getURLLabel()
    {
        return Strings.INSTANCE.linkEmailLabel();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String getURLHelpLabel()
    {
        return Strings.INSTANCE.linkEmailHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkToEmail();
    }
}