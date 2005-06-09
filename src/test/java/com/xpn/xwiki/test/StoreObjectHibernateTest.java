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
 */
package com.xpn.xwiki.test;

import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class StoreObjectHibernateTest extends HibernateTestCase {

    public void testStringBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        string(store);
        store.beginTransaction(getXWikiContext());
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties", getXWikiContext());
        store.endTransaction(getXWikiContext(), true);
        string(store);
    }

    public void testStringBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        string(store);
        store.beginTransaction(getXWikiContext());
        StoreHibernateTest.runSQL(store, "delete from xwikistrings", getXWikiContext());
        store.endTransaction(getXWikiContext(), true);
        string(store);
    }

    public void testNumberBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        number(store);
        store.beginTransaction(getXWikiContext());
        StoreHibernateTest.runSQL(store, "delete from xwikiproperties", getXWikiContext());
        store.endTransaction(getXWikiContext(), true);
        number(store);
    }

    public void testNumberBadDatabase2() throws XWikiException, HibernateException{
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        number(store);
        store.beginTransaction(getXWikiContext());
        StoreHibernateTest.runSQL(store, "delete from xwikiintegers", getXWikiContext());
        StoreHibernateTest.runSQL(store, "delete from xwikilongs", getXWikiContext());
        store.endTransaction(getXWikiContext(), true);
        number(store);
    }

    public void number(XWikiHibernateStore store) throws XWikiException {
        IntegerProperty prop = Utils.prepareIntegerProperty();
        store.saveXWikiProperty(prop, getXWikiContext(), true);
        IntegerProperty prop2 = new IntegerProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, getXWikiContext(), true);
        assertEquals("IntegerProperty is different", prop, prop2);
    }

    public void testNumberEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        number(store);
    }

    public void testNumberUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        number(store);
        number(store);
    }

    public void string(XWikiHibernateStore store) throws XWikiException {
        StringProperty prop = Utils.prepareStringProperty();
        store.saveXWikiProperty(prop, getXWikiContext(), true);
        StringProperty prop2 = new StringProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, getXWikiContext(), true);
        assertEquals("StringProperty is different", prop, prop2);
    }

    public void testStringEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        string(store);
    }

    public void testStringUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        string(store);
        string(store);
    }

    public void stringList(XWikiHibernateStore store) throws XWikiException {
        StringListProperty prop = Utils.prepareStringListProperty();
        store.saveXWikiProperty(prop, getXWikiContext(), true);
        StringListProperty prop2 = new StringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, getXWikiContext(), true);
        assertEquals("StringListProperty is different", prop, prop2);
    }

    public void testStringListEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        stringList(store);
    }

    public void testStringListUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        stringList(store);
        stringList(store);
    }

    public void dBStringList(XWikiHibernateStore store) throws XWikiException {
        DBStringListProperty prop = Utils.prepareDBStringListProperty();
        store.saveXWikiProperty(prop, getXWikiContext(), true);
        DBStringListProperty prop2 = new DBStringListProperty();
        prop2.setName(prop.getName());
        prop2.setPrettyName(prop.getPrettyName());
        prop2.setObject(prop.getObject());
        store.loadXWikiProperty(prop2, getXWikiContext(), true);
        assertEquals("DBStringListProperty is different", prop, prop2);
    }

    public void testDBStringListEmptyDatabase() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        dBStringList(store);
    }

    public void testDBStringListUpdate() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        dBStringList(store);
        dBStringList(store);
    }

    public void writeObject(XWikiHibernateStore store, BaseObject object) throws  XWikiException {
        store.saveXWikiObject(object, getXWikiContext(),true);
    }

    public void readObject(XWikiHibernateStore store, BaseObject object) throws  XWikiException {
        // Prepare object2 for reading
        BaseObject object2 = new BaseObject();
        object2.setClassName(object.getClassName());
        object2.setName(object.getName());

        // Read object2
        store.loadXWikiObject(object2, getXWikiContext(), true);

        // Verify object2
        Utils.assertProperty(object2, object, "first_name");
        Utils.assertProperty(object2, object, "age");

        assertEquals("Object is different", object, object2);
    }

    public void testWriteObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeObject(store, object);
    }

    public void testReadWriteObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeObject(store, object);
        readObject(store, object);
    }

    public void testWriteAdvancedObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeObject(store, object);
    }

    public void testReadWriteAdvancedObject()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeObject(store, object);
        readObject(store, object);
    }

    public void writeClass(XWikiHibernateStore store, BaseClass bclass) throws  XWikiException {
        store.saveXWikiClass(bclass, getXWikiContext(), true);
    }

    public void readClass(XWikiHibernateStore store, BaseClass bclass) throws  XWikiException {
        // Prepare object2 for reading
        BaseClass bclass2 = new BaseClass();
        bclass2.setName(bclass.getName());

        // Read object2
        store.loadXWikiClass(bclass2, getXWikiContext(), true);

        // Verify object2
        Utils.assertProperty(bclass2, bclass, "first_name");
        Utils.assertProperty(bclass2, bclass, "age");

        assertEquals("Class is different", bclass, bclass2);
    }

    public void testWriteClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeClass(store, bclass);
    }

    public void testReadWriteClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeClass(store, bclass);
        readClass(store, bclass);
    }

     public void testWriteAdvancedClass()  throws  XWikiException {
         XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
         XWikiDocument doc = new XWikiDocument();
         Utils.prepareAdvancedObject(doc);
         BaseClass bclass = doc.getxWikiClass();
         BaseObject object = doc.getObject(bclass.getName(), 0);
        writeClass(store, bclass);
    }

    public void testReadWriteAdvancedClass()  throws  XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        writeClass(store, bclass);
        readClass(store, bclass);
    }

    public void testSearchClass() throws XWikiException {
        XWikiHibernateStore store = (XWikiHibernateStore) getXWiki().getStore();
        List list = store.getClassList(getXWikiContext());
        assertTrue("No result", (list.size()==0) );
        testWriteClass();
        list = store.getClassList(getXWikiContext());
        assertTrue("No result", (list.size()>0) );
    }

    public void testSearchCount() throws XWikiException {
//        XWiki xwiki = new XWiki("./xwiki.cfg", getXWikiContext());
//        getXWikiContext().setWiki(xwiki);
//        XWikiHibernateStore store = xwiki.getHibernateStore();
        Utils.createDoc(getXWiki().getStore(), "XWiki", "XWikiServerXwikitest", getXWikiContext());
        Utils.setStringValue("XWiki.XWikiServerXwikitest", "XWiki.XWikiServerClass", "server", "127.0.0.1", getXWikiContext());

        List list = getXWiki().getStore().search("select count(*) from XWikiDocument doc, BaseObject as obj where obj.name="
                                    + getXWiki().getFullNameSQL() + " and obj.className='XWiki.XWikiServerClass' and obj.name<>'XWiki.XWikiServerTemplate' group by obj.className", 0 , 0, getXWikiContext());
        Integer result = (Integer) list.get(0);
        assertEquals("Search for object property failed", 1, result.intValue());
    }


    public void testObjectSavePerf() throws XWikiException {
//         XWiki xwiki = new XWiki("./xwiki.cfg", getXWikiContext());
//         getXWikiContext().setWiki(xwiki);
//         XWikiHibernateStore store = xwiki.getHibernateStore();

        // Start monitoring timer
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        if (monitor!=null)
          monitor.startRequest("", "test", null);

        Date starttime = new Date();
        Utils.addManyMembers(getXWiki(), getXWikiContext(), "XWiki.LudovicDubost", "XWiki.XWikiAllGroup", 40);
        Date endtime = new Date();
        long delay = endtime.getTime() - starttime.getTime();

        if (monitor!=null)
          monitor.endRequest();
        assertTrue("Creation delay is way too long (over 10s): " + delay, (delay < 10000));
    }

    public void testObjectReadPerf() throws XWikiException {
        int nb = 100;
//        XWiki xwiki = new XWiki("./xwiki.cfg", getXWikiContext());
//         getXWikiContext().setWiki(xwiki);
//         XWikiHibernateStore store = xwiki.getHibernateStore();
         Utils.addManyMembers(getXWiki(), getXWikiContext(), "XWiki.LudovicDubost", "XWiki.XWikiAllGroup", nb);
         getXWiki().flushCache();

        // Start monitoring timer
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        if (monitor!=null)
          monitor.startRequest("", "test", null);

        Date starttime = new Date();
        XWikiDocument doc = getXWiki().getDocument("XWiki.XWikiAllGroup", getXWikiContext());
        Date endtime = new Date();
        long delay = endtime.getTime() - starttime.getTime();

        if (monitor!=null)
          monitor.endRequest();
        assertTrue("Read delay is way too long (over 5s): " + delay, (delay < 1000));
        assertEquals("Could not find members", nb, doc.getObjects("XWiki.XWikiGroups").size());
    }
}
