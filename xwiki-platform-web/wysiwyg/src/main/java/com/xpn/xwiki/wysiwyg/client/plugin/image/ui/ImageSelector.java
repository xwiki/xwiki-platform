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
        updateImagesPanel(currentWiki, currentSpace, currentPage);
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
        spaceSelector.refreshList(currentSpace);
        pageSelector = new PageSelector(currentWiki, currentSpace);
        pageSelector.refreshList(currentPage);
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
                if (result) {
                    wikiSelector.refreshList(currentWiki);
                }
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
                    .getSelectedPage());
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
     */
    public void setSelection(String wiki, final String space, final String page)
    {
        boolean isMultiWiki = wikiSelector.isVisible();
        if (isMultiWiki && !wikiSelector.getSelectedWiki().equals(wiki)) {
            wikiSelector.setSelectedWiki(wiki);
            spaceSelector.setWiki(wikiSelector.getSelectedWiki());
            spaceSelector.refreshList(space, new AsyncCallback<List<String>>()
            {
                public void onFailure(Throwable caught)
                {
                }

                public void onSuccess(List<String> result)
                {
                    pageSelector.setWiki(wikiSelector.getSelectedWiki());
                    pageSelector.setSpace(spaceSelector.getSelectedSpace());
                    if (!spaceSelector.getSelectedSpace().equals(space)) {
                        pageSelector.refreshList(page);
                    } else {
                        // just set page selector selection
                        pageSelector.setSelectedPage(page);
                    }
                    updateImagesPanel(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(), pageSelector
                        .getSelectedPage());
                }
            });
        } else {
            // just set selection on space
            if (!spaceSelector.getSelectedSpace().equals(space)) {
                // just set the space selection
                spaceSelector.setSelectedSpace(space);
                pageSelector.setSpace(space);
                pageSelector.refreshList(page);
            } else {
                // just set selection on page
                pageSelector.setSelectedPage(page);
            }
            updateImagesPanel(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(), pageSelector
                .getSelectedPage());
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
     */
    public void populateImagesPanel(List<ImageConfig> images)
    {
        for (ImageConfig imageData : images) {
            final ImagePreviewWidget imageWidget = new ImagePreviewWidget(imageData);
            imageWidget.addClickListener(this);
            imagesPanel.add(imageWidget);
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
     */
    public void updateImagesPanel(String selectedWiki, String selectedSpace, String selectedPage)
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
                    populateImagesPanel(result);
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
