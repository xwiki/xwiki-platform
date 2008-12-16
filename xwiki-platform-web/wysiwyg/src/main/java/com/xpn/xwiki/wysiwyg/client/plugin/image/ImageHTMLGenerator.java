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
     * Get an HTML image block for an image attached to the current document, that is, referred only through its
     * filename.
     * 
     * @param imageConfig the image description, through its filename and url.
     * @param currentWiki name of the wiki of the edited document
     * @param currentSpace name of the space of the edited document
     * @param currentPage name of the page of the edited document
     * @return the HTML block for the passed image.
     */
    public String getAttachedImageHTML(ImageConfig imageConfig, String currentWiki, String currentSpace,
        String currentPage)
    {
        String imageReference = getImageReference(imageConfig, currentWiki, currentSpace, currentPage);
        String styleAttribute = imageConfig.getAlignment() != null ? getAlignmentStyle(imageConfig.getAlignment()) : "";
        return "<!--startimage:" + imageReference + "--><img src=\"" + imageConfig.getImageURL() + "\" alt=\""
            + ((imageConfig.getAltText() != null) ? imageConfig.getAltText() : imageConfig.getImageFileName()) + "\" "
            + ((styleAttribute.length() > 0) ? "style=\"" + styleAttribute + "\" " : "")
            + ((imageConfig.getWidth() != null) ? "width=\"" + imageConfig.getWidth() + "\" " : "")
            + ((imageConfig.getHeight() != null) ? "height=\"" + imageConfig.getHeight() + "\" " : "")
            + "/><!--stopimage-->";
    }

    /**
     * Generates a new image reference based on the image configuration data and the current context.
     * 
     * @param imageConfig image data
     * @param currentWiki the current wiki name
     * @param currentSpace the space name for the current document
     * @param currentPage the page name for the current document
     * @return the image reference for this image
     */
    private String getImageReference(ImageConfig imageConfig, String currentWiki, String currentSpace,
        String currentPage)
    {
        String imageReference = imageConfig.getImageFileName();
        if (!StringUtils.isEmpty(imageConfig.getSpace()) && !StringUtils.isEmpty(imageConfig.getPage())) {
            // the image has page and space set, check if they are not the current page space, wiki
            if (!imageConfig.getSpace().equals(currentSpace) || !imageConfig.getPage().equals(currentPage)
                || (!StringUtils.isEmpty(imageConfig.getWiki()) && !imageConfig.getWiki().equals(currentWiki))) {
                // the space / page / wiki are different from current, generate full ref
                imageReference = imageConfig.getSpace() + "." + imageConfig.getPage() + "@" + imageReference;
                if (!StringUtils.isEmpty(imageConfig.getWiki())) {
                    imageReference = imageConfig.getWiki() + ":" + imageReference;
                }
            }
        }

        return imageReference;
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
