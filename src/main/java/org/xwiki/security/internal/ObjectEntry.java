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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.security.RightCacheEntry;

import java.util.Collection;
import java.util.LinkedList;

/**
 * A right cache entry that caches objects, which may be of type
 * RightsObject or GroupObject.
 * @version $Id$
 */
class ObjectEntry implements RightCacheEntry
{
    /** The list of objects. */
    private final Collection objects = new LinkedList();

    /**
     * Add an object to this entry.
     * @param o The object to add.
     */
    public void addObject(Object o)
    {
        objects.add(o);
    }

    @Override
    public Type getType()
    {
        return Type.HAVE_OBJECTS;
    }

    @Override
    public <T> Collection<T> getObjects(Class<T> type)
    {
        Collection<T> matchingObjects = new LinkedList();
        for (Object o : objects) {
            if (type.isInstance(o)) {
                matchingObjects.add(type.cast(o));
            }
        }
        return matchingObjects;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof ObjectEntry && objects.equals(((ObjectEntry) other).objects);
    }

    @Override
    public int hashCode()
    {
        return objects.hashCode();
    }
}