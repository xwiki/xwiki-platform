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

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Inserts HTML block-level elements in place of the current rich text area selection.
 * 
 * @version $Id$
 */
public class InsertBlockHTMLExecutable extends InsertHTMLExecutable
{
    /**
     * Creates a new instance that can be used to insert HTML block-level elements in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public InsertBlockHTMLExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The given node is most likely a block-level element and thus we need to split the DOM tree up to the nearest flow
     * container starting from the point indicated by the given range.
     * </p>
     * <p>
     * NOTE: The node is not wrapped by the range after the insertion. The range is collapsed and placed in the split
     * point after the inserted node. If you wish to select the inserted node then override this method and select the
     * node after insertion.
     * </p>
     * 
     * @see InsertHTMLExecutable#insertNode(Range, Node)
     */
    @Override
    protected boolean insertNode(Range range, Node node)
    {
        Node start = range.getStartContainer();
        Node flowContainer = domUtils.getNearestFlowContainer(start);
        if (flowContainer == null) {
            return false;
        }

        if (flowContainer == start) {
            range.insertNode(node);
        } else {
            Node startNextLevelSibling = domUtils.splitHTMLNode(flowContainer, start, range.getStartOffset());
            domUtils.insertAfter(node, domUtils.getChild(flowContainer, start));
            // Place the range end in the split point after the inserted node.
            range.setEnd(startNextLevelSibling, 0);
        }
        range.collapse(false);

        return true;
    }
}
