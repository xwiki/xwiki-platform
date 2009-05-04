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
package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.ListBox;
import com.xpn.xwiki.wysiwyg.client.widget.ListItem;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

/**
 * Wizard step to select an image from the list of images attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageImageSelectorWizardStep extends AbstractSelectorWizardStep<ImageConfig>
{
    /**
     * Fake image preview widget to hold the option of attaching a new image.
     */
    private static class NewImageOptionWidget extends ImagePreviewWidget
    {
        /**
         * Default constructor.
         */
        public NewImageOptionWidget()
        {
            super(null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Widget getUI()
        {
            FlowPanel newOptionPanel = new FlowPanel();
            newOptionPanel.addStyleName("xNewImagePreview");
            Label newOptionLabel = new Label(Strings.INSTANCE.imageUploadNewFileLabel());
            newOptionPanel.add(newOptionLabel);
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
     * The list of images.
     */
    private ListBox imageList = new ListBox();

    /**
     * Specifies whether the new image option should be shown on top or on bottom of the list.
     */
    private boolean newOptionOnTop;

    /**
     * Builds a selector from the images of the specified page.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public CurrentPageImageSelectorWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;
        mainPanel.addStyleName("xImagesSelector");
        // create an empty images list
        mainPanel.add(imageList);
        // put the new image option on top
        newOptionOnTop = true;
    }

    /**
     * {@inheritDoc}
     */
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
     * Reloads the list of image previews in asynchronous manner.
     * 
     * @param cb the callback to handle server call
     */
    private void refreshAttachmentsList(final AsyncCallback< ? > cb)
    {
        WysiwygService.Singleton.getInstance().getImageAttachments(editedResource.getWiki(), editedResource.getSpace(),
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
     * Fills the preview list with image preview widgets.
     * 
     * @param attachments the list of images to build the preview for
     */
    private void fillAttachmentsList(List<Attachment> attachments)
    {
        String oldSelection = null;
        if (!StringUtils.isEmpty(getData().getReference())) {
            ResourceName r = new ResourceName();
            r.fromString(getData().getReference(), true);
            oldSelection = r.getFile();
        } else if (imageList.getSelectedItem() != null
            && !(imageList.getSelectedItem().getWidget(0) instanceof NewImageOptionWidget)) {
            oldSelection =
                ((ImagePreviewWidget) imageList.getSelectedItem().getWidget(0)).getAttachment().getFilename();
        }
        imageList.clear();
        for (Attachment attach : attachments) {
            ListItem newItem = new ListItem();
            newItem.add(new ImagePreviewWidget(attach));
            imageList.addItem(newItem);
            // preserve selection
            if (oldSelection != null && oldSelection.equals(attach.getFilename())) {
                imageList.setSelectedItem(newItem);
            }
        }
        ListItem newOptionListItem = new ListItem();
        newOptionListItem.add(new NewImageOptionWidget());
        if (newOptionOnTop) {
            imageList.insertItem(newOptionListItem, 0);
        } else {
            imageList.addItem(newOptionListItem);
        }
        if (oldSelection == null) {
            imageList.setSelectedItem(newOptionListItem);
        }

        // fake container to clear the floats set for the images preview. It's here exclusively for styling reasons
        ListItem fakeClearListItem = new ListItem();
        fakeClearListItem.addStyleName("clearfloats");
        imageList.addItem(fakeClearListItem);
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
        if (imageList.getSelectedItem() != null
            && imageList.getSelectedItem().getWidget(0) instanceof NewImageOptionWidget) {
            return ImageWizardSteps.IMAGEUPLOAD.toString();
        }
        return ImageWizardSteps.IMAGECONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.imageSelectImageTitle();
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
        ImagePreviewWidget selectedOption =
            (ImagePreviewWidget) 
                (imageList.getSelectedItem() != null ? imageList.getSelectedItem().getWidget(0) : null);
        if (selectedOption == null) {
            Window.alert(Strings.INSTANCE.imageNoImageSelectedError());
            async.onSuccess(false);
            return;
        }
        if (selectedOption instanceof NewImageOptionWidget) {
            // new image option, let's setup the image data accordingly, to be handled by the file upload step
            getData().setWiki(editedResource.getWiki());
            getData().setSpace(editedResource.getSpace());
            getData().setPage(editedResource.getPage());
            async.onSuccess(true);
        } else {
            // existing file option, set up the ImageConfig
            getData().setReference(selectedOption.getAttachment().getReference());
            getData().setImageURL(selectedOption.getAttachment().getDownloadUrl());
            async.onSuccess(true);
        }
    }
}
