


package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.*;
import junit.framework.TestCase;
import net.sf.hibernate.HibernateException;

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
 * Date: 19 déc. 2003
 * Time: 17:31:37
 */

public class ClassesTest extends TestCase {

    public void testNumber() {
        NumberClass pclass = new NumberClass();
        NumberProperty property;
        property = (NumberProperty)pclass.fromString("10");
        assertEquals("Default type long not supported", new Long("10"), property.getValue());
        pclass.setNumberType("integer");
        property = (NumberProperty)pclass.fromString("10");
        assertEquals("Integer number not supported", new Integer("10"), property.getValue());
        pclass.setNumberType("long");
        property = (NumberProperty)pclass.fromString("10");
        assertEquals("Long number not supported", new Long("10"), property.getValue());
        pclass.setNumberType("double");
        property = (NumberProperty)pclass.fromString("10.01");
        assertEquals("Double number not supported", new Double("10.01"), property.getValue());
        pclass.setNumberType("float");
        property = (NumberProperty)pclass.fromString("10.01");
        assertEquals("Float number not supported", new Float("10.01"), property.getValue());
    }


    public void testString() {
        StringClass pclass = new StringClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("Hello");
        assertEquals("String not supported", new String("Hello"), property.getValue());
    }

    public void testPassword() {
        PasswordClass pclass = new PasswordClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("Hello");
        assertEquals("Password not supported",  new String("Hello"), property.getValue());
    }

    public void testTextArea() {
        TextAreaClass pclass = new TextAreaClass();
        LargeStringProperty property;
        property = (LargeStringProperty)pclass.fromString("Hello1\nHello2\nHello3\n");
        assertEquals("TextArea not supported", new String("Hello1\nHello2\nHello3\n"), property.getValue());
    }

    public void testBoolean() {
        BooleanClass pclass = new BooleanClass();
        IntegerProperty property;
        property = (IntegerProperty)pclass.fromString("1");
        assertEquals("Boolean not supported", new Integer(1), property.getValue());
    }

    public void testStaticList() {
        StaticListClass pclass = new StaticListClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("1");
        assertEquals("StaticList failed on single value", "1", property.toText());
        assertEquals("StaticList failed on single value", "1", property.toFormString());

        property = (StringProperty)pclass.fromString("1|2");
        assertEquals("StaticList failed on multiple value", "1|2", property.toText());
        assertEquals("StaticList failed on multiple value", "1|2", property.toFormString());
    }

    public void testStaticMultiList() {
        StaticListClass pclass = new StaticListClass();
        pclass.setMultiSelect(true);
        ListProperty property;
        property = (ListProperty)pclass.fromString("1");
        assertEquals("StaticList failed on single value", "1", property.toText());
        assertEquals("StaticList failed on single value", "1", property.toFormString());

        property = (ListProperty)pclass.fromString("1|2");
        assertEquals("StaticList failed on multiple value", "1 2", property.toText());
        assertEquals("StaticList failed on multiple value", "1|2", property.toFormString());
    }

    public void testRelationalStaticList() {
            StaticListClass pclass = new StaticListClass();
            pclass.setRelationalStorage(true);
            pclass.setMultiSelect(true);

            ListProperty property;
            property = (ListProperty)pclass.fromString("1");
            assertEquals("StaticList failed on single value", property.toText(), "1");
            assertEquals("StaticList failed on single value", property.toFormString(), "1");
            property = (ListProperty)pclass.fromString("1|2");
            assertEquals("StaticList failed on multiple value", property.toText(), "1 2");
            assertEquals("StaticList failed on multiple value", property.toFormString(), "1|2");
        }


    public void testDisplayer(String cname, BaseObject obj, String viewexpected, String editexpected) {
        testDisplayer(cname, obj, viewexpected, editexpected, null);
    }

    public void testDisplayer(String cname, BaseObject obj, String viewexpected, String editexpected, XWikiContext context) {
      if (context==null)
         context = new XWikiContext();
      StringBuffer result = new StringBuffer();
      PropertyClass pclass = (PropertyClass)obj.getxWikiClass().get(cname);
      pclass.displayView(result,cname, "", obj, context);
      assertEquals("Class " + cname + " view displayer not correct:\n" +
                    "Expected: " + viewexpected + "\nResult: " + result,
                    viewexpected.toLowerCase(), result.toString().toLowerCase());

      result = new StringBuffer();
      pclass.displayEdit(result,cname, "", obj, context);
      assertTrue("Class " + cname + " edit displayer not correct" +
                 "\nExpected: " + editexpected + "\nResult: " + result,
                 result.toString().toLowerCase().indexOf(editexpected.toLowerCase())!=-1);

      pclass.displayHidden(result,cname, "", obj, context);
      pclass.displaySearch(result,cname, "", obj, context);
    }

    public void testBasicDisplayers() throws XWikiException {
        BaseObject obj = Utils.prepareObject();
        testDisplayer("age", obj, "33", "value=\'33\'");
        testDisplayer("first_name", obj, "Ludovic", "value=\'Ludovic\'");
        testDisplayer("last_name", obj, "von Dubost", "value=\'von Dubost\'");
        testDisplayer("password", obj, "********", "value=\'********\'");
        testDisplayer("comment", obj, "Hello1\nHello2\nHello3\n", "textarea");
    }

    public void testBooleanDisplayers() throws XWikiException {
        BaseObject obj = Utils.prepareAdvancedObject();
        testDisplayer("driver", obj, "true", "<select");
    }

    public void testListDisplayers() throws XWikiException {
        BaseObject obj = Utils.prepareAdvancedObject();
        testDisplayer("category", obj, "1", "<select");
        testDisplayer("category2", obj, "1 2", "multiple");
        testDisplayer("category3", obj, "1 2", "multiple");
    }


    public XWikiHibernateStore getHibStore(XWiki xwiki) {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheInterface)
            return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void testDBListDisplayers() throws XWikiException, HibernateException {
        XWikiContext context = new XWikiContext();
        StoreHibernateTest.cleanUp(new XWikiHibernateStore(StoreHibernateTest.hibpath), context);
        XWiki xwiki = new XWiki("./xwiki.cfg", context);

        try {
            BaseObject obj = Utils.prepareAdvancedObject();
            testDisplayer("dblist", obj, "XWikiUsers", "<option selected='selected' value='XWikiUsers' label='XWikiUsers'>", context);
        } finally {
            xwiki = null;
            System.gc();
        }
    }

    public void testObject() throws XWikiException {
        BaseObject object = Utils.prepareObject();
    }

}
