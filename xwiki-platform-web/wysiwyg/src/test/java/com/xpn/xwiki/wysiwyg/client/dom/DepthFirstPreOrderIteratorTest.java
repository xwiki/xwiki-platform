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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link DepthFirstPreOrderIterator}.
 * 
 * @version $Id$
 */
public class DepthFirstPreOrderIteratorTest extends AbstractWysiwygClientTest
{

    /**
     * Test that a call to next when hasNext returns false throws exception.
     */
    public void testNextThrows()
    {
        Document doc = Document.get().cast();
        DivElement container = doc.createDivElement();
        doc.getBody().appendChild(container);
        Iterator<Node> it = doc.getIterator(doc);
        for (; it.hasNext();) {
            it.next();
        }
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
            // nothing, pass
        }
    }

    /**
     * Tests the remove function is not implemented.
     */
    public void testRemoveNotImplemented()
    {
        Document doc = Document.get().cast();

        DivElement container = doc.createDivElement();
        container.setInnerHTML("<strong>aa</strong><em>bb</em>");
        doc.getBody().appendChild(container);
        Iterator<Node> it = doc.getIterator(container);
        it.next();
        try {
            it.remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // nothing, pass
        }
    }

    /**
     * Test that we can iterate over the document element and that the body is amongst the iterated elements.
     */
    public void testDocumentElement()
    {
        Document doc = Document.get().cast();
        List<Node> foundNodes = new ArrayList<Node>();
        for (Iterator<Node> it = doc.getIterator(doc); it.hasNext();) {
            foundNodes.add(it.next());
        }
        assertTrue(foundNodes.contains(doc.getBody()));
    }

    /**
     * Test that an iterator over an empty element only returns that element.
     */
    public void testEmptyElement()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement container = doc.createDivElement();
        doc.getBody().appendChild(container);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(container);

        assertSame(expected, doc.getIterator(container));
    }

    /**
     * Test that an iterator over an empty text node only returns that element.
     */
    public void testEmptyText()
    {
        // setup document
        Document doc = Document.get().cast();
        Node text = doc.createTextNode("tim");
        doc.getBody().appendChild(text);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(text);

        assertSame(expected, doc.getIterator(text));
    }

    /**
     * Test that an iterator over a subtree starting in a comment returns only that comment.
     */
    public void testCommentAsRoot()
    {
        // setup document
        Document doc = Document.get().cast();
        Node commentNode = doc.createComment("xwikirox");

        doc.getBody().appendChild(commentNode);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(commentNode);

        assertSame(expected, doc.getIterator(commentNode));
    }

    /**
     * Test an iterator over a node with only text children.
     */
    public void testOnlyTextChildren()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        Node aliceText = doc.createTextNode("alice");
        Node bobText = doc.createTextNode("bob");
        Node carolText = doc.createTextNode("carol");
        startContainer.appendChild(aliceText);
        startContainer.appendChild(bobText);
        startContainer.appendChild(carolText);
        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(aliceText);
        expected.add(bobText);
        expected.add(carolText);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with only some empty element children.
     */
    public void testElementChildren()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        Node aliceText = doc.createTextNode("alice");
        Node bobText = doc.createTextNode("bob");
        DivElement cargo = doc.createDivElement();
        startContainer.appendChild(aliceText);
        startContainer.appendChild(cargo);
        startContainer.appendChild(bobText);
        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(aliceText);
        expected.add(cargo);
        expected.add(bobText);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with element with subtree children to its right.
     */
    public void testElementSubtreeRight()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        Node aliceText = doc.createTextNode("alice");
        Node bobText = doc.createTextNode("bob");
        Node come = doc.createComment("come");
        DivElement cargo = doc.createDivElement();
        startContainer.appendChild(aliceText);
        startContainer.appendChild(cargo);
        cargo.appendChild(bobText);
        cargo.appendChild(come);
        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(aliceText);
        expected.add(cargo);
        expected.add(bobText);
        expected.add(come);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with element with subtree children to its left.
     */
    public void testElementSubtreeLeft()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        Node aliceText = doc.createTextNode("alice");
        Node bobText = doc.createTextNode("bob");
        Node come = doc.createComment("come");
        DivElement cargo = doc.createDivElement();
        startContainer.appendChild(cargo);
        cargo.appendChild(bobText);
        cargo.appendChild(come);
        startContainer.appendChild(aliceText);
        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(cargo);
        expected.add(bobText);
        expected.add(come);
        expected.add(aliceText);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Test that an iterator over a node with element with subtree children to its left.
     */
    public void testElementSubtreeMiddle()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
        Node aliceText = doc.createTextNode("alice");
        Node bobText = doc.createTextNode("bob");
        Node come = doc.createComment("come");
        DivElement cargo = doc.createDivElement();
        startContainer.appendChild(come);
        startContainer.appendChild(cargo);
        cargo.appendChild(bobText);
        startContainer.appendChild(aliceText);
        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(come);
        expected.add(cargo);
        expected.add(bobText);
        expected.add(aliceText);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Test an iterator over an enclosing element of a wikilink.
     */
    public void testWikiLinkSubtree()
    {
        // setup document
        Document doc = Document.get().cast();
        DivElement emptyContainer = doc.createDivElement();
        doc.getBody().appendChild(emptyContainer);

        // do setup in the empty container
        Node startContainer = doc.createDivElement();
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

        emptyContainer.appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(preambleText);
        expected.add(startWikiLinkComment);
        expected.add(wrappingSpan);
        expected.add(anchor);
        expected.add(labelPreamble);
        expected.add(boldWiki);
        expected.add(labelBoldWiki);
        expected.add(stopWikiLinkComment);
        expected.add(endText);

        assertSame(expected, doc.getIterator(startContainer));
    }

    /**
     * Asserts the expected list of nodes with the actual list returned by this iterator.
     * 
     * @param expected the list of expected nodes
     * @param actual the iterator to compare with the expected list
     */
    private void assertSame(List<Node> expected, Iterator<Node> actual)
    {
        int listIndex = 0;
        for (; actual.hasNext();) {
            assertSame(expected.get(listIndex++), actual.next());
        }
    }
}
