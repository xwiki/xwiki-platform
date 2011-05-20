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

import java.util.List;

import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Wizard step to select the wiki page to link to, from the recently modified ones for the current user.
 * 
 * @version $Id$
 */
public class RecentChangesSelectorWizardStep extends AbstractPageListSelectorWizardStep
{
    /**
     * Creates a new page selector based on the recently modified pages.
     * 
     * @param wikiService the service used to retrieve the list of recently modified pages
     */
    public RecentChangesSelectorWizardStep(WikiServiceAsync wikiService)
    {
        super(wikiService);
        display().addStyleName("xPagesRecent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData(AsyncCallback<List<WikiPage>> callback)
    {
        String wikiName = new WikiPageReference(getData().getOrigin()).getWikiName();
        getWikiService().getRecentlyModifiedPages(wikiName, 0, 20, callback);
    }
}
