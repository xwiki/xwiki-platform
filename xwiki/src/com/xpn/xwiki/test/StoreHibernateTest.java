
package com.xpn.xwiki.test;

import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.impl.SessionImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;

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

    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiStoreInterface store;

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
        hibstore.checkHibernate();
        hibstore.beginTransaction();
        StoreHibernateTest.runSQL(hibstore, "drop database if exists xwikitest");
        StoreHibernateTest.runSQL(hibstore, "create database xwikitest");
                /*
        StoreHibernateTest.runSQL(hibstore, "drop table xwikidoc");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiattachment");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiattachment_content");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiattachment_archive");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiobjects");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiproperties");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiintegers");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikifloats");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikilongs");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikidoubles");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikistrings");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiclasses");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikiclassesprop");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikistringclasses");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikinumberclasses");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikibooleanclasses");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikilists");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikilistitems");
        StoreHibernateTest.runSQL(hibstore, "drop table xwikislistclasses");
        */

        hibstore.endTransaction(true);
        hibstore.updateSchema();
    }

    public void setUp() throws HibernateException {
        cleanUp(getHibStore());
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate();
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
