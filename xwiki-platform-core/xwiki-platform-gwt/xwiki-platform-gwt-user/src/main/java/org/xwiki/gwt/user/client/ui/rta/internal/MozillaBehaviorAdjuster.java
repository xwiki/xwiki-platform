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
package org.xwiki.gwt.user.client.ui.rta.internal;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;

import com.google.gwt.dom.client.Node;

/**
 * Adjusts the behavior of the rich text area in Mozilla based browsers, like Firefox.
 * 
 * @version $Id$
 */
public class MozillaBehaviorAdjuster extends BehaviorAdjuster
{
    @Override
    protected void navigateOutsideTableCell(Event event, boolean before)
    {
        super.navigateOutsideTableCell(event, before);

        if (!event.isCancelled()) {
            return;
        }

        Document document = getTextArea().getDocument();
        Selection selection = document.getSelection();
        Range caret = selection.getRangeAt(0);
        Node emptyTextNode = caret.getStartContainer();

        // (1) We need to add a BR to make the new empty line visible.
        // (2) The caret is rendered at the start of the document when we place it inside an empty text node. To fix
        // this, we move the caret before the BR and remove the empty text node.
        emptyTextNode.getParentNode().insertBefore(document.createBRElement(), emptyTextNode);
        caret.setStartBefore(emptyTextNode.getPreviousSibling());
        caret.collapse(true);
        emptyTextNode.getParentNode().removeChild(emptyTextNode);

        // Update the selection.
        selection.removeAllRanges();
        selection.addRange(caret);
    }
}
