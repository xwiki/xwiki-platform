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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Specialized {@link AbstractExplorerWizardStep} to select a wiki page (existing or new).
 * 
 * @version $Id$
 */
public class WikipageExplorerWizardStep extends AbstractExplorerWizardStep
{
    /**
     * The resource edited currently (the wiki page for which this wysiwyg is instantiated).
     */
    private ResourceName editedResource;

    /**
     * Creates a wiki page selection wizard step with the specified default selection. The selection will be used to
     * position the wiki page selection tree on the resource named by it on the first load.
     * 
     * @param editedResource the currently edited resource
     */
    public WikipageExplorerWizardStep(ResourceName editedResource)
    {
        // build a standard selector which shows "Add page" and no attachments.
        super(true, false, false, editedResource.toString());
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeSelection()
    {
        if (!StringUtils.isEmpty(getData().getReference())) {
            // resolve the edited link to the currently edited page and then set the tree selection
            ResourceName r = new ResourceName();
            r.fromString(getData().getReference(), false);
            getExplorer().setValue(r.resolveRelativeTo(editedResource).toString());
        }
        // else leave the tree where the last selection was
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        if (getData().getType() == LinkType.NEW_WIKIPAGE && StringUtils.isEmpty(getData().getPage())) {
            return LinkWizardSteps.WIKIPAGECREATOR.toString();
        } else {
            return LinkWizardSteps.WIKIPAGECONFIG.toString();
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
    public void onCancel(AsyncCallback<Boolean> async)
    {
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // should check that the selection is ok according to the desired type and to "commit" it in the link config
        String selectedValue = getExplorer().getValue();
        // selected resource should not be empty
        if (StringUtils.isEmpty(selectedValue) && !getExplorer().isNewPage()) {
            Window.alert(Strings.INSTANCE.linkNoPageSelectedError());
            async.onSuccess(false);
        } else {
            // commit the changes in the config
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
            // TODO: restrict this to new pages when the explorer will return the selected resource URL, and get the
            // reference from the value of the tree
            WysiwygService.Singleton.getInstance().getPageLink(getExplorer().getSelectedWiki(),
                getExplorer().getSelectedSpace(), getExplorer().getSelectedPage(), null, null,
                new AsyncCallback<LinkConfig>()
                {
                    public void onSuccess(LinkConfig result)
                    {
                        getData().setUrl(result.getUrl());
                        getData().setReference(result.getReference());
                        async.onSuccess(true);
                    }

                    public void onFailure(Throwable caught)
                    {
                        async.onSuccess(false);
                    }
                });
        }
    }
}
