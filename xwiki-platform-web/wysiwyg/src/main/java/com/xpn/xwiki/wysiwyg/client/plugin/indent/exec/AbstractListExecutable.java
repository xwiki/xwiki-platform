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
package com.xpn.xwiki.wysiwyg.client.plugin.indent.exec;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Superclass for all the list executables, such as indent and outdent executables, to handle frequent list operations.
 * 
 * @version $Id$
 */
public abstract class AbstractListExecutable extends AbstractExecutable
{
    /**
     * List item element name.
     */
    protected static final String LIST_ITEM_TAG = "li";

    /**
     * Unordered list element name.
     */
    protected static final String UNORDERED_LIST_TAG = "ul";

    /**
     * Ordered list element name.
     */
    protected static final String ORDERED_LIST_TAG = "ol";

    /**
     * @param rta the {@link RichTextArea} for which the selection is checked
     * @return the list item in which the selection is positioned currently, or null if no such thing exists.
     */
    protected Element getListItem(RichTextArea rta)
    {
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        return (Element) domUtils.getFirstAncestor(range.getCommonAncestorContainer(), LIST_ITEM_TAG);
    }

    /**
     * Checks if the passed node is a list node: either an ordered list or an unordered one. If the passed node is null,
     * false is returned.
     * 
     * @param node the node to check if it's a list or not
     * @return true if the passed node is a list node, false otherwise (including the case when the node is null).
     */
    protected boolean isList(Node node)
    {
        return node != null
            && (node.getNodeName().equalsIgnoreCase(ORDERED_LIST_TAG) || node.getNodeName().equalsIgnoreCase(
                UNORDERED_LIST_TAG));
    }
}
