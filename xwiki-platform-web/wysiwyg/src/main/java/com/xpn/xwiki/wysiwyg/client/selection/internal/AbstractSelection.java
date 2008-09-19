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

public abstract class AbstractSelection implements Selection
{
    private final JavaScriptObject jsSelection;

    AbstractSelection(JavaScriptObject jsSelection)
    {
        this.jsSelection = jsSelection;
    }

    public final JavaScriptObject getJSSelection()
    {
        return this.jsSelection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selection#addRange(Range)
     */
    public final void addRange(Range range)
    {
        addRange(((AbstractRange) range).getJSRange());
    }

    protected abstract void addRange(JavaScriptObject range);

    /**
     * {@inheritDoc}
     * 
     * @see Selection#removeRange(Range)
     */
    public final void removeRange(Range range)
    {
        removeRange(((AbstractRange) range).getJSRange());
    }

    protected abstract void removeRange(JavaScriptObject range);
}
