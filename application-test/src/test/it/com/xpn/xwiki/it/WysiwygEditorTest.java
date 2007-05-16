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
 * Tests the WYSIWYG editor.
 *
 * @version $Id: $
 */
public class WysiwygEditorTest extends AbstractTinyMceTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        editInTinyMce("Test", "TestWysiwyg");
        clearTinyMceContent();
    }

    public void testSimpleList()
    {
        typeInTinyMce("item1");
        clickTinyMceUnorderedListButton();
        typeEnterInTinyMce();
        typeInTinyMce("item2");
        typeEnterInTinyMce();
        typeInTinyMce("item3");

        assertWikiTextGeneratedByTinyMCE("* item1\n* item2\n* item3");
    }

    public void testIndentation()
    {
        typeInTinyMce("Text");
        typeEnterInTinyMce();
        clickTinyMceIndentButton();
        typeInTinyMce("some indented text");

        assertWikiTextGeneratedByTinyMCE("Text\n<blockquote>\nsome indented text\n</blockquote>");
    }

    public void testLineFeed()
    {
        typeInTinyMce("Text");
        typeEnterInTinyMce();
        typeInTinyMce("Text");

        assertWikiTextGeneratedByTinyMCE("Text\n\nText");
    }

    public void testLineFeedWhenUsingShiftEnter()
    {
        typeInTinyMce("Text");
        typeShiftEnterInTinyMce();
        typeInTinyMce("Text");

        assertWikiTextGeneratedByTinyMCE("Text\\\\\nText");
    }

    public void testLineFeedBeforeAndAfterLists()
    {
        typeInTinyMce("Text");
        typeEnterInTinyMce();
        typeInTinyMce("item");
        clickTinyMceUnorderedListButton();
        typeEnterInTinyMce();
        clickTinyMceUnorderedListButton();
        typeInTinyMce("Text");

        assertWikiTextGeneratedByTinyMCE("Text\n\n* item\n\nText");
    }

    public void testEscapedHtmlElement()
    {
        typeInTinyMce("http://\\<yourserver\\>:8080/something");
        assertWikiTextGeneratedByTinyMCE("http://\\<yourserver\\>:8080/something");
    }

    public void testHtmlElementIsRendered()
    {
        typeInTinyMce("<table><tr><td>hello</td></tr></table>");
        assertWikiTextGeneratedByTinyMCE("<table><tr><td>hello</td></tr></table>");
    }

    public void testNestedNumberedList()
    {
        clickTinyMceOrderedListButton();
        typeInTinyMce("level 1");
        typeEnterInTinyMce();
        clickTinyMceIndentButton();
        typeInTinyMce("level 2");
        
        assertWikiTextGeneratedByTinyMCE("1. level 1\n11. level 2");
    }
}
