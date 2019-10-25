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
package org.xwiki.tree.internal;

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EntityTreeNodeIdConverter}.
 * 
 * @version $Id$
 * @since 11.10RC1
 */
@ComponentTest
public class EntityTreeNodeIdConverterTest
{
    @InjectMockComponents
    private EntityTreeNodeIdConverter converter;

    @MockComponent
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Test
    public void convertNullToString()
    {
        assertNull(this.converter.convertToString(null));
    }

    @Test
    public void convertToString()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.defaultEntityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:Path.To.Page");
        assertEquals("document:wiki:Path.To.Page", this.converter.convertToString(documentReference));

        ClassPropertyReference classPropertyReference = new ClassPropertyReference("age", documentReference);
        when(this.defaultEntityReferenceSerializer.serialize(classPropertyReference))
            .thenReturn("wiki:Path.To.Page^age");
        assertEquals("classProperty:wiki:Path.To.Page^age", this.converter.convertToString(classPropertyReference));
    }

    @Test
    public void convertToEntityReferenceWithoutEntityType()
    {
        assertNull(this.converter.convertToType(null, "test"));
    }

    @Test
    public void convertToEntityReferenceWithInvalidEntityType()
    {
        assertNull(this.converter.convertToType(null, "foo:bar"));
    }

    @Test
    public void convertToEntityReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page", EntityType.DOCUMENT))
            .thenReturn(documentReference);
        assertEquals(documentReference, this.converter.convertToType(null, "document:wiki:Path.To.Page"));

        ClassPropertyReference classPropertyReference = new ClassPropertyReference("age", documentReference);
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page^age", EntityType.CLASS_PROPERTY))
            .thenReturn(classPropertyReference);
        assertEquals(classPropertyReference, this.converter.convertToType(null, "classProperty:wiki:Path.To.Page^age"));
    }
}
