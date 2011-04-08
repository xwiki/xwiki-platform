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
package org.xwiki.gwt.wysiwyg.client.plugin.line;

import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;

/**
 * Mozilla specific implementation of the {@link LinePlugin}.
 * 
 * @version $Id$
 */
public class MozillaLinePlugin extends LinePlugin
{

    /**
     * {@inheritDoc}<br/>
     * We overwrite in order to fix a Mozilla bug which causes the caret to be rendered on the same line after you press
     * Enter, if the new line doesn't have any visible contents. Once you start typing the caret moves below, but it
     * looks strange before you type. We fixed the bug by adding a BR at the end of the new line.
     * 
     * @see LinePlugin#insertLineBreak(Node, Range)
     */
    protected void insertLineBreak(Node container, Range caret)
    {
        super.insertLineBreak(container, caret);

        // Start container should be a text node.
        Node lastLeaf;
        Node leaf = caret.getStartContainer();
        // Look if there is any visible element on the new line, taking care to remain in the current block container.
        do {
            if (needsSpace(leaf)) {
                return;
            }
            lastLeaf = leaf;
            leaf = domUtils.getNextLeaf(leaf);
        } while (leaf != null && container == domUtils.getNearestBlockContainer(leaf));

        // It seems there's no visible element on the new line. We should add a spacer up in the tree.
        Node ancestor = lastLeaf;
        while (ancestor.getParentNode() != container && ancestor.getNextSibling() == null) {
            ancestor = ancestor.getParentNode();
        }
        domUtils.insertAfter(getTextArea().getDocument().createBRElement(), ancestor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We overwrite in order to fix a Firefox 3.6 bug which causes the caret to be rendered at the start of the document
     * when we split a line at its end. It seems Firefox 3.6 doesn't like the fact that the caret ends up inside an
     * empty text node when the line is split at the end. This wan't the case with older versions of Firefox.
     * 
     * @see LinePlugin#splitLine(Node, Range)
     */
    @Override
    protected void splitLine(Node container, Range caret)
    {
        super.splitLine(container, caret);

        Node start = caret.getStartContainer();
        // Firefox 3.6 renders the caret badly when we place it inside an empty text node that is the first child of a
        // block level element, such as a paragraph. Strangely, if the empty text node is wrapped by an in-line element
        // like SPAN or STRONG the caret is displayed in the correct position. The caret is also displayed correctly if
        // we remove the empty text node.
        if (start.getNodeType() == Node.TEXT_NODE && start.getNodeValue().length() == 0) {
            caret.setStartBefore(start);
            start.getParentNode().removeChild(start);
        }
    }
}
