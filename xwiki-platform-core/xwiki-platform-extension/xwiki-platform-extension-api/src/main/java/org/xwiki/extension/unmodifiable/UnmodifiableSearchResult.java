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
package org.xwiki.extension.unmodifiable;

import java.util.Iterator;

import org.xwiki.extension.Extension;
import org.xwiki.extension.repository.search.SearchResult;

/**
 * Provide a readonly access to a search result.
 * 
 * @param <E> the extension type
 * @version $Id$
 */
public class UnmodifiableSearchResult<E extends Extension> implements SearchResult<E>
{
    /**
     * The wrapped result.
     */
    private SearchResult<E> result;

    /**
     * @param result the wrapped result
     */
    public UnmodifiableSearchResult(SearchResult<E> result)
    {
        this.result = result;
    }

    @Override
    public Iterator<E> iterator()
    {
        return new UnmodifiableExtensionIterator<E>(result.iterator());
    }

    @Override
    public int getTotalHits()
    {
        return this.result.getTotalHits();
    }

    @Override
    public int getOffset()
    {
        return this.result.getOffset();
    }

    @Override
    public int getSize()
    {
        return this.result.getSize();
    }

}
