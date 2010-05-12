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
package org.xwiki.gwt.user.client.ui.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xwiki.gwt.user.client.ui.CompositeDialogBox;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;

/**
 * Wizard to handle the loading and navigation in a list of chained dialogs.
 * 
 * @version $Id$
 */
public class Wizard implements NavigationListener, CloseHandler<CompositeDialogBox>
{
    /**
     * Asynchronous callback adapter to handle the callback fails by displaying the error inside the dialog.
     * 
     * @param <T> the return type of the callback
     * @see AsyncCallback
     */
    protected abstract class AbstractDefaultAsyncCallback<T> implements AsyncCallback<T>
    {
        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onFailure(Throwable)
         */
        public void onFailure(Throwable caught)
        {
            dialog.showError(caught);
        }
    }

    /**
     * The list of wizard listeners to be notified by this wizard on events.
     */
    protected final List<WizardListener> wizardListeners = new ArrayList<WizardListener>();

    /**
     * Navigation stack to store the user's path through the chain of wizard steps.
     */
    protected final Stack<String> navigationStack = new Stack<String>();

    /**
     * The wizard step provider to get the steps for the next wizard dialogs.
     */
    protected WizardStepProvider provider;

    /**
     * The dialog in which the wizard steps are loaded.
     */
    protected final WizardDialog dialog;

    /**
     * The current step of the wizard.
     */
    protected WizardStep currentStep;

    /**
     * Builds a wizard from the passed title and with the specified icon.
     * 
     * @param wizardTitle the title of this wizard
     * @param wizardIcon the icon of this wizard
     */
    public Wizard(String wizardTitle, Image wizardIcon)
    {
        // initialize the dialog
        dialog = new WizardDialog(wizardTitle, wizardIcon);
        dialog.addNavigationListener(this);
        dialog.addCloseHandler(this);
    }

    /**
     * @return the wizard step provider
     */
    public WizardStepProvider getProvider()
    {
        return provider;
    }

    /**
     * @param provider the wizard step provider to set
     */
    public void setProvider(WizardStepProvider provider)
    {
        this.provider = provider;
    }

    /**
     * Shows the wizard from the starting step, with the passed data.
     * 
     * @param startStep the name of the start step for this wizard
     * @param data the input data for this wizard, to pass to the start step
     */
    public void start(String startStep, Object data)
    {
        // start over
        navigationStack.clear();

        currentStep = provider.getStep(startStep);
        if (currentStep == null) {
            onFinish();
            return;
        }
        navigationStack.push(startStep);
        initAndDisplayCurrentStep(data);
    }

    /**
     * Initializes and displays the current wizard step, with the passed data.
     * 
     * @param data the data to initialize the current step with
     */
    protected void initAndDisplayCurrentStep(Object data)
    {
        if (!dialog.isShowing()) {
            dialog.center();
        }
        dialog.setLoading(true);
        currentStep.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                dialog.displayStep(currentStep, navigationStack.size() > 1);
                if (currentStep instanceof SourcesNavigationEvents) {
                    ((SourcesNavigationEvents) currentStep).addNavigationListener(Wizard.this);
                }
            }

            public void onFailure(Throwable caught)
            {
                dialog.showError(caught);
            }
        });
    }

    /**
     * Prepares the current step to be unloaded (remove all listeners, etc.), to be executed before loading a next (or
     * previous) step.
     */
    protected void unloadCurrentStep()
    {
        if (currentStep instanceof SourcesNavigationEvents) {
            // remove this from the list of listened navigation sources
            ((SourcesNavigationEvents) currentStep).removeNavigationListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see NavigationListener#onDirection(NavigationDirection)
     */
    public void onDirection(final NavigationDirection direction)
    {
        if (direction == NavigationDirection.CANCEL || direction == NavigationDirection.PREVIOUS) {
            // call the step's onCancel and check the result
            dialog.setLoading(true);
            currentStep.onCancel();
            if (direction == NavigationDirection.CANCEL) {
                onCancel();
            } else {
                onPrevious();
            }
        }

        if (direction == NavigationDirection.FINISH || direction == NavigationDirection.NEXT) {
            // call the step's onSubmit and check the result
            dialog.setLoading(true);
            currentStep.onSubmit(new AbstractDefaultAsyncCallback<Boolean>()
            {
                public void onSuccess(Boolean result)
                {
                    if (result) {
                        // it's ok, take specific actions
                        if (direction == NavigationDirection.FINISH) {
                            onFinish();
                        } else {
                            onNext();
                        }
                    } else {
                        // nothing, keep it open
                        dialog.setLoading(false);
                    }
                }
            });
        }
    }

    /**
     * Handle cancel of the whole wizard.
     */
    protected void onCancel()
    {
        // unload current step
        unloadCurrentStep();
        currentStep = null;
        // hide UIs
        hideDialog();
        // notify listeners of cancel
        for (WizardListener wListener : wizardListeners) {
            wListener.onCancel(this);
        }
    }

    /**
     * Handle going to the previous step.
     */
    protected void onPrevious()
    {
        // get this step out of the stack
        navigationStack.pop();
        // if there are no more steps, cancel everything
        if (navigationStack.isEmpty()) {
            onCancel();
            return;
        }
        // get the previous step
        WizardStep previousStep = provider.getStep(navigationStack.peek());
        if (previousStep == null) {
            onCancel();
            return;
        }
        // prepare previous step
        Object result = currentStep.getResult();
        unloadCurrentStep();
        currentStep = previousStep;
        initAndDisplayCurrentStep(result);
    }

    /**
     * Handle advancing to the next step.
     */
    protected void onNext()
    {
        // get the step from the next action of this step
        String nextStepName = currentStep.getNextStep();
        WizardStep nextStep = provider.getStep(nextStepName);
        if (nextStep == null) {
            onFinish();
            return;
        }
        // prepare next step
        Object result = currentStep.getResult();
        unloadCurrentStep();
        currentStep = nextStep;
        navigationStack.push(nextStepName);
        initAndDisplayCurrentStep(result);
    }

    /**
     * Handle commanding the finish of the whole wizard.
     */
    protected void onFinish()
    {
        // unload things from the dialog
        unloadCurrentStep();
        Object result = getResult();
        currentStep = null;
        // hide UIs
        hideDialog();
        // fire the listeners
        for (WizardListener wListener : wizardListeners) {
            wListener.onFinish(this, result);
        }
    }

    /**
     * @return the result of this wizard
     */
    protected Object getResult()
    {
        return currentStep != null ? currentStep.getResult() : null;
    }

    /**
     * Adds a wizard listener to the list notified by this wizard.
     * 
     * @param listener the listener to add
     */
    public void addWizardListener(WizardListener listener)
    {
        wizardListeners.add(listener);
    }

    /**
     * Removes the specified wizard listener from the list notified by this wizard.
     * 
     * @param listener the listener to remove
     */
    public void removeWizardListener(WizardListener listener)
    {
        wizardListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CloseHandler#onClose(CloseEvent)
     */
    public void onClose(CloseEvent<CompositeDialogBox> event)
    {
        if (event.getTarget() == dialog) {
            if (!dialog.isCanceled()) {
                // it's a programmatic close, nothing to do
                return;
            } else {
                // it's a user close, do a cancel
                onDirection(NavigationDirection.CANCEL);
            }
        }
    }

    /**
     * Helper function to hide the dialog and mark that it's a programmatic close not a user close.
     */
    public void hideDialog()
    {
        dialog.setCanceled(false);
        dialog.hide();
    }
}
