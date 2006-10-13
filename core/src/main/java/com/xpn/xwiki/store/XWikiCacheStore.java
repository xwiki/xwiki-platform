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
 * @author ludovic
 * @author sdumitriu
 * @author thomas
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;



public class XWikiCacheStore implements XWikiCacheStoreInterface {

    private static final Log log = LogFactory.getLog(XWikiCacheStore.class);

    private XWikiStoreInterface store;
    private XWikiCache cache;
    private XWikiCache pageExistCache;
    private int cacheCapacity = 100;
    private int pageExistCacheCapacity = 10000;

    public XWikiCacheStore(XWikiStoreInterface store, XWikiContext context) throws XWikiException {
        setStore(store);
        initCache(context);
    }

    public synchronized void initCache(XWikiContext context) throws XWikiException {
        if ((cache==null)||(pageExistCache==null))  {
            try {
                String capacity = context.getWiki().Param("xwiki.store.cache.capacity");
                if (capacity != null)
                    cacheCapacity = Integer.parseInt(capacity);
            } catch (Exception e) {
            }
            try {
                String capacity = context.getWiki().Param("xwiki.store.cache.pageexistcapacity");
                if (capacity != null)
                    pageExistCacheCapacity = Integer.parseInt(capacity);
            } catch (Exception e) {
            }
            initCache(cacheCapacity, pageExistCacheCapacity, context);
        }
    }

    public void initCache(int capacity, int pageExistCacheCapacity, XWikiContext context) throws XWikiException {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        setCache(cacheService.newCache("xwiki.store.pagecache", capacity));
        setPageExistCache(cacheService.newCache("xwiki.store.pageexistcache",pageExistCacheCapacity));
    }

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        saveXWikiDoc(doc, context, true);
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        String key = getKey(doc, context);
        synchronized(key) {
            store.saveXWikiDoc(doc, context, bTransaction);
            doc.setStore(store);
            // Make sure cache is initialized
            initCache(context);

            // We need to flush so that caches
            // on the cluster are informed about the change
            getCache().flushEntry(key);
            getCache().putInCache(key, doc);
            getPageExistCache().flushEntry(key);
            getPageExistCache().putInCache(key, new Boolean(true));
        }
    }

    public void flushCache() {
        if (cache!=null) {
          cache.flushAll();
          cache = null;
        }
        if (pageExistCache!=null) {
          pageExistCache.flushAll();
          pageExistCache = null;
        }
    }

    public String getKey(XWikiDocument doc, XWikiContext context) {
        return getKey(doc.getFullName(), doc.getLanguage(), context);
    }

    public String getKey(String fullName, String language, XWikiContext context) {
        String db = context.getDatabase();
        if (db==null)
            db = "";
        String key = db + ":" + fullName;
        if ("".equals(language))
            return key;
        else
            return key + ":" + language;
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        if (log.isDebugEnabled())
         log.debug("Cache: begin for doc " + key + " in cache");

        // Make sure cache is initialized
        initCache(context);

        synchronized (key) {
            try {
                if (log.isDebugEnabled())
                 log.debug("Cache: Trying to get doc " + key + " from cache");
                doc = (XWikiDocument) getCache().getFromCache(key);
                doc.setFromCache(true);
                if (log.isDebugEnabled())
                 log.debug("Cache: got doc " + key + " from cache");
            } catch (XWikiCacheNeedsRefreshException e) {
                try {
                    if (log.isDebugEnabled())
                     log.debug("Cache: Trying to get doc " + key + " for real");
                    doc = store.loadXWikiDoc(doc, context);
                    doc.setStore(store);
                    if (log.isDebugEnabled())
                     log.debug("Cache: Got doc " + key + " for real");
                } catch (XWikiException xwikiexception) {
                    if (log.isDebugEnabled())
                     log.debug("Cache: exception while getting doc " + key + " for real");
                    getCache().cancelUpdate(key);
                    throw xwikiexception;
                }
                if (log.isDebugEnabled())
                 log.debug("Cache: put doc " + key + " in cache");

                getCache().putInCache(key, doc);
                getPageExistCache().putInCache(key, new Boolean(!doc.isNew()));
            }
        }
        if (log.isDebugEnabled())
         log.debug("Cache: end for doc " + key + " in cache");
        return doc;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        synchronized(key) {
            store.deleteXWikiDoc(doc, context);

            // Make sure cache is initialized
            initCache(context);

            getCache().flushEntry(key);
            getPageExistCache().flushEntry(key);
            getPageExistCache().putInCache(key, new Boolean(false));
        }
    }

    public List getClassList(XWikiContext context) throws XWikiException {
        return store.getClassList(context);
    }

    public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException {
        return store.searchDocumentsNames(wheresql, context);
    }

    public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return store.searchDocumentsNames(wheresql, nb, start, context);
    }

    public List searchDocumentsNames(String wheresql, int nb, int start, String selectColumns, XWikiContext context) throws XWikiException {
        return store.searchDocumentsNames(wheresql, nb, start, selectColumns, context);
    }

    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context) throws XWikiException {
        return store.isCustomMappingValid(bclass, custommapping1, context);
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context) throws XWikiException {
        return store.injectCustomMapping(doc1class, context);
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException {
        return store.injectCustomMappings(doc, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, distinctbyname, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbyname, int nb, int start, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, distinctbyname, nb, start, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, nb, start, context);
    }

    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, context);
    }

    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, nb, start, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, checkRight, nb, start, context);
    }

    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
        return store.loadLock(docId, context, bTransaction);
    }

    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveLock(lock, context, bTransaction);
    }

    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.deleteLock(lock, context, bTransaction);
    }

    public List loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
        return store.loadLinks(docId, context, bTransaction);
    }

    public List loadBacklinks(String fullName, XWikiContext context, boolean bTransaction) throws XWikiException {
        return store.loadBacklinks(fullName, context, bTransaction);
    }

    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveLinks(doc, context, bTransaction);
    }

    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.deleteLinks(docId, context, bTransaction);
    }

     public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
        return store.search(sql, nb, start, context);
    }

    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context) throws XWikiException {
        return store.search(sql, nb, start, whereParams, context);
    }

    public synchronized void cleanUp(XWikiContext context) {
        store.cleanUp(context);
    }

    public void createWiki(String wikiName, XWikiContext context) throws XWikiException {
        synchronized(wikiName) {
            store.createWiki(wikiName, context);
        }
    }

    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc,context);
        initCache(context);
        synchronized(key) {
            try {
                try {
                    Boolean result = (Boolean) getPageExistCache().getFromCache(key);
                    return result.booleanValue();
                } catch (XWikiCacheNeedsRefreshException e) {
                    getPageExistCache().cancelUpdate(key);
                }
            } catch (Exception e) {
            }
            boolean result = store.exists(doc, context);
            getPageExistCache().putInCache(key, new Boolean(result));
            return result;
        }
    }

    public XWikiCache getCache() {
        return cache;
    }

    public void setCache(XWikiCache cache) {
        this.cache = cache;
    }

    public XWikiCache getPageExistCache() {
        return pageExistCache;
    }

    public void setPageExistCache(XWikiCache pageExistCache) {
        this.pageExistCache = pageExistCache;
    }

    public List getCustomMappingPropertyList(BaseClass bclass) {
        return store.getCustomMappingPropertyList(bclass);
    }

    public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException {
        store.injectCustomMappings(context);
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException {
        store.injectUpdatedCustomMappings(context);
    }

	public List getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException {
		return store.getTranslationList(doc, context);
	}
}
