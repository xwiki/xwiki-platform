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
 *
 */
package org.xwiki.observation.event;

/**
 * An event triggered whenever a client request (action) is processed, like <tt>/upload/</tt> or <tt>/view/</tt>. A
 * specific event corresponds to only one {@link #actionName action type}.
 * 
 * @todo use the enumerated Action class when it's implemented...
 * @version $Id$
 */
public class ActionExecutionEvent implements Event
{
    /** The name of the executed action. */
    private String actionName;

    /**
     * Constructor initializing the action name of the event.
     * 
     * @param actionName the name of the executed action
     */
    public ActionExecutionEvent(String actionName)
    {
        this.actionName = actionName;
    }

    /**
     * Gets the name of the action causing this event.
     * 
     * @return the action causing this event, like <code>upload</code> or <code>login</code>
     */
    public String getActionName()
    {
        return this.actionName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getActionName().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object)
    {
        return getActionName().equals(object);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Event#matches(Object)
     */
    public boolean matches(Object otherEvent)
    {
        boolean isMatching = false;
        if (ActionExecutionEvent.class.isAssignableFrom(otherEvent.getClass())) {
            ActionExecutionEvent actionEvent = (ActionExecutionEvent) otherEvent;
            isMatching = getActionName().equals(actionEvent.getActionName());
        }
        return isMatching;
    }
}
