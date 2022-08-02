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
package org.xwiki.security.authorization.testwikis.internal.entities;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An generic implementation for providing read-only collection of object assignable to type T from any collection
 * of elements extending type T.
 *
 * @param <T> Type element of the source collection that are kept in the filtered collection.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TypeFilteredCollection<T> extends AbstractCollection<T>
{
    /** The source collection. */
    private final Collection<?> source;

    /** The type to filter. */
    private final Class<T> type;

    /**
     * Protected constructor.
     * @param source the source collection to be filtered
     * @param type the type to keep in the resulting collection
     */
    TypeFilteredCollection(Collection<?> source, Class<T> type) {
        this.source = source;
        this.type = type;
    }

    /**
     * Create a new filtered collection based on a source collection and a type to filter.
     * @param source the source collection to be filtered
     * @param type the type to keep in the resulting collection
     * @param <T> the type to keep in the resulting collection
     * @return a read-only wrapped collection that contains only element of the given type.
     */
    public static <T> TypeFilteredCollection<T> getNewInstance(Collection<?> source, Class<T> type) {
        return new TypeFilteredCollection<T>(source, type);
    }

    @Override
    public int size()
    {
        int count = 0;
        for (Object entity : source) {
            if (type.isAssignableFrom(entity.getClass())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new TypeFilteredIterator<T>();
    }

    /**
     * Specialized iterator for filtering on Type.
     * @param <T> the type to keep during iteration.
     */
    class TypeFilteredIterator<T> implements Iterator<T>
    {
        /** Iterator on the source collection. */
        private final Iterator<?> it = source.iterator();

        /** Temporary cache of the next available object during iteration. */
        private Object next;

        @Override
        public boolean hasNext()
        {
            if (next == null) {
                do {
                    try {
                        next = it.next();
                    } catch (NoSuchElementException e) {
                        next = null;
                    }
                } while (next != null && !(type.isAssignableFrom(next.getClass())));
            }
            return next != null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next()
        {
            if (hasNext()) {
                T result = (T) next;
                next = null;
                return result;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
