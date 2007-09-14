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
 * @todo use the enumerated Action class when it's implemented...
 */
public class ActionExecutionEvent implements Event
{
    private String actionName;

    public ActionExecutionEvent(String actionName)
    {
        this.actionName = actionName;
    }

    public String getActionName()
    {
        return this.actionName;
    }

    public int hashCode()
    {
        return getActionName().hashCode();
    }

    public boolean equals(Object object)
    {
        return getActionName().equals(object);
    }

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
