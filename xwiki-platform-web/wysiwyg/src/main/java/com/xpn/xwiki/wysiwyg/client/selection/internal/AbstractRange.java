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
import com.xpn.xwiki.wysiwyg.client.selection.RangeCompare;

/**
 * Abstract range that implements the W3C range specification using the API offered by the browser. Concrete extensions
 * of these class have the role to adapt the specific range API offered by each browser to the W3C range specification.
 * 
 * @param <R> The underlying range API used to implement the W3C range specification.
 * @version $Id$
 */
public abstract class AbstractRange<R extends JavaScriptObject> implements Range
{
    /**
     * Browser specific range implementation.
     */
    private R jsRange;

    /**
     * Creates a new instance that has to adapt the given browser-specific range to the W3C specification.
     * 
     * @param jsRange The range implementation to adapt.
     */
    AbstractRange(R jsRange)
    {
        setJSRange(jsRange);
    }

    /**
     * @return The underlying range implementation used.
     */
    public final R getJSRange()
    {
        return jsRange;
    }

    /**
     * Sets the underlying range implementation to be used. This method should not be called unless the browser provides
     * more than one type of range and you need to swap between them.
     * 
     * @param jsRange The underlying range implementation to be used.
     */
    protected final void setJSRange(R jsRange)
    {
        this.jsRange = jsRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Range#compareBoundaryPoints(RangeCompare, Range)
     */
    @SuppressWarnings("unchecked")
    public short compareBoundaryPoints(RangeCompare how, Range sourceRange)
    {
        return compareBoundaryPoints(how, ((AbstractRange<R>) sourceRange).getJSRange());
    }

    /**
     * Compare the boundary-points of two Ranges in a document.
     * 
     * @param how The type of comparison.
     * @param sourceJSRange The Range on which this current Range is compared to.
     * @return -1, 0 or 1 depending on whether the corresponding boundary-point of the Range is respectively before,
     *         equal to, or after the corresponding boundary-point of sourceRange.
     */
    protected abstract short compareBoundaryPoints(RangeCompare how, R sourceJSRange);
}
