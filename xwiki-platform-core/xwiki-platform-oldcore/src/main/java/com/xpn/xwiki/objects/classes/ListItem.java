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
package com.xpn.xwiki.objects.classes;

import java.util.Comparator;

import org.apache.commons.collections.ComparatorUtils;

/**
 * An entry in a List or in a Tree.
 * 
 * @version $Id$
 */
public class ListItem
{
    /** Comparator that orders two strings in their natural order, keeping nulls at the end. */
    @SuppressWarnings("unchecked")
    private static final Comparator<String> BASE_COMPARATOR = ComparatorUtils
        .nullHighComparator(new Comparator<String>()
        {
            /**
             * Case insensitive comparison of two Strings.
             * 
             * @param o1 the first item to be compared.
             * @param o2 the second item to be compared.
             * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
             *         greater than the second.
             */
            @Override
            public int compare(String o1, String o2)
            {
                return o1.compareToIgnoreCase(o2);
            }
        });

    /** Comparator that orders list items on their identifiers, keeping null items at the end. */
    @SuppressWarnings("unchecked")
    protected static final Comparator<ListItem> ID_COMPARATOR = ComparatorUtils
        .nullHighComparator(new Comparator<ListItem>()
        {
            /**
             * Sorts the items on their ID: the option with the lower ID (case insensitive String comparison) will be
             * placed before the other one.
             * 
             * @param o1 the first item to be compared.
             * @param o2 the second item to be compared.
             * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
             *         greater than the second.
             */
            @Override
            public int compare(ListItem o1, ListItem o2)
            {
                return BASE_COMPARATOR.compare(o1.getId(), o2.getId());
            }
        });

    /** Comparator that orders list items on their values, keeping null items at the end. */
    @SuppressWarnings("unchecked")
    protected static final Comparator<ListItem> VALUE_COMPARATOR = ComparatorUtils
        .nullHighComparator(new Comparator<ListItem>()
        {
            /**
             * Sorts the items on their value: the option with the lower value (case insensitive String comparison) will
             * be placed before the other one.
             * 
             * @param o1 the first item to be compared.
             * @param o2 the second item to be compared.
             * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
             *         greater than the second.
             */
            @Override
            public int compare(ListItem o1, ListItem o2)
            {
                return BASE_COMPARATOR.compare(o1.getValue(), o2.getValue());
            }
        });

    /** A unique identifier of this item, the actual value stored in the database when selecting items from a list. */
    private String id = "";

    /** A user-friendly value that gets displayed in the user interface, representing this item. */
    private String value = "";

    /** An optional reference to another item that allows to build parent-child relations, forming a tree. */
    private String parent = "";

    /**
     * Constructor that initializes both the {@link #id internal ID} and the {@link #value displayed value} with the
     * same value, leaving the {@link #parent} field empty.
     * 
     * @param id the value to use for the id and the displayed value
     */
    public ListItem(String id)
    {
        this.setId(id);
        this.setValue(id);
    }

    /**
     * Constructor that initializes the {@link #id internal ID} and the {@link #value displayed value}, leaving the
     * {@link #parent} field empty.
     * 
     * @param id the value to use for the internal id
     * @param value the value to use for the displayed value
     */
    public ListItem(String id, String value)
    {
        this.setId(id);
        this.setValue(value);
    }

    /**
     * Constructor that initializes all of the {@link #id internal ID}, the {@link #value displayed value}, and the
     * {@link #parent} fields.
     * 
     * @param id the value to use for the internal id
     * @param value the value to use for the displayed value
     * @param parent the value to use for the item's parent
     */
    public ListItem(String id, String value, String parent)
    {
        this.setId(id);
        this.setValue(value);
        this.setParent(parent);
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getParent()
    {
        return this.parent;
    }

    public void setParent(String parent)
    {
        this.parent = parent;
    }

    @Override
    public String toString()
    {
        return "[" + getId() + ", " + getValue() + ", " + getParent() + "]";
    }
}
