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
package org.xwiki.gwt.wysiwyg.client.cleaner;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Cleaner interface to be used on the client. It should have all the methods from {@link HTMLCleaner} with an
 * additional {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface HTMLCleanerAsync
{
    /**
     * Makes a request to the server to clean the given HTML fragment.
     * 
     * @param dirtyHTML the string containing the HTML output of the WYSIWYG editor
     * @param callback the object used to notify the caller when the server response is received
     */
    void clean(String dirtyHTML, AsyncCallback<String> callback);
}
