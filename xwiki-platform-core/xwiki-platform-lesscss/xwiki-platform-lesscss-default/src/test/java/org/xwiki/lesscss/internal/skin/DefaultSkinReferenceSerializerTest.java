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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.lesscss.skin.DocumentSkinReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultSkinReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultSkinReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(DefaultSkinReferenceSerializer.class);
    
    @Test
    public void serialize() throws Exception
    {
        // Mocks
        SkinReferenceSerializer fsSerializer = mock(SkinReferenceSerializer.class);
        mocker.registerComponent(SkinReferenceSerializer.class, FSSkinReference.class.getName(), fsSerializer);
        SkinReferenceSerializer documentSkinSerializer = mock(SkinReferenceSerializer.class);
        mocker.registerComponent(SkinReferenceSerializer.class, DocumentSkinReference.class.getName(),
            documentSkinSerializer);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentSkinReference documentSkinReference = new DocumentSkinReference(documentReference);
        FSSkinReference fsSkinReference = new FSSkinReference("skin");
        
        when(fsSerializer.serialize(fsSkinReference)).thenReturn("r1");
        when(documentSkinSerializer.serialize(documentSkinReference)).thenReturn("r2");
        
        // Test
        assertEquals("r1", mocker.getComponentUnderTest().serialize(fsSkinReference));
        assertEquals("r2", mocker.getComponentUnderTest().serialize(documentSkinReference));
        
    }

    @Test
    public void serializeBadType() throws Exception
    {
        FSSkinReference fsSkinReference = new FSSkinReference("skin");
        assertNull(mocker.getComponentUnderTest().serialize(fsSkinReference));
        
        verify(mocker.getMockedLogger()).warn(eq("The skin type [{}] is not handled by the LESS Module."),
            eq(fsSkinReference), any(ComponentLookupException.class));
    }
}
