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
     * @param userName the user id (e.g. for a full reference of {@code xwiki:XWiki.JohnDoe}, the id is {@code JohnDoe})
     * @return the full User reference. Also handles Guest and SuperAdmin users.
     */
    protected UserReference resolveName(String userName)
    {
        UserReference reference = null;
        if (isGuest(userName)) {
            reference = UserReference.GUEST_REFERENCE;
        } else if (isSuperAdmin(userName)) {
            reference = UserReference.SUPERADMIN_REFERENCE;
        }
        return reference;
    }
}
