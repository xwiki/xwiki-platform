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

import java.util.Collection;
import java.util.Iterator;

/**
 * A simple collection based {@link IterableResult}.
 * 
 * @param <T> the type
 * @version $Id$
 */
public class CollectionIterableResult<T> extends AbstractSearchResult<T>
{
    /**
     * The wrapped result.
     */
    private Collection<T> result;

    /**
     * @param totalHits the total number of possible results without offset or maximum results limits
     * @param offset the index in the total number of possible search result where this extract starts
     * @param result the actual results
     */
    public CollectionIterableResult(int totalHits, int offset, Collection<T> result)
    {
        super(totalHits, offset);

        this.result = result;
    }

    @Override
    public Iterator<T> iterator()
    {
        return this.result.iterator();
    }

    @Override
    public int getSize()
    {
        return this.result.size();
    }
}
