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
package com.xpn.xwiki.objects;

import java.util.Comparator;

import org.apache.commons.collections.ComparatorUtils;

/**
 * Compare and sort instances of ElementInterface by name.
 * 
 */
public class ElementComparator implements Comparator
{ 
    /**
     * Compares two objects (that implement ElementInterface) by name according 
     * to the rules for the compare method.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Object o1, Object o2)
    {
        // Get a null comparator that is backed by a static "natural" comparator
        Comparator c = ComparatorUtils.nullLowComparator(null);
        
        // convert o1 and o2 into the string names when not null
        Object no1 = ( o1 == null ) ? null : ((ElementInterface) o1).getName();
        Object no2 = ( o2 == null ) ? null : ((ElementInterface) o2).getName();
        
        // let the null comparator handle possible null values, where null < non-null string
        return c.compare(no1, no2);
    }

}
