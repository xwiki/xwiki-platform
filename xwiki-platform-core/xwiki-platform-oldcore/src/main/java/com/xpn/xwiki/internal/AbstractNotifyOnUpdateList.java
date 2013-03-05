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
package com.xpn.xwiki.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;

/**
 * Generic list implementation with method to call when updating the list.
 *
 * This is used for the attachment list in XWikiDocument.
 *
 * @version $Id$ 
 * @since 4.3M2
 * @param <E> type parameter.
 */
public abstract class AbstractNotifyOnUpdateList<E> implements List<E>
{

    /** The list to wrap. */
    private final List<E> list;

    /**
     * @param list the list to wrap.
     */
    protected AbstractNotifyOnUpdateList(List<E> list)
    {
        this.list = list;
    }

    /** Called when the list is updated.  The method will be called at least once, but may be called several times. */
    protected abstract void onUpdate();

    @Override
    public boolean add(E e)
    {
        boolean ret = list.add(e);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public void add(int index, E element)
    {
        list.add(index, element);
        onUpdate();
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        boolean ret = list.addAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        boolean ret = list.addAll(index, c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public void clear()
    {
        list.clear();
        onUpdate();
    }

    @Override
    public boolean contains(Object o)
    {
        return list.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return list.containsAll(c);
    }

    @Override
    public boolean equals(Object o)
    {
        return list.equals(o);
    }

    @Override
    public E get(int index)
    {
        return list.get(index);
    }

    @Override
    public int hashCode()
    {
        return list.hashCode();
    }

    @Override
    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }

    @Override
    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    @Override
    public Iterator<E> iterator()
    {
        return new Itr(list.iterator());
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return new ListItr(list.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return new ListItr(list.listIterator(index));
    }

    @Override
    public E remove(int index)
    {
        E ret = list.remove(index);
        onUpdate();
        return ret;
    }

    @Override
    public boolean remove(Object o)
    {
        boolean ret = list.remove(o);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean ret = list.removeAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean ret = list.retainAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public E set(int index, E element)
    {
        E ret = list.set(index, element);
        onUpdate();
        return ret;
    }

    @Override
    public int size()
    {
        return list.size();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        return new AbstractNotifyOnUpdateList<E>(list.subList(fromIndex, toIndex))
        {
            @Override
            public void onUpdate()
            {
                AbstractNotifyOnUpdateList.this.onUpdate();
            }
        };
    }

    @Override
    public Object[] toArray()
    {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return list.toArray(a);
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