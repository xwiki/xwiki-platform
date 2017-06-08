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
import org.openqa.selenium.By;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

/**
 * Functional tests for font support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class FontTest extends AbstractWysiwygTestCase
{
    /**
     * The XPath selector used to access the font size list box.
     */
    private static final String FONT_SIZE_SELECTOR = "//select[@title=\"Font Size\"]";

    /**
     * The XPath selector used to access the font name list box.
     */
    private static final String FONT_NAME_SELECTOR = "//select[@title=\"Font Name\"]";

    /**
     * Selects a plain text and applies a specific font size.
     * 
     * @see XWIKI-3295: Font size are not handled properly
     */
    @Test
    public void testSetFontSizeOnAPlainTextSelection()
    {
        typeText("abc");
        select("document.body.firstChild", 1, "document.body.firstChild", 2);
        applyFontSize("24px");
        switchToSource();
        assertSourceText("a(% style=\"font-size: 24px;\" %)b(%%)c");
    }

    /**
     * Selects a plain text and applies a specific font name.
     */
    @Test
    public void testSetFontNameOnAPlainTextSelection()
    {
        typeText("abc");
        select("document.body.firstChild", 1, "document.body.firstChild", 2);
        applyFontName("Georgia");
        switchToSource();
        assertSourceText("a(% style=\"font-family: Georgia;\" %)b(%%)c");
    }

    /**
     * Selects a plain text and applies a specific font name and font size.
     */
    @Test
    public void testSetFontNameAndSizeOnAPlainTextSelection()
    {
        typeText("abc");
        select("document.body.firstChild", 1, "document.body.firstChild", 2);
        applyFontName("Arial");
        applyFontSize("18px");
        switchToSource();
        assertSourceText("a(% style=\"font-family: Arial; font-size: 18px;\" %)b(%%)c");
    }

    /**
     * Test if the font size and font name are detected correctly.
     */
    @Test
    public void testDetectFont()
    {
        switchToSource();
        setSourceText("(% style=\"font-size: 24px; font-family: foo,verdana,sans-serif\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontSize("24px");
        waitForDetectedFontName("Verdana");
    }

    /**
     * Test if a known font name (contained in the list box) that is not supported by the current browser is correctly
     * detected.
     */
    @Test
    public void testDetectKnownUnsupportedFontName()
    {
        switchToSource();
        setSourceText("(% style=\"font-family: wingdings\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontName("Wingdings");

        switchToSource();
        setSourceText("(% style=\"font-family: wingdings,arial\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontName("Arial");
    }

    /**
     * Tests if an unknown font name if detected.
     */
    @Test
    public void testDetectUnknownFontName()
    {
        switchToSource();
        setSourceText("(% style=\"font-family: unknown\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontName("unknown");

        switchToSource();
        setSourceText("(% style=\"font-family: unknown,arial\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontName("Arial");
    }

    /**
     * Detect a font size that is not listed.
     */
    @Test
    public void testDetectUnlistedFontSize()
    {
        switchToSource();
        setSourceText("(% style=\"font-size: 21px\" %)\nabc");
        switchToWysiwyg();
        selectAllContent();
        waitForDetectedFontSize("21px");
    }

    /**
     * Test if the font name for a cross paragraph selection is correctly detected.
     */
    @Test
    public void testDetectFontNameOnCrossParagraphSelection()
    {
        switchToSource();
        setSourceText("(% style=\"font-family: courier new\" %)\nabc\n\n(% style=\"font-family: times new roman\" %)\nxyz");
        switchToWysiwyg();
        moveCaret("document.body.getElementsByTagName('p')[0].firstChild", 1);
        waitForDetectedFontName("Courier New");
        moveCaret("document.body.getElementsByTagName('p')[1].firstChild", 1);
        waitForDetectedFontName("Times New Roman");
        select("document.body.getElementsByTagName('p')[0].firstChild", 1,
            "document.body.getElementsByTagName('p')[1].firstChild", 1);
        waitForDetectedFontName("");
    }

    /**
     * Selects a font size from the list box.
     * 
     * @param fontSize the font size to select from the list box
     */
    protected void applyFontSize(String fontSize)
    {
        getSelenium().select(FONT_SIZE_SELECTOR, fontSize);
    }

    /**
     * Selects a font name from the list box.
     * 
     * @param fontName the font name to select from the list box
     */
    protected void applyFontName(String fontName)
    {
        getSelenium().select(FONT_NAME_SELECTOR, fontName);
    }

    /**
     * Waits for the editor to detect the font size of the current selection and asserts if the detected font size
     * equals the expected font size.
     * 
     * @param expectedFontSize the expected font size
     */
    protected void waitForDetectedFontSize(final String expectedFontSize)
    {
        getDriver().waitUntilElementHasAttributeValue(By.xpath(FONT_SIZE_SELECTOR), "value", expectedFontSize);
    }

    /**
     * Asserts if the detected font name equals the expected font name.
     * 
     * @param expectedFontName the expected font name
     */
    protected void waitForDetectedFontName(final String expectedFontName)
    {
        getDriver().waitUntilElementHasAttributeValue(By.xpath(FONT_NAME_SELECTOR), "value", expectedFontName);
    }
}
