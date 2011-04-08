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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.LinkUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;

import com.google.gwt.user.client.ui.Image;

/**
 * The link wizard, used to configure image parameters in a
 * {@link org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig} object, in successive steps. This class extends the
 * {@link Wizard} class by encapsulating {@link WizardStepProvider} behavior specific to images.
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
        /**
         * Steps managed by this wizard: the image selector, the image parameters step and the new image upload step.
         */
        IMAGE_SELECTOR, IMAGE_CONFIG, IMAGE_UPLOAD
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
            switch (requestedStep) {
                case IMAGE_SELECTOR:
                    boolean selectionLimitedToCurrentPage = "currentpage".equals(config.getParameter("insertimages"));
                    boolean allowExternalImages = Boolean.valueOf(config.getParameter("allowExternalImages", "true"));
                    step =
                        new ImageSelectorAggregatorWizardStep(selectionLimitedToCurrentPage, allowExternalImages,
                            wikiService);
                    break;
                case IMAGE_CONFIG:
                    step = new ImageConfigWizardStep();
                    break;
                case IMAGE_UPLOAD:
                    LinkUploadWizardStep<ImageConfig> imageUploadStep =
                        new LinkUploadWizardStep<ImageConfig>(wikiService);
                    imageUploadStep.setFileHelpLabel(Strings.INSTANCE.imageUploadHelpLabel());
                    imageUploadStep.setNextStep(ImageWizardStep.IMAGE_CONFIG.toString());
                    step = imageUploadStep;
                    break;
                default:
                    // nothing here, leave it null
                    break;
            }
            // if something has been created, add it in the map
            if (step != null) {
                stepsMap.put(requestedStep, step);
            }
        }
        // return the found or newly created step
        return step;
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
