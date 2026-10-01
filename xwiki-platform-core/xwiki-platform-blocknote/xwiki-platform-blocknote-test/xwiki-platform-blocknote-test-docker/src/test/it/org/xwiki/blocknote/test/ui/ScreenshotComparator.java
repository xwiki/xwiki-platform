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
package org.xwiki.blocknote.test.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compares screenshots of page elements with reference screenshots committed in the test resources, under
 * {@code screenshots/<TestClassName>/<browser>/}, since each browser renders the page slightly differently (e.g. the
 * text anti-aliasing). The screenshots are saved in the {@code screenshots} folder of the build
 * directory, along with an image highlighting the differences for each screenshot that doesn't match its reference.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ScreenshotComparator
{
    /**
     * How much the color of a pixel is allowed to differ before that pixel is counted as different.
     * This absorbs the small differences in the way text and icons are anti-aliased.
     */
    private static final double PIXEL_TOLERANCE_LEVEL = 0.1;

    private final String testClassName;

    private final String browser;

    private final File outputFolder;

    /**
     * @param testConfiguration the test configuration, used to find the build directory and the browser the
     *         screenshots are taken with
     * @param testReference the test reference, used to find the reference screenshots and to name the saved screenshots
     */
    public ScreenshotComparator(TestConfiguration testConfiguration, TestReference testReference)
    {
        this.testClassName = testReference.getLastSpaceReference().getParent().getName();
        this.browser = testConfiguration.getBrowser().name().toLowerCase(Locale.ROOT);
        this.outputFolder = new File(testConfiguration.getMavenBuildDirectory(), "screenshots");
    }

    /**
     * Takes a screenshot of the specified element and fails if it doesn't match the reference screenshot with the
     * specified name.
     * @param name the name of the reference screenshot, without the extension
     * @param element the element to take the screenshot of
     * @throws IOException if the screenshot can't be taken or the reference screenshot can't be read
     */
    public void assertScreenshotMatches(String name, WebElement element) throws IOException
    {
        // The test class name and the browser are part of the file names because the screenshots folder is shared by
        // all the tests.
        String prefix = "%s-%s-%s".formatted(this.testClassName, this.browser, name);
        File actualFile = new File(this.outputFolder, prefix + ".png");
        BufferedImage actual = takeScreenshot(element);
        ImageComparisonUtil.saveImage(actualFile, actual);

        String referencePath = "src/test/resources/" + getReferencePath(name);
        BufferedImage reference = readReference(name);
        assertNotNull(reference, () -> ("There is no reference screenshot for [%s]. Check the screenshot taken by the "
            + "test, at [%s], and copy it to [%s] if it is correct.").formatted(name, actualFile, referencePath));

        File differenceFile = new File(this.outputFolder, prefix + "-diff.png");
        ImageComparisonResult result = new ImageComparison(reference, actual, differenceFile)
            .setPixelToleranceLevel(PIXEL_TOLERANCE_LEVEL).compareImages();
        assertEquals(ImageComparisonState.MATCH, result.getImageComparisonState(),
            () -> ("The screenshot [%s] doesn't match its reference (%s%% of the pixels are different). Compare the "
                + "screenshot taken by the test, at [%s], with the reference screenshot, at [%s]. The differences are "
                + "highlighted at [%s].").formatted(name, result.getDifferencePercent(), actualFile, referencePath,
                    differenceFile));
    }

    private BufferedImage takeScreenshot(WebElement element) throws IOException
    {
        return ImageIO.read(new ByteArrayInputStream(element.getScreenshotAs(OutputType.BYTES)));
    }

    private String getReferencePath(String name)
    {
        return "screenshots/%s/%s/%s.png".formatted(this.testClassName, this.browser, name);
    }

    private BufferedImage readReference(String name) throws IOException
    {
        try (InputStream reference = ScreenshotComparator.class.getResourceAsStream('/' + getReferencePath(name))) {
            return reference == null ? null : ImageIO.read(reference);
        }
    }
}
