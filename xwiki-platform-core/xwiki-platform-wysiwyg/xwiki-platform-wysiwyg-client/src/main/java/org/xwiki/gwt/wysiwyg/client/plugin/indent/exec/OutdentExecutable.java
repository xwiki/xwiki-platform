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
package org.xwiki.gwt.wysiwyg.client.plugin.indent.exec;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

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
     * Creates a new executable that can be used to decrease the identation of list items inside the specified rich text
     * area.
     * 
     * @param rta the execution target
     */
    public OutdentExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    protected boolean canExecute(Element listItem)
    {
        if (!super.canExecute(listItem)) {
            return false;
        }
        return listItem.getParentNode() != null;
    }

    /**
     * {@inheritDoc}
     * <p>Moves the passed lust item one level up, as a next sibling of the list item it takes part of. If
     * it's a first level list item, it is pulled out of its list.</p>
     */
    @Override
    protected void execute(Element listItem)
    {
        Element parentList = (Element) listItem.getParentNode();
        // check if this item is a first level item or inside a sublist
        Element parentListItem = (Element) parentList.getParentNode();
        if (parentListItem != null && parentListItem.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
            outdentItem(listItem, parentList, parentListItem);
        } else {
            outdentFirstLevelItem(listItem, parentList);
        }
    }

    @Override
    protected boolean executeOnMultipleItems(Range range, boolean perform)
    {
        // outdent all list items in the selection, whose ancestors are not to be outdented, and make sure a list item
        // is not indented twice.
        // List items in the selection are detected as the li ancestors of all leaves in the selection, the outdent is
        // done on a given list item if it is not the descendant or self of the previously processed list item
        // the previously processed item is updated each time the condition passes

        // since the condition test result is altered by the actual outdent during the pass through the elements, we'll
        // store all that need to be outdented in a list and outdent at the end
        List<Element> toOutdent = new ArrayList<Element>();

        // assume none can be outdented, and || on actual outdent results
        boolean outdentResult = false;
        // iterate through the leafs in the range
        Node rangeLeaf = domUtils.getFirstLeaf(range);
        Node lastLeaf = domUtils.getLastLeaf(range);
        // store the last li on which an outdent was executed, regardless of the actual result
        Element lastProcessed = null;
        // check the ancestor li to indent for each leaf in the range
        while (rangeLeaf != null) {
            Element currentLi = (Element) domUtils.getFirstAncestor(rangeLeaf, LIST_ITEM_TAG);
            // check the condition
            if (lastProcessed == null || (currentLi != null && !lastProcessed.isOrHasChild(currentLi))) {
                boolean canOutdent = canExecute(currentLi);
                if (canOutdent && perform) {
                    toOutdent.add(currentLi);
                }
                outdentResult = canOutdent || outdentResult;
                // update the last item attempted to be handled, regardless of the actual result of the indent function
                lastProcessed = currentLi;
            }
            // go to next leaf
            rangeLeaf = getNextLeafBefore(rangeLeaf, lastLeaf);
        }
        // actually execute the outdents
        if (perform) {
            for (Element listItem : toOutdent) {
                execute(listItem);
            }
        }
        return outdentResult;
    }

    /**
     * Returns the next leaf before the passed leaf, including {@code lastLeaf}.
     * 
     * @param currentLeaf the leaf to get the following leaf of
     * @param lastLeaf the last leaf to be returned
     * @return the following leaf after {@code currentLeaf}, before (an including) {@code lastLeaf}, or {@code null} if
     *         no more leaves exist to satisfy this condition
     */
    private Node getNextLeafBefore(Node currentLeaf, Node lastLeaf)
    {
        if (currentLeaf == lastLeaf) {
            return null;
        } else {
            return domUtils.getNextLeaf(currentLeaf);
        }
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
        // split the parent list item in two lists, at the point where the listItem to outdent is
        domUtils.splitNode(parentListItem.getParentNode(), parentList, domUtils.getNodeIndex(listItem));
        Element newListItem = (Element) parentListItem.getNextSibling();
        // if the split list is now empty, remove it
        if (!parentList.hasChildNodes()) {
            // remove the line break first, if any, and then the sublist
            if (parentList.getPreviousSibling().getNodeName().equalsIgnoreCase(LINE_BREAK_TAG)) {
                parentListItem.removeChild(parentList.getPreviousSibling());
            }
            parentListItem.removeChild(parentList);
        }
        // store the new parent list of the listItem to outdent
        Element newParentList = (Element) listItem.getParentNode();
        // now replace the newListItem in its parent, as a sibling of the parentListItem, with the list item
        // we need to make sure the listItem is the one that stays in the tree to not invalidate listItem after the call
        // of this function
        newListItem.getParentNode().replaceChild(listItem, newListItem);
        // get the subtree of the new List item created through split
        DocumentFragment newListItemChildren = newListItem.extractContents();
        // append the contents of the newListItem as children of the listItem...
        listItem.appendChild(newListItemChildren);
        // ... handling the merging or removal of the newParentList
        if (!newParentList.hasChildNodes()) {
            // if the split parent list of the list item to outdent has no more elements, remove it
            newParentList.getParentNode().removeChild(newParentList);
        } else {
            // merge, if the new previous sibling is a list
            Node previousNewListSibling = newParentList.getPreviousSibling();
            if (previousNewListSibling != null && isList(previousNewListSibling)) {
                // merge
                while (newParentList.getFirstChild() != null) {
                    previousNewListSibling.appendChild(newParentList.getFirstChild());
                }
                // and remove it since now it's empty
                newParentList.getParentNode().removeChild(newParentList);
            }
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
        Element newList = (Element) domUtils.splitNode(parentList, domUtils.getNodeIndex(listItem));
        // remove the separator
        newList.removeChild(listItem);
        // insert the extracted Li in the document
        domUtils.insertAfter(extractFirstLevelListItem(listItem), parentList);
        // remove the parentList and/or the newList if they are empty
        if (!parentList.hasChildNodes()) {
            parentList.getParentNode().removeChild(parentList);
        }

        if (!newList.hasChildNodes()) {
            newList.getParentNode().removeChild(newList);
        } else {
            // merge, if the new previous sibling is a list
            Node previousNewListSibling = newList.getPreviousSibling();
            if (previousNewListSibling != null && isList(previousNewListSibling)) {
                // merge
                while (newList.getFirstChild() != null) {
                    previousNewListSibling.appendChild(newList.getFirstChild());
                }
                // and remove it since now it's empty
                newList.getParentNode().removeChild(newList);
            }
        }
    }

    /**
     * Extracts the first level list item handling the inline elements which should all be grouped in paragraphs, while
     * leaving the block elements as they are.
     * 
     * @param listItem the list item to extract the contents of
     * @return the document fragment with the elements in the list item, extracted and wrapped, if necessary
     */
    private DocumentFragment extractFirstLevelListItem(Element listItem)
    {
        // extract the list item and insert it after the parentList
        DocumentFragment extractedLi = ((Document) listItem.getOwnerDocument()).createDocumentFragment();
        // all the children which are wrappable in a paragraph (non block items), should be wrapped in a paragraph
        Element wrappingP = null;
        while (listItem.hasChildNodes()) {
            Node currentChild = listItem.getFirstChild();
            if (!DOMUtils.getInstance().isBlock(currentChild)) {
                if (wrappingP == null) {
                    wrappingP = listItem.getOwnerDocument().createPElement().cast();
                }
                wrappingP.appendChild(currentChild);
            } else {
                // dump current wrapping P in extractedLi, add the current element too, and reset the wrappingP
                if (wrappingP != null) {
                    extractedLi.appendChild(wrappingP);
                }
                extractedLi.appendChild(currentChild);
                wrappingP = null;
            }
        }
        // add the last wrappingP as well
        if (wrappingP != null) {
            extractedLi.appendChild(wrappingP);
        }

        return extractedLi;
    }
}
