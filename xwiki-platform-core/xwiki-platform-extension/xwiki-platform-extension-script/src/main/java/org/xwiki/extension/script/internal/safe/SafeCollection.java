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
package org.xwiki.extension.script.internal.safe;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;

import org.xwiki.extension.internal.safe.AbstractSafeObject;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a collection.
 * 
 * @param <E> the type of the elements in the list
 * @param <C> the type of the Collection
 * @version $Id$
 * @since 4.1M1
 */
public class SafeCollection<E, C extends Collection<E>> extends AbstractSafeObject<C> implements Collection<E>
{
    /**
     * Safe implementation of the iterator elements.
     */
    protected Constructor< ? extends E> safeConstructor;

    /**
     * @param collection the wrapped list
     * @param safeProvider the provider of instances safe for public scripts
     * @param safeConstructor the cinstructor to use to create new instance of list elements
     */
    public SafeCollection(C collection, ScriptSafeProvider< ? > safeProvider, Constructor< ? extends E> safeConstructor)
    {
        super(collection, safeProvider);

        this.safeConstructor = safeConstructor;
    }

    /**
     * @param element the element to wrap
     * @return the wrapped element
     */
    protected E safeElement(E element)
    {
        if (this.safeConstructor != null) {
            try {
                return this.safeConstructor.newInstance(element, this.safeProvider);
            } catch (Exception e) {
                return safe(element);
            }
        } else {
            return safe(element);
        }
    }

    // List

    @Override
    public int size()
    {
        return getWrapped().size();
    }

    @Override
    public boolean isEmpty()
    {
        return getWrapped().isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return getWrapped().contains(o);
    }

    @Override
    public Iterator<E> iterator()
    {
        return new SafeIterator<E, Iterator<E>>(getWrapped().iterator(), this.safeProvider, this.safeConstructor);
    }

    @Override
    public Object[] toArray()
    {
        Object[] copy = getWrapped().toArray();

        for (int i = 0; i < copy.length; ++i) {
            copy[i] = safe(copy[i]);
        }

        return copy;
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        T[] copy = getWrapped().toArray(a);

        for (int i = 0; i < copy.length; ++i) {
            copy[i] = safe(copy[i]);
        }

        return copy;
    }

    @Override
    public boolean add(E e)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public boolean containsAll(Collection< ? > c)
    {
        return getWrapped().containsAll(c);
    }

    @Override
    public boolean addAll(Collection< ? extends E> c)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public boolean removeAll(Collection< ? > c)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public boolean retainAll(Collection< ? > c)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }
}
