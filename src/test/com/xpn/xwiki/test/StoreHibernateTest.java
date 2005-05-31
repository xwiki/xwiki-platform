/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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

package com.xpn.xwiki.test;

import java.sql.Connection;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

public class StoreHibernateTest extends StoreTest {

    public static final String HIB_LOCATION = "/hibernate-test.cfg.xml";
    public XWikiStoreInterface store;
    public boolean cleanup = false;

    public static void runSQL(XWikiHibernateStore hibstore, String sql, XWikiContext context) {
           try {
               Session session = hibstore.getSession(context);
               Connection connection = session.connection();
               Statement st = connection.createStatement();
               st.execute(sql);
           } catch (Exception e) {
               e.printStackTrace();
           }
       }

    public static void cleanUp(XWikiHibernateStore hibstore, XWikiContext context) throws HibernateException, XWikiException {
        cleanUp(hibstore, false, false, context);
    }

    public static void cleanUp(XWikiHibernateStore hibstore, boolean bFullCleanup, boolean bSchemaUpdate, XWikiContext context) throws HibernateException, XWikiException {
        hibstore.checkHibernate(context);
        hibstore.beginTransaction(context);
        String database = context.getDatabase();
        if (database==null)
            context.setDatabase("xwikitest");
        if (bFullCleanup) {
            try {
            StoreHibernateTest.runSQL(hibstore, "drop database if exists " + context.getDatabase(), context);
            } catch (Exception e) {}
            StoreHibernateTest.runSQL(hibstore, "create database " + context.getDatabase(), context);
        } else {
            StoreHibernateTest.runSQL(hibstore, "delete from xwikibooleanclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikinumberclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikislistclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikidateclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikistringclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikidblistclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiclassesprop", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiclasses", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikidates", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikidoubles", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikifloats", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikilongs", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiintegers", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikilargestrings", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikilistitems", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikilists", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikistrings", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiproperties", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiobjects", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiattachment_content", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiattachment_archive", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikiattachment", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikidoc", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikilock", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikistatsdoc", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikistatsreferer", context);
            StoreHibernateTest.runSQL(hibstore, "delete from xwikistatsvisit", context);
        }
        hibstore.endTransaction(context, true);

        if (bFullCleanup&&bSchemaUpdate)
         hibstore.updateSchema(context);
    }

    public void setUp() throws HibernateException, XWikiException {
        context.setDatabase("xwikitest");
        cleanUp(getHibStore(), context);
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        store = null;
        context = null;
        System.gc();
    }

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public XWikiStoreInterface getStore() {
       if (store != null)
        return store;

       String hibPath = AllTests.class.getResource(HIB_LOCATION).getFile();
       store = new XWikiHibernateStore(hibPath);
       return store;
   }

    public void testLockReadWrite() throws XWikiException {
        XWikiLock lock = new XWikiLock(1, "AnyUser");

        store.saveLock(lock, context, true);

        XWikiLock newlock = store.loadLock(1, context, true);
        assertEquals("Same user", newlock.getUserName(), lock.getUserName());
        assertTrue("Same date", Math.abs(newlock.getDate().getTime()-lock.getDate().getTime())<1000);

        store.deleteLock(lock, context, true);

        XWikiLock testlock = store.loadLock(1, context, true);
        assertEquals("No lock", null, testlock);
    }
}
