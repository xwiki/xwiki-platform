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
 * Special event matcher used by a listener which need to listened to all possible events.
 * 
 * @version $Id$
 */
// TODO: introduce a generic multi-events matcher system to observation manager
public final class AllEvent implements Event
{
    /**
     * Unique instance of the event matcher.
     */
    public static final Event ALLEVENT = new AllEvent();

    /**
     * Use {@link #ALLEVENT} instead.
     */
    private AllEvent()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.Event#matches(java.lang.Object)
     */
    public boolean matches(Object otherEvent)
    {
        return true;
    }
}
