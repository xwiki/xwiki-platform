/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */
package com.xpn.xwiki.store;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import org.apache.commons.jrcs.rcs.Version;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 14:40:49
 * To change this template use Options | File Templates.
 */
public class XWikiCache implements XWikiCacheInterface {

    private XWikiStoreInterface store;
    private Cache cache;

    public XWikiCache(XWikiStoreInterface store) {
        setStore(store);
        initCache();
    }

    public void initCache() {
        cache = new Cache(true, false);
        cache.setCapacity(100);
    }

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }

    public void saveXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        store.saveXWikiDoc(doc, context);
        String key = getKey(doc, context);
        // We need to flush so that caches
        // on the cluster are informed about the change
        cache.flushEntry(key);
        cache.putInCache(key, doc);
    }

    public void flushCache() {
        initCache();
    }

    public String getKey(XWikiDocInterface doc, XWikiContext context) {
        String db = context.getDatabase();
        if (db==null)
            db = "";
        return db + ":" + doc.getFullName();
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        String key = getKey(doc, context);
        try {
            doc = (XWikiDocInterface) cache.getFromCache(key, CacheEntry.INDEFINITE_EXPIRY);
            doc.setFromCache(true);
        } catch (NeedsRefreshException e) {
            try {
                doc = store.loadXWikiDoc(doc, context);
            } catch (XWikiException xwikiexception) {
                cache.cancelUpdate(key);
                throw xwikiexception;
            }
            cache.putInCache(key, doc);
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, String version, XWikiContext context) throws XWikiException {
        return store.loadXWikiDoc(doc, version, context);
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        return store.getXWikiDocVersions(doc, context);
    }

    public List getClassList(XWikiContext context) throws XWikiException {
        return store.getClassList(context);
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

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.loadAttachmentContent(attachment, context, bTransaction);
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        store.loadAttachmentArchive(attachment, context, bTransaction);
    }

    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
        return store.search(sql, nb, start, context);
    }
}
