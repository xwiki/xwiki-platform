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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.EmojiPicker;
import org.xwiki.blocknote.test.po.SlashMenu;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the two ways of inserting an emoji (the quick action from the slash menu and typing a colon) and the
 * case where the typed text matches no emoji.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@UITest(
    properties = {
        // The Image Wizard needs this to be able to upload images.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",

        // The page, attachment and image suggestions execute a Solr query from XWiki.SuggestSolrService /
        // XWiki.SuggestSolrMacros, which requires programming right. These pages are authored by a user that has
        // programming right in a normal wiki, but the test wiki blocks programming right by default, so we need to
        // explicitly exclude them.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.SuggestSolr(Service|Macros)"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link and image quick actions.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class EmojiPickerIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void insertEmojiByTypingColon(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        // The colon has to be typed on its own, otherwise it goes in too fast to be seen as a trigger character. The
        // emoji picker then needs at least two more characters before it starts suggesting.
        textArea.click().sendKeys(":");
        textArea.sendKeys("smile");
        // Which emoji comes first depends on the emoji database, so we assert on whatever is suggested.
        String emoji = new EmojiPicker().getSelectedEmoji();
        textArea.sendKeys(Keys.ENTER);
        textArea.waitUntilTextContains(emoji);

        page.save();
        assertThat(page.editWiki().getContent(), containsString(emoji));
    }

    @Test
    @Order(2)
    void typeColonWithoutMatchingEmoji(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        // BlockNote closes the picker once the query grows more than 3 characters past the last one that matched, so
        // the query has to stay short: "xyz" matches no emoji yet is never long enough to close the picker.
        textArea.click().sendKeys(":");
        textArea.sendKeys("xyz");
        assertEquals("No emoji found", new EmojiPicker().getNoResultsMessage());
    }

    @Test
    @Order(3)
    void insertEmojiUsingSlashMenu(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        textArea.click().sendKeys("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("emoji");
        slashMenu.waitForItemSelected("Emoji");
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();

        // Unlike the colon, the quick action opens the picker on an empty query, listing all the emoji.
        String emoji = new EmojiPicker().getSelectedEmoji();
        textArea.sendKeys(Keys.ENTER);
        textArea.waitUntilTextContains(emoji);

        page.save();
        assertThat(page.editWiki().getContent(), containsString(emoji));
    }
}
