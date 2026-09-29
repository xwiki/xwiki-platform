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

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Verify that the block side menu is properly aligned on the block you hover, no matter the type of that block.
 * <p>
 * The alignment is checked by comparing a screenshot of the content area, taken while hovering each block, with a
 * reference screenshot committed in the test resources (see {@link ScreenshotComparator}). The side menu is displayed
 * on the left of the rich text area, flush against the left edge of the content area, which is why the screenshot is
 * taken on the whole content area. When a change in the way the content is rendered makes this test fail, check the
 * screenshots this test saves and, if the side menu is still properly aligned, use them as the new reference
 * screenshots.
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

    // The blocks are split between two tests, i.e. two pages, so that each page fits in the browser window: taking the
    // screenshot of an element scrolls the page (when it can be scrolled) and BlockNote hides the side menu when the
    // page is scrolled.

    /**
     * The headings the side menu is checked on, in the order they appear in {@link #HEADINGS_CONTENT}. The name of each
     * block is also the name of its reference screenshot.
     */
    private static final String[] HEADINGS = {"heading1", "heading2", "heading3", "heading4", "heading5", "heading6"};

    private static final String HEADINGS_CONTENT = """
        = Heading 1 =

        == Heading 2 ==

        === Heading 3 ===

        ==== Heading 4 ====

        ===== Heading 5 =====

        ====== Heading 6 ======""";

    /**
     * The other blocks the side menu is checked on, in the order they appear in {@link #OTHER_BLOCKS_CONTENT}. The
     * name of each block is also the name of its reference screenshot.
     */
    private static final String[] OTHER_BLOCKS = {"paragraph", "wrappingParagraph", "bulletItem", "quote", "divider",
        "image"};

    private static final String OTHER_BLOCKS_CONTENT = """
        A short paragraph.

        A much longer paragraph that is going to wrap on multiple lines so that we can check that the side menu stays \
        aligned with the first line only, regardless of how many lines the block spans in total, since the side menu \
        should never move down to follow the block's growing height.

        * A bullet item

        > A quote

        ----

        [[image:%s]]""".formatted(IMAGE_NAME);

    @Test
    void sideMenuIsAlignedOnHeadings(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, HEADINGS_CONTENT);

        assertSideMenuIsAligned(editInplace(), setup, testConfiguration, HEADINGS);
    }

    @Test
    void sideMenuIsAlignedOnOtherBlocks(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.attachFile(testReference, IMAGE_NAME, getClass().getResourceAsStream('/' + IMAGE_NAME), false);
        setup.createPage(testReference, OTHER_BLOCKS_CONTENT);

        BlockNoteRichTextArea textArea = editInplace();
        // An image that is still loading would make the screenshots unstable.
        textArea.waitUntilImageIsLoaded(0);
        assertSideMenuIsAligned(textArea, setup, testConfiguration, OTHER_BLOCKS);
    }

    /**
     * Edits the current page in-place.
     *
     * @return the rich text area, with its caret hidden since its blinking would make the screenshots unstable
     */
    private BlockNoteRichTextArea editInplace()
    {
        new InplaceEditablePage().editInplace();
        return new BlockNoteEditor("content").getRichTextArea().hideCaret();
    }

    /**
     * Compares a screenshot of the content area, taken while hovering each block, with the reference screenshot of
     * that block.
     *
     * @param textArea the rich text area holding the blocks
     * @param blocks the names of the blocks, in the order they appear in the rich text area
     */
    private void assertSideMenuIsAligned(BlockNoteRichTextArea textArea, TestUtils setup,
        TestConfiguration testConfiguration, String[] blocks) throws IOException
    {
        WebElement content = setup.getDriver().findElement(By.id("xwikicontent"));
        ScreenshotComparator screenshots = new ScreenshotComparator(testConfiguration, getClass());
        for (int i = 0; i < blocks.length; i++) {
            textArea.hoverBlock(i);
            screenshots.compare(blocks[i], content);
        }
        screenshots.assertAllMatch();
    }
}
