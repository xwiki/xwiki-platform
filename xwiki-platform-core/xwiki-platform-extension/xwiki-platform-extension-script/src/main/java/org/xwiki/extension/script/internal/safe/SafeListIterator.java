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
import java.util.ListIterator;

import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an iterator.
 * 
 * @param <E> the type of the iterated value
 * @version $Id$
 * @since 4.1M1
 */
public class SafeListIterator<E> extends SafeIterator<E, ListIterator<E>> implements ListIterator<E>
{
    /**
     * @param it the wrapped iterator
     * @param safeProvider the provider of instances safe for public scripts
     * @param safeConstructor the constructor used to create new safe wrapper for iterator elements
     */
    public SafeListIterator(ListIterator<E> it, ScriptSafeProvider< ? > safeProvider,
        Constructor< ? extends E> safeConstructor)
    {
        super(it, safeProvider, safeConstructor);
    }

    @Override
    public boolean hasPrevious()
    {
        return getWrapped().hasPrevious();
    }

    @Override
    public E previous()
    {
        return safeElement(getWrapped().previous());
    }

    @Override
    public int nextIndex()
    {
        return getWrapped().nextIndex();
    }

    @Override
    public int previousIndex()
    {
        return getWrapped().previousIndex();
    }

    @Override
    public void set(E e)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public void add(E e)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }
}
