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

/**
 * Test case for wysiwyg content import plugin.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class ImportTest extends AbstractWysiwygTestCase
{
    public static final String IMPORT_BUTTON = "Import";

    public static final String FILTER_STYLES = "//div[@class = 'xDialogBody']//input[@type = 'checkbox']";

    /**
     * Test that the paste wizard works.
     */
    @Test
    public void testPasteFromClipboard()
    {
        clickPasteButton();
        populateOfficeContentEditor("<p>Hello <font color=\"#ff0000\">World</font></p>");
        getSelenium().uncheck(FILTER_STYLES);
        clickButtonWithText(IMPORT_BUTTON);
        waitForDialogToClose();
        switchToSource();
        assertSourceText("Hello (% style=\"color:#ff0000;\" %)World");
    }

    /**
     * @see XWIKI-3040: A rich text area on a dialog box looses its content if we move the dialog box
     */
    @Test
    public void testPastedContentIsPreservedWhenDialogIsMoved()
    {
        clickPasteButton();
        // Put some content inside the rich text area of the office import dialog.
        populateOfficeContentEditor("office");
        // Move the dialog.
        getSelenium().dragdrop("//div[@class='gwt-Label xDialogCaption']", "100, 100");
        // Import the pasted content.
        clickButtonWithText(IMPORT_BUTTON);
        waitForDialogToClose();
        // Check the result.
        switchToSource();
        assertSourceText("office");
    }

    /**
     * Click the paste button on the tool bar.
     */
    protected void clickPasteButton()
    {
        pushToolBarButton("Paste");
    }

    /**
     * Utility method for injecting html content into office import wizard's copy paste area.
     * 
     * @param innerHTML html content.
     */
    private void populateOfficeContentEditor(String innerHTML)
    {
        StringBuilder script = new StringBuilder();
        script.append("var eframe = document.getElementsByClassName('xImportOfficeContentEditor')[0];\n");
        script.append("var rte = eframe.contentWindow.document.body;\n");
        script.append(String.format("rte.innerHTML = '%s';\n", innerHTML));
        getSelenium().getEval(script.toString());
    }
}
