

package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.XWikiException;
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
 * Date: 24 déc. 2003
 * Time: 10:37:55
 */

public class StoreObjectTest extends TestCase {

    public static String hibpath = "hibernate-test.cfg.xml";

    public void setUp() throws HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        cleanUp(hibstore);
    }

    public static void cleanUp(XWikiHibernateStore hibstore) throws HibernateException {
        hibstore.checkHibernate();
        hibstore.beginTransaction();
        StoreTest.runSQL(hibstore, "drop table xwikidoc");
        StoreTest.runSQL(hibstore, "drop table xwikiobjects");
        StoreTest.runSQL(hibstore, "drop table xwikiproperties");
        StoreTest.runSQL(hibstore, "drop table xwikiintegers");
        StoreTest.runSQL(hibstore, "drop table xwikifloats");
        StoreTest.runSQL(hibstore, "drop table xwikilongs");
        StoreTest.runSQL(hibstore, "drop table xwikidoubles");
        StoreTest.runSQL(hibstore, "drop table xwikistrings");
        StoreTest.runSQL(hibstore, "drop table xwikiclasses");
        StoreTest.runSQL(hibstore, "drop table xwikiclassesprop");
        StoreTest.runSQL(hibstore, "drop table xwikistringclasses");
        StoreTest.runSQL(hibstore, "drop table xwikinumberclasses");
        hibstore.endTransaction(true);
        hibstore.updateSchema();
    }

    public void testNumber(XWikiHibernateStore hibstore) throws XWikiException {
        IntegerProperty prop = ObjectTest.prepareIntegerProperty();
        hibstore.saveXWikiProperty(prop, true);
    }


    public void testNumberEmptyDatabase() throws XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testNumber(hibstore);
    }

    public void testNumberUpdate() throws XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testNumber(hibstore);
        testNumber(hibstore);
    }

    public void testNumberBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testNumber(hibstore);
        hibstore.beginTransaction();
        StoreTest.runSQL(hibstore, "delete from xwikiproperties");
        hibstore.endTransaction(true);
        testNumber(hibstore);
    }

    public void testNumberBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testNumber(hibstore);
        hibstore.beginTransaction();
        StoreTest.runSQL(hibstore, "delete from xwikiintegers");
        StoreTest.runSQL(hibstore, "delete from xwikilongs");
        hibstore.endTransaction(true);
        testNumber(hibstore);
    }

    public void testString(XWikiHibernateStore hibstore) throws XWikiException {
        StringProperty prop = ObjectTest.prepareStringProperty();
        hibstore.saveXWikiProperty(prop, true);
    }


    public void testStringEmptyDatabase() throws XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testString(hibstore);
    }

    public void testStringUpdate() throws XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testString(hibstore);
        testString(hibstore);
    }

    public void testStringBadDatabase1() throws XWikiException, HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testString(hibstore);
        hibstore.beginTransaction();
        StoreTest.runSQL(hibstore, "delete from xwikiproperties");
        hibstore.endTransaction(true);
        testString(hibstore);
    }

    public void testStringBadDatabase2() throws XWikiException, HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testString(hibstore);
        hibstore.beginTransaction();
        StoreTest.runSQL(hibstore, "delete from xwikistrings");
        hibstore.endTransaction(true);
        testString(hibstore);
    }


    public void testWriteObject(XWikiHibernateStore hibstore, BaseObject object) throws HibernateException, XWikiException {
        hibstore.saveXWikiObject(object, true);
    }

    public void testReadObject(XWikiHibernateStore hibstore, BaseObject object) throws HibernateException, XWikiException {
        // Prepare object2 for reading
        BaseObject object2 = new BaseObject();
        object2.setxWikiClass(object.getxWikiClass());
        object2.setName(object.getName());

        // Read object2
        hibstore.loadXWikiObject(object2, true);

        // Verify object2
        assertProperty(object2, object, "name");
        assertProperty(object2, object, "age");
    }

    public void testWriteObject()  throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        BaseObject object = ObjectTest.prepareObject();
        testWriteObject(hibstore, object);
    }

    public void testReadWriteObject()  throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        BaseObject object = ObjectTest.prepareObject();
        testWriteObject(hibstore, object);
        testReadObject(hibstore, object);
    }

    public void assertProperty(BaseCollection object1, BaseCollection object2, String propname) {
        BaseElement prop1 = (BaseElement)object1.safeget(propname);
        BaseElement prop2 = (BaseElement)object2.safeget(propname);
        assertTrue("Property " + propname + " is different (" + prop1.getName() + "," + prop2.getName() + ")",
                prop1.equals(prop2));
    }


    public void testWriteClass(XWikiHibernateStore hibstore, BaseClass bclass) throws HibernateException, XWikiException {
        hibstore.saveXWikiClass(bclass, true);
    }

    public void testReadClass(XWikiHibernateStore hibstore, BaseClass bclass) throws HibernateException, XWikiException {
        // Prepare object2 for reading
        BaseClass bclass2 = new BaseClass();
        bclass2.setName(bclass.getName());

        // Read object2
        hibstore.loadXWikiClass(bclass2, true);

        // Verify object2
        assertProperty(bclass2, bclass, "name");
        assertProperty(bclass2, bclass, "age");
    }

    public void testWriteClass()  throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        BaseObject object = ObjectTest.prepareObject();
        testWriteClass(hibstore, object.getxWikiClass());
    }

    public void testReadWriteClass()  throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        BaseObject object = ObjectTest.prepareObject();
        testWriteClass(hibstore, object.getxWikiClass());
        testReadClass(hibstore, object.getxWikiClass());
    }

    public void testSearchClass() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        List list = hibstore.getClassList();
        assertTrue("No result", (list.size()==0) );
        testWriteClass();
        list = hibstore.getClassList();
        assertTrue("No result", (list.size()>0) );
    }

}
