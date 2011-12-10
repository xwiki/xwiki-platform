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
package org.xwiki.extension.repository.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.xwiki.extension.Extension;

/**
 * Make several search results look like one.
 * 
 * @version $Id$
 */
public class AggregatedSearchResult implements SearchResult<Extension>
{
    /**
     * The aggregated search results.
     */
    private List<SearchResult<Extension>> results = new ArrayList<SearchResult<Extension>>();

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
    public AggregatedSearchResult(int offset)
    {
        this.offset = offset;
    }

    /**
     * @param result a search result instance to append
     */
    public void addSearchResult(SearchResult< ? extends Extension> result)
    {
        this.results.add((SearchResult<Extension>) result);

        // Reset caches
        this.totalHits = null;
        this.size = null;
    }

    @Override
    public Iterator<Extension> iterator()
    {
        Collection<Iterator<Extension>> resultItarators = new ArrayList<Iterator<Extension>>();
        for (SearchResult<Extension> result : this.results) {
            resultItarators.add(result.iterator());
        }

        return new AggregatedIterator<Extension>(resultItarators.iterator());
    }

    @Override
    public int getTotalHits()
    {
        if (this.totalHits == null) {
            this.totalHits = 0;
            for (SearchResult<Extension> result : this.results) {
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
            for (SearchResult<Extension> result : this.results) {
                this.size += result.getTotalHits();
            }
        }

        return this.size;
    }
}
