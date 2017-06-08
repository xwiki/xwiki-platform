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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import static org.junit.Assert.*;

/**
 * Tests if the state of the WYSIWYG editor is preserved (cached) against the browser's Back button and the "soft" page
 * refresh.
 * 
 * @version $Id$
 */
public class CacheTest extends AbstractWysiwygTestCase
{
    /**
     * Test that the content of the rich text area is preserved when the user leaves the editing without saving and then
     * comes back.
     */
    @Test
    public void testPreserveUnsavedRichContentAgainstBackButton()
    {
        // Type text and cancel edition.
        typeText("1");
        clickEditCancelEdition();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // Type text and leave the editing by clicking on a link.
        typeText("2");
        getSelenium().click("//a[. = '" + getTestMethodName() + "']");
        waitPage();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // Check the result.
        typeText("3");
        switchToSource();
        assertSourceText("321");
    }

    /**
     * Test that the content of the source text area is preserved when the user leaves the editing without saving and
     * then comes back.
     */
    @Test
    public void testPreserveUnsavedSourceAgainstBackButton()
    {
        switchToSource();

        // Type text and cancel edition.
        getSourceTextArea().sendKeys("a");
        clickEditCancelEdition();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // Type text and leave the editing by clicking on a link.
        getSourceTextArea().sendKeys("b");
        getSelenium().click("//a[. = '" + getTestMethodName() + "']");
        waitPage();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // Check the result.
        getSourceTextArea().sendKeys("c");
        assertSourceText("abc");
    }

    /**
     * Tests that the currently active editor (WYSIWYG or Source) is preserved when the user leaves the editing without
     * saving and then comes back.
     */
    @Test
    public void testPreserveSelectedEditorAgainstBackButton()
    {
        // The WYSIWYG editor should be initially active.
        assertFalse(getSourceTextArea().isEnabled());

        // Switch to Source editor, cancel the edition and then come back.
        switchToSource();
        clickEditCancelEdition();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // The Source editor should be active now because it was selected before canceling the edition.
        assertTrue(getSourceTextArea().isEnabled());

        // Switch to WYSIWYG editor, leave editing and then come back.
        switchToWysiwyg();
        getSelenium().click("//a[. = '" + getTestMethodName() + "']");
        waitPage();
        getSelenium().goBack();
        waitPage();
        waitForEditorToLoad();

        // The WYSIWYG editor should be active now because it was selected before we left the edit mode.
        assertFalse(getSourceTextArea().isEnabled());
    }

    /**
     * @see XWIKI-4162: When in edit mode (all editors) back/forward looses the content you have changed
     */
    @Test
    public void testBackForwardCache()
    {
        // Make sure we can go back.
        clickEditCancelEdition();
        clickEditPageInWysiwyg();
        waitForEditorToLoad();
        // Write some text.
        typeText("123");
        // Go back.
        getSelenium().goBack();
        waitPage();
        // Go forward.
        // See http://jira.openqa.org/browse/SEL-543 (Simulate forward button in browser).
        getSelenium().runScript("window.history.forward()");
        waitPage();
        // Make sure the rich text area is loaded.
        waitForEditorToLoad();
        // Assert the text content.
        assertEquals("123", getRichTextArea().getText());
    }
}
