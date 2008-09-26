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
package com.xpn.xwiki.wysiwyg.server;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;
import com.xpn.xwiki.wysiwyg.client.sync.SyncStatus;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;
import com.xpn.xwiki.wysiwyg.server.sync.DefaultSyncEngine;
import com.xpn.xwiki.wysiwyg.server.sync.SyncEngine;

public class DefaultWysiwygService extends RemoteServiceServlet implements WysiwygService
{
    private SyncEngine syncEngine;

    public DefaultWysiwygService()
    {
        syncEngine = new DefaultSyncEngine();
    }

    private HTMLCleaner getHTMLCleaner()
    {
        return (HTMLCleaner) Utils.getComponent(HTMLCleaner.ROLE);
    }

    private HTMLConverter getHTMLConverter(String syntax)
    {
        return (HTMLConverter) Utils.getComponent(HTMLConverter.ROLE, syntax);
    }

    private DocumentAccessBridge getDocumentAccessBridge()
    {
        return (DocumentAccessBridge) Utils.getComponent(DocumentAccessBridge.ROLE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#fromHTML(String, String)
     */
    public String fromHTML(String html, String syntax)
    {
        // We need to clean the editor's output because it isn't always HTML-valid
        return getHTMLConverter(syntax).fromHTML(cleanHTML(html));
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#toHTML(String, String)
     */
    public String toHTML(String source, String syntax)
    {
        return getHTMLConverter(syntax).toHTML(source);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#cleanHTML(String)
     */
    public String cleanHTML(String dirtyHTML)
    {
        return XMLUtils.toString(getHTMLCleaner().clean(dirtyHTML));
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#syncEditorContent(Revision, String, int)
     */
    public synchronized SyncResult syncEditorContent(Revision revision, String pageName, int version)
    {
        try {
            SyncStatus syncStatus = syncEngine.getSyncStatus(pageName);
            if (syncStatus == null) {
                String content = getDocumentAccessBridge().getDocumentContent(pageName);
                syncStatus = new SyncStatus(pageName, toHTML(content, "xwiki/2.0"));
                syncEngine.setSyncStatus(pageName, syncStatus);
            }
            return syncEngine.sync(syncStatus, revision, version);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
