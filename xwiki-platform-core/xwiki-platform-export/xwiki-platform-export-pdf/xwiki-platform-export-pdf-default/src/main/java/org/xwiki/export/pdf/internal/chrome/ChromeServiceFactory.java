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
package org.xwiki.export.pdf.internal.chrome;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.WebSocketService;
import com.github.kklisura.cdt.services.config.ChromeDevToolsServiceConfiguration;
import com.github.kklisura.cdt.services.exceptions.ChromeServiceException;
import com.github.kklisura.cdt.services.exceptions.WebSocketServiceException;
import com.github.kklisura.cdt.services.factory.WebSocketServiceFactory;
import com.github.kklisura.cdt.services.impl.ChromeDevToolsServiceImpl;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import com.github.kklisura.cdt.services.impl.WebSocketServiceImpl;
import com.github.kklisura.cdt.services.invocation.CommandInvocationHandler;
import com.github.kklisura.cdt.services.types.ChromeTab;
import com.github.kklisura.cdt.services.types.ChromeVersion;
import com.github.kklisura.cdt.services.utils.ProxyUtils;

/**
 * Factory for {@link ChromeService}.
 * 
 * @version $Id$
 * @since 14.9RC1
 */
@Component(roles = ChromeServiceFactory.class)
@Singleton
public class ChromeServiceFactory implements Initializable
{
    private WebSocketServiceFactory webSocketServiceFactory;

    @Override
    public void initialize() throws InitializationException
    {
        this.webSocketServiceFactory = (webSocketURL -> WebSocketServiceImpl.create(URI.create(webSocketURL)));
    }

    /**
     * Creates a new {@link ChromeService} instance to communicate with the Chrome web browser running on the specified
     * host behind the specified port.
     * 
     * @param host the host running the Chrome web browser
     * @param remoteDebuggingPort the debugging port exposed by the Chrome web browser
     * @return a new Chrome service instance that forwards the calls to the specified host and port
     */
    public ChromeService createChromeService(String host, int remoteDebuggingPort)
    {
        return new ChromeServiceImpl(host, remoteDebuggingPort, this.webSocketServiceFactory);
    }

    /**
     * Opens a WebSocket connection / session between XWiki and Chrome's remote debugging service, that allows us to
     * control the Chrome web browser (create new tabs, load web pages, etc.).
     * <p>
     * Code adapted from {@link ChromeServiceImpl#createDevToolsService(ChromeTab)}. The main difference is that we're
     * connecting to the browser WebSocket end-point rather than to a tab end-point (otherwise we wouldn't be able /
     * allowed to create a new browser context).
     * 
     * @param chromeVersion provides information about the Chrome version, including the WebSocket debugger URL
     * @return the service used to communicate with the specified Chrome instance
     * @throws ChromeServiceException if connecting to the browser WebSocket end-point fails
     */
    public ChromeDevToolsService createBrowserDevToolsService(ChromeVersion chromeVersion) throws ChromeServiceException
    {
        try {
            // Connect to the browser via WebSocket.
            String webSocketDebuggerUrl = chromeVersion.getWebSocketDebuggerUrl();
            WebSocketService webSocketService =
                this.webSocketServiceFactory.createWebSocketService(webSocketDebuggerUrl);

            // Create invocation handler.
            CommandInvocationHandler commandInvocationHandler = new CommandInvocationHandler();

            // Setup command cache for this session.
            Map<Method, Object> commandsCache = new ConcurrentHashMap<>();

            // Create developer tools service.
            ChromeDevToolsService browserDevToolsService =
                ProxyUtils.createProxyFromAbstract(ChromeDevToolsServiceImpl.class,
                    new Class[] {WebSocketService.class, ChromeDevToolsServiceConfiguration.class},
                    new Object[] {webSocketService, new ChromeDevToolsServiceConfiguration()},
                    (unused, method, args) -> commandsCache.computeIfAbsent(method, key -> {
                        Class<?> returnType = method.getReturnType();
                        return ProxyUtils.createProxy(returnType, commandInvocationHandler);
                    }));

            // Register developer tools service with invocation handler.
            commandInvocationHandler.setChromeDevToolsService(browserDevToolsService);

            return browserDevToolsService;
        } catch (WebSocketServiceException e) {
            throw new ChromeServiceException("Failed to connect to the browser web socket.", e);
        }
    }
}
