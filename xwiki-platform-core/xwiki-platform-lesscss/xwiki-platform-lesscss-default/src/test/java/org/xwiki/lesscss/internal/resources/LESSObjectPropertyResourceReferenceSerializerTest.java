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
package org.xwiki.lesscss.internal.resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.resources.LESSObjectPropertyResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class LESSObjectPropertyResourceReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<LESSObjectPropertyResourceReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(LESSObjectPropertyResourceReferenceSerializer.class);

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Before
    public void setUp() throws Exception
    {
        entityReferenceSerializer = mocker.getInstance(new DefaultParameterizedType(null,
                EntityReferenceSerializer.class, String.class));
    }

    @Test
    public void serialize() throws Exception
    {
        // Mocks
        when(entityReferenceSerializer.serialize(any(ObjectPropertyReference.class))).thenReturn("object_code");

        // Test
        assertEquals("LessEntity[object_code]", mocker.getComponentUnderTest().serialize(
                new LESSObjectPropertyResourceReference(new ObjectPropertyReference("code", new ObjectReference("Object",
                new DocumentReference("wiki", "space", "page"))))));

        // Verify
        verifyZeroInteractions(mocker.getMockedLogger());
    }

    @Test
    public void serializeWithUnsupportedResource() throws Exception
    {
        // Test
        assertNull(mocker.getComponentUnderTest().serialize(new LESSResourceReference(){}));

        // Verify
        verify(mocker.getMockedLogger()).warn(eq("Invalid LESS resource type [{}]."), anyString());
    }
}
