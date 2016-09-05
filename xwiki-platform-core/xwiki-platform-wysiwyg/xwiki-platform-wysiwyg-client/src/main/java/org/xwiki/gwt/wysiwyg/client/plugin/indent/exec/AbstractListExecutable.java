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
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractSelectionExecutable;

import com.google.gwt.dom.client.Node;

/**
 * Superclass for all the list executables, such as indent and outdent executables, to handle frequent list operations.
 * 
 * @version $Id$
 */
public abstract class AbstractListExecutable extends AbstractSelectionExecutable
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
     * Creates a new executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public AbstractListExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * @param range a DOM range
     * @return the list item in which the given range is positioned, or {@code null} if no such thing exists
     */
    protected Element getListItem(Range range)
    {
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

    @Override
    public boolean execute(String param)
    {
        boolean executionResult = false;
        Selection selection = rta.getDocument().getSelection();
        Range range = selection.getRangeAt(0);
        if (range.isCollapsed()) {
            Element listItem = getListItem(range);
            if (canExecute(listItem)) {
                execute(listItem);
                executionResult = true;
            }
        } else {
            executionResult = executeOnMultipleItems(range, true);
        }
        // try to restore selection, hope it all stays well
        selection.removeAllRanges();
        selection.addRange(range);

        return executionResult;
    }

    /**
     * Actually executes the operation on a single list item. This should be called only after
     * {@link AbstractListExecutable#canExecute(Element)} on the same list item returns true.
     * 
     * @param listItem the list item to execute the operation on
     */
    protected abstract void execute(Element listItem);

    /**
     * Checks if this command can be executed on a single list item.
     * 
     * @param listItem the list item to check if the command can be executed on
     * @return {@code true} if the command can be executed, {@code false} otherwise
     */
    protected boolean canExecute(Element listItem)
    {
        return listItem != null;
    }

    /**
     * Executes this list operation on all items in the non-collapsed selection. The {@code perform} parameter specifies
     * if the operation is actually performed or just checked to be possible (while this kind of parameters are not good
     * practice, it's the best way right now to make sure we use the same detection algorithm in the
     * {@link #execute(String)} and {@link #isEnabled()} functions).
     * 
     * @param range the current range to execute the operation on
     * @param perform {@code true} if the operation is to be actually executed, {@code false} if it's only to be checked
     * @return {@code true} if at least one of the items in the selection was affected, {@code false} otherwise.
     */
    protected abstract boolean executeOnMultipleItems(Range range, boolean perform);

    @Override
    public boolean isEnabled()
    {
        if (!super.isEnabled()) {
            return false;
        }

        // Get the range and check if execution is possible: if it's collapsed, it's the common list item ancestor to
        // perform operation on, if it's expanded, it's each "touched" list item.
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        if (range.isCollapsed()) {
            Element listItem = getListItem(range);
            return canExecute(listItem);
        } else {
            // Check the execution is possible on multiple items, without actually performing it.
            return executeOnMultipleItems(range, false);
        }
    }
}
