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

import java.util.List;



public class XWikiCacheStore implements XWikiCacheStoreInterface {

    private XWikiStoreInterface store;
    private XWikiCache cache;
    private XWikiCache pageExistCache;
    private int cacheCapacity = 100;
    private int pageExistCacheCapacity = 10000;

    public XWikiCacheStore(XWikiStoreInterface store, XWikiContext context) {
        setStore(store);
        initCache(cacheCapacity, pageExistCacheCapacity, context);
    }

    public void initCache(int capacity, int pageExistCacheCapacity, XWikiContext context) {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        setCache(cacheService.newCache(capacity));
        setPageExistCache(cacheService.newCache(pageExistCacheCapacity));
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
        store.saveXWikiDoc(doc, context);
        doc.setStore(store);
        String key = getKey(doc, context);
        // We need to flush so that caches
        // on the cluster are informed about the change
        getCache().flushEntry(key);
        getCache().putInCache(key, doc);
        getPageExistCache().flushEntry(key);
        getPageExistCache().putInCache(key, new Boolean(true));
    }

    public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveXWikiDoc(doc, context, bTransaction);
        doc.setStore(store);
        String key = getKey(doc, context);
        // We need to flush so that caches
        // on the cluster are informed about the change
        getCache().flushEntry(key);
        getCache().putInCache(key, doc);
        getPageExistCache().flushEntry(key);
        getPageExistCache().putInCache(key, new Boolean(true));
    }

    public void flushCache() {
        getCache().flushAll();
        getPageExistCache().flushAll();
    }

    public String getKey(XWikiDocument doc, XWikiContext context) {
        String db = context.getDatabase();
        if (db==null)
            db = "";
        String key = db + ":" + doc.getFullName();
        if ("".equals(doc.getLanguage()))
            return key;
        else
            return key + ":" + doc.getLanguage();
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        try {
            doc = (XWikiDocument) getCache().getFromCache(key);
            doc.setFromCache(true);
        } catch (XWikiCacheNeedsRefreshException e) {
            try {
                doc = store.loadXWikiDoc(doc, context);
                doc.setStore(store);
            } catch (XWikiException xwikiexception) {
                getCache().cancelUpdate(key);
                throw xwikiexception;
            }
            getCache().putInCache(key, doc);
            getPageExistCache().putInCache(key, new Boolean(!doc.isNew()));
        }
        return doc;
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException {
        XWikiDocument doc2 = store.loadXWikiDoc(doc, version, context);
        doc2.setStore(store);
        return doc2;
    }

    public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        store.deleteXWikiDoc(doc, context);
        getCache().flushEntry(key);
        getPageExistCache().flushEntry(key);
        getPageExistCache().putInCache(key, new Boolean(false));
    }

    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException {
        return store.getXWikiDocVersions(doc, context);
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

    public void saveLinks(List links, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.saveLinks( links, context, bTransaction);
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

    public void cleanUp(XWikiContext context) {
        store.cleanUp(context);
    }

    public void createWiki(String wikiName, XWikiContext context) throws XWikiException {
        store.createWiki(wikiName, context);
    }

    public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc,context);
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

    public void injectCustomMappings(XWikiContext context) throws XWikiException {
        store.injectCustomMappings(context);
    }
}
