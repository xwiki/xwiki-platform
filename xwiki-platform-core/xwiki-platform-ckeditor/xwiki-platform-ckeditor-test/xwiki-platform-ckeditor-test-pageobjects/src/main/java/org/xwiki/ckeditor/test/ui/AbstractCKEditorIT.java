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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Base class for CKEditor integration tests.
 * 
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
public abstract class AbstractCKEditorIT
{
    protected CKEditor editor;

    protected RichTextAreaElement textArea;

    WYSIWYGEditPage edit(TestUtils setup, TestReference testReference)
    {
        return edit(setup, testReference, true);
    }

    WYSIWYGEditPage edit(TestUtils setup, TestReference testReference, boolean startFresh)
    {
        if (startFresh) {
            setup.deletePage(testReference, true);
        }
        WYSIWYGEditPage editPage = WYSIWYGEditPage.gotoPage(testReference);
        this.editor = new CKEditor("content").waitToLoad();
        this.textArea = this.editor.getRichTextArea();
        return editPage;
    }

    protected void createAndLoginStandardUser(TestUtils setup)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    protected void assertSourceEquals(String expected)
    {
        editor.getToolBar().toggleSourceMode();
        assertEquals(expected, editor.getSourceTextArea().getAttribute("value"));
        editor.getToolBar().toggleSourceMode();
        this.textArea = this.editor.getRichTextArea();
    }

    protected void assertSourceContains(String expectedSource)
    {
        editor.getToolBar().toggleSourceMode();
        String actualSource = editor.getSourceTextArea().getAttribute("value");
        assertTrue(actualSource.contains(expectedSource), "Unexpected source: " + actualSource);
        editor.getToolBar().toggleSourceMode();
        this.textArea = this.editor.getRichTextArea();
    }

    protected void setSource(String source)
    {
        editor.getToolBar().toggleSourceMode();
        WebElement sourceTextArea = editor.getSourceTextArea();
        sourceTextArea.clear();
        sourceTextArea.sendKeys(source);
        editor.getToolBar().toggleSourceMode();
        this.textArea = this.editor.getRichTextArea();
    }

    protected void maybeLeaveEditMode(TestUtils setup, TestReference testReference)
    {
        try {
            // Dismiss the page leave confirmation modal if already open. We have to do this because we need to insert
            // the page reload marker, see below, which is not possible while the modal is open.
            setup.getDriver().switchTo().alert().dismiss();
        } catch (Exception e) {
            // The page leave confirmation modal wasn't open.
        }

        if (setup.isInWYSIWYGEditMode() || setup.isInWikiEditMode()) {
            // Leaving the edit mode with unsaved changes triggers the confirmation alert which stops the navigation.
            // Selenium doesn't wait for the new web page to be loaded after the alert is handled so we have to do this
            // ourselves. Adding the page reload marker helps us detect when the new web page is loaded after the
            // confirmation alert is handled.
            setup.getDriver().addPageNotYetReloadedMarker();

            // We pass the action because we don't want this call to wait for view mode to be loaded. We do our own wait
            // (after handling the confirmation alert), as mentioned above.
            setup.gotoPage(testReference, "view");

            try {
                // Confirm the page leave (discard unsaved changes) if we are asked for.
                setup.getDriver().switchTo().alert().accept();
            } catch (Exception e) {
                // The page leave confirmation hasn't been shown, probably because there were no unsaved changes.
            }

            // Wait for the new web page to be loaded.
            setup.getDriver().waitUntilPageIsReloaded();
            new ViewPage();
        }
    }

    protected void waitForSolrIndexing(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmptyQueue();
    }

    protected String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
