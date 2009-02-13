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
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Indent executable to handle valid XHTML lists indent, semantically: when a list item is indented, all its subitems
 * are indented as well.
 * 
 * @version $Id$
 */
public class IndentExecutable extends AbstractExecutable
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
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        // add the current list item as a child of the previous list item
        Element listItem = getListItem(rta);
        if (listItem == null) {
            return false;
        }        
        // get the previous list item
        Node previousListItem = listItem.getPreviousSibling();
        if (previousListItem == null || !previousListItem.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
            return false;
        }
        // move it, inside the last list of the previous list item
        Node lastChild = previousListItem.getLastChild();
        Element lastSecondLevelList = null;
        if (lastChild != null
            && (lastChild.getNodeName().equalsIgnoreCase(ORDERED_LIST_TAG) || lastChild.getNodeName().equalsIgnoreCase(
                UNORDERED_LIST_TAG))) {
            lastSecondLevelList = (Element) lastChild;
        }
        // if there is no second level list, create a new list and add it to the previous list item
        if (lastSecondLevelList == null) {
            lastSecondLevelList = rta.getDocument().xCreateElement(listItem.getParentNode().getNodeName());
            previousListItem.appendChild(lastSecondLevelList);
        }
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // add the current list item as the last child of the lastSecondLevelList in the previous list item
        lastSecondLevelList.appendChild(listItem);
        rta.getDocument().getSelection().removeAllRanges();
        rta.getDocument().getSelection().addRange(range);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        Element listItem = getListItem(rta);
        if (listItem == null) {
            return false;
        }
        // get previous list item
        return (listItem.getPreviousSibling() != null && listItem.getPreviousSibling().getNodeName().equalsIgnoreCase(
            LIST_ITEM_TAG));
    }

    /**
     * @param rta the {@link RichTextArea} for which the selection is checked
     * @return the list item in which the selection is positioned currently, or null if no such thing exists.
     */
    protected Element getListItem(RichTextArea rta)
    {
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        return (Element) DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), LIST_ITEM_TAG);
    }
}
