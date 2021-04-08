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
package org.xwiki.user.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

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
class UserReferenceConverterTest
{
    @InjectMockComponents
    private UserReferenceConverter converter;

    @MockComponent
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Test
    void convertToType()
    {
        UserReference userReference = () -> false;
        when(this.userReferenceResolver.resolve("space.userPage")).thenReturn(userReference);

        UserReference result = this.converter.convertToType(null, "space.userPage");
        assertEquals(userReference, result);
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
        String convertedReference = this.converter.convert(String.class, userReference);
        assertEquals("space.userPage", convertedReference);
    }
}