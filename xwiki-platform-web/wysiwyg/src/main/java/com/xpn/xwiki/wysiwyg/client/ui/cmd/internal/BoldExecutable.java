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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

public class BoldExecutable extends StyleExecutable
{
    public BoldExecutable()
    {
        super("strong", null, "font-weight", "bold", true, Command.BOLD.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see StyleExecutable#matchesStyle(Node)
     */
    protected boolean matchesStyle(Node node)
    {
        if (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getParentNode();
        }
        String fontWeight = Element.as(node).getComputedStyleProperty("font-weight");
        if ("bold".equalsIgnoreCase(fontWeight) || "bolder".equalsIgnoreCase(fontWeight)) {
            return true;
        } else {
            try {
                int iFontWeight = Integer.parseInt(fontWeight);
                return iFontWeight > 400;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
