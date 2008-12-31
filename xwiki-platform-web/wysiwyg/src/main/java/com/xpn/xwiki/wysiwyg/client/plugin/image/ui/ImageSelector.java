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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.widget.PageSelector;
import com.xpn.xwiki.wysiwyg.client.widget.SpaceSelector;
import com.xpn.xwiki.wysiwyg.client.widget.WikiSelector;

/**
 * Widget to display image previews and allow selection of an image, from a page, space or wiki.
 * 
 * @version $Id$
 */
public class ImageSelector extends Composite implements ChangeListener, ClickListener
{
    /**
     * Panel with the existing images previews.
     */
    private FlowPanel imagesPanel;

    /**
     * Selector for the wiki to get images from.
     */
    private WikiSelector wikiSelector;

    /**
     * Selector for the space to get images from.
     */
    private SpaceSelector spaceSelector;

    /**
     * Selector for the page to get images from.
     */
    private PageSelector pageSelector;

    /**
     * The selected image in this image selector.
     */
    private ImagePreviewWidget selectedImage;

    /**
     * Builds an image selector panel with the specified wiki, space and page as start values.
     * 
     * @param currentWiki the current wiki
     * @param currentSpace space of the starting page
     * @param currentPage page name of the starting page
     */
    public ImageSelector(String currentWiki, String currentSpace, String currentPage)
    {
        Panel imageChooserPanel = new FlowPanel();
        imageChooserPanel.addStyleName("xImageChooser");

        Label chooseLabel = new Label(Strings.INSTANCE.fileChooseLabel());
        imageChooserPanel.add(chooseLabel);
        imageChooserPanel.add(getSelectorsPanel(currentWiki, currentSpace, currentPage));
        ScrollPanel containerPanel = new ScrollPanel();
        containerPanel.addStyleName("xImagesContainerPanel");
        imagesPanel = new FlowPanel();
        containerPanel.add(imagesPanel);
        updateImagesPanel(currentWiki, currentSpace, currentPage, null);
        imageChooserPanel.add(containerPanel);

        initWidget(imageChooserPanel);
    }

    /**
     * @param currentWiki the current wiki from which to start selection
     * @param currentSpace the current space from which to start selection
     * @param currentPage the current page from which to start selection
     * @return the panel with the selectors to choose the source for the attachments panel.
     */
    private Panel getSelectorsPanel(final String currentWiki, String currentSpace, String currentPage)
    {
        // create selectors for the page to get images from
        FlowPanel selectorsPanel = new FlowPanel();
        wikiSelector = new WikiSelector();
        spaceSelector = new SpaceSelector(currentWiki);
        pageSelector = new PageSelector(currentWiki, currentSpace);
        wikiSelector.setVisible(false);
        WysiwygService.Singleton.getInstance().isMultiWiki(new AsyncCallback<Boolean>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(Boolean result)
            {
                wikiSelector.setVisible(result);
            }
        });

        wikiSelector.addChangeListener(this);
        spaceSelector.addChangeListener(this);

