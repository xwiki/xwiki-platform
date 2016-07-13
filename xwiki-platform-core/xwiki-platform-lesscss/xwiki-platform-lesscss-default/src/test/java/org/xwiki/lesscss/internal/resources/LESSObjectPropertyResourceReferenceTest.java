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
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LESSObjectPropertyResourceReference}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
public class LESSObjectPropertyResourceReferenceTest
{
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private DocumentAccessBridge bridge;
    
    @Before
    public void SetUp() throws Exception
    {
        entityReferenceSerializer = mock(EntityReferenceSerializer.class);
        bridge = mock(DocumentAccessBridge.class);
    }
    
    @Test
    public void getContent() throws Exception
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference("property",
                new ObjectReference("class", new DocumentReference("wiki", "Space", "Document")));
        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference
                = new LESSObjectPropertyResourceReference(objectPropertyReference, entityReferenceSerializer, bridge);

        // Mock
        when(bridge.getProperty(eq(objectPropertyReference))).thenReturn("content");
        
        // Test
        assertEquals("content", lessObjectPropertyResourceReference.getContent("skin"));
    }

    @Test
    public void serialize() throws Exception
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference("property",
                new ObjectReference("class", new DocumentReference("wiki", "Space", "Document")));
        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference
                = new LESSObjectPropertyResourceReference(objectPropertyReference, entityReferenceSerializer, bridge);

        // Mock
        when(entityReferenceSerializer.serialize(eq(objectPropertyReference))).thenReturn("objPropertyRef");

        // Test
        assertEquals("LessXObjectProperty[objPropertyRef]", lessObjectPropertyResourceReference.serialize());
    }
}
