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
package com.xpn.xwiki.wysiwyg.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

/**
 * Service interface used on the client. It should have all the methods from {@link WysiwygService} with an additional
 * {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface WysiwygServiceAsync
{
    /**
     * Makes a request to the server to convert the given HTML fragment to the specified syntax.
     * 
     * @param html The HTML fragment to be converted.
     * @param syntax The syntax of the result.
     * @param async The callback to be used for notifying the caller after receiving the response from the server.
     */
    void fromHTML(String html, String syntax, AsyncCallback<String> async);

    /**
     * Makes a request to the server to convert the given text from the specified syntax to HTML.
     * 
     * @param source The text to be converted.
     * @param syntax The syntax of the given text.
     * @param async The callback to be used for notifying the caller after receiving the response from the server.
     */
    void toHTML(String source, String syntax, AsyncCallback<String> async);

    /**
     * Makes a request to the server to clean the given HTML fragment.
     * 
     * @param dirtyHTML The HTML fragment to be cleaned.
     * @param async The callback to be used for notifying the caller after receiving the response from the server.
     */
    void cleanHTML(String dirtyHTML, AsyncCallback<String> async);

    /**
     * Synchronizes this editor with others that edit the same page.
     * 
     * @param syncedRevision The changes to this editor's content, since the last update.
     * @param pageName The page being edited.
     * @param version The version affected by syncedRevision.
     * @param async The callback to be used for notifying the caller after receiving the response from the server.
     */
    void syncEditorContent(Revision syncedRevision, String pageName, int version, AsyncCallback<SyncResult> async);
}
