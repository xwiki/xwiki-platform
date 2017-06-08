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
import org.openqa.selenium.Keys;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

/**
 * Functional tests for history support (undo/redo) inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class HistoryTest extends AbstractWysiwygTestCase
{
    /**
     * Basic integration test for the history mechanism.
     */
    @Test
    public void testUndoRedo()
    {
        getRichTextArea().sendKeys("a b", Keys.TAB, "c");
        clickSymbolButton();
        getSelenium().click("//div[@title='copyright sign']");
        applyStyleTitle1();
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        clickUndoButton(4);
        assertContent("a b<br>");
        clickUndoButton(3);
        assertContent("<br>");
        waitForPushButton(TOOLBAR_BUTTON_REDO_TITLE, true);
        clickRedoButton(7);
        assertContent("<h1>a b&nbsp;&nbsp;&nbsp; c\u00A9<br></h1>");
    }

    /**
     * Tests the shortcut keys for undo and redo operations. Undo is triggered by CTRL+Z or META+Z. The second is used
     * on apple keyboards. Redo is triggered by CTRL+Y or META+Y. The second is also used on apple keyboards.
     * 
     * @see XWIKI-3048: Undo/Redo/Copy/Paste/Cut Mac shortcuts should be mapped to the corresponding features of the
     *      WYSIWYG editor
     */
    @Test
    public void testUndoRedoShortcutKeys()
    {
        setContent("March 9th, 2009");
        select("document.body.firstChild", 0, "document.body.firstChild", 5);

        // Make text bold.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "b"));

        // Make text italic.
        // FIXME: getRichTextArea().sendKeys(Keys.chord(Keys.META, "i"));
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "i"));

        // Make text underline.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "u"));

        // Undo last 3 steps.
        // The undo tool bar button is initially disabled because no action has been taken on the edited document. We
        // have to wait for it to become enabled because the tool bar is updated with delay after each edit action.
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        // FIXME: getRichTextArea().sendKeys(Keys.chord(Keys.META, "zzz"));
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "zzz"));

        // Redo 2 steps.
        // We have to wait for the redo tool bar button to become enabled because the tool bar is updated with delay
        // after an undo operation.
        waitForPushButton(TOOLBAR_BUTTON_REDO_TITLE, true);
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "yy"));

        switchToSource();
        assertSourceText("**//March//** 9th, 2009");
    }
}
