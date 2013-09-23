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
package org.xwiki.bridge.event;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for all action execution related events.
 * 
 * @version $Id$
 * @since 3.2M3
 */
// TODO: use the enumerated Action class when it's implemented
public abstract class AbstractActionExecutionEvent implements Serializable, ActionExecutionEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the executed action.
     */
    private String actionName;

    /**
     * Match any {@link ActionExecutedEvent}.
     */
    public AbstractActionExecutionEvent()
    {

    }

    /**
     * Constructor initializing the action name of the event.
     * 
     * @param actionName the name of the action
     */
    public AbstractActionExecutionEvent(String actionName)
    {
        this.actionName = actionName;
    }

    @Override
    public String getActionName()
    {
        return this.actionName;
    }

    @Override
    public int hashCode()
    {
        if (getActionName() == null) {
            return 0;
        }
        return getActionName().hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && getClass().isAssignableFrom(object.getClass())) {
            return StringUtils.equals(getActionName(), ((ActionExecutionEvent) object).getActionName());
        }
        return false;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == null) {
            return false;
        }
        boolean isMatching = false;
        if (getClass().isAssignableFrom(otherEvent.getClass())) {
            ActionExecutionEvent actionEvent = (ActionExecutionEvent) otherEvent;
            isMatching = getActionName() == null || getActionName().equals(actionEvent.getActionName());
        }
        return isMatching;
    }

    @Override
    public String toString()
    {
        return getClass() + " (" + getActionName() + ")";
    }
}
