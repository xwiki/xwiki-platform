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

import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.gwt.api.server.XWikiServiceImpl;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;
import com.xpn.xwiki.wysiwyg.client.sync.SyncStatus;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;
import com.xpn.xwiki.wysiwyg.server.sync.SyncEngine;
import com.xpn.xwiki.wysiwyg.server.sync.DefaultSyncEngine;

public class DefaultWysiwygService extends XWikiServiceImpl implements WysiwygService
{
    private SyncEngine syncEngine;

    public DefaultWysiwygService()
    {
        super();

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

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#fromHTML(String)
     */
    public String fromHTML(String html, String syntax) throws XWikiGWTException
    {
        try {
            // We need to clean the editor's output because it isn't always HTML-valid
            return getHTMLConverter(syntax).fromHTML(cleanHTML(html));
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#toHTML(String)
     */
    public String toHTML(String source, String syntax) throws XWikiGWTException
    {
        try {
            return getHTMLConverter(syntax).toHTML(source);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#cleanHTML(String)
     */
    public String cleanHTML(String dirtyHTML) throws XWikiGWTException
    {
        try {
            return XMLUtils.toString(getHTMLCleaner().clean(dirtyHTML));
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygService#syncEditorContent(Revision, String, int)
     */
    public synchronized SyncResult syncEditorContent(Revision revision, String pageName, int version)
        throws XWikiGWTException
    {
        try {
            SyncStatus syncStatus = syncEngine.getSyncStatus(pageName);
            if (syncStatus == null) {
                XWikiDocument doc = getXWikiContext().getWiki().getDocument(pageName, getXWikiContext());
                syncStatus = new SyncStatus(pageName, toHTML(doc.getContent(), "xwiki/2.0"));
                syncEngine.setSyncStatus(pageName, syncStatus);
            }
            return syncEngine.sync(syncStatus, revision, version);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }
}
