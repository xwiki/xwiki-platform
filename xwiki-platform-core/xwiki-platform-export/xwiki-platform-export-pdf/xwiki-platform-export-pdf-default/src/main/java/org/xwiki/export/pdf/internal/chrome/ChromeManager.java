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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;

/**
 * Help interact with the headless Chrome web browser.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
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

    private ChromeService chromeService;

    private String pageReadyPromise;

    @Override
    public void initialize() throws InitializationException
    {
        String filePath = "/pageReadyPromise.js";
        try {
            this.pageReadyPromise = IOUtils.toString(getClass().getResourceAsStream(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InitializationException(String.format("Failed to read the [%s] file.", filePath), e);
        }
    }

    /**
     * Connect to the headless Chrome web browser that runs on local host, behind the specified port.
     * 
     * @param remoteDebuggingPort the port number to connect to
     * @throws TimeoutException if the connection timeouts
     */
    public void connect(int remoteDebuggingPort) throws TimeoutException
    {
        this.logger.debug("Connecting to the Chrome remote debugging service.");
        this.chromeService = new ChromeServiceImpl(remoteDebuggingPort);
        waitForChromeService(REMOTE_DEBUGGING_TIMEOUT);
    }

    /**
     * @return the Chrome remote debugging service
     */
    public ChromeService getChromeService()
    {
        return this.chromeService;
    }

    private void waitForChromeService(int timeoutSeconds) throws TimeoutException
    {
        this.logger.debug("Wait for Chrome to accept remote debugging connections.");

        int timeoutMillis = timeoutSeconds * 1000;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                this.chromeService.getVersion();
                return;
            } catch (Exception e) {
                this.logger.debug("Chrome remote debugging not available. Retrying in 2s.");
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
        throw new TimeoutException(String
            .format("Timeout waiting for Chrome remote debugging to become available. Waited [%s] seconds.", waitTime));
    }

    /**
     * Wait for a page to be ready.
     * 
     * @param runtime the page runtime
     */
    public void waitForPageReady(Runtime runtime) throws IOException
    {
        Evaluate evaluate = runtime.evaluate(/* expression */ this.pageReadyPromise, /* objectGroup */ null,
            /* includeCommandLineAPI */ false, /* silent */ false, /* contextId */ null, /* returnByValue */ true,
            /* generatePreview */ false, /* userGesture */ false, /* awaitPromise */ true,
            /* throwOnSideEffect */ false, /* timeout */ REMOTE_DEBUGGING_TIMEOUT * 1000.0, /* disableBreaks */ true,
            /* replMode */ false, /* allowUnsafeEvalBlockedByCSP */ false, /* uniqueContextId */ null);
        if (evaluate.getExceptionDetails() != null) {
            RemoteObject exception = evaluate.getExceptionDetails().getException();
            Object cause = exception.getDescription();
            if (cause == null) {
                cause = exception.getValue();
            }
            throw new IOException("Failed to wait for page to be ready. Root cause: " + cause);
        } else {
            RemoteObject result = evaluate.getResult();
            if (!"Page ready.".equals(result.getValue())) {
                throw new IOException("Timeout waiting for page to be ready. Root cause: " + result.getValue());
            }
        }
    }

    /**
     * Print the current page to PDF.
     * 
     * @param devToolsService the developer tools service corresponding to the page to print
     * @param cleanup the code to execute after the PDF was generated, useful for performing cleanup
     * @return the PDF input stream
     */
    public InputStream printToPDF(ChromeDevToolsService devToolsService, Runnable cleanup)
    {
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

        PrintToPDF printToPDF = devToolsService.getPage().printToPDF(landscape, displayHeaderFooter, printBackground,
            scale, paperWidth, paperHeight, marginTop, marginBottom, marginLeft, marginRight, pageRanges,
            ignoreInvalidPageRanges, headerTemplate, footerTemplate, preferCSSPageSize, mode);
        return new PrintToPDFInputStream(devToolsService.getIO(), printToPDF.getStream(), cleanup);
    }
}
