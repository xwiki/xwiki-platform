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
package org.xwiki.user.internal.document;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Helps implement Document-based User Reference Resolvers.
 *
 * @param <T> the type of the raw user reference
 * @version $Id$
 * @since 12.2RC1
 */
public abstract class AbstractUserReferenceResolver<T> implements UserReferenceResolver<T>
{
    private static final String GUEST_STRING = "XWikiGuest";

    private static final String SUPERADMIN_STRING = "superadmin";

    private boolean isGuest(String userName)
    {
        return GUEST_STRING.equalsIgnoreCase(userName);
    }

    private boolean isSuperAdmin(String userName)
    {
        return SUPERADMIN_STRING.equalsIgnoreCase(userName);
    }

    /**
     * @param userName the user id (e.g. for a full reference of {@code xwiki:XWiki.JohnDoe}, the id is
     *                  {@code JohnDoe}). If null or empty then resolve to the current user reference
     * @return the full User reference. Also handles Guest and SuperAdmin users.
     */
    protected UserReference resolveName(String userName)
    {
        UserReference reference = null;
        if (StringUtils.isEmpty(userName)) {
            reference = CurrentUserReference.INSTANCE;
        } else if (isGuest(userName)) {
            reference = GuestUserReference.INSTANCE;
        } else if (isSuperAdmin(userName)) {
            reference = SuperAdminUserReference.INSTANCE;
        }
        return reference;
    }
}
