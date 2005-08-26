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

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.Configuration;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class HibernateCustomMappingTest extends HibernateTestCase {

    private String custommapping1 =
            "<property name=\"first_name\" type=\"string\">\n" +
            "<column name=\"XWO_FIRST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"last_name\" type=\"string\">\n" +
            "<column name=\"XWO_LAST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"age\" type=\"integer\">\n" +
            "<column name=\"XWO_AGE\" />\n" +
            "</property>\n" +
            "<property name=\"password\" type=\"string\">\n" +
            "<column name=\"XWO_PASSWORD\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"comment\" type=\"string\">\n" +
            "<column name=\"XWO_COMMENT\" length=\"60000\" />\n" +
            "</property>\n";

    private String invalid_custommapping0 = "<property name=\"first_name\" type=\"string\">";

    private String invalid_custommapping1 =
            "<property name=\"first_name\">\n" +
            "<column name=\"XWO_FIRST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"last_name\" type=\"string\">\n" +
            "<column name=\"XWO_LAST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"age\" type=\"integer\">\n" +
            "<column name=\"XWO_AGE\" />\n" +
            "</property>\n" +
            "<property name=\"password\" type=\"string\">\n" +
            "<column name=\"XWO_PASSWORD\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"comment\" type=\"string\">\n" +
            "<column name=\"XWO_COMMENT\" length=\"60000\" />\n" +
            "</property>\n";

    private String invalid_custommapping2 =
            "<property name=\"first\" type=\"string\">\n" +
            "<column name=\"XWO_FIRST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"last_name\" type=\"string\">\n" +
            "<column name=\"XWO_LAST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"age\" type=\"integer\">\n" +
            "<column name=\"XWO_AGE\" />\n" +
            "</property>\n" +
            "<property name=\"password\" type=\"string\">\n" +
            "<column name=\"XWO_PASSWORD\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"comment\" type=\"string\">\n" +
            "<column name=\"XWO_COMMENT\" length=\"60000\" />\n" +
            "</property>\n";

    private String invalid_custommapping3 =
            "<property name=\"first_name\" type=\"integer\">\n" +
            "<column name=\"XWO_FIRST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"last_name\" type=\"string\">\n" +
            "<column name=\"XWO_LAST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"age\" type=\"integer\">\n" +
            "<column name=\"XWO_AGE\" />\n" +
            "</property>\n" +
            "<property name=\"password\" type=\"string\">\n" +
            "<column name=\"XWO_PASSWORD\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"comment\" type=\"string\">\n" +
            "<column name=\"XWO_COMMENT\" length=\"60000\" />\n" +
            "</property>\n";

    private String invalid_custommapping4 =
            "<property name=\"first_name\" type=\"string\">\n" +
            "<column name=\"XWO_FIRST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"last_name\" type=\"string\">\n" +
            "<column name=\"XWO_LAST_NAME\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"age\" type=\"string\">\n" +
            "<column name=\"XWO_AGE\" />\n" +
            "</property>\n" +
            "<property name=\"password\" type=\"string\">\n" +
            "<column name=\"XWO_PASSWORD\" length=\"255\"/>\n" +
            "</property>\n" +
            "<property name=\"comment\" type=\"string\">\n" +
            "<column name=\"XWO_COMMENT\" length=\"60000\" />\n" +
            "</property>\n";

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
        XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
        try {
            hibstore.beginTransaction(getXWikiContext());
            runSQL(hibstore, "delete from xwikicustom_test_hcmclass", getXWikiContext());
        } finally {
            hibstore.endTransaction(getXWikiContext(), true);
        }
    }

    public void testGetCustomMapping() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        String custommapping2 = doc1class.getCustomMapping();
        assertEquals("Custom Mapping is incorrect", custommapping1, custommapping2);
    }

    public void testGetCustomMappingInDoc() throws XWikiException {
        XWikiDocument doc1 = Utils.createDoc(getXWiki().getHibernateStore(), "Test", "HCMClass", getXWikiContext());
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        getXWiki().saveDocument(doc1, getXWikiContext());
        getXWiki().flushCache();
        XWikiDocument doc2 = getXWiki().getDocument("Test.HCMClass", getXWikiContext());
        String custommapping2 = doc2.getxWikiClass().getCustomMapping();
        assertEquals("Custom Mapping is incorrect", custommapping1, custommapping2);
    }

    public void testGetCustomMappingValidity() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        assertTrue("Custom Mapping is invalid", doc1class.isCustomMappingValid(getXWikiContext()));
    }

    public void testGetCustomMappingValidity2() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        assertTrue("Custom Mapping is invalid", doc1class.isCustomMappingValid(getXWikiContext()));
    }

    public void testGetCustomMappingValidityShouldBeInvalid() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        assertTrue("Custom Mapping is invalid", doc1class.isCustomMappingValid("",getXWikiContext()));
        assertTrue("Custom Mapping is invalid", doc1class.isCustomMappingValid(custommapping1,getXWikiContext()));
        assertFalse("Custom Mapping should be invalid", doc1class.isCustomMappingValid(invalid_custommapping0,getXWikiContext()));
        assertFalse("Custom Mapping should be invalid", doc1class.isCustomMappingValid(invalid_custommapping1,getXWikiContext()));
        assertFalse("Custom Mapping should be invalid", doc1class.isCustomMappingValid(invalid_custommapping2,getXWikiContext()));
        assertFalse("Custom Mapping should be invalid", doc1class.isCustomMappingValid(invalid_custommapping3,getXWikiContext()));
        assertFalse("Custom Mapping should be invalid", doc1class.isCustomMappingValid(invalid_custommapping4,getXWikiContext()));
    }

    public void testGetCustomMappingUpdateSchema() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        getXWiki().getHibernateStore().updateSchema(doc1class, getXWikiContext());

        try {
            getXWiki().getHibernateStore().beginTransaction(getXWikiContext());
            Object result = runSQLuniqueResult(getXWiki().getHibernateStore(),"select count(*) from xwikicustom_test_hcmclass", getXWikiContext());
            assertEquals("Table does not exist", 0, ((Number)result).intValue());
            List list = runSQLwithReturn(getXWiki().getHibernateStore(),"select xwo_first_name, xwo_last_name, xwo_comment, xwo_age, xwo_password from xwikicustom_test_hcmclass", getXWikiContext());
            assertNotNull("Table items incorrect does not exist", list);
        } finally {
            getXWiki().getHibernateStore().endTransaction(getXWikiContext(), false);
        }
    }


    public void testInjectCustomMapping() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        getXWiki().getStore().injectCustomMapping(doc1class, getXWikiContext());

        Configuration config = getXWiki().getHibernateStore().getConfiguration();
        assertNotNull("Document is not in mapping", config.getClassMapping("com.xpn.xwiki.doc.XWikiDocument"));
        assertNotNull("New class is not in mapping", config.getClassMapping(doc1class.getName()));
    }

    public void testInjectCustomMappings() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        getXWiki().saveDocument(doc1, getXWikiContext());
        getXWiki().getStore().injectCustomMappings(getXWikiContext());
        Configuration config = getXWiki().getHibernateStore().getConfiguration();
        assertNotNull("Document is not in mapping", config.getClassMapping("com.xpn.xwiki.doc.XWikiDocument"));
        assertNotNull("New class is not in mapping", config.getClassMapping(doc1class.getName()));
    }

    public void testSaveUsingCustomMapping() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());
        getXWiki().getStore().injectCustomMappings(getXWikiContext());
        getXWiki().getHibernateStore().updateSchema(doc1class, getXWikiContext());

        // Now add the object
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");

        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());

        try {
            getXWiki().getHibernateStore().beginTransaction(getXWikiContext());
            Object result = runSQLuniqueResult(getXWiki().getHibernateStore(),"select count(*) from xwikicustom_test_hcmclass", getXWikiContext());
            assertEquals("Table does not exist", 1, ((Number)result).intValue());
            List list = runSQLwithReturn(getXWiki().getHibernateStore(),"select xwo_first_name, xwo_last_name, xwo_comment, xwo_age, xwo_password from xwikicustom_test_hcmclass", getXWikiContext());
            assertNotNull("Table items incorrect does not exist", list);
            assertEquals("Result size incorrect", 1, list.size());
            Map item = (Map)list.get(0);
            assertEquals("First Name incorrect", "Ludovic", item.get("xwo_first_name"));
            assertEquals("Last Name incorrect", "Von Dubost", item.get("xwo_last_name"));
            assertEquals("Age incorrect", new Integer(33), item.get("xwo_age"));
            assertEquals("Password incorrect", "sesame", item.get("xwo_password"));
            assertEquals("Comment incorrect", "Hello1\nHello2\nHello3\n", item.get("xwo_comment"));
        } finally {
            getXWiki().getHibernateStore().endTransaction(getXWikiContext(), false);
        }
    }

    public void testReadUsingCustomMapping() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());

        getXWiki().getHibernateStore().updateSchema(doc1class, getXWikiContext());
        getXWiki().getStore().injectCustomMappings(getXWikiContext());

        // Now add the object
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");

        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());

        getXWiki().flushCache();
        XWikiDocument doc2 = getXWiki().getDocument("Test.HCMClass", getXWikiContext());
        BaseObject object = doc2.getObject("Test.HCMClass");
        assertNotNull("Object does not exist", object);
        assertEquals("First Name incorrect", "Ludovic", object.getStringValue("first_name"));
        assertEquals("Last Name incorrect", "Von Dubost", object.getStringValue("last_name"));
        assertEquals("Age incorrect", 33, object.getIntValue("age"));
        assertEquals("Password incorrect", "sesame", object.getStringValue("password"));
        assertEquals("Comment incorrect", "Hello1\nHello2\nHello3\n", object.getStringValue("comment"));
        assertEquals("Driver incorrect", "1", object.getStringValue("driver"));
    }

    public void testDeleteUsingCustomMapping() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomMapping(custommapping1);
        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());

        getXWiki().getHibernateStore().updateSchema(doc1class, getXWikiContext());
        getXWiki().getStore().injectCustomMappings(getXWikiContext());

        // Now add the object
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");

        // Let's save this document that has a custom mapping
        getXWiki().saveDocument(doc1, getXWikiContext());

        getXWiki().flushCache();
        XWikiDocument doc2 = getXWiki().getDocument("Test.HCMClass", getXWikiContext());

        // We now delete the document
        getXWiki().deleteDocument(doc2, getXWikiContext());

        try {
            getXWiki().getHibernateStore().beginTransaction(getXWikiContext());
        // We should have nothing in the custom mapped table
        List list = runSQLwithReturn(getXWiki().getHibernateStore(),"select xwo_first_name, xwo_last_name, xwo_comment, xwo_age, xwo_password from xwikicustom_test_hcmclass", getXWikiContext());
        assertNotNull("Table items incorrect does not exist", list);
        assertEquals("Result size incorrect", 0, list.size());
        } finally {
            getXWiki().getHibernateStore().endTransaction(getXWikiContext(), false);
        }
    }

}
