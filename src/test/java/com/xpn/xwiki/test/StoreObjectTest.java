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

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.dom4j.DocumentException;

import java.text.ParseException;

public abstract class StoreObjectTest extends HibernateTestCase {

    public String rcspath = "./rcs";

    public void testWriteObjectInDoc(XWikiStoreInterface store, BaseObject object, BaseClass bclass) throws  XWikiException {
        XWikiDocument doc = new XWikiDocument("Test","TestObject");
        object.setName("Test.TestObject");
        doc.setObject("Test.TestObject", 0, object);
        doc.setxWikiClass(bclass);
        store.saveXWikiDoc(doc, getXWikiContext());
    }

    public void testReadObjectInDoc(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        testReadObjectInDoc(store, object, false);
    }

    public void testReadObjectInDoc(XWikiStoreInterface store, BaseObject object, boolean advanced) throws  XWikiException {
        // Prepare object2 for reading
        XWikiDocument doc = new XWikiDocument("Test","TestObject");

        // Read object2
        doc = (XWikiDocument) store.loadXWikiDoc(doc, getXWikiContext());
        BaseObject object2 = doc.getxWikiObject();

        // Verify object2
        Utils.assertProperty(object2, object, "first_name");
        Utils.assertProperty(object2, object, "age");

        if (advanced) {
            Utils.assertProperty(object2, object, "driver");
            Utils.assertProperty(object2, object, "category");
            Utils.assertProperty(object2, object, "category2");
            Utils.assertProperty(object2, object, "category3");
            Utils.assertProperty(object2, object, "dblist");
        } else {
            assertNull("driver Field should not exist", object2.safeget("driver"));
            assertNull("category Field should not exist", object2.safeget("category"));
            assertNull("category2 Field should not exist", object2.safeget("category2"));
            assertNull("category3 Field should not exist", object2.safeget("category3"));
            assertNull("dblist Field should not exist", object2.safeget("dblist"));
        }


    }

    public void testDeleteObjectAndDoc(XWikiStoreInterface store, BaseObject object) throws  XWikiException {
        // Prepare object2 for reading
        XWikiDocument doc = new XWikiDocument("Test","TestObject");

        // Load document (needed to delete)
        doc = (XWikiDocument) store.loadXWikiDoc(doc, getXWikiContext());
        // Delete object2
        store.deleteXWikiDoc(doc, getXWikiContext());
        XWikiDocument doc2 = new XWikiDocument("Test","TestObject");

        // Read object2
        doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());
        assertTrue("Document should not exist", doc2.isNew());
    }

    public void testWriteObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
    }

    public void testReadWriteObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
        testReadObjectInDoc(store, object);
    }

    public void testDeleteObjectAndDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
        testDeleteObjectAndDoc(store, object);
        // To test that delete did it's work properly
        // we use a smaller object
        doc = new XWikiDocument();
        Utils.prepareObject(doc);
        bclass = doc.getxWikiClass();
        object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
        testReadObjectInDoc(store, object);
    }

    public void testWriteAdvancedObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
    }

    public void testReadWriteAdvancedObjectInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteObjectInDoc(store, object, bclass);
        testReadObjectInDoc(store, object, true);
    }



    public void testWriteClassInDoc(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        XWikiDocument doc = new XWikiDocument("Test","TestClass");
        bclass.setName("Test.TestClass");
        doc.setxWikiClass(bclass);
        store.saveXWikiDoc(doc, getXWikiContext());
    }

    public void testReadClassInDoc(XWikiStoreInterface store, BaseClass bclass) throws  XWikiException {
        // Prepare object2 for reading
        XWikiDocument doc = new XWikiDocument("Test","TestClass");

        // Read class
        doc = (XWikiDocument) store.loadXWikiDoc(doc, getXWikiContext());
        BaseClass bclass2 = doc.getxWikiClass();

        // Verify object2
        Utils.assertProperty(bclass2, bclass, "first_name");
        Utils.assertProperty(bclass2, bclass, "age");
    }

    public void testWriteClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClassInDoc(store, bclass);
    }

    public void testReadWriteClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClassInDoc(store, bclass);
        testReadClassInDoc(store, bclass);
    }

    public void testWriteAdvancedClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClassInDoc(store, bclass);
    }

    public void testReadWriteAdvancedClassInDoc()  throws  XWikiException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject object = doc.getObject(bclass.getName(), 0);
        testWriteClassInDoc(store, bclass);
        testReadClassInDoc(store, bclass);
    }


    public void testVersionedObject(XWikiStoreInterface store, BaseObject object, BaseClass bclass) throws  XWikiException {
       Utils.createDoc(store, "Test", "TestVersion", object, bclass, null, getXWikiContext());
       XWikiDocument doc1 = new XWikiDocument("Test", "TestVersion");
       doc1 = (XWikiDocument) store.loadXWikiDoc(doc1, getXWikiContext());
       BaseObject bobject1 = doc1.getxWikiObject();
       BaseProperty bprop1 = ((BaseProperty)bobject1.safeget("age"));
       assertEquals("Age should be 33", new Integer(33), bprop1.getValue());
       bprop1.setValue(new Integer(5));
       store.saveXWikiDoc(doc1, getXWikiContext());
       XWikiDocument doc2 = new XWikiDocument("Test", "TestVersion");
       doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());
       BaseObject bobject2 = doc2.getxWikiObject();
       BaseProperty bprop2 = ((BaseProperty)bobject2.safeget("age"));
       assertEquals("Age should be 5", new Integer(5), bprop2.getValue());
       XWikiDocument doc3 = store.loadXWikiDoc(doc2, "1.1", getXWikiContext());
       BaseObject bobject3 = doc3.getxWikiObject();
       BaseProperty bprop3 = ((BaseProperty)bobject3.safeget("age"));
       assertEquals("Age should be 33", new Integer(33), bprop3.getValue());
   }

    public void testVersionedObject() throws XWikiException {
         XWikiStoreInterface store = getXWiki().getStore();
         XWikiDocument doc = new XWikiDocument();
         Utils.prepareObject(doc, "Test.TestVersion");
         BaseClass bclass = doc.getxWikiClass();
         BaseObject bobject = doc.getObject(bclass.getName(), 0);
         testVersionedObject(store, bobject, bclass);
     }

    public void testVersionedAdvancedObject() throws XWikiException {
         XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc, "Test.TestVersion");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        testVersionedObject(store, bobject, bclass);
     }

    public void testXML(XWikiStoreInterface store, BaseObject bobject, BaseClass bclass) throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
         XWikiDocument doc0 = Utils.createDoc(store, "Test", "TestVersion", bobject, bclass, null, getXWikiContext());
         String xml0 = doc0.toXML(getXWikiContext());
         XWikiDocument doc1 = new XWikiDocument("Test", "TestVersion");
         doc1 = (XWikiDocument) store.loadXWikiDoc(doc1, getXWikiContext());
         Utils.assertEquals(doc0, doc1);
         String xml1 = doc1.toXML(getXWikiContext());
         // Cannot test this because XML tags can be ordered differently
         // assertEquals("XML is different", xml0, xml1);
         XWikiDocument doc2 = new XWikiDocument();
         doc2.fromXML(xml1);
         Utils.assertEquals(doc1, doc2);
     }

    public void testXML() throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Test.TestVersion");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        testXML(store, bobject, bclass);
    }

    public void testAdvancedXML() throws XWikiException, DocumentException, IllegalAccessException, ParseException, ClassNotFoundException, InstantiationException {
        XWikiStoreInterface store = getXWiki().getStore();
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc, "Test.TestVersion");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        testXML(store, bobject, bclass);
    }

}
