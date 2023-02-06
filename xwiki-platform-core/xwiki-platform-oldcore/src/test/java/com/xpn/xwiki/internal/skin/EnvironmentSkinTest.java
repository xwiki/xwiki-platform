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
package com.xpn.xwiki.internal.skin;

import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.environment.Environment;
import org.xwiki.skin.Resource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EnvironmentSkin}.
 *
 * @version $Id$
 */
@ComponentTest
class EnvironmentSkinTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private EnvironmentSkin skin;

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp()
    {
        @SuppressWarnings("unchecked")
        Provider<XWikiContext> xcontextProvider = mock(Provider.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        URLConfiguration urlConfiguration = mock(URLConfiguration.class);

        this.skin = new EnvironmentSkin("test", mock(InternalSkinManager.class), mock(InternalSkinConfiguration.class),
            mock(Logger.class), this.environment, xcontextProvider, urlConfiguration);
    }

    @Test
    void getLocalResource() throws Exception
    {
        String relativePath = "o;ne/t?w&o/../t=hr#e e";
        String fullPath = "/skins/test/" + relativePath;
        when(this.environment.getResource(fullPath)).thenReturn(new URL("http://resourceURL"));

        Resource<?> resource = this.skin.getLocalResource(relativePath);
        assertEquals(fullPath, resource.getPath());
    }

    @Test
    void getLocalResourceWithBreakInAttempt()
    {
        assertNull(this.skin.getLocalResource("one/../../two"));
        assertEquals("Direct access to skin file [/skins/two] refused. Possible break-in attempt!",
            this.logCapture.getMessage(0));
    }

    @Test
    void exists() throws Exception
    {
        when(this.environment.getResource("/skins/test/skin.properties"))
            .thenReturn(new URL("http://resourceURL"), (URL) null);
        assertTrue(this.skin.exists());
        assertFalse(this.skin.exists());
    }
}
