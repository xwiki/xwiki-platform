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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Unit tests for {@link DOMUtils}.
 * 
 * @version $Id$
 */
public class DOMUtilsTest extends AbstractWysiwygClientTest
{
    /**
     * The rich text area to perform tests on.
     */
    private RichTextArea rta;

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
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    public void testGetTextRange()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestGetTextRange();
            }
        }).schedule(100);
    }

    /**
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    private void doTestGetTextRange()
    {
        Document doc = rta.getDocument();

        Element container = doc.xCreateSpanElement();
        container.setInnerHTML("<a href=\"http://www.xwiki.org\"><strong>ab</strong>cd<em>ef</em></a>");
        doc.getBody().appendChild(doc.createTextNode("before"));
        doc.getBody().appendChild(container);
        doc.getBody().appendChild(doc.createTextNode("after"));

        Range range = doc.createRange();
        range.setStart(container, 0);
        range.setEnd(container, 1);

        Range textRange = DOMUtils.getInstance().getTextRange(range);

        assertEquals(range.toString(), textRange.toString());

        assertEquals(Node.TEXT_NODE, textRange.getStartContainer().getNodeType());
        assertEquals(0, textRange.getStartOffset());

        assertEquals(Node.TEXT_NODE, textRange.getEndContainer().getNodeType());
        assertEquals(2, textRange.getEndOffset());

        finishTest();
    }

    /**
     * Test the first ancestor search, on a wikilink like html structure.
     */
    public void testGetFirstAncestor()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestGetFirstAncestor();
            }
        }).schedule(100);
    }

    /**
     * Test the first ancestor search, on a wikilink like html structure.
     */
    private void doTestGetFirstAncestor()
    {
        // setup a wikiLink tree
        Document doc = rta.getDocument();
        DivElement emptyContainer = doc.xCreateDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        DivElement startContainer = doc.xCreateDivElement();
        String wikilinkHTML =
            "our<!--startwikilink:Ref.erence--><span class=\"wikilink\" id=\"wrappingSpan\">"
                + "<a id=\"anchor\">x<strong id=\"boldWiki\">wiki</strong></a></span><!--stopwikilink-->rox";
        startContainer.setInnerHTML(wikilinkHTML);
        emptyContainer.appendChild(startContainer);
        Node anchor = doc.getElementById("anchor");
        Node wrappingSpan = doc.getElementById("wrappingSpan");
        Node boldWiki = doc.getElementById("boldWiki");
        Node labelBoldWiki = boldWiki.getFirstChild();

        // check if there is a first ancestor of type a for the bold inside the anchor
        assertSame("There isn't an anchor ancestor for the bold inside the anchor", anchor, DOMUtils.getInstance()
            .getFirstAncestor(boldWiki, "a"));
        // check if there is a first ancestor of type a for the text inside bold in the anchor
        assertSame("There isn't an anchor ancestor for the text in the bold inside the anchor", anchor, DOMUtils
            .getInstance().getFirstAncestor(labelBoldWiki, "a"));
        // check there is no a ancestor of the wikilink span
        assertNull("There is an anchor ancestor for the wikilink span", DOMUtils.getInstance().getFirstAncestor(
            wrappingSpan, "a"));
        // check a finds itself as ancestor
        assertSame("The anchor is not an anhor ancestor of itself", anchor, DOMUtils.getInstance().getFirstAncestor(
            anchor, "a"));
        // check div ancestor search stops at startContainer
        assertSame("Div ancestor search for the anchor does not stop at first div", startContainer, DOMUtils
            .getInstance().getFirstAncestor(anchor, "div"));

        finishTest();
    }

    /**
     * Test the first descendant search function, on a wikilink like structure.
     */
    public void testGetFirstDescendant()
    {
        delayTestFinish(300);
        (new Timer()
        {
            public void run()
            {
                doTestFirstDescendant();
            }
        }).schedule(100);
    }

    /**
     * Test the first descendant search function, on a wikilink like structure.
     */
    private void doTestFirstDescendant()
    {
        // setup a wikiLink tree
        Document doc = rta.getDocument();
        DivElement emptyContainer = doc.xCreateDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        DivElement startContainer = doc.xCreateDivElement();

        String wikilinkHTML =
            "our<!--startwikilink:Ref.erence--><span class=\"wikilink\" id=\"wrappingSpan\">"
                + "<a id=\"anchor\">x<span id=\"styledWiki\">wiki</span></a></span><!--stopwikilink-->rox";

        startContainer.setInnerHTML(wikilinkHTML);
        emptyContainer.appendChild(startContainer);

        Node anchor = doc.getElementById("anchor");
        Node wrappingSpan = doc.getElementById("wrappingSpan");
        Node preambleText = startContainer.getFirstChild();

        // check anchor shows up as descendant of startContainer
        assertSame("Anchor does not show up as descendant of startContainer", anchor, DOMUtils.getInstance()
            .getFirstDescendant(startContainer, "a"));
        // check anchor shows up as descendant of itself
        assertSame("Anchor does not show up as descendant of itself", anchor, DOMUtils.getInstance()
            .getFirstDescendant(anchor, "a"));
        // check there is no descendant of type bold in the wrapping span
        assertNull("There is a descendant of type bold in the wrapping span", DOMUtils.getInstance()
            .getFirstDescendant(wrappingSpan, "strong"));
        // check the first span descendant stops at the wrapping span
        assertSame("The first span descendant does not stop at the wrapping span", wrappingSpan, DOMUtils.getInstance()
            .getFirstDescendant(emptyContainer, "span"));
        // check there is no anchor descendant of a text
        assertNull("There is an anchor descendant of a text", DOMUtils.getInstance().getFirstDescendant(preambleText,
            "a"));

        finishTest();
    }
}
