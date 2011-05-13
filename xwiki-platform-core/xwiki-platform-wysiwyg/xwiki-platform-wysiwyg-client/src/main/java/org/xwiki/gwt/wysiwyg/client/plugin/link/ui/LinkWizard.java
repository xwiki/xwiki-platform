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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AttachmentSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.LinkUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;

import com.google.gwt.user.client.ui.Image;

/**
 * The link wizard, used to configure link parameters in a {@link LinkConfig} object, in successive steps. This class
 * extends the {@link Wizard} class by encapsulating {@link WizardStepProvider} behavior specific to links.
 * 
 * @version $Id$
 */
public class LinkWizard extends Wizard implements WizardStepProvider
{
    /**
     * Enumeration steps handled by this link wizard.
     */
    public static enum LinkWizardStep
    {
        /**
         * Steps managed by this wizard.
         */
        WEB_PAGE, EMAIL, WIKI_PAGE, WIKI_PAGE_CREATOR, ATTACHMENT, ATTACHMENT_UPLOAD, WIKI_PAGE_CONFIG
    };

    /**
     * Maps a link wizard step to the type of resource that step creates links to.
     */
    private static final Map<LinkWizardStep, ResourceType> WIZARD_STEP_TO_RESOURCE_TYPE_MAP;

    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<LinkWizardStep, WizardStep> stepsMap = new HashMap<LinkWizardStep, WizardStep>();

    /**
     * The resource currently edited by this WYSIWYG, used to determine the context in which link creation takes place.
     */
    private final Config config;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    static {
        WIZARD_STEP_TO_RESOURCE_TYPE_MAP = new HashMap<LinkWizardStep, ResourceType>();
        WIZARD_STEP_TO_RESOURCE_TYPE_MAP.put(LinkWizardStep.WIKI_PAGE, ResourceType.DOCUMENT);
        WIZARD_STEP_TO_RESOURCE_TYPE_MAP.put(LinkWizardStep.ATTACHMENT, ResourceType.ATTACHMENT);
        WIZARD_STEP_TO_RESOURCE_TYPE_MAP.put(LinkWizardStep.WEB_PAGE, ResourceType.URL);
        WIZARD_STEP_TO_RESOURCE_TYPE_MAP.put(LinkWizardStep.EMAIL, ResourceType.MAILTO);
    }

    /**
     * Builds a {@link LinkWizard} from the passed {@link Config}. The configuration is used to get WYSIWYG editor
     * specific information for this wizard, such as the current page, etc.
     * 
     * @param config the context configuration for this {@link LinkWizard}
     * @param wikiService the service used to access the wiki
     */
    public LinkWizard(Config config, WikiServiceAsync wikiService)
    {
        super(Strings.INSTANCE.link(), new Image(Images.INSTANCE.link()));
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
        LinkWizardStep requestedStep = parseStepName(name);
        WizardStep step = stepsMap.get(requestedStep);
        if (step == null) {
            switch (requestedStep) {
                case EMAIL:
                    step = new EmailAddressLinkWizardStep(wikiService);
                    break;
                case WIKI_PAGE:
                    step = new PageSelectorWizardStep(wikiService);
                    break;
                case WIKI_PAGE_CREATOR:
                    step = new CreateNewPageWizardStep(wikiService);
                    break;
                case ATTACHMENT:
                    step = createAttachmentSelectorWizardStep();
                    break;
                case ATTACHMENT_UPLOAD:
                    LinkUploadWizardStep<LinkConfig> attachmentUploadStep =
                        new LinkUploadWizardStep<LinkConfig>(wikiService);
                    attachmentUploadStep.setFileHelpLabel(Strings.INSTANCE.linkAttachmentUploadHelpLabel());
                    attachmentUploadStep.setNextStep(LinkWizardStep.WIKI_PAGE_CONFIG.toString());
                    step = attachmentUploadStep;
                    break;
                case WIKI_PAGE_CONFIG:
                    step = new LinkConfigWizardStep(wikiService);
                    break;
                case WEB_PAGE:
                    step = new WebPageLinkWizardStep(wikiService);
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
     * @return the wizard step to be used for selecting an attachment
     */
    private WizardStep createAttachmentSelectorWizardStep()
    {
        boolean selectionLimitedToCurrentPage = "currentpage".equals(config.getParameter("linkfiles"));
        AttachmentSelectorAggregatorWizardStep<LinkConfig> attachmentSelector =
            new AttachmentSelectorAggregatorWizardStep<LinkConfig>(selectionLimitedToCurrentPage, wikiService);
        attachmentSelector.setStepTitle(Strings.INSTANCE.linkSelectAttachmentTitle());
        attachmentSelector.setCurrentPageSelector(new CurrentPageAttachmentSelectorWizardStep(wikiService));
        if (!selectionLimitedToCurrentPage) {
            attachmentSelector.setAllPagesSelector(new AttachmentExplorerWizardStep(wikiService));
        }
        return attachmentSelector;
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
        destination.setType(WIZARD_STEP_TO_RESOURCE_TYPE_MAP.get(parseStepName(startStep)));
        destination.setTyped(destination.getType() != ResourceType.URL);

        super.start(startStep, new EntityLink<LinkConfig>(origin.getEntityReference(), destination, (LinkConfig) data));
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
        return ((EntityLink<LinkConfig>) super.getResult()).getData();
    }

    /**
     * Parses the specified step name in a {@link LinkWizardStep} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link LinkWizardStep} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private LinkWizardStep parseStepName(String name)
    {
        // let's be careful about this
        LinkWizardStep requestedStep = null;
        try {
            requestedStep = LinkWizardStep.valueOf(name);
        } catch (IllegalArgumentException e) {
            // nothing, just leave it null if it cannot be found in the enum
        }
        return requestedStep;
    }
}
