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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the block side menu is properly aligned on the block you hover, no matter the type of that block.
 * <p>
 * The alignment is checked by comparing a screenshot of the content area, taken while hovering a block, with a
 * reference screenshot committed in the test resources. When a change in the way the content is rendered makes this
 * test fail, check the screenshots this test saves in {@code target/screenshots} and, if the side menu is still
 * properly aligned, use them as the new reference screenshots.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest(
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class SideMenuAlignmentIT extends AbstractBlockNoteIT
{
    /**
     * The name of the image attached to the test page, taken from the test resources.
     */
    private static final String IMAGE_NAME = "image.gif";

    /**
     * The blocks the side menu is checked on, in the order they appear in the test content below. The name of each
     * block is also the name of its reference screenshot.
     */
    private static final String[] BLOCKS = {"heading1", "heading2", "heading3", "quote", "image"};

    private static final String CONTENT = """
        = Heading 1 =

        == Heading 2 ==

        === Heading 3 ===

        > Quote

        [[image:%s]]""".formatted(IMAGE_NAME);

    /**
     * Where the reference screenshots are looked up, relative to the test resources folder.
     */
    private static final String REFERENCE_FOLDER = "/screenshots/";

    /**
     * Where the screenshots taken by this test are saved, so that they can be reviewed when the test fails, and used
     * as reference screenshots when there is none yet.
     */
    private static final File OUTPUT_FOLDER = new File("target/screenshots");

    /**
     * How much the color of a pixel is allowed to differ, on each channel, before that pixel is counted as different.
     * This absorbs the small differences in the way text and icons are anti-aliased.
     */
    private static final int COLOR_TOLERANCE = 32;

    /**
     * How many pixels are allowed to differ. The side menu is a small (9 by 13 pixels) light gray icon, so moving it
     * from one block to another changes less than 60 pixels out of the 300 thousand pixels the content area has. This
     * is why the number of pixels allowed to differ is an absolute value rather than a ratio of the image size, and
     * why it is zero: the screenshots taken by two consecutive runs are identical, so there is no rendering noise to
     * leave room for, and any tolerance here would be in the range of the difference we want to catch.
     */
    private static final long MAX_DIFFERENT_PIXELS = 0;

    @Test
    void sideMenuIsAlignedOnTheHoveredBlock(TestUtils setup, TestReference testReference) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.attachFile(testReference, IMAGE_NAME, getClass().getResourceAsStream('/' + IMAGE_NAME), false);
        setup.createPage(testReference, CONTENT);

        new InplaceEditablePage().editInplace();

        BlockNoteRichTextArea textArea = new BlockNoteEditor("content").getRichTextArea();
        // Both the blinking caret and an image that is still loading would make the screenshots unstable.
        textArea.hideCaret().waitUntilImageIsLoaded(0);

        // Take all the screenshots before asserting anything, so that a failed assertion doesn't prevent the
        // remaining screenshots from being taken (they are needed to review the failure, and to create the reference
        // screenshots the first time this test is run).
        for (int i = 0; i < BLOCKS.length; i++) {
            FileUtils.writeByteArrayToFile(getScreenshotFile(BLOCKS[i]),
                textArea.hoverBlock(i).getContentScreenshot());
        }

        for (String block : BLOCKS) {
            assertScreenshotMatchesReference(block);
        }
    }

    /**
     * @param name the name of the block the screenshot was taken for
     * @return the file where the screenshot taken by this test is saved
     */
    private File getScreenshotFile(String name)
    {
        return new File(OUTPUT_FOLDER, name + ".png");
    }

    /**
     * Asserts that the screenshot taken for the specified block matches its reference screenshot.
     *
     * @param name the name of the block, which is also the name of its reference screenshot
     */
    private void assertScreenshotMatchesReference(String name) throws IOException
    {
        File screenshotFile = getScreenshotFile(name);
        BufferedImage actual = ImageIO.read(screenshotFile);
        BufferedImage reference = readReference(name);

        assertNotNull(reference, ("There is no reference screenshot for the [%s] block. Check the screenshot taken by "
            + "this test, at [%s], and copy it to [src/test/resources%s] if the side menu is properly aligned on that "
            + "block.").formatted(name, screenshotFile, REFERENCE_FOLDER));

        String failureMessage = ("The side menu is not aligned on the [%s] block as expected. Compare the screenshot "
            + "taken by this test, at [%s], with the reference screenshot, at [src/test/resources%s%s.png].")
                .formatted(name, screenshotFile, REFERENCE_FOLDER, name);
        assertTrue(reference.getWidth() == actual.getWidth() && reference.getHeight() == actual.getHeight(),
            failureMessage);

        long differentPixels = countDifferentPixels(reference, actual);
        assertTrue(differentPixels <= MAX_DIFFERENT_PIXELS,
            "%s %s pixels are different.".formatted(failureMessage, differentPixels));
    }

    /**
     * @param name the name of the reference screenshot to read
     * @return the reference screenshot, or {@code null} if there is no reference screenshot with the given name
     */
    private BufferedImage readReference(String name) throws IOException
    {
        try (InputStream reference = getClass().getResourceAsStream(REFERENCE_FOLDER + name + ".png")) {
            return reference == null ? null : ImageIO.read(reference);
        }
    }

    /**
     * @param reference the reference screenshot
     * @param actual the screenshot taken by the test, which has the same size as the reference screenshot
     * @return the number of pixels whose color differs by more than {@link #COLOR_TOLERANCE}
     */
    private long countDifferentPixels(BufferedImage reference, BufferedImage actual)
    {
        long differentPixels = 0;
        for (int x = 0; x < reference.getWidth(); x++) {
            for (int y = 0; y < reference.getHeight(); y++) {
                if (!isSameColor(reference.getRGB(x, y), actual.getRGB(x, y))) {
                    differentPixels++;
                }
            }
        }
        return differentPixels;
    }

    private boolean isSameColor(int reference, int actual)
    {
        // Compare the blue, green and red channels, ignoring the alpha channel (screenshots are opaque).
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs(((reference >> shift) & 0xFF) - ((actual >> shift) & 0xFF)) > COLOR_TOLERANCE) {
                return false;
            }
        }
        return true;
    }
}
