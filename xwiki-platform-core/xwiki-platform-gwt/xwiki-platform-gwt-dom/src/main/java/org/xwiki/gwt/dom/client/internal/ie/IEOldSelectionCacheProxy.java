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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.Range;

/**
 * Extends {@link IEOldSelection} with the ability to cache returned ranges. The cache is invalidated when the
 * underlying native selection changes.
 * 
 * @version $Id$
 */
public class IEOldSelectionCacheProxy extends IEOldSelection
{
    /**
     * The cached range.
     */
    private Range cachedRange;

    /**
     * The object notified when the selection changes.
     */
    private JavaScriptObject selectionChangeHandler;

    /**
     * Creates a new instance that wraps the given native selection object.
     * 
     * @param nativeSelection the underlying native selection object to be used
     */
    public IEOldSelectionCacheProxy(NativeSelection nativeSelection)
    {
        super(nativeSelection);
        invalidateCacheOnSelectionChange(nativeSelection.getOwnerDocument());
    }

    @Override
    public Range getRangeAt(int index)
    {
        if (cachedRange == null) {
            cachedRange = super.getRangeAt(index);
        } else if (index != 0) {
            throw new IndexOutOfBoundsException();
        }
        return cachedRange;
    }

    @Override
    public void removeAllRanges()
    {
        super.removeAllRanges();
        // In IE6 the selection change event is not fired when we select hidden text. It's good to invalidate the cache
        // after removing all ranges, which usually happens before selecting a new range.
        invalidateCache();
    }

    /**
     * @return the object notified when the selection changes
     */
    private native JavaScriptObject getSelectionChangeHandler()
    /*-{
        if (!this.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::selectionChangeHandler) {
            var self = this;
            this.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::selectionChangeHandler = function() {
                self.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::invalidateCache()();
            }
        }
        return this.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::selectionChangeHandler;
    }-*/;

    /**
     * Sets the {@link #cachedRange} to {@code null} when the selection changes in the given document.
     * 
     * @param document a DOM document
     */
    private native void invalidateCacheOnSelectionChange(Document document)
    /*-{
        var handler = this.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::getSelectionChangeHandler()();
        document.attachEvent('onselectionchange', handler);
        // The selection change event is not triggered when we type and thus we are forced to catch key the down event.
        document.body.attachEvent('onkeydown', handler);
    }-*/;

    /**
     * Call this method when you want to release this selection object. This way the selection object will unregister
     * its listeners preventing a memory leak.
     */
    public native void release()
    /*-{
        var handler = this.@org.xwiki.gwt.dom.client.internal.ie.IEOldSelectionCacheProxy::getSelectionChangeHandler()();
        document.detachEvent('onselectionchange', handler);
        document.body.detachEvent('onkeydown', handler);
    }-*/;

    /**
     * Invalidates the cached range.
     */
    private void invalidateCache()
    {
        cachedRange = null;
    }
}
