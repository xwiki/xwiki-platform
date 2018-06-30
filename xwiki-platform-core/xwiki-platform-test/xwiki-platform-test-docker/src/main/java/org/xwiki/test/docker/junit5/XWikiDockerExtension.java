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
import java.net.InetAddress;
import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.VncRecordingContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.PersistentTestContext;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * JUnit5 Extension to inject {@link TestUtils} and {@link XWikiWebDriver} instances in tests, to start/start XWiki
 * automatically and start/stop Browsers automatically.
 * <p>
 * Example:
 * <pre><code>
 * @UITest
 * public class SeleniumTest
 * {
 *     @Test
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
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class XWikiDockerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
    AfterEachCallback, ParameterResolver
{
    private static final String URL_PREFIX_PROPERTY = "xwiki.test.baseURL";

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(XWikiDockerExtension.class);

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception
    {
        // Since the browser is running inside a Docker container, it needs to access the host where XWiki is running.
        // Thus it cannot access it with "localhost" since that points to the Docker container itself. Thus we need
        // to find one IP (we use the first IP, hoping that it'll be a good one).
        // We do this only if the "xwiki.test.baseURL" system property has not already been defined, thus allowing
        // user override.
        // Note that this system property is used by the XWikiExecutor code to start/stop XWiki.
        if (System.getProperty(URL_PREFIX_PROPERTY) == null) {
            System.setProperty(URL_PREFIX_PROPERTY, "http://" + InetAddress.getLocalHost().getHostAddress());
        }

        // Create a single BrowserWebDriverContainer instance and reuse it for all the tests in the test class.
        UITest uiTestAnnotation = extensionContext.getRequiredTestClass().getAnnotation(UITest.class);
        BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer<>()
            .withDesiredCapabilities(uiTestAnnotation.value().getCapabilities())
            .withNetwork(Network.SHARED)
            .withNetworkAliases("vnchost")
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())));

        webDriverContainer.start();

        // Store it so that we can stop it later on
        saveBrowserWebDriverContainer(extensionContext, webDriverContainer);

        // Construct the XWikiWebDriver instance and save it in case it needs to be injected as a test method parameter.
        XWikiWebDriver xwikiWebDriver = new XWikiWebDriver(webDriverContainer.getWebDriver());
        saveXWikiWebDriver(extensionContext, xwikiWebDriver);

        // Initialize the test context
        PersistentTestContext testContext = initializePersistentTestContext(xwikiWebDriver);
        savePersistentTestContext(extensionContext, testContext);

        // Start XWiki
        // TODO: in the future, refactor XWikiExecutor so that it becomes an interface and so that we can have
        // various implementations, including one using Docker to start/stop XWiki.
        for (XWikiExecutor executor : testContext.getExecutors()) {
            executor.start();
        }

        // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
        // in TestUtils), including the login form. Whenever a new user logs in we need to recache.
        // Note that this requires a running XWiki instance.
        testContext.getUtil().recacheSecretToken();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception
    {
        BrowserWebDriverContainer webDriverContainer = loadBrowserWebDriverContainer(extensionContext);

        VncRecordingContainer vnc = new VncRecordingContainer(webDriverContainer);
        saveVNC(extensionContext, vnc);
        vnc.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception
    {
        VncRecordingContainer vnc = loadVNC(extensionContext);
        // TODO: Record the video only if the test has failed, when Junit5 add support for extensions to know the test
        // result status... See https://github.com/junit-team/junit5/issues/542
        File recordingDir = new File("./target/");
        vnc.saveRecordingToFile(new File(recordingDir, extensionContext.getRequiredTestClass().getName() + "-"
            + extensionContext.getRequiredTestMethod().getName() + ".flv"));
        vnc.stop();

        // Note: We don't need to stop the BrowserWebDriverContainer since that's done automatically by TestContainers.
        // This allows the test to finish faster and thus provide faster results (because stopping the container takes
        // a bit of time).
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

        // Stop XWiki
        for (XWikiExecutor executor : testContext.getExecutors()) {
            executor.stop();
        }

        // Shutdown the test context
        shutdownPersistentTestContext(testContext);
    }

    private void saveXWikiWebDriver(ExtensionContext context, XWikiWebDriver xwikiWebDriver)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(XWikiWebDriver.class, xwikiWebDriver);
    }

    private XWikiWebDriver loadXWikiWebDriver(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(XWikiWebDriver.class, XWikiWebDriver.class);
    }

    private void saveVNC(ExtensionContext context, VncRecordingContainer vnc)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(VncRecordingContainer.class, vnc);
    }

    private VncRecordingContainer loadVNC(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(VncRecordingContainer.class, VncRecordingContainer.class);
    }

    private void saveBrowserWebDriverContainer(ExtensionContext context, BrowserWebDriverContainer container)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(BrowserWebDriverContainer.class, container);
    }

    private BrowserWebDriverContainer loadBrowserWebDriverContainer(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(BrowserWebDriverContainer.class, BrowserWebDriverContainer.class);
    }

    private void savePersistentTestContext(ExtensionContext context, PersistentTestContext testContext)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(PersistentTestContext.class, testContext);
    }

    private PersistentTestContext loadPersistentTestContext(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(PersistentTestContext.class, PersistentTestContext.class);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE);
    }

    private PersistentTestContext initializePersistentTestContext(XWikiWebDriver driver)
    {
        PersistentTestContext testContext;

        try {
            PersistentTestContext initialTestContext =
                new PersistentTestContext(Arrays.asList(new XWikiExecutor(0)), driver);
            testContext = initialTestContext.getUnstoppable();
            AbstractTest.initializeSystem(testContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PersistentTestContext", e);
        }

        return testContext;
    }

    private void shutdownPersistentTestContext(PersistentTestContext testContext)
    {
        if (testContext != null) {
            try {
                testContext.shutdown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to shutdown PersistentTestContext", e);
            }
        }
    }
}
