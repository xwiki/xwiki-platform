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

import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Wizard step to create a link to an email address (the mailto: protocol).
 * 
 * @version $Id$
 */
public class EmailAddressLinkWizardStep extends AbstractExternalLinkWizardStep
{
    /**
     * Creates a new wizard step for configuring a link to an email address.
     * 
     * @param wikiService the service to be used for parsing the image reference when the link label is an image
     */
    EmailAddressLinkWizardStep(WikiServiceAsync wikiService)
    {
        super(wikiService);
        setStepTitle(Strings.INSTANCE.linkToEmail());
    }

    @Override
    protected String getURLTextBoxTooltip()
    {
        return Strings.INSTANCE.linkURLToEmailAddressTextBoxTooltip();
    }

    @Override
    protected String getURLErrorMessage()
    {
        return Strings.INSTANCE.linkEmailAddressError();
    }

    @Override
    protected String getURLLabel()
    {
        return Strings.INSTANCE.linkEmailLabel();
    }

    @Override
    protected String getURLHelpLabel()
    {
        return Strings.INSTANCE.linkEmailHelpLabel();
    }

    @Override
    protected void setURL(String url)
    {
        super.setURL(url != null && url.startsWith("mailto:") ? url.substring(7) : url);
    }
}
