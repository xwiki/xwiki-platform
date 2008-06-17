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
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import java.util.List;

/**
 * A proxy store implementation that caches Documents when they are first fetched and subsequently return them from a
 * cache. It delegates all write and search operations to an underlying store without doing any caching on them.
 * 
 * @version $Id$
 */
public class XWikiCacheStore implements XWikiCacheStoreInterface
{
    private static final Log log = LogFactory.getLog(XWikiCacheStore.class);

    private XWikiStoreInterface store;

    private Cache<XWikiDocument> cache;

    private XWikiCacheListener cacheListener;

    private Cache<Boolean> pageExistCache;

    private int cacheCapacity = 100;

    private int pageExistCacheCapacity = 10000;

    public XWikiCacheStore(XWikiStoreInterface store, XWikiContext context) throws XWikiException
    {
        this.cacheListener = new XWikiCacheListener(context);

        setStore(store);
        initCache(context);
    }

    public synchronized void initCache(XWikiContext context) throws XWikiException
    {
        if ((cache == null) || (pageExistCache == null)) {
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

    public void initCache(int capacity, int pageExistCacheCapacity, XWikiContext context) throws XWikiException
    {
        CacheFactory cacheFactory = context.getWiki().getCacheFactory();
        
        try {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId("xwiki.store.pagecache");
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(capacity);
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            Cache<XWikiDocument> pageCache = cacheFactory.newCache(cacheConfiguration);
            setCache(pageCache);

            cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId("xwiki.store.pageexistcache");
            lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(pageExistCacheCapacity);
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            Cache<Boolean> pageExistcache = cacheFactory.newCache(cacheConfiguration);
            setPageExistCache(pageExistcache);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to initialize cache", e);
        }
    }

    public XWikiStoreInterface getStore()
    {
        return store;
    }

    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        saveXWikiDoc(doc, context, true);
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        String key = getKey(doc, context);
        synchronized (key) {
            store.saveXWikiDoc(doc, context, bTransaction);
            doc.setStore(store);
            // Make sure cache is initialized
            initCache(context);

            // We need to flush so that caches
            // on the cluster are informed about the change
            getCache().remove(key);
            getCache().set(key, doc);
            getPageExistCache().remove(key);
            getPageExistCache().set(key, new Boolean(true));
        }
    }

    public void flushCache()
    {
        if (cache != null) {
            cache.removeAll();
            cache = null;
        }
        if (pageExistCache != null) {
            pageExistCache.removeAll();
            pageExistCache = null;
        }
    }

    public String getKey(XWikiDocument doc, XWikiContext context)
    {
        return getKey(doc.getFullName(), doc.getLanguage(), context);
    }

    public String getKey(String fullName, String language, XWikiContext context)
    {
        String db = context.getDatabase();
        if (db == null)
            db = "";
        String key = db + ":" + fullName;
        if ("".equals(language))
            return key;
        else
            return key + ":" + language;
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);
        if (log.isDebugEnabled())
            log.debug("Cache: begin for doc " + key + " in cache");

        // Make sure cache is initialized
        initCache(context);

        synchronized (key) {
            if (log.isDebugEnabled()) {
                log.debug("Cache: Trying to get doc " + key + " from cache");
            }

            XWikiDocument cachedoc = getCache().get(key);

            if (cachedoc != null) {
                doc = cachedoc;
                doc.setFromCache(true);

                if (log.isDebugEnabled()) {
                    log.debug("Cache: got doc " + key + " from cache");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache: Trying to get doc " + key + " for real");
                }

                doc = store.loadXWikiDoc(doc, context);
                doc.setStore(store);

                if (log.isDebugEnabled()) {
                    log.debug("Cache: Got doc " + key + " for real");
                    log.debug("Cache: put doc " + key + " in cache");
                }

                getCache().set(key, doc);
                getPageExistCache().set(key, new Boolean(!doc.isNew()));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache: end for doc " + key + " in cache");
        }

        return doc;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);
        synchronized (key) {
            store.deleteXWikiDoc(doc, context);

            // Make sure cache is initialized
            initCache(context);

            getCache().remove(key);
            getPageExistCache().remove(key);
            getPageExistCache().set(key, new Boolean(false));
        }
    }

    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        return store.getClassList(context);
    }

    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return store.searchDocumentsNames(wheresql, context);
    }

    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return store.searchDocumentsNames(wheresql, nb, start, context);
    }

    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return store.searchDocumentsNames(wheresql, nb, start, selectColumns, context);
    }

    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
        throws XWikiException
    {
        return store.isCustomMappingValid(bclass, custommapping1, context);
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context) throws XWikiException
    {
        return store.injectCustomMapping(doc1class, context);
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return store.injectCustomMappings(doc, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context)
        throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbyname, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbyname, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb,
        int start, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return store.searchDocuments(wheresql, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, boolean, int,
     *      int, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbyname, customMapping, checkRight, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List parameterValues, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbylanguage, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        return store.searchDocuments(wheresql, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, int, int,
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, boolean, int,
     *      int, java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        return store.searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start,
            parameterValues, context);
    }

    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start, List parameterValues,
        XWikiContext context) throws XWikiException
    {
        return store.searchDocumentsNames(parametrizedSqlClause, nb, start, parameterValues, context);
    }

    public List<String> searchDocumentsNames(String parametrizedSqlClause, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        return store.searchDocumentsNames(parametrizedSqlClause, parameterValues, context);
    }

    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return store.loadLock(docId, context, bTransaction);
    }

    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        store.saveLock(lock, context, bTransaction);
    }

    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        store.deleteLock(lock, context, bTransaction);
    }

    public List loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return store.loadLinks(docId, context, bTransaction);
    }

    public List loadBacklinks(String fullName, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return store.loadBacklinks(fullName, context, bTransaction);
    }

    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        store.saveLinks(doc, context, bTransaction);
    }

    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        store.deleteLinks(docId, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return store.search(sql, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return store.search(sql, nb, start, whereParams, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, List parameterValues, XWikiContext context) throws XWikiException
    {
        return store.search(sql, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List search(String sql, int nb, int start, Object[][] whereParams, List parameterValues, XWikiContext context)
        throws XWikiException
    {
        return store.search(sql, nb, start, whereParams, parameterValues, context);
    }

    public synchronized void cleanUp(XWikiContext context)
    {
        store.cleanUp(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#isWikiNameAvailable(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            return store.isWikiNameAvailable(wikiName, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#createWiki(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void createWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            store.createWiki(wikiName, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#deleteWiki(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            store.deleteWiki(wikiName, context);
        }
    }

    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);
        initCache(context);
        synchronized (key) {
            try {
                Boolean result = getPageExistCache().get(key);

                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
            }

            boolean result = store.exists(doc, context);
            getPageExistCache().set(key, new Boolean(result));

            return result;
        }
    }

    public Cache<XWikiDocument> getCache()
    {
        return cache;
    }

    public void setCache(Cache<XWikiDocument> cache)
    {
        if (this.cache != null) {
            this.cache.removeCacheEntryListener(cacheListener);
        }

        this.cache = cache;

        this.cache.addCacheEntryListener(cacheListener);
    }

    public Cache<Boolean> getPageExistCache()
    {
        return pageExistCache;
    }

    public void setPageExistCache(Cache<Boolean> pageExistCache)
    {
        this.pageExistCache = pageExistCache;
    }

    public List getCustomMappingPropertyList(BaseClass bclass)
    {
        return store.getCustomMappingPropertyList(bclass);
    }

    public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        store.injectCustomMappings(context);
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        store.injectUpdatedCustomMappings(context);
    }

    public List getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return store.getTranslationList(doc, context);
    }
}
