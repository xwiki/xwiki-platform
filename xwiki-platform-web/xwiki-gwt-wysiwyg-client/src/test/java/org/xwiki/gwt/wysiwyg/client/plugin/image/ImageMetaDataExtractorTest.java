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
package org.xwiki.gwt.wysiwyg.client.plugin.image;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

import com.google.gwt.dom.client.Node;

/**
 * Tests the {@link ImageMetaDataExtractor} class to check that image HTML blocks are parsed correctly.
 * 
 * @version $Id$
 */
public class ImageMetaDataExtractorTest extends WysiwygTestCase
{
    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    /**
     * The {@link ImageMetaDataExtractor} to test.
     */
    private ImageMetaDataExtractor extractor;

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        container = Document.get().createDivElement().cast();
        Document.get().getBody().appendChild(container);
        extractor = new ImageMetaDataExtractor();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygTestCase#gwtTearDown()
     */
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();
        container.getParentNode().removeChild(container);
    }

    /**
     * Tests whether the {@link ImageMetaDataExtractor#onInnerHTMLChange(com.google.gwt.dom.client.Element)} parses the
     * element content correctly.
     */
    public void testOnInnerHTMLChange()
    {
        String imageInnerHTML = "<!--startimage:Space.Page@my.png--><img/><!--stopimage-->";
        container.xSetInnerHTML(imageInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element imgElement = (Element) container.getFirstChild();
        // test the elements left in the container.
        // We test the elements and not the inner html because the IE returns modified html (resolved links, non quoted
        // attributes and the string test would fail)
        assertEquals(1, container.getChildNodes().getLength());
        assertEquals("img", container.getChildNodes().getItem(0).getNodeName().toLowerCase());
        // Get Meta data fragment
        DocumentFragment metaFragment = imgElement.getMetaData();
        assertNotNull(metaFragment);
        // test the elements in the metaFragment
        assertEquals(3, metaFragment.getChildNodes().getLength());
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(0).getNodeType());
        assertEquals("startimage:Space.Page@my.png", metaFragment.getChildNodes().getItem(0).getNodeValue());
        assertEquals(Node.TEXT_NODE, metaFragment.getChildNodes().getItem(1).getNodeType());
        assertEquals(Element.INNER_HTML_PLACEHOLDER, metaFragment.getChildNodes().getItem(1).getNodeValue());
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(2).getNodeType());
        assertEquals("stopimage", metaFragment.getChildNodes().getItem(2).getNodeValue());
    }
}
