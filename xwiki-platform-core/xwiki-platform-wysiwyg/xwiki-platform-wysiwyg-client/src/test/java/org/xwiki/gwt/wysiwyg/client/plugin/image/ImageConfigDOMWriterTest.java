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

import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Unit;

/**
 * Unit tests for {@link ImageConfigDOMWriter} and {@link ImageConfigDOMReader}.
 * 
 * @version $Id$
 */
public class ImageConfigDOMWriterTest extends WysiwygTestCase
{
    /**
     * The object used to extract image configuration from an image element.
     */
    private final ImageConfigDOMReader imageConfigDOMReader = new ImageConfigDOMReader();

    /**
     * The object used to update an image element from a configuration object.
     */
    private final ImageConfigDOMWriter imageConfigDOMWriter = new ImageConfigDOMWriter();

    /**
     * The object used to check if two {@link ImageConfig} instances are equal.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * Tests if {@link ImageConfigDOMReader} and {@link ImageConfigDOMWriter} are compatible.
     */
    public void testParseAndSerialize()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setReference("XWiki.AdminSheet@general.png");
        imageConfig.setUrl("http://www.xwiki.org/missing.png");
        imageConfig.setAltText("A missing image.");
        imageConfig.setAlignment(ImageAlignment.TOP);
        imageConfig.setWidth("50");
        imageConfig.setHeight("30");

        ImageElement image = Document.get().createImageElement();
        imageConfigDOMWriter.write(imageConfig, image);

        assertEquals(imageConfigJSONSerializer.serialize(imageConfig), imageConfigJSONSerializer
            .serialize(imageConfigDOMReader.read(image)));
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

        ImageElement image = Document.get().createImageElement();
        imageConfigDOMWriter.write(imageConfig, image);

        assertEquals(imageConfig.getWidth(), image.getAttribute(Style.WIDTH));
        assertEquals(imageConfig.getHeight(), image.getStyle().getHeight());
    }

    /**
     * Tests if the image size is properly updated.
     */
    public void testUpdateImageSize()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setWidth("17.5%");
        imageConfig.setHeight("8em");

        ImageElement image = Document.get().createImageElement();
        image.getStyle().setWidth(10, Unit.MM);
        imageConfigDOMWriter.write(imageConfig, image);

        assertEquals(imageConfig.getWidth(), image.getAttribute(Style.WIDTH));
        assertTrue(StringUtils.isEmpty(image.getStyle().getWidth()));
        assertEquals(imageConfig.getHeight(), image.getStyle().getHeight());
    }

    /**
     * Tests if image size is properly removed.
     */
    public void testRemoveImageSize()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setWidth("10cm");

        ImageElement image = Document.get().createImageElement();
        image.setAttribute(Style.WIDTH, "130px");
        image.setAttribute(Style.HEIGHT, "75");
        imageConfigDOMWriter.write(imageConfig, image);

        assertFalse(image.hasAttribute(Style.WIDTH));
        assertFalse(image.hasAttribute(Style.HEIGHT));
        assertEquals(imageConfig.getWidth(), image.getStyle().getWidth());
    }

    /**
     * Tests if the image alignment is properly updated.
     */
    public void testUpdateAlignment()
    {
        ImageConfig imageConfig = new ImageConfig();
        imageConfig.setAlignment(ImageAlignment.TOP);

        ImageElement image = Document.get().createImageElement();
        // Apply left alignment.
        image.getStyle().setFloat(com.google.gwt.dom.client.Style.Float.LEFT);
        // Update the alignment.
        imageConfigDOMWriter.write(imageConfig, image);

        assertEquals(ImageAlignment.TOP, imageConfigDOMReader.read(image).getAlignment());
        assertTrue(StringUtils.isEmpty(image.getStyle().getProperty(Style.FLOAT.getJSName())));
    }
}
