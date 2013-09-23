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
package org.xwiki.test.ui;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates debugging information on test failure:
 * <ul>
 * <li>captures a screenshot of the browser window</li>
 * <li>logs the URL of the current page</li>
 * <li>logs the source of the current page</li>
 * </ul>
 * NOTE: The reason we also log when a test starts and passes is simply to overcome a deficiency in error reporting in
 * Jenkins. The reason is that Jenkins bases its test reporting on the Maven Surefire plugin reporting which itself is
 * using a file to report test status. Since ui-tests are using a test suite, {@link PageObjectSuite}, there's only a
 * single file generated and it's only generated when all tests have finished executing. Thus if a test hangs there
 * won't be any file generated and looking at the Jenkins UI it won't be possible to see which tests have executed.
 * <p>
 * Normally each JUnit Test Runner knows what test is executing and when it's finished and thus can report them in its
 * own console (as this is the case for IDEs for example). Again the issue here is that Jenkins doesn't have any JUnit
 * Test Runner but instead is calling JUnit by delegation to the Maven Surefire plugin.
 * 
 * @version $Id$
 * @since 4.3
 */
public class TestDebugger extends TestWatcher
{
    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDebugger.class);

    /**
     * The folder where to save the screenshot.
     */
    private static final String SCREENSHOT_DIR = System.getProperty("screenshotDirectory");

    /**
     * The object used to get the debugging information on test failure.
     */
    private final WebDriver driver;

    /**
     * Creates a new test rule that generates debugging information on test failure.
     * 
     * @param driver the object used to get the debugging information on test failure
     */
    public TestDebugger(WebDriver driver)
    {
        this.driver = driver;
    }

    @Override
    protected void starting(Description description)
    {
        LOGGER.info("{} started", getTestName(description));
    }

    @Override
    protected void succeeded(Description description)
    {
        LOGGER.info("{} passed", getTestName(description));
    }

    @Override
    protected void failed(Throwable e, Description description)
    {
        LOGGER.info("{} failed", getTestName(description));
        takeScreenshot(description);
        LOGGER.info("Current page URL is [{}]", driver.getCurrentUrl());
        LOGGER.info("Current page source is [{}]", driver.getPageSource());
    }

    /**
     * @param description the test description
     * @return the test name (using the format TestSimpleClassName-TestMethodName)
     */
    private String getTestName(Description description)
    {
        return description.getTestClass().getSimpleName() + "-" + description.getMethodName();
    }

    /**
     * Captures a screenshot of the browser window.
     * 
     * @param description the description of the failing test
     */
    private void takeScreenshot(Description description)
    {
        takeScreenshot(getTestName(description));
    }

    /**
     * Captures a screenshot of the browser window.
     *
     * @param testName the name of the file in which the screenshot will be taken. A ".png" suffix will be appended
     */
    public void takeScreenshot(String testName)
    {
        if (!(driver instanceof TakesScreenshot)) {
            LOGGER.warn("The WebDriver that is currently used doesn't support taking screenshots.");
            return;
        }

        try {
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File screenshotFile;
            if (SCREENSHOT_DIR != null) {
                File screenshotDir = new File(SCREENSHOT_DIR);
                screenshotDir.mkdirs();
                screenshotFile = new File(screenshotDir, testName + ".png");
            } else {
                screenshotFile = new File(new File(System.getProperty("java.io.tmpdir")), testName + ".png");
            }
            FileUtils.copyFile(sourceFile, screenshotFile);
            LOGGER.info("Screenshot for failing test [{}] saved at [{}].", testName, screenshotFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to take screenshot for failing test [{}].", testName, e);
        }
    }
}
