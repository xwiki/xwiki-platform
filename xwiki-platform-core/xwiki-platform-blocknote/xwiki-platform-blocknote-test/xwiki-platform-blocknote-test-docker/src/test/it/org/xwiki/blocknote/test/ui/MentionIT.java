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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteInplaceEditPage;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.blocknote.test.po.SlashMenu;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

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
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket"
    }
)
class MentionIT extends AbstractBlockNoteIT
{
    @AfterAll
    void ignoreTheErrorsOfTheMentionsBackendThisInstanceDoesNotInstall(
        LogCaptureConfiguration logCaptureConfiguration)
    {
        // Only the mention macro is installed, so the mention event produced when saving has no converter. Both
        // shapes are logged, depending on which component manager reports the missing converter first.
        logCaptureConfiguration.registerExcludes("Failed to send event [targets = ",
            "Failed to lookup component with type [interface org.xwiki.eventstream.RecordableEventConverter]");
        // The page is saved while the instance is being torn down, after the notification caches are gone.
        logCaptureConfiguration.registerExcludes(
            "Cache 'notification.rest.longCache.events' is in 'TERMINATED' state");
    }

    @Test
    void insertMentionUsingMentionMenu(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "", "");

        BlockNoteInplaceEditPage page = new BlockNoteInplaceEditPage().editInplace();
        BlockNoteRichTextArea textArea = page.getContentEditor().getRichTextArea();

        // The trigger character has to be sent on its own: typed together with the text before it, the browser
        // delivers the whole string as a single text input, which doesn't match the mention menu's trigger.
        textArea.click().sendKeys("Hello ").sendKeys("@");
        SlashMenu mentionMenu = new SlashMenu();
        textArea.sendKeys("joh");
        mentionMenu.waitForItemSelected("John");
        textArea.sendKeys(Keys.ENTER);
        mentionMenu.waitForItemSubmitted();

        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        // The anchor ends with a random suffix, so that the notifications can tell apart two mentions of the same user.
        assertThat(wikiEditor.getContent(),
            matchesPattern("Hello \\{\\{mention reference=\"XWiki\\.John\" style=\"FULL_NAME\" "
                + "anchor=\"XWiki-John-[a-z0-9]{6}\"/\\}\\}"));
    }
}
