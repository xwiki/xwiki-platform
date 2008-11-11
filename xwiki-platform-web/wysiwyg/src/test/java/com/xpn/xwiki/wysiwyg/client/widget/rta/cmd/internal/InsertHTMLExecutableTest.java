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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Unit tests for {@link InsertHTMLExecutable}.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutableTest extends AbstractWysiwygClientTest
{
    /**
     * The rich text area on which we run the tests.
     */
    private RichTextArea rta;

    /**
     * The executable being tested.
     */
    private InsertHTMLExecutable insertHTML = new InsertHTMLExecutable();

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        rta = new RichTextArea();
        RootPanel.get().add(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtTearDown()
     */
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        RootPanel.get().remove(rta);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes between DOM child nodes after the selection is
     * deleted.
     */
    public void testInsertBetweenChildren()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestInsertBetweenChildren();
            }
        }).schedule(100);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes between DOM child nodes after the selection is
     * deleted.
     */
    private void doTestInsertBetweenChildren()
    {
        rta.setFocus(true);

        Element container = rta.getDocument().xCreateDivElement().cast();
        rta.getDocument().getBody().appendChild(container);
        container.xSetInnerHTML("<em>ab</em><strong>cd</strong><ins>ef</ins>");

        Range range = rta.getDocument().createRange();
        range.setStartBefore(container.getChildNodes().getItem(1));
        range.setEndAfter(container.getChildNodes().getItem(1));

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("cd", selection.toString());
        assertTrue(insertHTML.execute(rta, "<!--x-->y<del>z</del>"));
        assertEquals("<em>ab</em><!--x-->y<del>z</del><ins>ef</ins>", container.getInnerHTML());

        finishTest();
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes at the end of an element after the selection is
     * deleted.
     */
    public void testInsertAfterLastChild()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestInsertAfterLastChild();
            }
        }).schedule(100);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes at the end of an element after the selection is
     * deleted.
     */
    private void doTestInsertAfterLastChild()
    {
        rta.setFocus(true);

        Element container = rta.getDocument().xCreateDivElement().cast();
        rta.getDocument().getBody().appendChild(container);
        container.xSetInnerHTML("<em>ab</em><strong>ij</strong>");

        Range range = rta.getDocument().createRange();
        range.setStartBefore(container.getChildNodes().getItem(1));
        range.setEndAfter(container.getChildNodes().getItem(1));

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("ij", selection.toString());
        assertTrue(insertHTML.execute(rta, "#"));
        assertEquals("<em>ab</em>#", container.getInnerHTML());

        finishTest();
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes inside a text node after the selection is deleted.
     */
    public void testInsertInTextNode()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestInsertInTextNode();
            }
        }).schedule(100);
    }

    /**
     * Tests the {@link InsertHTMLExecutable} when the caret goes inside a text node after the selection is deleted.
     */
    private void doTestInsertInTextNode()
    {
        rta.setFocus(true);

        Element container = rta.getDocument().xCreateDivElement().cast();
        rta.getDocument().getBody().appendChild(container);
        container.xSetInnerHTML("xyz");

        Range range = rta.getDocument().createRange();
        range.setStart(container.getFirstChild(), 1);
        range.setEnd(container.getFirstChild(), 2);

        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        assertEquals("y", selection.toString());
        assertTrue(insertHTML.execute(rta, "*2<em>=</em>1+"));
        assertEquals("x*2<em>=</em>1+z", container.getInnerHTML());

        finishTest();
    }
}
