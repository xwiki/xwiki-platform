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
package org.xwiki.gwt.user.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * An {@link AsyncCallback} that can be canceled before it is called.
 * 
 * @param <T> response data type
 * @version $Id$
 */
public class CancelableAsyncCallback<T> implements AsyncCallback<T>
{
    /**
     * The call-back that can be canceled.
     */
    private final AsyncCallback<T> callback;

    /**
     * Flag indicating if this call-back was canceled.
     */
    private boolean canceled;

    /**
     * Wraps the given call-back and add the ability to cancel it.
     * 
     * @param callback the call-back that might be canceled
     */
    public CancelableAsyncCallback(AsyncCallback<T> callback)
    {
        this.callback = callback;
    }

    @Override
    public void onFailure(Throwable caught)
    {
        if (!isCanceled()) {
            callback.onFailure(caught);
        }
    }

    @Override
    public void onSuccess(T result)
    {
        if (!isCanceled()) {
            callback.onSuccess(result);
        }
    }

    /**
     * @return {@code true} if this call-back is canceled, {@code false} otherwise
     */
    public boolean isCanceled()
    {
        return canceled;
    }

    /**
     * Sets the canceled state of this call-back.
     * 
     * @param canceled {@code true} to cancel this call-back, {@code false} to reactivate it
     */
    public void setCanceled(boolean canceled)
    {
        this.canceled = canceled;
    }
}
