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
package org.xwiki.ckeditor.test.ui;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditorToolBar;
import org.xwiki.ckeditor.test.po.EmojiPanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the CKEditor tool bar actions that insert content.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",
        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    resolveExtraJARs = true
)
class ToolBarIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        createAndLoginStandardUser(setup);
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    @Test
    @Order(1)
    void insertSpecialCharactersAndEmojis(TestUtils setup, TestReference testReference)
    {
        WYSIWYGEditPage editPage = edit(setup, testReference);
        CKEditorToolBar toolBar = this.editor.getToolBar();

        // The special character dialog is closed after each inserted character.
        for (String character : List.of("©", "€", "½")) {
            toolBar.insertSpecialCharacter().insert(character);
        }
        this.textArea.waitUntilTextContains("©€½");

        // Select an emoji from the first category, which is displayed when opening the panel.
        toolBar.openEmojiPanel().selectEmoji("😀");

        // Select an emoji from another category.
        EmojiPanel emojiPanel = toolBar.openEmojiPanel().selectCategory(EmojiPanel.Category.FLAGS);
        assertEquals(EmojiPanel.Category.FLAGS, emojiPanel.getActiveCategory());
        emojiPanel.selectEmoji("🏁");

        // Search for an emoji.
        emojiPanel = toolBar.openEmojiPanel().search("pizza");
        assertEquals(List.of("🍕"), emojiPanel.getEmojis());
        emojiPanel.selectEmoji("🍕");

        String expectedContent = "©€½😀🏁🍕";
        this.textArea.waitUntilTextContains(expectedContent);
        assertSourceEquals(expectedContent);

        ViewPage viewPage = editPage.clickSaveAndView();
        assertEquals(expectedContent, viewPage.getContent());
    }
}
