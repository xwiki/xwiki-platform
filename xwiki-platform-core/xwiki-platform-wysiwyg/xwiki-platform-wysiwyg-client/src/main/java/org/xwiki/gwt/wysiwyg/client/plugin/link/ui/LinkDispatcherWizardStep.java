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

import java.util.EnumSet;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceParserWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Forwards the control to the next step based on the link type.
 * 
 * @version $Id$
 */
public class LinkDispatcherWizardStep extends ResourceReferenceParserWizardStep<LinkConfig>
{
    /**
     * Creates a new wizard step that forwards the control to the next step based on the link type.
     * 
     * @param wikiService the service used to parse the link reference
     */
    public LinkDispatcherWizardStep(WikiServiceAsync wikiService)
    {
        super(wikiService);

        setValidDirections(EnumSet.of(NavigationDirection.NEXT));
    }

    /**
     * {@inheritDoc}
     * 
     * @see ResourceReferenceParserWizardStep#getNextStep()
     */
    @Override
    public String getNextStep()
    {
        switch (getData().getData().getType()) {
            case WIKIPAGE:
            case NEW_WIKIPAGE:
                return LinkWizardStep.WIKI_PAGE.toString();
            case ATTACHMENT:
                return LinkWizardStep.ATTACHMENT.toString();
            case EMAIL:
                return LinkWizardStep.EMAIL.toString();
            case EXTERNAL:
            default:
                return LinkWizardStep.WEB_PAGE.toString();
        }
    }
}
