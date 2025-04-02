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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
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

    WYSIWYGEditPage edit(TestUtils setup, EntityReference entityReference)
    {
        return edit(setup, entityReference, true);
    }

    WYSIWYGEditPage edit(TestUtils setup, EntityReference pageReference, boolean startFresh)
    {
        if (startFresh) {
            setup.deletePage(pageReference, true);
        }
        WYSIWYGEditPage editPage = WYSIWYGEditPage.gotoPage(pageReference);
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
        assertSourceEquals(expected, false);
    }

    protected void assertSourceEquals(String expected, boolean normalizeSpaces)
    {
        editor.getToolBar().toggleSourceMode();
        String actualSource = editor.getSourceTextArea().getDomProperty("value");
        if (actualSource != null && normalizeSpaces) {
            actualSource = actualSource.replace('\u00A0', ' ');
        }
        assertEquals(expected, actualSource);
        editor.getToolBar().toggleSourceMode();
        this.textArea = this.editor.getRichTextArea();
    }

    protected void assertSourceContains(String expectedSource)
    {
        editor.getToolBar().toggleSourceMode();
        String actualSource = editor.getSourceTextArea().getDomProperty("value");
        assertTrue(StringUtils.contains(actualSource, expectedSource), "Unexpected source: " + actualSource);
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
        if (StringUtils.isNotEmpty(setup.getEditMode())) {
            // Use the cancel shortcut key to leave the edit mode.
            setup.getDriver().switchTo().activeElement().sendKeys(Keys.chord(Keys.ALT, "c"));
        }
    }

    protected void waitForSolrIndexing(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();
    }
}
