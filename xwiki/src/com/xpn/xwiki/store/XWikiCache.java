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
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import org.apache.commons.jrcs.rcs.Version;

import java.util.List;
import java.util.Date;

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
        cache = new Cache(true, false);
        cache.setCapacity(100);
    }

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }

    public void saveXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        store.saveXWikiDoc(doc);
        cache.putInCache(doc.getFullName(), doc);
    }

    public void flushCache() {
        cache.flushAll(new Date());
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc) throws XWikiException {
        String docname = doc.getFullName();
        try {
            doc = (XWikiDocInterface) cache.getFromCache(docname, CacheEntry.INDEFINITE_EXPIRY);
            doc.setFromCache(true);
        } catch (NeedsRefreshException e) {
            try {
                doc = store.loadXWikiDoc(doc);
            } catch (XWikiException xwikiexception) {
                cache.cancelUpdate(docname);
                throw xwikiexception;
            }
            cache.putInCache(docname, doc);
        }
        return doc;
    }

    public XWikiDocInterface loadXWikiDoc(XWikiDocInterface doc, String version) throws XWikiException {
        return store.loadXWikiDoc(doc, version);
    }

    public Version[] getXWikiDocVersions(XWikiDocInterface doc) throws XWikiException {
        return store.getXWikiDocVersions(doc);
    }

    public List getClassList() throws XWikiException {
        return store.getClassList();
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return store.searchDocuments(wheresql);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        return store.searchDocuments(wheresql, nb, start);
    }

    public void saveXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
        store.saveXWikiObject(object, bTransaction);
    }

    public void loadXWikiObject(BaseObject object, boolean bTransaction) throws XWikiException {
        store.loadXWikiObject(object, bTransaction);
    }

    public void saveXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        store.saveXWikiClass(bclass, bTransaction);
    }

    public void loadXWikiClass(BaseClass bclass, boolean bTransaction) throws XWikiException {
        store.loadXWikiClass(bclass, bTransaction);
    }

    public void saveXWikiProperty(PropertyInterface property, boolean bTransaction) throws XWikiException {
        store.saveXWikiProperty(property, bTransaction);
    }

    public void saveXWikiClassProperty(PropertyClass property, boolean bTransaction) throws XWikiException {
        store.saveXWikiClassProperty(property, bTransaction);
    }

}
