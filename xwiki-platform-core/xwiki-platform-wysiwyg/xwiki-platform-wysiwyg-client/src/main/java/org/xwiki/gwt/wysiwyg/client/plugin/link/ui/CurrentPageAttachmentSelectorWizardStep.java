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

import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractEntityListSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step to select a file attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageAttachmentSelectorWizardStep extends
    AbstractEntityListSelectorWizardStep<LinkConfig, Attachment>
{
    /**
     * The service used to fetch the list of files attached to the current page.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Builds a selector from the attachments of the link origin page.
     * 
     * @param wikiService the service used to retrieve the attachments of the current page
     */
    public CurrentPageAttachmentSelectorWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;

        setStepTitle(Strings.INSTANCE.linkSelectAttachmentTitle());
        display().addStyleName("xAttachmentsSelector");
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
        wikiService.getAttachments(new WikiPageReference(getData().getOrigin()), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getListItem(Attachment data)
    {
        ListItem<Attachment> item = new ListItem<Attachment>();
        item.setData(data);
        Label attachmentLabel = new Label(new AttachmentReference(data.getReference()).getFileName());
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
            return LinkWizardStep.ATTACHMENT_UPLOAD.toString();
        }
        return LinkWizardStep.LINK_CONFIG.toString();
    }
}
