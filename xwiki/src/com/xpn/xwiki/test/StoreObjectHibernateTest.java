
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
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

    public XWikiHibernateStore store;
    public String hibpath = "hibernate-test.cfg.xml";

    public void setUp() throws HibernateException {
        StoreHibernateTest.cleanUp(getHibStore());
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
        IntegerProperty prop2 = new IntegerProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, true);
        assertEquals("IntegerProperty is different", prop, prop2);

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
        StringProperty prop2 = new StringProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, true);
        assertEquals("StringProperty is different", prop, prop2);

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

    public void testStringList(XWikiStoreInterface store) throws XWikiException {
        StringListProperty prop = Utils.prepareStringListProperty();
        store.saveXWikiProperty(prop, true);
        StringListProperty prop2 = new StringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, true);
        assertEquals("StringListProperty is different", prop, prop2);

    }


    public void testStringListEmptyDatabase() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testStringList(store);
    }

    public void testStringListUpdate() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testStringList(store);
        testStringList(store);
    }

    public void testDBStringList(XWikiStoreInterface store) throws XWikiException {
        DBStringListProperty prop = Utils.prepareDBStringListProperty();
        store.saveXWikiProperty(prop, true);
        DBStringListProperty prop2 = new DBStringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, true);
        assertEquals("DBStringListProperty is different", prop, prop2);
    }


    public void testDBStringListEmptyDatabase() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testDBStringList(store);
    }

    public void testDBStringListUpdate() throws XWikiException {
        XWikiStoreInterface store = getStore();
        testDBStringList(store);
        testDBStringList(store);
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

        assertEquals("Object is different", object, object2);
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

    public void testWriteAdvancedObject()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
        testWriteObject(store, object);
    }

    public void testReadWriteAdvancedObject()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
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

        assertEquals("Class is different", bclass, bclass2);
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

     public void testWriteAdvancedClass()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
        testWriteClass(store, object.getxWikiClass());
    }

    public void testReadWriteAdvancedClass()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
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