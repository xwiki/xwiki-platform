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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.github.kklisura.cdt.protocol.commands.DOM;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.commands.Target;
import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.protocol.types.target.TargetInfo;
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
import com.github.kklisura.cdt.services.utils.ProxyUtils;

/**
 * Help interact with the headless Chrome web browser.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component(roles = ChromeManager.class)
@Singleton
public class ChromeManager implements Initializable
{
    /**
     * The number of seconds to wait for Chrome remote debugging before giving up.
     */
    private static final int REMOTE_DEBUGGING_TIMEOUT = 10;

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

    /**
     * The JavaScript code used to wait for the page to be fully ready before printing it to PDF.
     */
    private String pageReadyPromise;

    @Override
    public void initialize() throws InitializationException
    {
        // Read the JavaScript code for page ready promise once and cache it.
        String filePath = "/pageReadyPromise.js";
        try {
            this.pageReadyPromise = IOUtils.toString(getClass().getResourceAsStream(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InitializationException(String.format("Failed to read the [%s] file.", filePath), e);
        }
    }

    /**
     * Connect to the headless Chrome web browser that runs on the specified host, behind the specified port.
     * 
     * @param host the host running the headless Chrome web browser, specified either as an IP address or a host name
     * @param remoteDebuggingPort the port number to connect to
     * @throws TimeoutException if the connection timeouts
     */
    void connect(String host, int remoteDebuggingPort) throws TimeoutException
    {
        this.logger.debug("Connecting to the Chrome remote debugging service on [{}:{}].", host, remoteDebuggingPort);
        this.webSocketServiceFactory = (webSocketURL -> WebSocketServiceImpl.create(URI.create(webSocketURL)));
        this.chromeService = new ChromeServiceImpl(host, remoteDebuggingPort, this.webSocketServiceFactory);
        waitForChromeService(REMOTE_DEBUGGING_TIMEOUT);
        createBrowserDevToolsService();
    }

    private void waitForChromeService(int timeoutSeconds) throws TimeoutException
    {
        this.logger.debug("Waiting [{}] seconds for Chrome to accept remote debugging connections.", timeoutSeconds);

        int timeoutMillis = timeoutSeconds * 1000;
        long start = System.currentTimeMillis();
        Exception exception = null;

        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                this.chromeService.getVersion();
                return;
            } catch (Exception e) {
                exception = e;
                this.logger.debug("Chrome remote debugging not available. Retrying in 2s.", e);
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
        String message = String.format("Timeout waiting for Chrome remote debugging to become available. Waited [%s] "
            + "seconds", waitTime);
        if (exception != null) {
            message = String.format("%s. Root cause: [%s]", message, ExceptionUtils.getRootCauseMessage(exception));
        }
        throw new TimeoutException(message);
    }

    /**
     * Navigates to the specified web page and waits for it to be ready (fully loaded).
     * 
     * @param tabDevToolsService the developer tools service corresponding to the browser tab used to navigate to the
     *            specified web page
     * @param printPreviewURL the URL of the web page we are going to navigate to
     * @param wait {@code true} to wait for the page to be ready, {@code false} otherwise
     * @return {@code true} if the navigation was successful, {@code false} otherwise
     * @throws IOException if navigating to the specified web page fails
     */
    public boolean navigate(ChromeDevToolsService tabDevToolsService, URL printPreviewURL, boolean wait)
        throws IOException
    {
        this.logger.debug("Navigating to [{}].", printPreviewURL);
        Page page = tabDevToolsService.getPage();
        page.enable();
        Navigate navigate = page.navigate(printPreviewURL.toString());
        boolean success = navigate.getErrorText() == null;

        if (success && wait) {
            Runtime runtime = tabDevToolsService.getRuntime();
            runtime.enable();
            waitForPageReady(runtime);
        }

        return success;
    }

    /**
     * Wait for a page to be ready.
     * 
     * @param runtime the page runtime
     */
    private void waitForPageReady(Runtime runtime) throws IOException
    {
        this.logger.debug("Waiting for page to be ready.");
        Evaluate evaluate = runtime.evaluate(/* expression */ this.pageReadyPromise, /* objectGroup */ null,
            /* includeCommandLineAPI */ false, /* silent */ false, /* contextId */ null, /* returnByValue */ true,
            /* generatePreview */ false, /* userGesture */ false, /* awaitPromise */ true,
            /* throwOnSideEffect */ false, /* timeout */ REMOTE_DEBUGGING_TIMEOUT * 1000.0, /* disableBreaks */ true,
            /* replMode */ false, /* allowUnsafeEvalBlockedByCSP */ false, /* uniqueContextId */ null);
        checkEvaluation(evaluate, "Page ready.", "Failed to wait for page to be ready.",
            "Timeout waiting for page to be ready.");
    }

    private void checkEvaluation(Evaluate evaluate, Object expectedValue, String evaluationException,
        String unexpectedValueException) throws IOException
    {
        String messageTemplae = "%s Root cause: %s";
        if (evaluate.getExceptionDetails() != null) {
            RemoteObject exception = evaluate.getExceptionDetails().getException();
            Object cause = exception.getDescription();
            if (cause == null) {
                // When the exception was thrown as a string or when a promise was rejected.
                cause = exception.getValue();
            }
            throw new IOException(String.format(messageTemplae, evaluationException, cause));
        } else {
            RemoteObject result = evaluate.getResult();
            if (!Objects.equals(expectedValue, result.getValue())) {
                throw new IOException(String.format(messageTemplae, unexpectedValueException, result.getValue()));
            }
        }
    }

    /**
     * Print the current page to PDF.
     * 
     * @param tabDevToolsService the developer tools service corresponding to the page to print
     * @param cleanup the code to execute after the PDF was generated, useful for performing cleanup
     * @return the PDF input stream
     */
    public InputStream printToPDF(ChromeDevToolsService tabDevToolsService, Runnable cleanup)
    {
        this.logger.debug("Printing web page to PDF.");
        Boolean landscape = false;
        Boolean displayHeaderFooter = false;
        Boolean printBackground = false;
        Double scale = 1d;
        // A4 paper format
        Double paperWidth = 8.27d;
        Double paperHeight = 11.7d;
        Double marginTop = 0d;
        Double marginBottom = 0d;
        Double marginLeft = 0d;
        Double marginRight = 0d;
        String pageRanges = "";
        Boolean ignoreInvalidPageRanges = false;
        String headerTemplate = "";
        String footerTemplate = "";
        Boolean preferCSSPageSize = false;
        PrintToPDFTransferMode mode = PrintToPDFTransferMode.RETURN_AS_STREAM;

        // See https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-printToPDF
        PrintToPDF printToPDF = tabDevToolsService.getPage().printToPDF(landscape, displayHeaderFooter, printBackground,
            scale, paperWidth, paperHeight, marginTop, marginBottom, marginLeft, marginRight, pageRanges,
            ignoreInvalidPageRanges, headerTemplate, footerTemplate, preferCSSPageSize, mode);
        return new PrintToPDFInputStream(tabDevToolsService.getIO(), printToPDF.getStream(), cleanup);
    }

    /**
     * Converts servlet cookies to browser cookies.
     *
     * @param cookies the servlet cookies to convert to browser cookies
     * @return the browser cookies
     */
    public List<CookieParam> toCookieParams(Cookie[] cookies)
    {
        if (cookies == null) {
            return Collections.emptyList();
        } else {
            return Stream.of(cookies).filter(Objects::nonNull).map(this::toCookieParam).collect(Collectors.toList());
        }
    }

    /**
     * Converts a servlet cookie to a browser cookie.
     *
     * @param servletCookie the servlet cookie to convert to a browser cookie
     * @return the browser cookie
     */
    public CookieParam toCookieParam(Cookie servletCookie)
    {
        CookieParam browserCookie = new CookieParam();
        browserCookie.setName(servletCookie.getName());
        browserCookie.setValue(servletCookie.getValue());
        browserCookie.setDomain(servletCookie.getDomain());
        browserCookie.setPath(servletCookie.getPath());
        browserCookie.setSecure(servletCookie.getSecure());
        browserCookie.setHttpOnly(servletCookie.isHttpOnly());
        return browserCookie;
    }

    /**
     * Sets the specified cookies.
     * 
     * @param tabDevToolsService the developer tools service for which to set the cookies
     * @param cookies the cookies to set
     */
    public void setCookies(ChromeDevToolsService tabDevToolsService, List<CookieParam> cookies)
    {
        this.logger.debug("Setting cookies [{}].", cookies.stream()
            .map(cookie -> String.format("%s: %s", cookie.getName(), cookie.getValue())).collect(Collectors.toList()));
        Network network = tabDevToolsService.getNetwork();
        network.enable();
        network.clearBrowserCookies();
        network.setCookies(cookies);
    }

    /**
     * @return a new incognito tab, using a separate browser context (profile)
     */
    public ChromeDevToolsService createIncognitoTab() throws IOException
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
            return this.chromeService.createDevToolsService(tab.get());
        } else {
            throw new IOException(String.format("The incognito tab [%s] we just created is missing.", tabTargetId));
        }
    }

    /**
     * Closes the browser tab associated with the given developer tools service.
     * 
     * @param tabDevToolsService the developer tools service used to close the browser tab
     */
    public void closeIncognitoTab(ChromeDevToolsService tabDevToolsService)
    {
        TargetInfo tabInfo = tabDevToolsService.getTarget().getTargetInfo();
        this.logger.debug("Closing incognito tab [{}].", tabInfo.getTargetId());
        String browserContextId = tabInfo.getBrowserContextId();
        tabDevToolsService.close();
        this.logger.debug("Disposing browser context [{}].", browserContextId);
        this.browserDevToolsService.getTarget().disposeBrowserContext(browserContextId);
    }

    /**
     * Code adapted from {@link ChromeServiceImpl#createDevToolsService(ChromeTab)}. The main difference is that we're
     * connecting to the browser WebSocket end-point rather than to a tab end-point (otherwise we wouldn't be able /
     * allowed to create a new browser context).
     * 
     * @throws ChromeServiceException if connecting to the browser WebSocket end-point fails
     */
    private void createBrowserDevToolsService() throws ChromeServiceException
    {
        try {
            // Connect to the browser via WebSocket.
            String webSocketDebuggerUrl = this.chromeService.getVersion().getWebSocketDebuggerUrl();
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

    /**
     * Sets the base URL for the page loaded on the specified browser tab.
     * 
     * @param tabDevToolsService the developer tools service associated with the target page
     * @param baseURL the base URL to set
     */
    public void setBaseURL(ChromeDevToolsService tabDevToolsService, URL baseURL) throws IOException
    {
        this.logger.debug("Setting base URL [{}].", baseURL);
        Runtime runtime = tabDevToolsService.getRuntime();
        runtime.enable();

        // Add the BASE tag to the page head. I couldn't find a way to create this node using the DOM domain, so I'm
        // using JavaScript instead (i.e. the runtime domain).
        Evaluate evaluate = runtime.evaluate("jQuery('<base/>').prependTo('head').length");
        checkEvaluation(evaluate, 1, "Failed to insert the BASE tag.", "Unexpected page HTML.");

        DOM dom = tabDevToolsService.getDOM();
        dom.enable();

        // Look for the BASE tag we just added and set its href attribute in order to change the page base URL.
        Integer baseNodeId = dom.querySelector(dom.getDocument().getNodeId(), "base");
        dom.setAttributeValue(baseNodeId, "href", baseURL.toString());
    }
}
