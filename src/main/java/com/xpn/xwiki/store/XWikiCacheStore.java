/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */
package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.jrcs.rcs.Version;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;



public class XWikiCacheStore implements XWikiCacheStoreInterface {

    private static final Log log = LogFactory.getLog(XWikiCacheStore.class);

    private XWikiStoreInterface store;
    private XWikiCache cache;
    private XWikiCache pageExistCache;
    private XWikiCache classCache;
    private XWikiCache prefsCache;
    private int cacheCapacity = 100;
    private int classCacheCapacity = 100;
    private int prefsCacheCapacity = 1000;
    private int pageExistCacheCapacity = 10000;

    public XWikiCacheStore(XWikiStoreInterface store, XWikiContext context) {
        setStore(store);
        initCache(cacheCapacity, pageExistCacheCapacity, getPrefsCacheCapacity(), getClassCacheCapacity(), context);
    }

    public void initCache(int capacity, int pageExistCacheCapacity, int prefsCacheCapacity, int classCacheCapacity, XWikiContext context) {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        setCache(cacheService.newCache(capacity));
        setPageExistCache(cacheService.newCache(pageExistCacheCapacity));
        setPrefsCache(cacheService.newCache(prefsCacheCapacity));
        setClassCache(cacheService.newCache(classCacheCapacity));
    }

    public void setCacheCapacity(int capacity) {
        cacheCapacity = capacity;
        getCache().setCapacity(capacity);
    }

    public void setPageExistCacheCapacity(int capacity) {
        pageExistCacheCapacity = capacity;
        getPageExistCache().setCapacity(capacity);
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
            // We need to flush so that caches
            // on the cluster are informed about the change
            getCache().flushEntry(key);
            getCache().putInCache(key, doc);
            getPageExistCache().flushEntry(key);
            getPageExistCache().putInCache(key, new Boolean(true));

            // We need to populate the class cache
            BaseClass bclass = doc.getxWikiClass();
            if (bclass.getFieldList().size()>0)
             getClassCache().putInCache(key, bclass);
            else
             getClassCache().flushEntry(key);
        }
    }

    public void flushCache() {
        getCache().flushAll();
        getPageExistCache().flushAll();
        getClassCache().flushAll();
        getPrefsCache().flushAll();
    }

    public String getKey(XWikiDocument doc, XWikiContext context) {
        return getKey(doc.getFullName(), doc.getLanguage(), context);
    }

    public String getKey(BaseClass bclass, XWikiContext context) {
        return getKey(bclass.getName(), "", context);
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

                // We need to populate the class cache
                BaseClass bclass = doc.getxWikiClass();
                if (bclass.getFieldList().size()>0)
                 getClassCache().putInCache(key, bclass);
                getCache().putInCache(key, doc);
                getPageExistCache().putInCache(key, new Boolean(!doc.isNew()));
            }
        }
        if (log.isDebugEnabled())
         log.debug("Cache: end for doc " + key + " in cache");
        return doc;
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException {
        XWikiDocument doc2 = store.loadXWikiDoc(doc, version, context);
        doc2.setStore(store);
        return doc2;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        synchronized(key) {
            store.deleteXWikiDoc(doc, context);
            getCache().flushEntry(key);
            getClassCache().flushEntry(key);
            getPageExistCache().flushEntry(key);
            getPageExistCache().putInCache(key, new Boolean(false));
        }
    }

    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException {
        return store.getXWikiDocVersions(doc, context);
    }

    public BaseClass loadXWikiClassFromCache(BaseClass bclass, XWikiContext context) throws XWikiException {
        String key = getKey(bclass, context);
        try {
            return (BaseClass) getClassCache().getFromCache(key);
        } catch (XWikiCacheNeedsRefreshException e) {
            getCache().cancelUpdate(key);
            return null;
        }
    }

    public BaseClass loadXWikiClass(String className, XWikiContext context) throws XWikiException {
        BaseClass bclass = new BaseClass();
        bclass.setName(className);
        return loadXWikiClass(bclass, context);
    }

    public BaseClass loadXWikiClass(BaseClass bclass, XWikiContext context) throws XWikiException {
        String key = getKey(bclass, context);

        synchronized (key) {
            // Let's first look into the document
            try {
                XWikiDocument doc  = (XWikiDocument) getCache().getFromCache(key);
                bclass = doc.getxWikiClass();
                if (bclass.getFieldList().size()>0)
                 getClassCache().putInCache(key, bclass);
                return bclass;
            } catch (XWikiCacheNeedsRefreshException e) {
                getCache().cancelUpdate(key);
            }

            // Then let's look in our class cache
            try {
                bclass = (BaseClass) getClassCache().getFromCache(key);
            } catch (XWikiCacheNeedsRefreshException e) {
                try {
                    bclass = store.loadXWikiClass(bclass, context);
                } catch (XWikiException xwikiexception) {
                    getClassCache().cancelUpdate(key);
                    throw xwikiexception;
                }
                if (bclass.getFieldList().size()>0)
                 getClassCache().putInCache(key, bclass);
            }
        }
        return bclass;
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

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveAttachmentContent(attachment, context, bTransaction);
    }

    public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveAttachmentContent(attachment, bParentUpdate, context, bTransaction);
    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.loadAttachmentContent(attachment, context, bTransaction);
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.loadAttachmentArchive(attachment, context, bTransaction);
    }

    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.deleteXWikiAttachment(attachment, context, bTransaction);
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

    public XWikiCache getClassCache() {
        return classCache;
    }

    public void setClassCache(XWikiCache classCache) {
        this.classCache = classCache;
    }

    public XWikiCache getPrefsCache() {
        return prefsCache;
    }

    public void setPrefsCache(XWikiCache prefsCache) {
        this.prefsCache = prefsCache;
    }

    public int getClassCacheCapacity() {
        return classCacheCapacity;
    }

    public void setClassCacheCapacity(int classCacheCapacity) {
        this.classCacheCapacity = classCacheCapacity;
        getClassCache().setCapacity(classCacheCapacity);
    }

    public int getPrefsCacheCapacity() {
        return prefsCacheCapacity;
    }

    public void setPrefsCacheCapacity(int prefsCacheCapacity) {
        this.prefsCacheCapacity = prefsCacheCapacity;
        getPrefsCache().setCapacity(prefsCacheCapacity);
    }
}
