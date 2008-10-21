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
package com.xpn.xwiki.wysiwyg.client.selection.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.RangeFactory;

/**
 * The default {@link RangeFactory} implementation. It creates JavaScript range objects using Mozilla's API.
 * 
 * @version $Id$
 */
public class DefaultRangeFactory implements RangeFactory
{
    /**
     * {@inheritDoc}
     * 
     * @see RangeFactory#createRange(Document)
     */
    public Range createRange(Document doc)
    {
        return new DefaultRange(createJSRange(doc));
    }

    /**
     * @param doc The DOM document for which to create the range.
     * @return A new JavaScript range object for the specified document, created using Mozilla's API.
     */
    protected native JavaScriptObject createJSRange(Document doc) /*-{
        return doc.createRange();
    }-*/;
}
