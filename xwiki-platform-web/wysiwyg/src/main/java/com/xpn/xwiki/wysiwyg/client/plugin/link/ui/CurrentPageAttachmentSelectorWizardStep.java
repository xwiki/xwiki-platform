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
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.AttachmentPreviewWidget;
import com.xpn.xwiki.wysiwyg.client.widget.ListBox;
import com.xpn.xwiki.wysiwyg.client.widget.ListItem;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

/**
 * Wizard step to select a file attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageAttachmentSelectorWizardStep extends AbstractSelectorWizardStep<LinkConfig>
{
    /**
     * Fake attachment preview widget to hold the option of attaching a new file.
     */
    private static class NewAttachmentOptionWidget extends AttachmentPreviewWidget
    {
        /**
         * Default constructor.
         */
        public NewAttachmentOptionWidget()
        {
            super(null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Widget getUI()
        {
            Label newOptionPanel = new Label(Strings.INSTANCE.fileUploadNewFileLabel());
            newOptionPanel.addStyleName("xNewFilePreview");
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
     * The list of attachments.
     */
    private ListBox attachmentsList = new ListBox();

    /**
     * Specifies whether the new attachment option should be shown on top or on bottom of the list.
     */
    private boolean newOptionOnTop;

    /**
     * Builds a selector from the attachments of the specified page.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public CurrentPageAttachmentSelectorWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;
        mainPanel.addStyleName("xAttachmentsSelector");
        // create an empty attachments list
        mainPanel.add(attachmentsList);
        // put the new attachment option on top
        newOptionOnTop = true;
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
                refreshAttachmentsList(cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Reloads the list of attachments previews in asynchronous manner.
     * 
     * @param cb the callback to handle server call
     */
    private void refreshAttachmentsList(final AsyncCallback< ? > cb)
    {
        WysiwygService.Singleton.getInstance().getAttachments(editedResource.getWiki(), editedResource.getSpace(),
            editedResource.getPage(), new AsyncCallback<List<Attachment>>()
            {
                public void onSuccess(List<Attachment> result)
                {
                    fillAttachmentsList(result);
                    cb.onSuccess(null);
                }

                public void onFailure(Throwable caught)
                {
                    cb.onFailure(caught);
                }
            });
    }

    /**
     * Fills the preview list with attachments.
     * 
     * @param attachments the list of attachments to build the preview for
     */
    private void fillAttachmentsList(List<Attachment> attachments)
    {
        String oldSelection = null;
        if (!StringUtils.isEmpty(getData().getReference())) {
            ResourceName r = new ResourceName(getData().getReference(), true);
            oldSelection = r.getFile();
        } else if (attachmentsList.getSelectedItem() != null
            && !(attachmentsList.getSelectedItem().getWidget(0) instanceof NewAttachmentOptionWidget)) {
            oldSelection =
                ((AttachmentPreviewWidget) attachmentsList.getSelectedItem().getWidget(0)).getAttachment()
                    .getFilename();
        }
        attachmentsList.clear();
        for (Attachment attach : attachments) {
            ListItem newItem = new ListItem();
            newItem.add(new AttachmentPreviewWidget(attach));
            attachmentsList.addItem(newItem);
            // preserve selection
            if (oldSelection != null && oldSelection.equals(attach.getFilename())) {
                attachmentsList.setSelectedItem(newItem);
            }
        }
        ListItem newOptionListItem = new ListItem();
        newOptionListItem.add(new NewAttachmentOptionWidget());
        if (newOptionOnTop) {
            attachmentsList.insertItem(newOptionListItem, 0);
        } else {
            attachmentsList.addItem(newOptionListItem);
        }
        if (oldSelection == null) {
            attachmentsList.setSelectedItem(newOptionListItem);
        }
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
    public String getNextStep()
    {
        // check out the selection
        if (attachmentsList.getSelectedItem() != null
            && attachmentsList.getSelectedItem().getWidget(0) instanceof NewAttachmentOptionWidget) {
            return LinkWizardSteps.ATTACHUPLOAD.toString();
        }
        return LinkWizardSteps.WIKIPAGECONFIG.toString();
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
    public void onCancel(AsyncCallback<Boolean> async)
    {
        // nothing special
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        AttachmentPreviewWidget selectedOption =
            (AttachmentPreviewWidget) (attachmentsList.getSelectedItem() != null ? attachmentsList.getSelectedItem()
                .getWidget(0) : null);
        if (selectedOption == null) {
            Window.alert(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
            return;
        }
        if (selectedOption instanceof NewAttachmentOptionWidget) {
            // new file option, let's setup the attachment link data accordingly
            getData().setWiki(editedResource.getWiki());
            getData().setSpace(editedResource.getSpace());
            getData().setPage(editedResource.getPage());
            async.onSuccess(true);
        } else {
            // check if attachment changed
            boolean changedAttachment = true;
            ResourceName editedAttach = new ResourceName(getData().getReference(), true);
            if (!StringUtils.isEmpty(getData().getReference())
                && editedAttach.getFile().equals(selectedOption.getAttachment().getFilename())) {
                changedAttachment = false;
            }
            if (changedAttachment) {
                // existing file option, set up the LinkConfig
                // attachment reference has to be relative to the currently edited page
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedOption.getAttachment().getReference(), true);
                String attachmentRef = "attach:" + ref.getRelativeTo(editedResource).toString();
                String attachmentURL = selectedOption.getAttachment().getDownloadUrl();
                getData().setReference(attachmentRef);
                getData().setUrl(attachmentURL);
            }
            async.onSuccess(true);
        }
    }
}
