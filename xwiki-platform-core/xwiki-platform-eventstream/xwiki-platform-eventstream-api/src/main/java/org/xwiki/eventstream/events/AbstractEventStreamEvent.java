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
package org.xwiki.eventstream.events;

import java.io.Serializable;

/**
 * Abstract class for every event that is related to the event store.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public abstract class AbstractEventStreamEvent implements org.xwiki.observation.event.Event, Serializable
{
    /**
     * Used to provide a key to a property in the current execution context that avoids stepping into a loop when
     * triggering new events.
     * 
     * @deprecated not set anymore
     */
    @Deprecated
    public static final String EVENT_LOOP_CONTEXT_LOCK_PROPERTY = "eventLoopContextLockProperty";

    private static final long serialVersionUID = 1L;

    @Override
    public boolean matches(Object o)
    {
        return (this.getClass().isInstance(o));
    }
}
