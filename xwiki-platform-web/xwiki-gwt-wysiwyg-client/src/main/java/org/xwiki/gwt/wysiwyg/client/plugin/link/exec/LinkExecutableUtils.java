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

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;

/**
 * Utility class to perform functions associated to link executables, such as detecting the selected anchor. We have
 * this as static function in a distinct class since there is no way to insert this on both the
 * {@link CreateLinkExecutable} and {@link UnlinkExecutable} hierarchies.
 * 
 * @version $Id$
 */
public class LinkExecutableUtils
{
    /**
     * The tag name of the anchor element.
     */
    public static final String ANCHOR_TAG_NAME = "a";

    /**
     * Protected constructor.
     */
    protected LinkExecutableUtils()
    {
    }

    /**
     * Returns the currently selected anchor element in the passed {@link RichTextArea}, as the user perceives the
     * selection. The algorithm used is as follows:
     * <ul>
     * <li>find anchor ancestor of the first leaf of the selection. If the first leaf is the selection start container
     * and the offset is at the end, then eliminate this first leaf and analyze the second</li>
     * <li>find anchor ancestor of the last leaf of the selection. If the last leaf is the end container and the
     * selection starts at its very beginning, skip this and analyze the previous leaf</li>
     * <li>is the two ancestor anchors are not null and are the same, this anchor is the selected anchor</li>
     * </ul>
     * 
     * @param rta the rich text area whose selection to test
     * @return the anchor in which the current selection is found
     */
    public static AnchorElement getSelectedAnchor(RichTextArea rta)
    {
        // there is no wrapping anchor if there is no selection
        if (rta.getDocument().getSelection().getRangeCount() == 0) {
            return null;
        }
        // Find the selected anchor by the following algorithm:
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // if the selection is collapsed, we have our answer already
        if (range.isCollapsed()) {
            return (AnchorElement) DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), ANCHOR_TAG_NAME);
        }
        AnchorElement foundAncestorStart = null;
        AnchorElement foundAncestorEnd = null;
        // get the first leaf of the selection and check it out
        Node firstLeaf = DOMUtils.getInstance().getFirstLeaf(range);
        if (firstLeaf == range.getStartContainer()
            && range.getStartOffset() == DOMUtils.getInstance().getLength(firstLeaf)) {
            firstLeaf = DOMUtils.getInstance().getNextLeaf(firstLeaf);
        }
        foundAncestorStart = (AnchorElement) DOMUtils.getInstance().getFirstAncestor(firstLeaf, ANCHOR_TAG_NAME);
        // if no anchor found for start, it's pointless to keep looking, won't find anything
        if (foundAncestorStart == null) {
            return null;
        }
        // check the other end
        Node lastLeaf = DOMUtils.getInstance().getLastLeaf(range);
        if (lastLeaf == range.getEndContainer() && range.getEndOffset() == 0) {
            lastLeaf = DOMUtils.getInstance().getPreviousLeaf(lastLeaf);
        }
        foundAncestorEnd = (AnchorElement) DOMUtils.getInstance().getFirstAncestor(lastLeaf, ANCHOR_TAG_NAME);

        if (foundAncestorStart == foundAncestorEnd && foundAncestorStart != null) {
            return foundAncestorStart;
        }

        return null;
    }
}
