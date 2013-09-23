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
     * Initializes the current wizard step, according to the passed data. This method is called before the wizard step
     * is displayed, on both navigation directions. The input data is either the data the wizard was started with or the
     * data returned by the {@link #getResult()} method of the previous step. The {@code callback} parameter should
     * handle the asynchronous initialization of this wizard step.
     * 
     * @param data an object to pass to the wizard step, which can contain configuration data
     * @param callback the object to be notified when the wizard step has finished initializing
     */
    void init(Object data, AsyncCallback< ? > callback);

    /**
     * This method is called right after the wizard step has been successfully initialized.
     * 
     * @return the UI representation of this wizard step
     */
    Widget display();

    /**
     * @return the title of the current step, as it should be displayed when the step is shown
     */
    String getStepTitle();

    /**
     * @return the result of the current step, in its current state. The result of the current wizard step will be used
     *         as the input for the next step in the wizard chain, either the next (as returned by
     *         {@link #getNextStep()}) or the previous in the navigation stack.
     */
    Object getResult();

    /**
     * Called before submitting the current wizard step. Here is the point to do validation and to compute the current
     * result of the dialog as well as the next step name. The asynchronous callback result (true or false) will be used
     * to determine if the submit should continue or it should be prevented.
     * 
     * @param callback the object to be notified when the submit is done; pass {@code true} to notify that the submit
     *            was successful and the wizard can move to the next step; pass {@code false} if the user needs to
     *            adjust the submitted data
     */
    void onSubmit(AsyncCallback<Boolean> callback);

    /**
     * @return {@code true} if this step should be submitted automatically, {@code false} otherwise; the
     *         {@link #display()} method should return {@code null} if the step is submitted automatically
     */
    boolean isAutoSubmit();

    /**
     * Called before canceling the current wizard step. Here is the point to do all adjustments before the previous
     * dialogs loaded, in the case of a chained wizard.
     */
    void onCancel();

    // Note: the following methods could be as well moved to a NavigatingWizardStep interface, so that we don't hold
    // navigation responsibilities in the wizard step itself, but for the moment it would be a bit over-engineered.

    /**
     * Returns the key of the next step in the wizard. Note that this function is called after
     * {@link #onSubmit(AsyncCallback)} has returned successfully, so the computing of the next step can be done safely
     * at {@link #onSubmit(AsyncCallback)} time.
     * 
     * @return the key of the next step in the wizard
     */
    String getNextStep();

    /**
     * @return the set of valid directions from this step
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
