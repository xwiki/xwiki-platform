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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link DOMUtils}.
 * 
 * @version $Id$
 */
public class DOMUtilsTest extends AbstractWysiwygClientTest
{
    /**
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    public void testGetTextRange()
    {
        Document doc = Document.get().cast();

        Element container = doc.createSpanElement().cast();
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
    }

    /**
     * Unit test for {@link DOMUtils#getFirstAncestor(Node, String)}.
     */
    public void testGetFirstAncestor()
    {
        //setup a wikiLink tree
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        emptyContainer.appendChild(startContainer);
        Node preambleText = doc.createTextNode("our");
        startContainer.appendChild(preambleText);
        Node startWikiLinkComment = doc.createComment("startwikilink:Ref.erence");
        startContainer.appendChild(startWikiLinkComment);
        SpanElement wrappingSpan = doc.createSpanElement();
        startContainer.appendChild(wrappingSpan);
        AnchorElement anchor = doc.createAnchorElement();
        wrappingSpan.appendChild(anchor);
        Node labelPreamble = doc.createTextNode("x");
        anchor.appendChild(labelPreamble);
        Element boldWiki = doc.createElement("strong");
        anchor.appendChild(boldWiki);
        Node labelBoldWiki = doc.createTextNode("wiki");
        boldWiki.appendChild(labelBoldWiki);
        Node stopWikiLinkComment = doc.createComment("stopwikilink");
        startContainer.appendChild(stopWikiLinkComment);
        Node endText = doc.createTextNode("rox");
        startContainer.appendChild(endText);
        
        // check if there is a first ancestor of type a for the bold inside the anchor
        assertSame(anchor, DOMUtils.getInstance().getFirstAncestor(boldWiki, "a"));
        // check if there is a first ancestor of type a for the text inside bold in the anchor
        assertSame(anchor, DOMUtils.getInstance().getFirstAncestor(labelBoldWiki, "a"));
        // check there is no a ancestor of the wikilink span
        assertNull(DOMUtils.getInstance().getFirstAncestor(wrappingSpan, "a"));
        // check a finds itself as ancestor
        assertSame(anchor, DOMUtils.getInstance().getFirstAncestor(anchor, "a"));
        //check div ancestor search stops at startContainer
        assertSame(startContainer, DOMUtils.getInstance().getFirstAncestor(anchor, "div"));
    }

    /**
     * Unit test for {@link DOMUtils#getFirstDescendant(Node, String)}.
     */
    public void testGetFirstDescendant()
    {
        //setup a wikiLink tree
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        emptyContainer.appendChild(startContainer);
        Node preambleText = doc.createTextNode("our");
        startContainer.appendChild(preambleText);
        Node startWikiLinkComment = doc.createComment("startwikilink:Ref.erence");
        startContainer.appendChild(startWikiLinkComment);
        SpanElement wrappingSpan = doc.createSpanElement();
        startContainer.appendChild(wrappingSpan);
        AnchorElement anchor = doc.createAnchorElement();
        wrappingSpan.appendChild(anchor);
        Node labelPreamble = doc.createTextNode("x");
        anchor.appendChild(labelPreamble);
        SpanElement styledWiki = doc.createSpanElement();
        anchor.appendChild(styledWiki);
        Node labelBoldWiki = doc.createTextNode("wiki");
        styledWiki.appendChild(labelBoldWiki);
        Node stopWikiLinkComment = doc.createComment("stopwikilink");
        startContainer.appendChild(stopWikiLinkComment);
        Node endText = doc.createTextNode("rox");
        startContainer.appendChild(endText);
        
        //check anchor shows up as descendant of startContainer
        assertSame(anchor, DOMUtils.getInstance().getFirstDescendant(startContainer, "a"));
        //check anchor shows up as descendant of itself
        assertSame(anchor, DOMUtils.getInstance().getFirstDescendant(anchor, "a"));
        // check there is no descendant of type bold in the wrapping span
        assertNull(DOMUtils.getInstance().getFirstDescendant(wrappingSpan, "strong"));
        // check the first span descendant stops at the wrapping span
        assertSame(wrappingSpan, DOMUtils.getInstance().getFirstDescendant(emptyContainer, "span"));
        // check there is no anchor descendant of a text
        assertNull(DOMUtils.getInstance().getFirstDescendant(preambleText, "a"));
    }    
}
