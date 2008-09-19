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

public final class IESelection extends AbstractSelection
{
    IESelection(JavaScriptObject jsSelection)
    {
        super(jsSelection);
    }

    protected void addRange(JavaScriptObject range)
    {
    }

    public void collapse(Node parentNode, int offset)
    {
    }

    public void collapseToEnd()
    {
    }

    public void collapseToStart()
    {
    }

    public boolean containsNode(Node node, boolean partlyContained)
    {
        return false;
    }

    public void deleteFromDocument()
    {
    }

    public void extend(Node parentNode, int offset)
    {
    }

    public Node getAnchorNode()
    {
        return null;
    }

    public int getAnchorOffset()
    {
        return 0;
    }

    public Node getFocusNode()
    {
        return null;
    }

    public int getFocusOffset()
    {
        return 0;
    }

    public Range getRangeAt(int index)
    {
        return null;
    }

    public int getRangeCount()
    {
        return 0;
    }

    public boolean isCollapsed()
    {
        return false;
    }

    public void removeAllRanges()
    {
    }

    protected void removeRange(JavaScriptObject range)
    {
    }

    public void selectAllChildren(Node parentNode)
    {
    }

    public void selectionLanguageChange(boolean langRTL)
    {
    }
}
