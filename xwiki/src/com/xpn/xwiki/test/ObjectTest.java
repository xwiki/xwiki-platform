

package com.xpn.xwiki.test;

import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
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
 * Date: 30 déc. 2003
 * Time: 22:49:51
 */

public class ObjectTest extends TestCase {

    public static IntegerProperty prepareIntegerProperty() {
         BaseObject object = new BaseObject();
         object.setName("Main.TestObject1");
         IntegerProperty prop = new IntegerProperty();
         prop.setObject(object);
         prop.setPrettyName("Age");
         prop.setName("age");
         prop.setValue(new Integer(34));
         return prop;
     }

    public static StringProperty prepareStringProperty() {
        BaseObject object = new BaseObject();
        object.setName("Main.TestObject1");
        StringProperty prop = new StringProperty();
        prop.setObject(object);
        prop.setName("name");
        prop.setPrettyName("Name");
        prop.setValue(new String("Dubost"));
        return prop;
    }

    public static BaseObject prepareObject() {
        // Prepare class definition
        BaseClass bclass = new BaseClass();
        StringClass sclass = new StringClass();
        sclass.setName("nameclass");
        sclass.setObject(bclass);
        NumberClass nclass = new NumberClass();
        nclass.setName("ageclass");
        nclass.setObject(bclass);
        bclass.put("nameclass", sclass);
        bclass.put("ageclass", nclass);
        bclass.setName("Main.TestObject2");

        // Prepare object
        BaseObject object = new BaseObject();
        object.setxWikiClass(bclass);
        object.setName("Main.TestObject2");
        StringProperty sprop = new StringProperty();
        sprop.setObject(object);
        sprop.setName("name");
        sprop.setValue(new String("Dubost"));
        object.safeput("name", sprop);
        IntegerProperty iprop = new IntegerProperty();
        iprop.setObject(object);
        iprop.setName("age");
        iprop.setValue(new Integer(34));
        object.safeput("age", iprop);
        return object;
    }

    public void testEqualsNumberProperty() {
         // Test both cloning and equals
         IntegerProperty prop = prepareIntegerProperty();
         IntegerProperty prop2 = (IntegerProperty) prop.clone();

         // test cloning and equals at the same time
         assertTrue("Cloning did not created equals integer property", prop.equals(prop2));

         // Test name (this is common to all elements)
         prop2.setName("toto");
         assertTrue("Equals did not detect different name", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setName(null);
         assertTrue("Equals did not detect different name with null", !prop.equals(prop2));
         prop.setName(null);
         assertTrue("Equals did not detect same null name", prop.equals(prop2));

         // Test pretty name (this is common to all elements)
         prop2 = (IntegerProperty) prop.clone();
         prop2.setPrettyName("toto");
         assertTrue("Equals did not detect different pretty name", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setPrettyName(null);
         assertTrue("Equals did not detect different pretty name with null", !prop.equals(prop2));
         prop.setPrettyName(null);
         assertTrue("Equals did not detect same null pretty name", prop.equals(prop2));

         // Test value (this is integer specific)
         prop2 = (IntegerProperty) prop.clone();
         prop2.setValue(new Integer(1000));
         assertTrue("Equals did not detect different value", !prop.equals(prop2));
         prop2 = (IntegerProperty) prop.clone();
         prop2.setValue(null);
         assertTrue("Equals did not detect different value with null", !prop.equals(prop2));
         prop.setValue(null);
         assertTrue("Equals did not detect same null value", prop.equals(prop2));
     }

     public void testEqualsStringProperty() {
         // Test both cloning and equals
         IntegerProperty iprop = new IntegerProperty();
         StringProperty sprop = new StringProperty();
         assertTrue("IntegerProperty cannot be equals to StringProperty", !iprop.equals(sprop));

         StringProperty prop = prepareStringProperty();
         StringProperty prop2 = (StringProperty) prop.clone();

         // test cloning and equals at the same time
         assertTrue("Cloning did not created equals string property", prop.equals(prop2));

         // Test value (this is string specific)
         prop2 = (StringProperty) prop.clone();
         prop2.setValue("blabla");
         assertTrue("Equals did not detect different value", !prop.equals(prop2));
         prop2 = (StringProperty) prop.clone();
         prop2.setValue(null);
         assertTrue("Equals did not detect different value with null", !prop.equals(prop2));
         prop.setValue(null);
         assertTrue("Equals did not detect same null value", prop.equals(prop2));
     }

     public void testEqualsObject() {
         BaseObject object = prepareObject();
         BaseObject object2 = (BaseObject)object.clone();

         // test cloning and equals at the same time
         assertTrue("Cloning did not created equals objects", object.equals(object2));

         object2.setName("titi");
         assertTrue("Equals did not detect different object name", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object.safeget("age")).setName("titi");
         assertTrue("Equals did not detect changed field property", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         object.getFields().remove("age");
         assertTrue("Equals did not detect missing age field", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         object.getxWikiClass().safeget("ageclass").setName("titi");
         assertTrue("Equals did not detect different class property name", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         ((NumberClass)object.getxWikiClass().safeget("ageclass")).setSize(1);
         assertTrue("Equals did not detect different class property size", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         object.getxWikiClass().getFields().remove("ageclass");
         assertTrue("Equals did not detect different null class", !object.equals(object2));

         object2 = (BaseObject)object.clone();
         object.setxWikiClass(null);
         assertTrue("Equals did not detect different null class", !object.equals(object2));

     }

     public void testMergeObject() {
         BaseObject object = prepareObject();
         BaseObject object2 = (BaseObject)object.clone();

         object2.merge(object);
         // test merging and equals at the same time
         assertTrue("Merging did not created equals objects", object.equals(object2));

         object2 = (BaseObject)object.clone();
         object2.getFields().remove("age");
         object2.merge(object);
         assertTrue("Merging did not created equals objects", object.equals(object2));

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object2.safeget("age")).setName("titi");
         object2.merge(object);
         assertTrue("Merging overrided age property", !object.equals(object2));

     }


}
