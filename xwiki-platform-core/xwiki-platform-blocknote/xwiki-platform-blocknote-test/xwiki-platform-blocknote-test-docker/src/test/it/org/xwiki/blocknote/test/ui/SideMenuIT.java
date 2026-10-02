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
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Verify that the block side menu is properly aligned on the block you hover, no matter the type of that block.
 * <p>
 * The alignment is checked by comparing a screenshot of the content area, taken while hovering each block, with a
 * reference screenshot committed in the test resources (see {@link ScreenshotComparator}).
 * <p>
 * When a change in the way the content is rendered makes this test fail, check the screenshots this test saves and, if
 * the side menu is still properly aligned, use them as the new reference screenshots.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest(
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-websocket"
    }
)
@ExtendWith(ScreenshotComparatorParameterResolver.class)
class SideMenuIT extends AbstractBlockNoteIT
{
    /**
     * The name of the image attached to the test page, taken from the test resources. It is tall enough to make it
     * obvious whether the side menu is centered on it.
     */
    private static final String IMAGE_NAME = "picture.png";

    // We split the blocks into groups so that no test page is taller than the viewport, because taking a
    // screenshot of a taller page scrolls it, which makes the editor hide the side menu.

    private static final String[] HEADINGS = {"heading1", "heading2", "heading3", "heading4", "heading5", "heading6"};

    private static final String HEADINGS_CONTENT = """
        = Heading 1 =

        == Heading 2 ==

        === Heading 3 ===

        ==== Heading 4 ====

        ===== Heading 5 =====

        ====== Heading 6 ======""";

    private static final String[] OTHER_BLOCKS = {"paragraph", "wrappingParagraph", "bulletItem", "quote", "divider"};

    private static final String OTHER_BLOCKS_CONTENT = """
        A short paragraph.

        A much longer paragraph that is going to wrap on multiple lines so that we can check that the side menu stays \
        aligned with the first line only, regardless of how many lines the block spans in total, since the side menu \
        should never move down to follow the block's growing height.

        * A bullet item

        > A quote

        ----""";

    private static final String IMAGE_CONTENT = "[[image:%s]]".formatted(IMAGE_NAME);

    // Blocks whose first line isn't text, so the side menu must not align on the text that comes after it.

    private static final String CAPTIONED_IMAGE_CONTENT =
        "[[A caption, which is the only text of the block>>image:%s||width=\"150\"]]".formatted(IMAGE_NAME);

    private static final String INFO_BOX_CONTENT =
        "{{info}}An info box, whose icon is rendered before its text.{{/info}}";

    private static final String IMAGE_BEFORE_TEXT_CONTENT = """
        {{html wiki="true"}}
        [[image:%s]]<br/>Some text after an image.
        {{/html}}""".formatted(IMAGE_NAME);

    @Test
    void sideMenuIsAlignedOnHeadings(TestUtils setup, TestReference testReference,
        ScreenshotComparator screenshots) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, HEADINGS_CONTENT);

        assertSideMenuIsAligned(setup, editInplace(), screenshots, HEADINGS);
    }

    @Test
    void sideMenuIsAlignedOnOtherBlockTypes(TestUtils setup, TestReference testReference,
        ScreenshotComparator screenshots) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, OTHER_BLOCKS_CONTENT);

        assertSideMenuIsAligned(setup, editInplace(), screenshots, OTHER_BLOCKS);
    }

    @Test
    void sideMenuIsAlignedOnImage(TestUtils setup, TestReference testReference, ScreenshotComparator screenshots)
        throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, IMAGE_CONTENT);
        setup.attachFile(testReference, IMAGE_NAME, getClass().getResourceAsStream('/' + IMAGE_NAME), false);

        BlockNoteRichTextArea textArea = editInplace();
        // An image that is still loading would make the screenshot unstable.
        textArea.waitUntilImageIsLoaded(0);
        assertSideMenuIsAligned(setup, textArea, screenshots, new String[] {"image"});
    }

    @Test
    void sideMenuIsAlignedOnCaptionedImage(TestUtils setup, TestReference testReference,
        ScreenshotComparator screenshots) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, CAPTIONED_IMAGE_CONTENT);
        setup.attachFile(testReference, IMAGE_NAME, getClass().getResourceAsStream('/' + IMAGE_NAME), false);

        BlockNoteRichTextArea textArea = editInplace();
        // An image that is still loading would make the screenshot unstable.
        textArea.waitUntilImagesAreLoaded();
        assertSideMenuIsAligned(setup, textArea, screenshots, new String[] {"captionedImage"});
    }

    @Test
    void sideMenuIsAlignedOnInfoBox(TestUtils setup, TestReference testReference, ScreenshotComparator screenshots)
        throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, INFO_BOX_CONTENT);

        BlockNoteRichTextArea textArea = editInplace();
        // A macro whose output is not rendered yet would make the screenshot unstable.
        textArea.waitUntilMacrosAreRendered();
        assertSideMenuIsAligned(setup, textArea, screenshots, new String[] {"infoBox"});
    }

    @Test
    void sideMenuIsAlignedOnImageBeforeText(TestUtils setup, TestReference testReference,
        ScreenshotComparator screenshots) throws Exception
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, IMAGE_BEFORE_TEXT_CONTENT);
        setup.attachFile(testReference, IMAGE_NAME, getClass().getResourceAsStream('/' + IMAGE_NAME), false);

        BlockNoteRichTextArea textArea = editInplace();
        // A macro whose output is not rendered yet, or an image that is still loading, would move the blocks around
        // and thus make the screenshot unstable.
        textArea.waitUntilMacrosAreRendered().waitUntilImagesAreLoaded();
        assertSideMenuIsAligned(setup, textArea, screenshots, new String[] {"imageBeforeText"});
    }

    /**
     * Edits the current page in-place and moves the focus to the page title.
     *
     * @return the rich text area, which is left unfocused so that its blinking caret doesn't make the screenshots
     *         unstable
     */
    private BlockNoteRichTextArea editInplace()
    {
        InplaceEditablePage page = new InplaceEditablePage().editInplace();
        BlockNoteRichTextArea textArea = new BlockNoteEditor("content").getRichTextArea();
        page.focusDocumentTitle();
        return textArea;
    }

    /**
     * Compares a screenshot of the content area, taken while hovering each block, with the reference screenshot of
     * that block.
     *
     * @param setup the test setup, used to scroll the page
     * @param textArea the rich text area holding the blocks
     * @param screenshots the comparator to check the screenshots with
     * @param blocks the names of the blocks, in the order they appear in the rich text area
     */
    private void assertSideMenuIsAligned(TestUtils setup, BlockNoteRichTextArea textArea,
        ScreenshotComparator screenshots, String[] blocks) throws IOException
    {
        WebElement content = new InplaceEditablePage().getContentContainer();
        // Taking a screenshot scrolls the page down to bring the content into view, which would leave the mouse over
        // another block and make the editor hide the side menu, so we scroll to the bottom before hovering.
        setup.getDriver().scrollTo(0, Integer.MAX_VALUE);
        for (int i = 0; i < blocks.length; i++) {
            textArea.hoverBlock(i);
            screenshots.assertScreenshotMatches(blocks[i], content);
        }
    }
}
