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

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link UserReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
class UserReferenceResolverTest
{
    @Component
    @Named("test")
    @Singleton
    public static class TestStringUserReferenceResolver implements UserReferenceResolver<String>
    {
        @Override
        public UserReference resolve(String rawReference, Object... parameters)
        {
            return GuestUserReference.INSTANCE;
        }
    }

    @Component
    @Named("test")
    @Singleton
    public static class TestDocumentReferenceUserReferenceResolver implements UserReferenceResolver<DocumentReference>
    {
        @Override
        public UserReference resolve(DocumentReference rawReference, Object... parameters)
        {
            return SuperAdminUserReference.INSTANCE;
        }
    }

    @Test
    void stringType(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(TestStringUserReferenceResolver.class);
        UserReferenceResolver<String> resolver =
            componentManager.getInstance(UserReferenceResolver.TYPE_STRING, "test");
        assertSame(GuestUserReference.INSTANCE, resolver.resolve("one", "two", "three"));
    }

    @Test
    void documentReferenceType(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(TestDocumentReferenceUserReferenceResolver.class);
        UserReferenceResolver<DocumentReference> resolver =
            componentManager.getInstance(UserReferenceResolver.TYPE_DOCUMENT_REFERENCE, "test");
        assertSame(SuperAdminUserReference.INSTANCE, resolver.resolve(new DocumentReference("wiki", "Space", "Page")));
    }
}
