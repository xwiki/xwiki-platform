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
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class LESSSkinFileResourceReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<LESSSkinFileResourceReferenceSerializer> mocker =
            new MockitoComponentMockingRule<>(LESSSkinFileResourceReferenceSerializer.class);

    @Test
    public void serialize() throws Exception
    {
        // Test
        assertEquals("LessSkinFile[myFile.less]", mocker.getComponentUnderTest().serialize(
                new LESSSkinFileResourceReference("myFile.less")));

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
