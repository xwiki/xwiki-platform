
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
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
 * Date: 27 janv. 2004
 * Time: 14:47:25
 */

public class StoreObjectHibernateTest extends StoreObjectTest {

    public XWikiHibernateStore store;
    public String hibpath = "hibernate-test.cfg.xml";

    public void setUp() throws HibernateException, XWikiException {
        context.setDatabase("xwikitest");
        StoreHibernateTest.cleanUp(getHibStore(), context);
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

    public void testStringBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
        store.beginTransaction(context);
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties", context);
        store.endTransaction(context, true);
        testString(store);
    }

    public void testStringBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
        store.beginTransaction(context);
        StoreHibernateTest.runSQL(store, "delete from xwikistrings", context);
        store.endTransaction(context, true);
        testString(store);
    }

    public void testNumberBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
        store.beginTransaction(context);
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties", context);
        store.endTransaction(context, true);
        testNumber(store);
    }

    public void testNumberBadDatabase2() throws XWikiException, HibernateException{
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
        store.beginTransaction(context);
        StoreHibernateTest.runSQL(store, "delete from xwikiintegers", context);
        StoreHibernateTest.runSQL(store, "delete from xwikilongs", context);
        store.endTransaction(context, true);
        testNumber(store);
    }

    public void testNumber(XWikiHibernateStore store) throws XWikiException {
        IntegerProperty prop = Utils.prepareIntegerProperty();
        store.saveXWikiProperty(prop, context, true);
        IntegerProperty prop2 = new IntegerProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, context, true);
        assertEquals("IntegerProperty is different", prop, prop2);

    }


    public void testNumberEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
    }

    public void testNumberUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testNumber(store);
        testNumber(store);
    }


    public void testString(XWikiHibernateStore store) throws XWikiException {
        StringProperty prop = Utils.prepareStringProperty();
        store.saveXWikiProperty(prop, context, true);
        StringProperty prop2 = new StringProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, context, true);
        assertEquals("StringProperty is different", prop, prop2);

    }


    public void testStringEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
    }

    public void testStringUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testString(store);
        testString(store);
    }

    public void testStringList(XWikiHibernateStore store) throws XWikiException {
        StringListProperty prop = Utils.prepareStringListProperty();
        store.saveXWikiProperty(prop, context, true);
        StringListProperty prop2 = new StringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, context, true);
        assertEquals("StringListProperty is different", prop, prop2);

    }


    public void testStringListEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testStringList(store);
    }

    public void testStringListUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testStringList(store);
        testStringList(store);
    }

    public void testDBStringList(XWikiHibernateStore store) throws XWikiException {
        DBStringListProperty prop = Utils.prepareDBStringListProperty();
        store.saveXWikiProperty(prop, context, true);
        DBStringListProperty prop2 = new DBStringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, context, true);
        assertEquals("DBStringListProperty is different", prop, prop2);
    }


    public void testDBStringListEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testDBStringList(store);
    }

    public void testDBStringListUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        testDBStringList(store);
        testDBStringList(store);
    }


    public void testWriteObject(XWikiHibernateStore store, BaseObject object) throws  XWikiException {
        store.saveXWikiObject(object, context,true);
    }

    public void testReadObject(XWikiHibernateStore store, BaseObject object) throws  XWikiException {
        // Prepare object2 for reading
        BaseObject object2 = new BaseObject();
        object2.setClassName(object.getClassName());
        object2.setName(object.getName());

        // Read object2
        store.loadXWikiObject(object2, context, true);

        // Verify object2
        Utils.assertProperty(object2, object, "first_name");
        Utils.assertProperty(object2, object, "age");

        assertEquals("Object is different", object, object2);
    }

    public void testWriteObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObject(store, object);
    }

    public void testReadWriteObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObject(store, object);
        testReadObject(store, object);
    }

    public void testWriteAdvancedObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObject(store, object);
    }

    public void testReadWriteAdvancedObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObject(store, object);
        testReadObject(store, object);
    }


    public void testWriteClass(XWikiHibernateStore store, BaseClass bclass) throws  XWikiException {
        store.saveXWikiClass(bclass, context, true);
    }

    public void testReadClass(XWikiHibernateStore store, BaseClass bclass) throws  XWikiException {
        // Prepare object2 for reading
        BaseClass bclass2 = new BaseClass();
        bclass2.setName(bclass.getName());

        // Read object2
        store.loadXWikiClass(bclass2, context, true);

        // Verify object2
        Utils.assertProperty(bclass2, bclass, "first_name");
        Utils.assertProperty(bclass2, bclass, "age");

        assertEquals("Class is different", bclass, bclass2);
    }

    public void testWriteClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClass(store, bclass);
    }

    public void testReadWriteClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClass(store, bclass);
        testReadClass(store, bclass);
    }

     public void testWriteAdvancedClass()  throws  XWikiException {
         XWikiHibernateStore store = (XWikiHibernateStore)getStore();
         XWikiSimpleDoc doc = new XWikiSimpleDoc();
         Utils.prepareAdvancedObject(doc);
         BaseClass bclass = doc.getxWikiClass();
         BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClass(store, bclass);
    }

    public void testReadWriteAdvancedClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClass(store, bclass);
        testReadClass(store, bclass);
    }

    public void testSearchClass() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore)getStore();
        List list = store.getClassList(context);
        assertTrue("No result", (list.size()==0) );
        testWriteClass();
        list = store.getClassList(context);
        assertTrue("No result", (list.size()>0) );
    }

     

}