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
package org.xwiki.gwt.wysiwyg.client.plugin.image.ui;

import java.util.List;

import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardStep;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractEntityListSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference.ResourceType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * Wizard step to select an image from the list of images attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageImageSelectorWizardStep extends AbstractEntityListSelectorWizardStep<ImageConfig, Attachment>
    implements SelectionHandler<ListItem<Attachment>>
{
    /**
     * Fake list item to add to the end of the list, for styling purposes, to clear the floats of the image previews
     * list items.
     */
    private final ListItem<Attachment> clearFloatsListItem;

    /**
     * Flag indicating which end point of the link is the current page.
     */
    private final boolean useLinkDestination;

    /**
     * The service used to fetch the list of images attached to the current page.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new image selector that displays the images attached to the link destination page.
     * 
     * @param wikiService the service used to retrieve the list of image attachments
     * @param useLinkDestination {@code true} to consider that the link destination is the current page, {@code false}
     *            to consider that the link origin is the current page
     */
    public CurrentPageImageSelectorWizardStep(WikiServiceAsync wikiService, boolean useLinkDestination)
    {
        this.wikiService = wikiService;
        this.useLinkDestination = useLinkDestination;
        setStepTitle(Strings.INSTANCE.imageSelectImageTitle());

        display().addStyleName("xImagesSelector");

        clearFloatsListItem = new ListItem<Attachment>();
        clearFloatsListItem.setStyleName("clearfloats");

        getList().addSelectionHandler(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectHelpLabel()
    {
        return Strings.INSTANCE.imageSelectImageHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSelectErrorMessage()
    {
        return Strings.INSTANCE.imageNoImageSelectedError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData(AsyncCallback<List<Attachment>> callback)
    {
        EntityReference currentPage =
            useLinkDestination ? getData().getDestination().getEntityReference() : getData().getOrigin();
        wikiService.getImageAttachments(new WikiPageReference(currentPage), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> fillList(List<Attachment> attachments, Attachment selectedAttachment)
    {
        ListItem<Attachment> selectedItem = super.fillList(attachments, selectedAttachment);
        getList().addItem(clearFloatsListItem);
        return selectedItem;
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        if (getSelectedItem() != null && getSelectedItem().getData() == null) {
            return ImageWizardStep.IMAGE_UPLOAD.toString();
        }
        return ImageWizardStep.IMAGE_CONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getListItem(Attachment image)
    {
        ListItem<Attachment> imageItem = new ListItem<Attachment>();
        imageItem.setData(image);
        Image htmlImage = new Image(image.getUrl() + "?width=135");
        htmlImage.setTitle(new AttachmentReference(image.getReference()).getFileName());
        FlowPanel previewPanel = new FlowPanel();
        previewPanel.addStyleName("xImagePreview");
        previewPanel.add(htmlImage);
        imageItem.add(previewPanel);
        return imageItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getNewOptionListItem()
    {
        ListItem<Attachment> newImageOption = new ListItem<Attachment>();
        newImageOption.setData(null);
        FlowPanel newOptionPanel = new FlowPanel();
        newOptionPanel.addStyleName("xNewImagePreview");
        Label newOptionLabel = new Label(Strings.INSTANCE.imageUploadNewFileLabel());
        newOptionPanel.add(newOptionLabel);
        newImageOption.add(newOptionPanel);
        return newImageOption;
    }

    /**
     * {@inheritDoc}
     */
    public void onSelection(SelectionEvent<ListItem<Attachment>> event)
    {
        // if the selection is the clear floats fake item, move it to the last item
        if (event.getSelectedItem() == clearFloatsListItem) {
            // it's the fake item, select the last item in the list
            getList().setSelectedItem(getList().getItem(getList().getItemCount() - 2));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractEntityListSelectorWizardStep#saveSelectedValue(AsyncCallback)
     */
    @Override
    protected void saveSelectedValue(final AsyncCallback<Boolean> async)
    {
        // Make sure the resource type is ATTACHMENT since this wizard step selects an attachment.
        getData().getDestination().setType(ResourceType.ATTACHMENT);
        super.saveSelectedValue(async);
    }
}
