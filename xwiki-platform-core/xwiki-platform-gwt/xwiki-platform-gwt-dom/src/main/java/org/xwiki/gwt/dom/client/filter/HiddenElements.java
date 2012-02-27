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
import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Display;

/**
 * Accepts only elements that are not displayed, i.e. elements that are attached to the document but for which the
 * computed value of the {@code display} CSS property is equal to {@code none}.
 * 
 * @version $Id$
 */
public class HiddenElements implements NodeFilter
{
    @Override
    public Action acceptNode(Node node)
    {
        return node.getNodeType() == Node.ELEMENT_NODE
            && Display.NONE.getCssName().equals(Element.as(node).getComputedStyleProperty(Style.DISPLAY))
            ? Action.ACCEPT : Action.SKIP;
    }
}
