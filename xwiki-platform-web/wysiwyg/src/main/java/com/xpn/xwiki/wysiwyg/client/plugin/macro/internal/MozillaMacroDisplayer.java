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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.internal;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDisplayer;

/**
 * Mozilla-specific implementation for a macro displayer fixing the following issues:
 * <ul>
 * <li>Pressing delete before or backspace after a macro container places the caret inside the container.</li>
 * </ul>
 * 
 * @version $Id$
 */
public class MozillaMacroDisplayer extends MacroDisplayer
{
    /**
     * {@inheritDoc}
     * 
     * @see MacroDisplayer#createMacroContainer(Node, Node, int)
     */
    protected Element createMacroContainer(Node start, Node stop, int siblingCount)
    {
        Element container = super.createMacroContainer(start, stop, siblingCount);
        // We have to add a caret blocker at the beginning and at the end to prevent the caret from getting inside.
        container.insertBefore(createCaretBlocker(), container.getFirstChild());
        container.appendChild(createCaretBlocker());
        return container;
    }

    /**
     * Creates a DOM node that can be inserted at the beginning or at the end of a macro container to prevent the caret
     * from getting inside. Mozilla allows the caret to get inside a button in some situations like for instance when we
     * delete the last character before the button. The returned node can be used to fix this bug.
     * 
     * @return the newly created caret blocker
     */
    private Node createCaretBlocker()
    {
        ImageElement img = getTextArea().getDocument().xCreateImageElement();
        img.setWidth(0);
        img.setHeight(0);
        return img;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroDisplayer#getOutput(Element)
     */
    protected Element getOutput(Element container)
    {
        return (Element) container.getLastChild().getPreviousSibling();
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroDisplayer#getPlaceHolder(Element)
     */
    protected Element getPlaceHolder(Element container)
    {
        return (Element) container.getFirstChild().getNextSibling();
    }
}
