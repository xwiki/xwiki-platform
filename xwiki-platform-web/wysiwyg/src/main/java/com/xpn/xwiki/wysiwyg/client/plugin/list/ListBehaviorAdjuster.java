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
package com.xpn.xwiki.wysiwyg.client.plugin.list;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.InnerHTMLListener;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;

/**
 * Handles keyboard actions on valid HTML lists, to ensure that the lists stay valid even after keyboard changes such as
 * adding or deleting list items (enter or delete / backspace). Also ensures that all the list items which only contain
 * a sublist inside are editable.
 * 
 * @version $Id$
 */
public class ListBehaviorAdjuster implements InnerHTMLListener, KeyboardListener, CommandListener
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
     * The rich text area to do adjustments for.
     */
    private RichTextArea textArea;

    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(Element)
     */
    public void onInnerHTMLChange(Element element)
    {
        // Clean up the lists in this element
        cleanUp(element);
    }

    /**
     * Executes lists clean up on the subtree rooted in the element passed as parameter. Lists cleanup consists of:
     * <ul>
     * <li>finding all the {@code ul} or {@code ol} tags which are in another {@code ul} or {@code ol} and adding a
     * {@code li} wrapper around each</li>
     * <li>finding all the {@code ul} or {@code ol} tags which are at the beginning (first child) of a list item and
     * making the parent list items editable</li>
     * </ul>
     * (but these operations are executed in a single pass). <br />
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
            if (listElement.getParentNode().getNodeType() == Node.ELEMENT_NODE
                && (listElement.getParentNode().getNodeName().equalsIgnoreCase(ORDERED_LIST_TAG) || listElement
                    .getParentNode().getNodeName().equalsIgnoreCase(UNORDERED_LIST_TAG))) {
                // wrap this element in a list item
                Element wrappingListItem = ((Document) listElement.getOwnerDocument()).xCreateElement(LIST_ITEM_TAG);
                wrappingListItem.wrap(listElement);
            }
            // check if this element is the first element of a listItem, and the parent is a list item
            if (listElement.getParentElement().getFirstChild() == listElement
                && listElement.getParentNode().getNodeName().equalsIgnoreCase(LIST_ITEM_TAG)) {
                // is first element
                handleEmptyListItem((Element) listElement.getParentElement());
            }
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
        DOMUtils.getInstance().insertAt(li, ((Document) li.getOwnerDocument()).xCreateBRElement(), 0);
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
     */
    protected void onDelete(Element li)
    {
        // check if we're at the end of the list item and, if so, move the next list item into this one
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
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
            getTextArea().getCurrentEvent().preventDefault();
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
                getTextArea().getCurrentEvent().preventDefault();
                return;
            }
            nextLeafAncestorLi = (Element) DOMUtils.getInstance().getFirstAncestor(nextLeaf, LIST_ITEM_TAG);
        }
        // if we're in the same list item, we still have things to delete, so fall to default behaviour
        if (nextLeafAncestorLi == li || nextLeafAncestorLi == null) {
            return;
        }

        // execute the delete
        executeDelete(li, nextLeafAncestorLi, nextEmptyItemPlacehodlerLeaf, range);
        getTextArea().getCurrentEvent().preventDefault();
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
     */
    protected void onBackspace(Element li)
    {
        // check if we're at the end of the list item and, if so, move the next list item into this one
        Range range = getTextArea().getDocument().getSelection().getRangeAt(0);
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

        // if the leaf is still in this list item (there are more things to delete) or it's not in a list item, return
        if (previousLeafAncestorLi == li || previousLeafAncestorLi == null) {
            return;
        }

        // setup the range before move, put it in a convenient place: if the leaf is an empty placeholder,
        // put it before the leaf, and set the placeholder as the skipped item to delete on move
        if (isEmptyListItemPlaceholder(previousLeaf)) {
            range.setEnd(previousLeafAncestorLi, DOMUtils.getInstance().getNodeIndex(previousLeaf));
            previousEmptyItemPlacehodlerLeaf = previousLeaf;
        } else {
            range.setEnd(previousLeafAncestorLi, DOMUtils.getInstance().getNodeIndex(previousLeaf) + 1);
        }

        // effectively execute the move
        executeDelete(previousLeafAncestorLi, li, previousEmptyItemPlacehodlerLeaf, range);
        getTextArea().getCurrentEvent().preventDefault();
    }

    /**
     * Effectively executes the delete operation at the end of a list item by moving the next list item in this one, for
     * the passed parameters.
     * 
     * @param li the list item in which the delete was hit
     * @param nextLi the next list item after the current list item end, to move in the current list item
     * @param range the selection range for which this operation is executed, used to determine where the {@code nextLi}
     *            needs to be inserted and how selection needs to be restored
     * @param skippedEmptyPlaceHolder the first empty list item placeholder that was skipped by the next leaf lookup
     *            algorithm in this delete operation, and which needs to be removed with the execution of the delete
     */
    protected void executeDelete(Element li, Element nextLi, Node skippedEmptyPlaceHolder, Range range)
    {
        // save the position of the cursor to restore it after insert
        int endOffset = range.getEndOffset();
        Node endContainer = range.getEndContainer();

        // else get the next leaf's li from its parent and put it here
        Node extractedLi =
            DOMUtils.getInstance().extractNodeContents(nextLi, 0, DOMUtils.getInstance().getLength(nextLi));
        // insert the content of the found next list item, with the li as a parent
        if (endContainer == li) {
            DOMUtils.getInstance().insertAt(li, extractedLi, range.getEndOffset());
        } else {
            Node insertAfter = DOMUtils.getInstance().getChild(li, endContainer);
            DOMUtils.getInstance().insertAfter(extractedLi, insertAfter);
        }

        // restore the position of the cursor
        range.setEnd(endContainer, endOffset);
        getTextArea().getDocument().getSelection().removeAllRanges();
        getTextArea().getDocument().getSelection().addRange(range);

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

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (textArea != sender) {
            return;
        }

        // get current range for some checks
        Range range = textArea.getDocument().getSelection().getRangeAt(0);
        Node li = DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), LIST_ITEM_TAG);
        if (li == null) {
            return;
        }

        switch (keyCode) {
            case KeyboardListener.KEY_DELETE:
                onDelete((Element) li);
                break;
            case KeyboardListener.KEY_BACKSPACE:
                onBackspace((Element) li);
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        // check we're on the right element
        if (textArea != sender) {
            return;
        }

        // Execute cleanup after each delete, enter, backspace key
        boolean needsCleanup =
            (keyCode == KEY_ENTER && (modifiers != MODIFIER_SHIFT)) || keyCode == KEY_DELETE
                || keyCode == KEY_BACKSPACE;
        if (needsCleanup) {
            // Clean the whole document as an operation on a list can impact more than one list (two consecutive lists
            // can be impacted by the same delete)
            cleanUp(textArea.getDocument().getDocumentElement());
        } else {
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // nothing by default
    }

    /**
     * @return the textArea that this handler is operating on
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * @param textArea the textArea to operate on
     */
    public void setTextArea(RichTextArea textArea)
    {
        this.textArea = textArea;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // nothing to do here by default
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        // clean up the lists in the document on the delete command
        if (command == Command.DELETE) {
            cleanUp(getTextArea().getDocument().getDocumentElement());
        }
    }
}
