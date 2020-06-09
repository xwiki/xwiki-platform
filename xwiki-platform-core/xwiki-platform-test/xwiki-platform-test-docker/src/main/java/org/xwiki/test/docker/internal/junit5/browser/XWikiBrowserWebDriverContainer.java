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

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.BrowserWebDriverContainer;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Extending TestContainer's {@code BrowserWebDriverContainer} to increase the default timeout, in order to try to
 * debug a container starting problem.
 *
 * @param <T> the type of the current container
 * @version $Id$
 */
public class XWikiBrowserWebDriverContainer<T extends BrowserWebDriverContainer<T>> extends BrowserWebDriverContainer<T>
{
    private Capabilities capabilities;

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo)
    {
        Unreliables.retryUntilSuccess(120, TimeUnit.SECONDS,
            Timeouts.getWithTimeout(80, TimeUnit.SECONDS,
                () -> () -> new RemoteWebDriver(this.getSeleniumAddress(), this.capabilities)));
        super.containerIsStarted(containerInfo);
    }

    @Override
    public T withCapabilities(Capabilities capabilities)
    {
        this.capabilities = capabilities;
        return super.withCapabilities(capabilities);
    }
}
