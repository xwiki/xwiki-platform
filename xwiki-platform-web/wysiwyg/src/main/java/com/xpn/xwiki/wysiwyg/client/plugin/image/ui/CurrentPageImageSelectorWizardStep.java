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

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
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
import com.xpn.xwiki.wysiwyg.client.widget.VerticalResizePanel;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListenerCollection;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.SourcesNavigationEvents;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

/**
 * Wizard step to select an image from the list of images attached to a page.
 * 
 * @version $Id$
 */
public class CurrentPageImageSelectorWizardStep extends AbstractSelectorWizardStep<ImageConfig> implements
    DoubleClickHandler, KeyUpHandler, SourcesNavigationEvents
{
    /**
     * The style for an field in error.
     */
    private static final String FIELD_ERROR_STYLE = "xFieldError";

    /**
     * The main panel of this wizard step.
     */
    private VerticalResizePanel mainPanel = new VerticalResizePanel();

    /**
     * The currently edited resource (the currently edited page).
     */
    private ResourceName editedResource;

    /**
     * The currentPage for which to show the images selector.
     */
    private ResourceName currentPage;

    /**
     * The list of images.
     */
    private ListBox<Attachment> imageList = new ListBox<Attachment>();

    /**
     * Label to display the selection error in this wizard step.
     */
    private final Label errorLabel = new Label();

    /**
     * Specifies whether the new image option should be shown on top or on bottom of the list.
     */
    private boolean newOptionOnTop;

    /**
     * Navigation listeners to be notified by navigation events from this step. It generates navigation to the next step
     * when an item is double clicked in the list, or enter key is pressed on a selected item.
     */
    private final NavigationListenerCollection navigationListeners = new NavigationListenerCollection();

    /**
     * Builds a selector from the images of the specified current page to edit the specified resource.
     * 
     * @param currentPage the page to currently show images for
     * @param editedResource the currently edited resource (currentPage for which editing is done)
     */
    public CurrentPageImageSelectorWizardStep(ResourceName currentPage, ResourceName editedResource)
    {
        this.editedResource = editedResource;
        this.currentPage = currentPage;
        mainPanel.addStyleName("xImagesSelector");

        Label helpLabel = new Label(Strings.INSTANCE.imageSelectImageHelpLabel());
        helpLabel.addStyleName("xHelpLabel");
        mainPanel.add(helpLabel);

        errorLabel.addStyleName("xImageParameterError");
        errorLabel.setVisible(false);
        mainPanel.add(errorLabel);

        imageList.addKeyUpHandler(this);
        imageList.addDoubleClickHandler(this);
        mainPanel.add(imageList);
        mainPanel.setExpandingWidget(imageList, false);
        // put the new image option on top
        newOptionOnTop = true;
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
    public void init(final Object data, final AsyncCallback< ? > cb)
    {
        hideError();
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
        WysiwygService.Singleton.getInstance().getImageAttachments(currentPage.getWiki(), currentPage.getSpace(),
            currentPage.getPage(), new AsyncCallback<List<Attachment>>()
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
            ResourceName r = new ResourceName(getData().getReference(), true);
            oldSelection = r.getFile();
        } else if (imageList.getSelectedItem() != null && imageList.getSelectedItem().getData() != null) {
            oldSelection = imageList.getSelectedItem().getData().getFilename();
        }
        imageList.clear();
        for (Attachment attach : attachments) {
            ListItem<Attachment> newItem = getImageListItem(attach);
            imageList.addItem(newItem);
            // preserve selection
            if (oldSelection != null && oldSelection.equals(attach.getFilename())) {
                imageList.setSelectedItem(newItem);
            }
        }
        ListItem<Attachment> newOptionListItem = getNewImageListItem();
        if (newOptionOnTop) {
            imageList.insertItem(newOptionListItem, 0);
        } else {
            imageList.addItem(newOptionListItem);
        }
        if (oldSelection == null) {
            imageList.setSelectedItem(newOptionListItem);
        }

        // fake container to clear the floats set for the images preview. It's here exclusively for styling reasons
        ListItem<Attachment> fakeClearListItem = new ListItem<Attachment>();
        fakeClearListItem.setStyleName("clearfloats");
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
        if (imageList.getSelectedItem() != null && imageList.getSelectedItem().getData() == null) {
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
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        hideError();
        if (imageList.getSelectedItem() == null) {
            displayError(Strings.INSTANCE.imageNoImageSelectedError());
            async.onSuccess(false);
            return;
        }
        Attachment selectedImage = imageList.getSelectedItem() != null ? imageList.getSelectedItem().getData() : null;
        if (selectedImage == null) {
            // new image option, let's setup the image data accordingly, to be handled by the file upload step
            getData().setWiki(currentPage.getWiki());
            getData().setSpace(currentPage.getSpace());
            getData().setPage(currentPage.getPage());
            async.onSuccess(true);
        } else {
            // check if attachment changed
            boolean changedFile = true;
            ResourceName editedFile = new ResourceName(getData().getReference(), true);
            if (!StringUtils.isEmpty(getData().getReference())
                && editedFile.getFile().equals(selectedImage.getFilename())) {
                changedFile = false;
            }
            if (changedFile) {
                // existing file option, set up the ImageConfig
                // image reference has to be relative to the currently edited currentPage
                // FIXME: move the reference setting logic in a controller
                ResourceName ref = new ResourceName(selectedImage.getReference(), true);
                getData().setReference(ref.getRelativeTo(editedResource).toString());
                getData().setImageURL(selectedImage.getDownloadUrl());
            }
            async.onSuccess(true);
        }
    }

    /**
     * Creates a list item with an image preview.
     * 
     * @param image the attachment data for the image to preview
     * @return a list item containing a preview of the {@code image}.
     */
    protected ListItem<Attachment> getImageListItem(Attachment image)
    {
        ListItem<Attachment> imageItem = new ListItem<Attachment>();
        imageItem.setData(image);
        Image htmlImage = new Image(image.getDownloadUrl() + "?width=135");
        htmlImage.setTitle(image.getFilename());
        FlowPanel previewPanel = new FlowPanel();
        previewPanel.addStyleName("xImagePreview");
        previewPanel.add(htmlImage);
        imageItem.add(previewPanel);
        return imageItem;
    }

    /**
     * Creates the list item for the new image upload option.
     * 
     * @return a list item with the new image option.
     */
    protected ListItem<Attachment> getNewImageListItem()
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
     * Displays the specified error message and error markers for this wizard step.
     * 
     * @param message the error message to display
     */
    protected void displayError(String message)
    {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        imageList.addStyleName(FIELD_ERROR_STYLE);
        mainPanel.refreshHeights();
    }

    /**
     * Hides the error markers for this wizard step.
     */
    protected void hideError()
    {
        errorLabel.setVisible(false);
        imageList.removeStyleName(FIELD_ERROR_STYLE);
        mainPanel.refreshHeights();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DoubleClickHandler#onDoubleClick(DoubleClickEvent)
     */
    public void onDoubleClick(DoubleClickEvent event)
    {
        if (event.getSource() == imageList && imageList.getSelectedItem() != null) {
            navigationListeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getSource() == imageList && event.getNativeKeyCode() == KeyCodes.KEY_ENTER
            && imageList.getSelectedItem() != null) {
            navigationListeners.fireNavigationEvent(NavigationDirection.NEXT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addNavigationListener(NavigationListener listener)
    {
        navigationListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        navigationListeners.remove(listener);
    }
}
