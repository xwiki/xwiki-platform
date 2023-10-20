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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentStringUserReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentStringUserReferenceSerializerTest
{
    @InjectMockComponents
    private DocumentStringUserReferenceSerializer serializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Test
    void serialize()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "user");
        DocumentUserReference documentUserReference = new DocumentUserReference(documentReference, true);

        this.serializer.serialize(documentUserReference);

        verify(this.entityReferenceSerializer).serialize(documentReference);
    }

    @Test
    void serializeWhenNotDocumentUserReference()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class,
            () -> this.serializer.serialize(mock(UserReference.class)));
        assertEquals("Only DocumentUserReference are handled", exception.getMessage());
    }

    @Test
    void serializeWhenNull()
    {
        assertNull(this.serializer.serialize(null));
    }

    @Test
    void serializeSuperAdminReference()
    {
        assertEquals("XWiki.superadmin", this.serializer.serialize(SuperAdminUserReference.INSTANCE));
    }

    @Test
    void serializeGuestReference()
    {
        assertEquals("XWiki.XWikiGuest", this.serializer.serialize(GuestUserReference.INSTANCE));
    }

    @Test
    void serializeCurrentUserReferenceWhenCurrentUserExists()
    {
        DocumentUserReference userReference = mock(DocumentUserReference.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "user");
        when(userReference.getReference()).thenReturn(documentReference);
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(userReference);
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:space.user");

        assertEquals("wiki:space.user", this.serializer.serialize(CurrentUserReference.INSTANCE));
    }

    @Test
    void serializeCurrentUserReferenceWhenNoCurrentUser()
    {
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);

        assertEquals("XWiki.XWikiGuest", this.serializer.serialize(CurrentUserReference.INSTANCE));
    }

    @Test
    void serializeCurrentUserReferenceWhenCurrentUserIsSuperAdmin()
    {
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(SuperAdminUserReference.INSTANCE);

        assertEquals("XWiki.superadmin", this.serializer.serialize(CurrentUserReference.INSTANCE));
    }
}
