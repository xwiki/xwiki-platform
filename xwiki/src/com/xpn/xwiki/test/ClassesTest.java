


package com.xpn.xwiki.test;

import com.xpn.xwiki.objects.classes.*;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringProperty;
import junit.framework.TestCase;

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
        assertEquals("Default type long not supported", property.getValue(), new Long("10"));
        pclass.setNumberType("integer");
        property = (NumberProperty)pclass.fromString("10");
        assertEquals("Integer number not supported", property.getValue(), new Integer("10"));
        pclass.setNumberType("long");
        property = (NumberProperty)pclass.fromString("10");
        assertEquals("Long number not supported", property.getValue(), new Long("10"));
        pclass.setNumberType("double");
        property = (NumberProperty)pclass.fromString("10.01");
        assertEquals("Double number not supported", property.getValue(), new Double("10.01"));
        pclass.setNumberType("float");
        property = (NumberProperty)pclass.fromString("10.01");
        assertEquals("Float number not supported", property.getValue(), new Float("10.01"));
    }


    public void testString() {
        StringClass pclass = new StringClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("Hello");
        assertEquals("String not supported", property.getValue(), new String("Hello"));
    }

    public void testPassword() {
        PasswordClass pclass = new PasswordClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("Hello");
        assertEquals("Password not supported", property.getValue(), new String("Hello"));
    }

    public void testTextArea() {
        TextAreaClass pclass = new TextAreaClass();
        StringProperty property;
        property = (StringProperty)pclass.fromString("Hello1\nHello2\nHello3\n");
        assertEquals("TextArea not supported", property.getValue(), new String("Hello1\nHello2\nHello3\n"));
    }

    public void testDisplayer(String cname, BaseObject obj, String viewexpected, String editexpected) {
      XWikiContext context = new XWikiContext();
      StringBuffer result = new StringBuffer();
      PropertyClass pclass = (PropertyClass)obj.getxWikiClass().get(cname);
      pclass.displayView(result,cname, "", obj, context);
      assertEquals("Class " + cname + " view displayer not correct:\n" +
                    "Expected: " + viewexpected + "\nResult: " + result,
                    result.toString().toLowerCase(),viewexpected.toLowerCase());

      result = new StringBuffer();
      pclass.displayEdit(result,cname, "", obj, context);
      assertTrue("Class " + cname + " edit displayer not correct" +
                 "\nExpected: " + editexpected + "\nResult: " + result,
                 result.toString().toLowerCase().indexOf(editexpected.toLowerCase())!=-1);

      pclass.displayHidden(result,cname, "", obj, context);
      pclass.displaySearch(result,cname, "", obj, context);
    }

    public void testNumberDisplayers() throws XWikiException {
        BaseObject obj = Utils.prepareObject();
        testDisplayer("age", obj, "33", "value=\'33\'");
        testDisplayer("first_name", obj, "Ludovic", "value=\'Ludovic\'");
        testDisplayer("last_name", obj, "von Dubost", "value=\'von Dubost\'");
        testDisplayer("password", obj, "********", "value=\'********\'");
        testDisplayer("comment", obj, "Hello1\nHello2\nHello3\n", "textarea");
    }

    public void testObject() throws XWikiException {
        BaseObject object = Utils.prepareObject();
    }

}
