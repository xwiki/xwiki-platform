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
package com.xpn.xwiki.wysiwyg.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A cancelable {@link AsyncCallback}.
 * 
 * @version $Id$
 * @param <T> The type of the return value that was declared in the synchronous version of the method. If the return
 *            type is a primitive, use the boxed version of that primitive (for example, an {@code int} return type
 *            becomes an {@link Integer} type argument, and a {@code void} return type becomes a {@link Void} type
 *            argument, which is always {@code null}).
 */
public class CancelableAsyncCallback<T> implements AsyncCallback<T>
{
    /**
     * The underlying callback.
     */
    private final AsyncCallback<T> callback;

    /**
     * Flag indicating if this callback was canceled or not.
     */
    private boolean canceled;

    /**
     * Wraps the given callback, providing a way to cancel it.
     * 
     * @param callback the underlying callback
     */
    public CancelableAsyncCallback(AsyncCallback<T> callback)
    {
        this.callback = callback;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onFailure(Throwable)
     */
    public void onFailure(Throwable caught)
    {
        if (!canceled) {
            callback.onFailure(caught);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onSuccess(Object)
     */
    public void onSuccess(T result)
    {
        if (!canceled) {
            callback.onSuccess(result);
        }
    }

    /**
     * Sets the canceled state of this callback.
     * 
     * @param canceled {@code true} to cancel this callback, {@code false} otherwise
     */
    public void setCanceled(boolean canceled)
    {
        this.canceled = canceled;
    }

    /**
     * @return {@code true} if this callback was canceled, {@code false} otherwise
     */
    public boolean isCanceled()
    {
        return canceled;
    }
}
