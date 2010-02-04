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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.ListItem;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractListSelectorWizardStep;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiPage;

/**
 * Wizard step to select the wiki page to link to, from a list of wiki pages.
 * 
 * @version $Id$
 */
public abstract class AbstractPageListSelectorWizardStep extends AbstractListSelectorWizardStep<LinkConfig, WikiPage>
{
    /**
     * The currently edited resource (the currently edited page).
     */
    private ResourceName editedResource;

    /**
     * Builds a selector from a list of pages of the specified page.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public AbstractPageListSelectorWizardStep(ResourceName editedResource)
    {
        getMainPanel().addStyleName("xPagesSelector");
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectHelpLabel()
    {
        return Strings.INSTANCE.linkSelectWikipageHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectErrorMessage()
    {
        return Strings.INSTANCE.linkNoPageSelectedError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelection()
    {
        if (!StringUtils.isEmpty(getData().getReference())) {
            return getData().getReference();
        } else if (getSelectedItem() != null && getSelectedItem().getData() != null) {
            return getSelectedItem().getData().getName();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean matchesSelection(WikiPage item, String selection)
    {
        return selection != null && selection.equals(item.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<WikiPage> getListItem(WikiPage data)
    {
        ListItem<WikiPage> item = new ListItem<WikiPage>();
        item.setData(data);
        Label pageName = new Label(data.getName());
        pageName.addStyleName("xPagePreviewFullname");
        Label title = new Label(data.getTitle());
        title.addStyleName("xPagePreviewTitle");

        FlowPanel ui = new FlowPanel();
        if (!StringUtils.isEmpty(data.getTitle())) {
            ui.add(title);
        }
        String prettyName = StringUtils.isEmpty(data.getTitle()) ? "" : data.getTitle() + " - ";
        prettyName += data.getName();
        ui.setTitle(prettyName);
        ui.add(pageName);
        ui.addStyleName("xPagePreview");
        item.add(ui);
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<WikiPage> getNewOptionListItem()
    {
        ListItem<WikiPage> item = new ListItem<WikiPage>();
        item.setData(null);
        Label newOptionPanel = new Label(Strings.INSTANCE.linkNewPageOptionLabel());
        newOptionPanel.addStyleName("xNewPagePreview");
        item.add(newOptionPanel);
        return item;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        // check out the selection, if it's a new page option
        if (getSelectedItem() != null && getSelectedItem().getData() == null) {
            return LinkWizardSteps.WIKI_PAGE_CREATOR.toString();
        }
        return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
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
    @Override
    protected void saveSelectedValue()
    {
        WikiPage selectedDocument = getSelectedItem().getData();
        if (selectedDocument == null) {
            // new page option, let's setup the link data accordingly
            getData().setWiki(editedResource.getWiki());
            getData().setSpace(editedResource.getSpace());
        } else {
            // check if document changed
            boolean changedDoc = true;
            ResourceName editedPage = new ResourceName(getData().getReference(), false).getRelativeTo(editedResource);
            ResourceName selectedPage =
                new ResourceName(selectedDocument.getName(), false).getRelativeTo(editedResource);
            if (!StringUtils.isEmpty(getData().getReference()) && editedPage.equals(selectedPage)) {
                changedDoc = false;
            }
            if (changedDoc) {
                // existing page option, set up the LinkConfig
                // page reference has to be relative to the currently edited page
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedDocument.getName(), false);
                getData().setReference(ref.getRelativeTo(editedResource).toString());
                getData().setUrl(selectedDocument.getURL());
            }
        }
    }
}
