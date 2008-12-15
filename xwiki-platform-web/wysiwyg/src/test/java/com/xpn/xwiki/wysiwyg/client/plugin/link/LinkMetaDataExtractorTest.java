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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Element;

/**
 * Class to test the {@link LinkMetaDataExtractor} class, to check whether all links are correctly parsed and extracted
 * in meta data fragments.
 * 
 * @version $Id$
 */
public class LinkMetaDataExtractorTest extends AbstractWysiwygClientTest
{
    /**
     * The DOM element in which we run the tests.
     */
    private Element container;

    /**
     * The {@link LinkMetaDataExtractor} to test.
     */
    private LinkMetaDataExtractor extractor;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        container = ((Document) Document.get()).xCreateDivElement().cast();
        Document.get().getBody().appendChild(container);
        extractor = new LinkMetaDataExtractor();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtTearDown()
     */
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
        container.setInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the inner html left in the container
        String expected = "<a href=\"/xwiki/bin/view/Space/ExistingPage\">label</a>";
        assertEquals(expected, container.getInnerHTML());
        String expectedMeta =
            "<!--startwikilink:Space.ExistingPage--><span class=\"wikilink\">" + Element.INNER_HTML_PLACEHOLDER
                + "</span><!--stopwikilink-->";
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        assertEquals(expectedMeta, metaFragment.getInnerHTML());
    }

    /**
     * Tests whether the {@link LinkMetaDataExtractor} detects and parses a link an external resource.
     */
    public void testExternalLink()
    {
        String linkInnerHTML =
            "<!--startwikilink:http://xwiki.org--><span class=\"wikiexternallink\">"
                + "<a href=\"http://xwiki.org\">label</a></span><!--stopwikilink-->";
        container.setInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the inner html left in the container
        String expected = "<a href=\"http://xwiki.org\">label</a>";
        assertEquals(expected, container.getInnerHTML());
        String expectedMeta =
            "<!--startwikilink:http://xwiki.org--><span class=\"wikiexternallink\">" + Element.INNER_HTML_PLACEHOLDER
                + "</span><!--stopwikilink-->";
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        assertEquals(expectedMeta, metaFragment.getInnerHTML());
    }

    /**
     * Tests whether the {@link LinkMetaDataExtractor} detects and parses a link to an internal new page.
     */
    public void testWikiNewLink()
    {
        String linkInnerHTML =
            "<!--startwikilink:Space.Page--><span class=\"wikicreatelink\">"
                + "<a href=\"/xwiki/bin/view/Space/Page\">label</a></span><!--stopwikilink-->";
        container.setInnerHTML(linkInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element anchorElement = (Element) container.getFirstChild();
        // test the inner html left in the container
        String expected = "<a href=\"/xwiki/bin/view/Space/Page\">label</a>";
        assertEquals(expected, container.getInnerHTML());
        String expectedMeta =
            "<!--startwikilink:Space.Page--><span class=\"wikicreatelink\">" + Element.INNER_HTML_PLACEHOLDER
                + "</span><!--stopwikilink-->";
        // Get Meta data fragment
        DocumentFragment metaFragment = anchorElement.getMetaData();
        assertNotNull(metaFragment);
        assertEquals(expectedMeta, metaFragment.getInnerHTML());
    }
}
