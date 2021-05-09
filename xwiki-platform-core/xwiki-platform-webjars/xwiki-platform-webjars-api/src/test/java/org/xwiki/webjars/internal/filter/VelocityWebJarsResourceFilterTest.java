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
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link VelocityWebJarsResourceFilter}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class VelocityWebJarsResourceFilterTest
{
    @InjectMockComponents
    private VelocityWebJarsResourceFilter resourceFilter;

    @MockComponent
    private VelocityManager velocityManager;

    @Mock
    private VelocityEngine velocityEngine;

    @Mock
    private VelocityContext velocityContext;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);
        when(this.velocityManager.getVelocityContext()).thenReturn(this.velocityContext);
    }

    @Test
    void handle() throws Exception
    {
        when(this.velocityEngine
            .evaluate(eq(this.velocityContext), any(Writer.class), eq("resourceName.css"), any(Reader.class)))
            .thenAnswer(invocation -> {
                IOUtils.write("some content", (Writer) invocation.getArgument(1));
                return true;
            });

        try (InputStream handle = this.resourceFilter.filter(mock(InputStream.class), "resourceName.css")) {
            verify(this.velocityEngine)
                .evaluate(eq(this.velocityContext), any(Writer.class), eq("resourceName.css"), any(Reader.class));
            assertEquals("some content", IOUtils.toString(handle, StandardCharsets.UTF_8));
        }
    }

    @Test
    void handlerVelocityError() throws Exception
    {
        when(this.velocityEngine
            .evaluate(eq(this.velocityContext), any(Writer.class), eq("resourceName.css"), any(Reader.class)))
            .thenThrow(XWikiVelocityException.class);

        ResourceReferenceHandlerException exception =
            assertThrows(ResourceReferenceHandlerException.class,
                () -> this.resourceFilter.filter(mock(InputStream.class), "resourceName.css"));

        assertEquals("Failed to evaluate the Velocity code from WebJar resource [resourceName.css]",
            exception.getMessage());
        assertThat(exception.getCause(), instanceOf(XWikiVelocityException.class));
    }
}