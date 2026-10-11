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
import org.openqa.selenium.TimeoutException;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verify that the content edited in a realtime collaboration session is saved without the user asking for it.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@UITest(
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class AutoSaveIT extends AbstractBlockNoteIT
{
    /**
     * The version summary the auto-save records, see the {@code blocknote.autoSaveSummary} translation key.
     */
    private static final String AUTO_SAVE_SUMMARY = "Auto-saved during real-time collaboration";

    /**
     * The initial version of a freshly created page.
     */
    private static final String INITIAL_VERSION = "1.1";

    /**
     * The interval between two consecutive auto-saves, in seconds. The default one (a minute) would make these tests
     * needlessly slow, so each editor is asked to use this one instead.
     */
    private static final int AUTO_SAVE_INTERVAL = 5;

    /**
     * How long to wait for the auto-save to create a new version of the edited document, in seconds. A save lands a
     * bit later than the interval above: the saver adds a random amount to it, holds the save election for a moment
     * and then waits for the server. Waiting costs nothing when the save does happen, so leave a wide margin rather
     * than risk a flickering test on a loaded machine.
     */
    private static final int NEW_VERSION_TIMEOUT = 4 * AUTO_SAVE_INTERVAL;

    /**
     * How long to watch the edited document for a version that must not be created, in seconds. It has to outlast the
     * save interval, otherwise the absence of a save proves nothing. Unlike the timeout above this one is always
     * waited out in full, so keep it just above the point where a save would have happened.
     */
    private static final int NO_NEW_VERSION_TIMEOUT = 2 * AUTO_SAVE_INTERVAL;

    @Test
    @Order(1)
    void autoSavesWithoutClickingSave(TestReference testReference, TestUtils setup)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        new ViewPage().editWYSIWYG();
        BlockNoteEditor editor = new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL);
        BlockNoteRichTextArea textArea = editor.getRichTextArea();
        textArea.click();
        textArea.sendKeys(Keys.END, " two");

        // Nobody clicks any save button: the edit form must be submitted on its own.
        editor.waitForAutoSave();

        setup.leaveEditMode();
        ViewPage viewPage = new ViewPage();
        assertEquals("one two", viewPage.getContent());

        HistoryPane historyPane = viewPage.openHistoryDocExtraPane().showMinorEdits();
        // The auto-save created exactly one version on top of the one the page was created with.
        assertEquals(2, historyPane.getNumberOfVersions());
        assertEquals(AUTO_SAVE_SUMMARY, historyPane.getCurrentVersionComment());
    }

    @Test
    @Order(2)
    void autoSavesOnlyOnceWhenTwoClientsEdit(TestReference testReference, TestUtils setup,
        MultiUserTestUtils multiUserSetup) throws Exception
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        new ViewPage().editWYSIWYG();
        BlockNoteRichTextArea firstTextArea =
            new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();

        //
        // Second Tab
        //

        // The same user joins the session from another tab, in-place this time. The tab uses another host so that it
        // gets its own session, hence the login.
        String secondTabHandle = multiUserSetup.openNewBrowserTab(XWIKI_ALIAS);
        loginAsJohn(setup);
        setup.gotoPage(testReference);
        new InplaceEditablePage().editInplace();
        // Each tab has its own auto-saver, so the interval has to be set again here.
        BlockNoteRichTextArea secondTextArea =
            new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        // Typing starts the auto-save countdown, so both editors are loaded before anyone types: otherwise a slow
        // second load could let the first client save before the second one types, and the second client would
        // rightfully save its own later changes as another version.
        firstTextArea.click();
        firstTextArea.sendKeys(Keys.END, " two");

        //
        // Second Tab
        //

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        secondTextArea.waitUntilTextContains("one two");
        secondTextArea.click();
        secondTextArea.sendKeys(Keys.END, " three");

        //
        // First Tab
        //

        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        firstTextArea.waitUntilTextContains("three");

        // Both clients have changes to save, and only the one that wins the save election must save them. We watch
        // the document rather than the save notification because that notification is displayed only in the tab that
        // actually saved, and we cannot tell in advance which client the election picks.
        String savedVersion = setup.rest().waitForVersionChange(testReference, INITIAL_VERSION, NEW_VERSION_TIMEOUT);

        // Stay in the editing session well past another save interval: had the election let both clients through,
        // the second one would save here. We watch the document rather than the save notification because the
        // notification of the save above is still displayed in the tab that performed it.
        assertThrows(TimeoutException.class,
            () -> setup.rest().waitForVersionChange(testReference, savedVersion, NO_NEW_VERSION_TIMEOUT),
            "A second client saved the same changes.");

        multiUserSetup.switchToBrowserTab(secondTabHandle);
        setup.leaveEditMode();
        multiUserSetup.switchToBrowserTab(multiUserSetup.getFirstTabHandle());
        setup.leaveEditMode();

        ViewPage viewPage = setup.gotoPage(testReference);
        assertEquals("one two three", viewPage.getContent());
        assertEquals(savedVersion, setup.rest().<Page>get(testReference).getVersion(),
            "A second client saved the same changes.");

        HistoryPane historyPane = viewPage.openHistoryDocExtraPane().showMinorEdits();
        // A single version, even though both clients had changes to save.
        assertEquals(2, historyPane.getNumberOfVersions());
        assertEquals(AUTO_SAVE_SUMMARY, historyPane.getCurrentVersionComment());
    }

    /**
     * The first client joining a realtime session loads the initial content into the shared document, which is a
     * local change as far as Yjs is concerned. Counting it would auto-save a document that nobody edited, so the
     * editor, not the shared document, is what tells the auto-saver which changes are the local user's.
     */
    @Test
    @Order(3)
    void doesNotSaveWhenNobodyTypes(TestReference testReference, TestUtils setup)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        // Open the editor, which joins the realtime session, and then leave it alone.
        new ViewPage().editWYSIWYG();
        BlockNoteEditor editor = new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL);
        editor.getRichTextArea();

        assertFalse(editor.isAutoSavedWithinNextInterval(), "The content was saved even though nobody edited it.");

        setup.leaveEditMode();
        HistoryPane historyPane = new ViewPage().openHistoryDocExtraPane().showMinorEdits();
        // Note that we don't count the versions here: the history pagination, which is where that count is read
        // from, is not displayed when there is a single version.
        assertEquals(INITIAL_VERSION, historyPane.getCurrentVersion());
    }
}
