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
package org.xwiki.eventstream.internal;

import java.util.stream.Stream;

import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;

/**
 * An empty {@link EventSearchResult}.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
public class EmptyEventSearchResult implements EventSearchResult
{
    /**
     * An empty instance of {@link EventSearchResult}.
     */
    public static final EmptyEventSearchResult INSTANCE = new EmptyEventSearchResult();

    /**
     * Create an empty search result. There is no reason to make this constructor public since it's always the same
     * thing
     */
    protected EmptyEventSearchResult()
    {
    }

    @Override
    public void close() throws Exception
    {
        // Nothing to do
    }

    @Override
    public long getTotalHits()
    {
        return 0;
    }

    @Override
    public long getOffset()
    {
        return 0;
    }

    @Override
    public long getSize()
    {
        return 0;
    }

    @Override
    public Stream<Event> stream()
    {
        return Stream.empty();
    }
}
