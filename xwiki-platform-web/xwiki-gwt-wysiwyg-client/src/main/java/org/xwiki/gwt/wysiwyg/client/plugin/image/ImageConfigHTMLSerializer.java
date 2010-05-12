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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigHTMLSerializer;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

/**
 * Serializes an {@link ImageConfig} object to an HTML fragment that can be used to insert an image into the edited
 * document.
 * 
 * @version $Id$
 */
public final class ImageConfigHTMLSerializer implements ConfigHTMLSerializer<ImageConfig>
{
    /**
     * Regular expression used to determine if a string can be used as the value of the {@code width} and {@code height}
     * image attributes. These attributes accept only values that are measured in pixels and percent. If the unit is not
     * specified pixels are implied. If the value uses a different unit then it will be set using the {@code style}
     * attribute instead.
     */
    private static final String PIXELS_OR_PERCENT = "^\\+?[1-9]\\d*(\\.\\d+)?(px|%)?$";

    /**
     * Maps an {@link ImageAlignment} to a CSS string that can be used to enforce that alignment.
     */
    private static final Map<ImageAlignment, String> ALIGNMENT = new HashMap<ImageAlignment, String>();

    static {
        ALIGNMENT.put(ImageAlignment.LEFT, "float: left; margin-right: 1em;");
        ALIGNMENT.put(ImageAlignment.RIGHT, "float: right; margin-left: 1em;");
        ALIGNMENT.put(ImageAlignment.CENTER, "margin-right: auto; margin-left: auto; display: block;");
        ALIGNMENT.put(ImageAlignment.TOP, "vertical-align: top;");
        ALIGNMENT.put(ImageAlignment.MIDDLE, "vertical-align: middle;");
        ALIGNMENT.put(ImageAlignment.BOTTOM, "vertical-align: bottom;");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfigHTMLSerializer#serialize(Object)
     */
    public String serialize(ImageConfig imageConfig)
    {
        StringBuffer imageHTML = new StringBuffer();
        imageHTML.append("<!--startimage:");
        imageHTML.append(imageConfig.getReference());
        imageHTML.append("--><img src=\"");
        imageHTML.append(imageConfig.getUrl());

        imageHTML.append("\" alt=\"");
        String altText = imageConfig.getAltText();
        if (StringUtils.isEmpty(altText)) {
            altText = imageConfig.getReference();
        }
        imageHTML.append(altText + "\" ");

        StringBuffer style = new StringBuffer();
        String alignment = ALIGNMENT.get(imageConfig.getAlignment());
        if (alignment != null) {
            style.append(alignment);
        }

        String width = imageConfig.getWidth();
        if (!StringUtils.isEmpty(width)) {
            if (width.matches(PIXELS_OR_PERCENT)) {
                imageHTML.append("width=\"" + width + "\" ");
            } else {
                style.append("width:" + width + ';');
            }
        }

        String height = imageConfig.getHeight();
        if (!StringUtils.isEmpty(height)) {
            if (height.matches(PIXELS_OR_PERCENT)) {
                imageHTML.append("height=\"" + height + "\" ");
            } else {
                style.append("height:" + height + ';');
            }
        }

        imageHTML.append(style.length() > 0 ? "style=\"" + style + "\" " : "");
        imageHTML.append("/><!--stopimage-->");

        return imageHTML.toString();
    }
}
