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
package org.xwiki.gwt.wysiwyg.client.plugin.format.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractBlockExecutable;

import com.google.gwt.dom.client.Node;

/**
 * Wraps the HTML fragment including the current selection in a specified block level element.
 * 
 * @version $Id$
 */
public class FormatBlockExecutable extends AbstractBlockExecutable
{
    /**
     * Create a new executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public FormatBlockExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Formats as block the in-line neighborhood of the given node, using the specified block tag. If the specified tag
     * name is empty then the block format is removed (in-line formatting will be used instead).
     * 
     * @see AbstractBlockExecutable#execute(Node, int, int, String)
     */
    protected void execute(Node node, int startOffset, int endOffset, String tagName)
    {
        Node ancestor = node;
        int index = startOffset;
        if (domUtils.isInline(node)) {
            ancestor = domUtils.getFarthestInlineAncestor(node);
            index = domUtils.getNodeIndex(ancestor);
            ancestor = ancestor.getParentNode();
        }

        if (domUtils.isFlowContainer(ancestor)) {
            // Currently we have in-line formatting.
            if (tagName.length() > 0) {
                wrap((Element) ancestor, index, tagName);
            }
        } else if (domUtils.isBlockLevelInlineContainer(ancestor)) {
            // Currently we have block formatting.
            if (tagName.length() == 0) {
                Element.as(ancestor).unwrap();
            } else if (!tagName.equalsIgnoreCase(ancestor.getNodeName())) {
                replace((Element) ancestor, tagName);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the tag used for block formatting. If the returned string is empty it means there's no block formatting
     * (in other words, in-line formatting). If the returned string is null it means the given node doesn't support
     * block formatting.
     * 
     * @see AbstractBlockExecutable#getParameter(Node)
     */
    protected String getParameter(Node node)
    {
        Node target = domUtils.getFarthestInlineAncestor(node);
        if (target == null) {
            target = node;
        } else {
            target = target.getParentNode();
        }
        if (domUtils.isFlowContainer(target)) {
            return "";
        } else if (domUtils.isBlockLevelInlineContainer(target)) {
            return target.getNodeName().toLowerCase();
        } else {
            return null;
        }
    }

    /**
     * Replaces the given element with an element with the specified tag name, moving all the child nodes to the new
     * element.
     * 
     * @param element the element to be replaced
     * @param tagName the tag name of the replacing element
     */
    public static void replace(Element element, String tagName)
    {
        // Create a new element with the specified tag name.
        Element replacement = (Element) element.getOwnerDocument().createElement(tagName);
        // Moves all the child nodes of the old element to the new element.
        replacement.appendChild(element.extractContents());
        // If the old element has in-line style, copy it.
        if (element.hasAttribute(Style.STYLE_ATTRIBUTE)) {
            replacement.xSetAttribute(Style.STYLE_ATTRIBUTE, element.xGetAttribute(Style.STYLE_ATTRIBUTE));
        }
        // Replace the old element with the new one.
        if (element.getParentNode() != null) {
            element.getParentNode().replaceChild(replacement, element);
        }
    }
}
