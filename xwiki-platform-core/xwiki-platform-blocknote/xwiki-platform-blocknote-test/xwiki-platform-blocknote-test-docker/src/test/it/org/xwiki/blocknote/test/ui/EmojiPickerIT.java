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

/**
 * Verify the two ways of inserting an emoji: the quick action from the slash menu and typing a colon.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@UITest(
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-websocket"
    }
)
class EmojiPickerIT extends AbstractBlockNoteIT
{
    @Test
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
