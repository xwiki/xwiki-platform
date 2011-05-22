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
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AttachmentSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.LinkUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceSerializerWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.ui.Image;

/**
 * The link wizard, used to configure image parameters in a {@link ImageConfig} object, in successive steps. This class
 * extends the {@link Wizard} class by encapsulating {@link WizardStepProvider} behavior specific to images.
 * 
 * @version $Id$
 */
public class ImageWizard extends Wizard implements WizardStepProvider
{
    /**
     * Enumeration steps handled by this image wizard.
     */
    public static enum ImageWizardStep
    {
        /** The step that parses the image reference. Loaded when editing an image. */
        IMAGE_REFERENCE_PARSER,

        /** The step that selects an attached image. */
        ATTACHED_IMAGE_SELECTOR,

        /** The step that selects an external image specified by its URL. */
        URL_IMAGE_SELECTOR,

        /** The step that uploads a new image. */
        IMAGE_UPLOAD,

        /** The step that configures the image parameters. */
        IMAGE_CONFIG,

        /** The step that serializes the image reference. */
        IMAGE_REFERENCE_SERIALIZER
    };

    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<ImageWizardStep, WizardStep> stepsMap = new HashMap<ImageWizardStep, WizardStep>();

    /**
     * The object used to configure this wizard.
     */
    private final Config config;

