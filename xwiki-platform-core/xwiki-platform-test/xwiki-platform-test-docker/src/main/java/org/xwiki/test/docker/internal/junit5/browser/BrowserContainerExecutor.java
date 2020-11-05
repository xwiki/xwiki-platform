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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SeleniumUtils;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.xwiki.test.docker.internal.junit5.AbstractContainerExecutor;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.browser.Browser;

import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.startContainer;

/**
 * Create and execute the browser docker container for driving the tests.
 *
 * @version $Id$
 * @since 10.11RC1
 */
public class BrowserContainerExecutor extends AbstractContainerExecutor
{
    /**
     * Width resolution to be used by the browser container.
     */
    private static final String DEFAULT_WIDTH_RESOLUTION = "1280";

    /**
     * Height resolution to be used by the browser container.
     */
    private static final String DEFAULT_HEIGHT_RESOLUTION = "960";

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserContainerExecutor.class);

    private TestConfiguration testConfiguration;

    /**
     * @param testConfiguration the configuration used to parameterize this container.
     */
    public BrowserContainerExecutor(TestConfiguration testConfiguration)
    {
        this.testConfiguration = testConfiguration;
    }

    /**
     * Create and start the {@link BrowserWebDriverContainer} based on the test given test configuration.
     *
     * @return the started browser driver container.
     * @throws Exception if the container fails to start
     */
    public BrowserWebDriverContainer start() throws Exception
    {
        LOGGER.info("(*) Starting browser [{}]...", this.testConfiguration.getBrowser());
        Browser browser = testConfiguration.getBrowser();

        // Create a single BrowserWebDriverContainer instance and reuse it for all the tests in the test class.
        BrowserWebDriverContainer<?> webDriverContainer = new XWikiBrowserWebDriverContainer<>()
            // We set the width and height to one of the most used resolution by users so that we can reproduce issues
            // for the larger use case and also we use a relatively large resolution to display the maximum number of
            // elements on screen and reduce the risk of false positives in tests that could be due to elements not
            // visible or missing elements (on small screens we don't display all elements).
            .withEnv("SCREEN_WIDTH", DEFAULT_WIDTH_RESOLUTION)
            .withEnv("SCREEN_HEIGHT", DEFAULT_HEIGHT_RESOLUTION)
            .withCapabilities(browser.getCapabilities())
            .withNetwork(Network.SHARED)
            .withNetworkAliases("vnchost")
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null);

        // In case some test-resources are provided, they need to be available from the browser
        // for example in order to upload some files on the wiki.
        mountFromHostToContainer(webDriverContainer, getTestResourcePathOnHost(), browser.getTestResourcesPath());

        if (this.testConfiguration.isVerbose()) {
            LOGGER.info("Test resource path mapped: On Host [{}], in Docker: [{}]",
                getTestResourcePathOnHost(), browser.getTestResourcesPath());
            LOGGER.info("Docker image used: [{}]", BrowserWebDriverContainer.getImageForCapabilities(
                this.testConfiguration.getBrowser().getCapabilities(),
                SeleniumUtils.determineClasspathSeleniumVersion()));
            webDriverContainer.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        startContainer(webDriverContainer, this.testConfiguration);

        if (this.testConfiguration.vnc()) {
            LOGGER.info("VNC server address: [{}]", webDriverContainer.getVncAddress());
        }

        return webDriverContainer;
    }

    /**
     * @return the path where the test resources are stored by Maven after test compilation, on the host.
     */
    private String getTestResourcePathOnHost()
    {
        String testClassesDirectory;
        String mavenBuildDir = System.getProperty("maven.build.dir");
        if (mavenBuildDir == null) {
            testClassesDirectory = "target/test-classes";
        } else {
            testClassesDirectory = String.format("%s/test-classes", this.testConfiguration.getOutputDirectory());
        }
        return testClassesDirectory;
    }
}
