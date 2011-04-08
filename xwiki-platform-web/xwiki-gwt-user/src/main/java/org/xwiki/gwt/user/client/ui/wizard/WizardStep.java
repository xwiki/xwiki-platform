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

import java.util.EnumSet;

import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Defines the behavior of a wizard step.
 * 
 * @version $Id$
 */
public interface WizardStep
{
    /**
     * Initializes the current wizard step, according to the passed data. The state of the current wizard step should be
     * entirely computed from the passed input data. This function will be called always on showing the dialog for this
     * wizard step (both when shown as next and when shown as previous), with the data obtained from the
     * {@link #getResult()} function of the previous step. The {@code cb} parameter should handle the asynchronous
     * initialization of this wizard step, and should return normally through {@code onSuccess()} and error through
     * {@code onFailure()}.
     * 
     * @param data an object to pass to the wizard step, which can contain configuration data, etc.
     * @param cb callback to handle asynchronous loading of this wizard step
     */
    void init(Object data, AsyncCallback< ? > cb);

    /**
     * @return the UI representation of this wizard step
     */
    Widget display();

    /**
     * @return the title of the current step, as it should be displayed when the step is shown
     */
    String getStepTitle();

    /**
     * @return the result of the current step, in its current state. The result of the current wizard step will be used
     *         as the input for the next dialog in the chain, either the next (as returned by {@link #getNextStep()}) or
     *         the previous in the navigation stack.
     */
    Object getResult();

    /**
     * Called before submitting the current wizard step. Here is the point to do validation and to compute the current
     * result of the dialog as well as the next step name. The asynchronous callback result (true or false) will be used
     * to determine if the submit should continue or it should be prevented.
     * 
     * @param async asynchronous callback to handle asynchronous nature of this function (in case of validation on the
     *            server side, for example).
     */
    void onSubmit(AsyncCallback<Boolean> async);

    /**
     * Called before canceling the current wizard step. Here is the point to do all adjustments before the previous
     * dialogs loaded, in the case of a chained wizard.
     */
    void onCancel();

    // Note: the following functions could be as well moved to a NavigatingWizardStep interface, so that we don't hold
    // navigation responsibilities in the wizard step itself, but for the moment it would be a bit overengineered.

    /**
     * Returns the key of the next step in the wizard. Note that this function is called after
     * {@link #onSubmit(AsyncCallback)} has returned successfully, so the computing of the next step can be done safely
     * at {@link #onSubmit(AsyncCallback)} time.
     * 
     * @return the key of the next step in the wizard.
     */
    String getNextStep();

    /**
     * @return the set of valid directions from this step.
     */
    EnumSet<NavigationDirection> getValidDirections();

    /**
     * Allows this step to overwrite the default printed for the navigation directions.
     * 
     * @param direction the direction to get the name for
     * @return the String with the direction name or null if the default should be kept
     */
    String getDirectionName(NavigationDirection direction);
}
