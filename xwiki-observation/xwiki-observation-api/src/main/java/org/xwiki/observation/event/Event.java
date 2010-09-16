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
 * All Event types must implement this interface.
 * 
 * @version $Id$
 */
public interface Event
{
    /**
     * Compares two events to see if they <em>match</em>, meaning that a listener that registered to receive
     * notifications <em>like</em> <code>referenceEvent</code> will be notified of any occuring event for which
     * <code>referenceEvent.matches(occuringEvent)</code> will return <code>true</code>. The matching algorithm depends
     * on the event implementation. For example for Document events two events match if they implement the same event
     * class and if their {@link org.xwiki.observation.event.filter.EventFilter} match. Note that the implementation is
     * left open in order to cater for all the possible ways this Observation component can be used.
     * 
     * @param otherEvent the occuring event matched against the current object
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    boolean matches(Object otherEvent);
}
