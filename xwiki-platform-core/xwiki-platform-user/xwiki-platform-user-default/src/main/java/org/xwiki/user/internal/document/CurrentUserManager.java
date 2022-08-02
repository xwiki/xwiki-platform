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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * CRUD operations for the current user.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named("org.xwiki.user.CurrentUserReference")
@Singleton
public class CurrentUserManager implements UserManager
{
    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    @Named("org.xwiki.user.internal.document.DocumentUserReference")
    private UserManager documentUserManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public boolean exists(UserReference userReference) throws UserException
    {
        boolean exists;

        // Note: the passed userReference is always CurrentUserReference.INSTANCE since this user manager is called
        // only in this case. That's why it's not used.

        // If there's no user in the context, then it means guest and thus it doesn't exist.
        DocumentReference currentUserReference = getXWikiContext().getUserReference();
        if (currentUserReference == null) {
            exists = false;
        } else {
            // Resolve the current user reference into a real reference.
            UserReference resolvedUserReference = this.userReferenceResolver.resolve(currentUserReference);
            if (SuperAdminUserReference.INSTANCE == resolvedUserReference
                || GuestUserReference.INSTANCE == resolvedUserReference)
            {
                exists = false;
            } else {
                exists = this.documentUserManager.exists(resolvedUserReference);
            }
        }
        return exists;
    }

    private XWikiContext getXWikiContext()
    {
        return this.contextProvider.get();
    }

}
