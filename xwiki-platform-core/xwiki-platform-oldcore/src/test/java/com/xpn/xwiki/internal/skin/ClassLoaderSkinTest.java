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
import org.xwiki.skin.Resource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClassLoaderSkin}.
 *
 * @version $Id$
 * @since 13.8RC1
 */
@ComponentTest
class ClassLoaderSkinTest
{
    private ClassLoaderSkin skin;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private ClassLoader classLoader;

    @BeforeEach
    void setUp()
    {
        @SuppressWarnings("unchecked")
        Provider<XWikiContext> xcontextProvider = mock(Provider.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        URLConfiguration urlConfiguration = mock(URLConfiguration.class);

        this.skin = new ClassLoaderSkin("test", mock(InternalSkinManager.class), mock(InternalSkinConfiguration.class),
            mock(Logger.class), xcontextProvider, urlConfiguration, this.classLoader);
    }

    @Test
    void getLocalResource() throws Exception
    {
        String relativePath = "o;ne/t?w&o/../t=hr#e e";
        String fullPath = "skins/test/" + relativePath;

        doReturn(new URL("file:/skins/test/o;ne/t?w&o/../t=hr#e e"))
            .when(this.classLoader)
            .getResource("skins/test/o;ne/t?w&o/../t=hr#e e");

        Resource<?> resource = this.skin.getLocalResource(relativePath);
        assertEquals(fullPath, resource.getPath());
    }

    @Test
    void getLocalResourceWithBreakInAttempt()
    {
        assertNull(this.skin.getLocalResource("../../notskin/a"));
        assertEquals(1, this.logCapture.size());
        assertEquals("Direct access to skin file [notskin/a] refused. Possible break-in attempt!",
            this.logCapture.getMessage(0));
    }
}
