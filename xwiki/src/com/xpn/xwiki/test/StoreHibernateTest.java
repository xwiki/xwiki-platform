
package com.xpn.xwiki.test;

import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import net.sf.hibernate.impl.SessionImpl;
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
 * Time: 14:28:31
 */

public class StoreHibernateTest extends StoreTest {

    public static String hibpath = "hibernate-test.cfg.xml";

    public static void runSQL(XWikiHibernateStore hibstore, String sql) {
           try {
               Connection connection = ((SessionImpl)hibstore.getSession()).connection();
               PreparedStatement ps = connection.prepareStatement(sql);
               ps.execute();
           } catch (Exception e) {
               e.printStackTrace();
           }
       }

    public static void cleanUp(XWikiHibernateStore hibstore) throws HibernateException {
        StoreObjectTest.cleanUp(hibstore);
    }

    public void setUp() throws HibernateException {
        cleanUp(getHibStore());
    }

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public XWikiStoreInterface getStore() {
       XWikiStoreInterface store = new XWikiHibernateStore(hibpath);
       return store;
   }


}
