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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Abstract {@link WizardStep} to implement basic selector functions (step names, directions, initialization),
 * regardless of the actual selecting method.
 * 
 * @version $Id$
 */
public abstract class AbstractSelectorWizardStep implements WizardStep
{
    /**
     * The link config edited by this wizard step.
     */
    private LinkConfig linkData;

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        linkData = (LinkConfig) data;
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
        return Strings.INSTANCE.select();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return linkData;
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.NEXT, NavigationDirection.PREVIOUS, NavigationDirection.CANCEL);
    }

    /**
     * @return the {@link LinkConfig} configured by this {@link WizardStep}
     */
    public LinkConfig getLinkData()
    {
        return linkData;
    }
}
