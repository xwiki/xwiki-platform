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
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Resolve a current user reference to a {@link DocumentReference} for the user in the context.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Singleton
public class CurrentUserReferenceResolver implements UserReferenceResolver<CurrentUserReference>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Override
    public UserReference resolve(CurrentUserReference unused, Object... parameters)
    {
        UserReference userReference;
        DocumentReference currentDocumentReference = getXWikiContext().getUserReference();
        // If there's no user in the context then we consider the current user is the Guest user.
        if (currentDocumentReference == null) {
            userReference = GuestUserReference.INSTANCE;
        } else {
            userReference = this.documentReferenceUserReferenceResolver.resolve(currentDocumentReference);
        }
        return userReference;
    }

    private XWikiContext getXWikiContext()
    {
        return this.contextProvider.get();
    }
}
