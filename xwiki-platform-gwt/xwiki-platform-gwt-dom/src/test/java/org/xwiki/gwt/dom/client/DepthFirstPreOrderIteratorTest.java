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
package org.xwiki.gwt.dom.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

/**
 * Unit tests for {@link DepthFirstPreOrderIterator}.
 * 
 * @version $Id$
 */
public class DepthFirstPreOrderIteratorTest extends DOMTestCase
{

    /**
     * Test that a call to next when hasNext returns false throws exception.
     */
    public void testNextThrows()
    {
        Iterator<Node> it = getDocument().getIterator(getDocument());
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
        getContainer().setInnerHTML("<strong>aa</strong><em>bb</em>");
        Iterator<Node> it = getDocument().getIterator(getContainer());
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
        List<Node> foundNodes = new ArrayList<Node>();
        for (Iterator<Node> it = getDocument().getIterator(getDocument()); it.hasNext();) {
            foundNodes.add(it.next());
        }
        assertTrue(foundNodes.contains(getDocument().getBody()));
    }

    /**
     * Test that an iterator over an empty element only returns that element.
     */
    public void testEmptyElement()
    {
        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(getContainer());

        assertSame(expected, getDocument().getIterator(getContainer()));
    }

    /**
     * Test that an iterator over an empty text node only returns that element.
     */
    public void testEmptyText()
    {
        // setup document
        Node text = getDocument().createTextNode("tim");
        getContainer().appendChild(text);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(text);

        assertSame(expected, getDocument().getIterator(text));
    }

    /**
     * Test that an iterator over a subtree starting in a comment returns only that comment.
     */
    public void testCommentAsRoot()
    {
        // setup document
        Node commentNode = getDocument().createComment("xwikirox");
        getContainer().appendChild(commentNode);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(commentNode);

        assertSame(expected, getDocument().getIterator(commentNode));
    }

    /**
     * Test an iterator over a node with only text children.
     */
    public void testOnlyTextChildren()
    {
        Node startContainer = getDocument().createDivElement();
        Node oneText = getDocument().createTextNode("one");
        Node twoText = getDocument().createTextNode("two");
        Node threeText = getDocument().createTextNode("three");
        startContainer.appendChild(oneText);
        startContainer.appendChild(twoText);
        startContainer.appendChild(threeText);
        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(oneText);
        expected.add(twoText);
        expected.add(threeText);

        assertSame(expected, getDocument().getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with only some empty element children.
     */
    public void testElementChildren()
    {
        Node startContainer = getDocument().createDivElement();
        Node johnText = getDocument().createTextNode("john");
        Node doeText = getDocument().createTextNode("doe");
        DivElement cargo = getDocument().createDivElement();
        startContainer.appendChild(johnText);
        startContainer.appendChild(cargo);
        startContainer.appendChild(doeText);
        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(johnText);
        expected.add(cargo);
        expected.add(doeText);

        assertSame(expected, getDocument().getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with element with subtree children to its right.
     */
    public void testElementSubtreeRight()
    {
        Node startContainer = getDocument().createDivElement();
        Node fooText = getDocument().createTextNode("foo");
        Node barText = getDocument().createTextNode("bar");
        Node far = getDocument().createComment("far");
        DivElement cargo = getDocument().createDivElement();
        startContainer.appendChild(fooText);
        startContainer.appendChild(cargo);
        cargo.appendChild(barText);
        cargo.appendChild(far);
        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(fooText);
        expected.add(cargo);
        expected.add(barText);
        expected.add(far);

        assertSame(expected, getDocument().getIterator(startContainer));
    }

    /**
     * Test an iterator over a node with element with subtree children to its left.
     */
    public void testElementSubtreeLeft()
    {
        Node startContainer = getDocument().createDivElement();
        Node xText = getDocument().createTextNode("xw");
        Node wikiText = getDocument().createTextNode("ikio");
        Node us = getDocument().createComment("us");
        DivElement cargo = getDocument().createDivElement();
        startContainer.appendChild(cargo);
        cargo.appendChild(wikiText);
        cargo.appendChild(us);
        startContainer.appendChild(xText);
        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(cargo);
        expected.add(wikiText);
        expected.add(us);
        expected.add(xText);

        assertSame(expected, getDocument().getIterator(startContainer));
    }

    /**
     * Test that an iterator over a node with element with subtree children to its left.
     */
    public void testElementSubtreeMiddle()
    {
        Node startContainer = getDocument().createDivElement();
        Node aliceText = getDocument().createTextNode("alice");
        Node bobText = getDocument().createTextNode("bob");
        Node come = getDocument().createComment("come");
        DivElement cargo = getDocument().createDivElement();
        startContainer.appendChild(come);
        startContainer.appendChild(cargo);
        cargo.appendChild(bobText);
        startContainer.appendChild(aliceText);
        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected = new ArrayList<Node>();
        expected.add(startContainer);
        expected.add(come);
        expected.add(cargo);
        expected.add(bobText);
        expected.add(aliceText);

        assertSame(expected, getDocument().getIterator(startContainer));
    }

    /**
     * Test an iterator over an enclosing element of a wikilink.
     */
    public void testWikiLinkSubtree()
    {
        Node startContainer = getDocument().createDivElement();
        Node preambleText = getDocument().createTextNode("our");
        startContainer.appendChild(preambleText);
        Node startWikiLinkComment = getDocument().createComment("startwikilink:Ref.erence");
        startContainer.appendChild(startWikiLinkComment);
        SpanElement wrappingSpan = getDocument().createSpanElement();
        startContainer.appendChild(wrappingSpan);
        AnchorElement anchor = getDocument().createAnchorElement();
        wrappingSpan.appendChild(anchor);
        Node labelPreamble = getDocument().createTextNode("x");
        anchor.appendChild(labelPreamble);
        Element boldWiki = getDocument().createElement("strong");
        anchor.appendChild(boldWiki);
        Node labelBoldWiki = getDocument().createTextNode("wiki");
        boldWiki.appendChild(labelBoldWiki);
        Node stopWikiLinkComment = getDocument().createComment("stopwikilink");
        startContainer.appendChild(stopWikiLinkComment);
        Node endText = getDocument().createTextNode("rox");
        startContainer.appendChild(endText);

        getContainer().appendChild(startContainer);

        // setup expected
        List<Node> expected =
            Arrays.asList(new Node[] {startContainer, preambleText, startWikiLinkComment, wrappingSpan, anchor,
                labelPreamble, boldWiki, labelBoldWiki, stopWikiLinkComment, endText});

        assertSame(expected, getDocument().getIterator(startContainer));
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
