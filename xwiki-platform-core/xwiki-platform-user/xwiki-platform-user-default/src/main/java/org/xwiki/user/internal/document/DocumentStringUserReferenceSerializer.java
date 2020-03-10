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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Converts a {@link DocumentUserReference} into a String representation.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named("document")
@Singleton
public class DocumentStringUserReferenceSerializer implements UserReferenceSerializer<String>
{
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public String serialize(UserReference userReference)
    {
        String result;
        if (userReference == null) {
            result = null;
        } else if (UserReference.SUPERADMIN_REFERENCE == userReference) {
            result = "XWiki.superadmin";
        } else if (UserReference.GUEST_REFERENCE == userReference) {
            result = "XWiki.XWikiGuest";
        } else if (UserReference.CURRENT_USER_REFERENCE == userReference) {
            result = "";
        } else if (!(userReference instanceof DocumentUserReference)) {
            throw new IllegalArgumentException("Only DocumentUserReference are handled");
        } else {
            DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
            result = this.entityReferenceSerializer.serialize(documentUserReference.getReference());
        }
        return result;
    }
}
