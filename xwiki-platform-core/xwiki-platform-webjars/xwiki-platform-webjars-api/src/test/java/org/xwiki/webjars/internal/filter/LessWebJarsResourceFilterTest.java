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
package org.xwiki.webjars.internal.filter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link LessWebJarsResourceFilter}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class LessWebJarsResourceFilterTest
{
    @InjectMockComponents
    private LessWebJarsResourceFilter resourceFilter;

    @MockComponent
    private LESSCompiler lessCompiler;

    @Test
    void handle() throws Exception
    {
        when(this.lessCompiler.compile(any(LESSResourceReference.class), eq(true), eq(false), eq(false)))
            .thenReturn("some content");

        try (InputStream stream = this.resourceFilter.filter(mock(InputStream.class), "resourceName.less")) {
            verify(this.lessCompiler).compile(any(LESSResourceReference.class), eq(true), eq(false), eq(false));
            assertEquals("some content", IOUtils.toString(stream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void handleLessError() throws Exception
    {
        when(this.lessCompiler.compile(any(LESSResourceReference.class), eq(true), eq(false), eq(false)))
            .thenThrow(LESSCompilerException.class);

        ResourceReferenceHandlerException exception = assertThrows(ResourceReferenceHandlerException.class,
            () -> this.resourceFilter.filter(mock(InputStream.class), "resourceName.less"));

        assertEquals("Error when compiling the resource [resourceName.less]", exception.getMessage());
        assertThat(exception.getCause(), instanceOf(LESSCompilerException.class));
    }
}
