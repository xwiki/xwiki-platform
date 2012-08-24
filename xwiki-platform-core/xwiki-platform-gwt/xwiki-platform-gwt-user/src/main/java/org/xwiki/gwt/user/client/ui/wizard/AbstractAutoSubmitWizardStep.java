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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Abstract wizard step that is automatically submitted after it is initialized. This should be the base class for all
 * wizard steps that don't require user input, i.e. that are non-interactive. Derived classes should overwrite
 * {@link #onSubmit(AsyncCallback)} to process the input data.
 * 
 * @param <T> the type of data processed by this wizard step
 * @version $Id$
 */
public abstract class AbstractAutoSubmitWizardStep<T> extends AbstractNavigationAwareWizardStep
{
    /**
     * The data processed by this wizard step.
     */
    private T data;

    /**
     * The step title, visible while the data is processed and if the initialization/submit fails.
     */
    private String stepTitle;

    /**
     * {@inheritDoc}
     * <p>
     * Don't overwrite this method. Overwrite {@link #onSubmit(AsyncCallback)} instead and use {@link #getData()}.
     * 
     * @see AbstractNavigationAwareWizardStep#init(Object, AsyncCallback)
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void init(Object data, AsyncCallback< ? > cb)
    {
        this.data = (T) data;
        cb.onSuccess(null);
    }

    /**
     * @return the data processed by this wizard step
     */
    protected T getData()
    {
        return data;
    }

    @Override
    public String getStepTitle()
    {
        return stepTitle;
    }

    /**
     * Sets the step title.
     * 
     * @param stepTitle the new step title
     */
    public void setStepTitle(String stepTitle)
    {
        this.stepTitle = stepTitle;
    }

    @Override
    public final Widget display()
    {
        // Steps that are automatically submitted should not be displayed.
        return null;
    }

    @Override
    public Object getResult()
    {
        return data;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overwrite if you want to change the input data.
     * 
     * @see AbstractNavigationAwareWizardStep#onSubmit(AsyncCallback)
     */
    @Override
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        // Leave the input data unchanged by default.
        async.onSuccess(Boolean.TRUE);
    }

    @Override
    public final boolean isAutoSubmit()
    {
        return true;
    }

    @Override
    public void onCancel()
    {
        // Do nothing by default.
    }
}
