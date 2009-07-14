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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.ListBox;
import com.xpn.xwiki.wysiwyg.client.widget.ListItem;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

/**
 * Wizard step to select the wiki page to link to, from a list of wiki pages.
 * 
 * @version $Id$
 */
public abstract class AbstractPageListSelectorWizardStep extends AbstractSelectorWizardStep<LinkConfig>
{
    /**
     * Fake page preview widget to hold the option of creating a new page.
     */
    private static class NewPageOptionWidget extends PagePreviewWidget
    {
        /**
         * Default constructor.
         */
        public NewPageOptionWidget()
        {
            super(null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Widget getUI()
        {
            Label newOptionPanel = new Label(Strings.INSTANCE.linkNewPageOptionLabel());
            newOptionPanel.addStyleName("xNewPagePreview");
            return newOptionPanel;
        }
    }

    /**
     * The main panel of this wizard step.
     */
    private FlowPanel mainPanel = new FlowPanel();

    /**
     * The currently edited resource (the currently edited page).
     */
    private ResourceName editedResource;

    /**
     * The list of pages.
     */
    private ListBox pagesList = new ListBox();

    /**
     * Specifies whether the new attachment option should be shown on top or on bottom of the list.
     */
    private boolean newOptionOnTop;

    /**
     * Builds a selector from a list of pages of the specified page.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public AbstractPageListSelectorWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;
        mainPanel.addStyleName("xPagesSelector");
        // create an empty pages list
        mainPanel.add(pagesList);
        // put the new attachment option on top
        newOptionOnTop = true;
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final Object data, final AsyncCallback< ? > cb)
    {
        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                refreshPagesList(cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Reloads the list of pages previews in asynchronous manner.
     * 
     * @param cb the callback to handle server call
     */
    protected abstract void refreshPagesList(AsyncCallback< ? > cb);

    /**
     * Fills the preview list with the pages in the passed list.
     * 
     * @param pages the list of pages to build the preview for
     */
    protected void fillPagesList(List<Document> pages)
    {
        String oldSelection = null;
        if (!StringUtils.isEmpty(getData().getReference())) {
            oldSelection = getData().getReference();
        } else if (pagesList.getSelectedItem() != null
            && !(pagesList.getSelectedItem().getWidget(0) instanceof NewPageOptionWidget)) {
            oldSelection = ((PagePreviewWidget) pagesList.getSelectedItem().getWidget(0)).getDocument().getFullName();
        }
        pagesList.clear();
        for (Document doc : pages) {
            ListItem newItem = new ListItem();
            newItem.add(new PagePreviewWidget(doc));
            pagesList.addItem(newItem);
            // preserve selection
            if (oldSelection != null && oldSelection.equals(doc.getFullName())) {
                pagesList.setSelectedItem(newItem);
            }
        }
        ListItem newOptionListItem = new ListItem();
        newOptionListItem.add(new NewPageOptionWidget());
        if (newOptionOnTop) {
            pagesList.insertItem(newOptionListItem, 0);
        } else {
            pagesList.addItem(newOptionListItem);
        }
        // if there is no old selection or old selection didn't match, select the new page
        if (oldSelection == null) {
            pagesList.setSelectedItem(newOptionListItem);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        // check out the selection
        if (pagesList.getSelectedItem() != null
            && pagesList.getSelectedItem().getWidget(0) instanceof NewPageOptionWidget) {
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
    public void onCancel(AsyncCallback<Boolean> async)
    {
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        PagePreviewWidget selectedOption =
            (PagePreviewWidget) (pagesList.getSelectedItem() != null ? pagesList.getSelectedItem().getWidget(0) : null);
        if (selectedOption == null) {
            Window.alert(Strings.INSTANCE.linkNoPageSelectedError());
            async.onSuccess(false);
            return;
        }
        if (selectedOption instanceof NewPageOptionWidget) {
            // new page option, let's setup the link data accordingly
            getData().setWiki(editedResource.getWiki());
            getData().setSpace(editedResource.getSpace());
            async.onSuccess(true);
        } else {
            // check if document changed
            boolean changedDoc = true;
            ResourceName editedPage = new ResourceName(getData().getReference(), false).getRelativeTo(editedResource);
            ResourceName selectedPage =
                new ResourceName(selectedOption.getDocument().getFullName(), false).getRelativeTo(editedResource);
            if (!StringUtils.isEmpty(getData().getReference()) && editedPage.equals(selectedPage)) {
                changedDoc = false;
            }
            if (changedDoc) {
                // existing page option, set up the LinkConfig
                // page reference has to be relative to the currently edited page
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedOption.getDocument().getFullName(), false);
                getData().setReference(ref.getRelativeTo(editedResource).toString());
                // FIXME: shouldn't use upload URL here, but since we don't want to add a new field...
                getData().setUrl(selectedOption.getDocument().getUploadURL());
            }
            async.onSuccess(true);
        }
    }

    /**
     * @return the pagesList
     */
    public ListBox getPagesList()
    {
        return pagesList;
    }

    /**
     * @return the mainPanel
     */
    public FlowPanel getMainPanel()
    {
        return mainPanel;
    }
}
