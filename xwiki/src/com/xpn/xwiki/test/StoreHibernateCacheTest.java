

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiCache;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;

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

 * Created by
 * User: Ludovic Dubost
 * Date: 19 janv. 2004
 * Time: 14:25:48
 */

public class StoreHibernateCacheTest extends StoreHibernateTest {

    public void setUp() throws HibernateException {
        cleanUp(getHibStore());
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate();
        System.gc();
    }

    public XWikiHibernateStore getHibStore() {
        XWikiCacheInterface cache = (XWikiCacheInterface) getStore();
        return (XWikiHibernateStore) cache.getStore();
    }

    public XWikiStoreInterface getStore() {
        if (store!=null)
         return store;

        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        store = new XWikiCache(hibstore);
        return store;
    }

    public void testCachedReadWrite() throws XWikiException {
        Utils.setStandardData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name);
        XWikiSimpleDoc doc3 = new XWikiSimpleDoc(Utils.web, Utils.name);
        doc3 = (XWikiSimpleDoc) store.loadXWikiDoc(doc3);
        String content3b = doc3.getContent();
        assertEquals(Utils.content3,content3b);
        assertEquals(doc3.getAuthor(), Utils.author2);
        assertEquals(doc3.getVersion(), Utils.version2);
        assertTrue("Document should be from Cache", doc3.isFromCache());
    }
}
