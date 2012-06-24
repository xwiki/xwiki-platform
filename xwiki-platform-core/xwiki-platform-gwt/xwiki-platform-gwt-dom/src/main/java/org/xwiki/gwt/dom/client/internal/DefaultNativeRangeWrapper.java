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

import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.Range;

/**
 * Wraps a native range to be used by the selection who created this object. For instance, in order to remove a range
 * from the selection we must keep a reference to the native range returned by the native selection. The selection
 * implementation is responsible for setting and handling the reference to the native range.
 * 
 * @version $Id$
 */
public class DefaultNativeRangeWrapper extends DefaultRange implements NativeRangeWrapper
{
    /**
     * The native range wrapped by this object.
     */
    private JavaScriptObject nativeRange;

    @Override
    public JavaScriptObject getNativeRange()
    {
        return nativeRange;
    }

    @Override
    public void setNativeRange(JavaScriptObject nativeRange)
    {
        this.nativeRange = nativeRange;
    }

    @Override
    public Range cloneRange()
    {
        // We should use Object.clone when it is implemented in GWT
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=1843
        DefaultNativeRangeWrapper clone = new DefaultNativeRangeWrapper();
        clone.setStart(getStartContainer(), getStartOffset());
        clone.setEnd(getEndContainer(), getEndOffset());
        return clone;
    }
}
