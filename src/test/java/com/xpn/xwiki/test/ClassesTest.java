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

import junit.framework.TestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

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

    public void testBasicDisplayers() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject obj = doc.getObject(bclass.getName(), 0);
        testDisplayer("age", obj, bclass, "33", "value=\'33\'");
        testDisplayer("first_name", obj, bclass,  "Ludovic", "value=\'Ludovic\'");
        testDisplayer("last_name", obj, bclass, "von Dubost", "value=\'von Dubost\'");
        testDisplayer("password", obj, bclass, "********", "value=\'********\'");
        testDisplayer("comment", obj, bclass, "Hello1\nHello2\nHello3\n", "textarea");
    }

    public void testBooleanDisplayers() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject obj = doc.getObject(bclass.getName(), 0);

        testDisplayer("driver", obj, bclass, "1", "<select");
    }

    public void testListDisplayers() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc);
        BaseClass bclass = doc.getxWikiClass();
        BaseObject obj = doc.getObject(bclass.getName(), 0);

        testDisplayer("category", obj, bclass, "1", "<select");
        testDisplayer("category2", obj, bclass, "1 2", "multiple");
        testDisplayer("category3", obj, bclass, "1 2", "multiple");
    }

    public void testObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc);
        BaseObject obj = doc.getObject(doc.getxWikiClass().getName(), 0);
    }

    public static void testDisplayer(String cname, BaseObject obj, BaseClass bclass, String viewexpected, String editexpected) {
        testDisplayer(cname, obj, bclass, viewexpected, editexpected, null);
    }

    public static void testDisplayer(String cname, BaseObject obj, BaseClass bclass, String viewexpected, String editexpected, XWikiContext context) {
        if (context==null)
            context = new XWikiContext();
        StringBuffer result = new StringBuffer();
        PropertyClass pclass = (PropertyClass)bclass.get(cname);
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
}
