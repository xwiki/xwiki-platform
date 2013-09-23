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
import org.xwiki.gwt.user.client.Cache;
import org.xwiki.gwt.user.client.Cache.CacheCallback;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Base class for all executables that manipulate a DOM document using the selection and range APIs.
 * 
 * @version $Id$
 */
public abstract class AbstractSelectionExecutable extends AbstractRichTextAreaExecutable
{
    /**
     * Collection of DOM utility methods.
     */
    protected final DOMUtils domUtils = DOMUtils.getInstance();

    /**
     * The object used to cache some of the evaluations done in this class.
     */
    protected final Cache cache;

    /**
     * Creates a new executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public AbstractSelectionExecutable(RichTextArea rta)
    {
        super(rta);
        cache = new Cache(rta.getElement());
    }

    @Override
    public String getParameter()
    {
        return null;
    }

    @Override
    public boolean isEnabled()
    {
        return cache.get(AbstractSelectionExecutable.class.getName() + "#enabled", new CacheCallback<Boolean>()
        {
            public Boolean get()
            {
                return AbstractSelectionExecutable.super.isEnabled() && hasValidSelection();
            }
        });
    }

    /**
     * @return {@code true} if the underlying rich text area has a valid selection, {@code false} otherwise
     */
    protected boolean hasValidSelection()
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

    @Override
    public boolean isExecuted()
    {
        return getParameter() != null;
    }
}
