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
package com.xpn.xwiki.wysiwyg.client.dom.internal.mozilla;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.internal.DefaultSelection;
import com.xpn.xwiki.wysiwyg.client.dom.internal.NativeRangeWrapper;

/**
 * Fixes selection problems found in Firefox versions prior to 3.0.
 * 
 * @version $Id$
 */
public class MozillaSelection extends DefaultSelection
{
    /**
     * Creates a new selection object.
     * 
     * @param nativeSelection the underlying native selection to be used
     */
    public MozillaSelection(NativeSelection nativeSelection)
    {
        super(nativeSelection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultSelection#addRange(Range)
     */
    public void addRange(Range range)
    {
        NativeRangeWrapper wrapper = ((NativeRangeWrapper) range);
        Document doc = (Document) range.getStartContainer().getOwnerDocument();
        if (wrapper.getNativeRange() == null) {
            wrapper.setNativeRange(NativeRange.newInstance(doc));
        } else {
            // Firefox prior to version 3.0 throws NS_ERROR_ILLEGAL_VALUE if we try to set the start boundary of the
            // native range after the end boundary, or the other way around. Firefox 3 collapses the native range in
            // this case. The DefaultRange already implements this behavior so the input range should be valid. We
            // select all content to avoid the exception in Firefox 2.
            ((NativeRange) wrapper.getNativeRange()).selectNode(doc.getBody());
        }
        NativeRange nativeRange = wrapper.getNativeRange().cast();
        nativeRange
            .setStart(range.getStartContainer(), adjustOffset(range.getStartContainer(), range.getStartOffset()));
        nativeRange.setEnd(range.getEndContainer(), adjustOffset(range.getEndContainer(), range.getEndOffset()));
        getNativeSelection().addRange(nativeRange);
    }

    /**
     * Adjusts the specified offset within the given node to avoid NS_ERROR_DOM_INDEX_SIZE_ERR.
     * 
     * @param node A DOM node.
     * @param offset The offset within the given node.
     * @return the adjusted value of the specified offset.
     */
    protected int adjustOffset(Node node, int offset)
    {
        if (node.getNodeType() == DOMUtils.COMMENT_NODE) {
            // Only 0 is allowed.
            return 0;
        } else {
            return offset;
        }
    }
}
