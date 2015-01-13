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
package com.xpn.xwiki.store;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

/**
 * A proxy store implementation that caches Documents when they are first fetched and subsequently return them from a
 * cache. It delegates all write and search operations to an underlying store without doing any caching on them.
 * 
 * @version $Id$
 */
public class XWikiCacheStore implements XWikiCacheStoreInterface, EventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiCacheStore.class);

    private XWikiStoreInterface store;

    private Cache<XWikiDocument> cache;

    private Cache<Boolean> pageExistCache;

    private int cacheCapacity = 100;

    private int pageExistCacheCapacity = 10000;

    /**
     * Used to know if a received event is a local or remote one.
     */
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Used to register XWikiCacheStore to receive documents events.
     */
    private ObservationManager observationManager;

    public XWikiCacheStore(XWikiStoreInterface store, XWikiContext context) throws XWikiException
    {
        setStore(store);
        initCache(context);

        // register XWikiCacheStore as listener to remote document events
        this.remoteObservationManagerContext = Utils.getComponent(RemoteObservationManagerContext.class);
        this.observationManager = Utils.getComponent(ObservationManager.class);
        this.observationManager.addListener(this);
    }

    @Override
    public String getName()
    {
        return "XWikiCacheStore";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(), new WikiDeletedEvent());
    }

    public synchronized void initCache(XWikiContext context) throws XWikiException
    {
        if ((this.cache == null) || (this.pageExistCache == null)) {
            try {
                String capacity = context.getWiki().Param("xwiki.store.cache.capacity");
                if (capacity != null) {
                    this.cacheCapacity = Integer.parseInt(capacity);
                }
            } catch (Exception e) {
            }
            try {
                String capacity = context.getWiki().Param("xwiki.store.cache.pageexistcapacity");
                if (capacity != null) {
                    this.pageExistCacheCapacity = Integer.parseInt(capacity);
                }
            } catch (Exception e) {
            }
            initCache(this.cacheCapacity, this.pageExistCacheCapacity, context);
        }
    }

    @Override
    public void initCache(int capacity, int pageExistCacheCapacity, XWikiContext context) throws XWikiException
    {
        CacheManager cacheManager = Utils.getComponent(CacheManager.class);

        try {
            CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId("xwiki.store.pagecache");
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(capacity);
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            Cache<XWikiDocument> pageCache = cacheManager.createNewCache(cacheConfiguration);
            setCache(pageCache);

            cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setConfigurationId("xwiki.store.pageexistcache");
            lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(pageExistCacheCapacity);
            cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            Cache<Boolean> pageExistcache = cacheManager.createNewCache(cacheConfiguration);
            setPageExistCache(pageExistcache);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to initialize cache", e);
        }
    }

    @Override
    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    @Override
    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    @Override
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        saveXWikiDoc(doc, context, true);
    }

    @Override
    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        String key = doc.getKey();
        this.store.saveXWikiDoc(doc, context, bTransaction);
        doc.setStore(this.store);
        // Make sure cache is initialized
        initCache(context);

        // We need to flush so that caches
        // on the cluster are informed about the change
        getCache().remove(key);
        getPageExistCache().remove(key);

        /*
         * We do not want to save the document in the cache at this time. If we did, this would introduce the
         * possibility for cache incoherence if the document is not saved in the database properly. In addition, the
         * attachments uploaded to the document stay with it so we want the document in it's current form to be garbage
         * collected as soon as the request is complete.
         */
    }

    @Override
    public void flushCache()
    {
        if (this.cache != null) {
            this.cache.dispose();
            this.cache = null;
        }

        if (this.pageExistCache != null) {
            this.pageExistCache.dispose();
            this.pageExistCache = null;
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // only react to remote events since local actions are already taken into account
        if (this.remoteObservationManagerContext.isRemoteState()) {
            if (event instanceof WikiDeletedEvent) {
                flushCache();
            } else {
                XWikiDocument doc = (XWikiDocument) source;

                String key = doc.getKey();

                if (getCache() != null) {
                    getCache().remove(key);
                }
                if (getPageExistCache() != null) {
                    getPageExistCache().remove(key);
                }
            }
        }
    }

    @Deprecated
    public String getKey(XWikiDocument doc)
    {
        return doc.getKey();
    }

    /**
     * @deprecated since 4.0M1 use {@link com.xpn.xwiki.doc.XWikiDocument#getKey()}
     */
    @Deprecated
    public String getKey(XWikiDocument doc, XWikiContext context)
    {
        return doc.getKey();
    }

    /**
     * @deprecated since 4.0M1 use {@link com.xpn.xwiki.doc.XWikiDocument#getKey()}
     */
    @Deprecated
    public String getKey(String fullName, String language, XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument(null, fullName);
        doc.setLanguage(language);
        return doc.getKey();
    }

    /**
     * @deprecated since 4.0M1 use {@link com.xpn.xwiki.doc.XWikiDocument#getKey()}
     */
    @Deprecated
    public String getKey(final String wiki, final String fullName, final String language)
    {
        XWikiDocument doc = new XWikiDocument(wiki, null, fullName);
        doc.setLanguage(language);
        return doc.getKey();
    }

    @Override
    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = doc.getKey();

        LOGGER.debug("Cache: begin for doc {} in cache", key);

        // Make sure cache is initialized
        initCache(context);

        LOGGER.debug("Cache: Trying to get doc {} from cache", key);

        XWikiDocument cachedoc;
        try {
            cachedoc = getCache().get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to get document from the cache", e);

            cachedoc = null;
        }

        if (cachedoc != null) {
            doc = cachedoc;
            doc.setFromCache(true);

            LOGGER.debug("Cache: got doc {} from cache", key);
        } else {
            LOGGER.debug("Cache: Trying to get doc {} from persistent storage", key);

            doc = this.store.loadXWikiDoc(doc, context);
            doc.setStore(this.store);

            LOGGER.debug("Cache: Got doc {} from storage", key);

            getCache().set(key, doc);
            getPageExistCache().set(key, new Boolean(!doc.isNew()));

            LOGGER.debug("Cache: put doc {} in cache", key);
        }

        LOGGER.debug("Cache: end for doc {} in cache", key);

        return doc;
    }

    @Override
    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = doc.getKey();

        this.store.deleteXWikiDoc(doc, context);

        // Make sure cache is initialized
        initCache(context);

        getCache().remove(key);
        getPageExistCache().remove(key);
        getPageExistCache().set(key, new Boolean(false));
    }

    @Override
    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        return this.store.getClassList(context);
    }

    @Override
    public int countDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.countDocuments(wheresql, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, nb, start, context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, nb, start, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, nb, start, selectColumns, context);
    }

    @Override
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, nb, start, selectColumns, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(parametrizedSqlClause, nb, start, parameterValues, context);
    }

    @Override
    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(parametrizedSqlClause, nb, start, parameterValues, context);
    }

    @Override
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(parametrizedSqlClause, parameterValues, context);
    }

    @Override
    public List<String> searchDocumentsNames(String parametrizedSqlClause, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(parametrizedSqlClause, parameterValues, context);
    }

    @Override
    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
        throws XWikiException
    {
        return this.store.isCustomMappingValid(bclass, custommapping1, context);
    }

    @Override
    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context) throws XWikiException
    {
        return this.store.injectCustomMapping(doc1class, context);
    }

    @Override
    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return this.store.injectCustomMappings(doc, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbyname, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbyname, nb, start, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb,
        int start, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, nb, start, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocuments(wheresql, nb, start, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, checkRight, nb, start, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, nb, start, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocuments(wheresql, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start, parameterValues,
            context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, nb, start, parameterValues, context);
    }

    @Override
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start,
            parameterValues, context);
    }

    @Override
    public int countDocuments(String parametrizedSqlClause, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.countDocuments(parametrizedSqlClause, parameterValues, context);
    }

    @Override
    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return this.store.loadLock(docId, context, bTransaction);
    }

    @Override
    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.saveLock(lock, context, bTransaction);
    }

    @Override
    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.deleteLock(lock, context, bTransaction);
    }

    @Override
    public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return this.store.loadLinks(docId, context, bTransaction);
    }

    @Override
    public List<DocumentReference> loadBacklinks(DocumentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        return this.store.loadBacklinks(documentReference, bTransaction, context);
    }

    @Override
    public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return this.store.loadBacklinks(fullName, context, bTransaction);
    }

    @Override
    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.saveLinks(doc, context, bTransaction);
    }

    @Override
    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.deleteLinks(docId, context, bTransaction);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return this.store.search(sql, nb, start, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return this.store.search(sql, nb, start, whereParams, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.search(sql, nb, start, parameterValues, context);
    }

    @Override
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.search(sql, nb, start, whereParams, parameterValues, context);
    }

    @Override
    public synchronized void cleanUp(XWikiContext context)
    {
        this.store.cleanUp(context);
    }

    @Override
    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            return this.store.isWikiNameAvailable(wikiName, context);
        }
    }

    @Override
    public void createWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            this.store.createWiki(wikiName, context);
        }
    }

    @Override
    public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            this.store.deleteWiki(wikiName, context);
            flushCache();
        }
    }

    @Override
    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = doc.getKey();
        initCache(context);
        try {
            Boolean result = getPageExistCache().get(key);

            if (result != null) {
                return result;
            }
        } catch (Exception e) {
        }

        boolean result = this.store.exists(doc, context);
        getPageExistCache().set(key, new Boolean(result));

        return result;
    }

    public Cache<XWikiDocument> getCache()
    {
        return this.cache;
    }

    public void setCache(Cache<XWikiDocument> cache)
    {
        this.cache = cache;
    }

    public Cache<Boolean> getPageExistCache()
    {
        return this.pageExistCache;
    }

    public void setPageExistCache(Cache<Boolean> pageExistCache)
    {
        this.pageExistCache = pageExistCache;
    }

    @Override
    public List<String> getCustomMappingPropertyList(BaseClass bclass)
    {
        return this.store.getCustomMappingPropertyList(bclass);
    }

    @Override
    public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        this.store.injectCustomMappings(context);
    }

    @Override
    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        this.store.injectUpdatedCustomMappings(context);
    }

    @Override
    public List<String> getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return this.store.getTranslationList(doc, context);
    }

    @Override
    public QueryManager getQueryManager()
    {
        return getStore().getQueryManager();
    }
}
