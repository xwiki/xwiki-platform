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

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @version $Id$
 * @param <T> The type of the return value that was declared in the synchronous version of the method. If the return
 *            type is a primitive, use the boxed version of that primitive (for example, an {@code int} return type
 *            becomes an {@link Integer} type argument, and a {@code void} return type becomes a {@link Void} type
 *            argument, which is always {@code null}).
 */
public class NativeAsyncCallback<T> implements AsyncCallback<T>
{
    /**
     * The JavaScript function to call on success.
     */
    private final JavaScriptObject onSuccess;

    /**
     * The JavaScript function to call on failure.
     */
    private final JavaScriptObject onFailure;

    /**
     * Creates a new native {@link AsyncCallback} by wrapping the given JavaScript functions.
     * 
     * @param onSuccess the JavaScript function to call on success
     * @param onFailure the JavaScript function to call on failure
     */
    public NativeAsyncCallback(JavaScriptObject onSuccess, JavaScriptObject onFailure)
    {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public native void onFailure(Throwable caught)
    /*-{
        var onFailure = this.@org.xwiki.gwt.user.client.NativeAsyncCallback::onFailure;
        if (typeof onFailure == 'function') {
            var message;
            if (caught) {
                message = caught.@java.lang.Throwable::getMessage()();
            }
            onFailure(message);
        }
    }-*/;

    @Override
    public native void onSuccess(T result)
    /*-{
        var onSuccess = this.@org.xwiki.gwt.user.client.NativeAsyncCallback::onSuccess;
        if (typeof onSuccess == 'function') {
            onSuccess(result);
        }
    }-*/;
}
