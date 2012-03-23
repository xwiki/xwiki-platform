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
package org.xwiki.gwt.dom.client.filter;

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.dom.client.Node;

/**
 * Accepts only elements with the given CSS class name.
 * 
 * @version $Id$
 */
public class WithClassName implements NodeFilter
{
    /**
     * The CSS class name to look for.
     */
    private final String className;

    /**
     * Creates a new filter that accepts only elements that have the specified CSS class name.
     * 
     * @param className the CSS class name to look for
     */
    public WithClassName(String className)
    {
        this.className = className;
    }

    @Override
    public Action acceptNode(Node node)
    {
        return (node.getNodeType() == Node.ELEMENT_NODE && Element.as(node).hasClassName(className)) ? Action.ACCEPT
            : Action.SKIP;
    }
}
