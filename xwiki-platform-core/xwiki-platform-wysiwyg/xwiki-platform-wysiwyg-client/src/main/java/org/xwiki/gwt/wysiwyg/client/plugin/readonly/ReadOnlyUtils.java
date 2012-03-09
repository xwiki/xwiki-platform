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
package org.xwiki.gwt.wysiwyg.client.plugin.readonly;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.filter.WithClassName;

import com.google.gwt.dom.client.Node;

/**
 * Utility methods concerning read-only regions inside the rich text area.
 * 
 * @version $Id$
 */
public class ReadOnlyUtils
{
    /**
     * The CSS class used to mark read-only regions inside the rich text area.
     */
    private static final String READ_ONLY_STYLE_NAME = "readOnly";

    /**
     * Utility object to manipulate the DOM.
     */
    private final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * @param node a DOM node
     * @return the closest ancestor of the given node that is marked as read-only
     */
    public Element getClosestReadOnlyAncestor(Node node)
    {
        return (Element) domUtils.getFirstAncestor(node, new WithClassName(READ_ONLY_STYLE_NAME));
    }

    /**
     * @param document a DOM document
     * @return {@code true} if the selection starts or ends inside a read-only element
     */
    public boolean isSelectionBoundaryInsideReadOnlyElement(Document document)
    {
        return isRangeBoundaryInsideReadOnlyElement(document.getSelection().getRangeAt(0));
    }

    /**
     * @param range a DOM range
     * @return {@code true} if the range starts or ends inside a read-only element
     */
    public boolean isRangeBoundaryInsideReadOnlyElement(Range range)
    {
        Element startContainerReadOnlyAncestor = getClosestReadOnlyAncestor(range.getStartContainer());
        Element endContainerReadOnlyAncestor =
            range.getStartContainer() != range.getEndContainer() ? getClosestReadOnlyAncestor(range.getEndContainer())
                : startContainerReadOnlyAncestor;
        return startContainerReadOnlyAncestor != null || endContainerReadOnlyAncestor != null;
    }
}
