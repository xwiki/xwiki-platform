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
package org.xwiki.url.script;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLSecurityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link URLSecurityScriptService}.
 *
 * @version $Id$
 * @since 14.10.4
 * @since 15.0RC1
 */
@ComponentTest
class URLSecurityScriptServiceTest
{
    @InjectMockComponents
    private URLSecurityScriptService scriptService;

    @MockComponent
    private URLSecurityManager urlSecurityManager;

    @Test
    void isURITrusted() throws URISyntaxException
    {
        String location = "//xwiki.org/xwiki/something/";
        URI expectedURI = new URI(location);
        when(this.urlSecurityManager.parseToSafeURI(location)).thenReturn(expectedURI);
        assertEquals(expectedURI, this.scriptService.parseToSafeURI(location));
    }
}