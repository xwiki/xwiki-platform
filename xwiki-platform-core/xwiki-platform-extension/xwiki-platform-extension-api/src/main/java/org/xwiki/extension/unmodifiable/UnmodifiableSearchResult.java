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
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.wrap.AbstractWrappingObject;

/**
 * Provide a readonly access to a search result.
 * 
 * @param <E> the extension type
 * @version $Id$
 */
public class UnmodifiableSearchResult<E extends Extension> extends AbstractWrappingObject<IterableResult<E>> implements
    IterableResult<E>
{
    /**
     * @param result the wrapped result
     */
    public UnmodifiableSearchResult(IterableResult<E> result)
    {
        super(result);
    }

    @Override
    public Iterator<E> iterator()
    {
        return new UnmodifiableExtensionIterator<E>(getWrapped().iterator());
    }

    @Override
    public int getTotalHits()
    {
        return getWrapped().getTotalHits();
    }

    @Override
    public int getOffset()
    {
        return getWrapped().getOffset();
    }

    @Override
    public int getSize()
    {
        return getWrapped().getSize();
    }

}
