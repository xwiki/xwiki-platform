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
package com.xpn.xwiki.wysiwyg.client.syntax.rule;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.syntax.ValidationRule;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Validation rule used to define the behavior when the selection is on an image. This will disable features that refer
 * to text formatting (bold, italic, superscript) and in general all features that use the selection as text or replace
 * it with other elements (such as the symbol plugin).
 * 
 * @version $Id$
 */
public class ImageSelectionBehaviourRule implements ValidationRule
{
    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(RichTextArea)
     */
    public boolean areValid(RichTextArea textArea)
    {
        // Check the current text area selection, if it is an image selection
        Range range = textArea.getDocument().getSelection().getRangeAt(0);
        if (range.getStartContainer() == range.getEndContainer()
            && range.getStartContainer().getNodeType() == Node.ELEMENT_NODE
            && (range.getEndOffset() - range.getStartOffset()) == 1) {
            // The selection is exactly one element large, check the content of the selection
            Node selectedNode = range.getStartContainer().getChildNodes().getItem(range.getStartOffset());
            if (selectedNode.getNodeType() == Node.ELEMENT_NODE && selectedNode.getNodeName().equalsIgnoreCase("img")) {
                // The current selection is an image selection, the features must be disabled
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        // TODO: remove the link from this list when issue XWIKI-3003 will be implemented.
        return new String[] {"bold", "italic", "underline", "strikethrough", "subscript", "superscript",
            "unorderedlist", "orderedlist", "outdent", "indent", "format", "hr", "symbol", "inserttable", 
            "deletetable", "link"};
    }

}
