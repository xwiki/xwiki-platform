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
package org.xwiki.websocket.internal;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.context.Execution;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWebSocketContext}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({CompactWikiStringEntityReferenceSerializer.class, CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class, DefaultSymbolScheme.class, DefaultExecution.class,
    LocalStringEntityReferenceSerializer.class})
class DefaultWebSocketContextTest
{
    @InjectMockComponents
    private DefaultWebSocketContext webSocketContext;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private Execution execution;

    @MockComponent
    private Container container;

    @MockComponent
    private XWikiStubContextProvider xcontextProvider;

    @Mock
    private ServerEndpointConfig endPointConfig;

    private Map<String, Object> userProperties = new HashMap<>();

    @Mock
    private HandshakeRequest request;

    @Mock
    private HandshakeResponse response;

    private XWikiContext xcontext = new XWikiContext();

    @Mock
    private Session session;

    private DocumentReference currentUserReference = new DocumentReference("test", "Users", "Alice");

    @BeforeEach
    void setup() throws Exception
    {
        Utils.setComponentManager(this.componentManager);

        when(this.endPointConfig.getUserProperties()).thenReturn(this.userProperties);
        when(this.session.getUserProperties()).thenReturn(this.userProperties);

        when(this.request.getHttpSession()).thenReturn(mock(HttpSession.class));

        XWiki wiki = mock(XWiki.class);
        when(wiki.checkAuth(this.xcontext)).thenReturn(new XWikiUser(this.currentUserReference));
        this.xcontext.setWiki(wiki);
        when(this.xcontextProvider.createStubContext()).thenReturn(this.xcontext);

        this.execution = this.componentManager.getInstance(Execution.class);
    }

    @Test
    void run() throws Exception
    {
        when(this.endPointConfig.getPath()).thenReturn("/one/{roleHint}/two/{wiki}/three");
        when(this.request.getRequestURI()).thenReturn(new URI("ws://www.xwiki.org/xwiki/one/echo/two/dev/three"));

        assertNull(this.xcontext.getWikiId());
        assertNull(this.xcontext.getUserReference());

        this.webSocketContext.initialize(this.endPointConfig, this.request, this.response);
        this.webSocketContext.run(this.session, () -> {
            assertEquals("dev", this.xcontext.getWikiId());
            assertEquals(this.currentUserReference, this.xcontext.getUserReference());
            assertEquals("ws://www.xwiki.org/xwiki/one/echo/two/dev/three", this.xcontext.getRequest().getRequestURI());
        });

        assertNull(this.execution.getContext());

        verify(this.container, times(2)).setRequest(any(ServletRequest.class));
        verify(this.container, times(2)).setResponse(any(ServletResponse.class));
        verify(this.container, times(2)).setSession(any(ServletSession.class));
    }

    @Test
    void runBeforeXWikiIsReady() throws Exception
    {
        when(this.endPointConfig.getPath()).thenReturn("/one/{roleHint}/two/{wiki}/three");
        when(this.request.getRequestURI()).thenReturn(new URI("ws://www.xwiki.org/xwiki/one/echo/two/dev/three"));

        // XWiki is not ready yet.
        when(this.xcontextProvider.createStubContext()).thenReturn(null);

        this.webSocketContext.initialize(this.endPointConfig, this.request, this.response);
        this.webSocketContext.run(this.session, () -> {
            // XWiki context is not published on the execution context.
            assertFalse(this.execution.getContext().hasProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
        });

        verify(this.container, never()).setRequest(any(ServletRequest.class));
        verify(this.container, never()).setResponse(any(ServletResponse.class));
        verify(this.container, never()).setSession(any(ServletSession.class));
    }

    @Test
    void call() throws Exception
    {
        when(this.endPointConfig.getPath()).thenReturn("/one/{roleHint}/two");
        when(this.request.getRequestURI()).thenReturn(new URI("ws://www.xwiki.org/xwiki/one/echo/two"));
        when(this.request.getParameterMap())
            .thenReturn(Collections.singletonMap("wiki", Collections.singletonList("test")));

        assertNull(this.xcontext.getWikiId());
        assertNull(this.xcontext.getUserReference());

        this.webSocketContext.initialize(this.endPointConfig, this.request, this.response);
        assertEquals(13, this.webSocketContext.call(this.session, () -> {
            assertEquals("test", this.xcontext.getWikiId());
            assertEquals(this.currentUserReference, this.xcontext.getUserReference());
            assertEquals("ws://www.xwiki.org/xwiki/one/echo/two", this.xcontext.getRequest().getRequestURI());
            return 13;
        }));
    }
}
