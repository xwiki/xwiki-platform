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

import java.io.EOFException;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerLoggerFactory;
import org.xwiki.test.docker.junit5.TestConfiguration;

import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.core.command.PullImageResultCallback;

import ch.qos.logback.classic.Level;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

/**
 * Utility methods for setting up the test framework.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public final class DockerTestUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerTestUtils.class);

    private static final boolean IN_A_CONTAINER = new File("/.dockerenv").exists();

    private static final String DEFAULT = "default";

    private static final char DASH = '-';

    private static final Pattern REPETITION_PATTERN = Pattern.compile("\\[test-template-invocation:#(.*)\\]");

    private static List<String> pulledImages = new ArrayList<>();

    private DockerTestUtils()
    {
        // Prevents instantiation.
    }

    /**
     * Start following a docker container's logs from the moment this API is called.
     *
     * @param container the container for which to follow the logs
     * @param loggingClass the SLF4J logging class to use for logging
     */
    public static void followOutput(GenericContainer container, Class<?> loggingClass)
    {
        LogContainerCmd cmd = container.getDockerClient().logContainerCmd(container.getContainerId())
            .withFollowStream(true)
            .withTail(0);

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        Consumer<OutputFrame> consumer = new Slf4jLogConsumer(LoggerFactory.getLogger(loggingClass));
        callback.addConsumer(STDOUT, consumer);
        cmd.withStdOut(true);
        callback.addConsumer(STDERR, consumer);
        cmd.withStdErr(true);

        cmd.exec(callback);
    }

    /**
     * Captures a screenshot of the browser window.
     *
     * @param extensionContext the test context from which to extract the failing test name
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param driver the Selenium Web Driver instance to use to take the screenshot
     */
    public static void takeScreenshot(ExtensionContext extensionContext, TestConfiguration testConfiguration,
        WebDriver driver)
    {
        if (!(driver instanceof TakesScreenshot)) {
            LOGGER.warn("The WebDriver that is currently used doesn't support taking screenshots.");
            return;
        }

        String testName = extensionContext.getTestMethod().get().getName();

        try {
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File screenshotFile = DockerTestUtils.getResultFileLocation("png", testConfiguration, extensionContext);
            screenshotFile.getParentFile().mkdirs();
            FileUtils.copyFile(sourceFile, screenshotFile);
            LOGGER.info("Screenshot for test [{}] saved at [{}].", testName, screenshotFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to take screenshot for test [{}].", testName, e);
        }
    }

    /**
     * @param container the container to start
     * @param testConfiguration the configuration to build (database, debug mode, etc). Used to verify if we're online
     *        to pull the image
     * @throws Exception if the container fails to start
     */
    public static void startContainer(GenericContainer container, TestConfiguration testConfiguration) throws Exception
    {
        // Get the latest image in case the tag has been updated on dockerhub.
        String dockerImageName = container.getDockerImageName();

        // Don't pull if:
        // - we're offline
        // - we've already pulled this image in this test run
        // - the container corresponds to an image that's been dynamically built by the test and thus that doesn't
        //   exists on dockerhub.
        if (!testConfiguration.isOffline() && !pulledImages.contains(dockerImageName)
            && !(container instanceof XWikiLocalGenericContainer))
        {
            LOGGER.info("Pulling image [{}]", dockerImageName);
            PullImageCmd command = container.getDockerClient().pullImageCmd(dockerImageName);
            PullImageResultCallback response = new PullImageResultCallback();
            response = command.exec(response);
            response.awaitCompletion();
            pulledImages.add(dockerImageName);
        }

        // In debug mode, get container debug logs to try to diagnose container failing errors
        if (testConfiguration.isDebug()) {
            setLogbackLoggerLevel(DockerLoggerFactory.getLogger(container.getDockerImageName()).getName(), Level.DEBUG);
        }

        // Try to work around the following issue: https://github.com/testcontainers/testcontainers-java/issues/2208
        try {
            container.start();
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof EOFException) {
                // Retry once after waiting 5 seconds to increase the odds ;)
                LOGGER.info("Error starting docker container [{}]. Retrying once", container.getDockerImageName(), e);
                Thread.sleep(5000L);
                container.start();
            } else {
                throw e;
            }
        }
    }

    /**
     * @param loggingContextName the logger's context (i.e. name) for which to set the level
     * @param loggingLevel the level to set
     */
    public static void setLogbackLoggerLevel(String loggingContextName, Level loggingLevel)
    {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggingContextName);
        logger.setLevel(loggingLevel);
    }

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @return the directory where screenshots and video should be saved
     */
    public static File getScreenshotsDirectory(TestConfiguration testConfiguration)
    {
        File directory = new File(String.format("%s/screenshots", testConfiguration.getOutputDirectory()));
        directory.mkdirs();
        return directory;
    }

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @return the string representation identifying the current configuration
     */
    public static String getTestConfigurationName(TestConfiguration testConfiguration)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(testConfiguration.getDatabase().name());
        builder.append(DASH);
        builder.append(testConfiguration.getDatabaseTag() != null ? testConfiguration.getDatabaseTag() : DEFAULT);
        builder.append(DASH);
        builder.append(testConfiguration.getJDBCDriverVersion() != null
            ? testConfiguration.getJDBCDriverVersion() : DEFAULT);
        builder.append(DASH);
        builder.append(testConfiguration.getServletEngine().name());
        builder.append(DASH);
        builder.append(testConfiguration.getServletEngineTag() != null
            ? testConfiguration.getServletEngineTag() : DEFAULT);
        builder.append(DASH);
        builder.append(testConfiguration.getBrowser().name());
        return builder.toString().toLowerCase();
    }

    /**
     * @param fileSuffix the suffix of the file to create
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     * @param extensionContext the test execution context from which to extract the test class and test name used to
     *        compute the name of the new file
     * @return the file object in which to save a result
     */
    public static File getResultFileLocation(String fileSuffix,
        TestConfiguration testConfiguration, ExtensionContext extensionContext)
    {
        // Note: There's currently a limitation in Jenkins when archiving artifacts: they are copied without caring
        // about their locations. And since we run the same test several times but with different configurations,
        // the test name is not enough to uniquely point to a given test in a given configuration. Thus we also
        // need to save the configuration name, even though the directory in which we're saving the screenshot
        // is already named after the executing configuration name.
        File newDir = DockerTestUtils.getScreenshotsDirectory(testConfiguration);
        // Tests could be repeated and if they are we need to compute a file name that takes into account the repetition
        // There seems to be no easy way to get them right now, see https://github.com/junit-team/junit5/issues/1884
        // Thus we currently extract the repetition index from the display name which is like:
        //     ...[test-template-invocation:#2]
        String repetitionIndex = getRepetitionIndex(extensionContext);
        File newFile = new File(newDir, String.format("%s-%s-%s%s.%s",
            DockerTestUtils.getTestConfigurationName(testConfiguration),
            extensionContext.getRequiredTestClass().getName(), extensionContext.getRequiredTestMethod().getName(),
            repetitionIndex, fileSuffix));
        return newFile;
    }

    /**
     * @return true if the test framework is running inside a Docker container (DOOD pattern)
     * @since 11.3RC1
     */
    public static boolean isInAContainer()
    {
        return IN_A_CONTAINER;
    }

    /**
     * @return true if the test framework is executing locally on developer's machines
     * @since 11.3RC1
     */
    public static boolean isLocal()
    {
        return !DockerTestUtils.isInAContainer();
    }

    private static String getRepetitionIndex(ExtensionContext extensionContext)
    {
        String repetitionIndex;
        Matcher matcher = REPETITION_PATTERN.matcher(extensionContext.getUniqueId());
        if (matcher.find()) {
            repetitionIndex = matcher.group(1);
        } else {
            repetitionIndex = "";
        }
        return repetitionIndex;
    }

    /**
     * @return the current host name
     * @since 11.9RC1
     */
    public static String getHostName()
    {
        String hostname;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (Exception e) {
            LOGGER.warn("Failed to get hostname", e);
            hostname = "Unknown";
        }
        return hostname;
    }

}
