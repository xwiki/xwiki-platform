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
package org.xwiki.gwt.wysiwyg.client.plugin.list;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.KeyboardAdaptor;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Handles keyboard actions on valid HTML lists, to ensure that the lists stay valid even after keyboard changes such as
 * adding or deleting list items (enter or delete / backspace). Also ensures that all the list items which only contain
 * a sublist inside are editable.
 * 
 * @version $Id$
 */
public class ListBehaviorAdjuster extends KeyboardAdaptor implements CommandListener
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
     * The command that notifies when the content of the rich text area has been reset.
     */
    protected static final Command RESET_COMMAND = new Command("reset");

    /**
     * The rich text area to do adjustments for.
     */
    private final RichTextArea textArea;

    /**
     * Creates a new instance.
     * 
     * @param textArea the rich text area to do adjustments for
     */
    public ListBehaviorAdjuster(RichTextArea textArea)
    {
        this.textArea = textArea;
    }

    /**
     * Executes lists clean up on the subtree rooted in the element passed as parameter. Lists cleanup consists of:
     * <ul>
     * <li>finding all the {@code ul} or {@code ol} tags which are in another {@code ul} or {@code ol} and adding a
     * {@code li} wrapper around each</li>
     * <li>finding all the {@code ul} or {@code ol} tags which are at the beginning (first child) of a list item and
     * making the parent list items editable</li>
     * </ul>
     * (but these operations are executed in a single pass).
     * <p>
     * Note that while these operations are not enough from a strict xhtml cleaning point of view, they address all the
     * practical cases that appear so we chose to limit the operations executed to only these for performance reasons.
     * 
     * @param element the root element of the subtree in which to execute cleanup.
     */
    protected void cleanUp(Element element)
    {
        // find all lists
        NodeList<com.google.gwt.dom.client.Element> orderedLists = element.getElementsByTagName(ORDERED_LIST_TAG);
        NodeList<com.google.gwt.dom.client.Element> unorderedLists = element.getElementsByTagName(UNORDERED_LIST_TAG);
        // send them to the actual cleaner
        cleanUpLists(orderedLists);
        cleanUpLists(unorderedLists);

        // Ensure that each list item can be edited.
        NodeList<com.google.gwt.dom.client.Element> listItems = element.getElementsByTagName(LIST_ITEM_TAG);
        for (int i = 0; i < listItems.getLength(); i++) {
            Element.as(listItems.getItem(i)).ensureEditable();
        }
    }

    /**
     * Helper function to handle a list of list elements and clean them.
     * 
     * @param listElements the list elements to clean up, according to the description at {@link #cleanUp(Element)}
     */
    protected void cleanUpLists(NodeList<com.google.gwt.dom.client.Element> listElements)
    {
        for (int i = 0; i < listElements.getLength(); i++) {
            Element listElement = (Element) listElements.getItem(i);
            // check the parent of this list Element
            if (listElement.getParentNode().getNodeName().equalsIgnoreCase(ORDERED_LIST_TAG)
                || listElement.getParentNode().getNodeName().equalsIgnoreCase(UNORDERED_LIST_TAG)) {
                wrapList(listElement);
            }
            // check if this element is the first element of a list item
            if (listElement.getParentNode().getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)
                && listElement.getPreviousSibling() == null) {
                // is first element
                handleEmptyListItem((Element) listElement.getParentNode());
            }
        }
    }

    /**
     * Tries to reposition a list element that appears in another list element: if there is a previous sibling, it tries
     * to add it as a sublist, else it wraps it in a new list item.
     * 
     * @param listElement the list node to wrap
     */
    protected void wrapList(Element listElement)
    {
        Element previousListItem = (Element) listElement.getPreviousSibling();
        if (previousListItem != null && previousListItem.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
            previousListItem.appendChild(listElement);
        } else {
            // wrap this element in a list item
            Element wrappingListItem = listElement.getOwnerDocument().createLIElement().cast();
            wrappingListItem.wrap(listElement);
        }
    }

    /**
     * Handles a list item which void by adding a placeholder to render this element editable. Overwrite this function
     * to add browser specific behaviour.
     * 
     * @param li the empty list item to handle
     */
    protected void handleEmptyListItem(Element li)
    {
        DOMUtils.getInstance().insertAt(li, li.getOwnerDocument().createBRElement(), 0);
    }

    /**
     * Handles the delete key inside a list item, such that it meets the following behavior when hit at the end of a
     * list item:
     * <ul>
     * <li>The list item in which the delete button is hit is never deleted, if there is another list item following it.
     * On internet explorer, this may happen when the list item's entire content is deleted.</li>
     * <li>The next list item is found (if any) and moved in the current list item in which the delete button has been
     * hit</li>
     * </ul>
     * 
     * @param li the list item in which the delete key is hit
     * @param event the native event that was fired
     */
    protected void onDelete(Element li, Event event)
    {
        // check if we're at the end of the list item and, if so, move the next list item into this one
        Document document = li.getOwnerDocument().cast();
        Range range = document.getSelection().getRangeAt(0);
        // only handle collapsed ranges. Selection will be deleted as the default browser implementation dictates and
        // resulting list will be cleaned on key up
        if (!range.isCollapsed()) {
            return;
        }
        // check if the selection is at the end of a list item text, either just before an embedded ul / ol.
        Node endContainer = range.getEndContainer();
        if (endContainer.getNodeType() == Node.TEXT_NODE
            && DOMUtils.getInstance().getLength(endContainer) != range.getEndOffset()) {
            // nothing, there are still things to delete
            return;
        }
        // else, get the next leaf and check if it still is in this li or not
        Node nextLeaf = DOMUtils.getInstance().getNextLeaf(range);
        if (nextLeaf == null) {
            // don't allow delete in the last list item in the document, because it could lead to deleting the list
            // item, depending on the browser.
            event.xPreventDefault();
            return;
        }
        // get first li ancestor of nextLeaf
        Element nextLeafAncestorLi = (Element) DOMUtils.getInstance().getFirstAncestor(nextLeaf, LIST_ITEM_TAG);
        // if the next leaf is an empty element placeholder in the same list item as the caret, don't let it be deleted,
        // run the algo for the next leaf
        Node nextEmptyItemPlacehodlerLeaf = null;
        if (nextLeafAncestorLi == li && isEmptyListItemPlaceholder(nextLeaf)) {
            nextEmptyItemPlacehodlerLeaf = nextLeaf;
            nextLeaf = DOMUtils.getInstance().getNextLeaf(nextLeaf);
            if (nextLeaf == null) {
                // if there is no other leaf after the placeholder, don't allow to delete the placeholder: this would
                // lead to deleting the whole item, and if it's the last in the document, we don't want that.
                event.xPreventDefault();
                return;
            }
            nextLeafAncestorLi = (Element) DOMUtils.getInstance().getFirstAncestor(nextLeaf, LIST_ITEM_TAG);
        }
        // if the next leaf is not in a list item, fallback on default behavior
        if (nextLeafAncestorLi == null) {
            return;
        }

        if (needsDeleteAdjustment(nextLeafAncestorLi, li)) {
            // execute the delete
            executeDelete(getReferenceNode(endContainer, li, nextLeafAncestorLi), nextLeafAncestorLi,
                nextEmptyItemPlacehodlerLeaf, range);
            event.xPreventDefault();
        }
        // else browser default
    }

    /**
     * Handles the backspace key inside a list item, such that it meets the following behavior when hit at the beginning
     * of a list item:
     * <ul>
     * <li>The list item in which the delete backspace button is hit is always deleted, if there is another list item
     * preceeding it. Backspace never acts as unindent, for this, the unindent command should be used.</li>
     * <li>The previous list item is found (if any) and the current list item is moved at the end of it</li>
     * </ul>
     * 
     * @param li the list item in which the backspace key is hit
     * @param event the native event that was fired
     */
    protected void onBackspace(Element li, Event event)
    {
        // check if we're at the end of the list item and, if so, move the next list item into this one
        Document document = li.getOwnerDocument().cast();
        Range range = document.getSelection().getRangeAt(0);
        // only handle collapsed ranges. Selection will be deleted as the default browser implementation dictates and
        // resulting list will be cleaned on key up
        if (!range.isCollapsed()) {
            return;
        }
        // check if the selection is at the beginning of the list item. Look for the previous leaf
        Node startContainer = range.getStartContainer();
        if (startContainer.getNodeType() == Node.TEXT_NODE && range.getStartOffset() != 0) {
            // we are in a text node and still have elements to delete before, with a backspace
            return;
        }
        // check the previous leaf
        Node previousLeaf = DOMUtils.getInstance().getPreviousLeaf(range);
        // if there is no previous leaf, return
        if (previousLeaf == null) {
            return;
        }
        // get list item parent of the previous leaf
        Element previousLeafAncestorLi = (Element) DOMUtils.getInstance().getFirstAncestor(previousLeaf, LIST_ITEM_TAG);
        // check if the previous leaf is an empty list item placeholder in the same list item. if it is, it needs to be
        // skipped and looked for the leaf before
        Node previousEmptyItemPlacehodlerLeaf = null;

        // if the previous leaf is not in a list item, return
        if (previousLeafAncestorLi == null) {
            return;
        }

        if (needsDeleteAdjustment(li, previousLeafAncestorLi)) {
            // setup the range before move, put it in a convenient place: if the leaf is an empty placeholder,
            // put it before the leaf, and set the placeholder as the skipped item to delete on move
            if (isEmptyListItemPlaceholder(previousLeaf)) {
                range.setEndBefore(previousLeaf);
                previousEmptyItemPlacehodlerLeaf = previousLeaf;
            } else if (previousLeaf.getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
                // if the previousLeaf is an empty list item (<li />)
                range.setEnd(previousLeafAncestorLi, 0);
            } else {
                range.setEndAfter(previousLeaf);
            }
            // effectively execute the move
            executeDelete(getReferenceNode(previousLeaf, previousLeafAncestorLi, li), li,
                previousEmptyItemPlacehodlerLeaf, range);
            event.xPreventDefault();
        }
        // else browser default
    }

    /**
     * Helper function to determine whether deleting at the end / backspacing at the beginning of one of the list items
     * when next list item / previous list item is the other needs special handling or will fall back on the browser
     * default. The idea is to interfere only with backspace / delete inside the same list, between different levels
     * list items. If the two list items are in different lists (and none of them is included in the other), the delete
     * between them will be done with the browser default algorithm. Also, if the source list item is an ancestor of the
     * destination list item, the default browser behavior will be executed.
     * 
     * @param sourceListItem the list item from which content should be moved
     * @param destListItem the list item to which content should be moved
     * @return {@code true} if the delete / backspace between the two needs special handling, {@code false} otherwise
     */
    protected boolean needsDeleteAdjustment(Element sourceListItem, Element destListItem)
    {
        if (sourceListItem == destListItem) {
            return false;
        }
        // check that the destination list item is not a child of the source list item
        if (sourceListItem.isOrHasChild(destListItem)) {
            return false;
        }
        // check if the two list items do have a common ul or ol ancestor and this ul / ol is the parent of one of them
        Node commonAncestor = DOMUtils.getInstance().getNearestCommonAncestor(sourceListItem, destListItem);
        Node commonListAncestor =
            DOMUtils.getInstance().getFirstAncestor(commonAncestor, ORDERED_LIST_TAG, UNORDERED_LIST_TAG);
        return commonListAncestor != null && commonListAncestor == sourceListItem.getParentNode()
            || commonListAncestor == destListItem.getParentNode();
    }

    /**
     * Effectively executes the delete operation at the end of a list item by moving the next list item in this one, for
     * the passed parameters.
     * 
     * @param reference the reference element, to move the content of the {@code nextLi} after it
     * @param nextLi the next list item after the current list item end, to move in the current list item
     * @param range the selection range for which this operation is executed, used to determine where the {@code nextLi}
     *            needs to be inserted and how selection needs to be restored
     * @param skippedEmptyPlaceHolder the first empty list item placeholder that was skipped by the next leaf lookup
     *            algorithm in this delete operation, and which needs to be removed with the execution of the delete
     */
    protected void executeDelete(Node reference, Element nextLi, Node skippedEmptyPlaceHolder, Range range)
    {
        // save the position of the cursor to restore it after insert
        int endOffset = range.getEndOffset();
        Node endContainer = range.getEndContainer();

        // else get the next leaf's li from its parent and put it here
        Node extractedLi =
            DOMUtils.getInstance().extractNodeContents(nextLi, 0, DOMUtils.getInstance().getLength(nextLi));
        // insert the content of the found next list item, after the reference node
        DOMUtils.getInstance().insertAfter(extractedLi, reference);

        // restore the position of the cursor
        range.setEnd(endContainer, endOffset);
        Document document = reference.getOwnerDocument().cast();
        Selection selection = document.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);

        Element liParentElt = (Element) nextLi.getParentElement();
        liParentElt.removeChild(nextLi);
        // if the li from which we moved the li is an empty one, remove it
        if (liParentElt.getChildNodes().getLength() == 0) {
            // remove the list from its parent
            liParentElt.getParentElement().removeChild(liParentElt);
        }
        // delete the empty element placeholder in this li which we skipped, if any
        if (skippedEmptyPlaceHolder != null) {
            skippedEmptyPlaceHolder.getParentNode().removeChild(skippedEmptyPlaceHolder);
        }
    }

    /**
     * @param descendant the descendant of parentListItem, whose ancestor is to be found as a reference node
     * @param parentListItem the list item which is an ancestor of descendant, and under which the reference needs to be
     *            found
     * @param before the node before which the reference has to be found
     * @return the topmost node which contains {@code descendant}, is under {@code parentListItem} and does not contain
     *         {@code before}.
     */
    private Node getReferenceNode(Node descendant, Element parentListItem, Node before)
    {
        Node refNode = descendant;
        // go up to parentListItem and find the node which does not contain before, on parent relation
        while (refNode.getParentNode() != parentListItem
            && !(refNode.getParentNode().getNodeType() == Node.ELEMENT_NODE && Element.as(refNode.getParentNode())
                .isOrHasChild(Element.as(before)))) {
            refNode = refNode.getParentNode();
        }
        return refNode;
    }

    /**
     * Determines if a node is an empty list item placeholder. Overwrite this function to provide specific behavior
     * depending on the type of placeholder each browser uses.
     * 
     * @param node the node for which to check if it is the empty list item placeholder or not
     * @return true if the passed node is an empty list placeholder, or false otherwise.
     */
    public boolean isEmptyListItemPlaceholder(Node node)
    {
        return node.getNodeName().equalsIgnoreCase("br");
    }

    @Override
    protected void handleRepeatableKey(Event event)
    {
        // get current range for some checks
        Document document = Element.as(event.getEventTarget()).getOwnerDocument().cast();
        Range range = document.getSelection().getRangeAt(0);
        Node li = DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), LIST_ITEM_TAG);
        if (li == null) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyCodes.KEY_DELETE:
                onDelete((Element) li, event);
                break;
            case KeyCodes.KEY_BACKSPACE:
                onBackspace((Element) li, event);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleKeyRelease(Event event)
    {
        // Execute cleanup after each delete, enter, backspace key
        boolean needsCleanup =
            (event.getKeyCode() == KeyCodes.KEY_ENTER && !event.getShiftKey())
                || event.getKeyCode() == KeyCodes.KEY_DELETE || event.getKeyCode() == KeyCodes.KEY_BACKSPACE;
        if (needsCleanup) {
            // Clean the whole document as an operation on a list can impact more than one list (two consecutive lists
            // can be impacted by the same delete)
            cleanUp(Element.as(Element.as(event.getEventTarget()).getOwnerDocument().getDocumentElement()));
        }
    }

    @Override
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // nothing to do here by default
        return false;
    }

    @Override
    public void onCommand(CommandManager sender, Command command, String param)
    {
        List<Command> needCleanup =
            Arrays.asList(Command.DELETE, Command.INDENT, Command.OUTDENT, RESET_COMMAND, Command.INSERT_ORDERED_LIST,
                Command.INSERT_UNORDERED_LIST);
        // clean up the lists in the document on delete, indent, outdent and reset
        if (needCleanup.contains(command)) {
            cleanUp((Element) textArea.getDocument().getDocumentElement());
        }
    }
}
