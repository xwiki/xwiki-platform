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

import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.RangeCompare;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Executable for the unlink command, to remove a link in the wiki document. The following rules apply:
 * <ul>
 * <li>if there is no selection and the cursor is inside a wikilink, remove the enclosing link</li>
 * <li>if there is a selection which is fully inside or equal to a wikilink, remove the enclosing link</li>
 * <li>the command is not enabled in any other situation</li>
 * </ul>
 * 
 * @version $Id$
 */
public class UnlinkExecutable extends AbstractExecutable
{
    /**
     * Hold the name of the anchor tag.
     */
    private static final String ANCHOR = "a";

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        // Get the wrapping anchor
        Range range = domUtils.getTextRange(rta.getDocument().getSelection().getRangeAt(0));
        Element anchor = (Element) domUtils.getFirstAncestor(range.getCommonAncestorContainer(), ANCHOR);
        if (anchor == null) {
            return false;
        }
        // remove it
        // check where is the selection first. If the selection is a caret and is at one side of the anchor, just move
        // the care out instead of removing the link
        Range anchorTextRange = getAnchorTextRange(anchor, rta);
        boolean isBeginning = range.compareBoundaryPoints(RangeCompare.START_TO_START, anchorTextRange) <= 0;
        boolean isEnd = range.compareBoundaryPoints(RangeCompare.END_TO_END, anchorTextRange) >= 0;
        if (range.isCollapsed() && (isBeginning || isEnd)) {
            // cursor it's at the beginning or at the end, move it out of the anchor
            Range newRange = rta.getDocument().createRange();
            if (isBeginning) {
                newRange.setStartBefore(anchor);
            }
            if (isEnd) {
                newRange.setStartAfter(anchor);
            }
            newRange.collapse(true);
            // now set it on the document
            rta.getDocument().getSelection().removeAllRanges();
            rta.getDocument().getSelection().addRange(newRange);
        } else {
            ((Element) anchor).unwrap();
        }
        return true;
    }

    /**
     * @param anchor the anchor to return the text range from
     * @param rta the {@link RichTextArea} whose document we're using to create and extract ranges
     * @return the textrange inside the passed the anchor.
     */
    private Range getAnchorTextRange(Element anchor, RichTextArea rta)
    {
        // get anchor's text range
        Range anchorRange = rta.getDocument().createRange();
        anchorRange.setStart(anchor, 0);
        anchorRange.setEnd(anchor, anchor.getChildNodes().getLength());
        Range anchorTextRange = domUtils.getTextRange(anchorRange);
        return anchorTextRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        if (!super.isEnabled(rta)) {
            return false;
        }

        // Check the selection, to be either void or inside a link.
        Range range = domUtils.getTextRange(rta.getDocument().getSelection().getRangeAt(0));
        return domUtils.getFirstAncestor(range.getCommonAncestorContainer(), ANCHOR) != null;
    }
}
