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
import org.xwiki.gwt.user.client.ui.VerticalResizePanel;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.widget.PageSelector;
import org.xwiki.gwt.wysiwyg.client.widget.SpaceSelector;
import org.xwiki.gwt.wysiwyg.client.widget.WikiSelector;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractSelectorWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

/**
 * Wizard step to explore and select images from all the pages in the wiki.
 * 
 * @version $Id$
 */
public class ImagesExplorerWizardStep extends AbstractSelectorWizardStep<EntityLink<ImageConfig>> implements
    ChangeHandler, SourcesNavigationEvents
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
     * Flag to mark whether this explorer should show the selector to choose an image from a different wiki or not.
     */
    private boolean displayWikiSelector;

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
     * @param displayWikiSelector whether this explorer should show the selector to choose an image from a different
     *            wiki or not
     * @param wikiService the service used to access the wiki
     */
    public ImagesExplorerWizardStep(boolean displayWikiSelector, WikiServiceAsync wikiService)
    {
        super(new VerticalResizePanel());

        this.wikiService = wikiService;
        setStepTitle(Strings.INSTANCE.imageSelectImageTitle());

        Label helpLabel = new Label(Strings.INSTANCE.imageSelectImageLocationHelpLabel());
        helpLabel.addStyleName("xHelpLabel");
        display().add(helpLabel);
        // initialize selectors, mainPanel
        display().addStyleName("xImagesExplorer");
        this.displayWikiSelector = displayWikiSelector;
        display().add(getSelectorsPanel());
        pageWizardStep = new CurrentPageImageSelectorWizardStep(wikiService, true);
        display().add(pageWizardStep.display());
        display().setExpandingWidget(pageWizardStep.display(), true);
    }

    /**
     * @return the panel with the selectors to choose the source for the attachments panel
     */
    private Panel getSelectorsPanel()
    {
        // create selectors for the page to get images from
        FlowPanel selectorsPanel = new FlowPanel();

        if (displayWikiSelector) {
            wikiSelector = new WikiSelector(wikiService);
            wikiSelector.addChangeHandler(this);
            selectorsPanel.add(wikiSelector);
        }

        spaceSelector = new SpaceSelector(wikiService);
        spaceSelector.addChangeHandler(this);
        selectorsPanel.add(spaceSelector);

        pageSelector = new PageSelector(wikiService);
        selectorsPanel.add(pageSelector);

        Button updateImagesListButton = new Button(Strings.INSTANCE.imageUpdateListButton());
        updateImagesListButton.addClickHandler(new ClickHandler()
        {
            public void onClick(ClickEvent event)
            {
                WikiPageReference originPage = new WikiPageReference(getData().getOrigin());
                AttachmentReference imageReferenceTemplate = new AttachmentReference();
                imageReferenceTemplate.getWikiPageReference().setWikiName(
                    displayWikiSelector ? wikiSelector.getSelectedWiki() : originPage.getWikiName());
                imageReferenceTemplate.getWikiPageReference().setSpaceName(spaceSelector.getSelectedSpace());
                imageReferenceTemplate.getWikiPageReference().setPageName(pageSelector.getSelectedPage());
                initCurrentPage(imageReferenceTemplate, null);
            }
        });

        selectorsPanel.add(updateImagesListButton);
        selectorsPanel.addStyleName("xPageChooser");

        return selectorsPanel;
    }

    /**
     * Refreshes the list of images with the images attached to the same page as the specified image, and then selects
     * the specified image.
     * 
     * @param imageReference a reference to the image to be selected
     * @param cb the object to be notified after the specified image is selected
     */
    public void setSelection(final AttachmentReference imageReference, final AsyncCallback< ? > cb)
    {
        if (displayWikiSelector) {
            setWikiSelection(imageReference, cb);
        } else {
            setSpaceSelection(imageReference, cb);
        }
    }

    /**
     * Sets the selected wiki based on the specified image and updates the space selector.
     * 
     * @param imageReference the image to be selected
     * @param cb the object to be notified after the specified image is selected
     */
    private void setWikiSelection(final AttachmentReference imageReference, final AsyncCallback< ? > cb)
    {
        wikiSelector.refreshList(imageReference.getWikiPageReference().getWikiName(), new AsyncCallback<List<String>>()
        {
            public void onSuccess(List<String> result)
            {
                setSpaceSelection(imageReference, cb);
            }

            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            }
        });
    }

    /**
     * Sets the selected space based on the specified image and updates the page selector.
     * 
     * @param imageReference the image to be selected
     * @param cb the object to be notified after the specified image is selected
     */
    private void setSpaceSelection(AttachmentReference imageReference, final AsyncCallback< ? > cb)
    {
        // Clone the image reference because we might modify it. See the next comment.
        final AttachmentReference actualImageReference = imageReference.clone();
        WikiPageReference wikiPageReference = actualImageReference.getWikiPageReference();
        spaceSelector.setWiki(displayWikiSelector ? wikiSelector.getSelectedWiki() : wikiPageReference.getWikiName());
        // Replace the image reference components that point to missing entities with the entities selected by default.
        // In this case, if the image reference points to a wiki that doesn't exist then we update the image reference
        // to use the wiki selected by default.
        wikiPageReference.setWikiName(spaceSelector.getWiki());
        spaceSelector.refreshList(wikiPageReference.getSpaceName(), new AsyncCallback<List<String>>()
        {
            public void onSuccess(List<String> result)
            {
                setPageSelection(actualImageReference, cb);
            }

            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            }
        });
    }

    /**
     * Sets the selected page based on the specified image and updates the list of images accordingly.
     * 
     * @param imageReference the image to be selected
     * @param cb the object to be notified after the specified image is selected
     */
    private void setPageSelection(final AttachmentReference imageReference, final AsyncCallback< ? > cb)
    {
        pageSelector.setWiki(spaceSelector.getWiki());
        pageSelector.setSpace(spaceSelector.getSelectedSpace());
        // Replace the image reference components that point to missing entities with the entities selected by default.
        // In this case, if the image reference points to a space that doesn't exist then we update the image reference
        // to use the space selected by default.
        imageReference.getWikiPageReference().setSpaceName(pageSelector.getSpace());
        pageSelector.refreshList(imageReference.getWikiPageReference().getPageName(), new AsyncCallback<List<String>>()
        {
            public void onSuccess(List<String> result)
            {
                // Replace the image reference components that point to missing entities with the entities selected by
                // default. In this case, if the image reference points to a page that doesn't exist then we update the
                // image reference to use the page selected by default.
                imageReference.getWikiPageReference().setPageName(pageSelector.getSelectedPage());
                initCurrentPage(imageReference, cb);
            }

            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            }
        });
    }

    @Override
    public void onChange(ChangeEvent event)
    {
        if (event.getSource() == wikiSelector) {
            spaceSelector.setWiki(wikiSelector.getSelectedWiki());
            spaceSelector.refreshList(spaceSelector.getSelectedSpace(), new AsyncCallback<List<String>>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                }

                @Override
                public void onSuccess(List<String> result)
                {
                    pageSelector.setWiki(wikiSelector.getSelectedWiki());
                    pageSelector.setSpace(spaceSelector.getSelectedSpace());
                    pageSelector.refreshList(pageSelector.getSelectedPage());
                }
            });
        } else if (event.getSource() == spaceSelector) {
            pageSelector.setWiki(spaceSelector.getWiki());
            pageSelector.setSpace(spaceSelector.getSelectedSpace());
            pageSelector.refreshList(pageSelector.getSelectedPage());
        }
    }

    /**
     * Initializes and displays list of images attached to the same page as the specified image, and selects the
     * specified image.
     * 
     * @param imageReference a reference to the image to be selected after the list of images is updated
     * @param cb the object to be notified after the list of images is updated
     */
    protected void initCurrentPage(AttachmentReference imageReference, final AsyncCallback< ? > cb)
    {
        display().addStyleName(STYLE_LOADING);
        getData().getDestination().setEntityReference(imageReference.getEntityReference());
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
        display().removeStyleName(STYLE_LOADING);
        Label error = new Label(Strings.INSTANCE.linkErrorLoadingData());
        error.addStyleName("errormessage");
        display().remove(pageWizardStep.display());
        display().add(error);
    }

    /**
     * Helper function to handle the success on initialization of the current page wizard step.
     */
    private void onCurrenPageInitialization()
    {
        // if the current page's display is not there (maybe an error before removed it), remove the error and add
        if (display().getWidgetIndex(pageWizardStep.display()) < 0) {
            // FIXME: the error panel shouldn't be identified by its position!
            display().remove(display().getWidgetCount() - 1);
            display().add(pageWizardStep.display());
        }
        display().removeStyleName(STYLE_LOADING);
    }

    @Override
    public VerticalResizePanel display()
    {
        return (VerticalResizePanel) super.display();
    }

    @Override
    public String getNextStep()
    {
        return pageWizardStep.getNextStep();
    }

    @Override
    protected void initializeSelection(AsyncCallback< ? > cb)
    {
        if (!StringUtils.isEmpty(getData().getData().getReference())
            && getData().getDestination().getEntityReference().getType() == EntityType.ATTACHMENT) {
            // Edit internal image.
            setSelection(new AttachmentReference(getData().getDestination().getEntityReference()), cb);
        } else if (pageSelector.getSelectedPage() == null) {
            // Insert image. No page selected so initialize the list of images.
            setSelection(new AttachmentReference(getData().getOrigin()), cb);
        } else {
            // Insert image. There is a previous selection, preserve it and re-initialize the list of images.
            EntityReference destinationReference = pageWizardStep.getData().getDestination().getEntityReference();
            initCurrentPage(new AttachmentReference(destinationReference), cb);
        }
    }

    @Override
    public void onCancel()
    {
        pageWizardStep.onCancel();
    }

    @Override
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        pageWizardStep.onSubmit(async);
    }

    @Override
    public void addNavigationListener(NavigationListener listener)
    {
        pageWizardStep.addNavigationListener(listener);
    }

    @Override
    public void removeNavigationListener(NavigationListener listener)
    {
        pageWizardStep.removeNavigationListener(listener);
    }

    @Override
    public void setActive()
    {
        if (displayWikiSelector) {
            wikiSelector.setFocus(true);
        } else {
            spaceSelector.setFocus(true);
        }
    }

    @Override
    public Object getResult()
    {
        return pageWizardStep.getResult();
    }
}
