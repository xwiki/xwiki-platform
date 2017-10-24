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
import java.util.List;
import java.util.ListIterator;

/**
 * Generic list implementation with method to call when updating the list. This is used for the attachment list in
 * XWikiDocument.
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

    /** Called when the list is updated. The method will be called at least once, but may be called several times. */
    protected abstract void onUpdate();

    /**
     * @param element the element that just been added to the list
     * @since 9.10RC1
     */
    protected void added(E element)
    {
        // Should be overwritten by extending classes that need to know about new elements
    }

    @Override
    public boolean add(E e)
    {
        boolean ret = this.list.add(e);
        if (ret) {
            onUpdate();
            added(e);
        }
        return ret;
    }

    @Override
    public void add(int index, E element)
    {
        this.list.add(index, element);
        onUpdate();
        added(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        boolean ret = this.list.addAll(c);
        if (ret) {
            onUpdate();
            for (E e : c) {
                added(e);
            }
        }
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        boolean ret = this.list.addAll(index, c);
        if (ret) {
            onUpdate();
            for (E e : c) {
                added(e);
            }
        }
        return ret;
    }

    @Override
    public void clear()
    {
        this.list.clear();
        onUpdate();
    }

    @Override
    public boolean contains(Object o)
    {
        return this.list.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return this.list.containsAll(c);
    }

    @Override
    public boolean equals(Object o)
    {
        return this.list.equals(o);
    }

    @Override
    public E get(int index)
    {
        return this.list.get(index);
    }

    @Override
    public int hashCode()
    {
        return this.list.hashCode();
    }

    @Override
    public int indexOf(Object o)
    {
        return this.list.indexOf(o);
    }

    @Override
    public boolean isEmpty()
    {
        return this.list.isEmpty();
    }

    @Override
    public Iterator<E> iterator()
    {
        return new Itr(this.list.iterator());
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return this.list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator()
    {
        return new ListItr(this.list.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return new ListItr(this.list.listIterator(index));
    }

    @Override
    public E remove(int index)
    {
        E ret = this.list.remove(index);
        onUpdate();
        return ret;
    }

    @Override
    public boolean remove(Object o)
    {
        boolean ret = this.list.remove(o);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean ret = this.list.removeAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean ret = this.list.retainAll(c);
        if (ret) {
            onUpdate();
        }
        return ret;
    }

    @Override
    public E set(int index, E element)
    {
        E ret = this.list.set(index, element);
        onUpdate();
        added(element);
        return ret;
    }

    @Override
    public int size()
    {
        return this.list.size();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        return new AbstractNotifyOnUpdateList<E>(this.list.subList(fromIndex, toIndex))
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
        return this.list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return this.list.toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: We override the default {@link Object#toString()} implementation in order to preserve backwards
     * compatibility with code that was using {@link List#toString()} before we introduced
     * {@link AbstractNotifyOnUpdateList}.
     * </p>
     *
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-9013">XWIKI-9013: Multiple Select List values are not correctly
     *      indexed by Lucene</a>
     */
    @Override
    public String toString()
    {
        return this.list.toString();
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
        Itr(Iterator<E> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public E next()
        {
            return this.iterator.next();
        }

        @Override
        public void remove()
        {
            this.iterator.remove();
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
        ListItr(ListIterator<E> iterator)
        {
            super(iterator);
            this.iterator = iterator;
        }

        @Override
        public void add(E e)
        {
            this.iterator.add(e);
            onUpdate();
            added(e);
        }

        @Override
        public int nextIndex()
        {
            return this.iterator.nextIndex();
        }

        @Override
        public boolean hasPrevious()
        {
            return this.iterator.hasPrevious();
        }

        @Override
        public E previous()
        {
            return this.iterator.previous();
        }

        @Override
        public int previousIndex()
        {
            return this.iterator.previousIndex();
        }

        @Override
        public void set(E e)
        {
            this.iterator.set(e);
            onUpdate();
            added(e);
        }
    }
}
