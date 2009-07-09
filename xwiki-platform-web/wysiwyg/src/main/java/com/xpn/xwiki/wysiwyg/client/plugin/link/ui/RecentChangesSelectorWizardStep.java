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

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;

/**
 * Wizard step to select the wiki page to link to, from the recently modified ones for the current user.
 * 
 * @version $Id$
 */
public class RecentChangesSelectorWizardStep extends AbstractPageListSelectorWizardStep
{
    /**
     * Builds a page list selector wizard step for the currently edited resource.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public RecentChangesSelectorWizardStep(ResourceName editedResource)
    {
        super(editedResource);
    }

    /**
     * {@inheritDoc}
     */
    protected void refreshPagesList(final AsyncCallback< ? > cb)
    {
        WysiwygService.Singleton.getInstance().getRecentlyModifiedPages(0, 20, new AsyncCallback<List<Document>>()
        {
            public void onSuccess(List<Document> result)
            {
                fillPagesList(result);
                cb.onSuccess(null);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }
}
