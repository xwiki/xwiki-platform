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
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.export.pdf.browser.BrowserManager;
import org.xwiki.export.pdf.browser.BrowserTab;

import com.github.kklisura.cdt.protocol.commands.Target;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;
import com.github.kklisura.cdt.services.types.ChromeVersion;

/**
 * Help interact with the headless Chrome web browser.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Named("chrome")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ChromeManager implements BrowserManager
{
    /**
     * The number of seconds to wait for Chrome remote debugging before giving up.
     */
    static final int REMOTE_DEBUGGING_TIMEOUT = 10;

    @Inject
    private Logger logger;

    @Inject
    private ChromeServiceFactory chromeServiceFactory;

    /**
     * The top level service used to interact with the browser.
     */
    private ChromeService chromeService;

    /**
     * The service used to access the browser "target" and to create new browser contexts in order to isolate the
     * browser tabs used by different users.
     */
    private ChromeDevToolsService browserDevToolsService;

    @Override
    public void connect(String host, int remoteDebuggingPort) throws TimeoutException
    {
        if (this.browserDevToolsService != null) {
            throw new IllegalStateException(
                "Chrome is already connected. Please close the current connection before establishing a new one.");
        }

        this.logger.debug("Connecting to the Chrome remote debugging service on [{}:{}].", host, remoteDebuggingPort);
        this.chromeService = this.chromeServiceFactory.createChromeService(host, remoteDebuggingPort);

        // Create a new WebSocket session.
        ChromeVersion chromeVersion = waitForChromeService(REMOTE_DEBUGGING_TIMEOUT);
        this.browserDevToolsService = this.chromeServiceFactory.createBrowserDevToolsService(chromeVersion);
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
        if (this.browserDevToolsService == null) {
            throw new IllegalStateException("The Chrome web browser is not connected.");
        }

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

    @Override
    public void close()
    {
        this.chromeService = null;
        if (this.browserDevToolsService != null) {
            this.browserDevToolsService.close();
            this.browserDevToolsService = null;
        }
    }
}
