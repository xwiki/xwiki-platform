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
package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.Wizard;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStepProvider;

/**
 * The link wizard, used to configure image parameters in a
 * {@link com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig} object, in successive steps. This class extends the
 * {@link Wizard} class by encapsulating {@link WizardStepProvider} behavior specific to images.
 * 
 * @version $Id$
 */
public class ImageWizard extends Wizard implements WizardStepProvider
{
    /**
     * Enumeration steps handled by this image wizard.
     */
    public static enum ImageWizardSteps
    {
        /**
         * Steps managed by this wizard: the image selector, the image parameters step and the new image upload step.
         */
        IMAGESELECTOR, IMAGECONFIG, IMAGEUPLOAD
    };

    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<ImageWizardSteps, WizardStep> stepsMap = new HashMap<ImageWizardSteps, WizardStep>();

    /**
     * The resource currently edited by this WYSIWYG, used to determine the context in which image insertion takes
     * place.
     */
    private Config config;

    /**
     * Builds a {@link ImageWizard} from the passed {@link Config}. The configuration is used to get WYSIWYG editor
     * specific information for this wizard, such as the current page, configuration parameters.
     * 
     * @param config the context configuration for this {@link ImageWizard}
     */
    public ImageWizard(Config config)
    {
        super(Strings.INSTANCE.imageTooltip(), Images.INSTANCE.image().createImage());
        this.config = config;
        this.setProvider(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        ImageWizardSteps requestedStep = parseStepName(name);
        WizardStep step = stepsMap.get(requestedStep);
        if (step == null) {
            switch (requestedStep) {
                case IMAGESELECTOR:
                    step = dispatchImageSelectorStep();
                    break;
                case IMAGECONFIG:
                    step = new ImageConfigWizardStep();
                    break;
                case IMAGEUPLOAD:
                    step = new ImageUploadWizardStep();
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
     * @return the currently edited resource, from the configuration
     */
    private ResourceName getEditedResource()
    {
        return new ResourceName(config.getParameter("wiki"), config.getParameter("space"), config.getParameter("page"),
            null);
    }

    /**
     * @return the wizard step for image selector wrt the configuration parameters. If the {@code insertimages}
     *         parameter with the value {@code currentpage} is not found, then the selector will be enabled for the
     *         whole wiki, otherwise only for the current page.
     */
    private WizardStep dispatchImageSelectorStep()
    {
        String insertImages = config.getParameter("insertimages");
        if ("currentpage".equals(insertImages)) {
            return new CurrentPageImageSelectorWizardStep(getEditedResource());
        } else {
            return new ImageSelectorWizardStep(getEditedResource());
        }
    }

    /**
     * Parses the specified step name in a {@link ImageWizardSteps} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link ImageWizardSteps} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private ImageWizardSteps parseStepName(String name)
    {
        // let's be careful about this
        ImageWizardSteps requestedStep = null;
        try {
            requestedStep = ImageWizardSteps.valueOf(name);
        } catch (IllegalArgumentException e) {
            // nothing, just leave it null if it cannot be found in the enum
        }
        return requestedStep;
    }
}
