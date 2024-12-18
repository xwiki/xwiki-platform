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
package org.xwiki.test.docker.internal.junit5;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.TestcontainersConfiguration;
import org.xwiki.test.docker.internal.junit5.browser.BrowserContainerExecutor;
import org.xwiki.test.docker.internal.junit5.database.DatabaseContainerExecutor;
import org.xwiki.test.docker.internal.junit5.servletengine.ServletContainerExecutor;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;
import org.xwiki.test.integration.maven.RepositoryResolver;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

import com.google.common.primitives.Ints;

import ch.qos.logback.classic.Level;

import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.followOutput;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.getAgentName;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.getResultFileLocation;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.setLogbackLoggerLevel;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.startContainer;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.takeScreenshot;

/**
 * JUnit5 Extension to inject {@link TestUtils} and {@link XWikiWebDriver} instances in tests and that performs the
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
 *         driver.get("https://xwiki.org");
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
public class XWikiDockerExtension extends AbstractExecutionConditionExtension
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDockerExtension.class);

    private static final String SUPERADMIN = "superadmin";

    private boolean isVncStarted;

    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        try {
            beforeAllInternal(extensionContext);
        } catch (Exception e) {
            raiseException(e);
        }
    }

    private void beforeAllInternal(ExtensionContext extensionContext) throws Exception
    {
        // This method is going to be called for the top level test class but also for nested test classes. So if
        // the currently executing test class has parents and one of them has the @UITest annotation, it means the
        // test has already been setup (all Docker containers are already started, etc), and thus we should not do
        // anything.
        if (hasParentTestContainingUITestAnnotation(extensionContext)) {
            return;
        }

        TestConfiguration testConfiguration = computeTestConfiguration(extensionContext);

        // Programmatically enable logging for TestContainers code when verbose is on so that we can get the maximum
        // of debugging information.
        if (testConfiguration.isVerbose()) {
            enableVerboseLogs();
        }

        // Expose ports for SSH port forwarding so that containers can communicate with the host using the
        // "host.testcontainers.internal" host name.
        Testcontainers.exposeHostPorts(Ints.toArray(testConfiguration.getSSHPorts()));

        // Initialize resolvers.
        RepositoryResolver repositoryResolver = new RepositoryResolver(testConfiguration.isOffline());
        ArtifactResolver artifactResolver = new ArtifactResolver(testConfiguration.isOffline(),
            testConfiguration.isVerbose(), repositoryResolver);
        MavenResolver mavenResolver =
            new MavenResolver(testConfiguration.getProfiles(), artifactResolver, repositoryResolver);

        // If the Servlet Engine is external then consider XWiki is already configured, provisioned and running.
        if (!testConfiguration.getServletEngine().equals(ServletEngine.EXTERNAL)) {
            // Start the Database.
            // Note: We start the database before the XWiki WAR is created because we need the IP of the docker
            // container for the database when configuring the JDBC URL, in the case when the servlet container is
            // running outside of docker and thus outside of the shared docker network...
            LOGGER.info("(*) Starting database [{}]...", testConfiguration.getDatabase());
            startDatabase(testConfiguration);

            // Build the XWiki WAR
            LOGGER.info("(*) Building custom XWiki WAR...");
            File targetWARDirectory = getServletContainerExecutor(testConfiguration, artifactResolver, mavenResolver,
                repositoryResolver, extensionContext).getWARDirectory();
            WARBuilder builder = new WARBuilder(testConfiguration, targetWARDirectory, artifactResolver, mavenResolver,
                repositoryResolver);
            builder.build();

            // Start the Servlet Engine
            LOGGER.info("(*) Starting Servlet container [{}]...", testConfiguration.getServletEngine());
            startServletEngine(targetWARDirectory, testConfiguration, artifactResolver, mavenResolver,
                repositoryResolver, extensionContext);

            // Provision XWiki by installing all required extensions.
            LOGGER.info("(*) Provision extensions for test...");
            provisionExtensions(artifactResolver, mavenResolver, extensionContext);
        } else {
            // Set the IP/port for the container since startServletEngine() wasn't called and it's set there normally.
            testConfiguration.getServletEngine().setIP("localhost");
            testConfiguration.getServletEngine().setPort(8080);
            setXWikiURL(testConfiguration, extensionContext);

            LOGGER.info("XWiki is already started, using running instance at [{}] to execute the tests...",
                loadXWikiURL(extensionContext));

            // Note: Provisioning is not done in this case, you're supposed to have an XWiki instance that contains
            // what's needed for the tests.
        }

        // Start the Browser (this creates and initializes the PersistentTestContext, XWikiWebDriver objects)
        startBrowser(testConfiguration, extensionContext);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext)
    {
        try {
            beforeEachInternal(extensionContext);
        } catch (Exception e) {
            raiseException(e);
        }
    }

    private void enableVerboseLogs()
    {
        // Enable TC logs to get more info
        setLogbackLoggerLevel("org.testcontainers", Level.TRACE);
        setLogbackLoggerLevel("org.rnorth", Level.TRACE);
        setLogbackLoggerLevel("org.xwiki.test.docker.internal.junit5.browser", Level.TRACE);
        setLogbackLoggerLevel("com.github.dockerjava", Level.WARN);
        // Don't display the stack trace that TC displays when it cannot find a config file override
        // ("Testcontainers config override was found on file:/root/.testcontainers.properties but the file was not
        // found), since this is not a problem and it's optional.
        // See https://github.com/testcontainers/testcontainers-java/issues/2253
        setLogbackLoggerLevel("org.testcontainers.utility.TestcontainersConfiguration", Level.WARN);
        // Also enable some debug logs from our test framework
        setLogbackLoggerLevel("org.xwiki.test.docker", Level.DEBUG);
        setLogbackLoggerLevel("org.xwiki.test.extension", Level.DEBUG);
        // Get logs when starting the sshd container
        setLogbackLoggerLevel(DockerLoggerFactory.getLogger(
            TestcontainersConfiguration.getInstance().getSSHdImage()).getName(), Level.TRACE);
        // Get logs when starting the vnc container
        setLogbackLoggerLevel(DockerLoggerFactory.getLogger(
            TestcontainersConfiguration.getInstance().getVncRecordedContainerImage()).getName(), Level.TRACE);
    }

    private TestConfiguration computeTestConfiguration(ExtensionContext extensionContext)
    {
        // Note: TestConfiguration is created in evaluateExecutionCondition() which executes before beforeAll()
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        mergeTestConfigurationInGlobalContext(testConfiguration, extensionContext);
        saveTestConfiguration(extensionContext, testConfiguration);
        return testConfiguration;
    }

    private void beforeEachInternal(ExtensionContext extensionContext)
    {
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            LOGGER.info("(*) Starting VNC container...");

            BrowserWebDriverContainer<?> webDriverContainer = loadBrowserWebDriverContainer(extensionContext);

            // Use the maximum resolution available so that we have the maximum number of UI elements visible and
            // reduce the risks of false positives due to not visible elements.
            webDriverContainer.getWebDriver().manage().window().maximize();

            VncRecordingContainer vnc = new VncRecordingContainer(webDriverContainer);
            saveVNC(extensionContext, vnc);

            // TODO: Remove once we understand and fix flickerings when starting the VNC container on our CI agents.
            // If it fails to start, we just skip it. It means there'll be no screenshot taken and video recorded
            // but at least the tests will be able to execute.
            try {
                startContainer(vnc, testConfiguration);
                this.isVncStarted = true;
            } catch (Exception e) {
                LOGGER.warn("Failed to start the VNC container. Skipping it so that tests can execute. Root error [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
                LOGGER.debug("Error when starting VNC container", e);
                this.isVncStarted = false;
            }
        }
        String testMethodName = extensionContext.getTestMethod().get().getName();

        // Update the WCAG validation context.
        loadPersistentTestContext(extensionContext).getUtil().getWCAGUtils().changeWCAGTestMethod(testMethodName);

        LOGGER.info("(*) Starting test [{}]", testMethodName);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext)
    {
        LOGGER.info("(*) Stopping test [{}]", extensionContext.getTestMethod().get().getName());

        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc() && this.isVncStarted) {
            LOGGER.info("(*) Stopping VNC container...");
            VncRecordingContainer vnc = loadVNC(extensionContext);
            vnc.stop();

            // Note: We don't need to stop the BrowserWebDriverContainer since that's done automatically by
            // TestContainers. This allows the test to finish faster and thus provide faster results (because stopping
            // the container takes a bit of time).
        }

        // Reset current wiki to main wiki
        loadPersistentTestContext(extensionContext).getUtil().setCurrentWiki("xwiki");
    }

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable)
        throws Throwable
    {
        try {
            saveScreenshotAndVideo(extensionContext);
        } catch (Exception e) {
            LOGGER.error("Failed to save the video", e);
        }

        // Display the current jenkins agent name to have debug information printed in the Jenkins page for the test.
        displayAgentName();

        throw throwable;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        return XWikiWebDriver.class.isAssignableFrom(type) || TestUtils.class.isAssignableFrom(type)
            || TestConfiguration.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
    {
        Class<?> type = parameterContext.getParameter().getType();
        if (XWikiWebDriver.class.isAssignableFrom(type)) {
            return loadXWikiWebDriver(extensionContext);
        } else if (TestConfiguration.class.isAssignableFrom(type)) {
            return loadTestConfiguration(extensionContext);
        } else {
            return loadPersistentTestContext(extensionContext).getUtil();
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception
    {
        // If the current tests has parents and one of them has the @UITest annotation, it means the containers should
        // be stopped by the parent having the annotation.
        if (hasParentTestContainingUITestAnnotation(extensionContext)) {
            return;
        }

        PersistentTestContext testContext = loadPersistentTestContext(extensionContext);

        if (testContext != null) {
            // End the wcag validation process.
            testContext.getUtil().getWCAGUtils().endWCAGValidation();

            // Shutdown the test context
            shutdownPersistentTestContext(testContext);
        }

        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);

        // Only stop DB and Servlet Engine if we have started them
        if (!testConfiguration.getServletEngine().equals(ServletEngine.EXTERNAL)) {
            // Stop the DB
            LOGGER.info("(*) Stopping database [{}]...", testConfiguration.getDatabase());
            stopDatabase(testConfiguration);

            // Stop the Servlet Engine
            LOGGER.info("(*) Stopping Servlet container [{}]...", testConfiguration.getServletEngine());
            stopServletEngine(extensionContext);
        }
    }

    private BrowserWebDriverContainer startBrowser(TestConfiguration testConfiguration,
        ExtensionContext extensionContext) throws Exception
    {
        BrowserContainerExecutor browserContainerExecutor = new BrowserContainerExecutor(testConfiguration);
        BrowserWebDriverContainer<?> webDriverContainer = browserContainerExecutor.start();

        // Store it so that we can retrieve it later on.
        // Note that we don't need to stop it as this is taken care of by TestContainers
        saveBrowserWebDriverContainer(extensionContext, webDriverContainer);

        // Construct the XWikiWebDriver instance and save it in case it needs to be injected as a test method parameter.
        XWikiWebDriver xwikiWebDriver = new XWikiWebDriver(webDriverContainer.getWebDriver());
        saveXWikiWebDriver(extensionContext, xwikiWebDriver);

        // Initialize the test context
        LOGGER.info("(*) Initialize Test Context...");
        PersistentTestContext testContext =
            new PersistentTestContext(Arrays.asList(new XWikiExecutor(0)), xwikiWebDriver);
        AbstractTest.initializeSystem(testContext);
        savePersistentTestContext(extensionContext, testContext);

        // Set the URLs to access XWiki:
        // - the one used inside the Selenium container
        testContext.getUtil().setURLPrefix(computeXWikiURLPrefix(
            testConfiguration.getServletEngine().getInternalIP(),
            testConfiguration.getServletEngine().getInternalPort()));

        // Setup the wcag validation context.
        testContext.getUtil().getWCAGUtils().setupWCAGValidation(
            testConfiguration.isWCAG(),
            extensionContext.getTestClass().get().getName(),
            testConfiguration.shouldWCAGStopOnError());


        // - the one used by RestTestUtils, i.e. outside of any container
        testContext.getUtil().rest().setURLPrefix(loadXWikiURL(extensionContext));

        // Display logs after the container has been started so that we can see problems happening in the containers
        followOutput(webDriverContainer, getClass());

        // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
        // in TestUtils), including the login form. Whenever a new user logs in we need to recache.
        // Note that this requires a running XWiki instance.
        testContext.getUtil().recacheSecretToken();

        return webDriverContainer;
    }

    private void startDatabase(TestConfiguration testConfiguration) throws Exception
    {
        DatabaseContainerExecutor executor = new DatabaseContainerExecutor();
        executor.start(testConfiguration);
    }

    private void stopDatabase(TestConfiguration testConfiguration)
    {
        DatabaseContainerExecutor executor = new DatabaseContainerExecutor();
        executor.stop(testConfiguration);
    }

    private ServletContainerExecutor getServletContainerExecutor(TestConfiguration testConfiguration,
        ArtifactResolver artifactResolver, MavenResolver mavenResolver, RepositoryResolver repositoryResolver,
        ExtensionContext extensionContext)
    {
        ServletContainerExecutor executor = loadServletContainerExecutor(extensionContext);
        if (executor == null) {
            executor = new ServletContainerExecutor(testConfiguration, artifactResolver, mavenResolver,
                repositoryResolver);
            saveServletContainerExecutor(extensionContext, executor);
        }
        return executor;
    }

    private void startServletEngine(File sourceWARDirectory, TestConfiguration testConfiguration,
        ArtifactResolver artifactResolver, MavenResolver mavenResolver, RepositoryResolver repositoryResolver,
        ExtensionContext extensionContext) throws Exception
    {
        ServletContainerExecutor executor = getServletContainerExecutor(testConfiguration, artifactResolver,
            mavenResolver, repositoryResolver, extensionContext);
        executor.start(sourceWARDirectory);
        setXWikiURL(testConfiguration, extensionContext);
    }

    private void setXWikiURL(TestConfiguration testConfiguration, ExtensionContext extensionContext)
    {
        // URL to access XWiki from the host.
        String xwikiURL = computeXWikiURLPrefix(testConfiguration.getServletEngine().getIP(),
            testConfiguration.getServletEngine().getPort());
        saveXWikiURL(extensionContext, xwikiURL);

        if (testConfiguration.isVerbose()) {
            LOGGER.info("XWiki ping URL = {}", xwikiURL);
        }
    }

    private void stopServletEngine(ExtensionContext extensionContext) throws Exception
    {
        ServletContainerExecutor executor = loadServletContainerExecutor(extensionContext);
        if (executor != null) {
            executor.stop();
        }
    }

    private void provisionExtensions(ArtifactResolver artifactResolver, MavenResolver mavenResolver,
        ExtensionContext context) throws Exception
    {
        // Initialize an extension installer
        ExtensionInstaller extensionInstaller = new ExtensionInstaller(context, artifactResolver, mavenResolver);
        DockerTestUtils.setExtensionInstaller(context, extensionInstaller);

        // Install extensions in the running XWiki
        extensionInstaller.installExtensions(SUPERADMIN, "pass", SUPERADMIN);
    }

    private String computeXWikiURLPrefix(String ip, int port)
    {
        return String.format("http://%s:%s/xwiki", ip, port);
    }

    private void saveScreenshotAndVideo(ExtensionContext extensionContext)
    {
        // Take screenshot
        takeScreenshot(extensionContext, loadTestConfiguration(extensionContext), loadXWikiWebDriver(extensionContext));

        // Save the video
        saveVideo(extensionContext);
    }

    private void saveVideo(ExtensionContext extensionContext)
    {
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            VncRecordingContainer vnc = loadVNC(extensionContext);
            File recordingFile = getResultFileLocation("flv", testConfiguration, extensionContext);
            vnc.saveRecordingToFile(recordingFile);
            LOGGER.info("VNC recording of test has been saved to [{}]", recordingFile);
        }
    }

    private void displayAgentName()
    {
        String agentName = getAgentName();
        if (agentName != null) {
            LOGGER.info("Jenkins Agent: [{}]", agentName);
        }
    }

    private void raiseException(Exception e)
    {
        String extraMessage = getAgentName() == null ? "" : String.format(" on agent [%s]", getAgentName());
        throw new RuntimeException(
            String.format("Error setting up the XWiki testing environment%s", extraMessage), e);
    }
}
