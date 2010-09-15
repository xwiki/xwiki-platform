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

import org.xwiki.gwt.dom.client.Range;
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#getCacheKeyPrefix()
     */
    @Override
    protected String getCacheKeyPrefix()
    {
        return InsertImageExecutable.class.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the image element in the current selection.
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#newElement()
     */
    @Override
    protected ImageElement newElement()
    {
        return rta.getDocument().createImageElement();
    }
}
