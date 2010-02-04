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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.widget.PageSelector;
import com.xpn.xwiki.wysiwyg.client.widget.SpaceSelector;
import com.xpn.xwiki.wysiwyg.client.widget.WikiSelector;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * Wizard step to explore and select images from all the pages in the wiki. <br />
 * 
 * @version $Id$
 */
public class ImagesExplorerWizardStep extends AbstractSelectorWizardStep<ImageConfig> implements ChangeHandler,
    SourcesNavigationEvents
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
    private final VerticalResizePanel mainPanel = new VerticalResizePanel();

    /**
     * The image selector for the currently selected page in this wizard step. This will be instantiated every time the
     * list of pages for a selected page needs to be displayed, and the functionality of this aggregator will be
     * delegated to it.
     */
    private CurrentPageImageSelectorWizardStep pageWizardStep;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Builds an image explorer with the default selection on the passed resource.
     * 
     * @param editedResource the resource edited by the wizard in which this wizard step appears (the page currently
     *            edited with the wysiwyg)
     * @param displayWikiSelector whether this explorer should show the selector to choose an image from a different
     *            wiki or not
     * @param wikiService the service used to access the wiki
     */
    public ImagesExplorerWizardStep(ResourceName editedResource, boolean displayWikiSelector,
        WikiServiceAsync wikiService)
    {
        this.editedResource = editedResource;
        this.wikiService = wikiService;

        Label helpLabel = new Label(Strings.INSTANCE.imageSelectImageLocationHelpLabel());
        helpLabel.addStyleName("xHelpLabel");
        mainPanel.add(helpLabel);
        // initialize selectors, mainPanel
        mainPanel.addStyleName("xImagesExplorer");
        this.displayWikiSelector = displayWikiSelector;
        mainPanel.add(getSelectorsPanel(editedResource.getWiki(), editedResource.getSpace()));
        pageWizardStep = new CurrentPageImageSelectorWizardStep(editedResource);
        pageWizardStep.setWikiService(wikiService);
        mainPanel.add(pageWizardStep.display());
        mainPanel.setExpandingWidget(pageWizardStep.display(), true);
    }

    /**
     * @param currentWiki the current wiki from which to start selection
     * @param currentSpace the current space from which to start selection
     * @return the panel with the selectors to choose the source for the attachments panel.
     */
    private Panel getSelectorsPanel(final String currentWiki, String currentSpace)
    {
        // create selectors for the page to get images from
        FlowPanel selectorsPanel = new FlowPanel();
        wikiSelector = new WikiSelector();
        wikiSelector.setWikiService(wikiService);
        spaceSelector = new SpaceSelector(currentWiki);
        spaceSelector.setWikiService(wikiService);
        pageSelector = new PageSelector(currentWiki, currentSpace);
        pageSelector.setWikiService(wikiService);

        // hide this selector by default, until we get to update it from the server
        wikiSelector.setVisible(false);

        wikiSelector.addChangeHandler(this);
        spaceSelector.addChangeHandler(this);

        Button updateImagesListButton = new Button(Strings.INSTANCE.imageUpdateListButton());
        updateImagesListButton.addClickHandler(new ClickHandler()
        {
            public void onClick(ClickEvent event)
            {
                initCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(),
                    pageSelector.getSelectedPage(), null), null);
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
     * @param cb callback to handle asynchronous fill of the wiki, space, page list boxes
     */
    public void setSelection(final String wiki, final String space, final String page, final String fileName,
        final boolean forceRefresh, final AsyncCallback< ? > cb)
    {
        wikiService.isMultiWiki(new AsyncCallback<Boolean>()
        {
            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            }

            public void onSuccess(Boolean result)
            {
                if (result) {
                    setWikiSelection(wiki, space, page, fileName, forceRefresh, cb);
                } else {
                    setSpaceSelection(space, page, fileName, forceRefresh, cb);
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
     * @param cb callback to handle asynchronous initialization of the wikis list
     */
    private void setWikiSelection(String selectedWiki, final String space, final String page, final String fileName,
        final boolean forceRefresh, final AsyncCallback< ? > cb)
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
            setSpaceSelection(space, page, fileName, true, cb);
        } else {
            wikiSelector.setVisible(true);
            if (forceRefresh) {
                wikiSelector.refreshList(selectedWiki, new AsyncCallback<List<String>>()
                {
                    public void onSuccess(List<String> result)
                    {
                        setSpaceSelection(space, page, fileName, true, cb);
                    }

                    public void onFailure(Throwable caught)
                    {
                        if (cb != null) {
                            cb.onFailure(caught);
                        }
                    }
                });
            } else {
                // just set the selection
                if (!wikiSelector.getSelectedWiki().equals(selectedWiki)) {
                    wikiSelector.setSelectedWiki(selectedWiki);
                    setSpaceSelection(space, page, fileName, true, cb);
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
     * @param cb callback to handle asynchronous initialization of the spaces list
     */
    private void setSpaceSelection(String selectedSpace, final String selectedPage, final String selectedFile,
        final boolean forceRefresh, final AsyncCallback< ? > cb)
    {
        if (forceRefresh) {
            // refresh the spaces list
            spaceSelector.setWiki(wikiSelector.getSelectedWiki());
            spaceSelector.refreshList(selectedSpace, new AsyncCallback<List<String>>()
            {
                public void onSuccess(List<String> result)
                {
                    setPageSelection(selectedPage, selectedFile, true, cb);
                }

                public void onFailure(Throwable caught)
                {
                    if (cb != null) {
                        cb.onFailure(caught);
                    }
                }
            });
        } else {
            if (!selectedSpace.equals(spaceSelector.getSelectedSpace())) {
                spaceSelector.setSelectedSpace(selectedSpace);
                setPageSelection(selectedPage, selectedFile, true, cb);
            } else {
                setPageSelection(selectedPage, selectedFile, forceRefresh, cb);
            }
        }
    }

    /**
     * Sets the selection on the specified page, triggering the images panel update accordingly.
     * 
     * @param selectedPage the page to be set as selected
     * @param selectedFile the file to set as selected in the images list
     * @param forceRefresh if a refresh should be forced on the list of wikis, spaces, pages in the list boxes
     * @param cb callback to handle asynchronous initialization of the pages list
     */
    private void setPageSelection(String selectedPage, final String selectedFile, boolean forceRefresh,
        final AsyncCallback< ? > cb)
    {
        if (forceRefresh) {
            pageSelector.setWiki(wikiSelector.getSelectedWiki());
            pageSelector.setSpace(spaceSelector.getSelectedSpace());
            pageSelector.refreshList(selectedPage, new AsyncCallback<List<String>>()
            {
                public void onSuccess(List<String> result)
                {
                    initCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(),
                        pageSelector.getSelectedPage(), selectedFile), cb);
                }

                public void onFailure(Throwable caught)
                {
                    if (cb != null) {
                        cb.onFailure(caught);
                    }
                }
            });
        } else {
            if (!selectedPage.equals(pageSelector.getSelectedPage())) {
                pageSelector.setSelectedPage(selectedPage);
            }
            initCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(),
                pageSelector.getSelectedPage(), selectedFile), cb);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeHandler#onChange(ChangeEvent)
     */
    public void onChange(ChangeEvent event)
    {
        if (event.getSource() == wikiSelector) {
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
        } else if (event.getSource() == spaceSelector) {
            pageSelector.setWiki(wikiSelector.getSelectedWiki());
            pageSelector.setSpace(spaceSelector.getSelectedSpace());
            pageSelector.refreshList(pageSelector.getSelectedPage());
        }
    }

    /**
     * Initializes and displays the page selector panel for the currently selected resource.
     * 
     * @param resource the resource to display the selector panel for
     * @param cb the callback to handle asynchronous initialization
     */
    protected void initCurrentPage(ResourceName resource, final AsyncCallback< ? > cb)
    {
        pageWizardStep.setCurrentPage(resource);
        mainPanel.addStyleName(STYLE_LOADING);
        pageWizardStep.init(getData(), new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                onCurrenPageInitialization();
                if (cb != null) {
                    cb.onSuccess(null);
                }
            }

            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                } else {
                    showCurrentPageInitializationError();
                }

            }
        });
    }

    /**
     * Helper function to handle the error on current page initialization: display an error message in the reserved
     * panel.
     */
    private void showCurrentPageInitializationError()
    {
        mainPanel.removeStyleName(STYLE_LOADING);
        Label error = new Label(Strings.INSTANCE.linkErrorLoadingData());
        error.addStyleName("errormessage");
        mainPanel.remove(pageWizardStep.display());
        mainPanel.add(error);
    }

    /**
     * Helper function to handle the success on initialization of the current page wizard step.
     */
    private void onCurrenPageInitialization()
    {
        // if the current page's display is not there (maybe an error before removed it), remove the error and add
        if (mainPanel.getWidgetIndex(pageWizardStep.display()) < 0) {
            // FIXME: the error panel shouldn't be identified by its position!
            mainPanel.remove(mainPanel.getWidgetCount() - 1);
            mainPanel.add(pageWizardStep.display());
        }
        mainPanel.removeStyleName(STYLE_LOADING);
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
    protected void initializeSelection(AsyncCallback< ? > cb)
    {
        if (!StringUtils.isEmpty(getData().getReference())
            || (wikiSelector.getSelectedWiki() == null && spaceSelector.getSelectedSpace() == null && pageSelector
                .getSelectedPage() == null)) {
            // if it's the first display (i.e. no selection in the wikiselector, spaceSelector or pageSelector) or an
            // image needs to be edited, refresh selectors and page list
            ResourceName r = new ResourceName(getData().getReference(), true);
            ResourceName resolved = r.resolveRelativeTo(editedResource);
            setSelection(resolved.getWiki(), resolved.getSpace(), resolved.getPage(), resolved.getFile(), true, cb);
        } else {
            // just initialize the step for the space, page, wiki selection in the selectors. I.e. preserve last
            // selection
            initCurrentPage(new ResourceName(wikiSelector.getSelectedWiki(), spaceSelector.getSelectedSpace(),
                pageSelector.getSelectedPage(), null), cb);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel()
    {
        pageWizardStep.onCancel();
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        pageWizardStep.onSubmit(async);
    }

    /**
     * {@inheritDoc}
     */
    public void addNavigationListener(NavigationListener listener)
    {
        pageWizardStep.addNavigationListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        pageWizardStep.removeNavigationListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActive()
    {
        if (displayWikiSelector) {
            wikiSelector.setFocus(true);
        } else {
            spaceSelector.setFocus(true);
        }
    }
}
