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

import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Strings;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 * Abstract interactive wizard step to implement basic selector functions (storing edited data on
 * {@link #init(Object, AsyncCallback)} and returning on {@link #getResult()}, step names, directions, initialization),
 * regardless of the actual selecting method.
 * 
 * @param <T> the type of data edited by this wizard step
 * @version $Id$
 */
public abstract class AbstractSelectorWizardStep<T> extends AbstractInteractiveWizardStep
{
    /**
     * The data edited by this wizard step.
     */
    private T data;

    /**
     * Default constructor.
     */
    public AbstractSelectorWizardStep()
    {
        this(new FlowPanel());
    }

    /**
     * Creates a new step that uses the given panel.
     * 
     * @param panel the panel where to place the step's widgets
     */
    public AbstractSelectorWizardStep(FlowPanel panel)
    {
        super(panel);

        setDirectionName(NavigationDirection.NEXT, Strings.INSTANCE.select());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Object data, AsyncCallback< ? > cb)
    {
        this.data = (T) data;
        initializeSelection(cb);
    }

    /**
     * Initializes the selection on {@link #init(Object, AsyncCallback)} time.
     * 
     * @param initCallback the initialization callback, to handle asynchronous initialization
     */
    protected void initializeSelection(AsyncCallback< ? > initCallback)
    {
        initCallback.onSuccess(null);
    }

    @Override
    public Object getResult()
    {
        return data;
    }

    /**
     * @return the data configured by this wizard step
     */
    public T getData()
    {
        return data;
    }

    /**
     * Notifies this wizard step that it has been activated, when part of an aggregator for example.
     * <p>
     * FIXME: this function should not be here but in it's own decorating interface for aggregated wizard steps or,
     * cleaner, all wizard steps who need to be notified when they're activated should implement Focusable interface and
     * all wizard step handlers (the dialog, the aggregator) should call setFocus(). This is a quick solution to handle
     * tab change in the selector aggregator.
     */
    public void setActive()
    {
        // nothing by default, to be overriden by subclasses to execute specific activation code, such as setting focus
        // in the appropriate fields
    }
}
