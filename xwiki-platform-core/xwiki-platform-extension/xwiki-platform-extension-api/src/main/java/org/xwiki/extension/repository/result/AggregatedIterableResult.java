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
package org.xwiki.extension.repository.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Make several iterable results look like one.
 * 
 * @param <T> the type
 * @version $Id$
 */
public class AggregatedIterableResult<T> implements IterableResult<T>
{
    /**
     * The aggregated iterable results.
     */
    private List<IterableResult<T>> results = new ArrayList<IterableResult<T>>();

    /**
     * @see #getOffset()
     */
    private int offset;

    /**
     * Cached aggregated total hits.
     */
    private Integer totalHits;

    /**
     * Cached aggregated size.
     */
    private Integer size;

    /**
     * @param offset the initial offset
     */
    public AggregatedIterableResult(int offset)
    {
        this.offset = offset;
    }

    /**
     * @param result a iterable result instance to append
     */
    public void addSearchResult(IterableResult<T> result)
    {
        this.results.add(result);

        // Reset caches
        this.totalHits = null;
        this.size = null;
    }

    @Override
    public Iterator<T> iterator()
    {
        Collection<Iterator<T>> resultIterators = new ArrayList<Iterator<T>>();
        for (IterableResult<T> result : this.results) {
            resultIterators.add(result.iterator());
        }

        return new AggregatedIterator<T>(resultIterators.iterator());
    }

    @Override
    public int getTotalHits()
    {
        if (this.totalHits == null) {
            this.totalHits = 0;
            for (IterableResult<T> result : this.results) {
                this.totalHits += result.getTotalHits();
            }
        }

        return this.totalHits;
    }

    @Override
    public int getOffset()
    {
        return this.offset;
    }

    @Override
    public int getSize()
    {
        if (this.size == null) {
            this.size = 0;
            for (IterableResult<T> result : this.results) {
                this.size += result.getTotalHits();
            }
        }

        return this.size;
    }
}
