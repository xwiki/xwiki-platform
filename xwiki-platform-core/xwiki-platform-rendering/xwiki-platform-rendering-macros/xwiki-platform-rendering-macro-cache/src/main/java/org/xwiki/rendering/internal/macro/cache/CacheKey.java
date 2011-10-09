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
package org.xwiki.rendering.internal.macro.cache;

/**
 * Key to recognize a given Cache. We need to have several caches since currently there's one cache per 
 * timeToLive/maxEntry combination. This is because currently we cannot set these configuration values at the cache
 * entry level but only for the whole cache.
 *
 * @version $Id$ 
 * @since 3.3M1
 */
public class CacheKey
{
    /**
     * @see #getTimeToLive()
     */
    private int timeToLive;

    /**
     * @see #getMaxEntries()
     */
    private int maxEntries;

    /**
     * @param timeToLive see {@link #getTimeToLive()}
     * @param maxEntries see {@link #getMaxEntries()}
     */
    public CacheKey(int timeToLive, int maxEntries)
    {
        this.timeToLive = timeToLive;
        this.maxEntries = maxEntries;
    }

    /**
     * @return the number of seconds to cache the content
     */
    public int getTimeToLive()
    {
        return this.timeToLive;
    }

    /**
     * @return the maximum number of entries in the cache (Least Recently Used entries are ejected)
     */
    public int getMaxEntries()
    {
        return this.maxEntries;
    }

    @Override
    public boolean equals(Object object)
    {
        boolean result;

        // See http://www.technofundo.com/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                // object must be Syntax at this point
                CacheKey cacheKey = (CacheKey) object;
                result = (getTimeToLive() == cacheKey.getTimeToLive() && getMaxEntries() == cacheKey.getMaxEntries());
            }
        }
        return result;
    }

    @Override
    public int hashCode()
    {
        // Random number. See http://www.technofundo.com/tech/java/equalhash.html for the detail of this
        // algorithm.
        int hash = 9;
        hash = 31 * hash + getTimeToLive();
        hash = 31 * hash + getMaxEntries();
        return hash;
    }
}
