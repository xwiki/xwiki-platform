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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;

/**
 * Unit tests for {@link ImageConfigHTMLSerializer} and {@link ImageConfigHTMLParser}.
 * 
 * @version $Id$
 */
public class ImageConfigHTMLSerializerTest extends WysiwygTestCase
{
    /**
     * The object used to extract image configuration from an image element.
     */
    private final ImageConfigHTMLParser imageConfigHTMLParser = new ImageConfigHTMLParser();

    /**
     * The object used to serialize image configuration to HTML.
     */
    private final ImageConfigHTMLSerializer imageConfigHTMLSerializer = new ImageConfigHTMLSerializer();

    /**
     * The object used to check if two {@link ImageConfig} instances are equal.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * Tests if {@link ImageConfigHTMLParser} and {@link ImageConfigHTMLSerializer} are compatible.
     */
    public void testParseAndSerialize()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setReference("XWiki.AdminSheet@general.png");
        imageConfig.setImageURL("http://www.xwiki.org/missing.png");
        imageConfig.setAltText("A missing image.");
        imageConfig.setAlignment(ImageAlignment.TOP);
        imageConfig.setWidth("50");
        imageConfig.setHeight("30");

        Element container = Element.as(Document.get().createDivElement());
        container.xSetInnerHTML(imageConfigHTMLSerializer.serialize(imageConfig));
        ImageElement image = (ImageElement) container.getChildNodes().getItem(1);

        // Manually extract the meta data.
        container.replaceChild(Document.get().createTextNode(Element.INNER_HTML_PLACEHOLDER), image);
        Element.as(image).setAttribute(Element.META_DATA_ATTR, container.getInnerHTML());

        assertEquals(imageConfigJSONSerializer.serialize(imageConfig), imageConfigJSONSerializer
            .serialize(imageConfigHTMLParser.parse(image)));
    }

    /**
     * Image dimensions should go in the {@code width} and {@code height} attributes if the unit is pixel or percent.
     * Otherwise the dimension must go in the style attribute.
     */
    public void testSerializeImageDimension()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setWidth("20.5%");
        imageConfig.setHeight("13em");

        Element container = Element.as(Document.get().createDivElement());
        container.xSetInnerHTML(imageConfigHTMLSerializer.serialize(imageConfig));
        ImageElement image = (ImageElement) container.getChildNodes().getItem(1);

        assertEquals(imageConfig.getWidth(), image.getAttribute(Style.WIDTH));
        assertEquals(imageConfig.getHeight(), image.getStyle().getHeight());
    }

    /**
     * Images must always specify an alternative text.
     */
    public void testAlternativeTextIsSpecified()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setReference("Space.Page@photo.png");

        Element container = Element.as(Document.get().createDivElement());
        container.xSetInnerHTML(imageConfigHTMLSerializer.serialize(imageConfig));
        ImageElement image = (ImageElement) container.getChildNodes().getItem(1);

        assertTrue(image.hasAttribute("alt"));
    }
}
