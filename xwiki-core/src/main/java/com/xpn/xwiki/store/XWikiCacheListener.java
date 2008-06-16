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
 *
 * Allows initilization of OSCache in the proper way to use JGroups for
 * clustering. Basically this involves reading the oscache.properties file
 * located in WEB-INF/classes. This contains a listener setting for JGroups (and
 * other settings we could use if desired).
 *
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;

public class XWikiCacheListener implements CacheEntryListener<XWikiDocument>
{
    private static final Log log = LogFactory.getLog(XWikiCacheListener.class);

    private XWikiContext context;

    public XWikiCacheListener(XWikiContext context)
    {
        this.context = (XWikiContext) context.clone();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryAdded(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryAdded(CacheEntryEvent<XWikiDocument> event)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryRemoved(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryRemoved(CacheEntryEvent<XWikiDocument> event)
    {
        CacheEntry<XWikiDocument> entry = event.getEntry();

        String key = entry.getKey();

        XWikiDocument doc = null;
        try {
            doc = new XWikiDocument();
            String[] parts = key.split(":\\.");
            switch (parts.length) {
                case 4:
                    doc.setLanguage(parts[3]);
                case 3:
                    doc.setDatabase(parts[0]);
                    doc.setSpace(parts[1]);
                    doc.setName(parts[2]);
                    doc = context.getWiki().getStore().loadXWikiDoc(doc, context);
                    break;
                case 2:
                    doc.setSpace(parts[0]);
                    doc.setName(parts[1]);
                    doc = context.getWiki().getStore().loadXWikiDoc(doc, context);
                    break;
                default:
                    log.error("Failed to parse document id from cache key: " + key);
                    break;
            }
        } catch (Exception e) {
            log.error("cacheEntryFlushedError for key " + key + ": ", e);
        }

        if (doc != null) {
            // TODO: need to create an xwiki event code for when a cluster
            // member receives a cache flush for an object so that plugins that
            // need to know a document has changed can update their state (e.g.
            // email notification, lucene plugin)
            //
            // This doesn't really work since the context may not have stuff like the request object
            context.getWiki().getNotificationManager().verify(doc, "flush", context);
        } else {
            log.error("cannot send flush notification doc is null for key " + key);
        }

        log.info("entry flushed: " + key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryModified(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryModified(CacheEntryEvent<XWikiDocument> event)
    {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryAccessed(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryAccessed(CacheEntryEvent<XWikiDocument> event)
    {
    }
}
