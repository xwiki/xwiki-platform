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
package org.xwiki.gwt.wysiwyg.client.plugin.image.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.Cache.CacheCallback;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertHTMLExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigHTMLParser;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigHTMLSerializer;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONSerializer;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Unit;

/**
 * Handles the insertion of an image, passed through its corresponding HTML block.
 * 
 * @version $Id$
 */
public class InsertImageExecutable extends InsertHTMLExecutable
{
    /**
     * The object used to extract an {@link ImageConfig} from an {@link ImageElement}.
     */
    private final ImageConfigHTMLParser imageConfigHTMLParser = new ImageConfigHTMLParser();

    /**
     * The object used to serialize an {@link ImageConfig} to HTML.
     */
    private final ImageConfigHTMLSerializer imageConfigHTMLSerializer = new ImageConfigHTMLSerializer();

    /**
     * The object used to serialize an {@link ImageConfig} instance to JSON.
     */
    private final ImageConfigJSONSerializer imageConfigJSONSerializer = new ImageConfigJSONSerializer();

    /**
     * The object used to create an {@link ImageConfig} from JSON.
     */
    private final ImageConfigJSONParser imageConfigJSONParser = new ImageConfigJSONParser();

    /**
     * Creates a new executable that can be used to insert images in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public InsertImageExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#execute(String)
     */
    @Override
    public boolean execute(String imageJSON)
    {
        String imageHTML = imageConfigHTMLSerializer.serialize(imageConfigJSONParser.parse(imageJSON));
        ImageElement image = getSelectedImage();
        if (image == null) {
            // Insert a new image.
            return super.execute(imageHTML);
        } else {
            // Overwrite an existing image.
            Element container = Element.as(rta.getDocument().createDivElement());
            // Inner HTML listeners have to be notified in order to extract the image meta data.
            container.xSetInnerHTML(imageHTML);
            merge(image, (ImageElement) container.getFirstChild());
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isExecuted()
     */
    public boolean isExecuted()
    {
        return cache.get(InsertImageExecutable.class.getName() + "#executed", new CacheCallback<Boolean>()
        {
            public Boolean get()
            {
                return getSelectedImage() != null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter()
     */
    public String getParameter()
    {
        ImageElement selectedImageElement = getSelectedImage();
        if (selectedImageElement == null) {
            return null;
        }
        ImageConfig config = imageConfigHTMLParser.parse(selectedImageElement);
        return imageConfigJSONSerializer.serialize(config);
    }

    /**
     * Gets the image element in the current selection.
     * 
     * @return the image element in the current selection, if any or {@code null} otherwise
     */
    private ImageElement getSelectedImage()
    {
        // Check if the current selection perfectly wraps an image.
        // We expect the selection to have at least one range, otherwise this executable wouldn't be enabled.
        Range currentRange = rta.getDocument().getSelection().getRangeAt(0);
        Node startContainer = currentRange.getStartContainer();
        Node endContainer = currentRange.getEndContainer();

        if (startContainer == endContainer && startContainer.getNodeType() == Node.ELEMENT_NODE
            && (currentRange.getEndOffset() - currentRange.getStartOffset() == 1)) {
            // Check that the node inside is an image.
            Node nodeInside = startContainer.getChildNodes().getItem(currentRange.getStartOffset());
            if (nodeInside.getNodeType() == Node.ELEMENT_NODE && nodeInside.getNodeName().equalsIgnoreCase("img")) {
                return (ImageElement) nodeInside;
            }
        }
        return null;
    }

    /**
     * Merges the given image elements.
     * 
     * @param target the image that will be updated
     * @param source the image providing the change
     */
    private void merge(ImageElement target, ImageElement source)
    {
        // Remove redundant attributes.
        adjustDimension(target, source, Style.WIDTH);
        adjustDimension(target, source, Style.HEIGHT);
        // Merge complex attributes.
        // Merge class name attribute.
        if (!target.getClassName().equals(source.getClassName())) {
            String[] targetClassNames = target.getClassName().split("\\s+");
            for (int i = 0; i < targetClassNames.length; i++) {
                source.addClassName(targetClassNames[i]);
            }
        }
        // Merge style attribute.
        if (!Element.as(target).xGetAttribute(Style.STYLE_ATTRIBUTE).equals(
            Element.as(source).xGetAttribute(Style.STYLE_ATTRIBUTE))) {
            extend((Style) source.getStyle(), (Style) target.getStyle());
        }
        // Update all attributes.
        JsArrayString attributeNames = Element.as(source).getAttributeNames();
        for (int i = 0; i < attributeNames.length(); i++) {
            String newValue = Element.as(source).xGetAttribute(attributeNames.get(i));
            Element.as(target).xSetAttribute(attributeNames.get(i), newValue);
        }
    }

    /**
     * Adjusts the specified dimension of the target image based on the source image.
     * 
     * @param target the image whose dimension is being adjusted
     * @param source the image providing the new value for the specified dimension
     * @param dimension the dimension to adjust, either {@link Style#WIDTH} or {@link Style#HEIGHT}
     */
    private void adjustDimension(ImageElement target, ImageElement source, String dimension)
    {
        if (source.hasAttribute(dimension)) {
            // Keep the specified dimension only if it's different than the computed dimension.
            String specifiedValue = source.getAttribute(dimension);
            String computedValue = target.getPropertyString(dimension);
            if (specifiedValue.equals(computedValue) || specifiedValue.equals(computedValue + Unit.PX.getType())) {
                source.removeAttribute(dimension);
            }
        } else {
            // Restore the dimension of the target image to its default value if the dimension of the source image isn't
            // specified.
            target.removeAttribute(dimension);
        }
    }

    /**
     * Copies the properties from the source style to the target style only they are not defined in the target style.
     * 
     * @param target the style to be extended
     * @param source the extension source
     */
    private native void extend(Style target, Style source)
    /*-{
        for(propertyName in source) {
            if ('' + source[propertyName] != '' && '' + target[propertyName] == '') {
                target[propertyName] = source[propertyName];
            }
        }
    }-*/;
}
