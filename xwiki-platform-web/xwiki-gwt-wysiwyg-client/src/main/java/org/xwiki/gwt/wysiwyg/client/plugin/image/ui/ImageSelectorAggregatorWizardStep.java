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
import java.util.List;

import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AttachmentSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;

/**
 * Allows the user to select an image from different locations: edited page attachments, all attachments or external
 * URL.
 * 
 * @version $Id$
 */
public class ImageSelectorAggregatorWizardStep extends AttachmentSelectorAggregatorWizardStep<ImageConfig>
{
    /**
     * The wizard step used to select an external image.
     */
    private ExternalImageSelectorWizardStep externalImageSelector;

    /**
     * Creates a new wizard step that aggregates multiple views for selecting an image from different location.
     * 
     * @param selectionLimitedToCurrentPage {@code true} to limit the image selection to the attachments of the edited
     *            page, {@code false} to allow images to be selected from all pages
     * @param allowExternalImage {@code true} to allow the user to select an external image by specifying its URL,
     *            {@code false} otherwise
     * @param wikiService the object used to access the image attachments
     */
    public ImageSelectorAggregatorWizardStep(boolean selectionLimitedToCurrentPage, boolean allowExternalImage,
        WikiServiceAsync wikiService)
    {
        super(selectionLimitedToCurrentPage, wikiService);

        setStepTitle(Strings.INSTANCE.imageSelectImageTitle());
        setCurrentPageSelector(new CurrentPageImageSelectorWizardStep(wikiService, false));
        if (!selectionLimitedToCurrentPage) {
            setAllPagesSelector(new ImagesExplorerWizardStep(false, wikiService));
        }
        if (allowExternalImage) {
            externalImageSelector = new ExternalImageSelectorWizardStep(wikiService);
            externalImageSelector.setNextStep(ImageWizardStep.IMAGE_CONFIG.toString());
            externalImageSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
            externalImageSelector.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.select());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AttachmentSelectorAggregatorWizardStep#getRequiredStep()
     */
    @Override
    protected String getRequiredStep()
    {
        if (externalImageSelector != null && getData().getDestination().getType() != ResourceType.ATTACHMENT) {
            return Strings.INSTANCE.imageExternal();
        } else {
            return super.getRequiredStep();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AttachmentSelectorAggregatorWizardStep#getStepInstance(String)
     */
    @Override
    protected WizardStep getStepInstance(String name)
    {
        if (name.equals(Strings.INSTANCE.imageExternal())) {
            return externalImageSelector;
        } else {
            return super.getStepInstance(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AttachmentSelectorAggregatorWizardStep#getStepNames()
     */
    @Override
    protected List<String> getStepNames()
    {
        List<String> stepNames = super.getStepNames();
        stepNames.add(Strings.INSTANCE.imageExternal());
        return stepNames;
    }
}
