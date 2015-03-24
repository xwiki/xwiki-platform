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

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.EscapeUtils;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigDOMWriter;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig.ImageAlignment;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;

/**
 * Writes an {@link ImageConfig} object to a DOM element.
 * 
 * @version $Id$
 */
public class ImageConfigDOMWriter implements ConfigDOMWriter<ImageConfig, ImageElement>
{
    /**
     * Regular expression used to determine if a string can be used as the value of the {@code width} and {@code height}
     * image attributes. These attributes accept only values that are measured in pixels and percent. If the unit is not
     * specified pixels are implied. If the value uses a different unit then it will be set using the {@code style}
     * attribute instead.
     */
    private static final String PIXELS_OR_PERCENT = "^\\+?[1-9]\\d*(\\.\\d+)?(px|%)?$";

    /**
     * The object used to read the image alignment.
     * 
     * @see #updateAlignment(ImageElement, ImageAlignment)
     */
    private final ImageConfigDOMReader configDOMReader = new ImageConfigDOMReader();

    /**
     * {@inheritDoc}
     * 
     * @see ConfigDOMWriter#write(Object, com.google.gwt.dom.client.Element)
     */
    public void write(ImageConfig imageConfig, ImageElement image)
    {
        // Required attributes.
        updateMetaData(image, imageConfig.getReference());
        image.setSrc(imageConfig.getUrl());
        image.setAlt(imageConfig.getAltText());
        // Optional attributes.
        updateDimension(image, Style.WIDTH, imageConfig.getWidth());
        updateDimension(image, Style.HEIGHT, imageConfig.getHeight());
        updateAlignment(image, imageConfig.getAlignment());
    }

    /**
     * Updates the meta data of the given element.
     * 
     * @param image the image whose meta data will be updated
     * @param reference the new image reference
     */
    private void updateMetaData(ImageElement image, String reference)
    {
        Document document = (Document) image.getOwnerDocument();
        DocumentFragment metaData = document.createDocumentFragment();
        metaData.appendChild(document.createComment(EscapeUtils.escapeComment("startimage:" + reference)));
        metaData.appendChild(document.createTextNode(Element.INNER_HTML_PLACEHOLDER));
        metaData.appendChild(document.createComment("stopimage"));
        Element.as(image).setMetaData(metaData);
    }

    /**
     * Updates the specified dimension of the given image.
     * 
     * @param image the image whose dimension is updated
     * @param dimension the dimension to be updated, i.e. {@link Style#WIDTH} or {@link Style#HEIGHT}
     * @param value the new dimension value
     */
    protected void updateDimension(ImageElement image, String dimension, String value)
    {
        if (StringUtils.isEmpty(value)) {
            image.removeAttribute(dimension);
            image.getStyle().clearProperty(dimension);
        } else if (value.matches(PIXELS_OR_PERCENT)) {
            String computedValue = image.getPropertyString(dimension);
            if (!value.equals(computedValue) && !value.equals(computedValue + Unit.PX.getType())) {
                image.setAttribute(dimension, value);
                image.getStyle().clearProperty(dimension);
            }
        } else {
            image.removeAttribute(dimension);
            image.getStyle().setProperty(dimension, value);
        }
    }

    /**
     * Updates the alignment styles on the given image.
     * 
     * @param image the image whose alignment will be updated
     * @param alignment the new alignment
     */
    private void updateAlignment(ImageElement image, ImageAlignment alignment)
    {
        ImageAlignment currentAlignment = configDOMReader.readImageAlignment(image);
        if (currentAlignment != null && (alignment == null || !alignment.equals(currentAlignment))) {
            removeImageAlignment(image, currentAlignment);
        }
        if (alignment != null) {
            addImageAlignment(image, alignment);
        }
    }

    /**
     * Removes the alignment styles from the given image.
     * 
     * @param image the to remove the alignment style from
     * @param alignment specifies which alignment styles to remove
     */
    private void removeImageAlignment(ImageElement image, ImageAlignment alignment)
    {
        switch (alignment) {
            case TOP:
            case MIDDLE:
            case BOTTOM:
                image.getStyle().clearProperty(Style.VERTICAL_ALIGN.getJSName());
                break;
            case LEFT:
            case RIGHT:
                image.getStyle().clearFloat();
                break;
            case CENTER:
                image.getStyle().clearDisplay();
                image.getStyle().clearMarginLeft();
                image.getStyle().clearMarginRight();
                break;
            default:
                break;
        }
    }

    /**
     * Adds the specified alignment styles to the given image.
     * 
     * @param image the image to add the alignment to
     * @param alignment the new alignment
     */
    private void addImageAlignment(ImageElement image, ImageAlignment alignment)
    {
        switch (alignment) {
            case TOP:
                image.getStyle().setVerticalAlign(VerticalAlign.TOP);
                break;
            case MIDDLE:
                image.getStyle().setVerticalAlign(VerticalAlign.MIDDLE);
                break;
            case BOTTOM:
                image.getStyle().setVerticalAlign(VerticalAlign.BOTTOM);
                break;
            case LEFT:
                image.getStyle().setFloat(com.google.gwt.dom.client.Style.Float.LEFT);
                break;
            case RIGHT:
                image.getStyle().setFloat(com.google.gwt.dom.client.Style.Float.RIGHT);
                break;
            case CENTER:
                image.getStyle().setDisplay(Display.BLOCK);
                image.getStyle().setProperty(Style.MARGIN_LEFT.getJSName(), Style.Margin.AUTO);
                image.getStyle().setProperty(Style.MARGIN_RIGHT.getJSName(), Style.Margin.AUTO);
                break;
            default:
                break;
        }
    }
}
