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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Inserts a horizontal rule in place of the current selection. It should be noted that hr, being a block level element,
 * must be added under a flow container. Because of this, we split the DOM tree up to the nearest flow container and
 * insert the hr element between the two parts.
 * 
 * @version $Id$
 */
public class InsertHRExecutable implements Executable
{
    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        Selection selection = rta.getDocument().getSelection();
        Range range = selection.getRangeAt(0);

        // Leave the rest of the ranges intact.
        selection.removeAllRanges();

        // Delete the contents of the first range. The horizontal rule will be inserted in place of the deleted text.
        range.deleteContents();

        // Split the DOM tree up to the nearest flow container, insert a horizontal rule and place the caret after the
        // inserted horizontal rule.
        Node start = range.getStartContainer();
        int offset = range.getStartOffset();
        Node flowContainer = DOMUtils.getInstance().getNearestFlowContainer(start);
        if (flowContainer == null) {
            return false;
        }
        Node hr = rta.getDocument().xCreateHRElement();
        if (flowContainer == start) {
            DOMUtils.getInstance().insertAt(flowContainer, hr, offset);
            range.setEndAfter(hr);
        } else {
            Node startNextLevelSibling = DOMUtils.getInstance().splitNode(flowContainer, start, offset);
            DOMUtils.getInstance().insertAfter(hr, DOMUtils.getInstance().getChild(flowContainer, start));
            // We need to update the range after inserting the horizontal rule because otherwise the caret might jump
            // before it (this happens in IE for instance).
            range.setEnd(startNextLevelSibling, 0);
        }
        range.collapse(false);
        selection.addRange(range);

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        // NOTE: Horizontal rules are not allowed anywhere inside a DOM tree. For instance we shouldn't be able to add a
        // hr tag between table cells or between list items. This needs to be revisited since we need to enforce a
        // stricter constraint.
        return rta.getDocument().getSelection().getRangeCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }
}
