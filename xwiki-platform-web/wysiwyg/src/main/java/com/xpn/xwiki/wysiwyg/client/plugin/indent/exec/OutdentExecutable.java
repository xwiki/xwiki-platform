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

import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Outdent executable to handle valid XHTML lists outdent, semantically: when a list item is outdented, all its subitems
 * are outdented as well.
 * 
 * @version $Id$
 */
public class OutdentExecutable extends AbstractListExecutable
{
    /**
     * Line break tag, to help manage empty list items after outdenting sublists.
     */
    protected static final String LINE_BREAK_TAG = "br";

    /**
     * {@inheritDoc}
     * 
     * @see AbstractListExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        // find enclosing list item
        Element listItem = getListItem(rta);
        if (listItem == null) {
            return false;
        }
        // look for the "parent" list
        Element parentList = (Element) listItem.getParentNode();
        if (parentList == null) {
            return false;
        }

        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // check if this item is a first level item or inside a sublist
        Element parentListItem = (Element) parentList.getParentNode();
        if (parentListItem != null && parentListItem.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
            outdentItem(listItem, parentList, parentListItem);
        } else {
            outdentFirstLevelItem(listItem, parentList);
        }

        // restore selection
        rta.getDocument().getSelection().removeAllRanges();
        rta.getDocument().getSelection().addRange(range);
        return true;
    }

    /**
     * Actually executes the outdent on {@code listItem}, with respect to the list it takes part of.
     * 
     * @param listItem the list item to outdent
     * @param parentList the list in which it is placed
     * @param parentListItem the list item in which this sublist is placed
     */
    public void outdentItem(Element listItem, Element parentList, Element parentListItem)
    {
        // split the sublist in which this item is into 2 lists: one to stay in this parent, the other under outdented
        // list item
        Element newList = (Element) domUtils.splitNode(parentList, domUtils.getNodeIndex(listItem) + 1);
        // and pull it out of its parent
        parentListItem.removeChild(newList);
        // put the newList as a sublist of the current item: append its elements to the list on the last position there,
        // if any, or append it all as a child to the item.
        Node lastChild = listItem.getLastChild();
        if (newList.hasChildNodes()) {
            if (isList(lastChild)) {
                // append all items in the list to this item
                for (int i = 0; i < newList.getChildNodes().getLength(); i++) {
                    lastChild.appendChild(newList.getChildNodes().getItem(i));
                }
            } else {
                // add the new list as is
                listItem.appendChild(newList);
            }
        }
        // move the list item as a next sibling of the parent list item
        domUtils.insertAfter(listItem, parentListItem);
        // check if the list in the parent needs to be removed because it is left empty
        if (!parentList.hasChildNodes()) {
            // remove it from its parent
            // remove the line break first, if any, and then the sublist
            if (parentList.getPreviousSibling().getNodeName().equalsIgnoreCase(LINE_BREAK_TAG)) {
                parentListItem.removeChild(parentList.getPreviousSibling());
            }
            parentListItem.removeChild(parentList);
        }
    }

    /**
     * Outdents an element on the first level of the list, by splitting the list in two, and leaving the list item's
     * content as text between the lists.
     * 
     * @param listItem the list item that needs to be unindented
     * @param parentList the parent list of this item
     */
    protected void outdentFirstLevelItem(Element listItem, Element parentList)
    {
        // split the list in two, using the listItem as separator
        Element newList = (Element) domUtils.splitNode(parentList, domUtils.getNodeIndex(listItem) + 1);
        // remove the separator
        parentList.removeChild(listItem);
        // extract the list item and insert it after the parentList
        DocumentFragment extractedListItem = listItem.extractContents();
        domUtils.insertAfter(extractedListItem, parentList);
        // remove the parentList and/or the newList if they are empty
        if (!parentList.hasChildNodes()) {
            parentList.getParentNode().removeChild(parentList);
        }

        if (!newList.hasChildNodes()) {
            newList.getParentNode().removeChild(newList);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractListExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        if (!super.isEnabled(rta)) {
            return false;
        }
        // find parent list item
        Element listItem = getListItem(rta);
        if (listItem == null) {
            return false;
        }
        // look for the "parent" list item
        Element parentList = (Element) listItem.getParentNode();
        if (parentList == null) {
            return false;
        }
        return true;
    }
}
