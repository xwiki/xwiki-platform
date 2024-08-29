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
package org.xwiki.user.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;

/**
 * Document-based implementation of {@link UserManager} which proxies to the specific UserManager implementation
 * based on the type of the passed {@link UserReference}.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Singleton
public class DefaultUserManager implements UserManager
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public boolean exists(UserReference userReference) throws UserException
    {
        boolean exists;

        // Handle special cases
        UserReference normalizedUserReference = userReference;
        if (normalizedUserReference == null) {
            normalizedUserReference = CurrentUserReference.INSTANCE;
        }
        if (GuestUserReference.INSTANCE == normalizedUserReference
            || SuperAdminUserReference.INSTANCE == normalizedUserReference)
        {
            exists = false;
        } else {
            exists = resolveUserManager(normalizedUserReference).exists(normalizedUserReference);
        }
        return exists;
    }

    private UserManager resolveUserManager(UserReference userReference)
    {
        try {
            return this.componentManager.getInstance(UserManager.class, userReference.getClass().getName());
        } catch (ComponentLookupException e) {
            // If there's no manager for the passed UserReference type, then the XWiki instance cannot work and thus
            // we need to fail hard and fast. Hence the runtime exception.
            throw new RuntimeException(String.format(
                "Failed to find user manager for role [%s] and hint [%s]", UserManager.class.getName(),
                userReference.getClass().getName()), e);
        }
    }
}
