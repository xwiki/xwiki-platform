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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.URLUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigDOMReader;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigDOMWriter;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONSerializer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * Handles the insertion of an image, passed through its corresponding HTML block.
 * 
 * @version $Id$
 */
public class InsertImageExecutable extends AbstractInsertElementExecutable<ImageConfig, ImageElement>
{
    /**
     * Creates a new executable that can be used to insert images in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public InsertImageExecutable(RichTextArea rta)
    {
        super(rta);

        configDOMReader = new ImageConfigDOMReader();
        configDOMWriter = GWT.create(ImageConfigDOMWriter.class);
        configJSONParser = new ImageConfigJSONParser();
        configJSONSerializer = new ImageConfigJSONSerializer();
    }

    @Override
    protected void write(ImageConfig config, ImageElement image)
    {
        // Backup the new image URL.
        String url = config.getUrl();
        boolean internal = !StringUtils.areEqual(config.getReference(), url);
        if (internal) {
            // We keep the current image URL because we want to resize the current image before requesting the new
            // image. This way we can use the computed width/height to adjust the URL of the new image so that it is
            // resized on the server.
            config.setUrl(image.getSrc());
        }
        super.write(config, image);
        if (internal) {
            // Adjust the new image URL so that the image is resized on the server.
            updateURL(image, url);
        }
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        return InsertImageExecutable.class.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the image element in the current selection.
     * </p>
     * 
     * @see AbstractInsertElementExecutable#getSelectedElement()
     */
    protected ImageElement getSelectedElement()
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

    @Override
    protected ImageElement newElement()
    {
        ImageElement image = rta.getDocument().createImageElement();
        // Ensure the image URL is initially set. Use a blank image just to be able to determine the width and height of
        // the image element in pixels (matching the width/height attributes or the style attribute) before requesting
        // the real image. The computed width and height are used to adjust the real image URL so that the image is
        // resized on the server.
        image.setSrc(GWT.getModuleBaseURL() + "clear.cache.gif");
        return image;
    }

    /**
     * Updates the URL of the given image. If image width and height are specified they are added to the query string in
     * order to resize the image on the server side.
     * 
     * @param image the image whose URL is updated
     * @param url the new image URL
     */
    private void updateURL(ImageElement image, String url)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        // Put width and height information in the query string in order to resize images on the server side.
        Map<String, List<String>> parameters = URLUtils.parseQueryString(URLUtils.getQueryString(url));
        parameters.remove(Style.WIDTH);
        parameters.remove(Style.HEIGHT);
        parameters.remove("keepAspectRatio");
        if (isDimensionSpecified(image, Style.WIDTH)) {
            parameters.put(Style.WIDTH, Arrays.asList(new String[] {String.valueOf(width)}));
            if (isDimensionSpecified(image, Style.HEIGHT)) {
                parameters.put(Style.HEIGHT, Arrays.asList(new String[] {String.valueOf(height)}));
            }
        } else {
            // Width is unspecified.
            if (isDimensionSpecified(image, Style.HEIGHT)) {
                parameters.put(Style.HEIGHT, Arrays.asList(new String[] {String.valueOf(height)}));
            } else {
                // If image width and height are unspecified limit the image width to fit the rich text area (leaving
                // space for the vertical scroll bar).
                int widthLimit = image.getOwnerDocument().getClientWidth() - 22;
                if (widthLimit > 0) {
                    parameters.put(Style.WIDTH, Arrays.asList(new String[] {String.valueOf(widthLimit)}));
                }
            }
        }

        // Update the image URL.
        image.setSrc(URLUtils.setQueryString(url, parameters));
    }

    /**
     * @param image an image element
     * @param dimension either {@code width} or {@code height}
     * @return {@code true} if the specified dimension is explicitly set on the given image element
     */
    private boolean isDimensionSpecified(ImageElement image, String dimension)
    {
        return image.getPropertyInt(dimension) > 0
            && (!StringUtils.isEmpty(image.getStyle().getProperty(dimension)) || image.hasAttribute(dimension));
    }
}
