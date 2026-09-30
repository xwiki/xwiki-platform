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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check that the block handle (the drag handle / "+" button shown on hover) is vertically centered on the first line
 * of its block, regardless of the block's type, font size or content, since BlockNote itself only accounts for a
 * fixed, per-block-type pixel offset (see the comment in {@code c-blocknote-view.vue} next to
 * {@code SideMenuController.getBlockOffset()}) and doesn't know anything about the margin/padding the XWiki skin adds
 * on top of its own.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest(
    properties = {
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
    },
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-websocket",
        "org.xwiki.platform:xwiki-platform-extension-index",
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class BlockHandleAlignmentIT extends AbstractBlockNoteIT
{
    /**
     * A couple of pixels of tolerance for sub-pixel rendering / font metric rounding differences between browsers.
     */
    private static final double TOLERANCE_PX = 2;

    @Test
    void handleAlignment(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, """
            = Heading 1 =

            == Heading 2 ==

            === Heading 3 ===

            ==== Heading 4 ====

            ===== Heading 5 =====

            ====== Heading 6 ======

            A short paragraph.

            A much longer paragraph that is going to wrap on multiple lines so that we can check that the handle \
            stays aligned with the first line only, regardless of how many lines the block spans in total, since \
            the handle should never move down to follow the block's growing height.

            * a bullet item

            > a quote

            ----

            last paragraph""", "");

        new InplaceEditablePage().editInplace();
        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // One entry per top-level block, in document order, matching the content passed to createPage() above.
        String[] blockLabels = {
            "Heading 1", "Heading 2", "Heading 3", "Heading 4", "Heading 5", "Heading 6",
            "short paragraph", "long paragraph", "bullet item", "quote", "divider", "last paragraph"
        };

        for (int index = 0; index < blockLabels.length; index++) {
            WebElement blockContent = textArea.getBlockContent(index);
            WebElement handle = textArea.hoverAndGetBlockHandle(blockContent);
            assertHandleAlignedWithFirstLine(setup, blockLabels[index], blockContent, handle);
        }
    }

    /**
     * Asserts that the vertical center of the given handle matches the vertical center of the first line of the
     * given block content, within {@link #TOLERANCE_PX}. The first line's center is measured directly from a text
     * range (rather than recomputed from font metrics), so this reflects what actually got rendered, independently
     * of whatever formula the CSS uses.
     *
     * @param setup the test setup, used to access the browser driver
     * @param label a label identifying the block, used in the assertion failure message
     * @param blockContent the block's content element ({@code .bn-block-content})
     * @param handle the block's drag handle element
     */
    private void assertHandleAlignedWithFirstLine(TestUtils setup, String label, WebElement blockContent,
        WebElement handle)
    {
        String script = """
            const blockContent = arguments[0];
            const handle = arguments[1];

            const walker = document.createTreeWalker(blockContent, NodeFilter.SHOW_TEXT);
            let textNode = walker.nextNode();
            while (textNode && !textNode.textContent.trim()) {
                textNode = walker.nextNode();
            }

            let lineRect;
            if (textNode) {
                const range = document.createRange();
                range.setStart(textNode, 0);
                range.setEnd(textNode, 1);
                lineRect = range.getBoundingClientRect();
            }

            const lineCenter = (lineRect && lineRect.height > 0)
                ? lineRect.top + lineRect.height / 2
                : (() => {
                    // No text (e.g. a divider): fall back to the block's own visible content element (e.g. the
                    // <hr>), not the whole padded .bn-block-content wrapper, since that element's own box is what
                    // is actually visible and thus what "the first line" means for it.
                    const el = blockContent.firstElementChild || blockContent;
                    const r = el.getBoundingClientRect();
                    return r.top + r.height / 2;
                })();

            const handleRect = handle.getBoundingClientRect();
            const handleCenter = handleRect.top + handleRect.height / 2;

            const firstElementChild = blockContent.firstElementChild;
            const style = firstElementChild ? getComputedStyle(firstElementChild) : null;

            return {
                delta: handleCenter - lineCenter,
                tag: firstElementChild ? firstElementChild.tagName : null,
                marginTop: style ? style.marginTop : null,
                paddingTop: getComputedStyle(blockContent).paddingTop,
                fontSize: style ? style.fontSize : null,
                lineHeight: style ? style.lineHeight : null
            };
            """;
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setup.getDriver().executeScript(script, blockContent,
            handle);
        double delta = ((Number) result.get("delta")).doubleValue();
        assertTrue(Math.abs(delta) <= TOLERANCE_PX,
            "Expected the block handle to be vertically centered on the first line of the '" + label
                + "' block, but it is off by " + delta + "px (child=" + result.get("tag") + ", margin-top="
                + result.get("marginTop") + ", padding-top=" + result.get("paddingTop") + ", font-size="
                + result.get("fontSize") + ", line-height=" + result.get("lineHeight") + ")");
    }
}
