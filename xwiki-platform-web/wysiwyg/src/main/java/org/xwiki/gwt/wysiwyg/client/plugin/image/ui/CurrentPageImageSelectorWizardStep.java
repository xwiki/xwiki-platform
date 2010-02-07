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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardSteps;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractListSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;
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
public class CurrentPageImageSelectorWizardStep extends AbstractListSelectorWizardStep<ImageConfig, Attachment>
    implements SelectionHandler<ListItem<Attachment>>
{
    /**
     * The currently edited resource (the currently edited page).
     */
    private ResourceName editedResource;

    /**
     * The currentPage for which to show the images selector.
     */
    private ResourceName currentPage;

    /**
     * Fake list item to add to the end of the list, for styling purposes, to clear the floats of the image previews
     * list items.
     */
    private final ListItem<Attachment> clearFloatsListItem;

    /**
     * The service used to retrieve the list of image attachments.
     */
    private WikiServiceAsync wikiService;

    /**
     * Builds a selector from the images of the specified current page to edit the specified resource.
     * 
     * @param currentPage the page to currently show images for
     * @param editedResource the currently edited resource (currentPage for which editing is done)
     */
    public CurrentPageImageSelectorWizardStep(ResourceName currentPage, ResourceName editedResource)
    {
        getMainPanel().addStyleName("xImagesSelector");
        this.editedResource = editedResource;
        this.currentPage = currentPage;

        clearFloatsListItem = new ListItem<Attachment>();
        clearFloatsListItem.setStyleName("clearfloats");

        getList().addSelectionHandler(this);
    }

    /**
     * Builds a selector from the images of the specified current page.
     * 
     * @param currentPage the currently edited page
     */
    public CurrentPageImageSelectorWizardStep(ResourceName currentPage)
    {
        this(currentPage, currentPage);
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
        wikiService.getImageAttachments(currentPage.getWiki(), currentPage.getSpace(), currentPage.getPage(), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillList(List<Attachment> itemsList)
    {
        super.fillList(itemsList);

        getList().addItem(clearFloatsListItem);
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        // check out the selection
        if (getSelectedItem() != null && getSelectedItem().getData() == null) {
            return ImageWizardSteps.IMAGE_UPLOAD.toString();
        }
        return ImageWizardSteps.IMAGE_CONFIG.toString();
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
    @Override
    protected void saveSelectedValue()
    {
        Attachment selectedImage = getSelectedItem().getData();
        if (selectedImage == null) {
            // new image option, let's setup the image data accordingly, to be handled by the file upload step
            getData().setWiki(currentPage.getWiki());
            getData().setSpace(currentPage.getSpace());
            getData().setPage(currentPage.getPage());
        } else {
            // check if attachment changed
            boolean changedFile = true;
            ResourceName editedFile = new ResourceName(getData().getReference(), true);
            if (!StringUtils.isEmpty(getData().getReference())
                && editedFile.getFile().equals(selectedImage.getFileName())) {
                changedFile = false;
            }
            if (changedFile) {
                // existing file option, set up the ImageConfig
                // image reference has to be relative to the currently edited currentPage
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedImage.getReference(), true);
                getData().setReference(ref.getRelativeTo(editedResource).toString());
                getData().setImageURL(selectedImage.getURL());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListItem<Attachment> getListItem(Attachment image)
    {
        ListItem<Attachment> imageItem = new ListItem<Attachment>();
        imageItem.setData(image);
        Image htmlImage = new Image(image.getURL() + "?width=135");
        htmlImage.setTitle(image.getFileName());
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
     * Updates the current page for which this wizard step provides selection interface. However, it <strong>will
     * not</strong> update the displayed images, this will have to be done manually through
     * {@link #refreshList(AsyncCallback)}.
     * 
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(ResourceName currentPage)
    {
        this.currentPage = currentPage;
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
     * Injects the wiki service.
     * 
     * @param wikiService the service used to retrieve the list of image attachments
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}
