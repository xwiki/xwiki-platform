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
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Creates a link by inserting the link XHTML.
 * 
 * @version $Id$
 */
public class CreateLinkExecutable extends InsertHTMLExecutable
{
    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        if (!super.isEnabled(rta)) {
            return false;
        }

        String anchorTagName = "a";
        // This option is enabled only if we're not in another or the selection does not touch an anchor
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // Check the parent first, for it's shorter
        if (domUtils.getFirstAncestor(range.getCommonAncestorContainer(), anchorTagName) != null) {
            return false;
        }

        // if no anchor on ancestor, test all the nodes touched by the selection to not contain an anchor
        if (domUtils.getFirstDescendant(range.cloneContents(), anchorTagName) != null) {
            return false;
        }

        // Check if the selection does not contain any block elements
        Node commonAncestor = range.getCommonAncestorContainer();
        if (!domUtils.isInline(commonAncestor)) {
            // The selection may contain a block element, check if it actually does
            Node leaf = domUtils.getFirstLeaf(range);
            Node lastLeaf = domUtils.getLastLeaf(range);
            while (true) {
                if (leaf != null) {
                    // Check if it has any non-inline parents up to the commonAncestor
                    Node parentNode = leaf;
                    while (parentNode != commonAncestor) {
                        if (!domUtils.isInline(parentNode)) {
                            // Found a non-inline parent, return false
                            return false;
                        }
                        parentNode = parentNode.getParentNode();
                    }
                }
                // Go to next leaf, if any are left
                if (leaf == lastLeaf) {
                    break;
                } else {
                    leaf = domUtils.getNextLeaf(leaf);
                }
            }
        }
        return true;
    }
}
