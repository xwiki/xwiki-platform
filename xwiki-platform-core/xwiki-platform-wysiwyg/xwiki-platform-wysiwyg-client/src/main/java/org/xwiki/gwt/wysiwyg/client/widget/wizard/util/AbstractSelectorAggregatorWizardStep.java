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
package org.xwiki.gwt.wysiwyg.client.widget.wizard.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListenerCollection;
import org.xwiki.gwt.user.client.ui.wizard.SourcesNavigationEvents;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TabPanel;

/**
 * Wizard step used to aggregate a set of selectors for a file attached (file attachment or image) to a page in the
 * wiki, and switch between the current page view and the entire wiki view.
 * 
 * @param <T> the type of object edited by this wizard step
 * @see AbstractSelectorWizardStep
 * @version $Id$
 */
public abstract class AbstractSelectorAggregatorWizardStep<T> extends AbstractSelectorWizardStep<T> implements
    SelectionHandler<Integer>, SourcesNavigationEvents, NavigationListener
{
    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_ERROR = "errormessage";

    /**
     * The map of wizard step instances of the steps aggregated by this step.
     */
    private Map<String, WizardStep> steps = new HashMap<String, WizardStep>();

    /**
     * The state of the initialization of the aggregated wizard steps.
     */
    private Map<WizardStep, Boolean> initialized = new HashMap<WizardStep, Boolean>();

    /**
     * The tabbed panel of the wizard step.
     */
    private final TabPanel tabPanel = new TabPanel();

    /**
     * The navigation listeners for this selector step, to pass further potential navigation events launched by
     * aggregated steps.
     */
    private NavigationListenerCollection listeners = new NavigationListenerCollection();

    /**
     * Creates a new aggregator selector wizard step.
     */
    public AbstractSelectorAggregatorWizardStep()
    {
        tabPanel.addStyleName("xStepsTabs");
        tabPanel.addSelectionHandler(this);
        display().add(tabPanel);
        display().addStyleName("xSelectorAggregatorStep");
    }

    /**
     * @param name the name of the step to get
     * @return the step for the passed name
     */
    protected WizardStep getStep(String name)
    {
        if (steps.get(name) == null) {
            // save it in the steps
            WizardStep instance = getStepInstance(name);
            steps.put(name, instance);
            // add this as a listener, if it's the case, to pass further the navigation events
            if (instance instanceof SourcesNavigationEvents) {
                ((SourcesNavigationEvents) instance).addNavigationListener(this);
            }
            // as uninitialized
            initialized.put(instance, false);
        }
        return steps.get(name);
    }

    /**
     * @param name the name of the step to initialize
     * @return an instance of the step recognized by the passed name
     */
    protected abstract WizardStep getStepInstance(String name);

    /**
     * @return the list of all step names
     */
    protected abstract List<String> getStepNames();

    /**
     * @return the step which should be selected by default, the first step name by default
     */
    protected String getDefaultStepName()
    {
        return getStepNames().get(0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SelectionHandler#onSelection(SelectionEvent)
     */
    public void onSelection(SelectionEvent<Integer> event)
    {
        if (event.getSource() != tabPanel) {
            return;
        }
        tabPanel.addStyleName(STYLE_LOADING);

        // get the step to be prepared and shown
        String stepName = tabPanel.getTabBar().getTabHTML(event.getSelectedItem());
        final WizardStep stepToShow = getStep(stepName);

        final FlowPanel stepPanel = (FlowPanel) tabPanel.getWidget(tabPanel.getDeckPanel().getVisibleWidget());
        // hide its contents until after load
        if (stepPanel.getWidgetCount() > 0) {
            stepPanel.getWidget(0).setVisible(false);
        }

        // initialize only if it wasn't initialized before
        lazyInitializeStep(stepToShow, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                onStepInitialized(stepToShow, stepPanel);
            }

            public void onFailure(Throwable caught)
            {
                stepPanel.setVisible(true);
                tabPanel.removeStyleName(STYLE_LOADING);
                showError(Strings.INSTANCE.linkErrorLoadingData(), stepPanel);
            }
        });
    }

    /**
     * Helper function to handle the success of the step initialization on tab select.
     * 
     * @param step the step that just finished loading
     * @param stepPanel the container panel of the tab where this widget is to be displayed
     */
    private void onStepInitialized(WizardStep step, FlowPanel stepPanel)
    {
        // remove any existant error message
        if (stepPanel.getWidgetCount() > 0 && stepPanel.getWidget(0).getStyleName().contains(STYLE_ERROR)) {
            stepPanel.clear();
        }
        // add the UI of the step we switched to to the tabbed panel, if not already there
        if (stepPanel.getWidgetCount() == 0) {
            stepPanel.add(step.display());
        }
        stepPanel.getWidget(0).setVisible(true);
        tabPanel.removeStyleName(STYLE_LOADING);
        if (step instanceof AbstractSelectorWizardStep) {
            ((AbstractSelectorWizardStep<?>) step).setActive();
        }
    }

    /**
     * Helper function to show an error in the passed panel.
     * 
     * @param message the error message
     * @param panel the panel in which the error is to be displayed
     */
    private void showError(String message, Panel panel)
    {
        // remove all content before
        panel.clear();
        Label error = new Label(message);
        error.addStyleName(STYLE_ERROR);
        panel.add(error);
    }

    /**
     * Selects the tab indicated by the passed name.
     * 
     * @param tabName the name of the tab to select
     */
    protected void selectTab(String tabName)
    {
        // searched for the specified tab and select it
        for (int i = 0; i < tabPanel.getTabBar().getTabCount(); i++) {
            if (tabPanel.getTabBar().getTabHTML(i).equals(tabName)) {
                tabPanel.selectTab(i);
                break;
            }
        }
    }

    /**
     * @return the currently selected wizard step, or the default step if no selection is made
     */
    private WizardStep getCurrentStep()
    {
        String selectedStepName = getSelectedStepName();
        return getStep(selectedStepName == null ? getDefaultStepName() : selectedStepName);
    }

    /**
     * @return the name of the currently selected wizard step, or {@code null} if no selection is made
     */
    private String getSelectedStepName()
    {
        int selectedTab = tabPanel.getTabBar().getSelectedTab();
        String currentStepName = null;
        if (selectedTab > 0) {
            currentStepName = tabPanel.getTabBar().getTabHTML(selectedTab);
        }
        return currentStepName;
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
    public void init(Object data, final AsyncCallback<?> cb)
    {
        // Maybe initialize the tab bar.
        if (tabPanel.getTabBar().getTabCount() == 0) {
            // Fill the tab bar with the names of the aggregated wizard steps.
            for (String stepName : getStepNames()) {
                tabPanel.add(new FlowPanel(), stepName);
            }
        }

        // Aggregated wizard steps have to be reinitialized.
        for (WizardStep step : initialized.keySet()) {
            initialized.put(step, false);
        }

        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                dispatchInit(cb);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }

    /**
     * Dispatches the initialization of the tabbed panel to the appropriate step, depending on the required step, the
     * initialization of this aggregator and the current selected step, if any.
     * 
     * @param cb the initialization callback
     */
    private void dispatchInit(final AsyncCallback<?> cb)
    {
        // pick the right tab to select
        String stepName = getRequiredStep();
        if (stepName == null) {
            stepName = getSelectedStepName();
            if (stepName == null) {
                stepName = getDefaultStepName();
            }
        }
        // select the chosen tab
        selectTab(stepName);
        // always return null, failure of aggregated step initialization will be handled inside this aggregator step,
        // because one aggregated step failure should not prevent the others to be selected
        cb.onSuccess(null);
    }

    /**
     * Initializes the passed step only if it wasn't initialized yet (i.e. it's the first display of this step).
     * 
     * @param step the step to initialize
     * @param cb the call back to handle asynchronous load of the step
     */
    private void lazyInitializeStep(final WizardStep step, final AsyncCallback<?> cb)
    {
        if (!initialized.get(step)) {
            step.init(getData(), new AsyncCallback<Object>()
            {
                public void onSuccess(Object result)
                {
                    // only mark as initialized when init succeeded, so that a second retry is attempted and its panel
                    // is not used further if init fails to initialize it correctly
                    initialized.put(step, true);
                    if (cb != null) {
                        cb.onSuccess(null);
                    }
                }

                public void onFailure(Throwable caught)
                {
                    if (cb != null) {
                        cb.onFailure(caught);
                    }
                }
            });
            return;
        }
        // nothing to do, just signal success
        cb.onSuccess(null);
    }

    /**
     * @return the name of the step required to be loaded by the current created or edited element, if any, or null
     *         otherwise (if previous selection should be preserved). To be overwritten by subclasses to detect whether
     *         the data being handled requires the "all pages" step to be loaded or not.
     */
    protected String getRequiredStep()
    {
        // by default, no requirement is made
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel()
    {
        getCurrentStep().onCancel();
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
    public void addNavigationListener(NavigationListener listener)
    {
        // cannot delegate here because the steps shouldn't be initialized only to add listeners; only current step
        // should fire navigation events.
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNavigationListener(NavigationListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void onDirection(NavigationDirection direction)
    {
        // FIXME: at this point we assume that only the current step will send navigation event, or we relaunch the
        // navigation events of all steps, regardless if they're active or not. This is a good enough assumption ftm,
        // since navigation events are issued by user actions and right now only the current step is visible at a given
        // moment.
        listeners.fireNavigationEvent(direction);
    }
}
