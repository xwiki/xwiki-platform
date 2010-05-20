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
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.EscapeUtils;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigDOMReader;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * Creates an {@link ImageConfig} object from an {@link ImageElement}.
 * 
 * @version $Id$
 */
public class ImageConfigDOMReader implements ConfigDOMReader<ImageConfig, ImageElement>
{
    /**
     * The prefix of the start image comment text.
     */
    private static final String START_IMAGE = "startimage:";

    /**
     * {@inheritDoc}
     * 
     * @see ConfigDOMReader#read(com.google.gwt.dom.client.Element)
     */
    public ImageConfig read(ImageElement image)
    {
        ImageConfig config = new ImageConfig();
        config.setReference(readReference(image));
        config.setUrl(image.getSrc());

        // Image width priority: style attribute > width attribute > width property
        String width = image.getStyle().getWidth();
        if (StringUtils.isEmpty(width)) {
            width = image.getAttribute(Style.WIDTH);
            if (StringUtils.isEmpty(width)) {
                width = String.valueOf(image.getWidth());
            }
        }
        config.setWidth(width);

        // Image height priority: style attribute > height attribute > height property
        String height = image.getStyle().getHeight();
        if (StringUtils.isEmpty(height)) {
            height = image.getAttribute(Style.HEIGHT);
            if (StringUtils.isEmpty(height)) {
                height = String.valueOf(image.getHeight());
            }
        }
        config.setHeight(height);

        config.setAltText(image.getAlt());
        config.setAlignment(readImageAlignment(image));

        return config;
    }

    /**
     * Extracts the image reference from the image meta data.
     * 
     * @param image an image element
     * @return the reference of the given image, if specified in its meta data, {@code null} otherwise
     */
    public String readReference(ImageElement image)
    {
        DocumentFragment metaData = Element.as(image).getMetaData();
        if (metaData == null) {
            return null;
        }
        Node startComment = metaData.getFirstChild();
        if (startComment == null || startComment.getNodeType() != DOMUtils.COMMENT_NODE
            || !startComment.getNodeValue().startsWith(START_IMAGE)) {
            return null;
        }
        return EscapeUtils.unescapeBackslash(startComment.getNodeValue().substring(START_IMAGE.length()));
    }

    /**
     * Parses the style attribute of the given image to determine its {@link ImageAlignment}.
     * 
     * @param image the image to parse the alignment for
     * @return the determined alignment, if there is any or {@code null} otherwise
     */
    public ImageAlignment readImageAlignment(ImageElement image)
    {
        try {
            return ImageAlignment.valueOf(image.getStyle().getVerticalAlign().toUpperCase());
        } catch (Exception e) {
            try {
                return ImageAlignment.valueOf(image.getStyle().getProperty(Style.FLOAT.getJSName()).toUpperCase());
            } catch (Exception f) {
                String display = image.getStyle().getDisplay();
                String marginLeft = image.getStyle().getMarginLeft();
                String marginRight = image.getStyle().getMarginRight();
                if (Style.Display.BLOCK.equalsIgnoreCase(display) && Style.Margin.AUTO.equalsIgnoreCase(marginLeft)
                    && Style.Margin.AUTO.equalsIgnoreCase(marginRight)) {
                    return ImageAlignment.CENTER;
                }
                return null;
            }
        }
    }
}
