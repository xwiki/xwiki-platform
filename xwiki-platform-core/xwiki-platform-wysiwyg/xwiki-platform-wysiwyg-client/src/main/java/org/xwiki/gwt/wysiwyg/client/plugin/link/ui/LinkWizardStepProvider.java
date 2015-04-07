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
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AttachmentSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.LinkUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.ResourceReferenceSerializerWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Provides link wizard steps.
 * 
 * @version $Id$
 */
public class LinkWizardStepProvider implements WizardStepProvider
{
    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private final Map<LinkWizardStep, WizardStep> steps = new HashMap<LinkWizardStep, WizardStep>();

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new step provider.
     * 
     * @param config the configuration object
     * @param wikiService the service used to access the wiki
     */
    public LinkWizardStepProvider(Config config, WikiServiceAsync wikiService)
    {
        this.config = config;
        this.wikiService = wikiService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        LinkWizardStep requestedStep = parseStepName(name);
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
     * Parses the specified step name in a {@link LinkWizardStep} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link LinkWizardStep} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private LinkWizardStep parseStepName(String name)
    {
        try {
            return LinkWizardStep.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param requestedStep the request link wizard step
     * @return an instance of the specified wizard step
     */
    private WizardStep getStep(LinkWizardStep requestedStep)
    {
        WizardStep step = null;
        switch (requestedStep) {
            case LINK_REFERENCE_PARSER:
                step = new LinkDispatcherWizardStep(wikiService);
                break;
            case WIKI_PAGE:
                PageSelectorWizardStep pageSelector = new PageSelectorWizardStep(wikiService, config);
                pageSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
                step = pageSelector;
                break;
            case WIKI_PAGE_CREATOR:
                step = createNewPageSelectorStep();
                break;
            case ATTACHMENT:
                step = createAttachmentSelectorStep();
                break;
            case ATTACHMENT_UPLOAD:
                step = createAttachmentUploadStep();
                break;
            case WEB_PAGE:
                step = createWebPageSelectorStep();
                break;
            case EMAIL:
                step = createEmailSelectorStep();
                break;
            case LINK_CONFIG:
                step = createLinkConfigStep();
                break;
            case LINK_REFERENCE_SERIALIZER:
                step = createLinkReferenceSerializerStep();
                break;
            default:
        }
        return step;
    }

    /**
     * @return a wizard step that selects a page that doesn't exist
     */
    private WizardStep createNewPageSelectorStep()
    {
        CreateNewPageWizardStep newPageSelector = new CreateNewPageWizardStep();
        newPageSelector.setNextStep(LinkWizardStep.LINK_CONFIG.toString());
        newPageSelector.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.NEXT,
            NavigationDirection.FINISH));
        newPageSelector.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.linkSettingsLabel());
        newPageSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return newPageSelector;
    }

    /**
     * @return a wizard step to be used for selecting an attachment
     */
    private WizardStep createAttachmentSelectorStep()
    {
        boolean selectionLimitedToCurrentPage = "currentpage".equals(config.getParameter("linkfiles"));
        AttachmentSelectorAggregatorWizardStep<LinkConfig> attachmentSelector =
            new AttachmentSelectorAggregatorWizardStep<LinkConfig>(selectionLimitedToCurrentPage);
        attachmentSelector.setStepTitle(Strings.INSTANCE.linkSelectAttachmentTitle());
        attachmentSelector.setValidDirections(EnumSet.of(NavigationDirection.NEXT));
        attachmentSelector.setCurrentPageSelector(new CurrentPageAttachmentSelectorWizardStep(wikiService));
        if (!selectionLimitedToCurrentPage) {
            attachmentSelector.setAllPagesSelector(new AttachmentExplorerWizardStep(config));
        }
        return attachmentSelector;
    }

    /**
     * @return a wizard step that uploads a new file
     */
    private WizardStep createAttachmentUploadStep()
    {
        LinkUploadWizardStep<LinkConfig> attachmentUploadStep = new LinkUploadWizardStep<LinkConfig>(wikiService);
        attachmentUploadStep.setFileHelpLabel(Strings.INSTANCE.linkAttachmentUploadHelpLabel());
        attachmentUploadStep.setNextStep(LinkWizardStep.LINK_CONFIG.toString());
        attachmentUploadStep.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.NEXT,
            NavigationDirection.FINISH));
        attachmentUploadStep.setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.linkSettingsLabel());
        attachmentUploadStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return attachmentUploadStep;
    }

    /**
     * @return a wizard step that selects a web page specified by its URL
     */
    private WizardStep createWebPageSelectorStep()
    {
        WebPageLinkWizardStep webPageSelector = new WebPageLinkWizardStep(wikiService);
        webPageSelector.setNextStep(LinkWizardStep.LINK_REFERENCE_SERIALIZER.toString());
        webPageSelector.setValidDirections(EnumSet.of(NavigationDirection.FINISH));
        webPageSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return webPageSelector;
    }

    /**
     * @return a wizard step that selects a email address
     */
    private WizardStep createEmailSelectorStep()
    {
        EmailAddressLinkWizardStep emailSelector = new EmailAddressLinkWizardStep(wikiService);
        emailSelector.setNextStep(LinkWizardStep.LINK_REFERENCE_SERIALIZER.toString());
        emailSelector.setValidDirections(EnumSet.of(NavigationDirection.FINISH));
        emailSelector.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return emailSelector;
    }

    /**
     * @return a wizard step that configures the link parameters
     */
    private WizardStep createLinkConfigStep()
    {
        LinkConfigWizardStep linkConfigStep = new LinkConfigWizardStep(wikiService);
        linkConfigStep.setNextStep(LinkWizardStep.LINK_REFERENCE_SERIALIZER.toString());
        linkConfigStep.setValidDirections(EnumSet.of(NavigationDirection.FINISH, NavigationDirection.PREVIOUS));
        linkConfigStep.setDirectionName(NavigationDirection.FINISH, Strings.INSTANCE.linkCreateLinkButton());
        return linkConfigStep;
    }

    /**
     * @return a wizard step that serializes the link reference
     */
    private WizardStep createLinkReferenceSerializerStep()
    {
        ResourceReferenceSerializerWizardStep<LinkConfig> linkRefSerializer =
            new ResourceReferenceSerializerWizardStep<LinkConfig>(wikiService);
        linkRefSerializer.setValidDirections(EnumSet.of(NavigationDirection.PREVIOUS, NavigationDirection.FINISH));
        // Display the previous step title in case of an error.
        linkRefSerializer.setStepTitle(Strings.INSTANCE.linkConfigTitle());
        return linkRefSerializer;
    }
}
