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
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.SecurityAccessEntry;

/**
 * Loads access and rule entries into the security cache.
 * It depends on a {@link org.xwiki.security.authorization.SecurityEntryReader}
 * for reading rules missing from the cache, and on a
 * {@link org.xwiki.security.authorization.AuthorizationSettler} for resolving
 * access from rules.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface SecurityCacheLoader
{
    /**
     * Load the cache with the required entries to look up the access
     * for a given user on a given entity.
     *
     * @param user The user to check access for.
     * @param entity The entity to check access to.
     * @return The resulting access level for the user at the entity.
     * @exception org.xwiki.security.authorization.AuthorizationException if an error occurs.
     */
    SecurityAccessEntry load(UserSecurityReference user, SecurityReference entity)
        throws AuthorizationException;
}
