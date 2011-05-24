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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import java.util.EnumSet;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.plugin.alfresco.AlfrescoWizardStepProvider.AlfrescoWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceParserWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Parses the resource reference and forwards the control to the next step based on the {@link EntityConfig} type.
 * 
 * @version $Id$
 */
public class AlfrescoResourceReferenceParserWizardStep extends ResourceReferenceParserWizardStep<EntityConfig>
{
    /**
     * Creates a new instance.
     * 
     * @param wikiService the service used to parse the link reference
     */
    public AlfrescoResourceReferenceParserWizardStep(WikiServiceAsync wikiService)
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
        EntityConfig entityConfig = getData().getData();
        if (entityConfig instanceof LinkConfig) {
            return AlfrescoWizardStep.LINK_SELECTOR.toString();
        } else if (entityConfig instanceof ImageConfig) {
            return AlfrescoWizardStep.IMAGE_SELECTOR.toString();
        } else {
            return super.getNextStep();
        }
    }
}
