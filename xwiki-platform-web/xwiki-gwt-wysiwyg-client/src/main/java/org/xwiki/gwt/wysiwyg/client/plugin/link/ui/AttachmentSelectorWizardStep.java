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

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;


/**
 * Wizard step to select the attachment to link to, by aggregating a page selector step and an explorer wizard step.
 * 
 * @version $Id$
 */
public class AttachmentSelectorWizardStep extends AbstractSelectorAggregatorWizardStep<LinkConfig>
{
    /**
     * The service used to access the attachments.
     */
    private WikiServiceAsync wikiService;

    /**
     * Builds an attachment selector step for the currently edited resource.
     * 
     * @param editedResource the resource edited by this aggregator step
     */
    public AttachmentSelectorWizardStep(ResourceName editedResource)
    {
        super(editedResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRequiredStep()
    {
        if (StringUtils.isEmpty(getData().getReference())) {
            // no reference set, default with current page
            return Strings.INSTANCE.selectorSelectFromCurrentPage();
        }

        // check if the edited attachment is in the current page
        ResourceName resource = new ResourceName(getData().getReference(), true);
        // check match on current page
        if (resource.matchesUpToPage(getEditedResource())) {
            return Strings.INSTANCE.selectorSelectFromCurrentPage();
        } else {
            return Strings.INSTANCE.selectorSelectFromAllPages();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WizardStep getStepInstance(String name)
    {
        if (name.equals(Strings.INSTANCE.selectorSelectFromCurrentPage())) {
            CurrentPageAttachmentSelectorWizardStep step =
                new CurrentPageAttachmentSelectorWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        }
        if (name.equals(Strings.INSTANCE.selectorSelectFromAllPages())) {
            AttachmentExplorerWizardStep step = new AttachmentExplorerWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getStepNames()
    {
        return Arrays.asList(Strings.INSTANCE.selectorSelectFromCurrentPage(), Strings.INSTANCE
            .selectorSelectFromAllPages());
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkSelectAttachmentTitle();
    }

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to access the attachments
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
