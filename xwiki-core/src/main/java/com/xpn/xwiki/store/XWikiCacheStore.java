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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
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
    private static final Log log = LogFactory.getLog(XWikiCacheStore.class);

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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return "XWikiCacheStore";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
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
        return this.store;
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
         * possibility for cache incoherince if the document is not saved in the database properly. In addition, the
         * attachments uploaded to the document stay with it so we want the document in it's current form to be garbage
         * collected as soon as the request is complete.
         */
    }

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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        // only react to remote events since local actions are already taken into account
        if (this.remoteObservationManagerContext.isRemoteState()) {
            if (event instanceof WikiDeletedEvent) {
                flushCache();
            } else {
                XWikiDocument doc = (XWikiDocument) source;
                XWikiContext context = (XWikiContext) data;

                String key = getKey(doc, context);

                if (getCache() != null) {
                    getCache().remove(key);
                }
                if (getPageExistCache() != null) {
                    getPageExistCache().remove(key);
                }
            }
        }
    }

    public String getKey(XWikiDocument doc)
    {
        return getKey(doc.getWikiName(), doc.getFullName(), doc.getLanguage());
    }

    public String getKey(XWikiDocument doc, XWikiContext context)
    {
        return getKey(doc.getFullName(), doc.getLanguage(), context);
    }

    public String getKey(String fullName, String language, XWikiContext context)
    {
        return getKey(context.getDatabase(), fullName, language);
    }

    public String getKey(String wiki, String fullName, String language)
    {
        String key = (wiki == null ? "" : wiki) + ":" + fullName;

        if (StringUtils.isEmpty(language)) {
            return key;
        } else {
            return key + ":" + language;
        }
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);
        if (log.isDebugEnabled()) {
            log.debug("Cache: begin for doc " + key + " in cache");
        }

        // Make sure cache is initialized
        initCache(context);

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

            doc = this.store.loadXWikiDoc(doc, context);
            doc.setStore(this.store);

            if (log.isDebugEnabled()) {
                log.debug("Cache: Got doc " + key + " for real");
                log.debug("Cache: put doc " + key + " in cache");
            }

            getCache().set(key, doc);
            getPageExistCache().set(key, new Boolean(!doc.isNew()));
        }

        if (log.isDebugEnabled()) {
            log.debug("Cache: end for doc " + key + " in cache");
        }

        return doc;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);

        this.store.deleteXWikiDoc(doc, context);

        // Make sure cache is initialized
        initCache(context);

        getCache().remove(key);
        getPageExistCache().remove(key);
        getPageExistCache().set(key, new Boolean(false));
    }

    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        return this.store.getClassList(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStoreInterface#countDocuments(String, XWikiContext)
     */
    public int countDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.countDocuments(wheresql, context);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, context);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, nb, start, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, nb, start, context);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(wheresql, nb, start, selectColumns, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, String, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(wheresql, nb, start, selectColumns, context);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(parametrizedSqlClause, nb, start, parameterValues, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, int, int, List, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(parametrizedSqlClause, nb, start, parameterValues, context);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentReferences(parametrizedSqlClause, parameterValues, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #searchDocumentReferences(String, List, XWikiContext)}
     */
    @Deprecated
    public List<String> searchDocumentsNames(String parametrizedSqlClause, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocumentsNames(parametrizedSqlClause, parameterValues, context);
    }

    public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
        throws XWikiException
    {
        return this.store.isCustomMappingValid(bclass, custommapping1, context);
    }

    public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context) throws XWikiException
    {
        return this.store.injectCustomMapping(doc1class, context);
    }

    public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return this.store.injectCustomMappings(doc, context);
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
        return this.store.searchDocuments(wheresql, distinctbyname, context);
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
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, context);
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
        return this.store.searchDocuments(wheresql, distinctbyname, nb, start, context);
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
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, context);
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
        return this.store.searchDocuments(wheresql, nb, start, context);
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
        return this.store.searchDocuments(wheresql, distinctbyname, customMapping, checkRight, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.searchDocuments(wheresql, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, int, int,
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start, parameterValues,
            context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, boolean, boolean, int,
     *      int, java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List< ? > parameterValues, XWikiContext context) throws XWikiException
    {
        return this.store.searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start,
            parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStoreInterface#countDocuments(String, List, XWikiContext)
     */
    public int countDocuments(String parametrizedSqlClause, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.countDocuments(parametrizedSqlClause, parameterValues, context);
    }

    public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return this.store.loadLock(docId, context, bTransaction);
    }

    public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.saveLock(lock, context, bTransaction);
    }

    public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.deleteLock(lock, context, bTransaction);
    }

    public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return this.store.loadLinks(docId, context, bTransaction);
    }

    /**
     * @since 2.2M2
     */
    public List<DocumentReference> loadBacklinks(DocumentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        return this.store.loadBacklinks(documentReference, bTransaction, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #loadBacklinks(DocumentReference, boolean, XWikiContext)}
     */
    @Deprecated
    public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return this.store.loadBacklinks(fullName, context, bTransaction);
    }

    public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.saveLinks(doc, context, bTransaction);
    }

    public void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        this.store.deleteLinks(docId, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, com.xpn.xwiki.XWikiContext)
     */
    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return this.store.search(sql, nb, start, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return this.store.search(sql, nb, start, whereParams, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.util.List,
     *      com.xpn.xwiki.XWikiContext)
     */
    public <T> List<T> search(String sql, int nb, int start, List< ? > parameterValues, XWikiContext context)
        throws XWikiException
    {
        return this.store.search(sql, nb, start, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.lang.Object[][],
     *      java.util.List, com.xpn.xwiki.XWikiContext)
     */
    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, List< ? > parameterValues,
        XWikiContext context) throws XWikiException
    {
        return this.store.search(sql, nb, start, whereParams, parameterValues, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#cleanUp(com.xpn.xwiki.XWikiContext)
     */
    public synchronized void cleanUp(XWikiContext context)
    {
        this.store.cleanUp(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.XWikiStoreInterface#isWikiNameAvailable(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException
    {
        synchronized (wikiName) {
            return this.store.isWikiNameAvailable(wikiName, context);
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
            this.store.createWiki(wikiName, context);
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
            this.store.deleteWiki(wikiName, context);
            flushCache();
        }
    }

    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String key = getKey(doc, context);
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

    public List<String> getCustomMappingPropertyList(BaseClass bclass)
    {
        return this.store.getCustomMappingPropertyList(bclass);
    }

    public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException
    {
        this.store.injectCustomMappings(context);
    }

    public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException
    {
        this.store.injectUpdatedCustomMappings(context);
    }

    public List<String> getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return this.store.getTranslationList(doc, context);
    }

    /**
     * {@inheritDoc}
     */
    public QueryManager getQueryManager()
    {
        return getStore().getQueryManager();
    }
}
