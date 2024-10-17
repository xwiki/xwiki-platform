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
package org.xwiki.websocket.script;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketScriptService}.
 * 
 * @version $Id$
 */
@ComponentTest
class WebSocketScriptServiceTest
{
    @InjectMockComponents
    private WebSocketScriptService service;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setup() throws Exception
    {
        XWiki wiki = mock(XWiki.class);
        when(wiki.getWebAppPath(this.xcontext)).thenReturn("xwiki/");
        when(this.xcontext.getWiki()).thenReturn(wiki);

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(urlFactory.getServerURL(this.xcontext)).thenReturn(new URL("http://www.xwiki.org:80"));
        when(this.xcontext.getURLFactory()).thenReturn(urlFactory);

        when(this.xcontext.getWikiId()).thenReturn("test");
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void urlWithPath()
    {
        assertEquals("ws://www.xwiki.org:80/xwiki/websocket/one/two", this.service.url("/one/two"));
    }

    @Test
    void urlWithPathSecure() throws Exception
    {
        when(this.xcontext.getURLFactory().getServerURL(this.xcontext)).thenReturn(new URL("https://www.xwiki.org"));

        assertEquals("wss://www.xwiki.org/xwiki/websocket/hint", this.service.url("/hint"));
    }

    @Test
    void urlWithRoleHint()
    {
        assertEquals("ws://www.xwiki.org:80/xwiki/websocket/test/one%2Ftwo", this.service.url("one/two"));
    }

    @Test
    void urlWithException() throws Exception
    {
        when(this.xcontext.getURLFactory().getServerURL(this.xcontext))
            .thenThrow(new MalformedURLException("Invalid server URL!"));

        assertNull(this.service.url("test"));
        assertEquals(
            "Failed to create WebSocket URL for [test]. Root cause is [MalformedURLException: Invalid server URL!].",
            this.logCapture.getMessage(0));
    }

    @Test
    void urlWithRootServletContext() {
        when(this.xcontext.getWiki().getWebAppPath(xcontext)).thenReturn("/");
        assertEquals("ws://www.xwiki.org:80/websocket/one/two", this.service.url("/one/two"));
    }

}
