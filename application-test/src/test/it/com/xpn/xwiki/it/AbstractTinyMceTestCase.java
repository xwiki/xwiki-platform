/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it;

/**
 * Utility methods for easy testing of the Tiny MCE editor.
 *
 * @version $Id: $
 */
public abstract class AbstractTinyMceTestCase extends AbstractAuthenticatedAdminTestCase
{
    private static final String LOCATOR_FOR_KEY_EVENTS = "mceSpanFonts";

    public void editInTinyMce(String space, String page)
    {
        open("/xwiki/bin/edit/" + space + "/" + page + "?editor=wysiwyg");
    }

    public void clearTinyMceContent()
    {
        getSelenium().waitForCondition(
            "selenium.browserbot.getCurrentWindow().tinyMCE.setContent(\"\"); true", "18000");
    }

    public void typeInTinyMce(String text)
    {
        getSelenium().typeKeys(LOCATOR_FOR_KEY_EVENTS, text);
    }

    public void typeEnterInTinyMce()
    {
        getSelenium().keyPress(LOCATOR_FOR_KEY_EVENTS, "\\13");
    }

    public void clickTinyMceUnorderedListButton()
    {
        clickLinkWithLocator("//img[@title='Unordered list']", false);
    }

    public void clickTinyMceIndentButton()
    {
        clickLinkWithLocator("//img[@title='Indent']", false);
    }

    public void assertWikiTextGeneratedByTinyMCE(String text)
    {
        clickLinkWithText("Wiki");
        assertEquals(text, getSelenium().getValue("content"));
    }
}
