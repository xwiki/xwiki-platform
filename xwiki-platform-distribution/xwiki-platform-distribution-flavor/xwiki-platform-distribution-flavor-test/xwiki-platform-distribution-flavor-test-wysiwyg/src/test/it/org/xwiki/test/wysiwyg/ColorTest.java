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
 * Functional tests for color support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class ColorTest extends AbstractWysiwygTestCase
{
    /**
     * Tests if the text color can be changed.
     */
    @Test
    public void testChangeTextColor()
    {
        typeText("abc");

        // Select 'b'.
        select("document.body.firstChild", 1, "document.body.firstChild", 2);

        // Change the text color to red.
        clickForegroundColorButton();
        selectColor("rgb(255, 0, 0)");

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("a(% style=\"color: rgb(255, 0, 0);\" %)b(%%)c");
        switchToWysiwyg();

        // Place the caret after 'b' in order to check if the current color is detected.
        moveCaret("document.body.getElementsByTagName('span')[0].firstChild", 1);

        // Check if the editor detects the right color.
        clickForegroundColorButton();
        assertSelectedColor("rgb(255, 0, 0)", true);
        hideColorPicker();
    }

    /**
     * Tests if the background color can be changed.
     */
    @Test
    public void testChangeBackgroundColor()
    {
        typeText("abc");

        // Select 'b'.
        select("document.body.firstChild", 1, "document.body.firstChild", 2);

        // Change the text color to red.
        clickBackgroundColorButton();
        selectColor("rgb(255, 0, 0)");

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("a(% style=\"background-color: rgb(255, 0, 0);\" %)b(%%)c");
        switchToWysiwyg();

        // Place the caret after 'b' in order to check if the current color is detected.
        moveCaret("document.body.getElementsByTagName('span')[0].firstChild", 1);

        // Check if the editor detects the right color.
        clickBackgroundColorButton();
        assertSelectedColor("rgb(255, 0, 0)", true);
        hideColorPicker();
    }

    /**
     * Tests if both the text color and the background color can be changed on the current selection.
     */
    @Test
    public void testChangeTextAndBackgroudColor()
    {
        switchToSource();
        setSourceText("(% style=\"color: red; background-color:#777;\" %)foo");
        switchToWysiwyg();

        // Select the text.
        selectNodeContents("document.body.firstChild");

        // Change the text color.
        clickForegroundColorButton();
        selectColor("rgb(0, 255, 0)");

        // Change the background color.
        clickBackgroundColorButton();
        selectColor("rgb(252, 229, 205)");

        switchToSource();
        assertSourceText("(% style=\"color: rgb(0, 255, 0); background-color: rgb(252, 229, 205);\" %)foo");
    }

    /**
     * Makes a text bold, changes its color and then removes the bold style.
     */
    @Test
    public void testRemoveBoldStyleFromAColoredText()
    {
        // Type some text and make it bold.
        typeText("bar");
        selectAllContent();
        clickBoldButton();

        // Change the text color.
        clickForegroundColorButton();
        selectColor("rgb(0, 0, 255)");

        // Remove the bold style.
        clickBoldButton();

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("(% style=\"color: rgb(0, 0, 255);\" %)bar");
    }

    /**
     * Types two words in different colors, selects both and tries to change their color.
     * 
     * @see XWIKI-3564: Cannot change the text color after selecting text with different colors in IE
     */
    @Test
    public void testChangeTextColorAfterSelectingTextWithDifferentColors()
    {
        // Type the two words.
        typeText("a z");

        // Select the first word and change its color to red.
        select("document.body.firstChild", 0, "document.body.firstChild", 1);
        clickForegroundColorButton();
        selectColor("rgb(255, 0, 0)");

        // Select the second word and change its color to blue.
        select("document.body.childNodes[1]", 1, "document.body.childNodes[1]", 2);
        clickForegroundColorButton();
        selectColor("rgb(0, 0, 255)");

        // Select both words and change their color to green.
        selectAllContent();
        clickForegroundColorButton();
        selectColor("rgb(0, 255, 0)");

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("(% style=\"color: rgb(0, 255, 0);\" %)a z");
    }

    /**
     * Background-color CSS property is not inheritable so in order to detect its value we must iterate over all
     * ancestors of the current text selection.
     */
    @Test
    public void testDetectNestedBackgroundColor()
    {
        switchToSource();
        setSourceText("12 (% style=\"background-color:red\" %)34 "
            + "(% style=\"background-color:red;color:yellow\" %)56 **78** 90");
        switchToWysiwyg();
        // Place the caret between 7 and 8
        moveCaret("document.body.getElementsByTagName('strong')[0].firstChild", 1);
        // Check the detected background color.
        clickBackgroundColorButton();
        assertSelectedColor("rgb(255, 0, 0)", true);
        hideColorPicker();
    }

    /**
     * Clicks on the tool bar button for changing the text color.
     */
    protected void clickForegroundColorButton()
    {
        pushToolBarButton("Font Color");
    }

    /**
     * Clicks on the tool bar button for changing the text background color.
     */
    protected void clickBackgroundColorButton()
    {
        pushToolBarButton("Highlight Color");
    }

    /**
     * Selects the specified color from the color picker.
     * 
     * @param rgbColor the RGB color to select
     */
    protected void selectColor(String rgbColor)
    {
        getSelenium().click("//div[@class = 'colorCell' and @style = 'background-color: " + rgbColor + ";']");
    }

    /**
     * Asserts that the color selected by the color picker equals the given color.
     * 
     * @param rgbColor the expected selected color
     * @param dark {@code true} if the specified color is dark, {@code false} if it is light
     */
    protected void assertSelectedColor(String rgbColor, boolean dark)
    {
        assertElementPresent(String.format(
            "//div[contains(@class, 'colorCell-selected-%s') and @style = 'background-color: %s;']", dark ? "dark"
                : "light", rgbColor));
    }

    /**
     * Hides the color picker by clicking outside.
     */
    protected void hideColorPicker()
    {
        getRichTextArea().click();
    }
}
