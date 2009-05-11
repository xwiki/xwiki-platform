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
package org.xwiki.rendering.renderer.xhtml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.ImageType;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Simple implementation that considers image sources as external URLs.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class SimpleXHTMLImageRenderer implements XHTMLImageRenderer
{
    /**
     * @see #setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    private XHTMLWikiPrinter xhtmlPrinter;

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLImageRenderer#setXHTMLWikiPrinter(XHTMLWikiPrinter)
     */
    public void setXHTMLWikiPrinter(XHTMLWikiPrinter printer)
    {
        this.xhtmlPrinter = printer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XHTMLImageRenderer#onImage(Image, boolean, Map)
     */
    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // We only handle image with URL source.
        if (image.getType() == ImageType.URL) {
            String imageURL = ((URLImage) image).getURL();

            // Then add it as an attribute of the IMG element.
            Map<String, String> attributes = new LinkedHashMap<String, String>();
            attributes.put(SRC, imageURL);

            // Add the other parameters as attributes
            attributes.putAll(parameters);

            // If not ALT attribute has been specified, add it since the XHTML specifications makes it mandatory.
            if (!parameters.containsKey(ALTERNATE)) {
                attributes.put(ALTERNATE, image.getName());
            }

            this.xhtmlPrinter.printXMLElement(IMG, attributes);
        }
    }
}
