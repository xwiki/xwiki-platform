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
package com.xpn.xwiki.wysiwyg.client.dom.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.dom.SelectionManager;

/**
 * The default {@link SelectionManager} implementation. Retrieves the selection object using Mozilla's API.
 * 
 * @version $Id$
 */
public final class DefaultSelectionManager implements SelectionManager
{
    /**
     * {@inheritDoc}
     * 
     * @see SelectionManager#getSelection(Document)
     */
    public Selection getSelection(Document doc)
    {
        return new DefaultSelection(getJSSelection(doc));
    }

    /**
     * Retrieves the native selection object using Mozilla's API.
     * 
     * @param doc The DOM document for which to retrieve the native selection.
     * @return The selection JavaScript object associated with the specified document.
     */
    private native JavaScriptObject getJSSelection(Document doc)
    /*-{
        return doc.defaultView.getSelection();
    }-*/;
}
