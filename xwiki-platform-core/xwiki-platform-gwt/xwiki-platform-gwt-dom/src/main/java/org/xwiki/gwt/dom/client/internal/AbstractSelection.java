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
package org.xwiki.gwt.dom.client.internal;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;

/**
 * Abstract {@link Selection} implementation.
 * <p>
 * NOTE: In the current implementation we often make the assumption that the selection contains at most one range. All
 * the other ranges, if they exist, are sometimes ignored. Additionally, but somehow as a consequence, we consider the
 * anchor node as being the start container of the first range and the focus node as the end container of the first
 * range. This has to do with the fact that not all the browsers distinguish the direction in which the user makes the
 * selection (from left to right or the opposite).
 * 
 * @version $Id$
 */
public abstract class AbstractSelection implements Selection
{
    @Override
    public void collapse(Node parentNode, int offset)
    {
        Range range = ((Document) parentNode.getOwnerDocument()).createRange();
        range.setStart(parentNode, offset);
        range.collapse(true);
        removeAllRanges();
        addRange(range);
    }

    @Override
    public void collapseToEnd()
    {
        collapse(false);
    }

    @Override
    public void collapseToStart()
    {
        collapse(true);
    }

    /**
     * Collapses this selection to the specified end point.
     * 
     * @param toStart whether to collapse to the start or to the end point of the first range in this selection
     */
    private void collapse(boolean toStart)
    {
        if (getRangeCount() > 0) {
            Range range = getRangeAt(0);
            range.collapse(toStart);
            removeAllRanges();
            addRange(range);
        }
    }

    @Override
    public boolean containsNode(Node node, boolean partlyContained)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteFromDocument()
    {
        if (getRangeCount() > 0) {
            Range range = getRangeAt(0);
            range.deleteContents();
            removeAllRanges();
            addRange(range);
        }
    }

    @Override
    public void extend(Node parentNode, int offset)
    {
        if (getRangeCount() > 0) {
            Range range = getRangeAt(0);
            range.setEnd(parentNode, offset);
            removeAllRanges();
            addRange(range);
        }
    }

    @Override
    public Node getAnchorNode()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).getStartContainer();
        } else {
            return null;
        }
    }

    @Override
    public int getAnchorOffset()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).getStartOffset();
        } else {
            return -1;
        }
    }

    @Override
    public Node getFocusNode()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).getEndContainer();
        } else {
            return null;
        }
    }

    @Override
    public int getFocusOffset()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).getEndOffset();
        } else {
            return -1;
        }
    }

    @Override
    public boolean isCollapsed()
    {
        return getRangeCount() == 1 && getRangeAt(0).isCollapsed();
    }

    @Override
    public void selectAllChildren(Node parentNode)
    {
        Range range = ((Document) parentNode.getOwnerDocument()).createRange();
        range.selectNodeContents(parentNode);
        removeAllRanges();
        addRange(range);
    }

    @Override
    public void selectionLanguageChange(boolean langRTL)
    {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        if (getRangeCount() > 0) {
            return getRangeAt(0).toString();
        } else {
            return "";
        }
    }
}
