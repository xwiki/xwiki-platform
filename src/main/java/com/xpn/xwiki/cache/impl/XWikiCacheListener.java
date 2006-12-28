package com.xpn.xwiki.cache.impl;

import com.opensymphony.oscache.base.events.*;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

 * Catch when a doc is flushed from the cache so we can send our own
 * event.
 *
 * This event is mostly called due to jgroups flushing items from the cache.
 *
 * @author fitzgerald
 * @author wr0ngway
 * 
 */
public class XWikiCacheListener implements CacheEntryEventListener
{
    private static final Log log = LogFactory.getLog(XWikiCacheListener.class);
    
    private static XWiki xwiki;

    public static void setXWiki(XWiki xwiki)
    {
        XWikiCacheListener.xwiki = xwiki;
    }

    public void cacheEntryAdded(CacheEntryEvent event)
    {

    }

    public void cacheEntryFlushed(CacheEntryEvent event)
    {
        String sKey = event.getKey();

        // HACK: Figure out a better way to get a handle on the Context and XWiki here.
        
        XWikiContext context = new XWikiContext();
        context.setDatabase(xwiki.getDatabase());
        context.setWiki(xwiki);
        XWikiDocument doc = null;
        try
        {
            doc = new XWikiDocument();
            String[] parts = sKey.split(":\\.");
            switch (parts.length)
            {
                case 4:
                    doc.setLanguage(parts[3]);
                case 3:
                    doc.setDatabase(parts[0]);
                    doc.setSpace(parts[1]);
                    doc.setName(parts[2]);
                    doc = xwiki.getStore().loadXWikiDoc(doc, context);
                    break;
                case 2:
                    doc.setSpace(parts[0]);
                    doc.setName(parts[1]);
                    doc = xwiki.getStore().loadXWikiDoc(doc, context);
                    break;
                default:
                    log.error("Failed to parse document id from cache key: " + sKey);
                    break;
            }
        }
        catch (Exception e)
        {
            log.error("cacheEntryFlushedError for key " + sKey + ": ", e);
        }

        if (doc != null)
        {
            // TODO: need to create an xwiki event code for when a cluster
            // member receives a cache flush for an object so that plugins that
            // need to know a document has changed can update their state (e.g.
            // email notification, lucene plugin)
            //
            // This doesn't really work since the context may not have stuff like the request object
            xwiki.getNotificationManager().verify(doc, "flush", context);
        }
        else
        {
            log.error("cannot send flush notification doc is null for key " + sKey);
        }

        log.info("entry flushed: " + sKey);
    }

    public void cacheEntryRemoved(CacheEntryEvent event)
    {

    }

    public void cacheEntryUpdated(CacheEntryEvent event)
    {

    }

    public void cacheGroupFlushed(CacheGroupEvent event)
    {
    }

    public void cachePatternFlushed(CachePatternEvent event)
    {
    }

    public void cacheFlushed(CachewideEvent event)
    {
    }
}
                   