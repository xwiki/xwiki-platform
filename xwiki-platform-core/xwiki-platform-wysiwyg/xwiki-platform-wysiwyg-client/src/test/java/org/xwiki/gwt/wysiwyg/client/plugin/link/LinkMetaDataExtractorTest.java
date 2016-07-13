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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

import com.google.gwt.dom.client.Node;

/**
 * Class to test the {@link LinkMetaDataExtractor} class, to check whether all links are correctly parsed and extracted
 * in meta data fragments.
 * 
 * @version $Id$
 */
public class LinkMetaDataExtractorTest extends WysiwygTestCase
{
    /**
     * The name of the anchor tag.
     */
    private static final String ANCHOR_TAG_NAME = "a";

    /**
     * The name of the span tag.
     */
    private static final String SPAN_TAG_NAME = "span";

    /**
     * The content of the end wiki link marker comment.
     */
    private static final String STOPWIKILINK_COMMENT = "stopwikilink";

    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    /**
     * The {@link LinkMetaDataExtractor} to test.
     */
    private LinkMetaDataExtractor extractor;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        container = Document.get().createDivElement().cast();
        Document.get().getBody().appendChild(container);
        extractor = new LinkMetaDataExtractor();
    }

    @Override
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();
        container.getParentNode().removeChild(container);
    }

    /**
     * Tests whether the {@link LinkMetaDataExtractor} detects and parses a link to an internal existing page.
     */
    public void testWikiLink()
    {
        String linkInnerHTML =
            "<!--startwikilink:Space.ExistingPage--><span class=\"wikilink\">"
                + "<a href=\"/xwiki/bin/view/Space/ExistingPage\">label</a></span><!--stopwikilink-->";
        container.xSetInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the elements left in the container
        assertEquals(1, container.getChildNodes().getLength());
        assertEquals(ANCHOR_TAG_NAME, container.getChildNodes().getItem(0).getNodeName().toLowerCase());
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        assertEquals(3, metaFragment.getChildNodes().getLength());
        // Check the three nodes in the meta fragment and the content
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(0).getNodeType());
        assertEquals("startwikilink:Space.ExistingPage", metaFragment.getChildNodes().getItem(0).getNodeValue());
        assertEquals(Node.ELEMENT_NODE, metaFragment.getChildNodes().getItem(1).getNodeType());
        assertEquals(SPAN_TAG_NAME, metaFragment.getChildNodes().getItem(1).getNodeName().toLowerCase());
        assertEquals(1, metaFragment.getChildNodes().getItem(1).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0).getNodeType());
        assertEquals(Element.INNER_HTML_PLACEHOLDER, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0)
            .getNodeValue());
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(2).getNodeType());
        assertEquals(STOPWIKILINK_COMMENT, metaFragment.getChildNodes().getItem(2).getNodeValue());
    }

    /**
     * Tests whether the {@link LinkMetaDataExtractor} detects and parses a link an external resource.
     */
    public void testExternalLink()
    {
        String linkInnerHTML =
            "<!--startwikilink:http://xwiki.org--><span class=\"wikiexternallink\">"
                + "<a href=\"http://xwiki.org\">label</a></span><!--stopwikilink-->";
        container.xSetInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the elements left in the container
        assertEquals(1, container.getChildNodes().getLength());
        assertEquals(ANCHOR_TAG_NAME, container.getChildNodes().getItem(0).getNodeName().toLowerCase());
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        // test the meta data fragment
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(0).getNodeType());
        assertEquals("startwikilink:http://xwiki.org", metaFragment.getChildNodes().getItem(0).getNodeValue());
        assertEquals(Node.ELEMENT_NODE, metaFragment.getChildNodes().getItem(1).getNodeType());
        assertEquals(SPAN_TAG_NAME, metaFragment.getChildNodes().getItem(1).getNodeName().toLowerCase());
        assertEquals(1, metaFragment.getChildNodes().getItem(1).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0).getNodeType());
        assertEquals(Element.INNER_HTML_PLACEHOLDER, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0)
            .getNodeValue());
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(2).getNodeType());
        assertEquals(STOPWIKILINK_COMMENT, metaFragment.getChildNodes().getItem(2).getNodeValue());
    }

    /**
     * Tests whether the {@link LinkMetaDataExtractor} detects and parses a link to an internal new page.
     */
    public void testWikiNewLink()
    {
        String linkInnerHTML =
            "<!--startwikilink:Space.Page--><span class=\"wikicreatelink\">"
                + "<a href=\"/xwiki/bin/view/Space/Page\">label</a></span><!--stopwikilink-->";
        container.xSetInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the elements left in the container
        assertEquals(1, container.getChildNodes().getLength());
        assertEquals(ANCHOR_TAG_NAME, container.getChildNodes().getItem(0).getNodeName().toLowerCase());
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        // test the meta data fragment
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(0).getNodeType());
        assertEquals("startwikilink:Space.Page", metaFragment.getChildNodes().getItem(0).getNodeValue());
        assertEquals(Node.ELEMENT_NODE, metaFragment.getChildNodes().getItem(1).getNodeType());
        assertEquals(SPAN_TAG_NAME, metaFragment.getChildNodes().getItem(1).getNodeName().toLowerCase());
        assertEquals(1, metaFragment.getChildNodes().getItem(1).getChildNodes().getLength());
        assertEquals(Node.TEXT_NODE, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0).getNodeType());
        assertEquals(Element.INNER_HTML_PLACEHOLDER, metaFragment.getChildNodes().getItem(1).getChildNodes().getItem(0)
            .getNodeValue());
        assertEquals(DOMUtils.COMMENT_NODE, metaFragment.getChildNodes().getItem(2).getNodeType());
        assertEquals(STOPWIKILINK_COMMENT, metaFragment.getChildNodes().getItem(2).getNodeValue());
    }
}
