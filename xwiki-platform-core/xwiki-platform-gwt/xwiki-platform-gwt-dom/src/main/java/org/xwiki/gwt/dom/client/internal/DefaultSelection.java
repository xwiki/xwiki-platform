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
package org.xwiki.gwt.dom.client.internal;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.internal.mozilla.NativeRange;
import org.xwiki.gwt.dom.client.internal.mozilla.NativeSelection;

/**
 * The default selection implementation for browsers supporting the W3C Range specification and following Mozilla
 * Selection API.
 * 
 * @version $Id$
 */
public class DefaultSelection extends AbstractSelection
{
    /**
     * The underlying native selection object provided by the browser.
     */
    private final NativeSelection nativeSelection;

    /**
     * Creates a new selection object. This object will handle the conversion of {@link Range} objects to the native
     * range supported by the browser. The native ranges obtained will be applied to the underlying native selection.
     * 
     * @param nativeSelection the underlying native selection to be used
     */
    public DefaultSelection(NativeSelection nativeSelection)
    {
        this.nativeSelection = nativeSelection;
    }

    /**
     * @return {@link #nativeSelection}
     */
    protected NativeSelection getNativeSelection()
    {
        return nativeSelection;
    }

    @Override
    public void addRange(Range range)
    {
        NativeRangeWrapper wrapper = (NativeRangeWrapper) range;
        if (wrapper.getNativeRange() == null) {
            Document doc = (Document) range.getStartContainer().getOwnerDocument();
            wrapper.setNativeRange(NativeRange.newInstance(doc));
        }
        NativeRange nativeRange = wrapper.getNativeRange().cast();
        nativeRange.setStart(range.getStartContainer(), range.getStartOffset());
        nativeRange.setEnd(range.getEndContainer(), range.getEndOffset());
        getNativeSelection().addRange(nativeRange);
        DOMUtils.getInstance().scrollIntoView(range);
    }

    @Override
    public Range getRangeAt(int index)
    {
        if (index < 0 || index >= getRangeCount()) {
            throw new IndexOutOfBoundsException();
        }

        NativeRange nativeRange = getNativeSelection().getRangeAt(index);
        Range range = ((Document) nativeRange.getStartContainer().getOwnerDocument()).createRange();
        range.setStart(nativeRange.getStartContainer(), nativeRange.getStartOffset());
        range.setEnd(nativeRange.getEndContainer(), nativeRange.getEndOffset());
        ((NativeRangeWrapper) range).setNativeRange(nativeRange);

        return range;
    }

    @Override
    public int getRangeCount()
    {
        return getNativeSelection().getRangeCount();
    }

    @Override
    public void removeAllRanges()
    {
        getNativeSelection().removeAllRanges();
    }

    @Override
    public void removeRange(Range range)
    {
        NativeRangeWrapper wrapper = (NativeRangeWrapper) range;
        getNativeSelection().removeRange((NativeRange) wrapper.getNativeRange());
    }
}
