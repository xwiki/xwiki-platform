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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.websocket.WebSocketContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Default {@link WebSocketContext} implementation. Initializes the XWiki execution context and binds it to the
 * WebSocket session in order to be able to execute code within the right XWiki context when the session is opened and
 * when receiving messages.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
@Component
@Singleton
public class DefaultWebSocketContext implements WebSocketContext
{
    private static final String WIKI = "wiki";

    private static final String PATH_SEPARATOR = "/";

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private Container container;

    @Inject
    private XWikiStubContextProvider contextProvider;

    @Override
    public void initialize(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response)
    {
        ExecutionContext context = createExecutionContext(config, request, response);
        config.getUserProperties().put(ExecutionContext.class.getName(), context);
        initialize(context);

        // We have to authenticate the user after initializing the execution context because AuthServiceConfiguration
        // component uses the XWiki context through its provider which takes it from the execution context.
        authenticateUser(request);
    }

    @Override
    public void run(Session session, Runnable runnable)
    {
        try {
            initialize(getExecutionContext(session));
            runnable.run();
        } finally {
            cleanup(session);
        }
    }

    @Override
    public <T> T call(Session session, Callable<T> callable) throws Exception
    {
        try {
            initialize(getExecutionContext(session));
            return callable.call();
        } finally {
            cleanup(session);
        }
    }

    private void initialize(ExecutionContext context)
    {
        if (context == null) {
            throw new RuntimeException("The execution context has not been initialized during the handshake request.");
        }

        this.execution.pushContext(context, false);

        XWikiContext xcontext = getXWikiContext();
        if (xcontext != null) {
            this.container.setRequest(new ServletRequest(xcontext.getRequest()));
            this.container.setResponse(new ServletResponse(xcontext.getResponse()));
            this.container.setSession(new ServletSession(xcontext.getRequest()));
        }
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    private void cleanup(Session session)
    {
        ExecutionContext context = this.execution.getContext();
        ExecutionContext storedContext = getExecutionContext(session);
        if (context != null && context == storedContext) {
            this.execution.removeContext();
        } else {
            this.logger.warn("The execution context doesn't match the stored session context.");
        }
    }

    private ExecutionContext getExecutionContext(Session session)
    {
        return (ExecutionContext) session.getUserProperties().get(ExecutionContext.class.getName());
    }

    private ExecutionContext createExecutionContext(ServerEndpointConfig config, HandshakeRequest request,
        HandshakeResponse response)
    {
        ExecutionContext context = new ExecutionContext();

        XWikiContext xcontext = this.contextProvider.createStubContext();
        // The XWiki context is null if XWiki is not ready yet. See XWikiInitializerJob which initializes the
        // XWikiStubContextProvider after XWiki is ready to service requests. XWiki WebSocket end-points that rely on
        // the XWiki context should refuse the connection as long as the XWiki context is not set on the execution
        // context (i.e. as long as XWiki is not ready).
        if (xcontext != null) {
            String wiki = getWiki(config, request);
            if (wiki != null) {
                xcontext.setWikiId(wiki);
            }

            xcontext.setRequest(new XWikiWebSocketRequestStub(request));
            xcontext.setResponse(new XWikiWebSocketResponseStub(response));

            xcontext.declareInExecutionContext(context);
        }

        return context;
    }

    private String getWiki(ServerEndpointConfig config, HandshakeRequest request)
    {
        String wiki = getPathParameter(WIKI, request.getRequestURI().getPath(), config.getPath());
        if (wiki == null && request.getParameterMap().containsKey(WIKI)) {
            wiki = request.getParameterMap().get(WIKI).get(0);
        }
        return wiki;
    }

    private String getPathParameter(String name, String path, String template)
    {
        List<String> templateElements = Arrays.asList(template.split(PATH_SEPARATOR));
        int pathParamPos = templateElements.indexOf(String.format("{%s}", name));
        if (pathParamPos < 0) {
            return null;
        }

        List<String> pathElements = Arrays.asList(path.split(PATH_SEPARATOR));
        List<String> matchedElements =
            pathElements.subList(pathElements.size() - templateElements.size(), pathElements.size());
        return matchedElements.get(pathParamPos);
    }

    private void authenticateUser(HandshakeRequest request)
    {
        XWikiContext xcontext = getXWikiContext();
        // We can authenticate the user only after XWiki is ready (there's an XWiki context available) and if the
        // WebSocket handshake request (HTTP upgrade) has an associated session (this prevents null pointer exceptions
        // when authenticating the user because a session is expected in some cases, such as
        // SecurityRequestWrapper#getUserPrincipal).
        if (xcontext != null && request.getHttpSession() != null) {
            try {
                XWikiUser xwikiUser = xcontext.getWiki().checkAuth(xcontext);
                if (xwikiUser != null) {
                    xcontext.setUserReference(xwikiUser.getUserReference());
                }
            } catch (Exception e) {
                this.logger.warn("Failed to authenticate the user for [{}]. Root cause is:", request.getRequestURI(),
                    e);
            }
        }
    }
}
