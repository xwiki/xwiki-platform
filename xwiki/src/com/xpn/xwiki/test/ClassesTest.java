
import com.xpn.xwiki.objects.classes.*;
import com.xpn.xwiki.XWikiException;
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

    public void testObject() throws XWikiException {
        BaseClass wclass = new BaseClass();
        StringClass first_name_class = new StringClass();
        first_name_class.setSize(80);
        StringClass last_name_class = new StringClass();
        last_name_class.setSize(80);
        NumberClass age_class = new NumberClass();
        age_class.setSize(5);
        age_class.setNumberType("integer");
        wclass.put("first_name", first_name_class);
        wclass.put("last_name", last_name_class);
        wclass.put("age", age_class);
        BaseObject object = new BaseObject();
        object.setxWikiClass(wclass);
        object.put("first_name", first_name_class.fromString("Ludovic"));
        object.put("last_name", last_name_class.fromString("Dubost"));
        object.put("age", last_name_class.fromString("33"));
    }

}
