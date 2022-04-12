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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
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
import static org.mockito.Mockito.when;

@ComponentTest
public class DocumentDocumentReferenceUserReferenceSerializerTest
{
    @InjectMockComponents
    private DocumentDocumentReferenceUserReferenceSerializer serializer;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @BeforeEach
    void setup()
    {
        when(this.entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(
            new EntityReference("xwiki", EntityType.WIKI));
    }

    @Test
    void serializeWhenSuperAdmin()
    {
        DocumentReference expected = new DocumentReference("xwiki", "XWiki", "superadmin");
        assertEquals(expected, this.serializer.serialize(SuperAdminUserReference.INSTANCE));
    }

    @Test
    void serializeWhenGuest()
    {
        assertNull(this.serializer.serialize(GuestUserReference.INSTANCE));
    }

    @Test
    void serializeWhenNull()
    {
        DocumentUserReference currentUserReference = mock(DocumentUserReference.class);
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "XWiki", "user");
        when(currentUserReference.getReference()).thenReturn(currentDocumentReference);
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(currentUserReference);

        assertEquals(currentDocumentReference, this.serializer.serialize(null));
    }

    @Test
    void serializeWhenNotDocumentUserReference()
    {
        UserReference userReference = mock(UserReference.class);
        Throwable exception = assertThrows(RuntimeException.class,
            () -> this.serializer.serialize(userReference));
        assertEquals("Passed user reference must be of type [org.xwiki.user.internal.document.DocumentUserReference]",
            exception.getMessage());
    }

    @Test
    void serializeWhenCurrentUserReferenceIsGuest()
    {
        when(this.currentUserReferenceUserReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);
        assertNull(this.serializer.serialize(null));
    }
}