        Button updateImagesListButton = new Button(Strings.INSTANCE.fileUpdateListButton());
        updateImagesListButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                updateImagesPanel(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(), pageSelector
                    .getSelectedPage(), selectedImage != null ? selectedImage.getImage().getImageFileName() : null);
            }
        });

        selectorsPanel.add(wikiSelector);
        selectorsPanel.add(spaceSelector);
        selectorsPanel.add(pageSelector);
        selectorsPanel.add(updateImagesListButton);

        return selectorsPanel;
    }

    /**
     * Sets the selection to the specified wiki, space and page and reloads the images panel.
     * 
     * @param wiki the wiki to set the selection to
     * @param space the space to set the selection on
     * @param page the page to set the selection on
     * @param fileName the filename of the image to set as currently selected image
     * @param forceRefresh if a refresh should be forced on the list of wikis, spaces, pages in the list boxes
     */
    public void setSelection(String wiki, final String space, final String page, final String fileName,
        final boolean forceRefresh)
    {
        boolean isMultiWiki = wikiSelector.isVisible();
        if (isMultiWiki) {
            if (forceRefresh) {
                wikiSelector.refreshList(wiki, new AsyncCallback<List<String>>()
                {
                    public void onSuccess(List<String> result)
                    {
                        setSpaceSelection(space, page, fileName, true);
                    }

                    public void onFailure(Throwable caught)
                    {
                    }
                });
            } else {
                // just set the selection
                if (!wikiSelector.getSelectedWiki().equals(wiki)) {
                    wikiSelector.setSelectedWiki(wiki);
                    setSpaceSelection(space, page, fileName, true);
                }
            }
        } else {
            setSpaceSelection(space, page, fileName, forceRefresh);
        }
    }

    /**
     * Sets the selection on the specified space triggering the page selector update accordingly.
     * 
     * @param selectedSpace the space to be set as selected
     * @param selectedPage the page to be set as selected
     * @param selectedFile the file to set as selected in the images list
     * @param forceRefresh if a refresh should be forced on the list of wikis, spaces, pages in the list boxes
     */
    private void setSpaceSelection(String selectedSpace, final String selectedPage, final String selectedFile,
        final boolean forceRefresh)
    {
        if (forceRefresh) {
            // refresh the spaces list
            spaceSelector.setWiki(wikiSelector.getSelectedWiki());
            spaceSelector.refreshList(selectedSpace, new AsyncCallback<List<String>>()
            {
                public void onSuccess(List<String> result)
                {
                    setPageSelection(selectedPage, selectedFile, true);
                }

                public void onFailure(Throwable caught)
                {
                }
            });
        } else {
            if (!selectedSpace.equals(spaceSelector.getSelectedSpace())) {
                spaceSelector.setSelectedSpace(selectedSpace);
                setPageSelection(selectedPage, selectedFile, true);
            } else {
                setPageSelection(selectedPage, selectedFile, forceRefresh);
            }
        }
    }

    /**
     * Sets the selection on the specified page, triggering the images panel update accordingly.
     * 
     * @param selectedPage the page to be set as selected
     * @param selectedFile the file to set as selected in the images list
     * @param forceRefresh if a refresh should be forced on the list of wikis, spaces, pages in the list boxes
     */
    private void setPageSelection(String selectedPage, final String selectedFile, boolean forceRefresh)
    {
        if (forceRefresh) {
            pageSelector.setWiki(wikiSelector.getSelectedWiki());
            pageSelector.setSpace(spaceSelector.getSelectedSpace());
            pageSelector.refreshList(selectedPage, new AsyncCallback<List<String>>()
            {
                public void onSuccess(List<String> result)
                {
                    updateImagesPanel(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(), pageSelector
                        .getSelectedPage(), selectedFile);
                }

                public void onFailure(Throwable caught)
                {
                }
            });
        } else {
            if (!selectedPage.equals(pageSelector.getSelectedPage())) {
                pageSelector.setSelectedPage(selectedPage);
            }
            updateImagesPanel(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(), pageSelector
                .getSelectedPage(), selectedFile);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == pageSelector) {
            // nothing
        }
        if (sender == wikiSelector) {
            spaceSelector.setWiki(wikiSelector.getSelectedWiki());
            spaceSelector.refreshList(spaceSelector.getSelectedSpace(), new AsyncCallback<List<String>>()
            {
                public void onFailure(Throwable caught)
                {
                }

                public void onSuccess(List<String> result)
                {
                    pageSelector.setWiki(wikiSelector.getSelectedWiki());
                    pageSelector.setSpace(spaceSelector.getSelectedSpace());
                    pageSelector.refreshList(pageSelector.getSelectedPage());
                }
            });
        }
        if (sender == spaceSelector) {
            pageSelector.setWiki(wikiSelector.getSelectedWiki());
            pageSelector.setSpace(spaceSelector.getSelectedSpace());
            pageSelector.refreshList(pageSelector.getSelectedPage());
        }
    }

    /**
     * Populates the images panel with the images in the passed map.
     * 
     * @param images the list of images given by their URL and corresponding file names.
     * @param selectedImage the currently selected image to set in the built image panel. If no image is to be selected,
     *            this argument should be null.
     */
    public void populateImagesPanel(List<ImageConfig> images, String selectedImage)
    {
        for (ImageConfig imageData : images) {
            final ImagePreviewWidget imageWidget = new ImagePreviewWidget(imageData);
            imageWidget.addClickListener(this);
            imagesPanel.add(imageWidget);
            if (imageData.getImageFileName().equals(selectedImage)) {
                imageWidget.setSelected(true);
                this.selectedImage = imageWidget;
            }
        }
        // Add a div for float clear
        Panel clearPanel = new FlowPanel();
        clearPanel.addStyleName("clearfloats");
        imagesPanel.add(clearPanel);
    }

    /**
     * Fetches the new images list and repopulates the images panel with the new images.
     * 
     * @param selectedWiki the currently selected wiki
     * @param selectedSpace the currently selected space
     * @param selectedPage the currently selected page
     * @param selectedImage the currently selected image, passed by its filename. If no image is to be selected, this
     *            value should be null.
     */
    public void updateImagesPanel(String selectedWiki, String selectedSpace, String selectedPage,
        final String selectedImage)
    {
        imagesPanel.clear();
        // reset selection
        this.selectedImage = null;
        WysiwygService.Singleton.getInstance().getImageAttachments(selectedWiki, selectedSpace, selectedPage,
            new AsyncCallback<List<ImageConfig>>()
            {
                public void onFailure(Throwable caught)
                {
                    imagesPanel.add(new HTML(Strings.INSTANCE.fileListFetchError()));
                }

                public void onSuccess(List<ImageConfig> result)
                {
                    populateImagesPanel(result, selectedImage);
                }
            });
    }

    /**
     * @return the currently selected image from the image panel
     */
    public ImageConfig getSelectedImage()
    {
        if (selectedImage == null) {
            return null;
        }
        return selectedImage.getImage();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender instanceof ImagePreviewWidget) {
            // update the current selection
            // unselect the old image
            if (selectedImage != null) {
                selectedImage.setSelected(false);
            }
            // select the new image
            selectedImage = (ImagePreviewWidget) sender;
            selectedImage.setSelected(true);
        }
    }
}
