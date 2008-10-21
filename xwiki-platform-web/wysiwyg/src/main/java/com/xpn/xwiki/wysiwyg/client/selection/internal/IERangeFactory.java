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

import com.google.gwt.dom.client.Document;
import com.xpn.xwiki.wysiwyg.client.selection.Range;
import com.xpn.xwiki.wysiwyg.client.selection.RangeCacheProxy;
import com.xpn.xwiki.wysiwyg.client.selection.RangeFactory;
import com.xpn.xwiki.wysiwyg.client.selection.internal.ie.NativeRange;
import com.xpn.xwiki.wysiwyg.client.selection.internal.ie.TextRange;

/**
 * {@link RangeFactory} implementation for Internet Explorer.
 * 
 * @version $Id$
 */
public final class IERangeFactory implements RangeFactory
{
    /**
     * {@inheritDoc}
     * 
     * @see RangeFactory#createRange(Document)
     */
    public Range createRange(Document doc)
    {
        return createRange(TextRange.newInstance(doc));
    }

    /**
     * @param jsRange native range object
     * @return A new Range, created based on the given native range.
     */
    public static Range createRange(NativeRange jsRange)
    {
        return new RangeCacheProxy(new IERange(jsRange));
    }

    /**
     * Tries to cast the given range to an {@link IERange} instance.
     * 
     * @param range the range to be casted
     * @return casting result
     */
    public static IERange cast(Range range)
    {
        if (!(range instanceof RangeCacheProxy)) {
            throw new ClassCastException("Expecting RangeCacheProxy!");
        }
        Range cachedRange = ((RangeCacheProxy) range).getCachedRange();
        if (!(cachedRange instanceof IERange)) {
            throw new ClassCastException("Expecting IERange!");
        }
        return (IERange) cachedRange;
    }
}
