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

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Abstract {@link WizardStep} to implement basic selector functions (storing edited data on
 * {@link #init(Object, AsyncCallback)} and returning on {@link #getResult()}, step names, directions, initialization),
 * regardless of the actual selecting method.
 * 
 * @param <T> the type of data edited by this wizard step
 * @version $Id$
 */
public abstract class AbstractSelectorWizardStep<T> implements WizardStep
{
    /**
     * The data edited by this wizard step.
     */
    private T data;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void init(Object data, AsyncCallback< ? > cb)
    {
        this.data = (T) data;
        initializeSelection();
        cb.onSuccess(null);
    }

    /**
     * Initializes the selection on {@link #init(Object, AsyncCallback)} time.
     */
    protected void initializeSelection()
    {
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        if (direction == NavigationDirection.NEXT) {
            return Strings.INSTANCE.select();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.NEXT, NavigationDirection.PREVIOUS, NavigationDirection.CANCEL);
    }

    /**
     * @return the data configured by this {@link WizardStep}
     */
    public T getData()
    {
        return data;
    }
}
