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
package org.xwiki.security;

import java.util.Collection;
import java.util.Collections;

/**
 * Representation of an entry in the {@link RightCheckerCache}.
 * @version $Id: $
 */
public interface RightCacheEntry
{
    /** Represents the type of cache entry. */
    enum Type {
        /** The entry (wiki, space, or document) have an associated rights object. */
        HAVE_OBJECTS,
        /** The entry (wiki, space, or document) does not have an associated rights object. */
        HAVE_NO_OBJECTS,
        /** The entry stores an access level for a particular user on a particular entity. */
        ACCESS_LEVEL 
    };

    /** Instance for indicating that a rights object do not exist for the entry. */
    RightCacheEntry HAVE_NO_RIGHT_OBJECT_ENTRY = new RightCacheEntry() {
            @Override
            public Type getType()
            {
                return Type.HAVE_NO_OBJECTS;
            }
        
            @Override
            public <T> Collection<T> getObjects(Class<T> type)
            {
                return Collections.EMPTY_SET;
            }
        };

    /**
     * @return The type of this cache entry.
     */
    Type getType();

    /**
     * Return all objects of the given type.
     * @param type The type.
     * @param <T> Type of objects to match.
     * @return An iterable over the objects.
     */
    <T> Collection<T> getObjects(Class<T> type);
}
