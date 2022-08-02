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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Converts a {@link UserReference} into a {@link DocumentReference}.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named("document")
@Singleton
public class DocumentDocumentReferenceUserReferenceSerializer implements UserReferenceSerializer<DocumentReference>
{
    private static final String XWIKI_SPACE = "XWiki";

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Override
    public DocumentReference serialize(UserReference userReference)
    {
        DocumentReference result;
        UserReference normalizedUserReference = userReference;
        if (userReference == null || CurrentUserReference.INSTANCE == normalizedUserReference) {
            normalizedUserReference = this.currentUserReferenceUserReferenceResolver.resolve(null);
        }

        if (SuperAdminUserReference.INSTANCE == normalizedUserReference) {
            result = new DocumentReference(
                this.entityReferenceProvider.getDefaultReference(EntityType.WIKI).getName(), XWIKI_SPACE, "superadmin");
        } else if (GuestUserReference.INSTANCE == normalizedUserReference) {
            result = null;
        } else {
            if (!(normalizedUserReference instanceof DocumentUserReference)) {
                throw new IllegalArgumentException(String.format("Passed user reference must be of type [%s]",
                    DocumentUserReference.class.getName()));
            }
            result = ((DocumentUserReference) normalizedUserReference).getReference();
        }
        return result;
    }
}
