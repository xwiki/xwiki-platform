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
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTAppConstants;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

public interface WysiwygService extends XWikiService
{
    public static final class Singleton
    {
        private static WysiwygServiceAsync instance;

        public static synchronized WysiwygServiceAsync getInstance()
        {
            if (instance == null) {
                instance = (WysiwygServiceAsync) GWT.create(WysiwygService.class);
                String baseURL;
                if (GWT.isScript()) {
                    baseURL = XWikiGWTAppConstants.XWIKI_DEFAULT_BASE_URL;
                } else {
                    baseURL = GWT.getModuleBaseURL();
                    if (baseURL.endsWith("/")) {
                        baseURL = baseURL.substring(0, baseURL.length() - 1);
                    }
                }
                String serviceEntryPoint = baseURL + Constants.WYSIWYG_DEFAULT_SERVICE;
                ((ServiceDefTarget) instance).setServiceEntryPoint(serviceEntryPoint);
            }
            return instance;
        }
    }

    String fromXHTML(String xhtml, String syntax) throws XWikiGWTException;

    String toXHTML(String source, String syntax) throws XWikiGWTException;

    String cleanXHTML(String dirtyXHTML) throws XWikiGWTException;

    SyncResult syncEditorContent(Revision syncedRevision, String pageName, int version) throws XWikiGWTException;
}
