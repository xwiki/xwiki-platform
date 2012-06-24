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
package org.xwiki.gwt.user.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.shared.GwtEvent;

/**
 * A semantic event which indicates that a component-defined action occurred.
 * 
 * @version $Id$
 */
public class ActionEvent extends GwtEvent<ActionHandler>
{
    /**
     * The association between action names and event types. We use this map to ensure that events with the same action
     * name have the same type.
     */
    private static final Map<String, Type<ActionHandler>> TYPE = new HashMap<String, Type<ActionHandler>>();

    /**
     * The name of the action that triggers this event.
     */
    private final String actionName;

    /**
     * Each event has its own type, based on the action name, to allow us to register handlers per action. Events with
     * the same action name have the same type.
     */
    private final Type<ActionHandler> type;

    /**
     * Creates a new event for the specified action.
     * 
     * @param actionName the name of the action that triggers this event
     */
    protected ActionEvent(String actionName)
    {
        this.actionName = actionName;
        type = ActionEvent.getType(actionName);
    }

    /**
     * @param actionName the name of an action
     * @return the event type associated with the specified action
     */
    public static Type<ActionHandler> getType(String actionName)
    {
        Type<ActionHandler> type = TYPE.get(actionName);
        if (type == null) {
            type = new Type<ActionHandler>();
            TYPE.put(actionName, type);
        }
        return type;
    }

    /**
     * Fires an action event on all registered handlers in the handler manager. If no such handlers exist, this method
     * will do nothing.
     * 
     * @param source the source of the handlers
     * @param actionName the name of the action that occurred
     * @return the fired event, or {@code null} if no handlers of the implied event type have been registered
     */
    public static ActionEvent fire(HasActionHandlers source, String actionName)
    {
        if (TYPE.containsKey(actionName)) {
            ActionEvent event = new ActionEvent(actionName);
            source.fireEvent(event);
            return event;
        }
        return null;
    }

    @Override
    protected void dispatch(ActionHandler handler)
    {
        handler.onAction(this);
    }

    @Override
    public Type<ActionHandler> getAssociatedType()
    {
        return type;
    }

    /**
     * @return the name of the action that triggered this event
     */
    public String getActionName()
    {
        return actionName;
    }
}
