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
package org.xwiki.test.selenium;

import java.io.IOException;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;

import static org.junit.Assert.*;

/**
 * Tests the wiki editor.
 * 
 * @version $Id$
 */
public class WikiEditorTest extends AbstractXWikiTestCase
{
    private static final String SYNTAX = "xwiki/2.1";

    @Test
    public void testEmptyLineAndSpaceCharactersBeforeSectionTitleIsNotRemoved()
    {
        createPage("Test", "WikiEdit", "\n== Section ==\n\ntext", SYNTAX);
        open("Test", "WikiEdit", "edit", "editor=wiki");
        assertEquals("\n== Section ==\n\ntext", getFieldValue("content"));
    }

    @Test
    public void testBoldButton()
    {
        testToolBarButton("Bold", "**%s**", "Text in Bold");
    }

    @Test
    public void testItalicsButton()
    {
        testToolBarButton("Italics", "//%s//", "Text in Italics");
    }

    @Test
    public void testUnderlineButton()
    {
        testToolBarButton("Underline", "__%s__", "Text in Underline");
    }

    @Test
    public void testLinkButton()
    {
        testToolBarButton("Internal Link", "[[%s]]", "Link Example");
    }

    @Test
    public void testHRButton()
    {
        testToolBarButton("Horizontal ruler", "\n----\n", "");
    }

    @Test
    public void testImageButton()
    {
        testToolBarButton("Attached Image", "[[image:%s]]", "example.jpg");
    }

    /**
     * Tests that users can completely remove the content from a document (make the document empty). In previous
     * versions (pre-1.5M2), removing all content in page had no effect. See XWIKI-1007.
     */
    @Test
    public void testEmptyDocumentContentIsAllowed()
    {
        createPage("Test", "EmptyWikiContent", "this is some content", SYNTAX);
        editInWikiEditor("Test", "EmptyWikiContent", SYNTAX);
        setFieldValue("content", "");
        clickEditSaveAndView();
        assertFalse(getSelenium().isAlertPresent());
        assertEquals(-1, getSelenium().getLocation().indexOf("/edit/"));
        assertTextNotPresent("this is some content");
    }

    /**
     * Tests that the specified tool bar button works.
     * 
     * @param buttonTitle the title of a tool bar button
     * @param format the format of the text inserted by the specified button
     * @param defaultText the default text inserted if there's no text selected in the text area
     */
    private void testToolBarButton(String buttonTitle, String format, String defaultText)
    {
        editInWikiEditor(this.getClass().getSimpleName(), getTestMethodName(), SYNTAX);
        WebElement textArea = getDriver().findElement(By.id("content"));
        textArea.clear();
        textArea.sendKeys("a");
        String buttonLocator = "//img[@title = '" + buttonTitle + "']";
        getSelenium().click(buttonLocator);
        // Type b and c on two different lines and move the caret after b.
        textArea.sendKeys("b", Keys.RETURN, "c", Keys.ARROW_LEFT, Keys.ARROW_LEFT);
        getSelenium().click(buttonLocator);
        // Move the caret after c, type d and e, then select d.
        textArea.sendKeys(Keys.PAGE_DOWN, Keys.END, "de", Keys.ARROW_LEFT, Keys.chord(Keys.SHIFT, Keys.ARROW_LEFT));
        getSelenium().click(buttonLocator);
        if (defaultText.isEmpty()) {
            assertEquals("a" + format + "b" + format + "\nc" + format + "de", textArea.getAttribute("value"));
        } else {
            assertEquals(
                String.format("a" + format + "b" + format + "\nc" + format + "e", defaultText, defaultText, "d"),
                textArea.getAttribute("value"));
        }
    }
}
