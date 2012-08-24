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
import org.xwiki.gwt.dom.client.Property;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Looks for the nearest block-level elements from the current selection and applies a specific style to them.
 * 
 * @version $Id$
 */
public class BlockStyleExecutable extends AbstractBlockExecutable
{
    /**
     * The style property that is applied.
     */
    private final Property property;

    /**
     * Creates a new instance that applies the given style property to the nearest block-level elements from the current
     * selection in the specified rich text area.
     * 
     * @param rta the execution target
     * @param property the style property to be applied
     */
    public BlockStyleExecutable(RichTextArea rta, Property property)
    {
        super(rta);
        this.property = property;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Applies the underlying style property with the specified value to the nearest block-level ancestor of the given
     * node.
     * 
     * @see AbstractStyleExecutable#execute(Node, int, int, String)
     */
    @Override
    protected void execute(Node node, int startOffset, int endOffset, String value)
    {
        Node ancestor = node;
        int offset = startOffset;
        if (domUtils.isInline(node)) {
            ancestor = domUtils.getFarthestInlineAncestor(node);
            offset = domUtils.getNodeIndex(ancestor);
            ancestor = ancestor.getParentNode();
        }

        if (ancestor == ancestor.getOwnerDocument().getBody()) {
            // We cannot apply the style property to the BODY element because only its contents are persisted when the
            // edited document is saved; as a consequence all the attributes of the BODY element are dropped upon
            // saving the edited document (including the style attribute).
            // Let's wrap the in-line contents around the current selection in a paragraph.
            ancestor = wrap((Element) ancestor, offset, "p");
        }
        addStyle(Element.as(ancestor), value);
    }

    /**
     * Styles the given element.
     * 
     * @param element the element to be styled
     * @param value the value of the style property that is set
     */
    protected void addStyle(Element element, String value)
    {
        element.getStyle().setProperty(property.getJSName(), value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the value of the underlying style property for the nearest block-level ancestor of the given node.
     * 
     * @see AbstractStyleExecutable#getParameter(Node)
     */
    @Override
    protected String getParameter(Node node)
    {
        Element element = (Element) domUtils.getNearestBlockContainer(node);
        return element.getComputedStyleProperty(property.getJSName());
    }
}
