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
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageConfigWizardStep;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkConfigWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceSerializerWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Provides wizard steps for the Alfresco wizard.
 * 
 * @version $Id$
 */
public class AlfrescoWizardStepProvider implements WizardStepProvider
{
    /**
     * Available wizard steps.
     */
    public static enum AlfrescoWizardStep
    {
        /** The step that parses the link and image references. */
        RESOURCE_REFERENCE_PARSER,

        /** The step that selects the link target. */
        LINK_SELECTOR,

        /** The step that configures the link. */
        LINK_SETTINGS,

        /** The step that selects the image. */
        IMAGE_SELECTOR,

        /** The step that configures the image. */
        IMAGE_SETTINGS,

        /** The step that serializes the link and image reference. */
        RESOURCE_REFERENCE_SERIALIZER
    }

    /**
     * The service used to parse and serialize resource references.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The service used to access an Alfresco content management system.
     */
    private final AlfrescoServiceAsync alfrescoService;

    /**
     * The map of wizard step instances.
     */
    private final Map<AlfrescoWizardStep, WizardStep> steps = new HashMap<AlfrescoWizardStep, WizardStep>();

    /**
     * Creates a new wizard step provider for the Alfresco wizard.
     * 
     * @param wikiService the service used to parse and serialize resource references
     * @param alfrescoService the service used to access an Alfresco content management system
     */
    public AlfrescoWizardStepProvider(WikiServiceAsync wikiService, AlfrescoServiceAsync alfrescoService)
    {
        this.wikiService = wikiService;
        this.alfrescoService = alfrescoService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        AlfrescoWizardStep requestedStep = null;
        try {
            requestedStep = AlfrescoWizardStep.valueOf(name);
        } catch (Exception e) {
            return null;
        }
        WizardStep step = steps.get(requestedStep);
        if (step == null) {
            step = getStep(requestedStep);
            // If the step instance was created then cache it.
            if (step != null) {
                steps.put(requestedStep, step);
            }
        }
        return step;
    }

    /**
     * @param requestedStep the requested wizard step
     * @return an instance of the specified step
     */
    private WizardStep getStep(AlfrescoWizardStep requestedStep)
    {
        WizardStep step = null;
        switch (requestedStep) {
            case RESOURCE_REFERENCE_PARSER:
                step = new AlfrescoResourceReferenceParserWizardStep(wikiService);
                break;
            case LINK_SELECTOR:
                step = createLinkSelectorStep();
                break;
            case LINK_SETTINGS:
                step = createLinkSettingsStep();
                break;
            case IMAGE_SELECTOR:
                step = createImageSelectorStep();
                break;
            case IMAGE_SETTINGS:
                step = createImageSettingsStep();
                break;
            case RESOURCE_REFERENCE_SERIALIZER:
                step = createResourceReferenceSerializerStep();
                break;
            default:
        }
        return step;
    }

    /**
     * @return a wizard step that selects an Alfresco file to create a link to
     */
    private WizardStep createLinkSelectorStep()
    {
        AlfrescoEntitySelectorWizardStep linkSelector = new AlfrescoEntitySelectorWizardStep(alfrescoService);
        linkSelector.setStepTitle(AlfrescoConstants.INSTANCE.linkSelectorTitle());
        linkSelector.setNextStep(AlfrescoWizardStep.LINK_SETTINGS.toString());
        linkSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT, NavigationDirection.FINISH));
        linkSelector.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.linkSettingsLabel());
        linkSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return linkSelector;
    }

    /**
     * @return a wizard step that configures the link parameters
     */
    private WizardStep createLinkSettingsStep()
    {
        LinkConfigWizardStep linkConfigStep = new LinkConfigWizardStep(wikiService);
        linkConfigStep.setNextStep(AlfrescoWizardStep.RESOURCE_REFERENCE_SERIALIZER.toString());
        linkConfigStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH, NavigationDirection.PREVIOUS));
        linkConfigStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return linkConfigStep;
    }

    /**
     * @return a wizard step that selects an Alfresco image
     */
    private WizardStep createImageSelectorStep()
    {
        AlfrescoImageSelectorWizardStep imageSelector = new AlfrescoImageSelectorWizardStep(alfrescoService);
        imageSelector.setNextStep(AlfrescoWizardStep.IMAGE_SETTINGS.toString());
        imageSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT, NavigationDirection.FINISH));
        imageSelector.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.imageSettingsLabel());
        imageSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.imageCreateImageButton());
        return imageSelector;
    }

    /**
     * @return a wizard step that configures an image
     */
    private WizardStep createImageSettingsStep()
    {
        ImageConfigWizardStep imageConfigStep = new ImageConfigWizardStep();
        imageConfigStep.setNextStep(AlfrescoWizardStep.RESOURCE_REFERENCE_SERIALIZER.toString());
        imageConfigStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH, NavigationDirection.PREVIOUS));
        imageConfigStep.setDirectionName(NavigationDirection.PREVIOUS, Strings.INSTANCE.imageChangeImageButton());
        imageConfigStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.imageCreateImageButton());
        return imageConfigStep;
    }

    /**
     * @return a wizard step that serialzies a resource reference
     */
    private WizardStep createResourceReferenceSerializerStep()
    {
        ResourceReferenceSerializerWizardStep<EntityConfig> resourceReferenceSerializer =
            new ResourceReferenceSerializerWizardStep<EntityConfig>(wikiService);
        resourceReferenceSerializer.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS,
            NavigationDirection.FINISH));
        return resourceReferenceSerializer;
    }
}
