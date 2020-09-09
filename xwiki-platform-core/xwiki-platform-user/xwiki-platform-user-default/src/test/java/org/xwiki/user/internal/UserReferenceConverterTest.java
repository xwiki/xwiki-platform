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

import java.lang.reflect.Type;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

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
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Test
    void convertToType()
    {
        UserReference userReference = () -> false;

        DocumentReference documentReference = new DocumentReference("s1", "XWiki", "U1");
        when(this.documentReferenceResolver.resolve("XWiki.U1")).thenReturn(documentReference);
        when(this.userReferenceResolver.resolve(documentReference)).thenReturn(userReference);

        UserReference actual = this.converter.convertToType(mock(Type.class), "XWiki.U1");
        assertEquals(userReference, actual);
    }

    @Test
    void convertToTypeValueNull()
    {
        assertNull(this.converter.convertToType(mock(Type.class), null));

    }
}