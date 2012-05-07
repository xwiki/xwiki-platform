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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a list.
 * 
 * @param <E> the type of the elements in the list
 * @param <L> the type of the List
 * @version $Id$
 * @since 4.1M1
 */
public class SafeList<E, L extends List<E>> extends SafeCollection<E, L> implements List<E>
{
    /**
     * @param list the wrapped list
     * @param safeProvider the provider of instances safe for public scripts
     * @param safeConstructor the cinstructor to use to create new instance of list elements
     */
    public SafeList(L list, ScriptSafeProvider< ? > safeProvider, Constructor< ? extends E> safeConstructor)
    {
        super(list, safeProvider, safeConstructor);
    }

    // List

    @Override
    public boolean addAll(int index, Collection< ? extends E> c)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public E get(int index)
    {
        return safeElement(getWrapped().get(index));
    }

    @Override
    public E set(int index, E element)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public void add(int index, E element)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public E remove(int index)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public int indexOf(Object o)
    {
        return getWrapped().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return getWrapped().lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return new SafeListIterator<E>(getWrapped().listIterator(), this.safeProvider, this.safeConstructor);
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return new SafeListIterator<E>(getWrapped().listIterator(index), this.safeProvider, this.safeConstructor);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        List<E> list = getWrapped().subList(fromIndex, toIndex);

        List<E> safeList = new ArrayList<E>(list.size());
        for (E element : list) {
            safeList.add(safeElement(element));
        }

        return safeList;
    }
}
