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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.PageSelector;
import com.xpn.xwiki.wysiwyg.client.widget.SpaceSelector;
import com.xpn.xwiki.wysiwyg.client.widget.WikiSelector;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;

/**
 * Wizard step to explore and select images from all the pages in the wiki.
 * 
 * @version $Id$
 */
public class ImagesExplorerWizardStep extends AbstractSelectorWizardStep<ImageConfig> implements ChangeListener
{
    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_LOADING = "loading";

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
     * The current resource edited by this wizard step.
     */
    private ResourceName editedResource;

    /**
     * Flag to mark whether this explorer should show the selector to choose an image from a different wiki or not.
     */
    private boolean displayWikiSelector;

    /**
     * The main panel of this widget.
     */
    private final FlowPanel mainPanel = new FlowPanel();

    /**
     * The image selector for the currently selected page in this wizard step. This will be instantiated every time the
     * list of pages for a selected page needs to be displayed, and the functionality of this aggregator will be
     * delegated to it.
     */
    private CurrentPageImageSelectorWizardStep pageWizardStep;

    /**
     * Builds an image explorer with the default selection on the passed resource.
     * 
     * @param editedResource the resource edited by the wizard in which this wizard step appears (the page currently
     *            edited with the wysiwyg)
     * @param displayWikiSelector whether this explorer should show the selector to choose an image from a different
     *            wiki or not
     */
    public ImagesExplorerWizardStep(ResourceName editedResource, boolean displayWikiSelector)
    {
        this.editedResource = editedResource;
        // initialize selectors, mainPanel
        mainPanel.addStyleName("xImagesExplorer");
        this.displayWikiSelector = displayWikiSelector;
        mainPanel.add(getSelectorsPanel(editedResource.getWiki(), editedResource.getSpace(), editedResource.getPage()));
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

        // hide this selector by default, until we get to update it from the server
        wikiSelector.setVisible(false);

        wikiSelector.addChangeListener(this);
        spaceSelector.addChangeListener(this);

        Button updateImagesListButton = new Button(Strings.INSTANCE.imageUpdateListButton());
        updateImagesListButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                initAndDisplayCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector
                    .getSelectedSpace(), pageSelector.getSelectedPage(), null));
            }
        });

        selectorsPanel.add(wikiSelector);
        selectorsPanel.add(spaceSelector);
        selectorsPanel.add(pageSelector);
        selectorsPanel.add(updateImagesListButton);
        selectorsPanel.addStyleName("xPageChooser");

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
    public void setSelection(final String wiki, final String space, final String page, final String fileName,
        final boolean forceRefresh)
    {
        WysiwygService.Singleton.getInstance().isMultiWiki(new AsyncCallback<Boolean>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(Boolean result)
            {
                if (result) {
                    setWikiSelection(wiki, space, page, fileName, forceRefresh);
                } else {
                    setSpaceSelection(space, page, fileName, forceRefresh);
                }
            }
        });
    }

    /**
     * Sets the selection on the specified wiki, triggering the space selector update accordingly.
     * 
     * @param selectedWiki the wiki to set as selected
     * @param space the space to set as selected
     * @param page the page to set as selected
     * @param fileName the file to set as selected
     * @param forceRefresh if a refresh should be forced on the list of wikis, spaces, pages in the list boxes
     */
    private void setWikiSelection(String selectedWiki, final String space, final String page, final String fileName,
        final boolean forceRefresh)
    {
        if (!displayWikiSelector) {
            // if the wiki selector doesn't need to be displayed, add the edited resource wiki as the default selected
            // option so that it's always returned on getSelectedWiki()
            wikiSelector.clear();
            if (!StringUtils.isEmpty(editedResource.getWiki())) {
                wikiSelector.addItem(editedResource.getWiki());
                wikiSelector.setSelectedIndex(0);
            }
            // but keep it invisible
            wikiSelector.setVisible(false);
            // set the space selection further
            setSpaceSelection(space, page, fileName, true);
        } else {
            wikiSelector.setVisible(true);
            if (forceRefresh) {
                wikiSelector.refreshList(selectedWiki, new AsyncCallback<List<String>>()
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
                if (!wikiSelector.getSelectedWiki().equals(selectedWiki)) {
                    wikiSelector.setSelectedWiki(selectedWiki);
                    setSpaceSelection(space, page, fileName, true);
                }
            }
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
                    initAndDisplayCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector
                        .getSelectedSpace(), pageSelector.getSelectedPage(), selectedFile));
                }

                public void onFailure(Throwable caught)
                {
                }
            });
        } else {
            if (!selectedPage.equals(pageSelector.getSelectedPage())) {
                pageSelector.setSelectedPage(selectedPage);
            }
            initAndDisplayCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(),
                spaceSelector.getSelectedSpace(), pageSelector.getSelectedPage(), selectedFile));
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
     * Initializes and displays the page selector panel for the currently selected resource.
     * 
     * @param resource the resource to display the selector panel for
     */
    protected void initAndDisplayCurrentPage(ResourceName resource)
    {
        pageWizardStep = new CurrentPageImageSelectorWizardStep(resource);
        if (mainPanel.getWidgetCount() > 1) {
            mainPanel.remove(1);
        }
        mainPanel.addStyleName(STYLE_LOADING);
        pageWizardStep.init(getData(), new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                mainPanel.removeStyleName(STYLE_LOADING);
                mainPanel.add(pageWizardStep.display());
            };

            public void onFailure(Throwable caught)
            {
                mainPanel.removeStyleName(STYLE_LOADING);
                Label error = new Label(Strings.INSTANCE.linkErrorLoadingData());
                error.addStyleName("errormessage");
                mainPanel.add(error);
            }
        });
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
        return pageWizardStep.getNextStep();
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
    protected void initializeSelection()
    {
        if (!StringUtils.isEmpty(getData().getReference()) || pageWizardStep == null) {
            // if it's the first display (i.e. no pageWizardStep) or an image needs to be edited, refresh selectors and
            // page list
            ResourceName r = new ResourceName();
            r.fromString(getData().getReference(), true);
            ResourceName resolved = r.resolveRelativeTo(editedResource);
            setSelection(resolved.getWiki(), resolved.getSpace(), resolved.getPage(), resolved.getFile(), true);
        } else {
            // just initialize the step for the space, page, wiki selection in the selectors. I.e. preserve last
            // selection
            initAndDisplayCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(),
                spaceSelector.getSelectedSpace(), pageSelector.getSelectedPage(), null));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        pageWizardStep.onCancel(async);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        pageWizardStep.onSubmit(async);
    }
}
