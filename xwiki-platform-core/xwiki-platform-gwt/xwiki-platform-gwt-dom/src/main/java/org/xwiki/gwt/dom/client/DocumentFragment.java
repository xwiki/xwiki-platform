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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;

/**
 * A fragment of a DOM document.
 * <p>
 * We've added this class because at the time of writing GWT doesn't offer a similar implementation.
 * <p>
 * See http://code.google.com/p/google-web-toolkit/issues/detail?id=2955.
 * 
 * @version $Id$
 */
public final class DocumentFragment extends Node
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected DocumentFragment()
    {
    }

    /**
     * Determine whether the given {@link Node} can be cast to a {@link DocumentFragment}. A {@code null} node will
     * cause this method to return {@code false}.
     * 
     * @param node a DOM node
     * @return {@code true} if the given node is a document fragment, {@code false} otherwise
     */
    public static boolean is(Node node)
    {
        return (node != null) && (node.getNodeType() == DOMUtils.DOCUMENT_FRAGMENT_NODE);
    }

    /**
     * Assert that the given {@link Node} is a {@link DocumentFragment} and automatically typecast it.
     * 
     * @param node a document fragment DOM node
     * @return the given node, casted to a document fragment
     */
    public static DocumentFragment as(Node node)
    {
        assert is(node);
        return (DocumentFragment) node;
    }

    /**
     * @return the HTML serialization of this document fragment
     */
    public String getInnerHTML()
    {
        Element container = getOwnerDocument().createDivElement().cast();
        // We avoid attaching a clone to limit the memory used.
        container.appendChild(this);
        String innerHTML = container.xGetInnerHTML();
        // We restore the document fragment.
        appendChild(container.extractContents());
        return innerHTML;
    }

    /**
     * @return the text, without mark-up, found within this document fragment
     */
    public String getInnerText()
    {
        Element container = getOwnerDocument().createDivElement().cast();
        // We avoid attaching a clone to limit the memory used.
        container.appendChild(this);
        String innerText = container.xGetInnerText();
        // We restore the document fragment.
        appendChild(container.extractContents());
        return innerText;
    }
}
