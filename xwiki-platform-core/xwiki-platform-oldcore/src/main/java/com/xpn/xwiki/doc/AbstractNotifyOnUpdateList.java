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
package com.xpn.xwiki.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Generic list implementation with method to call when updating the list.
 *
 * This is used for the attachment list in XWikiDocument.
 *
 * @version $Id$ 
 * @since 4.3M2
 * @param <E> type parameter.
 */
public abstract class AbstractNotifyOnUpdateList<E> extends ArrayList<E>
{

    /** Called when the list is updated.  The method will be called at least once, but may be called several times. */
    protected abstract void onUpdate();

    @Override
    public boolean add(E e)
    {
        boolean ret = super.add(e);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public void add(int index, E element)
    {
        super.add(index, element);
        onUpdate();
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        boolean ret = super.addAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        boolean ret = super.addAll(index, c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public void clear()
    {
        super.clear();
        onUpdate();
    }

    @Override
    public Iterator<E> iterator()
    {
        return new Itr(super.iterator());
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return new ListItr(super.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return new ListItr(super.listIterator(index));
    }

    @Override
    public E remove(int index)
    {
        E ret = super.remove(index);
        onUpdate();
        return ret;
    }

    @Override
    public boolean remove(Object o)
    {
        boolean ret = super.remove(o);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean ret = super.removeAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public E set(int index, E element)
    {
        E ret = super.set(index, element);
        onUpdate();
        return ret;
    }

    /**
     * Wrap iterator.
     */
    private class Itr implements Iterator<E>
    {

        /** Wrapped iterator. */
        private final Iterator<E> iterator;

        /**
         * @param iterator wrapped iteratlr
         */
        public Itr(Iterator<E> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public E next()
        {
            return iterator.next();
        }

        @Override
        public void remove()
        {
            iterator.remove();
            onUpdate();
        }

    }

    /**
     * Wrap list iterator.
     */
    private class ListItr extends Itr implements ListIterator<E>
    {

        /** The wrapped iterator. */
        private final ListIterator<E> iterator;

        /**
         * @param iterator wrapped iterator.
         */
        public ListItr(ListIterator<E> iterator) {
            super(iterator);
            this.iterator = iterator;
        }

        @Override
        public void add(E e)
        {
            iterator.add(e);
            onUpdate();
        }

        @Override
        public int nextIndex()
        {
            return iterator.nextIndex();
        }

        @Override
        public boolean hasPrevious()
        {
            return iterator.hasPrevious();
        }

        @Override
        public E previous()
        {
            return iterator.previous();
        }

        @Override
        public int previousIndex()
        {
            return iterator.previousIndex();
        }

        @Override
        public void set(E e)
        {
            iterator.set(e);
            onUpdate();
        }
    }
}