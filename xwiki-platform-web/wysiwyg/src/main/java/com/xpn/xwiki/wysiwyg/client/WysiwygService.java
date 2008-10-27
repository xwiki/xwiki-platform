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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

/**
 * The service interface used on the server.
 * 
 * @version $Id$
 */
public interface WysiwygService extends RemoteService
{
    /**
     * Utility class for accessing the service stub.
     */
    public static final class Singleton
    {
        /**
         * The service stub.
         */
        private static WysiwygServiceAsync instance;

        /**
         * Private constructor because this is a utility class.
         */
        private Singleton()
        {
        }

        /**
         * @return The service stub.
         */
        public static synchronized WysiwygServiceAsync getInstance()
        {
            if (instance == null) {
                String moduleBaseURL = GWT.getModuleBaseURL();
                String baseURL = moduleBaseURL.substring(0, moduleBaseURL.indexOf(GWT.getModuleName()));

                instance = (WysiwygServiceAsync) GWT.create(WysiwygService.class);
                ((ServiceDefTarget) instance).setServiceEntryPoint(baseURL + "WysiwygService");

                // We cache the service calls.
                instance = new WysiwygServiceAsyncCacheProxy(instance);
            }
            return instance;
        }
    }

    /**
     * @param html The HTML fragment to be converted.
     * @param syntax The syntax of the result.
     * @return The result of converting the given HTML fragment to the specified syntax.
     */
    String fromHTML(String html, String syntax);

    /**
     * @param source The text to be converted.
     * @param syntax The syntax of the given text.
     * @return The result of converting the given text from the specified syntax to HTML.
     */
    String toHTML(String source, String syntax);

    /**
     * @param dirtyHTML The HTML fragment to be cleaned.
     * @return The result of cleaning the given HTML fragment.
     */
    String cleanHTML(String dirtyHTML);

    /**
     * @param syncedRevision The changes to this editor's content, since the last update.
     * @param pageName The page being edited.
     * @param version The version affected by syncedRevision.
     * @return The result of synchronizing this editor with others editing the same page.
     */
    SyncResult syncEditorContent(Revision syncedRevision, String pageName, int version);
}
