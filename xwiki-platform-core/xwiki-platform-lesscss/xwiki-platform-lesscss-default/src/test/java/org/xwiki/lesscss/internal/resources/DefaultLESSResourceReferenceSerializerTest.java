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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSResourceReferenceSerializer;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class DefaultLESSResourceReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSResourceReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSResourceReferenceSerializer.class);

    @Test
    public void serialize() throws Exception
    {
        // Mocks
        LESSResourceReferenceSerializer serializer = mock(LESSResourceReferenceSerializer.class);
        when(serializer.serialize(any(LESSResourceReference.class))).thenReturn("serialized resource");
        mocker.registerComponent(LESSResourceReferenceSerializer.class,
            "org.xwiki.lesscss.LESSSkinFileResourceReference", serializer);

        // Test
        assertEquals("serialized resource",
                mocker.getComponentUnderTest().serialize(new LESSSkinFileResourceReference("file")));

        // Verify
        verifyZeroInteractions(mocker.getMockedLogger());
    }

    @Test
    public void serializeWithUnsupportedLESSResource() throws Exception
    {
        // Test
        assertNull(mocker.getComponentUnderTest().serialize(new LESSSkinFileResourceReference("file")));

        // Verify
        verify(mocker.getMockedLogger()).warn(eq("The LESS Resource Reference [{}] is not handled by the compiler."),
            eq(new LESSSkinFileResourceReference("file")), any(ComponentLookupException.class));
    }
}
