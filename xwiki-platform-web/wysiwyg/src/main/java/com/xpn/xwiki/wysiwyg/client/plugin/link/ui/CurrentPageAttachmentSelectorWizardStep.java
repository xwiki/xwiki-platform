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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.ListItem;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractListSelectorWizardStep;
import com.xpn.xwiki.wysiwyg.client.wiki.Attachment;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Wizard step to select a file attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageAttachmentSelectorWizardStep extends AbstractListSelectorWizardStep<LinkConfig, Attachment>
{
    /**
     * The currently edited resource (the currently edited page).
     */
    private ResourceName editedResource;

    /**
     * The service used to retrieve the current page attachments.
     */
    private WikiServiceAsync wikiService;

    /**
     * Builds a selector from the attachments of the specified page.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public CurrentPageAttachmentSelectorWizardStep(ResourceName editedResource)
    {
        getMainPanel().addStyleName("xAttachmentsSelector");
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectHelpLabel()
    {
        return Strings.INSTANCE.linkSelectAttachmentHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectErrorMessage()
    {
        return Strings.INSTANCE.linkNoAttachmentSelectedError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData(AsyncCallback<List<Attachment>> callback)
    {
        wikiService.getAttachments(editedResource.getWiki(), editedResource.getSpace(), editedResource.getPage(),
            callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelection()
    {
        if (!StringUtils.isEmpty(getData().getReference())) {
            ResourceName r = new ResourceName(getData().getReference(), true);
            return r.getFile();
        } else if (getSelectedItem() != null && getSelectedItem().getData() != null) {
            return getSelectedItem().getData().getFileName();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean matchesSelection(Attachment item, String selection)
    {
        return selection != null && selection.equals(item.getFileName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getListItem(Attachment data)
    {
        ListItem<Attachment> item = new ListItem<Attachment>();
        item.setData(data);
        Label attachmentLabel = new Label(data.getFileName());
        attachmentLabel.addStyleName("xAttachPreview");
        item.add(attachmentLabel);
        return item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getNewOptionListItem()
    {
        ListItem<Attachment> item = new ListItem<Attachment>();
        // no data for the new option item
        item.setData(null);
        Label newOptionPanel = new Label(Strings.INSTANCE.fileUploadNewFileLabel());
        newOptionPanel.addStyleName("xNewFilePreview");
        item.add(newOptionPanel);
        return item;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        // check out the selection, if it's a new file option
        if (getSelectedItem() != null && getSelectedItem().getData() == null) {
            return LinkWizardSteps.ATTACHMENT_UPLOAD.toString();
        }
        return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkSelectAttachmentTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSelectedValue()
    {
        Attachment selectedAttach = getSelectedItem().getData();
        if (selectedAttach == null) {
            // new file option, let's setup the attachment link data accordingly
            getData().setWiki(editedResource.getWiki());
            getData().setSpace(editedResource.getSpace());
            getData().setPage(editedResource.getPage());
        } else {
            // check if attachment changed
            boolean changedAttachment = true;
            ResourceName editedAttach = new ResourceName(getData().getReference(), true);
            if (!StringUtils.isEmpty(getData().getReference())
                && editedAttach.getFile().equals(selectedAttach.getFileName())) {
                changedAttachment = false;
            }
            if (changedAttachment) {
                // existing file option, set up the LinkConfig
                // attachment reference has to be relative to the currently edited page
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedAttach.getReference(), true);
                String attachmentRef = "attach:" + ref.getRelativeTo(editedResource).toString();
                String attachmentURL = selectedAttach.getURL();
                getData().setReference(attachmentRef);
                getData().setUrl(attachmentURL);
            }
        }
    }

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to retrieve the current page attachments
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
