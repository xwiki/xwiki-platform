package com.xpn.xwiki.test;

import java.net.URL;
import java.sql.SQLException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 août 2005
 * Time: 16:53:04
 * To change this template use File | Settings | File Templates.
 */
public class HibernateCustomClassTest extends HibernateTestCase {

    public String customclassname = "com.xpn.xwiki.test.HibernateCustomClass";

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

    public void testGetCustomClassName() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomClass(customclassname);
        String customclassname2 = doc1class.getCustomClass();
        assertEquals("Custom Class Name is incorrect", customclassname, customclassname2);
    }

    public void testGetCustomClassNameInDoc() throws XWikiException {
        XWikiDocument doc1 = Utils.createDoc(getXWiki().getHibernateStore(), "Test", "HCMClass", getXWikiContext());
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomClass(customclassname);
        getXWiki().saveDocument(doc1, getXWikiContext());
        getXWiki().flushCache();
        XWikiDocument doc2 = getXWiki().getDocument("Test.HCMClass", getXWikiContext());
        String customclassname2 = doc2.getxWikiClass().getCustomClass();
        assertEquals("Custom Class Name is incorrect", customclassname, customclassname2);
    }

    public void testCreateNewObjectUsingCustomClass() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomClass(customclassname);
        // Let's save this document that has a custom class
        getXWiki().saveDocument(doc1, getXWikiContext());

        // Now add the new object
        doc1.createNewObject("Test.HCMClass", getXWikiContext());
        assertEquals("Custom class type is wrong", customclassname, doc1.getObject("Test.HCMClass").getClass().getName());
    }

    public void testReadUsingCustomClass() throws XWikiException, SQLException {
        XWikiDocument doc1 = new XWikiDocument("Test", "HCMClass");
        Utils.prepareClass(doc1, "Test.HCMClass");
        Utils.prepareAdvancedClass(doc1, "Test.HCMClass");
        BaseClass doc1class = doc1.getxWikiClass();
        doc1class.setCustomClass(customclassname);
        // Let's save this document that has a custom class
        getXWiki().saveDocument(doc1, getXWikiContext());

        // Now add the object
        Utils.prepareAdvancedObject(doc1, "Test.HCMClass");

        // Let's save this document that has a custom class
        getXWiki().saveDocument(doc1, getXWikiContext());
        getXWiki().flushCache();
        XWikiDocument doc2 = getXWiki().getDocument(doc1.getFullName(), getXWikiContext());
        assertEquals("Custom class type is wrong", customclassname, doc2.getObject("Test.HCMClass").getClass().getName());
    }
}
