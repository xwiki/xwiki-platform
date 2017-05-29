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
package org.xwiki.eventstream;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provide a description for a specific implementation of RecordableEvent so that users can knows what the event is
 * about.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
@Unstable
public interface RecordableEventDescriptor
{
    /**
     * The different filters we support to retrieve event.
     * @since 9.4RC1
     */
    enum EventFilter {
        /** There is no filter (except the event type) on this kind of events. */
        NO_FILTER,
        /** There is a custom filter on this kind of events. */
        CUSTOM_FILTER,
        /** There is a filter to enable the event on some wikis. */
        WIKI_FILTER,
        /** There is a filter to enable the event on some wikis and some spaces. */
        WIKI_AND_SPACE_FILTER,
        /** There is a filter to enable the event on some wikis, some spaces and/or some pages. */
        WIKI_SPACE_AND_DOCUMENT_FILTER
    }

    /**
     * @return the name of the event described by the descriptor, as it is stored in the event stream.
     */
    String getEventType();

    /**
     * @return the name of the application that provide this event
     */
    String getApplicationName();

    /**
     * @return the description of the event type
     */
    String getDescription();

    /**
     * @return the icon corresponding to the application
     */
    String getApplicationIcon();

    /**
     * @return the kind of filter to use for this event type
     * @since 9.4RC1
     */
    default EventFilter getFilter() {
        return EventFilter.NO_FILTER;
    }
}
