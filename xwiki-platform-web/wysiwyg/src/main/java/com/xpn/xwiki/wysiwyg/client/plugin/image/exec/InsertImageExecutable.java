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
package com.xpn.xwiki.wysiwyg.client.plugin.image.exec;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Handles the insertion of an image, passed through its corresponding HTML block.
 * 
 * @version $Id$
 */
public class InsertImageExecutable extends InsertHTMLExecutable
{
    /**
     * Gets the image element in the current selection.
     * 
     * @param rta the rich text area to get the selection from.
     * @return the image element in the current selection, if any or null otherwise.
     */
    private Element getSelectedImage(RichTextArea rta)
    {
        // Check if current selection perfectly wraps an image
        Range currentRange = rta.getDocument().getSelection().getRangeAt(0);
        Node startContainer = currentRange.getStartContainer();
        Node endContainer = currentRange.getEndContainer();

        if (startContainer == endContainer && startContainer.getNodeType() == Node.ELEMENT_NODE
            && (currentRange.getEndOffset() - currentRange.getStartOffset() == 1)) {
            // Check that the node inside is an image
            Node nodeInside = startContainer.getChildNodes().getItem(currentRange.getStartOffset());
            if (nodeInside.getNodeType() == Node.ELEMENT_NODE && nodeInside.getNodeName().equalsIgnoreCase("img")) {
                return (Element) nodeInside;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return getSelectedImage(rta) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        Element selectedImageElement = getSelectedImage(rta);
        if (selectedImageElement == null) {
            return null;
        }
        // Get the image reference
        DocumentFragment imageMetaData = selectedImageElement.getMetaData();
        if (imageMetaData == null) {
            return null;
        }
        Node startComment = imageMetaData.getChildNodes().getItem(0);
        if (startComment.getNodeType() != DOMUtils.COMMENT_NODE
            || !startComment.getNodeValue().startsWith("startimage:")) {
            return null;
        }

        // parse image and return stuff.
        ImageConfig config = new ImageConfig();
        parseImageReference(startComment.getNodeValue().substring(11), config);
        parseImageAttributes(selectedImageElement, config);
        return config.toJSON();
    }

    /**
     * Parses an image reference, obtaining its source (wiki, space, page, filename).
     * 
     * @param reference image reference
     * @param config the {@link ImageConfig} where to store the obtained information
     */
    private void parseImageReference(String reference, ImageConfig config)
    {
        String[] fileParts = reference.split("@");
        String wikiRef = null;
        if (fileParts.length >= 2) {
            config.setImageFileName(fileParts[1]);
            wikiRef = fileParts[0];
        } else {
            config.setImageFileName(reference);
            // Nothing left to parse, return
            return;
        }
        // Now parse what's left
        String[] wikiParts = wikiRef.split(":");
        String spaceReference = wikiRef;
        if (wikiParts.length >= 2) {
            config.setWiki(wikiParts[0]);
            spaceReference = wikiParts[1];
        }
        String[] spaceParts = spaceReference.split("\\.");
        String pageReference = spaceReference;
        if (spaceParts.length >= 2) {
            config.setSpace(spaceParts[0]);
            pageReference = spaceParts[1];
        }
        config.setPage(pageReference);
    }

    /**
     * Parses the image attributes to re-compose the Image configuration.
     * 
     * @param img the image element to get the attributes from
     * @param config the {@link ImageConfig} object in which to store found values
     */
    private void parseImageAttributes(Element img, ImageConfig config)
    {
        String widthName = "width";
        String heightName = "height";
        String widthValue = img.getStyle().getProperty(widthName);
        if (!StringUtils.isEmpty(widthValue)) {
            config.setWidth(widthValue);
        }
        String heightValue = img.getStyle().getProperty(heightName);
        if (!StringUtils.isEmpty(heightValue)) {
            config.setHeight(heightValue);
        }
        String altAttr = img.xGetAttribute("alt");
        if (altAttr != null && altAttr.trim().length() != 0) {
            config.setAltText(altAttr);
        }
        // search for width and height in attributes, if none were set
        if (config.getHeight() == null && config.getWidth() == null) {
            if (!StringUtils.isEmpty(img.xGetAttribute(widthName))) {
                config.setWidth(img.xGetAttribute(widthName));
            }
            if (!StringUtils.isEmpty(img.xGetAttribute(heightName))) {
                config.setHeight(img.xGetAttribute(heightName));
            }
        }
        ImageAlignment alignment = parseImageAlignment(img);
        if (alignment != null) {
            config.setAlignment(alignment);
        }
    }

    /**
     * Parses the style attribute of the image to re-compose the alignment for the image.
     * 
     * @return the found alignment, if there is any or null otherwise.
     * @param img the image to parse the alignment for
     */
    private ImageAlignment parseImageAlignment(Element img)
    {
        ImageAlignment foundAlignment = null;
        // Try to get the float of this element, either as "styleFloat" or as "cssFloat", to make sure we get it from
        // all browsers (IE uses "styleFloat")
        // cssFloat is the name of the float property in CSS2, it seems
        String floatValue = img.getStyle().getProperty("cssFloat");
        if (StringUtils.isEmpty(floatValue)) {
            floatValue = img.getStyle().getProperty("styleFloat");
        }
        if ("left".equalsIgnoreCase(floatValue)) {
            foundAlignment = ImageAlignment.LEFT;
        }
        if ("right".equalsIgnoreCase(floatValue)) {
            foundAlignment = ImageAlignment.RIGHT;
        }
        String autoValue = "auto";
        if ("block".equalsIgnoreCase(img.getStyle().getProperty("display"))
            && autoValue.equalsIgnoreCase(img.getStyle().getProperty(
                com.xpn.xwiki.wysiwyg.client.dom.Style.toCamelCase("margin-left")))
            && autoValue.equalsIgnoreCase(img.getStyle().getProperty(
                com.xpn.xwiki.wysiwyg.client.dom.Style.toCamelCase("margin-right")))) {
            foundAlignment = ImageAlignment.CENTER;
        }
        String verticalAlignValue =
            img.getStyle().getProperty(com.xpn.xwiki.wysiwyg.client.dom.Style.toCamelCase("vertical-align"));
        if ("top".equalsIgnoreCase(verticalAlignValue)) {
            foundAlignment = ImageAlignment.TOP;
        }
        if ("bottom".equalsIgnoreCase(verticalAlignValue)) {
            foundAlignment = ImageAlignment.BOTTOM;
        }
        if ("middle".equalsIgnoreCase(verticalAlignValue)) {
            foundAlignment = ImageAlignment.MIDDLE;
        }
        return foundAlignment;
    }
}
