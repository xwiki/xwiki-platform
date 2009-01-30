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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;

/**
 * Wraps the HTML fragment including the current selection in a specified block level element.
 * 
 * @version $Id$
 */
public class FormatBlockExecutable extends AbstractExecutable
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        SelectionPreserver preserver = new SelectionPreserver(rta);
        preserver.saveSelection();

        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            execute(selection.getRangeAt(i), param);
        }

        preserver.restoreSelection();
        return true;
    }

    /**
     * Format as block the in-line contents of the given range, using the specified block tag. If the specified tag name
     * is empty then the block format is removed (in-line formatting will be used instead).
     * 
     * @param range The range whose in-line contents will be formatted using the given tag.
     * @param tagName The tag used for block formatting the in-line contents of the given range.
     */
    protected void execute(Range range, String tagName)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        if (leaf == null) {
            execute(range.getStartContainer(), range.getStartOffset(), tagName);
        } else {
            Node lastLeaf = domUtils.getLastLeaf(range);
            execute(leaf, tagName);
            while (leaf != lastLeaf) {
                leaf = domUtils.getNextLeaf(leaf);
                execute(leaf, tagName);
            }
        }
    }

    /**
     * Formats as block the in-line neighborhood of the given node, using the specified block tag. If the specified tag
     * name is empty then the block format is removed (in-line formatting will be used instead).
     * 
     * @param node A DOM node.
     * @param tagName The tag used for block formatting the in-line neighborhood of the given node.
     */
    protected void execute(Node node, String tagName)
    {
        execute(Element.as(node.getParentNode()), domUtils.getNodeIndex(node), tagName);
    }

    /**
     * Formats as block the in-line neighborhood of a node specified by its parent and its child index, using the
     * specified block tag. If the specified tag name is empty then the block format is removed (in-line formatting will
     * be used instead).
     * 
     * @param parent The parent of the in-line DOM nodes that will be grouped in a block node.
     * @param offset Specifies the offset within the given parent node where to look for an in-line neighborhood.
     * @param tagName The tag used for block formatting the in-line neighborhood of the specified node.
     */
    protected void execute(Node parent, int offset, String tagName)
    {
        Node ancestor = parent;
        int index = offset;
        if (domUtils.isInline(parent)) {
            ancestor = domUtils.getFarthestInlineAncestor(parent);
            index = domUtils.getNodeIndex(ancestor);
            ancestor = ancestor.getParentNode();
        }

        if (domUtils.isFlowContainer(ancestor)) {
            // Currently we have in-line formatting.
            if (tagName.length() > 0) {
                wrap(ancestor, index, tagName);
            }
        } else if (domUtils.isBlockLevelInlineContainer(ancestor)) {
            // Currently we have block formatting.
            if (tagName.length() == 0) {
                Element.as(ancestor).unwrap();
            } else if (!tagName.equalsIgnoreCase(ancestor.getNodeName())) {
                replace(ancestor, tagName);
            }
        }
    }

    /**
     * Wraps all in-line child nodes of the given parent node, whose indexes are around the specified offset. The
     * element used to wrap the in-line contents has the given tag name.
     * 
     * @param parent The node whose in-line contents are wrapped in a block level element.
     * @param offset The offset within the given node, around which in-line contents are looked for.
     * @param tagName The name of the element used to wrap the in-line contents.
     */
    protected void wrap(Node parent, int offset, String tagName)
    {
        int startIndex = offset;
        while (startIndex > 0 && domUtils.isInline(parent.getChildNodes().getItem(startIndex - 1))) {
            startIndex--;
        }
        int endIndex = offset;
        while (endIndex < parent.getChildNodes().getLength()
            && domUtils.isInline(parent.getChildNodes().getItem(endIndex))) {
            endIndex++;
        }
        Element element = ((Document) parent.getOwnerDocument()).xCreateElement(tagName);
        for (int i = startIndex; i < endIndex; i++) {
            element.appendChild(parent.getChildNodes().getItem(startIndex));
        }
        domUtils.insertAt(parent, element, startIndex);
    }

    /**
     * Replaces the given node with an element with the specified tag name, moving all the child nodes to the new
     * element.
     * 
     * @param node The node to be replaced.
     * @param tagName The name of the element that will replace the given node.
     */
    protected void replace(Node node, String tagName)
    {
        Element element = ((Document) node.getOwnerDocument()).xCreateElement(tagName);
        Node child = node.getFirstChild();
        while (child != null) {
            element.appendChild(child);
            child = node.getFirstChild();
        }
        node.getParentNode().replaceChild(element, node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        Selection selection = rta.getDocument().getSelection();
        String selectionFormat = null;
        for (int i = 0; i < selection.getRangeCount(); i++) {
            String rangeFormat = getFormat(selection.getRangeAt(i));
            if (rangeFormat == null || (selectionFormat != null && !selectionFormat.equals(rangeFormat))) {
                return null;
            }
            selectionFormat = rangeFormat;
        }
        return selectionFormat;
    }

    /**
     * @param range A DOM range.
     * @return the tag used for block formatting all the in-line content included in the given range. If the returned
     *         string is empty it means there's no block formatting (in other words, in-line formatting). If the
     *         returned string is null it means there are many types of block formatting used in the given range.
     */
    protected String getFormat(Range range)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        if (leaf == null) {
            return getFormat(range.getStartContainer());
        }
        String rangeFormat = getFormat(leaf);
        Node lastLeaf = domUtils.getLastLeaf(range);
        while (leaf != lastLeaf) {
            leaf = domUtils.getNextLeaf(leaf);
            String leafFormat = getFormat(leaf);
            if (rangeFormat == null) {
                rangeFormat = leafFormat;
            } else if (leafFormat != null && !leafFormat.equals(rangeFormat)) {
                return null;
            }
        }
        return rangeFormat;
    }

    /**
     * @param node A DOM node.
     * @return the tag used for block formatting. If the returned string is empty it means there's no block formatting
     *         (in other words, in-line formatting). If the returned string is null it means the given node doesn't
     *         support block formatting.
     */
    protected String getFormat(Node node)
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
}
