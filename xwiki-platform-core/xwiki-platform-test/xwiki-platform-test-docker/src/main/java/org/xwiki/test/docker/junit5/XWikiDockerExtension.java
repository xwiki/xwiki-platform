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
package org.xwiki.test.docker.junit5;

import java.io.File;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.VncRecordingContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.xwiki.test.docker.junit5.database.DatabaseContainerExecutor;
import org.xwiki.test.docker.junit5.servletEngine.ServletContainerExecutor;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * JUnit5 Extension to inject {@link TestUtils} and {@link XWikiWebDriver} instances in tests and that peforms the
 * following tasks.
 * <ul>
 * <li>create a minimal XWiki WAR</li>
 * <li>start a database</li>
 * <li>start a servlet container and deploy xwiki in it</li>
 * <li>provision XAR dependencies from the test inside the running xwiki</li>
 * <li>start a browser automatically</li>
 * <li>start a VNC server to be able to connect to the UI and to record a video of the test execution</li>
 * </ul>
 *
 * <p>
 * Example:
 * <pre><code>
 * &#064;UITest
 * public class SeleniumTest
 * {
 *     &#064;Test
 *     public void test(XWikiWebDriver driver, TestUtils setup)
 *     {
 *         driver.get("http://xwiki.org");
 *         assertThat(driver.getTitle(),
 *             containsString("XWiki - The Advanced Open Source Enterprise and Application Wiki"));
 *         driver.findElement(By.linkText("XWiki's concept")).click();
 *     }
 * }
 * </code></pre>
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class XWikiDockerExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback,
    BeforeEachCallback, AfterEachCallback, ParameterResolver, TestExecutionExceptionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDockerExtension.class);

    private static final String SUPERADMIN = "superadmin";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception
    {
        TestConfiguration testConfiguration = new TestConfiguration(
            extensionContext.getRequiredTestClass().getAnnotation(UITest.class));
        // Save the test configuration so that we can access it in afterAll()
        saveTestConfiguration(extensionContext, testConfiguration);

        // Initialize resolvers.
        RepositoryResolver repositoryResolver = new RepositoryResolver(testConfiguration);
        ArtifactResolver artifactResolver = new ArtifactResolver(testConfiguration, repositoryResolver);
        MavenResolver mavenResolver = new MavenResolver(artifactResolver, repositoryResolver);

        // If the Servlet Engine is external then consider XWiki is already configured, provisioned and running.
        if (!testConfiguration.getServletEngine().equals(ServletEngine.EXTERNAL)) {
            // XWiki is not started
            LOGGER.info("XWiki is not started, starting all...");

            // Start the Database.
            // Note: We start the database before the XWiki WAR is created because we need the IP of the docker
            // container for the database when configuring the JDBC URL, in the case when the servlet container is
            // running outside of docker and thus outside of the shared docker network...
            LOGGER.info("(*) Starting database [{}]...", testConfiguration.getDatabase());
            startDatabase(testConfiguration);

            // Build the XWiki WAR
            LOGGER.info("(*) Building custom XWiki WAR...");
            File targetWARDirectory = new File(String.format("%s/xwiki", testConfiguration.getOutputDirectory()));
            // If the directory exists, skip the rebuilding of the full XWiki WAR, allowing to re-run the test faster
            WARBuilder builder;
            if (!targetWARDirectory.exists()) {
                LOGGER.info("XWiki WAR directory [{}] doesn't exists, rebuilding WAR!", targetWARDirectory);
                builder = new WARBuilder(testConfiguration, false, artifactResolver, mavenResolver, repositoryResolver);
            } else {
                LOGGER.info("XWiki WAR directory [{}] exists, rebuilding only the minimum!", targetWARDirectory);
                builder = new WARBuilder(testConfiguration, true, artifactResolver, mavenResolver, repositoryResolver);
            }
            builder.build(testConfiguration, targetWARDirectory);

            // Start the Servlet Engine
            LOGGER.info("(*) Starting Servlet container [{}]...", testConfiguration.getServletEngine());
            startServletEngine(targetWARDirectory, testConfiguration, artifactResolver, mavenResolver,
                repositoryResolver, extensionContext);

            // Provision XWiki by installing all required extensions.
            LOGGER.info("(*) Provision XAR extensions for test...");
            provisionExtensions(artifactResolver, mavenResolver, extensionContext);
        } else {
            LOGGER.info("XWiki is already started, using running instance at [%s] to execute the tests...",
                loadXWikiURL(extensionContext));

            // Note: Provisioning is not done in this case, you're supposed to have an XWiki instance that contains
            // what's needed for the tests.
        }

        // Start the Browser (this creates and initializes the PersistentTestContext, XWikiWebDriver objects)
        startBrowser(testConfiguration, extensionContext);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception
    {
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            LOGGER.info("(*) Start VNC container...");

            BrowserWebDriverContainer webDriverContainer = loadBrowserWebDriverContainer(extensionContext);

            VncRecordingContainer vnc = new VncRecordingContainer(webDriverContainer);
            saveVNC(extensionContext, vnc);
            vnc.start();
        }

        LOGGER.info("(*) Starting test...");
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception
    {
        LOGGER.info("(*) Stopping test...");

        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            VncRecordingContainer vnc = loadVNC(extensionContext);
            // TODO: Record the video only if the test has failed, when Junit5 add support for extensions to know the
            // test result status... See https://github.com/junit-team/junit5/issues/542
            File recordingDir = new File(testConfiguration.getOutputDirectory());
            File recordingFile = new File(recordingDir, String.format("%s-%s.flv",
                extensionContext.getRequiredTestClass().getName(), extensionContext.getRequiredTestMethod().getName()));
            vnc.saveRecordingToFile(recordingFile);
            vnc.stop();

            // Note: We don't need to stop the BrowserWebDriverContainer since that's done automatically by
            // TestContainers. This allows the test to finish faster and thus provide faster results (because stopping
            // the container takes a bit of time).
            LOGGER.info("(*) VNC recording of test has been saved to [{}]", recordingFile);
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable)
        throws Throwable
    {
        DockerTestUtils.takeScreenshot(extensionContext, loadTestConfiguration(extensionContext),
            loadXWikiWebDriver(extensionContext));
        throw throwable;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        return XWikiWebDriver.class.isAssignableFrom(type) || TestUtils.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        if (XWikiWebDriver.class.isAssignableFrom(type)) {
            return loadXWikiWebDriver(extensionContext);
        } else {
            return loadPersistentTestContext(extensionContext).getUtil();
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception
    {
        PersistentTestContext testContext = loadPersistentTestContext(extensionContext);

        // Shutdown the test context
        shutdownPersistentTestContext(testContext);

        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);

        // Only stop DB and Servlet Engine if we have started them
        if (!testConfiguration.getServletEngine().equals(ServletEngine.EXTERNAL)) {
            // Stop the DB
            stopDatabase(testConfiguration);

            // Stop the Servlet Engine
            stopServletEngine(testConfiguration, extensionContext);
        }
    }

    private BrowserWebDriverContainer startBrowser(TestConfiguration testConfiguration,
        ExtensionContext extensionContext)
    {
        LOGGER.info("(*) Starting browser [{}]...", testConfiguration.getBrowser());

        // If XWiki is not running inside a Docker container, then it's not sharing a shared network and we need
        // to expose its port so that the container running Selenium can do ssh port forwarding to the host.
        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            Testcontainers.exposeHostPorts(8080);
        }

        // Create a single BrowserWebDriverContainer instance and reuse it for all the tests in the test class.
        BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer<>()
            .withDesiredCapabilities(testConfiguration.getBrowser().getCapabilities())
            .withNetwork(Network.SHARED)
            .withNetworkAliases("vnchost")
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null);

        if (testConfiguration.isVerbose()) {
            LOGGER.info(String.format("Docker image used: [%s]",
                BrowserWebDriverContainer.getImageForCapabilities(testConfiguration.getBrowser().getCapabilities())));
            webDriverContainer.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));
        }

        webDriverContainer.start();

        if (testConfiguration.vnc()) {
            LOGGER.info("VNC server address = " + webDriverContainer.getVncAddress());
        }

        // Store it so that we can retrieve it later on.
        // Note that we don't need to stop it as this is taken care of by TestContainers
        saveBrowserWebDriverContainer(extensionContext, webDriverContainer);

        // Construct the XWikiWebDriver instance and save it in case it needs to be injected as a test method parameter.
        XWikiWebDriver xwikiWebDriver = new XWikiWebDriver(webDriverContainer.getWebDriver());
        saveXWikiWebDriver(extensionContext, xwikiWebDriver);

        // Initialize the test context
        LOGGER.info("(*) Initialize Test Context...");
        PersistentTestContext testContext = initializePersistentTestContext(xwikiWebDriver);
        savePersistentTestContext(extensionContext, testContext);

        // Set the URLs to access XWiki:
        // - the one used inside the Selenium container

        if (testConfiguration.getServletEngine().isOutsideDocker()) {
            testContext.getUtil().setURLPrefix("http://host.testcontainers.internal:8080/xwiki");
        } else {
            testContext.getUtil().setURLPrefix("http://xwikiweb:8080/xwiki");
        }

        // - the one used by RestTestUtils, i.e. outside of any container
        testContext.getUtil().rest().setURLPrefix(loadXWikiURL(extensionContext));

        // Display logs after the container has been started so that we can see problems happening in the containers
        DockerTestUtils.followOutput(webDriverContainer, getClass());

        // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
        // in TestUtils), including the login form. Whenever a new user logs in we need to recache.
        // Note that this requires a running XWiki instance.
        testContext.getUtil().recacheSecretToken();

        return webDriverContainer;
    }

    private void startDatabase(TestConfiguration testConfiguration)
    {
        DatabaseContainerExecutor executor = new DatabaseContainerExecutor();
        executor.start(testConfiguration);
    }

    private void stopDatabase(TestConfiguration testConfiguration)
    {
        DatabaseContainerExecutor executor = new DatabaseContainerExecutor();
        executor.stop(testConfiguration);
    }

    private void startServletEngine(File sourceWARDirectory, TestConfiguration testConfiguration,
        ArtifactResolver artifactResolver, MavenResolver mavenResolver, RepositoryResolver repositoryResolver,
        ExtensionContext extensionContext) throws Exception
    {
        ServletContainerExecutor executor =
            new ServletContainerExecutor(testConfiguration, artifactResolver, mavenResolver, repositoryResolver);
        saveServletContainerExecutor(extensionContext, executor);
        String xwikiURL = executor.start(testConfiguration, sourceWARDirectory);

        saveXWikiURL(extensionContext, xwikiURL);
        if (testConfiguration.isVerbose()) {
            LOGGER.info("XWiki ping URL = " + xwikiURL);
        }
    }

    private void stopServletEngine(TestConfiguration testConfiguration, ExtensionContext extensionContext)
        throws Exception
    {
        ServletContainerExecutor executor = loadServletContainerExecutor(extensionContext);
        executor.stop(testConfiguration);
    }

    private void provisionExtensions(ArtifactResolver artifactResolver, MavenResolver mavenResolver,
        ExtensionContext context) throws Exception
    {
        // Install extensions in the running XWiki
        String xwikiRESTURL = String.format("%s/rest", loadXWikiURL(context));
        ExtensionInstaller extensionInstaller = new ExtensionInstaller(artifactResolver, mavenResolver);
        extensionInstaller.installExtensions(xwikiRESTURL, SUPERADMIN, "pass", SUPERADMIN);
    }
}
