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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.services.ChromeDevToolsService;

/**
 * Utility class to wait for a web page to be ready before printing it to PDF.
 * 
 * @version $Id$
 * @since 16.10.8
 * @since 17.4.0RC1
 */
public class PageReadyPromise
{
    /**
     * The JavaScript code used to wait for the page to be fully ready before printing it to PDF.
     */
    static final String PAGE_READY_PROMISE;

    static {
        // Read the JavaScript code for page ready promise once and cache it.
        String filePath = "/pageReadyPromise.js";
        try {
            PAGE_READY_PROMISE =
                IOUtils.toString(ChromeTab.class.getResourceAsStream(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read the [%s] file.", filePath), e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PageReadyPromise.class);

    /**
     * The Chrome DevTools service used to interact with the browser tab.
     */
    private final ChromeDevToolsService tabService;

    /**
     * The number of seconds to wait when communicating with the Chrome DevTools service before timing out.
     */
    private final long connectionTimeout;

    /**
     * Create a new instance using the given Chrome DevTools service and connection timeout.
     * 
     * @param tabService the Chrome DevTools service used to interact with the browser tab
     * @param connectionTimeout the number of seconds to wait when communicating with the Chrome DevTools service before
     *            timing out
     */
    public PageReadyPromise(ChromeDevToolsService tabService, long connectionTimeout)
    {
        this.tabService = tabService;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Wait for a web page to be ready.
     * 
     * @param timeout the number of seconds to wait for the web page to be ready before timing out
     * @param isCanceled a supplier that indicates whether the process should be canceled
     * @throws IOException if waiting for the page to be ready fails
     */
    public void wait(int timeout, BooleanSupplier isCanceled) throws IOException
    {
        LOGGER.debug("Waiting for page to be ready.");

        Runtime runtime = this.tabService.getRuntime();
        runtime.enable();

        int timeLeft = timeout;
        while (timeLeft > 0 && !isCanceled.getAsBoolean()) {
            long retryTimeout = Math.min(timeLeft, this.connectionTimeout);
            timeLeft -= this.connectionTimeout;

            try {
                LOGGER.debug("Waiting [{}] seconds for page to be ready.", retryTimeout);
                Evaluate evaluate = awaitPageReadyPromise(runtime, retryTimeout * 1000);
                checkEvaluation(evaluate, "Page ready.", "Page ready promise resolved with unexpected value: %s",
                    "Failed to wait for page to be ready.");
                return;
            } catch (Exception e) {
                if (timeLeft <= 0) {
                    throw new IOException("Timeout waiting for page to be ready.", e);
                }
            }
        }
    }

    private Evaluate awaitPageReadyPromise(Runtime runtime, long timeout)
    {
        String pageReadyPromise =
                PAGE_READY_PROMISE.replace("__pageReadyTimeout__", String.valueOf(timeout));
        return runtime.evaluate(
            /* expression */ pageReadyPromise,
            /* objectGroup */ null,
            /* includeCommandLineAPI */ false,
            /* silent */ false,
            /* contextId */ null,
            /* returnByValue */ true,
            /* generatePreview */ false,
            /* userGesture */ false,
            /* awaitPromise */ true,
            /* throwOnSideEffect */ false,
            /* timeout */ (double) timeout,
            /* disableBreaks */ true,
            /* replMode */ false,
            /* allowUnsafeEvalBlockedByCSP */ false,
            /* uniqueContextId */ null,
            /* serializationOptions */ null
        );
    }

    private void checkEvaluation(Evaluate evaluate, Object expectedValue, String unexpectedValueException,
        String evaluationException) throws IOException
    {
        if (evaluate.getExceptionDetails() != null) {
            RemoteObject exception = evaluate.getExceptionDetails().getException();
            Object cause = exception.getDescription();
            if (cause == null) {
                // When the exception was thrown as a string or when a promise was rejected.
                cause = exception.getValue();
            }
            throw new IOException(Objects.toString(cause, evaluationException));
        } else {
            RemoteObject result = evaluate.getResult();
            if (!Objects.equals(expectedValue, result.getValue())) {
                throw new IOException(String.format(unexpectedValueException, result.getValue()));
            }
        }
    }
}
