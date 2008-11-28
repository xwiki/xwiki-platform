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

import com.xpn.xwiki.wysiwyg.client.plugin.image.ui.HasImage;

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
     * @param image the image description, through its filename and url.
     * @return the HTML block for the passed image.
     */
    public String getAttachedImageHTML(HasImage image)
    {
        return "<!--startimage:" + image.getImageFileName() + "--><img src=\"" + image.getImageURL() + "\" alt=\""
            + image.getImageFileName() + "\"/><!--stopimage-->";
    }
}
