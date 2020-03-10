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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.user.UserReference;

/**
 * Converts a {@link DocumentReference} into a {@link UserReference}.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named("document")
@Singleton
public class DocumentDocumentReferenceUserReferenceResolver extends AbstractUserReferenceResolver<DocumentReference>
{
    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Override
    public UserReference resolve(DocumentReference rawReference, Object... parameters)
    {
        UserReference reference;
        if (rawReference == null) {
            reference = UserReference.CURRENT_USER_REFERENCE;
        } else {
            reference = resolveName(rawReference.getName());
            if (reference == null) {
                reference = new DocumentUserReference(rawReference, this.entityReferenceProvider);
            }
        }
        return reference;
    }
}
