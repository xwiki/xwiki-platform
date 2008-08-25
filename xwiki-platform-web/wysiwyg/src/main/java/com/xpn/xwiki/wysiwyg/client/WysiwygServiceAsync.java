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
import com.xpn.xwiki.gwt.api.client.XWikiServiceAsync;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;

public interface WysiwygServiceAsync extends XWikiServiceAsync
{
    void fromHTML(String html, String syntax, AsyncCallback<String> async);

    void toHTML(String source, String syntax, AsyncCallback<String> async);

    void cleanHTML(String dirtyHTML, AsyncCallback<String> async);

    void syncEditorContent(Revision syncedRevision, String pageName, int version, AsyncCallback<SyncResult> async);
}
