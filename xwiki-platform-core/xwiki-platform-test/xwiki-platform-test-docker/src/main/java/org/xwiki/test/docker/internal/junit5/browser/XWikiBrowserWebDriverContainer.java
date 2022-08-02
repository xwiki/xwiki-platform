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

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Extending TestContainer's {@code BrowserWebDriverContainer} to increase the default timeout of 30s, since it seems
 * that on some slow agents it might not be enough.
 * See https://github.com/testcontainers/testcontainers-java/issues/3161
 *
 * @param <T> the type of the current container
 * @version $Id$
 */
public class XWikiBrowserWebDriverContainer<T extends BrowserWebDriverContainer<T>> extends BrowserWebDriverContainer<T>
{
    private Capabilities capabilities;

    private RemoteWebDriver driver;

    /**
     * Use the default docker image name for selenium.
     */
    public XWikiBrowserWebDriverContainer()
    {
        super();
    }

    /**
     * @param dockerImageName the selenium image name to use
     */
    public XWikiBrowserWebDriverContainer(String dockerImageName)
    {
        super(dockerImageName);
    }

    /**
     * @param dockerImageName the selenium image name to use
     */
    public XWikiBrowserWebDriverContainer(DockerImageName dockerImageName)
    {
        super(dockerImageName);
    }

    @Override
    public synchronized RemoteWebDriver getWebDriver()
    {
        if (this.driver == null) {
            if (this.capabilities == null) {
                this.logger().warn("No capabilities provided - this will cause an exception in future versions. "
                    + "Falling back to ChromeOptions");
                this.capabilities = new ChromeOptions();
            }
            this.driver = Unreliables.retryUntilSuccess(240, TimeUnit.SECONDS,
                () -> Timeouts.getWithTimeout(60, TimeUnit.SECONDS,
                    () -> new RemoteWebDriver(this.getSeleniumAddress(), this.capabilities)));
            setDriverField();
        }
        return super.getWebDriver();
    }

    @Override
    public T withCapabilities(Capabilities capabilities)
    {
        this.capabilities = capabilities;
        return super.withCapabilities(capabilities);
    }

    private void setDriverField()
    {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("driver");
            field.setAccessible(true);
            try {
                field.set(this, this.driver);
            } finally {
                field.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set selenium webdriver field", e);
        }
    }
}
