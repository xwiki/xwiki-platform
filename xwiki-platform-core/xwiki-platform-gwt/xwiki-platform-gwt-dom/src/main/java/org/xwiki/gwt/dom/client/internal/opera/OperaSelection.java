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
package org.xwiki.gwt.dom.client.internal.opera;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.internal.DefaultSelection;
import org.xwiki.gwt.dom.client.internal.NativeRangeWrapper;
import org.xwiki.gwt.dom.client.internal.mozilla.NativeRange;
import org.xwiki.gwt.dom.client.internal.mozilla.NativeSelection;

import com.google.gwt.dom.client.Node;

/**
 * Fixes selection problems found in Firefox versions prior to 3.0.
 * 
 * @version $Id$
 */
public class OperaSelection extends DefaultSelection
{
    /**
     * Creates a new selection object.
     * 
     * @param nativeSelection the underlying native selection to be used
     */
    public OperaSelection(NativeSelection nativeSelection)
    {
        super(nativeSelection);
    }

    /**
     * {@inheritDoc}<br/>
     * Opera native selection reports sometimes a wrong range (e.g. when the selection ends before an image) so we
     * compute the range based on the native selection anchor and focus nodes which seem to be correct.
     * 
     * @see DefaultSelection#getRangeAt(int)
     */
    @Override
    public Range getRangeAt(int index)
    {
        if (index < 0 || index >= getRangeCount()) {
            throw new IndexOutOfBoundsException();
        }

        Node anchorNode = getNativeSelection().getAnchorNode();
        int anchorOffset = getNativeSelection().getAnchorOffset();

        Node focusNode = getNativeSelection().getFocusNode();
        int focusOffset = getNativeSelection().getFocusOffset();

        NativeRange nativeRange = getNativeSelection().getRangeAt(index);
        if (DOMUtils.getInstance().comparePoints(anchorNode, anchorOffset, focusNode, focusOffset) <= 0) {
            nativeRange.setStart(anchorNode, anchorOffset);
            nativeRange.setEnd(focusNode, focusOffset);
        } else {
            nativeRange.setStart(focusNode, focusOffset);
            nativeRange.setEnd(anchorNode, anchorOffset);
        }

        Range range = ((Document) nativeRange.getStartContainer().getOwnerDocument()).createRange();
        range.setStart(nativeRange.getStartContainer(), nativeRange.getStartOffset());
        range.setEnd(nativeRange.getEndContainer(), nativeRange.getEndOffset());
        ((NativeRangeWrapper) range).setNativeRange(nativeRange);

        return range;
    }
}
