

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;

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

 * Created by
 * User: Ludovic Dubost
 * Date: 19 janv. 2004
 * Time: 14:25:48
 */

public class StoreHibernateCacheTest extends StoreHibernateTest {

    public void setUp() throws HibernateException, XWikiException {
        context.setDatabase("xwikitest");
        cleanUp(getHibStore(), context);
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        System.gc();
    }

    public XWikiHibernateStore getHibStore() {
        XWikiCacheStoreInterface cacheStore = (XWikiCacheStoreInterface) getStore();
        return (XWikiHibernateStore) cacheStore.getStore();
    }

    public XWikiStoreInterface getStore() {
        if (store!=null)
         return store;

        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        store = new XWikiCacheStore(hibstore, context);
        return store;
    }

    public void testCachedReadWrite() throws XWikiException {
        Utils.setStandardData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name);
        XWikiDocument doc3 = new XWikiDocument(Utils.web, Utils.name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, context);
        String content3b = doc3.getContent();
        assertEquals(Utils.content3,content3b);
        assertEquals(doc3.getAuthor(), Utils.author2);
        assertEquals(doc3.getVersion(), Utils.version2);
        assertTrue("Document should be from Cache", doc3.isFromCache());
    }

    public void testVirtualCachedReadWrite() throws XWikiException, HibernateException {
        XWikiStoreInterface store = getStore();

        context.setDatabase("xwikitest2");
        cleanUp(getHibStore(), context);

        context.setDatabase("xwikitest");
        Utils.setStandardData();
        testStandardWrite(store, Utils.web, Utils.name);
        testStandardRead(store, Utils.web, Utils.name);
        context.setDatabase("xwikitest2");
        Utils.setMediumData();
        testStandardWrite(store, Utils.web, Utils.name);
        testStandardRead(store, Utils.web, Utils.name);
    }

    public void testVirtualCachedReadWrite2() throws XWikiException, HibernateException {
        XWikiStoreInterface store = getStore();
        context.setDatabase("xwikitest2");
        cleanUp(getHibStore(), context);

        context.setDatabase("xwikitest");
        Utils.setStandardData();
        testStandardWrite(store, Utils.web, Utils.name);
        context.setDatabase("xwikitest2");
        Utils.setMediumData();
        testStandardWrite(store, Utils.web, Utils.name);

        ((XWikiCacheStoreInterface)store).flushCache();
        Utils.setStandardData();
        context.setDatabase("xwikitest");
        testStandardRead(store, Utils.web, Utils.name);
        Utils.setMediumData();
        context.setDatabase("xwikitest2");
        testStandardRead(store, Utils.web, Utils.name);
    }

}
