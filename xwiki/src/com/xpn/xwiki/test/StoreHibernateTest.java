
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
 * Time: 14:28:31
 */

public class StoreHibernateTest extends StoreTest {

    public static String hibpath = "hibernate-test.cfg.xml";
    public XWikiStoreInterface store;

    public static void runSQL(XWikiHibernateStore hibstore, String sql, XWikiContext context) {
           try {
               Session session = hibstore.getSession(context);
               Connection connection = session.connection();
               PreparedStatement ps = connection.prepareStatement(sql);
               ps.execute();
           } catch (Exception e) {
               e.printStackTrace();
           }
       }


    public static void cleanUp(XWikiHibernateStore hibstore, XWikiContext context) throws HibernateException, XWikiException {
        hibstore.checkHibernate(context);
        hibstore.beginTransaction(context);
        String database = context.getDatabase();
        if (database==null)
            context.setDatabase("xwikitest");
        StoreHibernateTest.runSQL(hibstore, "drop database if exists " + context.getDatabase(), context);
        StoreHibernateTest.runSQL(hibstore, "create database " + context.getDatabase(), context);
        hibstore.endTransaction(context, true);
        hibstore.updateSchema(context);
    }

    public void setUp() throws HibernateException, XWikiException {
        context.setDatabase("xwikitest");
        cleanUp(getHibStore(), context);
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        store = null;
        System.gc();
    }

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public XWikiStoreInterface getStore() {
       if (store!=null)
        return store;

       store = new XWikiHibernateStore(hibpath);
       return store;
   }


}
