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
import org.openqa.selenium.WindowType;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

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
     * The notification the edit form displays after it saved, see the
     * {@code core.editors.saveandcontinue.notification.done} translation key.
     */
    private static final String SAVED_NOTIFICATION = "Saved";

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
     * How long to wait for the auto-save, in seconds. A save lands a bit later than the interval above: the saver adds
     * a random amount to it, holds the save election for a moment and then waits for the server. Waiting costs nothing
     * when the save does happen, so leave a wide margin rather than risk a flickering test on a loaded machine.
     */
    private static final int AUTO_SAVE_TIMEOUT = 4 * AUTO_SAVE_INTERVAL;

    /**
     * How long to watch an editing session in which nothing must be saved, in seconds. It has to outlast the save
     * interval, otherwise the absence of a save proves nothing. Unlike the timeout above this one is always waited out
     * in full, so it's the one that costs test time: keep it just above the point where a save would have happened.
     */
    private static final int NO_AUTO_SAVE_TIMEOUT = 2 * AUTO_SAVE_INTERVAL;

    @Test
    @Order(1)
    void autoSavesWithoutClickingSave(TestReference testReference, TestUtils setup)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        WYSIWYGEditPage editPage = new ViewPage().editWYSIWYG();
        BlockNoteRichTextArea textArea =
            new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();
        textArea.click();
        textArea.sendKeys(Keys.END, " two");

        // Nobody clicks any save button: the edit form must be submitted on its own.
        waitForAutoSave(setup, editPage);

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
    void autoSavesOnlyOnceWhenTwoClientsEdit(TestReference testReference, TestUtils setup) throws Exception
    {
        //
        // First Tab
        //

        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, "one", "");

        String firstTabHandle = setup.getDriver().getWindowHandle();
        new ViewPage().editWYSIWYG();
        BlockNoteRichTextArea firstTextArea =
            new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();
        firstTextArea.click();
        firstTextArea.sendKeys(Keys.END, " two");

        //
        // Second Tab
        //

        // The same user joins the session from another tab, in-place this time.
        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        setup.gotoPage(testReference);
        new InplaceEditablePage().editInplace();
        // Each tab has its own auto-saver, so the interval has to be set again here.
        BlockNoteRichTextArea secondTextArea =
            new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();
        secondTextArea.waitUntilTextContains("one two");
        secondTextArea.click();
        secondTextArea.sendKeys(Keys.END, " three");

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.waitUntilTextContains("three");

        // Both clients have changes to save, and only the one that wins the save election must save them. We watch
        // the document rather than the save notification because that notification is displayed only in the tab that
        // actually saved, and we cannot tell in advance which client the election picks.
        String savedVersion = waitForNewVersion(setup, testReference);

        // Stay in the editing session well past another save interval: had the election let both clients through,
        // the second one would save here. We watch the document rather than the save notification because the
        // notification of the save above is still displayed in the tab that performed it.
        assertNoNewVersion(setup, testReference, savedVersion, "A second client saved the same changes.");

        setup.getDriver().switchTo().window(secondTabHandle);
        setup.leaveEditMode();
        setup.getDriver().switchTo().window(firstTabHandle);
        setup.leaveEditMode();

        ViewPage viewPage = setup.gotoPage(testReference);
        assertEquals("one two three", viewPage.getContent());
        assertEquals(savedVersion, getPage(setup, testReference).getVersion(),
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
        WYSIWYGEditPage editPage = new ViewPage().editWYSIWYG();
        new BlockNoteEditor("content").setAutoSaveInterval(AUTO_SAVE_INTERVAL).getRichTextArea();

        assertNoAutoSave(setup, editPage, "The content was saved even though nobody edited it.");

        setup.leaveEditMode();
        HistoryPane historyPane = new ViewPage().openHistoryDocExtraPane().showMinorEdits();
        // Note that we don't count the versions here: the history pagination, which is where that count is read
        // from, is not displayed when there is a single version.
        assertEquals(INITIAL_VERSION, historyPane.getCurrentVersion());
    }

    /**
     * Wait for the edit form to report that it saved.
     *
     * @param setup the test setup
     * @param page the page displaying the notification
     */
    private void waitForAutoSave(TestUtils setup, BaseElement page)
    {
        withTimeout(setup, AUTO_SAVE_TIMEOUT, () -> page.waitForNotificationSuccessMessage(SAVED_NOTIFICATION));
    }

    /**
     * Watch an editing session for longer than the save interval and fail if it saves.
     *
     * @param setup the test setup
     * @param page the page that would display the notification
     * @param message the failure message
     */
    private void assertNoAutoSave(TestUtils setup, BaseElement page, String message)
    {
        withTimeout(setup, NO_AUTO_SAVE_TIMEOUT, () -> assertThrows(TimeoutException.class,
            () -> page.waitForNotificationSuccessMessage(SAVED_NOTIFICATION), message));
    }

    /**
     * Watch the edited document for longer than the save interval and fail if a new version shows up.
     *
     * @param setup the test setup
     * @param reference the reference of the edited document
     * @param expectedVersion the version the document must stay at
     * @param message the failure message
     */
    private void assertNoNewVersion(TestUtils setup, DocumentReference reference, String expectedVersion,
        String message)
    {
        try {
            // Stop waiting as soon as another version shows up, which is the failure we're looking for.
            setup.getDriver().waitUntilCondition(
                driver -> expectedVersion.equals(getPage(setup, reference).getVersion()) ? null : true,
                NO_AUTO_SAVE_TIMEOUT);
        } catch (TimeoutException expected) {
            // No new version was created, which is what we want.
            return;
        }
        fail(message);
    }

    /**
     * Run the given code with a longer wait timeout, because the auto-save is scheduled a while after the content
     * became dirty, which is above the default.
     *
     * @param setup the test setup
     * @param timeout the timeout to use, in seconds
     * @param action the code to run
     */
    private void withTimeout(TestUtils setup, int timeout, Runnable action)
    {
        int originalTimeout = setup.getDriver().getTimeout();
        setup.getDriver().setTimeout(timeout);
        try {
            action.run();
        } finally {
            setup.getDriver().setTimeout(originalTimeout);
        }
    }

    /**
     * Wait for the auto-save to create a new version of the edited document.
     *
     * @param setup the test setup
     * @param reference the reference of the edited document
     * @return the version the auto-save created
     */
    private String waitForNewVersion(TestUtils setup, DocumentReference reference)
    {
        return setup.getDriver().waitUntilCondition(driver -> {
            String version = getPage(setup, reference).getVersion();
            return INITIAL_VERSION.equals(version) ? null : version;
        }, AUTO_SAVE_TIMEOUT);
    }

    /**
     * @param setup the test setup
     * @param reference the reference of the document to fetch
     * @return the document, as the server knows it
     */
    private Page getPage(TestUtils setup, DocumentReference reference)
    {
        try {
            return setup.rest().get(reference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch the edited document.", e);
        }
    }
}
