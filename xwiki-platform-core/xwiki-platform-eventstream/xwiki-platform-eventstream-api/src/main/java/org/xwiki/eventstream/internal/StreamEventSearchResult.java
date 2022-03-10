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
 * {@link Iterable} based implementation of {@link EventSearchResult}.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
public class StreamEventSearchResult implements EventSearchResult
{
    private final long totalHits;

    private final long offset;

    private final long size;

    private final Stream<Event> stream;

    /**
     * @param totalHits the total number of possible results without offset or maximum results limits
     * @param offset the index in the total number of possible search result where this extract starts
     * @param size the number of found events
     * @param stream the stream to navigate
     */
    public StreamEventSearchResult(long totalHits, long offset, long size, Stream<Event> stream)
    {
        this.totalHits = totalHits;
        this.offset = offset;
        this.size = size;

        this.stream = stream;
    }

    @Override
    public long getTotalHits()
    {
        return this.totalHits;
    }

    @Override
    public long getOffset()
    {
        return this.offset;
    }

    @Override
    public long getSize()
    {
        return this.size;
    }

    @Override
    public Stream<Event> stream()
    {
        return this.stream;
    }

    @Override
    public void close() throws Exception
    {

    }
}
