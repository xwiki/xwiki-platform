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
package org.xwiki.user;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link UserReferenceSerializer}.
 * 
 * @version $Id$
 */
@ComponentTest
class UserReferenceSerializerTest
{
    @Component
    @Named("test")
    @Singleton
    public static class TestStringUserReferenceSerializer implements UserReferenceSerializer<String>
    {
        @Override
        public String serialize(UserReference userReference)
        {
            return "test";
        }
    }

    @Component
    @Named("test")
    @Singleton
    public static class TestDocumentReferenceUserReferenceSerializer
        implements UserReferenceSerializer<DocumentReference>
    {
        @Override
        public DocumentReference serialize(UserReference userReference)
        {
            return new DocumentReference("wiki", "Space", "Page");
        }
    }

    @Test
    void stringType(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(TestStringUserReferenceSerializer.class);
        UserReferenceSerializer<String> serializer =
            componentManager.getInstance(UserReferenceSerializer.TYPE_STRING, "test");
        assertEquals("test", serializer.serialize(GuestUserReference.INSTANCE, "foo", "bar"));
    }

    @Test
    void documentReferenceType(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(TestDocumentReferenceUserReferenceSerializer.class);
        UserReferenceSerializer<DocumentReference> serializer =
            componentManager.getInstance(UserReferenceSerializer.TYPE_DOCUMENT_REFERENCE, "test");
        assertEquals(new DocumentReference("wiki", "Space", "Page"),
            serializer.serialize(SuperAdminUserReference.INSTANCE));
    }
}
