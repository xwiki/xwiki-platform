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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;

import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorAggregatorWizardStep;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Creates a page selector aggregator, to switch between the recent changed pages and all pages.
 * 
 * @version $Id$
 */
public class PageSelectorWizardStep extends AbstractSelectorAggregatorWizardStep<LinkConfig>
{
    /**
     * The service used to access the wiki.
     */
    private WikiServiceAsync wikiService;

    /**
     * Builds a page selector step for the currently edited resource.
     * 
     * @param editedResource the resource edited by this aggregator step
     */
    public PageSelectorWizardStep(ResourceName editedResource)
    {
        super(editedResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRequiredStep()
    {
        // if it's an edited link, require all pages
        if (!StringUtils.isEmpty(getData().getReference())) {
            return Strings.INSTANCE.selectorSelectFromAllPages();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WizardStep getStepInstance(String name)
    {
        if (name.equals(Strings.INSTANCE.selectorSelectFromRecentPages())) {
            RecentChangesSelectorWizardStep step = new RecentChangesSelectorWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        }
        if (name.equals(Strings.INSTANCE.selectorSelectFromAllPages())) {
            WikiPageExplorerWizardStep step = new WikiPageExplorerWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        }
        if (name.equals(Strings.INSTANCE.selectorSelectFromSearchPages())) {
            SearchSelectorWizardStep step = new SearchSelectorWizardStep(getEditedResource());
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
        return Arrays.asList(Strings.INSTANCE.selectorSelectFromRecentPages(), Strings.INSTANCE
            .selectorSelectFromAllPages(), Strings.INSTANCE.selectorSelectFromSearchPages());
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkSelectWikipageTitle();
    }

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to access the wiki
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
