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
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

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
        if (setup.isInWYSIWYGEditMode() || setup.isInWikiEditMode()) {
            new WikiEditPage().clickCancel();
        }
    }

    protected void waitForSolrIndexing(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();
    }
}
