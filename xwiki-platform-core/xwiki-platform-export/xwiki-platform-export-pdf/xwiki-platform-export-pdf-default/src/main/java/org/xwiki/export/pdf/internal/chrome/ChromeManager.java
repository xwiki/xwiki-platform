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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.PDFExportConfiguration;
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
public class ChromeManager implements BrowserManager, Initializable, Disposable
{
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

    /**
     * The executor service used to make requests to the Chrome remote debugging service. We need this because the
     * {@link ChromeService} implementation we rely on makes the HTTP requests using {@link java.net.HttpURLConnection}
     * without setting any timeout which in some cases can lead to the thread being blocked indefinitely. The executor
     * service allows us to overcome this by setting a timeout when waiting for {@link Future} results.
     */
    private ExecutorService executorService;

    @Inject
    private PDFExportConfiguration configuration;

    @Override
    public void initialize() throws InitializationException
    {
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void connect(String host, int remoteDebuggingPort) throws TimeoutException
    {
        if (this.executorService == null) {
            throw new IllegalStateException("The Chrome Manager must be initialized before making a connection.");
        }

        if (this.browserDevToolsService != null) {
            throw new IllegalStateException(
                "Chrome is already connected. Please close the current connection before establishing a new one.");
        }

        this.logger.debug("Connecting to the Chrome remote debugging service on [{}:{}].", host, remoteDebuggingPort);
        this.chromeService = this.chromeServiceFactory.createChromeService(host, remoteDebuggingPort);

        // Create a new WebSocket session.
        ChromeVersion chromeVersion = waitForChromeService(this.configuration.getChromeRemoteDebuggingTimeout());
        this.browserDevToolsService = this.chromeServiceFactory.createBrowserDevToolsService(chromeVersion);
    }

    @Override
    public boolean isConnected()
    {
        try {
            return getWithTimeout(() ->
            // Check the HTTP connection first.
            this.chromeService != null && this.chromeService.getVersion() != null
            // Then check the WebSocket connection.
                && this.browserDevToolsService != null
                && this.browserDevToolsService.getBrowser().getVersion() != null);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (Exception e) {
            this.logger.debug("The Chrome web browser is not connected. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }

        return false;
    }

    private ChromeVersion waitForChromeService(int timeoutSeconds) throws TimeoutException
    {
        this.logger.debug("Waiting [{}] seconds for Chrome to accept remote debugging connections.", timeoutSeconds);

        int timeoutMillis = timeoutSeconds * 1000;
        int retryIntervalSeconds = 2;
        Exception exception = null;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                return getWithTimeout(() -> this.chromeService.getVersion());
            } catch (Exception e) {
                exception = e;
                this.logger.debug("Chrome remote debugging not available. Root cause: [{}]. Retrying in {}s.",
                    ExceptionUtils.getRootCauseMessage(e), retryIntervalSeconds);
                try {
                    Thread.sleep(retryIntervalSeconds * 1000L);
                } catch (InterruptedException ie) {
                    handleInterruptedException(ie);
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

        Exception exception = null;
        try {
            return getWithTimeout(() -> {
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
                        this.chromeService.createDevToolsService(tab.get()), this.browserDevToolsService,
                        this.configuration);
                } else {
                    throw new IOException(
                        String.format("The incognito tab [%s] we just created is missing.", tabTargetId));
                }
            });
        } catch (InterruptedException e) {
            exception = e;
            // Restore the interrupted state.
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            exception = e;
        }

        throw new IOException("Failed to create incognito tab.", exception);
    }

    @Override
    public void close()
    {
        // The Chrome Manager instance can be reused after being closed, by reconnecting to the Chrome remote debugging
        // service.
        this.chromeService = null;
        if (this.browserDevToolsService != null) {
            this.browserDevToolsService.close();
            this.browserDevToolsService = null;
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // The Chrome Manager instance is unusable after being disposed.
        close();
        if (this.executorService != null) {
            shutdownExecutorServiceAndAwaitTermination();
            this.executorService = null;
        }
    }

    /**
     * Shutdown the Executor Service used to make requests to the Chrome remote debugging service and await for the
     * current tasks to terminate. Code adapted from the {@link ExecutorService}'s usage examples.
     */
    private void shutdownExecutorServiceAndAwaitTermination()
    {
        // Disable new tasks from being submitted.
        this.executorService.shutdown();
        try {
            int timeout = this.configuration.getChromeRemoteDebuggingTimeout();
            // Wait a while for existing tasks to terminate.
            if (!this.executorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks.
                this.executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled.
                if (!this.executorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
                    this.logger.error("Chrome Manager's Executor Service did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted.
            this.executorService.shutdownNow();
            // Preserve interrupt status.
            Thread.currentThread().interrupt();
        }
    }

    private <T> T getWithTimeout(Callable<T> request) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (this.executorService != null) {
            Future<T> future = this.executorService.submit(request);
            return future.get(this.configuration.getChromeRemoteDebuggingTimeout(), TimeUnit.SECONDS);
        } else {
            throw new IllegalStateException("The Chrome Manager must be initialized before making any requests.");
        }
    }

    private void handleInterruptedException(InterruptedException e)
    {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn("Interrupted thread [{}]. Root cause: [{}].", Thread.currentThread().getName(),
                ExceptionUtils.getRootCauseMessage(e));
        }
        // Restore the interrupted state.
        Thread.currentThread().interrupt();
    }
}
