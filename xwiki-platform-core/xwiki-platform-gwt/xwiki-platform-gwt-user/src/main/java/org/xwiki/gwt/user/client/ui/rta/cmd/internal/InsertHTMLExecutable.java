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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;

/**
 * Inserts an HTML fragment in place of the current selection. We overwrite the default implementation provided by the
 * predefined insertHTML command for two reasons:
 * <ul>
 * <li>Internet Explorer doesn't support the insertHTML predefined command.</li>
 * <li>Besides inserting the specified HTML in the edited DOM document, Mozilla also does some unwanted cleaning of the
 * DOM nodes like br's which leads to unexpected effects of executing this command. This is most annoying in tests when
 * we have to know how the DOM tree will be after executing the command.</li>
 * </ul>
 * 
 * @version $Id$
 */
public class InsertHTMLExecutable extends AbstractSelectionExecutable
{
    /**
     * Browser specific implementation required by this executable.
     */
    private InsertHTMLExecutableImpl impl = GWT.create(InsertHTMLExecutableImpl.class);

    /**
     * Creates a new instance that can be used to insert HTML fragments in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public InsertHTMLExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    public boolean execute(String param)
    {
        Element container = rta.getDocument().createDivElement().cast();
        container.xSetInnerHTML(param);
        return execute(container.extractContents());
    }

    /**
     * Inserts the given node in place of the current rich text area selection.
     * 
     * @param node the node to be inserted in place of the current rich text area selection
     * @return {@code true} if the execution was successful, {@code false} otherwise
     */
    public boolean execute(Node node)
    {
        Selection selection = rta.getDocument().getSelection();
        Range range = selection.isCollapsed() ? selection.getRangeAt(0) : impl.deleteSelection(rta);
        if (insertNode(range, node)) {
            selection.removeAllRanges();
            selection.addRange(range);
            return true;
        }
        return false;
    }

    /**
     * Inserts the given node at the specified position. The range that specifies where to insert the node must be
     * collapsed.
     * <p>
     * In the current implementation the node will be wrapped by the range after the insertion. Derived classes may
     * adopt a different behavior.
     * 
     * @param range specifies where you insert the node; the range must be initially collapsed
     * @param node the node to be inserted
     * @return {@code true} if the node was inserted successfully, {@code false} otherwise
     */
    protected boolean insertNode(Range range, Node node)
    {
        // NOTE: Range#insertNode(Node) is not allowed to change the start point of the target range. This means that if
        // the range starts inside a text node then it will start in the same text node after the insertion, but at the
        // end (of course, the text node would have been split).
        range.insertNode(node);
        // In order to perfectly wrap the inserted nodes (see also the previous comment) we have to contract the range.
        contractRange(range);
        return true;
    }

    /**
     * Contracts the given range in order to perfectly wrap the inserted nodes.
     * 
     * @param range the {@link Range} to be contracted
     */
    private void contractRange(Range range)
    {
        if (range.isCollapsed()) {
            return;
        }
        // If the range starts at the end of a DOM node that has value (text, comment, CDATA, etc.) then we have to move
        // the start point right after that node.
        String data = range.getStartContainer().getNodeValue();
        if (data != null && range.getStartOffset() == data.length()) {
            range.setStartAfter(range.getStartContainer());
        }
        // If the range ends at the beginning of a DOM node that has value (text, comment, CDATA, etc.) then we have to
        // move the end point right before that node.
        data = range.getEndContainer().getNodeValue();
        if (data != null && range.getEndOffset() == 0) {
            range.setEndBefore(range.getEndContainer());
        }
    }
}
