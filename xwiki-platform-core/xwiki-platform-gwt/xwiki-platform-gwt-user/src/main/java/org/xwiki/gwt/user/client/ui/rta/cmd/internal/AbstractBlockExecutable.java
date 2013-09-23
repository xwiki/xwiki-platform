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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.SelectionPreserver;

import com.google.gwt.dom.client.Node;

/**
 * Abstract executable that saves the current selection, iterates over all the leaf nodes touched by the current
 * selection and processes the block-level element ancestors of those leafs, and finally restores the selection.
 * Concrete implementations of this class have some peculiarities:
 * <ul>
 * <li>They don't distinguish between an empty selection (only the caret) and a non-empty selection</li>
 * <li>They don't distinguish between the first or last leaf touched by the current selection and the rest of the leafs;
 * the first and the last leaf can be partially included in the selection</li>
 * <li>When they insert a block they add to the block all the in-line nodes around the insertion point; the is very
 * important because the selection markers have to be included in the inserted block and not wrap it</li>
 * </ul>
 * 
 * @version $Id$
 */
public abstract class AbstractBlockExecutable extends AbstractSelectionExecutable
{
    /**
     * The object used to preserve the selection in the underlying rich text area.
     */
    private final SelectionPreserver preserver;

    /**
     * Creates a new block executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public AbstractBlockExecutable(RichTextArea rta)
    {
        super(rta);
        preserver = new SelectionPreserver(rta);
    }

    @Override
    public boolean execute(String parameter)
    {
        preserver.saveSelection();

        Selection selection = rta.getDocument().getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            execute(selection.getRangeAt(i), parameter);
        }

        preserver.restoreSelection();
        return true;
    }

    /**
     * Processes the given range.
     * 
     * @param range the range to be processed
     * @param parameter the execution parameter
     */
    protected void execute(Range range, String parameter)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        if (leaf == null) {
            // The range is collapsed between nodes.
            execute(range.getStartContainer(), range.getStartOffset(), range.getStartOffset(), parameter);
        } else {
            Node lastLeaf = domUtils.getLastLeaf(range);
            execute(range, leaf, parameter);
            while (leaf != lastLeaf) {
                leaf = domUtils.getNextLeaf(leaf);
                execute(range, leaf, parameter);
            }
        }
    }

    /**
     * Processed the given node.
     * 
     * @param range the range that touches the given node
     * @param leaf the DOM node to be processed
     * @param parameter the execution parameter
     */
    protected void execute(Range range, Node leaf, String parameter)
    {
        if (leaf == range.getStartContainer()) {
            if (leaf == range.getEndContainer()) {
                execute(leaf, range.getStartOffset(), range.getEndOffset(), parameter);
            } else {
                execute(leaf, range.getStartOffset(), domUtils.getLength(leaf), parameter);
            }
        } else if (leaf == range.getEndContainer()) {
            execute(leaf, 0, range.getEndOffset(), parameter);
        } else {
            execute(leaf, 0, domUtils.getLength(leaf), parameter);
        }
    }

    /**
     * Processes the given node between the specified offsets. For a text node the offset if a position between its
     * characters; for an element the offset is a position between its child nodes.
     * 
     * @param node the DOM node to be processed
     * @param startOffset the start offset within the give node
     * @param endOffset the end offset within the given node
     * @param parameter the execution parameter
     */
    protected abstract void execute(Node node, int startOffset, int endOffset, String parameter);

    @Override
    public String getParameter()
    {
        Selection selection = rta.getDocument().getSelection();
        String selectionParameter = null;
        for (int i = 0; i < selection.getRangeCount(); i++) {
            String rangeParameter = getParameter(selection.getRangeAt(i));
            if (rangeParameter == null || (selectionParameter != null && !selectionParameter.equals(rangeParameter))) {
                return null;
            }
            selectionParameter = rangeParameter;
        }
        return selectionParameter;
    }

    /**
     * @param range the DOM range from which to extract the execution parameter
     * @return the execution parameter if this executable has been called on the given range, {@code null} otherwise
     */
    protected String getParameter(Range range)
    {
        Node leaf = domUtils.getFirstLeaf(range);
        if (leaf == null) {
            return getParameter(range.getStartContainer());
        }
        String rangeParameter = getParameter(leaf);
        Node lastLeaf = domUtils.getLastLeaf(range);
        while (leaf != lastLeaf) {
            leaf = domUtils.getNextLeaf(leaf);
            String leafParameter = getParameter(leaf);
            if (rangeParameter == null) {
                rangeParameter = leafParameter;
            } else if (leafParameter != null && !leafParameter.equals(rangeParameter)) {
                return null;
            }
        }
        return rangeParameter;
    }

    /**
     * @param node a DOM node
     * @return the execution parameter if this executable has been called on the given node, {@code null} otherwise
     */
    protected abstract String getParameter(Node node);

    /**
     * Wraps all in-line child nodes of the given parent node, whose indexes are around the specified offset. The
     * element used to wrap the in-line contents has the given tag name.
     * 
     * @param parent The node whose in-line contents are wrapped in a block level element.
     * @param offset The offset within the given node, around which in-line contents are looked for.
     * @param tagName The name of the element used to wrap the in-line contents.
     * @return the inserted element
     */
    protected Element wrap(Element parent, int offset, String tagName)
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
        Element element = (Element) parent.getOwnerDocument().createElement(tagName);
        for (int i = startIndex; i < endIndex; i++) {
            element.appendChild(parent.getChildNodes().getItem(startIndex));
        }
        domUtils.insertAt(parent, element, startIndex);
        return element;
    }
}
