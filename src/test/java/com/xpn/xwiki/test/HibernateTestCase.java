/**
 * ===================================================================
 *
 * Copyright (c) 2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class HibernateTestCase extends TestCase {
    public static final String HIB_LOCATION = "/hibernate-test.cfg.xml";

    private XWiki xwiki;
    private XWikiConfig config;
    private XWikiContext context;

    protected void setUp() throws Exception {
        this.config = new XWikiConfig();

        // TODO: Should probably be modified to use a memory store for testing or a mock store
        // TODO: StoreHibernateTest should be refactored with this class in mind 
        this.config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
        this.config.put("xwiki.store.hibernate.path", getClass().getResource(HibernateTestCase.HIB_LOCATION).getFile());
        this.config.put("xwiki.backlinks", "1");
        this.config.put("xwiki.store.cache","1");
        this.config.put("xwiki.store.cache.capacity", "100");

        this.context = new XWikiContext();
        this.context.setDatabase("xwikitest");

        this.xwiki = new XWiki(this.config, this.context);
        this.xwiki.setDatabase("xwikitest");

        this.context.setWiki(this.xwiki);
        
        cleanUp(this.xwiki.getHibernateStore(), this.context);
        this.xwiki.flushCache();

        Velocity.init(getClass().getResource("/velocity.properties").getFile());
    }

    protected void tearDown() {
        this.xwiki.getHibernateStore().shutdownHibernate(this.context);
        this.xwiki = null;
        this.context = null;
        this.config = null;
        System.gc();
    }
    
    public XWikiContext getXWikiContext() {
        return this.context;
    }
    
    public XWikiConfig getXWikiConfig() {
        return this.config;
    }
    
    public XWiki getXWiki() {
        return this.xwiki;
    }

    // Helper test methods below
    
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

    public static List runSQLwithReturn(XWikiHibernateStore hibstore, String sql, XWikiContext context) {
        try {
            Session session = hibstore.getSession(context);
            Connection connection = session.connection();
            Statement st = connection.createStatement();
            st.execute(sql);
            ResultSet rs = st.getResultSet();
            ResultSetMetaData mdata = rs.getMetaData();
            int colcount = mdata.getColumnCount();
            List list = new ArrayList();
            while (rs.next()) {
                Map item = new HashMap();
                for (int i=1;i<=colcount;i++) {
                    String colname = mdata.getColumnName(i).toLowerCase();
                    item.put(colname, rs.getObject(i));
                }
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static Object runSQLuniqueResult(XWikiHibernateStore hibstore, String sql, XWikiContext context) {
        try {
            Session session = hibstore.getSession(context);
            Connection connection = session.connection();
            Statement st = connection.createStatement();
            st.execute(sql);
            ResultSet rs = st.getResultSet();
            rs.next();
            return rs.getObject(1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
                runSQL(hibstore, "drop database if exists " + context.getDatabase(), context);
            } catch (Exception e) {}
            runSQL(hibstore, "create database " + context.getDatabase(), context);
        } else {
            runSQL(hibstore, "delete from xwikibooleanclasses", context);
            runSQL(hibstore, "delete from xwikinumberclasses", context);
            runSQL(hibstore, "delete from xwikislistclasses", context);
            runSQL(hibstore, "delete from xwikidateclasses", context);
            runSQL(hibstore, "delete from xwikistringclasses", context);
            runSQL(hibstore, "delete from xwikidblistclasses", context);
            runSQL(hibstore, "delete from xwikiextlistclasses", context);
            runSQL(hibstore, "delete from xwikiclassesprop", context);
            runSQL(hibstore, "delete from xwikiclasses", context);
            runSQL(hibstore, "delete from xwikidates", context);
            runSQL(hibstore, "delete from xwikidoubles", context);
            runSQL(hibstore, "delete from xwikifloats", context);
            runSQL(hibstore, "delete from xwikilongs", context);
            runSQL(hibstore, "delete from xwikiintegers", context);
            runSQL(hibstore, "delete from xwikilargestrings", context);
            runSQL(hibstore, "delete from xwikilistitems", context);
            runSQL(hibstore, "delete from xwikilists", context);
            runSQL(hibstore, "delete from xwikistrings", context);
            runSQL(hibstore, "delete from xwikiproperties", context);
            runSQL(hibstore, "delete from xwikiobjects", context);
            runSQL(hibstore, "delete from xwikiattachment_content", context);
            runSQL(hibstore, "delete from xwikiattachment_archive", context);
            runSQL(hibstore, "delete from xwikiattachment", context);
            runSQL(hibstore, "delete from xwikidoc", context);
            runSQL(hibstore, "delete from xwikilock", context);
            runSQL(hibstore, "delete from xwikistatsdoc", context);
            runSQL(hibstore, "delete from xwikistatsreferer", context);
            runSQL(hibstore, "delete from xwikistatsvisit", context);
            runSQL(hibstore, "delete from xwikilinks", context);
        }
        hibstore.endTransaction(context, true);

        if (bFullCleanup&&bSchemaUpdate)
            hibstore.updateSchema(context);
    }
}
