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
package com.xpn.xwiki.wysiwyg.client.selection.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.util.DocumentFragment;

public class IERange extends AbstractRange
{
    IERange(JavaScriptObject jsRange)
    {
        super(jsRange);
    }

    public DocumentFragment cloneContents()
    {
        return null;
    }

    public Range cloneRange()
    {
        return null;
    }

    public void collapse(boolean toStart)
    {
    }

    protected short compareBoundaryPoints(int how, JavaScriptObject sourceRange)
    {
        return 0;
    }

    public void deleteContents()
    {
    }

    public void detach()
    {
    }

    public DocumentFragment extractContents()
    {
        return null;
    }

    public Node getCommonAncestorContainer()
    {
        return null;
    }

    public Node getEndContainer()
    {
        return null;
    }

    public int getEndOffset()
    {
        return 0;
    }

    public Node getStartContainer()
    {
        return null;
    }

    public int getStartOffset()
    {
        return 0;
    }

    public void insertNode(Node newNode)
    {
    }

    public boolean isCollapsed()
    {
        return false;
    }

    public void selectNode(Node refNode)
    {
    }

    public void selectNodeContents(Node refNode)
    {
    }

    public void setEnd(Node refNode, int offset)
    {
    }

    public void setEndAfter(Node refNode)
    {
    }

    public void setEndBefore(Node refNode)
    {
    }

    public void setStart(Node refNode, int offset)
    {
    }

    public void setStartAfter(Node refNode)
    {
    }

    public void setStartBefore(Node refNode)
    {
    }

    public void surroundContents(Node newParent)
    {
    }

    public String toString()
    {
        return super.toString();
    }
}
