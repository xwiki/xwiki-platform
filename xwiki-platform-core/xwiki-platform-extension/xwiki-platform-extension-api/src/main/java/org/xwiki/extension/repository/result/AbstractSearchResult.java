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

/**
 * Utility base class for {@link IterableResult} implementations.
 * 
 * @param <T> the type
 * @version $Id$
 */
public abstract class AbstractSearchResult<T> implements IterableResult<T>
{
    /**
     * @see #getTotalHits()
     */
    private int totalHits;

    /**
     * @see #getOffset()
     */
    private int offset;

    /**
     * @param totalHits the total number of possible results without offset or maximum results limits
     * @param offset the index in the total number of possible search result where this extract starts
     */
    public AbstractSearchResult(int totalHits, int offset)
    {
        this.totalHits = totalHits;
        this.offset = offset;
    }

    @Override
    public int getTotalHits()
    {
        return this.totalHits;
    }

    @Override
    public int getOffset()
    {
        return this.offset;
    }
}
