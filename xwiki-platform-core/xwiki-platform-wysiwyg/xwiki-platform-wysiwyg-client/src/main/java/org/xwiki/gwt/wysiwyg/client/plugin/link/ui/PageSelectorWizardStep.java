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

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractSelectorAggregatorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * A page selector that aggregates different views for selecting a page: recently modified pages, all pages or page
 * search.
 * 
 * @version $Id$
 */
public class PageSelectorWizardStep extends AbstractSelectorAggregatorWizardStep<EntityLink<LinkConfig>>
{
    /**
     * The service to be used for creating links to wiki pages.
     */
    private final WikiServiceAsync wikiService;

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * Creates a new page selector, that aggregates different views for selecting a page: recently modified pages, all
     * pages or page search.
     * 
     * @param wikiService the service to be used for creating links to wiki pages
     * @param config the configuration object
     */
    public PageSelectorWizardStep(WikiServiceAsync wikiService, Config config)
    {
        this.wikiService = wikiService;
        this.config = config;
        setStepTitle(Strings.INSTANCE.linkSelectWikipageTitle());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRequiredStep()
    {
        // If it's an edited link, require all pages.
        if (!StringUtils.isEmpty(getData().getData().getReference())) {
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
            return new RecentChangesSelectorWizardStep(wikiService);
        } else if (name.equals(Strings.INSTANCE.selectorSelectFromAllPages())) {
            return new WikiPageExplorerWizardStep(config);
        } else if (name.equals(Strings.INSTANCE.selectorSelectFromSearchPages())) {
            return new SearchSelectorWizardStep(wikiService);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getStepNames()
    {
        return Arrays.asList(Strings.INSTANCE.selectorSelectFromRecentPages(),
            Strings.INSTANCE.selectorSelectFromAllPages(), Strings.INSTANCE.selectorSelectFromSearchPages());
    }
}
