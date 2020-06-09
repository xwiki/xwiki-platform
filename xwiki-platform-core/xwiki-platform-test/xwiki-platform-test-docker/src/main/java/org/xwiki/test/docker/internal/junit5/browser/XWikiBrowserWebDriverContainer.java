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
package org.xwiki.test.docker.internal.junit5.browser;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;

import com.github.dockerjava.api.command.InspectContainerResponse;

import static org.rnorth.ducttape.Preconditions.check;

/**
 * Extending TestContainer's {@code BrowserWebDriverContainer} to increase the default timeout, in order to try to
 * debug a container starting problem.
 *
 * @param <T> the type of the current container
 * @version $Id$
 */
public class XWikiBrowserWebDriverContainer<T extends BrowserWebDriverContainer<T>> extends BrowserWebDriverContainer<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiBrowserWebDriverContainer.class);

    private Capabilities capabilities;

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo)
    {
        retryUntilSuccess(120, TimeUnit.SECONDS,
            Timeouts.getWithTimeout(80, TimeUnit.SECONDS,
                () -> () -> {
                    LOGGER.trace("Trying to create Remote WebDriver at [{}]", getSeleniumAddress());
                    RemoteWebDriver webDriver = new RemoteWebDriver(getSeleniumAddress(), this.capabilities);
                    LOGGER.trace("Successfully created Remote WebDriver");
                    return webDriver;
                }
            ));
        super.containerIsStarted(containerInfo);
    }

    @Override
    public T withCapabilities(Capabilities capabilities)
    {
        this.capabilities = capabilities;
        return super.withCapabilities(capabilities);
    }

    /**
     * Call a supplier repeatedly until it returns a result. If an exception is thrown, the call
     * will be retried repeatedly until the timeout is hit.
     *
     * @param timeout  how long to wait
     * @param timeUnit time unit for time interval
     * @param lambda   supplier lambda expression (may throw checked exceptions)
     * @param <T>      return type of the supplier
     * @return the result of the successful lambda expression call
     */
    public static <T> T retryUntilSuccess(final int timeout, @NotNull final TimeUnit timeUnit,
        @NotNull final Callable<T> lambda)
    {
        check("timeout must be greater than zero", timeout > 0);

        final int[] attempt = { 0 };
        final Exception[] lastException = { null };

        final AtomicBoolean doContinue = new AtomicBoolean(true);
        try {
            return Timeouts.getWithTimeout(timeout, timeUnit, () -> {
                while (doContinue.get()) {
                    try {
                        return lambda.call();
                    } catch (Exception e) {
                        // Failed
                        LOGGER.trace("Retrying lambda call on attempt {}", attempt[0]++, e);
                        lastException[0] = e;
                    }
                }
                return null;
            });
        } catch (org.rnorth.ducttape.TimeoutException e) {
            if (lastException[0] != null) {
                throw new org.rnorth.ducttape.TimeoutException("Timeout waiting for result with exception",
                    lastException[0]);
            } else {
                throw new org.rnorth.ducttape.TimeoutException(e);
            }
        } finally {
            doContinue.set(false);
        }
    }
}
