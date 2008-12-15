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
package com.xpn.xwiki.wysiwyg.client.plugin.image;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Element;

/**
 * Tests the {@link ImageMetaDataExtractor} class to check that image HTML blocks are parsed correctly.
 * 
 * @version $Id$
 */
public class ImageMetaDataExtractorTest extends AbstractWysiwygClientTest
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
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        container = ((Document) Document.get()).xCreateDivElement().cast();
        Document.get().getBody().appendChild(container);
        extractor = new ImageMetaDataExtractor();
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
     * Tests whether the {@link ImageMetaDataExtractor#onInnerHTMLChange(com.google.gwt.dom.client.Element)} parses the
     * element content correctly.
     */
    public void testOnInnerHTMLChange()
    {
        String imageInnerHTML = "<!--startimage:Space.Page@my.png--><img src=\"\" /><!--stopimage-->";
        container.setInnerHTML(imageInnerHTML);
        extractor.onInnerHTMLChange(container);
        Element imgElement = (Element) container.getFirstChild();
        // test the inner html left in the container
        String expected = "<img src=\"\">";
        assertEquals(expected, container.getInnerHTML());
        String expectedMeta =
            "<!--startimage:Space.Page@my.png-->" + Element.INNER_HTML_PLACEHOLDER + "<!--stopimage-->";
        // Get Meta data fragment
        DocumentFragment metaFragment = imgElement.getMetaData();
        assertNotNull(metaFragment);
        assertEquals(expectedMeta, metaFragment.getInnerHTML());
    }
}
