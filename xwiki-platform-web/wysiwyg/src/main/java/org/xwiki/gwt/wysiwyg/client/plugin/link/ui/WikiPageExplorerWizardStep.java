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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Specialized {@link AbstractExplorerWizardStep} to select a wiki page (existing or new).
 * 
 * @version $Id$
 */
public class WikiPageExplorerWizardStep extends AbstractExplorerWizardStep
{
    /**
     * The resource edited currently (the wiki page for which this wysiwyg is instantiated).
     */
    private ResourceName editedResource;

    /**
     * The service used to crate page links.
     */
    private WikiServiceAsync wikiService;

    /**
     * Creates a wiki page selection wizard step with the specified default selection. The selection will be used to
     * position the wiki page selection tree on the resource named by it on the first load.
     * 
     * @param editedResource the currently edited resource
     */
    public WikiPageExplorerWizardStep(ResourceName editedResource)
    {
        // build a standard selector which shows "Add page" and no attachments.
        // FIXME: size hardcoding is very bad, remove when a method to control this from CSS will be found
        super(true, false, false, editedResource.toString(), 455, 280);
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeSelection(AsyncCallback< ? > initCallback)
    {
        if (!StringUtils.isEmpty(getData().getReference())) {
            getExplorer().setValue(getData().getReference());
        }
        // else leave the tree where the last selection was
        super.initializeSelection(initCallback);
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        if (getData().getType() == LinkType.NEW_WIKIPAGE && StringUtils.isEmpty(getData().getReference())) {
            return LinkWizardSteps.WIKI_PAGE_CREATOR.toString();
        } else {
            return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkSelectWikipageTitle();
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        hideError();
        // should check that the selection is ok according to the desired type and to "commit" it in the link config
        String selectedValue = getExplorer().getValue();
        // selected resource should not be empty
        if (StringUtils.isEmpty(selectedValue) && !getExplorer().isNewPage()) {
            displayError(Strings.INSTANCE.linkNoPageSelectedError());
            async.onSuccess(false);
        } else if (StringUtils.isEmpty(getData().getReference()) || !selectedValue.equals(getData().getReference())) {
            // commit the changes in the config, only if necessary
            if (getExplorer().isNewPage()) {
                // if it's a new page to be created, set its parameters in the link config
                getData().setType(LinkType.NEW_WIKIPAGE);
                getData().setWiki(getExplorer().getSelectedWiki());
                getData().setSpace(getExplorer().getSelectedSpace());
                // if the user has clicked on a "New page..." node, return
                if (getExplorer().isNewPageSelectedFromTreeNode()) {
                    async.onSuccess(true);
                    return;
                }
                getData().setPage(getExplorer().getSelectedPage());
            } else {
                // it's an existing page
                getData().setType(LinkType.WIKIPAGE);
                // set the page space wiki on nothing, since the link will have a reference
                getData().setWiki(null);
                getData().setSpace(null);
                getData().setPage(null);
            }
            // build the link url and reference from the parameters.
            // FIXME: move the reference setting logic in a controller, along with the async fetching
            wikiService.getPageLink(getExplorer().getSelectedWiki(), getExplorer().getSelectedSpace(),
                getExplorer().getSelectedPage(), null, null, new AsyncCallback<LinkConfig>()
                {
                    public void onSuccess(LinkConfig result)
                    {
                        getData().setUrl(result.getUrl());
                        // set the reference in the link, but relative to the currently edited resource
                        ResourceName ref = new ResourceName(result.getReference(), false);
                        getData().setReference(ref.getRelativeTo(editedResource).toString());
                        async.onSuccess(true);
                    }

                    public void onFailure(Throwable caught)
                    {
                        async.onSuccess(false);
                    }
                });
        } else {
            async.onSuccess(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultErrorText()
    {
        return Strings.INSTANCE.linkNoPageSelectedError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getHelpLabelText()
    {
        return Strings.INSTANCE.linkSelectWikipageHelpLabel();
    }

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to create page links
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
