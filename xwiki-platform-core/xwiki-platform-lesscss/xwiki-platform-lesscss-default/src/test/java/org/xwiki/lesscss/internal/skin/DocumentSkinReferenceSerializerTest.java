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
package org.xwiki.lesscss.internal.skin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.skin.DocumentSkinReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DocumentSkinReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentSkinReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(DocumentSkinReferenceSerializer.class);
    
    private EntityReferenceSerializer<String> entityReferenceSerializer;
    
    @Before
    public void setUp() throws Exception
    {
        entityReferenceSerializer =  mocker.getInstance(new DefaultParameterizedType(null, 
            EntityReferenceSerializer.class, String.class));
    }
    
    @Test
    public void serialize() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(entityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:space.page");
        
        assertEquals("SkinDocument[wiki:space.page]", 
                mocker.getComponentUnderTest().serialize(new DocumentSkinReference(documentReference)));
    }

    @Test
    public void serializeBadType() throws Exception
    {
        SkinReference skinReference = new FSSkinReference("skin");
        assertNull(mocker.getComponentUnderTest().serialize(skinReference));
        
        verify(mocker.getMockedLogger()).warn("Invalid LESS resource type [{}].", skinReference.toString());
    }
}
