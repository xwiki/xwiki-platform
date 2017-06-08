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
 * Functional tests for remove formatting support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class RemoveFormattingTest extends AbstractWysiwygTestCase
{
    /**
     * Tests if formatting markers are removed properly.
     */
    @Test
    public void testRemoveFormattingMarkers()
    {
        switchToSource();
        setSourceText("==== **//__--abc--__//** ====");
        switchToWysiwyg();
        selectAllContent();
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("==== abc ====");
    }

    /**
     * Tests if in-line style is removed properly.
     */
    @Test
    public void testRemoveInlineStyle()
    {
        switchToSource();
        setSourceText("a(% style=\"color:red;\" %)b(% style=\"font-size:36pt;\" %)c(%%)d(%%)e");
        switchToWysiwyg();
        selectAllContent();
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("abcde");
    }

    /**
     * Tests if the formatting is removed properly when the selection spans across block-level elements.
     */
    @Test
    public void testRemoveFormattingFromCrossBlockSelection()
    {
        switchToSource();
        setSourceText("= a(% style=\"color:green\" %)b**cd**(%%)e =\n\nf(% style=\"font-size:36pt\" %)g//hi//(%%)j");
        switchToWysiwyg();
        select("document.body.getElementsByTagName('strong')[0].firstChild", 1,
            "document.body.getElementsByTagName('em')[0].firstChild", 1);
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("= a(% style=\"color:green\" %)b**c**(%%)de =\n\nfgh(% style=\"font-size:36pt\" %)//i//(%%)j");
    }

    /**
     * Tests if the anchors are kept after removing the formatting.
     */
    @Test
    public void testRemoveFormattingKeepsTheAnchorsIntact()
    {
        // Selection includes the anchor.
        switchToSource();
        setSourceText("a**b[[c//d//e>>http://www.xwiki.org]]f**g");
        switchToWysiwyg();
        selectAllContent();
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("ab[[cde>>http://www.xwiki.org]]fg");

        // Selection is included in the anchor.
        setSourceText("1**2[[3//456//7>>http://www.xwiki.org]]8**9");
        switchToWysiwyg();
        select("document.getElementsByTagName('em')[0].firstChild", 1,
            "document.getElementsByTagName('em')[0].firstChild", 2);
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("1**2**[[**3//4//**5**//6//7**>>http://www.xwiki.org]]**8**9");
    }

    /**
     * See XWIKI-3946: Standalone XHTML anchors with spans inside are converted badly to XWiki 2.0 syntax
     */
    @Test
    public void testRemoveFormattingFromStandaloneAnchor()
    {
        setContent("<a href=\"http://www.xwiki.org\" style=\"color: red; font-size: 18pt;\">123456</a>");
        select("document.body.firstChild.firstChild", 2, "document.body.firstChild.firstChild", 4);
        clickRemoveFormattingButton();
        switchToSource();
        assertSourceText("[[(% style=\"color: red; font-size: 18pt;\" %)12(%%)34"
            + "(% style=\"color: red; font-size: 18pt;\" %)56>>url:http://www.xwiki.org]]");
    }

    /**
     * Clicks on the tool bar button for removing the in-line formatting of the current selection.
     */
    protected void clickRemoveFormattingButton()
    {
        pushToolBarButton("Clear Formatting");
    }
}
