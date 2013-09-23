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
import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractEntityListSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step to select the wiki page to link to, from a list of wiki pages.
 * 
 * @version $Id$
 */
public abstract class AbstractPageListSelectorWizardStep extends
    AbstractEntityListSelectorWizardStep<LinkConfig, WikiPage>
{
    /**
     * The service used to fetch the list of pages.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new selector.
     * 
     * @param wikiService the service used to access the wiki
     */
    public AbstractPageListSelectorWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;

        setStepTitle(Strings.INSTANCE.linkSelectWikipageTitle());
        display().addStyleName("xPagesSelector");
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
    protected ListItem<WikiPage> getListItem(WikiPage data)
    {
        ListItem<WikiPage> item = new ListItem<WikiPage>();
        item.setData(data);
        String documentReferenceAsString = serializeDocumentReference(new WikiPageReference(data.getReference()));
        Label pageName = new Label(Strings.INSTANCE.entityLocatedIn() + " " + documentReferenceAsString);
        pageName.addStyleName("xPagePreviewFullname");
        Label title = new Label(data.getTitle());
        title.addStyleName("xPagePreviewTitle");

        FlowPanel ui = new FlowPanel();
        if (!StringUtils.isEmpty(data.getTitle())) {
            ui.add(title);
        }
        ui.setTitle(data.getTitle());
        ui.add(pageName);
        ui.addStyleName("xPagePreview");
        item.add(ui);
        return item;
    }

    /**
     * Serializes a document reference to be displayed to the user.
     * 
     * @param reference a document reference
     * @return a user friendly string serialization of a document reference
     */
    protected String serializeDocumentReference(WikiPageReference reference)
    {
        String separator = " \u00BB ";
        return reference.getWikiName() + separator + reference.getSpaceName() + separator + reference.getPageName();
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
            return LinkWizardStep.WIKI_PAGE_CREATOR.toString();
        }
        return LinkWizardStep.LINK_CONFIG.toString();
    }

    /**
     * @return the service used to fetch the lists of pages
     */
    protected WikiServiceAsync getWikiService()
    {
        return wikiService;
    }
}
