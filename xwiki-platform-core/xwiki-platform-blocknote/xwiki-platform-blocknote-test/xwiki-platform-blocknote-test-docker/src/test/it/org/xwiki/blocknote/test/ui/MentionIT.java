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
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.user.UserScope;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Verify the user mentions of the BlockNote editor integration.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@UITest(
    properties = {
        // Registering a user copies its notification filter preferences, which are stored through a Hibernate
        // mapping that has to be added to hibernate.cfg.xml.
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an extension, so it
        // has to be in WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The untyped event converter has to load from the same class loader as the event it instantiates, whose
        // constructor is package-private and which comes from xwiki-platform-eventstream-api, in WEB-INF/lib.
        "org.xwiki.platform:xwiki-platform-eventstream-default",
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",
        // The REST resources backing the realtime session are registered once, at startup, so they have to come from
        // the class loader that serves every wiki rather than from the one of the wiki that provisioned them.
        "org.xwiki.platform:xwiki-platform-netflux-api",
        "org.xwiki.platform:xwiki-platform-netflux-rest",
        // The editor configuration template reads the user scope through $services.wiki.user, and a template is
        // evaluated outside of any extension namespace, so the wiki modules have to be core extensions too.
        "org.xwiki.platform:xwiki-platform-wiki-creationjob",
        "org.xwiki.platform:xwiki-platform-wiki-template-default",
        "org.xwiki.platform:xwiki-platform-wiki-script",
        "org.xwiki.platform:xwiki-platform-wiki-user-default",
        "org.xwiki.platform:xwiki-platform-wiki-user-script"
    }
)
class MentionIT extends AbstractBlockNoteIT
{
    private static final String SUBWIKI = "subwiki";

    private static final String GLOBAL_USER = "John";

    private static final String LOCAL_USER = "Johnny";

    /**
     * Matches both users, so that each user scope is told apart by the suggestions it leaves out.
     */
    private static final String QUERY = "joh";

    @BeforeAll
    void createSubWiki(TestUtils setup, SubWikiTestUtils subWikiSetup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // The editor and the mention macro are only visible in the wiki holding them.
        subWikiSetup.createWiki(SUBWIKI, "org.xwiki.platform:xwiki-platform-blocknote-api",
            "org.xwiki.platform:xwiki-platform-mentions-ui", "org.xwiki.platform:xwiki-platform-edit-ui",
            "org.xwiki.platform:xwiki-platform-image-style-ui",
            "org.xwiki.platform:xwiki-platform-attachment-picker-ui");
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
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
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
     * Type the mention trigger followed by the query, and wait for the mention menu to open.
     */
    private SlashMenu startMention(BlockNoteInplaceEditPage page)
    {
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();
        // The trigger character has to be sent on its own: typed together with the text before it, the browser
        // delivers the whole string as a single text input, which doesn't match the mention menu's trigger.
        textArea.click().sendKeys("Hello ").sendKeys("@");
        SlashMenu mentionMenu = new SlashMenu();
        textArea.sendKeys(QUERY);
        return mentionMenu;
    }

    /**
     * Submit the suggestion of the given user, save, then check the mention macro the editor produced.
     */
    private void assertMentionSaved(BlockNoteInplaceEditPage page, SlashMenu mentionMenu, String user,
        String reference, String anchorPrefix)
    {
        // Also makes the menu track the item that has to go away when the mention is inserted, which is how
        // waitForItemSubmitted() below knows the suggestion was taken.
        mentionMenu.waitForItemSelected(user);
        page.getContentEditor().getRichTextArea().sendKeys(Keys.ENTER);
        mentionMenu.waitForItemSubmitted();
        page.save();

        // The anchor ends with a random suffix, so that the notifications can tell apart two mentions of the same user.
        assertThat(page.editWiki().getContent(), matchesPattern("Hello \\{\\{mention reference=\"" + reference
            + "\" style=\"FULL_NAME\" anchor=\"" + anchorPrefix + "-[a-z0-9]{6}\"/\\}\\}"));
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
