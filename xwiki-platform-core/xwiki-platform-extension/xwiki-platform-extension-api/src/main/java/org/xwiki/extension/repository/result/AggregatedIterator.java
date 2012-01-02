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

import java.util.Iterator;

/**
 * Makes several consecutive {@link Iterator}s look like one.
 * 
 * @param <T> the type of the iterated values
 * @version $Id$
 */
public class AggregatedIterator<T> implements Iterator<T>
{
    /**
     * The iterator of iterators.
     */
    private Iterator<Iterator<T>> iterators;

    /**
     * The current iterator.
     */
    private Iterator<T> currentIterator;

    /**
     * @param iterators the iterators to aggregated
     */
    public AggregatedIterator(Iterator<Iterator<T>> iterators)
    {
        this.iterators = iterators;
        this.currentIterator = iterators.next();
    }

    @Override
    public boolean hasNext()
    {
        boolean hasNext = this.currentIterator.hasNext();

        if (!hasNext && this.iterators.hasNext()) {
            this.currentIterator = this.iterators.next();
            hasNext = this.currentIterator.hasNext();
        }

        return hasNext;
    }

    @Override
    public T next()
    {
        boolean hasNext = this.currentIterator.hasNext();

        if (!hasNext && this.iterators.hasNext()) {
            this.currentIterator = this.iterators.next();
        }

        return this.currentIterator.next();
    }

    @Override
    public void remove()
    {
        this.currentIterator.remove();

        if (!this.currentIterator.hasNext() && this.iterators.hasNext()) {
            this.currentIterator = this.iterators.next();
        }
    }
}
