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

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigDOMReader;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigDOMWriter;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigJSONParser;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfigJSONSerializer;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;

/**
 * Creates a link by inserting the link XHTML.
 * 
 * @version $Id$
 */
public class CreateLinkExecutable extends AbstractInsertElementExecutable<LinkConfig, AnchorElement>
{
    /**
     * Creates a new executable that can be used to insert links in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public CreateLinkExecutable(RichTextArea rta)
    {
        super(rta);

        configDOMReader = new LinkConfigDOMReader();
        configDOMWriter = new LinkConfigDOMWriter();
        configJSONParser = new LinkConfigJSONParser();
        configJSONSerializer = new LinkConfigJSONSerializer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#isEnabled()
     */
    public boolean isEnabled()
    {
        if (!super.isEnabled()) {
            return false;
        }

        // Create link executable is enabled either for creating a new link or for editing an existing link. Check if
        // we're editing a link.
        if (getSelectedElement() != null) {
            return true;
        }

        // If no anchor on ancestor, test all the nodes touched by the selection to not contain an anchor.
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        if (domUtils.getFirstDescendant(range.cloneContents(), LinkExecutableUtils.ANCHOR_TAG_NAME) != null) {
            return false;
        }

        // Check if the selection does not contain any block elements.
        Node commonAncestor = range.getCommonAncestorContainer();
        if (!domUtils.isInline(commonAncestor)) {
            // The selection may contain a block element, check if it actually does.
            Node leaf = domUtils.getFirstLeaf(range);
            Node lastLeaf = domUtils.getLastLeaf(range);
            while (true) {
                if (leaf != null) {
                    // Check if it has any non-in-line parents up to the commonAncestor.
                    Node parentNode = leaf;
                    while (parentNode != commonAncestor) {
                        if (!domUtils.isInline(parentNode)) {
                            // Found a non-in-line parent, return false.
                            return false;
                        }
                        parentNode = parentNode.getParentNode();
                    }
                }
                // Go to next leaf, if any are left.
                if (leaf == lastLeaf) {
                    break;
                } else {
                    leaf = domUtils.getNextLeaf(leaf);
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#getCacheKeyPrefix()
     */
    @Override
    protected String getCacheKeyPrefix()
    {
        return CreateLinkExecutable.class.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#getSelectedElement()
     */
    @Override
    protected AnchorElement getSelectedElement()
    {
        return LinkExecutableUtils.getSelectedAnchor(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractInsertElementExecutable#newElement()
     */
    @Override
    protected AnchorElement newElement()
    {
        AnchorElement anchor = rta.getDocument().createAnchorElement();
        // Firefox 9 doesn't allow us to insert an empty anchor (no reference and no label) in an empty editable body
        // element. The call emptyEditableBody.appendChild(emptyAnchor) is simply ignored. I know that, following the
        // strict DTD, the body element can contain only block-level elements, but in this case inserting an empty image
        // or a text node works fine. So Firefox 9 has something against inserting an empty anchor in an empty editable
        // body. As a workaround we set the anchor reference and label to some default values. Note that these values
        // will be overwritten later because the insert link wizard doesn't accept an empty reference or label.
        anchor.setHref("http://www.xwiki.org");
        anchor.setInnerText("XWiki");
        return anchor;
    }
}
