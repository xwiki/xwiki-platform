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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Indent executable to handle valid XHTML lists indent, semantically: when a list item is indented, all its subitems
 * are indented as well.
 * 
 * @version $Id$
 */
public class IndentExecutable extends AbstractListExecutable
{
    /**
     * Creates a new executable that can be used to indent list items inside the specified rich text area.
     * 
     * @param rta the execution target
     */
    public IndentExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeOnMultipleItems(Range range, boolean perform)
    {
        // don't dive into this code unless you have a free afternoon, and definitely never before a release!
        // indent all list items in the selection, whose ancestors are not to be indented, and make sure a list item is
        // not indented twice.
        // List items in the selection are detected as the li ancestors of all leaves in the selection, the indent is
        // done on a given list item if:
        // 1. it is not the descendant or self of the previously indented list item
        // 2. it would not be the descendant or self of the previously indented list item after the indent execution
        // the previously processed item is updated each time the first condition passes: it means that an indent was
        // attempted on that list item

        // assume none can be indented, and || on indent results
        boolean indentResult = false;
        // iterate through the leafs in the range
        Node rangeLeaf = domUtils.getFirstLeaf(range);
        Node lastLeaf = domUtils.getLastLeaf(range);
        // store the last li on which an indent function was executed, regardless of the actual result of the indent
        Element lastProcessed = null;
        // check the ancestor li to indent for each leaf in the range
        while (rangeLeaf != null) {
            Element currentLi = (Element) domUtils.getFirstAncestor(rangeLeaf, LIST_ITEM_TAG);
            // check the conditions: first condition, before indent
            if (lastProcessed == null || (currentLi != null && !lastProcessed.isOrHasChild(currentLi))) {
                // second condition translates into lastProcessedLi != currentItem.previousSibling, since indent is
                // always performed relative to previous sibling.
                if (lastProcessed == null || (currentLi != null && lastProcessed != currentLi.getPreviousSibling())) {
                    // perform indent and update function result. First the fct execution!
                    indentResult = checkAndPerformIndent(currentLi, perform) || indentResult;
                }
                // update the last item attempted to be handled, regardless of the actual result of the indent function
                lastProcessed = currentLi;
            }
            // go to next leaf
            rangeLeaf = getNextLeafBefore(rangeLeaf, lastLeaf);
        }
        return indentResult;
    }

    /**
     * @param item the item to check and perform indent on
     * @param perform if the indent needs to be actually performed or just checked
     * @return {@code true} if the indent can be performed (and has been performed) or {@code false} otherwise
     */
    private boolean checkAndPerformIndent(Element item, boolean perform)
    {
        boolean canIndent = canExecute(item);
        // if can indent and should indent, indent
        if (perform && canIndent) {
            execute(item);
        }
        return canIndent;
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
     * {@inheritDoc}
     */
    @Override
    protected boolean canExecute(Element listItem)
    {
        if (!super.canExecute(listItem)) {
            return false;
        }

        // get the previous list item
        Node previousListItem = listItem.getPreviousSibling();
        if (previousListItem == null || !previousListItem.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}. Performs the indent of the passed list item, if possible: it turns it into the child list item of
     * its previous sibling, if such an element exists, also handling the merge if the previous sibling already has a
     * sublist.
     */
    @Override
    protected void execute(Element listItem)
    {
        Node previousListItem = listItem.getPreviousSibling();
        // move it, inside the last list of the previous list item
        Node lastChild = previousListItem.getLastChild();
        Element lastSecondLevelList = null;
        if (isList(lastChild)) {
            lastSecondLevelList = (Element) lastChild;
        }
        // if there is no second level list, create a new list and add it to the previous list item
        if (lastSecondLevelList == null) {
            lastSecondLevelList =
                (Element) listItem.getOwnerDocument().createElement(listItem.getParentNode().getNodeName());
            previousListItem.appendChild(lastSecondLevelList);
        }
        // add the current list item as the last child of the lastSecondLevelList in the previous list item
        lastSecondLevelList.appendChild(listItem);
    }
}
