
package com.xpn.xwiki.test;

import com.xpn.xwiki.test.StoreTest;
import com.xpn.xwiki.test.StoreObjectTest;
import com.xpn.xwiki.test.StoreHibernateTest;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import net.sf.hibernate.HibernateException;

import java.util.List;

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
 * Date: 27 janv. 2004
 * Time: 14:47:25
 */

public class StoreObjectHibernateTest extends StoreObjectTest {

    public static String hibpath = "hibernate-test.cfg.xml";

    public void setUp() throws HibernateException {
        StoreHibernateTest.cleanUp(getHibStore());
    }

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public XWikiStoreInterface getStore() {
       XWikiStoreInterface store = new XWikiHibernateStore(hibpath);
       return store;
   }

    public void testStringBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
        store.beginTransaction();
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties");
        store.endTransaction(true);
        testString(store);
    }

    public void testStringBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
        store.beginTransaction();
        StoreHibernateTest.runSQL(store, "delete from xwikistrings");
        store.endTransaction(true);
        testString(store);
    }

    public void testNumberBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
        store.beginTransaction();
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties");
        store.endTransaction(true);
        testNumber(store);
    }

    public void testNumberBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
        store.beginTransaction();
        StoreHibernateTest.runSQL(store, "delete from xwikiintegers");
        StoreHibernateTest.runSQL(store, "delete from xwikilongs");
        store.endTransaction(true);
        testNumber(store);
    }

    public void testNumber(XWikiStoreInterface store) throws XWikiException {
        IntegerProperty prop = Utils.prepareIntegerProperty();
        store.saveXWikiProperty(prop, true);
    }


    public void testNumberEmptyDatabase() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testNumber(store);
    }

    public void testNumberUpdate() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testNumber(store);
        testNumber(store);
    }


    public void testString(XWikiStoreInterface store) throws XWikiException {
        StringProperty prop = Utils.prepareStringProperty();
        store.saveXWikiProperty(prop, true);
    }


    public void testStringEmptyDatabase() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testString(store);
    }

    public void testStringUpdate() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testString(store);
        testString(store);
    }



    public void testWriteObject(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        store.saveXWikiObject(object, true);
    }

    public void testReadObject(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        // Prepare object2 for reading
        BaseObject object2 = new BaseObject();
        object2.setxWikiClass(object.getxWikiClass());
        object2.setName(object.getName());

        // Read object2
        store.loadXWikiObject(object2, true);

        // Verify object2
        Utils.assertProperty(object2, object, "first_name");
        Utils.assertProperty(object2, object, "age");
    }

    public void testWriteObject()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteObject(store, object);
    }

    public void testReadWriteObject()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteObject(store, object);
        testReadObject(store, object);
    }



    public void testWriteClass(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        store.saveXWikiClass(bclass, true);
    }

    public void testReadClass(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        // Prepare object2 for reading
        BaseClass bclass2 = new BaseClass();
        bclass2.setName(bclass.getName());

        // Read object2
        store.loadXWikiClass(bclass2, true);

        // Verify object2
        Utils.assertProperty(bclass2, bclass, "first_name");
        Utils.assertProperty(bclass2, bclass, "age");
    }

    public void testWriteClass()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteClass(store, object.getxWikiClass());
    }

    public void testReadWriteClass()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteClass(store, object.getxWikiClass());
        testReadClass(store, object.getxWikiClass());
    }

    public void testSearchClass() throws XWikiException {
        XWikiStoreInterface store = getStore();
        List list = store.getClassList();
        assertTrue("No result", (list.size()==0) );
        testWriteClass();
        list = store.getClassList();
        assertTrue("No result", (list.size()>0) );
    }

     

}