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

package org.xwiki.security.authorization.cache;

import org.xwiki.component.annotation.Role;
import org.xwiki.observation.EventListener;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;

/**
 * A cache for fast access right rules checking.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface SecurityCache
{
    /**
     * The priority used by the listener in charge of invalidating this cache.
     * 
     * @since 15.4RC1
     */
    int CACHE_INVALIDATION_PRIORITY = EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY;

    /**
     * Get a cached entry.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    SecurityAccessEntry get(UserSecurityReference user, SecurityReference entity);

    /**
     * Get a cached entry.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    SecurityRuleEntry get(SecurityReference entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(UserSecurityReference user, SecurityReference entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(SecurityReference entity);
}
