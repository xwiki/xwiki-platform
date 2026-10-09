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

import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.ui.XWikiWebDriver;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compares screenshots of page elements with reference screenshots committed in the test resources, under
 * {@code screenshots/<TestClassName>/<testMethodName>/<browser>/}, since each browser renders the page slightly
 * differently (e.g. the text anti-aliasing). The screenshots are saved in the {@code screenshots} folder of the build
 * directory, along with an image highlighting the differences for each screenshot that doesn't match its reference.
 * <p>
 * A single set of reference screenshots is maintained, taken with {@link Browser#FIREFOX}, so a test that compares
 * screenshots is skipped when it runs with another browser.
 * <p>
 * The screenshots are taken by cropping a screenshot of the page, rather than by screenshotting the element itself,
 * so that they can include the floating user interface an element shows outside of its own bounds, such as a menu.
 * <p>
 * Run the tests with {@code -Dxwiki.test.screenshots.update=true} to overwrite the reference screenshots with the
 * ones taken by the tests, e.g. after a change that is expected to modify them.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ScreenshotComparator
{
    /**
     * The only browser the reference screenshots are taken with. Its Docker image supports all the architectures we
     * run the tests on, so that the same references can be used on all of them.
     */
    private static final Browser REFERENCE_BROWSER = Browser.FIREFOX;

    /**
     * How much the color of a pixel is allowed to differ before that pixel is counted as different.
     * This absorbs the small differences in the way text and icons are anti-aliased.
     */
    private static final double PIXEL_TOLERANCE_LEVEL = 0.1;

    private static final Insets NO_MARGIN = new Insets(0, 0, 0, 0);

    /**
     * When this system property is set, the reference screenshots are overwritten with the screenshots taken by the
     * tests, instead of being compared with them.
     */
    private static final boolean UPDATE_REFERENCES = Boolean.getBoolean("xwiki.test.screenshots.update");

    /**
     * Reads the scroll offset of the page, its viewport height and its pixel density, in one round trip.
     */
    private static final String PAGE_METRICS =
        "return [window.scrollX, window.scrollY, window.innerHeight, window.devicePixelRatio];";

    private final XWikiWebDriver driver;

    private final String testClassName;

    private final String testMethodName;

    private final Browser browser;

    private final File outputFolder;

    private final File referenceFolder;

    /**
     * @param driver the web driver used to take the screenshots
     * @param testConfiguration the test configuration, used to find the build directory and the browser the
     *         screenshots are taken with
     * @param testClassName the name of the class declaring the test, used to find the reference screenshots
     * @param testMethodName the name of the test method, used to find the reference screenshots
     */
    public ScreenshotComparator(XWikiWebDriver driver, TestConfiguration testConfiguration, String testClassName,
        String testMethodName)
    {
        this.driver = driver;
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.browser = testConfiguration.getBrowser();
        this.outputFolder = new File(testConfiguration.getMavenBuildDirectory(), "screenshots");
        this.referenceFolder = new File(testConfiguration.getMavenBuildDirectory(), "../src/test/resources");
    }

    /**
     * Takes a screenshot of the specified element and fails if it doesn't match the reference screenshot with the
     * specified name.
     *
     * @param name the name of the reference screenshot, without the extension
     * @param element the element to take the screenshot of
     * @throws IOException if the screenshot can't be taken or the reference screenshot can't be read
     */
    public void assertMatches(String name, WebElement element) throws IOException
    {
        assertMatches(name, element, NO_MARGIN);
    }

    /**
     * Takes a screenshot of the specified element, extended by the specified margin, and fails if it doesn't match the
     * reference screenshot with the specified name.
     *
     * @param name the name of the reference screenshot, without the extension
     * @param element the element to take the screenshot of
     * @param margin how much of the page to capture around the element, e.g. to include a menu floating next to it
     * @throws IOException if the screenshot can't be taken or the reference screenshot can't be read
     */
    public void assertMatches(String name, WebElement element, Insets margin) throws IOException
    {
        assumeTrue(this.browser == REFERENCE_BROWSER, () -> ("The reference screenshots are only maintained for "
            + "[%s] and the tests run with [%s].").formatted(REFERENCE_BROWSER, this.browser));

        // The test name and the browser are part of the file names because the screenshots folder is shared by all
        // the tests.
        String prefix = "%s-%s-%s-%s".formatted(this.testClassName, this.testMethodName,
            this.browser.name().toLowerCase(Locale.ROOT), name);
        File actualFile = new File(this.outputFolder, prefix + ".png");
        BufferedImage actual = takeScreenshot(element, margin);
        ImageComparisonUtil.saveImage(actualFile, actual);

        File referenceFile = new File(this.referenceFolder, getReferencePath(name));
        if (UPDATE_REFERENCES) {
            ImageComparisonUtil.saveImage(referenceFile, actual);
            return;
        }

        BufferedImage reference = readReference(name);
        assertNotNull(reference, () -> ("There is no reference screenshot for [%s]. Check the screenshot taken by the "
            + "test, at [%s], and copy it to [%s] if it is correct.").formatted(name, actualFile, referenceFile));

        File differenceFile = new File(this.outputFolder, prefix + "-diff.png");
        ImageComparisonResult result = new ImageComparison(reference, actual, differenceFile)
            .setPixelToleranceLevel(PIXEL_TOLERANCE_LEVEL).compareImages();
        assertEquals(ImageComparisonState.MATCH, result.getImageComparisonState(),
            () -> ("The screenshot [%s] doesn't match its reference (%s%% of the pixels are different). Compare the "
                + "screenshot taken by the test, at [%s], with the reference screenshot, at [%s]. The differences are "
                + "highlighted at [%s].").formatted(name, result.getDifferencePercent(), actualFile, referenceFile,
                    differenceFile));
    }

    private BufferedImage takeScreenshot(WebElement element, Insets margin) throws IOException
    {
        BufferedImage page = ImageIO.read(new ByteArrayInputStream(this.driver.getScreenshotAs(OutputType.BYTES)));
        List<Number> metrics = (List<Number>) this.driver.executeJavascript(PAGE_METRICS);
        double scale = metrics.get(3).doubleValue();
        // Some browsers screenshot the whole page while others screenshot only its visible part, in which case the
        // element coordinates, which are relative to the page, have to be shifted by the scroll offset.
        boolean wholePage = page.getHeight() > metrics.get(2).doubleValue() * scale;
        double offsetX = wholePage ? 0 : metrics.get(0).doubleValue();
        double offsetY = wholePage ? 0 : metrics.get(1).doubleValue();

        Rectangle rect = element.getRect();
        // Keep the crop inside the screenshot, since the margin can go past the edge of the page.
        int left = clamp((rect.getX() - offsetX - margin.left) * scale, page.getWidth());
        int top = clamp((rect.getY() - offsetY - margin.top) * scale, page.getHeight());
        int right = clamp((rect.getX() + rect.getWidth() - offsetX + margin.right) * scale, page.getWidth());
        int bottom = clamp((rect.getY() + rect.getHeight() - offsetY + margin.bottom) * scale, page.getHeight());
        assertTrue(right > left && bottom > top, () -> ("The element to screenshot is outside the %sx%s screenshot "
            + "of the page, at %s.").formatted(page.getWidth(), page.getHeight(), rect));

        return page.getSubimage(left, top, right - left, bottom - top);
    }

    private int clamp(double value, int max)
    {
        return (int) Math.min(Math.max(Math.round(value), 0), max);
    }

    private String getReferencePath(String name)
    {
        return "screenshots/%s/%s/%s/%s.png".formatted(this.testClassName, this.testMethodName,
            this.browser.name().toLowerCase(Locale.ROOT), name);
    }

    private BufferedImage readReference(String name) throws IOException
    {
        try (InputStream reference = ScreenshotComparator.class.getResourceAsStream('/' + getReferencePath(name))) {
            return reference == null ? null : ImageIO.read(reference);
        }
    }
}
