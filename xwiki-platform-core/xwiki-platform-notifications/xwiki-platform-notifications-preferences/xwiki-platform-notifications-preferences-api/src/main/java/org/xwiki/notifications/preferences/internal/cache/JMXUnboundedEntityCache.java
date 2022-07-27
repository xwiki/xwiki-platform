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
package org.xwiki.notifications.preferences.internal.cache;

/**
 * Expose an unbounded entity cache as JMX mbean.
 * 
 * @param <T> the type of data stored in a cache
 * @version $Id$
 * @since 13.8RC1
 * @since 13.4.4
 * @since 12.10.10
 */
public class JMXUnboundedEntityCache<T> implements JMXUnboundedEntityCacheMBean
{
    private final UnboundedEntityCache<T> cache;

    /**
     * @param cache the cache
     */
    public JMXUnboundedEntityCache(UnboundedEntityCache<T> cache)
    {
        this.cache = cache;
    }

    @Override
    public int getSize()
    {
        return this.cache.getCache().size();
    }

    @Override
    public void clear()
    {
        this.cache.getCache().clear();
    }
}
