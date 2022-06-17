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
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.OutputType;
import org.testcontainers.containers.VncRecordingContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.ui.XWikiWebDriver;

import ch.qos.logback.classic.Level;

import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.INFO;

/**
 * Test of {@link XWikiDockerExtension}.
 *
 * @version $Id$
 * @since 14.5RC1
 * @since 14.4.2
 * @since 13.10.7
 */
@ComponentTest
class XWikiDockerExtensionTest
{
    @XWikiTempDir
    private File screenshotDirectory;

    @XWikiTempDir
    private File testOutputDirectory;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(INFO);

    private XWikiDockerExtension xWikiDockerExtension = new XWikiDockerExtension();

    @Test
    void handleAfterEachMethodExecutionException() throws Exception
    {
        RuntimeException thrownException = new RuntimeException("TEST");

        ExtensionContext extensionContext = mock(ExtensionContext.class);
        Store store = mock(Store.class);
        XWikiWebDriver xWikiWebDriver = mock(XWikiWebDriver.class);
        TestConfiguration testConfiguration = mock(TestConfiguration.class);
        VncRecordingContainer vncRecordingContainer = mock(VncRecordingContainer.class);
        // Retrieve an arbitrary method to have a response for the testMethod.
        Method method = Object.class.getMethod("getClass");
        // Initialize a screenshot with a dummy content.
        this.screenshotDirectory.mkdirs();
        File sourceFile = new File(this.screenshotDirectory, "video.flv");
        writeStringToFile(sourceFile, "somecontent", defaultCharset());

        File screenshotsDir = new File(this.testOutputDirectory, "screenshots");
        File expectedScreenshotFile =
            new File(screenshotsDir, "hsqldb_embedded-1.0-default-jetty-2.0-firefox-java.lang.Object-getClass.png");
        File expectedVideoFile =
            new File(screenshotsDir, "hsqldb_embedded-1.0-default-jetty-2.0-firefox-java.lang.Object-getClass.flv");

        // Extension Context initialization.
        when(extensionContext.getRoot()).thenReturn(extensionContext);
        when(extensionContext.getStore(any())).thenReturn(store);
        when(extensionContext.getTestMethod()).thenReturn(Optional.of(method));
        when(extensionContext.getRequiredTestClass()).thenReturn((Class) Object.class);
        when(extensionContext.getRequiredTestMethod()).thenReturn(method);
        when(extensionContext.getUniqueId()).thenReturn("");

        // Store initialization.
        when(store.get(XWikiWebDriver.class, XWikiWebDriver.class)).thenReturn(xWikiWebDriver);
        when(store.get(TestConfiguration.class, TestConfiguration.class)).thenReturn(testConfiguration);
        when(store.get(VncRecordingContainer.class, VncRecordingContainer.class)).thenReturn(vncRecordingContainer);
        when(xWikiWebDriver.getScreenshotAs(OutputType.FILE))
            .thenReturn(sourceFile);

        // Test configuration initialization.
        when(testConfiguration.getOutputDirectory()).thenReturn(this.testOutputDirectory.getAbsolutePath());
        when(testConfiguration.getBrowser()).thenReturn(Browser.FIREFOX);
        when(testConfiguration.getDatabase()).thenReturn(Database.HSQLDB_EMBEDDED);
        when(testConfiguration.getDatabaseTag()).thenReturn("1.0");
        when(testConfiguration.getServletEngine()).thenReturn(ServletEngine.JETTY);
        when(testConfiguration.getServletEngineTag()).thenReturn("2.0");
        when(testConfiguration.vnc()).thenReturn(true);

        // Verify that the exception is propagated.
        RuntimeException actualException = assertThrows(RuntimeException.class,
            () -> this.xWikiDockerExtension.handleAfterEachMethodExecutionException(extensionContext, thrownException));
        assertSame(thrownException, actualException);

        // Verify that the screenshot is saved in the right place.
        assertEquals("somecontent", readFileToString(expectedScreenshotFile, defaultCharset()));
        verify(vncRecordingContainer).saveRecordingToFile(expectedVideoFile);

        assertEquals(2, this.logCapture.size());
        assertEquals(
            String.format("Screenshot for test [getClass] saved at [%s].", expectedScreenshotFile.getAbsolutePath()),
            this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            String.format("(*) VNC recording of test has been saved to [%s]", expectedVideoFile.getAbsolutePath()),
            this.logCapture.getMessage(1));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(1).getLevel());
    }
}
