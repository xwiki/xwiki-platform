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
import com.xpn.xwiki.wysiwyg.client.dom.Range;

/**
 * Fixes selection problems found in Firefox versions prior to 3.0.
 * 
 * @version $Id$
 */
public class MozillaSelection extends DefaultSelection
{
    /**
     * Creates a new instance that wraps the given native selection object.
     * 
     * @param jsSelection The native selection object to be wrapped.
     */
    MozillaSelection(JavaScriptObject jsSelection)
    {
        super(jsSelection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultSelection#getRangeAt(int)
     */
    public Range getRangeAt(int index)
    {
        return new MozillaRange(getJSRangeAt(index));
    }
}
