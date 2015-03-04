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
import org.xwiki.lesscss.skin.DocumentSkinReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class FSSkinReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<FSSkinReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(FSSkinReferenceSerializer.class);
    
    @Test
    public void serialize() throws Exception
    {
        assertEquals("SkinFS[skin]", mocker.getComponentUnderTest().serialize(new FSSkinReference("skin")));
    }

    @Test
    public void serializeBadType() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        SkinReference skinReference = new DocumentSkinReference(documentReference);
        assertNull(mocker.getComponentUnderTest().serialize(skinReference));
        
        verify(mocker.getMockedLogger()).warn("Invalid LESS resource type [{}].", skinReference.toString());
    }
}
