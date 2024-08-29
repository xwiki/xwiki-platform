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
package com.xpn.xwiki.internal.doc;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Implement a list of {@link BaseObject} as seen by a document (meaning that the list index matched the object number)
 * using a {@link Map} storage to avoid wasting memory with null entries.
 * 
 * @version $Id$
 * @since 14.0RC1
 */
// TODO: expose APIs to navigate non null objects directly instead of going through each entry and skipping explicitly
// null ones
public class BaseObjects extends AbstractList<BaseObject>
{
    // Sort keys so that it's possible to navigate non null entries without going through them all
    private Map<Integer, BaseObject> map = new ConcurrentSkipListMap<>();

    private int size;

    /**
     * Constructs an empty list.
     */
    public BaseObjects()
    {

    }

    /**
     * Constructs a list containing the elements of the specified collection, in the order they are returned by the
     * collection's iterator.
     * 
     * @param collection the collection to copy
     */
    public BaseObjects(Collection<BaseObject> collection)
    {
        collection.forEach(this::add);
    }

    @Override
    public BaseObject get(int index)
    {
        rangeCheck(index);

        return this.map.get(index);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    private BaseObject put(int index, BaseObject element)
    {
        BaseObject old;
        if (element == null) {
            // We don't want to keep null values in memory
            old = this.map.remove(index);
        } else {
            // Make sure the object number is right
            element.setNumber(index);

            old = this.map.put(index, element);
        }

        // Increment size if needed
        if (this.size <= index) {
            this.size = index + 1;
        }

        return old;
    }

    @Override
    public void add(int index, BaseObject element)
    {
        // Check if the index is valid
        rangeCheckForAdd(index);

        // Shifts right values to the right
        if (index < this.size) {
            for (int i = this.size - 1; i >= index; --i) {
                put(i + 1, get(i));
            }
        }

        // Insert new value
        put(index, element);
    }

    @Override
    public BaseObject set(int index, BaseObject element)
    {
        // Check if the index is valid
        rangeCheck(index);

        // Set the value and remember the old one
        return put(index, element);
    }

    @Override
    public BaseObject remove(int index)
    {
        rangeCheck(index);

        BaseObject previous = this.map.remove(index);

        // Shifts right values to the left
        if (index < this.size - 1) {
            for (int i = index; i < this.size - 1; ++i) {
                put(i, get(i + 1));
            }
        }

        // The list is one element shorter
        --this.size;

        return previous;
    }

    @Override
    public void clear()
    {
        this.map.clear();
        this.size = 0;
    }

    private void rangeCheck(int index)
    {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    private void rangeCheckForAdd(int index)
    {
        if (index < 0 || index > this.size || index == Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    private String outOfBoundsMsg(int index)
    {
        return "Index: " + index + ", Size: " + size;
    }
}
