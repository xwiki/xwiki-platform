

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.NumberClass;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

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


    public void testEqualsNumberProperty() {
         // Test both cloning and equals
         IntegerProperty prop = Utils.prepareIntegerProperty();
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
         assertNotSame("IntegerProperty cannot be equals to StringProperty", iprop, sprop);

         StringProperty prop = Utils.prepareStringProperty();
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

    public void testEqualsListProperty() {
          // Test both cloning and equals
          ListProperty iprop = new ListProperty();
          StringProperty sprop = new StringProperty();
          assertNotSame("ListProperty cannot be equals to StringProperty", iprop, sprop);

          ListProperty prop = Utils.prepareStringListProperty();
          ListProperty prop2 = (ListProperty) prop.clone();

          // test cloning and equals at the same time
          assertEquals("Cloning did not created equals list property", prop, prop2);

          // Test value (this is string specific)
          prop2 = (ListProperty) prop.clone();
          List list = new ArrayList();
          list.add("1");
          list.add("2");
          prop2.setValue(list);
          assertNotSame("Equals did not detect different value", prop, prop2);
          prop2 = (ListProperty) prop.clone();
          prop2.setValue(null);
          assertNotSame("Equals did not detect different value with null", prop, prop2);
          prop.setValue(null);
          assertEquals("Equals did not detect same null value", prop, prop2);
      }

    public void testEqualsObject() throws XWikiException {
        BaseObject object = Utils.prepareObject();
        testEqualsObject(object);
    }

    public void testEqualsAdvancedObject() throws XWikiException {
        BaseObject object = Utils.prepareAdvancedObject();
        testEqualsObject(object);

        BaseObject object2 = (BaseObject)object.clone();
        ((StringProperty)object.safeget("category")).setValue(null);
        assertNotSame("Equals did not detect different List field value", object, object2);
    }

     public void testEqualsObject(BaseObject object) throws XWikiException {
         BaseObject object2 = (BaseObject)object.clone();

         // test cloning and equals at the same time
         assertEquals("Cloning did not created equals objects", object, object2);

         object2.setName("titi");
         assertNotSame("Equals did not detect different object name", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object.safeget("age")).setName("titi");
         assertNotSame("Equals did not detect changed field property", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object.safeget("age")).setValue(new Integer(45));
         assertNotSame("Equals did not detect changed field property", object, object2);

         object2 = (BaseObject)object.clone();
         object.getFields().remove("age");
         assertNotSame("Equals did not detect missing age field", object, object2);

         object2 = (BaseObject)object.clone();
         object.getxWikiClass().safeget("age").setName("titi");
         assertNotSame("Equals did not detect different class property name", object, object2);

         object2 = (BaseObject)object.clone();
         ((NumberClass)object.getxWikiClass().safeget("age")).setSize(1);
         assertNotSame("Equals did not detect different class property size", object, object2);

         object2 = (BaseObject)object.clone();
         object.getxWikiClass().getFields().remove("age");
         assertNotSame("Equals did not detect different null class", object, object2);

         object2 = (BaseObject)object.clone();
         object.setxWikiClass(null);
         assertNotSame("Equals did not detect different null class", object, object2);
     }

     public void testMergeObject() throws XWikiException {
         BaseObject object = Utils.prepareObject();
         BaseObject object2 = (BaseObject)object.clone();

         object2.merge(object);
         // test merging and equals at the same time
         assertEquals("Merging did not created equals objects", object, object2);

         object2 = (BaseObject)object.clone();
         object2.getFields().remove("age");
         object2.merge(object);
         assertEquals("Merging did not created equals objects", object, object2);

         object2 = (BaseObject)object.clone();
         ((BaseProperty)object2.safeget("age")).setName("titi");
         object2.merge(object);
         assertNotSame("Merging overrided age property", object, object2);

     }


}
