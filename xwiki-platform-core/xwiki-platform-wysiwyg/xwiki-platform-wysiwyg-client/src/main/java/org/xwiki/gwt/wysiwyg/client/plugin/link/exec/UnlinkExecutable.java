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
package org.xwiki.gwt.wysiwyg.client.plugin.link.exec;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractSelectionExecutable;

import com.google.gwt.dom.client.AnchorElement;


/**
 * Executable for the unlink command, to remove a link in the wiki document. The command will be enabled whenever a 
 * link is selected, according to the algorithm described by {@link LinkExecutableUtils#getSelectedAnchor(RichTextArea)}.
 * 
 * @version $Id$
 */
public class UnlinkExecutable extends AbstractSelectionExecutable
{
    /**
     * Creates a new executable that can be used to remove links from the specified rich text area.
     * 
     * @param rta the execution target
     */
    public UnlinkExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    public boolean execute(String param)
    {
        // Get the selected anchor
        AnchorElement selectedAnchor = LinkExecutableUtils.getSelectedAnchor(rta);
        if (selectedAnchor == null) {
            return false;
        }
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // unlink
        // but first check where is the selection. If the selection is a caret and is at one side of the anchor, just
        // move the caret out instead of removing the link
        boolean moveSelection = range.isCollapsed();
        boolean isBeginning = false;
        boolean isEnd = false;
        if (moveSelection) {
            // check if it's at the beginning or at the end
            isBeginning = (domUtils.getFirstAncestor(domUtils.getPreviousLeaf(range), 
                LinkExecutableUtils.ANCHOR_TAG_NAME) != selectedAnchor) && range.getStartOffset() == 0;
            isEnd = (domUtils.getFirstAncestor(domUtils.getNextLeaf(range), 
                LinkExecutableUtils.ANCHOR_TAG_NAME) != selectedAnchor) 
                && range.getEndOffset() == domUtils.getLength(range.getEndContainer());
        }
        if (moveSelection && (isEnd || isBeginning) && selectedAnchor.getOffsetWidth() > 0) {
            // cursor it's at the beginning or at the end, move it out of the anchor
            moveCaretOuside(rta, Element.as(selectedAnchor), isEnd);
        } else {
            Element.as(selectedAnchor).unwrap();
        }
        return true;
    }

    /**
     * Moves the caret outside the passed anchor.
     * 
     * @param rta the underlying rich text area
     * @param selectedAnchor the anchor to move the caret out of
     * @param atEnd {@code true} if the caret is at the end of the selected anchor, {@code false} if it's at the
     *            beginning.
     */
    private void moveCaretOuside(RichTextArea rta, Element selectedAnchor, boolean atEnd)
    {
        Range newRange = rta.getDocument().createRange();
        if (atEnd) {
            newRange.setStartAfter(selectedAnchor);
        } else {
            newRange.setStartBefore(selectedAnchor);
        }
        newRange.collapse(true);
        // now set it on the document
        rta.getDocument().getSelection().removeAllRanges();
        rta.getDocument().getSelection().addRange(newRange);
    }

    @Override
    public boolean isEnabled()
    {
        // check that there is a selected anchor
        return super.isEnabled() && LinkExecutableUtils.getSelectedAnchor(rta) != null;
    }
}
