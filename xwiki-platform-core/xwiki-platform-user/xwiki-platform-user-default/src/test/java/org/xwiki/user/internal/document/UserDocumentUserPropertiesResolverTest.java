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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link UserDocumentUserPropertiesResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class UserDocumentUserPropertiesResolverTest
{
    @InjectMockComponents
    private UserDocumentUserPropertiesResolver resolver;

    @Test
    void resolve()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        UserProperties userProperties = this.resolver.resolve(new DocumentUserReference(documentReference, null));
        assertNotNull(userProperties);
    }

    @Test
    void resolveWhenNotDocumentReference()
    {
        UserReference userReference = mock(UserReference.class);
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(userReference));
        assertEquals("You need to pass a user reference of type "
            + "[org.xwiki.user.internal.document.DocumentUserReference]", exception.getMessage());
    }

    @Test
    void resolveWhenNullReference()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> this.resolver.resolve(null));
        assertEquals("You need to pass a user reference of type "
            + "[org.xwiki.user.internal.document.DocumentUserReference]", exception.getMessage());
    }
}
