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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer;
import org.xwiki.test.docker.internal.junit5.browser.BrowserContainerExecutor;
import org.xwiki.test.docker.internal.junit5.database.DatabaseContainerExecutor;
import org.xwiki.test.docker.internal.junit5.servletengine.ServletContainerExecutor;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.maven.ArtifactResolver;
import org.xwiki.test.integration.maven.MavenResolver;
import org.xwiki.test.integration.maven.RepositoryResolver;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

import com.google.common.primitives.Ints;

import ch.qos.logback.classic.Level;

import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.followOutput;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.getResultFileLocation;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.isLocal;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.setLogbackLoggerLevel;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.startContainer;
import static org.xwiki.test.docker.internal.junit5.DockerTestUtils.takeScreenshot;

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
    BeforeEachCallback, AfterEachCallback, ParameterResolver, TestExecutionExceptionHandler, ExecutionCondition
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDockerExtension.class);

    private static final String SUPERADMIN = "superadmin";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception
    {
        // If the current tests has parents and one of them has the @UITest annotation, it means all containers are
        // already started and we should not do anything.
        if (hasParentTestContainingUITestAnnotation(extensionContext)) {
            return;
        }

        // Note: TestConfiguration is created in evaluateExecutionCondition()à which executes before beforeAll()
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);

        // Programmatically enable logging for TestContainers code when verbose is on so that we can get the maximum
        // of debugging information.
        if (testConfiguration.isDebug()) {
            setLogbackLoggerLevel("org.testcontainers", Level.TRACE);
            setLogbackLoggerLevel("com.github.dockerjava", Level.WARN);
        }

        // Expose ports for SSH port forwarding so that containers can communicate with the host using the
        // "host.testcontainers.internal" host name.
        Testcontainers.exposeHostPorts(Ints.toArray(testConfiguration.getSSHPorts()));

        // Initialize resolvers.
        RepositoryResolver repositoryResolver = new RepositoryResolver(testConfiguration.isOffline());
        ArtifactResolver artifactResolver = new ArtifactResolver(testConfiguration.isOffline(),
            testConfiguration.isDebug(), repositoryResolver);
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
            File targetWARDirectory = new File(String.format("%s/xwiki", testConfiguration.getOutputDirectory()));
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
    public void beforeEach(ExtensionContext extensionContext) throws Exception
    {
        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            LOGGER.info("(*) Start VNC container...");

            BrowserWebDriverContainer webDriverContainer = loadBrowserWebDriverContainer(extensionContext);

            // Use the maximum resolution available so that we have the maximum number of UI elements visible and
            // reduce the risks of false positives due to not visible elements.
            webDriverContainer.getWebDriver().manage().window().maximize();
            VncRecordingContainer vnc = new VncRecordingContainer(webDriverContainer);
            saveVNC(extensionContext, vnc);
            startContainer(vnc, testConfiguration);
        }

        LOGGER.info("(*) Starting test [{}]", extensionContext.getTestMethod().get().getName());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext)
    {
        LOGGER.info("(*) Stopping test [{}]", extensionContext.getTestMethod().get().getName());

        // If running locally then save the screenshot and the video by default for easier debugging. For the moment
        // we consider we're running locally if we're not running inside a Docker container. To be improved.
        if (isLocal()) {
            saveScreenshotAndVideo(extensionContext);
        }

        TestConfiguration testConfiguration = loadTestConfiguration(extensionContext);
        if (testConfiguration.vnc()) {
            VncRecordingContainer vnc = loadVNC(extensionContext);
            vnc.stop();

            // Note: We don't need to stop the BrowserWebDriverContainer since that's done automatically by
            // TestContainers. This allows the test to finish faster and thus provide faster results (because stopping
            // the container takes a bit of time).
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable)
        throws Throwable
    {
        // Only take screenshot & save video if not executing locally as otherwise they're always taken and saved!
        if (!isLocal()) {
            saveScreenshotAndVideo(extensionContext);
        }

        // Display the current jenkins agent name to have debug information printed in the Jenkins page for the test.
        String agentName = System.getProperty("jenkinsAgentName");
        if (agentName != null) {
            LOGGER.info("Jenkins Agent: [{}]", agentName);
        }

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

        // Shutdown the test context
        shutdownPersistentTestContext(testContext);

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

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext)
    {
        // If the tests has parent tests and one of them has the @UITest annotation then it means all containers
        // have already been started and thus the servlet engine is supported.
        if (!hasParentTestContainingUITestAnnotation(extensionContext)) {
            UITest uiTest = extensionContext.getRequiredTestClass().getAnnotation(UITest.class);
            TestConfiguration testConfiguration = new TestConfiguration(uiTest);
            // Save the test configuration so that we can access it in afterAll()
            saveTestConfiguration(extensionContext, testConfiguration);

            // Skip the test if the Servlet Engine selected is in the forbidden list
            if (isServletEngineForbidden(testConfiguration)) {
                return ConditionEvaluationResult.disabled(String.format("Servlet Engine [%s] is forbidden, skipping",
                    testConfiguration.getServletEngine()));
            } else {
                return ConditionEvaluationResult.enabled(String.format("Servlet Engine [%s] is supported, continuing",
                    testConfiguration.getServletEngine()));
            }
        } else {
            return ConditionEvaluationResult.enabled("Servlet Engine is supported by parent Test class, continuing");
        }
    }

    private boolean hasParentTestContainingUITestAnnotation(ExtensionContext extensionContext)
    {
        boolean hasUITest = false;
        ExtensionContext current = extensionContext;
        // Note: the top level context is the JUnitJupiterExtensionContext one and it doesn't contain any test and
        // thus calling getRequiredTestClass() throws an exception on it, which is why we skip it.
        while (current.getParent().get().getParent().isPresent() && !hasUITest) {
            current = current.getParent().get();
            hasUITest = current.getRequiredTestClass().isAnnotationPresent(UITest.class);
        }
        return hasUITest;
    }

    private BrowserWebDriverContainer startBrowser(TestConfiguration testConfiguration,
        ExtensionContext extensionContext) throws Exception
    {
        BrowserContainerExecutor browserContainerExecutor = new BrowserContainerExecutor(testConfiguration);
        BrowserWebDriverContainer webDriverContainer = browserContainerExecutor.start();

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
        testContext.getUtil().setURLPrefix(computeXWikiURLPrefix(
            testConfiguration.getServletEngine().getInternalIP(),
            testConfiguration.getServletEngine().getInternalPort()));

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

    private void startServletEngine(File sourceWARDirectory, TestConfiguration testConfiguration,
        ArtifactResolver artifactResolver, MavenResolver mavenResolver, RepositoryResolver repositoryResolver,
        ExtensionContext extensionContext) throws Exception
    {
        ServletContainerExecutor executor =
            new ServletContainerExecutor(testConfiguration, artifactResolver, mavenResolver, repositoryResolver);
        saveServletContainerExecutor(extensionContext, executor);
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
            LOGGER.info("XWiki ping URL = " + xwikiURL);
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
        // Install extensions in the running XWiki
        String xwikiRESTURL = String.format("%s/rest", loadXWikiURL(context));
        ExtensionInstaller extensionInstaller = new ExtensionInstaller(artifactResolver, mavenResolver);
        extensionInstaller.installExtensions(xwikiRESTURL, SUPERADMIN, "pass", SUPERADMIN);
    }

    private String computeXWikiURLPrefix(String ip, int port)
    {
        return String.format("http://%s:%s/xwiki", ip, port);
    }

    private boolean isServletEngineForbidden(TestConfiguration testConfiguration)
    {
        return testConfiguration.getForbiddenServletEngines().contains(testConfiguration.getServletEngine());
    }

    private void saveScreenshotAndVideo(ExtensionContext extensionContext)
    {
        // Take screenshot
        takeScreenshot(extensionContext, loadTestConfiguration(extensionContext),
            loadXWikiWebDriver(extensionContext));

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
            LOGGER.info("(*) VNC recording of test has been saved to [{}]", recordingFile);
        }
    }
}
