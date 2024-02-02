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
package org.xwiki.webjars.script;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.internal.WebjarUrlResolver;

import static org.mockito.Mockito.verify;

/**
 * Test of {@link WebJarsScriptService}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@ComponentTest
class WebJarsScriptServiceTest
{
    @InjectMockComponents
    private WebJarsScriptService webJarsScriptService;

    @MockComponent
    private WebjarUrlResolver webjarUrlResolver;

    @Test
    void urlWithResourceName()
    {
        this.webJarsScriptService.url("resourceName");
        verify(this.webjarUrlResolver).url("resourceName");
    }

    @Test
    void urlWithWebjarIdAndPath()
    {
        this.webJarsScriptService.url("webjarId", "path");
        verify(this.webjarUrlResolver).url("webjarId", "path");
    }

    @Test
    void urlWithWebjarIdNamespaceAndPath()
    {
        this.webJarsScriptService.url("webjarId", "ns", "path");
        verify(this.webjarUrlResolver).url("webjarId", "ns", "path");
    }

    @Test
    void urlWithWebjarIdPathAndParams()
    {
        this.webJarsScriptService.url("webjarId", "path", Map.of("a", "b"));
        verify(this.webjarUrlResolver).url("webjarId", "path", Map.of("a", "b"));

    }

    @Test
    void urlWithWebjarIdNamespacePathAndParams()
    {
        this.webJarsScriptService.url("webjarId", "ns", "path", Map.of("a", "b"));
        verify(this.webjarUrlResolver).url("webjarId", "ns","path", Map.of("a", "b"));
    }
}