    /**
     * The service used to access the image attachments.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Builds a {@link ImageWizard} from the passed {@link Config}. The configuration is used to get WYSIWYG editor
     * specific information for this wizard, such as the current page, configuration parameters.
     * 
     * @param config the context configuration for this {@link ImageWizard}
     * @param wikiService the service used to access the image attachments
     */
    public ImageWizard(Config config, WikiServiceAsync wikiService)
    {
        super(Strings.INSTANCE.imageTooltip(), new Image(Images.INSTANCE.image()));
        this.config = config;
        this.wikiService = wikiService;
        this.setProvider(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        ImageWizardStep requestedStep = parseStepName(name);
        WizardStep step = stepsMap.get(requestedStep);
        if (step == null) {
            step = getStep(requestedStep);
            // If the step instance was created then cache it.
            if (step != null) {
                stepsMap.put(requestedStep, step);
            }
        }
        return step;
    }

    /**
     * @param requestedStep the requested wizard step
     * @return an instance of the specified wizard step
     */
    private WizardStep getStep(ImageWizardStep requestedStep)
    {
        WizardStep step = null;
        switch (requestedStep) {
            case IMAGE_REFERENCE_PARSER:
                step = createImageDispatcherStep();
                break;
            case ATTACHED_IMAGE_SELECTOR:
                step = createAttachedImageSelectorStep();
                break;
            case URL_IMAGE_SELECTOR:
                step = createURLImageSelectorStep();
                break;
            case IMAGE_UPLOAD:
                step = createImageUploadStep();
                break;
            case IMAGE_CONFIG:
                step = createImageConfigStep();
                break;
            case IMAGE_REFERENCE_SERIALIZER:
                step = createImageReferenceSerializerStep();
                break;
            default:
                break;
        }
        return step;
    }

    /**
     * @return a wizard step that parses the image reference and forwards the control to the next step based on the
     *         image type
     */
    private WizardStep createImageDispatcherStep()
    {
        boolean allowExternalImages = Boolean.valueOf(config.getParameter("allowExternalImages", "true"));
        ImageDispatcherWizardStep imageDispatcher = new ImageDispatcherWizardStep(allowExternalImages, wikiService);
        // Display the next step title in case of an error.
        imageDispatcher.setStepTitle(Strings.INSTANCE.imageSelectImageTitle());
        return imageDispatcher;
    }

    /**
     * @return a wizard step that selects an attached image
     */
    private WizardStep createAttachedImageSelectorStep()
    {
        boolean selectionLimitedToCurrentPage = "currentpage".equals(config.getParameter("insertimages"));
        AttachmentSelectorAggregatorWizardStep<ImageConfig> attachedImageSelector =
            new AttachmentSelectorAggregatorWizardStep<ImageConfig>(selectionLimitedToCurrentPage);
        attachedImageSelector.setStepTitle(Strings.INSTANCE.imageSelectImageTitle());
        attachedImageSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
        attachedImageSelector.setCurrentPageSelector(new CurrentPageImageSelectorWizardStep(wikiService, false));
        if (!selectionLimitedToCurrentPage) {
            attachedImageSelector.setAllPagesSelector(new ImagesExplorerWizardStep(false, wikiService));
        }
        return attachedImageSelector;
    }

    /**
     * @return a wizard step that selects an external image specified by its URL
     */
    private WizardStep createURLImageSelectorStep()
    {
        URLImageSelectorWizardStep urlImageSelector = new URLImageSelectorWizardStep();
        urlImageSelector.setNextStep(ImageWizardStep.IMAGE_CONFIG.toString());
        urlImageSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT, NavigationDirection.FINISH));
        urlImageSelector.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.imageSettingsLabel());
        urlImageSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.imageCreateImageButton());
        return urlImageSelector;
    }

    /**
     * @return a wizard step that configures an image
     */
    private WizardStep createImageConfigStep()
    {
        ImageConfigWizardStep imageConfigStep = new ImageConfigWizardStep();
        imageConfigStep.setNextStep(ImageWizardStep.IMAGE_REFERENCE_SERIALIZER.toString());
        imageConfigStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH, NavigationDirection.PREVIOUS));
        imageConfigStep.setDirectionName(NavigationDirection.PREVIOUS, Strings.INSTANCE.imageChangeImageButton());
        imageConfigStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.imageCreateImageButton());
        return imageConfigStep;
    }

    /**
     * @return a wizard step that uploads a new image
     */
    private WizardStep createImageUploadStep()
    {
        LinkUploadWizardStep<ImageConfig> imageUploadStep = new LinkUploadWizardStep<ImageConfig>(wikiService);
        imageUploadStep.setFileHelpLabel(Strings.INSTANCE.imageUploadHelpLabel());
        imageUploadStep.setNextStep(ImageWizardStep.IMAGE_CONFIG.toString());
        imageUploadStep.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.NEXT,
            NavigationDirection.FINISH));
        imageUploadStep.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.imageSettingsLabel());
        imageUploadStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.imageCreateImageButton());
        return imageUploadStep;
    }

    /**
     * @return a wizard step that serializes the image reference
     */
    private WizardStep createImageReferenceSerializerStep()
    {
        ResourceReferenceSerializerWizardStep<ImageConfig> imageRefSerializer =
            new ResourceReferenceSerializerWizardStep<ImageConfig>(wikiService);
        imageRefSerializer.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.FINISH));
        // Display the previous step title in case of an error.
        imageRefSerializer.setStepTitle(Strings.INSTANCE.imageConfigTitle());
        return imageRefSerializer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Wizard#start(String, Object)
     */
    @Override
    public void start(String startStep, Object data)
    {
        WikiPageReference origin = new WikiPageReference();
        origin.setWikiName(config.getParameter("wiki"));
        origin.setSpaceName(config.getParameter("space"));
        origin.setPageName(config.getParameter("page"));

        ResourceReference destination = new ResourceReference();
        destination.setEntityReference(origin.getEntityReference().clone());
        destination.setType(ResourceType.ATTACHMENT);
        destination.setTyped(false);

        super.start(startStep,
            new EntityLink<ImageConfig>(origin.getEntityReference(), destination, (ImageConfig) data));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Wizard#getResult()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object getResult()
    {
        return ((EntityLink<ImageConfig>) super.getResult()).getData();
    }

    /**
     * Parses the specified step name in a {@link ImageWizardStep} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link ImageWizardStep} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private ImageWizardStep parseStepName(String name)
    {
        // let's be careful about this
        ImageWizardStep requestedStep = null;
        try {
            requestedStep = ImageWizardStep.valueOf(name);
        } catch (IllegalArgumentException e) {
            // nothing, just leave it null if it cannot be found in the enum
        }
        return requestedStep;
    }
}
