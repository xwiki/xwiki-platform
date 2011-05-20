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
package org.xwiki.gwt.wysiwyg.client.plugin.image.ui;

import java.util.EnumSet;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceParserWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Forwards the control to the next step based on the image type.
 * 
 * @version $Id$
 */
public class ImageDispatcherWizardStep extends ResourceReferenceParserWizardStep<ImageConfig>
{
    /**
     * {@code true} if users are allowed to select external images by specifying their URL, {@code false} otherwise.
     */
    private final boolean allowExternalImages;

    /**
     * Creates a new wizard step that forwards the control to the next step based on the image type.
     * 
     * @param allowExternalImages {@code true} if users are allowed to select external images by specifying their URL,
     *            {@code false} otherwise
     * @param wikiService the service used to parse the image reference
     */
    public ImageDispatcherWizardStep(boolean allowExternalImages, WikiServiceAsync wikiService)
    {
        super(wikiService);

        this.allowExternalImages = allowExternalImages;
        setValidDirections(EnumSet.of(NavigationDirection.NEXT));
    }

    /**
     * {@inheritDoc}
     * 
     * @see ResourceReferenceParserWizardStep#getNextStep()
     */
    public String getNextStep()
    {
        if (allowExternalImages && getData().getDestination().getType() != ResourceType.ATTACHMENT) {
            return ImageWizardStep.URL_IMAGE_SELECTOR.toString();
        } else {
            return ImageWizardStep.ATTACHED_IMAGE_SELECTOR.toString();
        }
    }
}
