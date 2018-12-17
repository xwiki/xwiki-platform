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
package org.xwiki.test.docker.junit5.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.xwiki.test.docker.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Create and execute the browser docker container for driving the tests.
 *
 * @version $Id$
 * @since 10.11RC1
 */
public class BrowserContainerExecutor extends AbstractContainerExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserContainerExecutor.class);

    /**
     * Create and start the {@link BrowserWebDriverContainer} based on the test given test configuration.
     *
     * @param testConfiguration the configuration used to parameterize this container.
     * @return the started browser driver container.
     */
    public BrowserWebDriverContainer start(TestConfiguration testConfiguration)
    {
        LOGGER.info("(*) Starting browser [{}]...", testConfiguration.getBrowser());
        Browser browser = testConfiguration.getBrowser();

        // Create a single BrowserWebDriverContainer instance and reuse it for all the tests in the test class.
        BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer<>()
            .withDesiredCapabilities(browser.getCapabilities())
            .withNetwork(Network.SHARED)
            .withNetworkAliases("vnchost")
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)

            // In case some test-resources are provided, they need to be available from the browser
            // for example in order to upload some files on the wiki.
            .withFileSystemBind(getTestResourcePathOnHost(testConfiguration), browser.getTestResourcesPath());

        if (testConfiguration.isVerbose()) {
            LOGGER.info(String.format("Test resource path mapped: On Host [%s], in Docker: [%s]",
                getTestResourcePathOnHost(testConfiguration), browser.getTestResourcesPath()));
            LOGGER.info(String.format("Docker image used: [%s]",
                BrowserWebDriverContainer.getImageForCapabilities(testConfiguration.getBrowser().getCapabilities())));
            webDriverContainer.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        webDriverContainer.start();

        if (testConfiguration.vnc()) {
            LOGGER.info("VNC server address = " + webDriverContainer.getVncAddress());
        }

        return webDriverContainer;
    }

    /**
     * @return the path where the test resources are stored by Maven after test compilation, on the host.
     */
    private String getTestResourcePathOnHost(TestConfiguration testConfiguration)
    {
        String testClassesDirectory;
        String mavenBuildDir = System.getProperty("maven.build.dir");
        if (mavenBuildDir == null) {
            testClassesDirectory = "target/test-classes";
        } else {
            testClassesDirectory = String.format("%s/test-classes", testConfiguration.getOutputDirectory());
        }
        return testClassesDirectory;
    }
}
