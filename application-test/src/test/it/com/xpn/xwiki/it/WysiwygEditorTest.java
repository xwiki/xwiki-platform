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

import com.xpn.xwiki.it.framework.AbstractXWikiTestCase;
import com.xpn.xwiki.it.framework.XWikiTestSuite;
import com.xpn.xwiki.it.framework.AlbatrossSkinExecutor;
import junit.framework.Test;

/**
 * Tests the WYSIWYG editor (content edited in WYSIWYG mode).
 *
 * @version $Id: $
 */
public class WysiwygEditorTest extends AbstractXWikiTestCase
{
    public static Test suite()
    {
        XWikiTestSuite suite = new XWikiTestSuite("Tests the wiki editor");
        suite.addTestSuite(WysiwygEditorTest.class, AlbatrossSkinExecutor.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        loginAsAdmin();
        editInWysiwyg("Test", "TestWysiwyg");
        clearWysiwygContent();
    }

    public void testSimpleList()
    {
        typeInWysiwyg("item1");
        clickWysiwygUnorderedListButton();
        typeEnterInWysiwyg();
        typeInWysiwyg("item2");
        typeEnterInWysiwyg();
        typeInWysiwyg("item3");

        assertWikiTextGeneratedByWysiwyg("* item1\n* item2\n* item3");
    }

    public void testIndentation()
    {
        typeInWysiwyg("Text");
        typeEnterInWysiwyg();
        clickWysiwygIndentButton();
        typeInWysiwyg("some indented text");

        assertWikiTextGeneratedByWysiwyg("Text\n<blockquote>\nsome indented text\n</blockquote>");
    }

    public void testLineFeed()
    {
        typeInWysiwyg("Text");
        typeEnterInWysiwyg();
        typeInWysiwyg("Text");

        assertWikiTextGeneratedByWysiwyg("Text\n\nText");
    }

    public void testLineFeedWhenUsingShiftEnter()
    {
        typeInWysiwyg("Text");
        typeShiftEnterInWysiwyg();
        typeInWysiwyg("Text");

        assertWikiTextGeneratedByWysiwyg("Text\\\\\nText");
    }

    public void testLineFeedBeforeAndAfterLists()
    {
        typeInWysiwyg("Text");
        typeEnterInWysiwyg();
        typeInWysiwyg("item");
        clickWysiwygUnorderedListButton();
        typeEnterInWysiwyg();
        clickWysiwygUnorderedListButton();
        typeInWysiwyg("Text");

        assertWikiTextGeneratedByWysiwyg("Text\n\n* item\n\nText");
    }

    public void testEscapedHtmlElement()
    {
        typeInWysiwyg("http://\\<yourserver\\>:8080/something");
        assertWikiTextGeneratedByWysiwyg("http://\\<yourserver\\>:8080/something");
    }

    public void testHtmlElementIsRendered()
    {
        typeInWysiwyg("<table><tr><td>hello</td></tr></table>");
        assertWikiTextGeneratedByWysiwyg("<table><tr><td>hello</td></tr></table>");
    }

    public void testNestedOrderedList()
    {
        clickWysiwygOrderedListButton();
        typeInWysiwyg("level 1");
        typeEnterInWysiwyg();
        clickWysiwygIndentButton();
        typeInWysiwyg("level 2");
        
        assertWikiTextGeneratedByWysiwyg("1. level 1\n11. level 2");
    }
}
