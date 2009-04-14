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
package com.xpn.xwiki.wysiwyg.client.plugin.separator.exec;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Inserts a horizontal rule in place of the current selection. It should be noted that hr, being a block level element,
 * must be added under a flow container. Because of this, we split the DOM tree up to the nearest flow container and
 * insert the hr element between the two parts.
 * 
 * @version $Id$
 */
public class InsertHRExecutable extends AbstractExecutable
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        Selection selection = rta.getDocument().getSelection();
        if (!selection.isCollapsed()) {
            // Delete the selected contents. The horizontal rule will be inserted in place of the deleted text.
            // NOTE: We cannot use Range#deleteContents because it may lead to DTD-invalid HTML. That's because it
            // operates on any DOM tree without taking care of the underlying XML syntax, (X)HTML in our case. Let's use
            // the Delete command instead which is HTML-aware. Moreover, others could listen to this command and adjust
            // the DOM before we insert the HR.
            rta.getCommandManager().execute(Command.DELETE);
        }

        // At this point the selection should be collapsed.
        // Split the DOM tree up to the nearest flow container, insert a horizontal rule and place the caret after the
        // inserted horizontal rule.
        Range range = selection.getRangeAt(0);
        Node start = range.getStartContainer();
        int offset = range.getStartOffset();
        Node flowContainer = domUtils.getNearestFlowContainer(start);
        if (flowContainer == null) {
            return false;
        }
        Node hr = rta.getDocument().xCreateHRElement();
        if (flowContainer == start) {
            domUtils.insertAt(flowContainer, hr, offset);
            range.setEndAfter(hr);
        } else {
            Node startNextLevelSibling = domUtils.splitHTMLNode(flowContainer, start, offset);
            domUtils.insertAfter(hr, domUtils.getChild(flowContainer, start));
            // We need to update the range after inserting the horizontal rule because otherwise the caret might jump
            // before it (this happens in IE for instance).
            range.setEnd(startNextLevelSibling, 0);
        }
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);

        return true;
    }
}
