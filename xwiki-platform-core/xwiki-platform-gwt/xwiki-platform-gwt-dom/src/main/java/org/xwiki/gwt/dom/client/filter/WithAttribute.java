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
 * Accepts only elements with the given attribute.
 * 
 * @version $Id$
 */
public class WithAttribute implements NodeFilter
{
    /**
     * The name of the attribute to look for.
     */
    private final String name;

    /**
     * The value of the attribute, {@code null} to match any value.
     */
    private final String value;

    /**
     * Creates a new filter that accepts only elements that have the specified attribute.
     * 
     * @param name the name of the attribute to look for
     */
    public WithAttribute(String name)
    {
        this(name, null);
    }

    /**
     * Creates a new filter that accepts only elements that have the specified attribute set to the given value. If the
     * value is {@code null} then only the presence of the attribute is checked.
     * 
     * @param name the name of the attribute to look for
     * @param value the value of the attribute, {@code null} to match any value
     */
    public WithAttribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public Action acceptNode(Node node)
    {
        return (node.getNodeType() == Node.ELEMENT_NODE && (value == null ? Element.as(node).hasAttribute(name) : value
            .equals(Element.as(node).getAttribute(name)))) ? Action.ACCEPT : Action.SKIP;
    }
}
