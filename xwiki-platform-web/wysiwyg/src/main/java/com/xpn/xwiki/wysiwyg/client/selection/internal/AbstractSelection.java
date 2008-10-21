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
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.Selection;

/**
 * Abstract selection that implements the Mozilla range specification using the API offered by the browser. Concrete
 * extensions of these class have the role to adapt the specific selection API offered by each browser to the Mozilla
 * selection specification.
 * 
 * @param <S> Browser specific selection implementation.
 * @param <R> Browser specific range implementation.
 * @version $Id$
 */
public abstract class AbstractSelection<S extends JavaScriptObject, R extends JavaScriptObject> implements Selection
{
    /**
     * Browser specific selection implementation.
     */
    private final S jsSelection;

    /**
     * Creates a new instance that has to adapt the given browser-specific selection to the Mozilla specification.
     * 
     * @param jsSelection The selection implementation to adapt.
     */
    AbstractSelection(S jsSelection)
    {
        this.jsSelection = jsSelection;
    }

    /**
     * @return The underlying selection implementation used.
     */
    public final S getJSSelection()
    {
        return this.jsSelection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#addRange(Range)
     */
    @SuppressWarnings("unchecked")
    public void addRange(Range range)
    {
        addRange(((AbstractRange<R>) range).getJSRange());
    }

    /**
     * @param range Adds this range to the current selection.
     */
    protected abstract void addRange(R range);

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeRange(Range)
     */
    @SuppressWarnings("unchecked")
    public void removeRange(Range range)
    {
        removeRange(((AbstractRange<R>) range).getJSRange());
    }

    /**
     * @param range Removes this range from the current selection.
     */
    protected abstract void removeRange(R range);
}
