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
package com.xpn.xwiki.wysiwyg.client.widget.wizard.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step used to aggregate a set of selectors for a file attached (file attachment or image) to a page in the
 * wiki, and switch between the current page view and the entire wiki view.
 * 
 * @param <T> the type of object edited by this wizard step
 * @see AbstractSelectorWizardStep
 * @version $Id$
 */
public abstract class AbstractSelectorAggregatorWizardStep<T> extends AbstractSelectorWizardStep<T> implements
    ClickListener
{
    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * Style for the selected tab change button.
     */
    private static final String STYLE_SELECTED_BUTTON = "xSelected";

    /**
     * Wizard step for the selection from the current page.
     */
    private WizardStep currentPageStep;

    /**
     * Flag to store if the current page step has been initialized or not.
     */
    private boolean isCurrentPageInitialized;

    /**
     * Wizard step for the selection from the whole wiki.
     */
    private WizardStep allWikiStep;

    /**
     * Flag to store if the all pages step has been initialized or not.
     */
    private boolean isAllPagesInitialized;

    /**
     * Specifies whether the current page is selected or not.
     */
    private boolean isCurrentPage;

    /**
     * The main panel of wizard step.
     */
    private final FlowPanel mainPanel = new FlowPanel();

    /**
     * The button to set the current page selector active.
     */
    private final Button setCurrentPageButton = new Button(Strings.INSTANCE.selectorSelectFromCurrentPage());

    /**
     * The button to set all the pages selector active.
     */
    private final Button setAllPagesButton = new Button(Strings.INSTANCE.selectorSelectFromAllPages());

    /**
     * The current resource edited by this wizard step.
     */
    private ResourceName editedResource;

    /**
     * Creates a new aggregator selector wizard step, for the currently edited resource.
     * 
     * @param editedResource the currently edited resource
     */
    public AbstractSelectorAggregatorWizardStep(ResourceName editedResource)
    {
        this.editedResource = editedResource;

        // instantiate the currentPage
        mainPanel.addStyleName("xSelectorStep");

        Panel buttonsStrip = new FlowPanel();
        buttonsStrip.addStyleName("xToggleButtons");

        setCurrentPageButton.addClickListener(this);
        setAllPagesButton.addClickListener(this);
        buttonsStrip.add(setCurrentPageButton);
        buttonsStrip.add(setAllPagesButton);

        mainPanel.add(buttonsStrip);

        // put the current page in by default. Will be toggled on init if necessary, else it will be initialized
        isCurrentPage = true;
        setCurrentPageButton.addStyleName(STYLE_SELECTED_BUTTON);
        setAllPagesButton.removeStyleName(STYLE_SELECTED_BUTTON);
        mainPanel.add(getCurrentPageStep().display());
    }

    /**
     * Lazy initializer and getter for the current page wizard step.
     * 
     * @return the current page selector wizard step
     */
    private WizardStep getCurrentPageStep()
    {
        if (currentPageStep == null) {
            currentPageStep = getCurrentPageSelectorInstance();
        }
        return currentPageStep;
    }

    /**
     * Returns an instance of the current page selector. To be overridden by subclasses to provide specific behaviour.
     * 
     * @return an instance of the selector for the current page
     */
    protected abstract WizardStep getCurrentPageSelectorInstance();

    /**
     * Lazy initializer and getter for the entire wiki wizard step.
     * 
     * @return the all wiki selector wizard step
     */
    private WizardStep getAllPagesStep()
    {
        if (allWikiStep == null) {
            allWikiStep = getAllPagesSelectorInstance();
        }
        return allWikiStep;
    }

    /**
     * Returns an instance of the all pages selector. To be overridden by subclasses to provide specific behaviour.
     * 
     * @return an instance of the selector for all the pages
     */
    protected abstract WizardStep getAllPagesSelectorInstance();

    /**
     * Toggles the current wizard step.
     */
    private void toggleCurrentStep()
    {
        if (mainPanel.getWidgetCount() > 1) {
            mainPanel.remove(1);
        }
        mainPanel.addStyleName(STYLE_LOADING);

        final WizardStep stepToShow = isCurrentPage ? getAllPagesStep() : getCurrentPageStep();
        lazyInitializeStep(stepToShow, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                mainPanel.add(stepToShow.display());
                mainPanel.removeStyleName(STYLE_LOADING);
            }

            public void onFailure(Throwable caught)
            {
                mainPanel.removeStyleName(STYLE_LOADING);
                Label error = new Label(Strings.INSTANCE.linkErrorLoadingData());
                error.addStyleName("errormessage");
                mainPanel.add(error);
            }
        });
        if (isCurrentPage) {
            setAllPagesButton.addStyleName(STYLE_SELECTED_BUTTON);
            setCurrentPageButton.removeStyleName(STYLE_SELECTED_BUTTON);
        } else {
            setCurrentPageButton.addStyleName(STYLE_SELECTED_BUTTON);
            setAllPagesButton.removeStyleName(STYLE_SELECTED_BUTTON);
        }
        isCurrentPage = !isCurrentPage;
    }

    /**
     * @return the currently selected wizard step
     */
    private WizardStep getCurrentStep()
    {
        if (isCurrentPage) {
            return getCurrentPageStep();
        } else {
            return getAllPagesStep();
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
    public String getDirectionName(NavigationDirection direction)
    {
        return getCurrentStep().getDirectionName(direction);
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return getCurrentStep().getNextStep();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return getCurrentStep().getResult();
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, final AsyncCallback< ? > cb)
    {
        // reset initialization of aggregated steps
        isCurrentPageInitialized = false;
        isAllPagesInitialized = false;

        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                // check if it needs to load the "all pages" tab
                boolean requiresAllPages = loadAllPages();
                // if it's a link to a different page than the current page and the current page is not current step or
                // if it's a current page step and current page is not active, change the step
                if ((requiresAllPages && isCurrentPage) || (!requiresAllPages && !isCurrentPage)) {
                    toggleCurrentStep();
                }

                lazyInitializeStep(getCurrentStep(), cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Initializes the passed step only if it wasn't initialized yet (i.e. it's the first display of this step).
     * 
     * @param step the step to initialize
     * @param cb the call back to handle asynchronous load of the step
     */
    private void lazyInitializeStep(WizardStep step, AsyncCallback< ? > cb)
    {
        if (step == currentPageStep && !isCurrentPageInitialized) {
            step.init(getData(), cb);
            isCurrentPageInitialized = true;
            return;
        }
        if (step == allWikiStep && !isAllPagesInitialized) {
            step.init(getData(), cb);
            isAllPagesInitialized = true;
            return;
        }
        // nothing to do, just signal success
        cb.onSuccess(null);
    }

    /**
     * Helper function to determine if a link to be edited / created needs the all pages tree or not. To be overwritten
     * by subclasses to detect whether the data being handled requires the "all pages" step to be loaded or not.
     * 
     * @return {@code true} if the link requires the full wiki, {@code false} otherwise.
     */
    protected boolean loadAllPages()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        getCurrentStep().onCancel(async);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        getCurrentStep().onSubmit(async);
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(Widget sender)
    {
        if (sender == setCurrentPageButton && !isCurrentPage) {
            toggleCurrentStep();
        }

        if (sender == setAllPagesButton && isCurrentPage) {
            toggleCurrentStep();
        }
    }

    /**
     * @return the editedResource
     */
    public ResourceName getEditedResource()
    {
        return editedResource;
    }
}
