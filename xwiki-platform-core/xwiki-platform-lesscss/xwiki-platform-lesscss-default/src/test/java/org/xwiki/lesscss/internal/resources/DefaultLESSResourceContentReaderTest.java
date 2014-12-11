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
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceContentReader;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class DefaultLESSResourceContentReaderTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSResourceContentReader> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSResourceContentReader.class);

    @Test
    public void getContent() throws Exception
    {
        // Mock
        LESSResourceContentReader reader = mock(LESSSkinFileContentReader.class);
        mocker.registerComponent(LESSResourceContentReader.class, "org.xwiki.lesscss.resources.LESSSkinFileResourceReference",
            reader);
        when(reader.getContent(any(LESSResourceReference.class), anyString())).thenReturn("content");

        // Test
        assertEquals("content", mocker.getComponentUnderTest().getContent(new LESSSkinFileResourceReference("file"),
            "skin"));
    }

    @Test
    public void getContentWithUnsupportedResource() throws Exception
    {
        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(new LESSSkinFileResourceReference("file"), "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        assertNotNull(caughtException);
        assertEquals("This LESS Resource is not handled by the compiler.", caughtException.getMessage());
    }
}
