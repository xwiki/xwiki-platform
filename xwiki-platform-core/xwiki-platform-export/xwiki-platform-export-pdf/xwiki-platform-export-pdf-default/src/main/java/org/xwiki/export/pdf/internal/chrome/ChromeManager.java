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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.browser.BrowserManager;
import org.xwiki.export.pdf.browser.BrowserTab;

import com.github.kklisura.cdt.protocol.commands.Target;
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
 * Help interact with the headless Chrome web browser.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Singleton
@Named("chrome")
public class ChromeManager implements BrowserManager, Initializable
{
    /**
     * The number of seconds to wait for Chrome remote debugging before giving up.
     */
    static final int REMOTE_DEBUGGING_TIMEOUT = 10;

    @Inject
    private Logger logger;

    private WebSocketServiceFactory webSocketServiceFactory;

    /**
     * The top level service used to interact with the browser.
     */
    private ChromeService chromeService;

    /**
     * The service used to access the browser "target" and to create new browser contexts in order to isolate the
     * browser tabs used by different users.
     */
    private ChromeDevToolsService browserDevToolsService;

    private InetSocketAddress chromeAddress;

    @Override
    public void initialize() throws InitializationException
    {
        this.webSocketServiceFactory = (webSocketURL -> WebSocketServiceImpl.create(URI.create(webSocketURL)));
    }

    @Override
    public void connect(String host, int remoteDebuggingPort) throws TimeoutException
    {
        this.logger.debug("Connecting to the Chrome remote debugging service on [{}:{}].", host, remoteDebuggingPort);
        InetSocketAddress newChromeAddress = InetSocketAddress.createUnresolved(host, remoteDebuggingPort);
        if (!Objects.equals(this.chromeAddress, newChromeAddress)) {
            // Connect to the new address.
            this.chromeAddress = newChromeAddress;
            this.chromeService = new ChromeServiceImpl(host, remoteDebuggingPort, this.webSocketServiceFactory);
        }

        // Close the previous WebSocket session.
        if (this.browserDevToolsService != null) {
            this.browserDevToolsService.close();
        }
        // Create a new WebSocket session.
        ChromeVersion chromeVersion = waitForChromeService(REMOTE_DEBUGGING_TIMEOUT);
        createBrowserDevToolsService(chromeVersion);
    }

    @Override
    public boolean isConnected()
    {
        try {
            // Check the HTTP connection first.
            return this.chromeService != null && this.chromeService.getVersion() != null
            // Then check the WebSocket connection.
                && this.browserDevToolsService != null && this.browserDevToolsService.getBrowser().getVersion() != null;
        } catch (Exception e) {
            this.logger.debug("The Chrome web browser is not connected. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    private ChromeVersion waitForChromeService(int timeoutSeconds) throws TimeoutException
    {
        this.logger.debug("Waiting [{}] seconds for Chrome to accept remote debugging connections.", timeoutSeconds);

        int timeoutMillis = timeoutSeconds * 1000;
        long start = System.currentTimeMillis();
        Exception exception = null;

        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                return this.chromeService.getVersion();
            } catch (Exception e) {
                exception = e;
                this.logger.debug("Chrome remote debugging not available. Root cause: [{}]. Retrying in 2s.",
                    ExceptionUtils.getRootCauseMessage(e));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    this.logger.warn("Interrupted thread [{}]. Root cause: [{}].", Thread.currentThread().getName(),
                        ExceptionUtils.getRootCauseMessage(e));
                    // Restore the interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
        }

        long waitTime = (System.currentTimeMillis() - start) / 1000;
        String message = String.format(
            "Timeout waiting for Chrome remote debugging to become available. Waited [%s] " + "seconds", waitTime);
        if (exception != null) {
            message = String.format("%s. Root cause: [%s]", message, ExceptionUtils.getRootCauseMessage(exception));
        }
        throw new TimeoutException(message);
    }

    @Override
    public BrowserTab createIncognitoTab() throws IOException
    {
        this.logger.debug("Creating incognito tab.");
        Target browserTarget = this.browserDevToolsService.getTarget();

        String browserContextId = browserTarget.createBrowserContext(true, null, null);
        this.logger.debug("Created browser context [{}].", browserContextId);

        String tabTargetId = browserTarget.createTarget("", null, null, browserContextId, false, false, false);
        this.logger.debug("Created incognito tab [{}].", tabTargetId);

        Optional<ChromeTab> tab =
            this.chromeService.getTabs().stream().filter(t -> tabTargetId.equals(t.getId())).findFirst();
        if (tab.isPresent()) {
            return new org.xwiki.export.pdf.internal.chrome.ChromeTab(
                this.chromeService.createDevToolsService(tab.get()), this.browserDevToolsService);
        } else {
            throw new IOException(String.format("The incognito tab [%s] we just created is missing.", tabTargetId));
        }
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
     * @throws ChromeServiceException if connecting to the browser WebSocket end-point fails
     */
    private void createBrowserDevToolsService(ChromeVersion chromeVersion) throws ChromeServiceException
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
            this.browserDevToolsService = ProxyUtils.createProxyFromAbstract(ChromeDevToolsServiceImpl.class,
                new Class[] {WebSocketService.class, ChromeDevToolsServiceConfiguration.class},
                new Object[] {webSocketService, new ChromeDevToolsServiceConfiguration()},
                (unused, method, args) -> commandsCache.computeIfAbsent(method, key -> {
                    Class<?> returnType = method.getReturnType();
                    return ProxyUtils.createProxy(returnType, commandInvocationHandler);
                }));

            // Register developer tools service with invocation handler.
            commandInvocationHandler.setChromeDevToolsService(this.browserDevToolsService);
        } catch (WebSocketServiceException e) {
            throw new ChromeServiceException("Failed to connect to the browser web socket.", e);
        }
    }

    @Override
    public void close()
    {
        this.chromeAddress = null;
        this.chromeService = null;
        this.browserDevToolsService.close();
        this.browserDevToolsService = null;
    }
}
