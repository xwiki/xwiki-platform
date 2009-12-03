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

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

import com.google.gwt.dom.client.Node;

/**
 * Base class for all {@link Executable}s that manipulate a DOM document using the selection and range APIs.
 * 
 * @version $Id$
 */
public abstract class AbstractExecutable implements Executable
{
    /**
     * Collection of DOM utility methods.
     */
    protected DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return isSupported(rta) && rta.isEnabled() && hasValidSelection(rta);
    }

    /**
     * @param rta a rich text area
     * @return {@code true} if the given rich text area has a valid selection, {@code false} otherwise
     */
    protected boolean hasValidSelection(RichTextArea rta)
    {
        Document document = rta.getDocument();
        Selection selection = document.getSelection();
        for (int i = 0; i < selection.getRangeCount(); i++) {
            Node ancestor = selection.getRangeAt(i).getCommonAncestorContainer();
            if (ancestor.getNodeType() == Node.DOCUMENT_NODE) {
                return false;
            } else if (ancestor.getNodeType() != Node.ELEMENT_NODE) {
                ancestor = ancestor.getParentNode();
            }
            if (!document.getBody().isOrHasChild(Element.as(ancestor))) {
                return false;
            }
        }
        return selection.getRangeCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return getParameter(rta) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return rta.isAttached() && rta.getDocument() != null;
    }
}
