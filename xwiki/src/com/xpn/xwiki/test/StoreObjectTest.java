

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import org.dom4j.DocumentException;

import java.text.ParseException;

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

    public String rcspath = "./rcs";

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

    public void testWriteAdvancedObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
        testWriteObjectInDoc(store, object);
    }

    public void testReadWriteAdvancedObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
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

    public void testWriteAdvancedClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
        testWriteClassInDoc(store, object.getxWikiClass());
    }

    public void testReadWriteAdvancedClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getStore();
        BaseObject object = Utils.prepareAdvancedObject();
        testWriteClassInDoc(store, object.getxWikiClass());
        testReadClassInDoc(store, object.getxWikiClass());
    }


    public void testVersionedObject(XWikiStoreInterface store, BaseObject object) throws  XWikiException {

       Utils.createDoc(store, "Test", "TestVersion", object, object.getxWikiClass(), null);
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

    public void testVersionedObject() throws XWikiException {
         XWikiStoreInterface store = getStore();
         BaseObject bobject = Utils.prepareObject("Test.TestVersion");
         testVersionedObject(store, bobject);
     }

    public void testVersionedAdvancedObject() throws XWikiException {
         XWikiStoreInterface store = getStore();
         BaseObject bobject = Utils.prepareAdvancedObject("Test.TestVersion");
         testVersionedObject(store, bobject);
     }

    public void testXML(XWikiStoreInterface store, BaseObject bobject) throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
         XWikiSimpleDoc doc0 = Utils.createDoc(store, "Test", "TestVersion", bobject, bobject.getxWikiClass(), null);
         String xml0 = doc0.toXML();
         XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Test", "TestVersion");
         doc1 = (XWikiSimpleDoc) store.loadXWikiDoc(doc1);
         Utils.assertEquals(doc0, doc1);
         String xml1 = doc1.toXML();
         // Cannot test this because XML tags can be ordered differently
         // assertEquals("XML is different", xml0, xml1);
         XWikiSimpleDoc doc2 = new XWikiSimpleDoc();
         doc2.fromXML(xml1);
         Utils.assertEquals(doc1, doc2);
     }

    public void testXML() throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
    XWikiStoreInterface store = getStore();
    BaseObject bobject = Utils.prepareObject("Test.TestVersion");
    testXML(store, bobject);
    }

    public void testAdvancedXML() throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
    XWikiStoreInterface store = getStore();
    BaseObject bobject = Utils.prepareAdvancedObject("Test.TestVersion");
    testXML(store, bobject);
    }

}
