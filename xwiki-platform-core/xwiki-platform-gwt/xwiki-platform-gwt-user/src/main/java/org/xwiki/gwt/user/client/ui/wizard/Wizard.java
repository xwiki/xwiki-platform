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
     */
    protected abstract class AbstractAsyncCallbackAdaptor<T> implements AsyncCallback<T>
    {
        @Override
        public void onFailure(Throwable caught)
        {
            // Keeping the finishing flag set prevents us from going back when an error occurs.
            finishing = false;
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
     * {@code true} if {@link NavigationDirection#FINISH} has been triggered and the queue of next wizard steps is not
     * empty, {@code false} otherwise. The wizard goes through all the remaining steps and automatically submits them
     * once this flag is set to {@code true}.
     */
    protected boolean finishing;

    /**
     * Builds a wizard from the passed title and with the specified icon.
     * 
     * @param wizardTitle the title of this wizard
     * @param wizardIcon the icon of this wizard
     */
    public Wizard(String wizardTitle, Image wizardIcon)
    {
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
        finishing = false;
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
        currentStep.init(data, new AbstractAsyncCallbackAdaptor<Object>()
        {
            public void onSuccess(Object result)
            {
                dialog.displayStep(currentStep, navigationStack.size() > 1);
                if (finishing) {
                    onDirection(NavigationDirection.FINISH);
                } else if (currentStep.isAutoSubmit()) {
                    onDirection(NavigationDirection.NEXT);
                } else if (currentStep instanceof SourcesNavigationEvents) {
                    ((SourcesNavigationEvents) currentStep).addNavigationListener(Wizard.this);
                }
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
            ((SourcesNavigationEvents) currentStep).removeNavigationListener(this);
        }
    }

    @Override
    public void onDirection(final NavigationDirection direction)
    {
        dialog.setLoading(true);
        if (direction == NavigationDirection.CANCEL || direction == NavigationDirection.PREVIOUS) {
            currentStep.onCancel();
            if (direction == NavigationDirection.CANCEL) {
                onCancel();
            } else {
                onPrevious();
            }
        } else if (direction == NavigationDirection.FINISH || direction == NavigationDirection.NEXT) {
            currentStep.onSubmit(new AbstractAsyncCallbackAdaptor<Boolean>()
            {
                public void onSuccess(Boolean success)
                {
                    if (success) {
                        if (direction == NavigationDirection.FINISH) {
                            finishing = true;
                            onFinish();
                        } else {
                            onNext();
                        }
                    } else {
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
        unloadCurrentStep();
        currentStep = null;
        hideDialog();
        for (WizardListener listener : wizardListeners) {
            listener.onCancel(this);
        }
    }

    /**
     * Handle going to the previous step.
     */
    protected void onPrevious()
    {
        // We have to jump over the previous wizard steps that are automatically submitted, without initializing nor
        // canceling them.
        Object result;
        do {
            // Get the current step out of the stack.
            navigationStack.pop();
            if (navigationStack.isEmpty()) {
                onCancel();
                return;
            }
            WizardStep previousStep = provider.getStep(navigationStack.peek());
            if (previousStep == null) {
                onCancel();
                return;
            }
            result = currentStep.getResult();
            unloadCurrentStep();
            currentStep = previousStep;
        } while (currentStep.isAutoSubmit());
        initAndDisplayCurrentStep(result);
    }

    /**
     * Handle advancing to the next step.
     */
    protected void onNext()
    {
        String nextStepName = currentStep.getNextStep();
        if (nextStepName != null) {
            WizardStep nextStep = provider.getStep(nextStepName);
            if (nextStep != null) {
                Object result = currentStep.getResult();
                unloadCurrentStep();
                currentStep = nextStep;
                navigationStack.push(nextStepName);
                initAndDisplayCurrentStep(result);
                return;
            }
        }

        finishing = false;
        onFinish();
    }

    /**
     * Handle commanding the finish of the whole wizard.
     */
    protected void onFinish()
    {
        if (finishing) {
            // We automatically submit all the following steps when finishing.
            onNext();
        } else {
            unloadCurrentStep();
            Object result = getResult();
            currentStep = null;
            hideDialog();
            for (WizardListener listener : wizardListeners) {
                listener.onFinish(this, result);
            }
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

    @Override
    public void onClose(CloseEvent<CompositeDialogBox> event)
    {
        if (event.getTarget() == dialog) {
            if (!dialog.isCanceled()) {
                // Closed by code, nothing to do.
                return;
            } else {
                // Closed by user action, do a cancel.
                onDirection(NavigationDirection.CANCEL);
            }
        }
    }

    /**
     * Helper function to hide the dialog and mark that it was closed by code and not by user action.
     */
    public void hideDialog()
    {
        dialog.setCanceled(false);
        dialog.hide();
    }
}
