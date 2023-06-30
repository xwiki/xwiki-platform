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

import org.openqa.selenium.NoAlertPresentException;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    void edit(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
        WYSIWYGEditPage.gotoPage(testReference);
        this.editor = new CKEditor("content").waitToLoad();
        this.textArea = this.editor.getRichTextArea();
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
    }

    protected void assertSourceContains(String expectedSource)
    {
        editor.getToolBar().toggleSourceMode();
        String actualSource = editor.getSourceTextArea().getAttribute("value");
        assertTrue(actualSource.contains(expectedSource), "Unexpected source: " + actualSource);
        editor.getToolBar().toggleSourceMode();
    }

    protected void maybeLeaveEditMode(TestUtils setup, TestReference testReference)
    {
        if (setup.isInWYSIWYGEditMode() || setup.isInWikiEditMode()) {
            // We pass the action because we don't want to wait for view mode to be loaded.
            setup.gotoPage(testReference, "view");
            try {
                // Confirm the page leave (discard unsaved changes) if we are asked for.
                setup.getDriver().switchTo().alert().accept();
            } catch (NoAlertPresentException e) {
                // Do nothing.
            }
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
