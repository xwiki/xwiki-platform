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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit tests for {@link SelectionPreserver}.
 * 
 * @version $Id$
 */
public class SelectionPreserverTest extends AbstractRichTextAreaTest
{
    /**
     * The selection preserver instance being tested.
     */
    private SelectionPreserver preserver;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (preserver == null) {
            preserver = new SelectionPreserver(rta);
        }
    }

    /**
     * Tests the preserver when the selection is inside a text node and the selected text doesn't change.
     */
    public void testPlainTextSelectionWithoutModification()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestPlainTextSelectionWithoutModification();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the preserver when the selection is inside a text node and the selected text doesn't change.
     */
    private void doTestPlainTextSelectionWithoutModification()
    {
        Element container = rta.getDocument().xCreateDivElement().cast();
        container.appendChild(rta.getDocument().createTextNode("xwiki"));
        rta.getDocument().getBody().appendChild(container);

        Range range = rta.getDocument().createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getFirstChild(), 3);
        String selectedText = "wi";

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals(selectedText, selection.toString());

        preserver.saveSelection();
        range.collapse(true);
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("", selection.toString());

        preserver.restoreSelection();
        assertEquals(selectedText, selection.toString());
    }

    /**
     * Tests the preserver when the selection is inside a text node and the selected text is replaced by some HTML.
     */
    public void testPlainTextSelectionWithHTMLInsertion()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestPlainTextSelectionWithHTMLInsertion();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the preserver when the selection is inside a text node and the selected text is replaced by some HTML.
     */
    private void doTestPlainTextSelectionWithHTMLInsertion()
    {
        Element container = rta.getDocument().xCreateDivElement().cast();
        container.appendChild(rta.getDocument().createTextNode("toucan"));
        rta.getDocument().getBody().appendChild(container);

        Range range = rta.getDocument().createRange();
        range.setStart(container.getFirstChild(), 0);
        range.setEnd(container.getFirstChild(), 2);

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("to", selection.toString());

        preserver.saveSelection();
        assertTrue(rta.getCommandManager().execute(Command.INSERT_HTML, "<ins>the</ins> <em>To</em>"));

        preserver.restoreSelection();
        assertEquals("the To", selection.toString());
    }

    /**
     * Tests the preserver when we have a text range and the range contents are replaced by some HTML.
     * 
     * @see com.xpn.xwiki.wysiwyg.client.dom.DOMUtils#getTextRange(Range)
     */
    public void testTextRangeSelectionWithHTMLInsertion()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestTextRangeSelectionWithHTMLInsertion();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the preserver when we have a text range and the range contents are replaced by some HTML.
     * 
     * @see com.xpn.xwiki.wysiwyg.client.dom.DOMUtils#getTextRange(Range)
     */
    private void doTestTextRangeSelectionWithHTMLInsertion()
    {
        Element container = rta.getDocument().xCreateDivElement().cast();
        container.setInnerHTML("ab<em>cd</em>ef<ins>gh</ins>ij");
        rta.getDocument().getBody().appendChild(container);

        Range range = rta.getDocument().createRange();
        range.setStart(container.getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(container.getChildNodes().getItem(3).getFirstChild(), 2);

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("defgh", selection.toString());

        preserver.saveSelection();
        assertTrue(rta.getCommandManager().execute(Command.INSERT_HTML, "<!--x--><span>y</span><!--z-->"));

        preserver.restoreSelection();
        assertEquals("y", selection.toString());
    }

    /**
     * Tests the preserver when the selection wraps an element and we replace it with some HTML.
     */
    public void testReplaceElement()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestReplaceElement();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests the preserver when the selection wraps an element and we replace it with some HTML.
     */
    private void doTestReplaceElement()
    {
        Element container = rta.getDocument().xCreateDivElement().cast();
        container.setInnerHTML("ab<em>cd</em>ef");
        rta.getDocument().getBody().appendChild(container);

        Range range = rta.getDocument().createRange();
        range.setStartBefore(container.getChildNodes().getItem(1));
        range.setEndAfter(container.getChildNodes().getItem(1));

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("cd", selection.toString());

        preserver.saveSelection();
        assertTrue(rta.getCommandManager().execute(Command.INSERT_HTML, "<ins>#</ins>"));

        preserver.restoreSelection();
        assertEquals("#", selection.toString());
    }
}
