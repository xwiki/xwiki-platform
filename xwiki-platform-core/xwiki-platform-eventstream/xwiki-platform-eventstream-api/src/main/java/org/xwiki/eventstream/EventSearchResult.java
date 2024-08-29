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

import java.util.stream.Stream;

import org.xwiki.eventstream.internal.EmptyEventSearchResult;

/**
 * The result of a search in the {@link EventStore}.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
public interface EventSearchResult extends AutoCloseable
{
    /**
     * An empty instance of {@link EventSearchResult}.
     */
    EventSearchResult EMPTY = EmptyEventSearchResult.INSTANCE;

    /**
     * @return the total number of possible results without offset or maximum results limits
     */
    long getTotalHits();

    /**
     * @return the index in the total number of possible search result where this extract starts
     */
    long getOffset();

    /**
     * @return the number of found events
     */
    long getSize();

    /**
     * @return a {@link Stream} containing the found events
     */
    Stream<Event> stream();
}
