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
package org.xwiki.user.internal.converter;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link UserReferenceConverter}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
@ComponentList({CurrentUserReferenceConverter.class, DocumentUserReferenceConverter.class,
    GuestUserReferenceConverter.class, SuperAdminUserReferenceConverter.class, ContextComponentManagerProvider.class})
class UserReferenceConverterTest
{
    @InjectMockComponents
    private DefaultConverterManager converterManager;

    @InjectMockComponents
    private UserReferenceConverter converter;

    @InjectMockComponents
    private DocumentReferenceConverter documentReferenceConverter;

    @MockComponent
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserReferenceSerializer;

    @Test
    void convertToType()
    {
        UserReference userReference = () -> false;
        when(this.userReferenceResolver.resolve("space.userPage")).thenReturn(userReference);

        UserReference result = this.converter.convertToType(null, "space.userPage");
        assertEquals(userReference, result);
    }

    @Test
    void convertToDocumentReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "user");
        UserReference userReference = new DocumentUserReference(documentReference, false);

        when(this.documentUserReferenceSerializer.serialize(userReference)).thenReturn(documentReference);

        DocumentReference result = this.converterManager.convert(DocumentReference.class, userReference);
        assertEquals(documentReference, result);
    }

    @Test
    void convertToTypeValueNull()
    {
        assertNull(this.converter.convertToType(null, null));
    }

    @Test
    void convertToString()
    {
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("space.userPage");
        assertEquals("space.userPage", this.converter.convert(String.class, userReference));

        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn("currentuser");
        assertEquals("currentuser", this.converterManager.convert(String.class, CurrentUserReference.INSTANCE));

        when(this.userReferenceSerializer.serialize(GuestUserReference.INSTANCE)).thenReturn("guestuser");
        assertEquals("guestuser", this.converterManager.convert(String.class, GuestUserReference.INSTANCE));

        when(this.userReferenceSerializer.serialize(SuperAdminUserReference.INSTANCE)).thenReturn("superadmin");
        assertEquals("superadmin", this.converterManager.convert(String.class, SuperAdminUserReference.INSTANCE));

        userReference = new DocumentUserReference(new DocumentReference("wiki", "space", "user"), false);
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("superadmin");
        assertEquals("superadmin", this.converterManager.convert(String.class, userReference));
    }
}
