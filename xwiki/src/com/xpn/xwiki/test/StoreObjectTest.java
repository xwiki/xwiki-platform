

package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.doc.XWikiDocInterface;
import net.sf.hibernate.HibernateException;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

import org.dom4j.DocumentException;

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

public abstract class StoreObjectTest extends TestCase {

    public static String rcspath = "./rcs";

    public abstract XWikiStoreInterface getStore();

    public void testWriteObjectInDoc(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Test","TestObject");
        object.setName("Test.TestObject");
        doc.setObject("Test.TestObject", 0, object);
        store.saveXWikiDoc(doc);
    }

    public void testReadObjectInDoc(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        // Prepare object2 for reading
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Test","TestObject");

        // Read object2
        doc = (XWikiSimpleDoc) store.loadXWikiDoc(doc);
        BaseObject object2 = doc.getxWikiObject();

        // Verify object2
        Utils.assertProperty(object2, object, "first_name");
        Utils.assertProperty(object2, object, "age");
    }

    public void testWriteObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteObjectInDoc(store, object);
    }

    public void testReadWriteObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteObjectInDoc(store, object);
        testReadObjectInDoc(store, object);
    }

    public void testWriteClassInDoc(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Test","TestClass");
        bclass.setName("Test.TestClass");
        doc.setxWikiClass(bclass);
        store.saveXWikiDoc(doc);
    }

    public void testReadClassInDoc(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        // Prepare object2 for reading
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Test","TestClass");

        // Read class
        doc = (XWikiSimpleDoc) store.loadXWikiDoc(doc);
        BaseClass bclass2 = doc.getxWikiClass();

        // Verify object2
        Utils.assertProperty(bclass2, bclass, "first_name");
        Utils.assertProperty(bclass2, bclass, "age");
    }

    public void testWriteClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteClassInDoc(store, object.getxWikiClass());
    }

    public void testReadWriteClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareObject();
        testWriteClassInDoc(store, object.getxWikiClass());
        testReadClassInDoc(store, object.getxWikiClass());
    }

   public void testVersionedObject() throws XWikiException {
       XWikiStoreInterface store = getStore();
       BaseObject bobject = Utils.prepareObject("Test.TestVersion");
       Utils.createDoc(store, "Test", "TestVersion", bobject, bobject.getxWikiClass(), null);
       XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Test", "TestVersion");
       doc1 = (XWikiSimpleDoc) store.loadXWikiDoc(doc1);
       BaseObject bobject1 = doc1.getxWikiObject();
       BaseProperty bprop1 = ((BaseProperty)bobject1.safeget("age"));
       assertEquals("Age should be 33", new Integer(33), bprop1.getValue());
       bprop1.setValue(new Integer(5));
       store.saveXWikiDoc(doc1);
       XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Test", "TestVersion");
       doc2 = (XWikiSimpleDoc) store.loadXWikiDoc(doc2);
       BaseObject bobject2 = doc2.getxWikiObject();
       BaseProperty bprop2 = ((BaseProperty)bobject2.safeget("age"));
       assertEquals("Age should be 5", new Integer(5), bprop2.getValue());
       XWikiDocInterface doc3 = store.loadXWikiDoc(doc2, "1.1");
       BaseObject bobject3 = doc3.getxWikiObject();
       BaseProperty bprop3 = ((BaseProperty)bobject3.safeget("age"));
       assertEquals("Age should be 33", new Integer(33), bprop3.getValue());
   }

    public void testXML() throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
         XWikiStoreInterface store = getStore();
         BaseObject bobject = Utils.prepareObject("Test.TestVersion");
         Utils.createDoc(store, "Test", "TestVersion", bobject, bobject.getxWikiClass(), null);
         XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Test", "TestVersion");
         doc1 = (XWikiSimpleDoc) store.loadXWikiDoc(doc1);
         String xml = doc1.toXML();
         XWikiSimpleDoc doc2 = new XWikiSimpleDoc();
         doc2.fromXML(xml);
         Utils.assertEquals(doc1, doc2);
     }


}
