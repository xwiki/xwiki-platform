
import com.xpn.xwiki.classes.*;
import com.xpn.xwiki.XWikiException;
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
        XWikiClassNumberProperty pclass = new XWikiClassNumberProperty();
        XWikiObjectNumberProperty property;
        property = (XWikiObjectNumberProperty)pclass.fromString("10");
        assertEquals("Default type long not supported", property.getValue(), new Long("10"));
        pclass.setNumberType("integer");
        property = (XWikiObjectNumberProperty)pclass.fromString("10");
        assertEquals("Integer number not supported", property.getValue(), new Integer("10"));
        pclass.setNumberType("long");
        property = (XWikiObjectNumberProperty)pclass.fromString("10");
        assertEquals("Long number not supported", property.getValue(), new Long("10"));
        pclass.setNumberType("double");
        property = (XWikiObjectNumberProperty)pclass.fromString("10.01");
        assertEquals("Double number not supported", property.getValue(), new Double("10.01"));
        pclass.setNumberType("float");
        property = (XWikiObjectNumberProperty)pclass.fromString("10.01");
        assertEquals("Float number not supported", property.getValue(), new Float("10.01"));
    }

    public void testString() {
        XWikiClassStringProperty pclass = new XWikiClassStringProperty();
        XWikiObjectStringProperty property;
        property = (XWikiObjectStringProperty)pclass.fromString("Hello");
        assertEquals("String not supported", property.getValue(), new String("Hello"));
    }

    public void testObject() throws XWikiException {
        XWikiClass wclass = new XWikiClass();
        XWikiClassStringProperty first_name_class = new XWikiClassStringProperty();
        first_name_class.setSize(80);
        XWikiClassStringProperty last_name_class = new XWikiClassStringProperty();
        last_name_class.setSize(80);
        XWikiClassNumberProperty age_class = new XWikiClassNumberProperty();
        age_class.setSize(5);
        age_class.setNumberType("integer");
        wclass.put("first_name", first_name_class);
        wclass.put("last_name", last_name_class);
        wclass.put("age", age_class);
        XWikiObject object = new XWikiObject();
        object.setxWikiClass(wclass);
        object.put("first_name", first_name_class.fromString("Ludovic"));
        object.put("last_name", last_name_class.fromString("Dubost"));
        object.put("age", last_name_class.fromString("33"));
    }

}
