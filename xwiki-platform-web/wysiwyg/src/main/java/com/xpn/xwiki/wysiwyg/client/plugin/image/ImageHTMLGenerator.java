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

import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Generates an HTML block for an image, given by it's URL and filename.
 * 
 * @version $Id$
 */
public final class ImageHTMLGenerator
{
    /**
     * The singleton instance of this class.
     */
    private static ImageHTMLGenerator instance;

    /**
     * Class constructor, private so that the class is a singleton.
     */
    private ImageHTMLGenerator()
    {
    }

    /**
     * @return the instance of this class.
     */
    public static synchronized ImageHTMLGenerator getInstance()
    {
        if (instance == null) {
            instance = new ImageHTMLGenerator();
        }
        return instance;
    }

    /**
     * Get an HTML image block for an image.
     * 
     * @param imageConfig the image configuration object
     * @return the HTML block for the passed image.
     */
    public String getAttachedImageHTML(ImageConfig imageConfig)
    {
        String imageReference = imageConfig.getReference();
        String styleAttribute = imageConfig.getAlignment() != null ? getAlignmentStyle(imageConfig.getAlignment()) : "";
        StringBuffer imageHTML = new StringBuffer();
        imageHTML.append("<!--startimage:");
        imageHTML.append(imageReference);
        imageHTML.append("--><img src=\"");
        imageHTML.append(imageConfig.getImageURL());
        imageHTML.append("\" alt=\"");
        String altText = imageConfig.getAltText();
        if (StringUtils.isEmpty(altText)) {
            ResourceName r = new ResourceName();
            r.fromString(imageConfig.getReference(), true);
            altText = r.getFile();
        }
        imageHTML.append(altText + "\" ");
        imageHTML.append(((styleAttribute.length() > 0) ? "style=\"" + styleAttribute + "\" " : ""));
        imageHTML.append((!StringUtils.isEmpty(imageConfig.getWidth()) ? "width=\"" + imageConfig.getWidth() + "\" "
            : ""));
        imageHTML.append((!StringUtils.isEmpty(imageConfig.getHeight()) ? "height=\"" + imageConfig.getHeight() + "\" "
            : ""));
        imageHTML.append("/><!--stopimage-->");

        return imageHTML.toString();
    }

    /**
     * Builds the style attribute for an image so that it matches the passed alignment.
     * 
     * @param alignment the alignment to create in the style attribute.
     * @return the style attribute value for this image so that it meets the specified alignment.
     */
    private String getAlignmentStyle(ImageAlignment alignment)
    {
        String styleAttribute = "";
        switch (alignment) {
            case LEFT:
                styleAttribute = "float: left; margin-right: 1em;";
                break;
            case RIGHT:
                styleAttribute = "float: right; margin-left: 1em;";
                break;
            case CENTER:
                styleAttribute = "margin-right: auto; margin-left: auto; display: block;";
                break;
            case TOP:
                styleAttribute = "vertical-align: top;";
                break;
            case MIDDLE:
                styleAttribute = "vertical-align: middle;";
                break;
            case BOTTOM:
                styleAttribute = "vertical-align: bottom;";
                break;
            default:
                break;
        }

        return styleAttribute;
    }
}
