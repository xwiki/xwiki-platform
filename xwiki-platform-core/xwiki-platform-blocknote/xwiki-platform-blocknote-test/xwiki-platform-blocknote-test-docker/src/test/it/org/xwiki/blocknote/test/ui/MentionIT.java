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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.SlashMenu;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.SubWikiTestUtils;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.user.UserScope;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Verify the user mentions of the BlockNote editor integration.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
class MentionIT extends AbstractBlockNoteIT
{
    private static final String SUBWIKI = "subwiki";

    private static final String GLOBAL_USER = "John";

    private static final String LOCAL_USER = "Johnny";

    /**
     * Matches both users, so that each user scope is told apart by the suggestions it leaves out.
     */
    private static final String QUERY = "joh";

    /**
     * The title of the slash menu item opening the mention menu, as the editor translations spell it.
     */
    private static final String MENTION_ITEM = "Mention";

    /**
     * The text the mention is inserted after, so that it isn't alone in its paragraph.
     */
    private static final String TEXT_BEFORE = "Hello ";

    /**
     * The text typed right after the mention is inserted, without any character that the expected pattern would have
     * to escape.
     */
    private static final String TEXT_AFTER = "and you";

    @BeforeAll
    void createSubWiki(TestUtils setup, SubWikiTestUtils subWikiSetup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // The editor and the mention macro are only visible in the wiki holding them.
        subWikiSetup.createWiki(SUBWIKI, "org.xwiki.platform:xwiki-platform-blocknote-api",
            "org.xwiki.platform:xwiki-platform-mentions-ui", "org.xwiki.platform:xwiki-platform-edit-ui");
        DocumentReference preferences = new DocumentReference(SUBWIKI, "XWiki", "XWikiPreferences");
        setup.addObject(preferences, "XWiki.EditorBindingClass", "dataType",
            "org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "roleHint", "blocknote");
        setup.updateObject(preferences, "XWiki.XWikiPreferences", 0, "editor", "Wysiwyg");
        // Matches the same query as the global user, but sorts after it.
        setup.rest().addObject(new DocumentReference(SUBWIKI, "XWiki", LOCAL_USER), "XWiki.XWikiUsers", "active", "1",
            "first_name", LOCAL_USER);

        backToMainWiki(setup);
    }

    @Test
    @Order(1)
    void insertMention(TestUtils setup, TestReference testReference)
    {
        BlockNoteInplaceEditPage page = editFreshPage(setup, testReference);
        SlashMenu mentionMenu = startMention(page);
        mentionMenu.waitForItemTitles(List.of(GLOBAL_USER));

        assertMentionSaved(page, mentionMenu, GLOBAL_USER, "XWiki\\.John", "XWiki-John");
    }

    static Stream<Arguments> suggestUsers()
    {
        return Stream.of(Arguments.of(UserScope.LOCAL_ONLY, List.of(LOCAL_USER)),
            Arguments.of(UserScope.GLOBAL_ONLY, List.of(GLOBAL_USER)),
            // Merged and sorted by name, so that the suggestions don't look split in two lists.
            Arguments.of(UserScope.LOCAL_AND_GLOBAL, List.of(GLOBAL_USER, LOCAL_USER)));
    }

    @ParameterizedTest
    @MethodSource
    @Order(2)
    void suggestUsers(UserScope userScope, List<String> expectedUsers, TestUtils setup,
        SubWikiTestUtils subWikiSetup, TestLocalReference testReference)
    {
        BlockNoteInplaceEditPage page = editSubWikiPage(setup, subWikiSetup, testReference, userScope);

        startMention(page).waitForItemTitles(expectedUsers);
    }

    @Test
    @Order(3)
    void mentionGlobalUser(TestUtils setup, SubWikiTestUtils subWikiSetup, TestLocalReference testReference)
    {
        BlockNoteInplaceEditPage page =
            editSubWikiPage(setup, subWikiSetup, testReference, UserScope.LOCAL_AND_GLOBAL);
        SlashMenu mentionMenu = startMention(page);
        mentionMenu.waitForItemTitles(List.of(GLOBAL_USER, LOCAL_USER));

        // A global user doesn't live in the subwiki, so it is mentioned by a reference qualified with its wiki.
        assertMentionSaved(page, mentionMenu, GLOBAL_USER, "xwiki:XWiki\\.John", "xwiki-XWiki-John");
    }

    @Test
    @Order(4)
    void keepTypingAfterMention(TestUtils setup, TestReference testReference)
    {
        BlockNoteInplaceEditPage page = editFreshPage(setup, testReference);
        submitMention(page, startMention(page), GLOBAL_USER);

        // A mention in the middle of a sentence has to read like one, so the caret is left after the space the
        // mention is followed by and the sentence goes on in the same paragraph.
        page.getContentEditor().getRichTextArea().sendKeys(TEXT_AFTER);
        page.save();

        assertThat(page.editWiki().getContent(),
            matchesPattern(TEXT_BEFORE + mentionPattern("XWiki\\.John", "XWiki-John") + " " + TEXT_AFTER));
    }

    @Test
    @Order(5)
    void mentionBeforeAnExistingSpace(TestUtils setup, TestReference testReference)
    {
        BlockNoteInplaceEditPage page = editFreshPage(setup, testReference);
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea().click();
        // Put the caret before a space the sentence already has, which is the case the mention doesn't add one in.
        textArea.sendKeys(TEXT_BEFORE + " ");
        textArea.sendKeys(Keys.ARROW_LEFT);
        submitMention(page, openMentionMenu(textArea), GLOBAL_USER);
        page.save();

        // One space, the mention having left the one that was already there alone rather than adding a second one.
        assertThat(page.editWiki().getContent(),
            matchesPattern(TEXT_BEFORE + mentionPattern("XWiki\\.John", "XWiki-John") + " "));
    }

    @Test
    @Order(6)
    void keepLoneMentionInline(TestUtils setup, TestReference testReference)
    {
        BlockNoteInplaceEditPage page = editFreshPage(setup, testReference);
        // A mention alone in its paragraph is the case the trailing space is there for.
        submitMention(page, startMention(page, ""), GLOBAL_USER);
        page.save();

        assertThat(page.editWiki().getContent(),
            matchesPattern(mentionPattern("XWiki\\.John", "XWiki-John") + " "));

        // Load the saved content back and keep typing at the end of the line the mention is on. Without the space the
        // call would be read as a block macro, which puts the typed text in a paragraph of its own.
        BlockNoteInplaceEditPage reloaded =
            BlockNoteInplaceEditPage.gotoPage(testReference).editInplace();
        BlockNoteRichTextArea textArea = reloaded.getContentEditor().getRichTextArea();
        // The line holds the mention and the space that keeps it inline, so its end is two positions in.
        textArea.click().moveCaretTo(2);
        textArea.sendKeys(TEXT_AFTER);
        reloaded.save();

        assertThat(reloaded.editWiki().getContent(),
            matchesPattern(mentionPattern("XWiki\\.John", "XWiki-John") + " " + TEXT_AFTER));
    }

    @Test
    @Order(7)
    void insertMentionFromSlashMenu(TestUtils setup, TestReference testReference)
    {
        BlockNoteInplaceEditPage page = editFreshPage(setup, testReference);
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea().click();
        textArea.sendKeys(TEXT_BEFORE);
        // Same reason as in startMention(): the trigger has to reach the editor as a text input of its own.
        textArea.sendKeys("/");
        SlashMenu slashMenu = new SlashMenu();
        textArea.sendKeys("mention");
        slashMenu.waitForItemSelected(MENTION_ITEM);
        textArea.sendKeys(Keys.ENTER);
        slashMenu.waitForItemSubmitted();

        // The item opens the mention menu with no query, so narrow it down the way the trigger path does.
        SlashMenu mentionMenu = new SlashMenu();
        textArea.sendKeys(QUERY);
        mentionMenu.waitForItemTitles(List.of(GLOBAL_USER));

        // The slash menu query is gone from the content, leaving the same mention the trigger path produces.
        assertMentionSaved(page, mentionMenu, GLOBAL_USER, "XWiki\\.John", "XWiki-John");
    }

    /**
     * Edit a fresh page of the main wiki, in place.
     */
    private BlockNoteInplaceEditPage editFreshPage(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");
        return new BlockNoteInplaceEditPage().editInplace();
    }

    /**
     * Set the user scope of the subwiki, then edit a fresh page of that subwiki as the global user.
     */
    private BlockNoteInplaceEditPage editSubWikiPage(TestUtils setup, SubWikiTestUtils subWikiSetup,
        TestLocalReference testReference, UserScope userScope)
    {
        setup.loginAsSuperAdmin();
        subWikiSetup.setUserScope(SUBWIKI, userScope);
        backToMainWiki(setup);

        DocumentReference page = new DocumentReference(testReference, new WikiReference(SUBWIKI));
        setup.deletePage(page);
        setup.createPage(page, "", "");
        return new BlockNoteInplaceEditPage().editInplace();
    }

    /**
     * Type the mention trigger after some text, then the query, and wait for the mention menu to open.
     */
    private SlashMenu startMention(BlockNoteInplaceEditPage page)
    {
        return startMention(page, TEXT_BEFORE);
    }

    /**
     * Type the given text, then the mention trigger followed by the query, and wait for the mention menu to open.
     */
    private SlashMenu startMention(BlockNoteInplaceEditPage page, String textBefore)
    {
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea().click();
        if (!textBefore.isEmpty()) {
            textArea.sendKeys(textBefore);
        }
        return openMentionMenu(textArea);
    }

    /**
     * Type the mention trigger followed by the query at the caret, and wait for the mention menu to open.
     */
    private SlashMenu openMentionMenu(BlockNoteRichTextArea textArea)
    {
        // The trigger character has to be sent on its own. Typed together with the text before it, the browser
        // delivers the whole string as a single text input, which doesn't match the mention menu's trigger.
        textArea.sendKeys("@");
        SlashMenu mentionMenu = new SlashMenu();
        textArea.sendKeys(QUERY);
        return mentionMenu;
    }

    /**
     * Submit the suggestion of the given user, which inserts the mention at the caret.
     */
    private void submitMention(BlockNoteInplaceEditPage page, SlashMenu mentionMenu, String user)
    {
        // Also makes the menu track the item that has to go away when the mention is inserted, which is how
        // waitForItemSubmitted() below knows the suggestion was taken.
        mentionMenu.waitForItemSelected(user);
        page.getContentEditor().getRichTextArea().sendKeys(Keys.ENTER);
        mentionMenu.waitForItemSubmitted();
    }

    /**
     * @return the pattern matching the mention macro call the editor produces for the given user, the anchor ending
     *     with a random suffix so that the notifications can tell apart two mentions of the same user
     */
    private static String mentionPattern(String reference, String anchorPrefix)
    {
        return "\\{\\{mention reference=\"" + reference + "\" style=\"FULL_NAME\" anchor=\"" + anchorPrefix
            + "-[a-z0-9]{6}\"/\\}\\}";
    }

    /**
     * Submit the suggestion of the given user, save, then check the mention macro the editor produced.
     */
    private void assertMentionSaved(BlockNoteInplaceEditPage page, SlashMenu mentionMenu, String user,
        String reference, String anchorPrefix)
    {
        submitMention(page, mentionMenu, user);
        page.save();

        // The trailing space is what keeps the call inline, see keepLoneMentionInline().
        assertThat(page.editWiki().getContent(),
            matchesPattern(TEXT_BEFORE + mentionPattern(reference, anchorPrefix) + " "));
    }

    /**
     * Editing the subwiki configuration navigates to the subwiki, where the global user can't log in.
     */
    private void backToMainWiki(TestUtils setup)
    {
        setup.setCurrentWiki("xwiki");
        loginAsJohn(setup);
    }
}
